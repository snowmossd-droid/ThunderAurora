package acore.aurora.gui.clickui.impl;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import acore.aurora.AcoreAurora;
import acore.aurora.gui.clickui.AbstractElement;
import acore.aurora.gui.font.FontRenderers;
import acore.aurora.setting.Setting;
import acore.aurora.setting.impl.ColorSetting;
import acore.aurora.utility.math.MathUtility;
import acore.aurora.utility.render.Render2DEngine;

import java.awt.*;

public class ColorPickerElement extends AbstractElement {
    private float hue;
    private float saturation;
    private float brightness;
    private int alpha;

    private boolean afocused;
    private boolean hfocused;
    private boolean sbfocused;

    private Color prevColor;
    private boolean extended;

    private final Setting colorSetting;

    private static final Identifier COLORS_ICON = Identifier.of("acoreaurora", "textures/gui/elements/colors.png");

    public ColorSetting getColorSetting() {
        return (ColorSetting) colorSetting.getValue();
    }

    public ColorPickerElement(Setting setting) {
        super(setting);
        this.colorSetting = setting;
        prevColor = getColorSetting().getColorObject();
        updatePos();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrixStack = context.getMatrices();

        boolean colorHovered = Render2DEngine.isHovered(mouseX, mouseY, x, y + 3f, width - 5, 11);

        context.drawTexture(COLORS_ICON, (int)(x + 4), (int)(y + 5), 9, 9, 0, 0, 64, 64, 64, 64);

        FontRenderers.sf_medium_mini.drawString(matrixStack, setting.getName(), x + 16, y + 8, new Color(-1).getRGB());

        Render2DEngine.drawBlurredShadow(matrixStack, x + width - 22f, y + 5f, 14, 7, colorHovered ? 6 : 10, getColorSetting().getColorObject());
        if (colorHovered)
            Render2DEngine.drawRound(matrixStack, x + width - 22.5f, y + 4.5f, 15, 8, 1, getColorSetting().getColorObject());
        else
            Render2DEngine.drawRound(matrixStack, x + width - 22, y + 5, 14, 7, 1, getColorSetting().getColorObject());

        if (!extended)
            return;

        renderPicker(context, matrixStack, mouseX, mouseY);
    }

    @Override
    public float getHeight() {
        return extended ? 80 : 15;
    }

    private void renderPicker(DrawContext context, MatrixStack matrixStack, int mouseX, int mouseY) {
        if (prevColor != getColorSetting().getColorObject()) {
            updatePos();
            prevColor = getColorSetting().getColorObject();
        }

        float sbX = x + 4;
        float sbY = y + 18;
        float sbW = width - 42;
        float sbH = 42;

        float hueX = x + width - 34;
        float hueY = sbY;
        float hueW = 8;
        float hueH = sbH;

        float alphaX = x + width - 22;
        float alphaY = sbY;
        float alphaW = 8;
        float alphaH = sbH;

        Color satA = Color.getHSBColor(hue, 0f, 1f);
        Color satB = Color.getHSBColor(hue, 1f, 1f);
        Render2DEngine.horizontalGradient(matrixStack, sbX, sbY, sbX + sbW, sbY + sbH, satA, satB);
        Render2DEngine.verticalGradient(matrixStack, sbX, sbY, sbX + sbW, sbY + sbH, new Color(0, 0, 0, 0), new Color(0, 0, 0, 255));

        float dotX = sbX + saturation * sbW;
        float dotY = sbY + (1f - brightness) * sbH;
        Render2DEngine.drawRound(matrixStack, dotX - 2.5f, dotY - 2.5f, 5, 5, 2.5f, Color.WHITE);
        Render2DEngine.drawRound(matrixStack, dotX - 1.5f, dotY - 1.5f, 3, 3, 1.5f, Color.getHSBColor(hue, saturation, brightness));

        for (float i = 0; i < hueH; i += 1f) {
            float h = i / hueH;
            Render2DEngine.drawRect(matrixStack, hueX, hueY + i, hueW, 1.5f, Color.getHSBColor(h, 1f, 1f));
        }
        float hueMarkerY = hueY + hue * hueH;
        Render2DEngine.drawRect(matrixStack, hueX - 1, hueMarkerY - 1, hueW + 2, 2, Color.WHITE);

        Color curRGB = Color.getHSBColor(hue, saturation, brightness);
        Render2DEngine.verticalGradient(matrixStack, alphaX, alphaY, alphaX + alphaW, alphaY + alphaH,
                new Color(curRGB.getRed(), curRGB.getGreen(), curRGB.getBlue(), 255),
                new Color(curRGB.getRed(), curRGB.getGreen(), curRGB.getBlue(), 0));
        float alphaMarkerY = alphaY + (1f - alpha / 255f) * alphaH;
        Render2DEngine.drawRect(matrixStack, alphaX - 1, alphaMarkerY - 1, alphaW + 2, 2, Color.WHITE);

        if (sbfocused) {
            saturation = MathUtility.clamp((mouseX - sbX) / sbW, 0f, 1f);
            brightness = MathUtility.clamp(1f - (mouseY - sbY) / sbH, 0f, 1f);
            applyColor();
        }
        if (hfocused) {
            hue = MathUtility.clamp((mouseY - hueY) / hueH, 0f, 1f);
            applyColor();
        }
        if (afocused) {
            alpha = (int)(MathUtility.clamp(1f - (mouseY - alphaY) / alphaH, 0f, 1f) * 255);
            applyColor();
        }

        float btnY = y + 64;
        float btnH = 8;

        boolean dark2 = Render2DEngine.isDark(getColorSetting().getColorObject());
        boolean dark = Render2DEngine.isDark(AcoreAurora.copy_color);

        Render2DEngine.drawRect(matrixStack, x + 4, btnY, 24, btnH, new Color(0x424242));
        FontRenderers.sf_medium_mini.drawString(matrixStack, "Copy", x + 8, btnY + 2, Color.WHITE.getRGB());

        Render2DEngine.drawRect(matrixStack, x + 32, btnY, 24, btnH, getColorSetting().isRainbow() ? getColorSetting().getColorObject() : new Color(0x424242));
        FontRenderers.sf_medium_mini.drawString(matrixStack, "RB", x + 40, btnY + 2, dark2 ? Color.WHITE.getRGB() : Color.BLACK.getRGB());

        Render2DEngine.drawRect(matrixStack, x + 60, btnY, 28, btnH, AcoreAurora.copy_color != null ? AcoreAurora.copy_color : new Color(0x424242));
        FontRenderers.sf_medium_mini.drawString(matrixStack, "Paste", x + 64, btnY + 2, dark ? Color.WHITE.getRGB() : Color.BLACK.getRGB());
    }

