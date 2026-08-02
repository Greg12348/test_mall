package com.libo.mall.gateway.integration;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiGatewayRouteIT {

    private static final HttpServer PRODUCT_SERVER = server("product");
    private static final HttpServer ORDER_SERVER = server("order");

    @LocalServerPort
    private int gatewayPort;

    @DynamicPropertySource
    static void downstreamProperties(DynamicPropertyRegistry registry) {
        registry.add("PRODUCT_SERVICE_URL",
                () -> "http://localhost:" + PRODUCT_SERVER.getAddress().getPort());
        registry.add("ORDER_SERVICE_URL",
                () -> "http://localhost:" + ORDER_SERVER.getAddress().getPort());
    }

    @AfterAll
    static void stopServers() {
        PRODUCT_SERVER.stop(0);
        ORDER_SERVER.stop(0);
    }

    @Test
    void shouldRouteProductRequestAndStripApiPrefix() throws Exception {
        HttpResponse<String> response = get("/api/products/7");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"source\":\"product\""));
        assertTrue(response.body().contains("\"path\":\"/products/7\""));
    }

    @Test
    void shouldRouteOrderRequestAndStripApiPrefix() throws Exception {
        HttpResponse<String> response = get("/api/orders/9");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"source\":\"order\""));
        assertTrue(response.body().contains("\"path\":\"/orders/9\""));
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + gatewayPort + path))
                .GET()
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static HttpServer server(String source) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/", exchange -> respond(exchange, source));
            server.start();
            return server;
        } catch (IOException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private static void respond(HttpExchange exchange, String source) throws IOException {
        String body = "{\"source\":\"" + source + "\",\"path\":\""
                + exchange.getRequestURI().getPath() + "\"}";
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
