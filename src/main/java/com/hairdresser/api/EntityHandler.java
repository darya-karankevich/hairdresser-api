package com.hairdresser.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

public interface EntityHandler extends HttpHandler {
    void handleGet(HttpExchange exchange, Connection conn) throws SQLException, IOException;
    void handlePost(HttpExchange exchange, Connection conn) throws SQLException, IOException;
    void handlePut(HttpExchange exchange, Connection conn) throws SQLException, IOException;
    void handleDelete(HttpExchange exchange, Connection conn) throws SQLException, IOException;
}