package qwertzite.extraexplosions.exp.spherical;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Pair;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ProtectionEnchantment;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import qwertzite.extraexplosions.core.ModLog;
import qwertzite.extraexplosions.core.debug.DebugRenderer;
import qwertzite.extraexplosions.core.explosion.EeExplosionBase;
import qwertzite.extraexplosions.exmath.RayTrigonal;
import qwertzite.extraexplosions.util.math.EeMath;

public class SphericalExplosion extends EeExplosionBase {

	@OnlyIn(Dist.CLIENT)
	public SphericalExplosion(Level worldIn, Entity entityIn, double x, double y, double z, float size, boolean fire, BlockInteraction interaction, List<BlockPos> affectedPositions) {
		super(worldIn, entityIn, x, y, z, size, fire, interaction, affectedPositions);
	}

	public SphericalExplosion(Level worldIn, Entity entityIn, double x, double y, double z, float size, boolean fire, BlockInteraction interaction) {
		super(worldIn, entityIn, x, y, z, size, fire, interaction);
	}
	
	@Override
	public void explode() {
		
		this.level.gameEvent(this.source, GameEvent.EXPLODE, new Vec3(this.x, this.y, this.z));
		
		DebugRenderer.clear();
		Set<BlockPos> destroyed = Sets.newConcurrentHashSet();
		
		Set<ExplosionRay> trigonals = Stream.of(RayTrigonal.createInitialSphere(this.getPosition(), /* init radius */ 4.2d, /* division step */ 3, this.random))
				.parallel()
				.map(ray -> { 
					float intencityBase = ThreadLocalRandom.current().nextFloat();
					return new ExplosionRay(ray, intencityBase, this.destructionIntencity(intencityBase), 0.0d, 0);
				})
				.collect(Collectors.toSet());
		
		var levelCache = new SphericalLevelCache(this.level);
		SphericalLevelCache.execute(levelCache, () -> {
			levelCache.getBlockProperty(new BlockPos(this.getPosition())); // load block at the centre to reduce race conditions.
			var tmp = trigonals;
			while (!tmp.isEmpty()) {
				DebugRenderer.addRays(tmp.parallelStream().map(ray -> ray.trigonal()).collect(Collectors.toSet()));
				tmp = tmp.parallelStream().<ExplosionRay>flatMap(
						ray -> {
							try {
								Stream<ExplosionRay> res = this.rayTraceStep(ray, destroyed, levelCache);
								return res;
							} catch (Exception e) {
								e.printStackTrace();
								ModLog.warn("Caught an unexpected exception while ray trace step. Please contact distributor of this mod.", e);
								return Stream.empty();
							}
						}).collect(Collectors.toSet());
			}
		});
		
		this.toBlow.addAll(destroyed);
		
		this.attackEntity();
	}
	
