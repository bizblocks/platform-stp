package com.groupstp.platform.entity;

import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author adiatullin
 */
@NamePattern("%s|name")
@Table(name = "PLSTP_PROJECT")
@Entity(name = "plstp$Project")
public class Project extends StandardEntity {
    private static final long serialVersionUID = -2650056859718552691L;

    @NotNull
    @Column(name = "NAME", nullable = false)
    private String name;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "PROJECTS_COMPANIES_LINK",
            joinColumns = @JoinColumn(name = "PROJECT_ID"),
            inverseJoinColumns = @JoinColumn(name = "COMPANY_ID"))
    private List<Company> companies;

    @Lob
    @Column(name = "COMMENT_")
    private String comment;


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Company> getCompanies() {
        return companies;
    }

    public void setCompanies(List<Company> companies) {
        this.companies = companies;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}