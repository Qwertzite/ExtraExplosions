package qwertzite.extraexplosions.exp.barostrain;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Stream;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import qwertzite.extraexplosions.util.math.EeMath;

public class FEM {
	
//	private CachedLevelAccessWrapper level;
	private BarostrainLevelCache level;
	
	private NodeSet nodeSet = new NodeSet();
	private ElementSet elementSet = new ElementSet();
	
	public FEM(BarostrainLevelCache level) {
//	public FEM(CachedLevelAccessWrapper level) {
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
					if (!this.filterExternalForceUpdated(e) || !this.isForceImbalanceAt(e)) return false;
					this.markAdjacentElements(e);
					this.computeInitialDisplacement(e);
					return true;
				}).count(); // number of nodes to be updated.
		System.out.println("count=" + count);
		
		Set<BlockPos> tmpDestroyed = ConcurrentHashMap.newKeySet();
		int iter = 0;
		while (count > 0 && iter < 10000) {
			this.elementSet.stream().filter(e -> e.needsUpdate()).forEach(e -> { // OPTIMISE: parallel
				this.computeElement(e);
				tmpDestroyed.add(e.getPosition());
			});
			count = this.nodeSet.stream()
					.filter(e -> {
						this.computeInternalForce(e); // 前のループでの
						
						if (!this.isForceImbalanceAt(e)) return false;
						this.computeNextDisplacement(e);
						this.markAdjacentElements(e); // ここまで確認
						return true;
					}).count();
			System.out.println("iter=" + iter++ + ", count2=" + count);
			
		}
		
		System.out.println("FEM COMPLETED!"); // DEBUG
		long time = System.nanoTime();
		this.computeBlockGroup();
		this.elementSet.stream().parallel().filter(e -> !e.isFixed()).forEach(e -> this.level.setDestroyed(e.getPosition()));
		time = System.nanoTime() - time;
		System.out.println("Took " + time + " ns for clustering.");
		
		System.out.println("DEFORMATION!"); // DEBUG
	}
	
	private boolean filterExternalForceUpdated(FemNode node) {
		boolean flag = node.getExternalForceUpdated();
		return flag;
	}
	
	private boolean isForceImbalanceAt(FemNode node) {
		double norm = node.getForceBalanceNormSquared();
		for (BlockPos adj : node.getAdjacentElements()) {
			var prop = this.level.getBlockProperty(adj);
			var limit = prop.getHardness() / 16;
			if (norm > limit*limit && norm > 0.05*0.05 && limit > 0) return true;
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
		var elasticProperty = this.level.getBlockProperty(elementPosition);
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
								EeMath.getXi(vertexDisp, j) * ShapeFunc.partial(vertexOffset, i, intPt));
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
			
			{ // non-linear strain
				double sigma_m = (sigma[0][0] + sigma[1][1] + sigma[2][2]) / 3.0d;
				double mieses = 0.0d;
				mieses += sigma[0][1] *sigma[0][1] + sigma[0][2] *sigma[0][2];
				mieses += sigma[1][0] *sigma[1][0] + sigma[1][2] *sigma[1][2];
				mieses += sigma[2][0] *sigma[2][0] + sigma[2][1] *sigma[2][1];
				mieses *= 3.0d;
				mieses += (sigma[0][0] - sigma[1][1]) * (sigma[0][0] - sigma[1][1]);
				mieses += (sigma[1][1] - sigma[2][2]) * (sigma[1][1] - sigma[2][2]);
				mieses += (sigma[2][2] - sigma[0][0]) * (sigma[2][2] - sigma[0][0]);
				mieses *= 0.5d;
				double hardness = elasticProperty.getHardness();
				double resistance = elasticProperty.getResistance();
				double g = sigma_m < 0 ? hardness / resistance : 1.0d;
				mieses += g * sigma_m * sigma_m * 0.1;
				
				// elastic deformation
				if (mieses >= hardness || !Double.isFinite(g)) { // beyond Mieses
					double deform = (Double.isFinite(g) && mieses > 0.0001d) ? hardness / mieses : 0.0d; // non finite "g" means that compressive strength is zero.
					for (int i = 0; i < 3; i++) {
						sigma[i][0] *= deform;
						sigma[i][1] *= deform;
						sigma[i][2] *= deform;
					}
					if (Double.isNaN(deform)) System.out.println("NAN " + mieses + " " + hardness);
					elem.setElasticDeformed(intPt);
				}
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
		var nodeDisplacement = node.getDisp();
		
		var intForce = new double[3];
		for (var elementOffset : NeighbourElement.values()) {
			
			var elementPosition = nodePosition.offset(elementOffset.getOffset());
			var element = this.elementSet.getElementAt(elementPosition);
			var elasticProperty = this.level.getBlockProperty(elementPosition);
			var mass = elasticProperty.getMass();
			
			for (var intPt : IntPoint.values()) {
				
				double[][] sigma = element.getSigmaAt(intPt);
				
				double[] partial = new double[3]; // partial_j
				partial[0] = ShapeFunc.partial(elementOffset.getNodeVertex(), 0, intPt);
				partial[1] = ShapeFunc.partial(elementOffset.getNodeVertex(), 1, intPt);
				partial[2] = ShapeFunc.partial(elementOffset.getNodeVertex(), 2, intPt);
				
				// elastic component
				for (int j = 0; j < 3; j++) {
					var del = partial[j];
					intForce[0] += sigma[j][0] * del;
					intForce[1] += sigma[j][1] * del;
					intForce[2] += sigma[j][2] * del;
				}
				intForce[0] += sigma[0][0] * partial[0];
				intForce[1] += sigma[1][1] * partial[1];
				intForce[2] += sigma[2][2] * partial[2];
				
			}
			// inertial component
			intForce[0] += mass * nodeDisplacement.x();
			intForce[1] += mass * nodeDisplacement.y();
			intForce[2] += mass * nodeDisplacement.z();
		}
		node.setInternalForce(intForce[0], intForce[1], intForce[2]);
	}
	
	private FemNode computeInitialDisplacement(FemNode node) {
		BlockPos pos0 = node.getPosition();
		
		var elemOffset = FemNode.getAdjacentElementOffsets();
		var jacobian = new double[3][3];
		
		for (var offset : elemOffset) {
			var elem = pos0.offset(offset);
			var property = this.level.getBlockProperty(elem);
			double mu = property.getMuForElement();
			double muLambda = 4*property.getLambdaForElement() / 3.0d + 2*mu;
			double mass = property.getMass();
			
			for (int i = 0; i < 3; i++) {
				for (int j = 0; j < 3; j++) { jacobian[i][j] += muLambda*ShapeFunc.nbij(offset, i, j); }
				jacobian[i][i] += mu * ShapeFunc.NA + muLambda * (ShapeFunc.NBii - ShapeFunc.nbij(offset, i, i)) + mass;
			}
		}
		
		var invJacobian = EeMath.inverseMatrix(jacobian);
		
		double dx = invJacobian[0][0] * node.getExForceX() + invJacobian[0][1] * node.getExForceY() + invJacobian[0][2] * node.getExForceZ();
		double dy = invJacobian[1][0] * node.getExForceX() + invJacobian[1][1] * node.getExForceY() + invJacobian[1][2] * node.getExForceZ();
		double dz = invJacobian[2][0] * node.getExForceX() + invJacobian[2][1] * node.getExForceY() + invJacobian[2][2] * node.getExForceZ();
		node.setDisplacement(dx, dy, dz, 1.0d);
		return node;
	}
	
	private FemNode computeNextDisplacement(FemNode node) {
		BlockPos pos0 = node.getPosition();
		
		var elemOffset = FemNode.getAdjacentElementOffsets();
		var jacobian = new double[3][3];
		
		for (var offset : elemOffset) {
			var elem = pos0.offset(offset);
			var property = this.level.getBlockProperty(elem);
			double mu = property.getMuForElement();
			double muLambda = property.getLambdaForElement() / 3.0d + mu;
			double mass = property.getMass();
			
			for (int i = 0; i < 3; i++) {
				for (int j = 0; j < 3; j++) { jacobian[i][j] += muLambda*ShapeFunc.nbij(offset, i, j); }
				jacobian[i][i] += mu * ShapeFunc.NA + muLambda * (ShapeFunc.NBii - ShapeFunc.nbij(offset, i, i)) + mass;
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
	
	// ======== destruction ========
	
	private void computeBlockGroup() {
		this.elementSet.getElements().values().stream() // DO NOT MAKE THIS STREAM PARALLEL
		.forEach(elem -> {
			if (elem.belongToCluster()) return;
			BlockCluster cluster = new BlockCluster();
			if(!elem.setCluster(cluster)) return; // already added to a group
			
			GroupResult summary;
			if (!elem.isElasticallyDeforming()) {
				ConcurrentLinkedQueue<FemElement> queue = new ConcurrentLinkedQueue<>();
				queue.add(elem); // the first element
				summary = GroupResult.empty();
				while (!queue.isEmpty()) {
					queue.spliterator();
					summary = Stream.generate(() -> queue.poll()).parallel().takeWhile(Objects::nonNull) // poll elements till the queue is empty
							.map(e -> this.searchBlockGrouping(e, cluster, queue))
							.reduce(summary, (s1, s2) -> s1.add(s2));
				}
			} else { // this block only
				summary = this.analyseElementStatus(elem);
				summary.fixed = false; // force destruction
			}
			
			cluster.setFixed(summary.fixed);
			// TODO: set result to block cluster
		});
	}
	
	private GroupResult searchBlockGrouping(FemElement elem, BlockCluster cluster, ConcurrentLinkedQueue<FemElement> queue) {
		var pos = elem.getPosition();
		Direction.stream().forEach(dir -> {
			BlockPos p = pos.relative(dir);
			var adjacent = this.elementSet.getExistingElementAt(p);
			if (adjacent == null) return; // Not computed.
			if (adjacent.isElasticallyDeforming()) return; // other group
			if (!adjacent.setCluster(cluster)) return; // already added to a group
			queue.add(adjacent);
		});
		return this.analyseElementStatus(elem);
	}
	
	private GroupResult analyseElementStatus(FemElement elem) {
		BlockPos pos = elem.getPosition();
		var elasticProperty = this.level.getBlockProperty(pos);
		var mass = elasticProperty.getMass();
		
		boolean fixed = false;
		double[] inertia = new double[3];
		for (ElemVertex vertex : ElemVertex.values()) {
			BlockPos p = pos.offset(vertex.getOffset());
			var node = this.nodeSet.getNodeAt(p);
			fixed |= node.getDisp().equals(Vec3.ZERO);
			var displacement = this.nodeSet.getDisplacementAt(p);
			inertia[0] += mass * displacement.x();
			inertia[1] += mass * displacement.y();
			inertia[2] += mass * displacement.z();
		}
		
		return new GroupResult(fixed, inertia);
	}
	
	private static class GroupResult {
		private boolean fixed = false;
		private double[] inertia = new double[3];
		private int depth;
		
		public GroupResult(boolean fixed, double[] inertia) {
			this.fixed = fixed;
			this.inertia = inertia;
		}
		
		public static GroupResult empty() {
			return new GroupResult(false, new double[3]);
		}
		
		public GroupResult add(GroupResult other) {
			this.fixed |= other.fixed;
			var inertia = other.inertia;
			this.inertia[0] += inertia[0];
			this.inertia[1] += inertia[1];
			this.inertia[2] += inertia[2];
			this.depth = Math.max(this.depth, other.depth);
			return this;
		}
		
		@Override
		public String toString() {
			return "fix=" + this.fixed + ",in=" + this.inertia; 
		}
	}
}
