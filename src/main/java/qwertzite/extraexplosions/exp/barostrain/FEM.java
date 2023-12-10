package qwertzite.extraexplosions.exp.barostrain;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import qwertzite.extraexplosions.util.math.EeMath;

public class FEM {
	
	private CachedLevelAccessWrapper level;
//	private BarostrainLevelCache level;
	
	private NodeSet nodeSet = new NodeSet();
	private ElementSet elementSet = new ElementSet();
	
//	public FEM(BarostrainLevelCache level) {
	public FEM(CachedLevelAccessWrapper level) {
		this.level = level;
	}
	
	public void applyPressure(BlockPos pos, Direction face, double pressure) {
		switch (face) {
		case EAST: // +x
			nodeSet.add(pos.offset(1, 0, 0), -pressure, 0, 0);
			nodeSet.add(pos.offset(1, 1, 0), -pressure, 0, 0);
			nodeSet.add(pos.offset(1, 0, 1), -pressure, 0, 0);
			nodeSet.add(pos.offset(1, 1, 1), -pressure, 0, 0);
			break;
		case WEST: // -x
			nodeSet.add(pos.offset(0, 0, 0), pressure, 0, 0);
			nodeSet.add(pos.offset(0, 1, 0), pressure, 0, 0);
			nodeSet.add(pos.offset(0, 0, 1), pressure, 0, 0);
			nodeSet.add(pos.offset(0, 1, 1), pressure, 0, 0);
			break;
		case UP: // +y
			nodeSet.add(pos.offset(0, 1, 0), 0, -pressure, 0);
			nodeSet.add(pos.offset(0, 1, 1), 0, -pressure, 0);
			nodeSet.add(pos.offset(1, 1, 0), 0, -pressure, 0);
			nodeSet.add(pos.offset(1, 1, 1), 0, -pressure, 0);
			break;
		case DOWN: // -y
			nodeSet.add(pos.offset(0, 0, 0), 0, pressure, 0);
			nodeSet.add(pos.offset(0, 0, 1), 0, pressure, 0);
			nodeSet.add(pos.offset(1, 0, 0), 0, pressure, 0);
			nodeSet.add(pos.offset(1, 0, 1), 0, pressure, 0);
			break;
		case SOUTH: // +z
			nodeSet.add(pos.offset(0, 0, 1), 0, 0, -pressure);
			nodeSet.add(pos.offset(1, 0, 1), 0, 0, -pressure);
			nodeSet.add(pos.offset(0, 1, 1), 0, 0, -pressure);
			nodeSet.add(pos.offset(1, 1, 1), 0, 0, -pressure);
			break;
		case NORTH: // -z
			nodeSet.add(pos.offset(0, 0, 0), 0, 0, pressure);
			nodeSet.add(pos.offset(1, 0, 0), 0, 0, pressure);
			nodeSet.add(pos.offset(0, 1, 0), 0, 0, pressure);
			nodeSet.add(pos.offset(1, 1, 0), 0, 0, pressure);
			break;
		default:
			break;
		}
		
		// どの面に当たったかを記録する (BlockPos x Directionからなるキー)
	}
	
	public void compute() {
		System.out.println("BEGIN FEM!"); // DEBUG
		
		long count = this.nodeSet.stream() // OPTIMISE: parallel
				.filter(e -> {
					if (!this.filterExternalForceUpdated(e) || !this.filterForceImbalance(e)) return false;
					this.markAdjacentElements(e);
					this.computeInitialDisplacement(e);
					return true;
				}).count(); // number of nodes to be updated.
		System.out.println("count=" + count);
		
		Set<BlockPos> tmpDestroyed = ConcurrentHashMap.newKeySet();
		int iter = 0;
		while (count > 0 && iter < 1024)
		{
			this.elementSet.stream().filter(e -> e.needsUpdate()).forEach(e -> { // OPTIMISE: parallel
				this.computeElement(e);
				tmpDestroyed.add(e.getPosition());
			});
			count = this.nodeSet.stream()
					.filter(e -> {
						this.computeInternalForce(e); // 前のループでの
						
						if (!this.filterForceImbalance(e)) return false;
						this.computeNextDisplacement(e);
						this.markAdjacentElements(e); // ここまで確認
						return true;
					}).count();
			System.out.println("iter=" + iter++ + ", count2=" + count);
			
		}
		tmpDestroyed.forEach(e -> this.level.setDestroyed(e)); // DEBUG
//		this.nodeSet.stream().forEach(e -> System.out.println(e.getPosition() + ": " + e.getForceBalanceX() + " " + e.getForceBalanceY() + " " + e.getForceBalanceZ())); // DEBUG
		
		/*
		 * COMEBACK 実装を進める
		 *    実装２：マークしておいて，全部マークし終えたら計算する
		 * nodeSet.stream()
		 * .filter	balance
		 * 			update displacement/vertex
		 * 			mark target elements
		 * .reduce	count
		 * 
		 * **** loop ****
		 * 
		 * femElements.stream()
		 * .filter	updated
		 * .forEach	computeElements		// ray iteration の間も保持する (変位が保持されている間，これも保持する)
		 * 			mark updated nodes
		 * 
		 * femNodes.stream()
		 * .filter	update balance
		 * .forEach	compute internal force
		 * .filter	balance
		 * 			update displacement/vertex
		 * 			mark target elements
		 * .reduce count
		 * 
		 * 
		 * NOTE: 破壊判定時は，=を破壊側に含める (h=0にfが掛かっている場合を含めるため)
		 * 
		 */
		
//		this.nodeSet.stream().forEach(e -> this.level.setDestroyed(e.getPosition())); // DEBUG
		
		System.out.println("FEM COMPLETED!"); // DEBUG
	}
	
