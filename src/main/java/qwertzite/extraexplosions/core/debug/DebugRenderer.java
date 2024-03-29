package qwertzite.extraexplosions.core.debug;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

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
import qwertzite.extraexplosions.exp.barostrain.BarostrainLevelCache;
import qwertzite.extraexplosions.exp.barostrain.ElementSet;
import qwertzite.extraexplosions.exp.barostrain.FemElement;
import qwertzite.extraexplosions.exp.barostrain.FemNode;
import qwertzite.extraexplosions.exp.barostrain.IntPoint;
import qwertzite.extraexplosions.exp.barostrain.NodeSet;
import qwertzite.extraexplosions.exp.barostrain.PressureRay;

public class DebugRenderer {
	
	private static final Set<PressureRay> RAY_TRIAGONALS = new HashSet<>();
	private static final Set<BlockPos> RENDER_TARGET = new HashSet<>();
	private static NodeSet nodeSet = new NodeSet();
	private static ElementSet elementSet = new ElementSet();
	public static boolean render = true;
	
	public static void clear() {
		synchronized (RAY_TRIAGONALS) {
			RAY_TRIAGONALS.clear();
		}
		synchronized (RENDER_TARGET) {
			RENDER_TARGET.clear();
		}
	}
	
	public static void addRays(Set<PressureRay> rays) {
		synchronized (RAY_TRIAGONALS) {
			RAY_TRIAGONALS.addAll(rays);
		}
	}
	
	public static void addVertexDisplacement(BarostrainLevelCache level$, NodeSet nodes, ElementSet elements) {
		var target = elements.getElements().values().parallelStream()
				.map(FemElement::getPosition)
//				.filter(e -> !level$.wasOriginallyAirAt(e))
				.collect(Collectors.toSet());
		synchronized (RENDER_TARGET) { RENDER_TARGET.addAll(target); }
		DebugRenderer.nodeSet = nodes;
		DebugRenderer.elementSet = elements;
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
		
		bufferbuilder.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
		synchronized (RAY_TRIAGONALS) {
			for (PressureRay ray : RAY_TRIAGONALS) {
				var trigonal = ray.trigonal();
				Vec3 s0 = trigonal.v1().add(trigonal.origin());
				Vec3 s1 = trigonal.v2().add(trigonal.origin());
				Vec3 s2 = trigonal.v3().add(trigonal.origin());
				float alpha = 0.05f;
				bufferbuilder.vertex(mat, (float) s0.x, (float) s0.y, (float) s0.z).color(0.0f, 0.99f, 0.0f, alpha).endVertex();
				bufferbuilder.vertex(mat, (float) s1.x, (float) s1.y, (float) s1.z).color(0.0f, 0.99f, 0.0f, alpha).endVertex();
				bufferbuilder.vertex(mat, (float) s1.x, (float) s1.y, (float) s1.z).color(0.0f, 0.99f, 0.0f, alpha).endVertex();
				bufferbuilder.vertex(mat, (float) s2.x, (float) s2.y, (float) s2.z).color(0.0f, 0.99f, 0.0f, alpha).endVertex();
				bufferbuilder.vertex(mat, (float) s2.x, (float) s2.y, (float) s2.z).color(0.0f, 0.99f, 0.0f, alpha).endVertex();
				bufferbuilder.vertex(mat, (float) s0.x, (float) s0.y, (float) s0.z).color(0.0f, 0.99f, 0.0f, alpha).endVertex();
				if (ray.divStep() == 0) {
					Vec3 from = trigonal.from();
					Vec3 to = trigonal.to();
					bufferbuilder.vertex(mat, (float) from.x, (float) from.y, (float) from.z).color(1.0f, 1.0f, 1.0f, 1).endVertex();
					bufferbuilder.vertex(mat, (float) to.x, (float) to.y, (float) to.z).color(1.0f, 0.5f, 0.5f, 0.0f).endVertex();
				}
			}
		}
		BufferUploader.drawWithShader(bufferbuilder.end());
	}
	
