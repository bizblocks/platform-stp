package com.groupstp.platform.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.entity.annotation.CaseConversion;

/**
 * @author adiatullin
 */
@NamePattern("%s|name,taxNumber,code")
@Table(name = "PLSTP_COMPANY")
@Entity(name = "plstp$Company")
public class Company extends StandardEntity {
    private static final long serialVersionUID = -7839458754352923394L;

    @NotNull
    @CaseConversion
    @Column(name = "CODE", unique = true, length = 4, nullable = false)
    private String code;

    @NotNull
    @Column(name = "NAME", nullable = false, length = 100)
    private String name;

    @Column(name = "FULL_NAME")
    private String fullName;

    @Column(name = "TAX_NUMBER", length = 50)
    private String taxNumber;

    @Column(name = "TAX_CODE", length = 20)
    private String taxCode;

    @Lob
    @Column(name = "COMMENT_")
    private String comment;


    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

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