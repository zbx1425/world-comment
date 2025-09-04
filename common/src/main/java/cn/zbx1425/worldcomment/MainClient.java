package cn.zbx1425.worldcomment;

import cn.zbx1425.worldcomment.data.client.ClientWorldData;
import cn.zbx1425.worldcomment.interop.BulletChatInterop;
import cn.zbx1425.worldcomment.network.*;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class MainClient {

	public static ClientConfig CLIENT_CONFIG = new ClientConfig();

	public static void init() {
		ClientWorldData.INSTANCE.proximityCommentSet.onCommentApproach = (comment -> {
			BulletChatInterop.addMessage(comment);
		});

		ClientPlatform.registerNetworkReceiver(
				PacketRegionDataS2C.IDENTIFIER, PacketRegionDataS2C.ClientLogics::handle);
		ClientPlatform.registerNetworkReceiver(
				PacketCollectionDataS2C.IDENTIFIER, PacketCollectionDataS2C.ClientLogics::handle);
		ClientPlatform.registerNetworkReceiver(
				PacketEntryUpdateS2C.IDENTIFIER, PacketEntryUpdateS2C.ClientLogics::handle);
		ClientPlatform.registerNetworkReceiver(
				PacketClientConfigS2C.IDENTIFIER, PacketClientConfigS2C.ClientLogics::handle);
		ClientPlatform.registerNetworkReceiver(
				PacketImageUploadS2C.IDENTIFIER, PacketImageUploadS2C.ClientLogics::handle);
		ClientPlatform.registerNetworkReceiver(
				PacketImageDownloadS2C.IDENTIFIER, PacketImageDownloadS2C.ClientLogics::handle);

		ClientPlatform.registerPlayerJoinEvent(ignored -> {
			ClientWorldData.INSTANCE.clear();
		});
	}

}
