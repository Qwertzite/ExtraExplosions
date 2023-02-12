package qwertzite.extraexplosions.core.network;

import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent.Context;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import qwertzite.extraexplosions.core.ModLog;

public class ModNetwork {
	private static final String PROTOCOL_VERSION = "1";
	private static SimpleChannel networkHandler;
	private static int id;
	
	public static void init(String modid) {
		networkHandler = NetworkRegistry.newSimpleChannel(new ResourceLocation(modid, "main"),
				() -> PROTOCOL_VERSION,
				PROTOCOL_VERSION::equals,
				PROTOCOL_VERSION::equals);
	}
	
	public static SimpleChannel getNetworkHandler() {
		return networkHandler;
	}
	
	public static void sendToServer(AbstractPacket message) {
		networkHandler.sendToServer(message);
	}
	
	public static void sendTo(ServerPlayer player, AbstractPacket message) {
		ModNetwork.getNetworkHandler().send(PacketDistributor.PLAYER.with(() -> player), message);
	}
	
	public static <T extends AbstractPacket> void registerPacket(Class<T> packetClass) {
		if (PacketToServer.class.isAssignableFrom(packetClass)) {
			networkHandler.registerMessage(id, packetClass,
					(msg, buf) -> ((AbstractPacket) msg).encode(buf),
					buf -> decoder(packetClass, buf),
					(msg, buf) -> handlePacket(msg, buf.get()), Optional.of(NetworkDirection.PLAY_TO_SERVER));
			ModLog.info("Registered Packet to server, %s with ID %s", packetClass.getName(), id);
			id++;
		}
		if (PacketToClient.class.isAssignableFrom(packetClass)) {
			networkHandler.registerMessage(id, packetClass,
					(msg, buf) -> ((AbstractPacket) msg).encode(buf),
					buf -> decoder(packetClass, buf),
					(msg, buf) -> handlePacket(msg, buf.get()), Optional.of(NetworkDirection.PLAY_TO_CLIENT));
			ModLog.info("Registered Packet to client, %s with ID %s", packetClass.getName(), id);
			id++;
		}
	}
	
	private static <T extends AbstractPacket> T decoder(Class<T> packetClass, FriendlyByteBuf buf) {
		T message;
		try {
			message = packetClass.getConstructor().newInstance();
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			CrashReport crashreport = CrashReport.forThrowable(e, "Instanciating packet.");
			CrashReportCategory crashreportcategory = crashreport.addCategory("Instanciating recieved packet");
			crashreportcategory.setDetail("Packet Class", packetClass.getCanonicalName());
			throw new ReportedException(crashreport);
		}
		message.decode(buf);
		return message;
	}
	
	private static <T extends AbstractPacket> void handlePacket(T packet, Context ctx) {
		packet.onMessage(packet, ctx);
	}
}
