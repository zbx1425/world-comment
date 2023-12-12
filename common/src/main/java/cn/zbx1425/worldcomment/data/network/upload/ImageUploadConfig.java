package cn.zbx1425.worldcomment.data.network.upload;

public class ImageUploadConfig {

    public String service;
    public String config;

    public ImageUploadConfig(String configStr) {
        int firstHash = configStr.indexOf(':');
        this.service = configStr.substring(0, firstHash).trim();
        this.config = configStr.substring(firstHash + 1).trim();
    }

    @Override
    public String toString() {
        return service + ":" + config;
    }
}
