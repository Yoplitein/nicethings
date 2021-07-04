package net.yoplitein.nicethings;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.fabricmc.api.ModInitializer;

public class NiceThings implements ModInitializer
{
	public static final Logger LOGGER = LogManager.getLogger();
	
	@Override
	public void onInitialize()
	{
		LOGGER.info("hello from nice things");
		
		CropClick.initialize();
		Twerking.initialize();
	}
}
