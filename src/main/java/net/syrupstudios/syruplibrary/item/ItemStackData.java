package net.syrupstudios.syruplibrary.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

//? if >=1.21 {
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
//?}

/** Provides defensive access to an item's custom data across Minecraft versions. */
public final class ItemStackData {
    private ItemStackData() {}

    /** Reads custom data into a defensive copy. */
    public static CompoundTag read(ItemStack stack) {
        //? if >=1.21 {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        //?} else {
        /*CompoundTag tag = stack.getTag();
        return tag == null ? new CompoundTag() : tag.copy();
        *///?}
    }

    /** Updates custom data through a defensive copy and writes the result back. */
    public static void update(ItemStack stack, Consumer<CompoundTag> updater) {
        CompoundTag tag = read(stack);
        updater.accept(tag);
        //? if >=1.21 {
        CustomData.set(DataComponents.CUSTOM_DATA, stack, tag);
        //?} else {
        /*stack.setTag(tag);
        *///?}
    }
}
