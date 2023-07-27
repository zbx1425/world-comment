package cn.zbx1425.worldcomment.data.network;

import cn.zbx1425.worldcomment.Main;
import cn.zbx1425.worldcomment.data.CommentEntry;
import cn.zbx1425.worldcomment.data.persist.Database;
import cn.zbx1425.worldcomment.network.PacketEntryCreateC2S;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class SubmitDispatcher {

    private static final Executor NETWORK_EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Long2ObjectMap<SubmitJob> pendingJobs = new Long2ObjectOpenHashMap<>();

    public static long addJob(CommentEntry comment, Path imagePath, Consumer<SubmitJob> callback) {
        synchronized (pendingJobs) {
            long jobId = Database.SNOWFLAKE.nextId();
            SubmitJob job = new SubmitJob(comment, imagePath, callback);
            pendingJobs.put(jobId, job);
            if (imagePath != null) {
                NETWORK_EXECUTOR.execute(() -> {
                    try {
                        job.setImage(ImageUpload.uploadImage(imagePath, 256));
                        trySendPackage(jobId);
                    } catch (Exception ex) {
                        job.exception = ex;
                        if (job.callback != null) job.callback.accept(job);
                        Main.LOGGER.error("Upload Image", ex);
                        removeJob(jobId);
                    } finally {
                        try {
                            Files.deleteIfExists(imagePath);
                        } catch (IOException ex) {
                            Main.LOGGER.error("Delete image file", ex);
                        }
                    }
                });
            }
            return jobId;
        }
    }

    public static void placeJobAt(long jobId, BlockPos blockPos) {
        synchronized (pendingJobs) {
            if (!pendingJobs.containsKey(jobId)) return;
            pendingJobs.get(jobId).setLocation(blockPos);
            trySendPackage(jobId);
        }
    }

    public static void removeJob(long jobId) {
        synchronized (pendingJobs) {
            pendingJobs.remove(jobId);
        }
    }

    private static void trySendPackage(long jobId) {
        SubmitJob job = pendingJobs.get(jobId);
        if (job.isReady()) {
            PacketEntryCreateC2S.ClientLogics.send(job.comment);
            if (job.callback != null) job.callback.accept(null);
            removeJob(jobId);
        } else {
            if (job.imagePath != null && !job.imageReady) {
                if (job.callback != null) job.callback.accept(job);
            }
        }
    }
}
