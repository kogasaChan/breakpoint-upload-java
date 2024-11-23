package org.bin.breakpointuploadjava;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/upload/status")
public class UploadStatusServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String fileMD5 = req.getParameter("fileMD5");

        if (fileMD5 == null || fileMD5.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Missing fileMD5 parameter");
            return;
        }

        boolean[] uploadInfo = UploadManager.getUploadInfo(fileMD5).getChunksReceived();

        if (uploadInfo == null) {
            resp.getWriter().write("Invalid fileMD5");
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(uploadInfo);

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(json);
    }
}
