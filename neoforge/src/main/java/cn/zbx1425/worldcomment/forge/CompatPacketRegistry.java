package cn.zbx1425.worldcomment.forge;

#if MC_VERSION >= "12100"
import cn.zbx1425.worldcomment.ServerPlatform;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class CompatPacketRegistry {

    public HashMap<ResourceLocation, CompatPacket> packets = new HashMap<>();
    public HashMap<ResourceLocation, Consumer<FriendlyByteBuf>> packetsS2C = new HashMap<>();
    public HashMap<ResourceLocation, ServerPlatform.C2SPacketHandler> packetsC2S = new HashMap<>();

    public void registerPacket(ResourceLocation resourceLocation) {
        packets.computeIfAbsent(resourceLocation, CompatPacket::new);
    }

    public void registerNetworkReceiverS2C(ResourceLocation resourceLocation, Consumer<FriendlyByteBuf> consumer) {
        packets.computeIfAbsent(resourceLocation, CompatPacket::new);
        packetsS2C.put(resourceLocation, consumer);
    }

    public void registerNetworkReceiverC2S(ResourceLocation resourceLocation, ServerPlatform.C2SPacketHandler consumer) {
        packets.computeIfAbsent(resourceLocation, CompatPacket::new);
        packetsC2S.put(resourceLocation, consumer);
    }

    public void commit(PayloadRegistrar registrar) {
        for (Map.Entry<ResourceLocation, CompatPacket> packets : packets.entrySet()) {
            Consumer<FriendlyByteBuf> handlerS2C = packetsS2C.getOrDefault(packets.getKey(), arg -> {});
            ServerPlatform.C2SPacketHandler handlerC2S = packetsC2S.getOrDefault(packets.getKey(), (server, player, arg) -> {});
            CompatPacket packet = packets.getValue();
            registrar.playBidirectional(packet.TYPE, packet.STREAM_CODEC, new DirectionalPayloadHandler<>(
                    (arg, iPayloadContext) -> handlerS2C.accept(arg.buffer),
                    (arg, iPayloadContext) -> handlerC2S.handlePacket(
                            iPayloadContext.player().getServer(), (ServerPlayer)iPayloadContext.player(), arg.buffer)
            ));
        }
    }

    public void sendS2C(ServerPlayer player, ResourceLocation id, FriendlyByteBuf payload) {
        CompatPacket packet = packets.get(id);
        PacketDistributor.sendToPlayer(player, packet.new Payload(payload));
    }

    public void sendC2S(ResourceLocation id, FriendlyByteBuf payload) {
        CompatPacket packet = packets.get(id);
        PacketDistributor.sendToServer(packet.new Payload(payload));
    }
}

#else
public class CompatPacketRegistry {

}

#endif