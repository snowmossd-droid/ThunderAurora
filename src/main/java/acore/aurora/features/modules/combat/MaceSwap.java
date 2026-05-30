package acore.aurora.features.modules.combat;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import acore.aurora.core.manager.client.ModuleManager;
import acore.aurora.events.impl.EventTick;
import acore.aurora.features.modules.Module;
import acore.aurora.setting.Setting;
import acore.aurora.setting.impl.BooleanSettingGroup;
import acore.aurora.setting.impl.SettingGroup;
import acore.aurora.utility.Timer;

import static acore.aurora.features.modules.Module.mc;

public class MaceSwap extends Module {

    // ── General ──────────────────────────────────────────────────────────────
    public final Setting<Mode>    mode         = new Setting<>("Mode",         Mode.Swap);
    public final Setting<Float>   delay        = new Setting<>("SwapDelay",    50f,  0f, 500f);
    public final Setting<Boolean> onlyFall     = new Setting<>("OnlyFall",     true);
    public final Setting<Float>   minHeight    = new Setting<>("MinHeight",    3f,   0f, 20f, v -> onlyFall.getValue());
    public final Setting<Boolean> autoSwapBack = new Setting<>("SwapBack",     true);
    public final Setting<Float>   swapBackDelay= new Setting<>("SwapBackDelay",200f, 0f, 1000f, v -> autoSwapBack.getValue());

    // ── Bypass ────────────────────────────────────────────────────────────────
    public final Setting<BooleanSettingGroup> bypass = new Setting<>("Bypass", new BooleanSettingGroup(true));

    /** Gửi UpdateSelectedSlot trước khi thay đổi inventory local → server thấy "hợp lệ" */
    public final Setting<Boolean> silentSwap    = new Setting<>("SilentSwap",     true).addToGroup(bypass);

    /** Dừng sprint trước swap → tránh Grim/Vulcan flag vì swap-while-sprinting */
    public final Setting<Boolean> dropSprint    = new Setting<>("StopSprint",     true).addToGroup(bypass);

    /** Gửi packet PlayerMove.OnGroundOnly trước swap → server tin rằng client đang đứng yên */
    public final Setting<Boolean> groundSpoof   = new Setting<>("GroundSpoof",    true).addToGroup(bypass);

    /** Đóng màn hình inventory trước swap để tránh flag InventoryMove */
    public final Setting<Boolean> closeInv      = new Setting<>("CloseInventory", true).addToGroup(bypass);

    /** Gửi CloseHandledScreen sau swap để flush trạng thái inventory phía server */
    public final Setting<Boolean> closeScreen   = new Setting<>("CloseScreen",    true).addToGroup(bypass);

    /** Giả mạo rotation về 0/0 lúc swap để tránh flag xoay người đột ngột */
    public final Setting<Boolean> fakeRotation  = new Setting<>("FakeRotation",   false).addToGroup(bypass);

    /** Delay ngẫu nhiên giống click người thật */
    public final Setting<Boolean> randomDelay   = new Setting<>("RandomDelay",    true).addToGroup(bypass);
    public final Setting<Integer> randomMin     = new Setting<>("RandomMin",       30, 0, 200, v -> randomDelay.getValue()).addToGroup(bypass);
    public final Setting<Integer> randomMax     = new Setting<>("RandomMax",      100, 0, 500, v -> randomDelay.getValue()).addToGroup(bypass);

    /** Thêm fake ping nhân tạo để AC không thấy swap quá nhanh */
    public final Setting<Boolean> fakeLatency   = new Setting<>("FakeLatency",    false).addToGroup(bypass);
    public final Setting<Integer> fakeDelayMs   = new Setting<>("FakeMs",          50, 0, 300, v -> fakeLatency.getValue()).addToGroup(bypass);

    /** Gửi StartSprinting lại sau swap để không bị desynced sprint */
    public final Setting<Boolean> restoreSprint = new Setting<>("RestoreSprint",   true).addToGroup(bypass);

    /** Grim-specific: gửi full position packet với onGround=true trước swap */
    public final Setting<Boolean> grimCompat    = new Setting<>("GrimCompat",      true).addToGroup(bypass);

    /** Vulcan/NCP: thêm nhỏ delay sau swap trước khi đánh để không bị "attacked before swap confirmed" */
    public final Setting<Integer> postSwapDelay = new Setting<>("PostSwapDelay",   60, 0, 300).addToGroup(bypass);

    // ── State ────────────────────────────────────────────────────────────────
    private final Timer swapTimer     = new Timer();
    private final Timer swapBackTimer = new Timer();
    private final Timer postSwapTimer = new Timer();
    private int  savedSlot  = -1;
    private boolean swapped = false;
    private boolean sprintWas = false;

    public MaceSwap() { super("MaceSwap", Category.COMBAT); }

