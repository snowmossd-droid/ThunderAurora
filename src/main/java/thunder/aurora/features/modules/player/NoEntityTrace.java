package thunder.aurora.features.modules.player;

import thunder.aurora.features.modules.Module;
import thunder.aurora.setting.Setting;

public final class NoEntityTrace extends Module {
    public NoEntityTrace() {
        super("NoEntityTrace", Category.PLAYER);
    }

    public static final Setting<Boolean> ponly = new Setting<>("Pickaxe Only", true);
    public static final Setting<Boolean> noSword = new Setting<>("No Sword", true);
}