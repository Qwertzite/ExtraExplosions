package qwertzite.extraexplosions.exp.barostrain;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/**
 * 
 * @param hitBlock
 * @param hitFace 
 * @param hitPos the position where the ray hit
 * @param hitDistance distance from the beginning of the PressureRay (PressureRay.from).
 * @param internal whether the ray hit from inside the hit block or not.
 * 
 * @author owner
 * @date 2024/02/01
 */
public record PressureTraceResult(BlockPos hitBlock, Direction hitFace, Vec3 hitPos, double hitDistance, boolean internal) {
	
	public Direction pressureDirection() {
		return internal ? hitFace : hitFace.getOpposite();
	}
}
