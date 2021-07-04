package net.yoplitein.nicethings;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.yoplitein.nicethings.event.PlayerSneakCallback;

public class Twerking
{
    static final int effectRadius = 6;
    static final float chancePerBlock = 0.2f; // 20% chance to tick per block
    
    static final Random random = new Random();
    static Map<UUID, Double> sneakTimes = new HashMap<>();
    static int purgeNticks = 0;
    
    public static void initialize()
    {
        NiceThings.LOGGER.info("twerking setup");
        PlayerSneakCallback.EVENT.register(Twerking::onPlayerSneak);
        ServerTickEvents.END_SERVER_TICK.register(Twerking::purgeTimestamps);
    }
    
    static double getNowSeconds()
    {
        return System.currentTimeMillis() / 1000.0;
    }
    
    static void purgeTimestamps(MinecraftServer server)
    {
        if(purgeNticks++ < 600) // only check every ~30 seconds
            return;
        
        purgeNticks = 0;
        
        final var now = getNowSeconds();
        sneakTimes.entrySet().removeIf(pair -> now - pair.getValue() >= 2);
    }
    
    static void onPlayerSneak(PlayerEntity player)
    {
        if(player.getEntityWorld().isClient) return; // TODO: just move this (and crop click) into server init
        final var world = (ServerWorld)player.getEntityWorld();
        
        final var uuid = player.getUuid();
        final var now = getNowSeconds();
        
        var lastSneakTime = sneakTimes.put(uuid, now);
        if(lastSneakTime == null || now - lastSneakTime > 1) return;
        
        final var origin = new BlockPos(player.getPos());
        final var coords = BlockPos.stream(origin.add(new Vec3i(-effectRadius, -effectRadius, -effectRadius)), origin.add(new Vec3i(effectRadius, effectRadius, effectRadius)));
        
        random.setSeed(System.currentTimeMillis());
        coords.forEach(pos -> {
            final var state = world.getBlockState(pos);
            final var stateAbove = world.getBlockState(pos.up()); // for vertically-growing crops
            if(!state.hasRandomTicks() || random.nextFloat() < (1.0 - chancePerBlock)) return;
            
            state.randomTick(world, pos, random);
            
            if(stateAbove != world.getBlockState(pos.up())) pos = pos.up(); // sound/particles from the maybe-new block
            else if(state == world.getBlockState(pos)) return;
            
            world.playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.ENTITY_VILLAGER_YES, SoundCategory.BLOCKS, 0.35f, 1.35f + 0.25f * (float)random.nextGaussian());
            world.spawnParticles(
                ParticleTypes.HAPPY_VILLAGER,
                // true,
                pos.getX() + 0.5,
                pos.getY() + 0.75,
                pos.getZ() + 0.5,
                15,
                0.5, 0.25, 0.5,
                2.0
            );
        });
    }
}
