package acore.aurora.features.modules;

import acore.aurora.features.modules.misc.ClientSounds;
import acore.aurora.features.modules.render.ClickGUI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class FunctionManager {
    public static final List<Function> functions = new CopyOnWriteArrayList<>();
    public final ClickGUI clickGUI = new ClickGUI();
    public final ClientSounds clientSounds = new ClientSounds();

    public List<Function> getFunctions() {
        return functions;
    }

    public List<Function> getFunctions(Type category) {
        List<Function> result = new ArrayList<>();
        for (Function f : functions) {
            if (f.getCategory() == category) {
                result.add(f);
            }
        }
        return result;
    }
}
