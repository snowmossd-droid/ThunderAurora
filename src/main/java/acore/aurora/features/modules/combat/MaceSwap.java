package acore.aurora.features.modules.combat;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.MaceItem;
import net.minecraft.item.SwordItem;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import acore.aurora.core.manager.client.ModuleManager;
import acore.aurora.events.impl.EventTick;
import acore.aurora.features.modules.Module;
import acore.aurora.setting.Setting;
import acore.aurora.utility.Timer;

import static acore.aurora.features.modules.Module.mc;

public class MaceSwap extends Module {

    public final Setting<Integer> toMaceDelay    = new Setting<>("ToMaceDelay",    50,  0, 500);
    public final Setting<Integer> toSwordDelay   = new Setting<>("ToSwordDelay",   200, 0, 1000);
    public final Setting<Boolean> attackWithMace = new Setting<>("AttackWithMace", true);
    public final Setting<Boolean> onlyFalling    = new Setting<>("OnlyFalling",    false);
    public final Setting<Float>   minFall        = new Setting<>("MinFall",        3f, 0f, 20f, v -> onlyFalling.getValue());

    private final Timer toMaceTimer  = new Timer();
    private final Timer toSwordTimer = new Timer();

    private int savedSlot = -1;
    private boolean swapped = false;
    private boolean attackedWithMace = false;

    public MaceSwap() {
        super("MaceSwap", Category.COMBAT);
    }

    @EventHandler
    public void onTick(EventTick e) {
        if (mc.player == null || mc.world == null) return;

        boolean hasTarget = ModuleManager.aura.getTarget() != null;

        if (!hasTarget) {
            restoreIfNeeded();
            return;
        }

        if (onlyFalling.getValue()) {
            boolean validFall = !mc.player.isOnGround()
                    && mc.player.getVelocity().y < 0
                    && mc.player.fallDistance >= minFall.getValue();
            if (!validFall) {
                restoreIfNeeded();
                return;
            }
        }

        if (!swapped) {
            if (!toMaceTimer.passedMs(toMaceDelay.getValue())) return;

            int maceSlot = findHotbar(true);
            if (maceSlot == -1) return;

            savedSlot = mc.player.getInventory().selectedSlot;
            silentSwap(maceSlot);
            swapped = true;
            attackedWithMace = false;
            toSwordTimer.reset();

            if (attackWithMace.getValue()) {
                mc.interactionManager.attackEntity(mc.player, ModuleManager.aura.getTarget());
                mc.player.swingHand(Hand.MAIN_HAND);
                attackedWithMace = true;
            }
        }

        if (swapped && toSwordTimer.passedMs(toSwordDelay.getValue())) {
            int swordSlot = findHotbar(false);
            silentSwap(swordSlot != -1 ? swordSlot : savedSlot);
            swapped = false;
            savedSlot = -1;
            attackedWithMace = false;
            toMaceTimer.reset();
        }
    }

    private void restoreIfNeeded() {
        if (swapped && toSwordTimer.passedMs(toSwordDelay.getValue())) {
            int swordSlot = findHotbar(false);
            silentSwap(swordSlot != -1 ? swordSlot : savedSlot);
            swapped = false;
            savedSlot = -1;
            toMaceTimer.reset();
        }
    }

    private void silentSwap(int slot) {
        if (slot < 0 || slot > 8 || mc.player == null) return;
        mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
        mc.player.getInventory().selectedSlot = slot;
    }

    private int findHotbar(boolean mace) {
        for (int i = 0; i < 9; i++) {
            var item = mc.player.getInventory().getStack(i).getItem();
            if (mace && item instanceof MaceItem) return i;
            if (!mace && item instanceof SwordItem) return i;
        }
        return -1;
    }

    @Override
    public void onDisable() {
        if (swapped && savedSlot != -1 && mc.player != null) {
            silentSwap(savedSlot);
        }
        swapped = false;
        savedSlot = -1;
        attackedWithMace = false;
    }
            }
