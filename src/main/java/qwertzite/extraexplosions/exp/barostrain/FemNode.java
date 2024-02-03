package qwertzite.extraexplosions.exp.barostrain;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;

/**
 * Represents vertex.
 * @author Qwertzite
 * @date 2023/12/23
 */
public class FemNode {
	
	private final BlockPos position;
	private final BlockPos[] neighourVertex;
	
	// external force
	private boolean externalForceUpdated;
	private double externalForceX;
	private double externalForceY;
	private double externalForceZ;
	
	// displacement
	private double prevDispX;
	private double prevDispY;
	private double prevDispZ;
	
	private Vec3 disp = Vec3.ZERO;
	private double dispX;
	private double dispY;
	private double dispZ;
	
	private double step;
	
	// internal force
	private boolean needComputeInternalForce;
	
	private Vec3 internalForce = Vec3.ZERO;
	private double internalForceX;
	private double internalForceY;
	private double internalForceZ;
	
	public FemNode(BlockPos pos) {
		this.position = pos;
		if (pos != null) {
			this.neighourVertex = new BlockPos[] {
					pos.offset(-1, -1, -1),
					pos.offset(-1, -1,  0),
					pos.offset(-1, -1,  1),
					pos.offset(-1,  0, -1),
					pos.offset(-1,  0,  0),
					pos.offset(-1,  0,  1),
					pos.offset(-1,  1, -1),
					pos.offset(-1,  1,  0),
					pos.offset(-1,  1,  1),
					
					pos.offset( 0, -1, -1),
					pos.offset( 0, -1,  0),
					pos.offset( 0, -1,  1),
					pos.offset( 0,  0, -1),
	//				pos.offset( 0,  0,  0),
					pos.offset( 0,  0,  1),
					pos.offset( 0,  1, -1),
					pos.offset( 0,  1,  0),
					pos.offset( 0,  1,  1),
	
					pos.offset( 1, -1, -1),
					pos.offset( 1, -1,  0),
					pos.offset( 1, -1,  1),
					pos.offset( 1,  0, -1),
					pos.offset( 1,  0,  0),
					pos.offset( 1,  0,  1),
					pos.offset( 1,  1, -1),
					pos.offset( 1,  1,  0),
					pos.offset( 1,  1,  1),
			};
		} else {
			this.neighourVertex = new BlockPos[] {};
		}
		
	}
	
	public void addExternalForce(double fx, double fy, double fz) {
		this.externalForceX += fx;
		this.externalForceY += fy;
		this.externalForceZ += fz;
		this.externalForceUpdated = true;
	}
	
	public void setDisplacement(double x, double y, double z, double step) {
		this.prevDispX = this.dispX;
		this.prevDispY = this.dispY;
		this.prevDispZ = this.dispZ;
		
		this.dispX = x;
		this.dispY = y;
		this.dispZ = z;
		this.disp = new Vec3(x, y, z);
		this.step = step;
	}
	
	public void addDisplacement(double x, double y, double z, double step) {
		this.setDisplacement(x + this.dispX, y + this.dispY, z + this.dispZ, step);
	}
	
	public static Vec3i[] getAdjacentElementOffsets() {
		return new Vec3i[] {
				new Vec3i( 0, 0, 0),
				new Vec3i(-1, 0, 0),
				new Vec3i( 0,-1, 0),
				new Vec3i(-1,-1, 0),
				new Vec3i( 0, 0,-1),
				new Vec3i(-1, 0,-1),
				new Vec3i( 0,-1,-1),
				new Vec3i(-1,-1,-1),
		};
	}
	
	public BlockPos[] getAdjacentElements() {
		var offsets = getAdjacentElementOffsets();
		var result = new BlockPos[offsets.length];
		for (int i = 0; i < offsets.length; i++) {
			result[i] = this.position.offset(offsets[i]);
		}
		return result;
	}
	
	
	public BlockPos getPosition() { return this.position; }
	
	public boolean getExternalForceUpdated() {
		var flag = this.externalForceUpdated;
		this.externalForceUpdated = false;
		return flag;
	}
	
	public double getExForceX() { return this.externalForceX; }
	public double getExForceY() { return this.externalForceY; }
	public double getExForceZ() { return this.externalForceZ; }
	/** For debugging use only. */
	public Vec3 getExForce() { return new Vec3(this.externalForceX, this.externalForceY, this.externalForceZ); }
	
	public BlockPos[] getAffectingVertex() {
		return this.neighourVertex;
	}
	
	public double getDispX() { return this.dispX; }
	public double getDispY() { return this.dispY; }
	public double getDispZ() { return this.dispZ; }
	public Vec3 getDisp() { return this.disp; }
	
	public void markToBeUpdatedInternalForce() { this.needComputeInternalForce = true; }
	public boolean needsIntForceUpdate() { return this.needComputeInternalForce; }
	
	public void setInternalForce(double x, double y, double z) {
		this.internalForce = new Vec3(x, y, z);
		this.internalForceX = x;
		this.internalForceY = y;
		this.internalForceZ = z;
		this.needComputeInternalForce = false;
	}
	
	public void addInternalForce(double x, double y, double z) {
		this.internalForce = this.internalForce.add(x, y, z);
		this.internalForceX += x;
		this.internalForceY += y;
		this.internalForceZ += z;
	}
	
	public Vec3 getInternalForce() { return this.internalForce; }
	
	
	public double getForceBalanceNormSquared() {
		var dx = this.externalForceX - this.internalForceX;
		var dy = this.externalForceY - this.internalForceY;
		var dz = this.externalForceZ - this.internalForceZ;
		return dx*dx + dy*dy + dz*dz;
	}
	
	public double getForceBalanceX() { return this.externalForceX - this.internalForceX; }
	public double getForceBalanceY() { return this.externalForceY - this.internalForceY; }
	public double getForceBalanceZ() { return this.externalForceZ - this.internalForceZ; }
	
	public void prepareForNextRayStep() {
		this.prevDispX = this.dispX;
		this.prevDispY = this.dispY;
		this.prevDispZ = this.dispZ;
	}
}
