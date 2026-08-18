package io.github.coolmineman.bitsandchisels.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.coolmineman.bitsandchisels.BitsBlockEntity;
import net.fabricmc.fabric.api.client.renderer.v1.Renderer;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.client.renderer.v1.model.FabricBlockStateModel;
import net.fabricmc.fabric.api.client.renderer.v1.render.FabricBlockModelRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Renders the 16^3 voxel grid as clipped faces of the original block models.
 *
 * The important part here is that source block UVs remain in full-block space.
 * A one-bit hole therefore removes one sixteenth of the original face texture
 * instead of rendering a miniature copy of the entire block texture.
 */
public final class BitsBlockEntityRenderer implements BlockEntityRenderer<BitsBlockEntity, BitsBlockRenderState> {
    private static final Direction[] X_DIRECTIONS = { Direction.EAST, Direction.WEST };
    private static final Direction[] Y_DIRECTIONS = { Direction.UP, Direction.DOWN };
    private static final Direction[] Z_DIRECTIONS = { Direction.SOUTH, Direction.NORTH };
    private static final float PIXEL = 1.0f / BitsBlockEntity.SIZE;

    public BitsBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public BitsBlockRenderState createRenderState() {
        return new BitsBlockRenderState();
    }

    @Override
    public void extractRenderState(BitsBlockEntity blockEntity, BitsBlockRenderState state, float tickProgress, Vec3 cameraPos, @Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, tickProgress, cameraPos, crumblingOverlay);

        long revision = blockEntity.renderRevision();
        if (state.meshRevision == revision) return;

