package net.yoplitein.nicethings.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.player.PlayerEntity;

public interface PlayerSneakCallback
{
    Event<PlayerSneakCallback> EVENT = EventFactory.createArrayBacked(PlayerSneakCallback.class, (listeners) ->
        (player) -> {
            for(var listener: listeners)
                listener.sneaked(player);
        }
    );
    
    void sneaked(PlayerEntity player);
}
