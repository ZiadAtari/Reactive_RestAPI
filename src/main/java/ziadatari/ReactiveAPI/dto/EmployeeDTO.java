package ziadatari.ReactiveAPI.dto;

import io.vertx.core.json.JsonObject;

// DTO Object for Employee
// Isolation between API and DB

public class EmployeeDTO {
  private String id;
  private String name;
  private String department;
  private Double salary;
  private Boolean active;

  // Constructor for serialization libraries
  public EmployeeDTO () {
    this.active = true;
  }

  // Full constructor for service layer
  public  EmployeeDTO (String id, String name, String department, Double salary, Boolean active) {
    this.id = id;
    this.name = name;
    this.department = department;
    this.salary = salary;
    this.active = (active != null) ? active : true;
  }

  // Helper to convert object to Vert.x JSON
  public JsonObject toJson() {
    return new JsonObject()
      .put("id", id)
      .put("name", name)
      .put("department", department)
      .put("salary", salary)
      .put("active", active);

  }

  // Create an object from JSON
  public static EmployeeDTO fromJson(JsonObject json) {
    return new EmployeeDTO(
      json.getString("id"),
      json.getString("name"),
      json.getString("department"),
      json.getDouble("salary"),
      json.getBoolean("active", true) // defaults to true
    );
  }
  //Getters and Setters
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
  public void setSalary(Double salary) {this.salary = salary;}
  public Boolean isActive() {return active;}
  public void setActive(Boolean active) {this.active = active;}
}


