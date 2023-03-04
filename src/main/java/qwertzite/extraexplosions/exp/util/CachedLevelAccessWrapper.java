package qwertzite.extraexplosions.exp.util;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public class CachedLevelAccessWrapper {
	private final static BlockProperty DESTROYED = new BlockProperty();
	
	
	private final Level level;
	private final Set<BlockPos> destroyed;
	private final Map<BlockPos, BlockProperty> blockProperty;
	
	public CachedLevelAccessWrapper(Level level) {
		this.level = level;
		this.destroyed = ConcurrentHashMap.newKeySet();
		this.blockProperty = new ConcurrentHashMap<>();
	}
	
	public void setDestroyed(BlockPos pos) {
		this.destroyed.add(pos);
		this.blockProperty.put(pos, DESTROYED);
	}
	
	public BlockProperty getBlockPropertyAt(BlockPos pos) {
		var prop =  this.blockProperty.computeIfAbsent(pos, p -> new BlockProperty());
		synchronized (prop) {
			if (!prop.initialised) {
				// TODO:
				prop.initialised = true;
			}
		}
		return prop;
	}
	
	public static class BlockProperty {
		private double resistance;
		private double hardness;
		private boolean initialised;
		
		public double getResistance() { return this.resistance; }
		public double getHardness() { return this.hardness; }
	}
}
