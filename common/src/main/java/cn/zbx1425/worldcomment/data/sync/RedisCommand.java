package cn.zbx1425.worldcomment.data.sync;

public class RedisCommand {

    public static final String COMMAND_CHANNEL = "WORLD_COMMENT_COMMAND_CHANNEL";

    public static final String HMAP_ALL_ID = "WORLD_COMMENT_DATA_ALL";

    public static String Update(String content) {
        return "UPDATE#" + content;
    }

    public static String Insert(String content) {
        return "INSERT#" + content;
    }

    public static String getAction(String rawCommand) {
        return rawCommand.substring(0, rawCommand.indexOf('#'));
    }

    public static String getContent(String rawCommand) {
        return rawCommand.substring(rawCommand.indexOf('#') + 1);
    }

}
