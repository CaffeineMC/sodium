package net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline;

import net.caffeinemc.mods.sodium.api.util.ColorARGB;
import net.caffeinemc.mods.sodium.api.util.ColorMixer;
import net.caffeinemc.mods.sodium.client.model.color.ColorProvider;
import net.caffeinemc.mods.sodium.client.model.color.ColorProviderRegistry;
import net.caffeinemc.mods.sodium.client.model.light.LightMode;
import net.caffeinemc.mods.sodium.client.model.light.LightPipelineProvider;
import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import net.caffeinemc.mods.sodium.client.model.quad.properties.ModelQuadOrientation;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuilder;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.DefaultMaterials;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.Material;
import net.caffeinemc.mods.sodium.client.render.chunk.translucent_sorting.TranslucentGeometryCollector;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.builder.ChunkMeshBufferBuilder;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import net.caffeinemc.mods.sodium.client.render.model.MutableQuadViewImpl;
import net.caffeinemc.mods.sodium.client.render.model.AbstractBlockRenderContext;
import net.caffeinemc.mods.sodium.client.render.model.SodiumShadeMode;
import net.caffeinemc.mods.sodium.client.render.texture.SpriteFinderCache;
import net.caffeinemc.mods.sodium.client.services.PlatformModelEmitter;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.util.TriState;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.joml.Vector3f;

public class BlockRenderer extends AbstractBlockRenderContext {
    private final ColorProviderRegistry colorProviderRegistry;
    private final int[] vertexColors = new int[4];
    private final ChunkVertexEncoder.Vertex[] vertices = ChunkVertexEncoder.Vertex.uninitializedQuad();

    private ChunkBuildBuffers buffers;

    private final Vector3f posOffset = new Vector3f();
    private final BlockPos.MutableBlockPos scratchPos = new BlockPos.MutableBlockPos();
    @Nullable
    private ColorProvider<BlockState> colorProvider;
    private TranslucentGeometryCollector collector;
    private boolean cutoutLeaves;

    public BlockRenderer(ColorProviderRegistry colorRegistry, LightPipelineProvider lighters) {
        this.colorProviderRegistry = colorRegistry;
        this.lighters = lighters;

        this.random = new SingleThreadedRandomSource(42L);
        this.cutoutLeaves = Minecraft.getInstance().options.cutoutLeaves().get();
    }

    public void prepare(ChunkBuildBuffers buffers, LevelSlice level, TranslucentGeometryCollector collector) {
        this.buffers = buffers;
        this.level = level;
        this.collector = collector;
        this.slice = level;
    }

    public void release() {
        this.buffers = null;
        this.level = null;
        this.collector = null;
        this.slice = null;
    }

    public void renderModel(BlockStateModel model, BlockState state, BlockPos pos, BlockPos origin) {
        this.state = state;
        this.pos = pos;

        this.prepareAoInfo(true);


        this.posOffset.set(origin.getX(), origin.getY(), origin.getZ());
        if (state.hasOffsetFunction()) {
            Vec3 modelOffset = state.getOffset(pos);
            this.posOffset.add((float) modelOffset.x, (float) modelOffset.y, (float) modelOffset.z);
        }

        this.colorProvider = this.colorProviderRegistry.getColorProvider(state.getBlock());

        this.prepareCulling(true);

        random.setSeed(state.getSeed(pos));

        this.forceOpaque = ModelBlockRenderer.forceOpaque(this.cutoutLeaves, state);

        PlatformModelEmitter.getInstance().emitModel(model, this::isFaceCulled, getForEmitting(), random, level, pos, state, this::bufferDefaultModel);

        this.forceOpaque = false;
    }

    /**
     * Process quad, after quad transforms and the culling check have been applied.
     */
    @Override
    protected void processQuad(MutableQuadViewImpl quad) {
        final TriState aoMode = quad.ambientOcclusion();
        final SodiumShadeMode shadeMode = quad.getShadeMode();
        final LightMode lightMode;
        if (aoMode == TriState.DEFAULT) {
            lightMode = this.defaultLightMode;
        } else {
            lightMode = this.useAmbientOcclusion && aoMode != TriState.FALSE ? LightMode.SMOOTH : LightMode.FLAT;
        }
        final boolean emissive = quad.emissive();

        final ChunkSectionLayer blendMode = quad.getRenderType();
        final Material material = DefaultMaterials.forChunkLayer(forceOpaque ? ChunkSectionLayer.SOLID : blendMode);

        this.tintQuad(quad);
        this.shadeQuad(quad, lightMode, emissive, shadeMode);
        this.bufferQuad(quad, this.quadLightData.br, material);
    }

    private void tintQuad(MutableQuadViewImpl quad) {
        int tintIndex = quad.getTintIndex();

        if (tintIndex != -1) {
            ColorProvider<BlockState> colorProvider = this.colorProvider;

            if (colorProvider != null) {
                int[] vertexColors = this.vertexColors;
                colorProvider.getColors(this.slice, this.pos, this.scratchPos, this.state, quad, vertexColors, slice.hasBiomeBlend());

                for (int i = 0; i < 4; i++) {
                    quad.setColor(i, ColorMixer.mulComponentWise(vertexColors[i], quad.baseColor(i)));
                }
            }
        }
    }

    private void bufferQuad(MutableQuadViewImpl quad, float[] brightnesses, Material material) {
        // TODO: Find a way to reimplement quad reorientation
        ModelQuadOrientation orientation = ModelQuadOrientation.NORMAL;
        ChunkVertexEncoder.Vertex[] vertices = this.vertices;
        Vector3f offset = this.posOffset;

        for (int dstIndex = 0; dstIndex < 4; dstIndex++) {
            int srcIndex = orientation.getVertexIndex(dstIndex);

            ChunkVertexEncoder.Vertex out = vertices[dstIndex];
            out.x = quad.getX(srcIndex) + offset.x;
            out.y = quad.getY(srcIndex) + offset.y;
            out.z = quad.getZ(srcIndex) + offset.z;

            // FRAPI uses ARGB color format; convert to ABGR.
            out.color = ColorARGB.toABGR(quad.baseColor(srcIndex));
            out.ao = brightnesses[srcIndex];

            out.u = quad.getTexU(srcIndex);
            out.v = quad.getTexV(srcIndex);

            out.light = quad.getLight(srcIndex);
        }

        var atlasSprite = quad.sprite(SpriteFinderCache.forBlockAtlas());
        var materialBits = material.bits();
        ModelQuadFacing normalFace = quad.normalFace();

        var pass = material.pass;

        // collect all translucent quads into the translucency sorting system if enabled,
        // and discard the quad if it's invalid (i.e. not visible)
        if (pass.isTranslucent() && this.collector != null &&
                this.collector.appendQuad(vertices, normalFace, quad.getFaceNormal())) {
            return;
        }

        ChunkModelBuilder builder = this.buffers.get(pass);
        ChunkMeshBufferBuilder vertexBuffer = builder.getVertexBuffer(normalFace);
        vertexBuffer.push(vertices, materialBits);

        if (atlasSprite != null) {
            builder.addSprite(atlasSprite);
        }
    }
}