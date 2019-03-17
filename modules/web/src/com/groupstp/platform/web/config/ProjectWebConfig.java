package com.groupstp.platform.web.config;

import com.haulmont.cuba.core.config.Config;
import com.haulmont.cuba.core.config.Property;
import com.haulmont.cuba.core.config.defaults.Default;

/**
 * Basic project web module settings and properties
 *
 * @author adiatullin
 */
public interface ProjectWebConfig extends Config {
    /**
     * @return Suppliers CSV export/import pattern
     */
    @Property("csv.supplier.pattern")
    @Default("Название→name[unique],Полное название→fullName,ИНН→taxNumber,КПП→taxCode,Комментарий→comment")
    String getCsvSupplierPattern();
    void setCsvSupplierPattern(String value);

    /**
     * @return Companies CSV export/import pattern
     */
    @Property("csv.company.pattern")
    @Default("Код→code[unique],Название→name[unique],Полное название→fullName,ИНН→taxNumber,КПП→taxCode,Комментарий→comment")
    String getCsvCompanyPattern();
    void setCsvCompanyPattern(String value);

    /**
     * @return Projects CSV export/import pattern
     */
    @Property("csv.project.pattern")
    @Default("Название→name[unique],Комментарий→comment")
    String getCsvProjectPattern();
    void setCsvProjectPattern(String value);
}
