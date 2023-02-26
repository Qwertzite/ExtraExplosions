package qwertzite.extraexplosions.exp.barostrain;

import com.mojang.brigadier.Command;

import net.minecraft.commands.arguments.coordinates.WorldCoordinates;
import net.minecraft.world.level.Explosion.BlockInteraction;
import net.minecraft.world.phys.Vec3;
import qwertzite.extraexplosions.api.ExtraExplosions;
import qwertzite.extraexplosions.core.ModLog;
import qwertzite.extraexplosions.core.command.CommandArgument;
import qwertzite.extraexplosions.core.command.CommandRegister;

public class CommandBaroStrainExplosion {

	public void init() {
		CommandArgument<Vec3> pos = CommandArgument.coord("pos")
				.setDefaultValue(ctx -> WorldCoordinates.current().getPosition(ctx.getSource()))
				.setDescription("Centre coordinate of explosion.");
		CommandArgument<Float> intencity = CommandArgument.floatArg("intencity", 0)
				.setDefaultValue(ctx -> 4.0f)
				.setDescription("Intencity of explosion");
		
		CommandRegister.$("explosion", "baro_strain", ctx -> {
			var position = pos.getValue();
			try {
				ExtraExplosions.baroStrainExplosion(ctx.getSource().getLevel(), ctx.getSource().getEntity(),
						position.x(), position.y(), position.z(),
						intencity.getValue(), BlockInteraction.DESTROY);
				ModLog.info("Caused baro-strain explosion at %s with intencity %f", position, intencity.getValue());
			} catch(Exception e) {
				ModLog.error("Caught an exception while causing explosion.", e);
			}
			return Command.SINGLE_SUCCESS;
		})
		.addPositionalArguments(pos).addPositionalArguments(intencity).setPermissionLevel(3)
		.setUsageString("Creates explosion which simulates pressure and strain using FEM.");
	}
}
