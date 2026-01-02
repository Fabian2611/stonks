package io.fabianbuthere.stonks.ui.setup;

import io.fabianbuthere.stonks.api.stock.Company;
import io.fabianbuthere.stonks.data.JobSavedData;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class StockMenu extends ChestMenu {
    private static final int ROWS = 6;
    private final ServerPlayer serverPlayer;
    private int scrollOffset = 0;

    public StockMenu(int id, Inventory playerInv, ServerPlayer player) {
        this(id, playerInv, new SimpleContainer(ROWS * 9), player);
    }

    public StockMenu(int id, Inventory playerInv, Container uiInv, ServerPlayer player) {
        super(MenuType.GENERIC_9x6, id, playerInv, uiInv, ROWS);
        this.serverPlayer = player;
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

        if (clickType == ClickType.QUICK_MOVE) return;
        super.clicked(slotId, button, clickType, player);
    }

    private void handleUiClick(int slotId, Player player) {
        JobSavedData data = JobSavedData.get(serverPlayer.serverLevel());
        
        // Back button
        if (slotId == 49) {
            MainMenu.open(serverPlayer);
            return;
        }
        
        // Add company button
        if (slotId == 45) {
            player.sendSystemMessage(Component.literal("§eUse command: /stonks setup company add <name> <symbol> <shareCount> <sharePercentage> <propertySize> <owner> <w1> <w2> <w3> <w4>"));
            player.sendSystemMessage(Component.literal("§7Example: /stonks setup company add \"My Corp\" MYC 100 1.5 5000 PlayerName 1000 1200 900 1100"));
            return;
        }
        
        // Scroll up
        if (slotId == 47) {
            scrollOffset = Math.max(0, scrollOffset - 1);
            buildUi(getContainer());
            return;
        }
        
        // Scroll down
        if (slotId == 51) {
            int maxScroll = Math.max(0, data.companies.size() - 36);
            scrollOffset = Math.min(maxScroll, scrollOffset + 1);
            buildUi(getContainer());
            return;
        }
        
        // Company slots (first 4 rows)
        if (slotId < 36) {
            int companyIndex = scrollOffset + slotId;
            if (companyIndex < data.companies.size()) {
                Company company = data.companies.get(companyIndex);
                CompanyEditMenu.open(serverPlayer, company.getSymbol());
            }
        }
    }

    private void buildUi(Container ui) {
        JobSavedData data = JobSavedData.get(serverPlayer.serverLevel());
        ui.clearContent();
        
        // Fill bottom row with glass
        ItemStack filler = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
        filler.setHoverName(Component.literal(""));
        for (int i = 45; i < 54; i++) {
            ui.setItem(i, filler);
        }
        
        // Display companies (first 4 rows = 36 slots)
        List<Company> companies = data.companies;
        for (int i = 0; i < 36; i++) {
            int companyIndex = scrollOffset + i;
            if (companyIndex < companies.size()) {
                Company company = companies.get(companyIndex);
                ItemStack item = new ItemStack(Items.PAPER);
                item.setHoverName(Component.literal("§6" + company.getName() + " §7(" + company.getSymbol() + ")"));
                
                List<Component> lore = new ArrayList<>();
                lore.add(Component.literal("§7Shares: §f" + company.getShareCount()));
                lore.add(Component.literal("§7Share %: §f" + company.getSharePercentage() + "%"));
                String ownerName = serverPlayer.getServer().getProfileCache()
                        .get(company.getOwnerId())
                        .map(p -> p.getName())
                        .orElse("Unknown");
                lore.add(Component.literal("§7Owner: §f" + ownerName));
                lore.add(Component.literal("§7Value: §a$" + String.format("%.2f", company.calculateCompanyValue())));
                lore.add(Component.literal(""));
                lore.add(Component.literal("§eClick to edit"));
                
                net.minecraft.nbt.ListTag loreTag = new net.minecraft.nbt.ListTag();
                for (Component c : lore) {
                    loreTag.add(net.minecraft.nbt.StringTag.valueOf(Component.Serializer.toJson(c)));
                }
                item.getOrCreateTagElement("display").put("Lore", loreTag);
                
                ui.setItem(i, item);
            }
        }
        
        // Control buttons
        UiItems.button(Items.EMERALD, "§aAdd Company", 45, ui);
        UiItems.button(Items.ARROW, "§eScroll Up", 47, ui);
        UiItems.button(Items.ARROW, "§eScroll Down", 51, ui);
        UiItems.button(Items.BARRIER, "§cBack", 49, ui);
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
        MenuOpener.open(player, (id, inv) -> new StockMenu(id, inv, player), Component.literal("Stock Management"));
    }
}
