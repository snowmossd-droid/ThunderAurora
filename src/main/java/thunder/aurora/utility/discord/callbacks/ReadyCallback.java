package thunder.aurora.utility.discord.callbacks;

import thunder.aurora.utility.discord.DiscordUser;
import com.sun.jna.Callback;

public interface ReadyCallback extends Callback {
    void apply(final DiscordUser p0);
}
