package ziadatari.ReactiveAPI.web;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.ext.web.RoutingContext;
import ziadatari.ReactiveAPI.dto.EmployeeDTO;
import ziadatari.ReactiveAPI.exception.ErrorCode;
import ziadatari.ReactiveAPI.exception.GlobalErrorHandler;
import ziadatari.ReactiveAPI.exception.ServiceException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.time.Instant;

/**
 * Controller responsible for handling HTTP requests related to Employees.
 * It parses incoming JSON, performs basic validation, and communicates with the
 * EmployeeVerticle via the Vert.x Event Bus.
 */
public class EmployeeController {

  private final Vertx vertx;

  /**
   * Constructs an EmployeeController.
   *
   * @param vertx the Vertx instance used for Event Bus communication
   */
  public EmployeeController(Vertx vertx) {
    this.vertx = vertx;
  }

  /**
   * Handles GET /employees.
   * Requests all employee records from the service layer via the Event Bus.
   *
   * @param ctx the routing context
   */
  public void getAll(RoutingContext ctx) {
    vertx.eventBus().<JsonArray>request("employees.get.all", null)
        .onSuccess(msg -> ctx.json(msg.body()))
        .onFailure(err -> handleError(ctx, err));
  }

  /**
   * Handles POST /employees.
   * Parses the request body and requests employee creation.
   *
   * @param ctx the routing context
   */
  public void create(RoutingContext ctx) {
    try {
      JsonObject body = ctx.body().asJsonObject();
      if (body == null) {
        throw new ServiceException(ErrorCode.EMPTY_BODY);
      }

      // Preliminary validation: attempt DTO parsing to catch format errors early.
      EmployeeDTO.fromJson(body);

      // Inject authenticated user for audit trail
      String user = "anonymous";
      if (ctx.user() != null && ctx.user().principal() != null) {
        user = ctx.user().principal().getString("sub", "anonymous");
      }
      body.put("lastModifiedBy", user);

      vertx.eventBus().<JsonObject>request("employees.create", body)
          .onSuccess(msg -> {
            JsonObject savedJson = msg.body();
            sendResponse(ctx, 201, "CREATE", savedJson.getString("id"), savedJson.getString("name"));
          })
          .onFailure(err -> {
            handleError(ctx, err);
          });

    } catch (Exception e) {
      GlobalErrorHandler.handle(ctx, e);
    }
  }

  /**
   * Handles PUT /employees/:id.
   * Updates an existing employee after merging the ID from the path into the
   * payload.
   *
   * @param ctx the routing context
   */
  public void update(RoutingContext ctx) {
    String id = ctx.pathParam("id");

    try {
      JsonObject body = ctx.body().asJsonObject();
      if (body == null) {
        throw new ServiceException(ErrorCode.EMPTY_BODY);
      }
      // Inject path ID into payload for unified Event Bus transmission
      JsonObject payload = body.copy().put("id", id);

      EmployeeDTO dto = EmployeeDTO.fromJson(body); // Validation check

      // Inject authenticated user for audit trail
      String user = "anonymous";
      if (ctx.user() != null && ctx.user().principal() != null) {
        user = ctx.user().principal().getString("sub", "anonymous");
      }
      payload.put("lastModifiedBy", user);

      vertx.eventBus().<JsonObject>request("employees.update", payload)
          .onSuccess(msg -> {
            sendResponse(ctx, 200, "UPDATE", id, dto.getName());
          })
          .onFailure(err -> handleError(ctx, err));

    } catch (Exception e) {
      GlobalErrorHandler.handle(ctx, e);
    }
  }

  /**
   * Handles DELETE /employees/:id.
   * Requests a soft-delete of an employee by ID.
   *
   * @param ctx the routing context
   */
  public void delete(RoutingContext ctx) {
    String id = ctx.pathParam("id");

    // Inject authenticated user for audit trail
    String user = "anonymous";
    if (ctx.user() != null && ctx.user().principal() != null) {
      user = ctx.user().principal().getString("sub", "anonymous");
    }
    JsonObject payload = new JsonObject().put("id", id).put("lastModifiedBy", user);

    vertx.eventBus().<JsonObject>request("employees.delete", payload)
        .onSuccess(msg -> {
          sendResponse(ctx, 200, "DELETE", id, "N/A");
        })
        .onFailure(err -> handleError(ctx, err));
  }

  /**
   * Maps Event Bus failures (ReplyException) back to ServiceExceptions
   * so they can be handled by the GlobalErrorHandler.
   *
   * @param ctx the routing context
   * @param err the error caught from the Event Bus
   */
  private void handleError(RoutingContext ctx, Throwable err) {
    if (err instanceof ReplyException) {
      ReplyException re = (ReplyException) err;
      int codeInt = re.failureCode();
      ErrorCode[] allCodes = ErrorCode.values();

      // Retrieve original ErrorCode enum by ordinal
      if (codeInt >= 0 && codeInt < allCodes.length) {
        GlobalErrorHandler.handle(ctx, new ServiceException(allCodes[codeInt], re.getMessage()));
      } else {
        GlobalErrorHandler.handle(ctx, new ServiceException(ErrorCode.INTERNAL_SERVER_ERROR, re.getMessage()));
      }
    } else {
      GlobalErrorHandler.handle(ctx, err);
    }
  }

  /**
   * Standardized helper to send unified success responses.
   *
   * @param ctx        the routing context
   * @param statusCode the HTTP status code
   * @param operation  the name of the operation performed
   * @param id         the ID of the affected record
   * @param name       the name of the affected record
   */
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
