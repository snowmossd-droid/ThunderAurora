package acore.aurora.features.modules.combat;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import acore.aurora.core.InputBlocker;
import acore.aurora.events.impl.EventSync;
import acore.aurora.features.modules.Module;
import acore.aurora.setting.Setting;
import acore.aurora.setting.impl.Bind;
import acore.aurora.utility.Timer;
import acore.aurora.utility.player.InteractionUtility;
import acore.aurora.utility.player.InventoryUtility;
import acore.aurora.utility.player.SearchInvResult;

public class AutoAnchor extends Module {
    private final Setting<Bind> placeKey = new Setting<>("Keybind", new Bind(-1, false, false));
    private final Setting<Boolean> lock = new Setting<>("Lock", false);
    private final Setting<Integer> charges = new Setting<>("Charges", 1, 1, 4);
    private final Setting<Integer> delay = new Setting<>("Delay", 2, 0, 20);
    private final Setting<Float> range = new Setting<>("Range", 5f, 1f, 7f);
    private final Setting<Boolean> returnSlot = new Setting<>("ReturnSlot", true);
    private final Setting<Boolean> swing = new Setting<>("Swing", true);
    private final Setting<Boolean> attackToBreak = new Setting<>("AttackToBreak", true);
    private final Setting<Boolean> shield = new Setting<>("Shield", true);

    private final Timer actionTimer = new Timer();
    private State state = State.IDLE;
    private BlockPos anchorPos;
    private Direction lockDirection;
    private int chargesDone;
    private int prevSlot;
    private boolean keyWasDown;

    private static final String SWITCH_BLOCK_OWNER = "autoanchor_switch";
    private static final long LEGIT_SWITCH_DELAY_MS = 5L;
    private boolean switchPending;
    private int switchPendingSlot = -1;
    private long switchPendingAt = -1L;

    public AutoAnchor() {
        super("AutoAnchor", Category.COMBAT);
    }

    @Override
    public void onDisable() {
        reset();
    }

    @EventHandler
    public void onSync(EventSync event) {
        if (fullNullCheck()) return;

        boolean keyDown = isKeyPressed(placeKey);
        boolean shouldStart = lock.getValue() ? keyDown : (keyDown && !keyWasDown);
        if (shouldStart && state == State.IDLE) start();
        keyWasDown = keyDown;

        if (state == State.IDLE) return;
        if (!actionTimer.passedMs(delay.getValue() * 50L)) return;

        if (state == State.PLACING) doPlace();
        else if (state == State.CHARGING) doCharge();
        else if (state == State.SHIELDING) doShield();
        else if (state == State.ATTACKING) doAttack();
    }

    private boolean ensureSlot(int slot) {
        if (mc.player == null) return false;
        if (mc.player.getInventory().selectedSlot == slot) return true;

        if (!switchPending) {
            InputBlocker.block(SWITCH_BLOCK_OWNER);
            switchPending = true;
            switchPendingSlot = slot;
            switchPendingAt = System.currentTimeMillis() + LEGIT_SWITCH_DELAY_MS;
            return false;
        }

        if (switchPendingSlot == slot) {
            if (System.currentTimeMillis() >= switchPendingAt) {
                InventoryUtility.switchTo(slot);
                clearSwitchPending();
                return true;
            }
            return false;
        }

        clearSwitchPending();
        return false;
    }

    private void clearSwitchPending() {
        switchPending = false;
        switchPendingSlot = -1;
        switchPendingAt = -1L;
        InputBlocker.unblock(SWITCH_BLOCK_OWNER);
    }

    private void start() {
        SearchInvResult anchorResult = InventoryUtility.findBlockInHotBar(Blocks.RESPAWN_ANCHOR);
        if (!anchorResult.found()) {
            sendMessage("Khong tim thay neo hoi sinh trong hotbar!");
            return;
        }

        prevSlot = mc.player.getInventory().selectedSlot;
        chargesDone = 0;
        anchorPos = null;
        state = State.PLACING;
        actionTimer.reset();
    }

    private void doPlace() {
        SearchInvResult anchorResult = InventoryUtility.findBlockInHotBar(Blocks.RESPAWN_ANCHOR);
        if (!anchorResult.found()) {
            sendMessage("Het neo hoi sinh!");
            finish();
            return;
        }

        BlockHitResult lookHit = rayTraceLookBlock();
        if (lookHit == null || lookHit.getType() != HitResult.Type.BLOCK) {
            finish();
            return;
        }

        BlockPos targetPos = lookHit.getBlockPos().offset(lookHit.getSide());
        if (mc.world == null || !mc.world.getBlockState(targetPos).isReplaceable()) {
            finish();
            return;
        }

        if (!ensureSlot(anchorResult.slot())) return;

        boolean placed = InteractionUtility.placeBlock(
                targetPos,
                InteractionUtility.Rotate.None,
                InteractionUtility.Interact.Strict,
                InteractionUtility.PlaceMode.Normal,
                anchorResult,
                false,
                false
        );

        if (placed) {
            anchorPos = targetPos;
            state = State.CHARGING;
        } else {
            finish();
        }

        actionTimer.reset();
    }

