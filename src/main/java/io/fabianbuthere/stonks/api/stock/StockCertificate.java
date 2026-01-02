package io.fabianbuthere.stonks.api.stock;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class StockCertificate {
    
    public static ItemStack createCertificate(Stock stock) {
        ItemStack paper = new ItemStack(Items.PAPER);
        
        // Add enchantment glint
        paper.enchant(Enchantments.UNBREAKING, 1);
        
        // Hide enchantments in tooltip
        CompoundTag tag = paper.getOrCreateTag();
        tag.putInt("HideFlags", 1);
        
        // Store stock data
        tag.put("StockData", stock.toNBT());
        
        // Set display name
        paper.setHoverName(Component.literal("Stock Certificate - " + stock.getCompanySymbol())
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        
        // Format date and time
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateStr = dateFormat.format(new Date(stock.getPurchaseTime()));
        
        // Add lore with stock information
        CompoundTag display = tag.getCompound("display");
        net.minecraft.nbt.ListTag lore = new net.minecraft.nbt.ListTag();
        
        lore.add(net.minecraft.nbt.StringTag.valueOf(
                Component.Serializer.toJson(Component.literal("Stock ID: " + stock.getStockId().toString().substring(0, 8))
                        .withStyle(ChatFormatting.GRAY))));
        lore.add(net.minecraft.nbt.StringTag.valueOf(
                Component.Serializer.toJson(Component.literal("Company: " + stock.getCompanySymbol())
                        .withStyle(ChatFormatting.YELLOW))));
        lore.add(net.minecraft.nbt.StringTag.valueOf(
                Component.Serializer.toJson(Component.literal("Purchase Date: " + dateStr)
                        .withStyle(ChatFormatting.AQUA))));
        
        // Convert cents to dollars for display
        double purchasePriceDollars = stock.getPurchasePrice() / 100.0;
        lore.add(net.minecraft.nbt.StringTag.valueOf(
                Component.Serializer.toJson(Component.literal("Purchase Price: $" + String.format("%.2f", purchasePriceDollars))
                        .withStyle(ChatFormatting.GREEN))));
        
        long timeSincePurchase = System.currentTimeMillis() - stock.getPurchaseTime();
        long freezeMinutes = io.fabianbuthere.stonks.config.StonksConfig.STOCK_FREEZE_MINUTES.get();
        long freezeMillis = freezeMinutes * 60L * 1000L;
        
        if (timeSincePurchase < freezeMillis) {
            long remainingMillis = freezeMillis - timeSincePurchase;
            long days = remainingMillis / (24L * 60L * 60L * 1000L);
            long hours = (remainingMillis % (24L * 60L * 60L * 1000L)) / (60L * 60L * 1000L);
            long minutes = (remainingMillis % (60L * 60L * 1000L)) / (60L * 1000L);
            
            lore.add(net.minecraft.nbt.StringTag.valueOf(
                    Component.Serializer.toJson(Component.literal(""))));
            lore.add(net.minecraft.nbt.StringTag.valueOf(
                    Component.Serializer.toJson(Component.literal("FROZEN - Cannot Sell")
                            .withStyle(ChatFormatting.RED, ChatFormatting.BOLD))));
            
            // Build time remaining string
            String timeStr;
            if (days > 0) {
                timeStr = days + "d " + hours + "h";
            } else if (hours > 0) {
                timeStr = hours + "h " + minutes + "m";
            } else {
                timeStr = minutes + "m";
            }
            
            lore.add(net.minecraft.nbt.StringTag.valueOf(
                    Component.Serializer.toJson(Component.literal("Time Remaining: " + timeStr)
                            .withStyle(ChatFormatting.RED))));
        } else {
            lore.add(net.minecraft.nbt.StringTag.valueOf(
                    Component.Serializer.toJson(Component.literal(""))));
            lore.add(net.minecraft.nbt.StringTag.valueOf(
                    Component.Serializer.toJson(Component.literal("✓ Ready to Sell")
                            .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD))));
        }
        
        // Show depreciation info
        double depreciationMultiplier = stock.getDepreciationMultiplier(System.currentTimeMillis());
        double depreciationPercent = (1.0 - depreciationMultiplier) * 100.0;
        if (depreciationPercent > 0.1) {
            lore.add(net.minecraft.nbt.StringTag.valueOf(
                    Component.Serializer.toJson(Component.literal(""))));
            lore.add(net.minecraft.nbt.StringTag.valueOf(
                    Component.Serializer.toJson(Component.literal("Depreciation: -" + String.format("%.1f", depreciationPercent) + "%")
                            .withStyle(ChatFormatting.GRAY))));
            lore.add(net.minecraft.nbt.StringTag.valueOf(
                    Component.Serializer.toJson(Component.literal("(2% per week)")
                            .withStyle(ChatFormatting.DARK_GRAY))));
        }
        
        display.put("Lore", lore);
        tag.put("display", display);
        
        return paper;
    }
    
    public static Stock getStockFromCertificate(ItemStack stack) {
        if (stack.getItem() != Items.PAPER) return null;
        
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains("StockData")) return null;
        
        return Stock.fromNBT(tag.getCompound("StockData"));
    }
    
    public static boolean isCertificate(ItemStack stack) {
        if (stack.getItem() != Items.PAPER) return false;
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains("StockData");
    }
}
