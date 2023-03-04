package qwertzite.extraexplosions.util.collection;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BinaryOperator;

import net.minecraft.world.phys.Vec3;

/**
 * 並列実行可能で，値を加算していくマップ
 * @author owner
 * @date 2023/03/02
 */
public class AccumulatorMap<K> {
	private static final Wrapper ZERO = new Wrapper();
	
	private final Map<K, Wrapper> map = new ConcurrentHashMap<>();
	
	public AccumulatorMap() {}
	
	public void add(K key, double x, double y, double z) {
		Wrapper wrapper = map.computeIfAbsent(key, k -> new Wrapper());
		synchronized (wrapper) {
			wrapper.x += x;
			wrapper.y += y;
			wrapper.z += z;
		}
	}
	
	public Vec3 get(K key) {
		var wrapper = map.getOrDefault(key, ZERO);
		synchronized (wrapper) {
			return new Vec3(wrapper.x, wrapper.y, wrapper.z);
		}
	}
	
	private static class Wrapper {
		double x;
		double y;
		double z;
		public Wrapper() {
			this.x = 0.0d;
			this.y = 0.0d;
			this.z = 0.0d;
		}
	}
}
