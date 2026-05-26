package thunder.aurora.features.modules.combat;

import thunder.aurora.features.modules.Module;
import thunder.aurora.setting.Setting;

import static thunder.aurora.core.manager.client.ServerManager.round2;

public final class HitBox extends Module {
    public HitBox() {
        super("HitBoxes", Category.COMBAT);
    }

    public static final Setting<Float> XZExpand = new Setting<>("XZExpand", 1.0f, 0.0f, 5.0f);
    public static final Setting<Float> YExpand = new Setting<>("YExpand", 0.0f, 0.0f, 5.0f);
    public static final Setting<Boolean> affectToAura = new Setting<>("AffectToAura", false);

    @Override
    public String getDisplayInfo() {
        return "H: " + round2(XZExpand.getValue()) + " V: " + round2(YExpand.getValue());
    }
}
