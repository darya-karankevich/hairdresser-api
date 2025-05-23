package com.hairdresser.api;

import com.sun.net.httpserver.HttpExchange;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LoginHandler implements EntityHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        if (!method.equals("POST")) {
            HairdresserRestServer.sendResponse(exchange, 405, "{\"error\": \"Метод не поддерживается\"}");
            return;
        }

        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            handlePost(exchange, conn);
        } catch (SQLException e) {
            HairdresserRestServer.sendResponse(exchange, 500, "{\"error\": \"Ошибка базы данных: " + e.getMessage() + "\"}");
        }
    }

    @Override
    public void handlePost(HttpExchange exchange, Connection conn) throws SQLException, IOException {
        String requestBody = HairdresserRestServer.readRequestBody(exchange);
        System.out.println("POST /api/login received: " + requestBody);

        try {
            JSONObject json = new JSONObject(requestBody);
            String username = json.getString("username");
            String password = json.getString("password");

            String query = "SELECT u.user_id, u.username, u.password, u.role_id, r.role_name " +
                    "FROM Users u JOIN Roles r ON u.role_id = r.role_id " +
                    "WHERE u.username = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String storedPassword = rs.getString("password");
                if (storedPassword.equals(password)) { // Для простоты, без хэширования
                    JSONObject response = new JSONObject();
                    response.put("user_id", rs.getInt("user_id"));
                    response.put("username", rs.getString("username"));
                    response.put("role_id", rs.getInt("role_id"));
                    response.put("role_name", rs.getString("role_name"));
                    HairdresserRestServer.sendResponse(exchange, 200, response.toString());
                } else {
                    HairdresserRestServer.sendResponse(exchange, 401, "{\"error\": \"Неверный пароль\"}");
                }
            } else {
                HairdresserRestServer.sendResponse(exchange, 401, "{\"error\": \"Пользователь не найден\"}");
            }
        } catch (Exception e) {
            System.err.println("Exception in LoginHandler: " + e.getMessage());
            HairdresserRestServer.sendResponse(exchange, 400, "{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    @Override
    public void handleGet(HttpExchange exchange, Connection conn) throws SQLException, IOException {
        HairdresserRestServer.sendResponse(exchange, 405, "{\"error\": \"Метод не поддерживается\"}");
    }

    @Override
    public void handlePut(HttpExchange exchange, Connection conn) throws SQLException, IOException {
        HairdresserRestServer.sendResponse(exchange, 405, "{\"error\": \"Метод не поддерживается\"}");
    }

    @Override
    public void handleDelete(HttpExchange exchange, Connection conn) throws SQLException, IOException {
        HairdresserRestServer.sendResponse(exchange, 405, "{\"error\": \"Метод не поддерживается\"}");
    }
}