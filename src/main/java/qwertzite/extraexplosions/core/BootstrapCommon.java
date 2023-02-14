package qwertzite.extraexplosions.core;

import net.minecraftforge.eventbus.api.IEventBus;
import qwertzite.extraexplosions.ExtraExplosionCore;
import qwertzite.extraexplosions.core.command.CommandRegister;
import qwertzite.extraexplosions.core.network.ModNetwork;
import qwertzite.extraexplosions.exp.dummy.CommandDummyExplosion;
import qwertzite.extraexplosions.exp.dummy.PacketDummyExplosion;
import qwertzite.extraexplosions.exp.spherical.CommandSphericalExplosion;
import qwertzite.extraexplosions.exp.spherical.PacketSphericalExplosion;
import qwertzite.extraexplosions.exp.vanilla.CommandVanillaExplosion;

public class BootstrapCommon {
	
	public void init(IEventBus modEventBus) {
		CommandRegister.initialise(modEventBus, ExtraExplosionCore.MODID);
		
		ModNetwork.registerPacket(PacketDummyExplosion.class);
		ModNetwork.registerPacket(PacketSphericalExplosion.class);
		
		new CommandVanillaExplosion().init();
		new CommandDummyExplosion().init();
		new CommandSphericalExplosion().init();
	}
}