	private Stream<ExplosionRay> rayTraceStep(ExplosionRay ray, Set<BlockPos> destroyed, SphericalLevelCache blockProperty) {
		
		Stream<ExplosionRay> ret;
		final float decrease = 0.3F;
		
		var from = ray.trigonal().from();
		var to = ray.trigonal().to();
		double dirX = to.x() - from.x();
		double dirY = to.y() - from.y();
		double dirZ = to.z() - from.z();
		double dirL2 = dirX * dirX + dirY * dirY + dirZ * dirZ;
		double dirL = Math.sqrt(dirL2);
		dirX /= dirL;
		dirY /= dirL;
		dirZ /= dirL;
		
		double ox = from.x();
		double oy = from.y();
		double oz = from.z();
		double cx = dirX * ray.posOffset();
		double cy = dirY * ray.posOffset();
		double cz = dirZ * ray.posOffset();
		
		float f = ray.intencity();
		for (; f > 0.0F; f -= 0.225F) {
			if (dirL2 < cx*cx + cy*cy + cz*cz) break;
			BlockPos blockpos = new BlockPos(cx + ox, cy + oy, cz + oz);
			
			var property = blockProperty.getBlockProperty(blockpos);
			if (!property.isInWorldBounds()) { 
				f = 0.0f;
				break;
			}
			BlockState blockstate = property.getBlockState();
			FluidState fluidstate = property.getFluidState();
			
			Optional<Float> optional = this.damageCalculator.getBlockExplosionResistance(this, this.level, blockpos, blockstate, fluidstate);
			if (optional.isPresent()) { f -= (optional.get() + decrease) * decrease; }
			if (f > 0.0F && this.damageCalculator.shouldBlockExplode(this, this.level, blockpos, blockstate, f)) { destroyed.add(blockpos); }
			
			cx += dirX * (double) decrease;
			cy += dirY * (double) decrease;
			cz += dirZ * (double) decrease;
		}
		if (f <= 0.0f) return Stream.empty();
		
		Random rand = ThreadLocalRandom.current();
		int n = ray.divStep() + 1;
		
		RayTrigonal[] nextTrigonals = ray.trigonal().divide();
		var offset = Math.sqrt(cx*cx + cy*cy + cz*cz) - dirL;
		float diminished = this.destructionIntencity(ray.intencityBase) - f;
		
		float randMax = EeMath.pow(0.5f, n) * (float) Math.pow(rand.nextFloat(), 1.0/4); // new
		double[] intencity = { ray.intencityBase,
				ray.intencityBase - rand.nextFloat()*randMax,
				ray.intencityBase - rand.nextFloat()*randMax,
				ray.intencityBase - rand.nextFloat()*randMax, };
		
		EeMath.shuffle(intencity, rand);
		
		ret = IntStream.range(0, 4)
				.mapToObj(i -> new ExplosionRay(nextTrigonals[i], (float) intencity[i], this.destructionIntencity(intencity[i]) - diminished, offset, n));
		return ret;
	}
	
	private float destructionIntencity(double intencity) {
		double a = 8.0d;
		double b = a*a / 4.0d;
		double c = -a/2.0d; // -sqrt(b)
		return (float) (this.radius *1.0d + 0.6d * (Math.sqrt(this.radius*a + b) + c) * (intencity-0.5d));
	}
	
	private void attackEntity() {
		double attackRange = this.radius * 2.0F;
		int minX = Mth.floor(this.x - attackRange - 1.0D);
		int maxX = Mth.floor(this.x + attackRange + 1.0D);
		int minY = Mth.floor(this.y - attackRange - 1.0D);
		int maxY = Mth.floor(this.y + attackRange + 1.0D);
		int minZ = Mth.floor(this.z - attackRange - 1.0D);
		int maxZ = Mth.floor(this.z + attackRange + 1.0D);
		List<Entity> list = this.level.getEntities(this.source, new AABB((double) minX, (double) minY, (double) minZ, (double) maxX, (double) maxY, (double) maxZ));
		net.minecraftforge.event.ForgeEventFactory.onExplosionDetonate(this.level, this, list, attackRange);
		Vec3 centre = new Vec3(this.x, this.y, this.z);
		
		for (var entity : list) {
			if (entity.ignoreExplosion()) continue;

			double dimlessDist = Math.sqrt(entity.distanceToSqr(centre)) / (double) attackRange; // dimensionless distance
			if (dimlessDist > 1.0d) continue;

			double dirX = entity.getX() - this.x;
			double dirY = (entity instanceof PrimedTnt ? entity.getY() : entity.getEyeY()) - this.y;
			double dirZ = entity.getZ() - this.z;
			double dist = Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
			if (dist == 0.0d) continue;
			dirX /= dist;
			dirY /= dist;
			dirZ /= dist;
			
			double exposure = (double) getSeenPercent(centre, entity);
			double damageBase = (1.0D - dimlessDist) * exposure;
			entity.hurt(this.getDamageSource(), (float) ((int) ((damageBase * damageBase + damageBase) / 2.0D * 7.0D * (double) attackRange + 1.0D)));
			double knockback = damageBase;
			if (entity instanceof LivingEntity) {
				knockback = ProtectionEnchantment.getExplosionKnockbackAfterDampener((LivingEntity) entity, damageBase);
			}
			
			entity.setDeltaMovement(entity.getDeltaMovement().add(dirX * knockback, dirY * knockback, dirZ * knockback));
			if (entity instanceof Player) {
				Player player = (Player) entity;
				if (!player.isSpectator() && (!player.isCreative() || !player.getAbilities().flying)) {
					this.hitPlayers.put(player, new Vec3(dirX * damageBase, dirY * damageBase, dirZ * damageBase));
				}
			}
		}
	}

