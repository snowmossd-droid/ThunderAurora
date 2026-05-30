package acore.aurora.gui.clickui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import acore.aurora.core.Managers;
import acore.aurora.core.manager.client.ModuleManager;
import acore.aurora.features.modules.Module;
import acore.aurora.features.modules.client.HudEditor;
import acore.aurora.gui.font.FontRenderers;
import acore.aurora.setting.Setting;
import acore.aurora.setting.impl.*;
import acore.aurora.utility.render.Render2DEngine;

import java.awt.*;
import java.util.*;

import static acore.aurora.features.modules.Module.mc;

public class ClickGUI extends Screen {

    public static boolean anyHovered = false;
    public static boolean close = false;
    public static boolean imageDirection = true;
    public final ImageAnimationStub imageAnimation = new ImageAnimationStub();
    public static class ImageAnimationStub { public void reset() {} }

    private boolean isClose = false;

    private static final int PANEL_W  = 125;
    private static final int PANEL_H  = 280;
    private static final int PANEL_M  = 8;
    private static final int TITLE_MT = 5;
    private static final int TITLE_H  = 20;
    private static final int MOD_H    = 20;
    private static final int SCROLL_Y = TITLE_MT + TITLE_H;
    private static final int SCROLL_H = PANEL_H - SCROLL_Y - 5;
    private static final int SEARCH_W = 180;
    private static final int SEARCH_H = 20;
    private static final int SEARCH_MB= 10;
    private static final int THEME_H  = 16;
    private static final int THEME_MB = 40;
    private static final int THEME_W  = 180;
    private static final int VISIBLE_THEMES = 11;

    private static final Color GUI_BG  = new Color(28, 28, 36, 245);
    private static final Color TRACK_OFF = new Color(50, 50, 50, 200);

    private static final List<Module.Category> CATS = Arrays.asList(
        Module.Category.COMBAT, Module.Category.MISC,
        Module.Category.RENDER, Module.Category.MOVEMENT, Module.Category.PLAYER);

    private static final Map<Module.Category, Float> scrollOff = new HashMap<>();
    private static final Map<Module.Category, Float> scrollTgt = new HashMap<>();
    private final Map<Module, Float> expandProg = new HashMap<>();
    private final Map<Module, Float> arrowProg  = new HashMap<>();

    private float openAnim = 0f;

    public static String currentDescription = "";
    private final SearchState search = new SearchState();

    private Module bindingModule = null;
    private Setting<?> draggingSlider = null;
    private int draggingSliderX, draggingSliderW;

    private static boolean themeMenuOpen = false;
    private float themeMenuAnim = 0f;
    private static float themeScrollOff = 0f;
    private static float themeScrollTgt = 0f;

    private final ToggleSwitchRenderer toggleRend = new ToggleSwitchRenderer();
    private final SliderBarRenderer    sliderRend  = new SliderBarRenderer();
    private final ChipModeRenderer     modeRend    = new ChipModeRenderer();

    public ClickGUI() {
        super(Text.literal("AcoreClickGUI"));
        if (scrollOff.isEmpty()) CATS.forEach(c -> { scrollOff.put(c, 0f); scrollTgt.put(c, 0f); });
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

        Render2DEngine.drawRect(ctx.getMatrices(), 0, 0, width, height, new Color(0, 0, 0, (int)(110 * openAnim)));

        int idx = 0;
        for (Module.Category cat : CATS) renderPanel(ctx, sx + idx++ * (PANEL_W + PANEL_M), sy, cat, mx, my);

        Module hovered = getHoveredModule(mx, my, sx, sy);
        if (hovered != null && !hovered.getDescription().isEmpty())
            drawDescription(ctx, hovered.getDescription(), sy);

        DescriptionRenderQueue.renderAll(ctx);
        renderSearch(ctx, mx, my);
        renderThemeButton(ctx, mx, my);
        if (themeMenuAnim > 0.01f) renderThemeRow(ctx, mx, my);

        ctx.getMatrices().pop();
    }

