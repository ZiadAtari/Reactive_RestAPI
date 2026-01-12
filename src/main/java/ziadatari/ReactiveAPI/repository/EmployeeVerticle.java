package ziadatari.ReactiveAPI.repository;

import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.mysqlclient.MySQLBuilder;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import ziadatari.ReactiveAPI.dto.EmployeeDTO;
import ziadatari.ReactiveAPI.exception.ErrorCode;
import ziadatari.ReactiveAPI.exception.ServiceException;
import ziadatari.ReactiveAPI.service.EmployeeService;

/**
 * Verticle responsible for managing Employee data and business logic
 * transactions.
 * It listens for messages on the Event Bus and coordinates with the database
 * layer.
 * This verticle isolates the repository and service logic from the web layer.
 */
public class EmployeeVerticle extends AbstractVerticle {

    private EmployeeService service;

    /**
     * Initializes the verticle by setting up the database connection pool,
     * configuring a circuit breaker, and registering Event Bus consumers.
     *
     * @param startPromise a promise to signal deployment success or failure
     */
    @Override
    public void start(Promise<Void> startPromise) {
        try {
            // Configuration retrieval from deployment options
            JsonObject dbConfig = config().getJsonObject("db");

            // Database connection pool setup
            MySQLConnectOptions connectOptions = new MySQLConnectOptions()
                    .setHost(dbConfig.getString("host"))
                    .setPort(dbConfig.getInteger("port"))
                    .setDatabase(dbConfig.getString("database"))
                    .setUser(dbConfig.getString("user"))
                    .setPassword(dbConfig.getString("password"));

            // Pool options: Max 10 concurrent connections
            Pool dbPool = MySQLBuilder.pool()
                    .with(new PoolOptions().setMaxSize(10))
                    .connectingTo(connectOptions)
                    .using(vertx)
                    .build();

            // Circuit Breaker configuration for fault tolerance against DB failures
            CircuitBreakerOptions cbOptions = new CircuitBreakerOptions()
                    .setMaxFailures(5) // If 5 consecutive failures occur, circuit opens
                    .setTimeout(2000) // Timeout for each operation
                    .setFallbackOnFailure(false)
                    .setResetTimeout(1000); // Wait 1s before attempting recovery

            CircuitBreaker circuitBreaker = CircuitBreaker.create("api-circuit-breaker", vertx, cbOptions);

            // Circuit status logging
            circuitBreaker
                    .openHandler(v -> System.out.println("CIRCUIT BREAKER: OPENED (Service failing or timing out)"));
            circuitBreaker.closeHandler(v -> System.out.println("CIRCUIT BREAKER: CLOSED (Service recovered)"));
            circuitBreaker.halfOpenHandler(v -> System.out.println("CIRCUIT BREAKER: HALF-OPEN (Testing recovery...)"));

            // Initialize repository and service
            EmployeeRepository repository = new EmployeeRepository(dbPool);
            service = new EmployeeService(repository, circuitBreaker);

            // Register handlers for Event Bus addresses
            vertx.eventBus().consumer("employees.get.all", this::getAllEmployees);
            vertx.eventBus().consumer("employees.create", this::createEmployee);
            vertx.eventBus().consumer("employees.update", this::updateEmployee);
            vertx.eventBus().consumer("employees.delete", this::deleteEmployee);

            System.out.println("EmployeeVerticle Deployed and Listening on Event Bus");
            startPromise.complete();

        } catch (Exception e) {
            startPromise.fail(e);
        }
    }

    /**
     * Handler for 'employees.get.all' address.
     * Fetches all employees via the service layer.
     *
     * @param message the Event Bus message
     */
    private void getAllEmployees(Message<Object> message) {
        service.getAllEmployees().onSuccess(list -> {
            JsonArray response = new JsonArray();
            list.forEach(dto -> response.add(dto.toJson()));
            message.reply(response);
        }).onFailure(err -> handleError(message, err));
    }

    /**
     * Handler for 'employees.create' address.
     * Parses the request body and creates a new employee.
     *
     * @param message the Event Bus message containing employee data
     */
    private void createEmployee(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            if (body == null) {
                message.fail(ErrorCode.MISSING_NAME.ordinal(), "Body is null");
                return;
            }
            EmployeeDTO dto = EmployeeDTO.fromJson(body);

            service.createEmployee(dto).onSuccess(savedDto -> {
                message.reply(savedDto.toJson());
            }).onFailure(err -> {
                handleError(message, err);
            });
        } catch (Exception e) {
            e.printStackTrace();
            handleError(message, e);
        }
    }

    /**
     * Handler for 'employees.update' address.
     * Updates an employee record identified by ID.
     *
     * @param message the Event Bus message containing ID and new data
     */
    private void updateEmployee(Message<JsonObject> message) {
        try {
            JsonObject body = message.body();
            String id = body.getString("id");
            EmployeeDTO dto = EmployeeDTO.fromJson(body);

            service.updateEmployee(id, dto).onSuccess(found -> {
                message.reply(new JsonObject().put("status", "updated"));
            }).onFailure(err -> handleError(message, err));
        } catch (Exception e) {
            handleError(message, e);
        }
    }

    /**
     * Handler for 'employees.delete' address.
     * Deletes (soft-delete) an employee by ID.
     *
     * @param message the Event Bus message containing the employee ID
     */
    private void deleteEmployee(Message<String> message) {
        String id = message.body();
        service.deleteEmployee(id).onSuccess(done -> {
            message.reply(new JsonObject().put("status", "deleted"));
        }).onFailure(err -> handleError(message, err));
    }

    /**
     * Common error handling for Event Bus replies.
     * Translates exceptions into failed Event Bus messages with relevant error
     * codes.
     *
     * @param message the message to fail
     * @param err     the error that occurred
     */
    private void handleError(Message<?> message, Throwable err) {
        if (err instanceof ServiceException) {
            ServiceException se = (ServiceException) err;
            // Send back the unique error code's ordinal for numeric identification
            message.fail(se.getErrorCode().ordinal(), se.getMessage());
        } else {
            message.fail(ErrorCode.INTERNAL_SERVER_ERROR.ordinal(), err.getMessage());
        }
    }
}
