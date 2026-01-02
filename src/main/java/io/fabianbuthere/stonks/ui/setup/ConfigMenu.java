package io.fabianbuthere.stonks.ui.setup;

import io.fabianbuthere.stonks.config.StonksConfig;
import io.fabianbuthere.stonks.ui.util.MenuOpener;
import io.fabianbuthere.stonks.ui.util.UiItems;
import net.minecraft.ChatFormatting;
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

public class ConfigMenu extends ChestMenu {
    private static final int ROWS = 6;
    private ServerPlayer owner;
    private Container uiContainer;

    public ConfigMenu(int id, Inventory playerInv, Player requester) {
        this(id, playerInv, new SimpleContainer(ROWS * 9));
        if (requester instanceof ServerPlayer sp) {
            this.owner = sp;
            buildUi(this.uiContainer, sp);
        }
    }

    public ConfigMenu(int id, Inventory playerInv, Container uiInv) {
        super(MenuType.GENERIC_9x6, id, playerInv, uiInv, ROWS);
        this.uiContainer = uiInv;
        if (playerInv.player instanceof ServerPlayer sp) {
            this.owner = sp;
        }
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId < 0) {
            super.clicked(slotId, button, clickType, player);
            return;
        }

        if (slotId < ROWS * 9) {
            handleUiClick(slotId, button, player);
            return;
        }

