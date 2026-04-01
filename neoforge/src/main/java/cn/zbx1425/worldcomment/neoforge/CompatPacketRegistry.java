package cn.zbx1425.worldcomment.neoforge;

#if MC_VERSION >= "12100"
import cn.zbx1425.worldcomment.ServerPlatform;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.network.PacketDistributor;
#if MC_VERSION < "12102" import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler; #endif
#if MC_VERSION >= "12102" import net.neoforged.neoforge.client.network.ClientPacketDistributor; #endif
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class CompatPacketRegistry {

    public HashMap<Identifier, CompatPacket> packets = new HashMap<>();
    public HashMap<Identifier, Consumer<FriendlyByteBuf>> packetsS2C = new HashMap<>();
    public HashMap<Identifier, ServerPlatform.C2SPacketHandler> packetsC2S = new HashMap<>();

    public void registerPacket(Identifier resourceLocation) {
        packets.computeIfAbsent(resourceLocation, CompatPacket::new);
    }

    public void registerNetworkReceiverS2C(Identifier resourceLocation, Consumer<FriendlyByteBuf> consumer) {
        packets.computeIfAbsent(resourceLocation, CompatPacket::new);
        packetsS2C.put(resourceLocation, consumer);
    }

    public void registerNetworkReceiverC2S(Identifier resourceLocation, ServerPlatform.C2SPacketHandler consumer) {
        packets.computeIfAbsent(resourceLocation, CompatPacket::new);
        packetsC2S.put(resourceLocation, consumer);
    }

    public void commitCommon(PayloadRegistrar registrar) {
        for (Map.Entry<Identifier, CompatPacket> packets : packets.entrySet()) {
            Consumer<FriendlyByteBuf> handlerS2C = packetsS2C.getOrDefault(packets.getKey(), arg -> {});
            ServerPlatform.C2SPacketHandler handlerC2S = packetsC2S.getOrDefault(packets.getKey(), (server, player, arg) -> {});
            CompatPacket packet = packets.getValue();
#if MC_VERSION >= "12102"
            registrar.playBidirectional(packet.TYPE, packet.STREAM_CODEC,
                    (arg, iPayloadContext) -> handlerC2S.handlePacket(
                       iPayloadContext.player().level().getServer(), (ServerPlayer)iPayloadContext.player(), arg.buffer)
            );
#else
            registrar.playBidirectional(packet.TYPE, packet.STREAM_CODEC, new DirectionalPayloadHandler<>(
                    (arg, iPayloadContext) -> handlerS2C.accept(arg.buffer),
                    (arg, iPayloadContext) -> handlerC2S.handlePacket(
                            iPayloadContext.player().getServer(), (ServerPlayer)iPayloadContext.player(), arg.buffer)
            ));
#endif
        }
    }

#if MC_VERSION >= "12102"
    public void commitClient(RegisterClientPayloadHandlersEvent event) {
        for (Map.Entry<Identifier, CompatPacket> packets : packets.entrySet()) {
            Consumer<FriendlyByteBuf> handlerS2C = packetsS2C.getOrDefault(packets.getKey(), arg -> {});
            CompatPacket packet = packets.getValue();
            event.register(packet.TYPE, (arg, iPayloadContext) -> handlerS2C.accept(arg.buffer));
        }
    }
#endif

    public void sendS2C(ServerPlayer player, Identifier id, FriendlyByteBuf payload) {
        CompatPacket packet = packets.get(id);
        PacketDistributor.sendToPlayer(player, packet.new Payload(payload));
    }

    public void sendC2S(Identifier id, FriendlyByteBuf payload) {
        CompatPacket packet = packets.get(id);
#if MC_VERSION >= "12102"
        ClientPacketDistributor.sendToServer(packet.new Payload(payload));
#else
        PacketDistributor.sendToServer(packet.new Payload(payload));
#endif
    }
}

#else
public class CompatPacketRegistry {

}

#endif