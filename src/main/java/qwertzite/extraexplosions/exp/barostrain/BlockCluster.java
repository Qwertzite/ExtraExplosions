package qwertzite.extraexplosions.exp.barostrain;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

/**
 * Represents a group of blocks which is connected and not separated by elastically deforming blocks.
 * 
 * @author Qwertzite
 * @date 2023/12/29
 */
public class BlockCluster {
	
	private boolean isFixed;
	
	public boolean transmitAll;
	public double pressureXNeg;
	public double pressureXPos;
	public double pressureYNeg;
	public double pressureYPos;
	public double pressureZNeg;
	public double pressureZPos;
	
	public double inertiaX;
	public double inertiaY;
	public double inertiaZ;
	
	public BlockCluster() {}
	
	public void setFixed(boolean fixed) { this.isFixed = fixed; }
	public boolean isFixed() { return this.isFixed; }
	
	/**
	 * 
	 * @param dir the direction of the force applied by pressure.
	 * @return
	 */
	public double transmittance(Direction dir) {
		if (this.isFixed) return 0.0d;
		return this.transmitAll ? 1.0d : switch (dir) {
		case EAST:  yield pressureXPos == 0.0d ? 0.0d : Mth.clamp(inertiaX / pressureXPos, 0.0d, 1.0d); // +x
		case WEST:  yield pressureXNeg == 0.0d ? 0.0d : Mth.clamp(inertiaX / pressureXNeg, 0.0d, 1.0d); // -x
		case UP:    yield pressureYPos == 0.0d ? 0.0d : Mth.clamp(inertiaY / pressureYPos, 0.0d, 1.0d); // +y
		case DOWN:  yield pressureYNeg == 0.0d ? 0.0d : Mth.clamp(inertiaY / pressureYNeg, 0.0d, 1.0d); // -y
		case SOUTH: yield pressureZPos == 0.0d ? 0.0d : Mth.clamp(inertiaZ / pressureZPos, 0.0d, 1.0d); // +z
		case NORTH: yield pressureZNeg == 0.0d ? 0.0d : Mth.clamp(inertiaZ / pressureZNeg, 0.0d, 1.0d); // -z
		default: assert(false); yield 0.0d;
		};
	}
}