	public void renderVectorMap(Matrix4f mat) {
//		System.out.println(VECTOR_MAP.size());
		if (RENDER_TARGET.isEmpty()) return;
		
		Tesselator tessellator = Tesselator.getInstance();
		BufferBuilder bufferbuilder = tessellator.getBuilder();
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		
		bufferbuilder.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
		final double scale = 1.0d / 4.0d;
		
		synchronized (RENDER_TARGET) {
//			for (var element : this.nodeSet.stream().map(e -> e.getPosition()).collect(Collectors.toSet())) {
			for (var element : RENDER_TARGET) {
				this.renderElement(element, bufferbuilder, mat, 1.0d, e -> Vec3.ZERO, new float[] {1.0f, 1.0f, 1.0f, 0.5f}); // block boundary
//				this.renderElement(element, bufferbuilder, mat, 1.0d/4.0d, e -> e.getExForce(), new float[] {0.0f, 1.0f, 1.0f, 1.0f});
				this.renderElement(element, bufferbuilder, mat, scale, e -> {
					var d = e.getDisp(); 
					var l = 1 / Math.sqrt(d.length());
					if (!Double.isFinite(l)) { l = 0.0d; }
					return new Vec3(d.x * l, d.y*l, d.z * l);}, new float[] {0.f, 0.8f, 0.f, 1.0f});
//				this.renderElement(element, bufferbuilder, mat, 1.0d/4.0d, e -> e.getInternalForce(), new float[] {0.4f, 0.4f, 1.0f, 1.0f});
//				this.renderElement(element, bufferbuilder, mat, 1.0d/4.0d, e -> e.getExForce().subtract(e.getInternalForce()), new float[] {1.0f, 0.2f, 1.0f, 1.0f});
				
				this.renderBody(element, bufferbuilder, mat, scale);
			}
		}
		
		BufferUploader.drawWithShader(bufferbuilder.end());
		
	}
	
