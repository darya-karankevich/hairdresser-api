package com.hairdresser.api;

import com.sun.net.httpserver.HttpExchange;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.*;

public class VisitorsHandler implements EntityHandler {
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
        String query = "SELECT * FROM Visitors";
        PreparedStatement stmt = conn.prepareStatement(query);
        ResultSet rs = stmt.executeQuery();
        JSONArray visitors = new JSONArray();
        while (rs.next()) {
            JSONObject visitor = new JSONObject();
            visitor.put("visitor_id", rs.getInt("visitor_id"));
            visitor.put("full_name", rs.getString("full_name"));
            visitor.put("phone_number", rs.getString("phone_number"));
            visitors.put(visitor);
        }
        HairdresserRestServer.sendResponse(exchange, 200, visitors.toString());
    }

    @Override
    public void handlePost(HttpExchange exchange, Connection conn) throws SQLException, IOException {
        String requestBody = HairdresserRestServer.readRequestBody(exchange);
        JSONObject json = new JSONObject(requestBody);
        String fullName = json.getString("full_name");
        String phoneNumber = json.getString("phone_number");

        String query = "INSERT INTO Visitors (full_name, phone_number) VALUES (?, ?)";
        PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
        stmt.setString(1, fullName);
        stmt.setString(2, phoneNumber);
        stmt.executeUpdate();
        ResultSet rs = stmt.getGeneratedKeys();
        rs.next();
        int visitorId = rs.getInt(1);

        JSONObject response = new JSONObject();
        response.put("visitor_id", visitorId);
        response.put("full_name", fullName);
        response.put("phone_number", phoneNumber);
        HairdresserRestServer.sendResponse(exchange, 201, response.toString());
    }

    @Override
    public void handlePut(HttpExchange exchange, Connection conn) throws SQLException, IOException {
        String requestBody = HairdresserRestServer.readRequestBody(exchange);
        JSONObject json = new JSONObject(requestBody);
        int visitorId = json.getInt("visitor_id");
        String fullName = json.getString("full_name");
        String phoneNumber = json.getString("phone_number");

        String query = "UPDATE Visitors SET full_name = ?, phone_number = ? WHERE visitor_id = ?";
        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setString(1, fullName);
        stmt.setString(2, phoneNumber);
        stmt.setInt(3, visitorId);
        int rows = stmt.executeUpdate();

        if (rows > 0) {
            JSONObject response = new JSONObject();
            response.put("visitor_id", visitorId);
            response.put("full_name", fullName);
            response.put("phone_number", phoneNumber);
            HairdresserRestServer.sendResponse(exchange, 200, response.toString());
        } else {
            HairdresserRestServer.sendResponse(exchange, 404, "{\"error\": \"Посетитель не найден\"}");
        }
    }

    @Override
    public void handleDelete(HttpExchange exchange, Connection conn) throws SQLException, IOException {
        String path = exchange.getRequestURI().getPath();
        String[] parts = path.split("/");
        int visitorId = Integer.parseInt(parts[parts.length - 1]);

        String query = "DELETE FROM Visitors WHERE visitor_id = ?";
        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setInt(1, visitorId);
        int rows = stmt.executeUpdate();

        if (rows > 0) {
            HairdresserRestServer.sendResponse(exchange, 204, "");
        } else {
            HairdresserRestServer.sendResponse(exchange, 404, "{\"error\": \"Посетитель не найден\"}");
        }
    }
}