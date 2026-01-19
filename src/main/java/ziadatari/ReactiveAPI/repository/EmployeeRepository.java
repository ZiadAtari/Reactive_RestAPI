package ziadatari.ReactiveAPI.repository;

import ziadatari.ReactiveAPI.dto.EmployeeDTO;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Handles all direct database interactions for Employee records.
 * Uses the Vert.x MySQL client for non-blocking SQL operations.
 */
public class EmployeeRepository {

  private final Pool client;

  /**
   * Constructs an EmployeeRepository with a shared database connection pool.
   *
   * @param client the database client pool
   */
  public EmployeeRepository(Pool client) {
    this.client = client;
  }

  /**
   * Retrieves all active employees from the database.
   *
   * @return a Future containing a list of EmployeeDTOs
   */
  public Future<List<EmployeeDTO>> findAll() {
    return client.query("SELECT * FROM employees WHERE active = true")
        .execute()
        .map(this::mapRowSetToDTOs);
  }

  /**
   * Saves a new employee record to the database.
   * Generates a unique UUID as the primary key.
   *
   * @param employee the employee data to save
   * @return a Future that completes when the operation is done
   */
  public Future<Void> save(EmployeeDTO employee) {
    // Generate unique identifier (UUID v4) before persistence
    String id = UUID.randomUUID().toString();
    employee.setId(id);

    // Using prepared query for security (SQL injection prevention) and performance
    // The Tuple protects against malicious input in 'name' or 'department'
    return client.preparedQuery(
        "INSERT INTO employees (id, name, department, salary, last_modified_by, last_modified_at) Values (?, ?, ?, ?, ?, ?)")
        .execute(Tuple.of(employee.getId(), employee.getName(), employee.getDepartment(), employee.getSalary(),
            employee.getLastModifiedBy(), employee.getLastModifiedAt()))
        .mapEmpty();
  }

  /**
   * Updates an existing employee's details.
   *
   * @param id       the ID of the employee to update
   * @param employee the new data for the employee
   * @return a Future containing true if a row was updated, false otherwise
   */
  public Future<Boolean> update(String id, EmployeeDTO employee) {
    return client.preparedQuery(
        "UPDATE employees SET name = ?, department = ?, salary = ?, last_modified_by = ?, last_modified_at = ? WHERE id = ?")
        .execute(Tuple.of(employee.getName(), employee.getDepartment(), employee.getSalary(),
            employee.getLastModifiedBy(), employee.getLastModifiedAt(), id))
        .map(rowSet -> rowSet.rowCount() > 0);
  }

  /**
   * Performs a soft delete by marking an employee as inactive.
   *
   * @param id        the ID of the employee to delete
   * @param user      the user performing the deletion
   * @param timestamp the timestamp of the deletion
   * @return a Future containing true if a row was updated, false otherwise
   */
  public Future<Boolean> delete(String id, String user, String timestamp) {
    return client
        .preparedQuery("UPDATE employees SET active = false, last_modified_by = ?, last_modified_at = ? WHERE id = ?")
        .execute(Tuple.of(user, timestamp, id))
        .map(row -> row.rowCount() > 0);
  }

  /**
   * Finds an employee by name and department (used for duplicate checks).
   *
   * @param name       the employee name
   * @param department the department name
   * @return a Future containing the EmployeeDTO if found, or null
   */
  public Future<EmployeeDTO> findByNameAndDepartment(String name, String department) {
    return client.preparedQuery("SELECT * FROM employees WHERE name = ? AND department = ?")
        .execute(Tuple.of(name, department))
        .map(rows -> {
          if (rows.size() == 0)
            return null;
          return mapRowSetToDTOs(rows).get(0);
        });
  }

  /**
   * Reactivates a soft-deleted employee and updates their salary.
   *
   * @param id        the ID of the employee to reactivate
   * @param newSalary the new salary to be set
   * @return a Future that completes when the operation is done
   */
  public Future<Void> reactivate(String id, Double newSalary) {
    return client.preparedQuery("UPDATE employees SET active = true, salary = ? WHERE id = ?")
        .execute(Tuple.of(newSalary, id))
        .mapEmpty();
  }

  /**
   * Helper method to map a SQL RowSet to a list of EmployeeDTO objects.
   *
   * @param rows the RowSet containing database results
   * @return a list of DTOs
   */
  private List<EmployeeDTO> mapRowSetToDTOs(RowSet<Row> rows) {
    List<EmployeeDTO> result = new ArrayList<>();
    for (Row row : rows) {
      result.add(new EmployeeDTO(
          row.getString("id"),
          row.getString("name"),
          row.getString("department"),
          row.getDouble("salary"),
          row.getBoolean("active"),
          row.getString("last_modified_by"),
          row.getString("last_modified_at")));
    }
    return result;
  }

}
