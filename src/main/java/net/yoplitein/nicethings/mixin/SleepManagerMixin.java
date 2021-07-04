package net.yoplitein.nicethings.mixin;


import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.SleepManager;

@Mixin(SleepManager.class)
public class SleepManagerMixin
{
    @Shadow
    private int total;
    
    @Shadow
    private int sleeping;
    
    public boolean update(List<ServerPlayerEntity> players)
    {
        final var prevTotal = total;
        final var prevSleeping = sleeping;
        int numMiners = 0;
        total = sleeping = 0;
        
        for(var player: players)
        {
            if(player.isSpectator()) continue;
            total++;
            
            final var isMiner = player.getPos().y < 60.0;
            if(player.isSleeping() || isMiner)
            {
                if(isMiner) numMiners++;
                sleeping++;
            }
        }
        
        // fixes a bug where killing someone while crossing the threshold would trigger a skip
        if(numMiners == total) sleeping = 0;
        
        return (prevSleeping > 0 || sleeping > 0) && (prevTotal != total || prevSleeping != sleeping);
    }
}
