package cn.zbx1425.worldcomment;

import cn.zbx1425.worldcomment.data.client.Screenshot;
import cn.zbx1425.worldcomment.gui.CommentListScreen;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;

import java.util.function.BiFunction;
import java.util.function.Function;

public class ClientCommand {

    public static <T> void register(CommandDispatcher<T> dispatcher,
                                    Function<String, LiteralArgumentBuilder<T>> literal,
                                    BiFunction<String, ArgumentType<?>, RequiredArgumentBuilder<T, ?>> argument) {
        dispatcher.register(literal.apply("wc")
                .executes(context -> {
                    Screenshot.triggerCommentSend(false);
                    return 1;
                })
                .then(literal.apply("send")
                        .executes(context -> {
                            Screenshot.triggerCommentSend(false);
                            return 1;
                        }))
//                .then(literal.apply("visible")
//                        .executes(context -> {
//
//                        })
//                        .then(argument.apply("visible", BoolArgumentType.bool()))
//                                .executes(context -> {
//
//                                })
//                )
                .then(literal.apply("list")
                        .executes(context -> {
                            CommentListScreen.triggerOpen();
                            return 1;
                        }))
        );
    }
}
