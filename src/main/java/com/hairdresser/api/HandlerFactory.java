package com.hairdresser.api;

import com.sun.net.httpserver.HttpHandler;

public class HandlerFactory {
    public HttpHandler createHandler(String entity) {
        switch (entity.toLowerCase()) {
            case "roles":
                return new RolesHandler();
            case "users":
                return new UsersHandler();
            case "servicetypes":
                return new ServiceTypesHandler();
            case "shifts":
                return new ShiftsHandler();
            case "visits":
                return new VisitsHandler();
            case "visitors":
                return new VisitorsHandler();
            case "reports":
                return new ReportsHandler();
            default:
                throw new IllegalArgumentException("Неизвестная сущность: " + entity);
        }
    }
}