package net.syrupstudios.syruplibrary.profile;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
//? if >=1.21.11
import net.minecraft.server.players.ProfileResolver;
import net.minecraft.world.level.block.entity.SkullBlockEntity;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/** Resolves and caches player profiles with version-compatible Minecraft APIs. */
public final class SyrupProfiles {
    private static final Map<String, GameProfile> PROFILE_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, CompletableFuture<GameProfile>> IN_FLIGHT = new ConcurrentHashMap<>();
    private static final Map<String, Long> FAILED_UNTIL = new ConcurrentHashMap<>();
    private static final AtomicLong CACHE_GENERATION = new AtomicLong();
    //? if >=1.21.11
    private static volatile ProfileResolver PROFILE_RESOLVER;
    private static final Pattern VALID_PLAYER_NAME = Pattern.compile("^[A-Za-z0-9_]{1,16}$");
    private static final long FAILURE_COOLDOWN_NANOS = TimeUnit.MINUTES.toNanos(10);

    private SyrupProfiles() {}

    //? if >=1.21.11 {
    /** Sets the resolver required by Minecraft 1.21.11+ lookups; clear it when the server shuts down. */
    public static void setProfileResolver(MinecraftServer server) {
        PROFILE_RESOLVER = server == null ? null : server.services().profileResolver();
    }
    //?}

    /** Resolves a player profile asynchronously; the callback receives null on failure and may run on the completion thread. */
    public static void resolveProfileAsync(String ownerName, Consumer<GameProfile> callback) {
        String normalizedName = normalizePlayerName(ownerName);
        if (normalizedName == null) {
            callback.accept(null);
            return;
        }
        //? if >=1.21.11 {
        if (PROFILE_RESOLVER == null) {
            callback.accept(null);
            return;
        }
        //?}

        String cacheKey = normalizedName.toLowerCase(Locale.ROOT);
        GameProfile cached = PROFILE_CACHE.get(cacheKey);
        if (cached != null) {
            callback.accept(cached);
            return;
        }
        if (isFailureCoolingDown(cacheKey)) {
            callback.accept(null);
            return;
        }

        CompletableFuture<GameProfile> request = new CompletableFuture<>();
        CompletableFuture<GameProfile> existing = IN_FLIGHT.putIfAbsent(cacheKey, request);
        CompletableFuture<GameProfile> result = existing != null ? existing : request;
        result.whenComplete((profile, error) -> callback.accept(error == null ? profile : null));
        if (existing != null) return;

        long generation = CACHE_GENERATION.get();
        try {
            //? if >=1.21.11 {
            ProfileResolver resolver = PROFILE_RESOLVER;
            CompletableFuture<GameProfile> lookup = resolver == null
                    ? CompletableFuture.completedFuture(null)
                    : CompletableFuture.supplyAsync(() -> resolver.fetchByName(normalizedName).orElse(null));
            lookup.whenComplete((profile, error) -> completeLookup(cacheKey, request, generation, profile, error));
            //?} else if >=1.21 {
            /*SkullBlockEntity.fetchGameProfile(normalizedName).whenComplete((optionalProfile, error) ->
                    completeLookup(cacheKey, request, generation,
                            error == null ? optionalProfile.orElse(null) : null, error));
            *///?} else {
            /*SkullBlockEntity.updateGameprofile(new GameProfile(null, normalizedName), profile ->
                    completeLookup(cacheKey, request, generation, profile, null));
            *///?}
        } catch (RuntimeException error) {
            completeLookup(cacheKey, request, generation, null, error);
        }
    }

    /** Returns a cached profile, or null when no profile is cached. */
    public static GameProfile getCachedProfile(String ownerName) {
        return ownerName != null ? PROFILE_CACHE.get(ownerName.toLowerCase(Locale.ROOT)) : null;
    }

