package acore.aurora.features.modules.combat;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.MaceItem;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import acore.aurora.events.impl.PacketEvent;
import acore.aurora.features.modules.Module;
import acore.aurora.setting.Setting;

public class MaceDamage extends Module {

    public final Setting<Integer> blocks     = new Setting<>("Blocks",    22, 1, 100);
    public final Setting<Integer> packets    = new Setting<>("Packets",   80, 1, 200);
    public final Setting<Boolean> onlyMace   = new Setting<>("OnlyMace",  true);
    public final Setting<Boolean> onGround   = new Setting<>("OnGround",  true);

    public MaceDamage() {
        super("MaceDamage", Category.COMBAT);
    }

    @EventHandler
    public void onPacketSend(PacketEvent.Send e) {
        if (mc.player == null) return;
        if (onlyMace.getValue() && !(mc.player.getMainHandStack().getItem() instanceof MaceItem)) return;

        if (!(e.getPacket() instanceof PlayerMoveC2SPacket)) return;
        if (onGround.getValue() && !mc.player.isOnGround()) return;

        double x = mc.player.getX();
        double y = mc.player.getY();
        double z = mc.player.getZ();
        double height = blocks.getValue() + 2.1;

        for (int i = 0; i < packets.getValue(); i++) {
            mc.getNetworkHandler().sendPacket(
                new PlayerMoveC2SPacket.PositionAndOnGround(x, y + height, z, false, mc.player.horizontalCollision)
            );
            mc.getNetworkHandler().sendPacket(
                new PlayerMoveC2SPacket.PositionAndOnGround(x, y + 0.05, z, false, mc.player.horizontalCollision)
            );
        }
        mc.getNetworkHandler().sendPacket(
            new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, true, mc.player.horizontalCollision)
        );
    }
}
