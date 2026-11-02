---
title: Commands
description: Declare commands with loader-aware permissions.
---

# Commands

Use `SyrupCommands` to declare commands during mod initialization. The helper registers them with the active loader.

```java
import net.syrupstudios.syruplibrary.command.SyrupCommands;
import net.minecraft.commands.Commands;

SyrupCommands.register("my_mod", commands -> {
    var homeSetPermission = commands.subcommandPermission(
            "home", "set", SyrupCommands.Access.GAME_MASTERS);
    commands.everyone("home", command -> command
            .then(Commands.literal("set")
                    .requires(homeSetPermission)
                    .executes(context -> 1)));
    commands.admin("reload", command -> command.executes(context -> 1));
});
```

`everyone` uses the loader's default permission for all users. `admin` requires game-master permission by default. `command(name, access, extraRequirement, configure)` supports an additional source predicate.

Call `subcommandPermission(root, path, access)` directly in the registration callback during initialization, then capture its result and pass it to the subcommand builder's `.requires(...)`. Do not call it inside the `configure` callback; that callback runs later when the command tree is built. The method also has an overload with an `extraRequirement` predicate. The root is one literal name. The path uses dot-separated literal names, such as `"set"` or `"config.set"`. Each node uses `<modId>.command.<root>.<path>`. Nodes are registered when this method is called, before Forge and NeoForge gather permission nodes. Duplicate nodes and collisions with declared root command nodes throw `IllegalArgumentException`.

Brigadier checks the root requirement while traversing to a child. A child permission is independent, but it cannot grant access when the root requirement denies access. Both short aliases and namespaced commands use the same root and child requirements.

The short overload uses the mod ID as the command namespace and enables both short and namespaced forms. A command named `home` is available as `/home` and `/my_mod:home`. The full overload lets you set the mod ID, namespace, whether to register namespaced commands, and whether to register the short alias. When namespaced commands are disabled, the short form is registered. When they are enabled, the namespaced form is always registered; the short form is registered only when aliases are enabled.

Root permissions use `<modId>.command.<name>`. For both root and subcommand nodes, `EVERYONE` allows all users by default and `GAME_MASTERS` requires vanilla game-master permission by default. Fabric uses its permission API when available (`EVERYONE` level 0, `GAME_MASTERS` level 2); without that API it uses the vanilla fallback. Forge and NeoForge register boolean permission nodes whose fallback is allow for `EVERYONE` and game-master permission for `GAME_MASTERS`. Declare commands and subcommand permissions once during initialization. The library retains declarations for dispatch when the loader registers its command tree.
