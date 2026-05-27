package acore.aurora.gui.clickui.impl;

import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;
import acore.aurora.core.Managers;
import acore.aurora.gui.clickui.AbstractElement;
import acore.aurora.gui.clickui.ClickGUI;
import acore.aurora.gui.font.FontRenderers;
import acore.aurora.setting.Setting;
import acore.aurora.utility.render.Render2DEngine;

import java.awt.*;

import static acore.aurora.core.manager.IManager.mc;
import static acore.aurora.utility.render.animation.AnimationUtility.fast;

public class BooleanElement extends AbstractElement {

    private float anim  = 0f;
    private float anim2 = 0f;

    private static final Color TRACK_OFF = new Color(50, 50, 62, 220);
    private static final Color TRACK_ON  = new Color(215, 205, 175, 245);
    private static final Color KNOB      = new Color(248, 248, 250, 255);
    private static final Color NAME_COL  = new Color(200, 200, 212, 255);
    private static final Color INDENT    = new Color(80, 130, 220, 200);

    public BooleanElement(Setting setting) {
        super(setting);
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        super.render(ctx, mx, my, delta);

        anim  = fast(anim,  (boolean) setting.getValue() ? 1f : 0f, 20f);
        anim2 = fast(anim2, (boolean) setting.getValue() ? 1f : 0f, 8f);

        float trackW = 20f;
        float trackH = 9f;
        float trackX = x + width - trackW - 6;
        float trackY = y + height / 2f - trackH / 2f;

        int r = (int)(TRACK_OFF.getRed()   + (TRACK_ON.getRed()   - TRACK_OFF.getRed())   * anim);
        int g = (int)(TRACK_OFF.getGreen() + (TRACK_ON.getGreen() - TRACK_OFF.getGreen()) * anim);
        int b = (int)(TRACK_OFF.getBlue()  + (TRACK_ON.getBlue()  - TRACK_OFF.getBlue())  * anim);
        Render2DEngine.drawRound(ctx.getMatrices(), trackX, trackY, trackW, trackH, 5f, new Color(r, g, b, 220));

        float knobSize   = trackH - 3f;
        float knobTravel = trackW - knobSize - 3f;
        float knobX      = trackX + 1.5f + knobTravel * anim2;
        float knobY      = trackY + 1.5f;
        Render2DEngine.drawRound(ctx.getMatrices(), knobX, knobY, knobSize, knobSize, 4f, KNOB);

        if (setting.group != null)
            Render2DEngine.drawRect(ctx.getMatrices(), x + 4, y + 2, 1.5f, height - 4, INDENT);

        float nameX = x + 6 + (setting.group != null ? 4f : 0f);
        float nameY = y + height / 2f - 3f;
        FontRenderers.sf_medium_mini.drawString(ctx.getMatrices(), setting.getName(), nameX, nameY, NAME_COL.getRGB());

        if (Render2DEngine.isHovered(mx, my, trackX, trackY, trackW, trackH)) {
            if (GLFW.glfwGetPlatform() != GLFW.GLFW_PLATFORM_WAYLAND)
                GLFW.glfwSetCursor(mc.getWindow().getHandle(),
                        GLFW.glfwCreateStandardCursor(GLFW.GLFW_HAND_CURSOR));
            ClickGUI.anyHovered = true;
        }
    }

    @Override
    public void mouseClicked(int mx, int my, int button) {
        if (hovered && button == 0) {
            setting.setValue(!((Boolean) setting.getValue()));
            Managers.SOUND.playBoolean();
        }
        super.mouseClicked(mx, my, button);
    }
}
