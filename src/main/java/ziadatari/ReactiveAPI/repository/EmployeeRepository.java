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

public class EmployeeRepository {

  private final Pool client;

  // Dependency Injection: Pool passed in but not created
  public EmployeeRepository(Pool client) {
    this.client = client;
  }

  // GET /employees
  public Future<List<EmployeeDTO>> findAll() {
    return client.query("SELECT * FROM employees")
      .execute()
      .map(this::mapRowSetToDTOs);
  }

  //POST /employees
  public Future<Void> save(EmployeeDTO employee) {
    //Generate ID MANUALLY
    String id = UUID.randomUUID().toString();
    employee.setId(id);

    // Tuple for immutability
    return client.preparedQuery("INSERT INTO employees (id, name, department, salary) Values (?, ?, ?, ?)")
      .execute(Tuple.of(employee.getId(), employee.getName(), employee.getDepartment(), employee.getSalary()))
      .mapEmpty(); //Doesn't return result, just success or failure
  }

  // PUT /employees/:id
  public Future<Boolean> update(String id, EmployeeDTO employee) {
    return client.preparedQuery("UPDATE employees SET name = ?, department = ?, salary = ? WHERE id = ?")
      .execute(Tuple.of(employee.getName(), employee.getDepartment(), employee.getSalary(), id))
      .map(rowSet -> rowSet.rowCount() > 0); //return true if row found & updated
  }

  // DELETE /employees/:id
  public Future<Boolean> delete(String id) {
    return client.preparedQuery("DELETE FROM employees WHERE id = ?")
      .execute(Tuple.of(id))
      .map(row -> row.rowCount() > 0);
  }

  // Private Helper
  // Keeps public methods clean by isolating mapper logic
  private List<EmployeeDTO> mapRowSetToDTOs(RowSet<Row> rows) {
    List<EmployeeDTO> result = new ArrayList<>();
    for (Row row : rows) {
      result.add(new EmployeeDTO(
        row.getString("id"),
        row.getString("name"),
        row.getString("department"),
        row.getDouble("salary")
      ));
    }
    return result;
  }

}
