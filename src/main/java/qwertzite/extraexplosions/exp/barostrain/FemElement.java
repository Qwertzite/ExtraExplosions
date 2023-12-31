package qwertzite.extraexplosions.exp.barostrain;

import java.util.EnumSet;

import net.minecraft.core.BlockPos;

public class FemElement {

	private final BlockPos position;
	
	private double[][] displacement = new double[IntPoint.values().length][3];
	private double[][][] sigma = new double[IntPoint.values().length][3][3];
	private EnumSet<IntPoint> elasticDeformation = EnumSet.noneOf(IntPoint.class);
	
	private boolean needUpdate;
	
	private BlockCluster cluster;
	
	public FemElement(BlockPos position) {
		this.position = position;
	}
	
	public BlockPos getPosition() { return this.position; }
	
	public void markToBeUpdated() {
		this.needUpdate = true;
	}
	
	public boolean needsUpdate() { return this.needUpdate; }
	
	public void setElasticDeformed(IntPoint intPoint) {
		this.elasticDeformation.add(intPoint);
	}
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
	
	public boolean isElasticallyDeforming() {
		return !this.elasticDeformation.isEmpty();
	}
	
	// ======== After deformation is determined ========
	
	public boolean belongToCluster() {
		return this.cluster != null;
	}
	
	/**
	 * 
	 * @param cluster
	 * @return false if already set.
	 */
	public synchronized boolean setCluster(BlockCluster cluster) {
		if (this.belongToCluster()) return false;
		this.cluster = cluster;
		return true;
	}
}
