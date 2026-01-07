package ziadatari.ReactiveAPI.service;

import io.vertx.core.Future;
import ziadatari.ReactiveAPI.dto.EmployeeDTO;
import ziadatari.ReactiveAPI.exception.ErrorCode;
import ziadatari.ReactiveAPI.exception.ServiceException;
import ziadatari.ReactiveAPI.repository.EmployeeRepository;

import java.util.List;

// Business Logic

public class EmployeeService {

  private final EmployeeRepository repository;

  // Dependency Injection
  public EmployeeService(EmployeeRepository employeeRepository) {
    this.repository = employeeRepository;
  }

  public Future<List<EmployeeDTO>> getAllEmployees() {
    // Passthrough, no logic needed
    return repository.findAll();
  }

  public Future<Void> createEmployee(EmployeeDTO dto) {
    // Validate Name
    if (dto.getName() == null || dto.getName().isBlank()) {
      return Future.failedFuture(new ServiceException(ErrorCode.MISSING_NAME));
    }
    // Validate department
    if (dto.getDepartment() == null || dto.getDepartment().isBlank()) {
      return Future.failedFuture(new ServiceException(ErrorCode.INVALID_DEPARTMENT));
    }
    // Validate Salary != null
    if (dto.getSalary() == null)  {
      return Future.failedFuture(new ServiceException(ErrorCode.MISSING_SALARY));
    }
    // Validate salary value
    if (dto.getSalary() < 0) {
      return Future.failedFuture(new ServiceException(ErrorCode.NEGATIVE_SALARY));
    }
    // If valid, proceed to storage
    return repository.save(dto);
  }

  public Future<Boolean> updateEmployee(String id, EmployeeDTO dto) {
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
