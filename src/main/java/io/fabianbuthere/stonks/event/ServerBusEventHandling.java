package io.fabianbuthere.stonks.event;

import io.fabianbuthere.stonks.Stonks;
import io.fabianbuthere.stonks.data.JobSavedData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Stonks.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ServerBusEventHandling {
    // Use for server side lifecycle events
    // Make sure to manually ensure server side only handling

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = event.getServer();
        if (server == null) return;
        ServerLevel level = server.getLevel(Level.OVERWORLD);
        if (level == null) return;
        try {
            JobSavedData savedData = JobSavedData.get(level);
            savedData.serverTick(level);
            savedData.checkTransportTimeouts(level);
            
            // Update signs every 20 ticks (1 second)
            if (level.getGameTime() % 20 == 0) {
                io.fabianbuthere.stonks.api.util.SignDisplayManager.updateAllSigns(level);
            }
        } catch (Exception e) {
            Stonks.LOGGER.error("Error during job generation tick", e);
        }
    }
}
