package acore.aurora.gui.clickui;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
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
import acore.aurora.setting.impl.PositionSetting;
import acore.aurora.utility.color.ColorUtil;
import acore.aurora.utility.render.Render2DEngine;
import acore.aurora.utility.render.ThemeManager;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;
import java.util.List;
import static acore.aurora.features.modules.Module.mc;
public class ClickGUI extends Screen {
    public static boolean anyHovered = false;
    public static boolean close = false;
    public static String currentDescription = "";
    public static class SimpleAnimation { public void reset() {} }
    public final SimpleAnimation imageAnimation = new SimpleAnimation();
    private boolean isClose = false;
    private static final int PANEL_W  = 125;
    private static final int PANEL_H  = 280;
    private static final int PANEL_M  = 8;
    private static final int TITLE_MT = 5;
    private static final int TITLE_H  = 20;
    private static final int MOD_H    = 20;
    private static final int SCROLL_Y = TITLE_MT + TITLE_H;
    private static final int SCROLL_H = PANEL_H - SCROLL_Y - 5;
    private static final int SEARCH_W  = 180;
    private static final int SEARCH_H  = 20;
    private static final int SEARCH_MB = 10;
    private static final int THEME_H   = 16;
    private static final int THEME_MB  = 40;
    private static final int THEME_W   = 180;
    private static final int VISIBLE_T = 11;
    private static final Color GUI_BG = new Color(17, 15, 28, 235);
    private static final List<Module.Category> CATS = Arrays.asList(
        Module.Category.COMBAT, Module.Category.MOVEMENT,
        Module.Category.RENDER, Module.Category.PLAYER, Module.Category.MISC);
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
    private static boolean colorPickerOpen = false;
    private float colorPickerAnim = 0f;
    private float picker1CursorX = 0.5f, picker1CursorY = 0.5f;
    private float picker2CursorX = 0.5f, picker2CursorY = 0.5f;
    private boolean draggingPicker1 = false, draggingPicker2 = false;
    private int selectedColor1 = Color.WHITE.getRGB();
    private int selectedColor2 = Color.WHITE.getRGB();
    private static BufferedImage colorImage = null;
    private final ExosToggleSwitchRenderer toggleRend = new ExosToggleSwitchRenderer();
    private final ExosSliderRenderer       sliderRend = new ExosSliderRenderer();
    private final ExosModeRenderer         modeRend   = new ExosModeRenderer();
    public ClickGUI() {
        super(Text.literal("AcoreAurora"));
        if (scrollOff.isEmpty()) {
            CATS.forEach(c -> { scrollOff.put(c, 0f); scrollTgt.put(c, 0f); });
        }
        loadColorImage();
    }
    private void loadColorImage() {
        try {
            colorImage = javax.imageio.ImageIO.read(
                Objects.requireNonNull(getClass().getClassLoader()
                    .getResourceAsStream("assets/acoreaurora/textures/gui/elements/pick.png")));
        } catch (Exception ignored) {}
    }
    private int getPixelColor(float u, float v) {
        if (colorImage == null) return Color.WHITE.getRGB();
        int x = MathHelper.clamp((int)(u * colorImage.getWidth()),  0, colorImage.getWidth()  - 1);
        int y = MathHelper.clamp((int)(v * colorImage.getHeight()), 0, colorImage.getHeight() - 1);
        return colorImage.getRGB(x, y);
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
        openAnim = java.lang.Math.min(1f, openAnim + delta * 5f);
        if (openAnim < 0.01f) return;
        CATS.forEach(c -> {
            float t = scrollTgt.get(c), o = scrollOff.get(c);
            scrollOff.put(c, o + (t - o) * java.lang.Math.min(1f, 0.12f * 20f * delta));
        });
        themeScrollOff += (themeScrollTgt - themeScrollOff) * 0.18f;
        themeMenuAnim  += ((themeMenuOpen ? 1f : 0f) - themeMenuAnim) * 0.18f;
        colorPickerAnim += ((colorPickerOpen ? 1f : 0f) - colorPickerAnim) * 0.2f;
        int totalW = CATS.size() * (PANEL_W + PANEL_M) - PANEL_M;
        int sx = (width - totalW) / 2;
        int sy = (height - PANEL_H) / 2;
        ctx.getMatrices().push();
        ctx.getMatrices().translate(width / 2f, height / 2f, 0);
        ctx.getMatrices().scale(openAnim, openAnim, 1f);
        ctx.getMatrices().translate(-width / 2f, -height / 2f, 0);
        Render2DEngine.drawRect(ctx.getMatrices(), 0, 0, width, height,
            new Color(0, 0, 0, (int)(110 * openAnim)));
        renderTitle(ctx, sx, sy, totalW);
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
        if (colorPickerAnim > 0.01f) renderColorPicker(ctx, mx, my);
        ctx.getMatrices().pop();
    }
    private void renderTitle(DrawContext ctx, int sx, int sy, int totalW) {
        String name = "AcoreAurora";
        float tw = FontRenderers.sf_bold.getStringWidth(name);
        int tx = (int)(sx + (totalW - tw) / 2f);
        int ty = sy - 22;
        int c1 = ColorUtil.getFirstColor();
        FontRenderers.sf_bold.drawString(ctx.getMatrices(), name, tx, ty, c1);
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
        float tw = FontRenderers.sf_medium.getStringWidth(title);
        FontRenderers.sf_medium.drawString(ctx.getMatrices(), title,
            x + (PANEL_W - tw) / 2f, y + TITLE_MT + 3, Color.WHITE.getRGB());
        int maxScroll = calcMaxScroll(cat);
        float tgt = MathHelper.clamp(scrollTgt.get(cat), 0, maxScroll);
        float off = MathHelper.clamp(scrollOff.get(cat), 0, maxScroll);
        scrollTgt.put(cat, tgt);
        scrollOff.put(cat, off);
        renderScrollbar(ctx, x, y, cat, off);
        Render2DEngine.addWindow(ctx.getMatrices(),
            x, y + SCROLL_Y, x + PANEL_W, y + SCROLL_Y + SCROLL_H, 1f);
        ctx.getMatrices().push();
        float curY = y + SCROLL_Y - off;
        String filter = search.text.toLowerCase();
        for (Module mod : Managers.MODULE.getModulesByCategory(cat)) {
            if (!filter.isEmpty() && !mod.getName().toLowerCase().contains(filter)) continue;
            float ep   = expandProg.getOrDefault(mod, mod.isExpanded() ? 1f : 0f);
            float etgt = mod.isExpanded() ? 1f : 0f;
            ep += (etgt - ep) * 0.15f;
            if (java.lang.Math.abs(etgt - ep) < 0.001f) ep = etgt;
            expandProg.put(mod, ep);
            int settH  = (int)(computeSettH(mod) * ep);
            int totalH = MOD_H + settH;
            if (curY + totalH < y + SCROLL_Y) { curY += totalH; continue; }
            if (curY > y + PANEL_H) break;
            int accentInt = ColorUtil.getFirstColor();
            Color accent  = new Color(accentInt, true);
            int col1 = mod.isOn() ? accentInt : new Color(198, 198, 198).getRGB();
            if (mod.isOn()) {
                Render2DEngine.drawGradientRound(ctx.getMatrices(),
                    x + 4, curY - 1, PANEL_W - 8, totalH - 1, 4f,
                    new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 35),
                    new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 35),
                    new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 35),
                    new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 35));
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
                        (float) java.lang.Math.toRadians(90f * ap)));
                FontRenderers.sf_medium_modules.drawString(ctx.getMatrices(), "\u2192", -4, -4, col1);
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
                    if (isBool(s))
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
    private void renderScrollbar(DrawContext ctx, int x, int y, Module.Category cat, float off) {
        int max = calcMaxScroll(cat);
        if (max <= 0) return;
        int sbX = x + PANEL_W - 4, sbH = SCROLL_H - 30, sbY = y + SCROLL_Y + 15;
        Render2DEngine.drawRect(ctx.getMatrices(), sbX, sbY, 3, sbH, new Color(0, 0, 0, 50));
        float prog = off / max;
        int   th   = java.lang.Math.max(6, (int)(sbH * (SCROLL_H / (float)(SCROLL_H + max))));
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
                int p = java.lang.Math.min(search.cursorPosition, t.length());
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
        Render2DEngine.drawRound(ctx.getMatrices(), bx, by, 16, 16, 3f, hov ? GUI_BG.brighter() : GUI_BG);
        Identifier colorsIcon = Identifier.of("acoreaurora", "textures/gui/elements/colors2.png");
        ctx.drawTexture(colorsIcon, bx + 3, by + 3, 0, 0, 10, 10, 10, 10);
    }
    private void renderThemeRow(DrawContext ctx, int mx, int my) {
        int   themeX  = (width - THEME_W) / 2;
        int   themeY  = (height + PANEL_H) / 2 + THEME_MB;
        float offsetY = (1f - themeMenuAnim) * 10f;
        Render2DEngine.drawRound(ctx.getMatrices(), themeX, themeY + offsetY, THEME_W, THEME_H, 3f,
            new Color(GUI_BG.getRed(), GUI_BG.getGreen(), GUI_BG.getBlue(), (int)(220 * themeMenuAnim)));
        int circleSize = THEME_H - 5;
        int pad        = 5;
        List<ThemeManager.Style> styles = ThemeManager.INSTANCE.getStyles();
        float startX = themeX + pad - themeScrollOff;
        int   cy     = (int)(themeY + (THEME_H - circleSize) / 2f + offsetY);
        Render2DEngine.addWindow(ctx.getMatrices(),
            themeX, themeY + offsetY, themeX + THEME_W, themeY + offsetY + THEME_H, 1f);
        float pipsStartX = startX;
        if (pipsStartX + circleSize >= themeX && pipsStartX <= themeX + THEME_W) {
            boolean pipsHov = mx >= pipsStartX && mx <= pipsStartX + circleSize && my >= cy && my <= cy + circleSize;
            if (selectedColor1 != -1 && selectedColor2 != -1) {
                int c1 = selectedColor1, c2 = selectedColor2;
                Render2DEngine.drawGradientRound(ctx.getMatrices(),
                    pipsStartX, cy + 0.5f, circleSize, circleSize, (float)circleSize / 2f,
                    new Color(c1, true), new Color(c1, true),
                    new Color(c2, true), new Color(c2, true));
            } else {
                Identifier pipsId = Identifier.of("acoreaurora", "textures/gui/elements/pips.png");
                ctx.drawTexture(pipsId,
                    (int)pipsStartX, cy, 0, 0, circleSize, circleSize, circleSize, circleSize);
            }
            if (pipsHov) {
                Render2DEngine.drawRound(ctx.getMatrices(),
                    pipsStartX - 1, cy - 0.5f, circleSize + 2, circleSize + 2,
                    (float)(circleSize + 2) / 2f, new Color(255, 255, 255, 80));
            }
        }
        startX += circleSize + pad;
        for (ThemeManager.Style style : styles) {
            if (startX + circleSize >= themeX && startX <= themeX + THEME_W) {
                int   c1   = style.colors[0];
                int   c2   = style.colors.length > 1 ? style.colors[1] : c1;
                Color col1 = new Color(c1, true);
                Color col2 = new Color(c2, true);
                boolean hov = mx >= startX && mx <= startX + circleSize && my >= cy && my <= cy + circleSize;
                boolean sel = style == ThemeManager.INSTANCE.getTheme();
                Render2DEngine.drawGradientRound(ctx.getMatrices(),
                    startX, cy + 0.5f, circleSize, circleSize, (float)circleSize / 2f,
                    col1, col1, col2, col2);
                if (hov || sel) {
                    Render2DEngine.drawRound(ctx.getMatrices(),
                        startX - 1, cy - 0.5f, circleSize + 2, circleSize + 2,
                        (float)(circleSize + 2) / 2f,
                        new Color(255, 255, 255, sel ? 160 : 80));
                }
            }
            startX += circleSize + pad;
        }
        Render2DEngine.popWindow();
    }
    private void renderColorPicker(DrawContext ctx, int mx, int my) {
        int themeX = (width - THEME_W) / 2;
        int themeY = (height + PANEL_H) / 2 + THEME_MB;
        int circleSize = THEME_H - 5;
        int pad = 5;
        int cy = (int)(themeY + (THEME_H - circleSize) / 2f);
        int fixedX = themeX + pad;
        int fixedY = cy;
        int panelW = 85, panelH = 51;
        float animOffX = (1f - colorPickerAnim) * 30f;
        float animScale = 0.95f + 0.05f * colorPickerAnim;
        float alpha = colorPickerAnim;
        int panelX = (int)(fixedX - panelW - 20 + animOffX);
        int panelY = fixedY - panelH / 2 - 12;
        ctx.getMatrices().push();
        ctx.getMatrices().translate(panelX + panelW / 2f, panelY + panelH / 2f, 0);
        ctx.getMatrices().scale(animScale, animScale, 1f);
        ctx.getMatrices().translate(-panelW / 2f, -panelH / 2f, 0);
        Render2DEngine.drawRound(ctx.getMatrices(), 0, 0, panelW, panelH, 4f,
            new Color(GUI_BG.getRed(), GUI_BG.getGreen(), GUI_BG.getBlue(), (int)(235 * alpha)));
        Identifier pickId = Identifier.of("acoreaurora", "textures/gui/elements/pick.png");
        int p1s = 30, p1x = 5, p1y = 5;
        ctx.drawTexture(pickId, p1x, p1y, 0, 0, p1s, p1s, p1s, p1s);
        Render2DEngine.drawRound(ctx.getMatrices(), p1x, p1y, p1s, p1s, 14f,
            new Color(0, 0, 0, (int)(40 * alpha)));
        int dot1X = (int)(p1x + picker1CursorX * p1s);
        int dot1Y = (int)(p1y + picker1CursorY * p1s);
        Render2DEngine.drawRound(ctx.getMatrices(), dot1X - 2, dot1Y - 2, 4, 4, 2f,
            new Color(0, 0, 0, (int)(200 * alpha)));
        int p2s = 30, p2x = 50, p2y = 5;
        ctx.drawTexture(pickId, p2x, p2y, 0, 0, p2s, p2s, p2s, p2s);
        Render2DEngine.drawRound(ctx.getMatrices(), p2x, p2y, p2s, p2s, 14f,
            new Color(0, 0, 0, (int)(40 * alpha)));
        int dot2X = (int)(p2x + picker2CursorX * p2s);
        int dot2Y = (int)(p2y + picker2CursorY * p2s);
        Render2DEngine.drawRound(ctx.getMatrices(), dot2X - 2, dot2Y - 2, 4, 4, 2f,
            new Color(0, 0, 0, (int)(200 * alpha)));
        int closeX = panelW - 10, closeY = 0;
        Render2DEngine.drawRound(ctx.getMatrices(), closeX, closeY, 10, 10, 3f,
            new Color(255, 255, 255, (int)(200 * alpha)));
        FontRenderers.sf_medium.drawString(ctx.getMatrices(), "x", closeX + 2, closeY + 1,
            new Color(200, 0, 0, (int)(255 * alpha)).getRGB());
        Render2DEngine.drawGradientRound(ctx.getMatrices(), 14, 39, 56, 8, 1f,
            new Color(ColorUtil.getFirstColor(), true),
            new Color(ColorUtil.getFirstColor(), true),
            new Color(ColorUtil.getSecondColor(), true),
            new Color(ColorUtil.getSecondColor(), true));
        FontRenderers.sf_medium_modules.drawString(ctx.getMatrices(), "Add theme", 18, 40,
            new Color(255, 255, 255, (int)(200 * alpha)).getRGB());
        ctx.getMatrices().pop();
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
    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        int totalW  = CATS.size() * (PANEL_W + PANEL_M) - PANEL_M;
        int startX  = (width - totalW) / 2;
        int startY  = (height - PANEL_H) / 2;
        int searchX = (width - SEARCH_W) / 2;
        int searchY = (height + PANEL_H) / 2 + SEARCH_MB;
        int bx = searchX + SEARCH_W + 5, by = searchY + 2;
        if (colorPickerOpen && btn == 0) {
            int themeX = (width - THEME_W) / 2;
            int themeY = (height + PANEL_H) / 2 + THEME_MB;
            int circleSize = THEME_H - 5;
            int pad = 5;
            int cy = (int)(themeY + (THEME_H - circleSize) / 2f);
            int fixedX = themeX + pad;
            int fixedY = cy;
            int panelW = 85, panelH = 51;
            int panelX = fixedX - panelW - 20;
            int panelY = fixedY - panelH / 2 - 12;
            if (mx >= panelX + panelW - 10 && mx <= panelX + panelW && my >= panelY && my <= panelY + 10) {
                colorPickerOpen = false; return true;
            }
            if (mx >= panelX + 5 && mx <= panelX + 35 && my >= panelY + 5 && my <= panelY + 35) {
                float u = (float)(mx - panelX - 5) / 30f;
                float v = (float)(my - panelY - 5) / 30f;
                picker1CursorX = MathHelper.clamp(u, 0, 1);
                picker1CursorY = MathHelper.clamp(v, 0, 1);
                selectedColor1 = getPixelColor(picker1CursorX, picker1CursorY);
                draggingPicker1 = true; return true;
            }
            if (mx >= panelX + 50 && mx <= panelX + 80 && my >= panelY + 5 && my <= panelY + 35) {
                float u = (float)(mx - panelX - 50) / 30f;
                float v = (float)(my - panelY - 5) / 30f;
                picker2CursorX = MathHelper.clamp(u, 0, 1);
                picker2CursorY = MathHelper.clamp(v, 0, 1);
                selectedColor2 = getPixelColor(picker2CursorX, picker2CursorY);
                draggingPicker2 = true; return true;
            }
            if (mx >= panelX + 14 && mx <= panelX + 70 && my >= panelY + 39 && my <= panelY + 47) {
                String base = "Custom";
                String candidate = base;
                int index = 2;
                boolean exists;
                do {
                    exists = false;
                    for (ThemeManager.Style s : ThemeManager.INSTANCE.getStyles()) {
                        if (s.name.equals(candidate)) { exists = true; candidate = base + "-" + index++; break; }
                    }
                } while (exists);
                ThemeManager.Style ns = new ThemeManager.Style(candidate, selectedColor1, selectedColor2);
                ThemeManager.INSTANCE.addCustom(candidate, selectedColor1, selectedColor2);
                ThemeManager.INSTANCE.setTheme(ns);
                colorPickerOpen = false; return true;
            }
        }
        if (mx >= bx && mx <= bx + 16 && my >= by && my <= by + 16) {
            themeMenuOpen = !themeMenuOpen; return true;
        }
        if (themeMenuOpen && btn == 0) {
            int themeX = (width - THEME_W) / 2;
            int themeY = (height + PANEL_H) / 2 + THEME_MB;
            int circleSize = THEME_H - 5;
            int pad = 5;
            int cy = themeY + (THEME_H - circleSize) / 2;
            float curX = themeX + pad - themeScrollOff;
            if (mx >= curX && mx <= curX + circleSize && my >= cy && my <= cy + circleSize) {
                colorPickerOpen = !colorPickerOpen; return true;
            }
            curX += circleSize + pad;
            for (ThemeManager.Style style : ThemeManager.INSTANCE.getStyles()) {
                if (mx >= curX && mx <= curX + circleSize && my >= cy && my <= cy + circleSize) {
                    ThemeManager.INSTANCE.setTheme(style); return true;
                }
                curX += circleSize + pad;
            }
        }
        if (themeMenuOpen && btn == 1) {
            int themeX = (width - THEME_W) / 2;
            int themeY = (height + PANEL_H) / 2 + THEME_MB;
            int circleSize = THEME_H - 5;
            int pad = 5;
            int cy = themeY + (THEME_H - circleSize) / 2;
            float curX = themeX + pad + circleSize + pad - themeScrollOff;
            for (ThemeManager.Style style : new ArrayList<>(ThemeManager.INSTANCE.getStyles())) {
                if (mx >= curX && mx <= curX + circleSize && my >= cy && my <= cy + circleSize) {
                    if (style.name.startsWith("Custom")) ThemeManager.INSTANCE.removeStyle(style);
                    return true;
                }
                curX += circleSize + pad;
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
            int   px   = startX + idx++ * (PANEL_W + PANEL_M);
            float off  = scrollOff.get(cat);
            float curY = startY + SCROLL_Y - off;
            String filter = search.text.toLowerCase();
            for (Module mod : Managers.MODULE.getModulesByCategory(cat)) {
                if (!filter.isEmpty() && !mod.getName().toLowerCase().contains(filter)) continue;
                float ep   = expandProg.getOrDefault(mod, mod.isExpanded() ? 1f : 0f);
                int settH  = (int)(computeSettH(mod) * ep);
                int totalH = MOD_H + settH;
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
                            if (isBool(s))
                                toggleRend.mouseClicked(s, mx, my, btn, px + 10, (int)setY, PANEL_W - 20, sh);
                            else if (s.isNumberSetting() && s.hasRestriction()) {
                                if (sliderRend.mouseClicked(s, mx, my, btn, px + 10, (int)setY - 2, PANEL_W - 20, sh)) {
                                    draggingSlider  = s;
                                    draggingSliderX = px + 10;
                                    draggingSliderW = PANEL_W - 20;
                                    return true;
                                }
                            } else if (s.isEnumSetting() && !(s.getValue() instanceof PositionSetting))
                                modeRend.mouseClicked(s, mx, my, btn, px + 10, (int)setY, PANEL_W - 20, sh);
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
        if (btn == 0 && colorPickerOpen) {
            int themeX = (width - THEME_W) / 2;
            int themeY = (height + PANEL_H) / 2 + THEME_MB;
            int circleSize = THEME_H - 5;
            int pad = 5;
            int cy = (int)(themeY + (THEME_H - circleSize) / 2f);
            int panelW = 85, panelH = 51;
            int panelX = themeX + pad - panelW - 20;
            int panelY = cy - panelH / 2 - 12;
            if (draggingPicker1) {
                float u = (float)(mx - panelX - 5) / 30f;
                float v = (float)(my - panelY - 5) / 30f;
                picker1CursorX = MathHelper.clamp(u, 0, 1);
                picker1CursorY = MathHelper.clamp(v, 0, 1);
                selectedColor1 = getPixelColor(picker1CursorX, picker1CursorY);
                return true;
            }
            if (draggingPicker2) {
                float u = (float)(mx - panelX - 50) / 30f;
                float v = (float)(my - panelY - 5) / 30f;
                picker2CursorX = MathHelper.clamp(u, 0, 1);
                picker2CursorY = MathHelper.clamp(v, 0, 1);
                selectedColor2 = getPixelColor(picker2CursorX, picker2CursorY);
                return true;
            }
        }
        if (draggingSlider != null && btn == 0) {
            sliderRend.mouseDragged(draggingSlider, mx, draggingSliderX, draggingSliderW);
            return true;
        }
        return super.mouseDragged(mx, my, btn, dx, dy);
    }
    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        draggingPicker1 = false;
        draggingPicker2 = false;
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
        if (kc == GLFW.GLFW_KEY_ESCAPE) { mc.setScreen(null); return true; }
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
        int total = 0;
        String filter = search.text.toLowerCase();
        for (Module m : Managers.MODULE.getModulesByCategory(cat)) {
            if (!filter.isEmpty() && !m.getName().toLowerCase().contains(filter)) continue;
            float ep = expandProg.getOrDefault(m, m.isExpanded() ? 1f : 0f);
            total += MOD_H + (int)(computeSettH(m) * ep);
        }
        return java.lang.Math.max(0, total - SCROLL_H);
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
        if (isBool(s)) return toggleRend.getHeight();
        if (s.isNumberSetting() && s.hasRestriction()) return sliderRend.getHeight();
        if (s.isEnumSetting() && !(s.getValue() instanceof PositionSetting))
            return modeRend.getHeight(s, PANEL_W - 20);
        return 0;
    }
    private boolean isBool(Setting<?> s) {
        return s.getValue() instanceof Boolean
            && !s.getName().equals("Enabled")
            && !s.getName().equals("Drawn");
    }
    private static class SearchState {
        String  text            = "";
        boolean focused         = false;
        boolean cursorVisible   = true;
        int     cursorPosition  = 0;
        long    lastCursorBlink = 0;
    }
}