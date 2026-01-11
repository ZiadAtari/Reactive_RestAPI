package ziadatari.ReactiveAPI.service;

import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.core.Future;
import ziadatari.ReactiveAPI.dto.EmployeeDTO;
import ziadatari.ReactiveAPI.exception.ErrorCode;
import ziadatari.ReactiveAPI.exception.ServiceException;
import ziadatari.ReactiveAPI.repository.EmployeeRepository;

import java.util.List;

// Business Logic

public class EmployeeService {

  private final EmployeeRepository repository;
  private final CircuitBreaker circuitBreaker;

  // Dependency Injection
  public EmployeeService(EmployeeRepository employeeRepository, CircuitBreaker circuitBreaker) {
    this.repository = employeeRepository;
    this.circuitBreaker = circuitBreaker;
  }

  /**
   * Fetches all employees from the database.
   * This call is wrapped in a Circuit Breaker.
   * If the database is under heavy load or down, the breaker will "trip" (open)
   * after 5 failures, preventing further calls from wasting system resources.
   */
  public Future<List<EmployeeDTO>> getAllEmployees() {
    return circuitBreaker.execute(promise -> {
      repository.findAll().onSuccess(promise::complete).onFailure(promise::fail);
    });
  }

  /**
   * Creates a new employee with business validation.
   * Also wrapped in the Circuit Breaker to protect the database 'save' operation.
   */
  public Future<EmployeeDTO> createEmployee(EmployeeDTO dto) {
    return circuitBreaker.execute(promise -> {
      createEmployeeLogic(dto).onSuccess(promise::complete).onFailure(promise::fail);
    });
  }

  private Future<EmployeeDTO> createEmployeeLogic(EmployeeDTO dto) {
    // Validate Name
    if (dto.getName() == null || dto.getName().isBlank()) {
      return Future.failedFuture(new ServiceException(ErrorCode.MISSING_NAME));
    }
    // Validate department
    if (dto.getDepartment() == null || dto.getDepartment().isBlank()) {
      return Future.failedFuture(new ServiceException(ErrorCode.INVALID_DEPARTMENT));
    }
    // Validate Salary != null
    if (dto.getSalary() == null) {
      return Future.failedFuture(new ServiceException(ErrorCode.MISSING_SALARY));
    }
    // Validate salary value
    if (dto.getSalary() < 0) {
      return Future.failedFuture(new ServiceException(ErrorCode.NEGATIVE_SALARY));
    }

    // Check for duplicates
    return repository.findByNameAndDepartment(dto.getName(), dto.getDepartment())
        .compose(existing -> {

          if (existing == null) {
            // CASE 1: Brand-new employee
            return repository.save(dto).map(dto);
          }

        else if (existing.isActive()) {
            // CASE 2: Strict Duplicate (Active)
            return Future.failedFuture(new ServiceException(ErrorCode.DUPLICATE_EMPLOYEE));
          }

        else {
            // CASE 3: Reactivate (Soft Deleted)
            dto.setId(existing.getId());

            return repository.reactivate(existing.getId(), dto.getSalary())
                .map(dto); // Return the DTO with the old ID
          }
        });
  }

  public Future<Boolean> updateEmployee(String id, EmployeeDTO dto) {
    return circuitBreaker.execute(promise -> {
      updateEmployeeLogic(id, dto).onSuccess(promise::complete).onFailure(promise::fail);
    });
  }

  private Future<Boolean> updateEmployeeLogic(String id, EmployeeDTO dto) {
    // 1. Validate ID
    if (id == null || id.isBlank()) {
      return Future.failedFuture(new ServiceException(ErrorCode.EMPLOYEE_ID_REQUIRED));
    }

    // 2. Validate Name
    if (dto.getName() == null || dto.getName().isBlank()) {
      return Future.failedFuture(new ServiceException(ErrorCode.MISSING_NAME));
    }

    // 3. Validate Salary
    if (dto.getSalary() != null && dto.getSalary() < 0) {
      return Future.failedFuture(new ServiceException(ErrorCode.NEGATIVE_SALARY));
    }

    // 4. Validate Department
    if (dto.getDepartment() != null && dto.getDepartment().isBlank()) {
      return Future.failedFuture(new ServiceException(ErrorCode.INVALID_DEPARTMENT));
    }

    return repository.update(id, dto)
        .flatMap(found -> {
          if (!found) {
            return Future.failedFuture(new ServiceException(ErrorCode.EMPLOYEE_NOT_FOUND));
          }
          return Future.succeededFuture(true);
        });
  }

  public Future<Boolean> deleteEmployee(String id) {
    return circuitBreaker.execute(promise -> {
      deleteEmployeeLogic(id).onSuccess(promise::complete).onFailure(promise::fail);
    });
  }

  private Future<Boolean> deleteEmployeeLogic(String id) {
    if (id == null || id.isBlank()) {
      return Future.failedFuture(new ServiceException(ErrorCode.EMPLOYEE_ID_REQUIRED));
    }

    return repository.delete(id)
        .flatMap(found -> {
          if (!found) {
            return Future.failedFuture(new ServiceException(ErrorCode.EMPLOYEE_NOT_FOUND));
          }
          return Future.succeededFuture(true);
        });
  }

}
