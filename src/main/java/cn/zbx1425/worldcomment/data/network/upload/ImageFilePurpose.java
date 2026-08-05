package cn.zbx1425.worldcomment.data.network.upload;

public enum ImageFilePurpose {

    /** If the user wants a separate ARCHIVE quality, then SOURCE=ARCHIVE, otherwise SOURCE=DETAIL=(ARCHIVE). */
    SOURCE("src", ""),
    /** If the user wants a separate ARCHIVE quality, then MEDIUM=DETAIL, or unused. */
    MEDIUM("mid", ".mid"),
    THUMBNAIL("thumb", ".thumb");

    private final String fileTag;
    private final String dotFileTag;

    ImageFilePurpose(String fileTag, String dotFileTag) {
        this.fileTag = fileTag;
        this.dotFileTag = dotFileTag;
    }

    /** Returns "src", "mid", or "thumb" for use in {variant} */
    public String fileTag() {
        return fileTag;
    }

    /** Returns "", ".mid", or ".thumb" for use in {.variant} (source has no suffix) */
    public String dotFileTag() {
        return dotFileTag;
    }
}
