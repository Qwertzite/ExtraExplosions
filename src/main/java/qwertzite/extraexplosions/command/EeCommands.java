package qwertzite.extraexplosions.command;

import net.minecraftforge.common.MinecraftForge;
import qwertzite.extraexplosions.command.multiple.CommandMultiBaroStrain;
import qwertzite.extraexplosions.command.multiple.CommandMultiVanilla;
import qwertzite.extraexplosions.command.multiple.CommandMultipleExplosionBase;
import qwertzite.extraexplosions.command.single.CommandBaroStrainExplosion;
import qwertzite.extraexplosions.command.single.CommandDummyExplosion;
import qwertzite.extraexplosions.command.single.CommandSphericalExplosion;
import qwertzite.extraexplosions.command.single.CommandVanillaExplosion;

public class EeCommands {
	public static void init() {
		new CommandVanillaExplosion().init();
		new CommandDummyExplosion().init();
		new CommandSphericalExplosion().init();
		new CommandBaroStrainExplosion().init();
		
		MinecraftForge.EVENT_BUS.addListener(CommandMultipleExplosionBase::tick);
		new CommandMultiVanilla().init();
		new CommandMultiBaroStrain().init();
	}
}
