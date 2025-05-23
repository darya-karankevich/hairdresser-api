package com.hairdresser.api;

import com.sun.net.httpserver.HttpExchange;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class RegisterHandler implements EntityHandler {
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
        System.out.println("POST /api/register received: " + requestBody);

        try {
            JSONObject json = new JSONObject(requestBody);
            String username = json.getString("username");
            String password = json.getString("password");
            String roleName = json.getString("role_name");

            // Проверка существующего пользователя
            String checkQuery = "SELECT user_id FROM Users WHERE username = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkQuery);
            checkStmt.setString(1, username);
            ResultSet checkRs = checkStmt.executeQuery();
            if (checkRs.next()) {
                HairdresserRestServer.sendResponse(exchange, 400, "{\"error\": \"Пользователь уже существует\"}");
                return;
            }

            // Получение role_id
            String roleQuery = "SELECT role_id FROM Roles WHERE role_name = ?";
            PreparedStatement roleStmt = conn.prepareStatement(roleQuery);
            roleStmt.setString(1, roleName);
            ResultSet roleRs = roleStmt.executeQuery();
            if (!roleRs.next()) {
                HairdresserRestServer.sendResponse(exchange, 400, "{\"error\": \"Роль не найдена\"}");
                return;
            }
            int roleId = roleRs.getInt("role_id");

            // Создание пользователя
            String insertQuery = "INSERT INTO Users (username, password, role_id) VALUES (?, ?, ?)";
            PreparedStatement insertStmt = conn.prepareStatement(insertQuery, PreparedStatement.RETURN_GENERATED_KEYS);
            insertStmt.setString(1, username);
            insertStmt.setString(2, password); // Для простоты, без хэширования
            insertStmt.setInt(3, roleId);
            int rowsAffected = insertStmt.executeUpdate();
            System.out.println("Rows affected: " + rowsAffected); // Логирование результата

            if (rowsAffected > 0) {
                ResultSet rs = insertStmt.getGeneratedKeys();
                if (rs.next()) {
                    JSONObject response = new JSONObject();
                    response.put("user_id", rs.getInt(1));
                    response.put("username", username);
                    response.put("role_id", roleId);
                    response.put("role_name", roleName);
                    HairdresserRestServer.sendResponse(exchange, 201, response.toString());
                } else {
                    HairdresserRestServer.sendResponse(exchange, 500, "{\"error\": \"Не удалось создать пользователя\"}");
                }
            } else {
                HairdresserRestServer.sendResponse(exchange, 500, "{\"error\": \"Не удалось создать пользователя\"}");
            }
        } catch (Exception e) {
            System.err.println("Exception in RegisterHandler: " + e.getMessage());
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