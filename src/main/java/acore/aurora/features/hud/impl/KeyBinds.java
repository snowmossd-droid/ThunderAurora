package acore.aurora.features.hud.impl;

import net.minecraft.client.gui.DrawContext;
import org.jetbrains.annotations.NotNull;
import acore.aurora.core.Managers;
import acore.aurora.core.manager.client.ModuleManager;
import acore.aurora.gui.font.FontRenderers;
import acore.aurora.features.hud.HudElement;
import acore.aurora.features.modules.Module;
import acore.aurora.features.modules.render.HudEditor;
import acore.aurora.setting.Setting;
import acore.aurora.setting.impl.ColorSetting;
import acore.aurora.utility.render.Render2DEngine;
import acore.aurora.utility.render.animation.AnimationUtility;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class KeyBinds extends HudElement {
    public final Setting<ColorSetting> oncolor = new Setting<>("OnColor", new ColorSetting(-1));
    public final Setting<ColorSetting> offcolor = new Setting<>("OffColor", new ColorSetting(1));
    public final Setting<Boolean> onlyEnabled = new Setting<>("OnlyEnabled", false);

    private static final Map<String, String> CAT_ICONS = new HashMap<>();
    static {
        CAT_ICONS.put("Combat",   "f");
        CAT_ICONS.put("Movement", "w");
        CAT_ICONS.put("Render",   "E");
        CAT_ICONS.put("Player",   "r");
        CAT_ICONS.put("Misc",     "v");
    }

    private static final int   CARD_H        = 22;
    private static final int   CARD_GAP      = 4;
    private static final int   CARD_PAD      = 6;
    private static final int   ICON_BOX      = 16;
    private static final float CARD_RADIUS   = 6f;
    private static final Color CARD_BG_ON    = new Color(0x34, 0x3A, 0x48, 235);
    private static final Color CARD_BG_OFF   = new Color(0x1D, 0x25, 0x36, 200);
    private static final Color ICON_BG_ON    = new Color(0x4A, 0x52, 0x62, 255);
    private static final Color ICON_BG_OFF   = new Color(0x2A, 0x32, 0x42, 255);
    private static final Color KEY_BG        = new Color(0x10, 0x14, 0x1E, 220);

    public KeyBinds() {
        super("KeyBinds", 130, 100);
    }

    private float vAnimation, hAnimation;

    public void onRender2D(DrawContext context) {
        super.onRender2D(context);

        int   visibleCount = 0;
        float maxNameWidth = 0;
        float maxKeyWidth  = 0;

        for (Module feature : Managers.MODULE.modules) {
            if (feature.isDisabled() && onlyEnabled.getValue()) continue;
            if (Objects.equals(feature.getBind().getBind(), "None")
                    || feature == ModuleManager.clickGui || feature == ModuleManager.thunderHackGui) continue;

            visibleCount++;
            float nameWidth = FontRenderers.sf_bold_mini.getStringWidth(feature.getName());
            float keyWidth  = FontRenderers.sf_bold_mini.getStringWidth(getShortKeyName(feature));
            if (nameWidth > maxNameWidth) maxNameWidth = nameWidth;
            if (keyWidth  > maxKeyWidth)  maxKeyWidth  = keyWidth;
        }

        float targetW = ICON_BOX + CARD_PAD * 3 + maxNameWidth + 18 + maxKeyWidth + 4;
        targetW = Math.max(targetW, 90);
        float targetH = visibleCount > 0 ? visibleCount * (CARD_H + CARD_GAP) - CARD_GAP : 0;

        hAnimation = AnimationUtility.fast(hAnimation, targetW, 15);
        vAnimation = AnimationUtility.fast(vAnimation, targetH, 15);

        if (vAnimation <= 0.5f) {
            setBounds(getPosX(), getPosY(), Math.max(hAnimation, 1), 1);
            return;
        }

        Render2DEngine.addWindow(context.getMatrices(), getPosX(), getPosY(),
                getPosX() + hAnimation, getPosY() + vAnimation, 1f);

        float cardY = getPosY();
        for (Module feature : Managers.MODULE.modules) {
            if (feature.isDisabled() && onlyEnabled.getValue()) continue;
            if (Objects.equals(feature.getBind().getBind(), "None")
                    || feature == ModuleManager.clickGui || feature == ModuleManager.thunderHackGui) continue;

            boolean on = feature.isOn();
            Color cardBg = on ? CARD_BG_ON : CARD_BG_OFF;
            Color iconBg = on ? ICON_BG_ON : ICON_BG_OFF;
            Color textColor = on ? oncolor.getValue().getColor() : offcolor.getValue().getColor();

            Render2DEngine.drawRound(context.getMatrices(), getPosX(), cardY, hAnimation, CARD_H, CARD_RADIUS, cardBg);

            float iconY = cardY + (CARD_H - ICON_BOX) / 2f;
            Render2DEngine.drawRound(context.getMatrices(), getPosX() + CARD_PAD, iconY, ICON_BOX, ICON_BOX, 4f, iconBg);
            String icon = CAT_ICONS.getOrDefault(feature.category.toString(), "");
            if (!icon.isEmpty()) {
                float iw = FontRenderers.icons.getStringWidth(icon);
                FontRenderers.icons.drawString(context.getMatrices(), icon,
                        getPosX() + CARD_PAD + (ICON_BOX - iw) / 2f,
                        iconY + 3.5f, on ? Color.WHITE.getRGB() : new Color(170, 178, 195).getRGB());
            }

            float nameX = getPosX() + CARD_PAD * 2 + ICON_BOX;
            FontRenderers.sf_bold_mini.drawString(context.getMatrices(), feature.getName(),
                    nameX, cardY + CARD_H / 2f - 3.5f, textColor);

            String keyStr = getShortKeyName(feature);
            float keyW = FontRenderers.sf_bold_mini.getStringWidth(keyStr) + 8;
            float keyX = getPosX() + hAnimation - CARD_PAD - keyW;
            float keyY = cardY + (CARD_H - 12f) / 2f;
            Render2DEngine.drawRound(context.getMatrices(), keyX, keyY, keyW, 12f, 3f, KEY_BG);
            FontRenderers.sf_bold_mini.drawCenteredString(context.getMatrices(), keyStr,
                    keyX + keyW / 2f, keyY + 2f, on ? oncolor.getValue().getColor() : new Color(170, 178, 195).getRGB());

            cardY += CARD_H + CARD_GAP;
        }

        Render2DEngine.popWindow();
        setBounds(getPosX(), getPosY(), hAnimation, vAnimation);
    }

    @NotNull
    public static String getShortKeyName(Module feature) {
        String sbind = feature.getBind().getBind();
        return switch (feature.getBind().getBind()) {
            case "LEFT_CONTROL" -> "LCtrl";
            case "RIGHT_CONTROL" -> "RCtrl";
            case "LEFT_SHIFT" -> "LShift";
            case "RIGHT_SHIFT" -> "RShift";
            case "LEFT_ALT" -> "LAlt";
            case "RIGHT_ALT" -> "RAlt";
            default -> sbind.toUpperCase();
        };
    }
    }
                
