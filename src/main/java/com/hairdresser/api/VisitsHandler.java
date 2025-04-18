package com.hairdresser.api;

import com.sun.net.httpserver.HttpExchange;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.*;

public class VisitsHandler implements EntityHandler {
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
        String query = "SELECT v.*, u.username, s.shift_hours, vi.full_name, vi.phone_number, st.service_name " +
                "FROM Visits v " +
                "JOIN Users u ON v.user_id = u.user_id " +
                "JOIN Shifts s ON v.shift_id = s.shift_id " +
                "JOIN Visitors vi ON v.visitor_id = vi.visitor_id " +
                "JOIN ServiceTypes st ON v.service_type_id = st.service_type_id";
        PreparedStatement stmt = conn.prepareStatement(query);
        ResultSet rs = stmt.executeQuery();
        JSONArray visits = new JSONArray();
        while (rs.next()) {
            JSONObject visit = new JSONObject();
            visit.put("visit_id", rs.getInt("visit_id"));
            visit.put("visit_date", rs.getString("visit_date"));
            visit.put("user_id", rs.getInt("user_id"));
            visit.put("username", rs.getString("username"));
            visit.put("shift_id", rs.getInt("shift_id"));
            visit.put("shift_hours", rs.getString("shift_hours"));
            visit.put("visitor_id", rs.getInt("visitor_id"));
            visit.put("visitor_full_name", rs.getString("full_name"));
            visit.put("visitor_phone_number", rs.getString("phone_number"));
            visit.put("service_type_id", rs.getInt("service_type_id"));
            visit.put("service_name", rs.getString("service_name"));
            visits.put(visit);
        }
        HairdresserRestServer.sendResponse(exchange, 200, visits.toString());
    }

    @Override
    public void handlePost(HttpExchange exchange, Connection conn) throws SQLException, IOException {
        String requestBody = HairdresserRestServer.readRequestBody(exchange);
        JSONObject json = new JSONObject(requestBody);
        String visitDate = json.getString("visit_date");
        int userId = json.getInt("user_id");
        int shiftId = json.getInt("shift_id");
        int visitorId = json.getInt("visitor_id");
        int serviceTypeId = json.getInt("service_type_id");

        String query = "INSERT INTO Visits (visit_date, user_id, shift_id, visitor_id, service_type_id) VALUES (?, ?, ?, ?, ?)";
        PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
        stmt.setDate(1, Date.valueOf(visitDate));
        stmt.setInt(2, userId);
        stmt.setInt(3, shiftId);
        stmt.setInt(4, visitorId);
        stmt.setInt(5, serviceTypeId);
        stmt.executeUpdate();
        ResultSet rs = stmt.getGeneratedKeys();
        rs.next();
        int visitId = rs.getInt(1);

        JSONObject response = new JSONObject();
        response.put("visit_id", visitId);
        response.put("visit_date", visitDate);
        response.put("user_id", userId);
        response.put("shift_id", shiftId);
        response.put("visitor_id", visitorId);
        response.put("service_type_id", serviceTypeId);
        HairdresserRestServer.sendResponse(exchange, 201, response.toString());
    }

    @Override
    public void handlePut(HttpExchange exchange, Connection conn) throws SQLException, IOException {
        String requestBody = HairdresserRestServer.readRequestBody(exchange);
        JSONObject json = new JSONObject(requestBody);
        int visitId = json.getInt("visit_id");
        String visitDate = json.getString("visit_date");
        int userId = json.getInt("user_id");
        int shiftId = json.getInt("shift_id");
        int visitorId = json.getInt("visitor_id");
        int serviceTypeId = json.getInt("service_type_id");

        String query = "UPDATE Visits SET visit_date = ?, user_id = ?, shift_id = ?, visitor_id = ?, service_type_id = ? WHERE visit_id = ?";
        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setDate(1, Date.valueOf(visitDate));
        stmt.setInt(2, userId);
        stmt.setInt(3, shiftId);
        stmt.setInt(4, visitorId);
        stmt.setInt(5, serviceTypeId);
        stmt.setInt(6, visitId);
        int rows = stmt.executeUpdate();

        if (rows > 0) {
            JSONObject response = new JSONObject();
            response.put("visit_id", visitId);
            response.put("visit_date", visitDate);
            response.put("user_id", userId);
            response.put("shift_id", shiftId);
            response.put("visitor_id", visitorId);
            response.put("service_type_id", serviceTypeId);
            HairdresserRestServer.sendResponse(exchange, 200, response.toString());
        } else {
            HairdresserRestServer.sendResponse(exchange, 404, "{\"error\": \"Посещение не найдено\"}");
        }
    }

    @Override
    public void handleDelete(HttpExchange exchange, Connection conn) throws SQLException, IOException {
        String path = exchange.getRequestURI().getPath();
        String[] parts = path.split("/");
        int visitId = Integer.parseInt(parts[parts.length - 1]);

        String query = "DELETE FROM Visits WHERE visit_id = ?";
        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setInt(1, visitId);
        int rows = stmt.executeUpdate();

        if (rows > 0) {
            HairdresserRestServer.sendResponse(exchange, 204, "");
        } else {
            HairdresserRestServer.sendResponse(exchange, 404, "{\"error\": \"Посещение не найдено\"}");
        }
    }
}