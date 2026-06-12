package acore.aurora.gui.clickui;
import net.minecraft.client.gui.DrawContext;
import acore.aurora.features.modules.client.HudEditor;
import acore.aurora.gui.font.FontRenderers;
import acore.aurora.setting.Setting;
import acore.aurora.utility.render.Render2DEngine;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
public class SliderBarRenderer {
    private static final int H = 20, BAR_H = 4;
    private static final float CR = 5f;
    public int getHeight() { return H; }
    @SuppressWarnings("unchecked")
    public void render(DrawContext ctx, Setting<?> s, int x, int y, int width, int height) {
        int barX = x; int barY = y + height - BAR_H - 4; int barW = width;
        Render2DEngine.drawRect(ctx.getMatrices(), barX, barY, barW, BAR_H, new Color(50, 50, 50, 180));
        double min = getMin(s), max = getMax(s), val = getVal(s);
        double prog = Math.max(0, Math.min(1, (val - min) / (max - min)));
        int pw = (int)(barW * prog);
        Color accent = HudEditor.getColor(0);
        if (accent.getAlpha() < 10 || (accent.getRed() < 5 && accent.getGreen() < 5 && accent.getBlue() < 5))
            accent = new Color(180, 60, 60);
        Render2DEngine.drawRect(ctx.getMatrices(), barX, barY, pw, BAR_H, accent);
        FontRenderers.sf_medium_mini.drawString(ctx.getMatrices(), s.getName(), x, y + 2, Color.WHITE.getRGB());
        String valStr = fmtVal(val, getIncrement(s));
        float vw = FontRenderers.sf_medium_mini.getStringWidth(valStr);
        FontRenderers.sf_medium_mini.drawString(ctx.getMatrices(), valStr, x + width - vw, y + 2, Color.WHITE.getRGB());
        float cx = barX + pw; float cy = barY + BAR_H / 2f;
        Render2DEngine.renderRoundedQuad(ctx.getMatrices(), Color.WHITE, cx - CR, cy - CR, cx + CR, cy + CR, CR, 8);
    }
    public boolean mouseClicked(Setting<?> s, double mx, double my, int btn, int x, int y, int width, int height) {
        if (btn != 0) return false;
        int barX = x; int barY = y + height - BAR_H - 4; int barW = width;
        if (mx >= barX && mx <= barX + barW && my >= barY - 3 && my <= barY + BAR_H + 6) {
            updateVal(s, mx, barX, barW); return true;
        }
        return false;
    }
    public void mouseDragged(Setting<?> s, double mx, int x, int width) { updateVal(s, mx, x, width); }
    public void mouseReleased(Setting<?> s) {}
    @SuppressWarnings("unchecked")
    private void updateVal(Setting<?> s, double mx, int barX, int barW) {
        double pct = Math.max(0, Math.min(1, (mx - barX) / barW));
        double min = getMin(s), max = getMax(s), inc = getIncrement(s);
        double nv = min + pct * (max - min);
        nv = Math.round(nv / inc) * inc;
        nv = Math.max(min, Math.min(max, nv));
        @SuppressWarnings("unchecked")
        Setting<Float> fs = (Setting<Float>) s;
        fs.setValue((float)nv);
    }
    private double getVal(Setting<?> s) { Object v = s.getValue(); if (v instanceof Float f) return f; if (v instanceof Double d) return d; if (v instanceof Integer i) return i; return 0; }
    private double getMin(Setting<?> s) { try { return (double)(float)s.getClass().getMethod("getMin").invoke(s); } catch(Exception e) { return 0; } }
    private double getMax(Setting<?> s) { try { return (double)(float)s.getClass().getMethod("getMax").invoke(s); } catch(Exception e) { return 1; } }
    private double getIncrement(Setting<?> s) { return 0.01; }
    private String fmtVal(double v, double inc) {
        if (inc >= 1) return String.format(Locale.US, "%d", (long)v);
        if (inc >= 0.1) return String.format(Locale.US, "%.1f", v);
        return String.format(Locale.US, "%.2f", v);
    }
    public int getHeight(Setting<?> s, int width) { return H; }
}