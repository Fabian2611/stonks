package io.fabianbuthere.stonks.ui.setup;

import io.fabianbuthere.stonks.data.JobSavedData;
import io.fabianbuthere.stonks.ui.util.MenuOpener;
import io.fabianbuthere.stonks.ui.util.UiItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;

public class LocationsMenu extends ChestMenu {
    private static final int ROWS = 3;
    private Container uiContainer;

    private enum Category { DELIVERY, TRANSPORT }
    private Category current = Category.DELIVERY;

    public LocationsMenu(int id, Inventory playerInv) {
        this(id, playerInv, new SimpleContainer(ROWS * 9));
    }

    public LocationsMenu(int id, Inventory playerInv, Container uiInv) {
        super(MenuType.GENERIC_9x3, id, playerInv, uiInv, ROWS);
        this.uiContainer = uiInv;
        if (playerInv.player instanceof ServerPlayer sp) buildUi(uiInv, sp);
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

        if (clickType == ClickType.QUICK_MOVE) return;
        super.clicked(slotId, button, clickType, player);
    }

    private void handleUiClick(int slotId, Player player) {
        if (!(player instanceof ServerPlayer sp)) return;
        ServerLevel level = (ServerLevel) sp.level();
        var data = JobSavedData.get(level).getJobData();

        if (slotId == 11) {
            current = Category.DELIVERY;
            buildUi(this.uiContainer, sp);
            return;
        } else if (slotId == 13) {
            current = Category.TRANSPORT;
            buildUi(this.uiContainer, sp);
            return;
        } else if (slotId == 15) {
            // add current player location to selected list
            BlockPos pos = sp.blockPosition();
            if (current == Category.DELIVERY) {
                data.deliveryLocations.add(pos);
                level.setBlock(pos, Blocks.BARREL.defaultBlockState(), 3);
            } else {
                data.transportLocations.add(pos);
                level.setBlock(pos, Blocks.BARREL.defaultBlockState(), 3);
            }
            JobSavedData.get(level).setDirty();
            buildUi(this.uiContainer, sp);
            return;
        }

        // clicking location entries removes them
        if (slotId >= 0 && slotId < 13) {
            int idx = slotId;
            if (current == Category.DELIVERY) {
                if (idx < data.deliveryLocations.size()) {
                    data.deliveryLocations.remove(idx);
                    JobSavedData.get(level).setDirty();
                    buildUi(this.uiContainer, sp);
                }
            } else {
                if (idx < data.transportLocations.size()) {
                    data.transportLocations.remove(idx);
                    JobSavedData.get(level).setDirty();
                    buildUi(this.uiContainer, sp);
                }
            }
        }
    }

    private void buildUi(Container ui, ServerPlayer player) {
        UiItems.fill(ui, ROWS);
        if (player == null) return;
        ServerLevel level = (ServerLevel) player.level();
        var data = JobSavedData.get(level).getJobData();

        UiItems.button(Items.PAPER, current == Category.DELIVERY ? "▶ Delivery Locations" : "Delivery Locations", 11, ui);
        UiItems.button(Items.CHEST, current == Category.TRANSPORT ? "▶ Transport Locations" : "Transport Locations", 13, ui);
        UiItems.button(Items.EMERALD_BLOCK, "Add Location (your pos)", 15, ui);

        if (current == Category.DELIVERY) {
            for (int i = 0; i < data.deliveryLocations.size() && i < 13; i++) {
                BlockPos p = data.deliveryLocations.get(i);
                ItemStack stack = new ItemStack(Items.BARREL);
                stack.setHoverName(Component.literal("#%d — %s".formatted(i, p.toString())));
                ui.setItem(i, stack);
            }
        } else {
            for (int i = 0; i < data.transportLocations.size() && i < 13; i++) {
                BlockPos p = data.transportLocations.get(i);
                ItemStack stack = new ItemStack(Items.BARREL);
                stack.setHoverName(Component.literal("#%d — %s".formatted(i, p.toString())));
                ui.setItem(i, stack);
            }
        }

        UiItems.button(Items.BARRIER, "Close", 26, ui);
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
        MenuOpener.open(player, (id, inv, p) -> new LocationsMenu(id, inv), Component.literal("Location Management"));
    }
}
