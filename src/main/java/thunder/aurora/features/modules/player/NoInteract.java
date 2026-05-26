package thunder.aurora.features.modules.player;

import thunder.aurora.features.modules.Module;
import thunder.aurora.setting.Setting;

public class NoInteract extends Module {
    public NoInteract() {
        super("NoInteract", Category.PLAYER);
    }

    public static Setting<Boolean> onlyAura = new Setting<>("OnlyAura", false);
}
