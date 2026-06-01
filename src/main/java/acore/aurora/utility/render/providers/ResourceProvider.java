package acore.aurora.utility.render.providers;

import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.util.Identifier;

public class ResourceProvider {
    public static final net.minecraft.client.gl.ShaderProgram TEXTURE_SHADER_KEY = null;
    public static final net.minecraft.client.gl.ShaderProgram RECTANGLE_SHADER_KEY = null;
    public static final net.minecraft.client.gl.ShaderProgram BLUR_SHADER_KEY = null;
    public static final net.minecraft.client.gl.ShaderProgram RECTANGLE_BORDER_SHADER_KEY = null;
    public static final net.minecraft.client.gl.ShaderProgram GLASS_SHADER_KEY = null;

    public static final Identifier color_image = Identifier.of("acoreaurora", "textures/gui/colorpicker.png");

    public static Identifier getShaderIdentifier(String name) {
        return Identifier.of("acoreaurora", "core/shaders/" + name + ".json");
    }

    public static Identifier getGlass(String name) {
        return Identifier.of("acoreaurora", "core/glass/" + name + ".json");
    }
}

    public static Identifier getShaderIdentifier(String name) {
        return Identifier.of("acoreaurora", "core/shaders/" + name + ".json");
    }

    public static Identifier getGlass(String name) {
        return Identifier.of("acoreaurora", "core/glass/" + name + ".json");
    }
        }
        
