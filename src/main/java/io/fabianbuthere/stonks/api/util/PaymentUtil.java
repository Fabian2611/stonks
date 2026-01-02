package io.fabianbuthere.stonks.api.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

public class PaymentUtil {
    
    private static final String NETHERITE_COIN = "lightmanscurrency:coin_netherite";
    private static final String DIAMOND_COIN = "lightmanscurrency:coin_diamond";
    private static final String EMERALD_COIN = "lightmanscurrency:coin_emerald";
    private static final String GOLD_COIN = "lightmanscurrency:coin_gold";
    private static final String IRON_COIN = "lightmanscurrency:coin_iron";
    private static final String COPPER_COIN = "lightmanscurrency:coin_copper";
    
    private static final int NETHERITE_VALUE = 100000; // $1000.00
    private static final int DIAMOND_VALUE = 10000;    // $100.00
    private static final int EMERALD_VALUE = 1000;     // $10.00
    private static final int GOLD_VALUE = 100;         // $1.00
    private static final int IRON_VALUE = 10;          // $0.10
    private static final int COPPER_VALUE = 1;         // $0.01
    
    private static Boolean lcCoinsAvailable = null;
    
    /**
     * Awards payment to player using highest denomination coins possible.
     * Falls back to vanilla items if Lightman's Currency is not available.
     * @param player The player to award payment to
     * @param amount Total payment amount (floored to integer)
     */
    public static void awardPayment(ServerPlayer player, int amount) {
        if (amount <= 0) return;
        
        // Check if LC coins are available (cached)
        if (lcCoinsAvailable == null) {
            lcCoinsAvailable = ForgeRegistries.ITEMS.containsKey(new ResourceLocation(NETHERITE_COIN));
        }
        
        if (lcCoinsAvailable) {
            awardPaymentLC(player, amount);
        } else {
            awardPaymentVanilla(player, amount);
        }
    }
    
    private static void awardPaymentLC(ServerPlayer player, int amount) {
        int remaining = amount;
        
        // Award netherite coins (100000 each = $1000.00)
        if (remaining >= NETHERITE_VALUE) {
            int count = remaining / NETHERITE_VALUE;
            remaining %= NETHERITE_VALUE;
            giveCoins(player, NETHERITE_COIN, count);
        }
        
        // Award diamond coins (10000 each = $100.00)
        if (remaining >= DIAMOND_VALUE) {
            int count = remaining / DIAMOND_VALUE;
            remaining %= DIAMOND_VALUE;
            giveCoins(player, DIAMOND_COIN, count);
        }
        
        // Award emerald coins (1000 each = $10.00)
        if (remaining >= EMERALD_VALUE) {
            int count = remaining / EMERALD_VALUE;
            remaining %= EMERALD_VALUE;
            giveCoins(player, EMERALD_COIN, count);
        }
        
        // Award gold coins (100 each = $1.00)
        if (remaining >= GOLD_VALUE) {
            int count = remaining / GOLD_VALUE;
            remaining %= GOLD_VALUE;
            giveCoins(player, GOLD_COIN, count);
        }
        
        // Award iron coins (10 each = $0.10)
        if (remaining >= IRON_VALUE) {
            int count = remaining / IRON_VALUE;
            remaining %= IRON_VALUE;
            giveCoins(player, IRON_COIN, count);
        }
        
        // Award copper coins (1 each = $0.01)
        if (remaining >= COPPER_VALUE) {
            int count = remaining / COPPER_VALUE;
            giveCoins(player, COPPER_COIN, count);
        }
    }
    
