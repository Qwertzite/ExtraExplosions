package qwertzite.extraexplosions.exp.spherical;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.LockSupport;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public class BlockPropertyCache {
	
	private final Level level;
	private final Map<BlockPos, BlockProperty> cache;
	
	@SuppressWarnings("unused")
	private final Thread mainThread;
	private final ConcurrentLinkedQueue<Entry> queue = new ConcurrentLinkedQueue<>();
	
	private volatile boolean executeJob;
	
	public BlockPropertyCache(Level level) {
		this.level = level;
		this.cache = new ConcurrentHashMap<>();
		this.mainThread = Thread.currentThread();
	}
	
	public BlockProperty getBlockProperty(BlockPos blockpos) {
		var blockProperty = cache.computeIfAbsent(blockpos, p -> new BlockProperty());
		
		synchronized (blockProperty) {
			while (!blockProperty.initialised) {
				var currentThread = Thread.currentThread();
				var entry = new Entry(blockProperty, blockpos, currentThread);
				this.queue.add(entry);
				
				synchronized (this) { this.notify(); }
				LockSupport.park();
			}
			if (blockProperty.blockState == null) System.out.println("warn!!!");
		}
		if (blockProperty.blockState == null || blockProperty.fluidState == null) System.out.println("warn!!"); // DEBUG
		return blockProperty;
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
	
	private void initProperty(BlockProperty blockProperty, BlockPos blockpos) {
		blockProperty.isInWorldBounds = this.level.isInWorldBounds(blockpos); // DEBUG
		blockProperty.blockState = this.level.getBlockState(blockpos);
		blockProperty.fluidState = this.level.getFluidState(blockpos);
		blockProperty.initialised = true;
	}
	
	public void endJobExecution() {
		this.executeJob = false;
		synchronized (this) {
			this.notify();
		}
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
	
	private static record Entry(BlockProperty blockProperty, BlockPos blockpos, Thread thread) {
		
	}
	
	static {
		@SuppressWarnings("unused")
		Class<?> ensureLoaded = LockSupport.class;
	}
}