	@Override
	public void finalizeExplosion(boolean pSpawnParticles) {
		if (this.level.isClientSide) {
			this.level.playLocalSound(this.x, this.y, this.z, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 4.0F,
					(1.0F + (this.level.random.nextFloat() - this.level.random.nextFloat()) * 0.2F) * 0.7F, false);
		}

		if (pSpawnParticles) {
			if (this.radius >= 2.0F) {
				this.level.addParticle(ParticleTypes.EXPLOSION_EMITTER, this.x, this.y, this.z, 1.0D, 0.0D, 0.0D);
			} else {
				this.level.addParticle(ParticleTypes.EXPLOSION, this.x, this.y, this.z, 1.0D, 0.0D, 0.0D);
			}
		}
		
		if (this.blockInteraction != Explosion.BlockInteraction.NONE) {
			ObjectArrayList<Pair<ItemStack, BlockPos>> objectarraylist = new ObjectArrayList<>();
			boolean explodedByPlayer = this.getSourceMob() instanceof Player;
			Util.shuffle(this.toBlow, this.level.random);

			for (BlockPos blockpos : this.toBlow) {
				BlockState blockstate = this.level.getBlockState(blockpos);
				if (!blockstate.isAir()) {
					BlockPos immutablePos = blockpos.immutable();
					this.level.getProfiler().push("ee.spherical.explosion_blocks");
					if (blockstate.canDropFromExplosion(this.level, blockpos, this)) {
						Level level = this.level;
						if (level instanceof ServerLevel) {
							ServerLevel serverlevel = (ServerLevel) level;
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
		
//		if (pSpawnParticles) { old style fragment-like smoke.
//			for (BlockPos blockpos : this.toBlow) {
//				double ax = (double) ((float) blockpos.getX() + this.random.nextFloat());
//				double ay = (double) ((float) blockpos.getY() + this.random.nextFloat());
//				double az = (double) ((float) blockpos.getZ() + this.random.nextFloat());
//				double rx = ax - this.x;
//				double ry = ay - this.y;
//				double rz = az - this.z;
//				double len = (double) Mth.sqrt((float) (rx * rx + ry * ry + rz * rz));
//				rx = rx / len;
//				ry = ry / len;
//				rz = rz / len;
//				double rad = 0.5D / (len / (double) this.radius + 0.1D);
//				rad = rad * (double) (this.random.nextFloat() * this.random.nextFloat() + 0.3F);
//				rx = rx * rad;
//				ry = ry * rad;
//				rz = rz * rad;
//				this.level.addParticle(ParticleTypes.POOF, (ax + this.x) / 2.0D, (ay + this.y) / 2.0D, (az + this.z) / 2.0D, rx, ry, rz);
//				this.level.addParticle(ParticleTypes.SMOKE, ax, ay, az, rx, ry, rz);
//			}
//		}
		
		if (this.fire) {
			for (BlockPos blockPos : this.toBlow) {
				if (this.random.nextInt(3) == 0 && this.level.getBlockState(blockPos).isAir()
						&& this.level.getBlockState(blockPos.below()).isSolidRender(this.level, blockPos.below())) {
					this.level.setBlockAndUpdate(blockPos, BaseFireBlock.getState(this.level, blockPos));
				}
			}
		}
		
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
	
	public record ExplosionRay(RayTrigonal trigonal, float intencityBase, float intencity, double posOffset, int divStep) {}
}
