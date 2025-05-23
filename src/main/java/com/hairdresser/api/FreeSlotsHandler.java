package com.hairdresser.api;
import com.sun.net.httpserver.HttpExchange;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class FreeSlotsHandler implements EntityHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equals("GET")) {
            HairdresserRestServer.sendResponse(exchange, 405, "{\"error\": \"Метод не поддерживается\"}");
            return;
        }
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            handleGet(exchange, conn);
        } catch (SQLException e) {
            HairdresserRestServer.sendResponse(exchange, 500, "{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    @Override
    public void handleGet(HttpExchange exchange, Connection conn) throws SQLException, IOException {
        String query = "SELECT s.shift_id, s.shift_hours, " +
                "(SELECT COUNT(*) FROM Visits v WHERE v.shift_id = s.shift_id) AS booked_slots " +
                "FROM Shifts s";
        PreparedStatement stmt = conn.prepareStatement(query);
        ResultSet rs = stmt.executeQuery();
        JSONArray slots = new JSONArray();
        while (rs.next()) {
            JSONObject slot = new JSONObject();
            slot.put("shift_id", rs.getInt("shift_id"));
            slot.put("shift_hours", rs.getString("shift_hours"));
            slot.put("available_slots", 10 - rs.getInt("booked_slots")); // Предполагаем, что максимум 10 слотов
            slots.put(slot);
        }
        HairdresserRestServer.sendResponse(exchange, 200, slots.toString());
    }

    @Override
    public void handlePost(HttpExchange exchange, Connection conn) throws SQLException, IOException {
    }

    @Override
    public void handlePut(HttpExchange exchange, Connection conn) throws SQLException, IOException {
    }

    @Override
    public void handleDelete(HttpExchange exchange, Connection conn) throws SQLException, IOException {
    }
}