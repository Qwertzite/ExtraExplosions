package qwertzite.extraexplosions.core.debug;

import java.util.HashSet;
import java.util.Set;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Matrix4f;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import qwertzite.extraexplosions.exp.spherical.RayTrigonal;

public class DebugRenderer {
	
	private static final Set<RayTrigonal> RAY_TRIAGONALS = new HashSet<>();
	
	public static void clearRays() {
		synchronized (RAY_TRIAGONALS) {
			RAY_TRIAGONALS.clear();
		}
	}
	
	public static void addRays(Set<RayTrigonal> rays) {
		synchronized (RAY_TRIAGONALS) {
			RAY_TRIAGONALS.addAll(rays);
		}
	}
	
	@SubscribeEvent
	public void onRenderEven(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS) return;
		
		PoseStack pose = event.getPoseStack();
		pose.pushPose();
		@SuppressWarnings("resource")
		Vec3 cameraPos = Minecraft.getInstance().getEntityRenderDispatcher().camera.getPosition();
		pose.translate(-cameraPos.x(), -cameraPos.y(), -cameraPos.z());
		
		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
		RenderSystem.disableTexture();
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.enableDepthTest();
//		RenderSystem.enableRescaleNormal();
//		RenderSystem.enableCull();
		
		Matrix4f mat = pose.last().pose();
		Tesselator tessellator = Tesselator.getInstance();
		BufferBuilder bufferbuilder = tessellator.getBuilder();
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		
		bufferbuilder.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
		synchronized (RAY_TRIAGONALS) {
			for (RayTrigonal ray : RAY_TRIAGONALS) {
				Vec3 from = ray.from();
				Vec3 to = ray.to();
				bufferbuilder.vertex(mat, (float) from.x, (float) from.y, (float) from.z).color(1.0f, 0.0f, 0.0f, 0.333f).endVertex();
				bufferbuilder.vertex(mat, (float)   to.x, (float)   to.y, (float)   to.z).color(1.0f, 0.0f, 0.0f, 1.0f).endVertex();
			}
		}
		BufferUploader.drawWithShader(bufferbuilder.end());
		
		bufferbuilder.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
		synchronized (RAY_TRIAGONALS) {
			for (RayTrigonal ray : RAY_TRIAGONALS) {
				Vec3 s0 = ray.v1().add(ray.origin());
				Vec3 s1 = ray.v2().add(ray.origin());
				Vec3 s2 = ray.v3().add(ray.origin());
				bufferbuilder.vertex(mat, (float) s0.x, (float) s0.y, (float) s0.z).color(0.0f, 1.0f, 0.0f, 1.0f).endVertex();
				bufferbuilder.vertex(mat, (float) s1.x, (float) s1.y, (float) s1.z).color(0.0f, 1.0f, 0.0f, 1.0f).endVertex();
				bufferbuilder.vertex(mat, (float) s1.x, (float) s1.y, (float) s1.z).color(0.0f, 1.0f, 0.0f, 1.0f).endVertex();
				bufferbuilder.vertex(mat, (float) s2.x, (float) s2.y, (float) s2.z).color(0.0f, 1.0f, 0.0f, 1.0f).endVertex();
				bufferbuilder.vertex(mat, (float) s2.x, (float) s2.y, (float) s2.z).color(0.0f, 1.0f, 0.0f, 1.0f).endVertex();
				bufferbuilder.vertex(mat, (float) s0.x, (float) s0.y, (float) s0.z).color(0.0f, 1.0f, 0.0f, 1.0f).endVertex();
			}
		}
		BufferUploader.drawWithShader(bufferbuilder.end());
		
		pose.popPose();
		
		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
//		RenderSystem.disableAlpha();
		RenderSystem.enableDepthTest();
//		RenderSystem.lightenableLighting();
		RenderSystem.disableBlend();
		RenderSystem.defaultBlendFunc();
//		RenderSystem.disableFog();
		RenderSystem.enableTexture();
	}
	
}
