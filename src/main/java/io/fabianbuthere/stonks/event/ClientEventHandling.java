package io.fabianbuthere.stonks.event;

import io.fabianbuthere.stonks.Stonks;
import io.fabianbuthere.stonks.api.stock.Stock;
import io.fabianbuthere.stonks.api.stock.StockCertificate;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Stonks.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientEventHandling {
    
    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        var stack = event.getItemStack();
        
        // Check if it's a stock certificate
        if (!StockCertificate.isCertificate(stack)) {
            return;
        }
        
        Stock stock = StockCertificate.getStockFromCertificate(stack);
        if (stock == null) {
            return;
        }
        
        // Clear existing tooltip lines (except the first line which is the title)
        var tooltipLines = event.getToolTip();
        if (tooltipLines.size() > 1) {
            tooltipLines.subList(1, tooltipLines.size()).clear();
        }
        
        // Add updated tooltip
        long currentTime = System.currentTimeMillis();
        
        tooltipLines.add(Component.literal("Stock ID: " + stock.getStockId().toString().substring(0, 8))
                .withStyle(ChatFormatting.GRAY));
        tooltipLines.add(Component.literal("Company: " + stock.getCompanySymbol())
                .withStyle(ChatFormatting.YELLOW));
        
        java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateStr = dateFormat.format(new java.util.Date(stock.getPurchaseTime()));
        tooltipLines.add(Component.literal("Purchase Date: " + dateStr)
                .withStyle(ChatFormatting.AQUA));
        
        // Convert cents to dollars for display
        double purchasePriceDollars = stock.getPurchasePrice() / 100.0;
        tooltipLines.add(Component.literal("Purchase Price: $" + String.format("%.2f", purchasePriceDollars))
                .withStyle(ChatFormatting.GREEN));
        
        long timeSincePurchase = currentTime - stock.getPurchaseTime();
        long freezeMinutes = io.fabianbuthere.stonks.config.StonksConfig.STOCK_FREEZE_MINUTES.get();
        long freezeMillis = freezeMinutes * 60L * 1000L;
        
        if (timeSincePurchase < freezeMillis) {
            long remainingMillis = freezeMillis - timeSincePurchase;
            long days = remainingMillis / (24L * 60L * 60L * 1000L);
            long hours = (remainingMillis % (24L * 60L * 60L * 1000L)) / (60L * 60L * 1000L);
            long minutes = (remainingMillis % (60L * 60L * 1000L)) / (60L * 1000L);
            
            tooltipLines.add(Component.literal(""));
            tooltipLines.add(Component.literal("FROZEN - Cannot Sell")
                    .withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
            
            // Build time remaining string
            String timeStr;
            if (days > 0) {
                timeStr = days + "d " + hours + "h";
            } else if (hours > 0) {
                timeStr = hours + "h " + minutes + "m";
            } else {
                timeStr = minutes + "m";
            }
            
            tooltipLines.add(Component.literal("Time Remaining: " + timeStr)
                    .withStyle(ChatFormatting.RED));
        } else {
            tooltipLines.add(Component.literal(""));
            tooltipLines.add(Component.literal("✓ Ready to Sell")
                    .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
        }
        
        // Show depreciation info
        double depreciationMultiplier = stock.getDepreciationMultiplier(currentTime);
        double depreciationPercent = (1.0 - depreciationMultiplier) * 100.0;
        if (depreciationPercent > 0.1) {
            tooltipLines.add(Component.literal(""));
            tooltipLines.add(Component.literal("Depreciation: -" + String.format("%.1f", depreciationPercent) + "%")
                    .withStyle(ChatFormatting.GRAY));
            tooltipLines.add(Component.literal("(2% per week)")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
