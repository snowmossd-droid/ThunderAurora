package acore.aurora.utility.render.providers;

import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;

public class ResourceProvider {
    public static final ShaderProgramKeys TEXTURE_SHADER_KEY = ShaderProgramKeys.POSITION_TEX_COLOR;
    public static final ShaderProgramKeys RECTANGLE_SHADER_KEY = ShaderProgramKeys.POSITION_COLOR;
    public static final ShaderProgramKeys BLUR_SHADER_KEY = ShaderProgramKeys.POSITION_COLOR;
    public static final ShaderProgramKeys RECTANGLE_BORDER_SHADER_KEY = ShaderProgramKeys.POSITION_COLOR;
    public static final ShaderProgramKeys GLASS_SHADER_KEY = ShaderProgramKeys.POSITION_TEX_COLOR;

    public static Identifier getShaderIdentifier(String name) {
        return Identifier.of("acoreaurora", "core/shaders/" + name + ".json");
    }

    public static Identifier getGlass(String name) {
        return Identifier.of("acoreaurora", "core/glass/" + name + ".json");
    }
}
