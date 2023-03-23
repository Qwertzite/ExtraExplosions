package qwertzite.extraexplosions.exp.barostrain;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.checkerframework.checker.units.qual.K;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public class ExternalForce {
	private static final Wrapper ZERO = new Wrapper();
	
	private final Map<BlockPos, Wrapper> map = new ConcurrentHashMap<>();
	
	public ExternalForce() {}
	
	public void add(BlockPos key, double x, double y, double z) {
		Wrapper wrapper = map.computeIfAbsent(key, k -> new Wrapper());
		synchronized (wrapper) {
			wrapper.x += x;
			wrapper.y += y;
			wrapper.z += z;
			wrapper.updated = true;
		}
	}
	
	public Vec3 get(K key) {
		var wrapper = map.getOrDefault(key, ZERO);
		synchronized (wrapper) {
			return new Vec3(wrapper.x, wrapper.y, wrapper.z);
		}
	}
	
	public Map<BlockPos, Vec3> getAsMap() {
		synchronized (map) {
			return map.entrySet().parallelStream().collect(Collectors.toMap(e -> e.getKey(), e -> { var val = e.getValue(); return new Vec3(val.x, val.y, val.z); }));
		}
	}
	
	private static class Wrapper {
		boolean updated;
		double x;
		double y;
		double z;
		public Wrapper() {
			this.updated = false;
			this.x = 0.0d;
			this.y = 0.0d;
			this.z = 0.0d;
		}
	}
}
