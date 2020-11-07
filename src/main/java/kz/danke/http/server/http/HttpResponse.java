package kz.danke.http.server.http;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HttpResponse {
    private static final String NEW_LINE = "\r\n";

    private Map<String, String> headers;
    private String body;
    private int rawStatusCode = 200;
    private String status = "OK";

    public HttpResponse() {
        this.headers = new ConcurrentHashMap<>();
        this.headers.put("Server", "http-server");
        this.headers.put("Connection", "Close");
    }

    public void addHeader(String key, String value) {
        this.headers.put(key, value);
    }

    public void addHeaders(Map<String, String> headers) {
        this.headers.putAll(headers);
    }

    public String message() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("HTTP/1.1 ")
                .append(rawStatusCode)
                .append(" ")
                .append(status)
                .append(NEW_LINE);

        headers.forEach((key, value) -> buffer.append(key)
                .append(": ")
                .append(value)
                .append(NEW_LINE)
        );

        return buffer
                .append(NEW_LINE)
                .append(body)
                .toString();
    }

    public byte[] getBytes() {
        return message().getBytes();
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.headers.put("Content-Length", String.valueOf(body.length()));
        this.body = body;
    }

    public int getRawStatusCode() {
        return rawStatusCode;
    }

    public void setRawStatusCode(int statusCode) {
        this.rawStatusCode = statusCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
