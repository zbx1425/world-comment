package cn.zbx1425.worldcomment.data;

import java.util.Locale;

public class CommentCommand {

    public static boolean isCommand(CommentEntry comment) {
        return comment.message.startsWith("$SNCMD:");
    }

    public static void executeCommand(CommentEntry comment, ServerWorldData worldData) {
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
}
