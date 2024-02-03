package qwertzite.extraexplosions.command.multiple;

import java.util.function.Consumer;

import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.level.Explosion.BlockInteraction;
import net.minecraft.world.phys.Vec3;
import qwertzite.extraexplosions.core.command.CommandArgument;

public class CommandMultiVanilla extends CommandMultipleExplosionBase {
	
	private CommandArgument<Boolean> fireArg = CommandArgument.flag("fire")
			.setDefaultValue(ctx -> false)
			.setDescription("Cause fire on explosion.");
	
	public void init() {
		
		super.baseInit("vanilla").addOption(fireArg)
		.setUsageString("Randomly spreads multiple vanilla explosions.");
		
		
	}
	
	@Override
	protected Consumer<Vec3> explosionProvider(CommandContext<CommandSourceStack> context) {
		var level = context.getSource().getLevel();
		var entity = context.getSource().getEntity();
		var intencity = super.intencity.getValue();
		var fire = this.fireArg.getValue();
		
		return pos -> level.explode(entity,
				pos.x(), pos.y(), pos.z(),
				intencity, fire, BlockInteraction.DESTROY);
	}

}
