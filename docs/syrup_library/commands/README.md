---
title: Commands
description: Declare commands with loader-aware permissions.
---

# Commands

Use `SyrupCommands` to declare commands during mod initialization. The helper registers them with the active loader.

```java
import net.syrupstudios.syruplibrary.command.SyrupCommands;

SyrupCommands.register("my_mod", commands -> {
    commands.everyone("home", command -> command.executes(context -> {
        // Run the command.
        return 1;
    }));
    commands.admin("reload", command -> command.executes(context -> 1));
});
```

`everyone` uses the loader's default permission for all users. `admin` requires game-master permission by default. `command(name, access, extraRequirement, configure)` supports an additional source predicate.

The short overload uses the mod ID as the command namespace and enables both short and namespaced forms. A command named `home` is available as `/home` and `/my_mod:home`. The full overload lets you set the mod ID, namespace, whether to register namespaced commands, and whether to register the short alias. When namespaced commands are disabled, the short form is registered. When they are enabled, the namespaced form is always registered; the short form is registered only when aliases are enabled.

Permissions are assigned when each command is declared. The permission node is `<modId>.command.<name>`. Loaders use their permission API when available and otherwise use vanilla access defaults. Declare commands once during initialization. The library retains declarations for dispatch when the loader registers its command tree.
