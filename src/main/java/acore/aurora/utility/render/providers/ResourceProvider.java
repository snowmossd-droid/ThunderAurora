package acore.aurora.utility.render.providers;

import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.Shader;
import net.minecraft.util.Identifier;

public class ResourceProvider {
    public static Shader getTextureShader() {
        return GameRenderer.getPositionTexColorProgram();
    }

    public static Shader getRectangleShader() {
        return GameRenderer.getPositionColorProgram();
    }

    public static Shader getBlurShader() {
        return GameRenderer.getPositionColorProgram();
    }

    public static Shader getRectangleBorderShader() {
        return GameRenderer.getPositionColorProgram();
    }

    public static Shader getGlassShader() {
        return GameRenderer.getPositionTexColorProgram();
    }

    public static Identifier getShaderIdentifier(String name) {
        return Identifier.of("acoreaurora", "core/shaders/" + name + ".json");
    }

    public static Identifier getGlass(String name) {
        return Identifier.of("acoreaurora", "core/glass/" + name + ".json");
    }
}
