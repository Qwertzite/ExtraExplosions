package qwertzite.extraexplosions.core.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

public abstract class AbstractPacket {
	
	public abstract void encode(FriendlyByteBuf byteBuf);
	public abstract void decode(FriendlyByteBuf byteBuf);
	
	public void onMessage(AbstractPacket message, Context ctx) {
		switch (ctx.getDirection()) {
		case LOGIN_TO_CLIENT:
		case PLAY_TO_CLIENT:
			if (message instanceof PacketToClient) {
				Player player = this.getPlayer();
				if (player == null) break;
				if (this.synchExec()) ctx.enqueueWork(() ->
						// Make sure it's only executed on the physical client
						DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> this.handleClient(player, ctx, message)));
				else this.handleClient(player, ctx, message);
				ctx.setPacketHandled(true);
			}
			break;
		case LOGIN_TO_SERVER:
		case PLAY_TO_SERVER:
			if (message instanceof PacketToServer) {
				ServerPlayer player = ctx.getSender();
				if (this.synchExec()) ctx.enqueueWork(() -> this.handleServer(player, ctx, message));
				else this.handleServer(player, ctx, message);
				ctx.setPacketHandled(true);
			}
			break;
		default:
		}
	}
	
	private void handleClient(Player player, Context ctx, AbstractPacket message) {
		AbstractPacket reply = ((PacketToClient) message).handleClientSide(player, ctx);
		if (reply != null) ModNetwork.sendToServer(reply);
	}
	private void handleServer(ServerPlayer player, Context ctx, AbstractPacket message) {
		AbstractPacket reply = ((PacketToServer) message).handleServerSide(player, ctx);
		if (reply != null) ModNetwork.sendTo(player, message);
	}
	
	@SuppressWarnings("resource")
	@OnlyIn(Dist.CLIENT)
	private Player getPlayer() {
		return Minecraft.getInstance().player;
	}
	
	protected boolean synchExec() {
		return true;
	}
}
