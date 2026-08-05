package cn.zbx1425.worldcomment.network;

public class ChunkCollector {
    private final byte[] completeData;
    private int receivedChunks = 0;
    private final int totalChunks;
    private final long timestamp;

    public ChunkCollector(int totalSize, int totalChunks, long timestamp) {
        this.completeData = new byte[totalSize];
        this.totalChunks = totalChunks;
        this.timestamp = timestamp;
    }

    public ChunkCollector(int totalSize, int totalChunks) {
        this(totalSize, totalChunks, 0);
    }

    public boolean addChunk(int chunkIndex, byte[] chunkData, int offset) {
        System.arraycopy(chunkData, 0, completeData, offset, chunkData.length);
        receivedChunks++;
        return receivedChunks == totalChunks;
    }

    public byte[] getCompleteData() {
        return completeData;
    }

    public long getTimestamp() {
        return timestamp;
    }
} 