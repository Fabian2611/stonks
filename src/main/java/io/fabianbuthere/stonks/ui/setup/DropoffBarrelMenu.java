package io.fabianbuthere.stonks.ui.setup;

import io.fabianbuthere.stonks.data.JobSavedData;
import io.fabianbuthere.stonks.ui.util.MenuOpener;
import io.fabianbuthere.stonks.ui.util.UiItems;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

public class DropoffBarrelMenu extends ChestMenu {
    private static final int ROWS = 3;
    private int jobIndex;
    private ServerPlayer owner;
    private Container uiContainer;

    public DropoffBarrelMenu(int id, Inventory playerInv, int jobIndex) {
        this(id, playerInv, new SimpleContainer(ROWS * 9), jobIndex);
    }

    public DropoffBarrelMenu(int id, Inventory playerInv, Container uiInv, int jobIndex) {
        super(MenuType.GENERIC_9x3, id, playerInv, uiInv, ROWS);
        this.uiContainer = uiInv;
        this.jobIndex = jobIndex;
        if (playerInv.player instanceof ServerPlayer sp) this.owner = sp;
        buildUi(uiInv, this.owner);
    }

    public static void open(ServerPlayer player, int jobIndex) {
        MenuOpener.open(player, (id, inv) -> new DropoffBarrelMenu(id, inv, jobIndex), Component.literal("Dropoff Barrel"));
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId < 0) { super.clicked(slotId, button, clickType, player); return; }
        if (slotId < ROWS * 9) {
            if (slotId >= 0 && slotId <= 8) {
                if (!(player instanceof ServerPlayer sp)) return;
                ServerLevel level = (ServerLevel) sp.level();
                var data = JobSavedData.get(level).getJobData();
                boolean canInput = false;
                if (jobIndex >= 0 && jobIndex < data.activeTransports.size()) {
                    canInput = data.transportKnownJobs.containsKey(jobIndex) 
                        && data.transportKnownJobs.get(jobIndex).equals(sp.getUUID())
                        && data.transportPickedUp.contains(jobIndex);
                }
                if (canInput) { super.clicked(slotId, button, clickType, player); }
                else { sp.sendSystemMessage(Component.literal("You cannot put items here")); }
                return;
            }
            handleUiClick(slotId, player);
            return;
        }
        if (clickType == ClickType.QUICK_MOVE) return;
        super.clicked(slotId, button, clickType, player);
    }

    private void handleUiClick(int slotId, Player player) {
        if (!(player instanceof ServerPlayer sp)) return;
        if (slotId == 26) {
            sp.closeContainer();
            return;
        }
        if (slotId == 22) {
            ServerLevel level = (ServerLevel) sp.level();
            var data = JobSavedData.get(level).getJobData();
            if (jobIndex < 0 || jobIndex >= data.activeTransports.size()) return;
            if (!data.transportKnownJobs.containsKey(jobIndex)) return;
            if (!data.transportKnownJobs.get(jobIndex).equals(sp.getUUID())) return;
            if (!data.transportPickedUp.contains(jobIndex)) return;

            var job = data.activeTransports.get(jobIndex);
            java.util.Map<String, Integer> agg = new java.util.HashMap<>();
            for (int i = 0; i < 9; i++) {
                ItemStack st = uiContainer.getItem(i);
                if (st == null || st.isEmpty()) continue;
                String itemId = ForgeRegistries.ITEMS.getKey(st.getItem()).toString();
                if (itemId.endsWith("shulker_box")) {
                    CompoundTag tag = st.getTag();
                    if (tag != null && tag.contains("BlockEntityTag")) {
                        CompoundTag be = tag.getCompound("BlockEntityTag");
                        ListTag items = be.getList("Items", 10);
                        for (int j = 0; j < items.size(); j++) {
                            CompoundTag it = items.getCompound(j);
                            ItemStack inner = ItemStack.of(it);
                            String key = ForgeRegistries.ITEMS.getKey(inner.getItem()).toString();
                            agg.put(key, agg.getOrDefault(key, 0) + inner.getCount());
                        }
                    }
                } else {
                    agg.put(itemId, agg.getOrDefault(itemId, 0) + st.getCount());
                }
            }

            boolean ok = true;
            for (var p : job.parts()) {
                int have = agg.getOrDefault(p.item(), 0);
                if (have < p.count()) { ok = false; break; }
            }

            if (!ok) { sp.sendSystemMessage(Component.literal("Transport incomplete")); return; }

            java.util.Map<String, Integer> remaining = new java.util.HashMap<>();
            for (var p : job.parts()) remaining.put(p.item(), p.count());

            for (int i = 0; i < 9; i++) {
                ItemStack st = uiContainer.getItem(i);
                if (st == null || st.isEmpty()) continue;
                String itemId = ForgeRegistries.ITEMS.getKey(st.getItem()).toString();
                if (itemId.endsWith("shulker_box")) {
                    CompoundTag tag = st.getTag();
                    if (tag != null && tag.contains("BlockEntityTag")) {
                        CompoundTag be = tag.getCompound("BlockEntityTag");
                        ListTag items = be.getList("Items", 10);
                        ListTag leftover = new ListTag();
                        int outSlot = 0;
                        for (int j = 0; j < items.size(); j++) {
                            CompoundTag it = items.getCompound(j);
                            ItemStack inner = ItemStack.of(it);
                            String key = ForgeRegistries.ITEMS.getKey(inner.getItem()).toString();
                            int need = remaining.getOrDefault(key, 0);
                            if (need > 0) {
                                int take = Math.min(need, inner.getCount());
                                inner.setCount(inner.getCount() - take);
                                need -= take;
                                remaining.put(key, need);
                            }
                            if (inner.getCount() > 0) {
                                CompoundTag nt = new CompoundTag();
                                inner.save(nt);
                                nt.putByte("Slot", (byte) outSlot);
                                leftover.add(nt);
                                outSlot++;
                            }
                        }
                        if (leftover.size() > 0) {
                            ItemStack ret = st.copy();
                            CompoundTag t = ret.getOrCreateTag();
                            CompoundTag be2 = new CompoundTag();
                            be2.put("Items", leftover);
                            t.put("BlockEntityTag", be2);
                            ret.setTag(t);
                            sp.addItem(ret);
                        }
                    }
                } else {
                    String key = itemId;
                    int need = remaining.getOrDefault(key, 0);
                    if (need > 0) {
                        int take = Math.min(need, st.getCount());
                        int leftover = st.getCount() - take;
                        remaining.put(key, need - take);
                        if (leftover > 0) {
                            ItemStack ret = new ItemStack(st.getItem(), leftover);
                            sp.addItem(ret);
                        }
                    } else {
                        sp.addItem(st.copy());
                    }
                }
                uiContainer.setItem(i, ItemStack.EMPTY);
            }

            JobSavedData.get(level).deleteTransportJob(jobIndex);
            JobSavedData.get(level).setDirty();
            
            // Award payment (convert dollars to cents)
            int paymentInCents = job.payment() * 100;
            io.fabianbuthere.stonks.api.util.PaymentUtil.awardPayment(sp, paymentInCents);
            
            sp.sendSystemMessage(Component.literal("§aCompleted transport job #" + jobIndex + " — " + 
                io.fabianbuthere.stonks.api.util.PaymentUtil.formatPayment(paymentInCents)));
        }
    }

    private void buildUi(Container ui, ServerPlayer player) {
        UiItems.fill(ui, ROWS);
        if (player == null) return;
        ServerLevel level = (ServerLevel) player.level();
        var data = JobSavedData.get(level).getJobData();
        boolean canConfirm = false;
        String label = "Confirm Dropoff";
        if (jobIndex >= 0 && jobIndex < data.activeTransports.size()) {
            if (!data.transportKnownJobs.containsKey(jobIndex)) label = "Not assigned";
            else if (!data.transportKnownJobs.get(jobIndex).equals(player.getUUID())) label = "Not assigned to you";
            else if (!data.transportPickedUp.contains(jobIndex)) label = "Items not picked up";
            else canConfirm = true;
        } else {
            label = "No current job";
        }

        if (canConfirm) {
            for (int i = 0; i < 9; i++) ui.setItem(i, ItemStack.EMPTY);
        } else {
            for (int i = 0; i < 9; i++) ui.setItem(i, UiItems.filler());
            UiItems.button(Items.PAPER, label, 4, ui);
        }

        UiItems.button(canConfirm ? Items.GREEN_CONCRETE : Items.GRAY_CONCRETE, label, 22, ui);
        UiItems.button(Items.BARRIER, "Close", 26, ui);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide && player instanceof ServerPlayer sp) {
            ServerLevel level = (ServerLevel) sp.level();
            var data = JobSavedData.get(level).getJobData();
            boolean canInput = false;
            if (jobIndex >= 0 && jobIndex < data.activeTransports.size()) {
                canInput = data.transportKnownJobs.containsKey(jobIndex) 
                    && data.transportKnownJobs.get(jobIndex).equals(sp.getUUID())
                    && data.transportPickedUp.contains(jobIndex);
            }
            
            if (canInput) {
                for (int i = 0; i < 9; i++) {
                    ItemStack stack = uiContainer.getItem(i);
                    if (stack != null && !stack.isEmpty()) {
                        sp.addItem(stack.copy());
                        uiContainer.setItem(i, ItemStack.EMPTY);
                    }
                }
            }
        }
    }

    @Override
    public boolean stillValid(Player player) { return true; }

    @Override
    public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
}
