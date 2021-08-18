package net.yoplitein.nicethings.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.SpawnHelper;
import net.minecraft.world.WorldView;
import net.minecraft.world.poi.PointOfInterestStorage.OccupationStatus;
import net.yoplitein.nicethings.NiceThings;
import net.yoplitein.nicethings.block.MegatorchBlock;

@Mixin(SpawnHelper.class)
public class SpawnHelperMixin
{
    @Inject(method = "canSpawn(Lnet/minecraft/entity/SpawnRestriction$Location;Lnet/minecraft/world/WorldView;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/EntityType;)Z", at = @At("RETURN"), cancellable = true)
    private static void megatorchCanSpawn(SpawnRestriction.Location location, WorldView worldView, BlockPos pos, @Nullable EntityType<?> entityType, CallbackInfoReturnable<Boolean> info)
    {
        if(!info.getReturnValueZ())
        {
            info.setReturnValue(false);
            return;
        }
        
        if(entityType == null)
        {
            NiceThings.LOGGER.debug("canSpawn: entity type is null, defaulting to allow (pos {}, srloc {})", pos, location);
            info.setReturnValue(true);
            return;
        }
        else if(entityType.getSpawnGroup() != SpawnGroup.MONSTER)
        {
            info.setReturnValue(true);
            return;
        }
        
        final var world = ((ServerWorldAccess)worldView).toServerWorld();
        final var poiStorage = world.getPointOfInterestStorage();
        
        var nearbyTorch = poiStorage.getInSquare(
            type -> type == NiceThings.MEGATORCHES_POI,
            pos,
            MegatorchBlock.RADIUS,
            OccupationStatus.ANY
        ).findFirst();
        
        // if(nearbyTorch.isPresent()) NiceThings.LOGGER.debug("canSpawn: blocking {} at {} because a torch is at {}", entityType, pos, nearbyTorch.get());
        info.setReturnValue(nearbyTorch.isEmpty());
    }
}
