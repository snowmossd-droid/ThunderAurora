package acore.aurora.utility.render.providers;

import net.minecraft.util.Identifier;

public class ResourceProvider {

    public static Identifier getShaderIdentifier(String name) {
        return Identifier.of("acoreaurora", "core/shaders/" + name + ".json");
    }

    public static Identifier getGlass(String name) {
        return Identifier.of("acoreaurora", "core/glass/" + name + ".json");
    }
}