	private void renderElement(BlockPos element, BufferBuilder bufferbuilder, Matrix4f mat, double scale, Function<FemNode, Vec3> target, float[] colour) {
		Vec3 vec;
		Vec3 tmp;
		BlockPos pos0;
		BlockPos pos1;
		
		var elem = elementSet.getElementAt(element);
		if (elem.isElasticallyDeforming()) return;
		
		pos0 = element;
		vec = target.apply(nodeSet.getNodeIfExist(pos0));
		pos1 = element.offset(1, 0, 0); // x
		tmp = target.apply(nodeSet.getNodeIfExist(pos1));
		bufferbuilder.vertex(mat, (float) (pos0.getX() + vec.x()*scale),  (float) (pos0.getY() + vec.y()*scale),  (float) (pos0.getZ() + vec.z()*scale)).color(colour[0], colour[1], colour[2], colour[3]).endVertex();
		bufferbuilder.vertex(mat, (float) (pos1.getX() + tmp.x()*scale),  (float) (pos1.getY() + tmp.y()*scale),  (float) (pos1.getZ() + tmp.z()*scale)).color(colour[0], colour[1], colour[2], colour[3]).endVertex();
		
		pos1 = element.offset(0, 1, 0); // y
		tmp = target.apply(nodeSet.getNodeIfExist(pos1));
		bufferbuilder.vertex(mat, (float) (pos0.getX() + vec.x()*scale),  (float) (pos0.getY() + vec.y()*scale),  (float) (pos0.getZ() + vec.z()*scale)).color(colour[0], colour[1], colour[2], colour[3]).endVertex();
		bufferbuilder.vertex(mat, (float) (pos1.getX() + tmp.x()*scale),  (float) (pos1.getY() + tmp.y()*scale),  (float) (pos1.getZ() + tmp.z()*scale)).color(colour[0], colour[1], colour[2], colour[3]).endVertex();
		
		pos1 = element.offset(0, 0, 1); // z
		tmp = target.apply(nodeSet.getNodeIfExist(pos1));
		bufferbuilder.vertex(mat, (float) (pos0.getX() + vec.x()*scale),  (float) (pos0.getY() + vec.y()*scale),  (float) (pos0.getZ() + vec.z()*scale)).color(colour[0], colour[1], colour[2], colour[3]).endVertex();
		bufferbuilder.vertex(mat, (float) (pos1.getX() + tmp.x()*scale),  (float) (pos1.getY() + tmp.y()*scale),  (float) (pos1.getZ() + tmp.z()*scale)).color(colour[0], colour[1], colour[2], colour[3]).endVertex();
		
		pos0 = element.offset(1, 0, 0); // x
		if (!RENDER_TARGET.contains(pos0) || elementSet.getElementAt(pos0).isElasticallyDeforming()) {
			vec = target.apply(nodeSet.getNodeIfExist(pos0));
			pos1 = pos0.offset(0, 1, 0);
			tmp = target.apply(nodeSet.getNodeIfExist(pos1));
			bufferbuilder.vertex(mat, (float) (pos0.getX() + vec.x()*scale),  (float) (pos0.getY() + vec.y()*scale),  (float) (pos0.getZ() + vec.z()*scale)).color(colour[0], colour[1], colour[2], colour[3]).endVertex();
			bufferbuilder.vertex(mat, (float) (pos1.getX() + tmp.x()*scale),  (float) (pos1.getY() + tmp.y()*scale),  (float) (pos1.getZ() + tmp.z()*scale)).color(colour[0], colour[1], colour[2], colour[3]).endVertex();
			
			pos1 = pos0.offset(0, 0, 1);
			tmp = target.apply(nodeSet.getNodeIfExist(pos1));
			bufferbuilder.vertex(mat, (float) (pos0.getX() + vec.x()*scale),  (float) (pos0.getY() + vec.y()*scale),  (float) (pos0.getZ() + vec.z()*scale)).color(colour[0], colour[1], colour[2], colour[3]).endVertex();
			bufferbuilder.vertex(mat, (float) (pos1.getX() + tmp.x()*scale),  (float) (pos1.getY() + tmp.y()*scale),  (float) (pos1.getZ() + tmp.z()*scale)).color(colour[0], colour[1], colour[2], colour[3]).endVertex();
		}
		
		pos0 = element.offset(0, 1, 0); // y
		if (!RENDER_TARGET.contains(pos0) || elementSet.getElementAt(pos0).isElasticallyDeforming()) {
			vec = target.apply(nodeSet.getNodeIfExist(pos0));
			pos1 = pos0.offset(1, 0, 0);
			tmp = target.apply(nodeSet.getNodeIfExist(pos1));
			bufferbuilder.vertex(mat, (float) (pos0.getX() + vec.x()*scale),  (float) (pos0.getY() + vec.y()*scale),  (float) (pos0.getZ() + vec.z()*scale)).color(colour[0], colour[1], colour[2], colour[3]).endVertex();
			bufferbuilder.vertex(mat, (float) (pos1.getX() + tmp.x()*scale),  (float) (pos1.getY() + tmp.y()*scale),  (float) (pos1.getZ() + tmp.z()*scale)).color(colour[0], colour[1], colour[2], colour[3]).endVertex();
			
			pos1 = pos0.offset(0, 0, 1);
			tmp = target.apply(nodeSet.getNodeIfExist(pos1));
			bufferbuilder.vertex(mat, (float) (pos0.getX() + vec.x()*scale),  (float) (pos0.getY() + vec.y()*scale),  (float) (pos0.getZ() + vec.z()*scale)).color(colour[0], colour[1], colour[2], colour[3]).endVertex();
			bufferbuilder.vertex(mat, (float) (pos1.getX() + tmp.x()*scale),  (float) (pos1.getY() + tmp.y()*scale),  (float) (pos1.getZ() + tmp.z()*scale)).color(colour[0], colour[1], colour[2], colour[3]).endVertex();
		}
		
		pos0 = element.offset(0, 0, 1); // z
		if (!RENDER_TARGET.contains(pos0) || elementSet.getElementAt(pos0).isElasticallyDeforming()) {
			vec = target.apply(nodeSet.getNodeIfExist(pos0));
			pos1 = pos0.offset(1, 0, 0);
			tmp = target.apply(nodeSet.getNodeIfExist(pos1));
			bufferbuilder.vertex(mat, (float) (pos0.getX() + vec.x()*scale),  (float) (pos0.getY() + vec.y()*scale),  (float) (pos0.getZ() + vec.z()*scale)).color(colour[0], colour[1], colour[2], colour[3]).endVertex();
			bufferbuilder.vertex(mat, (float) (pos1.getX() + tmp.x()*scale),  (float) (pos1.getY() + tmp.y()*scale),  (float) (pos1.getZ() + tmp.z()*scale)).color(colour[0], colour[1], colour[2], colour[3]).endVertex();
			
			pos1 = pos0.offset(0, 1, 0);
			tmp = target.apply(nodeSet.getNodeIfExist(pos1));
			bufferbuilder.vertex(mat, (float) (pos0.getX() + vec.x()*scale),  (float) (pos0.getY() + vec.y()*scale),  (float) (pos0.getZ() + vec.z()*scale)).color(colour[0], colour[1], colour[2], colour[3]).endVertex();
			bufferbuilder.vertex(mat, (float) (pos1.getX() + tmp.x()*scale),  (float) (pos1.getY() + tmp.y()*scale),  (float) (pos1.getZ() + tmp.z()*scale)).color(colour[0], colour[1], colour[2], colour[3]).endVertex();
		}
		
		pos0 = element.offset(1, 1, 0); // xy
		if (!RENDER_TARGET.contains(pos0) || elementSet.getElementAt(pos0).isElasticallyDeforming()) {
			vec = target.apply(nodeSet.getNodeIfExist(pos0));
			pos1 = pos0.offset(0, 0, 1);
			tmp = target.apply(nodeSet.getNodeIfExist(pos1));
			bufferbuilder.vertex(mat, (float) (pos0.getX() + vec.x()*scale),  (float) (pos0.getY() + vec.y()*scale),  (float) (pos0.getZ() + vec.z()*scale)).color(colour[0], colour[1], colour[2], colour[3]).endVertex();
			bufferbuilder.vertex(mat, (float) (pos1.getX() + tmp.x()*scale),  (float) (pos1.getY() + tmp.y()*scale),  (float) (pos1.getZ() + tmp.z()*scale)).color(colour[0], colour[1], colour[2], colour[3]).endVertex();
		}
		
		pos0 = element.offset(1, 0, 1); // xz
		if (!RENDER_TARGET.contains(pos0) || elementSet.getElementAt(pos0).isElasticallyDeforming()) {
			vec = target.apply(nodeSet.getNodeIfExist(pos0));
			pos1 = pos0.offset(0, 1, 0);
			tmp = target.apply(nodeSet.getNodeIfExist(pos1));
			bufferbuilder.vertex(mat, (float) (pos0.getX() + vec.x()*scale),  (float) (pos0.getY() + vec.y()*scale),  (float) (pos0.getZ() + vec.z()*scale)).color(colour[0], colour[1], colour[2], colour[3]).endVertex();
			bufferbuilder.vertex(mat, (float) (pos1.getX() + tmp.x()*scale),  (float) (pos1.getY() + tmp.y()*scale),  (float) (pos1.getZ() + tmp.z()*scale)).color(colour[0], colour[1], colour[2], colour[3]).endVertex();
		}
		
		pos0 = element.offset(0, 1, 1); // yx
		if (!RENDER_TARGET.contains(pos0) || elementSet.getElementAt(pos0).isElasticallyDeforming()) {
			vec = target.apply(nodeSet.getNodeIfExist(pos0));
			pos1 = pos0.offset(1, 0, 0);
			tmp = target.apply(nodeSet.getNodeIfExist(pos1));
			bufferbuilder.vertex(mat, (float) (pos0.getX() + vec.x()*scale),  (float) (pos0.getY() + vec.y()*scale),  (float) (pos0.getZ() + vec.z()*scale)).color(colour[0], colour[1], colour[2], colour[3]).endVertex();
			bufferbuilder.vertex(mat, (float) (pos1.getX() + tmp.x()*scale),  (float) (pos1.getY() + tmp.y()*scale),  (float) (pos1.getZ() + tmp.z()*scale)).color(colour[0], colour[1], colour[2], colour[3]).endVertex();
		}
	}
	
