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
        // final var that = (SleepManager)(Object)this;
        final var prevTotal = total;
        final var prevSleeping = sleeping;
        total = sleeping = 0;
        
        for(var player: players)
        {
            if(player.isSpectator()) continue;
            total++;
            
            if(player.isSleeping() || player.getPos().y < 64.0) sleeping++;
        }
        
        return (prevSleeping > 0 || sleeping > 0) && (prevTotal != total || prevSleeping != sleeping);
    }
}
