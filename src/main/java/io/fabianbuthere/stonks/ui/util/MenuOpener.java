package io.fabianbuthere.stonks.ui.util;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.BiFunction;

@ParametersAreNonnullByDefault
public final class MenuOpener {
    private MenuOpener() {}

    @FunctionalInterface
    public interface MenuFactory {
        AbstractContainerMenu create(int id, Inventory inventory, Player player);
    }

    public static void open(ServerPlayer player, MenuFactory factory, Component title) {
        NetworkHooks.openScreen(player, new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return title;
            }

            @Override
            public AbstractContainerMenu createMenu(int id, Inventory inventory, Player p) {
                return factory.create(id, inventory, p);
            }
        });
    }

    public static void open(ServerPlayer player, BiFunction<Integer, Inventory, AbstractContainerMenu> factory, Component title) {
        open(player, (id, inventory, p) -> factory.apply(id, inventory), title);
    }
}
