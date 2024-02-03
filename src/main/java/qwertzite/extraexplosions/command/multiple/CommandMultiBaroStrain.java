package qwertzite.extraexplosions.command.multiple;

import java.util.function.Consumer;

import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.level.Explosion.BlockInteraction;
import net.minecraft.world.phys.Vec3;
import qwertzite.extraexplosions.api.ExtraExplosions;

public class CommandMultiBaroStrain extends CommandMultipleExplosionBase {
	
	public void init() {
		
		super.baseInit("baro_strain")
		.setUsageString("Randomly spreads multiple FEM explosions.");
		
		
	}
	
	@Override
	protected Consumer<Vec3> explosionProvider(CommandContext<CommandSourceStack> context) {
		var level = context.getSource().getLevel();
		var entity = context.getSource().getEntity();
		var intencity = super.intencity.getValue();
		
			return position -> ExtraExplosions.baroStrainExplosion(level, entity,
				position.x(), position.y(), position.z(),
				intencity, BlockInteraction.DESTROY);
	}

}
