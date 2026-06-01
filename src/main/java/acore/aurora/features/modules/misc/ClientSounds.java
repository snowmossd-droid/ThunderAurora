package acore.aurora.features.modules.misc;
import acore.aurora.gui.clickui.setting.ModeSetting;
import acore.aurora.gui.clickui.setting.MultiSetting;
import acore.aurora.gui.clickui.setting.SliderSetting;
import acore.aurora.events.Event;
import acore.aurora.features.modules.Function;
import acore.aurora.features.modules.FunctionAnnotation;
import acore.aurora.features.modules.Type;

import java.util.Arrays;

@FunctionAnnotation(name = "ClientSounds", desc = "Звуки", type = Type.Misc)
public class ClientSounds extends Function {
    public final MultiSetting check = new MultiSetting(
            "Выбрать",
            Arrays.asList("Вход в клиент"),
            new String[]{"Вход в клиент"}
    );
    public final ModeSetting mode = new ModeSetting("Мод", "Type-1", "Type-1", "Type-2", "Type-3","Type-4");
    public final SliderSetting volume = new SliderSetting("Громкость", 100f, 1f, 100f,1f);


    public ClientSounds() {
        addSettings(check,mode,volume);
    }

    @Override
    public void onEvent(Event event) {
    }
}
