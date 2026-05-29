package acore.aurora.features.modules.combat;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import acore.aurora.AcoreAurora;
import acore.aurora.core.Managers;
import acore.aurora.core.manager.client.ModuleManager;
import acore.aurora.events.impl.EventTick;
import acore.aurora.events.impl.PacketEvent;
import acore.aurora.events.impl.PlayerUpdateEvent;
import acore.aurora.features.modules.Module;
import acore.aurora.setting.Setting;
import acore.aurora.setting.impl.BooleanSettingGroup;
import acore.aurora.setting.impl.SettingGroup;
import acore.aurora.utility.Timer;
import acore.aurora.utility.player.InventoryUtility;
import acore.aurora.utility.player.SearchInvResult;

import static acore.aurora.features.modules.Module.mc;

public class MaceSwap extends Module {

    public final Setting<Mode> mode        = new Setting<>("Mode",      Mode.Swap);
    public final Setting<Float> delay      = new Setting<>("SwapDelay", 50f, 0f, 500f);
    public final Setting<Boolean> onlyFall = new Setting<>("OnlyFall",  true);
    public final Setting<Float> minHeight  = new Setting<>("MinHeight", 3f, 0f, 20f, v -> onlyFall.getValue());
    public final Setting<Boolean> autoSwapBack = new Setting<>("SwapBack", true);
    public final Setting<Float> swapBackDelay  = new Setting<>("SwapBackDelay", 200f, 0f, 1000f, v -> autoSwapBack.getValue());

    public final Setting<BooleanSettingGroup> bypass = new Setting<>("Bypass", new BooleanSettingGroup(false));
    public final Setting<Boolean> silentSwap   = new Setting<>("Silent",    false).addToGroup(bypass);
    public final Setting<Boolean> noSwingAnim  = new Setting<>("NoSwing",   false).addToGroup(bypass);
    public final Setting<Boolean> fakeLatency  = new Setting<>("FakeDelay", false).addToGroup(bypass);
    public final Setting<Integer> fakeDelayMs  = new Setting<>("FakeMs",    50, 0, 300, v -> fakeLatency.getValue()).addToGroup(bypass);
    public final Setting<Boolean> randomDelay  = new Setting<>("RandomDelay", true).addToGroup(bypass);
    public final Setting<Integer> randomMin    = new Setting<>("RandomMin",  30, 0, 200, v -> randomDelay.getValue()).addToGroup(bypass);
    public final Setting<Integer> randomMax    = new Setting<>("RandomMax", 100, 0, 500, v -> randomDelay.getValue()).addToGroup(bypass);
    public final Setting<Boolean> packetSwap   = new Setting<>("Packet",    true).addToGroup(bypass);

    private final Timer swapTimer    = new Timer();
    private final Timer swapBackTimer = new Timer();
    private int savedSlot = -1;
    private boolean swapped = false;
    private int maceSlot = -1;

    public MaceSwap() { super("MaceSwap", Category.COMBAT); }

    @EventHandler
    public void onTick(EventTick e) {
        if (mc.player == null || mc.world == null) return;

        maceSlot = findMace();
        if (maceSlot == -1) return;

        boolean shouldSwap = shouldSwap();

        if (shouldSwap && !swapped) {
            long d = randomDelay.getValue()
                    ? randomMin.getValue() + (long)(Math.random() * (randomMax.getValue() - randomMin.getValue()))
                    : delay.getValue().longValue();
            if (!swapTimer.hasTimeElapsed((long)(fakeLatency.getValue() ? d + fakeDelayMs.getValue() : d))) return;

            savedSlot = mc.player.getInventory().selectedSlot;
            doSwap(maceSlot);
            swapped = true;
            swapBackTimer.reset();
        }

        if (swapped && !shouldSwap && autoSwapBack.getValue()) {
            if (swapBackTimer.hasTimeElapsed(swapBackDelay.getValue().longValue()) && savedSlot != -1) {
                doSwap(savedSlot);
                swapped = false;
                savedSlot = -1;
                swapTimer.reset();
            }
        }
    }

    private boolean shouldSwap() {
        if (onlyFall.getValue()) {
            return !mc.player.isOnGround()
                    && mc.player.getVelocity().y < 0
                    && getFallHeight() >= minHeight.getValue();
        }
        return ModuleManager.aura.getTarget() != null;
    }

    private float getFallHeight() {
        float y = (float) mc.player.getY();
        BlockPos.Mutable pos = new BlockPos.Mutable((int)mc.player.getX(), (int)y, (int)mc.player.getZ());
        while (pos.getY() > mc.world.getBottomY() && mc.world.getBlockState(pos).isAir()) pos.move(Direction.DOWN);
        return y - pos.getY();
    }

    private void doSwap(int slot) {
        if (slot >= 9) {
            if (packetSwap.getValue()) {
                mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(
                        PlayerActionC2SPacket.Action.DROP_ITEM, BlockPos.ORIGIN, Direction.DOWN));
            }
            mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId,
                    slot, mc.player.getInventory().selectedSlot,
                    net.minecraft.screen.slot.SlotActionType.SWAP, mc.player);
        } else {
            if (silentSwap.getValue()) {
                mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
                mc.player.getInventory().selectedSlot = slot;
            } else {
                mc.player.getInventory().selectedSlot = slot;
                mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
            }
        }
        swapTimer.reset();
    }

    private int findMace() {
        for (int i = 0; i < 9; i++) {
            ItemStack s = mc.player.getInventory().getStack(i);
            if (s.getItem() instanceof MaceItem) return i;
        }
        for (int i = 9; i < 36; i++) {
            ItemStack s = mc.player.getInventory().getStack(i);
            if (s.getItem() instanceof MaceItem) return i;
        }
        return -1;
    }

    public enum Mode { Swap, Auto }
}
