package acore.aurora.gui.clickui;

import net.minecraft.client.gui.DrawContext;
import acore.aurora.features.modules.client.HudEditor;
import acore.aurora.gui.font.FontRenderers;
import acore.aurora.setting.Setting;
import acore.aurora.utility.render.Render2DEngine;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

import static acore.aurora.features.modules.Module.mc;

public class ToggleSwitchRenderer {
    private static final int H = 16, SW = 22, SH = 12, KR = 8;
    private final Map<Setting<?>, Float> toggleMap = new HashMap<>();

    public int getHeight() { return H; }

    public void render(DrawContext ctx, Setting<?> s, int x, int y, int width, int height) {
        float prog = toggleMap.getOrDefault(s, (boolean)s.getValue() ? 1f : 0f);
        prog += (((boolean)s.getValue() ? 1f : 0f) - prog) * 0.15f;
        toggleMap.put(s, prog);

        int swX = x + width - SW + 4;
        int swY = y + (H - SH) / 2 - 2;

        Color off = new Color(50, 50, 50, 200);
        Color raw = HudEditor.getColor(0);
        Color on = (raw.getAlpha() < 10 || (raw.getRed() < 5 && raw.getGreen() < 5 && raw.getBlue() < 5))
                ? new Color(180, 60, 60) : raw;

        int r = (int)(off.getRed()   + (on.getRed()   - off.getRed())   * prog);
        int g = (int)(off.getGreen() + (on.getGreen() - off.getGreen()) * prog);
        int b = (int)(off.getBlue()  + (on.getBlue()  - off.getBlue())  * prog);
        Render2DEngine.drawRound(ctx.getMatrices(), swX, swY, SW, SH, 5f, new Color(r, g, b, 200));

        float kx = swX + 3 + (SW - KR - 5) * prog + KR / 2f;
        float ky = swY + (SH - KR) / 2f + KR / 2f;
        Render2DEngine.renderRoundedQuad(ctx.getMatrices(), Color.WHITE, kx - KR/2f, ky - KR/2f, kx + KR/2f, ky + KR/2f, KR/2f, 8);

        float maxW = swX - x - 4;
        FontRenderers.sf_medium_mini.drawString(ctx.getMatrices(), s.getName(), x, y + (H - 7) / 2f, Color.WHITE.getRGB());
    }

    public boolean mouseClicked(Setting<?> s, double mx, double my, int btn, int x, int y, int width, int height) {
        if (btn != 0) return false;
        int swX = x + width - SW + 4;
        int swY = y + (H - SH) / 2 - 2;
        if (mx >= swX && mx <= swX + SW && my >= swY && my <= swY + SH) {
            @SuppressWarnings("unchecked")
            Setting<Boolean> bs = (Setting<Boolean>) s;
            bs.setValue(!bs.getValue()); return true;
        }
        return false;
    }
}
