package cn.zbx1425.worldcomment.data.synchronizer;

public class Command {
    public static final String COMMAND_CHANNEL = "WORLD_COMMENT_COMMAND_CHANNEL";


    public static String DataKey(String ID) {
        if (ID.isEmpty()) {
            return "WORLD_COMMENT_DATA#ALL";
        }

        return "WORLD_COMMENT_DATA#"+ID;
    }

    public static String Request(String ID) {
        if (ID.isEmpty()) {
            return "REQUEST#ALL";
        }

        return "REQUEST#"+ID;
    }

    public static String Update(String ID) {
        return "UPDATE#"+ID;
    }

    public static String Updated(String ID) {
        return "UPDATED#"+ID;
    }

}
