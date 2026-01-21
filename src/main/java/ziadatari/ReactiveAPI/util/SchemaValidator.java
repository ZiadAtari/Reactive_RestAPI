package ziadatari.ReactiveAPI.util;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.SchemaParser;
import io.vertx.json.schema.SchemaRouter;
import io.vertx.json.schema.SchemaRouterOptions;
import io.vertx.json.schema.common.SchemaRouterImpl;
import io.vertx.core.Vertx;
import ziadatari.ReactiveAPI.exception.ErrorCode;
import ziadatari.ReactiveAPI.exception.ServiceException;

/**
 * Utility class for JSON Schema Validation (Part of 3.4 Schema Update).
 * <p>
 * This class provides a centralized way to validate incoming JSON requests
 * against
 * pre-defined schemas. It supports "fail-fast" validation for:
 * <ul>
 * <li>Login Requests ({@code validateLogin})</li>
 * <li>Single Employee Creation/Update ({@code validateEmployee})</li>
 * <li>Batch Employee Creation ({@code validateEmployeeBatch})</li>
 * </ul>
 * <p>
 * It uses the Vert.x JSON Schema parser (Draft 2019-09) and maps validation
 * failures to the application's specific {@link ServiceException} and
 * {@link ErrorCode}.
 */
public class SchemaValidator {

    private static final JsonObject LOGIN_SCHEMA = new JsonObject()
            .put("type", "object")
            .put("properties", new JsonObject()
                    .put("username", new JsonObject().put("type", "string").put("minLength", 1))
                    .put("password", new JsonObject().put("type", "string").put("minLength", 1)))
            .put("required", new JsonArray().add("username").add("password"));

    private static final JsonObject EMPLOYEE_SCHEMA = new JsonObject()
            .put("type", "object")
            .put("properties", new JsonObject()
                    .put("name", new JsonObject().put("type", "string").put("minLength", 1))
                    .put("department", new JsonObject().put("type", "string").put("minLength", 1))
                    .put("salary", new JsonObject().put("type", "number").put("minimum", 0))
                    .put("active", new JsonObject().put("type", "boolean")))
            .put("required", new JsonArray().add("name").add("department").add("salary"));

    private static final JsonObject EMPLOYEE_BATCH_SCHEMA = new JsonObject()
            .put("type", "array")
            .put("items", EMPLOYEE_SCHEMA)
            .put("minItems", 1)
            .put("maxItems", 100);

    // Changed from Validator to Schema as parser.parse returns Schema
    private static io.vertx.json.schema.Schema loginValidator;
    private static io.vertx.json.schema.Schema employeeValidator;
    private static io.vertx.json.schema.Schema employeeBatchValidator;

    static {
        Vertx vertx = Vertx.vertx();
        SchemaRouter router = SchemaRouter.create(vertx, new SchemaRouterOptions());
        SchemaParser parser = SchemaParser.createDraft201909SchemaParser(router);

        loginValidator = parser.parse(LOGIN_SCHEMA);
        employeeValidator = parser.parse(EMPLOYEE_SCHEMA);
        employeeBatchValidator = parser.parse(EMPLOYEE_BATCH_SCHEMA);
    }

    public static void validateLogin(JsonObject body) {
        try {
            loginValidator.validateSync(body);
        } catch (io.vertx.json.schema.ValidationException e) {
            throw mapError(e);
        } catch (Exception e) {
            // Fallback for NoSyncValidationException or others
            throw new ServiceException(ErrorCode.VALIDATION_ERROR, "Schema Validation Failed: " + e.getMessage());
        }
    }

    public static void validateEmployee(JsonObject body) {
        try {
            employeeValidator.validateSync(body);
        } catch (io.vertx.json.schema.ValidationException e) {
            throw mapError(e);
        } catch (Exception e) {
            throw new ServiceException(ErrorCode.VALIDATION_ERROR, "Schema Validation Failed: " + e.getMessage());
        }
    }

    public static void validateEmployeeBatch(JsonArray body) {
        try {
            employeeBatchValidator.validateSync(body);
        } catch (io.vertx.json.schema.ValidationException e) {
            throw mapError(e);
        } catch (Exception e) {
            throw new ServiceException(ErrorCode.VALIDATION_ERROR, "Schema Validation Failed: ");
        }
    }

    // Changed parameter to Object to handle unknown SchemaError type safely
    /**
     * Maps generic schema validation errors to specific system ErrorCodes.
     * <p>
     * This ensures that clients still receive granular error codes (e.g.,
     * MISSING_NAME,
     * NEGATIVE_SALARY) instead of a generic "Validation Failed" message, preserving
     * backward compatibility with the API's error contract.
     *
     * @param error the raw error object from the schema validator
     * @return a mapped ServiceException
     */
    private static ServiceException mapError(Exception error) {
        String msg = error.getMessage(); // ValidationException usually has a readable message

        if (msg.contains("name"))
            return new ServiceException(ErrorCode.MISSING_NAME);
        if (msg.contains("salary") && msg.contains("minimum"))
            return new ServiceException(ErrorCode.NEGATIVE_SALARY);
        if (msg.contains("salary"))
            return new ServiceException(ErrorCode.MISSING_SALARY);
        if (msg.contains("department"))
            return new ServiceException(ErrorCode.INVALID_DEPARTMENT);

        return new ServiceException(ErrorCode.VALIDATION_ERROR, "Schema Violation: " + msg);
    }
}
