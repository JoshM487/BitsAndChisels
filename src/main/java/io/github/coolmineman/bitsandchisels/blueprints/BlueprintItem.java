package io.github.coolmineman.bitsandchisels.blueprints;

import io.github.coolmineman.bitsandchisels.BitData;
import io.github.coolmineman.bitsandchisels.BitUtils;
import io.github.coolmineman.bitsandchisels.BitsAndChisels;
import io.github.coolmineman.bitsandchisels.BitsBlockEntity;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;

public final class BlueprintItem extends Item {
    private static final String BLUEPRINT = "bitsandchisels_blueprint";

    public BlueprintItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide()) return InteractionResult.SUCCESS;

        ItemStack blueprint = context.getItemInHand();
        CompoundTag root = blueprint.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!root.contains(BLUEPRINT)) {
            BitsBlockEntity bits = BitUtils.getBits(context.getLevel(), context.getClickedPos());
            if (bits == null) return InteractionResult.PASS;
            CompoundTag data = new CompoundTag();
            BlockState[] states = bits.copyBits();
            for (int i = 0; i < states.length; i++) data.put("s" + i, NbtUtils.writeBlockState(states[i]));
            root.put(BLUEPRINT, data);
            blueprint.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
            return InteractionResult.SUCCESS;
        }

        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        CompoundTag data = root.getCompound(BLUEPRINT);
        var target = context.getClickedPos().relative(context.getClickedFace());
        boolean changed = false;

        for (int i = 0; i < BitsBlockEntity.COUNT; i++) {
            String key = "s" + i;
            if (!data.contains(key)) continue;
            BlockState state = NbtUtils.readBlockState(BuiltInRegistries.BLOCK, data.getCompound(key));
            if (state.isAir()) continue;

            int x = i % 16;
            int z = (i / 16) % 16;
            int y = i / 256;
            if (!BitUtils.getBit(context.getLevel(), target, x, y, z).isAir()) continue;

            ItemStack payment = player.isCreative() ? ItemStack.EMPTY : findMatchingBit(player, state);
            if (!player.isCreative() && payment.isEmpty()) continue;

            if (BitUtils.setBit(context.getLevel(), target, x, y, z, state)) {
                if (!player.isCreative()) payment.shrink(1);
                changed = true;
            }
        }
        return changed ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    private static ItemStack findMatchingBit(Player player, BlockState wanted) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(BitsAndChisels.BIT_ITEM) && BitData.getState(stack).equals(wanted)) return stack;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public Component getName(ItemStack stack) {
        CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return root.contains(BLUEPRINT)
            ? Component.translatable("item.bitsandchisels.blueprint")
            : Component.translatable("item.bitsandchisels.blueprint.unwritten");
    }
}
