package qwertzite.extraexplosions.exp.barostrain;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import qwertzite.extraexplosions.core.explosion.EeExplosionBase;

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
		
		
		// TODO
		System.out.println("boom! A");
	}

	@Override
	public void finalizeExplosion(boolean pSpawnParticles) {
		// TODO Auto-generated method stub
		System.out.println("boom! B");
	}

}
