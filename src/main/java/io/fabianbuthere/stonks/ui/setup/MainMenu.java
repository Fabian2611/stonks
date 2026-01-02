package io.fabianbuthere.stonks.ui.setup;

import io.fabianbuthere.stonks.ui.util.MenuOpener;
import io.fabianbuthere.stonks.ui.util.UiItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class MainMenu extends ChestMenu {
    private static final int ROWS = 3;

    public MainMenu(int id, Inventory playerInv) {
        this(id, playerInv, new SimpleContainer(ROWS * 9));
    }

    public MainMenu(int id, Inventory playerInv, Container uiInv) {
        super(MenuType.GENERIC_9x3, id, playerInv, uiInv, ROWS);
        buildUi(uiInv);
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId < 0) {
            super.clicked(slotId, button, clickType, player);
            return;
        }

        if (slotId < ROWS * 9) {
            handleUiClick(slotId, player);
            return;
        }

        // Prevent shift-click from moving items into UI etc.
        if (clickType == ClickType.QUICK_MOVE) return;

        // Optionally allow normal player inventory behavior:
        super.clicked(slotId, button, clickType, player);
    }

    private void handleUiClick(int slotId, Player player) {
        if (slotId == 11) {
            StockMenu.open((ServerPlayer) player);
        } else if (slotId == 13) {
            ConfigMenu.open((ServerPlayer) player);
        } else if (slotId == 15) {
            JobMenu.open((ServerPlayer) player);
        }
    }

    private void buildUi(Container ui) {
        UiItems.fill(ui, ROWS);

        UiItems.button(Items.EMERALD, "Stocks", 11, ui);
        UiItems.button(Items.LEVER, "Settings", 13, ui);
        UiItems.button(Items.IRON_PICKAXE, "Jobs", 15, ui);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    public static void open(ServerPlayer player) {
        MenuOpener.open(player, (a, b) -> new MainMenu(a, b), Component.literal("Stonks Setup"));
    }
}
