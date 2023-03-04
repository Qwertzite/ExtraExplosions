package qwertzite.extraexplosions.exp.barostrain;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import qwertzite.extraexplosions.core.ModLog;
import qwertzite.extraexplosions.core.explosion.EeExplosionBase;
import qwertzite.extraexplosions.exmath.RayTrigonal;
import qwertzite.extraexplosions.util.collection.AccumulatorMap;

public class BaroStrainExplosion extends EeExplosionBase {

	@OnlyIn(Dist.CLIENT)
	public BaroStrainExplosion(Level worldIn, Entity entityIn, double x, double y, double z, float size, BlockInteraction interaction, List<BlockPos> affectedPositions) {
		super(worldIn, entityIn, x, y, z, size, false, interaction, affectedPositions);
	}

	public BaroStrainExplosion(Level worldIn, Entity entityIn, double x, double y, double z, float size, BlockInteraction interaction) {
		super(worldIn, entityIn, x, y, z, size, false, interaction);
	}
	
	@Override
	public void explode() {
		
		this.level.gameEvent(this.source, GameEvent.EXPLODE, new Vec3(this.x, this.y, this.z));
		
		Set<PressureRay> trigonals = Stream.of(RayTrigonal.createInitialSphere(this.getPosition(), /* init radius */ 4.2d, /* division step */ 3, this.random))
				.parallel()
				.map(ray -> { 
//					float intencityBase = ThreadLocalRandom.current().nextFloat();
					return new PressureRay(ray, this.radius * (0.7F + this.radius * 0.6F), 0);
				})
				.collect(Collectors.toSet());
		
		var blastForce = new AccumulatorMap<BlockPos>();
		while (!trigonals.isEmpty()) {
			trigonals = trigonals.parallelStream().flatMap(ray -> this.rayTraceStep(ray, blastForce)).collect(Collectors.toSet());
		}
		// TODO
		System.out.println("boom! A");
	}
	
	private Stream<PressureRay> rayTraceStep(PressureRay ray, AccumulatorMap<BlockPos> blastForce) {
		var from = ray.trigonal().from();
		var to = ray.trigonal().to();
		double dirX = to.x - from.x;
		double dirY = to.y - from.y;
		double dirZ = to.z - from.z;
		double length = Math.sqrt(dirX*dirX + dirY*dirY + dirZ*dirZ);
		double invNDirX = dirX != 0 ? length / dirX :  Double.POSITIVE_INFINITY; // positive infinity if d = 0, because sign is treated as positive.
		double invNDirY = dirY != 0 ? length / dirY :  Double.POSITIVE_INFINITY;
		double invNDirZ = dirZ != 0 ? length / dirZ :  Double.POSITIVE_INFINITY;
		double normDirX = dirX / length;
		double normDirY = dirY / length;
		double normDirZ = dirZ / length;
		boolean dSignXPosv = dirX >= 0; // whether sign of delta x is positive.
		boolean dSignYPosv = dirY >= 0;
		boolean dSignZPosv = dirZ >= 0;
		
		double goalX = dSignXPosv ? Math.floor(to.x) + 1 : Math.ceil(to.x) - 1;
		double goalY = dSignYPosv ? Math.floor(to.y) + 1 : Math.ceil(to.y) - 1;
		double goalZ = dSignZPosv ? Math.floor(to.z) + 1 : Math.ceil(to.z) - 1;
		
		double currentX = from.x;
		double currentY = from.y;
		double currentZ = from.z;
		// when from < to then floor + 1 else ceil - 1
		double stepX = dSignXPosv ? Math.floor(currentX) + 1 : Math.ceil(currentX) - 1;
		double stepY = dSignYPosv ? Math.floor(currentX) + 1 : Math.ceil(currentX) - 1;
		double stepZ = dSignZPosv ? Math.floor(currentX) + 1 : Math.ceil(currentX) - 1;
		
		while (stepX < goalX && stepY < goalY && stepZ < goalZ) {
			
			double lenX = (stepX - currentX) * invNDirX;
			double lenY = (stepY - currentY) * invNDirY;
			double lenZ = (stepZ - currentZ) * invNDirZ;
			
			Axis hitAxis;
			if (lenX < lenY) {
				if (lenX < lenZ) { /* minimum = x */ hitAxis = Axis.X; }
				else if (lenZ < lenX) { /* minimum = z */ hitAxis = Axis.Z; }
				else { // x = z
					if (normDirX >= normDirZ) { hitAxis = Axis.X; }
					else { hitAxis = Axis.Z; }
				}
			} else if (lenY < lenX) {
				if (lenY < lenZ) { /* minimum = y */ hitAxis = Axis.Y; }
				else if (lenZ < lenY) { /* minimum = z */ hitAxis = Axis.Z; } else { // minimum = y = z
					if (normDirY >= normDirZ) { hitAxis = Axis.Y; }
					else { hitAxis = Axis.Z; }
				}
			} else { // x == y
				if (lenX < lenZ) { // minimum = x = y
					if (normDirX >= normDirY) { hitAxis = Axis.X; }
					else { hitAxis = Axis.Y; } }
				else if (lenZ < lenX){ /* minimum = z */ hitAxis = Axis.Z; }
				else { // x = y = z
					if (normDirX >= normDirY) {
						if (normDirX >= normDirZ) { hitAxis = Axis.X; }
						else { hitAxis = Axis.Z; } // z > x > y
					} else { // y > x
						if (normDirY >= normDirZ) { hitAxis = Axis.Y; }
						else { hitAxis = Axis.Z; } // z > y > x
					}
				}
			}
			
			double len;
			Direction hitFace;
			switch (hitAxis) {
			case X:
				len = lenX;
				hitFace = dSignXPosv ? Direction.WEST : Direction.EAST;
				break;
			case Y:
				len = lenY;
				hitFace = dSignYPosv ? Direction.DOWN : Direction.UP;
				break;
			case Z:
				len = lenZ;
				hitFace = dSignZPosv ? Direction.NORTH : Direction.SOUTH;
				break;
			default:
				assert(false);
				len = 0.0f;
			}
			
			double nextX = currentX + normDirX * len;
			double nextY = currentY + normDirY * len;
			double nextZ = currentZ + normDirZ * len;
			switch (hitAxis) {
			case X -> nextX = stepX;
			case Y -> nextY = stepY;
			case Z -> nextZ = stepZ;
			}
			if (currentX == nextX && currentY == nextY && currentZ == nextZ) {
				ModLog.warn("current = (%f, %f, %f), step=(%f,%f,%f), dir=(%f,%f,%f), ndir=(%f,%f,%f)",
						currentX, currentY, currentZ,
						stepX, stepY, stepZ,
						dirX, dirY, dirZ,
						normDirX, normDirY, normDirZ);
				ModLog.warn("from=%s, to=%s", from, to);
				ModLog.warn("hitAxis=%s", hitAxis);
				throw new RuntimeException("This should not happen!");
			}
			
			currentX = nextX;
			currentY = nextY;
			currentZ = nextZ;
			
			stepX = dSignXPosv ? Math.floor(currentX) + 1 : Math.ceil(currentX) - 1;
			stepY = dSignYPosv ? Math.floor(currentX) + 1 : Math.ceil(currentX) - 1;
			stepZ = dSignZPosv ? Math.floor(currentX) + 1 : Math.ceil(currentX) - 1;
		}
		
		return Stream.empty(); // TODO
	}
	
	private PressureTraceResult pressureTrace() {
		return null;
	}

	@Override
	public void finalizeExplosion(boolean pSpawnParticles) {
		// TODO Auto-generated method stub
		System.out.println("boom! B");
	}

}
