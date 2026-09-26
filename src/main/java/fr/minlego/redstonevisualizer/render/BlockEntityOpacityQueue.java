package fr.minlego.redstonevisualizer.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.block.BlockState;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.LayeringTransform;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.client.render.block.MovingBlockRenderState;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.command.RenderCommandQueue;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.BlockStateModel;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.Identifier;
import org.joml.Quaternionf;

/** Applies one block entity's opacity to model commands and delegates everything else. */
public final class BlockEntityOpacityQueue implements OrderedRenderCommandQueue {
    private static final String MAIN_TEXTURE = "Sampler0";
    private static final RenderPipeline TRANSLUCENT_CULL_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
                    .withLocation(Identifier.of("redstone_visualizer",
                            "pipeline/block_entity_translucent_cull"))
                    .withShaderDefine("ALPHA_CUTOUT", 0.1F)
                    .withShaderDefine("PER_FACE_LIGHTING")
                    .withSampler("Sampler1")
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .withDepthTestFunction(DepthTestFunction.LESS_DEPTH_TEST)
                    .build());
    private static final RenderPipeline TRANSLUCENT_NO_CULL_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
                    .withLocation(Identifier.of("redstone_visualizer",
                            "pipeline/block_entity_translucent"))
                    .withShaderDefine("ALPHA_CUTOUT", 0.1F)
                    .withShaderDefine("PER_FACE_LIGHTING")
                    .withSampler("Sampler1")
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .withCull(false)
                    .withDepthTestFunction(DepthTestFunction.LESS_DEPTH_TEST)
                    .build());
    private static final Map<RenderLayer, RenderLayer> TRANSLUCENT_LAYERS =
            new IdentityHashMap<>();

    private final OrderedRenderCommandQueue delegate;
    private final int opacity;

    public BlockEntityOpacityQueue(OrderedRenderCommandQueue delegate, int opacity) {
        this.delegate = delegate;
        this.opacity = opacity;
    }

    public static boolean shouldRender(int opacity) {
        return opacity != 0;
    }

    public static boolean shouldWrap(int opacity) {
        return opacity > 0 && opacity < 255;
    }

    public static int applyOpacity(int color, int opacity) {
        return ColorHelper.scaleAlpha(color, opacity / 255.0F);
    }

    public static RenderLayer translucent(RenderLayer layer) {
        var pipeline = layer.getRenderPipeline();
        if (pipeline.getBlendFunction().isPresent()) {
            return layer;
        }
        if (pipeline != RenderPipelines.ENTITY_SOLID
                && pipeline != RenderPipelines.ENTITY_SOLID_OFFSET_FORWARD
                && pipeline != RenderPipelines.ENTITY_CUTOUT
                && pipeline != RenderPipelines.ENTITY_CUTOUT_NO_CULL
                && pipeline != RenderPipelines.ENTITY_CUTOUT_NO_CULL_Z_OFFSET
                && pipeline != RenderPipelines.ENTITY_SMOOTH_CUTOUT) {
            return layer;
        }
        var texture = layer.renderSetup.textures.get(MAIN_TEXTURE);
        if (texture == null) {
            return layer;
        }
        return TRANSLUCENT_LAYERS.computeIfAbsent(layer,
                BlockEntityOpacityQueue::translucentLayer);
    }

    private static RenderLayer translucentLayer(RenderLayer source) {
        var sourcePipeline = source.getRenderPipeline();
        var pipeline = sourcePipeline.isCull()
                ? TRANSLUCENT_CULL_PIPELINE : TRANSLUCENT_NO_CULL_PIPELINE;
        return RenderLayer.of(sourcePipeline.isCull()
                ? "redstone_visualizer_block_entity_translucent_cull"
                : "redstone_visualizer_block_entity_translucent", RenderSetup.builder(pipeline)
                .texture(MAIN_TEXTURE, source.renderSetup.textures.get(MAIN_TEXTURE).location())
                .useLightmap()
                .useOverlay()
                .layeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING_FORWARD)
                .crumbling()
                .translucent()
                .outlineMode(source.getAffectedOutline().isPresent()
                        ? RenderSetup.OutlineMode.AFFECTS_OUTLINE
                        : RenderSetup.OutlineMode.NONE)
                .build());
    }

    @Override
    public RenderCommandQueue getBatchingQueue(int layer) {
        return delegate.getBatchingQueue(layer);
    }

    @Override
    public void submitShadowPieces(MatrixStack matrices, float radius,
            List<EntityRenderState.ShadowPiece> pieces) {
        delegate.submitShadowPieces(matrices, radius, pieces);
    }

    @Override
    public void submitLabel(MatrixStack matrices, Vec3d position, int light,
            Text text, boolean seeThrough, int backgroundColor, double distance,
            CameraRenderState camera) {
        delegate.submitLabel(matrices, position, light, text, seeThrough,
                backgroundColor, distance, camera);
    }

    @Override
    public void submitText(MatrixStack matrices, float x, float y, OrderedText text,
            boolean shadow, TextRenderer.TextLayerType layer, int light,
            int color, int backgroundColor, int outlineColor) {
        delegate.submitText(matrices, x, y, text, shadow, layer, light,
                color, backgroundColor, outlineColor);
    }

    @Override
    public void submitFire(MatrixStack matrices, EntityRenderState state,
            Quaternionf rotation) {
        delegate.submitFire(matrices, state, rotation);
    }

    @Override
    public void submitLeash(MatrixStack matrices, EntityRenderState.LeashData leash) {
        delegate.submitLeash(matrices, leash);
    }

    @Override
    public <S> void submitModel(Model<? super S> model, S state,
            MatrixStack matrices, RenderLayer layer, int light, int overlay,
            int color, Sprite sprite, int outlineColor,
            ModelCommandRenderer.CrumblingOverlayCommand crumblingOverlay) {
        delegate.submitModel(model, state, matrices, translucent(layer), light,
                overlay, applyOpacity(color, opacity), sprite, outlineColor,
                crumblingOverlay);
    }

    @Override
    public void submitModelPart(ModelPart modelPart, MatrixStack matrices,
            RenderLayer layer, int light, int overlay, Sprite sprite,
            boolean hasFoil, boolean glint, int color,
            ModelCommandRenderer.CrumblingOverlayCommand crumblingOverlay,
            int outlineColor) {
        delegate.submitModelPart(modelPart, matrices, layer, light, overlay, sprite,
                hasFoil, glint, color, crumblingOverlay, outlineColor);
    }

    @Override
    public void submitBlock(MatrixStack matrices, BlockState state, int light,
            int overlay, int color) {
        delegate.submitBlock(matrices, state, light, overlay, color);
    }

    @Override
    public void submitMovingBlock(MatrixStack matrices, MovingBlockRenderState state) {
        delegate.submitMovingBlock(matrices, state);
    }

    @Override
    public void submitBlockStateModel(MatrixStack matrices, RenderLayer layer,
            BlockStateModel model, float red, float green, float blue,
            int light, int overlay, int color) {
        delegate.submitBlockStateModel(matrices, layer, model, red, green, blue,
                light, overlay, color);
    }

    @Override
    public void submitItem(MatrixStack matrices, ItemDisplayContext displayContext,
            int light, int overlay, int color, int[] tints, List<BakedQuad> quads,
            RenderLayer layer, ItemRenderState.Glint glint) {
        delegate.submitItem(matrices, displayContext, light, overlay, color, tints,
                quads, layer, glint);
    }

    @Override
    public void submitCustom(MatrixStack matrices, RenderLayer layer, Custom command) {
        delegate.submitCustom(matrices, layer, command);
    }

    @Override
    public void submitCustom(LayeredCustom command) {
        delegate.submitCustom(command);
    }
}
