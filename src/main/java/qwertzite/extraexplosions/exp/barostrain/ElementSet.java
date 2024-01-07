package qwertzite.extraexplosions.exp.barostrain;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import net.minecraft.core.BlockPos;

public class ElementSet {
	private final Map<BlockPos, FemElement> map = new ConcurrentHashMap<>();
	
	public void markAsUpdateTarget(BlockPos elementPos) {
		var element = this.getElementAt(elementPos);
		element.markToBeUpdated();
	}
	
	public Stream<FemElement> stream() {
		return this.map.values().stream();
	}
	
	public FemElement getElementAt(BlockPos pos) {
		return map.computeIfAbsent(pos, k -> new FemElement(k));
	}
	
	public FemElement getExistingElementAt(BlockPos pos) {
		return map.getOrDefault(pos, null);
	}
	
	public Map<BlockPos, FemElement> getElements() {
		return this.map;
	}
}
