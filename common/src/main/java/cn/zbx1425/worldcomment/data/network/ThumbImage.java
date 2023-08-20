package cn.zbx1425.worldcomment.data.network;

import com.google.gson.JsonObject;

public class ThumbImage {

    public final String url;
    public final String thumbUrl;

    public static final ThumbImage NONE = new ThumbImage("", "");

    public ThumbImage(String url, String thumbUrl) {
        this.url = url;
        this.thumbUrl = thumbUrl;
    }

    public ThumbImage(JsonObject json) {
        this.url = json.get("url").getAsString();
        this.thumbUrl = json.get("thumb").getAsString();
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("url", url);
        json.addProperty("thumb", thumbUrl);
        return json;
    }
}
