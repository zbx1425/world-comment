package cn.zbx1425.worldcomment.data.sync;

public class Command {
    public static final String COMMAND_CHANNEL = "WORLD_COMMENT_COMMAND_CHANNEL";

    public static final String ALL_DATA_ID = "ALL";

    public static String DataKey(String ID) {
        return "WORLD_COMMENT_DATA#"+ID;
    }

    public static String Request(String ID) {
        return "REQUEST#"+ID;
    }

    public static String Update(String ID) {
        return "UPDATE#"+ID;
    }

    public static String Updated(String ID) {
        return "UPDATED#"+ID;
    }

    public static String Delete(String ID) {
        return "DELETE#"+ID;
    }

    public static long IsUpdated(String Command) {
        if (!Command.startsWith("UPDATED#")) {
            return 0;
        };

        return Long.parseLong(Command.substring(8));
    }

    public static long IsUpdate(String Command) {
        if (!Command.startsWith("UPDATE#")) {
            return 0;
        };

        return Long.parseLong(Command.substring(7));
    }


}