        if (clickType == ClickType.QUICK_MOVE) return;
        super.clicked(slotId, button, clickType, player);
    }

    private void handleUiClick(int slotId, int button, Player player) {
        if (!(player instanceof ServerPlayer sp)) return;

        boolean needsRefresh = false;

        // Row 0: Job Generation Settings
        if (slotId == 4) { // Generation Interval (centered)
            int current = StonksConfig.JOB_GENERATION_INTERVAL_SECONDS.get();
            int newValue = button == 0 ? current + 60 : Math.max(10, current - 60);
            StonksConfig.JOB_GENERATION_INTERVAL_SECONDS.set(newValue);
            StonksConfig.COMMON_CONFIG.save();
            sp.sendSystemMessage(Component.literal("Job generation interval: " + newValue + "s"));
            needsRefresh = true;
        }

        // Row 1: Max Jobs
        if (slotId == 11) { // Max Delivery Jobs
            int current = StonksConfig.MAX_JOBS_PER_CATEGORY_DELIVERING.get();
            int newValue = button == 0 ? current + 5 : Math.max(1, current - 5);
            StonksConfig.MAX_JOBS_PER_CATEGORY_DELIVERING.set(newValue);
            StonksConfig.COMMON_CONFIG.save();
            sp.sendSystemMessage(Component.literal("Max delivery jobs: " + newValue));
            needsRefresh = true;
        } else if (slotId == 15) { // Max Transport Jobs
            int current = StonksConfig.MAX_JOBS_PER_CATEGORY_TRANSPORT.get();
            int newValue = button == 0 ? current + 5 : Math.max(1, current - 5);
            StonksConfig.MAX_JOBS_PER_CATEGORY_TRANSPORT.set(newValue);
            StonksConfig.COMMON_CONFIG.save();
            sp.sendSystemMessage(Component.literal("Max transport jobs: " + newValue));
            needsRefresh = true;
        }

        // Row 2: Min Jobs
        if (slotId == 11 + 9) { // Min Delivery Jobs (slot 20)
            int current = StonksConfig.MIN_JOBS_PER_CATEGORY_DELIVERING.get();
            int newValue = button == 0 ? current + 1 : Math.max(0, current - 1);
            StonksConfig.MIN_JOBS_PER_CATEGORY_DELIVERING.set(newValue);
            StonksConfig.COMMON_CONFIG.save();
            sp.sendSystemMessage(Component.literal("Min delivery jobs: " + newValue));
            needsRefresh = true;
        } else if (slotId == 15 + 9) { // Min Transport Jobs (slot 24)
            int current = StonksConfig.MIN_JOBS_PER_CATEGORY_TRANSPORT.get();
            int newValue = button == 0 ? current + 1 : Math.max(0, current - 1);
            StonksConfig.MIN_JOBS_PER_CATEGORY_TRANSPORT.set(newValue);
            StonksConfig.COMMON_CONFIG.save();
            sp.sendSystemMessage(Component.literal("Min transport jobs: " + newValue));
            needsRefresh = true;
        }

        // Row 3: Player Limits - Delivery
        if (slotId == 20 + 9) { // Max Accepted Delivery per Player (slot 29)
            int current = StonksConfig.MAX_ACCEPTED_PER_CATEGORY_PER_PLAYER_DELIVERING.get();
            int newValue = button == 0 ? current + 1 : Math.max(0, current - 1);
            StonksConfig.MAX_ACCEPTED_PER_CATEGORY_PER_PLAYER_DELIVERING.set(newValue);
            StonksConfig.COMMON_CONFIG.save();
            sp.sendSystemMessage(Component.literal("Max accepted delivery per player: " + newValue));
            needsRefresh = true;
        } else if (slotId == 24 + 9) { // Max Accepted Transport per Player (slot 33)
            int current = StonksConfig.MAX_ACCEPTED_PER_CATEGORY_PER_PLAYER_TRANSPORT.get();
            int newValue = button == 0 ? current + 1 : Math.max(0, current - 1);
            StonksConfig.MAX_ACCEPTED_PER_CATEGORY_PER_PLAYER_TRANSPORT.set(newValue);
            StonksConfig.COMMON_CONFIG.save();
            sp.sendSystemMessage(Component.literal("Max accepted transport per player: " + newValue));
            needsRefresh = true;
        }

        // Row 4: Total Player Limit
        if (slotId == 31 + 9) { // Max Total Accepted per Player (slot 40)
            int current = StonksConfig.MAX_ACCEPTED_TOTAL_PER_PLAYER.get();
            int newValue = button == 0 ? current + 1 : Math.max(0, current - 1);
            StonksConfig.MAX_ACCEPTED_TOTAL_PER_PLAYER.set(newValue);
            StonksConfig.COMMON_CONFIG.save();
            sp.sendSystemMessage(Component.literal("Max total accepted per player: " + newValue));
            needsRefresh = true;
        }

        // Back button
        if (slotId == 45) {
            MainMenu.open(sp);
            return;
        }

        // Close button
        if (slotId == 53) {
            sp.closeContainer();
            return;
        }

        // Refresh the menu to show updated values
        if (needsRefresh) {
            sp.closeContainer();
            // Small delay to ensure menu closes before reopening
            sp.getServer().execute(() -> ConfigMenu.open(sp));
        }
    }

    private void buildUi(Container ui, ServerPlayer player) {
        UiItems.fill(ui, ROWS);
        if (player == null) return;

        // Row 0: Generation Settings (centered at slot 4)
        int interval = StonksConfig.JOB_GENERATION_INTERVAL_SECONDS.get();
        UiItems.button(Items.CLOCK, Component.literal("§eGeneration Interval"), 4, ui, 
            "§7Current: §f" + interval + "s",
            "§7Left-click: §a+60s",
            "§7Right-click: §c-60s");

        // Row 1: Max Jobs per Category (slots 11 and 15, more centered)
        int maxDelivery = StonksConfig.MAX_JOBS_PER_CATEGORY_DELIVERING.get();
        int maxTransport = StonksConfig.MAX_JOBS_PER_CATEGORY_TRANSPORT.get();
        
        UiItems.button(Items.PAPER, Component.literal("§eMax Delivery Jobs"), 11, ui,
            "§7Current: §f" + maxDelivery,
            "§7Left-click: §a+5",
            "§7Right-click: §c-5");
        
        UiItems.button(Items.CHEST_MINECART, Component.literal("§eMax Transport Jobs"), 15, ui,
            "§7Current: §f" + maxTransport,
            "§7Left-click: §a+5",
            "§7Right-click: §c-5");

        // Row 2: Min Jobs per Category (slots 20 and 24)
        int minDelivery = StonksConfig.MIN_JOBS_PER_CATEGORY_DELIVERING.get();
        int minTransport = StonksConfig.MIN_JOBS_PER_CATEGORY_TRANSPORT.get();
        
        UiItems.button(Items.PAPER, Component.literal("§eMin Delivery Jobs"), 20, ui,
            "§7Current: §f" + minDelivery,
            "§7Always maintain this many",
            "§7Left-click: §a+1",
            "§7Right-click: §c-1");
        
        UiItems.button(Items.CHEST_MINECART, Component.literal("§eMin Transport Jobs"), 24, ui,
            "§7Current: §f" + minTransport,
            "§7Always maintain this many",
            "§7Left-click: §a+1",
            "§7Right-click: §c-1");

        // Row 3: Per-Category Player Limits (slots 29 and 33)
        int maxDeliveryPlayer = StonksConfig.MAX_ACCEPTED_PER_CATEGORY_PER_PLAYER_DELIVERING.get();
        int maxTransportPlayer = StonksConfig.MAX_ACCEPTED_PER_CATEGORY_PER_PLAYER_TRANSPORT.get();
        
        UiItems.button(Items.PLAYER_HEAD, Component.literal("§eMax Delivery/Player"), 29, ui,
            "§7Current: §f" + maxDeliveryPlayer,
            "§7Left-click: §a+1",
            "§7Right-click: §c-1");
        
        UiItems.button(Items.PLAYER_HEAD, Component.literal("§eMax Transport/Player"), 33, ui,
            "§7Current: §f" + maxTransportPlayer,
            "§7Left-click: §a+1",
            "§7Right-click: §c-1");

        // Row 4: Total Player Limit (centered at slot 40)
        int maxTotal = StonksConfig.MAX_ACCEPTED_TOTAL_PER_PLAYER.get();
        
        UiItems.button(Items.DIAMOND, Component.literal("§eMax Total/Player"), 40, ui,
            "§7Current: §f" + maxTotal,
            "§7All categories combined",
            "§7Left-click: §a+1",
            "§7Right-click: §c-1");

        // Row 5: Navigation buttons
        UiItems.button(Items.ARROW, "§eBack to Setup", 45, ui);
        UiItems.button(Items.BARRIER, "§cClose", 53, ui);
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
        MenuOpener.open(player, (id, inv, p) -> new ConfigMenu(id, inv, p), Component.literal("Job System Settings"));
    }
}
