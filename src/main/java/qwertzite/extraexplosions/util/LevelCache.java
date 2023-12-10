package qwertzite.extraexplosions.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.locks.LockSupport;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import qwertzite.extraexplosions.core.ModLog;

public abstract class LevelCache<T extends LevelCache.BlockProperty> {
	
	private static final ForkJoinPool THREAD_POOL = new ForkJoinPool(Runtime.getRuntime().availableProcessors() * 2);
	
	public static void execute(LevelCache<?> cache, Runnable job) {
		Future<?> task = THREAD_POOL.submit(() -> {
			try {
				job.run();
			} catch (Exception e) {
				ModLog.error("Caught an exception while parallel level access.", e);
			} finally {
				cache.endJobExecution();
			}
		});
		cache.awaitJob();
		
		try {
			task.get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
	}
	
	private final Thread serverThread;
	protected final Level level;
	private final Map<BlockPos, T> cache;
	
	private final ConcurrentLinkedQueue<Entry<T>> queue = new ConcurrentLinkedQueue<>();
	private volatile boolean executeJob;
	
	public LevelCache(Level level) {
		this.serverThread = Thread.currentThread();;
		this.level = level;
		this.cache = new ConcurrentHashMap<>();
	}
	
	/**
	 * Can only be executed on specific threads.
	 * @param pos
	 * @return
	 */
	public T getBlockProperty(BlockPos pos) {
		T prop =  this.cache.computeIfAbsent(pos, p -> this.createNewPropertyInstance());
		synchronized (prop) { // initialisation
			while (!((BlockProperty) prop).initialised) {
				var currentThread = Thread.currentThread();
				if (currentThread == this.serverThread) {
					this.initProperty(prop, pos);
					break;
				}
				
				this.queue.add(new Entry<T>(prop, pos, currentThread));
				synchronized (this) { this.notify(); }
				LockSupport.park();
			}
//			if (prop.blockState == null) System.out.println("warn!!!");
		}
//		if (blockProperty.blockState == null || blockProperty.fluidState == null) System.out.println("warn!!"); // DEBUG
		return prop;
	}
	
	private void awaitJob() {
		this.executeJob = true;
		while (this.executeJob) {
			while (!this.queue.isEmpty()) {
				var entry = this.queue.poll();
				var prop = entry.property();
				if (!prop.isInitialised()) {
					this.initProperty(prop, entry.blockPos());
					prop.setInitialised();
				}
				LockSupport.unpark(entry.currentThread());
			}
			
			synchronized (this) {
				if (!this.queue.isEmpty()) continue;
				try {
					this.wait(1);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	private void endJobExecution() {
		this.executeJob = false;
		synchronized (this) {
			this.notify();
		}
	}
	
	protected abstract T createNewPropertyInstance();
	protected abstract void initProperty(T prop, BlockPos pos);
	
	protected Map<BlockPos, T> getCache() {
		return this.cache;
	}
	
	public static class BlockProperty {
		private boolean initialised = false;
		boolean isInitialised() { return this.initialised; }
		void setInitialised() { this.initialised = true; }
	}
	
	private static final record Entry<T>(T property, BlockPos blockPos, Thread currentThread) {}
}
