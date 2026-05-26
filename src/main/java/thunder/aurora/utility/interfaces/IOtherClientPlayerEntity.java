package thunder.aurora.utility.interfaces;

import thunder.aurora.features.modules.combat.Aura;

public interface IOtherClientPlayerEntity {
    void resolve(Aura.Resolver mode);

    void releaseResolver();
}
