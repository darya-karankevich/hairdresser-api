package com.hairdresser.api;

import com.sun.net.httpserver.HttpExchange;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.*;

public class RolesHandler implements EntityHandler {
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
        String query = "SELECT * FROM Roles";
        PreparedStatement stmt = conn.prepareStatement(query);
        ResultSet rs = stmt.executeQuery();
        JSONArray roles = new JSONArray();
        while (rs.next()) {
            JSONObject role = new JSONObject();
            role.put("role_id", rs.getInt("role_id"));
            role.put("role_name", rs.getString("role_name"));
            roles.put(role);
        }
        HairdresserRestServer.sendResponse(exchange, 200, roles.toString());
    }

    @Override
    public void handlePost(HttpExchange exchange, Connection conn) throws SQLException, IOException {
        String requestBody = HairdresserRestServer.readRequestBody(exchange);
        JSONObject json = new JSONObject(requestBody);
        String roleName = json.getString("role_name");

        String query = "INSERT INTO Roles (role_name) VALUES (?)";
        PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
        stmt.setString(1, roleName);
        stmt.executeUpdate();
        ResultSet rs = stmt.getGeneratedKeys();
        rs.next();
        int roleId = rs.getInt(1);

        JSONObject response = new JSONObject();
        response.put("role_id", roleId);
        response.put("role_name", roleName);
        HairdresserRestServer.sendResponse(exchange, 201, response.toString());
    }

    @Override
    public void handlePut(HttpExchange exchange, Connection conn) throws SQLException, IOException {
        String requestBody = HairdresserRestServer.readRequestBody(exchange);
        JSONObject json = new JSONObject(requestBody);
        int roleId = json.getInt("role_id");
        String roleName = json.getString("role_name");

        String query = "UPDATE Roles SET role_name = ? WHERE role_id = ?";
        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setString(1, roleName);
        stmt.setInt(2, roleId);
        int rows = stmt.executeUpdate();

        if (rows > 0) {
            JSONObject response = new JSONObject();
            response.put("role_id", roleId);
            response.put("role_name", roleName);
            HairdresserRestServer.sendResponse(exchange, 200, response.toString());
        } else {
            HairdresserRestServer.sendResponse(exchange, 404, "{\"error\": \"Роль не найдена\"}");
        }
    }

    @Override
    public void handleDelete(HttpExchange exchange, Connection conn) throws SQLException, IOException {
        String path = exchange.getRequestURI().getPath();
        String[] parts = path.split("/");
        int roleId = Integer.parseInt(parts[parts.length - 1]);

        String query = "DELETE FROM Roles WHERE role_id = ?";
        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setInt(1, roleId);
        int rows = stmt.executeUpdate();

        if (rows > 0) {
            HairdresserRestServer.sendResponse(exchange, 204, "");
        } else {
            HairdresserRestServer.sendResponse(exchange, 404, "{\"error\": \"Роль не найдена\"}");
        }
    }
}