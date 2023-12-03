package cn.zbx1425.worldcomment.data;

import cn.zbx1425.worldcomment.data.network.ImageDump;

import java.util.Locale;

public class CommentCommand {

    public static boolean isCommand(CommentEntry comment) {
        return comment.message.startsWith("$SNCMD:");
    }

    public static void executeCommandServer(CommentEntry comment, ServerWorldData worldData) {
        if (!isCommand(comment)) return;
        String commandContent = comment.message.substring(7);
        String command = commandContent.split(" ")[0].toLowerCase(Locale.ROOT);
        String[] args = commandContent.substring(command.length()).trim().split(" ");
        switch (command) {
            case "uplinksendall" -> {
                for (CommentEntry commentEntry : worldData.comments.timeIndex.values()) {
                    worldData.uplinkDispatcher.insert(commentEntry);
                }
            }
        }
    }

    public static void executeCommandClient(CommentEntry comment) {
        if (!isCommand(comment)) return;
        String commandContent = comment.message.substring(7);
        String command = commandContent.split(" ")[0].toLowerCase(Locale.ROOT);
        String[] args = commandContent.substring(command.length()).trim().split(" ");
        switch (command) {
            case "imagedumpall" -> {
                if (args.length < 1) return;
                ImageDump.requestDumpComments(args[0]);
            }
        }
    }
}
