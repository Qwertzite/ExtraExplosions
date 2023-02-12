package qwertzite.extraexplosions;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import qwertzite.extraexplosions.core.BootstrapClientSide;
import qwertzite.extraexplosions.core.BootstrapCommon;
import qwertzite.extraexplosions.core.BootstrapServerSide;
import qwertzite.extraexplosions.core.command.CommandRegister;
import qwertzite.extraexplosions.core.network.ModNetwork;

@Mod(ExtraExplosionCore.MODID)
public class ExtraExplosionCore {
	public static final String MODID = "extraexplosions";
	public static final String VERSION = "0.0.0-alpha";
	public static final String LOG_BASE_NAME ="EE";
	
	public static ExtraExplosionCore INSTANCE;
	private final BootstrapCommon bootstrap;
	
	public ExtraExplosionCore() {
		if (INSTANCE != null) throw new IllegalStateException();
		INSTANCE = this;
		bootstrap = DistExecutor.safeRunForDist(
				() -> BootstrapClientSide::new,
				() -> BootstrapServerSide::new);
		
		ModNetwork.init(MODID);
		
		MinecraftForge.EVENT_BUS.addListener(this::onServerStarting);
		
		IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
		bootstrap.init(bus);
	}
	
	public void onServerStarting(RegisterCommandsEvent event) {
		CommandRegister.onRegisterCommand(event);
	}
}
