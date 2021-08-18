package net.yoplitein.nicethings.item;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

public class MagnetItem extends Item
{
	private static final double RANGE = 10.0;
	private static final int COOLDOWN_TICKS = 5;
	private static final String TAG_ACTIVE = "magnet_active";
	private static final String TAG_COOLDOWN = "magnet_cooldown";
	
	public MagnetItem(Settings settings)
	{
		super(settings);
	}
	
	@Override
	public boolean shouldSyncTagToClient()
	{
		return true;
	}
	
	@Override
	public boolean isEnchantable(ItemStack stack)
	{
		return false;
	}
	
	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand)
	{
		final var stack = player.getStackInHand(hand);
		if(!player.isSneaking()) return TypedActionResult.pass(stack);
		
		final var tag = stack.getOrCreateTag();
		final var activated = isActivated(tag);
		
		tag.putBoolean(TAG_ACTIVE, !activated);
		world.playSound(
			player,
			player.getBlockPos(),
			SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
			SoundCategory.PLAYERS,
			1.0f,
			activated ? 0.75f : 1.15f
		);
		return TypedActionResult.consume(stack);
	}
	
	@Override
	public boolean hasGlint(ItemStack stack)
	{
		if(!stack.hasTag()) return false;
		
		final var tag = stack.getTag();
		return tag.contains(TAG_ACTIVE, NbtType.BYTE) && tag.getBoolean(TAG_ACTIVE);
	}
	
	@Override
	public void inventoryTick(ItemStack stack, World world, Entity player, int slot, boolean selected)
	{
		final var tag = stack.getTag();
		if(world.isClient || !isActivated(tag)) return;
		
		final var cooldown = !tag.contains(TAG_COOLDOWN, NbtType.BYTE) ? 0 : tag.getByte(TAG_COOLDOWN);
		if(cooldown > 0)
		{
			tag.putByte(TAG_COOLDOWN, (byte)(cooldown - 1));
			return;
		}
		else
			tag.putByte(TAG_COOLDOWN, (byte)COOLDOWN_TICKS);
		
		final var box = new Box(player.getBlockPos()).expand(RANGE);
		final var items = world.getEntitiesByClass(ItemEntity.class, box, e -> !e.cannotPickup());
		
		final var pos = player.getPos();
		for(var item: items) item.setPosition(pos);
	}
	
	public static boolean isActivated(@Nullable NbtCompound tag)
	{
		return
			tag != null &&
			tag.contains(TAG_ACTIVE, NbtType.BYTE) &&
			tag.getBoolean(TAG_ACTIVE)
		;
	}
}
