package qwertzite.extraexplosions.exp.barostrain;

import net.minecraft.core.Vec3i;

public enum ElemVertex {
	NNN(0, 0, 0),
	NNP(0, 0, 1),
	NPN(0, 1, 0),
	NPP(0, 1, 1),
	PNN(1, 0, 0),
	PNP(1, 0, 1),
	PPN(1, 1, 0),
	PPP(1, 1, 1);
	
	private final int offsetX;
	private final int offsetY;
	private final int offsetZ;
	private final Vec3i offset;
	private final int signX; // sign of natural coordinate
	private final int signY;
	private final int signZ;
	
	private ElemVertex(int offsetX, int offsetY, int offsetZ) {
		this.offsetX = offsetX;
		this.offsetY = offsetY;
		this.offsetZ = offsetZ;
		this.offset = new Vec3i(offsetX, offsetY, offsetZ);
		this.signX = offsetX > 0 ? 1 : -1;
		this.signY = offsetY > 0 ? 1 : -1;
		this.signZ = offsetZ > 0 ? 1 : -1;
	}
	
	public Vec3i getOffset() {
		return this.offset;
	}
	
	/**
	 * returns sign of natural coordinate of x_i axis.
	 * @param i
	 * @return 1 if positive, -1 if negative
	 */
	public int getSign(int i) {
		return switch(i) {
		case 0 -> this.signX;
		case 1 -> this.signY;
		case 2 -> this.signZ;
		default -> throw new IllegalArgumentException("Unexpected value: " + i);
		};
	}
	
	public int getOffsetI(int i) {
		return switch(i) {
		case 0 -> this.offsetX;
		case 1 -> this.offsetY;
		case 2 -> this.offsetZ;
		default -> throw new IllegalArgumentException("Unexpected value: " + i);
		};
	}
}
