package org.bin.breakpointuploadjava;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.*;
import java.nio.file.Files;

@WebServlet("/upload")
@MultipartConfig
public class UploadServlet extends HttpServlet {
    private static final String UPLOAD_DIRECTORY = "D:\\upload";

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String fileName = req.getParameter("fileName");
        int chunkIndex = Integer.parseInt(req.getParameter("chunkIndex"));
        int totalChunks = Integer.parseInt(req.getParameter("totalChunks"));
        String fileMD5 = req.getParameter("fileMD5");

        FileUploadInfo uploadInfo = UploadManager.getOrCreateUploadInfo(fileMD5, totalChunks);

        if (uploadInfo.isChunkReceived(chunkIndex)) {
            resp.getWriter().write("Chunk already received");
            return;
        }

        Part filePart = req.getPart("file");
        File chunkFile = new File(UPLOAD_DIRECTORY, fileName + "_" + fileMD5 + ".part" + chunkIndex);

        try (InputStream fileContent = filePart.getInputStream();
             FileOutputStream fos = new FileOutputStream(chunkFile)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fileContent.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
        }

        uploadInfo.markChunkReceived(chunkIndex);
        uploadInfo.uploadedChunksCount();

        if (uploadInfo.getUploadedChunksCount() == totalChunks) {
            mergeChunks(fileName, fileMD5, totalChunks);
            UploadManager.removeUploadInfo(fileMD5);
        }

        resp.getWriter().write("Upload successful");
    }

    private void mergeChunks(String fileName, String fileMD5, int totalChunks) {
        File mergedFile = new File(UPLOAD_DIRECTORY, fileName);

        try (FileOutputStream fos = new FileOutputStream(mergedFile, true)) {
            byte[] buffer = new byte[1024];

            for (int i = 0; i < totalChunks; i++) {
                File chunkFile = new File(UPLOAD_DIRECTORY, fileName + "_" + fileMD5 + ".part" + i);

                try (InputStream in = Files.newInputStream(chunkFile.toPath())) {
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                    }
                }

                Files.deleteIfExists(chunkFile.toPath());
            }
        } catch (IOException e) {
            throw new RuntimeException("Error merging file chunks: " + e.getMessage(), e);
        }
    }
}
