package com.groupstp.platform.web.employee;

import com.groupstp.platform.entity.Employee;
import com.groupstp.platform.service.EmployeeService;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.LoadContext;
import com.haulmont.cuba.core.global.View;
import com.haulmont.cuba.gui.components.EntityCombinedScreen;
import com.haulmont.cuba.gui.components.FieldGroup;
import com.haulmont.cuba.gui.components.SuggestionPickerField;
import com.haulmont.cuba.gui.data.CollectionDatasource;
import com.haulmont.cuba.gui.data.Datasource;
import com.haulmont.cuba.security.entity.User;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import javax.inject.Inject;
import java.util.*;

/**
 * @author adiatullin
 */
public class EmployeeBrowse extends EntityCombinedScreen {

    @Inject
    protected DataManager dataManager;
    @Inject
    protected EmployeeService employeeService;

    @Inject
    protected SuggestionPickerField userField;
    @Inject
    protected SuggestionPickerField managerField;
    @Inject
    protected Datasource<Employee> employeeDs;
    @Inject
    protected CollectionDatasource<Employee, UUID> subordinatesDs;
    @Inject
    protected FieldGroup fieldGroup;

    protected Set<Employee> ignoreEmployees = new HashSet<>();

    @Override
    public void init(Map<String, Object> params) {
        super.init(params);

        initUserSelectionEdit();
        initManagerSelectionEdit();
        initSubordinatesTable();
    }

    protected void initUserSelectionEdit() {
        userField.removeAllActions();
        userField.addLookupAction();
        userField.addClearAction();
        userField.setMinSearchStringLength(1);
        userField.setAsyncSearchDelayMs(200);
        userField.setSuggestionsLimit(15);
        userField.setSearchExecutor((searchString, searchParams) -> {
            if (StringUtils.isEmpty(searchString)) {
                return Collections.emptyList();
            } else {
                LoadContext<User> ctx = LoadContext.create(User.class)
                        .setQuery(new LoadContext.Query("select e from sec$User e where lower(e.name) like :search")
                                .setParameter("search", "%" + searchString.toLowerCase() + "%")
                                .setMaxResults(15))
                        .setView("user-with-name");
                return dataManager.loadList(ctx);
            }
        });
        employeeDs.addItemPropertyChangeListener(e -> {
            if ("user".equals(e.getProperty())) {//set name automatically to fields if user selected
                User user = (User) e.getValue();
                if (user != null) {
                    Employee employee = (Employee) getFieldGroup().getDatasource().getItem();
                    if (StringUtils.isEmpty(employee.getName())) {
                        employee.setName(user.getName());
                    }
                    if (StringUtils.isEmpty(employee.getFullName())) {
                        String fullName = getNullSafe(user.getFirstName()) + " " +
                                getNullSafe(user.getMiddleName()) + " " +
                                getNullSafe(user.getLastName());
                        if (!StringUtils.isBlank(fullName)) {
                            employee.setFullName(fullName);
                        }
                    }
                }
            }
        });
    }

    protected void initManagerSelectionEdit() {
        managerField.removeAllActions();
        managerField.addClearAction();
        managerField.setMinSearchStringLength(1);
        managerField.setAsyncSearchDelayMs(200);
        managerField.setSuggestionsLimit(15);
        managerField.setSearchExecutor((searchString, searchParams) -> {
            if (StringUtils.isEmpty(searchString)) {
                return Collections.emptyList();
            } else {
                LoadContext<Employee> ctx = LoadContext.create(Employee.class)
                        .setQuery(new LoadContext.Query("select e from plstp$Employee e where lower(e.name) like :search")
                                .setParameter("search", "%" + searchString.toLowerCase() + "%")
                                .setMaxResults(15))
                        .setView(View.MINIMAL);
                List<Employee> list = new ArrayList<>(dataManager.loadList(ctx));
                list.removeAll(ignoreEmployees);
                return list;
            }
        });
    }

    protected void initSubordinatesTable() {
        employeeDs.addItemChangeListener(e -> {
            subordinatesDs.clear();
            ignoreEmployees.clear();

            Employee currentEmployee = employeeDs.getItem();
            if (currentEmployee != null) {
                Set<Employee> list = employeeService.getAllSubordinates(currentEmployee);
                if (!CollectionUtils.isEmpty(list)) {
                    for (Employee employee : list) {
                        subordinatesDs.includeItem(employee);
                        ignoreEmployees.add(employee);
                    }
                }
                ignoreEmployees.add(currentEmployee);
            }
        });
    }

    @Override
    public boolean validate(List<Validatable> fields) {
        if (super.validate(fields)) {
            Employee item = (Employee) getFieldGroup().getDatasource().getItem();
            if (!isUnique(item)) {
                showNotification(getMessage("employeeBrowse.employeeWithThisEmailAlreadyExist"), NotificationType.WARNING);
                fieldGroup.getFieldNN("email").getComponentNN().requestFocus();
                return false;
            }
            if (isUserUsed(item)) {
                showNotification(getMessage("employeeBrowse.employeeForThisUserAlreadySpecified"), NotificationType.WARNING);
                userField.requestFocus();
                return false;
            }
            return true;
        }
        return false;
    }

    protected boolean isUnique(Employee item) {
        if (!StringUtils.isEmpty(item.getEmail())) {
            List same = dataManager.loadList(LoadContext.create(Employee.class)
                    .setQuery(new LoadContext.Query("select e from plstp$Employee e where e.email = :email and e.id <> :id")
                            .setParameter("email", item.getEmail())
                            .setParameter("id", item.getId())
                            .setMaxResults(1))
                    .setView(View.MINIMAL));
            return CollectionUtils.isEmpty(same);
        }
        return true;
    }

    protected boolean isUserUsed(Employee employee) {
        if (employee.getUser() != null) {
            List same = dataManager.loadList(LoadContext.create(Employee.class)
                    .setQuery(new LoadContext.Query("select e from plstp$Employee e where e.user.id = :userId and e.id <> :id")
                            .setParameter("userId", employee.getUser().getId())
                            .setParameter("id", employee.getId())
                            .setMaxResults(1))
                    .setView(View.MINIMAL));
            return !CollectionUtils.isEmpty(same);
        }
        return false;
    }

    protected String getNullSafe(String value) {
        return StringUtils.isEmpty(value) ? StringUtils.EMPTY : value;
    }
}