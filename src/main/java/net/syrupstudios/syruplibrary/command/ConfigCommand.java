package net.syrupstudios.syruplibrary.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import de.marhali.json5.Json5;
import de.marhali.json5.Json5Array;
import de.marhali.json5.Json5Element;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.syrupstudios.syruplibrary.SyrupLibrary;
import net.syrupstudios.syruplibrary.config.ConfigSpec;
import net.syrupstudios.syruplibrary.config.Json5InputGuard;
import net.syrupstudios.syruplibrary.config.RegisteredConfig;
import net.syrupstudios.syruplibrary.config.SyrupConfigManager;
import net.syrupstudios.syruplibrary.config.diagnostic.ConfigUpdateResult;
import net.syrupstudios.syruplibrary.config.value.ConfigValue;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** Server commands for configs that explicitly allow remote edits. */
public final class ConfigCommand {
    private static final int MAX_VALUE_LENGTH = 4096;
    private static final int MAX_PATHS = 100;
    private static final int MAX_PATH_OUTPUT_LENGTH = 3000;
    private static final Json5 JSON5 = new Json5();

    private ConfigCommand() {}

    public static void register() {
        SyrupCommands.register(SyrupLibrary.MOD_ID, registrar -> registrar.command("syrupconfig",
                SyrupCommands.Access.GAME_MASTERS, ConfigCommand::allowed, command ->
                command.then(Commands.literal("list").executes(context -> list(context.getSource())))
                        .then(Commands.literal("paths")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .suggests(ConfigCommand::suggestConfigIds)
                                        .executes(context -> paths(context.getSource(),
                                                StringArgumentType.getString(context, "id")))))
                        .then(Commands.literal("get")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .suggests(ConfigCommand::suggestConfigIds)
                                        .then(Commands.argument("path", StringArgumentType.word())
                                                .suggests(ConfigCommand::suggestPaths)
                                                .executes(context -> get(context.getSource(),
                                                        StringArgumentType.getString(context, "id"),
                                                        StringArgumentType.getString(context, "path"))))))
                        .then(Commands.literal("set")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .suggests(ConfigCommand::suggestConfigIds)
                                        .then(Commands.argument("path", StringArgumentType.word())
                                                .suggests(ConfigCommand::suggestPaths)
                                                .then(Commands.argument("value", StringArgumentType.greedyString())
                                                        .executes(context -> set(context.getSource(),
                                                                StringArgumentType.getString(context, "id"),
                                                                StringArgumentType.getString(context, "path"),
                                                                StringArgumentType.getString(context, "value")))))))));
    }

    private static int list(CommandSourceStack source) {
        if (!allowed(source)) return denied(source);
        var ids = SyrupConfigManager.getInstance().registeredConfigs().values().stream()
                .map(RegisteredConfig::spec).filter(ConfigSpec::remotelyEditable)
                .map(ConfigSpec::id).sorted().toList();
        source.sendSuccess(() -> Component.literal(ids.isEmpty()
                ? "No remotely editable configs are registered."
                : "Remotely editable configs: " + String.join(", ", ids)), false);
        return ids.size();
    }

    private static int paths(CommandSourceStack source, String id) {
        if (!allowed(source)) return denied(source);
        RegisteredConfig config = find(source, id);
        if (config == null) return 0;
        var values = config.spec().values();
        StringBuilder output = new StringBuilder("Declared paths for ").append(id).append(": ");
        int shown = 0;
        for (ConfigValue<?> value : values) {
            int extraLength = value.path().length() + (shown == 0 ? 0 : 2);
            if (shown == MAX_PATHS || output.length() + extraLength > MAX_PATH_OUTPUT_LENGTH) break;
            if (shown > 0) output.append(", ");
            output.append(value.path());
            shown++;
        }
        if (shown < values.size()) output.append(" … (").append(values.size() - shown).append(" more)");
        if (values.isEmpty()) output.append("no values");
        source.sendSuccess(() -> Component.literal(output.toString()), false);
        return shown;
    }

    private static int get(CommandSourceStack source, String id, String path) {
        if (!allowed(source)) return denied(source);
        RegisteredConfig config = find(source, id);
        if (config == null) return 0;
        ConfigValue<?> value = findValue(source, config, path);
        if (value == null) return 0;
        Object current = config.configuredSnapshot().get(value);
        final String encoded;
        try {
            encoded = encode(value, current);
        } catch (IOException exception) {
            source.sendFailure(Component.literal("Could not serialize the configured value."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(id + "." + path + " = " + encoded), false);
        return 1;
    }

    private static int set(CommandSourceStack source, String id, String path, String input) {
        if (!allowed(source)) return denied(source);
        RegisteredConfig config = find(source, id);
        if (config == null) return 0;
        ConfigValue<?> value = findValue(source, config, path);
        if (value == null) return 0;
        if (input.length() > MAX_VALUE_LENGTH) {
            source.sendFailure(Component.literal("Config value is too long (maximum 4096 characters)."));
            return 0;
        }
        final Object decoded;
        try {
            Json5Element wrapper = Json5InputGuard.parse("[" + input + "]");
            if (wrapper == null || !wrapper.isJson5Array()) throw new IllegalArgumentException("Expected one JSON5 value");
            Json5Array values = wrapper.getAsJson5Array();
            if (values.size() != 1) throw new IllegalArgumentException("Expected one JSON5 value");
            Json5Element element = values.get(0);
            decoded = value.type().decode(element);
            value.validate(decoded);
        } catch (RuntimeException exception) {
            source.sendFailure(Component.literal("Invalid value for " + id + "." + path + ": " + usefulMessage(exception)));
            return 0;
        }
        ConfigUpdateResult result = update(config, value, decoded);
        if (!result.successful()) {
            source.sendFailure(Component.literal("Could not update " + id + "." + path + ": "
                    + result.issues().stream().map(issue -> issue.message()).findFirst().orElse("update rejected")));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Updated " + id + "." + path + " and saved the config."), true);
        return 1;
    }

    private static RegisteredConfig find(CommandSourceStack source, String id) {
        RegisteredConfig config = SyrupConfigManager.getInstance().find(id).orElse(null);
        if (config == null || !config.spec().remotelyEditable()) {
            source.sendFailure(Component.literal("Config is not registered for remote editing."));
            return null;
        }
        return config;
    }

    private static CompletableFuture<Suggestions> suggestConfigIds(
            CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        if (!allowed(context.getSource())) return builder.buildFuture();
        var ids = SyrupConfigManager.getInstance().registeredConfigs().values().stream()
                .map(RegisteredConfig::spec).filter(ConfigSpec::remotelyEditable)
                .map(ConfigSpec::id).toList();
        return SharedSuggestionProvider.suggest(ids, builder);
    }

    private static CompletableFuture<Suggestions> suggestPaths(
            CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        if (!allowed(context.getSource())) return builder.buildFuture();
        String id = StringArgumentType.getString(context, "id");
        RegisteredConfig config = SyrupConfigManager.getInstance().find(id).orElse(null);
        if (config == null || !config.spec().remotelyEditable()) return builder.buildFuture();
        return SharedSuggestionProvider.suggest(config.spec().values().stream().map(ConfigValue::path).toList(), builder);
    }

    private static ConfigValue<?> findValue(CommandSourceStack source, RegisteredConfig config, String path) {
        return config.spec().values().stream().filter(value -> value.path().equals(path)).findFirst().orElseGet(() -> {
            source.sendFailure(Component.literal("Config value is not declared."));
            return null;
        });
    }

    private static <T> ConfigUpdateResult update(RegisteredConfig config, ConfigValue<T> value, Object decoded) {
        return config.updateAndSaveAll(Map.of(value, value.validate(decoded)));
    }

    private static <T> String encode(ConfigValue<T> value, Object current) throws IOException {
        return JSON5.serialize(value.type().encode(value.cast(current)));
    }

    private static boolean allowed(CommandSourceStack source) {
        //? if >=1.21.11 {
        /*return Commands.hasPermission(Commands.LEVEL_OWNERS).test(source);
        *///?} else {
        return source.hasPermission(4);
        //?}
    }

    private static int denied(CommandSourceStack source) {
        source.sendFailure(Component.literal("Permission level 4 is required."));
        return 0;
    }

    private static String usefulMessage(Exception exception) {
        return exception.getMessage() == null || exception.getMessage().isBlank()
                ? "value could not be decoded" : exception.getMessage();
    }
}
