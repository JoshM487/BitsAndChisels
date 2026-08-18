package io.github.coolmineman.bitsandchisels;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class BitData {
    private static final String BIT_STATE = "bitsandchisels_bit_state";
    private BitData() {}

    public static void setState(ItemStack stack, BlockState state) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.put(BIT_STATE, NbtUtils.writeBlockState(state));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static BlockState getState(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.contains(BIT_STATE)) return Blocks.AIR.defaultBlockState();
        return NbtUtils.readBlockState(BuiltInRegistries.BLOCK, tag.getCompound(BIT_STATE));
    }
}
