package thunder.aurora.events.impl;

import thunder.aurora.events.Event;

public class PostPlayerUpdateEvent extends Event {
    private int iterations;

    public int getIterations() {
        return iterations;
    }

    public void setIterations(int in) {
        iterations = in;
    }
}