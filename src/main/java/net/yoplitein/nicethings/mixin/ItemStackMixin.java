package net.yoplitein.nicethings.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.item.ItemStack;
import net.yoplitein.nicethings.item.MagnetItem;

@Mixin(ItemStack.class)
public class ItemStackMixin
{
	@Inject(method = "areEqual", at = @At("HEAD"), cancellable = true, require = 1, allow = 1)
	private static void areEqual(ItemStack left, ItemStack right, CallbackInfoReturnable<Boolean> cir)
	{
		if(left.getItem() instanceof MagnetItem && right.getItem() instanceof MagnetItem)
		{
			cir.setReturnValue(
				MagnetItem.isActivated(left.getTag()) ==
				MagnetItem.isActivated(right.getTag())
			);
		}
	}
}
