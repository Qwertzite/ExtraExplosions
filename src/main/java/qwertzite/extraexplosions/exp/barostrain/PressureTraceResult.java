package qwertzite.extraexplosions.exp.barostrain;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public record PressureTraceResult(BlockPos hitBlock, Direction hitFace, Vec3 hitPos) {

}
