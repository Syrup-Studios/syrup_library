---
title: Item custom data
description: Read and update item custom data safely.
---

# Item custom data

Use `ItemStackData` to read or change an item's custom data without exposing the stack's stored tag to accidental mutation.

```java
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.syrupstudios.syruplibrary.item.ItemStackData;

CompoundTag data = ItemStackData.read(stack);
ItemStackData.update(stack, tag -> tag.putString("owner", playerName));
```

`read` returns a defensive copy. Changes to that copy do not change the item. Pass changes to `update`; it reads a copy, applies the consumer, then writes the result back to the stack. Use the Minecraft-version-specific custom data representation through this API.
