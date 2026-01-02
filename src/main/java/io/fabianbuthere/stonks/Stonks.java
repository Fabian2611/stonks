package io.fabianbuthere.stonks;

import com.mojang.logging.LogUtils;
import io.fabianbuthere.stonks.config.StonksConfig;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

@Mod(Stonks.MOD_ID)
public class Stonks
{
    public static final String MOD_ID = "stonks";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Stonks(FMLJavaModLoadingContext context)
    {
        if (FMLEnvironment.dist.isClient()) {
            LOGGER.error("Stonks is server-side only and should NOT be installed on a client.");
            LOGGER.error("Expect crashes and other issues!");
        }

        IEventBus modEventBus = context.getModEventBus();

        modEventBus.addListener(this::commonSetup);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, StonksConfig.COMMON_CONFIG);

        MinecraftForge.EVENT_BUS.register(this);
    }

    @SuppressWarnings("removal")
    public Stonks() {
        this(FMLJavaModLoadingContext.get());
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {

    }
}
