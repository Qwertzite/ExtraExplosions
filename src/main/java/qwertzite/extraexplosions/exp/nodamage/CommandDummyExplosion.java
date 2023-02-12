package qwertzite.extraexplosions.exp.nodamage;

import com.mojang.brigadier.Command;

import net.minecraft.commands.arguments.coordinates.WorldCoordinates;
import net.minecraft.world.phys.Vec3;
import qwertzite.extraexplosions.api.ExtraExplosions;
import qwertzite.extraexplosions.core.ModLog;
import qwertzite.extraexplosions.core.command.CommandArgument;
import qwertzite.extraexplosions.core.command.CommandRegister;

public class CommandDummyExplosion {
	
	public void init() {
		CommandArgument<Vec3> pos = CommandArgument.coord("pos")
				.setDefaultValue(ctx -> WorldCoordinates.current().getPosition(ctx.getSource()))
				.setDescription("Centre coordinate of explosion.");
		CommandArgument<Float> intencity = CommandArgument.floatArg("intencity", 0)
				.setDefaultValue(ctx -> 4.0f)
				.setDescription("Intencity of explosion");
		
		CommandRegister.$("explosion", "dummy", ctx -> {
			var position = pos.getValue();
			ExtraExplosions.dummyExplosion(ctx.getSource().getLevel(), ctx.getSource().getEntity(),
					position.x(), position.y(), position.z(),
					intencity.getValue());
			ModLog.info("Caused dummy explosion at %s with intencity %f", position, intencity.getValue());
			return Command.SINGLE_SUCCESS;
		})
		.addPositionalArguments(pos).addPositionalArguments(intencity).setPermissionLevel(3)
		.setUsageString("Creates explosion which does not harm entities nor destroy blocks.");
	}
}
