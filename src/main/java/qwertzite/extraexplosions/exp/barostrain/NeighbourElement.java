package qwertzite.extraexplosions.exp.barostrain;

import net.minecraft.core.Vec3i;

public enum NeighbourElement {
	NNN(-1, -1, -1, ElemVertex.PPP),
	NNP(-1, -1,  0, ElemVertex.PPN),
	NPN(-1,  0, -1, ElemVertex.PNP),
	NPP(-1,  0,  0, ElemVertex.PNN),
	PNN( 0, -1, -1, ElemVertex.NPP),
	PNP( 0, -1,  0, ElemVertex.NPN),
	PPN( 0,  0, -1, ElemVertex.NNP),
	PPP( 0,  0,  0, ElemVertex.NNN);
	
	private final int offsetX;
	private final int offsetY;
	private final int offsetZ;
	private final Vec3i offset;
	private final ElemVertex nodeVertex;
	
	private NeighbourElement(int offsetX, int offsetY, int offsetZ, ElemVertex elemVertex) {
		this.offsetX = offsetX;
		this.offsetY = offsetY;
		this.offsetZ = offsetZ;
		this.offset = new Vec3i(offsetX, offsetY, offsetZ);
		this.nodeVertex = elemVertex;
	}
	
	public Vec3i getOffset() { return this.offset; }
	public ElemVertex getNodeVertex() { return this.nodeVertex; }
}
