package qwertzite.extraexplosions.core;

import net.minecraftforge.eventbus.api.IEventBus;
import qwertzite.extraexplosions.ExtraExplosionCore;
import qwertzite.extraexplosions.core.command.CommandRegister;
import qwertzite.extraexplosions.core.network.ModNetwork;
import qwertzite.extraexplosions.exp.nodamage.CommandDummyExplosion;
import qwertzite.extraexplosions.exp.nodamage.PacketDummyExplosion;

public class BootstrapCommon {
	
	public void init(IEventBus modEventBus) {
		CommandRegister.initialise(modEventBus, ExtraExplosionCore.MODID);
		
		ModNetwork.registerPacket(PacketDummyExplosion.class);
		
		new CommandDummyExplosion().init();
		
	}
}
