package cn.zbx1425.worldcomment.data.client;

import cn.zbx1425.worldcomment.MainClient;
import cn.zbx1425.worldcomment.data.CommentEntry;
import cn.zbx1425.worldcomment.render.CommentWorldRenderer;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector2f;

import java.util.*;

public class ClientRayPicking {

    public static Map<BlockPos, List<CommentAndSituation>> visibleComments = new Object2ObjectArrayMap<>();
    public static int nearbyCommentsCount = 0;
    public static List<CommentEntry> pickedComments = new ArrayList<>();
    public static int overlayOffset;

    private static final Random RANDOM = new Random();

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

                NeighborSituation neighborSituation = new NeighborSituation(bp, minecraft.level);
                if (neighborSituation.isDying) continue;

                for (CommentEntry comment : blockData.getValue()) {
                    boolean isVisible = MainClient.CLIENT_CONFIG.isCommentVisible(minecraft, comment);
                    if (isVisible) {
                        RANDOM.setSeed(comment.id);
                        Vector2f randomOff = AreaSampler.INSTANCE.sample(neighborSituation.leewayAbsences, RANDOM);
                        Optional<Vec3> clipPos = blockHitArea.move(randomOff.x - 0.5, 0, randomOff.y - 0.5).clip(pickStart, pickEnd);
                        boolean isPicked = clipPos.isPresent() && clipPos.get().distanceToSqr(pickStart) < vanillaDistSqr;

                        visibleComments.computeIfAbsent(comment.location, ignored -> new ArrayList<>())
                            .add(new CommentAndSituation(comment, isPicked, neighborSituation, randomOff));
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

    public static class NeighborSituation {

        public final boolean isDying;
        public final boolean isConfined;
        public final boolean[] leewayAbsences = new boolean[9];

        public NeighborSituation(BlockPos anchorPos, Level level) {
            isDying = level.getBlockState(anchorPos.below()).isAir()
                || !level.getBlockState(anchorPos).isAir();
            if (isDying) {
                isConfined = false; // Just init
                return;
            }
            isConfined = !level.getBlockState(anchorPos.above()).isAir()
                || !level.getBlockState(anchorPos.above(2)).isAir();
            int yOff = isConfined ? 0 : 2;
            if (!level.getBlockState(anchorPos.offset(-1, yOff, -1)).isAir()) leewayAbsences[0] = true;
            if (!level.getBlockState(anchorPos.offset(1, yOff, -1)).isAir()) leewayAbsences[2] = true;
            if (!level.getBlockState(anchorPos.offset(-1, yOff, 1)).isAir()) leewayAbsences[6] = true;
            if (!level.getBlockState(anchorPos.offset(1, yOff, 1)).isAir()) leewayAbsences[8] = true;
            if (!level.getBlockState(anchorPos.offset(0, yOff, -1)).isAir()) {
                leewayAbsences[0] = true;
                leewayAbsences[1] = true;
                leewayAbsences[2] = true;
            }
            if (!level.getBlockState(anchorPos.offset(-1, yOff, 0)).isAir()) {
                leewayAbsences[0] = true;
                leewayAbsences[3] = true;
                leewayAbsences[6] = true;
            }
            if (!level.getBlockState(anchorPos.offset(1, yOff, 0)).isAir()) {
                leewayAbsences[2] = true;
                leewayAbsences[5] = true;
                leewayAbsences[8] = true;
            }
            if (!level.getBlockState(anchorPos.offset(0, yOff, 1)).isAir()) {
                leewayAbsences[6] = true;
                leewayAbsences[7] = true;
                leewayAbsences[8] = true;
            }
        }
    }

    public static class AreaSampler {

        private static final float PADDING = 0.1f;
        private static final float INFLATION = 0.35f;

        private final AABB[] regions = new AABB[9];
        private final float[] regionAreas = new float[9];

        public static final AreaSampler INSTANCE = new AreaSampler();

        private AreaSampler() {
            regions[0] = new AABB(PADDING, 0, PADDING, INFLATION, 0, INFLATION);
            regions[1] = new AABB(INFLATION, 0, PADDING, 1 - INFLATION, 0, INFLATION);
            regions[2] = new AABB(1 - INFLATION, 0, PADDING, 1 - PADDING, 0, INFLATION);
            regions[3] = new AABB(PADDING, 0, INFLATION, INFLATION, 0, 1 - INFLATION);
            regions[4] = new AABB(INFLATION, 0, INFLATION, 1 - INFLATION, 0, 1 - INFLATION);
            regions[5] = new AABB(1 - INFLATION, 0, INFLATION, 1 - PADDING, 0, 1 - INFLATION);
            regions[6] = new AABB(PADDING, 0, 1 - INFLATION, INFLATION, 0, 1 - PADDING);
            regions[7] = new AABB(INFLATION, 0, 1 - INFLATION, 1 - INFLATION, 0, 1 - PADDING);
            regions[8] = new AABB(1 - INFLATION, 0, 1 - INFLATION, 1 - PADDING, 0, 1 - PADDING);

            for (int i = 0; i < 9; i++) {
                regionAreas[i] = (float)((regions[i].maxX - regions[i].minX) * (regions[i].maxZ - regions[i].minZ));
            }
        }

        public Vector2f sample(boolean[] regionInverseMask, Random random) {
            float totalWeight = 0;
            for (int i = 0; i < 9; i++) {
                if (!regionInverseMask[i]) totalWeight += regionAreas[i];
            }
            float needle = random.nextFloat() * totalWeight;
            int lastValidIndex = -1;
            for (int i = 0; i < 9; i++) {
                if (regionInverseMask[i]) continue;
                lastValidIndex = i;
                if (needle < regionAreas[i]) {
                    return new Vector2f(
                        (float)(regions[i].minX + random.nextFloat() * (regions[i].maxX - regions[i].minX)),
                        (float)(regions[i].minZ + random.nextFloat() * (regions[i].maxZ - regions[i].minZ))
                    );
                }
                needle -= regionAreas[i];
            }
            if (lastValidIndex != -1) {
                return new Vector2f(
                    (float)(regions[lastValidIndex].minX + random.nextFloat() * (regions[lastValidIndex].maxX - regions[lastValidIndex].minX)),
                    (float)(regions[lastValidIndex].minZ + random.nextFloat() * (regions[lastValidIndex].maxZ - regions[lastValidIndex].minZ))
                );
            }
            return new Vector2f(PADDING + random.nextFloat() * (1 - 2 * PADDING),
                PADDING + random.nextFloat() * (1 - 2 * PADDING));
        }
    }

    public static class CommentAndSituation {

        public CommentEntry commentEntry;

        public boolean picked;
        public NeighborSituation neighborSituation;
        public Vector2f renderOffset;

        public CommentAndSituation(CommentEntry commentEntry, boolean picked, NeighborSituation neighborSituation, Vector2f renderOffset) {
            this.commentEntry = commentEntry;
            this.picked = picked;
            this.neighborSituation = neighborSituation;
            this.renderOffset = renderOffset;
        }
    }
}