    private static void awardPaymentVanilla(ServerPlayer player, int amount) {
        int remaining = amount;
        
        // Use vanilla items as currency
        // 100000 = netherite ingot, 10000 = diamond, 1000 = emerald, 100 = gold ingot, 10 = iron ingot, 1 = copper ingot
        
        if (remaining >= NETHERITE_VALUE) {
            int count = remaining / NETHERITE_VALUE;
            remaining %= NETHERITE_VALUE;
            giveVanillaItem(player, Items.NETHERITE_INGOT, count);
        }
        
        if (remaining >= DIAMOND_VALUE) {
            int count = remaining / DIAMOND_VALUE;
            remaining %= DIAMOND_VALUE;
            giveVanillaItem(player, Items.DIAMOND, count);
        }
        
        if (remaining >= EMERALD_VALUE) {
            int count = remaining / EMERALD_VALUE;
            remaining %= EMERALD_VALUE;
            giveVanillaItem(player, Items.EMERALD, count);
        }
        
        if (remaining >= GOLD_VALUE) {
            int count = remaining / GOLD_VALUE;
            remaining %= GOLD_VALUE;
            giveVanillaItem(player, Items.GOLD_INGOT, count);
        }
        
        if (remaining >= IRON_VALUE) {
            int count = remaining / IRON_VALUE;
            remaining %= IRON_VALUE;
            giveVanillaItem(player, Items.IRON_INGOT, count);
        }
        
        if (remaining >= COPPER_VALUE) {
            int count = remaining / COPPER_VALUE;
            giveVanillaItem(player, Items.COPPER_INGOT, count);
        }
    }
    
    private static void giveCoins(ServerPlayer player, String coinId, int count) {
        if (count <= 0) return;
        
        Item coin = ForgeRegistries.ITEMS.getValue(new ResourceLocation(coinId));
        if (coin == null) {
            io.fabianbuthere.stonks.Stonks.LOGGER.warn("Coin item not found: " + coinId);
            return;
        }
        
        // Give coins in stacks (max 64 per stack)
        while (count > 0) {
            int stackSize = Math.min(count, 64);
            ItemStack stack = new ItemStack(coin, stackSize);
            player.addItem(stack);
            count -= stackSize;
        }
    }
    
    private static void giveVanillaItem(ServerPlayer player, Item item, int count) {
        if (count <= 0) return;
        
        // Give items in stacks (max 64 per stack)
        while (count > 0) {
            int stackSize = Math.min(count, 64);
            ItemStack stack = new ItemStack(item, stackSize);
            player.addItem(stack);
            count -= stackSize;
        }
    }
    
    /**
     * Formats payment amount with coin breakdown
     * @param amount Total payment amount in cents
     * @return Formatted string showing coin breakdown
     */
    public static String formatPayment(int amount) {
        if (amount <= 0) return "§e$0.00";
        
        double dollars = amount / 100.0;
        StringBuilder result = new StringBuilder(String.format("§e$%.2f §7(", dollars));
        boolean first = true;
        
        int netherite = amount / NETHERITE_VALUE;
        amount %= NETHERITE_VALUE;
        
        int diamond = amount / DIAMOND_VALUE;
        amount %= DIAMOND_VALUE;
        
        int emerald = amount / EMERALD_VALUE;
        amount %= EMERALD_VALUE;
        
        int gold = amount / GOLD_VALUE;
        amount %= GOLD_VALUE;
        
        int iron = amount / IRON_VALUE;
        amount %= IRON_VALUE;
        
        int copper = amount;
        
        if (netherite > 0) {
            result.append("§8").append(netherite).append("N");
            first = false;
        }
        if (diamond > 0) {
            if (!first) result.append("§7, ");
            result.append("§b").append(diamond).append("D");
            first = false;
        }
        if (emerald > 0) {
            if (!first) result.append("§7, ");
            result.append("§a").append(emerald).append("E");
            first = false;
        }
        if (gold > 0) {
            if (!first) result.append("§7, ");
            result.append("§6").append(gold).append("G");
            first = false;
        }
        if (iron > 0) {
            if (!first) result.append("§7, ");
            result.append("§f").append(iron).append("I");
            first = false;
        }
        if (copper > 0) {
            if (!first) result.append("§7, ");
            result.append("§c").append(copper).append("C");
        }
        
        result.append("§7)");
        return result.toString();
    }
    
