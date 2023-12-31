package qwertzite.extraexplosions.exp.barostrain;

/**
 * Represents a group of blocks which is connected and not separated by elastically deforming blocks.
 * 
 * @author Qwertzite
 * @date 2023/12/29
 */
public class BlockCluster {
	
	private boolean isFixed;
	
	public BlockCluster() {}
	
	public void setFixed(boolean fixed) { this.isFixed = fixed; }
	public boolean isFixed() { return this.isFixed; }
	
}
