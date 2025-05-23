package com.hairdresser.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.InetSocketAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Executors;

public class HairdresserRestServer {
    private static final String DB_URL = "jdbc:h2:./hairdresser_db";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "";
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) throws Exception {
        // Инициализация базы данных
        DatabaseManager.getInstance().initializeDatabase();

        // Создание HTTP-сервера
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.setExecutor(Executors.newFixedThreadPool(10)); // Пул потоков для многопоточности

        // Определение REST-эндпоинтов с использованием фабрики
        HandlerFactory factory = new HandlerFactory();
        server.createContext("/api/roles", exchange -> {
            logRequest(exchange);
            factory.createHandler("roles").handle(exchange);
        });
        server.createContext("/api/users", exchange -> {
            logRequest(exchange);
            factory.createHandler("users").handle(exchange);
        });
        server.createContext("/api/serviceTypes", exchange -> {
            logRequest(exchange);
            factory.createHandler("serviceTypes").handle(exchange);
        });
        server.createContext("/api/shifts", exchange -> {
            logRequest(exchange);
            factory.createHandler("shifts").handle(exchange);
        });
        server.createContext("/api/visits", exchange -> {
            // logRequest(exchange);
            factory.createHandler("visits").handle(exchange);
        });
        server.createContext("/api/visitors", exchange -> {
            logRequest(exchange);
            factory.createHandler("visitors").handle(exchange);
        });
        server.createContext("/api/reports", exchange -> {
            logRequest(exchange);
            factory.createHandler("reports").handle(exchange);
        });
        server.createContext("/api/login", exchange -> {
            //logRequest(exchange);
            new LoginHandler().handle(exchange);
        });
        server.createContext("/api/register", exchange -> {
            //logRequest(exchange);
            new RegisterHandler().handle(exchange);
        });

        // Запуск сервера
        server.start();
        System.out.println("Сервер запущен на порту 8080");
    }

    private static void logRequest(HttpExchange exchange) throws IOException {
        String timestamp = dateFormat.format(new Date());
        String method = exchange.getRequestMethod();
        String uri = exchange.getRequestURI().toString();
        String body = readRequestBody(exchange);
        System.out.printf("[%s] Request: %s %s%n", timestamp, method, uri);
        if (!body.isEmpty()) {
            System.out.printf("[%s] Request Body: %s%n", timestamp, body);
        }
    }

    public static String readRequestBody(HttpExchange exchange) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(exchange.getRequestBody()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    public static void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        String timestamp = dateFormat.format(new Date());
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        if (response.isEmpty()) {
            exchange.sendResponseHeaders(statusCode, -1);
            System.out.printf("[%s] Response: %d (empty)%n", timestamp, statusCode);
        } else {
            byte[] responseBytes = response.getBytes();
            exchange.sendResponseHeaders(statusCode, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
            System.out.printf("[%s] Response: %d %s%n", timestamp, statusCode, response);
        }
    }
}