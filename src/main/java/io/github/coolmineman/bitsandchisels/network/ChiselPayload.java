package io.github.coolmineman.bitsandchisels.network;

import io.github.coolmineman.bitsandchisels.BitUtils;
import io.github.coolmineman.bitsandchisels.BitsAndChisels;
import io.github.coolmineman.bitsandchisels.chisel.ChiselItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public record ChiselPayload(BlockPos origin, int x1, int y1, int z1, int x2, int y2, int z2, int mode) implements CustomPacketPayload {
    public static final Type<ChiselPayload> TYPE = new Type<>(BitsAndChisels.id("chisel"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ChiselPayload> CODEC = CustomPacketPayload.codec(ChiselPayload::write, ChiselPayload::new);

    private ChiselPayload(RegistryFriendlyByteBuf buf) {
        this(buf.readBlockPos(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt());
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(origin);
        buf.writeInt(x1);
        buf.writeInt(y1);
        buf.writeInt(z1);
        buf.writeInt(x2);
        buf.writeInt(y2);
        buf.writeInt(z2);
        buf.writeInt(mode);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(ServerPlayer player) {
        if (!(player.getMainHandItem().getItem() instanceof ChiselItem chisel)) return;
        if (player.blockPosition().distSqr(origin) > 100.0) return;

        ChiselItem.Mode expected = switch (mode) {
            case 0 -> ChiselItem.Mode.SINGLE;
            case 1 -> ChiselItem.Mode.FOUR_BY_FOUR;
            case 2 -> ChiselItem.Mode.SMART;
            default -> null;
        };
        if (expected == null || chisel.mode() != expected) return;

        if (expected == ChiselItem.Mode.SINGLE) {
            BitUtils.breakBit(player, origin, x1, y1, z1);
            return;
        }

        long dx = Math.abs((long)x2 - x1);
        long dy = Math.abs((long)y2 - y1);
        long dz = Math.abs((long)z2 - z1);
        // Smart chisel is intentionally limited to the original nearby-selection behavior.
        if (dx > 63 || dy > 63 || dz > 63) return;
        if ((dx + 1L) * (dy + 1L) * (dz + 1L) > 262_144L) return;
        BitUtils.breakRegion(player, origin, x1, y1, z1, x2, y2, z2);
    }
}
