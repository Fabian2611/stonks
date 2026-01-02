package io.fabianbuthere.stonks.ui.setup;

import io.fabianbuthere.stonks.api.stock.Company;
import io.fabianbuthere.stonks.data.JobSavedData;
import io.fabianbuthere.stonks.ui.util.UiItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
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

public class CompanyEditMenu extends ChestMenu {
    private static final int ROWS = 6;
    private final ServerPlayer serverPlayer;
    private final String companySymbol;
    private int bankScrollOffset = 0;

    public CompanyEditMenu(int id, Inventory playerInv, ServerPlayer player, String companySymbol) {
        this(id, playerInv, new SimpleContainer(ROWS * 9), player, companySymbol);
    }

    public CompanyEditMenu(int id, Inventory playerInv, Container uiInv, ServerPlayer player, String companySymbol) {
        super(MenuType.GENERIC_9x6, id, playerInv, uiInv, ROWS);
        this.serverPlayer = player;
        this.companySymbol = companySymbol;
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
        Company company = data.companies.stream()
                .filter(c -> c.getSymbol().equals(companySymbol))
                .findFirst()
                .orElse(null);
                
        if (company == null) {
            StockMenu.open(serverPlayer);
            return;
        }
        
        // Back button
        if (slotId == 49) {
            StockMenu.open(serverPlayer);
            return;
        }
        
        // Delete company
        if (slotId == 53) {
            data.companies.removeIf(c -> c.getSymbol().equals(companySymbol));
            data.setDirty();
            player.sendSystemMessage(Component.literal("§cDeleted company: " + company.getName()));
            StockMenu.open(serverPlayer);
            return;
        }
        
        // Edit shares (slot 11)
        if (slotId == 11) {
            player.sendSystemMessage(Component.literal("§eUse: /stonks setup company <index> shares <count>"));
            player.sendSystemMessage(Component.literal("§eUse: /stonks setup company <index> percentage <value>"));
            return;
        }
        
        // Edit property (slot 13)
        if (slotId == 13) {
            player.sendSystemMessage(Component.literal("§eUse: /stonks setup company <index> property <sqm>"));
            return;
        }
        
        // Edit profits (slot 15)
        if (slotId == 15) {
            player.sendSystemMessage(Component.literal("§eUse: /stonks setup company <index> profits <w1> <w2> <w3> <w4>"));
            return;
        }
        
        // Add profit button (slot 20)
        if (slotId == 20) {
            player.sendSystemMessage(Component.literal("§eUse: /stonks setup company <index> addprofit <amount>"));
            player.sendSystemMessage(Component.literal("§7Example: /stonks setup company 0 addprofit 1000"));
            return;
        }
        
        // Add bank account button (slot 24)
        if (slotId == 24) {
            player.sendSystemMessage(Component.literal("§eUse: /stonks setup company <index> addaccount <name>"));
            return;
        }
        
        // Bank account clicks (slots 27-35)
        if (slotId >= 27 && slotId <= 35) {
            int bankIndex = (slotId - 27) + (bankScrollOffset * 9);
            List<String> bankNames = new ArrayList<>(company.getBankAccounts().keySet());
            if (bankIndex < bankNames.size()) {
                String bankName = bankNames.get(bankIndex);
                player.sendSystemMessage(Component.literal("§eBank Account: §f" + bankName));
                player.sendSystemMessage(Component.literal("§7Set balance: /stonks setup company <index> setbalance " + bankName + " <amount>"));
                player.sendSystemMessage(Component.literal("§7Set as primary: /stonks setup company <index> setprimary " + bankName));
            }
            return;
        }
    }

