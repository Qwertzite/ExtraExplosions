package qwertzite.extraexplosions.exp.barostrain;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import qwertzite.extraexplosions.util.collection.AccumulatorMap;

public class FEM {
	
	private AccumulatorMap<BlockPos> blastForce = new AccumulatorMap<BlockPos>();
	
	public void applyPressure(BlockPos pos, Direction face, double pressure) {
		switch (face) {
		case EAST: // +x
			blastForce.add(pos.offset(1, 0, 0), -pressure, 0, 0);
			blastForce.add(pos.offset(1, 1, 0), -pressure, 0, 0);
			blastForce.add(pos.offset(1, 0, 1), -pressure, 0, 0);
			blastForce.add(pos.offset(1, 1, 1), -pressure, 0, 0);
			break;
		case WEST: // -x
			blastForce.add(pos.offset(0, 0, 0), pressure, 0, 0);
			blastForce.add(pos.offset(0, 1, 0), pressure, 0, 0);
			blastForce.add(pos.offset(0, 0, 1), pressure, 0, 0);
			blastForce.add(pos.offset(0, 1, 1), pressure, 0, 0);
			break;
		case UP: // +y
			blastForce.add(pos.offset(0, 1, 0), 0, -pressure, 0);
			blastForce.add(pos.offset(0, 1, 1), 0, -pressure, 0);
			blastForce.add(pos.offset(1, 1, 0), 0, -pressure, 0);
			blastForce.add(pos.offset(1, 1, 1), 0, -pressure, 0);
			break;
		case DOWN: // -y
			blastForce.add(pos.offset(0, 0, 0), 0, pressure, 0);
			blastForce.add(pos.offset(0, 0, 1), 0, pressure, 0);
			blastForce.add(pos.offset(1, 0, 0), 0, pressure, 0);
			blastForce.add(pos.offset(1, 0, 1), 0, pressure, 0);
			break;
		case SOUTH: // +z
			blastForce.add(pos.offset(0, 0, 1), 0, 0, -pressure);
			blastForce.add(pos.offset(1, 0, 1), 0, 0, -pressure);
			blastForce.add(pos.offset(0, 1, 1), 0, 0, -pressure);
			blastForce.add(pos.offset(1, 1, 1), 0, 0, -pressure);
			break;
		case NORTH: // -z
			blastForce.add(pos.offset(0, 0, 0), 0, 0, pressure);
			blastForce.add(pos.offset(1, 0, 0), 0, 0, pressure);
			blastForce.add(pos.offset(0, 1, 0), 0, 0, pressure);
			blastForce.add(pos.offset(1, 1, 0), 0, 0, pressure);
			break;
		default:
			break;
		}
		
		// どの面に当たったかを記録する (BlockPos x Directionからなるキー)
	}
	
	public void compute() {
		
		Map<BlockPos, Vec3> displacement = new HashMap<>();
		while (true) {
			
			
			
		}
	}
	
	private void computeCalcTarget(Map<BlockPos, Vec3> externalForce, Map<BlockPos, Vec3> internalForce) {
		
	}
	
	private void computeDisplacement(Map<BlockPos, Vec3> residualForce) {
		
	}
	
	private void computeInternalForce(Map<BlockPos, Vec3> displacement) {
		// TODO: Compute internal force from vertex displacement
	}
	
	
	
	public Map<BlockPos, Vec3> getForceMap(){
		return blastForce.getAsMap();
	}
}
