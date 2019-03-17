package com.groupstp.platform.core.bean.employee;

import com.groupstp.platform.core.bean.EmployeeWorker;
import com.groupstp.platform.core.bean.MessageableBean;
import com.groupstp.platform.entity.Employee;
import com.haulmont.bali.util.Preconditions;
import com.haulmont.cuba.core.EntityManager;
import com.haulmont.cuba.core.Persistence;
import com.haulmont.cuba.core.Transaction;
import com.haulmont.cuba.core.TypedQuery;
import com.haulmont.cuba.core.entity.BaseUuidEntity;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.LoadContext;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.security.entity.User;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Employee working bean
 *
 * @author adiatullin
 */
@Component(EmployeeWorker.NAME)
public class EmployeeWorkerBean extends MessageableBean implements EmployeeWorker {
    private static final Logger log = LoggerFactory.getLogger(EmployeeWorkerBean.class);

    @Inject
    protected Persistence persistence;
    @Inject
    protected Metadata metadata;
    @Inject
    protected DataManager dataManager;


    @Override
    public Employee getOrCreateEmployee(User user) {
        Preconditions.checkNotNullArgument(user, getMessage("EmployeeWorkerBean.emptyUser"));

        Employee employee;
        try (Transaction tr = persistence.getTransaction()) {
            EntityManager em = persistence.getEntityManager();

            TypedQuery<Employee> query = em.createQuery("select e from plstp$Employee e where e.user.id = :userId", Employee.class);
            query.setParameter("userId", user.getId());
            query.setViewName("employee-with-user");
            query.setMaxResults(1);
            employee = query.getFirstResult();

            if (employee == null) {
                boolean exist = false;
                if (!StringUtils.isEmpty(user.getEmail())) {
                    query = em.createQuery("select e from plstp$Employee e where e.email = :email", Employee.class);
                    query.setParameter("email", user.getEmail());
                    query.setViewName("employee-with-user");
                    query.setMaxResults(1);
                    employee = query.getFirstResult();

                    if (employee != null) {
                        exist = true;

                        log.warn(String.format("Found employee with email '%s' and user '%s'. But another user '%s' has a email which are set in employee. User in employee '%s' will be changed",
                                employee.getEmail(), employee.getUser() == null ? "N/A" : employee.getUser().getInstanceName(), user.getInstanceName(), employee.getInstanceName()));
                    }
                }

                if (!exist) {
                    employee = metadata.create(Employee.class);
                }
                employee.setUser(user);
                employee.setEmail(user.getEmail());
                employee.setName(StringUtils.isEmpty(user.getFirstName()) ? user.getLogin() : user.getFirstName());
                String fullName = user.getName();
                if (!StringUtils.isEmpty(user.getFirstName())) {
                    fullName = user.getFirstName() +
                            (StringUtils.isEmpty(user.getMiddleName()) ? StringUtils.EMPTY : " " + user.getMiddleName()) +
                            (StringUtils.isEmpty(user.getLastName()) ? StringUtils.EMPTY : " " + user.getLastName());
                }
                employee.setFullName(fullName);

                if (exist) {
                    em.merge(employee);
                } else {
                    em.persist(employee);
                }
            }

            tr.commit();
        }

        return employee;
    }

    @Override
    public Set<Employee> getAllSubordinates(Employee employee) {
        Preconditions.checkNotNullArgument(employee, getMessage("EmployeeWorkerBean.emptyEmployee"));
        return getAllSubordinates(Collections.singletonList(employee));
    }

    @Override
    public String getName(Employee employee) {
        Preconditions.checkNotNullArgument(employee, getMessage("EmployeeWorkerBean.emptyEmployee"));

        String name = employee.getFullName();
        if (StringUtils.isEmpty(name)) {
            name = employee.getName();
            if (StringUtils.isEmpty(name)) {
                User user = employee.getUser();
                if (user != null) {
                    name = user.getName();
                    if (StringUtils.isEmpty(name)) {
                        if (!StringUtils.isEmpty(user.getFirstName())) {
                            name = user.getFirstName() +
                                    (StringUtils.isEmpty(user.getMiddleName()) ? StringUtils.EMPTY : " " + user.getMiddleName()) +
                                    (StringUtils.isEmpty(user.getLastName()) ? StringUtils.EMPTY : " " + user.getLastName());
                        } else {
                            name = user.getLogin();
                        }
                    }
                }
            }
        }
        return name;
    }

    protected Set<Employee> getAllSubordinates(List<Employee> employees) {
        if (CollectionUtils.isEmpty(employees)) {
            return Collections.emptySet();
        }

        Set<Employee> result = new HashSet<>();
        int batchSize = 50;
        Set<UUID> batch = new HashSet<>(batchSize);

        processEmployees(result, batch, batchSize, reloadEmployees(employees.stream().map(BaseUuidEntity::getId).collect(Collectors.toSet())), false);
        endEmployeesBatch(result, batch, batchSize);

        return result;
    }

    protected void processEmployees(Set<Employee> result, Set<UUID> batch, int batchSize, List<Employee> employees, boolean put) {
        if (!CollectionUtils.isEmpty(employees)) {
            List<Employee> processChildren = null;

            for (Employee employee : employees) {
                if (!result.contains(employee)) {
                    if (put) {
                        result.add(employee);
                    }

                    if (!CollectionUtils.isEmpty(employee.getSubordinates())) {
                        if (processChildren == null) {
                            processChildren = new ArrayList<>();
                        }
                        processChildren.add(employee);
                    }
                }
            }

            if (!CollectionUtils.isEmpty(processChildren)) {
                for (Employee employee : processChildren) {
                    for (Employee subordinate : employee.getSubordinates()) {
                        if (!result.contains(subordinate)) {
                            batch.add(subordinate.getId());

                            if (batch.size() >= batchSize) {
                                List<Employee> children = reloadEmployees(batch);
                                batch.clear();
                                processEmployees(result, batch, batchSize, children, true);
                            }
                        }
                    }
                }
            }
        }
    }

    protected void endEmployeesBatch(Set<Employee> result, Set<UUID> batch, int batchSize) {
        if (batch.size() > 0) {
            List<Employee> employees = reloadEmployees(batch);
            batch.clear();
            processEmployees(result, batch, batchSize, employees, true);
            endEmployeesBatch(result, batch, batchSize);
        }
    }

    protected List<Employee> reloadEmployees(Set<UUID> ids) {
        return dataManager.loadList(LoadContext.create(Employee.class)
                .setQuery(new LoadContext.Query("select e from plstp$Employee e where e.id in :ids")
                        .setParameter("ids", ids))
                .setView("employee-hierarchy"));
    }
}
