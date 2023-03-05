package qwertzite.extraexplosions.exp.spherical;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public class BlockPropertyCache {
	
	private final Level level;
	private final Map<BlockPos, BlockProperty> cache;
	
	public BlockPropertyCache(Level level) {
		this.level = level;
		this.cache = new HashMap<>();
//		this.cache = new ConcurrentHashMap<>();
	}
	
	public BlockProperty getBlockProperty(BlockPos blockpos) {
		var blockProperty = cache.computeIfAbsent(blockpos, p -> new BlockProperty());
//		synchronized (blockProperty) {
			if (!blockProperty.initialised) {
				blockProperty.initialised = true;
				blockProperty.isInWorldBounds = this.level.isInWorldBounds(blockpos);
				blockProperty.blockState = this.level.getBlockState(blockpos);
				blockProperty.fluidState = this.level.getFluidState(blockpos);
			}
//		}
		return blockProperty;
	}
	
	public static class BlockProperty {
		private boolean initialised;
		private boolean isInWorldBounds;
		private BlockState blockState;
		private FluidState fluidState;
		
		public boolean isInWorldBounds() { return this.isInWorldBounds; }
		public BlockState getBlockState() { return this.blockState; }
		public FluidState getFluidState() { return this.fluidState; }
	}
}
