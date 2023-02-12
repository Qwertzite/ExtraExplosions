package qwertzite.extraexplosions.exp.spherical;

import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import qwertzite.extraexplosions.core.debug.DebugRenderer;
import qwertzite.extraexplosions.core.explosion.EeExplosionBase;

public class SphericalExplosion extends EeExplosionBase {

	@OnlyIn(Dist.CLIENT)
	public SphericalExplosion(Level worldIn, Entity entityIn, double x, double y, double z, float size, BlockInteraction interaction, List<BlockPos> affectedPositions) {
		super(worldIn, entityIn, x, y, z, size, false, interaction, affectedPositions);
	}

	public SphericalExplosion(Level worldIn, Entity entityIn, double x, double y, double z, float size, BlockInteraction interaction) {
		super(worldIn, entityIn, x, y, z, size, false, interaction);
	}
	
	@Override
	public void explode() {
		this.level.gameEvent(this.source, GameEvent.EXPLODE, new Vec3(this.x, this.y, this.z));
		
		DebugRenderer.clearRays();
		Set<BlockPos> destroyed = Sets.newHashSet();
		
		Set<RayTrigonal> trigonals = Sets.newHashSet(RayTrigonal.createInitialSphere(this.getPosition(), /* init radius */ 7.6d, /* division step */3));
		System.out.println(trigonals.size());
//		while (!trigonals.isEmpty()) {
//			
//			
//			float decrease = 0.3F;
//			
//			// COMEBACK
//		}
		
		DebugRenderer.addRays(trigonals);
		// TODO Auto-generated method stub
		
	}

	@Override
	public void finalizeExplosion(boolean pSpawnParticles) {
		// TODO Auto-generated method stub
		
	}

}
