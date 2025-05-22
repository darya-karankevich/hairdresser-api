package com.hairdresser.api;

import com.sun.net.httpserver.HttpExchange;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class VisitsHandler implements EntityHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            if (method.equals("GET")) {
                handleGet(exchange, conn);
            } else if (method.equals("POST")) {
                handlePost(exchange, conn);
            } else {
                HairdresserRestServer.sendResponse(exchange, 405, "{\"error\": \"Метод не поддерживается\"}");
            }
        } catch (SQLException e) {
            HairdresserRestServer.sendResponse(exchange, 500, "{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    @Override
    public void handleGet(HttpExchange exchange, Connection conn) throws SQLException, IOException {
        String query = "SELECT v.visit_id, v.visitor_id, vi.full_name AS visitor_name, " +
                "v.service_type_id, st.service_name, v.user_id, u.username AS master_name, " +
                "v.shift_id, s.shift_hours, v.visit_date " +
                "FROM Visits v " +
                "JOIN Visitors vi ON v.visitor_id = vi.visitor_id " +
                "JOIN ServiceTypes st ON v.service_type_id = st.service_type_id " +
                "JOIN Users u ON v.user_id = u.user_id " +
                "JOIN Shifts s ON v.shift_id = s.shift_id";
        PreparedStatement stmt = conn.prepareStatement(query);
        ResultSet rs = stmt.executeQuery();
        JSONArray visits = new JSONArray();
        while (rs.next()) {
            JSONObject visit = new JSONObject();
            visit.put("visit_id", rs.getInt("visit_id"));
            visit.put("visitor_id", rs.getInt("visitor_id"));
            visit.put("visitor_name", rs.getString("visitor_name"));
            visit.put("service_type_id", rs.getInt("service_type_id"));
            visit.put("service_name", rs.getString("service_name"));
            visit.put("user_id", rs.getInt("user_id"));
            visit.put("master_name", rs.getString("master_name"));
            visit.put("shift_id", rs.getInt("shift_id"));
            visit.put("shift_hours", rs.getString("shift_hours"));
            visit.put("visit_date", rs.getString("visit_date"));
            visits.put(visit);
        }
        HairdresserRestServer.sendResponse(exchange, 200, visits.toString());
    }

    @Override
    public void handlePost(HttpExchange exchange, Connection conn) throws SQLException, IOException {
        String requestBody = HairdresserRestServer.readRequestBody(exchange);
        try {
            JSONObject json = new JSONObject(requestBody);
            String query = "INSERT INTO Visits (visitor_id, service_type_id, user_id, shift_id, visit_date) " +
                    "VALUES (?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(query, PreparedStatement.RETURN_GENERATED_KEYS);
            stmt.setInt(1, json.getInt("visitor_id"));
            stmt.setInt(2, json.getInt("service_type_id"));
            stmt.setInt(3, json.getInt("user_id"));
            stmt.setInt(4, json.getInt("shift_id"));
            stmt.setString(5, json.getString("visit_date"));
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                JSONObject response = new JSONObject();
                response.put("visit_id", rs.getInt(1));
                response.put("visitor_id", json.getInt("visitor_id"));
                response.put("service_type_id", json.getInt("service_type_id"));
                response.put("user_id", json.getInt("user_id"));
                response.put("shift_id", json.getInt("shift_id"));
                response.put("visit_date", json.getString("visit_date"));
                HairdresserRestServer.sendResponse(exchange, 201, response.toString());
            } else {
                HairdresserRestServer.sendResponse(exchange, 500, "{\"error\": \"Не удалось создать визит\"}");
            }
        } catch (Exception e) {
            HairdresserRestServer.sendResponse(exchange, 400, "{\"error\": \"" + e.getMessage() + "\"}");
        }
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