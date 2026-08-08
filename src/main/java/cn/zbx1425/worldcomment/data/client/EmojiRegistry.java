package cn.zbx1425.worldcomment.data.client;

import cn.zbx1425.worldcomment.Main;
import cn.zbx1425.worldcomment.data.CommentEntry;
import cn.zbx1425.worldcomment.item.CommentToolItem;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.jspecify.annotations.NonNull;

public class EmojiRegistry implements ResourceManagerReloadListener {

    public static final Identifier ATLAS_TEXTURE_ID = Identifier.fromNamespaceAndPath(Main.MOD_ID, "textures/atlas/emoji.png");
    public static final Identifier ATLAS_ID = Identifier.fromNamespaceAndPath(Main.MOD_ID, "emoji");

    public static final int HIGH_EMOJI_BASE_ID = CommentEntry.HIGH_EMOJI_BASE_ID;

    private TextureAtlas atlas;
    private Int2ObjectMap<TextureAtlasSprite> sprites = new Int2ObjectOpenHashMap<>();
    private int[] spriteIds;
    private int[] markerSpriteIds;
    private TextureAtlasSprite poleSprite;

    public static final EmojiRegistry INSTANCE = new EmojiRegistry();

    @Override
    public void onResourceManagerReload(@NonNull ResourceManager resourceManager) {
        AbstractTexture emojiAtlasATex = Minecraft.getInstance().getTextureManager().getTexture(ATLAS_TEXTURE_ID);
        if (!(emojiAtlasATex instanceof TextureAtlas emojiAtlas)) throw new IllegalStateException("Emoji atlas texture is not a TextureAtlas");
        atlas = emojiAtlas;

        sprites.clear();
        sprites.put(0, atlas.missingSprite());

        IntList spriteIdList = new IntArrayList();
        for (int spriteId = 1; ; spriteId++) {
            TextureAtlasSprite sprite = emojiAtlas.getSprite(Identifier.fromNamespaceAndPath(Main.MOD_ID,
                String.format("emoji/id_%03d", spriteId)));
            if (sprite != emojiAtlas.missingSprite()) {
                sprites.put(spriteId, sprite);
                spriteIdList.add(spriteId);
            } else {
                break;
            }
        }
        spriteIds = spriteIdList.toIntArray();
        IntList markerIdList = new IntArrayList();
        for (int spriteId = HIGH_EMOJI_BASE_ID; ; spriteId++) {
            TextureAtlasSprite sprite = emojiAtlas.getSprite(Identifier.fromNamespaceAndPath(Main.MOD_ID,
                String.format("emoji/id_%05d", spriteId)));
            if (sprite != emojiAtlas.missingSprite()) {
                sprites.put(spriteId, sprite);
                markerIdList.add(spriteId);
            } else {
                break;
            }
        }
        markerSpriteIds = markerIdList.toIntArray();

        poleSprite = emojiAtlas.getSprite(Identifier.fromNamespaceAndPath(Main.MOD_ID, "entity/comment_pole"));
    }

    public TextureAtlasSprite getSprite(int id) {
        return sprites.getOrDefault(id, atlas.missingSprite());
    }

    public int[] getSpriteIds() {
        return spriteIds;
    }

    public int[] getMarkerSpriteIds() {
        return markerSpriteIds;
    }

    public TextureAtlasSprite getPoleSprite() {
        return poleSprite;
    }
}
