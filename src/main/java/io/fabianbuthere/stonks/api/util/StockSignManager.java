package io.fabianbuthere.stonks.api.util;

import io.fabianbuthere.stonks.api.stock.Company;
import io.fabianbuthere.stonks.data.JobSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.List;

public class StockSignManager {
    
    public static void initializeStockSignMatrix(ServerLevel level, BlockPos upperLeft, int width, int height) {
        BlockState existingState = level.getBlockState(upperLeft);
        if (!(existingState.getBlock() instanceof WallSignBlock)) {
            return;
        }
        
        Direction facing = existingState.getValue(BlockStateProperties.HORIZONTAL_FACING);
        Direction rightDir;
        Direction downDir = Direction.DOWN;
        
        switch (facing) {
            case NORTH:
                rightDir = Direction.EAST;
                break;
            case SOUTH:
                rightDir = Direction.WEST;
                break;
            case EAST:
                rightDir = Direction.SOUTH;
                break;
            case WEST:
                rightDir = Direction.NORTH;
                break;
            default:
                rightDir = Direction.EAST;
        }
        
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                if (row == 0 && col == 0) continue;
                
                BlockPos pos = upperLeft
                        .relative(rightDir, col)
                        .relative(downDir, row);
                
                level.setBlock(pos, existingState, 3);
                
                if (level.getBlockEntity(pos) instanceof SignBlockEntity sign) {
                    sign.setAllowedPlayerEditor(null);
                }
            }
        }
        
        updateStockSigns(level);
    }
    
    public static void updateStockSigns(ServerLevel level) {
        JobSavedData data = JobSavedData.get(level);
        
        if (data.stockSignMatrixPos == null) {
            return;
        }
        
        BlockPos upperLeft = data.stockSignMatrixPos;
        int width = data.stockSignMatrixWidth;
        int height = data.stockSignMatrixHeight;
        
        BlockState existingState = level.getBlockState(upperLeft);
        if (!(existingState.getBlock() instanceof WallSignBlock)) {
            return;
        }
        
        Direction facing = existingState.getValue(BlockStateProperties.HORIZONTAL_FACING);
        Direction rightDir;
        Direction downDir = Direction.DOWN;
        
        switch (facing) {
            case NORTH:
                rightDir = Direction.EAST;
                break;
            case SOUTH:
                rightDir = Direction.WEST;
                break;
            case EAST:
                rightDir = Direction.SOUTH;
                break;
            case WEST:
                rightDir = Direction.NORTH;
                break;
            default:
                rightDir = Direction.EAST;
        }
        
        List<Company> companies = data.companies;
        int totalSlots = width * height;
        
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                int index = row * width + col;
                
                BlockPos pos = upperLeft
                        .relative(rightDir, col)
                        .relative(downDir, row);
                
                if (level.getBlockEntity(pos) instanceof SignBlockEntity sign) {
                    if (index < companies.size()) {
                        Company company = companies.get(index);
                        int stockPrice = company.calculateStockPrice();
                        int availableShares = company.getAvailableShares();
                        
                        sign.updateText((signText) -> signText.setMessage(0, Component.literal("§6§l" + company.getSymbol())), true);
                        sign.updateText((signText) -> signText.setMessage(1, Component.literal("§f" + company.getName())), true);
                        sign.updateText((signText) -> signText.setMessage(2, Component.literal("§a$" + stockPrice)), true);
                        sign.updateText((signText) -> signText.setMessage(3, Component.literal("§7" + availableShares + " shares")), true);
                    } else {
                        sign.updateText((signText) -> signText.setMessage(0, Component.literal("")), true);
                        sign.updateText((signText) -> signText.setMessage(1, Component.literal("")), true);
                        sign.updateText((signText) -> signText.setMessage(2, Component.literal("")), true);
                        sign.updateText((signText) -> signText.setMessage(3, Component.literal("")), true);
                    }
                    sign.setChanged();
                    level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
                }
            }
        }
    }
    
    public static Company getCompanyAtSign(ServerLevel level, BlockPos signPos) {
        JobSavedData data = JobSavedData.get(level);
        
        if (data.stockSignMatrixPos == null) {
            return null;
        }
        
        BlockPos upperLeft = data.stockSignMatrixPos;
        int width = data.stockSignMatrixWidth;
        
        BlockState existingState = level.getBlockState(upperLeft);
        if (!(existingState.getBlock() instanceof WallSignBlock)) {
            return null;
        }
        
        Direction facing = existingState.getValue(BlockStateProperties.HORIZONTAL_FACING);
        Direction rightDir;
        Direction downDir = Direction.DOWN;
        
        switch (facing) {
            case NORTH:
                rightDir = Direction.EAST;
                break;
            case SOUTH:
                rightDir = Direction.WEST;
                break;
            case EAST:
                rightDir = Direction.SOUTH;
                break;
            case WEST:
                rightDir = Direction.NORTH;
                break;
            default:
                rightDir = Direction.EAST;
        }
        
        BlockPos relative = signPos.subtract(upperLeft);
        int col = 0;
        int row = 0;
        
        BlockPos testPos = upperLeft;
        while (!testPos.equals(signPos)) {
            BlockPos nextRight = testPos.relative(rightDir);
            if (nextRight.equals(signPos) || isOnPath(upperLeft, signPos, nextRight, rightDir, downDir)) {
                col++;
                testPos = nextRight;
            } else {
                BlockPos nextDown = upperLeft.relative(downDir, row + 1);
                if (nextDown.equals(signPos) || isOnPath(upperLeft, signPos, nextDown, rightDir, downDir)) {
                    row++;
                    col = 0;
                    testPos = nextDown;
                } else {
                    break;
                }
            }
            
            if (col >= width) {
                return null;
            }
        }
        
        int index = row * width + col;
        
        if (index >= 0 && index < data.companies.size()) {
            return data.companies.get(index);
        }
        
        return null;
    }
    
    private static boolean isOnPath(BlockPos start, BlockPos target, BlockPos current, Direction right, Direction down) {
        return current.closerThan(target, start.distManhattan(target) + 1);
    }
}
