# Leave Management System - Quick Start Guide

## System Requirements
- Java 11 or higher
- Maven 3.6+
- Windows/Linux/Mac

## Quick Start

### Step 1: Build the Project
```bash
cd "d:\My details\M.S\PAI\Leave_management_project"
mvn clean install
```

### Step 2: Run the Application
```bash
mvn exec:java -Dexec.mainClass="com.leavemanagement.LeaveManagementApplication"
```

### Step 3: First Time Setup
When you run the application for the first time:
1. The SQLite database will be automatically created
2. All required tables will be initialized
3. You can start adding employees and managing leaves

## Project Features at a Glance

### For Employees
- Register in the system
- Apply for leave
- View leave balance
- Track leave requests status
- View approved/rejected leaves

### For Managers
- View all employees
- Approve/Reject leave requests
- View department-wise pending requests
- Monitor leave usage

### For HR
- Add and manage employees
- Configure leave types
- Reset yearly leave balances
- View leave statistics

## File Structure

```
leave-management-project/
├── src/main/java/com/leavemanagement/
│   ├── LeaveManagementApplication.java    # Main entry point
│   ├── entity/                            # Data models
│   ├── service/                           # Business logic
│   ├── dao/                               # Database access
│   └── util/                              # Utility classes
├── pom.xml                                # Maven configuration
└── README.md                              # Full documentation
```

## Key Classes

| Class | Purpose |
|-------|---------|
| Employee | Employee data model |
| LeaveRequest | Leave request data model |
| LeaveBalance | Leave balance tracking |
| LeaveType | Leave type configuration |
| EmployeeService | Employee operations |
| LeaveRequestService | Leave request operations |
| LeaveService | Leave balance operations |
| DatabaseConnection | Database setup & connection |

## Sample Data to Add

### Add a Test Employee
```
First Name: John
Last Name: Doe
Email: john.doe@company.com
Department: IT
Designation: Software Engineer
Date of Joining: 2023-01-15
Salary: 50000
```

### Apply Test Leave
```
Employee ID: 1
Leave Type ID: 2 (Casual Leave)
Start Date: 2026-09-01
End Date: 2026-09-03
Reason: Personal work
```

## Database Location
The SQLite database file `leavemanagement.db` will be created in the project root directory automatically.

## Troubleshooting

### If Maven is not recognized:
- Install Maven from https://maven.apache.org/
- Add Maven bin folder to system PATH

### If Java is not recognized:
- Install Java 11+ from https://www.oracle.com/java/technologies/downloads/
- Set JAVA_HOME environment variable

### Database connection issues:
- Check that leavemanagement.db has read/write permissions
- Delete the database file and restart to recreate it

## Default Configuration

- **Database Type**: SQLite (file-based, no external server needed)
- **Port**: N/A (embedded database)
- **Connection URL**: jdbc:sqlite:leavemanagement.db

## Build Outputs

After building:
- JAR file: `target/leave-management-system.jar`
- Compiled classes: `target/classes/`

## Running the JAR directly

```bash
java -jar target/leave-management-system.jar
```

## Console Menu Navigation

1. Use numeric keys (1-8) to navigate menus
2. Enter data as prompted
3. Dates should be in yyyy-MM-dd format
4. Enter 'Back' option to return to previous menu

## Support & Help

- Full documentation in README.md
- Check console output for error messages
- Ensure all inputs are valid before submitting

---

Happy managing leaves! 🎉
