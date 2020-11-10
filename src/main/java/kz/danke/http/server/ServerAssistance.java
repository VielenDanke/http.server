package kz.danke.http.server;

public class ServerAssistance {

    public static HostPortConfig setPortHostIfEmpty(String host, Integer port) {
        if (port == null) {
            port = 0;
        }
        if (host == null || host.isBlank()) {
            host = "localhost";
        }
        return new HostPortConfig(host, port);
    }
}
