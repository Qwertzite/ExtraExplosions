package qwertzite.extraexplosions.command.multiple;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.coordinates.WorldCoordinates;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TickEvent.LevelTickEvent;
import qwertzite.extraexplosions.core.ModLog;
import qwertzite.extraexplosions.core.command.CommandArgument;
import qwertzite.extraexplosions.core.command.CommandRegister;
import qwertzite.extraexplosions.util.math.EeMath;

public abstract class CommandMultipleExplosionBase {
	
	protected static final Set<Entry> ENTRIES = new HashSet<>();
	
	public static void tick(LevelTickEvent event) {
		if (event.phase == TickEvent.Phase.START) {
			ENTRIES.removeIf(Entry::tick);
		}
	}
	
	private CommandArgument<Double> posX1 = CommandArgument.doubleArg("x1")
			.setDefaultValue(ctx -> WorldCoordinates.current().getPosition(ctx.getSource()).x)
			.setDescription("X axis boundary 1");
	private CommandArgument<Double> posZ1 = CommandArgument.doubleArg("z1")
			.setDefaultValue(ctx -> WorldCoordinates.current().getPosition(ctx.getSource()).z)
			.setDescription("Z axis boundary 1");
	private CommandArgument<Double> posX2 = CommandArgument.doubleArg("x2")
			.setDefaultValue(ctx -> WorldCoordinates.current().getPosition(ctx.getSource()).x)
			.setDescription("X axis boundary 2");
	private CommandArgument<Double> posZ2 = CommandArgument.doubleArg("z2")
			.setDefaultValue(ctx -> WorldCoordinates.current().getPosition(ctx.getSource()).z)
			.setDescription("Z axis boundary 2");
	private CommandArgument<Float> height = CommandArgument.floatArg("height")
			.setDefaultValue(ctx -> 0.0f)
			.setDescription("Height from the surface.");
	private CommandArgument<Float> density = CommandArgument.floatArg("density", 0)
			.setDefaultValue(ctx -> 0.01f)
			.setDescription("Explosions per square block.");
	private CommandArgument<Float> explosionInterval = CommandArgument.floatArg("interval", 0)
			.setDefaultValue(ctx -> 2.0f)
			.setDescription("Interval between each explosion in seconds");
	protected CommandArgument<Float> intencity = CommandArgument.floatArg("intencity", 0)
			.setDefaultValue(ctx -> 4.0f)
			.setDescription("Intencity of explosion");
	protected CommandArgument<Boolean> signleExplosion = CommandArgument.flag("single")
			.setDefaultValue(ctx -> false)
			.setDescription("Ignore density and cause one explosion only.");
	
	/**
	 * add additional arguments and set usage string.
	 * @param explosionTypeName
	 * @return
	 */
	protected CommandRegister baseInit(String explosionTypeName) {
		
		return CommandRegister.$("bombard", explosionTypeName, ctx -> {
			double x1 = this.posX1.getValue();
			double z1 = this.posZ1.getValue();
			double x2 = this.posX2.getValue();
			double z2 = this.posZ2.getValue();
			float height = this.height.getValue();
			float spread = this.density.getValue();
			float rate = this.explosionInterval.getValue();
			boolean single = this.signleExplosion.getValue();
			
			var level = ctx.getSource().getLevel();
			
			var explosion = this.explosionProvider(ctx);
			var bb = new AABB(x1, height, z1, x2, height, z2);
			var count = single ? 1 : EeMath.randomRound(spread * bb.getXsize() * bb.getZsize(), level.getRandom());
			
			ENTRIES.add(new Entry(level, count, explosion, bb, rate));
			
			ModLog.info("Begin bombardment. (%f,%f) to (%f,%f), count %d".formatted(x1, z1, x2, z2, count));
			return Command.SINGLE_SUCCESS;
		})
				.addPositionalArguments(posX1).addPositionalArguments(posZ1)
				.addPositionalArguments(posX2).addPositionalArguments(posZ2)
				.addPositionalArguments(height).addPositionalArguments(density).addPositionalArguments(explosionInterval)
				.addPositionalArguments(intencity)
				.addOption(signleExplosion).setPermissionLevel(3);
	}
	
	protected abstract Consumer<Vec3> explosionProvider(CommandContext<CommandSourceStack> context);
	
	private static class Entry {
		private final Level level;
		private final Consumer<Vec3> explosion;
		private final AABB range;
		private final float rate;
		
		private int count;
		private float timer = 0.0f;
		
		/**
		 * 
		 * @param level the level in which explosions are caused.
		 * @param count number of explosions to cause.
		 * @param explosion Consumer to cause an explosion.
		 * @param range the area in which explosions are cause.
		 * @param interval interval between explosions in seconds. Must be positive and non-zero.
		 */
		public Entry(Level level, int count, Consumer<Vec3> explosion, AABB range, float interval) {
			this.level = level;
			this.explosion = explosion;
			this.range = range;
			this.rate = 1.0f/20 / interval;
			this.count = count;
			assert(interval > 0.0f);
		}
		
		/**
		 * 
		 * @return true if completed
		 */
		public boolean tick() {
			for (;this.timer >= 0.0f; timer -= 1.0f) {
				if (this.count-- <= 0) {
					ModLog.info("Finished causing explosions.");
					return true;
				}
				System.out.println(count);
				
				try {
					int posX = Mth.floor(Mth.lerp(this.level.getRandom().nextDouble(), this.range.minX, this.range.maxX));
					int posZ = Mth.floor(Mth.lerp(this.level.getRandom().nextDouble(), this.range.minZ, this.range.maxZ));
					int elev = this.level.getHeight(Types.WORLD_SURFACE, posX, posZ);
					
					this.explosion.accept(new Vec3(posX, elev + range.minY, posZ));
				} catch(Exception e) {
					ModLog.error("Caught an exception while causing explosion. Aborting bombardment.", e);
					return true;
				}
			}
			this.timer += rate;
			return false;
		}
	}
}
