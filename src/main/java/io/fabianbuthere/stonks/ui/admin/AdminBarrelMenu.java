package io.fabianbuthere.stonks.ui.admin;

import io.fabianbuthere.stonks.api.BarrelMode;
import io.fabianbuthere.stonks.data.JobSavedData;
import io.fabianbuthere.stonks.ui.util.MenuOpener;
import net.minecraft.core.BlockPos;
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

public class AdminBarrelMenu extends ChestMenu {
    private static final int ROWS = 3;
    private final BlockPos barrelPos;
    private ServerPlayer owner;
    private Container uiContainer;

    public AdminBarrelMenu(int id, Inventory playerInv, BlockPos barrelPos) {
        this(id, playerInv, new SimpleContainer(ROWS * 9), barrelPos);
    }

    public AdminBarrelMenu(int id, Inventory playerInv, Container uiInv, BlockPos barrelPos) {
        super(MenuType.GENERIC_9x3, id, playerInv, uiInv, ROWS);
        this.uiContainer = uiInv;
        this.barrelPos = barrelPos;
        if (playerInv.player instanceof ServerPlayer sp) this.owner = sp;
        buildUi(uiInv, this.owner);
    }

    public static void open(ServerPlayer player, BlockPos barrelPos) {
        MenuOpener.open(player, (id, inv) -> new AdminBarrelMenu(id, inv, barrelPos), 
                       Component.literal("§6§lBarrel Configuration"));
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId < 0) { super.clicked(slotId, button, clickType, player); return; }
        if (slotId < ROWS * 9) { handleUiClick(slotId, player); return; }
        if (clickType == ClickType.QUICK_MOVE) return;
        super.clicked(slotId, button, clickType, player);
    }

    private void handleUiClick(int slot, Player player) {
        if (!(player instanceof ServerPlayer sp)) return;
        
        ItemStack clicked = uiContainer.getItem(slot);
        if (clicked.isEmpty()) return;

        String action = clicked.getOrCreateTag().getString("stonks_action");
        String mode = clicked.getOrCreateTag().getString("stonks_mode");

        JobSavedData data = JobSavedData.get(sp.serverLevel());

        if ("remove_barrel".equals(action)) {
            // Remove barrel from all lists
            data.jobData.deliveryLocations.remove(barrelPos);
            data.jobData.transportLocations.remove(barrelPos);
            data.jobData.barrelModes.remove(barrelPos);
            
            // Remove the physical barrel
            sp.serverLevel().destroyBlock(barrelPos, false);
            
            sp.closeContainer();
            sp.sendSystemMessage(Component.literal("§cBarrel removed"));
            data.setDirty();
        } else if (!mode.isEmpty()) {
            // Change mode
            BarrelMode newMode = BarrelMode.fromSerializedName(mode);
            BarrelMode oldMode = data.jobData.barrelModes.getOrDefault(barrelPos, BarrelMode.ONLY_DELIVERY);
            
            if (newMode != oldMode) {
                // Remove from old lists
                if (oldMode.allowsDelivery()) {
                    data.jobData.deliveryLocations.remove(barrelPos);
                }
                if (oldMode.allowsTransportDropoff()) {
                    data.jobData.transportLocations.remove(barrelPos);
                }
                if (oldMode.allowsTransportPickup()) {
                    data.jobData.transportLocations.remove(barrelPos);
                }

                // Add to new lists
                if (newMode.allowsDelivery() && !data.jobData.deliveryLocations.contains(barrelPos)) {
                    data.jobData.deliveryLocations.add(barrelPos);
                }
                if (newMode.allowsTransportDropoff() && !data.jobData.transportLocations.contains(barrelPos)) {
                    data.jobData.transportLocations.add(barrelPos);
                }
                if (newMode.allowsTransportPickup() && !data.jobData.transportLocations.contains(barrelPos)) {
                    data.jobData.transportLocations.add(barrelPos);
                }

                data.jobData.barrelModes.put(barrelPos, newMode);
                data.setDirty();
                
                sp.sendSystemMessage(Component.literal("§aBarrel mode changed to: " + newMode.getDisplayName()));
                buildUi(uiContainer, sp);
            }
        }
    }

    private void buildUi(Container container, ServerPlayer player) {
        if (player == null) return;
        
        JobSavedData data = JobSavedData.get(player.serverLevel());
        BarrelMode currentMode = data.jobData.barrelModes.getOrDefault(barrelPos, BarrelMode.ONLY_DELIVERY);

        // Clear inventory
        for (int i = 0; i < 27; i++) {
            container.setItem(i, ItemStack.EMPTY);
        }

        // Glass pane filler
        ItemStack filler = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
        filler.setHoverName(Component.literal(" "));
        for (int i = 0; i < 27; i++) {
            if (i != 10 && i != 12 && i != 14 && i != 16 && i != 22) {
                container.setItem(i, filler);
            }
        }

        // Mode buttons
        addModeButton(container, 10, BarrelMode.ONLY_DELIVERY, currentMode);
        addModeButton(container, 12, BarrelMode.ONLY_TRANSPORT_DROPOFF, currentMode);
        addModeButton(container, 14, BarrelMode.BOTH, currentMode);
        addModeButton(container, 16, BarrelMode.TRANSPORT_PICKUP, currentMode);

        // Remove barrel button
        ItemStack removeButton = new ItemStack(Items.BARRIER);
        removeButton.setHoverName(Component.literal("§c§lRemove Barrel"));
        removeButton.getOrCreateTag().putString("stonks_action", "remove_barrel");
        container.setItem(22, removeButton);
    }

    private void addModeButton(Container container, int slot, BarrelMode mode, BarrelMode currentMode) {
        ItemStack button;
        boolean isCurrent = mode == currentMode;

        switch (mode) {
            case ONLY_DELIVERY:
                button = new ItemStack(isCurrent ? Items.LIME_WOOL : Items.WHITE_WOOL);
                button.setHoverName(Component.literal((isCurrent ? "§a§l" : "§7") + "Delivery Only"));
                break;
            case ONLY_TRANSPORT_DROPOFF:
                button = new ItemStack(isCurrent ? Items.LIME_WOOL : Items.LIGHT_BLUE_WOOL);
                button.setHoverName(Component.literal((isCurrent ? "§a§l" : "§7") + "Transport Dropoff"));
                break;
            case BOTH:
                button = new ItemStack(isCurrent ? Items.LIME_WOOL : Items.YELLOW_WOOL);
                button.setHoverName(Component.literal((isCurrent ? "§a§l" : "§7") + "Both Delivery & Dropoff"));
                break;
            case TRANSPORT_PICKUP:
                button = new ItemStack(isCurrent ? Items.LIME_WOOL : Items.ORANGE_WOOL);
                button.setHoverName(Component.literal((isCurrent ? "§a§l" : "§7") + "Transport Pickup"));
                break;
            default:
                button = new ItemStack(Items.BARRIER);
        }

        button.getOrCreateTag().putString("stonks_mode", mode.getSerializedName());
        container.setItem(slot, button);
    }
}
