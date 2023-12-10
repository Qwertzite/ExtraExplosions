package qwertzite.extraexplosions.exp.barostrain;

public enum IntPoint {
	NNN(-1, -1, -1),
	NNP(-1, -1,  1),
	NPN(-1,  1, -1),
	NPP(-1,  1,  1),
	PNN( 1, -1, -1),
	PNP( 1, -1,  1),
	PPN( 1,  1, -1),
	PPP( 1,  1,  1);
	
	private final int[] sign;
	private final double xi0, xi1, xi2;
	
	private IntPoint(int xi0, int xi1, int xi2) {
		this.sign = new int[] { xi0, xi1, xi2 };
		this.xi0 = xi0 / Math.sqrt(3);
		this.xi1 = xi1 / Math.sqrt(3);
		this.xi2 = xi2 / Math.sqrt(3);
	}
	
	public double getXi_i(int i) {
		switch (i) {
		case 0 : return xi0;
		case 1 : return xi1;
		case 2 : return xi2;
		default: throw new IllegalArgumentException("Argument must be betweeen 0 to 2 both inclusive.");
		}
	}
	
	public int getSign_i(int i) {
		return this.sign[i];
	}
	
}
