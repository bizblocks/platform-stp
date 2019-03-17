package com.groupstp.platform.entity;

import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

/**
 * @author adiatullin
 */
@NamePattern("%s|name,taxNumber")
@Table(name = "PLSTP_SUPPLIER")
@Entity(name = "plstp$Supplier")
public class Supplier extends StandardEntity {
    private static final long serialVersionUID = -6729721376045777498L;

    @NotNull
    @Column(name = "NAME", nullable = false, length = 100)
    private String name;

    @Column(name = "FULL_NAME")
    private String fullName;

    @NotNull
    @Column(name = "TAX_NUMBER", nullable = false, length = 50)
    private String taxNumber;

    @Column(name = "TAX_CODE", length = 20)
    private String taxCode;

    @Lob
    @Column(name = "COMMENT_")
    private String comment;


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getTaxNumber() {
        return taxNumber;
    }

    public void setTaxNumber(String taxNumber) {
        this.taxNumber = taxNumber;
    }

    public String getTaxCode() {
        return taxCode;
    }

    public void setTaxCode(String taxCode) {
        this.taxCode = taxCode;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}