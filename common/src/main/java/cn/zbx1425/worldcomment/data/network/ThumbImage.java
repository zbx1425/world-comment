package cn.zbx1425.worldcomment.data.network;

public class ThumbImage {

    public final String url;
    public final String thumbUrl;

    public static final ThumbImage NONE = new ThumbImage("", "");

    public ThumbImage(String url, String thumbUrl) {
        this.url = url;
        this.thumbUrl = thumbUrl;
    }
}
