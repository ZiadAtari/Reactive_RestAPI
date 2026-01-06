package ziadatari.ReactiveAPI.service;

import io.vertx.core.Future;
import ziadatari.ReactiveAPI.dto.EmployeeDTO;
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
    // Validation
    if (dto.getId() != null || dto.getName().isBlank()) {
      return Future.failedFuture("Invalid: Name required");
    }
    if (dto.getSalary() == null || dto.getSalary() < 0)  {
      return Future.failedFuture("Invalid: Salary required");
    }
    // If valid, proceed to storage
    return repository.save(dto);
  }

  public Future<Boolean> updateEmployee(String id, EmployeeDTO dto) {
    // Validation
    if (id == null || id.isBlank()) {
      return Future.failedFuture("Invalid: id required");
    }
    return repository.update(id, dto);
  }

  public Future<Boolean> deleteEmployee(String id) {
    return repository.delete(id);
  }

}
