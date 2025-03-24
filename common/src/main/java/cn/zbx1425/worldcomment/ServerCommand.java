package cn.zbx1425.worldcomment;

import cn.zbx1425.worldcomment.network.PacketClientConfigS2C;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.util.function.BiFunction;
import java.util.function.Function;

public class ServerCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher,
                                    Function<String, LiteralArgumentBuilder<CommandSourceStack>> literal,
                                    BiFunction<String, ArgumentType<?>, RequiredArgumentBuilder<CommandSourceStack, ?>> argument) {
        dispatcher.register(literal.apply("wcs")
                .then(literal.apply("imageGlobalKill").then(argument.apply("kill", BoolArgumentType.bool()))
                        .executes(context -> {
                            boolean kill = BoolArgumentType.getBool(context, "kill");
                            Main.SERVER_CONFIG.imageGlobalKill.value = kill ? "true" : "false";
                            Main.SERVER_CONFIG.imageGlobalKill.isFromJson = kill;
                            try {
                                Main.SERVER_CONFIG.save();
                            } catch (IOException ex) {
                                Main.LOGGER.warn("Failed to save config", ex);
                            }
                            for (ServerPlayer player : context.getSource().getServer().getPlayerList().getPlayers()) {
                                PacketClientConfigS2C.send(player, Main.SERVER_CONFIG);
                            }
                            return 1;
                        }))
        );
    }
}
