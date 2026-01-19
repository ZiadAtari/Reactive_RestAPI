# Reactive REST API



### Overview:

This application is a reactive microservice built with Eclipse Vert.x. It differs from traditional servlet application by utilizing a asynchronous, non-blocking event loop incorporating:

* Reactive architecture: All I/O operations are non-blocking, using promises and future patterns to handle asynchronous results to prevent blocking the event loop.
* Packaged Layered design:

 	- Main Package: Initializes verticals and starts server.

 	- Web Package: Handles HTTP routing and JSON parsing.

 	- Service Package: encapsulates business logic and validation.

 	- Repository Package: Handles SQL queries and DB management.

 	- DTO Package: Handles DTO creation for object isolation.

 	- Exception Package: Error handling and exception management.






### How to Use:

1. ##### GET /v3/employees

**Function:** Receives list of all active employees
**Command:** curl -X GET http://localhost:8888/v3/employees

##### 

##### 2\. POST /v3/employees

**Function:** Creates a new employee \& Reactivates soft-deleted employee

**Command:** curl -X POST http://localhost:8888/v3/employees \\

 	  -H "Content-Type: application/json" \\

 	  -d '{

 	    "name": "Jane Smith",

 	    "department": "Finance",

 	   "salary": 82000.0

 	  }'



##### 3\. PUT /v3/employees/:id
**Function:** Updates existing employee details
**Command:** curl -X PUT http://localhost:8888/v3/employees/550e8400-e29b-41d4-a716-446655440000 \\

 	  -H "Content-Type: application/json" \\

 	  -d '{

 	    "name": "John Doe",

  	  "department": "Engineering",

 	   "salary": 80000.0

 	  }'



##### 4\. DELETE /employees/:id

**Function:** Soft-deletes employee

**Command:** curl -X DELETE http://localhost:8888/v3/employees/550e8400-e29b-41d4-a716-446655440000



### Changelog:

#### V3: Authentication & Containerization
* ###### v3.3.1 Bug Fixes
* ###### v3.3.0 DTO Improvments
* ###### v3.2.1 Error Handling 
* ###### v3.2.0 User Login added
* ###### v3.1.0 AuthVerticle added and refactored auth logic 
* ###### v3.0.3 Expanded Documentation folder 
* ###### v3.0.2 Polishing & Bug fixes
* ###### v3.0.1 Exception Handling for Invalid Keys
* ###### v3.0.0 JWT Authentication & API versioning added

#### V2: API Safeguards and Improvements
* ###### v2.2.0 Refactoring & Documentation 
* ###### v2.1.3 Polishing & Documentation 
* ###### v2.1.2 Fixed Race condition for Delete & Post
* ###### v2.1.1 Added Event Bus to isolate repository and service logic from web layer
* ###### v2.1.0 IP verification handler added
* ###### v2.0.1 Rate Limiting fixed for Race Conditions
* ###### v2.0.0 Rate Limiting and Circuit Breaking added
 
#### V1: Core Functionality
* ###### v1.3.0 Duplicate POST Handling \& Reactivation added
* ###### v1.2.0 Exception Handling added
* ###### v1.1.0 Soft-deletion added
* ###### v1.0.0 CURD Functionality Implemented
#### V0: Initial Setup
* ###### v0.2.0 Env Setup
* ###### v0.1.0 Structure Setup
