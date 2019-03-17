package com.groupstp.platform.service;

import com.groupstp.platform.entity.Employee;
import com.haulmont.cuba.security.entity.User;

import java.util.Set;

/**
 * This service provide logic to work with employees
 *
 * @author adiatullin
 */
public interface EmployeeService {
    String NAME = "plstp_EmployeeService";

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
}