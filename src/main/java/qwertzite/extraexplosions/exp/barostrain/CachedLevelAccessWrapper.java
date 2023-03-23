package qwertzite.extraexplosions.exp.barostrain;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public class CachedLevelAccessWrapper {
	
	private final Level level;
	private final Explosion explosion;
	private final Map<BlockPos, BlockProperty> blockProperty;
	
	public CachedLevelAccessWrapper(Level level, Explosion explosion) {
		this.level = level;
		this.explosion = explosion;
		this.blockProperty = new ConcurrentHashMap<>();
	}
	
	public boolean isCurrentlyAirAt(BlockPos pos) {
		var blockProperty = this.getBlockPropertyAt(pos);
		return blockProperty.isAir || blockProperty.destroyed;
	}
	
	public void setDestroyed(BlockPos pos) {
		var blockProperty = this.getBlockPropertyAt(pos);
		blockProperty.setDestroyed();
	}
	
	public BlockProperty getBlockPropertyAt(BlockPos pos) {
		var prop =  this.blockProperty.computeIfAbsent(pos, p -> new BlockProperty());
		synchronized (prop) { // initialisation
			if (!prop.initialised) {
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
				prop.initialised = true;
			}
		}
		return prop;
	}
	

	
	
	public static class BlockProperty {
		private boolean isAir;
		private double resistance;
		private double hardness;
		private boolean initialised;
		
		/** Indicates that block state has changed from non air block to air block. */
		private boolean destroyed;
		
		public double getResistance() { return this.resistance; }
		public double getHardness() { return this.hardness; }
		private void setDestroyed() { 
			if (!this.isAir) this.destroyed = true;
			this.isAir = true;
			this.resistance = 0.0d;
			this.hardness = 0.0d;
		}
		
		protected double getYoungsModulus() {
			return hardness != 0 ? resistance / hardness : 0.0d;
		}
		
		protected double getPoissonCoeff() {
			return hardness != 0 ? 0.5 * hardness / (hardness + 1.0d) * 3.0d / (resistance / hardness + 2.0d) : 0.0d;
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
		
		public double getMass() { return this.resistance / this.hardness / 16; }
	}
}
