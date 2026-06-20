package acore.aurora.gui.clickui.impl;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import acore.aurora.core.Managers;
import acore.aurora.gui.font.FontRenderers;
import acore.aurora.setting.Setting;
import acore.aurora.setting.impl.BooleanSettingGroup;
import acore.aurora.setting.impl.SettingGroup;
import acore.aurora.utility.render.Render2DEngine;
import acore.aurora.utility.render.TextureStorage;
import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import static acore.aurora.features.modules.Module.mc;

public class ExosGroupRenderer {
    private static final int HEIGHT      = 16;
    private static final int TOGGLE_W    = 20;
    private static final int TOGGLE_H    = 9;
    private final Map<Setting<?>, Float> arrowAnim = new HashMap<>();
    private final Map<Setting<?>, Float> toggleAnim = new HashMap<>();

    public int getHeight() { return HEIGHT; }

    public boolean isExtended(Setting<?> s) {
        Object v = s.getValue();
        if (v instanceof SettingGroup g) return g.isExtended();
        if (v instanceof BooleanSettingGroup g) return g.isExtended();
        return false;
    }

    public void render(DrawContext ctx, Setting<?> setting, int x, int y, int width, int height) {
        Object value = setting.getValue();
        boolean extended = isExtended(setting);

        float arrow = arrowAnim.getOrDefault(setting, extended ? 0f : 1f);
        arrow += ((extended ? 0f : 1f) - arrow) * 0.18f;
        arrowAnim.put(setting, arrow);

        MatrixStack ms = ctx.getMatrices();
        float tx = x + width - 11f;
        float ty = y + height / 2f - 0.5f;
        ms.push();
        ms.translate(tx, ty, 0);
        ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-180f * arrow));
        ms.translate(-tx, -ty, 0);
        ms.translate(x + width - 14f, y + height / 2f - 3f, 0);
        ctx.drawTexture(TextureStorage.guiArrow, 0, 0, 0, 0, 6, 6, 6, 6);
        ms.translate(-(x + width - 14f), -(y + height / 2f - 3f), 0);
        ms.pop();

        int textRightPad = 6;
        if (value instanceof BooleanSettingGroup bsg) {
            float anim = toggleAnim.getOrDefault(setting, bsg.isEnabled() ? 1f : 0f);
            anim += ((bsg.isEnabled() ? 1f : 0f) - anim) * 0.2f;
            toggleAnim.put(setting, anim);

            int toggleX = x + width - 14 - TOGGLE_W - 6;
            int toggleY = y + (height - TOGGLE_H) / 2;
            Color offColor = new Color(50, 50, 62, 220);
            int onColor = new Color(0, 200, 83, 255).getRGB();
            int bg = acore.aurora.utility.color.ColorUtil.interpolateColor(offColor.getRGB(), onColor, anim);
            Render2DEngine.drawRound(ctx.getMatrices(), toggleX, toggleY, TOGGLE_W, TOGGLE_H, 5f, new Color(bg, true));
            float ks = TOGGLE_H - 3f;
            float kt = TOGGLE_W - ks - 3f;
            Render2DEngine.drawRound(ctx.getMatrices(), toggleX + 1.5f + kt * anim, toggleY + 1.5f, ks, ks, 4f, Color.WHITE);
            textRightPad = (x + width) - toggleX + 6;
        }

        FontRenderers.sf_medium_mini.drawString(ctx.getMatrices(), setting.getName(),
                x + 4, y + height / 2f - 3f, Color.WHITE.getRGB());
    }

    public boolean mouseClicked(Setting<?> setting, double mx, double my, int btn,
                                 int x, int y, int width, int height) {
        if (btn != 0) return false;
        Object value = setting.getValue();

        if (value instanceof BooleanSettingGroup bsg) {
            int toggleX = x + width - 14 - TOGGLE_W - 6;
            int toggleY = y + (height - TOGGLE_H) / 2;
            if (mx >= toggleX && mx <= toggleX + TOGGLE_W && my >= toggleY && my <= toggleY + TOGGLE_H) {
                bsg.setEnabled(!bsg.isEnabled());
                Managers.SOUND.playBoolean();
                return true;
            }
            bsg.setExtended(!bsg.isExtended());
            if (bsg.isExtended()) Managers.SOUND.playSwipeIn(); else Managers.SOUND.playSwipeOut();
            return true;
        }

        if (value instanceof SettingGroup sg) {
            sg.setExtended(!sg.isExtended());
            if (sg.isExtended()) Managers.SOUND.playSwipeIn(); else Managers.SOUND.playSwipeOut();
            return true;
        }

        return false;
    }
  }
          
