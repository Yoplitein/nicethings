package net.yoplitein.nicethings;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.AbstractPlantPartBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.block.StemBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;

public class CropClick {
    static final Logger LOGGER = LogManager.getLogger();
    static final Property[] allAgeProps = {
        Properties.AGE_1,
        Properties.AGE_2,
        Properties.AGE_3,
        Properties.AGE_5,
        Properties.AGE_7,
        Properties.AGE_15,
        Properties.AGE_25,
    };
    static HashMap<Identifier, CropInfo> whitelist = new HashMap<>();
    
    static record CropInfo(Item seeds, boolean vertical, IntProperty ageProp, int maxAge) {}
    
    public static void initialize()
    {
        ServerLifecycleEvents.SERVER_STARTED.register(CropClick::buildWhitelist);
        UseBlockCallback.EVENT.register(CropClick::onCropClicked);
    }
    
    static int maxOf(IntProperty prop)
    {
        var vals = prop.getValues();
        return vals.stream().skip(vals.size() - 1).iterator().next();
    }
    
    static List<ItemStack> getDrops(MinecraftServer server, Block block, IntProperty ageProp)
    {
        var ctx = new LootContext.Builder(server.getOverworld())
            .parameter(LootContextParameters.ORIGIN, Vec3d.ZERO)
            .parameter(LootContextParameters.TOOL, ItemStack.EMPTY)
        ;
        return block.getDefaultState().with(ageProp, maxOf(ageProp)).getDroppedStacks(ctx);
    }
    
    static List<ItemStack> getDrops(World world, BlockPos pos, BlockState state)
    {
        var ctx = new LootContext.Builder((ServerWorld)world)
            .parameter(LootContextParameters.ORIGIN, Vec3d.of(pos))
            .parameter(LootContextParameters.TOOL, ItemStack.EMPTY)
        ;
        return state.getDroppedStacks(ctx);
    }
    
    static @Nullable Item getSeed(MinecraftServer server, Block block, IntProperty ageProp)
    {
        if(block instanceof CropBlock)
            return ((CropBlock)block).getSeedsItem().asItem();
        else
        {
            var drops = getDrops(server, block, ageProp);
            var dropsSelf = drops.stream().anyMatch(stack -> stack.getItem() == block.asItem());
            if(dropsSelf) return block.asItem();
            else return null;
        }
    }
    
    private static boolean isVertical(World world, Block block)
    {
        final var basePos = new BlockPos(0, 250, 0);
        var chunk = world.getChunk(basePos);
        
        // there's no other way AFAICT to determine whether a block can be placed on
        // any given block, so we use this wonderful kludge
        chunk.setBlockState(basePos, block.getDefaultState(), false);
        var result = block.getDefaultState().canPlaceAt(world, basePos.add(0, 1, 0));
        chunk.setBlockState(basePos, Blocks.AIR.getDefaultState(), false);
        
        return result;
    }
    
    static void buildWhitelist(MinecraftServer server)
    {
        final var blockUseMethodName = System.getProperty("fabric.development", "false").equalsIgnoreCase("true") ? "onUse" : "method_9534";
        
        for(var regEntry: Registry.BLOCK.getEntries())
        {
            var block = regEntry.getValue();
            
            // discard melon/pumpkin, and cave plants (at least as of 1.17?)
            if(block instanceof StemBlock || block instanceof AbstractPlantPartBlock)
                continue;
            
            var state = block.getDefaultState();
            IntProperty ageProp = null;
            
            // only consider blocks which have some sort of age property
            outer: for(var prop: state.getProperties())
                for(var testAgeProp: allAgeProps)
                    if(prop == testAgeProp) {
                        ageProp = (IntProperty)prop;
                        break outer;
                    }
            if(ageProp == null) continue;
            
            // do nothing if the block itself has right-click functionality (e.g. berry bushes)
            Method onUseMethod = null;
            try { onUseMethod = block.getClass().getMethod(blockUseMethodName, BlockState.class, World.class, BlockPos.class, PlayerEntity.class, Hand.class, BlockHitResult.class); }
            catch(Exception ex) { throw new RuntimeException("this shouldn't be possible", ex); }
            if(onUseMethod.getDeclaringClass() != AbstractBlock.class)
            {
                LOGGER.debug("skip {} because it implements onUse", block);
                continue;
            }
            
            var seed = getSeed(server, block, ageProp);
            if(seed == null)
            {
                LOGGER.debug("skip {} because it has no seed", block);
                continue;
            }
            
            var maxAge = maxOf(ageProp);
            var vertical = isVertical(server.getOverworld(), block);
            LOGGER.debug("whitelist {} with seed {} ({})", block, seed, vertical ? "grows vertically" : String.format("grows to %d", maxAge));
            whitelist.put(regEntry.getKey().getValue(), new CropInfo(seed, vertical, ageProp, maxAge));
        }
    }

