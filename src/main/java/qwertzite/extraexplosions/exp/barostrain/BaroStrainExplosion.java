package qwertzite.extraexplosions.exp.barostrain;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.mojang.datafixers.util.Pair;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import qwertzite.extraexplosions.core.ModLog;
import qwertzite.extraexplosions.core.debug.DebugRenderer;
import qwertzite.extraexplosions.core.explosion.EeExplosionBase;
import qwertzite.extraexplosions.exmath.RayTrigonal;
import qwertzite.extraexplosions.util.LevelCache;

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
		
		DebugRenderer.clear();
		double division = 1.0d * 1d; // TODO: バニラと同じくらいになるように調整する
		
		this.level.gameEvent(this.source, GameEvent.EXPLODE, new Vec3(this.x, this.y, this.z));
		
		var levelCache = new BarostrainLevelCache(level, this);
		
		FEM fem = new FEM(levelCache);
		
		var hitRays = new ConcurrentHashMap<PressureRay, PressureTraceResult>();
		LevelCache.execute(levelCache, () -> {
			Set<PressureRay> trigonals = Stream.of(RayTrigonal.createInitialSphere(this.getPosition(), /* init radius */ 4.2d, /* division step */ 3, this.random))
					.parallel()
					.map(ray -> { 
						return new PressureRay(ray, this.radius, division);
					})
					.collect(Collectors.toSet());
			
			while (!trigonals.isEmpty()) {
				
				// ray tracing till all the rays hit a block or diminish.
				ConcurrentLinkedQueue<PressureRay> queue = new ConcurrentLinkedQueue<>();
				queue.addAll(trigonals);
				while (!queue.isEmpty()) {
					Stream.generate(() -> queue.poll()).parallel().takeWhile(Objects::nonNull)
					.forEach(ray -> this.rayTraceStep(ray, fem, levelCache, hitRays).forEach(queue::offer));
				}
				trigonals.clear();
				
				fem.compute();
				
				// reflected or transmitted rays.
				// TODO: ここから hitRays -> entries
				
			}
		});
		DebugRenderer.addVertexDisplacement(levelCache, fem.getNodeSet(), fem.getElementSet());
		
		this.toBlow.addAll(levelCache.getDestroyeds());
		
		System.out.println("boom! A");
	}
	
	private Stream<PressureRay> rayTraceStep(PressureRay ray, FEM fem, BarostrainLevelCache levelAccess, ConcurrentHashMap<PressureRay, PressureTraceResult> hitRays) {
		var from = ray.trigonal().from();
		var to = ray.trigonal().to();
		
		PressureTraceResult traceResult = null;
		boolean internal = true;
		if (ray.initial()) {
			traceResult = this.internalPressureTrace(from, to, levelAccess);
		}
		if (traceResult == null) {
			traceResult = this.pressureTrace(from, to, levelAccess);
			internal = false;
		}
		
		if (traceResult != null) {
			double distance = from.distanceTo(traceResult.hitPos());
			double pressure = ray.computePressureAt(distance) * (internal ? -1 : 1);
			
			if (pressure > 0.0d) {
				fem.applyPressure(traceResult.hitBlock(), traceResult.hitFace(), pressure);
				hitRays.put(ray, traceResult);
			}
			
			return Stream.empty(); // TODO: reflected rays and transmitted rays
		}
		
		double rayLength = from.distanceTo(to);
		if (ray.computePressureAt(rayLength) <= 0.0d) return Stream.empty();
		RayTrigonal[] nextTrigonals = ray.trigonal().divide();
		double nextDivision = ray.division() / 4.0d;
		double travelled = ray.travelledDistance() + rayLength;
		int nextDivStep = ray.divStep() + 1;
		return Arrays.stream(nextTrigonals).map(t -> new PressureRay(t, ray.intencity(), nextDivision, travelled, nextDivStep, false));
	}
	
	private PressureTraceResult internalPressureTrace(Vec3 from, Vec3 to, BarostrainLevelCache levelAccess) {
		double dirX = to.x - from.x;
		double dirY = to.y - from.y;
		double dirZ = to.z - from.z;
		double length = Math.sqrt(dirX*dirX + dirY*dirY + dirZ*dirZ);
		if (length == 0) return null; // no hit
		
		double currentX = from.x;
		double currentY = from.y;
		double currentZ = from.z;
		if (currentX % 1 == 0 || currentY % 1 == 0 || currentZ % 1 == 0) return null; // treat as external block hit when starting point is on block border.
		int blockX = Mth.floor(currentX);
		int blockY = Mth.floor(currentY);
		int blockZ = Mth.floor(currentZ);
		BlockPos pos = new BlockPos(blockX, blockY, blockZ);
		if (levelAccess.isCurrentlyAirAt(pos)) return null;
		
		double invNDirX = dirX != 0 ? length / dirX :  Double.POSITIVE_INFINITY; // positive infinity if d = 0, because sign is treated as positive.
		double invNDirY = dirY != 0 ? length / dirY :  Double.POSITIVE_INFINITY;
		double invNDirZ = dirZ != 0 ? length / dirZ :  Double.POSITIVE_INFINITY;
		double normDirX = dirX / length;
		double normDirY = dirY / length;
		double normDirZ = dirZ / length;
		boolean dSignXPosv = dirX >= 0; // whether sign of delta x is positive.
		boolean dSignYPosv = dirY >= 0;
		boolean dSignZPosv = dirZ >= 0;
		
		// when from < to then floor + 1 else ceil - 1
		double stepX = dSignXPosv ? Math.floor(currentX) + 1 : Math.ceil(currentX) - 1;
		double stepY = dSignYPosv ? Math.floor(currentY) + 1 : Math.ceil(currentY) - 1;
		double stepZ = dSignZPosv ? Math.floor(currentZ) + 1 : Math.ceil(currentZ) - 1;
		
		double lenX = (stepX - currentX) * invNDirX;
		double lenY = (stepY - currentY) * invNDirY;
		double lenZ = (stepZ - currentZ) * invNDirZ;
		
		Axis hitAxis = this.computeHitAxis(lenX, lenY, lenZ, normDirX, normDirY, normDirZ);
		
		double len;
		Direction hitFace;
		switch (hitAxis) {
		case X:
			len = lenX;
			hitFace = !dSignXPosv ? Direction.WEST : Direction.EAST; // +x: east, -x: west
			break;
		case Y:
			len = lenY;
			hitFace = !dSignYPosv ? Direction.DOWN : Direction.UP;
			break;
		case Z:
			len = lenZ;
			hitFace = !dSignZPosv ? Direction.NORTH : Direction.SOUTH; // +z: south, -z: north
			break;
		default:
			assert(false);
			len = 0.0f;
			hitFace = null;
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
			throw new RuntimeException("This is not supposed to happen!");
		}
		double distToEnd =
				(to.x() - currentX) * normDirX +
				(to.y() - currentY) * normDirY + 
				(to.z() - currentZ) * normDirZ;
		if (distToEnd < len) return null; // reached the end of this ray before reaching another block bound.
		
		return new PressureTraceResult(pos, hitFace, new Vec3(nextX, nextY, nextZ));
	}
	
	
	/**
	 * 圧力用の正確なレイトレーシングを行う．
	 * from と to に差がない場合は反射しない．
	 * 
	 * @param from
	 * @param to
	 * @param levelAccess
	 * @return
	 */
	private PressureTraceResult pressureTrace(Vec3 from, Vec3 to, BarostrainLevelCache levelAccess) {
		double dirX = to.x - from.x;
		double dirY = to.y - from.y;
		double dirZ = to.z - from.z;
		double length = Math.sqrt(dirX*dirX + dirY*dirY + dirZ*dirZ);
		if (length == 0) return null; // no hit
		double invNDirX = dirX != 0 ? length / dirX :  Double.POSITIVE_INFINITY; // positive infinity if d = 0, because sign is treated as positive.
		double invNDirY = dirY != 0 ? length / dirY :  Double.POSITIVE_INFINITY;
		double invNDirZ = dirZ != 0 ? length / dirZ :  Double.POSITIVE_INFINITY;
		double normDirX = dirX / length;
		double normDirY = dirY / length;
		double normDirZ = dirZ / length;
		boolean dSignXPosv = dirX >= 0; // whether sign of delta x is positive.
		boolean dSignYPosv = dirY >= 0;
		boolean dSignZPosv = dirZ >= 0;
		
		double currentX = from.x;
		double currentY = from.y;
		double currentZ = from.z;
		// when from < to then floor + 1 else ceil - 1
		double stepX = dSignXPosv ? Math.floor(currentX) + 1 : Math.ceil(currentX) - 1;
		double stepY = dSignYPosv ? Math.floor(currentY) + 1 : Math.ceil(currentY) - 1;
		double stepZ = dSignZPosv ? Math.floor(currentZ) + 1 : Math.ceil(currentZ) - 1;
		
		while (true) {
			double lenX = (stepX - currentX) * invNDirX;
			double lenY = (stepY - currentY) * invNDirY;
			double lenZ = (stepZ - currentZ) * invNDirZ;
			
			Axis hitAxis = this.computeHitAxis(lenX, lenY, lenZ, normDirX, normDirY, normDirZ);
			
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
				hitFace = null;
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
			double distToEnd =
					(to.x() - currentX) * normDirX +
					(to.y() - currentY) * normDirY + 
					(to.z() - currentZ) * normDirZ;
			if (distToEnd < len) return null; // reached the end of this ray before reaching another block bound.
			
			
			int blockX = Mth.floor(nextX) - (hitFace == Direction.EAST ? 1 : 0);
			int blockY = Mth.floor(nextY) - (hitFace == Direction.UP ? 1 : 0);
			int blockZ = Mth.floor(nextZ) - (hitFace == Direction.SOUTH ? 1 : 0);
			BlockPos pos = new BlockPos(blockX, blockY, blockZ);
			
			if (!levelAccess.isCurrentlyAirAt(pos)) {
				return new PressureTraceResult(pos, hitFace, new Vec3(nextX, nextY, nextZ));
			}
			
			
			currentX = nextX;
			currentY = nextY;
			currentZ = nextZ;
			
			stepX = dSignXPosv ? Math.floor(currentX) + 1 : Math.ceil(currentX) - 1;
			stepY = dSignYPosv ? Math.floor(currentY) + 1 : Math.ceil(currentY) - 1;
			stepZ = dSignZPosv ? Math.floor(currentZ) + 1 : Math.ceil(currentZ) - 1;
			
		}
	}
	
	
	private Axis computeHitAxis(double lenX, double lenY, double lenZ, double normDirX, double normDirY, double normDirZ) {
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
		return hitAxis;
	}

	@Override
	public void finalizeExplosion(boolean pSpawnParticles) {
		// TODO Auto-generated method stub
		// delete following program.
		
		if (this.blockInteraction != Explosion.BlockInteraction.NONE) {
			ObjectArrayList<Pair<ItemStack, BlockPos>> objectarraylist = new ObjectArrayList<>();
			boolean explodedByPlayer = this.getSourceMob() instanceof Player;
			Util.shuffle(this.toBlow, this.level.random);
			System.out.println("Destroy blocks! " + this.toBlow.size());
			for (BlockPos blockpos : this.toBlow) {
				BlockState blockstate = this.level.getBlockState(blockpos);
//				Block block = blockstate.getBlock();
				if (!blockstate.isAir()) {
					BlockPos immutablePos = blockpos.immutable();
					this.level.getProfiler().push("ee.spherical.explosion_blocks");
					if (blockstate.canDropFromExplosion(this.level, blockpos, this)) {
						Level level = this.level;
						if (level instanceof ServerLevel serverlevel) {
							
							BlockEntity blockentity = blockstate.hasBlockEntity() ? this.level.getBlockEntity(blockpos) : null;
							LootContext.Builder lootcontext$builder = (new LootContext.Builder(serverlevel)).withRandom(this.level.random)
									.withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(blockpos)).withParameter(LootContextParams.TOOL, ItemStack.EMPTY)
									.withOptionalParameter(LootContextParams.BLOCK_ENTITY, blockentity)
									.withOptionalParameter(LootContextParams.THIS_ENTITY, this.source);
							if (this.blockInteraction == Explosion.BlockInteraction.DESTROY) {
								lootcontext$builder.withParameter(LootContextParams.EXPLOSION_RADIUS, this.radius);
							}

							blockstate.spawnAfterBreak(serverlevel, blockpos, ItemStack.EMPTY, explodedByPlayer);
							blockstate.getDrops(lootcontext$builder).forEach((stack) -> {
								addBlockDrops(objectarraylist, stack, immutablePos);
							});
						}
					}

					blockstate.onBlockExploded(this.level, blockpos, this);
					this.level.getProfiler().pop();
				}
			}

			for (Pair<ItemStack, BlockPos> pair : objectarraylist) {
				Block.popResource(this.level, pair.getSecond(), pair.getFirst());
			}
		}
		
		System.out.println("boom! B");
	}

	
	private static void addBlockDrops(ObjectArrayList<Pair<ItemStack, BlockPos>> pDropPositionArray, ItemStack pStack, BlockPos pPos) {
		int i = pDropPositionArray.size();
		for (int j = 0; j < i; ++j) {
			Pair<ItemStack, BlockPos> pair = pDropPositionArray.get(j);
			ItemStack itemstack = pair.getFirst();
			if (ItemEntity.areMergable(itemstack, pStack)) {
				ItemStack itemstack1 = ItemEntity.merge(itemstack, pStack, 16);
				pDropPositionArray.set(j, Pair.of(itemstack1, pair.getSecond()));
				if (pStack.isEmpty()) {
					return;
				}
			}
		}
		pDropPositionArray.add(Pair.of(pStack, pPos));
	}
	
}