    private void drawDescription(DrawContext ctx, String desc, int startY) {
        float tw = FontRenderers.sf_medium.getStringWidth(desc) + 14;
        float th = 16;
        float tx = (width - tw) / 2f;
        float ty = startY - th - 8;
        Render2DEngine.drawRound(ctx.getMatrices(), tx, ty, tw, th, 5f, GUI_BG);
        FontRenderers.sf_medium.drawString(ctx.getMatrices(), desc, tx + 7, ty + th / 2f - 3.5f, Color.WHITE.getRGB());
    }

    private void renderPanel(DrawContext ctx, int x, int y, Module.Category cat, int mx, int my) {
        Render2DEngine.drawRound(ctx.getMatrices(), x, y, PANEL_W, PANEL_H, 10f, GUI_BG);

        String title = cat.getName().toUpperCase();
        float tw = FontRenderers.sf_medium.getStringWidth(title);
        FontRenderers.sf_medium.drawString(ctx.getMatrices(), title, x + (PANEL_W - tw) / 2f, y + TITLE_MT + 3, Color.WHITE.getRGB());

        int maxScroll = calcMaxScroll(cat);
        float tgt = MathHelper.clamp(scrollTgt.get(cat), 0, maxScroll);
        float off = MathHelper.clamp(scrollOff.get(cat), 0, maxScroll);
        scrollTgt.put(cat, tgt); scrollOff.put(cat, off);

        renderScrollbar(ctx, x, y, cat, off);

        Render2DEngine.addWindow(ctx.getMatrices(), x, y + SCROLL_Y, x + PANEL_W, y + SCROLL_Y + SCROLL_H, 1f);
        ctx.getMatrices().push();

        float curY = y + SCROLL_Y - off;
        String filter = search.text.toLowerCase();

        for (Module mod : Managers.MODULE.getModulesByCategory(cat)) {
            if (!filter.isEmpty() && !mod.getName().toLowerCase().contains(filter)) continue;

            float ep = expandProg.getOrDefault(mod, mod.isExpanded() ? 1f : 0f);
            float etgt = mod.isExpanded() ? 1f : 0f;
            ep += (etgt - ep) * 0.15f;
            if (Math.abs(etgt - ep) < 0.001f) ep = etgt;
            expandProg.put(mod, ep);

            int settH = (int)(computeSettH(mod) * ep);
            int totalH = MOD_H + settH;

            if (curY + totalH < y + SCROLL_Y) { curY += totalH; continue; }
            if (curY > y + PANEL_H) break;

            Color accent = getAccent();
            int col1 = mod.isEnabled() ? accent.getRGB() : new Color(198, 198, 198).getRGB();

            if (mod.isEnabled())
                Render2DEngine.drawRect(ctx.getMatrices(), x + 4, curY - 1, PANEL_W - 8, totalH - 1,
                        new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 35));

            String label = (bindingModule == mod) ? "Binding..." : mod.getName();
            FontRenderers.sf_medium_modules.drawString(ctx.getMatrices(), label, x + 10, curY + 4, col1);

            if (!mod.getSettings().isEmpty()) {
                float ap = arrowProg.getOrDefault(mod, mod.isExpanded() ? 1f : 0f);
                ap += ((mod.isExpanded() ? 1f : 0f) - ap) * 0.15f;
                arrowProg.put(mod, ap);

                int ax = x + PANEL_W - 15, ay = (int)(curY + MOD_H / 2f - 2);
                ctx.getMatrices().push();
                ctx.getMatrices().translate(ax, ay, 0);
                ctx.getMatrices().multiply(new Quaternionf().fromAxisAngleRad(new Vector3f(0, 0, 1), (float)Math.toRadians(90f * ap)));
                FontRenderers.sf_medium_modules.drawString(ctx.getMatrices(), "\u2192", -4, -4, col1);
                ctx.getMatrices().pop();
            }

            if (settH > 0) {
                float setY = curY + MOD_H;
                Render2DEngine.addWindow(ctx.getMatrices(), x + 1, (int)setY, x + PANEL_W - 1, (int)(setY + settH), 1f);
                ctx.getMatrices().push();

                for (Setting<?> s : mod.getSettings()) {
                    if (!s.isVisible()) continue;
                    int sh = getSettH(s);
                    if (sh <= 0) continue;
                    if (s.getValue() instanceof Boolean && !s.getName().equals("Enabled") && !s.getName().equals("Drawn"))
                        toggleRend.render(ctx, s, x + 10, (int)setY, PANEL_W - 20, sh);
                    else if (s.isNumberSetting() && s.hasRestriction())
                        sliderRend.render(ctx, s, x + 10, (int)setY - 2, PANEL_W - 20, sh);
                    else if (s.isEnumSetting() && !(s.getValue() instanceof PositionSetting))
                        modeRend.render(ctx, s, x + 10, (int)setY, PANEL_W - 20, sh);
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
        Render2DEngine.drawRect(ctx.getMatrices(), sbX, sbY, 3, sbH, new Color(0,0,0,50));
        float prog = off / max;
        int th = Math.max(6, (int)(sbH * (SCROLL_H / (float)(SCROLL_H + max))));
        int ty = sbY + (int)(prog * (sbH - th));
        Render2DEngine.drawRect(ctx.getMatrices(), sbX, ty, 3, th, new Color(255,255,255,150));
    }

    private void renderSearch(DrawContext ctx, int mx, int my) {
        int sx = (width - SEARCH_W) / 2, sy = (height + PANEL_H) / 2 + SEARCH_MB;
        Render2DEngine.drawRound(ctx.getMatrices(), sx, sy, SEARCH_W, SEARCH_H, 6f, GUI_BG);

        String disp; int col; float tx;
        if (search.text.isEmpty() && !search.focused) {
            disp = "Search..."; col = new Color(255,255,255,120).getRGB();
            tx = sx + (SEARCH_W - FontRenderers.sf_medium.getStringWidth(disp)) / 2f;
        } else {
            String t = search.text;
            if (search.focused && search.cursorVisible) { int p = Math.min(search.cursorPosition, t.length()); t = t.substring(0,p)+"|"+t.substring(p); }
            disp = t; col = Color.WHITE.getRGB(); tx = sx + 6;
        }
        FontRenderers.sf_medium.drawString(ctx.getMatrices(), disp, tx, sy + (SEARCH_H - 8) / 2f, col);
    }

    private void renderThemeButton(DrawContext ctx, int mx, int my) {
        int sx = (width - SEARCH_W) / 2, sy = (height + PANEL_H) / 2 + SEARCH_MB;
        int bx = sx + SEARCH_W + 5, by = sy + 2;
        boolean hov = mx >= bx && mx <= bx + 16 && my >= by && my <= by + 16;
        Color bg = hov ? GUI_BG.brighter() : GUI_BG;
        Render2DEngine.drawRound(ctx.getMatrices(), bx, by, 16, 16, 3f, bg);
        Color accent = getAccent();
        Render2DEngine.drawRound(ctx.getMatrices(), bx + 3, by + 3, 10, 10, 5f, accent);
    }

    private void renderThemeRow(DrawContext ctx, int mx, int my) {
        int themeX = (width - THEME_W) / 2;
        int themeY = (height + PANEL_H) / 2 + THEME_MB;
        float offsetY = (1f - themeMenuAnim) * 10f;

        Render2DEngine.drawRound(ctx.getMatrices(), themeX, themeY + offsetY, THEME_W, THEME_H,
                3f, new Color(GUI_BG.getRed(), GUI_BG.getGreen(), GUI_BG.getBlue(), (int)(220 * themeMenuAnim)));

        int circleSize = THEME_H - 5;
        int pad = 5;
        Color[] presets = {
            new Color(200, 60, 60), new Color(60, 140, 200), new Color(60, 180, 80),
            new Color(180, 100, 200), new Color(200, 140, 40), new Color(40, 180, 180),
            new Color(200, 80, 140), new Color(100, 100, 200), new Color(180, 180, 60),
            new Color(200, 120, 60), new Color(60, 200, 140)
        };

        float startX = themeX + pad - themeScrollOff;
        int cy = (int)(themeY + (THEME_H - circleSize) / 2f + offsetY);

        Render2DEngine.addWindow(ctx.getMatrices(), themeX, themeY + offsetY, themeX + THEME_W, themeY + offsetY + THEME_H, 1f);
        for (Color c : presets) {
            if (startX + circleSize >= themeX && startX <= themeX + THEME_W) {
                boolean hov = mx >= startX && mx <= startX + circleSize && my >= cy && my <= cy + circleSize;
                Render2DEngine.drawRound(ctx.getMatrices(), startX, cy, circleSize, circleSize, (float)circleSize / 2f, c);
                if (hov) Render2DEngine.drawRound(ctx.getMatrices(), startX - 1, cy - 1, circleSize + 2, circleSize + 2, (float)(circleSize + 2) / 2f,
                        new Color(255, 255, 255, 80));
            }
            startX += circleSize + pad;
        }
        Render2DEngine.popWindow();
    }

    private Color getAccent() {
        Color raw = HudEditor.getColor(0);
        return (raw.getAlpha() < 10 || (raw.getRed() < 5 && raw.getGreen() < 5 && raw.getBlue() < 5))
                ? new Color(180, 60, 60) : raw;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        int totalW = CATS.size() * (PANEL_W + PANEL_M) - PANEL_M;
        int startX = (width - totalW) / 2;
        int startY = (height - PANEL_H) / 2;
        int idx = 0;
        for (Module.Category cat : CATS) {
            int px = startX + idx++ * (PANEL_W + PANEL_M);
            if (mx >= px && mx <= px + PANEL_W && my >= startY + SCROLL_Y && my <= startY + SCROLL_Y + SCROLL_H) {
                int max = calcMaxScroll(cat);
                scrollTgt.compute(cat, (k, v) -> MathHelper.clamp(v - (float)sy * 12f, 0, max));
                return true;
            }
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        int totalW = CATS.size() * (PANEL_W + PANEL_M) - PANEL_M;
        int startX = (width - totalW) / 2;
        int startY = (height - PANEL_H) / 2;
        int searchX = (width - SEARCH_W) / 2, searchY = (height + PANEL_H) / 2 + SEARCH_MB;
        int bx = searchX + SEARCH_W + 5, by = searchY + 2;

        if (mx >= bx && mx <= bx + 16 && my >= by && my <= by + 16) {
            themeMenuOpen = !themeMenuOpen; return true;
        }

        if (themeMenuOpen && btn == 0) {
            int themeX = (width - THEME_W) / 2, themeY = (height + PANEL_H) / 2 + THEME_MB;
            int circleSize = THEME_H - 5, pad = 5;
            Color[] presets = {
                new Color(200,60,60), new Color(60,140,200), new Color(60,180,80),
                new Color(180,100,200), new Color(200,140,40), new Color(40,180,180),
                new Color(200,80,140), new Color(100,100,200), new Color(180,180,60),
                new Color(200,120,60), new Color(60,200,140)
            };
            int cy = themeY + (THEME_H - circleSize) / 2;
            float startX2 = themeX + pad - themeScrollOff;
            for (Color c : presets) {
                if (mx >= startX2 && mx <= startX2 + circleSize && my >= cy && my <= cy + circleSize) {
                    setAccentColor(c); return true;
                }
                startX2 += circleSize + pad;
            }
        }

        if (mx >= searchX && mx <= searchX + SEARCH_W && my >= searchY && my <= searchY + SEARCH_H) {
            search.focused = true; search.cursorPosition = search.text.length(); return true;
        } else search.focused = false;

        if (bindingModule != null) {
            bindingModule.setBind(-(btn + 2), true, false);
            bindingModule = null; return true;
        }

        int idx = 0;
        for (Module.Category cat : CATS) {
            int px = startX + idx++ * (PANEL_W + PANEL_M);
            float off = scrollOff.get(cat);
            float curY = startY + SCROLL_Y - off;
            String filter = search.text.toLowerCase();

            for (Module mod : Managers.MODULE.getModulesByCategory(cat)) {
                if (!filter.isEmpty() && !mod.getName().toLowerCase().contains(filter)) continue;
                float ep = expandProg.getOrDefault(mod, mod.isExpanded() ? 1f : 0f);
                int settH = (int)(computeSettH(mod) * ep);
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
                            if (s.getValue() instanceof Boolean && !s.getName().equals("Enabled") && !s.getName().equals("Drawn"))
                                toggleRend.mouseClicked(s, mx, my, btn, px+10, (int)setY, PANEL_W-20, sh);
                            else if (s.isNumberSetting() && s.hasRestriction()) {
                                if (sliderRend.mouseClicked(s, mx, my, btn, px+10, (int)setY-2, PANEL_W-20, sh)) {
                                    draggingSlider = s; draggingSliderX = px+10; draggingSliderW = PANEL_W-20; return true;
                                }
                            } else if (s.isEnumSetting() && !(s.getValue() instanceof PositionSetting))
                                modeRend.mouseClicked(s, mx, my, btn, px+10, (int)setY, PANEL_W-20, sh);
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
        if (draggingSlider != null && btn == 0) { sliderRend.mouseDragged(draggingSlider, mx, draggingSliderX, draggingSliderW); return true; }
        return super.mouseDragged(mx, my, btn, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        if (draggingSlider != null) { sliderRend.mouseReleased(draggingSlider); draggingSlider = null; return true; }
        return super.mouseReleased(mx, my, btn);
    }

    @Override
    public boolean charTyped(char c, int kc) {
        if (search.focused && search.text.length() < 30) {
            search.text = search.text.substring(0, search.cursorPosition) + c + search.text.substring(search.cursorPosition);
            search.cursorPosition++; resetScroll(); return true;
        }
        return super.charTyped(c, kc);
    }

    @Override
    public boolean keyPressed(int kc, int sc, int mods) {
        if (bindingModule != null) {
            if (kc == GLFW.GLFW_KEY_ESCAPE || kc == GLFW.GLFW_KEY_DELETE) bindingModule.setBind(-1, false, false);
            else bindingModule.setBind(kc, false, false);
            bindingModule = null; return true;
        }
        if (search.focused) {
            switch (kc) {
                case GLFW.GLFW_KEY_ESCAPE, GLFW.GLFW_KEY_ENTER -> { search.focused = false; return true; }
                case GLFW.GLFW_KEY_BACKSPACE -> { if (search.cursorPosition > 0) { search.text = search.text.substring(0, search.cursorPosition-1) + search.text.substring(search.cursorPosition); search.cursorPosition--; resetScroll(); } return true; }
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
        if (now - search.lastCursorBlink >= 500) { search.cursorVisible = !search.cursorVisible; search.lastCursorBlink = now; }
    }

    private void setAccentColor(Color c) {
        try {
            var field = ModuleManager.hudEditor.getClass().getDeclaredField("color");
            field.setAccessible(true);
        } catch (Exception ignored) {}
    }

    private void resetScroll() { CATS.forEach(c -> { scrollTgt.put(c, 0f); scrollOff.put(c, 0f); }); }

    private Module getHoveredModule(int mx, int my, int sx, int sy) {
        int idx = 0;
        for (Module.Category cat : CATS) {
            int px = sx + idx++ * (PANEL_W + PANEL_M);
            if (mx < px || mx > px + PANEL_W || my < sy || my > sy + PANEL_H) continue;
            float off = scrollOff.get(cat);
            float curY = sy + SCROLL_Y - off;
            for (Module mod : Managers.MODULE.getModulesByCategory(cat)) {
                if (!search.text.isEmpty() && !mod.getName().toLowerCase().contains(search.text.toLowerCase())) continue;
                float ep = expandProg.getOrDefault(mod, mod.isExpanded() ? 1f : 0f);
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
        return Math.max(0, total - SCROLL_H);
    }

    private int computeSettH(Module mod) {
        int h = 0;
        for (Setting<?> s : mod.getSettings()) { if (!s.isVisible()) continue; h += getSettH(s) + 1; }
        return h;
    }

    private int getSettH(Setting<?> s) {
        if (s.getValue() instanceof Boolean && !s.getName().equals("Enabled") && !s.getName().equals("Drawn")) return 16;
        if (s.isNumberSetting() && s.hasRestriction()) return 20;
        if (s.isEnumSetting() && !(s.getValue() instanceof PositionSetting)) return modeRend.getHeight(s, PANEL_W - 20);
        return 0;
    }
}
