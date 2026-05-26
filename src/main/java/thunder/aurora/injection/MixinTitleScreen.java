package thunder.aurora.injection;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thunder.aurora.ThunderAurora;
import thunder.aurora.gui.misc.DialogScreen;
import thunder.aurora.gui.mainmenu.MainMenuScreen;
import thunder.aurora.utility.render.TextureStorage;
import thunder.aurora.core.manager.client.ModuleManager;
import thunder.aurora.features.modules.client.ClientSettings;

import java.net.URI;

import static thunder.aurora.features.modules.Module.mc;
import static thunder.aurora.features.modules.client.ClientSettings.isRu;

@Mixin(TitleScreen.class)
public class MixinTitleScreen extends Screen {
    protected MixinTitleScreen(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("RETURN"))
    public void postInitHook(CallbackInfo ci) {
        if (ClientSettings.customMainMenu.getValue() && !MainMenuScreen.getInstance().confirm && ModuleManager.clickGui.getBind().getKey() != -1) {
            mc.setScreen(MainMenuScreen.getInstance());
        }
        if (ModuleManager.clickGui.getBind().getKey() == -1) {
            DialogScreen dialogScreen2 = new DialogScreen(
                    TextureStorage.cutie,
                    isRu() ? "Спасибо что скачали ThunderAurora!" : "Thank you for downloading ThunderAurora!",
                    isRu() ? "Меню с функциями клиента открывается на клавишу - P" : "Menu with client modules is opened with the key - P",
                    isRu() ? "Зайти в майн" : "Join on minecraft",
                    isRu() ? "Закрыть майн" : "Close minecraft",
                    () -> {
                        ModuleManager.clickGui.setBind(InputUtil.fromTranslationKey("key.keyboard.p").getCode(), false, false);
                        mc.setScreen(MainMenuScreen.getInstance());
                    },
                    () -> {
                        ModuleManager.clickGui.setBind(InputUtil.fromTranslationKey("key.keyboard.p").getCode(), false, false);
                        mc.stop();
                    }
            );
            DialogScreen dialogScreen1 = new DialogScreen(
                    TextureStorage.questionPic,
                    "Hello!",
                    "What's your language?",
                    "Русский",
                    "English",
                    () -> {
                        ClientSettings.language.setValue(ClientSettings.Language.RU);
                        mc.setScreen(dialogScreen2);
                    },
                    () -> {
                        ClientSettings.language.setValue(ClientSettings.Language.ENG);
                        mc.setScreen(dialogScreen2);
                    }
            );
            mc.setScreen(dialogScreen1);
        }

        if (ThunderAurora.isOutdated && !FabricLoader.getInstance().isDevelopmentEnvironment()) {
            mc.setScreen(new ConfirmScreen(
                    confirm -> {
                        if (confirm) Util.getOperatingSystem().open(URI.create("https://github.com/Pan4ur/ThunderAurora-Recode/releases/download/latest/thunderaurora-1.7.jar/"));
                        else mc.stop();
                    },
                    Text.of(Formatting.RED + "You are using an outdated version of ThunderAurora Recode"), Text.of("Please update to the latest release"), Text.of("Download"), Text.of("Quit Game")));
        }
    }
}