    /**
     * Takes money from a player by removing coins from their inventory
     * @param player The player to take money from
     * @param amount The amount to take
     * @return true if successful, false if player doesn't have enough
     */
    public static boolean takeMoneyFromPlayer(ServerPlayer player, int amount) {
        if (amount <= 0) return true;
        
        // Check if LC coins are available (cached)
        if (lcCoinsAvailable == null) {
            lcCoinsAvailable = ForgeRegistries.ITEMS.containsKey(new ResourceLocation(NETHERITE_COIN));
        }
        
        // Calculate total money player has
        int totalMoney = 0;
        if (lcCoinsAvailable) {
            totalMoney += countCoins(player, NETHERITE_COIN) * NETHERITE_VALUE;
            totalMoney += countCoins(player, DIAMOND_COIN) * DIAMOND_VALUE;
            totalMoney += countCoins(player, EMERALD_COIN) * EMERALD_VALUE;
            totalMoney += countCoins(player, GOLD_COIN) * GOLD_VALUE;
            totalMoney += countCoins(player, IRON_COIN) * IRON_VALUE;
            totalMoney += countCoins(player, COPPER_COIN) * COPPER_VALUE;
        } else {
            totalMoney += countItems(player, Items.NETHERITE_INGOT) * NETHERITE_VALUE;
            totalMoney += countItems(player, Items.DIAMOND) * DIAMOND_VALUE;
            totalMoney += countItems(player, Items.EMERALD) * EMERALD_VALUE;
            totalMoney += countItems(player, Items.GOLD_INGOT) * GOLD_VALUE;
            totalMoney += countItems(player, Items.IRON_INGOT) * IRON_VALUE;
            totalMoney += countItems(player, Items.COPPER_INGOT) * COPPER_VALUE;
        }
        
        if (totalMoney < amount) {
            return false;
        }
        
        // Remove coins/items
        int remaining = amount;
        if (lcCoinsAvailable) {
            remaining = removeCoins(player, COPPER_COIN, remaining, COPPER_VALUE);
            remaining = removeCoins(player, IRON_COIN, remaining, IRON_VALUE);
            remaining = removeCoins(player, GOLD_COIN, remaining, GOLD_VALUE);
            remaining = removeCoins(player, EMERALD_COIN, remaining, EMERALD_VALUE);
            remaining = removeCoins(player, DIAMOND_COIN, remaining, DIAMOND_VALUE);
            remaining = removeCoins(player, NETHERITE_COIN, remaining, NETHERITE_VALUE);
        } else {
            remaining = removeItems(player, Items.COPPER_INGOT, remaining, COPPER_VALUE);
            remaining = removeItems(player, Items.IRON_INGOT, remaining, IRON_VALUE);
            remaining = removeItems(player, Items.GOLD_INGOT, remaining, GOLD_VALUE);
            remaining = removeItems(player, Items.EMERALD, remaining, EMERALD_VALUE);
            remaining = removeItems(player, Items.DIAMOND, remaining, DIAMOND_VALUE);
            remaining = removeItems(player, Items.NETHERITE_INGOT, remaining, NETHERITE_VALUE);
        }
        
        return true;
    }
    
    /**
     * Gives money to a player
     */
    public static void giveMoneyToPlayer(ServerPlayer player, int amount) {
        awardPayment(player, amount);
    }
    
    private static int countCoins(ServerPlayer player, String coinId) {
        Item coin = ForgeRegistries.ITEMS.getValue(new ResourceLocation(coinId));
        if (coin == null) return 0;
        return countItems(player, coin);
    }
    
    private static int countItems(ServerPlayer player, Item item) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() == item) {
                count += stack.getCount();
            }
        }
        return count;
    }
    
    private static int removeCoins(ServerPlayer player, String coinId, int amountNeeded, int coinValue) {
        Item coin = ForgeRegistries.ITEMS.getValue(new ResourceLocation(coinId));
        if (coin == null) return amountNeeded;
        return removeItems(player, coin, amountNeeded, coinValue);
    }
    
    private static int removeItems(ServerPlayer player, Item item, int amountNeeded, int itemValue) {
        if (amountNeeded <= 0) return 0;
        
        int coinsNeeded = (int) Math.ceil((double) amountNeeded / itemValue);
        int coinsRemoved = 0;
        
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() == item) {
                int toRemove = Math.min(stack.getCount(), coinsNeeded - coinsRemoved);
                stack.shrink(toRemove);
                coinsRemoved += toRemove;
                
                if (coinsRemoved >= coinsNeeded) {
                    break;
                }
            }
        }
        
        int valueRemoved = coinsRemoved * itemValue;
        int remaining = amountNeeded - valueRemoved;
        
        // Give change if we took too much
        if (remaining < 0) {
            giveMoneyToPlayer(player, -remaining);
            remaining = 0;
        }
        
        return remaining;
    }
}
