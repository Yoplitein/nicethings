package net.yoplitein.nicethings;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.loot.v1.FabricLootSupplierBuilder;
import net.fabricmc.fabric.api.loot.v1.event.LootTableLoadingCallback.LootTableSetter;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.AbstractPlantPartBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CropBlock;
import net.minecraft.block.StemBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootManager;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.resource.ResourceManager;
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
import net.minecraft.world.GameMode;
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
    
    public static record CropInfo(Item seeds, IntProperty ageProp, int maxAge) {}
    
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
    
    static void buildWhitelist(MinecraftServer server)
    {
        for(var regEntry: Registry.BLOCK.getEntries())
        {
            var block = regEntry.getValue();
            if(block instanceof StemBlock || block instanceof AbstractPlantPartBlock)
                continue;
            
            var state = block.getDefaultState();
            IntProperty ageProp = null;
            
            outer: for(var prop: state.getProperties())
                for(var testAgeProp: allAgeProps)
                    if(prop == testAgeProp) {
                        ageProp = (IntProperty)prop;
                        break outer;
                    }
            
            if(ageProp == null) continue;
            
            Method onUseMethod = null;
            try { onUseMethod = block.getClass().getMethod("onUse", BlockState.class, World.class, BlockPos.class, PlayerEntity.class, Hand.class, BlockHitResult.class); }
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
            
            // TODO check if vertical (block.canPlaceOn(block))
            
            var maxAge = maxOf(ageProp);
            LOGGER.debug("whitelist {} with seed {} (grows to {})", block, seed, maxAge);
            whitelist.put(regEntry.getKey().getValue(), new CropInfo(seed, ageProp, maxAge));
        }
    }
    
    static ActionResult onCropClicked(PlayerEntity player, World world, Hand hand, BlockHitResult hitResult)
    {
        GameMode gameMode = world.isClient ? MinecraftClient.getInstance().interactionManager.getCurrentGameMode() : ((ServerPlayerEntity)player).interactionManager.getGameMode();
        if(/* TODO: network whitelist */ world.isClient || !gameMode.isSurvivalLike()) return ActionResult.PASS;
        
        var pos = hitResult.getBlockPos();
        var state = world.getBlockState(pos);
        var block = state.getBlock();
        var info = whitelist.get(Registry.BLOCK.getId(block));
        
        if(info == null) return ActionResult.PASS;
        
        if(state.get(info.ageProp) < info.maxAge)
        {
            LOGGER.debug("skipping {} because it is not fully grown");
            return ActionResult.PASS;
        }
        
        LOGGER.info("plant {} clicked at {} (info {})", state, pos, info);
        
        var ctx = new LootContext.Builder((ServerWorld)world)
            .parameter(LootContextParameters.ORIGIN, Vec3d.ZERO)
            .parameter(LootContextParameters.TOOL, ItemStack.EMPTY)
        ;
        List<ItemStack> drops = state.getDroppedStacks(ctx);
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
        
        drops.removeIf(stack -> stack.getCount() <= 0);
        drops.forEach(stack -> giveOrDrop(stack, player));
        world.setBlockState(pos, state.with(info.ageProp, 0));
        
        return ActionResult.PASS;
    }
    
    static void giveOrDrop(ItemStack stack, PlayerEntity ply)
    {
        if(!ply.getInventory().insertStack(stack)
            || stack.getCount() > 0) // also drop any remaining items if the player's inventory filled up
            ply.dropStack(stack);
    }
}