    static ActionResult onCropClicked(PlayerEntity player, World world, Hand hand, BlockHitResult hitResult)
    {
        if(world.isClient || !((ServerPlayerEntity)player).interactionManager.getGameMode().isSurvivalLike()) return ActionResult.PASS;
        
        // only work with an empty main hand
        // cheap fix to prevent weird bugs when trying to place plants on oneanother
        if(hand != Hand.MAIN_HAND || !player.getStackInHand(hand).isEmpty()) return ActionResult.PASS;
        
        var pos = hitResult.getBlockPos();
        var state = world.getBlockState(pos);
        var block = state.getBlock();
        var info = whitelist.get(Registry.BLOCK.getId(block));
        
        if(info == null) return ActionResult.PASS;
        
        List<ItemStack> drops;
        if(info.vertical)
        {
            var topPos = pos;
            while(true)
            {
                var up = topPos.up();
                if(world.getBlockState(up).getBlock() != block) break;
                topPos = up;
            }
            
            drops = new ArrayList<>();
            
            BlockPos stemPos = topPos;
            BlockState stemState = world.getBlockState(stemPos);
            for(int _n = 0; _n < 16; _n++) // bamboo only grows up to 16 blocks tall, so limit ourselves to that
            {
                // first we check what block is underneath us
                // if we're the bottom-most block then we should break, *not* adding drops
                var nextPos = stemPos.down();
                var nextState = world.getBlockState(nextPos);
                if(nextState.getBlock() != block)
                {
                    // reset the base block instead of the block that was clicked on
                    pos = stemPos;
                    break;
                }
                
                drops.addAll(getDrops(world, stemPos, stemState));
                world.setBlockState(stemPos, Blocks.AIR.getDefaultState());
                
                stemPos = nextPos;
                stemState = nextState;
            }
        }
        else
        {
            if(state.get(info.ageProp) < info.maxAge)
            {
                LOGGER.debug("skipping {} because it is not fully grown");
                return ActionResult.PASS;
            }
            
            drops = getDrops(world, pos, state);
            var foundSeed = false;
            
            for(var drop: drops)
                if(drop.isOf(info.seeds))
                {
                    drop.setCount(drop.getCount() - 1);
                    foundSeed = true;
                    break;
                }
            
            if(!foundSeed)
            {
                LOGGER.warn("no seed found! {} at {} (info {})", state, pos, info);
                return ActionResult.PASS;
            }
        }
        
        drops.removeIf(stack -> stack.getCount() <= 0);
        drops.forEach(stack -> giveOrDrop(stack, player));
        world.setBlockState(pos, state.with(info.ageProp, 0));
        
        return ActionResult.SUCCESS;
    }
    
    static void giveOrDrop(ItemStack stack, PlayerEntity ply)
    {
        if(!ply.getInventory().insertStack(stack)
            || stack.getCount() > 0) // also drop any remaining items if the player's inventory filled up
            ply.dropStack(stack);
    }
}
