package ziadatari.ReactiveAPI.service;

import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.core.Future;
import ziadatari.ReactiveAPI.dto.EmployeeDTO;
import ziadatari.ReactiveAPI.exception.ErrorCode;
import ziadatari.ReactiveAPI.exception.ServiceException;
import ziadatari.ReactiveAPI.repository.EmployeeRepository;

import java.util.List;

// Business Logic

/**
 * Service layer responsible for business logic, validation, and fault
 * tolerance.
 * Acts as an intermediary between the repository and the application interface.
 * Uses a Circuit Breaker to ensure system stability during database issues.
 */
public class EmployeeService {

  private final EmployeeRepository repository;
  private final CircuitBreaker circuitBreaker;

  /**
   * Constructs an EmployeeService with its dependencies.
   *
   * @param employeeRepository the repository for database access
   * @param circuitBreaker     the circuit breaker for fault tolerance
   */
  public EmployeeService(EmployeeRepository employeeRepository, CircuitBreaker circuitBreaker) {
    this.repository = employeeRepository;
    this.circuitBreaker = circuitBreaker;
  }

  /**
   * Fetches all active employees.
   * Wrapped in a Circuit Breaker to prevent resource exhaustion if the DB is
   * slow/down.
   *
   * @return a Future containing a list of employees
   */
  public Future<List<EmployeeDTO>> getAllEmployees() {
    return circuitBreaker.execute(promise -> {
      repository.findAll().onSuccess(promise::complete).onFailure(promise::fail);
    });
  }

  /**
   * Orchestrates the creation of a new employee, including validation.
   * Wrapped in a Circuit Breaker.
   *
   * @param dto the employee data to create
   * @return a Future containing the created employee (with assigned ID)
   */
  public Future<EmployeeDTO> createEmployee(EmployeeDTO dto) {
    return circuitBreaker.execute(promise -> {
      createEmployeeLogic(dto).onSuccess(promise::complete).onFailure(promise::fail);
    });
  }

  /**
   * Internal logic for employee creation, including input validation and
   * duplicate checks.
   *
   * @param dto the employee data
   * @return a Future with the result
   */
  private Future<EmployeeDTO> createEmployeeLogic(EmployeeDTO dto) {
    // 1. Mandatory field validation
    // Ensure all critical fields are present before hitting the DB
    if (dto.getName() == null || dto.getName().isBlank()) {
      return Future.failedFuture(new ServiceException(ErrorCode.MISSING_NAME));
    }
    if (dto.getDepartment() == null || dto.getDepartment().isBlank()) {
      return Future.failedFuture(new ServiceException(ErrorCode.INVALID_DEPARTMENT));
    }
    if (dto.getSalary() == null) {
      return Future.failedFuture(new ServiceException(ErrorCode.MISSING_SALARY));
    }
    if (dto.getSalary() < 0) {
      return Future.failedFuture(new ServiceException(ErrorCode.NEGATIVE_SALARY));
    }

    // 2. Set Audit Timestamp
    dto.setLastModifiedAt(java.time.Instant.now().toString());

    // 3. Conflict detection and recovery (Soft Delete handling)
    // We check if an employee with the same Name/Dept exists
    return repository.findByNameAndDepartment(dto.getName(), dto.getDepartment())
        .compose(existing -> {

          if (existing == null) {
            // CASE 1: Brand-new employee entry
            return repository.save(dto).map(dto);
          }

        else if (existing.isActive()) {
            // CASE 2: Active duplicate found - reject creation to prevent data
            // inconsistency
            return Future.failedFuture(new ServiceException(ErrorCode.DUPLICATE_EMPLOYEE));
          }

        else {
            // CASE 3: Inactive record found (Soft Deleted)
            // Reactivate the old record instead of creating a new one to preserve history
            dto.setId(existing.getId());
            return repository.reactivate(existing.getId(), dto.getSalary())
                .map(dto);
          }
        });
  }

  /**
   * Orchestrates the update of an existing employee.
   *
   * @param id  the ID of the employee to update
   * @param dto the new data
   * @return a Future indicating success
   */
  public Future<Boolean> updateEmployee(String id, EmployeeDTO dto) {
    return circuitBreaker.execute(promise -> {
      updateEmployeeLogic(id, dto).onSuccess(promise::complete).onFailure(promise::fail);
    });
  }

  /**
   * Internal logic for updating an employee, including validation and existence
   * checks.
   */
  private Future<Boolean> updateEmployeeLogic(String id, EmployeeDTO dto) {
    if (id == null || id.isBlank()) {
      return Future.failedFuture(new ServiceException(ErrorCode.EMPLOYEE_ID_REQUIRED));
    }
    if (dto.getName() == null || dto.getName().isBlank()) {
      return Future.failedFuture(new ServiceException(ErrorCode.MISSING_NAME));
    }
    if (dto.getSalary() != null && dto.getSalary() < 0) {
      return Future.failedFuture(new ServiceException(ErrorCode.NEGATIVE_SALARY));
    }
    if (dto.getDepartment() != null && dto.getDepartment().isBlank()) {
      return Future.failedFuture(new ServiceException(ErrorCode.INVALID_DEPARTMENT));
    }

    // Set Audit Timestamp
    dto.setLastModifiedAt(java.time.Instant.now().toString());

    return repository.update(id, dto)
        .flatMap(found -> {
          if (!found) {
            return Future.failedFuture(new ServiceException(ErrorCode.EMPLOYEE_NOT_FOUND));
          }
          return Future.succeededFuture(true);
        });
  }

  /**
   * Orchestrates the deletion of an employee by ID.
   *
   * @param id the employee ID
   * @return a Future indicating success
   */
  public Future<Boolean> deleteEmployee(String id) {
    return deleteEmployee(id, "anonymous");
  }

  /**
   * Orchestrates the deletion of an employee by ID with audit info.
   *
   * @param id   the employee ID
   * @param user the user performing the deletion
   * @return a Future indicating success
   */
  public Future<Boolean> deleteEmployee(String id, String user) {
    return circuitBreaker.execute(promise -> {
      deleteEmployeeLogic(id, user).onSuccess(promise::complete).onFailure(promise::fail);
    });
  }

  /**
   * Internal logic for deletion.
   */
  private Future<Boolean> deleteEmployeeLogic(String id, String user) {
    if (id == null || id.isBlank()) {
      return Future.failedFuture(new ServiceException(ErrorCode.EMPLOYEE_ID_REQUIRED));
    }

    String timestamp = java.time.Instant.now().toString();

    return repository.delete(id, user, timestamp)
        .flatMap(found -> {
          if (!found) {
            return Future.failedFuture(new ServiceException(ErrorCode.EMPLOYEE_NOT_FOUND));
          }
          return Future.succeededFuture(true);
        });
  }

}
