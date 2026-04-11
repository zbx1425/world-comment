package cn.zbx1425.worldcomment.data.client;

import cn.zbx1425.worldcomment.MainClient;
import cn.zbx1425.worldcomment.data.CommentEntry;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class ClientRayPicking {

    public static Map<BlockPos, List<CommentEntry>> visibleComments = new Object2ObjectArrayMap<>();
    public static int nearbyCommentsCount = 0;
    public static List<CommentEntry> pickedComments = new ArrayList<>();
    public static int overlayOffset;

    public static void tick(float partialTicks, float hitDistance) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) return;

        Vec3 pickStart = minecraft.player.getEyePosition(partialTicks);
        Vec3 pickDir = minecraft.player.getViewVector(partialTicks);
        Vec3 pickEnd = pickStart.add(pickDir.x * hitDistance, pickDir.y * hitDistance, pickDir.z * hitDistance);
        HitResult hitResult = minecraft.player.pick(hitDistance, partialTicks, true);
        double vanillaDistSqr = hitResult.getType() == HitResult.Type.MISS
                ? 65472 : hitResult.getLocation().distanceToSqr(pickStart);

        int nearbyCommentsCount = 0;
        visibleComments.clear();
        pickedComments.clear();
        for (Map<BlockPos, List<CommentEntry>> region : ClientWorldData.INSTANCE.regions.values()) {
            for (Map.Entry<BlockPos, List<CommentEntry>> blockData : region.entrySet()) {
                nearbyCommentsCount += blockData.getValue().size();
                BlockPos bp = blockData.getKey();
                AABB blockHitArea = new AABB(bp.getX(), bp.getY(), bp.getZ(),
                        bp.getX() + 1, bp.getY() + 3, bp.getZ() + 1);
                Frustum cullingFrustum = minecraft.gameRenderer.getMainCamera().getCullFrustum();
                if (!cullingFrustum.isVisible(blockHitArea)) continue;
                if (minecraft.level.isLoaded(bp) && minecraft.level.getBlockState(bp.below()).isAir()) continue;
                Optional<Vec3> clipPos = blockHitArea.clip(pickStart, pickEnd);
                boolean isPicked = clipPos.isPresent() && clipPos.get().distanceToSqr(pickStart) < vanillaDistSqr;
                for (CommentEntry comment : blockData.getValue()) {
                    boolean isVisible = MainClient.CLIENT_CONFIG.isCommentVisible(minecraft, comment);
                    if (isVisible) {
                        visibleComments.computeIfAbsent(comment.location, ignored -> new ArrayList<>()).add(comment);
                        if (isPicked) {
                            pickedComments.add(comment);
                        }
                    }
                }
            }
        }
        overlayOffset = Math.clamp(pickedComments.size() - 1, 0, overlayOffset);
        ClientRayPicking.nearbyCommentsCount = nearbyCommentsCount;
    }
}
