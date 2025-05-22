package com.hairdresser.api;

import com.sun.net.httpserver.HttpExchange;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Date;

public class ReportsHandler implements EntityHandler {
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        String[] parts = path.split("/");
        String reportType = parts.length > 3 ? parts[3] : "";

        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            if (method.equals("GET")) {
                handleGet(exchange, conn, reportType);
            } else {
                HairdresserRestServer.sendResponse(exchange, 405, "{\"error\": \"Метод не поддерживается\"}");
            }
        } catch (SQLException e) {
            HairdresserRestServer.sendResponse(exchange, 500, "{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    @Override
    public void handleGet(HttpExchange exchange, Connection conn) throws SQLException, IOException {
        String path = exchange.getRequestURI().getPath();
        String[] parts = path.split("/");
        String reportType = parts.length > 3 ? parts[3] : "";
        handleGet(exchange, conn, reportType);
    }

    private void handleGet(HttpExchange exchange, Connection conn, String reportType) throws SQLException, IOException {
        switch (reportType.toLowerCase()) {
            case "master-schedule":
                getMasterSchedule(exchange, conn);
                break;
            case "client-report":
                getClientReport(exchange, conn);
                break;
            case "master-report":
                getMasterReport(exchange, conn);
                break;
            case "revenue-by-master":
                getRevenueByMaster(exchange, conn);
                break;
            case "clients-by-time":
                getClientsByTime(exchange, conn);
                break;
            case "profit-by-service":
                getProfitByService(exchange, conn);
                break;
            case "salary-calculation":
                getSalaryCalculation(exchange, conn);
                break;
            default:
                HairdresserRestServer.sendResponse(exchange, 400, "{\"error\": \"Неизвестный тип отчета\"}");
        }
    }

    private void getMasterSchedule(HttpExchange exchange, Connection conn) throws SQLException, IOException {
        String query = "SELECT v.visit_date, v.shift_id, s.shift_hours, vi.full_name, st.service_name " +
                "FROM Visits v " +
                "JOIN Shifts s ON v.shift_id = s.shift_id " +
                "JOIN Visitors vi ON v.visitor_id = vi.visitor_id " +
                "JOIN ServiceTypes st ON v.service_type_id = st.service_type_id " +
                "WHERE v.user_id = ? AND v.visit_date BETWEEN ? AND ?";
        String userIdStr = exchange.getRequestURI().getQuery() != null ?
                exchange.getRequestURI().getQuery().replace("userId=", "") : "0";
        int userId = Integer.parseInt(userIdStr);
        LocalDate today = LocalDate.now();
        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setInt(1, userId);
        stmt.setDate(2, java.sql.Date.valueOf(today));
        stmt.setDate(3, java.sql.Date.valueOf(today.plusDays(7)));
        ResultSet rs = stmt.executeQuery();
        JSONArray schedule = new JSONArray();
        while (rs.next()) {
            JSONObject entry = new JSONObject();
            entry.put("visit_date", rs.getString("visit_date"));
            entry.put("shift_hours", rs.getString("shift_hours"));
            entry.put("visitor_name", rs.getString("full_name"));
            entry.put("service_name", rs.getString("service_name"));
            schedule.put(entry);
        }
        HairdresserRestServer.sendResponse(exchange, 200, schedule.toString());
    }

    private void getClientReport(HttpExchange exchange, Connection conn) throws SQLException, IOException {
        String query = "SELECT vi.visitor_id, vi.full_name, vi.phone_number, COUNT(v.visit_id) as visit_count, " +
                "SUM(st.price) as total_spent " +
                "FROM Visitors vi " +
                "LEFT JOIN Visits v ON vi.visitor_id = v.visitor_id " +
                "LEFT JOIN ServiceTypes st ON v.service_type_id = st.service_type_id " +
                "GROUP BY vi.visitor_id, vi.full_name, vi.phone_number";
        PreparedStatement stmt = conn.prepareStatement(query);
        ResultSet rs = stmt.executeQuery();
        JSONArray report = new JSONArray();
        while (rs.next()) {
            JSONObject entry = new JSONObject();
            entry.put("visitor_id", rs.getInt("visitor_id"));
            entry.put("full_name", rs.getString("full_name"));
            entry.put("phone_number", rs.getString("phone_number"));
            entry.put("visit_count", rs.getInt("visit_count"));
            entry.put("total_spent", rs.getDouble("total_spent"));
            report.put(entry);
        }
        HairdresserRestServer.sendResponse(exchange, 200, report.toString());
    }

    private void getMasterReport(HttpExchange exchange, Connection conn) throws SQLException, IOException {
        String query = "SELECT u.user_id, u.username, COUNT(v.visit_id) as visit_count, " +
                "SUM(st.price) as total_revenue " +
                "FROM Users u " +
                "LEFT JOIN Visits v ON u.user_id = v.user_id " +
                "LEFT JOIN ServiceTypes st ON v.service_type_id = st.service_type_id " +
                "GROUP BY u.user_id, u.username";
        PreparedStatement stmt = conn.prepareStatement(query);
        ResultSet rs = stmt.executeQuery();
        JSONArray report = new JSONArray();
        while (rs.next()) {
            JSONObject entry = new JSONObject();
            entry.put("user_id", rs.getInt("user_id"));
            entry.put("username", rs.getString("username"));
            entry.put("visit_count", rs.getInt("visit_count"));
            entry.put("total_revenue", rs.getDouble("total_revenue"));
            report.put(entry);
        }
        HairdresserRestServer.sendResponse(exchange, 200, report.toString());
    }

    private void getRevenueByMaster(HttpExchange exchange, Connection conn) throws SQLException, IOException {
        String query = "SELECT u.user_id, u.username, SUM(st.price) as revenue " +
                "FROM Users u " +
                "LEFT JOIN Visits v ON u.user_id = v.user_id " +
                "LEFT JOIN ServiceTypes st ON v.service_type_id = st.service_type_id " +
                "GROUP BY u.user_id, u.username";
        PreparedStatement stmt = conn.prepareStatement(query);
        ResultSet rs = stmt.executeQuery();
        JSONArray data = new JSONArray();
        while (rs.next()) {
            JSONObject entry = new JSONObject();
            entry.put("username", rs.getString("username"));
            entry.put("revenue", rs.getDouble("revenue"));
            data.put(entry);
        }
        HairdresserRestServer.sendResponse(exchange, 200, data.toString());
    }

    private void getClientsByTime(HttpExchange exchange, Connection conn) throws SQLException, IOException {
        String query = "SELECT v.shift_id, s.shift_hours, COUNT(v.visit_id) as client_count " +
                "FROM Visits v " +
                "JOIN Shifts s ON v.shift_id = s.shift_id " +
                "GROUP BY v.shift_id, s.shift_hours";
        PreparedStatement stmt = conn.prepareStatement(query);
        ResultSet rs = stmt.executeQuery();
        JSONArray data = new JSONArray();
        while (rs.next()) {
            JSONObject entry = new JSONObject();
            entry.put("shift_id", rs.getInt("shift_id"));
            entry.put("shift_hours", rs.getString("shift_hours"));
            entry.put("client_count", rs.getInt("client_count"));
            data.put(entry);
        }
        HairdresserRestServer.sendResponse(exchange, 200, data.toString());
    }

    private void getProfitByService(HttpExchange exchange, Connection conn) throws SQLException, IOException {
        String query = "SELECT st.service_type_id, st.service_name, SUM(st.price) as profit " +
                "FROM ServiceTypes st " +
                "LEFT JOIN Visits v ON st.service_type_id = v.service_type_id " +
                "GROUP BY st.service_type_id, st.service_name";
        PreparedStatement stmt = conn.prepareStatement(query);
        ResultSet rs = stmt.executeQuery();
        JSONArray data = new JSONArray();
        while (rs.next()) {
            JSONObject entry = new JSONObject();
            entry.put("service_name", rs.getString("service_name"));
            entry.put("profit", rs.getDouble("profit"));
            data.put(entry);
        }
        HairdresserRestServer.sendResponse(exchange, 200, data.toString());
    }

    private void getSalaryCalculation(HttpExchange exchange, Connection conn) throws SQLException, IOException {
        String timestamp = dateFormat.format(new Date());
        String query = "SELECT u.user_id, u.username, s.salary_percentage, " +
                "COUNT(v.visit_id) as visit_count, " +
                "SUM(COALESCE(st.price * s.salary_percentage / 100, 0)) as salary " +
                "FROM Users u " +
                "JOIN Salaries s ON u.user_id = s.user_id " +
                "LEFT JOIN Visits v ON u.user_id = v.user_id " +
                "LEFT JOIN ServiceTypes st ON v.service_type_id = st.service_type_id " +
                "GROUP BY u.user_id, u.username, s.salary_percentage";
        PreparedStatement stmt = conn.prepareStatement(query);
        ResultSet rs = stmt.executeQuery();
        JSONArray salaries = new JSONArray();
        System.out.printf("[%s] Salary Calculation: Starting query execution%n", timestamp);
        while (rs.next()) {
            int userId = rs.getInt("user_id");
            String username = rs.getString("username");
            double salaryPercentage = rs.getDouble("salary_percentage");
            int visitCount = rs.getInt("visit_count");
            double salary = rs.getDouble("salary");
            System.out.printf("[%s] Salary Calculation: user_id=%d, username=%s, visit_count=%d, salary_percentage=%.2f, salary=%.2f%n",
                    timestamp, userId, username, visitCount, salaryPercentage, salary);
            JSONObject entry = new JSONObject();
            entry.put("user_id", userId);
            entry.put("username", username);
            entry.put("salary_percentage", salaryPercentage);
            entry.put("salary", salary);
            salaries.put(entry);
        }
        System.out.printf("[%s] Salary Calculation: Query completed, returning %d records%n", timestamp, salaries.length());
        HairdresserRestServer.sendResponse(exchange, 200, salaries.toString());
    }

    @Override
    public void handlePost(HttpExchange exchange, Connection conn) throws SQLException, IOException {
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