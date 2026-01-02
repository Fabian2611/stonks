package io.fabianbuthere.stonks.ui.setup;

import io.fabianbuthere.stonks.api.DeliveryJob;
import io.fabianbuthere.stonks.api.TransportJob;
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

public class JobMenu extends ChestMenu {
    private static final int ROWS = 6;
    private ServerPlayer owner;
    private Container uiContainer;

    private enum Category { DELIVERY, TRANSPORT }
    private Category currentCategory = Category.DELIVERY;

    public JobMenu(int id, Inventory playerInv, Player requester) {
        this(id, playerInv, new SimpleContainer(ROWS * 9));
        if (requester instanceof ServerPlayer sp) this.owner = sp;
        if (this.uiContainer != null) buildUi(this.uiContainer, this.owner);
    }

    public JobMenu(int id, Inventory playerInv, Container uiInv) {
        super(MenuType.GENERIC_9x6, id, playerInv, uiInv, ROWS);
        this.uiContainer = uiInv;
        buildUi(uiInv, this.owner);
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
        ServerLevel level = (ServerLevel) sp.level();
        var jobData = JobSavedData.get(level).getJobData();

        // Tab buttons (row 0): 2 = Delivery, 4 = Transport, 6 = Locations
        if (slotId == 2) {
            currentCategory = Category.DELIVERY;
            buildUi(this.uiContainer, this.owner);
            return;
        } else if (slotId == 4) {
            currentCategory = Category.TRANSPORT;
            buildUi(this.uiContainer, this.owner);
            return;
        } else if (slotId == 6) {
            LocationsMenu.open(sp);
            return;
        }

        // Jobs displayed in rows 1-5 (slots 9-53)
        int jobSlotStart = 9;
        int jobSlotEnd = 53;
        
        if (slotId >= jobSlotStart && slotId <= jobSlotEnd) {
            int idx = slotId - jobSlotStart;
            
            if (currentCategory == Category.DELIVERY) {
                if (idx < jobData.activeJobs.size()) {
                    // Right-click to delete
                    if (button == 1) {
                        boolean ok = JobSavedData.get(level).deleteDeliveryJob(idx);
                        if (ok) sp.sendSystemMessage(Component.literal("§cDeleted delivery job #" + idx));
                        else sp.sendSystemMessage(Component.literal("§cFailed to delete delivery job #" + idx));
                        buildUi(this.uiContainer, this.owner);
                    }
                }
            } else if (currentCategory == Category.TRANSPORT) {
                if (idx < jobData.activeTransports.size()) {
                    // Right-click to delete
                    if (button == 1) {
                        boolean ok = JobSavedData.get(level).deleteTransportJob(idx);
                        if (ok) sp.sendSystemMessage(Component.literal("§cDeleted transport job #" + idx));
                        else sp.sendSystemMessage(Component.literal("§cFailed to delete transport job #" + idx));
                        buildUi(this.uiContainer, this.owner);
                    }
                }
            }
        }
        
        if (slotId == 53) {
            player.closeContainer();
        }
    }

    private void buildUi(Container ui, ServerPlayer player) {
        UiItems.fill(ui, ROWS);
        if (player == null) return;
        ServerLevel level = (ServerLevel) player.level();
        var jobData = JobSavedData.get(level).getJobData();

        // Row 0: Category tabs
        UiItems.button(Items.PAPER, currentCategory == Category.DELIVERY ? "§e▶ Deliveries" : "§7Deliveries", 2, ui);
        UiItems.button(Items.CHEST_MINECART, currentCategory == Category.TRANSPORT ? "§e▶ Transports" : "§7Transports", 4, ui);
        UiItems.button(Items.COMPASS, "§7Manage Locations", 6, ui);

        // Rows 1-5: Jobs (45 slots)
        int jobSlotStart = 9;
        
        if (currentCategory == Category.DELIVERY) {
            for (int i = 0; i < jobData.activeJobs.size() && i < 45; i++) {
                DeliveryJob job = jobData.activeJobs.get(i);
                ItemStack stack;
                String locText = "?";
                try { locText = jobData.deliveryLocations.get(job.locationIndex()).toShortString(); } catch (Exception ignored) {}
                
                boolean isMine = jobData.knownJobs.containsKey(i) && jobData.knownJobs.get(i).equals(player.getUUID());
                boolean isTaken = jobData.knownJobs.containsKey(i);
                
                if (isMine) {
                    stack = new ItemStack(Items.LIME_CONCRETE);
                } else if (isTaken) {
                    stack = new ItemStack(Items.RED_CONCRETE);
                } else {
                    stack = new ItemStack(Items.PAPER);
                }
                
                String status = isTaken ? (isMine ? "§a[YOU]" : "§c[TAKEN]") : "§7[AVAILABLE]";
                stack.setHoverName(Component.literal("§6#" + i + " " + status + " §f" + job.summary() + " §7@ " + locText + " §e$" + job.payment() + " §c[Right-click to delete]"));
                ui.setItem(jobSlotStart + i, stack);
            }
        } else {
            for (int i = 0; i < jobData.activeTransports.size() && i < 45; i++) {
                TransportJob job = jobData.activeTransports.get(i);
                ItemStack stack;
                String fromText = "?";
                String toText = "?";
                try {
                    fromText = jobData.transportLocations.get(job.fromIndex()).toShortString();
                    toText = jobData.transportLocations.get(job.toIndex()).toShortString();
                } catch (Exception ignored) {}
                
                boolean isMine = jobData.transportKnownJobs.containsKey(i) && jobData.transportKnownJobs.get(i).equals(player.getUUID());
                boolean isTaken = jobData.transportKnownJobs.containsKey(i);
                boolean pickedUp = jobData.transportPickedUp.contains(i);
                
                if (isMine) {
                    stack = new ItemStack(pickedUp ? Items.LIME_CONCRETE : Items.YELLOW_CONCRETE);
                } else if (isTaken) {
                    stack = new ItemStack(Items.RED_CONCRETE);
                } else {
                    stack = new ItemStack(Items.CHEST);
                }
                
                String status = isTaken ? (isMine ? (pickedUp ? "§a[YOU-PICKED]" : "§e[YOU]") : "§c[TAKEN]") : "§7[AVAILABLE]";
                stack.setHoverName(Component.literal("§6#" + i + " " + status + " §f" + job.summary() + " §7" + fromText + " → " + toText + " §e$" + job.payment() + " §c[Right-click to delete]"));
                ui.setItem(jobSlotStart + i, stack);
            }
        }

        // Bottom right: close button
        UiItems.button(Items.BARRIER, "Close", 53, ui);
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
        MenuOpener.open(player, (id, inv, p) -> new JobMenu(id, inv, p), Component.literal("Job Management (Admin)"));
    }
}
