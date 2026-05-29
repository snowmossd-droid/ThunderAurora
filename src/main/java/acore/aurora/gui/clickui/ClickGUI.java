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
import acore.aurora.features.modules.client.ClickGui;
import acore.aurora.features.modules.client.HudEditor;
import acore.aurora.gui.font.FontRenderers;
import acore.aurora.setting.Setting;
import acore.aurora.setting.impl.*;
import acore.aurora.utility.render.Render2DEngine;

import java.awt.*;
import java.util.*;
import static acore.aurora.features.modules.Module.mc;

public class ClickGUI extends Screen {

    private static final int PANEL_W      = 125;
    private static final int PANEL_H      = 280;
    private static final int PANEL_MARGIN = 8;
    private static final int TITLE_H      = 22;
    private static final int MODULE_H     = 20;
    private static final int SCROLL_Y     = TITLE_H;
    private static final int SCROLL_H     = PANEL_H - SCROLL_Y - 5;
    private static final int SEARCH_W     = 180;
    private static final int SEARCH_H     = 20;
    private static final int SEARCH_MB    = 10;

    private static final Set<Module.Category> CATS = new java.util.LinkedHashSet<>(Arrays.asList(
            Module.Category.COMBAT, Module.Category.MISC,
            Module.Category.RENDER, Module.Category.MOVEMENT, Module.Category.PLAYER));

    private static final Map<Module.Category, Float> scrollOff = new HashMap<>();
    private static final Map<Module.Category, Float> scrollTgt = new HashMap<>();
    private final Map<Module, Float> expandAnim   = new HashMap<>();
    private final Map<Module, Float> arrowAnim    = new HashMap<>();

    public static boolean anyHovered = false;
    public static boolean close = false;
    public static boolean imageDirection = true;
    public final ImageAnimationStub imageAnimation = new ImageAnimationStub();

    public static class ImageAnimationStub { public void reset() {} }

    private float openAnim = 0f;
    private boolean closing = false;

    public static String currentDescription = "";
    private final SearchState search = new SearchState();

    private Module bindingModule = null;

    private Setting<?> draggingSlider = null;
    private int draggingSliderX, draggingSliderW;

    private static final ToggleSwitchRenderer  boolRend   = new ToggleSwitchRenderer();
    private static final SliderBarRenderer   sliderRend = new SliderBarRenderer();
    private static final ChipModeRenderer     modeRend   = new ChipModeRenderer();

    public ClickGUI() {
        super(Text.literal("AcoreClickGUI"));
        if (scrollOff.isEmpty()) {
            CATS.forEach(c -> { scrollOff.put(c, 0f); scrollTgt.put(c, 0f); });
        }
    }

    public static ClickGUI getClickGui() { return new ClickGUI(); }

    @Override public boolean shouldPause() { return false; }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        openAnim += (1f - openAnim) * 0.18f;
        if (closing) { openAnim += (0f - openAnim) * 0.22f; if (openAnim < 0.02f) { super.close(); return; } }
        if (openAnim < 0.01f) return;

        for (Module.Category c : CATS) {
            float t = scrollTgt.get(c), o = scrollOff.get(c);
            scrollOff.put(c, o + (t - o) * Math.min(1f, 0.1f * 20f * delta));
        }

        int totalW = CATS.size() * (PANEL_W + PANEL_MARGIN) - PANEL_MARGIN;
        int sx = (width - totalW) / 2;
        int sy = (height - PANEL_H) / 2;

        ctx.getMatrices().push();
        ctx.getMatrices().translate(width / 2f, height / 2f, 0);
        ctx.getMatrices().scale(openAnim, openAnim, 1f);
        ctx.getMatrices().translate(-width / 2f, -height / 2f, 0);

        Render2DEngine.drawRect(ctx.getMatrices(), 0, 0, width, height, new Color(0, 0, 0, (int)(120 * openAnim)));

        int idx = 0;
        for (Module.Category cat : CATS) renderPanel(ctx, sx + idx++ * (PANEL_W + PANEL_MARGIN), sy, cat, mx, my);

