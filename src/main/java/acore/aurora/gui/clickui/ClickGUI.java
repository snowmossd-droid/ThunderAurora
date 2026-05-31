package acore.aurora.gui.clickui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import acore.aurora.core.Managers;
import acore.aurora.features.modules.Module;
import acore.aurora.gui.clickui.impl.ExosModeRenderer;
import acore.aurora.gui.clickui.impl.ExosSliderRenderer;
import acore.aurora.gui.clickui.impl.ExosToggleSwitchRenderer;
import acore.aurora.gui.font.FontRenderers;
import acore.aurora.setting.Setting;
import acore.aurora.setting.impl.BooleanSettingGroup;
import acore.aurora.setting.impl.ColorSetting;
import acore.aurora.setting.impl.PositionSetting;
import acore.aurora.setting.impl.SettingGroup;
import acore.aurora.utility.color.ColorUtil;
import acore.aurora.utility.render.Render2DEngine;
import acore.aurora.utility.render.ThemeManager;
import net.minecraft.util.Identifier;

import java.awt.*;
import java.util.*;
import java.util.List;

import static acore.aurora.features.modules.Module.mc;

public class ClickGUI extends Screen {

    public static boolean anyHovered = false;
    public static boolean close = false;
    public static String currentDescription = "";

    public static class SimpleAnimation {
        public void reset() {}
    }
    public final SimpleAnimation imageAnimation = new SimpleAnimation();

    private boolean isClose = false;

    private static final int PANEL_W   = 125;
    private static final int PANEL_H   = 280;
    private static final int PANEL_M   = 8;
    private static final int TITLE_MT  = 5;
    private static final int TITLE_H   = 20;
    private static final int MOD_H     = 20;
    private static final int SCROLL_Y  = TITLE_MT + TITLE_H;
    private static final int SCROLL_H  = PANEL_H - SCROLL_Y - 5;
    private static final int SEARCH_W  = 180;
    private static final int SEARCH_H  = 20;
    private static final int SEARCH_MB = 10;
    private static final int THEME_H   = 16;
    private static final int THEME_MB  = 40;
    private static final int THEME_W   = 180;

    private static final int COLOR_PICKER_W = 130;
    private static final int COLOR_PICKER_H = 110;

    private static final Identifier COLORS_ICON = Identifier.of("acoreaurora", "textures/gui/elements/colors.png");

    private static final Color GUI_BG       = new Color(18, 18, 24, 220);
    private static final Color ACCENT_L     = new Color(110, 60, 200, 195);
    private static final Color ACCENT_R     = new Color(140, 90, 240, 195);
    private static final Color TEXT_ENABLED = new Color(255, 255, 255, 255);
    private static final Color TEXT_NORMAL  = new Color(190, 190, 200, 255);

    private static final List<Module.Category> CATS = Arrays.asList(
        Module.Category.COMBAT,   Module.Category.MOVEMENT,
        Module.Category.RENDER,   Module.Category.PLAYER,
        Module.Category.MISC);

    private static final Map<Module.Category, Float> scrollOff = new HashMap<>();
    private static final Map<Module.Category, Float> scrollTgt = new HashMap<>();
    private final Map<Module, Float> expandProg = new HashMap<>();
    private final Map<Module, Float> arrowProg  = new HashMap<>();

    private float openAnim = 0f;

    private final SearchState search = new SearchState();
    private Module bindingModule = null;
    private Setting<?> draggingSlider = null;
    private int draggingSliderX, draggingSliderW;

    private static boolean themeMenuOpen = false;
    private float themeMenuAnim = 0f;
    private static float themeScrollOff = 0f;
    private static float themeScrollTgt = 0f;

    private final ExosToggleSwitchRenderer toggleRend = new ExosToggleSwitchRenderer();
    private final ExosSliderRenderer       sliderRend = new ExosSliderRenderer();
    private final ExosModeRenderer         modeRend   = new ExosModeRenderer();

    private Setting<?> openColorSetting = null;
    private float colorPickerX, colorPickerY;
    private boolean draggingHue = false;
    private boolean draggingAlpha = false;
    private boolean draggingSB = false;
    private float cpHue, cpSat, cpBri;
    private int cpAlpha;
    private boolean cpNeedsInit = false;

    public ClickGUI() {
        super(Text.literal("AcoreAurora"));
        if (scrollOff.isEmpty()) {
            CATS.forEach(c -> { scrollOff.put(c, 0f); scrollTgt.put(c, 0f); });
        }
    }

    public static ClickGUI getClickGui() { return new ClickGUI(); }

    @Override public boolean shouldPause() { return false; }
    @Override public void renderBackground(DrawContext ctx, int mx, int my, float delta) {}

