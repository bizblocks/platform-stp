package com.groupstp.platform.entity;

import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.security.entity.User;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author adiatullin
 */
@NamePattern("%s %s|fullName,email")
@Table(name = "PLSTP_EMPLOYEE")
@Entity(name = "plstp$Employee")
public class Employee extends StandardEntity {
    private static final long serialVersionUID = -5685310984554930822L;

    @NotNull
    @Column(name = "NAME", length = 100, nullable = false)
    private String name;

    @Column(name = "EMAIL", unique = true, length = 50)
    private String email;

    @Column(name = "FULL_NAME")
    private String fullName;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MANAGER_ID")
    private Employee manager;

    @OneToMany(mappedBy = "manager")
    private List<Employee> subordinates;


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Employee getManager() {
        return manager;
    }

    public void setManager(Employee manager) {
        this.manager = manager;
    }

    public List<Employee> getSubordinates() {
        return subordinates;
    }

    public void setSubordinates(List<Employee> subordinates) {
        this.subordinates = subordinates;
    }
}