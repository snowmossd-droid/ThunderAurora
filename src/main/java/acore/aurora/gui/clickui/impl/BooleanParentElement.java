package acore.aurora.gui.clickui.impl;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import org.lwjgl.glfw.GLFW;
import acore.aurora.core.Managers;
import acore.aurora.gui.clickui.AbstractElement;
import acore.aurora.gui.clickui.ClickGUI;
import acore.aurora.gui.font.FontRenderers;
import acore.aurora.setting.Setting;
import acore.aurora.setting.impl.BooleanSettingGroup;
import acore.aurora.features.modules.client.HudEditor;
import acore.aurora.utility.render.Render2DEngine;
import acore.aurora.utility.render.TextureStorage;
import java.awt.*;
import static acore.aurora.core.manager.IManager.mc;
import static acore.aurora.utility.render.animation.AnimationUtility.fast;
public class BooleanParentElement extends AbstractElement {
    private final Setting<BooleanSettingGroup> parentSetting;
    private float anim, arrowAnim;
    private static final Color TRACK_OFF = new Color(50, 50, 62, 220);
    private static final Color KNOB      = new Color(248, 248, 250, 255);
    private static final Color NAME_COL  = new Color(200, 200, 212, 255);
    public BooleanParentElement(Setting setting) {
        super(setting);
        this.parentSetting = setting;
    }
    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        super.render(ctx, mx, my, delta);
        anim      = fast(anim,      getParentSetting().getValue().isEnabled() ? 1f : 0f, 20f);
        arrowAnim = fast(arrowAnim, getParentSetting().getValue().isExtended() ? 0f : 1f, 15f);
        MatrixStack ms = ctx.getMatrices();
        float tx = x + width - 11;
        float ty = y + 7.5f;
        ms.push();
        ms.translate(tx, ty, 0);
        ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-180f * arrowAnim));
        ms.translate(-tx, -ty, 0);
        ms.translate((x + width - 14), (y + 4.5f), 0);
        ctx.drawTexture(TextureStorage.guiArrow, 0, 0, 0, 0, 6, 6, 6, 6);
        ms.translate(-(x + width - 14), -(y + 4.5f), 0);
        ms.pop();
        FontRenderers.sf_medium_mini.drawString(ms, setting.getName(), x + 6, y + height / 2f - 1f, NAME_COL.getRGB());
        float trackW = 20f;
        float trackH = 9f;
        float trackX = x + width - trackW - 20;
        float trackY = y + height / 2f - trackH / 2f;
        Color raw = HudEditor.getColor(0);
        Color accentOn = raw.getAlpha() < 10 || (raw.getRed() < 5 && raw.getGreen() < 5 && raw.getBlue() < 5)
                ? new Color(200, 80, 80, 255) : new Color(raw.getRed(), raw.getGreen(), raw.getBlue(), 255);
        int r = (int)(TRACK_OFF.getRed()   + (accentOn.getRed()   - TRACK_OFF.getRed())   * anim);
        int g = (int)(TRACK_OFF.getGreen() + (accentOn.getGreen() - TRACK_OFF.getGreen()) * anim);
        int b = (int)(TRACK_OFF.getBlue()  + (accentOn.getBlue()  - TRACK_OFF.getBlue())  * anim);
        Render2DEngine.drawRound(ctx.getMatrices(), trackX, trackY, trackW, trackH, 5f, new Color(r, g, b, 220));
        float knobSize   = trackH - 3f;
        float knobTravel = trackW - knobSize - 3f;
        float knobX      = trackX + 1.5f + knobTravel * anim;
        float knobY      = trackY + 1.5f;
        Render2DEngine.drawRound(ctx.getMatrices(), knobX, knobY, knobSize, knobSize, 4f, KNOB);
        if (Render2DEngine.isHovered(mx, my, trackX, trackY, trackW, trackH)) {
            if (GLFW.glfwGetPlatform() != GLFW.GLFW_PLATFORM_WAYLAND)
                GLFW.glfwSetCursor(mc.getWindow().getHandle(),
                        GLFW.glfwCreateStandardCursor(GLFW.GLFW_HAND_CURSOR));
            ClickGUI.anyHovered = true;
        }
    public void mouseClicked(int mx, int my, int button) {
        if (hovered) {
            if (button == 0) {
                getParentSetting().getValue().setEnabled(!getParentSetting().getValue().isEnabled());
                Managers.SOUND.playBoolean();
            } else {
                getParentSetting().getValue().setExtended(!getParentSetting().getValue().isExtended());
                if (getParentSetting().getValue().isExtended()) Managers.SOUND.playSwipeIn();
                else Managers.SOUND.playSwipeOut();
            }
        super.mouseClicked(mx, my, button);
    public Setting<BooleanSettingGroup> getParentSetting() { return parentSetting; }
}
