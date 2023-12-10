package qwertzite.extraexplosions.exp.barostrain;

import net.minecraft.core.BlockPos;

public interface IForceAccumlator {
	public void addExternalForce(double fx, double fy, double fz);
	
	public static IForceAccumlator getImplInstance(BlockPos pos) {
		return new FemNode(pos);
	}
}
