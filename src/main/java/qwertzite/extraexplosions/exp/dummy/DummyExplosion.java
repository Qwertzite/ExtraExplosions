package qwertzite.extraexplosions.exp.nodamage;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.Sets;

import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import qwertzite.extraexplosions.core.explosion.EeExplosionBase;

public class DummyExplosion extends EeExplosionBase {

	@OnlyIn(Dist.CLIENT)
	public DummyExplosion(Level worldIn, Entity entityIn, double x, double y, double z, float size, List<BlockPos> affectedPositions) {
		super(worldIn, entityIn, x, y, z, size, false, BlockInteraction.NONE, affectedPositions);
	}

	public DummyExplosion(Level worldIn, Entity entityIn, double x, double y, double z, float size) {
		super(worldIn, entityIn, x, y, z, size, false, BlockInteraction.NONE);
	}
	
	@Override
	public void explode() {
		this.level.gameEvent(this.source, GameEvent.EXPLODE, new Vec3(this.x, this.y, this.z));
		Set<BlockPos> destroyed = Sets.newHashSet();
		final int cube = 16;

		for (int ix = 0; ix < cube; ++ix) {
			for (int iy = 0; iy < cube; ++iy) {
				for (int iz = 0; iz < cube; ++iz) {
					if (ix == 0 || ix == cube-1 || iy == 0 || iy == cube-1 || iz == 0 || iz == cube-1) {
						double dirX = (double) ((float) ix / 15.0F * 2.0F - 1.0F);
						double dirY = (double) ((float) iy / 15.0F * 2.0F - 1.0F);
						double dirZ = (double) ((float) iz / 15.0F * 2.0F - 1.0F);
						double dirL = Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
						dirX /= dirL;
						dirY /= dirL;
						dirZ /= dirL;
						
						double cx = this.x;
						double cy = this.y;
						double cz = this.z;
						float decrease = 0.3F;
						for (float f = this.radius * (0.7F + this.level.random.nextFloat() * 0.6F); f > 0.0F; f -= 0.22500001F) {
							BlockPos blockpos = new BlockPos(cx, cy, cz);
							BlockState blockstate = this.level.getBlockState(blockpos);
							FluidState fluidstate = this.level.getFluidState(blockpos);
							if (!this.level.isInWorldBounds(blockpos)) {
								break;
							}
							Optional<Float> optional = this.damageCalculator.getBlockExplosionResistance(this, this.level, blockpos, blockstate, fluidstate);
							if (optional.isPresent()) {
								if (optional.get() != 0) f = 0;
								else f -= (optional.get() + decrease) * decrease;
							}
							if (f > 0.0F && this.damageCalculator.shouldBlockExplode(this, this.level, blockpos, blockstate, f)) {
								destroyed.add(blockpos);
							}
							cx += dirX * (double) decrease;
							cy += dirY * (double) decrease;
							cz += dirZ * (double) decrease;
						}
					}
				}
			}
		}
		this.toBlow.addAll(destroyed);
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
		
		Util.shuffle(this.toBlow, this.level.random);

		if (pSpawnParticles) {
			for (BlockPos blockpos : this.toBlow) {
				double ax = (double) ((float) blockpos.getX() + this.random.nextFloat());
				double ay = (double) ((float) blockpos.getY() + this.random.nextFloat());
				double az = (double) ((float) blockpos.getZ() + this.random.nextFloat());
				double rx = ax - this.x;
				double ry = ay - this.y;
				double rz = az - this.z;
				double len = (double) Mth.sqrt((float) (rx * rx + ry * ry + rz * rz));
				rx = rx / len;
				ry = ry / len;
				rz = rz / len;
				double rad = 0.5D / (len / (double) this.radius + 0.1D);
				rad = rad * (double) (this.random.nextFloat() * this.random.nextFloat() + 0.3F);
				rx = rx * rad;
				ry = ry * rad;
				rz = rz * rad;
				this.level.addParticle(ParticleTypes.POOF, (ax + this.x) / 2.0D, (ay + this.y) / 2.0D, (az + this.z) / 2.0D, rx, ry, rz);
				this.level.addParticle(ParticleTypes.SMOKE, ax, ay, az, rx, ry, rz);
			}
		}
	}
}
