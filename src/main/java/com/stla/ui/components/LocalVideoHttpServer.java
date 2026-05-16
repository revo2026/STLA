package com.stla.ui.components;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;

/**
 * Serves a local video file over HTTP so JavaFX MediaPlayer can stream it (file:// often fails on Windows).
 */
public final class LocalVideoHttpServer {

    private static HttpServer server;
    private static Path currentFile;
    private static String contentType = "video/mp4";

    private LocalVideoHttpServer() {}

    public static synchronized String serve(Path file, String mime) throws IOException {
        stop();
        currentFile = file;
        contentType = mime != null && !mime.isBlank() ? mime : "video/mp4";

        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/play", LocalVideoHttpServer::handle);
        server.setExecutor(Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "local-video-http");
            t.setDaemon(true);
            return t;
        }));
        server.start();

        int port = server.getAddress().getPort();
        String url = "http://127.0.0.1:" + port + "/play";
        System.out.println("[IntroVideo] Local HTTP server → " + url + " (" + file.getFileName() + ")");
        return url;
    }

    public static synchronized void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
        currentFile = null;
    }

    private static void handle(HttpExchange ex) throws IOException {
        if (currentFile == null || !Files.exists(currentFile)) {
            ex.sendResponseHeaders(404, -1);
            ex.close();
            return;
        }

        if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
            ex.sendResponseHeaders(405, -1);
            ex.close();
            return;
        }

        long fileLength = Files.size(currentFile);
        String range = ex.getRequestHeaders().getFirst("Range");

        ex.getResponseHeaders().set("Content-Type", contentType);
        ex.getResponseHeaders().set("Accept-Ranges", "bytes");
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");

        if (range != null && range.startsWith("bytes=")) {
            long start = 0;
            long end = fileLength - 1;
            try {
                String spec = range.substring(6).trim();
                int dash = spec.indexOf('-');
                if (dash >= 0) {
                    if (!spec.substring(0, dash).isEmpty()) {
                        start = Long.parseLong(spec.substring(0, dash));
                    }
                    if (dash < spec.length() - 1 && !spec.substring(dash + 1).isEmpty()) {
                        end = Long.parseLong(spec.substring(dash + 1));
                    }
                }
            } catch (NumberFormatException ignored) {
                start = 0;
                end = fileLength - 1;
            }
            if (start > end || start >= fileLength) {
                ex.sendResponseHeaders(416, -1);
                ex.close();
                return;
            }
            end = Math.min(end, fileLength - 1);
            long chunkLen = end - start + 1;

            ex.getResponseHeaders().set("Content-Range", "bytes " + start + "-" + end + "/" + fileLength);
            ex.sendResponseHeaders(206, chunkLen);

            try (InputStream in = Files.newInputStream(currentFile);
                 OutputStream out = ex.getResponseBody()) {
                in.skipNBytes(start);
                transferBytes(in, out, chunkLen);
            }
        } else {
            ex.sendResponseHeaders(200, fileLength);
            try (InputStream in = Files.newInputStream(currentFile);
                 OutputStream out = ex.getResponseBody()) {
                in.transferTo(out);
            }
        }
        ex.close();
    }

    private static void transferBytes(InputStream in, OutputStream out, long maxBytes) throws IOException {
        byte[] buf = new byte[8192];
        long remaining = maxBytes;
        while (remaining > 0) {
            int toRead = (int) Math.min(buf.length, remaining);
            int read = in.read(buf, 0, toRead);
            if (read < 0) break;
            out.write(buf, 0, read);
            remaining -= read;
        }
    }
}
