package thunder.aurora.utility.discord;

import java.util.Arrays;
import java.util.List;
import thunder.aurora.utility.discord.callbacks.JoinGameCallback;
import thunder.aurora.utility.discord.callbacks.ErroredCallback;
import thunder.aurora.utility.discord.callbacks.ReadyCallback;
import thunder.aurora.utility.discord.callbacks.SpectateGameCallback;
import thunder.aurora.utility.discord.callbacks.JoinRequestCallback;
import thunder.aurora.utility.discord.callbacks.DisconnectedCallback;
import com.sun.jna.Structure;

public class DiscordEventHandlers extends Structure {
    public DisconnectedCallback disconnected;
    public JoinRequestCallback joinRequest;
    public SpectateGameCallback spectateGame;
    public ReadyCallback ready;
    public ErroredCallback errored;
    public JoinGameCallback joinGame;
    
    protected List<String> getFieldOrder() {
        return Arrays.asList("ready", "disconnected", "errored", "joinGame", "spectateGame", "joinRequest");
    }
}