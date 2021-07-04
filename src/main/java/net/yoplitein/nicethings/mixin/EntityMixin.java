package net.yoplitein.nicethings.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.yoplitein.nicethings.event.PlayerSneakCallback;

@Mixin(Entity.class)
public class EntityMixin
{
    @Inject(at = @At("RETURN"), method = "setSneaking(Z)V")
    public void setSneaking(boolean sneaking, CallbackInfo info)
    {
        var that = (Entity)(Object)this;
        if(!(that instanceof PlayerEntity)) return;
        
        if(sneaking) PlayerSneakCallback.EVENT.invoker().sneaked((PlayerEntity)that);
    }
}
