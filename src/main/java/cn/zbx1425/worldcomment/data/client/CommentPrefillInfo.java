package cn.zbx1425.worldcomment.data.client;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public class CommentPrefillInfo {

    public UUID initiator;
    public String initiatorName;

    public boolean unlisted;
    public BlockPos imageLocation;
    public byte[] imagePngBytes;
    public long overrideTimestamp = -1;

    public CommentPrefillInfo(Player initiator, BlockPos imageLocation, byte[] imagePngBytes) {
        this.initiator = initiator.getGameProfile().id();
        this.initiatorName = initiator.getGameProfile().name();
        this.imageLocation = imageLocation;
        this.imagePngBytes = imagePngBytes;
    }

}
