package io.fabianbuthere.stonks.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import io.fabianbuthere.stonks.Stonks;
import io.fabianbuthere.stonks.api.DeliveryJob;
import io.fabianbuthere.stonks.api.DeliveryJobPart;
import io.fabianbuthere.stonks.api.TransportJob;
import io.fabianbuthere.stonks.api.stock.Company;
import io.fabianbuthere.stonks.api.util.JobPool;
import io.fabianbuthere.stonks.config.StonksConfig;
import io.fabianbuthere.stonks.data.JobSavedData;
import io.fabianbuthere.stonks.ui.setup.JobMenu;
import io.fabianbuthere.stonks.ui.setup.MainMenu;
import io.fabianbuthere.stonks.ui.setup.PlayerJobMenu;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Stonks.MOD_ID)
public class CommandEvents {
    
    private static List<DeliveryJobPart> parseJobParts(ServerPlayer player, String itemsStr) {
        List<DeliveryJobPart> parts = new ArrayList<>();
        for (String part : itemsStr.split(",")) {
            String p = part.trim();
            if (p.isEmpty()) continue;
            
            // Find the last occurrence of : or = to split item from count
            int separatorIndex = -1;
            for (int i = p.length() - 1; i >= 0; i--) {
                if (p.charAt(i) == ':' || p.charAt(i) == '=') {
                    separatorIndex = i;
                    break;
                }
            }
            
            if (separatorIndex == -1 || separatorIndex == 0 || separatorIndex == p.length() - 1) {
                player.sendSystemMessage(Component.literal("Invalid format in: " + part + " (expected 'item:count' or 'item=count')"));
                return null;
            }
            
            String item = p.substring(0, separatorIndex).trim();
            String countStr = p.substring(separatorIndex + 1).trim();
            
            int count;
            try { 
                count = Integer.parseInt(countStr); 
            } catch (NumberFormatException e) {
                player.sendSystemMessage(Component.literal("Invalid count '" + countStr + "' in: " + part));
                return null;
            }
            
            if (count <= 0) {
                player.sendSystemMessage(Component.literal("Count must be positive in: " + part));
                return null;
            }
            
            parts.add(new DeliveryJobPart(item, count));
        }
        
        if (parts.isEmpty()) {
            player.sendSystemMessage(Component.literal("No valid items specified"));
            return null;
        }
        
        return parts;
    }
    
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
                Commands.literal("stonks")
                        .executes(ctx -> {
                            if (!ctx.getSource().isPlayer()) return 0;
                            ServerPlayer player = ctx.getSource().getPlayer();
                            PlayerJobMenu.open(player);
                            return 1;
                        })
                        .then(Commands.literal("toggleAdmin")
                                .requires(src -> src.hasPermission(2))
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    JobSavedData data = JobSavedData.get(player.serverLevel());
                                    UUID playerId = player.getUUID();
                                    
                                    if (data.jobData.adminModeEnabled.contains(playerId)) {
                                        data.jobData.adminModeEnabled.remove(playerId);
                                        player.sendSystemMessage(Component.literal("§cAdmin mode disabled"));
                                    } else {
                                        data.jobData.adminModeEnabled.add(playerId);
                                        player.sendSystemMessage(Component.literal("§aAdmin mode enabled"));
                                    }
                                    data.setDirty();
                                    return 1;
                                }))
                        .then(Commands.literal("debug")
                                .requires(source -> source.hasPermission(2))
                                .requires(CommandSourceStack::isPlayer)
                                .then(Commands.literal("items")
                                        .then(Commands.literal("delivering")
                                                .executes(ctx -> {
                                                    ServerPlayer player = ctx.getSource().getPlayer();
                                                    player.sendSystemMessage(Component.literal("Delivering Items:"));
                                                    StonksConfig.DELIVERING_ITEMS.get().forEach(itemStr ->
                                                            player.sendSystemMessage(Component.literal(" - " + itemStr))
                                                    );
                                                    return 0;
                                                })
                                        )
                                )
                                .then(Commands.literal("draw")
                                        .then(Commands.literal("delivering")
                                                .executes(ctx -> {
                                                    ServerPlayer player = ctx.getSource().getPlayer();
                                                    DeliveryJob job = JobPool.getInstance().getRandomDeliveryJob();
                                                    player.sendSystemMessage(Component.literal("Drew Delivering Job: " +
                                                            job.summary()));
                                                    return 0;
                                                })
                                        )
                                )
                        )
                        .then(Commands.literal("setup")
                                .requires(source -> source.hasPermission(2))
                                .requires(CommandSourceStack::isPlayer)
                                .executes(ctx -> {
                                    MainMenu.open(ctx.getSource().getPlayer());
                                    return 1;
                                })
                                .then(Commands.literal("company")
                                        .then(Commands.literal("create")
                                                .then(Commands.argument("name", StringArgumentType.string())
                                                        .then(Commands.argument("symbol", StringArgumentType.string())
                                                                .then(Commands.argument("shareCount", IntegerArgumentType.integer(50, 1000))
                                                                        .then(Commands.argument("sharePercentage", DoubleArgumentType.doubleArg(0.01, 100.0))
                                                                                .then(Commands.argument("propertySize", IntegerArgumentType.integer(0))
                                                                                        .then(Commands.argument("week1", IntegerArgumentType.integer())
                                                                                                .then(Commands.argument("week2", IntegerArgumentType.integer())
                                                                                                        .then(Commands.argument("week3", IntegerArgumentType.integer())
                                                                                                                .then(Commands.argument("week4", IntegerArgumentType.integer())
                                                                                                                        .then(Commands.argument("ownerName", StringArgumentType.greedyString())
                                                                                                                                .executes(ctx -> {
                                                                                                                                    ServerPlayer player = ctx.getSource().getPlayer();
                                                                                                                                    net.minecraft.server.level.ServerLevel level = player.serverLevel();
                                                                                                                                    JobSavedData data = JobSavedData.get(level);
                                                                                                                                    
                                                                                                                                    String name = StringArgumentType.getString(ctx, "name");
                                                                                                                                    String symbol = StringArgumentType.getString(ctx, "symbol");
                                                                                                                                    int shareCount = IntegerArgumentType.getInteger(ctx, "shareCount");
                                                                                                                                    double sharePercentage = DoubleArgumentType.getDouble(ctx, "sharePercentage");
                                                                                                                                    int propertySize = IntegerArgumentType.getInteger(ctx, "propertySize");
                                                                                                                                    int week1 = IntegerArgumentType.getInteger(ctx, "week1");
                                                                                                                                    int week2 = IntegerArgumentType.getInteger(ctx, "week2");
                                                                                                                                    int week3 = IntegerArgumentType.getInteger(ctx, "week3");
                                                                                                                                    int week4 = IntegerArgumentType.getInteger(ctx, "week4");
                                                                                                                                    String ownerName = StringArgumentType.getString(ctx, "ownerName");
                                                                                                                                    
                                                                                                                                    if (symbol.length() != 3) {
                                                                                                                                        player.sendSystemMessage(Component.literal("§cSymbol must be exactly 3 letters!"));
                                                                                                                                        return 0;
                                                                                                                                    }
                                                                                                                                    
                                                                                                                                    Company company = new Company(
                                                                                                                                            name,
                                                                                                                                            symbol.toUpperCase(),
                                                                                                                                            shareCount,
                                                                                                                                            sharePercentage,
                                                                                                                                            new int[]{week1, week2, week3, week4},
                                                                                                                                            propertySize,
                                                                                                                                            player.getUUID()
                                                                                                                                    );
                                                                                                                                    
                                                                                                                                    data.companies.add(company);
                                                                                                                                    data.setDirty();
                                                                                                                                    
                                                                                                                                    player.sendSystemMessage(Component.literal("§aCreated company: " + name + " (" + symbol + ")"));
                                                                                                                                    return 1;
                                                                                                                                })
                                                                                                                        )
                                                                                                                )
                                                                                                        )
                                                                                                )
                                                                                        )
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                        .then(Commands.literal("addProfit")
                                                .then(Commands.argument("symbol", StringArgumentType.string())
                                                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg())
                                                                .executes(ctx -> {
                                                                    ServerPlayer player = ctx.getSource().getPlayer();
                                                                    net.minecraft.server.level.ServerLevel level = player.serverLevel();
                                                                    JobSavedData data = JobSavedData.get(level);
                                                                    
                                                                    String symbol = StringArgumentType.getString(ctx, "symbol").toUpperCase();
                                                                    double amount = DoubleArgumentType.getDouble(ctx, "amount");
                                                                    
                                                                    Company company = data.companies.stream()
                                                                            .filter(c -> c.getSymbol().equals(symbol))
                                                                            .findFirst()
                                                                            .orElse(null);
                                                                    
                                                                    if (company == null) {
                                                                        player.sendSystemMessage(Component.literal("§cCompany not found: " + symbol));
                                                                        return 0;
                                                                    }
                                                                    
                                                                    if (company.getPrimaryBankAccount() == null || company.getPrimaryBankAccount().isEmpty()) {
                                                                        player.sendSystemMessage(Component.literal("§cNo main bank account set for " + symbol));
                                                                        return 0;
                                                                    }
                                                                    
                                                                    double current = company.getBankAccounts().getOrDefault(company.getPrimaryBankAccount(), 0.0);
                                                                    company.getBankAccounts().put(company.getPrimaryBankAccount(), current + amount);
                                                                    data.setDirty();
                                                                    
                                                                    player.sendSystemMessage(Component.literal("§aAdded " + amount + " to " + company.getName() + "'s main account"));
                                                                    player.sendSystemMessage(Component.literal("§7New balance: " + (current + amount)));
                                                                    return 1;
                                                                })
                                                        )
                                                )
                                        )
                                        .then(Commands.literal("addBankAccount")
                                                .then(Commands.argument("symbol", StringArgumentType.string())
                                                        .then(Commands.argument("accountName", StringArgumentType.greedyString())
                                                                .executes(ctx -> {
                                                                    ServerPlayer player = ctx.getSource().getPlayer();
                                                                    net.minecraft.server.level.ServerLevel level = player.serverLevel();
                                                                    JobSavedData data = JobSavedData.get(level);
                                                                    
                                                                    String symbol = StringArgumentType.getString(ctx, "symbol").toUpperCase();
                                                                    String accountName = StringArgumentType.getString(ctx, "accountName");
                                                                    
                                                                    Company company = data.companies.stream()
                                                                            .filter(c -> c.getSymbol().equals(symbol))
                                                                            .findFirst()
                                                                            .orElse(null);
                                                                    
                                                                    if (company == null) {
                                                                        player.sendSystemMessage(Component.literal("§cCompany not found: " + symbol));
                                                                        return 0;
                                                                    }
                                                                    
                                                                    company.getBankAccounts().put(accountName, 0.0);
                                                                    if (company.getPrimaryBankAccount() == null || company.getPrimaryBankAccount().isEmpty()) {
                                                                        company.setPrimaryBankAccount(accountName);
                                                                        player.sendSystemMessage(Component.literal("§aSet as main bank account"));
                                                                    }
                                                                    data.setDirty();
                                                                    
                                                                    player.sendSystemMessage(Component.literal("§aAdded bank account '" + accountName + "' to " + company.getName()));
                                                                    return 1;
                                                                })
                                                        )
                                                )
                                        )
                                        .then(Commands.literal("setMainAccount")
                                                .then(Commands.argument("symbol", StringArgumentType.string())
                                                        .then(Commands.argument("accountName", StringArgumentType.greedyString())
                                                                .executes(ctx -> {
                                                                    ServerPlayer player = ctx.getSource().getPlayer();
                                                                    net.minecraft.server.level.ServerLevel level = player.serverLevel();
                                                                    JobSavedData data = JobSavedData.get(level);
                                                                    
                                                                    String symbol = StringArgumentType.getString(ctx, "symbol").toUpperCase();
                                                                    String accountName = StringArgumentType.getString(ctx, "accountName");
                                                                    
                                                                    Company company = data.companies.stream()
                                                                            .filter(c -> c.getSymbol().equals(symbol))
                                                                            .findFirst()
                                                                            .orElse(null);
                                                                    
                                                                    if (company == null) {
                                                                        player.sendSystemMessage(Component.literal("§cCompany not found: " + symbol));
                                                                        return 0;
                                                                    }
                                                                    
                                                                    if (!company.getBankAccounts().containsKey(accountName)) {
                                                                        player.sendSystemMessage(Component.literal("§cBank account not found: " + accountName));
                                                                        return 0;
                                                                    }
                                                                    
                                                                    company.setPrimaryBankAccount(accountName);
                                                                    data.setDirty();
                                                                    
                                                                    player.sendSystemMessage(Component.literal("§aSet main bank account to '" + accountName + "'"));
                                                                    return 1;
                                                                })
                                                        )
                                                )
                                        )
                                )
                                .then(Commands.literal("signs")
                                        .then(Commands.literal("init")
                                                .then(Commands.literal("delivery")
                                                        .then(Commands.argument("x", IntegerArgumentType.integer())
                                                                .then(Commands.argument("y", IntegerArgumentType.integer())
                                                                        .then(Commands.argument("z", IntegerArgumentType.integer())
                                                                                .then(Commands.argument("width", IntegerArgumentType.integer(1, 20))
                                                                                        .then(Commands.argument("height", IntegerArgumentType.integer(1, 20))
                                                                                                .executes(ctx -> {
                                                                                                    ServerPlayer player = ctx.getSource().getPlayer();
                                                                                                    net.minecraft.server.level.ServerLevel level = player.serverLevel();
                                                                                                    
                                                                                                    int x = IntegerArgumentType.getInteger(ctx, "x");
                                                                                                    int y = IntegerArgumentType.getInteger(ctx, "y");
                                                                                                    int z = IntegerArgumentType.getInteger(ctx, "z");
                                                                                                    int width = IntegerArgumentType.getInteger(ctx, "width");
                                                                                                    int height = IntegerArgumentType.getInteger(ctx, "height");
                                                                                                    
                                                                                                    net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(x, y, z);
                                                                                                    
                                                                                                    // Update config
                                                                                                    StonksConfig.DELIVERY_SIGN_X.set(x);
                                                                                                    StonksConfig.DELIVERY_SIGN_Y.set(y);
                                                                                                    StonksConfig.DELIVERY_SIGN_Z.set(z);
                                                                                                    StonksConfig.DELIVERY_SIGN_WIDTH.set(width);
                                                                                                    StonksConfig.DELIVERY_SIGN_HEIGHT.set(height);
                                                                                                    StonksConfig.COMMON_CONFIG.save();
                                                                                                    
                                                                                                    io.fabianbuthere.stonks.api.util.SignDisplayManager.initializeSignMatrix(
                                                                                                        level, pos, width, height
                                                                                                    );
                                                                                                    
                                                                                                    player.sendSystemMessage(Component.literal(
                                                                                                        "§aInitialized delivery sign matrix (" + width + "x" + height + ") at " + pos.toShortString()
                                                                                                    ));
                                                                                                    return 1;
                                                                                                })
                                                                                        )
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                )
                                                .then(Commands.literal("transport")
                                                        .then(Commands.argument("x", IntegerArgumentType.integer())
                                                                .then(Commands.argument("y", IntegerArgumentType.integer())
                                                                        .then(Commands.argument("z", IntegerArgumentType.integer())
                                                                                .then(Commands.argument("width", IntegerArgumentType.integer(1, 20))
                                                                                        .then(Commands.argument("height", IntegerArgumentType.integer(1, 20))
                                                                                                .executes(ctx -> {
                                                                                                    ServerPlayer player = ctx.getSource().getPlayer();
                                                                                                    net.minecraft.server.level.ServerLevel level = player.serverLevel();
                                                                                                    
                                                                                                    int x = IntegerArgumentType.getInteger(ctx, "x");
                                                                                                    int y = IntegerArgumentType.getInteger(ctx, "y");
                                                                                                    int z = IntegerArgumentType.getInteger(ctx, "z");
                                                                                                    int width = IntegerArgumentType.getInteger(ctx, "width");
                                                                                                    int height = IntegerArgumentType.getInteger(ctx, "height");
                                                                                                    
                                                                                                    net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(x, y, z);
                                                                                                    
                                                                                                    // Update config
                                                                                                    StonksConfig.TRANSPORT_SIGN_X.set(x);
                                                                                                    StonksConfig.TRANSPORT_SIGN_Y.set(y);
                                                                                                    StonksConfig.TRANSPORT_SIGN_Z.set(z);
                                                                                                    StonksConfig.TRANSPORT_SIGN_WIDTH.set(width);
                                                                                                    StonksConfig.TRANSPORT_SIGN_HEIGHT.set(height);
                                                                                                    StonksConfig.COMMON_CONFIG.save();
                                                                                                    
                                                                                                    io.fabianbuthere.stonks.api.util.SignDisplayManager.initializeSignMatrix(
                                                                                                        level, pos, width, height
                                                                                                    );
                                                                                                    
                                                                                                    player.sendSystemMessage(Component.literal(
                                                                                                        "§aInitialized transport sign matrix (" + width + "x" + height + ") at " + pos.toShortString()
                                                                                                    ));
                                                                                                    return 1;
                                                                                                })
                                                                                        )
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                )
                                                .then(Commands.literal("stocks")
                                                        .then(Commands.argument("x", IntegerArgumentType.integer())
                                                                .then(Commands.argument("y", IntegerArgumentType.integer())
                                                                        .then(Commands.argument("z", IntegerArgumentType.integer())
                                                                                .then(Commands.argument("width", IntegerArgumentType.integer(1, 20))
                                                                                        .then(Commands.argument("height", IntegerArgumentType.integer(1, 20))
                                                                                                .executes(ctx -> {
                                                                                                    ServerPlayer player = ctx.getSource().getPlayer();
                                                                                                    net.minecraft.server.level.ServerLevel level = player.serverLevel();
                                                                                                    
                                                                                                    int x = IntegerArgumentType.getInteger(ctx, "x");
                                                                                                    int y = IntegerArgumentType.getInteger(ctx, "y");
                                                                                                    int z = IntegerArgumentType.getInteger(ctx, "z");
                                                                                                    int width = IntegerArgumentType.getInteger(ctx, "width");
                                                                                                    int height = IntegerArgumentType.getInteger(ctx, "height");
                                                                                                    
                                                                                                    net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(x, y, z);
                                                                                                    
                                                                                                    // Update config
                                                                                                    StonksConfig.STOCK_SIGN_X.set(x);
                                                                                                    StonksConfig.STOCK_SIGN_Y.set(y);
                                                                                                    StonksConfig.STOCK_SIGN_Z.set(z);
                                                                                                    StonksConfig.STOCK_SIGN_WIDTH.set(width);
                                                                                                    StonksConfig.STOCK_SIGN_HEIGHT.set(height);
                                                                                                    StonksConfig.COMMON_CONFIG.save();
                                                                                                    
                                                                                                    io.fabianbuthere.stonks.api.util.SignDisplayManager.initializeStockSignMatrix(
                                                                                                        level, pos, width, height
                                                                                                    );
                                                                                                    
                                                                                                    player.sendSystemMessage(Component.literal(
                                                                                                        "§aInitialized stock sign matrix (" + width + "x" + height + ") at " + pos.toShortString()
                                                                                                    ));
                                                                                                    return 1;
                                                                                                })
                                                                                        )
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                        .then(Commands.literal("refresh")
                                                .executes(ctx -> {
                                                    ServerPlayer player = ctx.getSource().getPlayer();
                                                    io.fabianbuthere.stonks.api.util.SignDisplayManager.updateAllSigns(player.serverLevel());
                                                    player.sendSystemMessage(Component.literal("§aRefreshed all job signs"));
                                                    return 1;
                                                })
                                        )
                                )
                        )
                        .then(Commands.literal("job")
                                .then(Commands.literal("generate")
                                        .requires(source -> source.hasPermission(2))
                                        .requires(CommandSourceStack::isPlayer)
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayer();
                                            DeliveryJob job = JobPool.getInstance().getRandomDeliveryJob();
                                            player.sendSystemMessage(Component.literal("Generated Job: " + job.summary()));
                                            JobSavedData.get(player.serverLevel()).addNewDeliveryJob(job);
                                            return 0;
                                        })
                                )
                                .then(Commands.literal("add")
                                        .then(Commands.literal("delivery")
                                                .then(Commands.argument("items", StringArgumentType.string())
                                                        .then(Commands.argument("payment", IntegerArgumentType.integer())
                                                                .then(Commands.argument("locationIndex", IntegerArgumentType.integer(0))
                                                                        .requires(CommandSourceStack::isPlayer)
                                                                        .executes(ctx -> {
                                                                            ServerPlayer player = ctx.getSource().getPlayer();
                                                                            String itemsStr = StringArgumentType.getString(ctx, "items");
                                                                            int payment = IntegerArgumentType.getInteger(ctx, "payment");
                                                                            int locationIndex = IntegerArgumentType.getInteger(ctx, "locationIndex");
                                                                            
                                                                            List<DeliveryJobPart> parts = parseJobParts(player, itemsStr);
                                                                            if (parts == null) return 0;
                                                                            
                                                                            DeliveryJob job = new DeliveryJob(parts, false, payment, locationIndex, System.currentTimeMillis());
                                                                            JobSavedData.get(player.serverLevel()).addNewDeliveryJob(job);
                                                                            player.sendSystemMessage(Component.literal("Added delivery job: " + job.summary() + " (payment: $" + payment + ", location: " + locationIndex + ")"));
                                                                            return 1;
                                                                        })
                                                                )
                                                        )
                                                )
                                        )
                                        .then(Commands.literal("transport")
                                                .then(Commands.argument("items", StringArgumentType.string())
                                                        .then(Commands.argument("payment", IntegerArgumentType.integer())
                                                                .then(Commands.argument("fromIndex", IntegerArgumentType.integer(0))
                                                                        .then(Commands.argument("toIndex", IntegerArgumentType.integer(0))
                                                                                .requires(CommandSourceStack::isPlayer)
                                                                                .executes(ctx -> {
                                                                                    ServerPlayer player = ctx.getSource().getPlayer();
                                                                                    String itemsStr = StringArgumentType.getString(ctx, "items");
                                                                                    int payment = IntegerArgumentType.getInteger(ctx, "payment");
                                                                                    int fromIndex = IntegerArgumentType.getInteger(ctx, "fromIndex");
                                                                                    int toIndex = IntegerArgumentType.getInteger(ctx, "toIndex");
                                                                                    
                                                                                    List<DeliveryJobPart> parts = parseJobParts(player, itemsStr);
                                                                                    if (parts == null) return 0;
                                                                                    
                                                                                    TransportJob job = new TransportJob(parts, false, fromIndex, toIndex, payment, System.currentTimeMillis());
                                                                                    JobSavedData.get(player.serverLevel()).addNewTransportJob(job);
                                                                                    player.sendSystemMessage(Component.literal("Added transport job: " + job.summary() + " (payment: $" + payment + ", from: " + fromIndex + ", to: " + toIndex + ")"));
                                                                                    return 1;
                                                                                })
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                )
                                .then(Commands.literal("delete")
                                        .then(Commands.literal("all")
                                                .executes(ctx -> {
                                                    ServerPlayer player = ctx.getSource().getPlayer();
                                                    JobSavedData.get(player.serverLevel()).deleteAllJobs();
                                                    player.sendSystemMessage(Component.literal("Deleted all jobs"));
                                                    return 0;
                                                })
                                        )
                                        .then(Commands.literal("delivery")
                                                .then(Commands.argument("index", IntegerArgumentType.integer())
                                                        .executes(ctx -> {
                                                            ServerPlayer player = ctx.getSource().getPlayer();
                                                            int idx = IntegerArgumentType.getInteger(ctx, "index");
                                                            boolean ok = JobSavedData.get(player.serverLevel()).deleteDeliveryJob(idx);
                                                            if (ok) player.sendSystemMessage(Component.literal("Deleted delivery job #" + idx));
                                                            else player.sendSystemMessage(Component.literal("Failed to delete delivery job #" + idx));
                                                            return 0;
                                                        })
                                                )
                                        )
                                        .then(Commands.literal("transport")
                                                .then(Commands.argument("index", IntegerArgumentType.integer())
                                                        .executes(ctx -> {
                                                            ServerPlayer player = ctx.getSource().getPlayer();
                                                            int idx = IntegerArgumentType.getInteger(ctx, "index");
                                                             boolean ok = JobSavedData.get(player.serverLevel()).deleteTransportJob(idx);
                                                            if (ok) player.sendSystemMessage(Component.literal("Deleted transport job #" + idx));
                                                            else player.sendSystemMessage(Component.literal("Failed to delete transport job #" + idx));
                                                            return 0;
                                                        })
                                                )
                                        )
                                )
                                .then(Commands.literal("company")
                                        .then(Commands.literal("add")
                                                .then(Commands.argument("name", StringArgumentType.string())
                                                        .then(Commands.argument("symbol", StringArgumentType.string())
                                                                .then(Commands.argument("shareCount", IntegerArgumentType.integer(50, 1000))
                                                                        .then(Commands.argument("sharePercentage", DoubleArgumentType.doubleArg(0.01, 100.0))
                                                                                .then(Commands.argument("propertySize", IntegerArgumentType.integer(0))
                                                                                        .then(Commands.argument("owner", StringArgumentType.string())
                                                                                                .then(Commands.argument("week1", IntegerArgumentType.integer())
                                                                                                        .then(Commands.argument("week2", IntegerArgumentType.integer())
                                                                                                                .then(Commands.argument("week3", IntegerArgumentType.integer())
                                                                                                                        .then(Commands.argument("week4", IntegerArgumentType.integer())
                                                                                                                                .executes(ctx -> {
                                                                                                                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                                                                                                                    JobSavedData data = JobSavedData.get(player.serverLevel());
                                                                                                                                    
                                                                                                                                    String name = StringArgumentType.getString(ctx, "name");
                                                                                                                                    String symbol = StringArgumentType.getString(ctx, "symbol");
                                                                                                                                    int shareCount = IntegerArgumentType.getInteger(ctx, "shareCount");
                                                                                                                                    double sharePercentage = DoubleArgumentType.getDouble(ctx, "sharePercentage");
                                                                                                                                    int propertySize = IntegerArgumentType.getInteger(ctx, "propertySize");
                                                                                                                                    String ownerName = StringArgumentType.getString(ctx, "owner");
                                                                                                                                    int week1 = IntegerArgumentType.getInteger(ctx, "week1");
                                                                                                                                    int week2 = IntegerArgumentType.getInteger(ctx, "week2");
                                                                                                                                    int week3 = IntegerArgumentType.getInteger(ctx, "week3");
                                                                                                                                    int week4 = IntegerArgumentType.getInteger(ctx, "week4");
                                                                                                                                    
                                                                                                                                    // Lookup player UUID
                                                                                                                                    UUID ownerId = player.getServer().getProfileCache()
                                                                                                                                        .get(ownerName)
                                                                                                                                        .map(p -> p.getId())
                                                                                                                                        .orElse(null);
                                                                                                                                    
                                                                                                                                    if (ownerId == null) {
                                                                                                                                        player.sendSystemMessage(Component.literal("§cPlayer not found: " + ownerName));
                                                                                                                                        return 0;
                                                                                                                                    }
                                                                                                                                    
                                                                                                                                    Company company = new Company(
                                                                                                                                        name,
                                                                                                                                        symbol,
                                                                                                                                        shareCount,
                                                                                                                                        sharePercentage,
                                                                                                                                        new int[]{week1, week2, week3, week4},
                                                                                                                                        propertySize,
                                                                                                                                        ownerId
                                                                                                                                    );
                                                                                                                                    
                                                                                                                                    data.addCompany(company);
                                                                                                                                    player.sendSystemMessage(Component.literal("§aCompany created: " + name + " (" + symbol + ")"));
                                                                                                                                    return 1;
                                                                                                                                })
                                                                                                                        )
                                                                                                                )
                                                                                                        )
                                                                                                )
                                                                                        )
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                        .then(Commands.argument("index", IntegerArgumentType.integer(0))
                                                .then(Commands.literal("rename")
                                                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                                                .executes(ctx -> {
                                                                    ServerPlayer player = ctx.getSource().getPlayer();
                                                                    int index = IntegerArgumentType.getInteger(ctx, "index");
                                                                    String name = StringArgumentType.getString(ctx, "name");
                                                                    JobSavedData data = JobSavedData.get(player.serverLevel());
                                                                    Company company = data.getCompany(index);
                                                                    if (company == null) {
                                                                        player.sendSystemMessage(Component.literal("§cCompany not found"));
                                                                        return 0;
                                                                    }
                                                                    company.setName(name);
                                                                    data.setDirty();
                                                                    player.sendSystemMessage(Component.literal("§aCompany renamed to: " + name));
                                                                    return 1;
                                                                })
                                                        )
                                                )
                                                .then(Commands.literal("symbol")
                                                        .then(Commands.argument("symbol", StringArgumentType.string())
                                                                .executes(ctx -> {
                                                                    ServerPlayer player = ctx.getSource().getPlayer();
                                                                    int index = IntegerArgumentType.getInteger(ctx, "index");
                                                                    String symbol = StringArgumentType.getString(ctx, "symbol");
                                                                    if (symbol.length() != 3) {
                                                                        player.sendSystemMessage(Component.literal("§cSymbol must be exactly 3 characters"));
                                                                        return 0;
                                                                    }
                                                                    JobSavedData data = JobSavedData.get(player.serverLevel());
                                                                    Company company = data.getCompany(index);
                                                                    if (company == null) {
                                                                        player.sendSystemMessage(Component.literal("§cCompany not found"));
                                                                        return 0;
                                                                    }
                                                                    company.setSymbol(symbol.toUpperCase());
                                                                    data.setDirty();
                                                                    player.sendSystemMessage(Component.literal("§aSymbol changed to: " + symbol.toUpperCase()));
                                                                    return 1;
                                                                })
                                                        )
                                                )
                                                .then(Commands.literal("shares")
                                                        .then(Commands.argument("count", IntegerArgumentType.integer(50, 1000))
                                                                .executes(ctx -> {
                                                                    ServerPlayer player = ctx.getSource().getPlayer();
                                                                    int index = IntegerArgumentType.getInteger(ctx, "index");
                                                                    int count = IntegerArgumentType.getInteger(ctx, "count");
                                                                    JobSavedData data = JobSavedData.get(player.serverLevel());
                                                                    Company company = data.getCompany(index);
                                                                    if (company == null) {
                                                                        player.sendSystemMessage(Component.literal("§cCompany not found"));
                                                                        return 0;
                                                                    }
                                                                    company.setShareCount(count);
                                                                    data.setDirty();
                                                                    player.sendSystemMessage(Component.literal("§aShare count set to: " + count));
                                                                    return 1;
                                                                })
                                                        )
                                                )
                                                .then(Commands.literal("percentage")
                                                        .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.0, 100.0))
                                                                .executes(ctx -> {
                                                                    ServerPlayer player = ctx.getSource().getPlayer();
                                                                    int index = IntegerArgumentType.getInteger(ctx, "index");
                                                                    double value = DoubleArgumentType.getDouble(ctx, "value");
                                                                    JobSavedData data = JobSavedData.get(player.serverLevel());
                                                                    Company company = data.getCompany(index);
                                                                    if (company == null) {
                                                                        player.sendSystemMessage(Component.literal("§cCompany not found"));
                                                                        return 0;
                                                                    }
                                                                    company.setSharePercentage(value);
                                                                    data.setDirty();
                                                                    player.sendSystemMessage(Component.literal("§aShare percentage set to: " + String.format("%.2f", value) + "%"));
                                                                    return 1;
                                                                })
                                                        )
                                                )
                                                .then(Commands.literal("property")
                                                        .then(Commands.argument("sqm", IntegerArgumentType.integer(0))
                                                                .executes(ctx -> {
                                                                    ServerPlayer player = ctx.getSource().getPlayer();
                                                                    int index = IntegerArgumentType.getInteger(ctx, "index");
                                                                    int sqm = IntegerArgumentType.getInteger(ctx, "sqm");
                                                                    JobSavedData data = JobSavedData.get(player.serverLevel());
                                                                    Company company = data.getCompany(index);
                                                                    if (company == null) {
                                                                        player.sendSystemMessage(Component.literal("§cCompany not found"));
                                                                        return 0;
                                                                    }
                                                                    company.setTotalPropertySize(sqm);
                                                                    data.setDirty();
                                                                    player.sendSystemMessage(Component.literal("§aProperty size set to: " + sqm + " sqm"));
                                                                    return 1;
                                                                })
                                                        )
                                                )
                                                .then(Commands.literal("profits")
                                                        .then(Commands.argument("week1", IntegerArgumentType.integer())
                                                                .then(Commands.argument("week2", IntegerArgumentType.integer())
                                                                        .then(Commands.argument("week3", IntegerArgumentType.integer())
                                                                                .then(Commands.argument("week4", IntegerArgumentType.integer())
                                                                                        .executes(ctx -> {
                                                                                            ServerPlayer player = ctx.getSource().getPlayer();
                                                                                            int index = IntegerArgumentType.getInteger(ctx, "index");
                                                                                            int w1 = IntegerArgumentType.getInteger(ctx, "week1");
                                                                                            int w2 = IntegerArgumentType.getInteger(ctx, "week2");
                                                                                            int w3 = IntegerArgumentType.getInteger(ctx, "week3");
                                                                                            int w4 = IntegerArgumentType.getInteger(ctx, "week4");
                                                                                            JobSavedData data = JobSavedData.get(player.serverLevel());
                                                                                            Company company = data.getCompany(index);
                                                                                            if (company == null) {
                                                                                                player.sendSystemMessage(Component.literal("§cCompany not found"));
                                                                                                return 0;
                                                                                            }
                                                                                            company.setProfitsLast4Weeks(new int[]{w1, w2, w3, w4});
                                                                                            data.setDirty();
                                                                                            player.sendSystemMessage(Component.literal("§aProfits updated"));
                                                                                            return 1;
                                                                                        })
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                )
                                                .then(Commands.literal("addprofit")
                                                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg())
                                                                .executes(ctx -> {
                                                                    ServerPlayer player = ctx.getSource().getPlayer();
                                                                    int index = IntegerArgumentType.getInteger(ctx, "index");
                                                                    double amount = DoubleArgumentType.getDouble(ctx, "amount");
                                                                    JobSavedData data = JobSavedData.get(player.serverLevel());
                                                                    Company company = data.getCompany(index);
                                                                    if (company == null) {
                                                                        player.sendSystemMessage(Component.literal("§cCompany not found"));
                                                                        return 0;
                                                                    }
                                                                    company.addProfit(amount);
                                                                    data.setDirty();
                                                                    player.sendSystemMessage(Component.literal("§aAdded " + String.format("%.2f", amount) + " to primary account"));
                                                                    return 1;
                                                                })
                                                        )
                                                )
                                                .then(Commands.literal("addaccount")
                                                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                                                .executes(ctx -> {
                                                                    ServerPlayer player = ctx.getSource().getPlayer();
                                                                    int index = IntegerArgumentType.getInteger(ctx, "index");
                                                                    String name = StringArgumentType.getString(ctx, "name");
                                                                    JobSavedData data = JobSavedData.get(player.serverLevel());
                                                                    Company company = data.getCompany(index);
                                                                    if (company == null) {
                                                                        player.sendSystemMessage(Component.literal("§cCompany not found"));
                                                                        return 0;
                                                                    }
                                                                    company.addBankAccount(name);
                                                                    data.setDirty();
                                                                    player.sendSystemMessage(Component.literal("§aAdded bank account: " + name));
                                                                    return 1;
                                                                })
                                                        )
                                                )
                                                .then(Commands.literal("setbalance")
                                                        .then(Commands.argument("account", StringArgumentType.string())
                                                                .then(Commands.argument("balance", DoubleArgumentType.doubleArg())
                                                                        .executes(ctx -> {
                                                                            ServerPlayer player = ctx.getSource().getPlayer();
                                                                            int index = IntegerArgumentType.getInteger(ctx, "index");
                                                                            String account = StringArgumentType.getString(ctx, "account");
                                                                            double balance = DoubleArgumentType.getDouble(ctx, "balance");
                                                                            JobSavedData data = JobSavedData.get(player.serverLevel());
                                                                            Company company = data.getCompany(index);
                                                                            if (company == null) {
                                                                                player.sendSystemMessage(Component.literal("§cCompany not found"));
                                                                                return 0;
                                                                            }
                                                                            company.setBankAccountBalance(account, balance);
                                                                            data.setDirty();
                                                                            player.sendSystemMessage(Component.literal("§aSet " + account + " balance to: $" + String.format("%.2f", balance)));
                                                                            return 1;
                                                                        })
                                                                )
                                                        )
                                                )
                                                .then(Commands.literal("setprimary")
                                                        .then(Commands.argument("account", StringArgumentType.greedyString())
                                                                .executes(ctx -> {
                                                                    ServerPlayer player = ctx.getSource().getPlayer();
                                                                    int index = IntegerArgumentType.getInteger(ctx, "index");
                                                                    String account = StringArgumentType.getString(ctx, "account");
                                                                    JobSavedData data = JobSavedData.get(player.serverLevel());
                                                                    Company company = data.getCompany(index);
                                                                    if (company == null) {
                                                                        player.sendSystemMessage(Component.literal("§cCompany not found"));
                                                                        return 0;
                                                                    }
                                                                    if (!company.getBankAccounts().containsKey(account)) {
                                                                        player.sendSystemMessage(Component.literal("§cBank account not found"));
                                                                        return 0;
                                                                    }
                                                                    company.setPrimaryBankAccount(account);
                                                                    data.setDirty();
                                                                    player.sendSystemMessage(Component.literal("§aSet primary account to: " + account));
                                                                    return 1;
                                                                })
                                                        )
                                                )
                                        )
                                )
                                .then(Commands.literal("stocks")
                                        .then(Commands.literal("init")
                                                .then(Commands.argument("x", IntegerArgumentType.integer())
                                                        .then(Commands.argument("y", IntegerArgumentType.integer())
                                                                .then(Commands.argument("z", IntegerArgumentType.integer())
                                                                        .then(Commands.argument("width", IntegerArgumentType.integer(1, 20))
                                                                                .then(Commands.argument("height", IntegerArgumentType.integer(1, 20))
                                                                                        .executes(ctx -> {
                                                                                            ServerPlayer player = ctx.getSource().getPlayer();
                                                                                            net.minecraft.server.level.ServerLevel level = player.serverLevel();
                                                                                            
                                                                                            int x = IntegerArgumentType.getInteger(ctx, "x");
                                                                                            int y = IntegerArgumentType.getInteger(ctx, "y");
                                                                                            int z = IntegerArgumentType.getInteger(ctx, "z");
                                                                                            int width = IntegerArgumentType.getInteger(ctx, "width");
                                                                                            int height = IntegerArgumentType.getInteger(ctx, "height");
                                                                                            
                                                                                            net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(x, y, z);
                                                                                            
                                                                                            JobSavedData data = JobSavedData.get(level);
                                                                                            data.stockSignMatrixPos = pos;
                                                                                            data.stockSignMatrixWidth = width;
                                                                                            data.stockSignMatrixHeight = height;
                                                                                            data.setDirty();
                                                                                            
                                                                                            io.fabianbuthere.stonks.api.util.StockSignManager.initializeStockSignMatrix(
                                                                                                level, pos, width, height
                                                                                            );
                                                                                            
                                                                                            player.sendSystemMessage(Component.literal(
                                                                                                "§aInitialized stock sign matrix (" + width + "x" + height + ") at " + pos.toShortString()
                                                                                            ));
                                                                                            return 1;
                                                                                        })
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                        .then(Commands.literal("refresh")
                                                .executes(ctx -> {
                                                    ServerPlayer player = ctx.getSource().getPlayer();
                                                    io.fabianbuthere.stonks.api.util.StockSignManager.updateStockSigns(player.serverLevel());
                                                    player.sendSystemMessage(Component.literal("§aRefreshed all stock signs"));
                                                    return 1;
                                                })
                                        )
                                )
                        )
        );
    }
}
