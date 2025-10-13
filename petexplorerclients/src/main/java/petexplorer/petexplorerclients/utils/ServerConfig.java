package petexplorer.petexplorerclients.utils;

public final class ServerConfig {

    public static final String BASE_IP = "10.0.2.2";
    public static final String BASE_PORT = "8080";

    public static final String BASE_URL = "http://" + BASE_IP + ":" + BASE_PORT;

    public static final String WS_URL = "ws://" + BASE_IP + ":" + BASE_PORT + "/ws-stomp/websocket";

    public static final String API_URL = "http://" + BASE_IP + ":" + BASE_PORT + "/api/";
}
