package io.github.coolmineman.bitsandchisels.wrench;

import io.github.coolmineman.bitsandchisels.BitUtils;
import io.github.coolmineman.bitsandchisels.BitsBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class WrenchItem extends Item {
    public WrenchItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide()) return InteractionResult.SUCCESS;
        BitsBlockEntity bits = BitUtils.getBits(context.getLevel(), context.getClickedPos());
        if (bits == null) return InteractionResult.PASS;

        boolean mirror = context.getPlayer() != null && context.getPlayer().isShiftKeyDown();
        transform(bits, context.getClickedFace().getAxis(), mirror);
        return InteractionResult.SUCCESS;
    }

    private static void transform(BitsBlockEntity bits, Direction.Axis axis, boolean mirror) {
        BlockState[] source = bits.copyBits();
        BlockState[] target = new BlockState[BitsBlockEntity.COUNT];
        java.util.Arrays.fill(target, Blocks.AIR.defaultBlockState());
        for (int x = 0; x < 16; x++) for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) {
            int nx = x, ny = y, nz = z;
            if (mirror) {
                switch (axis) {
                    case X -> nx = 15 - x;
                    case Y -> ny = 15 - y;
                    case Z -> nz = 15 - z;
                }
            } else {
                switch (axis) {
                    case X -> { ny = z; nz = 15 - y; }
                    case Y -> { nx = z; nz = 15 - x; }
                    case Z -> { nx = y; ny = 15 - x; }
                }
            }
            target[(ny * 256) + (nz * 16) + nx] = source[(y * 256) + (z * 16) + x];
        }
        bits.replaceAll(target);
    }
}
