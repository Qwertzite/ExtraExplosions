package qwertzite.extraexplosions.api;

import javax.annotation.Nullable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Explosion.BlockInteraction;
import net.minecraft.world.level.Level;
import qwertzite.extraexplosions.core.network.ModNetwork;
import qwertzite.extraexplosions.exp.barostrain.BaroStrainExplosion;
import qwertzite.extraexplosions.exp.barostrain.PacketBaroStrainExplosion;
import qwertzite.extraexplosions.exp.dummy.DummyExplosion;
import qwertzite.extraexplosions.exp.dummy.PacketDummyExplosion;
import qwertzite.extraexplosions.exp.spherical.PacketSphericalExplosion;
import qwertzite.extraexplosions.exp.spherical.SphericalExplosion;



public class ExtraExplosions {
	
//	/**
//	 * Creates torches.
//	 * Equivalent to {@link World#newExplosion(Entity, double, double, double, float, boolean, boolean)}
//	 * @param world
//	 * @param entityIn
//	 * @param x
//	 * @param y
//	 * @param z
//	 * @param strength
//	 * @param torch Number of torch. negative value means no limit.
//	 * @param isSmoking
//	 * @return
//	 */
//	public static Explosion torchExplosion(Level world, @Nullable Entity entityIn, double x, double y, double z, float strength, int torch, boolean isSmoking) {
//		boolean remote = world.isClientSide();
//		TorchExplosion explosion = new TorchExplosion(world, entityIn, x, y, z, strength, torch, isSmoking);
//		if (net.minecraftforge.event.ForgeEventFactory.onExplosionStart(world, explosion)) return explosion;
//		explosion.doExplosionA();
//		explosion.doExplosionB(remote);
//
//		if (!remote) {
//			if (!isSmoking) { explosion.clearAffectedBlockPositions(); }
//
//			for (EntityPlayer entityplayer : world.playerEntities) {
//				if (entityplayer.getDistanceSq(x, y, z) < 4096.0D) {
//					((EntityPlayerMP) entityplayer).connection // explosionBが変わっていないなら，SPacketExplosionでもよい
//							.sendPacket(new SPacketExplosion(x, y, z, strength, explosion.getAffectedBlockPositions(),
//									(Vec3d) explosion.getPlayerKnockbackMap().get(entityplayer)));
//				}
//			}
//		}
//		return explosion;
//	}
	
	/**
	 * An explosion which does not destroy blocks, damage entities, nor knock back entities.
	 * @param world
	 * @param entityIn
	 * @param x
	 * @param y
	 * @param z
	 * @param strength
	 * @return
	 */
	public static Explosion dummyExplosion(Level world, @Nullable Entity entityIn, double x, double y, double z, float strength) {
		boolean remote = world.isClientSide();
		DummyExplosion explosion = new DummyExplosion(world, entityIn, x, y, z, strength);
		if (net.minecraftforge.event.ForgeEventFactory.onExplosionStart(world, explosion)) return explosion;
		explosion.explode();
		explosion.finalizeExplosion(remote);

		if (!remote) {
			ServerLevel level = (ServerLevel) world;
			for (ServerPlayer serverplayer : level.players()) {
				if (serverplayer.distanceToSqr(x, y, z) < 4096.0D) {
					ModNetwork.sendTo(serverplayer, new PacketDummyExplosion(x, y, z, strength, explosion.getToBlow(), null));
				}
			}
		}
		return explosion;
	}
	
	public static Explosion sphericalExplosion(Level world, @Nullable Entity entity, double x, double y, double z, float strength, boolean fire, BlockInteraction interaction) {
		boolean remote = world.isClientSide();
		var explosion = new SphericalExplosion(world, entity, x, y, z, strength, fire, interaction);
		if (net.minecraftforge.event.ForgeEventFactory.onExplosionStart(world, explosion)) return explosion;
		explosion.explode();
		explosion.finalizeExplosion(remote);
		
		if (!remote) {
			ServerLevel level = (ServerLevel) world;
			for (ServerPlayer serverplayer : level.players()) {
				if (serverplayer.distanceToSqr(x, y, z) < 4096.0D) {
					ModNetwork.sendTo(serverplayer, new PacketSphericalExplosion(x, y, z, strength, fire, explosion.getToBlow(), null));
				}
			}
		}
		return explosion;
	}
	
	public static Explosion baroStrainExplosion(Level world, @Nullable Entity entity, double x, double y, double z, float strength, BlockInteraction interaction) {
		boolean remote = world.isClientSide();
		var explosion = new BaroStrainExplosion(world, entity, x, y, z, strength, interaction);
		if (net.minecraftforge.event.ForgeEventFactory.onExplosionStart(world, explosion)) return explosion;
		explosion.explode();
		explosion.finalizeExplosion(remote);
		
		if (!remote) {
			ServerLevel level = (ServerLevel) world;
			for (ServerPlayer serverplayer : level.players()) {
				if (serverplayer.distanceToSqr(x, y, z) < 4096.0D) {
					ModNetwork.sendTo(serverplayer, new PacketBaroStrainExplosion(x, y, z, strength, explosion.getToBlow(), null));
				}
			}
		}
		return explosion;
	}
}
