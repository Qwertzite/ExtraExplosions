package qwertzite.extraexplosions.exp.barostrain;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public class NodeSet {
	private static final FemNode DUMMY = new FemNode(null);
	private final Map<BlockPos, FemNode> map = new ConcurrentHashMap<>();
	
	public void add(BlockPos key, double x, double y, double z) {
		IForceAccumlator wrapper = this.getNodeAt(key);
		synchronized (wrapper) {
			wrapper.addExternalForce(x, y, z);
		}
	}
	
	public Stream<FemNode> stream() {
		return this.map.values().stream();
	}
	
	public Vec3 getDisplacementAt(BlockPos pos) {
		return map.getOrDefault(pos, DUMMY).getDisp();
	}
	
	public FemNode getNodeAt(BlockPos pos) {
		return map.computeIfAbsent(pos, k -> new FemNode(k));
	}
	
	public FemNode getNodeIfExist(BlockPos pos) {
		return map.getOrDefault(pos, DUMMY);
	}
	
	public void markToBeUpdated(BlockPos node) {
		this.getNodeAt(node).markToBeUpdatedInternalForce();
	}
}