    private void doCharge() {
        if (mc.world == null
                || anchorPos == null
                || chargesDone >= charges.getValue()
                || mc.world.getBlockState(anchorPos).getBlock() != Blocks.RESPAWN_ANCHOR) {
            finish();
            return;
        }

        SearchInvResult glowResult = InventoryUtility.findItemInHotBar(Items.GLOWSTONE);
        if (!glowResult.found()) {
            sendMessage("Het da phat sang!");
            finish();
            return;
        }

        if (!ensureSlot(glowResult.slot())) return;

        Vec3d hitPos = anchorPos.toCenterPos().add(0, 0.5, 0);
        BlockHitResult chargeHit = new BlockHitResult(hitPos, Direction.UP, anchorPos, false);

        float[] angle = InteractionUtility.calculateAngle(hitPos);
        mc.player.setYaw(angle[0]);
        mc.player.setPitch(angle[1]);

        if (mc.interactionManager != null)
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, chargeHit);

        if (swing.getValue())
            mc.player.swingHand(Hand.MAIN_HAND);

        chargesDone++;

        if (chargesDone >= charges.getValue()) {
            if (shield.getValue()) {
                state = State.SHIELDING;
            } else if (attackToBreak.getValue()) {
                state = State.ATTACKING;
            } else {
                finish();
            }
        }

        actionTimer.reset();
    }

    private void doShield() {
        if (mc.world == null || mc.player == null || anchorPos == null) {
            finish();
            return;
        }

        SearchInvResult glowResult = InventoryUtility.findItemInHotBar(Items.GLOWSTONE);
        if (!glowResult.found()) {
            sendMessage("Het da phat sang de lam tam chan!");
            state = attackToBreak.getValue() ? State.ATTACKING : State.IDLE;
            if (state == State.IDLE) finish();
            actionTimer.reset();
            return;
        }

        Vec3d toPlayer = mc.player.getPos().subtract(anchorPos.toCenterPos());
        Direction shieldDir = Direction.getFacing(toPlayer.x, toPlayer.y, toPlayer.z);
        BlockPos shieldPos = anchorPos.offset(shieldDir);

        if (!ensureSlot(glowResult.slot())) return;

        if (mc.world.getBlockState(shieldPos).isReplaceable()) {
            InteractionUtility.placeBlock(
                    shieldPos,
                    InteractionUtility.Rotate.None,
                    InteractionUtility.Interact.Vanilla,
                    InteractionUtility.PlaceMode.Normal,
                    glowResult,
                    false,
                    false
            );
        }

        state = attackToBreak.getValue() ? State.ATTACKING : State.IDLE;
        if (state == State.IDLE) finish();
        actionTimer.reset();
    }

    private void doAttack() {
        if (mc.world == null
                || anchorPos == null
                || mc.world.getBlockState(anchorPos).getBlock() != Blocks.RESPAWN_ANCHOR
                || mc.interactionManager == null) {
            finish();
            return;
        }

        Vec3d hitPos = anchorPos.toCenterPos().add(0, 0.5, 0);
        BlockHitResult attackHit = new BlockHitResult(hitPos, Direction.UP, anchorPos, false);

        float[] angle = InteractionUtility.calculateAngle(hitPos);
        mc.player.setYaw(angle[0]);
        mc.player.setPitch(angle[1]);

        mc.interactionManager.attackBlock(anchorPos, attackHit.getSide());

        if (swing.getValue())
            mc.player.swingHand(Hand.MAIN_HAND);

        finish();
    }

    private BlockHitResult rayTraceLookBlock() {
        if (mc.player == null || mc.world == null) return null;

        Vec3d eyePos = mc.player.getEyePos();
        Vec3d lookVec = mc.player.getRotationVec(1.0f);
        Vec3d endPos = eyePos.add(lookVec.multiply(range.getValue()));

        return mc.world.raycast(new RaycastContext(
                eyePos,
                endPos,
                RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.NONE,
                mc.player
        ));
    }

    private void finish() {
        clearSwitchPending();
        if (returnSlot.getValue() && mc.player != null)
            InventoryUtility.switchTo(prevSlot);
        reset();
    }

    private void reset() {
        state = State.IDLE;
        anchorPos = null;
        chargesDone = 0;
    }

    private enum State {
        IDLE,
        PLACING,
        CHARGING,
        SHIELDING,
        ATTACKING
    }
  }
  
