package io.fabianbuthere.stonks.api.util;

import io.fabianbuthere.stonks.config.StonksConfig;
import io.fabianbuthere.stonks.data.JobSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class SignDisplayManager {
    
    public static void updateAllSigns(ServerLevel level) {
        updateDeliverySigns(level);
        updateTransportSigns(level);
        updateStockSigns(level);
    }
    
    public static void initializeSignMatrix(ServerLevel level, BlockPos cornerPos, int width, int height) {
        BlockState cornerState = level.getBlockState(cornerPos);
        if (!(cornerState.getBlock() instanceof SignBlock)) {
            return; // No sign at corner position
        }
        
        // Determine directions based on sign orientation
        Direction right;
        Direction down;
        boolean isWallSign = cornerState.getBlock() instanceof WallSignBlock;
        Direction wallFacing = null;
        
        if (isWallSign) {
            wallFacing = cornerState.getValue(BlockStateProperties.HORIZONTAL_FACING);
            // For wall signs, right is clockwise from facing, down is always down
            right = wallFacing.getClockWise();
            down = Direction.DOWN;
        } else {
            // For standing signs, assume standard orientation
            right = Direction.EAST;
            down = Direction.DOWN;
        }
        
        // Place signs in matrix pattern
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                BlockPos pos = cornerPos.relative(right, x).relative(down, y);
                if (x == 0 && y == 0) continue; // Skip corner (already exists)
                
                if (isWallSign) {
                    // For wall signs, place on the wall with correct facing
                    BlockState newState = cornerState.getBlock().defaultBlockState()
                            .setValue(BlockStateProperties.HORIZONTAL_FACING, wallFacing);
                    level.setBlock(pos, newState, 3);
                } else {
                    // For standing signs, copy rotation
                    level.setBlock(pos, cornerState, 3);
                }
            }
        }
    }
    
    private static void updateDeliverySigns(ServerLevel level) {
        BlockPos cornerPos = new BlockPos(
            StonksConfig.DELIVERY_SIGN_X.get(),
            StonksConfig.DELIVERY_SIGN_Y.get(),
            StonksConfig.DELIVERY_SIGN_Z.get()
        );
        
        int width = StonksConfig.DELIVERY_SIGN_WIDTH.get();
        int height = StonksConfig.DELIVERY_SIGN_HEIGHT.get();
        
        var jobData = JobSavedData.get(level).getJobData();
        var jobs = jobData.activeJobs;
        
        updateSignMatrix(level, cornerPos, width, height, jobs, jobData.knownJobs);
    }
    
    private static void updateTransportSigns(ServerLevel level) {
        BlockPos cornerPos = new BlockPos(
            StonksConfig.TRANSPORT_SIGN_X.get(),
            StonksConfig.TRANSPORT_SIGN_Y.get(),
            StonksConfig.TRANSPORT_SIGN_Z.get()
        );
        
        int width = StonksConfig.TRANSPORT_SIGN_WIDTH.get();
        int height = StonksConfig.TRANSPORT_SIGN_HEIGHT.get();
        
        var jobData = JobSavedData.get(level).getJobData();
        var jobs = jobData.activeTransports;
        
        updateSignMatrix(level, cornerPos, width, height, jobs, jobData.transportKnownJobs);
    }
    
    private static <T> void updateSignMatrix(ServerLevel level, BlockPos cornerPos, int width, int height, 
                                             java.util.List<T> jobs, java.util.Map<Integer, java.util.UUID> assignments) {
        BlockState cornerState = level.getBlockState(cornerPos);
        if (!(cornerState.getBlock() instanceof SignBlock)) {
            return;
        }
        
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
        
        int totalSigns = width * height;
        int jobIndex = 0;
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                BlockPos pos = cornerPos.relative(right, x).relative(down, y);
                var blockEntity = level.getBlockEntity(pos);
                
                if (!(blockEntity instanceof SignBlockEntity sign)) {
                    continue;
                }
                
                if (jobIndex < jobs.size()) {
                    T job = jobs.get(jobIndex);
                    boolean taken = assignments.containsKey(jobIndex);
                    
                    // Format job display
                    String[] lines = formatJobForSign(job, jobIndex, taken);
                    for (int lineIdx = 0; lineIdx < 4 && lineIdx < lines.length; lineIdx++) {
                        final int finalLineIdx = lineIdx;
                        final String line = lines[lineIdx];
                        sign.updateText((signText) -> signText.setMessage(finalLineIdx, Component.literal(line)), true);
                    }
                    
                    jobIndex++;
                } else {
                    // Clear unused sign
                    for (int lineIdx = 0; lineIdx < 4; lineIdx++) {
                        final int finalLineIdx = lineIdx;
                        sign.updateText((signText) -> signText.setMessage(finalLineIdx, Component.literal("")), true);
                    }
                }
                
                sign.setChanged();
                level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
            }
        }
    }
    
    private static <T> String[] formatJobForSign(T job, int index, boolean taken) {
        String status = taken ? "§c[TAKEN]" : "§a[OPEN]";
        String jobStr;
        
        if (job instanceof io.fabianbuthere.stonks.api.DeliveryJob dj) {
            jobStr = dj.summary();
            if (jobStr.length() > 15) jobStr = jobStr.substring(0, 12) + "...";
            return new String[] {
                "§6Job #" + index,
                status,
                "§f" + jobStr,
                "§e$" + dj.payment()
            };
        } else if (job instanceof io.fabianbuthere.stonks.api.TransportJob tj) {
            jobStr = tj.summary();
            if (jobStr.length() > 15) jobStr = jobStr.substring(0, 12) + "...";
            return new String[] {
                "§6Job #" + index,
                status,
                "§f" + jobStr,
                "§e$" + tj.payment()
            };
        }
        
        return new String[] {"", "", "", ""};
    }
    
    public static void initializeStockSignMatrix(ServerLevel level, BlockPos cornerPos, int width, int height) {
        initializeSignMatrix(level, cornerPos, width, height);
    }
    
    private static void updateStockSigns(ServerLevel level) {
        BlockPos cornerPos = new BlockPos(
            StonksConfig.STOCK_SIGN_X.get(),
            StonksConfig.STOCK_SIGN_Y.get(),
            StonksConfig.STOCK_SIGN_Z.get()
        );
        
        int width = StonksConfig.STOCK_SIGN_WIDTH.get();
        int height = StonksConfig.STOCK_SIGN_HEIGHT.get();
        
        var companies = JobSavedData.get(level).getCompanies();
        
        BlockState cornerState = level.getBlockState(cornerPos);
        if (!(cornerState.getBlock() instanceof SignBlock)) {
            return;
        }
        
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
        
        int companyIndex = 0;
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                BlockPos pos = cornerPos.relative(right, x).relative(down, y);
                var blockEntity = level.getBlockEntity(pos);
                
                if (!(blockEntity instanceof SignBlockEntity sign)) {
                    continue;
                }
                
                if (companyIndex < companies.size()) {
                    var company = companies.get(companyIndex);
                    double stockPrice = company.calculateStockPrice();
                    
                    sign.updateText((signText) -> signText.setMessage(0, Component.literal("§6" + company.getSymbol())), true);
                    sign.updateText((signText) -> signText.setMessage(1, Component.literal("§f" + company.getName())), true);
                    sign.updateText((signText) -> signText.setMessage(2, Component.literal("§e$" + String.format("%.2f", stockPrice))), true);
                    sign.updateText((signText) -> signText.setMessage(3, Component.literal("§7Click to buy")), true);
                    
                    companyIndex++;
                } else {
                    // Clear unused sign
                    for (int lineIdx = 0; lineIdx < 4; lineIdx++) {
                        final int finalLineIdx = lineIdx;
                        sign.updateText((signText) -> signText.setMessage(finalLineIdx, Component.literal("")), true);
                    }
                }
                
                sign.setChanged();
                level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
            }
        }
    }
}
