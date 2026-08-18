package io.github.coolmineman.bitsandchisels;

import io.github.coolmineman.bitsandchisels.blueprints.BlueprintItem;
import io.github.coolmineman.bitsandchisels.chisel.ChiselItem;
import io.github.coolmineman.bitsandchisels.network.ChiselPayload;
import io.github.coolmineman.bitsandchisels.wrench.WrenchItem;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.Function;

public final class BitsAndChisels implements ModInitializer {
    public static final String MOD_ID = "bitsandchisels";

    public static final ResourceKey<Block> BITS_BLOCK_KEY = ResourceKey.create(Registries.BLOCK, id("bits_block"));
    public static final BitsBlock BITS_BLOCK = Registry.register(
        BuiltInRegistries.BLOCK,
        BITS_BLOCK_KEY,
        new BitsBlock(BlockBehaviour.Properties.of().strength(4.0f).sound(SoundType.STONE).noOcclusion().setId(BITS_BLOCK_KEY))
    );

    // Keep the original block-item ID for world/mod compatibility, even though it is not shown in the creative tab.
    public static final Item BITS_BLOCK_ITEM = registerBlockItem("bits_block", BITS_BLOCK);

    public static final BlockEntityType<BitsBlockEntity> BITS_BLOCK_ENTITY = Registry.register(
        BuiltInRegistries.BLOCK_ENTITY_TYPE,
        id("bits_block_entity"),
        FabricBlockEntityTypeBuilder.create(BitsBlockEntity::new, BITS_BLOCK).build()
    );

    public static final Item BIT_ITEM = registerItem("bit_item", BitItem::new, new Item.Properties().stacksTo(64));
    public static final Item DIAMOND_CHISEL = registerItem("diamond_chisel", p -> new ChiselItem(p, ChiselItem.Mode.SINGLE), new Item.Properties().stacksTo(1));
    public static final Item IRON_CHISEL = registerItem("iron_chisel", p -> new ChiselItem(p, ChiselItem.Mode.FOUR_BY_FOUR), new Item.Properties().stacksTo(1));
    public static final Item SMART_CHISEL = registerItem("smart_chisel", p -> new ChiselItem(p, ChiselItem.Mode.SMART), new Item.Properties().stacksTo(1));
    public static final Item WRENCH = registerItem("wrench", WrenchItem::new, new Item.Properties().stacksTo(1));
    public static final Item BLUEPRINT = registerItem("blueprint", BlueprintItem::new, new Item.Properties().stacksTo(1));

    public static final ResourceKey<CreativeModeTab> CREATIVE_TAB_KEY = ResourceKey.create(
        BuiltInRegistries.CREATIVE_MODE_TAB.key(), id(MOD_ID)
    );
    public static final CreativeModeTab CREATIVE_TAB = FabricCreativeModeTab.builder()
        .icon(() -> new ItemStack(DIAMOND_CHISEL))
        .title(Component.translatable("itemGroup.bitsandchisels.bitsandchisels"))
        .displayItems((params, output) -> {
            output.accept(DIAMOND_CHISEL);
            output.accept(IRON_CHISEL);
            output.accept(SMART_CHISEL);
            output.accept(WRENCH);
            output.accept(BLUEPRINT);
        })
        .build();

    @Override
    public void onInitialize() {
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, CREATIVE_TAB_KEY, CREATIVE_TAB);

        PayloadTypeRegistry.serverboundPlay().register(ChiselPayload.TYPE, ChiselPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ChiselPayload.TYPE, (payload, context) -> payload.handle(context.player()));

        // The client sends precise micro-bit coordinates. Prevent vanilla from also breaking the full block.
        AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> {
            if (player.getItemInHand(hand).getItem() instanceof ChiselItem && !level.isClientSide()) {
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });
    }

    private static Item registerBlockItem(String name, Block block) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id(name));
        BlockItem item = new BlockItem(block, new Item.Properties().useBlockDescriptionPrefix().setId(key));
        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }

    private static Item registerItem(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id(name));
        Item item = factory.apply(properties.setId(key));
        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
