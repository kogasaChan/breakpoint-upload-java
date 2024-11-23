package org.bin.breakpointuploadjava;

import java.util.concurrent.atomic.AtomicInteger;

public class FileUploadInfo {

    private final boolean[] chunksReceived;
    private final AtomicInteger uploadedChunksCount;

    public FileUploadInfo(int totalChunks) {
        this.chunksReceived = new boolean[totalChunks];
        this.uploadedChunksCount = new AtomicInteger(0);
    }

    public void markChunkReceived(int index) {
        chunksReceived[index] = true;
    }

    public boolean isChunkReceived(int index) {
        return chunksReceived[index];
    }

    public int getUploadedChunksCount() {
        return uploadedChunksCount.get();
    }

    public void uploadedChunksCount() {
        this.uploadedChunksCount.incrementAndGet();
    }

    public boolean[] getChunksReceived() {
        return chunksReceived;
    }
}