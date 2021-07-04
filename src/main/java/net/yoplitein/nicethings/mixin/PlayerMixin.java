package net.yoplitein.nicethings.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.entity.player.PlayerEntity;

@Mixin(PlayerEntity.class)
public class PlayerMixin
{
    @Shadow
    private int sleepTimer;
    
    public boolean isSleepingLongEnough()
    {
        final var that = (PlayerEntity)(Object)this;
        return that.getPos().y < 64.0 || (that.isSleeping() && sleepTimer >= 100);
    }
}
