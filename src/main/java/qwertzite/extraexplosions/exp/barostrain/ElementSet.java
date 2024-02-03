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
	
	/**
	 * Retrieves cached FemElement instance if exists. Else, creates a new instance and return.
	 * @param pos
	 * @return
	 */
	public FemElement getElementAt(BlockPos pos) {
		return map.computeIfAbsent(pos, k -> new FemElement(k));
	}
	
	public double[][] getStrainAt(BlockPos pos, IntPoint intPt) {
		return map.getOrDefault(pos, FemElement.ZERO).getSigmaAt(intPt);
	}
	
	public FemElement getExistingElementAt(BlockPos pos) {
		return map.getOrDefault(pos, null);
	}
	
	public boolean isTobeDestroyed(BlockPos pos) {
		return map.containsKey(pos) ? !map.get(pos).isFixed() : false;
	}
	
	public Map<BlockPos, FemElement> getElements() {
		return this.map;
	}
	
	public void processDestroyed(BlockPos destroyed) {
		map.get(destroyed).clearDestructedElement();
	}
}
