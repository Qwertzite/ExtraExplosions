package qwertzite.extraexplosions.core.command;

import com.google.gson.JsonObject;

import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.network.FriendlyByteBuf;

public class EeStringArgumentTypeInfo implements ArgumentTypeInfo<EeStringArgument, EeStringArgumentTypeInfo.Template> {

	
	@Override
	public void serializeToNetwork(Template pTemplate, FriendlyByteBuf pBuffer) {}

	@Override
	public Template deserializeFromNetwork(FriendlyByteBuf pBuffer) {
		return new Template();
	}

	@Override
	public void serializeToJson(Template pTemplate, JsonObject pJson) {}

	@Override
	public Template unpack(EeStringArgument pArgument) {
		return new Template();
	}

	public final class Template implements ArgumentTypeInfo.Template<EeStringArgument> {
		
		Template() {}
		
		@Override
		public EeStringArgument instantiate(CommandBuildContext pContext) {
			return new EeStringArgument();
		}
		
		@Override
		public ArgumentTypeInfo<EeStringArgument, ?> type() {
			return EeStringArgumentTypeInfo.this;
		}
	}
}
