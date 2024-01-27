package qwertzite.extraexplosions.exp.barostrain;

import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import qwertzite.extraexplosions.util.LevelCache;

public class BarostrainLevelCache extends LevelCache<qwertzite.extraexplosions.exp.barostrain.BarostrainLevelCache.BlockProperty> {

	private final Explosion explosion;
	
	public BarostrainLevelCache(Level level, Explosion explosion) {
		super(level);
		this.explosion = explosion;
	}

	@Override
	protected BlockProperty createNewPropertyInstance() {
		return new BlockProperty();
	}

	@Override
	protected void initProperty(BlockProperty prop, BlockPos pos) {
		if (!this.level.isInWorldBounds(pos)) {
			prop.isAir = true;
		} else {
			BlockState blockstate = this.level.getBlockState(pos);
			FluidState fluidstate = this.level.getFluidState(pos);
			if (blockstate.isAir() && fluidstate.isEmpty()) {
				prop.isAir = true;
			} else {
				double resistance = Math.max(
						blockstate.getExplosionResistance(this.level, pos, this.explosion),
						fluidstate.getExplosionResistance(this.level, pos, this.explosion));
				prop.resistance = resistance;
				double hardness = blockstate.getDestroySpeed(level, pos);
				if (hardness < 0) hardness = Double.POSITIVE_INFINITY;
				if (blockstate.getMaterial().isLiquid()) hardness /= 100.0d;
				prop.hardness = hardness;
			}
		}
	}
	
	public boolean isCurrentlyAirAt(BlockPos pos) {
		var blockProperty = this.getBlockProperty(pos);
		return blockProperty.isAir || blockProperty.destroyed;
	}
	
	public boolean wasOriginallyAirAt(BlockPos pos) {
		var blockProperty = this.getBlockProperty(pos);
		return blockProperty.isAir && (!blockProperty.destroyed);
	}
	
	public void setDestroyed(BlockPos pos) {
		var blockProperty = this.getBlockProperty(pos);
		blockProperty.setDestroyed();
	}
	
	public Set<BlockPos> getDestroyeds() {
		return this.getCache().entrySet().parallelStream().filter(e -> e.getValue().destroyed).map(e -> e.getKey()).collect(Collectors.toSet());
	}
	
	public static class BlockProperty extends LevelCache.BlockProperty {
		private boolean isAir;
		private double resistance;
		private double hardness;
		
		/** Indicates that block state has changed from non air block to air block. */
		private boolean destroyed;
		
		public double getResistance() { return this.resistance; }
		public double getHardness() { return this.hardness; }
		private synchronized void setDestroyed() {
			if (!this.isAir) this.destroyed = true;
			this.isAir = true;
			this.resistance = 0.0d;
			this.hardness = 0.0d;
		}
		
		protected double getYoungsModulus() {
			return hardness != 0 ? resistance / hardness : 0.0d;
		}
		
		protected double getPoissonCoeff() {
			return 0.5d * hardness / (resistance + 1.0d);
		}
		
//		public double getTolerance() {
//			boolean destroyed = true;
//			double resistance = 0.0d;
//			for (BlockPos elem : glbVertex.getBelongingElements()) {
//				if (!this.isDestoryed(elem)) {
//					destroyed = false;
//					resistance = Math.max(resistance, this.resistance(elem) / 16);
//				}
//			}
//			if (destroyed) resistance = Double.MAX_VALUE / 16;
//			return resistance;
//		}

		public double getMuForElement() { // OPTIMIZE: cache?
			double youngsModulus = this.getYoungsModulus();
			double poissonCoeffs = this.getPoissonCoeff();
			return youngsModulus / (2*(1 + poissonCoeffs));
		}

		public double getLambdaForElement() { // OPTIMIZE: cache?
			double youngsModulus = this.getYoungsModulus();
			double poissonCoeffs = this.getPoissonCoeff();
			return youngsModulus * poissonCoeffs / ((1 + poissonCoeffs)*(1 - 2*poissonCoeffs));
		}
		
		public double getSigmaYield() { return this.hardness; }
		
		public double getMass() { return this.resistance / 12; }
//		public double getMass() { return this.resistance / ((this.hardness + 1.0f) * 16); }
	}
}
