package io.github.coolmineman.bitsandchisels.client;

import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

public final class BitsBlockRenderState extends BlockEntityRenderState {
    public final BlockModelRenderState model = new BlockModelRenderState();
    public long meshRevision = Long.MIN_VALUE;
}
