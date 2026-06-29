package acore.aurora.features.modules.misc;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import acore.aurora.core.Managers;
import acore.aurora.events.impl.EventAttack;
import acore.aurora.features.modules.Module;
import acore.aurora.setting.Setting;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;

import static acore.aurora.core.manager.client.ConfigManager.CRITSOUND_FOLDER;

public class CritSound extends Module {
    public final Setting<Float> volume = new Setting<>("Volume", 1.0f, 0.1f, 2.0f);
    public final Setting<Boolean> onlyCrit = new Setting<>("OnlyCrit", true);

    public CritSound() {
        super("CritSound", Category.MISC);
    }

    @Override
    public void onEnable() {
        copyDefaultSound();
    }

    private void copyDefaultSound() {
        File dest = new File(CRITSOUND_FOLDER, "critsound.ogg");
        if (dest.exists()) return;
        try (InputStream in = getClass().getResourceAsStream("/assets/acoreaurora/sounds/wither.ogg")) {
            if (in != null) {
                Files.copy(in, dest.toPath());
            }
        } catch (Exception ignored) {}
    }

    @EventHandler
    public void onAttack(@NotNull EventAttack event) {
        if (!event.isPre()) return;
        if (!(event.getEntity() instanceof LivingEntity)) return;

        boolean isCrit = !mc.player.isOnGround()
                && !mc.player.isClimbing()
                && !mc.player.isInLava()
                && !mc.player.isSubmergedInWater()
                && mc.player.getVelocity().y < 0
                && !mc.player.getAbilities().flying;

        if (onlyCrit.getValue() && !isCrit) return;

        Managers.SOUND.playCritSoundFile(volume.getValue());
    }
}
