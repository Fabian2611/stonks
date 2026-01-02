package io.fabianbuthere.stonks.event;

import io.fabianbuthere.stonks.Stonks;
import io.fabianbuthere.stonks.config.StonksConfig;
import io.fabianbuthere.stonks.data.JobSavedData;
import io.fabianbuthere.stonks.ui.setup.DeliveryBarrelMenu;
import io.fabianbuthere.stonks.ui.setup.DropoffBarrelMenu;
import io.fabianbuthere.stonks.ui.setup.PickupBarrelMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Stonks.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ServerInteractEvents {

    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.HIGHEST)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        try {
            if (event.getHand() != InteractionHand.MAIN_HAND) return;
            var player = event.getEntity();
            var level = player.level();
            if (level == null || level.isClientSide) return;
            BlockPos pos = event.getPos();
            BlockState state = level.getBlockState(pos);

            // Check if sign was clicked - right-click = buy/sell or accept/cancel jobs
            if (state.getBlock() instanceof SignBlock) {
                if (player instanceof ServerPlayer sp) {
                    handleSignRightClick(sp, pos);
                    event.setCanceled(true);
                    event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
                    return;
                }
            }
            
            if (state.getBlock() != Blocks.BARREL) return;

            var data = JobSavedData.get((net.minecraft.server.level.ServerLevel) level).getJobData();
            int didx = data.deliveryLocations.indexOf(pos);
            int tidx = data.transportLocations.indexOf(pos);

            java.util.UUID playerId = player.getUUID();

            // check transport dropoff (toIndex)
            if (tidx >= 0) {
                boolean opened = false;
                for (var e : data.transportKnownJobs.entrySet()) {
                    if (!e.getValue().equals(playerId)) continue;
                    int jobIndex = e.getKey();
                    var job = data.activeTransports.get(jobIndex);
                    if (job.toIndex() == tidx) {
                        DropoffBarrelMenu.open((net.minecraft.server.level.ServerPlayer) player, jobIndex);
                        event.setCanceled(true);
                        opened = true;
                        break;
                    }
                    if (job.fromIndex() == tidx) {
                        PickupBarrelMenu.open((net.minecraft.server.level.ServerPlayer) player, jobIndex);
                        event.setCanceled(true);
                        opened = true;
                        break;
                    }
                }
                if (!opened) {
                    PickupBarrelMenu.open((net.minecraft.server.level.ServerPlayer) player, -1);
                    event.setCanceled(true);
                    return;
                } else return;
            }

            // check delivery (dropoff)
            if (didx >= 0) {
                boolean opened = false;
                for (var e : data.knownJobs.entrySet()) {
                    if (!e.getValue().equals(playerId)) continue;
                    int jobIndex = e.getKey();
                    var job = data.activeJobs.get(jobIndex);
                    if (job.locationIndex() == didx) {
                        DeliveryBarrelMenu.open((net.minecraft.server.level.ServerPlayer) player, jobIndex);
                        event.setCanceled(true);
                        opened = true;
                        break;
                    }
                }
                if (!opened) {
                    DeliveryBarrelMenu.open((net.minecraft.server.level.ServerPlayer) player, -1);
                    event.setCanceled(true);
                    return;
                } else return;
            }
        } catch (Exception e) {
            Stonks.LOGGER.error("Error in sign right-click handler", e);
        }
    }
    
    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.HIGHEST)
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        try {
            var player = event.getEntity();
            var level = player.level();
            if (level == null || level.isClientSide) return;
            BlockPos pos = event.getPos();
            BlockState state = level.getBlockState(pos);
            
            // Check if sign was clicked - left-click = show details
            if (state.getBlock() instanceof SignBlock) {
                if (player instanceof ServerPlayer sp) {
                    handleSignLeftClick(sp, pos);
                    event.setCanceled(true);
                    return;
                }
            }
        } catch (Exception e) {
            Stonks.LOGGER.error("Error in sign left-click handler", e);
        }
    }
    
    private static void handleSignRightClick(ServerPlayer player, BlockPos signPos) {
        var level = player.serverLevel();
        var jobData = JobSavedData.get(level).getJobData();
        
        // Check if it's a stock sign first
        var savedData = JobSavedData.get(level);
        if (savedData.stockSignMatrixPos != null) {
            Stonks.LOGGER.info("Stock sign matrix configured - checking for company at sign {}", signPos);
            var company = io.fabianbuthere.stonks.api.util.StockSignManager.getCompanyAtSign(level, signPos);
            if (company != null) {
                Stonks.LOGGER.info("Handling stock sign click for company {} at sign {}", company.getSymbol(), signPos);
                handleStockSignClick(player, signPos);
                return;
            } else {
                Stonks.LOGGER.info("Stock sign matrix configured but no company found at sign {}", signPos);
            }
        }

        Stonks.LOGGER.info("Handling job sign click at {}", signPos);
        
        // Check delivery signs
        BlockPos deliveryCorner = new BlockPos(
            StonksConfig.DELIVERY_SIGN_X.get(),
            StonksConfig.DELIVERY_SIGN_Y.get(),
            StonksConfig.DELIVERY_SIGN_Z.get()
        );
        
        int deliveryWidth = StonksConfig.DELIVERY_SIGN_WIDTH.get();
        int deliveryHeight = StonksConfig.DELIVERY_SIGN_HEIGHT.get();
        
        Integer jobIndex = getJobIndexFromSign(level, signPos, deliveryCorner, deliveryWidth, deliveryHeight);
        if (jobIndex != null && jobIndex < jobData.activeJobs.size()) {
            handleDeliveryJobSign(player, jobIndex, jobData);
            return;
        }
        
        // Check transport signs
        BlockPos transportCorner = new BlockPos(
            StonksConfig.TRANSPORT_SIGN_X.get(),
            StonksConfig.TRANSPORT_SIGN_Y.get(),
            StonksConfig.TRANSPORT_SIGN_Z.get()
        );
        
        int transportWidth = StonksConfig.TRANSPORT_SIGN_WIDTH.get();
        int transportHeight = StonksConfig.TRANSPORT_SIGN_HEIGHT.get();
        
        jobIndex = getJobIndexFromSign(level, signPos, transportCorner, transportWidth, transportHeight);
        if (jobIndex != null && jobIndex < jobData.activeTransports.size()) {
            handleTransportJobSign(player, jobIndex, jobData);
            return;
        }
    }
    
    private static Integer getJobIndexFromSign(net.minecraft.server.level.ServerLevel level, BlockPos signPos, 
                                                BlockPos cornerPos, int width, int height) {
        BlockState cornerState = level.getBlockState(cornerPos);
        if (!(cornerState.getBlock() instanceof SignBlock)) return null;
        
        // Determine directions
        Direction right;
        Direction down;
        
        if (cornerState.getBlock() instanceof WallSignBlock) {
            Direction facing = cornerState.getValue(BlockStateProperties.HORIZONTAL_FACING);
            right = facing.getClockWise();
            down = Direction.DOWN;
        } else {
            right = Direction.EAST;
            down = Direction.DOWN;
        }
        
        // Calculate job index based on position in matrix
        int jobIndex = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                BlockPos checkPos = cornerPos.relative(right, x).relative(down, y);
                if (checkPos.equals(signPos)) {
                    return jobIndex;
                }
                jobIndex++;
            }
        }
        
        return null;
    }
    
    private static void handleDeliveryJobSign(ServerPlayer player, int jobIndex, io.fabianbuthere.stonks.api.JobData jobData) {
        var level = player.serverLevel();
        java.util.UUID playerId = player.getUUID();
        
        // Check if player has this job
        if (jobData.knownJobs.containsKey(jobIndex) && jobData.knownJobs.get(jobIndex).equals(playerId)) {
            // Cancel job
            boolean ok = JobSavedData.get(level).cancelDeliveryForPlayer(player, jobIndex);
            if (ok) {
                player.sendSystemMessage(Component.literal("§aCancelled delivery job #" + jobIndex));
            } else {
                player.sendSystemMessage(Component.literal("§cFailed to cancel delivery job #" + jobIndex));
            }
        } else if (!jobData.knownJobs.containsKey(jobIndex)) {
            // Accept job
            boolean success = JobSavedData.get(level).acceptJobForPlayer(player, jobIndex);
            if (success) {
                var job = jobData.activeJobs.get(jobIndex);
                String locText = "?";
                try { 
                    locText = jobData.deliveryLocations.get(job.locationIndex()).toShortString(); 
                } catch (Exception ignored) {}
                player.sendSystemMessage(Component.literal("§aAccepted delivery job #" + jobIndex + " @ " + locText + " - §e$" + job.payment()));
            } else {
                player.sendSystemMessage(Component.literal("§cFailed to accept delivery job #" + jobIndex + " (limit reached)"));
            }
        } else {
            player.sendSystemMessage(Component.literal("§cThis job is already taken by another player"));
        }
    }
    
    private static void handleTransportJobSign(ServerPlayer player, int jobIndex, io.fabianbuthere.stonks.api.JobData jobData) {
        var level = player.serverLevel();
        java.util.UUID playerId = player.getUUID();
        
        // Check if player has this job
        if (jobData.transportKnownJobs.containsKey(jobIndex) && jobData.transportKnownJobs.get(jobIndex).equals(playerId)) {
            // Cancel job
            boolean ok = JobSavedData.get(level).cancelTransportForPlayer(player, jobIndex);
            if (ok) {
                player.sendSystemMessage(Component.literal("§aCancelled transport job #" + jobIndex));
            } else {
                player.sendSystemMessage(Component.literal("§cFailed to cancel transport job #" + jobIndex));
            }
        } else if (!jobData.transportKnownJobs.containsKey(jobIndex)) {
            // Accept job
            boolean success = JobSavedData.get(level).acceptTransportForPlayer(player, jobIndex);
            if (success) {
                var job = jobData.activeTransports.get(jobIndex);
                String fromText = "?";
                String toText = "?";
                try { 
                    fromText = jobData.transportLocations.get(job.fromIndex()).toShortString();
                    toText = jobData.transportLocations.get(job.toIndex()).toShortString();
                } catch (Exception ignored) {}
                player.sendSystemMessage(Component.literal("§aAccepted transport job #" + jobIndex + " from " + fromText + " to " + toText + " - §e$" + job.payment()));
            } else {
                player.sendSystemMessage(Component.literal("§cFailed to accept transport job #" + jobIndex + " (limit reached)"));
            }
        } else {
            player.sendSystemMessage(Component.literal("§cThis job is already taken by another player"));
        }
    }
    
    private static void handleSignLeftClick(ServerPlayer player, BlockPos signPos) {
        var level = player.serverLevel();
        var jobData = JobSavedData.get(level).getJobData();
        
        // Check delivery signs
        BlockPos deliveryCorner = new BlockPos(
            StonksConfig.DELIVERY_SIGN_X.get(),
            StonksConfig.DELIVERY_SIGN_Y.get(),
            StonksConfig.DELIVERY_SIGN_Z.get()
        );
        
        int deliveryWidth = StonksConfig.DELIVERY_SIGN_WIDTH.get();
        int deliveryHeight = StonksConfig.DELIVERY_SIGN_HEIGHT.get();
        
        Integer jobIndex = getJobIndexFromSign(level, signPos, deliveryCorner, deliveryWidth, deliveryHeight);
        if (jobIndex != null && jobIndex < jobData.activeJobs.size()) {
            showDeliveryJobDetails(player, jobIndex, jobData);
            return;
        }
        
        // Check transport signs
        BlockPos transportCorner = new BlockPos(
            StonksConfig.TRANSPORT_SIGN_X.get(),
            StonksConfig.TRANSPORT_SIGN_Y.get(),
            StonksConfig.TRANSPORT_SIGN_Z.get()
        );
        
        int transportWidth = StonksConfig.TRANSPORT_SIGN_WIDTH.get();
        int transportHeight = StonksConfig.TRANSPORT_SIGN_HEIGHT.get();
        
        jobIndex = getJobIndexFromSign(level, signPos, transportCorner, transportWidth, transportHeight);
        if (jobIndex != null && jobIndex < jobData.activeTransports.size()) {
            showTransportJobDetails(player, jobIndex, jobData);
            return;
        }
        
        // Check stock signs
        io.fabianbuthere.stonks.api.stock.Company company = io.fabianbuthere.stonks.api.util.StockSignManager.getCompanyAtSign(level, signPos);
        if (company != null) {
            showStockDetails(player, company);
            return;
        }
    }
    
    private static void showDeliveryJobDetails(ServerPlayer player, int jobIndex, io.fabianbuthere.stonks.api.JobData jobData) {
        var job = jobData.activeJobs.get(jobIndex);
        java.util.UUID playerId = player.getUUID();
        boolean isTaken = jobData.knownJobs.containsKey(jobIndex);
        boolean isYours = isTaken && jobData.knownJobs.get(jobIndex).equals(playerId);
        
        int expirationMinutes = io.fabianbuthere.stonks.config.StonksConfig.JOB_EXPIRATION_MINUTES.get();
        long remainingMillis = job.getRemainingTimeMillis(System.currentTimeMillis(), expirationMinutes);
        String timeColor = io.fabianbuthere.stonks.api.util.TimeUtil.getTimeColor(remainingMillis, expirationMinutes);
        String timeStr = io.fabianbuthere.stonks.api.util.TimeUtil.formatRemainingTime(remainingMillis);
        
        String status = isYours ? "§a[YOUR JOB]" : (isTaken ? "§c[TAKEN]" : "§a[AVAILABLE]");
        String location = "?";
        try { 
            location = jobData.deliveryLocations.get(job.locationIndex()).toShortString(); 
        } catch (Exception ignored) {}
        
        player.sendSystemMessage(Component.literal("§6━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        player.sendSystemMessage(Component.literal("§6§lDelivery Job #" + jobIndex + " " + status));
        player.sendSystemMessage(Component.literal("§7Location: §f" + location));
        player.sendSystemMessage(Component.literal("§7Payment: §e$" + job.payment()));
        if (!isTaken) {
            player.sendSystemMessage(Component.literal("§7Time Remaining: " + timeColor + timeStr));
        }
        player.sendSystemMessage(Component.literal("§7Items Required:"));
        for (var part : job.parts()) {
            player.sendSystemMessage(Component.literal("  §f• " + part.count() + "x §7" + part.item()));
        }
        if (isYours) {
            player.sendSystemMessage(Component.literal("§7Right-click to §ccancel §7this job"));
        } else if (!isTaken) {
            player.sendSystemMessage(Component.literal("§7Right-click to §aaccept §7this job"));
        }
        player.sendSystemMessage(Component.literal("§6━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    }
    
    private static void showTransportJobDetails(ServerPlayer player, int jobIndex, io.fabianbuthere.stonks.api.JobData jobData) {
        var job = jobData.activeTransports.get(jobIndex);
        java.util.UUID playerId = player.getUUID();
        boolean isTaken = jobData.transportKnownJobs.containsKey(jobIndex);
        boolean isYours = isTaken && jobData.transportKnownJobs.get(jobIndex).equals(playerId);
        
        int expirationMinutes = io.fabianbuthere.stonks.config.StonksConfig.JOB_EXPIRATION_MINUTES.get();
        long remainingMillis = job.getRemainingTimeMillis(System.currentTimeMillis(), expirationMinutes);
        String timeColor = io.fabianbuthere.stonks.api.util.TimeUtil.getTimeColor(remainingMillis, expirationMinutes);
        String timeStr = io.fabianbuthere.stonks.api.util.TimeUtil.formatRemainingTime(remainingMillis);
        
        String status = isYours ? "§a[YOUR JOB]" : (isTaken ? "§c[TAKEN]" : "§a[AVAILABLE]");
        String fromLoc = "?";
        String toLoc = "?";
        try { 
            fromLoc = jobData.transportLocations.get(job.fromIndex()).toShortString();
            toLoc = jobData.transportLocations.get(job.toIndex()).toShortString();
        } catch (Exception ignored) {}
        
        player.sendSystemMessage(Component.literal("§6━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        player.sendSystemMessage(Component.literal("§6§lTransport Job #" + jobIndex + " " + status));
        player.sendSystemMessage(Component.literal("§7From: §f" + fromLoc));
        player.sendSystemMessage(Component.literal("§7To: §f" + toLoc));
        player.sendSystemMessage(Component.literal("§7Payment: §e$" + job.payment()));
        if (!isTaken) {
            player.sendSystemMessage(Component.literal("§7Time Remaining: " + timeColor + timeStr));
        }
        player.sendSystemMessage(Component.literal("§7Items to Transport:"));
        for (var part : job.parts()) {
            player.sendSystemMessage(Component.literal("  §f• " + part.count() + "x §7" + part.item()));
        }
        if (isYours) {
            player.sendSystemMessage(Component.literal("§7Right-click to §ccancel §7this job"));
        } else if (!isTaken) {
            player.sendSystemMessage(Component.literal("§7Right-click to §aaccept §7this job"));
        }
        player.sendSystemMessage(Component.literal("§6━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    }
    
    private static void showStockDetails(ServerPlayer player, io.fabianbuthere.stonks.api.stock.Company company) {
        double stockPrice = company.calculateStockPrice();
        int stockPriceCents = (int) Math.round(stockPrice * 100);
        int availableShares = company.getAvailableShares();
        double companyValue = company.calculateCompanyValue();
        
        player.sendSystemMessage(Component.literal("§6━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        player.sendSystemMessage(Component.literal("§6§l" + company.getName() + " (" + company.getSymbol() + ")"));
        
        if (company.getOwnerId() != null) {
            player.sendSystemMessage(Component.literal("§7Owner: §f" + company.getOwnerId().toString().substring(0, 8) + "..."));
        }
        
        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("§eStock Information:"));
        player.sendSystemMessage(Component.literal("§7Price per share: §a" + io.fabianbuthere.stonks.api.util.PaymentUtil.formatPayment(stockPriceCents)));
        player.sendSystemMessage(Component.literal("§7Available shares: §f" + availableShares + " §7/ §f" + company.getShareCount()));
        player.sendSystemMessage(Component.literal("§7Share percentage: §f" + String.format("%.2f%%", company.getSharePercentage())));
        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("§eCompany Value: §a$" + String.format("%.2f", companyValue)));
        player.sendSystemMessage(Component.literal("§7Property: §f" + company.getTotalPropertySize() + " sqm"));
        
        int[] profits = company.getProfitsLast4Weeks();
        player.sendSystemMessage(Component.literal("§7Weekly Profits (last 4): §f" + profits[0] + " §7/ §f" + profits[1] + " §7/ §f" + profits[2] + " §7/ §f" + profits[3]));
        
        double totalBankBalance = company.getBankAccounts().values().stream().mapToDouble(Double::doubleValue).sum();
        player.sendSystemMessage(Component.literal("§7Total Bank Balance: §a$" + String.format("%.2f", totalBankBalance)));
        player.sendSystemMessage(Component.literal(""));
        player.sendSystemMessage(Component.literal("§7§oRight-click to buy/sell stocks"));
        player.sendSystemMessage(Component.literal("§6━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    }
    
    private static void handleStockSignClick(ServerPlayer player, BlockPos signPos) {
        Stonks.LOGGER.info("Handling stock sign click at {}", signPos);
        var level = player.serverLevel();
        var data = JobSavedData.get(level);
        
        var company = io.fabianbuthere.stonks.api.util.StockSignManager.getCompanyAtSign(level, signPos);
        if (company == null) {
            Stonks.LOGGER.warn("No company found at stock sign {}", signPos);
            return;
        }
        
        var heldItem = player.getMainHandItem();
        
        // Check if player is holding a stock certificate to sell
        if (io.fabianbuthere.stonks.api.stock.StockCertificate.isCertificate(heldItem)) {
            Stonks.LOGGER.info("Player is holding a stock certificate - attempting to sell");
            var stock = io.fabianbuthere.stonks.api.stock.StockCertificate.getStockFromCertificate(heldItem);
            if (stock == null) return;
            
            if (!stock.getCompanySymbol().equals(company.getSymbol())) {
                player.sendSystemMessage(Component.literal("§cThis stock is not for " + company.getSymbol()));
                return;
            }
            
            if (!stock.getOwnerUUID().equals(player.getUUID())) {
                player.sendSystemMessage(Component.literal("§cYou don't own this stock!"));
                return;
            }
            
            if (stock.isFrozen(System.currentTimeMillis())) {
                int freezeMinutes = io.fabianbuthere.stonks.config.StonksConfig.STOCK_FREEZE_MINUTES.get();
                long freezeMillis = freezeMinutes * 60L * 1000L;
                long remaining = (stock.getPurchaseTime() + freezeMillis) - System.currentTimeMillis();
                long days = remaining / (24L * 60L * 60L * 1000L);
                long hours = (remaining % (24L * 60L * 60L * 1000L)) / (60L * 60L * 1000L);
                long minutes = (remaining % (60L * 60L * 1000L)) / (60L * 1000L);
                
                String timeStr;
                if (days > 0) {
                    timeStr = days + "d " + hours + "h";
                } else if (hours > 0) {
                    timeStr = hours + "h " + minutes + "m";
                } else {
                    timeStr = minutes + "m";
                }
                
                player.sendSystemMessage(Component.literal("§cThis stock is still frozen! Time remaining: " + timeStr));
                return;
            }
            
            // Sell the stock
            double basePrice = company.calculateStockPrice();
            int basePriceCents = (int) Math.round(basePrice * 100);
            int actualPrice = stock.getCurrentValue(basePriceCents, System.currentTimeMillis());
            
            company.returnShare();
            data.activeStocks.remove(stock.getStockId());
            data.setDirty();
            
            heldItem.shrink(1);
            
            // Give money to player
            io.fabianbuthere.stonks.api.util.PaymentUtil.giveMoneyToPlayer(player, actualPrice);
            
            double depreciationPercent = (1.0 - stock.getDepreciationMultiplier(System.currentTimeMillis())) * 100.0;
            if (depreciationPercent > 0.1) {
                player.sendSystemMessage(Component.literal("§aSold stock for " + io.fabianbuthere.stonks.api.util.PaymentUtil.formatPayment(actualPrice) + " §7(depreciated " + String.format("%.1f", depreciationPercent) + "%)"));
            } else {
                player.sendSystemMessage(Component.literal("§aSold stock for " + io.fabianbuthere.stonks.api.util.PaymentUtil.formatPayment(actualPrice) + "!"));
            }
            
            // Update signs
            io.fabianbuthere.stonks.api.util.StockSignManager.updateStockSigns(level);
            return;
        }
        
        // Otherwise, buy stock
        Stonks.LOGGER.info("Checking shares...");
        if (company.getAvailableShares() <= 0) {
            player.sendSystemMessage(Component.literal("§cNo shares available for " + company.getSymbol()));
            return;
        }

        double stockPrice = company.calculateStockPrice();
        int stockPriceCents = (int) Math.round(stockPrice * 100);
        Stonks.LOGGER.info("Stock price is ${}", stockPrice);
        
        if (!io.fabianbuthere.stonks.api.util.PaymentUtil.takeMoneyFromPlayer(player, stockPriceCents)) {
            player.sendSystemMessage(Component.literal("§cYou need " + io.fabianbuthere.stonks.api.util.PaymentUtil.formatPayment(stockPriceCents) + " §cto buy this stock!"));
            return;
        }
        
        // Purchase successful - add money to company's primary account
        String primaryAccount = company.getPrimaryBankAccount();
        if (primaryAccount != null && company.getBankAccounts().containsKey(primaryAccount)) {
            company.getBankAccounts().put(primaryAccount, 
                company.getBankAccounts().get(primaryAccount) + stockPrice);
        }
        
        company.sellShare();
        
        // Create stock certificate
        var stock = new io.fabianbuthere.stonks.api.stock.Stock(
            java.util.UUID.randomUUID(),
            company.getSymbol(),
            System.currentTimeMillis(),
            stockPriceCents,
            player.getUUID()
        );
        
        data.activeStocks.put(stock.getStockId(), stock);
        data.setDirty();
        
        var certificate = io.fabianbuthere.stonks.api.stock.StockCertificate.createCertificate(stock);
        player.addItem(certificate);
        
        int freezeMinutes = io.fabianbuthere.stonks.config.StonksConfig.STOCK_FREEZE_MINUTES.get();
        String freezeTimeStr;
        if (freezeMinutes >= 1440) {
            int days = freezeMinutes / 1440;
            freezeTimeStr = days + " day" + (days > 1 ? "s" : "");
        } else if (freezeMinutes >= 60) {
            int hours = freezeMinutes / 60;
            freezeTimeStr = hours + " hour" + (hours > 1 ? "s" : "");
        } else {
            freezeTimeStr = freezeMinutes + " minute" + (freezeMinutes > 1 ? "s" : "");
        }
        
        player.sendSystemMessage(Component.literal("§aPurchased 1 share of " + company.getSymbol() + " for " + io.fabianbuthere.stonks.api.util.PaymentUtil.formatPayment(stockPriceCents) + "!"));
        player.sendSystemMessage(Component.literal("§7Stock is frozen for " + freezeTimeStr));
        
        // Update signs
        io.fabianbuthere.stonks.api.util.StockSignManager.updateStockSigns(level);
    }
}
