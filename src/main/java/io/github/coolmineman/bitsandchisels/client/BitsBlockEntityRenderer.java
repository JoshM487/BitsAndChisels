package io.github.coolmineman.bitsandchisels.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.coolmineman.bitsandchisels.BitsBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class BitsBlockEntityRenderer implements BlockEntityRenderer<BitsBlockEntity, BitsBlockRenderState> {
    private static final BlockDisplayContext DISPLAY_CONTEXT = BlockDisplayContext.create();
    private final BlockModelResolver modelResolver;

    public BitsBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.modelResolver = context.blockModelResolver();
    }

    @Override
    public BitsBlockRenderState createRenderState() {
        return new BitsBlockRenderState();
    }

    @Override
    public void extractRenderState(BitsBlockEntity blockEntity, BitsBlockRenderState state, float tickProgress, Vec3 cameraPos, @Nullable ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, tickProgress, cameraPos, crumblingOverlay);
        state.visibleBits.clear();
        for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    BlockState bit = blockEntity.get(x, y, z);
                    if (bit.isAir() || !isSurface(blockEntity, x, y, z)) continue;
                    BlockModelRenderState model = new BlockModelRenderState();
                    modelResolver.update(model, bit, DISPLAY_CONTEXT);
                    state.visibleBits.add(new BitsBlockRenderState.MicroBit(x, y, z, model));
                }
            }
        }
    }

    private static boolean isSurface(BitsBlockEntity bits, int x, int y, int z) {
        return x == 0 || x == 15 || y == 0 || y == 15 || z == 0 || z == 15
            || bits.get(x - 1, y, z).isAir() || bits.get(x + 1, y, z).isAir()
            || bits.get(x, y - 1, z).isAir() || bits.get(x, y + 1, z).isAir()
            || bits.get(x, y, z - 1).isAir() || bits.get(x, y, z + 1).isAir();
    }

    @Override
    public void submit(BitsBlockRenderState state, PoseStack matrices, SubmitNodeCollector queue, CameraRenderState cameraState) {
        final float scale = 1.0f / 16.0f;
        for (BitsBlockRenderState.MicroBit bit : state.visibleBits) {
            matrices.pushPose();
            matrices.translate(bit.x() * scale, bit.y() * scale, bit.z() * scale);
            matrices.scale(scale, scale, scale);
            bit.model().submit(matrices, queue, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            matrices.popPose();
        }
    }
}
