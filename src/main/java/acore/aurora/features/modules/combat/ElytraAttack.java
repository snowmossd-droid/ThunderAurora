package acore.aurora.features.modules.combat;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import acore.aurora.core.Managers;
import acore.aurora.core.manager.client.ModuleManager;
import acore.aurora.events.impl.EventTick;
import acore.aurora.features.modules.Module;
import acore.aurora.setting.Setting;
import acore.aurora.setting.impl.BooleanSettingGroup;
import acore.aurora.utility.Timer;
import acore.aurora.utility.player.InventoryUtility;

import java.util.Comparator;
import java.util.List;

import static acore.aurora.features.modules.Module.mc;

public class ElytraAttack extends Module {

    public final Setting<Float> range        = new Setting<>("Range",       4f, 1f, 6f);
    public final Setting<Float> minHeight    = new Setting<>("MinHeight",   1f, 0f, 10f);
    public final Setting<Speed> speed        = new Setting<>("Speed",       Speed.Normal);
    public final Setting<Float> attackDelay  = new Setting<>("AttackDelay", 200f, 50f, 1000f);
    public final Setting<Boolean> maceSwitch = new Setting<>("MaceSwitch",  true);
    public final Setting<Boolean> autoFall   = new Setting<>("AutoFall",    true);
    public final Setting<Float> fallSpeed    = new Setting<>("FallSpeed",   0.5f, 0.1f, 2f, v -> autoFall.getValue());

    public final Setting<BooleanSettingGroup> bypass = new Setting<>("Bypass", new BooleanSettingGroup(false));
    public final Setting<Boolean> silentSwap  = new Setting<>("SilentSwap",  false).addToGroup(bypass);
    public final Setting<Boolean> swingCancel = new Setting<>("SwingCancel", false).addToGroup(bypass);
    public final Setting<Boolean> motionFake  = new Setting<>("FakeMotion",  true).addToGroup(bypass);
    public final Setting<Boolean> packetAtk   = new Setting<>("PacketAtk",   false).addToGroup(bypass);
    public final Setting<Boolean> randomDelay = new Setting<>("RandomDelay", true).addToGroup(bypass);
    public final Setting<Integer> randomMin   = new Setting<>("RandMin",     20, 0, 200, v -> randomDelay.getValue()).addToGroup(bypass);
    public final Setting<Integer> randomMax   = new Setting<>("RandMax",     80, 0, 500, v -> randomDelay.getValue()).addToGroup(bypass);
    public final Setting<Boolean> nofall      = new Setting<>("NoFall",      true).addToGroup(bypass);

    private final Timer attackTimer = new Timer();
    private LivingEntity target = null;
    private int savedSlot = -1;
    private boolean swapped = false;

    public ElytraAttack() { super("ElytraAttack", Category.COMBAT); }

    @EventHandler
    public void onTick(EventTick e) {
        if (mc.player == null || mc.world == null) return;
        if (!isWearingElytra() && !ModuleManager.elytraSwapArmor.isEnabled()) return;

        target = findTarget();
        if (target == null) { resetSwap(); return; }

        double fallHeight = getFallHeight();
        if (fallHeight < minHeight.getValue()) return;
        if (!mc.player.isFallFlying() && mc.player.getVelocity().y >= 0) return;

        long delay = getDelay();
        if (!attackTimer.hasTimeElapsed(delay)) return;

        if (maceSwitch.getValue()) {
            int maceSlot = findMace();
            if (maceSlot != -1 && mc.player.getInventory().getStack(mc.player.getInventory().selectedSlot).getItem() != Items.MACE) {
                savedSlot = mc.player.getInventory().selectedSlot;
                if (maceSlot < 9) {
                    if (silentSwap.getValue()) {
                        mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(maceSlot));
                        mc.player.getInventory().selectedSlot = maceSlot;
                    } else {
                        mc.player.getInventory().selectedSlot = maceSlot;
                    }
                    swapped = true;
                }
            }
        }

        if (motionFake.getValue()) {
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(
                    mc.player.getX(), mc.player.getY() - 0.05, mc.player.getZ(),
                    mc.player.getYaw(), mc.player.getPitch(), false, mc.player.horizontalCollision));
        }

        if (nofall.getValue()) {
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(
                    mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                    mc.player.getYaw(), mc.player.getPitch(), true, mc.player.horizontalCollision));
        }

        if (packetAtk.getValue()) {
            mc.player.networkHandler.sendPacket(new net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket.create(
                    target, mc.player.isSneaking()));
        } else {
            mc.interactionManager.attackEntity(mc.player, target);
        }

        if (!swingCancel.getValue()) mc.player.swingHand(Hand.MAIN_HAND);

        if (swapped && savedSlot != -1) {
            mc.player.getInventory().selectedSlot = savedSlot;
            swapped = false; savedSlot = -1;
        }

        attackTimer.reset();
    }

    private long getDelay() {
        if (randomDelay.getValue()) return randomMin.getValue() + (long)(Math.random() * (randomMax.getValue() - randomMin.getValue()));
        return switch (speed.getValue()) {
            case Fast   -> (long)(attackDelay.getValue() * 0.5f);
            case Normal -> attackDelay.getValue().longValue();
            case Slow   -> (long)(attackDelay.getValue() * 2f);
        };
    }

    private LivingEntity findTarget() {
        if (ModuleManager.aura.getTarget() != null) return ModuleManager.aura.getTarget();
        return mc.world.getEntitiesByClass(PlayerEntity.class,
                        mc.player.getBoundingBox().expand(range.getValue()),
                        p -> p != mc.player && !p.isDead() && !Managers.FRIEND.isFriend(p.getGameProfile().getName()))
                .stream().min(Comparator.comparingDouble(p -> mc.player.squaredDistanceTo(p)))
                .orElse(null);
    }

    private double getFallHeight() {
        return mc.player.getY() - mc.world.getBottomY();
    }

    private boolean isWearingElytra() {
        return mc.player.getInventory().armor.get(2).getItem() instanceof ElytraItem
                && mc.player.isFallFlying();
    }

    private int findMace() {
        for (int i = 0; i < 9; i++)
            if (mc.player.getInventory().getStack(i).getItem() instanceof MaceItem) return i;
        return -1;
    }

    private void resetSwap() {
        if (swapped && savedSlot != -1) {
            mc.player.getInventory().selectedSlot = savedSlot;
            swapped = false; savedSlot = -1;
        }
    }

    public enum Speed { Fast, Normal, Slow }
}
