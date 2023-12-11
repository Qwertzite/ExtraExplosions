package qwertzite.extraexplosions.exp.barostrain;

import net.minecraft.core.BlockPos;

public class FemElement {

	private final BlockPos position;
	
	private double[][] displacement = new double[IntPoint.values().length][3];
	private double[][][] sigma = new double[IntPoint.values().length][3][3];
	
	private boolean needUpdate;
	
	public FemElement(BlockPos position) {
		this.position = position;
	}
	
	public BlockPos getPosition() { return this.position; }
	
	public void markToBeUpdated() {
		this.needUpdate = true;
	}
	
	public boolean needsUpdate() { return this.needUpdate; }
	
	public void setNewStatus(double[][] displacement, double[][][] sigma) {
		this.needUpdate = false;
		this.displacement = displacement;
		this.sigma = sigma;
	}
	
	public double[] getDisplacementAt(IntPoint intPt) {
		return this.displacement[intPt.ordinal()];
	}
	
	public double[][] getSigmaAt(IntPoint intPt) {
		return this.sigma[intPt.ordinal()];
	}
}