    @Override
    public void init() {
        super.init();
        isClose = false;
        openAnim = 0f;
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        if (isClose) { mc.setScreen(null); return; }

        openAnim = Math.min(1f, openAnim + delta * 5f);
        if (openAnim < 0.01f) return;

        CATS.forEach(c -> {
            float t = scrollTgt.get(c), o = scrollOff.get(c);
            scrollOff.put(c, o + (t - o) * Math.min(1f, 0.12f * 20f * delta));
        });
        themeScrollOff += (themeScrollTgt - themeScrollOff) * 0.18f;
        themeMenuAnim  += ((themeMenuOpen ? 1f : 0f) - themeMenuAnim) * 0.18f;

        int totalW = CATS.size() * (PANEL_W + PANEL_M) - PANEL_M;
        int sx = (width - totalW) / 2;
        int sy = (height - PANEL_H) / 2;

        ctx.getMatrices().push();
        ctx.getMatrices().translate(width / 2f, height / 2f, 0);
        ctx.getMatrices().scale(openAnim, openAnim, 1f);
        ctx.getMatrices().translate(-width / 2f, -height / 2f, 0);

        Render2DEngine.drawRect(ctx.getMatrices(), 0, 0, width, height,
                new Color(0, 0, 0, (int)(110 * openAnim)));

        String title = "ClickGui";
        float titleSize = FontRenderers.sf_bold.getStringWidth(title);
        int titleColor = ColorUtil.getFirstColor();
        FontRenderers.sf_bold.drawString(ctx.getMatrices(), title,
                (width - titleSize) / 2f, sy - 28, titleColor);

        int idx = 0;
        for (Module.Category cat : CATS) {
            renderPanel(ctx, sx + idx++ * (PANEL_W + PANEL_M), sy, cat, mx, my);
        }

        Module hovered = getHoveredModule(mx, my, sx, sy);
        if (hovered != null && !hovered.getDescription().isEmpty()) {
            drawDescription(ctx, hovered.getDescription(), sy);
        }

        DescriptionRenderQueue.renderAll(ctx);
        renderSearch(ctx, mx, my);
        renderThemeButton(ctx, mx, my);
        if (themeMenuAnim > 0.01f) renderThemeRow(ctx, mx, my);

        if (openColorSetting != null) {
            renderColorPicker(ctx, mx, my);
        }

        ctx.getMatrices().pop();
    }

    private void renderColorPicker(DrawContext ctx, int mx, int my) {
        ColorSetting cs = (ColorSetting) openColorSetting.getValue();

        float px = colorPickerX;
        float py = colorPickerY;
        float pw = COLOR_PICKER_W;
        float ph = COLOR_PICKER_H;

        Render2DEngine.drawRound(ctx.getMatrices(), px, py, pw, ph, 6f, new Color(22, 22, 30, 245));
        Render2DEngine.drawRound(ctx.getMatrices(), px + 1, py + 1, pw - 2, 1, 0f, new Color(255, 255, 255, 25));

        float sbX = px + 6;
        float sbY = py + 18;
        float sbW = pw - 46;
        float sbH = ph - 36;

        if (draggingSB) {
            cpSat = MathHelper.clamp((mx - sbX) / sbW, 0f, 1f);
            cpBri = MathHelper.clamp(1f - (my - sbY) / sbH, 0f, 1f);
            applyColor(cs);
        }

        Color satA = Color.getHSBColor(cpHue, 0f, 1f);
        Color satB = Color.getHSBColor(cpHue, 1f, 1f);
        Render2DEngine.horizontalGradient(ctx.getMatrices(), sbX, sbY, sbX + sbW, sbY + sbH, satA, satB);
        Render2DEngine.verticalGradient(ctx.getMatrices(), sbX, sbY, sbX + sbW, sbY + sbH,
                new Color(0, 0, 0, 0), new Color(0, 0, 0, 255));

        float dotX = sbX + cpSat * sbW;
        float dotY = sbY + (1f - cpBri) * sbH;
        Render2DEngine.drawRound(ctx.getMatrices(), dotX - 3, dotY - 3, 6, 6, 3f, Color.WHITE);
        Render2DEngine.drawRound(ctx.getMatrices(), dotX - 2, dotY - 2, 4, 4, 2f, Color.getHSBColor(cpHue, cpSat, cpBri));

        float hueX = px + pw - 36;
        float hueY = sbY;
        float hueW = 8;
        float hueH = sbH;

        for (float i = 0; i < hueH; i++) {
            float h = i / hueH;
            Render2DEngine.drawRect(ctx.getMatrices(), hueX, hueY + i, hueW, 1.5f, Color.getHSBColor(h, 1f, 1f));
        }
        float hueMarkerY = hueY + cpHue * hueH;
        Render2DEngine.drawRect(ctx.getMatrices(), hueX - 1, hueMarkerY - 1, hueW + 2, 2, Color.WHITE);

        if (draggingHue) {
            cpHue = MathHelper.clamp((my - hueY) / hueH, 0f, 1f);
            applyColor(cs);
        }

        float alphaX = px + pw - 22;
        float alphaY = sbY;
        float alphaW = 8;
        float alphaH = sbH;

        Color curRGB = Color.getHSBColor(cpHue, cpSat, cpBri);
        Render2DEngine.verticalGradient(ctx.getMatrices(), alphaX, alphaY, alphaX + alphaW, alphaY + alphaH,
                new Color(curRGB.getRed(), curRGB.getGreen(), curRGB.getBlue(), 255),
                new Color(curRGB.getRed(), curRGB.getGreen(), curRGB.getBlue(), 0));

        float alphaMarkerY = alphaY + (1f - cpAlpha / 255f) * alphaH;
        Render2DEngine.drawRect(ctx.getMatrices(), alphaX - 1, alphaMarkerY - 1, alphaW + 2, 2, Color.WHITE);

        if (draggingAlpha) {
            cpAlpha = (int)(MathHelper.clamp(1f - (my - alphaY) / alphaH, 0f, 1f) * 255);
            applyColor(cs);
        }

        String name = openColorSetting.getName();
        FontRenderers.sf_medium_mini.drawString(ctx.getMatrices(), name, px + 6, py + 6, Color.WHITE.getRGB());

        Color previewColor = cs.getColorObject();
        Render2DEngine.drawRound(ctx.getMatrices(), px + pw - 22, py + 5, 16, 8, 2f, previewColor);

        float btnY = py + ph - 13;
        float btnH = 9;

        Render2DEngine.drawRect(ctx.getMatrices(), px + 6, btnY, 34, btnH, new Color(0x424242));
        FontRenderers.sf_medium_mini.drawString(ctx.getMatrices(), "Copy", px + 10, btnY + 2, Color.WHITE.getRGB());

        boolean isRainbow = cs.isRainbow();
        Render2DEngine.drawRect(ctx.getMatrices(), px + 44, btnY, 34, btnH,
                isRainbow ? previewColor : new Color(0x424242));
        FontRenderers.sf_medium_mini.drawString(ctx.getMatrices(), "RB", px + 55, btnY + 2,
                isRainbow && !Render2DEngine.isDark(previewColor) ? Color.BLACK.getRGB() : Color.WHITE.getRGB());

        boolean hasCopy = acore.aurora.AcoreAurora.copy_color != null;
        Render2DEngine.drawRect(ctx.getMatrices(), px + 82, btnY, 34, btnH,
                hasCopy ? acore.aurora.AcoreAurora.copy_color : new Color(0x424242));
        FontRenderers.sf_medium_mini.drawString(ctx.getMatrices(), "Paste", px + 86, btnY + 2,
                (hasCopy && !Render2DEngine.isDark(acore.aurora.AcoreAurora.copy_color)) ? Color.BLACK.getRGB() : Color.WHITE.getRGB());
    }

