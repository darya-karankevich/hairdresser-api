package com.hairdresser.api;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class HairdresserRestServer {
    private static final String DB_URL = "jdbc:h2:./hairdresser_db";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "";

    public static void main(String[] args) throws Exception {
        // Инициализация базы данных
        DatabaseManager.getInstance().initializeDatabase();

        // Создание HTTP-сервера
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.setExecutor(Executors.newFixedThreadPool(10)); // Пул потоков для многопоточности

        // Определение REST-эндпоинтов с использованием фабрики
        HandlerFactory factory = new HandlerFactory();
        server.createContext("/api/roles", factory.createHandler("roles"));
        server.createContext("/api/users", factory.createHandler("users"));
        server.createContext("/api/serviceTypes", factory.createHandler("serviceTypes"));
        server.createContext("/api/shifts", factory.createHandler("shifts"));
        server.createContext("/api/visits", factory.createHandler("visits"));
        server.createContext("/api/visitors", factory.createHandler("visitors"));

        // Запуск сервера
        server.start();
        System.out.println("Сервер запущен на порту 8080");
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
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        if (response.isEmpty()) {
            exchange.sendResponseHeaders(statusCode, -1);
        } else {
            byte[] responseBytes = response.getBytes();
            exchange.sendResponseHeaders(statusCode, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        }
    }
}