	private void renderBody(BlockPos pos, BufferBuilder bufferbuilder, Matrix4f mat, double scale) {
		FemElement element = DebugRenderer.elementSet.getElementAt(pos);
		boolean isDeforming = element.isElasticallyDeforming();
		if (!isDeforming) return;
		var colour = isDeforming ? new float[] {1.0f, 0.0f, 0.7f, 1.0f} : new float[] {0.2f, 1.0f, 0.2f, 1.0f};
		double cx = pos.getX() + 0.5d;
		double cy = pos.getY() + 0.5d;
		double cz = pos.getZ() + 0.5d;
		Vec3[] poss = new Vec3[IntPoint.values().length];
		for (int i = 0; i < IntPoint.values().length; i++) {
			var intp = IntPoint.values()[i];
			var offs = element.getDisplacementAt(intp);
//			poss[i] = new Vec3(
//					cx + intp.getXi_i(0)/2.0d + offs[0]*scale,
//					cy + intp.getXi_i(1)/2.0d + offs[1]*scale,
//					cz + intp.getXi_i(2)/2.0d + offs[2]*scale);
			poss[i] = isDeforming ?
					new Vec3(
							cx + intp.getXi_i(0)/2.0d,
							cy + intp.getXi_i(1)/2.0d,
							cz + intp.getXi_i(2)/2.0d) :
					new Vec3(
							cx + intp.getSign_i(0)/2.0d + offs[0]*scale,
							cy + intp.getSign_i(1)/2.0d + offs[1]*scale,
							cz + intp.getSign_i(2)/2.0d + offs[2]*scale);
		}
		
		int i = 0;
		i = 0;
		bufferbuilder.vertex(mat, (float) (poss[i].x()), (float) (poss[i].y()), (float) (poss[i].z())).color(colour[0], colour[1], colour[2], colour[3]).endVertex();
		i = 1;
		bufferbuilder.vertex(mat, (float) (poss[i].x()), (float) (poss[i].y()), (float) (poss[i].z())).color(colour[0], colour[1], colour[2], colour[3]).endVertex();
		
		i = 1;
		bufferbuilder.vertex(mat, (float) (poss[i].x()), (float) (poss[i].y()), (float) (poss[i].z())).color(colour[0], colour[1], colour[2], colour[3]).endVertex();
		i = 5;
		bufferbuilder.vertex(mat, (float) (poss[i].x()), (float) (poss[i].y()), (float) (poss[i].z())).color(colour[0], colour[1], colour[2], colour[3]).endVertex();

		i = 5;
		bufferbuilder.vertex(mat, (float) (poss[i].x()), (float) (poss[i].y()), (float) (poss[i].z())).color(colour[0], colour[1], colour[2], colour[3]).endVertex();
		i = 4;
		bufferbuilder.vertex(mat, (float) (poss[i].x()), (float) (poss[i].y()), (float) (poss[i].z())).color(colour[0], colour[1], colour[2], colour[3]).endVertex();

		i = 4;
		bufferbuilder.vertex(mat, (float) (poss[i].x()), (float) (poss[i].y()), (float) (poss[i].z())).color(colour[0], colour[1], colour[2], colour[3]).endVertex();
		i = 0;
		bufferbuilder.vertex(mat, (float) (poss[i].x()), (float) (poss[i].y()), (float) (poss[i].z())).color(colour[0], colour[1], colour[2], colour[3]).endVertex();

		i = 0;
		bufferbuilder.vertex(mat, (float) (poss[i].x()), (float) (poss[i].y()), (float) (poss[i].z())).color(colour[0], colour[1], colour[2], colour[3]).endVertex();
		i = 2;
		bufferbuilder.vertex(mat, (float) (poss[i].x()), (float) (poss[i].y()), (float) (poss[i].z())).color(colour[0], colour[1], colour[2], colour[3]).endVertex();
		
		i = 1;
		bufferbuilder.vertex(mat, (float) (poss[i].x()), (float) (poss[i].y()), (float) (poss[i].z())).color(colour[0], colour[1], colour[2], colour[3]).endVertex();
		i = 3;
		bufferbuilder.vertex(mat, (float) (poss[i].x()), (float) (poss[i].y()), (float) (poss[i].z())).color(colour[0], colour[1], colour[2], colour[3]).endVertex();

		i = 5;
		bufferbuilder.vertex(mat, (float) (poss[i].x()), (float) (poss[i].y()), (float) (poss[i].z())).color(colour[0], colour[1], colour[2], colour[3]).endVertex();
		i = 7;
		bufferbuilder.vertex(mat, (float) (poss[i].x()), (float) (poss[i].y()), (float) (poss[i].z())).color(colour[0], colour[1], colour[2], colour[3]).endVertex();

		i = 4;
		bufferbuilder.vertex(mat, (float) (poss[i].x()), (float) (poss[i].y()), (float) (poss[i].z())).color(colour[0], colour[1], colour[2], colour[3]).endVertex();
		i = 6;
		bufferbuilder.vertex(mat, (float) (poss[i].x()), (float) (poss[i].y()), (float) (poss[i].z())).color(colour[0], colour[1], colour[2], colour[3]).endVertex();

		i = 2;
		bufferbuilder.vertex(mat, (float) (poss[i].x()), (float) (poss[i].y()), (float) (poss[i].z())).color(colour[0], colour[1], colour[2], colour[3]).endVertex();
		i = 3;
		bufferbuilder.vertex(mat, (float) (poss[i].x()), (float) (poss[i].y()), (float) (poss[i].z())).color(colour[0], colour[1], colour[2], colour[3]).endVertex();

		i = 3;
		bufferbuilder.vertex(mat, (float) (poss[i].x()), (float) (poss[i].y()), (float) (poss[i].z())).color(colour[0], colour[1], colour[2], colour[3]).endVertex();
		i = 7;
		bufferbuilder.vertex(mat, (float) (poss[i].x()), (float) (poss[i].y()), (float) (poss[i].z())).color(colour[0], colour[1], colour[2], colour[3]).endVertex();

		i = 7;
		bufferbuilder.vertex(mat, (float) (poss[i].x()), (float) (poss[i].y()), (float) (poss[i].z())).color(colour[0], colour[1], colour[2], colour[3]).endVertex();
		i = 6;
		bufferbuilder.vertex(mat, (float) (poss[i].x()), (float) (poss[i].y()), (float) (poss[i].z())).color(colour[0], colour[1], colour[2], colour[3]).endVertex();

		i = 6;
		bufferbuilder.vertex(mat, (float) (poss[i].x()), (float) (poss[i].y()), (float) (poss[i].z())).color(colour[0], colour[1], colour[2], colour[3]).endVertex();
		i = 2;
		bufferbuilder.vertex(mat, (float) (poss[i].x()), (float) (poss[i].y()), (float) (poss[i].z())).color(colour[0], colour[1], colour[2], colour[3]).endVertex();
	}
	
