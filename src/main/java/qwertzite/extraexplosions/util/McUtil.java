package qwertzite.extraexplosions.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.Sets;

import net.minecraft.Util;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.fml.util.thread.SidedThreadGroups;

/**
 * 
 * MineCraft-specific utility methods.
 * 
 * @author Qwertzite
 * @date 2022/09/28
 */
public class McUtil {
	
	/**
	 * For those occasions when {@link World#isRemote} cannot be accessed.
	 * 
	 * @return whether this MOD is run on server.
	 */
	public static boolean isServer() {
		return Thread.currentThread().getThreadGroup() == SidedThreadGroups.SERVER;
	}
	
	/**
	 * Human friendly biome expression.
	 * @param pBiomeHolder
	 * @return
	 */
	public static String printBiome(Holder<Biome> pBiomeHolder) {
		return pBiomeHolder.unwrap().map((p_205377_) -> {
			return p_205377_.location().toString();
		}, (p_205367_) -> {
			return "[unregistered " + p_205367_ + "]";
		});
	}
	
	/**
	 * DO NOT MODIFY RETURNED ARRAY.
	 */
	private static final Direction[] HORIZONTAL_DIR = new Direction[] { Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST };
	public static Direction[] horizontalDir() {
		return HORIZONTAL_DIR;
	}
	
	public static BoundingBox boundingBox(int x1, int y1, int z1, int x2, int y2, int z2) {
		if (x1 > x2) {
			var tmp = x1;
			x1 = x2;
			x2 = tmp;
		}
		if (y1 > y2) {
			var tmp = y1;
			y1 = y2;
			y2 = tmp;
		}
		if (z1 > z2) {
			var tmp = z1;
			z1 = z2;
			z2 = tmp;
		}
		return new BoundingBox(x1, y1, z1, x2, y2, z2);
	}
	
	/**
	 * 
	 * @param index must be less than 16.
	 * @return
	 */
	public static BlockState getColouredGlass(int index) {
		return GLASS_BY_DYE.get(index);
	}
	
	private static final Map<Integer, BlockState> GLASS_BY_DYE = Util.make(new HashMap<>(), (p_29841_) -> {
		p_29841_.put(DyeColor.WHITE.getId(), Blocks.WHITE_STAINED_GLASS.defaultBlockState());
		p_29841_.put(DyeColor.ORANGE.getId(), Blocks.ORANGE_STAINED_GLASS.defaultBlockState());
		p_29841_.put(DyeColor.MAGENTA.getId(), Blocks.MAGENTA_STAINED_GLASS.defaultBlockState());
		p_29841_.put(DyeColor.LIGHT_BLUE.getId(), Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState());
		p_29841_.put(DyeColor.YELLOW.getId(), Blocks.YELLOW_STAINED_GLASS.defaultBlockState());
		p_29841_.put(DyeColor.LIME.getId(), Blocks.LIME_STAINED_GLASS.defaultBlockState());
		p_29841_.put(DyeColor.PINK.getId(), Blocks.PINK_STAINED_GLASS.defaultBlockState());
		p_29841_.put(DyeColor.GRAY.getId(), Blocks.GRAY_STAINED_GLASS.defaultBlockState());
		p_29841_.put(DyeColor.LIGHT_GRAY.getId(), Blocks.LIGHT_GRAY_STAINED_GLASS.defaultBlockState());
		p_29841_.put(DyeColor.CYAN.getId(), Blocks.CYAN_STAINED_GLASS.defaultBlockState());
		p_29841_.put(DyeColor.PURPLE.getId(), Blocks.PURPLE_STAINED_GLASS.defaultBlockState());
		p_29841_.put(DyeColor.BLUE.getId(), Blocks.BLUE_STAINED_GLASS.defaultBlockState());
		p_29841_.put(DyeColor.BROWN.getId(), Blocks.BROWN_STAINED_GLASS.defaultBlockState());
		p_29841_.put(DyeColor.GREEN.getId(), Blocks.GREEN_STAINED_GLASS.defaultBlockState());
		p_29841_.put(DyeColor.RED.getId(), Blocks.RED_STAINED_GLASS.defaultBlockState());
		p_29841_.put(DyeColor.BLACK.getId(), Blocks.BLACK_STAINED_GLASS.defaultBlockState());
	});
	
	/**
	 * 
	 * @param index must be less than 16.
	 * @return
	 */
	public static BlockState getColouredWool(int index) {
		return WOOL_BY_DYE.get(index);
	}
	
	private static final Map<Integer, BlockState> WOOL_BY_DYE = Util.make(new HashMap<>(), (p_29841_) -> {
		p_29841_.put(DyeColor.WHITE.getId(), Blocks.WHITE_WOOL.defaultBlockState());
		p_29841_.put(DyeColor.ORANGE.getId(), Blocks.ORANGE_WOOL.defaultBlockState());
		p_29841_.put(DyeColor.MAGENTA.getId(), Blocks.MAGENTA_WOOL.defaultBlockState());
		p_29841_.put(DyeColor.LIGHT_BLUE.getId(), Blocks.LIGHT_BLUE_WOOL.defaultBlockState());
		p_29841_.put(DyeColor.YELLOW.getId(), Blocks.YELLOW_WOOL.defaultBlockState());
		p_29841_.put(DyeColor.LIME.getId(), Blocks.LIME_WOOL.defaultBlockState());
		p_29841_.put(DyeColor.PINK.getId(), Blocks.PINK_WOOL.defaultBlockState());
		p_29841_.put(DyeColor.GRAY.getId(), Blocks.GRAY_WOOL.defaultBlockState());
		p_29841_.put(DyeColor.LIGHT_GRAY.getId(), Blocks.LIGHT_GRAY_WOOL.defaultBlockState());
		p_29841_.put(DyeColor.CYAN.getId(), Blocks.CYAN_WOOL.defaultBlockState());
		p_29841_.put(DyeColor.PURPLE.getId(), Blocks.PURPLE_WOOL.defaultBlockState());
		p_29841_.put(DyeColor.BLUE.getId(), Blocks.BLUE_WOOL.defaultBlockState());
		p_29841_.put(DyeColor.BROWN.getId(), Blocks.BROWN_WOOL.defaultBlockState());
		p_29841_.put(DyeColor.GREEN.getId(), Blocks.GREEN_WOOL.defaultBlockState());
		p_29841_.put(DyeColor.RED.getId(), Blocks.RED_WOOL.defaultBlockState());
		p_29841_.put(DyeColor.BLACK.getId(), Blocks.BLACK_WOOL.defaultBlockState());
	});
	
	public static final Set<ToolAction> toolActionSetOf(ToolAction...actions) {
		return Stream.of(actions).collect(Collectors.toCollection(Sets::newIdentityHashSet));
	}
}