    private void applyColor(ColorSetting cs) {
        Color c = Color.getHSBColor(cpHue, cpSat, cpBri);
        cs.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), cpAlpha).getRGB());
    }

    private void initColorPicker(ColorSetting cs) {
        float[] hsb = Color.RGBtoHSB(cs.getColorObject().getRed(), cs.getColorObject().getGreen(), cs.getColorObject().getBlue(), null);
        cpHue = hsb[0];
        cpSat = hsb[1];
        cpBri = hsb[2];
        cpAlpha = cs.getAlpha();
    }

    private void drawDescription(DrawContext ctx, String desc, int startY) {
        float tw = FontRenderers.sf_medium.getStringWidth(desc) + 14;
        float th = 16;
        float tx = (width - tw) / 2f;
        float ty = startY - th - 8;
        Render2DEngine.drawRound(ctx.getMatrices(), tx, ty, tw, th, 5f, GUI_BG);
        FontRenderers.sf_medium.drawString(ctx.getMatrices(), desc,
                tx + 7, ty + th / 2f - 3.5f, Color.WHITE.getRGB());
    }

    private void renderPanel(DrawContext ctx, int x, int y, Module.Category cat, int mx, int my) {
        Render2DEngine.drawRound(ctx.getMatrices(), x, y, PANEL_W, PANEL_H, 10f, GUI_BG);

        String title = cat.getName().toUpperCase();
        float  tw    = FontRenderers.sf_medium.getStringWidth(title);
        FontRenderers.sf_medium.drawString(ctx.getMatrices(), title,
                x + (PANEL_W - tw) / 2f, y + TITLE_MT + 3, TEXT_ENABLED.getRGB());
        Render2DEngine.drawRect(ctx.getMatrices(), x + 6, y + TITLE_MT + 14, PANEL_W - 12, 1,
                new Color(255, 255, 255, 30));

        int   maxScroll = calcMaxScroll(cat);
        float tgt = MathHelper.clamp(scrollTgt.get(cat), 0, maxScroll);
        float off = MathHelper.clamp(scrollOff.get(cat), 0, maxScroll);
        scrollTgt.put(cat, tgt);
        scrollOff.put(cat, off);

        renderScrollbar(ctx, x, y, cat, off);

        Render2DEngine.addWindow(ctx.getMatrices(),
                x, y + SCROLL_Y, x + PANEL_W, y + SCROLL_Y + SCROLL_H, 1f);
        ctx.getMatrices().push();

        float  curY  = y + SCROLL_Y - off;
        String filter = search.text.toLowerCase();

        for (Module mod : Managers.MODULE.getModulesByCategory(cat)) {
            if (!filter.isEmpty() && !mod.getName().toLowerCase().contains(filter)) continue;

            float ep   = expandProg.getOrDefault(mod, mod.isExpanded() ? 1f : 0f);
            float etgt = mod.isExpanded() ? 1f : 0f;
            ep += (etgt - ep) * 0.15f;
            if (Math.abs(etgt - ep) < 0.001f) ep = etgt;
            expandProg.put(mod, ep);

            int settH  = (int)(computeSettH(mod) * ep);
            int totalH = MOD_H + settH;

            if (curY + totalH < y + SCROLL_Y)  { curY += totalH; continue; }
            if (curY > y + PANEL_H) break;

            int accentInt = ColorUtil.getFirstColor();
            Color accent  = new Color(accentInt, true);

            Color gradL = new Color(accent.getRed() / 2 + 50, accent.getGreen() / 2 + 20, accent.getBlue() / 2 + 80, 195);
            Color gradR = new Color(Math.min(255, accent.getRed() / 2 + 80), accent.getGreen() / 2 + 40, Math.min(255, accent.getBlue() / 2 + 110), 195);

            int col1 = mod.isEnabled() ? TEXT_ENABLED.getRGB() : TEXT_NORMAL.getRGB();

            if (mod.isEnabled()) {
                Render2DEngine.renderRoundedGradientRect(ctx.getMatrices(),
                        gradL, gradR, gradL, gradR,
                        x + 4, curY - 1, PANEL_W - 8, totalH - 1, 4f);
            }

            String label = (bindingModule == mod) ? "Binding..." : mod.getName();
            FontRenderers.sf_medium_modules.drawString(ctx.getMatrices(), label,
                    x + 10, curY + 4, col1);

            if (!mod.getSettings().isEmpty()) {
                float ap = arrowProg.getOrDefault(mod, mod.isExpanded() ? 1f : 0f);
                ap += ((mod.isExpanded() ? 1f : 0f) - ap) * 0.15f;
                arrowProg.put(mod, ap);

                int ax = x + PANEL_W - 15;
                int ay = (int)(curY + MOD_H / 2f - 2);
                ctx.getMatrices().push();
                ctx.getMatrices().translate(ax, ay, 0);
                ctx.getMatrices().multiply(
                        new Quaternionf().fromAxisAngleRad(new Vector3f(0, 0, 1),
                                (float) Math.toRadians(90f * ap)));
                FontRenderers.sf_medium_modules.drawString(ctx.getMatrices(),
                        "\u2192", -4, -4, col1);
                ctx.getMatrices().pop();
            }

            if (settH > 0) {
                float setY = curY + MOD_H;
                Render2DEngine.addWindow(ctx.getMatrices(),
                        x + 1, (int) setY, x + PANEL_W - 1, (int)(setY + settH), 1f);
                ctx.getMatrices().push();

                for (Setting<?> s : mod.getSettings()) {
                    if (!s.isVisible()) continue;
                    int sh = getSettH(s);
                    if (sh <= 0) continue;

                    if (isGroup(s)) {
                        boolean ext = isGroupExtended(s);
                        Color lineC = new Color(255, 255, 255, 40);
                        Render2DEngine.drawRect(ctx.getMatrices(), x + 10, (int) setY + 6, PANEL_W - 20, 1, lineC);
                        String gName = s.getName();
                        FontRenderers.sf_bold_micro.drawString(ctx.getMatrices(), gName,
                                x + 12, (int) setY + 1, new Color(180, 180, 190, 210).getRGB());
                        String arrow = ext ? "▼" : "▶";
                        float aw = FontRenderers.sf_bold_micro.getStringWidth(arrow);
                        FontRenderers.sf_bold_micro.drawString(ctx.getMatrices(), arrow,
                                x + PANEL_W - 14 - aw, (int) setY + 1, new Color(180, 180, 190, 210).getRGB());
                    } else if (isColor(s)) {
                        renderColorSwatch(ctx, s, x + 10, (int) setY, PANEL_W - 20, sh);
                    } else if (isBool(s))
                        toggleRend.render(ctx, s, x + 10, (int) setY, PANEL_W - 20, sh);
                    else if (s.isNumberSetting() && s.hasRestriction())
                        sliderRend.render(ctx, s, x + 10, (int) setY - 2, PANEL_W - 20, sh);
                    else if (s.isEnumSetting() && !(s.getValue() instanceof PositionSetting))
                        modeRend.render(ctx, s, x + 10, (int) setY, PANEL_W - 20, sh);

                    setY += sh + 1;
                }

                ctx.getMatrices().pop();
                Render2DEngine.popWindow();
            }
            curY += totalH;
        }

        ctx.getMatrices().pop();
        Render2DEngine.popWindow();
    }

    private void renderColorSwatch(DrawContext ctx, Setting<?> s, int x, int y, int w, int h) {
        ColorSetting cs = (ColorSetting) s.getValue();
        Color c = cs.getColorObject();
        String name = s.getName();
        boolean isOpen = openColorSetting == s;

        int iconSize = 9;
        int iconY = y + (h - iconSize) / 2;
        ctx.drawTexture(COLORS_ICON, x, iconY, iconSize, iconSize, 0, 0, 64, 64, 64, 64);

        FontRenderers.sf_medium_mini.drawString(ctx.getMatrices(), name, x + iconSize + 3, y + 3, TEXT_NORMAL.getRGB());

        if (isOpen)
            Render2DEngine.drawRound(ctx.getMatrices(), x + w - 17f, y + 1.5f, 14, 8, 2f, c);
        else
            Render2DEngine.drawRound(ctx.getMatrices(), x + w - 16, y + 2, 14, 7, 2f, c);
        Render2DEngine.drawBlurredShadow(ctx.getMatrices(), x + w - 16f, y + 2f, 14, 7, isOpen ? 4 : 8, c);
    }

    private void renderScrollbar(DrawContext ctx, int x, int y, Module.Category cat, float off) {
        int max = calcMaxScroll(cat);
        if (max <= 0) return;
        int sbX = x + PANEL_W - 4, sbH = SCROLL_H - 30, sbY = y + SCROLL_Y + 15;
        Render2DEngine.drawRect(ctx.getMatrices(), sbX, sbY, 3, sbH, new Color(0, 0, 0, 50));
        float prog = off / max;
        int   th   = Math.max(6, (int)(sbH * (SCROLL_H / (float)(SCROLL_H + max))));
        int   ty   = sbY + (int)(prog * (sbH - th));
        Render2DEngine.drawRect(ctx.getMatrices(), sbX, ty, 3, th, new Color(255, 255, 255, 150));
    }

    private void renderSearch(DrawContext ctx, int mx, int my) {
        int sx = (width - SEARCH_W) / 2;
        int sy = (height + PANEL_H) / 2 + SEARCH_MB;
        Render2DEngine.drawRound(ctx.getMatrices(), sx, sy, SEARCH_W, SEARCH_H, 6f, GUI_BG);

        String disp; int col; float tx;
        if (search.text.isEmpty() && !search.focused) {
            disp = "Search...";
            col  = new Color(255, 255, 255, 120).getRGB();
            tx   = sx + (SEARCH_W - FontRenderers.sf_medium.getStringWidth(disp)) / 2f;
        } else {
            String t = search.text;
            if (search.focused && search.cursorVisible) {
                int p = Math.min(search.cursorPosition, t.length());
                t = t.substring(0, p) + "|" + t.substring(p);
            }
            disp = t; col = Color.WHITE.getRGB(); tx = sx + 6;
        }
        FontRenderers.sf_medium.drawString(ctx.getMatrices(), disp,
                tx, sy + (SEARCH_H - 8) / 2f, col);
    }

    private void renderThemeButton(DrawContext ctx, int mx, int my) {
        int sx  = (width - SEARCH_W) / 2;
        int sy  = (height + PANEL_H) / 2 + SEARCH_MB;
        int bx  = sx + SEARCH_W + 5, by = sy + 2;
        boolean hov = mx >= bx && mx <= bx + 16 && my >= by && my <= by + 16;
        Color bg = hov ? GUI_BG.brighter() : GUI_BG;
        Render2DEngine.drawRound(ctx.getMatrices(), bx, by, 16, 16, 3f, bg);
        Color accent = new Color(ColorUtil.getFirstColor(), true);
        Render2DEngine.drawRound(ctx.getMatrices(), bx + 3, by + 3, 10, 10, 5f, accent);
    }

    private void renderThemeRow(DrawContext ctx, int mx, int my) {
        int   themeX  = (width - THEME_W) / 2;
        int   themeY  = (height + PANEL_H) / 2 + THEME_MB;
        float offsetY = (1f - themeMenuAnim) * 10f;

        Render2DEngine.drawRound(ctx.getMatrices(), themeX, themeY + offsetY, THEME_W, THEME_H, 3f,
                new Color(GUI_BG.getRed(), GUI_BG.getGreen(), GUI_BG.getBlue(),
                        (int)(220 * themeMenuAnim)));

        int circleSize = THEME_H - 5;
        int pad        = 5;

        List<ThemeManager.Style> styles = ThemeManager.INSTANCE.getStyles();
        float startX = themeX + pad - themeScrollOff;
        int   cy     = (int)(themeY + (THEME_H - circleSize) / 2f + offsetY);

        Render2DEngine.addWindow(ctx.getMatrices(),
                themeX, themeY + offsetY, themeX + THEME_W, themeY + offsetY + THEME_H, 1f);

        for (ThemeManager.Style style : styles) {
            if (startX + circleSize >= themeX && startX <= themeX + THEME_W) {
                int   c1   = style.colors[0];
                int   c2   = style.colors.length > 1 ? style.colors[1] : c1;
                Color col1 = new Color(c1, true);
                Color col2 = new Color(c2, true);
                boolean hov = mx >= startX && mx <= startX + circleSize && my >= cy && my <= cy + circleSize;

                Render2DEngine.renderRoundedGradientRect(ctx.getMatrices(),
                        col1, col1, col2, col2,
                        startX, cy, circleSize, circleSize, (float) circleSize / 2f);

                if (hov) {
                    Render2DEngine.drawRound(ctx.getMatrices(),
                            startX - 1, cy - 1, circleSize + 2, circleSize + 2,
                            (float)(circleSize + 2) / 2f,
                            new Color(255, 255, 255, 80));
                }
                if (style == ThemeManager.INSTANCE.getTheme()) {
                    Render2DEngine.drawRound(ctx.getMatrices(),
                            startX - 1, cy - 1, circleSize + 2, circleSize + 2,
                            (float)(circleSize + 2) / 2f,
                            new Color(255, 255, 255, 160));
                }
            }
            startX += circleSize + pad;
        }
        Render2DEngine.popWindow();
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        int totalW = CATS.size() * (PANEL_W + PANEL_M) - PANEL_M;
        int startX = (width - totalW) / 2;
        int startY = (height - PANEL_H) / 2;
        int idx = 0;
        for (Module.Category cat : CATS) {
            int px = startX + idx++ * (PANEL_W + PANEL_M);
            if (mx >= px && mx <= px + PANEL_W
                    && my >= startY + SCROLL_Y && my <= startY + SCROLL_Y + SCROLL_H) {
                int max = calcMaxScroll(cat);
                scrollTgt.compute(cat, (k, v) -> MathHelper.clamp(v - (float) sy * 12f, 0, max));
                return true;
            }
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    private boolean isInsideColorPicker(double mx, double my) {
        return openColorSetting != null
            && mx >= colorPickerX && mx <= colorPickerX + COLOR_PICKER_W
            && my >= colorPickerY && my <= colorPickerY + COLOR_PICKER_H;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (openColorSetting != null) {
            ColorSetting cs = (ColorSetting) openColorSetting.getValue();
            float px = colorPickerX;
            float py = colorPickerY;
            float sbX = px + 6, sbY = py + 18;
            float sbW = COLOR_PICKER_W - 46, sbH = COLOR_PICKER_H - 36;
            float hueX = px + COLOR_PICKER_W - 36, alphaX = px + COLOR_PICKER_W - 22;
            float btnY = py + COLOR_PICKER_H - 13;

            if (isInsideColorPicker(mx, my)) {
                if (my >= sbY && my <= sbY + sbH) {
                    if (mx >= sbX && mx <= sbX + sbW) { draggingSB = true; return true; }
                    if (mx >= hueX && mx <= hueX + 8) { draggingHue = true; return true; }
                    if (mx >= alphaX && mx <= alphaX + 8) { draggingAlpha = true; return true; }
                }
                if (my >= btnY && my <= btnY + 9) {
                    if (mx >= px + 6 && mx <= px + 40) {
                        acore.aurora.AcoreAurora.copy_color = cs.getColorObject();
                        return true;
                    }
                    if (mx >= px + 44 && mx <= px + 78) {
                        cs.setRainbow(!cs.isRainbow());
                        return true;
                    }
                    if (mx >= px + 82 && mx <= px + 116 && acore.aurora.AcoreAurora.copy_color != null) {
                        cs.setColor(acore.aurora.AcoreAurora.copy_color.getRGB());
                        initColorPicker(cs);
                        return true;
                    }
                }
                return true;
            } else {
                openColorSetting = null;
                return false;
            }
        }

        int totalW  = CATS.size() * (PANEL_W + PANEL_M) - PANEL_M;
        int startX  = (width - totalW) / 2;
        int startY  = (height - PANEL_H) / 2;
        int searchX = (width - SEARCH_W) / 2;
        int searchY = (height + PANEL_H) / 2 + SEARCH_MB;
        int bx = searchX + SEARCH_W + 5, by = searchY + 2;

        if (mx >= bx && mx <= bx + 16 && my >= by && my <= by + 16) {
            themeMenuOpen = !themeMenuOpen; return true;
        }

        if (themeMenuOpen && btn == 0) {
            int   themeX    = (width - THEME_W) / 2;
            int   themeY    = (height + PANEL_H) / 2 + THEME_MB;
            int   circleSize= THEME_H - 5;
            int   pad       = 5;
            int   cy        = themeY + (THEME_H - circleSize) / 2;
            float startX2   = themeX + pad - themeScrollOff;
            for (ThemeManager.Style style : ThemeManager.INSTANCE.getStyles()) {
                if (mx >= startX2 && mx <= startX2 + circleSize && my >= cy && my <= cy + circleSize) {
                    ThemeManager.INSTANCE.setTheme(style); return true;
                }
                startX2 += circleSize + pad;
            }
        }

        if (mx >= searchX && mx <= searchX + SEARCH_W
                && my >= searchY && my <= searchY + SEARCH_H) {
            search.focused = true;
            search.cursorPosition = search.text.length();
            return true;
        } else {
            search.focused = false;
        }

        if (bindingModule != null) {
            bindingModule.setBind(-(btn + 2), true, false);
            bindingModule = null; return true;
        }

        int idx = 0;
        for (Module.Category cat : CATS) {
            int   px     = startX + idx++ * (PANEL_W + PANEL_M);
            float off    = scrollOff.get(cat);
            float curY   = startY + SCROLL_Y - off;
            String filter = search.text.toLowerCase();

            for (Module mod : Managers.MODULE.getModulesByCategory(cat)) {
                if (!filter.isEmpty() && !mod.getName().toLowerCase().contains(filter)) continue;

                float ep    = expandProg.getOrDefault(mod, mod.isExpanded() ? 1f : 0f);
                int settH   = (int)(computeSettH(mod) * ep);
                int totalH  = MOD_H + settH;

                if (curY + totalH < startY + SCROLL_Y) { curY += totalH; continue; }
                if (curY > startY + SCROLL_Y + SCROLL_H) break;

                if (mx >= px && mx <= px + PANEL_W && my >= curY && my <= curY + MOD_H
                        && my >= startY + SCROLL_Y && my <= startY + SCROLL_Y + SCROLL_H) {
                    if (btn == 0) { mod.toggle(); return true; }
                    else if (btn == 1) { mod.setExpanded(!mod.isExpanded()); return true; }
                    else if (btn == 2) { bindingModule = mod; return true; }
                }

                if (settH > 0) {
                    float setY = curY + MOD_H;
                    for (Setting<?> s : mod.getSettings()) {
                        if (!s.isVisible()) continue;
                        int sh = getSettH(s);
                        if (sh <= 0) continue;

                        if (mx >= px && mx <= px + PANEL_W && my >= setY && my <= setY + sh
                                && my >= startY + SCROLL_Y && my <= startY + SCROLL_Y + SCROLL_H) {
                            if (isGroup(s)) {
                                if (btn == 0) toggleGroup(s);
                            } else if (isColor(s)) {
                                if (btn == 0) {
                                    if (openColorSetting == s) {
                                        openColorSetting = null;
                                    } else {
                                        openColorSetting = s;
                                        ColorSetting cs2 = (ColorSetting) s.getValue();
                                        initColorPicker(cs2);
                                        float cpx = px + PANEL_W + 4;
                                        float cpy = (float) setY;
                                        if (cpx + COLOR_PICKER_W > width) cpx = px - COLOR_PICKER_W - 4;
                                        if (cpy + COLOR_PICKER_H > height) cpy = height - COLOR_PICKER_H - 4;
                                        colorPickerX = cpx;
                                        colorPickerY = cpy;
                                    }
                                    return true;
                                }
                            } else if (isBool(s))
                                toggleRend.mouseClicked(s, mx, my, btn, px + 10, (int) setY, PANEL_W - 20, sh);
                            else if (s.isNumberSetting() && s.hasRestriction()) {
                                if (sliderRend.mouseClicked(s, mx, my, btn, px + 10, (int) setY - 2, PANEL_W - 20, sh)) {
                                    draggingSlider  = s;
                                    draggingSliderX = px + 10;
                                    draggingSliderW = PANEL_W - 20;
                                    return true;
                                }
                            } else if (s.isEnumSetting() && !(s.getValue() instanceof PositionSetting))
                                modeRend.mouseClicked(s, mx, my, btn, px + 10, (int) setY, PANEL_W - 20, sh);
                        }
                        setY += sh + 1;
                    }
                }
                curY += totalH;
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (draggingSlider != null && btn == 0) {
            sliderRend.mouseDragged(draggingSlider, mx, draggingSliderX, draggingSliderW);
            return true;
        }
        return super.mouseDragged(mx, my, btn, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        draggingHue = false;
        draggingAlpha = false;
        draggingSB = false;
        if (draggingSlider != null) {
            sliderRend.mouseReleased(draggingSlider);
            draggingSlider = null; return true;
        }
        return super.mouseReleased(mx, my, btn);
    }

    @Override
    public boolean charTyped(char c, int kc) {
        if (search.focused && search.text.length() < 30) {
            search.text = search.text.substring(0, search.cursorPosition)
                        + c + search.text.substring(search.cursorPosition);
            search.cursorPosition++;
            resetScroll(); return true;
        }
        return super.charTyped(c, kc);
    }

    @Override
    public boolean keyPressed(int kc, int sc, int mods) {
        if (bindingModule != null) {
            if (kc == GLFW.GLFW_KEY_ESCAPE || kc == GLFW.GLFW_KEY_DELETE)
                bindingModule.setBind(-1, false, false);
            else
                bindingModule.setBind(kc, false, false);
            bindingModule = null; return true;
        }
        if (search.focused) {
            switch (kc) {
                case GLFW.GLFW_KEY_ESCAPE, GLFW.GLFW_KEY_ENTER -> { search.focused = false; return true; }
                case GLFW.GLFW_KEY_BACKSPACE -> {
                    if (search.cursorPosition > 0) {
                        search.text = search.text.substring(0, search.cursorPosition - 1)
                                    + search.text.substring(search.cursorPosition);
                        search.cursorPosition--;
                        resetScroll();
                    }
                    return true;
                }
                case GLFW.GLFW_KEY_DELETE -> {
                    if (search.cursorPosition < search.text.length()) {
                        search.text = search.text.substring(0, search.cursorPosition)
                                    + search.text.substring(search.cursorPosition + 1);
                        resetScroll();
                    }
                    return true;
                }
                case GLFW.GLFW_KEY_LEFT  -> { if (search.cursorPosition > 0) search.cursorPosition--; return true; }
                case GLFW.GLFW_KEY_RIGHT -> { if (search.cursorPosition < search.text.length()) search.cursorPosition++; return true; }
            }
        }
        if (kc == GLFW.GLFW_KEY_ESCAPE) {
            if (openColorSetting != null) { openColorSetting = null; return true; }
            mc.setScreen(null); return true;
        }
        return super.keyPressed(kc, sc, mods);
    }

    @Override
    public void tick() {
        super.tick();
        long now = System.currentTimeMillis();
        if (now - search.lastCursorBlink >= 500) {
            search.cursorVisible = !search.cursorVisible;
            search.lastCursorBlink = now;
        }
    }

    private void resetScroll() {
        CATS.forEach(c -> { scrollTgt.put(c, 0f); scrollOff.put(c, 0f); });
    }

    private Module getHoveredModule(int mx, int my, int sx, int sy) {
        int idx = 0;
        for (Module.Category cat : CATS) {
            int   px   = sx + idx++ * (PANEL_W + PANEL_M);
            if (mx < px || mx > px + PANEL_W || my < sy || my > sy + PANEL_H) continue;
            float off  = scrollOff.get(cat);
            float curY = sy + SCROLL_Y - off;
            for (Module mod : Managers.MODULE.getModulesByCategory(cat)) {
                if (!search.text.isEmpty() && !mod.getName().toLowerCase().contains(search.text.toLowerCase())) continue;
                float ep   = expandProg.getOrDefault(mod, mod.isExpanded() ? 1f : 0f);
                int totalH = MOD_H + (int)(computeSettH(mod) * ep);
                if (mx >= px && mx <= px + PANEL_W && my >= curY && my <= curY + MOD_H
                        && my >= sy + SCROLL_Y && my <= sy + SCROLL_Y + SCROLL_H) return mod;
                curY += totalH;
            }
        }
        return null;
    }

    private int calcMaxScroll(Module.Category cat) {
        int    total  = 0;
        String filter = search.text.toLowerCase();
        for (Module m : Managers.MODULE.getModulesByCategory(cat)) {
            if (!filter.isEmpty() && !m.getName().toLowerCase().contains(filter)) continue;
            float ep = expandProg.getOrDefault(m, m.isExpanded() ? 1f : 0f);
            total += MOD_H + (int)(computeSettH(m) * ep);
        }
        return Math.max(0, total - SCROLL_H);
    }

    private int computeSettH(Module mod) {
        int h = 0;
        for (Setting<?> s : mod.getSettings()) {
            if (!s.isVisible()) continue;
            h += getSettH(s) + 1;
        }
        return h;
    }

    private int getSettH(Setting<?> s) {
        if (isColor(s)) return 13;
        if (isBool(s)) return toggleRend.getHeight();
        if (s.isNumberSetting() && s.hasRestriction()) return sliderRend.getHeight();
        if (s.isEnumSetting() && !(s.getValue() instanceof PositionSetting))
            return modeRend.getHeight(s, PANEL_W - 20);
        if (s.getValue() instanceof SettingGroup || s.getValue() instanceof BooleanSettingGroup)
            return 14;
        return 0;
    }

    private boolean isColor(Setting<?> s) {
        return s.getValue() instanceof ColorSetting;
    }

    private boolean isGroup(Setting<?> s) {
        return s.getValue() instanceof SettingGroup || s.getValue() instanceof BooleanSettingGroup;
    }

    private boolean isGroupExtended(Setting<?> s) {
        if (s.getValue() instanceof SettingGroup sg) return sg.isExtended();
        if (s.getValue() instanceof BooleanSettingGroup bg) return bg.isExtended();
        return false;
    }

    private void toggleGroup(Setting<?> s) {
        if (s.getValue() instanceof SettingGroup sg) sg.setExtended(!sg.isExtended());
        else if (s.getValue() instanceof BooleanSettingGroup bg) bg.setExtended(!bg.isExtended());
    }

    private boolean isBool(Setting<?> s) {
        return s.getValue() instanceof Boolean
            && !s.getName().equals("Enabled")
            && !s.getName().equals("Drawn");
    }
}
