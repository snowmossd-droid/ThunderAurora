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

public class ChipModeRenderer {
    private static final int TITLE_H = 14, BOX_H = 12, SPACING = 2, PH = 3;
    private final Map<String, Float> hoverProg = new HashMap<>();

    public int getHeight(Setting<?> s, int width) {
        int cx = 0, lines = 1;
        for (String m : s.getModes()) {
            int bw = (int)FontRenderers.sf_medium_mini.getStringWidth(m) + PH * 2;
            if (cx + bw > width) { cx = 0; lines++; }
            cx += bw + 4;
        }
        return TITLE_H + lines * (BOX_H + SPACING);
    }

    public void render(DrawContext ctx, Setting<?> s, int x, int y, int width, int height) {
        double mxD = mc.mouse.getX() / mc.getWindow().getScaleFactor();
        double myD = mc.mouse.getY() / mc.getWindow().getScaleFactor();

        FontRenderers.sf_medium_mini.drawString(ctx.getMatrices(), s.getName(), x, y, Color.WHITE.getRGB());

        Color accent = HudEditor.getColor(0);
        if (accent.getAlpha() < 10 || (accent.getRed() < 5 && accent.getGreen() < 5 && accent.getBlue() < 5))
            accent = new Color(180, 60, 60);

        int cx = x, cy = y + TITLE_H;
        for (String mode : s.getModes()) {
            int tw = (int)FontRenderers.sf_medium_mini.getStringWidth(mode);
            int bw = tw + PH * 2;
            if (cx + bw > x + width) { cx = x; cy += BOX_H + SPACING; }

            boolean sel = mode.equalsIgnoreCase(s.currentEnumName());
            boolean hov = mxD >= cx && mxD <= cx + bw && myD >= cy && myD <= cy + BOX_H;

            Color bg = sel ? accent : new Color(35, 35, 42, 200);
            Render2DEngine.drawRound(ctx.getMatrices(), cx, cy, bw, BOX_H, 2f, bg);

            int tc = sel ? Color.WHITE.getRGB() : new Color(180, 180, 180).getRGB();
            FontRenderers.sf_medium_mini.drawString(ctx.getMatrices(), mode, cx + PH, cy + (BOX_H - 7) / 2f, tc);
            cx += bw + 4;
        }
    }

    public boolean mouseClicked(Setting<?> s, double mx, double my, int btn, int x, int y, int width, int height) {
        if (btn != 0) return false;
        int cx = x, cy = y + TITLE_H;
        for (String mode : s.getModes()) {
            int bw = (int)FontRenderers.sf_medium_mini.getStringWidth(mode) + PH * 2;
            if (cx + bw > x + width) { cx = x; cy += BOX_H + SPACING; }
            if (mx >= cx && mx <= cx + bw && my >= cy && my <= cy + BOX_H) { s.setEnumByName(mode); return true; }
            cx += bw + 4;
        }
        return false;
    }
}
