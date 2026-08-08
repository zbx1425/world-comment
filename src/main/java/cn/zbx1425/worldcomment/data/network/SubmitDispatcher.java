package cn.zbx1425.worldcomment.data.network;

import cn.zbx1425.worldcomment.Main;
import cn.zbx1425.worldcomment.MainClient;
import cn.zbx1425.worldcomment.data.CommentEntry;
import cn.zbx1425.worldcomment.data.network.upload.CommentAffinityInfo;
import cn.zbx1425.worldcomment.data.network.upload.ImageUploadOrchestrator;
import cn.zbx1425.worldcomment.data.network.upload.ImageUploader;
import cn.zbx1425.worldcomment.network.PacketEntryCreateC2S;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.concurrent.CompletionException;
import java.util.function.Consumer;

public class SubmitDispatcher {

    private static final Long2ObjectMap<SubmitJob> pendingJobs = new Long2ObjectOpenHashMap<>();

    public static long addJob(CommentEntry comment, byte[] imageBytes, Consumer<SubmitStageEvent> callback) {
        SubmitJob job = new SubmitJob(comment, imageBytes, callback, MainClient.CLIENT_CONFIG);
        addJob(comment.id, job);
        return comment.id;
    }

    private static void addJob(long jobId, SubmitJob job) {
        synchronized (pendingJobs) {
            pendingJobs.put(jobId, job);
        }
        if (job.imageBytes != null) {
            ImageUploader uploader = job.uploaderToUse.poll();
            if (uploader == null) throw new IllegalStateException("All uploads failed");
            ImageUploadOrchestrator.upload(
                    job.imageBytes, uploader,
                    MainClient.CLIENT_CONFIG.serverIssuedConfig.imageVariants,
                    new CommentAffinityInfo(job.comment),
                    jobId
            )
                    .thenAccept(outcome -> {
                        job.acceptUploadOutcome(outcome);
                        trySendPackage(jobId);
                    })
                    .exceptionally(ex -> {
                        Main.LOGGER.error("Upload Image", ex);
                        if (job.callback != null) job.callback.accept(new SubmitStageEvent.UploaderFailed(unwrapCause(ex)));
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

    public static boolean placeJobAtSnapping(long jobId, BlockPos blockPos, Level level) {
        BlockPos groundPos = null;
        final int MAX_GROUND_DISTANCE = 3;
        for (int i = 0; i < MAX_GROUND_DISTANCE; i++) {
            BlockPos testGroundPos = blockPos.below(i + 1);
            if (level.isLoaded(testGroundPos) && !level.getBlockState(testGroundPos).isAir()) {
                groundPos = testGroundPos;
                break;
            }
        }
        synchronized (pendingJobs) {
            if (!pendingJobs.containsKey(jobId)) return true;
            if (groundPos != null) {
                pendingJobs.get(jobId).setLocation(groundPos.above());
            } else {
                pendingJobs.get(jobId).setLocation(blockPos);
            }
            trySendPackage(jobId);
        }
        return groundPos != null;
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
            if (job.callback != null) job.callback.accept(new SubmitStageEvent.Sent(job.uploadWarnings()));
            removeJob(jobId);
        } else {
            if (job.imageBytes != null && !job.imageReady) {
                if (job.callback != null) job.callback.accept(new SubmitStageEvent.WaitingForUpload());
            }
        }
    }

    private static Throwable unwrapCause(Throwable ex) {
        return (ex instanceof CompletionException cex && cex.getCause() != null)
                ? cex.getCause() : ex;
    }
}
