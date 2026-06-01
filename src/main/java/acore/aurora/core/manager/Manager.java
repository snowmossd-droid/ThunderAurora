package acore.aurora.core.manager;

import acore.aurora.core.Managers;
import acore.aurora.core.manager.client.NotificationManager;
import acore.aurora.core.manager.themeManager.StyleManager;
import acore.aurora.features.modules.FunctionManager;
import acore.aurora.protect.UserProfile;

public class Manager {
    public static FunctionManager FUNCTION_MANAGER = new FunctionManager();
    public static StyleManager STYLE_MANAGER = new StyleManager();
    public static NotificationManager NOTIFICATION_MANAGER = new NotificationManager();
    public static UserProfile USER_PROFILE = new UserProfile("Unknown", "User", "N/A");
}
