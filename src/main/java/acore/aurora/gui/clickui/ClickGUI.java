package acore.aurora.gui.clickui;

import com.google.common.collect.Lists;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import acore.aurora.core.Managers;
import acore.aurora.core.manager.client.ModuleManager;
import acore.aurora.features.hud.HudElement;
import acore.aurora.features.modules.Module;
import acore.aurora.features.modules.client.ClickGui;
import acore.aurora.gui.font.FontRenderers;
import acore.aurora.utility.render.Render2DEngine;
import acore.aurora.utility.render.animation.AnimationUtility;
import acore.aurora.utility.render.animation.EaseOutBack;

import java.awt.*;
import java.util.List;
import java.util.Objects;

import static acore.aurora.features.modules.Module.mc;

public class ClickGUI extends Screen {
    public static List<AbstractCategory> windows;
    public static boolean anyHovered;

    private boolean firstOpen;
    private float scrollY, closeAnimation, prevYaw, prevPitch, closeDirectionX, closeDirectionY;
    public static boolean close = false, imageDirection;
    public static String currentDescription = "";
    public EaseOutBack imageAnimation = new EaseOutBack(6);

    private static final Color OVERLAY       = new Color(0, 0, 0, 130);
    private static final Color TOOLTIP_BG    = new Color(22, 22, 28, 238);
    private static final Color TOOLTIP_STRIP = new Color(210, 200, 170, 255);
    private static final Color TOOLTIP_TEXT  = new Color(210, 210, 222, 255);

    public ClickGUI() {
        super(Text.of("AcoreClickGUI"));
        windows = Lists.newArrayList();
        firstOpen = true;
        this.setInstance();
    }

    private static ClickGUI INSTANCE = new ClickGUI();

    public static ClickGUI getInstance() {
        if (INSTANCE == null) INSTANCE = new ClickGUI();
        imageDirection = true;
        return INSTANCE;
    }

    public static ClickGUI getClickGui() {
        windows.forEach(AbstractCategory::init);
        return ClickGUI.getInstance();
    }

    private void setInstance() { INSTANCE = this; }

    @Override
    protected void init() {
        if (firstOpen) {
            float offset = 0;
            int halfWidth     = mc.getWindow().getScaledWidth() / 2;
            int halfWidthCats = (int)(((float)(Module.Category.values().size() - 1) / 2f)
                    * (ModuleManager.clickGui.moduleWidth.getValue() + 4f));

            for (Module.Category cat : Managers.MODULE.getCategories()) {
                if (cat == Module.Category.HUD) continue;
                Category w = new Category(cat, Managers.MODULE.getModulesByCategory(cat),
                        (halfWidth - halfWidthCats) + offset, 22, 100, 20);
                w.setOpen(true);
                windows.add(w);
                offset += ModuleManager.clickGui.moduleWidth.getValue() + 4;
                if (offset > mc.getWindow().getScaledWidth()) offset = 0;
            }
            firstOpen = false;
        } else {
            if (windows.getFirst().getX() < 0 || windows.getFirst().getY() < 0) {
                float offset      = 0;
                int halfWidth     = mc.getWindow().getScaledWidth() / 2;
                int halfWidthCats = (int)(3 * (ModuleManager.clickGui.moduleWidth.getValue() + 4f));
                for (AbstractCategory w : windows) {
                    w.setX((halfWidth - halfWidthCats) + offset);
                    w.setY(22);
                    offset += ModuleManager.clickGui.moduleWidth.getValue() + 4;
                    if (offset > mc.getWindow().getScaledWidth()) offset = 0;
                }
            }
        }
        windows.forEach(AbstractCategory::init);
    }

    @Override public boolean shouldPause() { return false; }

