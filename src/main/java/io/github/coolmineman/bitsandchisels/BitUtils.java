package io.github.coolmineman.bitsandchisels;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class BitUtils {
    private BitUtils() {}

    public static BitsBlockEntity getBits(Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity instanceof BitsBlockEntity bits ? bits : null;
    }

    public static BitsBlockEntity convertToBits(Level level, BlockPos pos) {
        BitsBlockEntity existing = getBits(level, pos);
        if (existing != null) return existing;

        BlockState original = level.getBlockState(pos);
        if (original.isAir() || original.is(BitsAndChisels.BITS_BLOCK)) return null;
        if (!original.isCollisionShapeFullBlock(level, pos)) return null;
        if (original.getDestroySpeed(level, pos) < 0.0f) return null;

        level.setBlock(pos, BitsAndChisels.BITS_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
        BitsBlockEntity bits = getBits(level, pos);
        if (bits != null) bits.fill(original);
        return bits;
    }

    public static BlockState getBit(Level level, BlockPos pos, int x, int y, int z) {
        BitsBlockEntity bits = getBits(level, pos);
        if (bits != null) return bits.get(x, y, z);
        return Blocks.AIR.defaultBlockState();
    }

    public static boolean setBit(Level level, BlockPos pos, int x, int y, int z, BlockState state) {
        if (!BitsBlockEntity.inside(x, y, z)) return false;
        BitsBlockEntity bits = getBits(level, pos);
        if (bits == null) {
            if (!level.getBlockState(pos).isAir()) return false;
            level.setBlock(pos, BitsAndChisels.BITS_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
            bits = getBits(level, pos);
        }
        if (bits == null || !bits.get(x, y, z).isAir()) return false;
        bits.set(x, y, z, state);
        return true;
    }

    public static boolean breakBit(ServerPlayer player, BlockPos pos, int x, int y, int z) {
        Level level = player.level();
        BitsBlockEntity bits = getBits(level, pos);
        if (bits == null) bits = convertToBits(level, pos);
        if (bits == null || !BitsBlockEntity.inside(x, y, z)) return false;

        BlockState state = bits.get(x, y, z);
        if (state.isAir()) return false;
        bits.set(x, y, z, Blocks.AIR.defaultBlockState());

        if (!player.isCreative()) {
            ItemStack drop = new ItemStack(BitsAndChisels.BIT_ITEM);
            BitData.setState(drop, state);
            player.drop(drop, false);
        }
        removeIfEmpty(level, pos, bits);
        return true;
    }

    public static int breakRegion(ServerPlayer player, BlockPos origin, int x1, int y1, int z1, int x2, int y2, int z2) {
        int minX = Math.min(x1, x2), maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2), maxY = Math.max(y1, y2);
        int minZ = Math.min(z1, z2), maxZ = Math.max(z1, z2);
        int broken = 0;
        for (int gx = minX; gx <= maxX; gx++) {
            for (int gy = minY; gy <= maxY; gy++) {
                for (int gz = minZ; gz <= maxZ; gz++) {
                    BlockPos pos = origin.offset(Math.floorDiv(gx, 16), Math.floorDiv(gy, 16), Math.floorDiv(gz, 16));
                    int x = Math.floorMod(gx, 16), y = Math.floorMod(gy, 16), z = Math.floorMod(gz, 16);
                    if (breakBit(player, pos, x, y, z)) broken++;
                }
            }
        }
        return broken;
    }

    private static void removeIfEmpty(Level level, BlockPos pos, BitsBlockEntity bits) {
        if (bits.isEmpty()) level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
    }
}
