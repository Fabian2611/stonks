package io.fabianbuthere.stonks.ui.setup;

import io.fabianbuthere.stonks.data.JobSavedData;
import io.fabianbuthere.stonks.ui.util.MenuOpener;
import io.fabianbuthere.stonks.ui.util.UiItems;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

public class PickupBarrelMenu extends ChestMenu {
    private static final int ROWS = 3;
    private int jobIndex;
    private ServerPlayer owner;
    private Container uiContainer;

    public PickupBarrelMenu(int id, Inventory playerInv, int jobIndex) {
        this(id, playerInv, new SimpleContainer(ROWS * 9), jobIndex);
    }

    public PickupBarrelMenu(int id, Inventory playerInv, Container uiInv, int jobIndex) {
        super(MenuType.GENERIC_9x3, id, playerInv, uiInv, ROWS);
        this.uiContainer = uiInv;
        this.jobIndex = jobIndex;
        if (playerInv.player instanceof ServerPlayer sp) this.owner = sp;
        buildUi(uiInv, this.owner);
    }

    public static void open(ServerPlayer player, int jobIndex) {
        MenuOpener.open(player, (id, inv) -> new PickupBarrelMenu(id, inv, jobIndex), Component.literal("Pickup Barrel"));
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId < 0) { super.clicked(slotId, button, clickType, player); return; }
        if (slotId < ROWS * 9) { handleUiClick(slotId, player); return; }
        if (clickType == ClickType.QUICK_MOVE) return;
        super.clicked(slotId, button, clickType, player);
    }

    private void handleUiClick(int slotId, Player player) {
        if (slotId == 26) {
            if (player instanceof ServerPlayer sp) sp.closeContainer();
            return;
        }
        if (slotId == 13) {
            if (!(player instanceof ServerPlayer sp)) return;
            ServerLevel level = (ServerLevel) sp.level();
            var data = JobSavedData.get(level).getJobData();
            if (jobIndex < 0 || jobIndex >= data.activeTransports.size()) return;
            if (!data.transportKnownJobs.containsKey(jobIndex)) return;
            if (!data.transportKnownJobs.get(jobIndex).equals(sp.getUUID())) return;
            if (data.transportPickedUp.contains(jobIndex)) { sp.sendSystemMessage(Component.literal("Already picked up")); return; }

            var job = data.activeTransports.get(jobIndex);

            // prepare items and pack into shulker boxes
            java.util.List<ItemStack> toPack = new java.util.ArrayList<>();
            for (var p : job.parts()) {
                Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(p.item()));
                if (item == null) item = Items.AIR;
                int remaining = p.count();
                while (remaining > 0) {
                    int take = Math.min(remaining, item.getMaxStackSize());
                    toPack.add(new ItemStack(item, take));
                    remaining -= take;
                }
            }

            // pack into shulker boxes (27 slots each)
            int slot = 0;
            net.minecraft.world.item.Item shulkerItem = Items.SHULKER_BOX;
            // Try to find a colored shulker box in the player's inventory to use same color
            for (int i = 0; i < sp.getInventory().getContainerSize(); i++) {
                ItemStack invStack = sp.getInventory().getItem(i);
                if (invStack != null && !invStack.isEmpty()) {
                    String itemKey = ForgeRegistries.ITEMS.getKey(invStack.getItem()).toString();
                    if (itemKey.endsWith("shulker_box")) {
                        shulkerItem = invStack.getItem();
                        break;
                    }
                }
            }
            
            ItemStack currentShulker = new ItemStack(shulkerItem);
            CompoundTag curTag = new CompoundTag();
            ListTag curList = new ListTag();
            for (ItemStack s : toPack) {
                if (slot >= 27) {
                    // finish current
                    CompoundTag be = new CompoundTag();
                    be.put("Items", curList);
                    curTag.put("BlockEntityTag", be);
                    currentShulker.setTag(curTag);
                    sp.addItem(currentShulker);

                    // new shulker
                    currentShulker = new ItemStack(shulkerItem);
                    curTag = new CompoundTag();
                    curList = new ListTag();
                    slot = 0;
                }
                CompoundTag itemTag = new CompoundTag();
                s.save(itemTag);
                itemTag.putByte("Slot", (byte) slot);
                curList.add(itemTag);
                slot++;
            }
            if (slot > 0) {
                CompoundTag be = new CompoundTag();
                be.put("Items", curList);
                curTag.put("BlockEntityTag", be);
                currentShulker.setTag(curTag);
                sp.addItem(currentShulker);
            }

            data.transportPickedUp.add(jobIndex);
            JobSavedData.get(level).setDirty();
            sp.sendSystemMessage(Component.literal("Picked up items for transport job #" + jobIndex));
        }
    }

    private void buildUi(Container ui, ServerPlayer player) {
        UiItems.fill(ui, ROWS);
        if (player == null) return;
        ServerLevel level = (ServerLevel) player.level();
        var data = JobSavedData.get(level).getJobData();
        boolean canPickup = false;
        String label = "Pickup Items (pack into shulker)";
        if (jobIndex >= 0 && jobIndex < data.activeTransports.size()) {
            if (data.transportPickedUp.contains(jobIndex)) label = "Already picked up";
            else if (!data.transportKnownJobs.containsKey(jobIndex)) label = "Not assigned";
            else if (!data.transportKnownJobs.get(jobIndex).equals(player.getUUID())) label = "Not assigned to you";
            else canPickup = true;
        } else {
            label = "Invalid job";
        }
        UiItems.button(canPickup ? Items.GREEN_CONCRETE : Items.GRAY_CONCRETE, label, 13, ui);
        UiItems.button(Items.BARRIER, "Close", 26, ui);
    }

    @Override
    public boolean stillValid(Player player) { return true; }

    @Override
    public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
}