    private void applyColor() {
        Color c = Color.getHSBColor(hue, saturation, brightness);
        setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha));
    }

    private void updatePos() {
        float[] hsb = Color.RGBtoHSB(
            getColorSetting().getColorObject().getRed(),
            getColorSetting().getColorObject().getGreen(),
            getColorSetting().getColorObject().getBlue(), null);
        hue = hsb[0];
        saturation = hsb[1];
        brightness = hsb[2];
        alpha = getColorSetting().getAlpha();
    }

    private void setColor(Color color) {
        getColorSetting().setColor(color.getRGB());
        prevColor = color;
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int button) {
        boolean colorHovered = Render2DEngine.isHovered(mouseX, mouseY, x, y + 3f, width - 5, 11);
        if (colorHovered) {
            extended = !extended;
            return;
        }

        if (!extended) return;

        float sbX = x + 4, sbY = y + 18, sbW = width - 42, sbH = 42;
        float hueX = x + width - 34, alphaX = x + width - 22;
        float btnY = y + 64, btnH = 8;

        if (Render2DEngine.isHovered(mouseX, mouseY, sbX, sbY, sbW, sbH) && button == 0) sbfocused = true;
        else if (Render2DEngine.isHovered(mouseX, mouseY, hueX, sbY, 8, sbH) && button == 0) hfocused = true;
        else if (Render2DEngine.isHovered(mouseX, mouseY, alphaX, sbY, 8, sbH) && button == 0) afocused = true;
        else if (Render2DEngine.isHovered(mouseX, mouseY, x + 4, btnY, 24, btnH)) AcoreAurora.copy_color = getColorSetting().getColorObject();
        else if (Render2DEngine.isHovered(mouseX, mouseY, x + 32, btnY, 24, btnH)) getColorSetting().setRainbow(!getColorSetting().isRainbow());
        else if (Render2DEngine.isHovered(mouseX, mouseY, x + 60, btnY, 28, btnH) && AcoreAurora.copy_color != null) {
            getColorSetting().setColor(AcoreAurora.copy_color.getRGB());
            updatePos();
        }

        super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int button) {
        hfocused = false;
        afocused = false;
        sbfocused = false;
    }

    @Override
    public void onClose() {
        hfocused = false;
        afocused = false;
        sbfocused = false;
    }
            }
                                           
