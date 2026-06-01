package acore.aurora.utility.render.providers;

import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;

public class ResourceProvider {
    public static final RegistryKey<ShaderProgram> TEXTURE_SHADER_KEY = ShaderProgramKeys.POSITION_TEX_COLOR;
    public static final RegistryKey<ShaderProgram> RECTANGLE_SHADER_KEY = ShaderProgramKeys.POSITION_COLOR;
    public static final RegistryKey<ShaderProgram> BLUR_SHADER_KEY = ShaderProgramKeys.POSITION_COLOR;
    public static final RegistryKey<ShaderProgram> RECTANGLE_BORDER_SHADER_KEY = ShaderProgramKeys.POSITION_COLOR;
    public static final RegistryKey<ShaderProgram> GLASS_SHADER_KEY = ShaderProgramKeys.POSITION_TEX_COLOR;

    public static Identifier getShaderIdentifier(String name) {
        return Identifier.of("acoreaurora", "core/shaders/" + name + ".json");
    }

    public static Identifier getGlass(String name) {
        return Identifier.of("acoreaurora", "core/glass/" + name + ".json");
    }
}
