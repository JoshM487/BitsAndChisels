package io.github.coolmineman.bitsandchisels;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BitsBlockEntity extends BlockEntity {
    public static final int SIZE = 16;
    public static final int COUNT = SIZE * SIZE * SIZE;
    private final BlockState[] bits = new BlockState[COUNT];
    private VoxelShape cachedShape = Shapes.empty();
    private boolean shapeDirty = true;

    public BitsBlockEntity(BlockPos pos, BlockState state) {
        super(BitsAndChisels.BITS_BLOCK_ENTITY, pos, state);
        Arrays.fill(bits, Blocks.AIR.defaultBlockState());
    }

    private static int index(int x, int y, int z) {
        return (y * SIZE * SIZE) + (z * SIZE) + x;
    }

    public BlockState get(int x, int y, int z) {
        if (!inside(x, y, z)) return Blocks.AIR.defaultBlockState();
        return bits[index(x, y, z)];
    }

    public void set(int x, int y, int z, BlockState state) {
        if (!inside(x, y, z)) return;
        bits[index(x, y, z)] = state == null ? Blocks.AIR.defaultBlockState() : state;
        shapeDirty = true;
        setChanged();
    }

    public void fill(BlockState state) {
        Arrays.fill(bits, state);
        shapeDirty = true;
        setChanged();
    }

    public void replaceAll(BlockState[] states) {
        System.arraycopy(states, 0, bits, 0, Math.min(states.length, bits.length));
        shapeDirty = true;
        setChanged();
    }

    public BlockState[] copyBits() {
        return bits.clone();
    }

    public boolean isEmpty() {
        for (BlockState state : bits) if (!state.isAir()) return false;
        return true;
    }

    public int nonAirCount() {
        int count = 0;
        for (BlockState state : bits) if (!state.isAir()) count++;
        return count;
    }

    public VoxelShape shape() {
        if (!shapeDirty) return cachedShape;
        VoxelShape result = Shapes.empty();
        for (int y = 0; y < SIZE; y++) {
            for (int z = 0; z < SIZE; z++) {
                for (int x = 0; x < SIZE; x++) {
                    if (!get(x, y, z).isAir()) {
                        double s = 1.0 / SIZE;
                        result = Shapes.or(result, Shapes.box(x * s, y * s, z * s, (x + 1) * s, (y + 1) * s, (z + 1) * s));
                    }
                }
            }
        }
        cachedShape = result;
        shapeDirty = false;
        return result;
    }

    public static boolean inside(int x, int y, int z) {
        return x >= 0 && y >= 0 && z >= 0 && x < SIZE && y < SIZE && z < SIZE;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        Map<BlockState, Integer> paletteMap = new LinkedHashMap<>();
        List<BlockState> palette = new ArrayList<>();
        int[] indices = new int[COUNT];
        for (int i = 0; i < COUNT; i++) {
            BlockState state = bits[i];
            Integer idx = paletteMap.get(state);
            if (idx == null) {
                idx = palette.size();
                paletteMap.put(state, idx);
                palette.add(state);
            }
            indices[i] = idx;
        }
        output.store("palette", BlockState.CODEC.listOf(), palette);
        output.putIntArray("bits", indices);
        super.saveAdditional(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        List<BlockState> palette = input.read("palette", BlockState.CODEC.listOf()).orElse(List.of(Blocks.AIR.defaultBlockState()));
        int[] indices = input.getIntArray("bits").orElse(new int[0]);
        for (int i = 0; i < COUNT; i++) {
            int paletteIndex = i < indices.length ? indices[i] : 0;
            bits[i] = paletteIndex >= 0 && paletteIndex < palette.size() ? palette.get(paletteIndex) : Blocks.AIR.defaultBlockState();
        }
        shapeDirty = true;
    }

    @Override
    public net.minecraft.nbt.CompoundTag getUpdateTag(HolderLookup.Provider registryLookup) {
        return saveWithoutMetadata(registryLookup);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_ALL);
        }
    }
}
