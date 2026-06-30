package acore.aurora.features.hud.impl;

import net.minecraft.client.gui.DrawContext;
import acore.aurora.core.Managers;
import acore.aurora.core.manager.client.ModuleManager;
import acore.aurora.gui.font.FontRenderers;
import acore.aurora.features.hud.HudElement;
import acore.aurora.features.modules.misc.NameProtect;
import acore.aurora.setting.Setting;
import acore.aurora.setting.impl.ColorSetting;
import acore.aurora.utility.render.Render2DEngine;
import acore.aurora.utility.render.TextUtil;
import acore.aurora.utility.render.animation.AnimationUtility;

import java.awt.*;

public class WaterMark extends HudElement {
    public WaterMark() {
        super("WaterMark", 110, 24);
    }

    public static final Setting<Mode> mode = new Setting<>("Mode", Mode.Default);
    private final Setting<Boolean> showPing = new Setting<>("ShowPing", true);
    private final Setting<Boolean> showServer = new Setting<>("ShowServer", false);
    private final Setting<ColorSetting> accentColor = new Setting<>("AccentColor", new ColorSetting(-9192762));

    private final TextUtil textUtil = new TextUtil("Aurora", "Aurora", "Aurora", "Aurora", "Aurora", "Aurora", "Aurora", "Aurora", "Aurora");

    private float widthAnim = 24;

    private static final int   LOGO_BOX     = 16;
    private static final float RADIUS       = 7f;
    private static final Color PANEL_BG     = new Color(0x1D, 0x25, 0x36, 235);
    private static final Color LOGO_BG      = new Color(0x34, 0x3A, 0x48, 255);
    private static final Color SEPARATOR    = new Color(0x4A, 0x52, 0x62, 160);
    private static final Color PING_GOOD    = new Color(76, 255, 122);
    private static final Color PING_MID     = new Color(255, 200, 76);
    private static final Color PING_BAD     = new Color(255, 76, 76);

    public enum Mode {
        Default, Compact
    }

    public void onRender2D(DrawContext context) {
        super.onRender2D(context);
        String username = ModuleManager.nameProtect.isEnabled() ? NameProtect.getCustomName() : mc.getSession().getUsername();
        String server = mc.isInSingleplayer() ? "Single Player" : mc.getNetworkHandler().getServerInfo().address;
        int ping = (int) Managers.SERVER.getPing();

        boolean compact = mode.getValue() == Mode.Compact;

        float nameW = FontRenderers.sf_bold_mini.getStringWidth(username);
        String pingText = ping + "ms";
        float pingW = FontRenderers.sf_bold_mini.getStringWidth(pingText);
        float serverW = showServer && !compact ? FontRenderers.sf_bold_mini.getStringWidth(server) : 0;

        float contentW = LOGO_BOX + 6 + 4 + FontRenderers.sf_bold.getStringWidth("Aurora") + 8;
        if (!compact) {
            contentW += 1 + 8 + nameW + 8;
            if (showPing.getValue()) contentW += 1 + 8 + pingW + 8;
            if (showServer.getValue()) contentW += 1 + 8 + serverW + 8;
        }

        widthAnim = AnimationUtility.fast(widthAnim, contentW, 15);
        float h = 24f;

        Render2DEngine.drawBlurredShadow(context.getMatrices(), getPosX() + 1, getPosY() + 1, widthAnim - 2, h - 2, 8, new Color(0, 0, 0, 70));
        Render2DEngine.drawRound(context.getMatrices(), getPosX(), getPosY(), widthAnim, h, RADIUS, PANEL_BG);
        Render2DEngine.drawRoundBorder(context.getMatrices(), getPosX(), getPosY(), widthAnim, h, RADIUS, 1f, SEPARATOR);

        Render2DEngine.addWindow(context.getMatrices(), getPosX(), getPosY(), getPosX() + widthAnim, getPosY() + h, 1f);

        float cx = getPosX() + 4;
        Render2DEngine.drawRound(context.getMatrices(), cx, getPosY() + 4, LOGO_BOX, LOGO_BOX, 5f, LOGO_BG);
        FontRenderers.sf_bold_mini.drawCenteredString(context.getMatrices(), "A", cx + LOGO_BOX / 2f, getPosY() + 8.5f, accentColor.getValue().getColor());
        cx += LOGO_BOX + 6;

        float logoTextY = getPosY() + h / 2f - 4.5f;
        FontRenderers.sf_bold.drawString(context.getMatrices(), "Aurora", cx, logoTextY, Color.WHITE.getRGB());
        cx += FontRenderers.sf_bold.getStringWidth("Aurora") + 8;

        if (!compact) {
            cx = drawSeparator(context, cx, h);
            FontRenderers.sf_bold_mini.drawString(context.getMatrices(), username, cx, getPosY() + h / 2f - 3.5f, new Color(200, 206, 220).getRGB());
            cx += nameW + 8;

            if (showPing.getValue()) {
                cx = drawSeparator(context, cx, h);
                Color pc = ping <= 0 ? new Color(150, 156, 170) : ping < 80 ? PING_GOOD : ping < 150 ? PING_MID : PING_BAD;
                FontRenderers.sf_bold_mini.drawString(context.getMatrices(), pingText, cx, getPosY() + h / 2f - 3.5f, pc.getRGB());
                cx += pingW + 8;
            }

            if (showServer.getValue()) {
                cx = drawSeparator(context, cx, h);
                FontRenderers.sf_bold_mini.drawString(context.getMatrices(), server, cx, getPosY() + h / 2f - 3.5f, new Color(150, 156, 170).getRGB());
            }
        }

        Render2DEngine.popWindow();
        setBounds(getPosX(), getPosY(), widthAnim, h);
    }

    private float drawSeparator(DrawContext context, float x, float h) {
        Render2DEngine.drawRect(context.getMatrices(), x, getPosY() + 6, 1f, h - 12, SEPARATOR);
        return x + 9;
    }

    @Override
    public void onUpdate() {
        textUtil.tick();
    }
    }
            