	private boolean filterExternalForceUpdated(FemNode node) {
		boolean flag = node.getExternalForceUpdated();
		return flag;
//		return node.getExternalForceUpdated();
	}
	
	private boolean filterForceImbalance(FemNode node) {
		double norm = node.getForceBalanceNormSquared();
		boolean allZero = true;
		for (BlockPos adj : node.getAdjacentElements()) {
//			var prop = this.level.getBlockProperty(adj);
			var prop = this.level.getBlockPropertyAt(adj);
			var limit = prop.getHardness() / 16;
			if (norm > limit*limit && norm > 0.05*0.05 && limit*limit > 0) return true;
		}
		return false;
	}
	
	private void markAdjacentElements(FemNode node) {
		var pos = node.getPosition();
		for (var adj : FemNode.getAdjacentElementOffsets()) {
			this.elementSet.markAsUpdateTarget(pos.offset(adj));
		}
	}
	
	/**
	 * Compute displacement distribution and strain.
	 * @param elem
	 */
	private void computeElement(FemElement elem) {
		var elementPosition = elem.getPosition();
//		var elasticProperty = this.level.getBlockProperty(elementPosition);
		var elasticProperty = this.level.getBlockPropertyAt(elementPosition);
		var mu = elasticProperty.getMuForElement();
		var lambda = elasticProperty.getLambdaForElement();
		
		double[][] finDisp = new double[IntPoint.values().length][3];
		double[][][] finSigma = new double[IntPoint.values().length][3][3];
		
		for (var intPt : IntPoint.values()) {
			
			var disp = new double[3];
			var epsilon = new double[3][3];
			for (var vertexOffset : ElemVertex.values()) {
				var vertex = elementPosition.offset(vertexOffset.getOffset());
				var vertexDisp = nodeSet.getDisplacementAt(vertex);
				for (int j = 0; j < 3; j++) {
					for (int i = 0; i < 3; i++) {
						epsilon[j][i] += (
								EeMath.getXi(vertexDisp, i) * ShapeFunc.partial(vertexOffset, j, intPt) +
								EeMath.getXi(vertexDisp, j) * ShapeFunc.partial(vertexOffset, i, intPt)) * 0.5;
					}
					disp[j] += EeMath.getXi(vertexDisp, j) * ShapeFunc.value(vertexOffset, intPt);
				}
			}
			
			var epsilon_m = epsilon[0][0] + epsilon[1][1] + epsilon[2][2];
			epsilon_m /= 3.0d;
			
			var sigma = new double[3][3];
			for (int i = 0; i < 3; i++) {
				for (int j = 0; j < 3; j++) { sigma[j][i] = 2 * mu * epsilon[j][i]; }
				sigma[i][i] += lambda * epsilon_m;
			}
			
			finDisp[intPt.ordinal()] = disp;
			finSigma[intPt.ordinal()] = sigma;
		}
		elem.setNewStatus(finDisp, finSigma);
		
		// mark nodes to be recalculated.
		for (var ev : ElemVertex.values()) {
			var nodePos = elementPosition.offset(ev.getOffset());
			this.nodeSet.markToBeUpdated(nodePos);
		}
	}
	
