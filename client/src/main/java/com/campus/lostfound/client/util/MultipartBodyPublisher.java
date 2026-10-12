package com.campus.lostfound.client.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MultipartBodyPublisher {

    private final String boundary = "----CampusLostFoundBoundary" + UUID.randomUUID().toString().replace("-", "");
    private final List<Part> parts = new ArrayList<>();

    public static MultipartBodyPublisher newBuilder() {
        return new MultipartBodyPublisher();
    }

    public MultipartBodyPublisher addJsonPart(String name, String json) {
        parts.add(new Part(name, null, "application/json", json.getBytes(StandardCharsets.UTF_8)));
        return this;
    }

    public MultipartBodyPublisher addTextPart(String name, String value) {
        parts.add(new Part(name, null, "text/plain; charset=UTF-8", value.getBytes(StandardCharsets.UTF_8)));
        return this;
    }

    public MultipartBodyPublisher addFilePart(String name, String filename, String contentType, byte[] data) {
        parts.add(new Part(name, filename, contentType != null ? contentType : "application/octet-stream", data));
        return this;
    }

    public String getContentType() {
        return "multipart/form-data; boundary=" + boundary;
    }

    public HttpRequest.BodyPublisher build() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            for (Part part : parts) {
                out.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
                StringBuilder header = new StringBuilder();
                header.append("Content-Disposition: form-data; name=\"").append(part.name).append("\"");
                if (part.filename != null) {
                    header.append("; filename=\"").append(part.filename).append("\"");
                }
                header.append("\r\n");
                if (part.contentType != null) {
                    header.append("Content-Type: ").append(part.contentType).append("\r\n");
                }
                header.append("\r\n");
                out.write(header.toString().getBytes(StandardCharsets.UTF_8));
                out.write(part.data);
                out.write("\r\n".getBytes(StandardCharsets.UTF_8));
            }
            out.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException("Error building multipart body", e);
        }

        return HttpRequest.BodyPublishers.ofByteArray(out.toByteArray());
    }

    private static class Part {
        final String name;
        final String filename;
        final String contentType;
        final byte[] data;

        Part(String name, String filename, String contentType, byte[] data) {
            this.name = name;
            this.filename = filename;
            this.contentType = contentType;
            this.data = data;
        }
    }
}
