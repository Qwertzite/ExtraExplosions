package qwertzite.extraexplosions.core.command;

import java.util.function.BiFunction;
import java.util.function.Function;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public class CommandArgument<T> {
	
	protected final String name;
	protected final String shortName;
	protected final String typeName;
	protected String description = "Documentation not available";
	
	protected final Function<CommandBuildContext, ArgumentType<?>> type;
	
	protected final BiFunction<CommandContext<CommandSourceStack>, String, T> argParser;
	protected Function<CommandContext<CommandSourceStack>, T> defaultValueProvider;
	
	protected T value;
	
	public static CommandArgument<Vec3> coord(String name) {
		return new CommandArgument<>(name, "position",
				(cbc) -> Vec3Argument.vec3(),
				(t,u) -> {
					return Vec3Argument.getVec3(t, u);
				});
	}
	
	public static CommandArgument<BlockPos> blockPos(String name) {
		return new CommandArgument<>(name, "block pos",
				(cbc) -> BlockPosArgument.blockPos(),
				(t, u) -> {
					try {
						return BlockPosArgument.getLoadedBlockPos(t, u);
					} catch (CommandSyntaxException e) {
						throw new RuntimeException(e);
					}
				});
	}
	
	public static CommandArgument<Boolean> flag(String name) {
		return new CommandArgument<>(name, "flag", null, (ctx, n) -> true)
				.setDefaultValue(ctx -> false);
	}
	
	public static CommandArgument<Long> longArg(String name) {
		return new CommandArgument<>(name, "long", (cbc) -> LongArgumentType.longArg(),
				LongArgumentType::getLong);
	}
	
	public static CommandArgument<Float> floatArg(String name, float minValue) {
		return new CommandArgument<>(name, "float", (cbc) -> FloatArgumentType.floatArg(minValue),
				FloatArgumentType::getFloat);
	}
	
	public static CommandArgument<BlockInput> blockType(String name) {
		return new CommandArgument<>(name, "block state", (cbc) -> BlockStateArgument.block(cbc),
				BlockStateArgument::getBlock);
	}
	
	public static CommandArgument<String> string(String name) {
		return new CommandArgument<>(name, "string", (cbc) -> EeStringArgument.string(),
				EeStringArgument::getString);
	}
	
	public CommandArgument(String name, String typeName, Function<CommandBuildContext, ArgumentType<?>> type, BiFunction<CommandContext<CommandSourceStack>, String, T> argParser) {
		this(null, name, typeName, type, argParser);
	}
	
	public CommandArgument(String shortName, String name, String typeName, Function<CommandBuildContext, ArgumentType<?>> type, BiFunction<CommandContext<CommandSourceStack>, String, T> argParser) {
		this.name = name;
		this.shortName = shortName;
		this.typeName = typeName;
		this.type = type;
		this.argParser = argParser;
	}
	
	public CommandArgument<T> setDefaultValue(Function<CommandContext<CommandSourceStack>, T> defaultValueProvider) {
		this.defaultValueProvider = defaultValueProvider;
		return this;
	}
	
	public CommandArgument<T> setDescription(String description) {
		this.description = description;
		return this;
	}
	
	public void clear() { this.value = null; }
	public void acceptValue(CommandContext<CommandSourceStack> ctx) {
		this.value = argParser.apply(ctx, this.getName());
	}
	public void applyDefault(CommandContext<CommandSourceStack> ctx) {
		if (this.hasDefaultValue()) this.value = this.defaultValueProvider.apply(ctx);
	}
	
	public T getValue() { return this.value; }
	public boolean hasValue() { return this.value != null; }
	
	public String getLongName() { return "--" + name; }
	public boolean hasShortName() { return this.shortName != null; }
	public String getShortName() { assert(shortName != null); return "-" + shortName; }
	public String getName() { return name; }
	public boolean hasVariable() { return this.type != null; }
	public ArgumentType<?> getType(CommandBuildContext cbc) { return type.apply(cbc); }
	public boolean hasDefaultValue() { return this.defaultValueProvider != null; }
	
	public String getTypeName() { return this.typeName; }
	public String getDescription() { return this.description; }
}