	private void computeInternalForce(FemNode node) {
		BlockPos nodePosition = node.getPosition();
		
		var intForce = new double[3];
		for (var elementOffset : NeighbourElement.values()) {
			
			var elementPosition = nodePosition.offset(elementOffset.getOffset());
			var element = this.elementSet.getElementAt(elementPosition);
			var elasticProperty = this.level.getBlockPropertyAt(elementPosition);
//			var elasticProperty = this.level.getBlockProperty(elementPosition);
			var mass = elasticProperty.getMass();
			
			for (var intPt : IntPoint.values()) {
				
				double[] disp = element.getDisplacementAt(intPt);
				double[][] sigma = element.getSigmaAt(intPt);
				
				// elastic component
				for (int j = 0; j < 3; j++) {
					var partial = ShapeFunc.partial(elementOffset.getNodeVertex(), j, intPt);
					intForce[0] += 0.5 * sigma[j][0] * partial;
					intForce[1] += 0.5 * sigma[j][1] * partial;
					intForce[2] += 0.5 * sigma[j][2] * partial;
				}
				
				// inertial component
				var shapeFunc = ShapeFunc.value(elementOffset.getNodeVertex(), intPt);
				intForce[0] += mass * disp[0] * shapeFunc;
				intForce[1] += mass * disp[1] * shapeFunc;
				intForce[2] += mass * disp[2] * shapeFunc;
			}
		}
		node.setInternalForce(intForce[0], intForce[1], intForce[2]);
	}
	
	private FemNode computeInitialDisplacement(FemNode node) {
		BlockPos pos0 = node.getPosition();
		
		var elemOffset = FemNode.getAdjacentElementOffsets();
		var jacobian = new double[3][3];
		
		for (var offset : elemOffset) {
			var elem = pos0.offset(offset);
			var property = this.level.getBlockPropertyAt(elem);
			double mu = property.getMuForElement();
			double muLambda = property.getLambdaForElement() / 3.0d + mu;
			double mass = property.getMass();
			
			for (int i = 0; i < 3; i++) {
				for (int j = 0; j < 3; j++) { jacobian[i][j] += muLambda*ShapeFunc.nbij(offset, i, j); }
				jacobian[i][i] += mu * ShapeFunc.NA + muLambda * (ShapeFunc.NBii - ShapeFunc.nbij(offset, i, i)) + mass * ShapeFunc.NC;
			}
		}
		
		var invJacobian = EeMath.inverseMatrix(jacobian);
		
		double dx = invJacobian[0][0] * node.getExForceX() + invJacobian[0][1] * node.getExForceY() + invJacobian[0][2] * node.getExForceZ();
		double dy = invJacobian[1][0] * node.getExForceX() + invJacobian[1][1] * node.getExForceY() + invJacobian[1][2] * node.getExForceZ();
		double dz = invJacobian[2][0] * node.getExForceX() + invJacobian[2][1] * node.getExForceY() + invJacobian[2][2] * node.getExForceZ();
		node.setDisplacement(dx, dy, dz, 1.0d);
		return node;
	}
	
	private FemNode computeNextDisplacement(FemNode node) { // IMPL: ******** 変位の状態によって影響は受けないのか確認する ********
		BlockPos pos0 = node.getPosition();
		
		var elemOffset = FemNode.getAdjacentElementOffsets();
		var jacobian = new double[3][3];
		
		for (var offset : elemOffset) {
			var elem = pos0.offset(offset);
			var property = this.level.getBlockPropertyAt(elem);
			double mu = property.getMuForElement();
			double muLambda = property.getLambdaForElement() / 3.0d + mu;
			double mass = property.getMass();
			
			for (int i = 0; i < 3; i++) {
				for (int j = 0; j < 3; j++) { jacobian[i][j] += muLambda*ShapeFunc.nbij(offset, i, j); }
				jacobian[i][i] += mu * ShapeFunc.NA + muLambda * (ShapeFunc.NBii - ShapeFunc.nbij(offset, i, i)) + mass * ShapeFunc.NC;
			}
		}
		
		var invJacobian = EeMath.inverseMatrix(jacobian);
		
		double dx = invJacobian[0][0] * node.getForceBalanceX() + invJacobian[0][1] * node.getForceBalanceY() + invJacobian[0][2] * node.getForceBalanceZ();
		double dy = invJacobian[1][0] * node.getForceBalanceX() + invJacobian[1][1] * node.getForceBalanceY() + invJacobian[1][2] * node.getForceBalanceZ();
		double dz = invJacobian[2][0] * node.getForceBalanceX() + invJacobian[2][1] * node.getForceBalanceY() + invJacobian[2][2] * node.getForceBalanceZ();
		node.addDisplacement(dx, dy, dz, 1.0d);
		return node;
	}
	
	
	public NodeSet getNodeSet() { return this.nodeSet; }
	public ElementSet getElementSet() { return this.elementSet; }
	
}
