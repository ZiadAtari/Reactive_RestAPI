package ziadatari.ReactiveAPI.web;

import io.vertx.ext.web.RoutingContext;
import ziadatari.ReactiveAPI.dto.EmployeeDTO;
import ziadatari.ReactiveAPI.exception.GlobalErrorHandler;
import ziadatari.ReactiveAPI.service.EmployeeService;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.time.Instant;

// Handles parsing HTTP requests and calling service

public class EmployeeController {

  // Allows services to be swapped later
  private final EmployeeService service;

  public EmployeeController(EmployeeService service) {
    this.service = service;
  }


  public void getAll(RoutingContext ctx) {
    service.getAllEmployees()
      .onSuccess(list -> {
        JsonArray response = new JsonArray();
        list.forEach(dto -> response.add(dto.toJson()));
        ctx.json(response);
      })
      .onFailure(err -> GlobalErrorHandler.handle(ctx, err)); // Centralized
  }

  public void create(RoutingContext ctx) {
    try {
      JsonObject body = ctx.body().asJsonObject();
      EmployeeDTO dto = EmployeeDTO.fromJson(body);

      service.createEmployee(dto)
        .onSuccess(savedDto -> {
          sendResponse(ctx, 201, "CREATE", savedDto.getId(), savedDto.getName());
        })
        .onFailure(err -> GlobalErrorHandler.handle(ctx, err));

    } catch (Exception e) {
      GlobalErrorHandler.handle(ctx, e);
    }
  }

  public void update(RoutingContext ctx) {
    String id = ctx.pathParam("id");

    try {
      JsonObject body = ctx.body().asJsonObject();
      EmployeeDTO dto = EmployeeDTO.fromJson(body);

      service.updateEmployee(id, dto)
        .onSuccess(found -> {
          sendResponse(ctx, 200, "UPDATE", id, dto.getName());
        }) // Service handles 404 now
        .onFailure(err -> GlobalErrorHandler.handle(ctx, err));

    } catch (Exception e) {
      GlobalErrorHandler.handle(ctx, e);
    }
  }

  public void delete(RoutingContext ctx) {
    String id = ctx.pathParam("id");

    service.deleteEmployee(id)
      .onSuccess(found -> {
        // No name for delete ONLY
        sendResponse(ctx, 200, "DELETE", id, "N/A");
      })
      .onFailure(err -> GlobalErrorHandler.handle(ctx, err));
  }

  // Helper for Response
  private void sendResponse(RoutingContext ctx, int statusCode, String operation, String id, String name) {
    JsonObject response = new JsonObject()
      .put("Operation", operation)
      .put("Status", "SUCCESS")
      .put("Affected ID", id)
      .put("Affected Name", name)
      .put("Time", Instant.now().toString());

    ctx.response()
      .setStatusCode(statusCode)
      .putHeader("content-type", "application/json")
      .end(response.encodePrettily());
  }

}
