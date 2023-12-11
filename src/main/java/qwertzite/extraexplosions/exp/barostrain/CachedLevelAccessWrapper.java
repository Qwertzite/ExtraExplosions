package qwertzite.extraexplosions.exp.barostrain;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.Collectors;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public class CachedLevelAccessWrapper {
	
	private final Thread mainThread; 
	private final Level level;
	private final Explosion explosion;
	private final Map<BlockPos, BlockProperty> blockProperty;
	
	private final ConcurrentLinkedQueue<Entry> queue = new ConcurrentLinkedQueue<>();
	private volatile boolean executeJob;
	
	public CachedLevelAccessWrapper(Thread serverThread, Level level, Explosion explosion) {
		this.mainThread = serverThread;
		this.level = level;
		this.explosion = explosion;
		this.blockProperty = new ConcurrentHashMap<>();
	}
	
	public boolean isCurrentlyAirAt(BlockPos pos) {
		var blockProperty = this.getBlockProperty(pos);
		return blockProperty.isAir || blockProperty.destroyed;
	}
	
	public void setDestroyed(BlockPos pos) {
		var blockProperty = this.getBlockProperty(pos);
		blockProperty.setDestroyed();
	}
	
	public Set<BlockPos> getDestroyeds() {
		return this.blockProperty.entrySet().parallelStream().filter(e -> e.getValue().destroyed).map(e -> e.getKey()).collect(Collectors.toSet());
	}
	
	/**
	 * Can only be executed on specific threads.
	 * @param pos
	 * @return
	 */
	public BlockProperty getBlockProperty(BlockPos pos) {
		var prop =  this.blockProperty.computeIfAbsent(pos, p -> new BlockProperty());
		synchronized (prop) { // initialisation
			while (!prop.initialised) {
				var currentThread = Thread.currentThread();
				if (currentThread == this.mainThread) {
					this.initProperty(prop, pos);
					break;
				}
				
				var entry = new Entry(prop, pos, currentThread);
				this.queue.add(entry);
				
				synchronized (this) { this.notify(); }
				LockSupport.park();
			}
//			if (blockProperty.blockState == null) System.out.println("warn!!!");
		}
//		if (blockProperty.blockState == null || blockProperty.fluidState == null) System.out.println("warn!!"); // DEBUG
		return prop;
	}
	
	public void awaitJob() {
		this.executeJob = true;
		while (this.executeJob) {
			while (!this.queue.isEmpty()) {
				var entry = this.queue.poll();
				this.initProperty(entry.blockProperty, entry.blockpos);
//				synchronized (entry) { entry.notify(); }
				LockSupport.unpark(entry.thread());
			}
			
			synchronized (this) {
				if (!this.queue.isEmpty()) continue;
				try {
//					this.wait();
					this.wait(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	private void initProperty(BlockProperty prop, BlockPos pos) {
		if (prop.initialised) return;
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
	
	public void endJobExecution() {
		this.executeJob = false;
		synchronized (this) {
			this.notify();
		}
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
		
		public double getMass() { return this.resistance / ((this.hardness + 1.0f) * 16); }
	}
	
	private static record Entry(BlockProperty blockProperty, BlockPos blockpos, Thread thread) {
		
	}
}
