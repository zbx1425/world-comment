package cn.zbx1425.worldcomment.data.network;

import cn.zbx1425.worldcomment.Main;
import cn.zbx1425.worldcomment.MainClient;
import cn.zbx1425.worldcomment.data.CommentEntry;
import cn.zbx1425.worldcomment.data.ServerWorldData;
import cn.zbx1425.worldcomment.data.network.upload.ImageUploader;
import cn.zbx1425.worldcomment.data.network.upload.ImageUploadConfig;
import cn.zbx1425.worldcomment.network.PacketEntryCreateC2S;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class SubmitDispatcher {

    private static final Long2ObjectMap<SubmitJob> pendingJobs = new Long2ObjectOpenHashMap<>();

    public static long addJob(CommentEntry comment, byte[] imageBytes, BiConsumer<SubmitJob, Throwable> callback) {
        long jobId = ServerWorldData.SNOWFLAKE.nextId();
        SubmitJob job = new SubmitJob(comment, imageBytes, callback, MainClient.CLIENT_CONFIG);
        addJob(jobId, job);
        return jobId;
    }

    private static void addJob(long jobId, SubmitJob job) {
        pendingJobs.put(jobId, job);
        if (job.imageBytes != null) {
                ImageUploader uploader = job.uploaderToUse.poll();
                if (uploader == null) throw new IllegalStateException("All uploads failed");
                uploader.uploadImage(job.imageBytes, job.comment)
                        .thenAccept(thumbImage -> {
                            job.setImage(thumbImage);
                            trySendPackage(jobId);
                        })
                        .exceptionally(ex -> {
                            Main.LOGGER.error("Upload Image", ex);
                            if (job.callback != null) job.callback.accept(job, ex);
                            if (job.uploaderToUse.isEmpty()) {
                                removeJob(jobId);
                            } else {
                                addJob(jobId, job);
                            }
                            return null;
                        });
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
            if (job.callback != null) job.callback.accept(null, null);
            removeJob(jobId);
        } else {
            if (job.imageBytes != null && !job.imageReady) {
                if (job.callback != null) job.callback.accept(job, null);
            }
        }
    }
}
