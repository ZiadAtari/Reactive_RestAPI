package ziadatari.ReactiveAPI.dto;

import io.vertx.core.json.JsonObject;

// DTO Object for Employee
// Isolation between API and DB

/**
 * Data Transfer Object for Employee information.
 * Acts as a bridge between the API layer and the internal logic/database,
 * ensuring decoupling and data isolation.
 */
public class EmployeeDTO {
  /** Unique identifier for the employee. UUID format. */
  private String id;
  /** Full name of the employee. */
  private String name;
  /** Department where the employee works. */
  private String department;
  /** Annual salary of the employee. */
  private Double salary;
  /** Employment status. True if currently employed, false otherwise. */
  private Boolean active;

  /**
   * Default constructor required for serialization libraries (e.g., Jackson).
   * Sets the 'active' status to true by default.
   */
  public EmployeeDTO() {
    this.active = true;
  }

  /**
   * Full constructor for manual instantiation in the service layer.
   *
   * @param id         the unique identifier of the employee
   * @param name       the name of the employee
   * @param department the department the employee belongs to
   * @param salary     the base salary of the employee
   * @param active     the employment status (defaults to true if null)
   */
  public EmployeeDTO(String id, String name, String department, Double salary, Boolean active) {
    this.id = id;
    this.name = name;
    this.department = department;
    this.salary = salary;
    this.active = (active != null) ? active : true;
  }

  /**
   * Helper method to convert this DTO to a Vert.x JsonObject.
   *
   * @return a JsonObject containing employee data
   */
  public JsonObject toJson() {
    return new JsonObject()
        .put("id", id)
        .put("name", name)
        .put("department", department)
        .put("salary", salary)
        .put("active", active);

  }

  /**
   * Static factory method to create an EmployeeDTO from a Vert.x JsonObject.
   *
   * @param json the JsonObject to parse
   * @return a new EmployeeDTO instance
   */
  public static EmployeeDTO fromJson(JsonObject json) {
    return new EmployeeDTO(
        json.getString("id"),
        json.getString("name"),
        json.getString("department"),
        json.getDouble("salary"),
        json.getBoolean("active", true) // defaults to true
    );
  }

  // Standard Getters and Setters

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDepartment() {
    return department;
  }

  public void setDepartment(String department) {
    this.department = department;
  }

  public Double getSalary() {
    return salary;
  }

  public void setSalary(Double salary) {
    this.salary = salary;
  }

  public Boolean isActive() {
    return active;
  }

  public void setActive(Boolean active) {
    this.active = active;
  }
}
