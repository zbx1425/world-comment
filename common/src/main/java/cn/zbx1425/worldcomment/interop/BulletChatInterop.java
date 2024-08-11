package cn.zbx1425.worldcomment.interop;

import cn.zbx1425.worldcomment.data.CommentEntry;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class BulletChatInterop {

    private static Method addMessageMethod;
    private static Object instance;

    static {
        try {
            Class<?> bulletComponentClass = Class.forName("com.lnatit.bchat.components.BulletComponent");
            Field instanceField = bulletComponentClass.getField("INSTANCE");
            addMessageMethod = bulletComponentClass.getMethod("addMessage", String.class, String.class);
            instance = instanceField.get(null);
        } catch (Exception e) {
            instance = null;
        }
    }

    public static boolean isInstalled() {
        return instance != null;
    }

    public static void addMessage(String message, String sender) {
        if (instance != null) {
            try {
                addMessageMethod.invoke(instance, message, sender);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void addMessage(CommentEntry comment) {
        if (comment.message.isEmpty()) return;
        addMessage(comment.message, comment.initiatorName);
    }
}
