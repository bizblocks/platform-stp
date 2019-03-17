package com.groupstp.platform.core.bean;

import com.groupstp.platform.entity.Employee;
import com.haulmont.cuba.security.entity.User;

import java.util.Set;

/**
 * Server side employee logic component
 *
 * @author adiatullin
 */
public interface EmployeeWorker {
    String NAME = "plstp_EmployeeWorker";

    /**
     * Get or create a new employee of provided user
     *
     * @param user entity
     * @return employee
     */
    Employee getOrCreateEmployee(User user);

    /**
     * Retrieve all current subordinates from provided employee
     *
     * @param employee provided employee
     * @return subordinates of employee
     */
    Set<Employee> getAllSubordinates(Employee employee);

    /**
     * Return correct name of employee
     *
     * @param employee entity
     * @return name of employee
     */
    String getName(Employee employee);
}
