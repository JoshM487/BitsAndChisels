package io.github.coolmineman.bitsandchisels;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class BitItem extends Item {
    public BitItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockState bitState = BitData.getState(context.getItemInHand());
        if (bitState.isAir()) return InteractionResult.PASS;

        BlockPos clicked = context.getClickedPos();
        Direction face = context.getClickedFace();
        Vec3 location = context.getClickLocation();

        int rawX = (int)Math.floor((location.x - clicked.getX()) * 16.0 + face.getStepX() * 0.5);
        int rawY = (int)Math.floor((location.y - clicked.getY()) * 16.0 + face.getStepY() * 0.5);
        int rawZ = (int)Math.floor((location.z - clicked.getZ()) * 16.0 + face.getStepZ() * 0.5);

        BlockPos target = clicked;
        int x = rawX, y = rawY, z = rawZ;
        if (!BitsBlockEntity.inside(rawX, rawY, rawZ)) {
            target = clicked.relative(face);
            x = face.getStepX() > 0 ? 0 : face.getStepX() < 0 ? 15 : clampBit(rawX);
            y = face.getStepY() > 0 ? 0 : face.getStepY() < 0 ? 15 : clampBit(rawY);
            z = face.getStepZ() > 0 ? 0 : face.getStepZ() < 0 ? 15 : clampBit(rawZ);
        } else {
            x = clampBit(x);
            y = clampBit(y);
            z = clampBit(z);
        }

        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!BitUtils.setBit(level, target, x, y, z, bitState)) return InteractionResult.PASS;

        if (context.getPlayer() != null && !context.getPlayer().isCreative()) {
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public Component getName(ItemStack stack) {
        BlockState state = BitData.getState(stack);
        if (!state.isAir()) return Component.translatable("item.bitsandchisels.bit_item", state.getBlock().getName());
        return Component.literal("Bit");
    }

    private static int clampBit(int value) {
        return Math.max(0, Math.min(15, value));
    }
}
