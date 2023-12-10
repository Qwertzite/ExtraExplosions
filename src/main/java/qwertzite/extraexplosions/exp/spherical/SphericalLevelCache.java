package qwertzite.extraexplosions.exp.spherical;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import qwertzite.extraexplosions.util.LevelCache;

public class SphericalLevelCache extends LevelCache<qwertzite.extraexplosions.exp.spherical.SphericalLevelCache.BlockProperty> {

	public SphericalLevelCache(Level level) {
		super(level);
	}

	@Override
	protected BlockProperty createNewPropertyInstance() {
		return new BlockProperty();
	}

	@Override
	protected void initProperty(BlockProperty prop, BlockPos pos) {
		prop.isInWorldBounds = this.level.isInWorldBounds(pos);
		prop.blockState = this.level.getBlockState(pos);
		prop.fluidState = this.level.getFluidState(pos);
	}

	public static class BlockProperty extends LevelCache.BlockProperty {
		private boolean isInWorldBounds;
		private BlockState blockState;
		private FluidState fluidState;
		
		public boolean isInWorldBounds() { return this.isInWorldBounds; }
		public BlockState getBlockState() { return this.blockState; }
		public FluidState getFluidState() { return this.fluidState; }
	}
}
