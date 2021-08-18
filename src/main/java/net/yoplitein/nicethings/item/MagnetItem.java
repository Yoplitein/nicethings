package net.yoplitein.nicethings.item;

import java.util.UUID;

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
	private static final double RANGE = 10.0; // range of the magnet, in blocks
	private static final int COOLDOWN_TICKS = 5; // number of ticks between scanning for items
	private static final int PICKUP_DELAY_TICKS = 100; // immunity ticks for items dropped by the player
	private static final int VANILLA_PICKUP_DELAY = 40; // vanilla pickup delay for items dropped by entities
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
		final var playerUUID = player.getUuid();
		final var items = world.getEntitiesByClass(ItemEntity.class, box, e -> isItemAffected(e, playerUUID));
		
		final var pos = player.getPos();
		for(var item: items) item.setPosition(pos);
	}
	
	private static boolean isItemAffected(ItemEntity item, UUID playerUUID)
	{
		if(playerUUID.equals(item.getThrower()))
			// if this item was thrown by the player holding the magnet, they probably
			// don't want to pick it right back up, so we apply some extra delay
			return item.getItemAge() > VANILLA_PICKUP_DELAY + PICKUP_DELAY_TICKS;
		else
			// otherwise default behaviour is to magnet items as soon as they can be picked up by anyone
			return !item.cannotPickup();
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
