package qwertzite.extraexplosions.core.debug;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import qwertzite.extraexplosions.exmath.RayTrigonal;

public class DebugRenderer {
	
	private static final Set<RayTrigonal> RAY_TRIAGONALS = new HashSet<>();
	private static final Map<BlockPos, Vec3> VECTOR_MAP = new HashMap<>();
	public static boolean render = true;
	
	public static void clear() {
		synchronized (RAY_TRIAGONALS) {
			RAY_TRIAGONALS.clear();
			VECTOR_MAP.clear();
		}
	}
	
	public static void addRays(Set<RayTrigonal> rays) {
		synchronized (RAY_TRIAGONALS) {
//			RAY_TRIAGONALS.addAll(rays);
		}
	}
	
	public static void addVertexDisplacement(Map<BlockPos, Vec3> displacement) {
		synchronized (VECTOR_MAP) {
			VECTOR_MAP.putAll(displacement);
		}
	}
	
	@SubscribeEvent
	public void onRenderEvent(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_CUTOUT_MIPPED_BLOCKS_BLOCKS) return;
		if (!render) return;
		
		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
		RenderSystem.disableTexture();
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.enableDepthTest();
//		RenderSystem.enableRescaleNormal();
//		RenderSystem.enableCull();
		
		PoseStack pose = event.getPoseStack();
		pose.pushPose();
		@SuppressWarnings("resource")
		Vec3 cameraPos = Minecraft.getInstance().getEntityRenderDispatcher().camera.getPosition();
		pose.translate(-cameraPos.x(), -cameraPos.y(), -cameraPos.z());
		
		var mat = pose.last().pose();
		this.renderRayTrigonals(mat);
		this.renderVectorMap(mat);
		
		
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
	
	private void renderRayTrigonals(Matrix4f mat) {
		if (RAY_TRIAGONALS.isEmpty()) return; 
		
		Tesselator tessellator = Tesselator.getInstance();
		BufferBuilder bufferbuilder = tessellator.getBuilder();
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		
//		bufferbuilder.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
//		synchronized (RAY_TRIAGONALS) {
//			for (RayTrigonal ray : RAY_TRIAGONALS) {
//				Vec3 from = ray.from();
//				Vec3 to = ray.to();
//				bufferbuilder.vertex(mat, (float) from.x, (float) from.y, (float) from.z).color(1.0f, 0.0f, 0.0f, 0.1f).endVertex();
//				bufferbuilder.vertex(mat, (float)   to.x, (float)   to.y, (float)   to.z).color(1.0f, 0.0f, 0.0f, 0.2f).endVertex();
//			}
//		}
//		BufferUploader.drawWithShader(bufferbuilder.end());
		
		bufferbuilder.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
		synchronized (RAY_TRIAGONALS) {
			for (RayTrigonal ray : RAY_TRIAGONALS) {
				Vec3 s0 = ray.v1().add(ray.origin());
				Vec3 s1 = ray.v2().add(ray.origin());
				Vec3 s2 = ray.v3().add(ray.origin());
				bufferbuilder.vertex(mat, (float) s0.x, (float) s0.y, (float) s0.z).color(0.0f, 1.0f, 0.0f, 0.2f).endVertex();
				bufferbuilder.vertex(mat, (float) s1.x, (float) s1.y, (float) s1.z).color(0.0f, 1.0f, 0.0f, 0.2f).endVertex();
				bufferbuilder.vertex(mat, (float) s1.x, (float) s1.y, (float) s1.z).color(0.0f, 1.0f, 0.0f, 0.2f).endVertex();
				bufferbuilder.vertex(mat, (float) s2.x, (float) s2.y, (float) s2.z).color(0.0f, 1.0f, 0.0f, 0.2f).endVertex();
				bufferbuilder.vertex(mat, (float) s2.x, (float) s2.y, (float) s2.z).color(0.0f, 1.0f, 0.0f, 0.2f).endVertex();
				bufferbuilder.vertex(mat, (float) s0.x, (float) s0.y, (float) s0.z).color(0.0f, 1.0f, 0.0f, 0.2f).endVertex();
			}
		}
		BufferUploader.drawWithShader(bufferbuilder.end());
		
	}
	
	public void renderVectorMap(Matrix4f mat) {
//		System.out.println(VECTOR_MAP.size());
		if (VECTOR_MAP.isEmpty()) return;
		
		Tesselator tessellator = Tesselator.getInstance();
		BufferBuilder bufferbuilder = tessellator.getBuilder();
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		
		bufferbuilder.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
		synchronized (VECTOR_MAP) {
			for (Map.Entry<BlockPos, Vec3> entry : VECTOR_MAP.entrySet()) {
				var blockPos = entry.getKey();
				var vec3 = entry.getValue();
//				System.out.println(blockPos);
				final double scale = -1.0d / 1000.0d;
				bufferbuilder.vertex(mat, (float)  blockPos.getX()          , (float)  blockPos.getY()          , (float)  blockPos.getZ())
						.color(1.0f, 0.2f, 1.0f, 1.0f).endVertex();
				bufferbuilder.vertex(mat,
						(float) (blockPos.getX()+vec3.x()*scale),
						(float) (blockPos.getY()+vec3.y()*scale),
						(float) (blockPos.getZ()+vec3.z()*scale))
						.color(1.0f, 0.2f, 1.0f, 1.0f).endVertex();
			}
		}
		BufferUploader.drawWithShader(bufferbuilder.end());
		
	}
	
}
