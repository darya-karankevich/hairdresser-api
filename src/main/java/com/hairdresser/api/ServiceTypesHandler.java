package com.hairdresser.api;

import com.sun.net.httpserver.HttpExchange;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.*;

public class ServiceTypesHandler implements EntityHandler {
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
        String query = "SELECT * FROM ServiceTypes";
        PreparedStatement stmt = conn.prepareStatement(query);
        ResultSet rs = stmt.executeQuery();
        JSONArray serviceTypes = new JSONArray();
        while (rs.next()) {
            JSONObject serviceType = new JSONObject();
            serviceType.put("service_type_id", rs.getInt("service_type_id"));
            serviceType.put("service_name", rs.getString("service_name"));
            serviceTypes.put(serviceType);
        }
        HairdresserRestServer.sendResponse(exchange, 200, serviceTypes.toString());
    }

    @Override
    public void handlePost(HttpExchange exchange, Connection conn) throws SQLException, IOException {
        String requestBody = HairdresserRestServer.readRequestBody(exchange);
        JSONObject json = new JSONObject(requestBody);
        String serviceName = json.getString("service_name");

        String query = "INSERT INTO ServiceTypes (service_name) VALUES (?)";
        PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
        stmt.setString(1, serviceName);
        stmt.executeUpdate();
        ResultSet rs = stmt.getGeneratedKeys();
        rs.next();
        int serviceTypeId = rs.getInt(1);

        JSONObject response = new JSONObject();
        response.put("service_type_id", serviceTypeId);
        response.put("service_name", serviceName);
        HairdresserRestServer.sendResponse(exchange, 201, response.toString());
    }

    @Override
    public void handlePut(HttpExchange exchange, Connection conn) throws SQLException, IOException {
        String requestBody = HairdresserRestServer.readRequestBody(exchange);
        JSONObject json = new JSONObject(requestBody);
        int serviceTypeId = json.getInt("service_type_id");
        String serviceName = json.getString("service_name");

        String query = "UPDATE ServiceTypes SET service_name = ? WHERE service_type_id = ?";
        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setString(1, serviceName);
        stmt.setInt(2, serviceTypeId);
        int rows = stmt.executeUpdate();

        if (rows > 0) {
            JSONObject response = new JSONObject();
            response.put("service_type_id", serviceTypeId);
            response.put("service_name", serviceName);
            HairdresserRestServer.sendResponse(exchange, 200, response.toString());
        } else {
            HairdresserRestServer.sendResponse(exchange, 404, "{\"error\": \"Тип услуги не найден\"}");
        }
    }

    @Override
    public void handleDelete(HttpExchange exchange, Connection conn) throws SQLException, IOException {
        String path = exchange.getRequestURI().getPath();
        String[] parts = path.split("/");
        int serviceTypeId = Integer.parseInt(parts[parts.length - 1]);

        String query = "DELETE FROM ServiceTypes WHERE service_type_id = ?";
        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setInt(1, serviceTypeId);
        int rows = stmt.executeUpdate();

        if (rows > 0) {
            HairdresserRestServer.sendResponse(exchange, 204, "");
        } else {
            HairdresserRestServer.sendResponse(exchange, 404, "{\"error\": \"Тип услуги не найден\"}");
        }
    }
}