package qwertzite.extraexplosions.core.network;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent.Context;

public interface PacketToClient {
	public AbstractPacket handleClientSide(Player player, Context ctx);

}