        buildMesh(blockEntity, state);
        state.meshRevision = revision;
    }

    private static void buildMesh(BitsBlockEntity blockEntity, BitsBlockRenderState renderState) {
        BlockState[] bits = blockEntity.copyBits();
        Map<BlockState, Boolean> translucency = new IdentityHashMap<>();
        boolean hasTranslucency = false;

        for (BlockState bit : bits) {
            if (!bit.isAir() && isTranslucent(bit, translucency)) {
                hasTranslucency = true;
                break;
            }
        }

        renderState.model.clear();
        QuadEmitter output = ((FabricBlockModelRenderState) (Object) renderState.model)
            .setupMesh(new Matrix4f(), hasTranslucency);

        Level level = blockEntity.getLevel();
        BlockAndTintGetter view = level == null ? BlockAndTintGetter.EMPTY : level;
        BlockPos pos = blockEntity.getBlockPos();

        emitGreedyFaces(bits, output, view, pos, translucency);
    }

    private static void emitGreedyFaces(BlockState[] bits, QuadEmitter output, BlockAndTintGetter view, BlockPos pos, Map<BlockState, Boolean> translucency) {
        boolean[][] used = new boolean[BitsBlockEntity.SIZE][BitsBlockEntity.SIZE];

        // Fixed X plane, merge along Y/Z.
        for (Direction face : X_DIRECTIONS) {
            for (int x = 0; x < 16; x++) {
                clear(used);
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        BlockState source = bit(bits, x, y, z);
                        if (source.isAir() || used[y][z] || !faceNeeded(bits, face, x, y, z, translucency)) continue;

                        int maxY = y;
                        int maxZ = z;
                        for (int ty = y; ty < 16; ty++) {
                            if (bit(bits, x, ty, z) == source && !used[ty][z] && faceNeeded(bits, face, x, ty, z, translucency)) {
                                maxY = ty;
                            } else break;
                        }

                        zLoop:
                        for (int tz = z; tz < 16; tz++) {
                            for (int ty = y; ty <= maxY; ty++) {
                                if (bit(bits, x, ty, tz) != source || used[ty][tz] || !faceNeeded(bits, face, x, ty, tz, translucency)) {
                                    break zLoop;
                                }
                            }
                            maxZ = tz;
                        }

                        for (int yy = y; yy <= maxY; yy++) {
                            for (int zz = z; zz <= maxZ; zz++) used[yy][zz] = true;
                        }

                        emitRegion(output, view, pos, source, face, x, y, z, x, maxY, maxZ);
                    }
                }
            }
        }

        // Fixed Y plane, merge along X/Z.
        for (Direction face : Y_DIRECTIONS) {
            for (int y = 0; y < 16; y++) {
                clear(used);
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        BlockState source = bit(bits, x, y, z);
                        if (source.isAir() || used[x][z] || !faceNeeded(bits, face, x, y, z, translucency)) continue;

                        int maxX = x;
                        int maxZ = z;
                        for (int tx = x; tx < 16; tx++) {
                            if (bit(bits, tx, y, z) == source && !used[tx][z] && faceNeeded(bits, face, tx, y, z, translucency)) {
                                maxX = tx;
                            } else break;
                        }

                        zLoop:
                        for (int tz = z; tz < 16; tz++) {
                            for (int tx = x; tx <= maxX; tx++) {
                                if (bit(bits, tx, y, tz) != source || used[tx][tz] || !faceNeeded(bits, face, tx, y, tz, translucency)) {
                                    break zLoop;
                                }
                            }
                            maxZ = tz;
                        }

                        for (int xx = x; xx <= maxX; xx++) {
                            for (int zz = z; zz <= maxZ; zz++) used[xx][zz] = true;
                        }

                        emitRegion(output, view, pos, source, face, x, y, z, maxX, y, maxZ);
                    }
                }
            }
        }

        // Fixed Z plane, merge along X/Y.
        for (Direction face : Z_DIRECTIONS) {
            for (int z = 0; z < 16; z++) {
                clear(used);
                for (int x = 0; x < 16; x++) {
                    for (int y = 0; y < 16; y++) {
                        BlockState source = bit(bits, x, y, z);
                        if (source.isAir() || used[x][y] || !faceNeeded(bits, face, x, y, z, translucency)) continue;

                        int maxX = x;
                        int maxY = y;
                        for (int tx = x; tx < 16; tx++) {
                            if (bit(bits, tx, y, z) == source && !used[tx][y] && faceNeeded(bits, face, tx, y, z, translucency)) {
                                maxX = tx;
                            } else break;
                        }

                        yLoop:
                        for (int ty = y; ty < 16; ty++) {
                            for (int tx = x; tx <= maxX; tx++) {
                                if (bit(bits, tx, ty, z) != source || used[tx][ty] || !faceNeeded(bits, face, tx, ty, z, translucency)) {
                                    break yLoop;
                                }
                            }
                            maxY = ty;
                        }

                        for (int xx = x; xx <= maxX; xx++) {
                            for (int yy = y; yy <= maxY; yy++) used[xx][yy] = true;
                        }

                        emitRegion(output, view, pos, source, face, x, y, z, maxX, maxY, z);
                    }
                }
            }
        }
    }

    private static void emitRegion(QuadEmitter output, BlockAndTintGetter view, BlockPos pos, BlockState sourceState, Direction face,
                                   int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        BlockStateModel sourceModel = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(sourceState);
        RandomSource random = RandomSource.create(42L);

        QuadEmitter sourceEmitter = Renderer.get().quadEmitter(sourceQuad -> {
            if (sourceQuad.lightFace() != face) return;

            output.copyFrom(sourceQuad);
            clipGeometryAndUvs(output, sourceQuad, face, minX, minY, minZ, maxX, maxY, maxZ);
            bakeTint(output, sourceState, view, pos);

            if (!canCull(face, minX, minY, minZ, maxX, maxY, maxZ)) {
                output.cullFace(null);
            }

            output.emit();
        });

        ((FabricBlockStateModel) (Object) sourceModel)
            .emitQuads(sourceEmitter, view, pos, sourceState, random, ignored -> false);
    }

    /**
     * Clips a full block-model quad to the micro-voxel region and remaps the
     * source UVs by the resulting full-block coordinates. This preserves the
     * original texture's orientation, flips, atlas position and overlays.
     */
    private static void clipGeometryAndUvs(MutableQuadView output, MutableQuadView source, Direction face,
                                           int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        float minXF = minX * PIXEL;
        float minYF = minY * PIXEL;
        float minZF = minZ * PIXEL;
        float maxXF = (maxX + 1) * PIXEL;
        float maxYF = (maxY + 1) * PIXEL;
        float maxZF = (maxZ + 1) * PIXEL;

        for (int vertex = 0; vertex < 4; vertex++) {
            float x = source.x(vertex);
            float y = source.y(vertex);
            float z = source.z(vertex);

            if (approx(x, 0.0f)) x = minXF;
            else if (approx(x, 1.0f)) x = maxXF;
            if (approx(y, 0.0f)) y = minYF;
            else if (approx(y, 1.0f)) y = maxYF;
            if (approx(z, 0.0f)) z = minZF;
            else if (approx(z, 1.0f)) z = maxZF;

            output.pos(vertex, x, y, z);
        }

        int axisA;
        int axisB;
        switch (face.getAxis()) {
            case X -> { axisA = 1; axisB = 2; } // Y / Z
            case Y -> { axisA = 0; axisB = 2; } // X / Z
            case Z -> { axisA = 0; axisB = 1; } // X / Y
            default -> throw new IllegalStateException();
        }

        float[] cornerU = new float[4];
        float[] cornerV = new float[4];
        boolean[] found = new boolean[4];

        for (int vertex = 0; vertex < 4; vertex++) {
            float a = coordinate(source, vertex, axisA);
            float b = coordinate(source, vertex, axisB);
            int corner = (a >= 0.5f ? 1 : 0) | (b >= 0.5f ? 2 : 0);
            cornerU[corner] = source.u(vertex);
            cornerV[corner] = source.v(vertex);
            found[corner] = true;
        }

        if (!(found[0] && found[1] && found[2] && found[3])) return;

        for (int vertex = 0; vertex < 4; vertex++) {
            float a = coordinate(output, vertex, axisA);
            float b = coordinate(output, vertex, axisB);
            float u = bilerp(cornerU[0], cornerU[1], cornerU[2], cornerU[3], a, b);
            float v = bilerp(cornerV[0], cornerV[1], cornerV[2], cornerV[3], a, b);
            output.uv(vertex, u, v);
        }
    }

    private static void bakeTint(MutableQuadView quad, BlockState sourceState, BlockAndTintGetter view, BlockPos pos) {
        int tintIndex = quad.tintIndex();
        if (tintIndex < 0) return;

        BlockTintSource tintSource = Minecraft.getInstance().getBlockColors().getTintSource(sourceState, tintIndex);
        if (tintSource != null) {
            int tint = tintSource.colorInWorld(sourceState, view, pos);
            quad.multiplyColor(0xFF000000 | (tint & 0x00FFFFFF));
        }

        // The tint is now baked into the vertex color. Leaving the tint index
        // active would tint it a second time when the mesh is submitted.
        quad.tintIndex(-1);
    }

    private static float coordinate(MutableQuadView quad, int vertex, int axis) {
        return switch (axis) {
            case 0 -> quad.x(vertex);
            case 1 -> quad.y(vertex);
            case 2 -> quad.z(vertex);
            default -> throw new IllegalArgumentException("axis");
        };
    }

    private static float bilerp(float c00, float c10, float c01, float c11, float a, float b) {
        float oneA = 1.0f - a;
        float oneB = 1.0f - b;
        return c00 * oneA * oneB + c10 * a * oneB + c01 * oneA * b + c11 * a * b;
    }

    private static boolean faceNeeded(BlockState[] bits, Direction face, int x, int y, int z, Map<BlockState, Boolean> translucency) {
        int nx = x + face.getStepX();
        int ny = y + face.getStepY();
        int nz = z + face.getStepZ();
        if (!BitsBlockEntity.inside(nx, ny, nz)) return true;

        BlockState state = bit(bits, x, y, z);
        BlockState neighbor = bit(bits, nx, ny, nz);
        if (neighbor.isAir()) return true;

        return neighbor != state && isTranslucent(neighbor, translucency);
    }

    private static boolean isTranslucent(BlockState state, Map<BlockState, Boolean> cache) {
        if (state.isAir()) return false;
        Boolean cached = cache.get(state);
        if (cached != null) return cached;

        BlockStateModel model = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(state);
        boolean result = model.hasMaterialFlag(BakedQuad.FLAG_TRANSLUCENT);
        cache.put(state, result);
        return result;
    }

    private static BlockState bit(BlockState[] bits, int x, int y, int z) {
        return bits[(y * 16 * 16) + (z * 16) + x];
    }

    private static boolean canCull(Direction face, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        return switch (face) {
            case UP -> maxY == 15;
            case DOWN -> minY == 0;
            case SOUTH -> maxZ == 15;
            case NORTH -> minZ == 0;
            case EAST -> maxX == 15;
            case WEST -> minX == 0;
        };
    }

    private static void clear(boolean[][] used) {
        for (boolean[] row : used) Arrays.fill(row, false);
    }

    private static boolean approx(float value, float target) {
        return Math.abs(value - target) < 0.01f;
    }

    @Override
    public void submit(BitsBlockRenderState state, PoseStack matrices, SubmitNodeCollector queue, CameraRenderState cameraState) {
        state.model.submit(matrices, queue, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
    }
}