    private void buildUi(Container container) {
        JobSavedData data = JobSavedData.get(serverPlayer.serverLevel());
        Company company = data.companies.stream()
                .filter(c -> c.getSymbol().equals(companySymbol))
                .findFirst()
                .orElse(null);
        
        if (company == null) {
            return;
        }
        
        // Name and info
        ItemStack nameItem = new ItemStack(Items.PAPER);
        nameItem.setHoverName(Component.literal("§6" + company.getName()));
        CompoundTag display = new CompoundTag();
        ListTag lore = new ListTag();
        lore.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal("§7Symbol: §f" + company.getSymbol()))));
        String ownerName = serverPlayer.getServer().getProfileCache()
                .get(company.getOwnerId())
                .map(p -> p.getName())
                .orElse("Unknown");
        lore.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal("§7Owner: §f" + ownerName))));
        display.put("Lore", lore);
        nameItem.getOrCreateTag().put("display", display);
        container.setItem(4, nameItem);
        
        // Shares info (editable)
        ItemStack sharesItem = new ItemStack(Items.GOLD_INGOT);
        sharesItem.setHoverName(Component.literal("§eShares"));
        display = new CompoundTag();
        lore = new ListTag();
        lore.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal("§7Count: §f" + company.getShareCount()))));
        lore.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal("§7Percentage: §f" + company.getSharePercentage() + "%"))));
        lore.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal("§7Sold: §f" + company.getSoldShares()))));
        lore.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal("§7Available: §f" + company.getAvailableShares()))));
        lore.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(""))));
        lore.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal("§eClick to edit"))));
        display.put("Lore", lore);
        sharesItem.getOrCreateTag().put("display", display);
        container.setItem(11, sharesItem);
        
        // Property info (editable)
        ItemStack propertyItem = new ItemStack(Items.GRASS_BLOCK);
        propertyItem.setHoverName(Component.literal("§2Property"));
        display = new CompoundTag();
        lore = new ListTag();
        lore.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal("§7Total Size: §f" + company.getTotalPropertySize() + " m²"))));
        lore.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(""))));
        lore.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal("§eClick to edit"))));
        display.put("Lore", lore);
        propertyItem.getOrCreateTag().put("display", display);
        container.setItem(13, propertyItem);
        
        // Profits info (editable)
        ItemStack profitsItem = new ItemStack(Items.EMERALD);
        profitsItem.setHoverName(Component.literal("§aProfits (Last 4 Weeks)"));
        display = new CompoundTag();
        lore = new ListTag();
        int[] profits = company.getProfitsLast4Weeks();
        for (int i = 0; i < 4; i++) {
            lore.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal("§7Week " + (i + 1) + ": §f$" + profits[i]))));
        }
        lore.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(""))));
        lore.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal("§eClick to edit"))));
        display.put("Lore", lore);
        profitsItem.getOrCreateTag().put("display", display);
        container.setItem(15, profitsItem);
        
        // Add profit button
        ItemStack addProfitItem = new ItemStack(Items.GOLD_NUGGET);
        addProfitItem.setHoverName(Component.literal("§aAdd Profit"));
        display = new CompoundTag();
        lore = new ListTag();
        lore.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal("§7Add to primary account"))));
        lore.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(""))));
        lore.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal("§eClick for command"))));
        display.put("Lore", lore);
        addProfitItem.getOrCreateTag().put("display", display);
        container.setItem(20, addProfitItem);
        
        // Add bank account button
        ItemStack addBankItem = new ItemStack(Items.CHEST_MINECART);
        addBankItem.setHoverName(Component.literal("§aAdd Bank Account"));
        display = new CompoundTag();
        lore = new ListTag();
        lore.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal("§eClick for command"))));
        display.put("Lore", lore);
        addBankItem.getOrCreateTag().put("display", display);
        container.setItem(24, addBankItem);
        
        // Company value
        ItemStack valueItem = new ItemStack(Items.DIAMOND);
        valueItem.setHoverName(Component.literal("§bCompany Value"));
        display = new CompoundTag();
        lore = new ListTag();
        double companyValue = company.calculateCompanyValue();
        lore.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal("§7Total: §f" + (int) companyValue))));
        lore.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal("§7Stock Price: §f" + company.calculateStockPrice()))));
        display.put("Lore", lore);
        valueItem.getOrCreateTag().put("display", display);
        container.setItem(22, valueItem);
        
        // Bank accounts section (slots 27-35)
        int bankSlot = 27;
        List<String> bankNames = new ArrayList<>(company.getBankAccounts().keySet());
        for (int i = bankScrollOffset * 9; i < Math.min(bankNames.size(), (bankScrollOffset + 1) * 9); i++) {
            String bankName = bankNames.get(i);
            ItemStack bankItem = new ItemStack(Items.CHEST);
            bankItem.setHoverName(Component.literal("§6" + bankName));
            display = new CompoundTag();
            lore = new ListTag();
            double balance = company.getBankAccounts().getOrDefault(bankName, 0.0);
            lore.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal("§7Balance: §f" + balance))));
            if (bankName.equals(company.getPrimaryBankAccount())) {
                lore.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal("§a§lPRIMARY"))));
            }
            display.put("Lore", lore);
            bankItem.getOrCreateTag().put("display", display);
            container.setItem(bankSlot++, bankItem);
        }
        
        // Back button
        ItemStack backButton = new ItemStack(Items.ARROW);
        backButton.setHoverName(Component.literal("§eBack"));
        container.setItem(49, backButton);
        
        // Delete company
        ItemStack deleteItem = new ItemStack(Items.BARRIER);
        deleteItem.setHoverName(Component.literal("§cDelete Company"));
        container.setItem(53, deleteItem);
        
        // Fill empty slots with glass panes
        ItemStack filler = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
        filler.setHoverName(Component.literal(""));
        for (int i = 0; i < ROWS * 9; i++) {
            if (container.getItem(i).isEmpty() && i != 4 && i != 11 && i != 13 && i != 15 && i != 20 && i != 22 && i != 24
                && (i < 27 || i > 35) && i != 49 && i != 53) {
                container.setItem(i, filler);
            }
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    public static void open(ServerPlayer player, String companySymbol) {
        player.openMenu(new net.minecraft.world.SimpleMenuProvider(
                (id, playerInv, p) -> new CompanyEditMenu(id, playerInv, player, companySymbol),
                Component.literal("Company: " + companySymbol)
        ));
    }
}