    /** Returns a cached server profile; modern versions may start async work and return null. */
    public static GameProfile getOrResolveServerProfile(String ownerName, MinecraftServer server) {
        String normalizedName = normalizePlayerName(ownerName);
        if (normalizedName == null) return null;

        String cacheKey = normalizedName.toLowerCase(Locale.ROOT);
        GameProfile profile = PROFILE_CACHE.get(cacheKey);
        if (profile != null || server == null || isFailureCoolingDown(cacheKey)) return profile;

        //? if >=1.21.11 {
        PROFILE_RESOLVER = server.services().profileResolver();
        resolveProfileAsync(normalizedName, ignored -> {});
        return null;
        //?} else if >=1.21 {
        /*resolveProfileAsync(normalizedName, ignored -> {});
        return null;
        *///?} else {
        /*try {
            profile = server.getProfileCache().get(normalizedName)
                    .map(cachedProfile -> server.getSessionService().fillProfileProperties(cachedProfile, true))
                    .filter(SyrupProfiles::hasTextures)
                    .orElse(null);
        } catch (RuntimeException error) {
            profile = null;
        }
        if (profile != null) cacheProfile(cacheKey, profile);
        else recordFailure(cacheKey);
        return profile;
        *///?}
    }

    /** Returns whether a profile lookup should be attempted for the name. */
    public static boolean shouldAttemptResolution(String ownerName) {
        String normalizedName = normalizePlayerName(ownerName);
        if (normalizedName == null) return false;
        String cacheKey = normalizedName.toLowerCase(Locale.ROOT);
        return !PROFILE_CACHE.containsKey(cacheKey)
                && !IN_FLIGHT.containsKey(cacheKey)
                && !isFailureCoolingDown(cacheKey);
    }

    /** Starts profile resolution when the name is not already cached or cooling down. */
    public static void preload(String ownerName) {
        if (shouldAttemptResolution(ownerName)) resolveProfileAsync(ownerName, profile -> {});
    }

    /** Clears cached profiles and invalidates completion from older requests. */
    public static void clearCache() {
        CACHE_GENERATION.incrementAndGet();
        PROFILE_CACHE.clear();
        IN_FLIGHT.clear();
        FAILED_UNTIL.clear();
    }

    private static void completeLookup(String cacheKey, CompletableFuture<GameProfile> request,
                                       long generation, GameProfile profile, Throwable error) {
        GameProfile resolved = error == null && hasTextures(profile) ? profile : null;
        if (CACHE_GENERATION.get() == generation) {
            if (resolved != null) cacheProfile(cacheKey, resolved);
            else recordFailure(cacheKey);
        }
        request.complete(resolved);
        IN_FLIGHT.remove(cacheKey, request);
    }

    private static String normalizePlayerName(String ownerName) {
        if (ownerName == null) return null;
        String trimmed = ownerName.trim();
        return VALID_PLAYER_NAME.matcher(trimmed).matches() ? trimmed : null;
    }

    private static boolean hasTextures(GameProfile profile) {
        //? if >=1.21.11 {
        return profile != null && profile.properties().containsKey("textures")
                && !profile.properties().get("textures").isEmpty();
        //?} else {
        /*return profile != null && profile.getProperties().containsKey("textures")
                && !profile.getProperties().get("textures").isEmpty();
        *///?}
    }

    private static void cacheProfile(String requestedKey, GameProfile profile) {
        PROFILE_CACHE.put(requestedKey, profile);
        //? if >=1.21.11 {
        if (profile.name() != null) PROFILE_CACHE.put(profile.name().toLowerCase(Locale.ROOT), profile);
        //?} else {
        /*if (profile.getName() != null) PROFILE_CACHE.put(profile.getName().toLowerCase(Locale.ROOT), profile);
        *///?}
        FAILED_UNTIL.remove(requestedKey);
    }

    private static void recordFailure(String cacheKey) {
        FAILED_UNTIL.put(cacheKey, System.nanoTime() + FAILURE_COOLDOWN_NANOS);
    }

    private static boolean isFailureCoolingDown(String cacheKey) {
        Long failedUntil = FAILED_UNTIL.get(cacheKey);
        if (failedUntil == null) return false;
        if (System.nanoTime() - failedUntil < 0) return true;
        FAILED_UNTIL.remove(cacheKey, failedUntil);
        return false;
    }
}
