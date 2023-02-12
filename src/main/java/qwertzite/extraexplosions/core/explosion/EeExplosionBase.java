package qwertzite.extraexplosions.core.explosion;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.common.collect.Maps;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.EntityBasedExplosionDamageCalculator;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Overrides all the methods and fields in {@link Explosion}
 * @author Qwertzite
 * @date 2022/11/17
 */
public abstract class EeExplosionBase extends Explosion {
	private static final ExplosionDamageCalculator EXPLOSION_DAMAGE_CALCULATOR = new ExplosionDamageCalculator();
	protected static final int MAX_DROPS_PER_COMBINED_STACK = 16;
	protected final boolean fire;
	protected final Explosion.BlockInteraction blockInteraction;
	protected final RandomSource random = RandomSource.create();
	protected final Level level;
	protected final double x;
	protected final double y;
	protected final double z;
	@Nullable
	protected final Entity source;
	protected final float radius;
	protected final DamageSource damageSource;
	protected final ExplosionDamageCalculator damageCalculator;
	protected final ObjectArrayList<BlockPos> toBlow = new ObjectArrayList<>();
	protected final Map<Player, Vec3> hitPlayers = Maps.newHashMap();
	protected final Vec3 position;

	public EeExplosionBase(Level pLevel, @Nullable Entity pSource, double pToBlowX, double pToBlowY, double pToBlowZ, float pRadius) {
		this(pLevel, pSource, pToBlowX, pToBlowY, pToBlowZ, pRadius, false, Explosion.BlockInteraction.DESTROY);
	}

	public EeExplosionBase(Level pLevel, @Nullable Entity pSource, double pToBlowX, double pToBlowY, double pToBlowZ, float pRadius,
			List<BlockPos> pPositions) {
		this(pLevel, pSource, pToBlowX, pToBlowY, pToBlowZ, pRadius, false, Explosion.BlockInteraction.DESTROY, pPositions);
	}

	public EeExplosionBase(Level pLevel, @Nullable Entity pSource, double pToBlowX, double pToBlowY, double pToBlowZ, float pRadius, boolean pFire,
			Explosion.BlockInteraction pBlockInteraction, List<BlockPos> pPositions) {
		this(pLevel, pSource, pToBlowX, pToBlowY, pToBlowZ, pRadius, pFire, pBlockInteraction);
		this.toBlow.addAll(pPositions);
	}

	public EeExplosionBase(Level pLevel, @Nullable Entity pSource, double pToBlowX, double pToBlowY, double pToBlowZ, float pRadius, boolean pFire,
			Explosion.BlockInteraction pBlockInteraction) {
		this(pLevel, pSource, (DamageSource) null, (ExplosionDamageCalculator) null, pToBlowX, pToBlowY, pToBlowZ, pRadius, pFire, pBlockInteraction);
	}

	public EeExplosionBase(Level pLevel, @Nullable Entity pSource, @Nullable DamageSource pDamageSource, @Nullable ExplosionDamageCalculator pDamageCalculator,
			double pToBlowX, double pToBlowY, double pToBlowZ, float pRadius, boolean pFire, Explosion.BlockInteraction pBlockInteraction) {
		super(pLevel, pSource, pDamageSource, pDamageCalculator, pToBlowX, pToBlowY, pToBlowZ, pRadius, pFire, pBlockInteraction);
		this.level = pLevel;
		this.source = pSource;
		this.radius = pRadius;
		this.x = pToBlowX;
		this.y = pToBlowY;
		this.z = pToBlowZ;
		this.fire = pFire;
		this.blockInteraction = pBlockInteraction;
		this.damageSource = pDamageSource == null ? DamageSource.explosion(this) : pDamageSource;
		this.damageCalculator = pDamageCalculator == null ? this.makeDamageCalculator(pSource) : pDamageCalculator;
		this.position = new Vec3(this.x, this.y, this.z);
	}

	private ExplosionDamageCalculator makeDamageCalculator(@Nullable Entity pEntity) {
		return (ExplosionDamageCalculator) (pEntity == null ? EXPLOSION_DAMAGE_CALCULATOR : new EntityBasedExplosionDamageCalculator(pEntity));
	}

	/**
	 * Does the first part of the explosion (destroy blocks)
	 */
	@Override
	public abstract void explode();

	/**
	 * Does the second part of the explosion (sound, particles, drop spawn)
	 */
	@Override
	public abstract void finalizeExplosion(boolean pSpawnParticles);

	@Override
	public DamageSource getDamageSource() {
		return this.damageSource;
	}

	@Override
	public Map<Player, Vec3> getHitPlayers() {
		return this.hitPlayers;
	}

	/**
	 * Returns either the entity that placed the explosive block, the entity that
	 * caused the explosion or null.
	 */
	@Override
	@Nullable
	public LivingEntity getSourceMob() {
		if (this.source == null) {
			return null;
		} else if (this.source instanceof PrimedTnt) {
			return ((PrimedTnt) this.source).getOwner();
		} else if (this.source instanceof LivingEntity) {
			return (LivingEntity) this.source;
		} else {
			if (this.source instanceof Projectile) {
				Entity entity = ((Projectile) this.source).getOwner();
				if (entity instanceof LivingEntity) {
					return (LivingEntity) entity;
				}
			}
			return null;
		}
	}

	@Override
	public void clearToBlow() {
		this.toBlow.clear();
	}

	@Override
	public List<BlockPos> getToBlow() {
		return this.toBlow;
	}

	@Override
	public Vec3 getPosition() {
		return this.position;
	}

	@Override
	@Nullable
	public Entity getExploder() {
		return this.source;
	}
}
