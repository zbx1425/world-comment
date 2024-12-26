package cn.zbx1425.worldcomment.interop;

import cn.zbx1425.worldcomment.data.CommentEntry;

import java.lang.reflect.Method;

public class BulletChatInterop {

    private static Method addMessageMethod;

    static {
        try {
            Class<?> bulletComponentClass = Class.forName("com.lnatit.bchat.compat.WorldCommentCompat");
            addMessageMethod = bulletComponentClass.getMethod("addWorldCommentMessage", CommentEntry.class);
        } catch (Exception e) {
            addMessageMethod = null;
        }
    }

    public static boolean isInstalled() {
        return addMessageMethod != null;
    }

    public static void addMessage(CommentEntry comment) {
        if (addMessageMethod != null) {
            try {
                addMessageMethod.invoke(null, comment);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
