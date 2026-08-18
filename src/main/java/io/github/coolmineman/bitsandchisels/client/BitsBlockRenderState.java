package io.github.coolmineman.bitsandchisels.client;

import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import java.util.ArrayList;
import java.util.List;

public final class BitsBlockRenderState extends BlockEntityRenderState {
    public final List<MicroBit> visibleBits = new ArrayList<>();

    public record MicroBit(int x, int y, int z, BlockModelRenderState model) {}
}
