package io.fabianbuthere.stonks.event;

import io.fabianbuthere.stonks.Stonks;
import io.fabianbuthere.stonks.data.JobSavedData;
import io.fabianbuthere.stonks.ui.admin.AdminBarrelMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Stonks.MOD_ID)
public class BarrelProtectionEvents {
    
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        
        BlockPos pos = event.getPos();
        if (!event.getLevel().getBlockState(pos).is(Blocks.BARREL)) return;
        
        JobSavedData data = JobSavedData.get(player.serverLevel());
        
        // Check if this barrel is a job barrel
        boolean isJobBarrel = data.jobData.deliveryLocations.contains(pos) 
                           || data.jobData.transportLocations.contains(pos)
                           || data.jobData.barrelModes.containsKey(pos);
        
        if (isJobBarrel) {
            event.setCanceled(true);
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cThis barrel cannot be broken by hand. Use admin menu to remove it."));
        }
    }
    
    @SubscribeEvent
    public static void onBarrelRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getLevel().isClientSide()) return;
        
        BlockPos pos = event.getPos();
        if (!event.getLevel().getBlockState(pos).is(Blocks.BARREL)) return;
        
        JobSavedData data = JobSavedData.get(player.serverLevel());
        
        // Check if this barrel is a job barrel
        boolean isJobBarrel = data.jobData.deliveryLocations.contains(pos) 
                           || data.jobData.transportLocations.contains(pos)
                           || data.jobData.barrelModes.containsKey(pos);
        
        if (!isJobBarrel) return;
        
        // Check if player has admin mode enabled
        boolean isAdmin = data.jobData.adminModeEnabled.contains(player.getUUID());
        
        if (isAdmin) {
            // Open admin configuration menu
            event.setCanceled(true);
            AdminBarrelMenu.open(player, pos);
        }
        // Otherwise, normal barrel menus will handle it via ServerInteractEvents
    }
}
