package cn.zbx1425.worldcomment;

import cn.zbx1425.worldcomment.data.client.Screenshot;
import cn.zbx1425.worldcomment.gui.CommentListScreen;
import cn.zbx1425.worldcomment.gui.CommentMockScreen;
import cn.zbx1425.worldcomment.item.CommentToolItem;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionCheck;
import net.minecraft.server.permissions.PermissionLevel;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

public class ClientCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher,
                                    Function<String, LiteralArgumentBuilder<CommandSourceStack>> literal,
                                    BiFunction<String, ArgumentType<?>, RequiredArgumentBuilder<CommandSourceStack, ?>> argument) {
        dispatcher.register(literal.apply("wc")
            .executes(context -> {
                CommentToolItem.Client.triggerCommentSend(true);
                return 1;
            })
            .then(literal.apply("send")
                .executes(context -> {
                    CommentToolItem.Client.triggerCommentSend(true);
                    return 1;
                }))
            .then(literal.apply("list")
                .executes(context -> {
                    CommentListScreen.triggerOpen();
                    return 1;
                }))
            .then(literal.apply("mock")
                .requires(Commands.hasPermission(new PermissionCheck.Require(new Permission.HasCommandLevel(PermissionLevel.byId(2)))))
                .executes(context -> {
                    if (isYaclAvailable()) {
                        Minecraft.getInstance().setScreen(CommentMockScreen.create(Minecraft.getInstance().screen));
                    } else {
                        context.getSource().sendSystemMessage(Component.literal("YetAnotherConfigLib is needed for this function."));
                    }
                    return 1;
                })
            )
        );
    }

    private static Boolean yaclAvailable = null;

    private static boolean isYaclAvailable() {
        if (yaclAvailable == null) {
            try {
                Class.forName("dev.isxander.yacl3.api.YetAnotherConfigLib");
                yaclAvailable = true;
            } catch (ClassNotFoundException e) {
                yaclAvailable = false;
            }
        }
        return yaclAvailable;
    }
}
