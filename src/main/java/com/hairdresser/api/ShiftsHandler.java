package com.hairdresser.api;

import com.sun.net.httpserver.HttpExchange;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.*;

public class ShiftsHandler implements EntityHandler {
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
        String query = "SELECT * FROM Shifts";
        PreparedStatement stmt = conn.prepareStatement(query);
        ResultSet rs = stmt.executeQuery();
        JSONArray shifts = new JSONArray();
        while (rs.next()) {
            JSONObject shift = new JSONObject();
            shift.put("shift_id", rs.getInt("shift_id"));
            shift.put("shift_hours", rs.getString("shift_hours"));
            shifts.put(shift);
        }
        HairdresserRestServer.sendResponse(exchange, 200, shifts.toString());
    }

    @Override
    public void handlePost(HttpExchange exchange, Connection conn) throws SQLException, IOException {
        String requestBody = HairdresserRestServer.readRequestBody(exchange);
        JSONObject json = new JSONObject(requestBody);
        String shiftHours = json.getString("shift_hours");

        String query = "INSERT INTO Shifts (shift_hours) VALUES (?)";
        PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
        stmt.setString(1, shiftHours);
        stmt.executeUpdate();
        ResultSet rs = stmt.getGeneratedKeys();
        rs.next();
        int shiftId = rs.getInt(1);

        JSONObject response = new JSONObject();
        response.put("shift_id", shiftId);
        response.put("shift_hours", shiftHours);
        HairdresserRestServer.sendResponse(exchange, 201, response.toString());
    }

    @Override
    public void handlePut(HttpExchange exchange, Connection conn) throws SQLException, IOException {
        String requestBody = HairdresserRestServer.readRequestBody(exchange);
        JSONObject json = new JSONObject(requestBody);
        int shiftId = json.getInt("shift_id");
        String shiftHours = json.getString("shift_hours");

        String query = "UPDATE Shifts SET shift_hours = ? WHERE shift_id = ?";
        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setString(1, shiftHours);
        stmt.setInt(2, shiftId);
        int rows = stmt.executeUpdate();

        if (rows > 0) {
            JSONObject response = new JSONObject();
            response.put("shift_id", shiftId);
            response.put("shift_hours", shiftHours);
            HairdresserRestServer.sendResponse(exchange, 200, response.toString());
        } else {
            HairdresserRestServer.sendResponse(exchange, 404, "{\"error\": \"Смена не найдена\"}");
        }
    }

    @Override
    public void handleDelete(HttpExchange exchange, Connection conn) throws SQLException, IOException {
        String path = exchange.getRequestURI().getPath();
        String[] parts = path.split("/");
        int shiftId = Integer.parseInt(parts[parts.length - 1]);

        String query = "DELETE FROM Shifts WHERE shift_id = ?";
        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setInt(1, shiftId);
        int rows = stmt.executeUpdate();

        if (rows > 0) {
            HairdresserRestServer.sendResponse(exchange, 204, "");
        } else {
            HairdresserRestServer.sendResponse(exchange, 404, "{\"error\": \"Смена не найдена\"}");
        }
    }
}