        renderSearch(ctx, mx, my);
        DescriptionRenderQueue.renderAll(ctx);
        ctx.getMatrices().pop();
    }

    private void renderPanel(DrawContext ctx, int x, int y, Module.Category cat, int mx, int my) {
        Color bg = new Color(22, 22, 28, 235);
        Render2DEngine.drawRound(ctx.getMatrices(), x, y, PANEL_W, PANEL_H, 10f, bg);

        String title = cat.getName().toUpperCase();
        float tw = FontRenderers.sf_medium.getStringWidth(title);
        FontRenderers.sf_medium.drawString(ctx.getMatrices(), title, x + (PANEL_W - tw) / 2f, y + 5, Color.WHITE.getRGB());

        renderScrollbar(ctx, x, y, cat);
        Render2DEngine.addWindow(ctx.getMatrices(), x, y + SCROLL_Y, x + PANEL_W, y + SCROLL_Y + SCROLL_H, 1f);
        ctx.getMatrices().push();

        float curY = y + SCROLL_Y - scrollOff.get(cat);
        String filter = search.text.toLowerCase();

        for (Module mod : Managers.MODULE.getModulesByCategory(cat)) {
            if (!filter.isEmpty() && !mod.getName().toLowerCase().contains(filter)) continue;

            float expandPrg = expandAnim.getOrDefault(mod, mod.isExpanded() ? 1f : 0f);
            float tgt = mod.isExpanded() ? 1f : 0f;
            expandPrg += (tgt - expandPrg) * 0.15f;
            if (Math.abs(tgt - expandPrg) < 0.001f) expandPrg = tgt;
            expandAnim.put(mod, expandPrg);

            int settH = (int)(computeSettingsH(mod) * expandPrg);
            int totalH = MODULE_H + settH;

            if (curY + totalH < y + SCROLL_Y || curY > y + PANEL_H) { curY += totalH; continue; }

            Color accent = HudEditor.getColor(0);
            if (accent.getAlpha() < 10 || (accent.getRed() < 5 && accent.getGreen() < 5 && accent.getBlue() < 5))
                accent = new Color(180, 60, 60);

            int col = mod.isEnabled() ? accent.getRGB() : new Color(198, 198, 198).getRGB();

            if (mod.isEnabled()) {
                Render2DEngine.drawRect(ctx.getMatrices(), x + 4, curY, PANEL_W - 8, (float)totalH, new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 25));
            }

            float arPrg = arrowAnim.getOrDefault(mod, mod.isExpanded() ? 1f : 0f);
            arPrg += ((mod.isExpanded() ? 1f : 0f) - arPrg) * 0.15f;
            arrowAnim.put(mod, arPrg);

            FontRenderers.sf_medium_modules.drawString(ctx.getMatrices(), mod.getName(), x + 10, curY + 4, col);

            if (!mod.getSettings().isEmpty()) {
                int arX = x + PANEL_W - 14; int arY = (int)(curY + MODULE_H / 2f - 3);
                ctx.getMatrices().push();
                ctx.getMatrices().translate(arX, arY, 0);
                float angle = (float)Math.toRadians(90f * arPrg);
                ctx.getMatrices().multiply(new Quaternionf().fromAxisAngleRad(new Vector3f(0,0,1), angle));
                FontRenderers.sf_medium_modules.drawString(ctx.getMatrices(), ">", -3, -4, col);
                ctx.getMatrices().pop();
            }

            if (settH > 0) {
                float setY = curY + MODULE_H;
                Render2DEngine.addWindow(ctx.getMatrices(), x + 1, (int)setY, x + PANEL_W - 1, (int)(setY + settH), 1f);
                ctx.getMatrices().push();
                for (Setting<?> s : mod.getSettings()) {
                    if (!s.isVisible()) continue;
                    int sh = getSettH(s);
                    if (s.getValue() instanceof Boolean && !s.getName().equals("Enabled") && !s.getName().equals("Drawn")) {
                        boolRend.render(ctx, s, x + 10, (int)setY, PANEL_W - 20, sh);
                    } else if (s.isNumberSetting() && s.hasRestriction()) {
                        sliderRend.render(ctx, s, x + 10, (int)setY - 2, PANEL_W - 20, sh);
                    } else if (s.isEnumSetting() && !(s.getValue() instanceof PositionSetting)) {
                        modeRend.render(ctx, s, x + 10, (int)setY, PANEL_W - 20, sh);
                    }
                    setY += sh + 1;
                }
                ctx.getMatrices().pop();
                Render2DEngine.popWindow();
            }

            double scale = mc.getWindow().getScaleFactor();
            double mxD = mc.mouse.getX() / scale, myD = mc.mouse.getY() / scale;
            if (mxD >= x && mxD <= x + PANEL_W && myD >= curY && myD <= curY + MODULE_H
                    && mod.getDescription() != null && !mod.getDescription().isEmpty()) {
                DescriptionRenderQueue.add(mod.getDescription(), (float)mxD + 6, (float)myD + 6);
            }

            curY += totalH;
        }

        ctx.getMatrices().pop();
        Render2DEngine.popWindow();
    }

    private void renderScrollbar(DrawContext ctx, int x, int y, Module.Category cat) {
        int max = calcMaxScroll(cat);
        if (max <= 0) return;
        float off = scrollOff.get(cat);
        int sbX = x + PANEL_W - 4; int sbH = SCROLL_H - 20; int sbY = y + SCROLL_Y + 10;
        Render2DEngine.drawRect(ctx.getMatrices(), sbX, sbY, 2, sbH, new Color(0,0,0,50));
        float prg = off / max;
        int th = Math.max(6, (int)(sbH * (SCROLL_H / (float)(SCROLL_H + max))));
        int ty = sbY + (int)(prg * (sbH - th));
        Render2DEngine.drawRect(ctx.getMatrices(), sbX, ty, 2, th, new Color(255,255,255,140));
    }

    private void renderSearch(DrawContext ctx, int mx, int my) {
        int sx = (width - SEARCH_W) / 2;
        int sy = (height + PANEL_H) / 2 + SEARCH_MB;
        Render2DEngine.drawRound(ctx.getMatrices(), sx, sy, SEARCH_W, SEARCH_H, 6f, new Color(22, 22, 28, 235));

        String disp; int col; float tx;
        if (search.text.isEmpty() && !search.focused) {
            disp = "Search..."; col = new Color(255,255,255,100).getRGB();
            tx = sx + (SEARCH_W - FontRenderers.sf_medium.getStringWidth(disp)) / 2f;
        } else {
            String t = search.text;
            if (search.focused && search.cursorVisible) { int p = Math.min(search.cursorPosition, t.length()); t = t.substring(0,p)+"|"+t.substring(p); }
            disp = t; col = Color.WHITE.getRGB(); tx = sx + 6;
        }
        FontRenderers.sf_medium.drawString(ctx.getMatrices(), disp, tx, sy + (SEARCH_H - 8) / 2f, col);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        int totalW = CATS.size() * (PANEL_W + PANEL_MARGIN) - PANEL_MARGIN;
        int startX = (width - totalW) / 2;
        int startY = (height - PANEL_H) / 2;
        int idx = 0;
        for (Module.Category cat : CATS) {
            int px = startX + idx++ * (PANEL_W + PANEL_MARGIN);
            if (mx >= px && mx <= px + PANEL_W && my >= startY + SCROLL_Y && my <= startY + SCROLL_Y + SCROLL_H) {
                int max = calcMaxScroll(cat);
                if (max > 0) {
                    float newTarget = scrollTgt.get(cat) - (float)sy * 8f;
                    newTarget = Math.max(0, Math.min(max, newTarget));
                    scrollTgt.put(cat, newTarget);
                    return true;
                }
                return true;
            }
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        int totalW = CATS.size() * (PANEL_W + PANEL_MARGIN) - PANEL_MARGIN;
        int startX = (width - totalW) / 2;
        int startY = (height - PANEL_H) / 2;
        int searchX = (width - SEARCH_W) / 2;
        int searchY = (height + PANEL_H) / 2 + SEARCH_MB;

        if (mx >= searchX && mx <= searchX + SEARCH_W && my >= searchY && my <= searchY + SEARCH_H) {
            search.focused = true; search.cursorPosition = search.text.length(); return true;
        } else search.focused = false;

        if (bindingModule != null) {
            if (btn == 0) bindingModule.setBind(-(btn + 2), true, false);
            bindingModule = null; return true;
        }

        int idx = 0;
        for (Module.Category cat : CATS) {
            int px = startX + idx++ * (PANEL_W + PANEL_MARGIN);
            float off = scrollOff.get(cat);
            float curY = startY + SCROLL_Y - off;
            String filter = search.text.toLowerCase();

            for (Module mod : Managers.MODULE.getModulesByCategory(cat)) {
                if (!filter.isEmpty() && !mod.getName().toLowerCase().contains(filter)) continue;

                float ep = expandAnim.getOrDefault(mod, mod.isExpanded() ? 1f : 0f);
                int settH = (int)(computeSettingsH(mod) * ep);
                int totalH = MODULE_H + settH;

                if (curY + totalH < startY + SCROLL_Y) { curY += totalH; continue; }
                if (curY > startY + SCROLL_Y + SCROLL_H) break;

                if (mx >= px && mx <= px + PANEL_W && my >= curY && my <= curY + MODULE_H
                        && my >= startY + SCROLL_Y && my <= startY + SCROLL_Y + SCROLL_H) {
                    if (btn == 0) { mod.toggle(); return true; }
                    else if (btn == 1) { mod.setExpanded(!mod.isExpanded()); return true; }
                    else if (btn == 2) { bindingModule = mod; return true; }
                }

                if (settH > 0) {
                    float setY = curY + MODULE_H;
                    for (Setting<?> s : mod.getSettings()) {
                        if (!s.isVisible()) continue;
                        int sh = getSettH(s);
                        if (mx >= px && mx <= px + PANEL_W && my >= setY && my <= setY + sh
                                && my >= startY + SCROLL_Y && my <= startY + SCROLL_Y + SCROLL_H) {
                            if (s.getValue() instanceof Boolean && !s.getName().equals("Enabled") && !s.getName().equals("Drawn")) {
                                boolRend.mouseClicked(s, mx, my, btn, px + 10, (int)setY, PANEL_W - 20, sh);
                            } else if (s.isNumberSetting() && s.hasRestriction()) {
                                if (sliderRend.mouseClicked(s, mx, my, btn, px + 10, (int)setY - 2, PANEL_W - 20, sh)) {
                                    draggingSlider = s;
                                    draggingSliderX = px + 10; draggingSliderW = PANEL_W - 20;
                                    return true;
                                }
                            } else if (s.isEnumSetting() && !(s.getValue() instanceof PositionSetting)) {
                                modeRend.mouseClicked(s, mx, my, btn, px + 10, (int)setY, PANEL_W - 20, sh);
                            }
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
            sliderRend.mouseDragged(draggingSlider, mx, draggingSliderX, draggingSliderW); return true;
        }
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
            search.cursorPosition++;
            resetScroll(); return true;
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
        if (kc == GLFW.GLFW_KEY_ESCAPE) { closing = true; return true; }
        return super.keyPressed(kc, sc, mods);
    }

    @Override
    public void tick() {
        super.tick();
        long now = System.currentTimeMillis();
        if (now - search.lastCursorBlink >= 500) { search.cursorVisible = !search.cursorVisible; search.lastCursorBlink = now; }
    }

    @Override
    public void renderBackground(DrawContext ctx, int mx, int my, float delta) {}

    private void resetScroll() { CATS.forEach(c -> { scrollTgt.put(c, 0f); scrollOff.put(c, 0f); }); }

    private int calcMaxScroll(Module.Category cat) {
        int total = 0;
        String filter = search.text.toLowerCase();
        for (Module m : Managers.MODULE.getModulesByCategory(cat)) {
            if (!filter.isEmpty() && !m.getName().toLowerCase().contains(filter)) continue;
            float ep = expandAnim.getOrDefault(m, m.isExpanded() ? 1f : 0f);
            total += MODULE_H + (int)(computeSettingsH(m) * ep);
        }
        return Math.max(0, total - SCROLL_H);
    }

    private int computeSettingsH(Module mod) {
        int h = 0;
        for (Setting<?> s : mod.getSettings()) {
            if (!s.isVisible()) continue;
            h += getSettH(s) + 1;
        }
        return h;
    }

    private int getSettH(Setting<?> s) {
        if (s.getValue() instanceof Boolean && !s.getName().equals("Enabled") && !s.getName().equals("Drawn")) return 16;
        if (s.isNumberSetting() && s.hasRestriction()) return 20;
        if (s.isEnumSetting() && !(s.getValue() instanceof PositionSetting)) return modeRend.getHeight(s, PANEL_W - 20);
        return 0;
    }
}
