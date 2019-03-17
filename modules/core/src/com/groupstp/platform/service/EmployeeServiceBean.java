package com.groupstp.platform.service;

import com.groupstp.platform.core.bean.EmployeeWorker;
import com.groupstp.platform.entity.Employee;
import com.haulmont.cuba.security.entity.User;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Set;

/**
 * Base implementation of employee service
 *
 * @author adiatullin
 */
@Service(EmployeeService.NAME)
public class EmployeeServiceBean implements EmployeeService {

    @Inject
    private EmployeeWorker worker;

    @Override
    public Employee getOrCreateEmployee(User user) {
        return worker.getOrCreateEmployee(user);
    }

    @Override
    public Set<Employee> getAllSubordinates(Employee employee) {
        return worker.getAllSubordinates(employee);
    }
}
