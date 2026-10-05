---
title: Player profiles
description: Resolve and cache player profiles.
---

# Player profiles

Use `SyrupProfiles.resolveProfileAsync` for profile lookups. The callback receives a texture-bearing `GameProfile` or `null` when the name is invalid, the resolver is unavailable, or lookup fails.

```java
import com.mojang.authlib.GameProfile;
import net.syrupstudios.syruplibrary.profile.SyrupProfiles;

SyrupProfiles.resolveProfileAsync(playerName, profile -> {
    if (profile == null) return;
    // Use the profile. Schedule game-state changes on the required game thread.
});
```

The callback can run immediately on the caller thread for cached results or early failures. A lookup callback can run on the completion thread. Do not assume one thread; schedule game-state changes on the game thread when needed.

Successful profiles are cached by lowercase player name. Concurrent requests for the same name share a lookup. Failed lookups use a ten-minute cooldown. `getCachedProfile` returns a cached profile or `null`. `getOrResolveServerProfile` returns a cached profile; on modern versions it can start an asynchronous lookup and return `null`.

On Minecraft 1.21.11 and later, asynchronous lookup needs a profile resolver. `getOrResolveServerProfile` sets the resolver from its server argument. If you call `resolveProfileAsync` directly, set the active server with `setProfileResolver(server)` first. Clear it with `setProfileResolver(null)` when the server shuts down. Without a resolver, asynchronous lookup returns `null` to the callback.

`clearCache` clears cached profiles, in-flight tracking, and failure cooldowns. It invalidates old requests for cache updates, but their callbacks still complete. On Minecraft 1.21.11 and later, clear the resolver at shutdown. On all versions, call `clearCache()` at shutdown.
