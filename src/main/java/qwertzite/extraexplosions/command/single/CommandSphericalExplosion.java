package qwertzite.extraexplosions.command.single;

import com.mojang.brigadier.Command;

import net.minecraft.commands.arguments.coordinates.WorldCoordinates;
import net.minecraft.world.level.Explosion.BlockInteraction;
import net.minecraft.world.phys.Vec3;
import qwertzite.extraexplosions.api.ExtraExplosions;
import qwertzite.extraexplosions.core.ModLog;
import qwertzite.extraexplosions.core.command.CommandArgument;
import qwertzite.extraexplosions.core.command.CommandRegister;

public class CommandSphericalExplosion {

	public void init() {
		CommandArgument<Vec3> pos = CommandArgument.coord("pos")
				.setDefaultValue(ctx -> WorldCoordinates.current().getPosition(ctx.getSource()))
				.setDescription("Centre coordinate of explosion.");
		CommandArgument<Float> intencity = CommandArgument.floatArg("intencity", 0)
				.setDefaultValue(ctx -> 4.0f)
				.setDescription("Intencity of explosion");
		CommandArgument<Boolean> fireArg = CommandArgument.flag("fire")
				.setDefaultValue(ctx -> false)
				.setDescription("Cause fire on explosion.");
		
		CommandRegister.$("explosion", "spherical", ctx -> {
			var position = pos.getValue();
			try {
				ExtraExplosions.sphericalExplosion(ctx.getSource().getLevel(), ctx.getSource().getEntity(),
						position.x(), position.y(), position.z(),
						intencity.getValue(), fireArg.getValue(), BlockInteraction.DESTROY);
				ModLog.info("Caused spherically ray-traced explosion at %s with intencity %f", position, intencity.getValue());
			} catch(Exception e) {
				ModLog.error("Caught an exception while causing explosion.", e);
			}
			return Command.SINGLE_SUCCESS;
		})
		.addPositionalArguments(pos).addPositionalArguments(intencity).addOption(fireArg).setPermissionLevel(3)
		.setUsageString("Creates explosion with spherical ray tracing.");
	}
}
