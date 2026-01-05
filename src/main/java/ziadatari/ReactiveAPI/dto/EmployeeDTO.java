package ziadatari.ReactiveAPI.dto;

import io.vertx.core.json.JsonObject;

public class EmployeeDTO {
  private String id;
  private String name;
  private String department;

  public EmployeeDTO () {}

  public  EmployeeDTO (String id, String name, String department) {
    this.id = id;
    this.name = name;
    this.department = department;
  }

  //Helper to convert Vert.x JSON
  public JsonObject toJson() {
    return new JsonObject()
      .put("id", id)
      .put("name", name)
      .put("department", department);
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
}