    @EventHandler
    public void onTick(EventTick e) {
        if (mc.player == null || mc.world == null) return;

        int maceSlot = findMace();
        if (maceSlot == -1) return;

        boolean should = shouldSwap();

        if (should && !swapped) {
            long d = getDelay();
            if (!swapTimer.passedMs(d)) return;

            savedSlot = mc.player.getInventory().selectedSlot;
            sprintWas = mc.player.isSprinting();
            doSwap(maceSlot);
            swapped = true;
            swapBackTimer.reset();
            postSwapTimer.reset();
            return;
        }

        if (swapped && !should && autoSwapBack.getValue()) {
            // Đợi postSwapDelay trước khi swap về để server confirm mace attack xong
            if (!postSwapTimer.passedMs(postSwapDelay.getValue())) return;
            if (swapBackTimer.passedMs(swapBackDelay.getValue().longValue()) && savedSlot != -1) {
                doSwap(savedSlot);
                // Restore sprint nếu đã dừng
                if (restoreSprint.getValue() && sprintWas) {
                    mc.player.setSprinting(true);
                    sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
                }
                swapped = false;
                savedSlot = -1;
                swapTimer.reset();
            }
        }
    }

    // ── Core swap logic ───────────────────────────────────────────────────────
    private void doSwap(int slot) {
        ClientPlayNetworkHandler net = mc.getNetworkHandler();
        if (net == null) return;

        // 1. Đóng inventory màn hình nếu đang mở → tránh InventoryMove flag
        if (closeInv.getValue() && mc.currentScreen != null)
            mc.execute(() -> mc.setScreen(null));

        // 2. Dừng sprint → Grim/Vulcan yêu cầu không sprint khi swap
        if (dropSprint.getValue() && mc.player.isSprinting()) {
            mc.player.setSprinting(false);
            sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
        }

        // 3. Grim compat: gửi full position với onGround=true
        if (grimCompat.getValue()) {
            sendPacket(new PlayerMoveC2SPacket.Full(
                mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                fakeRotation.getValue() ? 0f : mc.player.getYaw(),
                fakeRotation.getValue() ? 0f : mc.player.getPitch(),
                true // spoof onGround
            ));
        }

        // 4. Ground spoof nhẹ (OnGroundOnly packet)
        if (groundSpoof.getValue())
            sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true));

        // 5. Thực hiện swap
        if (slot >= 9) {
            // Slot ngoài hotbar → dùng SWAP action (slot ↔ hotbar slot hiện tại)
            int hotbar = mc.player.getInventory().selectedSlot;

            // Silent: báo server slot trước
            if (silentSwap.getValue())
                sendPacket(new UpdateSelectedSlotC2SPacket(hotbar));

            mc.interactionManager.clickSlot(
                mc.player.currentScreenHandler.syncId,
                slot, hotbar,
                SlotActionType.SWAP, mc.player
            );

            if (closeScreen.getValue())
                sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));

        } else {
            // Slot trong hotbar → UpdateSelectedSlot
            if (silentSwap.getValue()) {
                // Silent: gửi packet trước rồi cập nhật local
                sendPacket(new UpdateSelectedSlotC2SPacket(slot));
                mc.player.getInventory().selectedSlot = slot;
            } else {
                mc.player.getInventory().selectedSlot = slot;
                sendPacket(new UpdateSelectedSlotC2SPacket(slot));
            }
        }

        swapTimer.reset();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private long getDelay() {
        long base = delay.getValue().longValue();
        if (randomDelay.getValue())
            base = randomMin.getValue() + (long)(Math.random() * (randomMax.getValue() - randomMin.getValue()));
        if (fakeLatency.getValue())
            base += fakeDelayMs.getValue();
        return base;
    }

    private boolean shouldSwap() {
        if (onlyFall.getValue())
            return !mc.player.isOnGround()
                && mc.player.getVelocity().y < 0
                && getFallHeight() >= minHeight.getValue();
        return ModuleManager.aura.getTarget() != null;
    }

    private float getFallHeight() {
        float y = (float) mc.player.getY();
        BlockPos.Mutable pos = new BlockPos.Mutable(
            (int) mc.player.getX(), (int) y, (int) mc.player.getZ());
        while (pos.getY() > mc.world.getBottomY() && mc.world.getBlockState(pos).isAir())
            pos.move(Direction.DOWN);
        return y - pos.getY();
    }

    private int findMace() {
        for (int i = 0; i < 9; i++)
            if (mc.player.getInventory().getStack(i).getItem() instanceof MaceItem) return i;
        for (int i = 9; i < 36; i++)
            if (mc.player.getInventory().getStack(i).getItem() instanceof MaceItem) return i;
        return -1;
    }

    @Override
    public void onDisable() {
        swapped = false;
        savedSlot = -1;
    }

    public enum Mode { Swap, Auto }
    }
    
