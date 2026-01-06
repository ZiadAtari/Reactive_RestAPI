package ziadatari.ReactiveAPI.web;

import io.vertx.ext.web.RoutingContext;
import ziadatari.ReactiveAPI.dto.EmployeeDTO;
import ziadatari.ReactiveAPI.service.EmployeeService;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

// Handles parsing HTTP requests and calling service

public class EmployeeController {

  // Allows services to be swapped later
  private final EmployeeService service;

  public EmployeeController(EmployeeService service) {
    this.service = service;
  }

  public void getAll(RoutingContext ctx) {
    service.getAllEmployees() // returns a promise
      .onSuccess(list -> { // Callback
        JsonArray response = new JsonArray();
        list.forEach(dto -> response.add(dto.toJson()));
        ctx.json(response);
      })
      .onFailure(err -> ctx.fail(500, err));
  }

  public void create(RoutingContext ctx) {
    try {
      JsonObject body = ctx.body().asJsonObject();
      EmployeeDTO dto = EmployeeDTO.fromJson(body);

      service.createEmployee(dto)
        .onSuccess(v -> ctx.response().setStatusCode(201).end("created"))
        .onFailure(err -> {
          if (err.getMessage().contains("required")) {
            ctx.fail(400, err);
          }
          else {
            ctx.fail(500, err);
          }
        });
    } catch (Exception e) {
      ctx.fail(400, e);
    }
  }

  public void update(RoutingContext ctx) {
    String id = ctx.pathParam("id");
    System.out.println("Update Request received for ID: " + id); // DEBUG LOG 1

    JsonObject body = ctx.body().asJsonObject();
    System.out.println("Payload: " + body.encode()); // DEBUG LOG 2

    EmployeeDTO dto = EmployeeDTO.fromJson(body);

    service.updateEmployee(id, dto)
      .onSuccess(found -> {
        if (found) {
          System.out.println("Update Successful!");
          ctx.response().end("Updated");
        } else {
          // This is the likely culprit
          System.out.println("Update Failed: ID exists in URL but NOT in Database.");
          ctx.response().setStatusCode(404).end("ID Not Found");
        }
      })
      .onFailure(err -> {
        // This catches SQL errors or logic crashes
        System.err.println("CRASH in Update Method:");
        err.printStackTrace();
        ctx.fail(500, err);
      });
  }

  public void delete(RoutingContext ctx) {
    String id = ctx.pathParam("id");
    service.deleteEmployee(id)
      .onSuccess(found -> {
        if (found) ctx.response().setStatusCode(204).end();
        else ctx.fail(404);
      })
      .onFailure(err -> ctx.fail(500, err));
  }

}
