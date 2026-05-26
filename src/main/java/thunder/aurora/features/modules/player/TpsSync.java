package thunder.aurora.features.modules.player;

import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import thunder.aurora.ThunderAurora;
import thunder.aurora.core.Managers;
import thunder.aurora.core.manager.client.ModuleManager;
import thunder.aurora.events.impl.EventTick;
import thunder.aurora.features.modules.Module;

public class TpsSync extends Module {
    public TpsSync() {
        super("TpsSync", Module.Category.PLAYER);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onTick(EventTick e) {
        if (ModuleManager.timer.isEnabled()) return;
        if (Managers.SERVER.getTPS() > 1)
            ThunderAurora.TICK_TIMER = Managers.SERVER.getTPS() / 20f;
        else ThunderAurora.TICK_TIMER = 1f;
    }

    @Override
    public void onDisable() {
        ThunderAurora.TICK_TIMER = 1f;
    }
}
