package cn.zbx1425.worldcomment.util;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.List;

public class FrameTask {

    private static final List<FrameTaskEntry> TASKS = new ObjectArrayList<>();

    public static void enqueue(Runnable runnable, int afterFrames) {
        TASKS.add(new FrameTaskEntry(runnable, afterFrames + 1));
    }

    public static void onFrameFinished() {
        for (var it = TASKS.iterator(); it.hasNext(); ) {
            var entry = it.next();
            entry.afterFrames--;
            if (entry.afterFrames == 0) {
                entry.runnable.run();
                it.remove();
            }
        }
    }

    private static class FrameTaskEntry {
        final Runnable runnable;
        int afterFrames;

        public FrameTaskEntry(Runnable runnable, int afterFrames) {
            this.runnable = runnable;
            this.afterFrames = afterFrames;
        }
    }
}
