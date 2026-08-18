package io.github.coolmineman.bitsandchisels.client;

import io.github.coolmineman.bitsandchisels.BitsAndChisels;
import io.github.coolmineman.bitsandchisels.chisel.ChiselItem;
import io.github.coolmineman.bitsandchisels.network.ChiselPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;

public final class BitsBlockClient implements ClientModInitializer {
    private static BlockPos smartOrigin;
    private static int smartX, smartY, smartZ;

    @Override
    public void onInitializeClient() {
        BlockEntityRenderers.register(BitsAndChisels.BITS_BLOCK_ENTITY, BitsBlockEntityRenderer::new);

        AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> {
            if (!level.isClientSide()) return InteractionResult.PASS;
            if (!(player.getItemInHand(hand).getItem() instanceof ChiselItem chisel)) return InteractionResult.PASS;
            if (!(Minecraft.getInstance().hitResult instanceof BlockHitResult hit)) return InteractionResult.PASS;

            int x = bitCoord(hit.getLocation().x - pos.getX(), hit.getDirection().getStepX());
            int y = bitCoord(hit.getLocation().y - pos.getY(), hit.getDirection().getStepY());
            int z = bitCoord(hit.getLocation().z - pos.getZ(), hit.getDirection().getStepZ());
            x = clampBit(x);
            y = clampBit(y);
            z = clampBit(z);

            switch (chisel.mode()) {
                case SINGLE -> ClientPlayNetworking.send(new ChiselPayload(pos, x, y, z, x, y, z, 0));
                case FOUR_BY_FOUR -> {
                    int x1 = (x / 4) * 4;
                    int y1 = (y / 4) * 4;
                    int z1 = (z / 4) * 4;
                    ClientPlayNetworking.send(new ChiselPayload(pos, x1, y1, z1, x1 + 3, y1 + 3, z1 + 3, 1));
                }
                case SMART -> handleSmart(pos, x, y, z);
            }
            return InteractionResult.SUCCESS;
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || !(client.player.getMainHandItem().getItem() instanceof ChiselItem c) || c.mode() != ChiselItem.Mode.SMART) {
                smartOrigin = null;
            }
        });
    }

    private static int clampBit(int value) {
        return Math.max(0, Math.min(15, value));
    }

    private static int bitCoord(double local, int faceStep) {
        return (int)Math.floor(local * 16.0 + faceStep * -0.5);
    }

    private static void handleSmart(BlockPos pos, int x, int y, int z) {
        if (smartOrigin == null || smartOrigin.distSqr(pos) > 9.0) {
            smartOrigin = pos.immutable();
            smartX = x;
            smartY = y;
            smartZ = z;
            return;
        }

        int x2 = (pos.getX() - smartOrigin.getX()) * 16 + x;
        int y2 = (pos.getY() - smartOrigin.getY()) * 16 + y;
        int z2 = (pos.getZ() - smartOrigin.getZ()) * 16 + z;
        ClientPlayNetworking.send(new ChiselPayload(
            smartOrigin,
            Math.min(smartX, x2), Math.min(smartY, y2), Math.min(smartZ, z2),
            Math.max(smartX, x2), Math.max(smartY, y2), Math.max(smartZ, z2),
            2
        ));
        smartOrigin = null;
    }
}
