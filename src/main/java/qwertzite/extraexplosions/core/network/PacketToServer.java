package qwertzite.extraexplosions.core.network;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent.Context;

public interface PacketToServer {
	public AbstractPacket handleServerSide(Player player, Context ctx);
}
