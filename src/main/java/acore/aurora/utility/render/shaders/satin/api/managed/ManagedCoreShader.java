package acore.aurora.utility.render.shaders.satin.api.managed;

import acore.aurora.utility.render.shaders.satin.api.managed.uniform.UniformFinder;
import net.minecraft.client.gl.ShaderProgram;

public interface ManagedCoreShader extends UniformFinder {
    ShaderProgram getProgram();

    void release();
}