    @Override
    public void tick() {
        windows.forEach(AbstractCategory::tick);
        imageAnimation.update(imageDirection);
        if (close) {
            if (mc.player != null) {
                if (mc.player.getPitch() > prevPitch) closeDirectionY = (prevPitch - mc.player.getPitch()) * 300;
                if (mc.player.getPitch() < prevPitch) closeDirectionY = (prevPitch - mc.player.getPitch()) * 300;
                if (mc.player.getYaw()   > prevYaw)   closeDirectionX = (prevYaw   - mc.player.getYaw())   * 300;
                if (mc.player.getYaw()   < prevYaw)   closeDirectionX = (prevYaw   - mc.player.getYaw())   * 300;
            }
            if (closeDirectionX < 1 && closeDirectionY < 1 && closeAnimation > 2) closeDirectionY = -3000;
            closeAnimation++;
            if (closeAnimation > 6) {
                close = false;
                windows.forEach(AbstractCategory::restorePos);
                close();
            }
        }
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        if (ModuleManager.clickGui.blur.getValue()) applyBlur(delta);

        Render2DEngine.drawRect(ctx.getMatrices(), 0, 0,
                mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight(), OVERLAY);

        anyHovered = false;

        ClickGui.Image image = ModuleManager.clickGui.image.getValue();
        if (image != ClickGui.Image.None) {
            acore.aurora.utility.render.Render2DEngine.renderTexture(ctx.getMatrices(),
                    mc.getWindow().getScaledWidth() - image.fileWidth * imageAnimation.getAnimationd(),
                    mc.getWindow().getScaledHeight() - image.fileHeight,
                    image.fileWidth, image.fileHeight, 0, 0,
                    image.fileWidth, image.fileHeight, image.fileWidth, image.fileHeight);
        }

        if (closeAnimation <= 6) {
            windows.forEach(w -> {
                w.setX((float)(w.getX() + closeDirectionX * AnimationUtility.deltaTime()));
                w.setY((float)(w.getY() + closeDirectionY * AnimationUtility.deltaTime()));
            });
        }

        if (Module.fullNullCheck()) renderBackground(ctx, mx, my, delta);

        if (ModuleManager.clickGui.scrollMode.getValue() == ClickGui.scrollModeEn.Old) {
            for (AbstractCategory w : windows) {
                if (InputUtil.isKeyPressed(mc.getWindow().getHandle(), 264)) w.setY(w.getY() + 2);
                if (InputUtil.isKeyPressed(mc.getWindow().getHandle(), 265)) w.setY(w.getY() - 2);
                if (InputUtil.isKeyPressed(mc.getWindow().getHandle(), 262)) w.setX(w.getX() + 2);
                if (InputUtil.isKeyPressed(mc.getWindow().getHandle(), 263)) w.setX(w.getX() - 2);
                if (scrollY != 0) w.setY(w.getY() + scrollY);
            }
        } else {
            for (AbstractCategory w : windows)
                if (scrollY != 0) w.setModuleOffset(scrollY, mx, my);
        }

        scrollY = 0;
        windows.forEach(w -> w.render(ctx, mx, my, delta));

        if (!Objects.equals(currentDescription, "") && ModuleManager.clickGui.descriptions.getValue()) {
            float tw = FontRenderers.sf_medium.getStringWidth(currentDescription) + 16;
            float th = 16f;
            float tx = mx + 12;
            float ty = my + 10;
            Render2DEngine.drawRound(ctx.getMatrices(), tx, ty, tw, th, 5f, TOOLTIP_BG);
            Render2DEngine.drawRect(ctx.getMatrices(), tx, ty + 2, 2f, th - 4, TOOLTIP_STRIP);
            FontRenderers.sf_medium.drawString(ctx.getMatrices(), currentDescription,
                    tx + 7, ty + th / 2f - 3.5f, TOOLTIP_TEXT.getRGB());
            currentDescription = "";
        }

        if (!HudElement.anyHovered && !ClickGUI.anyHovered)
            if (GLFW.glfwGetPlatform() != GLFW.GLFW_PLATFORM_WAYLAND)
                GLFW.glfwSetCursor(mc.getWindow().getHandle(),
                        GLFW.glfwCreateStandardCursor(GLFW.GLFW_ARROW_CURSOR));
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double ha, double va) {
        scrollY += (int)(va * 5D);
        return super.mouseScrolled(mx, my, ha, va);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        windows.forEach(w -> {
            w.mouseClicked((int)mx, (int)my, btn);
            windows.forEach(w1 -> { if (w.dragging && w != w1) w1.dragging = false; });
        });
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        windows.forEach(w -> w.mouseReleased((int)mx, (int)my, btn));
        return super.mouseReleased(mx, my, btn);
    }

    @Override
    public boolean charTyped(char key, int mod) {
        windows.forEach(w -> w.charTyped(key, mod));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int mods) {
        windows.forEach(w -> w.keyTyped(keyCode));
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (mc.player == null || !ModuleManager.clickGui.closeAnimation.getValue()) {
                imageDirection = false;
                imageAnimation.reset();
                super.keyPressed(keyCode, scanCode, mods);
                return true;
            }
            if (close) return true;
            imageDirection = false;
            windows.forEach(AbstractCategory::savePos);
            closeDirectionX = 0;
            closeDirectionY = 0;
            close = true;
            mc.mouse.lockCursor();
            closeAnimation = 0;
            if (mc.player != null) {
                prevYaw   = mc.player.getYaw();
                prevPitch = mc.player.getPitch();
            }
            return true;
        }
        return false;
    }
}
