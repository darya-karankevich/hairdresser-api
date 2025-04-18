package com.hairdresser.api;

import com.sun.net.httpserver.HttpExchange;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.*;

public class UsersHandler implements EntityHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            switch (method) {
                case "GET":
                    handleGet(exchange, conn);
                    break;
                case "POST":
                    handlePost(exchange, conn);
                    break;
                case "PUT":
                    handlePut(exchange, conn);
                    break;
                case "DELETE":
                    handleDelete(exchange, conn);
                    break;
                default:
                    HairdresserRestServer.sendResponse(exchange, 405, "{\"error\": \"Метод не поддерживается\"}");
            }
        } catch (SQLException e) {
            HairdresserRestServer.sendResponse(exchange, 500, "{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    @Override
    public void handleGet(HttpExchange exchange, Connection conn) throws SQLException, IOException {
        String query = "SELECT u.*, r.role_name FROM Users u JOIN Roles r ON u.role_id = r.role_id";
        PreparedStatement stmt = conn.prepareStatement(query);
        ResultSet rs = stmt.executeQuery();
        JSONArray users = new JSONArray();
        while (rs.next()) {
            JSONObject user = new JSONObject();
            user.put("user_id", rs.getInt("user_id"));
            user.put("username", rs.getString("username"));
            user.put("password", rs.getString("password"));
            user.put("role_id", rs.getInt("role_id"));
            user.put("role_name", rs.getString("role_name"));
            users.put(user);
        }
        HairdresserRestServer.sendResponse(exchange, 200, users.toString());
    }

    @Override
    public void handlePost(HttpExchange exchange, Connection conn) throws SQLException, IOException {
        String requestBody = HairdresserRestServer.readRequestBody(exchange);
        JSONObject json = new JSONObject(requestBody);
        String username = json.getString("username");
        String password = json.getString("password");
        int roleId = json.getInt("role_id");

        String query = "INSERT INTO Users (username, password, role_id) VALUES (?, ?, ?)";
        PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
        stmt.setString(1, username);
        stmt.setString(2, password);
        stmt.setInt(3, roleId);
        stmt.executeUpdate();
        ResultSet rs = stmt.getGeneratedKeys();
        rs.next();
        int userId = rs.getInt(1);

        JSONObject response = new JSONObject();
        response.put("user_id", userId);
        response.put("username", username);
        response.put("password", password);
        response.put("role_id", roleId);
        HairdresserRestServer.sendResponse(exchange, 201, response.toString());
    }

    @Override
    public void handlePut(HttpExchange exchange, Connection conn) throws SQLException, IOException {
        String requestBody = HairdresserRestServer.readRequestBody(exchange);
        JSONObject json = new JSONObject(requestBody);
        int userId = json.getInt("user_id");
        String username = json.getString("username");
        String password = json.getString("password");
        int roleId = json.getInt("role_id");

        String query = "UPDATE Users SET username = ?, password = ?, role_id = ? WHERE user_id = ?";
        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setString(1, username);
        stmt.setString(2, password);
        stmt.setInt(3, roleId);
        stmt.setInt(4, userId);
        int rows = stmt.executeUpdate();

        if (rows > 0) {
            JSONObject response = new JSONObject();
            response.put("user_id", userId);
            response.put("username", username);
            response.put("password", password);
            response.put("role_id", roleId);
            HairdresserRestServer.sendResponse(exchange, 200, response.toString());
        } else {
            HairdresserRestServer.sendResponse(exchange, 404, "{\"error\": \"Пользователь не найден\"}");
        }
    }

    @Override
    public void handleDelete(HttpExchange exchange, Connection conn) throws SQLException, IOException {
        String path = exchange.getRequestURI().getPath();
        String[] parts = path.split("/");
        int userId = Integer.parseInt(parts[parts.length - 1]);

        String query = "DELETE FROM Users WHERE user_id = ?";
        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setInt(1, userId);
        int rows = stmt.executeUpdate();

        if (rows > 0) {
            HairdresserRestServer.sendResponse(exchange, 204, "");
        } else {
            HairdresserRestServer.sendResponse(exchange, 404, "{\"error\": \"Пользователь не найден\"}");
        }
    }
}