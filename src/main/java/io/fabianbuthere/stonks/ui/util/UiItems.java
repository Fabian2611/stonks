package io.fabianbuthere.stonks.ui.util;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class UiItems {
    private UiItems() {}

    public static ItemStack button(Item icon, Component title, String... lore) {
        ItemStack stack = new ItemStack(icon);
        stack.setHoverName(title);

        if (lore == null || lore.length == 0) return stack;

        CompoundTag display = stack.getOrCreateTagElement("display");
        ListTag list = new ListTag();
        for (String line : lore) {
            Component c = Component.literal(line).withStyle(ChatFormatting.GRAY);
            list.add(StringTag.valueOf(Component.Serializer.toJson(c)));
        }
        display.put("Lore", list);
        return stack;
    }

    public static ItemStack button(Item icon, String title) {
        return button(icon, Component.literal(title), (String[]) null);
    }

    public static ItemStack button(Item icon, Component title, int slot, Container ui, String... lore) {
        ItemStack stack = button(icon, title, lore);
        ui.setItem(slot, stack);
        return stack;
    }

    public static ItemStack button(Item icon, String title, int slot, Container ui, String... lore) {
        return button(icon, Component.translatable(title).withStyle(ChatFormatting.GOLD), slot, ui, lore);
    }

    public static ItemStack filler() {
        ItemStack stack = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
        stack.setHoverName(Component.literal(" "));
        return stack;
    }

    public static void fill(Container ui, int rows) {
        fill(ui, filler(), rows);
    }

    public static void fill(Container ui, ItemStack stack, int rows) {
        for (int i = 0; i < rows * 9; i++) {
            ui.setItem(i, stack.copy());
        }
    }
}
