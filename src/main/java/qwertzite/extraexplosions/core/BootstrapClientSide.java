package qwertzite.extraexplosions.core;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import qwertzite.extraexplosions.core.debug.DebugRenderer;

public class BootstrapClientSide extends BootstrapCommon {

	@Override
	public void init(IEventBus modEventBus) {
		super.init(modEventBus);
		
		MinecraftForge.EVENT_BUS.register(new DebugRenderer());
	}
}
