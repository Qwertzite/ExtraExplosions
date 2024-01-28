package qwertzite.extraexplosions.exp.barostrain;

import java.util.EnumSet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;

public class FemElement {
	public static final FemElement ZERO = new FemElement(BlockPos.ZERO) {
		@Override public void markToBeUpdated() { throw new UnsupportedOperationException("This object is immutable."); }
		@Override public void setElasticDeformed(IntPoint intPoint) {  throw new UnsupportedOperationException("This object is immutable."); }
		@Override public void setNewStatus(double[][] displacement, double[][][] sigma) { throw new UnsupportedOperationException("This object is immutable."); }
		@Override public synchronized boolean setCluster(BlockCluster cluster) { throw new UnsupportedOperationException("This object is immutable."); }
	};

	private final BlockPos position;
	
	public double pressForceXPos;
	public double pressForceXNeg;
	public double pressForceYPos;
	public double pressForceYNeg;
	public double pressForceZPos;
	public double pressForceZNeg;
	
	private double[][] displacement = new double[IntPoint.values().length][3];
	private double[][][] sigma = new double[IntPoint.values().length][3][3];
	private EnumSet<IntPoint> elasticDeformation = EnumSet.noneOf(IntPoint.class);
	
	private boolean needUpdate;
	
	private BlockCluster cluster;
	
	public FemElement(BlockPos position) {
		this.position = position;
	}
	
	public BlockPos getPosition() { return this.position; }
	
	public void addPressureForce(Axis axis, double force) {
		switch (axis) {
		case X -> { if (force > 0) { this.pressForceXPos += force; } else { this.pressForceXNeg += force; } }
		case Y -> { if (force > 0) { this.pressForceYPos += force; } else { this.pressForceYNeg += force; } }
		case Z -> { if (force > 0) { this.pressForceZPos += force; } else { this.pressForceZNeg += force; } }
		default -> { assert(false); }
		}
	}
	
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
	
	public void clearElementStatus() {
		this.cluster = null;
		this.pressForceXNeg = 0.0d;
		this.pressForceXPos = 0.0d;
		this.pressForceYNeg = 0.0d;
		this.pressForceYPos = 0.0d;
		this.pressForceZNeg = 0.0d;
		this.pressForceZPos = 0.0d;
	}
}
