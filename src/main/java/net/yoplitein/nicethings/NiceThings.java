package net.yoplitein.nicethings;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.world.poi.PointOfInterestHelper;
import net.minecraft.block.Material;
import net.minecraft.item.BlockItem;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.poi.PointOfInterestType;
import net.yoplitein.nicethings.block.MegatorchBlock;

public class NiceThings implements ModInitializer
{
	public static final String MODID = "nicethings";
	public static final Logger LOGGER = LogManager.getLogger();
	
	public static final MegatorchBlock MEGATORCH_BLOCK = new MegatorchBlock(
			FabricBlockSettings
				.of(Material.DECORATION)
				.hardness(0.5f)
				.resistance(1200.0f) // no creepering allowed
				.luminance(15)
				.sounds(BlockSoundGroup.WOOD)
	);
	
	public static PointOfInterestType MEGATORCHES_POI;
	
	@Override
	public void onInitialize()
	{
		LOGGER.info("hello from nice things");
		LOGGER.info("<loglevels>");
		LOGGER.trace("trace");
		LOGGER.debug("debug");
		LOGGER.info("info");
		LOGGER.warn("warn");
		LOGGER.error("error");
		LOGGER.info("</loglevels>");
		
		Registry.register(Registry.BLOCK, new Identifier(MODID, "megatorch"), MEGATORCH_BLOCK);
		Registry.register(Registry.ITEM, new Identifier(MODID, "megatorch"), new BlockItem(MEGATORCH_BLOCK, new FabricItemSettings().fireproof()));
		
		MEGATORCHES_POI = PointOfInterestHelper.register(new Identifier(MODID, "megatorches"), 0, 1, MEGATORCH_BLOCK);
		
		CropClick.initialize();
		Twerking.initialize();
	}
}
