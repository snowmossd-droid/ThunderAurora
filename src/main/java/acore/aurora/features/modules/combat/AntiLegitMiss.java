package acore.aurora.features.modules.combat;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.hit.EntityHitResult;
import acore.aurora.events.impl.EventAttack;
import acore.aurora.events.impl.EventHandleBlockBreaking;
import acore.aurora.features.modules.Module;

public class AntiLegitMiss extends Module {
    public AntiLegitMiss() {
        super("AntiLegitMiss", Category.COMBAT);
    }

    @EventHandler
    public void onAttack(EventAttack e) {
        if (!(mc.crosshairTarget instanceof EntityHitResult) && e.isPre())
            e.cancel();
    }

    @EventHandler
    public void onBlockBreaking(EventHandleBlockBreaking e) {
        if (!(mc.crosshairTarget instanceof EntityHitResult))
            e.cancel();
    }
}
