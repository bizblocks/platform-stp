-- begin PLSTP_COMPANY
create table PLSTP_COMPANY (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    CODE varchar(4) not null,
    NAME varchar(100) not null,
    FULL_NAME varchar(255),
    TAX_NUMBER varchar(50),
    TAX_CODE varchar(20),
    COMMENT_ text,
    --
    primary key (ID)
)^
-- end PLSTP_COMPANY
-- begin PLSTP_EMPLOYEE
create table PLSTP_EMPLOYEE (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    NAME varchar(100) not null,
    EMAIL varchar(50),
    FULL_NAME varchar(255),
    USER_ID uuid,
    MANAGER_ID uuid,
    --
    primary key (ID)
)^
-- end PLSTP_EMPLOYEE
-- begin PLSTP_PROJECT
create table PLSTP_PROJECT (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    NAME varchar(255) not null,
    COMMENT_ text,
    --
    primary key (ID)
)^
-- end PLSTP_PROJECT
-- begin PLSTP_SUPPLIER
create table PLSTP_SUPPLIER (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    NAME varchar(100) not null,
    FULL_NAME varchar(255),
    TAX_NUMBER varchar(50) not null,
    TAX_CODE varchar(20),
    COMMENT_ text,
    --
    primary key (ID)
)^
-- end PLSTP_SUPPLIER
-- begin PROJECTS_COMPANIES_LINK
create table PROJECTS_COMPANIES_LINK (
    PROJECT_ID uuid,
    COMPANY_ID uuid,
    primary key (PROJECT_ID, COMPANY_ID)
)^
-- end PROJECTS_COMPANIES_LINK
