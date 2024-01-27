package qwertzite.extraexplosions.exp.barostrain;

import java.util.EnumSet;

import net.minecraft.core.BlockPos;

public class FemElement {
	public static final FemElement ZERO = new FemElement(BlockPos.ZERO) {
		@Override public void markToBeUpdated() { throw new UnsupportedOperationException("This object is immutable."); }
		@Override public void setElasticDeformed(IntPoint intPoint) {  throw new UnsupportedOperationException("This object is immutable."); }
		@Override public void setNewStatus(double[][] displacement, double[][][] sigma) { throw new UnsupportedOperationException("This object is immutable."); }
		@Override public synchronized boolean setCluster(BlockCluster cluster) { throw new UnsupportedOperationException("This object is immutable."); }
	};

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
	
	public boolean isFixed() {
		return this.cluster.isFixed();
	}
	
	// ======== For next FEM computation ========
	
	public void clearDestructedElement() {
		this.displacement = new double[3][3];
		this.sigma = new double[3][3][3];
		this.elasticDeformation.clear();
		this.cluster = null;
	}
	
	public void clearClusterStatus() {
		this.cluster = null;
	}
}
