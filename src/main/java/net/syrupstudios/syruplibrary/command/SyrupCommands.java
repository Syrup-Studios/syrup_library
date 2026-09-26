package net.syrupstudios.syruplibrary.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.syrupstudios.syruplibrary.loaders.Platform;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

/** Registers loader-aware commands and their permission predicates. */
public final class SyrupCommands {
    private static final List<CommandSpec> COMMANDS = new ArrayList<>();

    private SyrupCommands() {}

    public enum Access {
        EVERYONE,
        GAME_MASTERS
    }

    /** Declares commands during mod initialization. */
    public static final class Registrar {
        private final String modId;
        private final String namespace;
        private final boolean namespaced;
        private final boolean aliases;

        private Registrar(String modId, String namespace, boolean namespaced, boolean aliases) {
            this.modId = modId;
            this.namespace = namespace;
            this.namespaced = namespaced;
            this.aliases = aliases;
        }

        public void command(String name, Access access,
                            Predicate<CommandSourceStack> extraRequirement,
                            Consumer<LiteralArgumentBuilder<CommandSourceStack>> configure) {
            validate(name, "name");
            Objects.requireNonNull(access, "access");
            Objects.requireNonNull(extraRequirement, "extraRequirement");
            Objects.requireNonNull(configure, "configure");
            COMMANDS.add(new CommandSpec(namespace, namespaced, aliases, name,
                    Platform.INSTANCE.commandPermission(modId, "command." + name,
                            access == Access.EVERYONE), extraRequirement, configure));
        }

        public void command(String name, Access access,
                            Consumer<LiteralArgumentBuilder<CommandSourceStack>> configure) {
            command(name, access, source -> true, configure);
        }

        public void everyone(String name,
                             Consumer<LiteralArgumentBuilder<CommandSourceStack>> configure) {
            command(name, Access.EVERYONE, configure);
        }

        public void admin(String name,
                          Consumer<LiteralArgumentBuilder<CommandSourceStack>> configure) {
            command(name, Access.GAME_MASTERS, configure);
        }
    }

    /** Declares a command set during mod initialization. */
    public static void register(String modId, Consumer<Registrar> registrarConsumer) {
        register(modId, modId, true, true, registrarConsumer);
    }

    /** Declares a command set during mod initialization. */
    public static void register(String modId, String namespace, boolean namespaced, boolean aliases,
                                Consumer<Registrar> registrarConsumer) {
        validate(modId, "modId");
        validate(namespace, "namespace");
        Objects.requireNonNull(registrarConsumer, "registrarConsumer")
                .accept(new Registrar(modId, namespace, namespaced, aliases));
    }

    /** Builds and registers all commands for the active loader. */
    public static void dispatch(CommandDispatcher<CommandSourceStack> dispatcher) {
        Objects.requireNonNull(dispatcher, "dispatcher");
        for (CommandSpec spec : COMMANDS) {
            if (!spec.namespaced() || spec.aliases()) {
                dispatcher.register(buildCommand(spec, spec.name()));
            }
            if (spec.namespaced()) {
                dispatcher.register(buildCommand(spec, spec.namespace() + ":" + spec.name()));
            }
        }
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildCommand(CommandSpec spec, String name) {
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal(name)
                .requires(source -> spec.permission().test(source) && spec.extraRequirement().test(source));
        spec.configure().accept(command);
        return command;
    }

    private static void validate(String value, String name) {
        if (Objects.requireNonNull(value, name).isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }

    private record CommandSpec(
            String namespace,
            boolean namespaced,
            boolean aliases,
            String name,
            Predicate<CommandSourceStack> permission,
            Predicate<CommandSourceStack> extraRequirement,
            Consumer<LiteralArgumentBuilder<CommandSourceStack>> configure) {}
}
