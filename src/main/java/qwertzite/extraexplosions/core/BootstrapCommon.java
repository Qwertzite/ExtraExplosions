package qwertzite.extraexplosions.core;

import net.minecraftforge.eventbus.api.IEventBus;
import qwertzite.extraexplosions.ExtraExplosionCore;
import qwertzite.extraexplosions.command.EeCommands;
import qwertzite.extraexplosions.core.command.CommandRegister;
import qwertzite.extraexplosions.core.network.ModNetwork;
import qwertzite.extraexplosions.exp.barostrain.PacketBaroStrainExplosion;
import qwertzite.extraexplosions.exp.dummy.PacketDummyExplosion;
import qwertzite.extraexplosions.exp.spherical.PacketSphericalExplosion;

public class BootstrapCommon {
	
	public void init(IEventBus modEventBus) {
		CommandRegister.initialise(modEventBus, ExtraExplosionCore.MODID);
		
		ModNetwork.registerPacket(PacketDummyExplosion.class);
		ModNetwork.registerPacket(PacketSphericalExplosion.class);
		ModNetwork.registerPacket(PacketBaroStrainExplosion.class);

		EeCommands.init();
	}
}
