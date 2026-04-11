package cn.zbx1425.worldcomment.data.client;

import cn.zbx1425.worldcomment.Main;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class EmojiRegistry implements ResourceManagerReloadListener {

    public static final Identifier ATLAS_TEXTURE_ID = Identifier.fromNamespaceAndPath(Main.MOD_ID, "textures/atlas/emoji.png");
    public static final Identifier ATLAS_ID = Identifier.fromNamespaceAndPath(Main.MOD_ID, "emoji");

    private TextureAtlas atlas;
    private TextureAtlasSprite[] sprites;

    public static final EmojiRegistry INSTANCE = new EmojiRegistry();

    @Override
    public void onResourceManagerReload(@NonNull ResourceManager resourceManager) {
        AbstractTexture emojiAtlasATex = Minecraft.getInstance().getTextureManager().getTexture(ATLAS_TEXTURE_ID);
        if (!(emojiAtlasATex instanceof TextureAtlas emojiAtlas)) throw new IllegalStateException("Emoji atlas texture is not a TextureAtlas");
        atlas = emojiAtlas;

        List<TextureAtlasSprite> spriteList = new ObjectArrayList<>();
        for (int spriteId = 0; ; spriteId++) {
            TextureAtlasSprite sprite = emojiAtlas.getSprite(Identifier.fromNamespaceAndPath(Main.MOD_ID,
                String.format("emoji/id_%03d", spriteId)));
            if (sprite != emojiAtlas.missingSprite()) {
                spriteList.add(sprite);
            } else {
                break;
            }
        }
        sprites = spriteList.toArray(new TextureAtlasSprite[0]);
    }

    public TextureAtlasSprite getSprite(int id) {
        if (id >= sprites.length) return atlas.missingSprite();
        return sprites[id];
    }

    public int getSpriteCount() {
        return sprites.length;
    }
}
