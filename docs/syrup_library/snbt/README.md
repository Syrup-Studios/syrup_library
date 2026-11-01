---
title: SNBT utilities
description: Format and parse SNBT with Syrup Library.
---

# SNBT utilities

`SyrupSnbt` formats SNBT and parses compound tags. Formatting expects valid SNBT. It normalizes whitespace outside quoted strings and keeps quoted contents unchanged.

```java
import net.minecraft.nbt.CompoundTag;
import net.syrupstudios.syruplibrary.nbt.SyrupSnbt;

String pretty = SyrupSnbt.format("{name:'A\\'B',values:[1,2,3]}");
CompoundTag tag = SyrupSnbt.parseCompound("{name:'Syrup'}");
String saved = SyrupSnbt.format(tag);
```

`parseCompound` accepts only a compound at the root. It throws Minecraft's `CommandSyntaxException` when the input is invalid.
