package cn.zbx1425.worldcomment.data.client;

import cn.zbx1425.worldcomment.Main;
import cn.zbx1425.worldcomment.data.CommentEntry;
import cn.zbx1425.worldcomment.data.network.ImageDownload;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ClientRayPicking {

    public static Set<BlockPos> blocksOnRay = new HashSet<>();
    public static List<CommentEntry> visibleComments = new ArrayList<>();
    public static List<CommentEntry> pickedComments = new ArrayList<>();
    public static int overlayOffset;

    private static long lastTickTime = 0;

    public static void tick(float partialTicks, float hitDistance) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) return;
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastTickTime < 100) return;
        lastTickTime = currentTime;

        ImageDownload.purgeUnused();
        blocksOnRay.clear();
        HitResult hitResult = minecraft.player.pick(hitDistance, partialTicks, true);
        BlockPos hitPos = null;
        if (hitResult.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHitResult = (BlockHitResult)minecraft.hitResult;
            hitPos = blockHitResult.getBlockPos().relative(blockHitResult.getDirection());
        } else if (hitResult.getType() == HitResult.Type.ENTITY) {
            EntityHitResult entityHitResult = (EntityHitResult)minecraft.hitResult;
            hitPos = entityHitResult.getEntity().blockPosition();
        }
        if (hitPos != null) {
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        blocksOnRay.add(hitPos.offset(x, y, z));
                    }
                }
            }
        }
        visibleComments.clear();
        pickedComments.clear();
        for (List<CommentEntry> region : ClientDatabase.INSTANCE.regions.values()) {
            for (CommentEntry comment : region) {
                boolean isVisible = comment.messageType >= 4
                        || minecraft.player.getMainHandItem().is(Main.ITEM_COMMENT_TOOL.get());
                if (isVisible) {
                    visibleComments.add(comment);
                    if (blocksOnRay.contains(comment.location)) {
                        pickedComments.add(comment);
                    }
                }
            }
        }
        overlayOffset = Math.min(overlayOffset, Math.max(pickedComments.size() - 1, 0));
    }
}
