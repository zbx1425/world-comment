package cn.zbx1425.worldcomment;

import cn.zbx1425.worldcomment.network.PacketClientConfigS2C;
import com.google.gson.JsonPrimitive;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionCheck;
import net.minecraft.server.permissions.PermissionLevel;

import java.io.IOException;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

public class ServerCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher,
                                    Function<String, LiteralArgumentBuilder<CommandSourceStack>> literal,
                                    BiFunction<String, ArgumentType<?>, RequiredArgumentBuilder<CommandSourceStack, ?>> argument) {
        dispatcher.register(literal.apply("wcs")
                .then(literal.apply("imageGlobalKill")
                    .requires(Commands.hasPermission(new PermissionCheck.Require(new Permission.HasCommandLevel(PermissionLevel.byId(4)))))
                    .then(argument.apply("kill", BoolArgumentType.bool())
                        .executes(context -> {
                            boolean kill = BoolArgumentType.getBool(context, "kill");
                            Main.SERVER_CONFIG.imageGlobalKill = Main.SERVER_CONFIG.imageGlobalKill.withNewValueToPersist(
                                    kill, new JsonPrimitive(kill));
                            try {
                                Main.SERVER_CONFIG.save();
                            } catch (IOException ex) {
                                Main.LOGGER.warn("Failed to save config", ex);
                            }
                            for (ServerPlayer player : context.getSource().getServer().getPlayerList().getPlayers()) {
                                PacketClientConfigS2C.send(player, Main.DATABASE.metadata, Main.SERVER_CONFIG);
                            }
                            context.getSource().sendSystemMessage(Component.translatable("gui.worldcomment.image_global_kill_feedback", kill));
                            return 1;
                        })))
        );
    }
}