	private void renderStrain(BlockPos pos, BufferBuilder bufferbuilder, Matrix4f mat, double scale) {
		FemElement element = DebugRenderer.elementSet.getElementAt(pos);
		var colour = new float[] {0.2f, 1.0f, 0.2f, 1.0f};
		double cx = pos.getX() + 0.5d;
		double cy = pos.getY() + 0.5d;
		double cz = pos.getZ() + 0.5d;
		Vec3[] poss = new Vec3[IntPoint.values().length];
		for (int i = 0; i < IntPoint.values().length; i++) {
			var intp = IntPoint.values()[i];
			var offs = element.getDisplacementAt(intp);
			poss[i] = new Vec3(
					cx + intp.getXi_i(0)/2.0d + offs[0]*scale,
					cy + intp.getXi_i(1)/2.0d + offs[1]*scale,
					cz + intp.getXi_i(2)/2.0d + offs[2]*scale);
		}
		
		int i = 0;
		i = 0;
		bufferbuilder.vertex(mat, (float) (poss[i].x()), (float) (poss[i].y()), (float) (poss[i].z())).color(colour[0], colour[1], colour[2], colour[3]).endVertex();
		i = 1;
		bufferbuilder.vertex(mat, (float) (poss[i].x()), (float) (poss[i].y()), (float) (poss[i].z())).color(colour[0], colour[1], colour[2], colour[3]).endVertex();
	}
	
}
