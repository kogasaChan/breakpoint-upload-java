package org.bin.breakpointuploadjava;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UploadManager {
    private static final Map<String, FileUploadInfo> uploadMap = new ConcurrentHashMap<>();

    public static FileUploadInfo getOrCreateUploadInfo(String fileMD5, int totalChunks) {
        return uploadMap.computeIfAbsent(fileMD5, k -> new FileUploadInfo(totalChunks));
    }

    public static FileUploadInfo getUploadInfo(String fileMD5) {
        return uploadMap.get(fileMD5);
    }

    public static void removeUploadInfo(String fileMD5) {
        uploadMap.remove(fileMD5);
    }
}
