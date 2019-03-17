package com.groupstp.platform.web.project;

import com.groupstp.platform.entity.Company;
import com.groupstp.platform.entity.Project;
import com.groupstp.platform.service.CsvImportExportService;
import com.groupstp.platform.service.ExtEntityImportExportService;
import com.groupstp.platform.web.config.ProjectWebConfig;
import com.haulmont.chile.core.model.impl.AbstractInstance;
import com.haulmont.cuba.core.app.importexport.CollectionImportPolicy;
import com.haulmont.cuba.core.app.importexport.EntityImportView;
import com.haulmont.cuba.core.app.importexport.ReferenceImportBehaviour;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.actions.ItemTrackingAction;
import com.haulmont.cuba.gui.export.ByteArrayDataProvider;
import com.haulmont.cuba.gui.export.ExportDisplay;
import com.haulmont.cuba.gui.export.ExportFormat;
import com.haulmont.cuba.gui.upload.FileUploadingAPI;
import com.haulmont.cuba.security.entity.EntityOp;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author adiatullin
 */
public class ProjectBrowse extends EntityCombinedScreen {
    private static final Logger log = LoggerFactory.getLogger(ProjectBrowse.class);

    public static final String COMPANIES_ID = "companiesIds";

    @Inject
    protected DataManager dataManager;
    @Inject
    protected FileUploadingAPI uploadingAPI;
    @Inject
    protected ExtEntityImportExportService entityImportExportService;
    @Inject
    protected ViewRepository viewRepository;
    @Inject
    protected ExportDisplay exportDisplay;
    @Inject
    protected Security security;
    @Inject
    protected CsvImportExportService csvService;

    @Inject
    protected ProjectWebConfig config;

    @Inject
    protected FileUploadField importBtn;
    @Inject
    protected PopupButton exportBtn;
    @Inject
    protected Table<Project> table;
    @Inject
    protected Table<Company> companiesTable;

    @Override
    public void init(Map<String, Object> params) {
        super.init(params);

        initImport();
        initExport();
        initTableCompanies();

        exportBtn.setEnabled(security.isEntityOpPermitted(Project.class, EntityOp.UPDATE));
    }

    protected void initImport() {
        importBtn.addFileUploadErrorListener(e -> {
            showNotification(getMessage("projectBrowse.importFailed"), NotificationType.ERROR);
            log.error("Failed to upload projects", e.getCause());
        });
        importBtn.addFileUploadSucceedListener(e -> {
            final UUID fileId = importBtn.getFileId();
            try {
                File file = uploadingAPI.getFile(fileId);
                if (file != null) {
                    byte[] data = java.nio.file.Files.readAllBytes(file.toPath());

                    long count;
                    String ext = FilenameUtils.getExtension(importBtn.getFileName());
                    if ("json".equalsIgnoreCase(ext)) {
                        Collection<Entity> importedEntities = entityImportExportService.importEntitiesFromJSON(
                                new String(data, StandardCharsets.UTF_8), getImportingView());
                        count = importedEntities.stream()
                                .filter(entity -> entity instanceof Project)
                                .count();
                    } else if ("zip".equalsIgnoreCase(ext)) {
                        Collection<Entity> importedEntities = entityImportExportService.importEntitiesFromZIP(
                                data, getImportingView());
                        count = importedEntities.stream()
                                .filter(entity -> entity instanceof Project)
                                .count();
                    } else {
                        count = csvService.importEntities(new CsvImportExportService.CsvDataContext(data, Project.class)
                                .setPattern(config.getCsvProjectPattern()));
                    }

                    showNotification(String.format(getMessage("projectBrowse.importedSuccess"), count), NotificationType.HUMANIZED);
                    getTable().getDatasource().refresh();
                } else {
                    showNotification(getMessage("projectBrowse.fileNotFound"), NotificationType.WARNING);
                    log.error("Upload file not found");
                }
            } catch (Exception ee) {
                showNotification(getMessage("projectBrowse.importError"), ee.getMessage(), NotificationType.ERROR);
                log.error("Projects import failed", ee);
            } finally {
                try {
                    uploadingAPI.deleteFile(fileId);
                } catch (FileStorageException ee) {
                    log.error(String.format("Unable to delete temp file '%s'", fileId), ee);
                }
            }
        });
        importBtn.setEnabled(security.isEntityOpPermitted(Project.class, EntityOp.CREATE));
    }

    protected void initExport() {
        exportBtn.addAction(new ItemTrackingAction("zipExport") {
            @Override
            public String getCaption() {
                return getMessage("projectBrowse.zipExport");
            }

            @Override
            public void actionPerform(Component component) {
                //noinspection unchecked
                Collection<Project> items = getTable().getSelected();
                if (!CollectionUtils.isEmpty(items)) {
                    try {
                        byte[] data = entityImportExportService.exportEntitiesSeparatelyToZIP(items, getExportingView());
                        exportDisplay.show(new ByteArrayDataProvider(data), getMessage("projectBrowse.csvFileName"), ExportFormat.ZIP);
                    } catch (Exception e) {
                        showNotification(getMessage("projectBrowse.exportFailed"), e.getMessage(), NotificationType.ERROR);
                        log.error("Projects export failed", e);
                    }
                }
            }

            @Override
            public boolean isPermitted() {
                return super.isPermitted() &&
                        !CollectionUtils.isEmpty(getTable().getSelected()) &&
                        security.isEntityOpPermitted(Project.class, EntityOp.READ);
            }
        });
        exportBtn.addAction(new ItemTrackingAction("csvExport") {
            @Override
            public String getCaption() {
                return getMessage("projectBrowse.csvExport");
            }

            @Override
            public void actionPerform(Component component) {
                //noinspection unchecked
                Collection<Project> items = getTable().getSelected();
                if (!CollectionUtils.isEmpty(items)) {
                    try {
                        byte[] data = csvService.exportEntities(new CsvImportExportService.CsvDataContext(new LinkedHashSet<>(items))
                                .setPattern(config.getCsvProjectPattern())
                        );
                        exportDisplay.show(new ByteArrayDataProvider(data), getMessage("projectBrowse.csvFileName"), ExportFormat.CSV);
                    } catch (Exception e) {
                        showNotification(getMessage("projectBrowse.exportFailed"), e.getMessage(), NotificationType.ERROR);
                        log.error("Projects export failed", e);
                    }
                }
            }

            @Override
            public boolean isPermitted() {
                return super.isPermitted() &&
                        !CollectionUtils.isEmpty(getTable().getSelected()) &&
                        security.isEntityOpPermitted(Project.class, EntityOp.READ);
            }
        });
    }

    protected void initTableCompanies() {
        table.addGeneratedColumn("companies", entity -> {
            String value = StringUtils.EMPTY;
            if (!CollectionUtils.isEmpty(entity.getCompanies())) {
                value = entity.getCompanies().stream()
                        .map(AbstractInstance::getInstanceName)
                        .sorted()
                        .collect(Collectors.joining(", "));
            }
            return new Table.PlainTextCell(value);
        });
    }

    protected EntityImportView getImportingView() {
        return new EntityImportView(Project.class)
                .addLocalProperties()
                .addManyToManyProperty("companies", ReferenceImportBehaviour.ERROR_ON_MISSING, CollectionImportPolicy.KEEP_ABSENT_ITEMS);
    }

    protected View getExportingView() {
        View view = viewRepository.getView(Project.class, "project-export");
        if (view == null) {
            throw new DevelopmentException("View 'project-export' for plstp$Project not found");
        }
        return view;
    }

    @Override
    protected void initEditComponents(boolean enabled) {
        super.initEditComponents(enabled);

        companiesTable.setEnabled(enabled);
    }

    /**
     * Before saving check item
     */
    @Override
    public boolean validate(List<Validatable> fields) {
        if (super.validate(fields)) {
            Project item = (Project) getFieldGroup().getDatasource().getItem();
            if (!isUnique(item)) {
                showNotification(getMessage("projectBrowse.sameProjectAlreadyExist"), NotificationType.WARNING);
                return false;
            }
            if (!isCompaniesSpecified(item)) {
                showNotification(getMessage("projectBrowse.enterCompanies"), NotificationType.WARNING);
                companiesTable.requestFocus();
                return false;
            }
            return true;
        }
        return false;
    }

    /**
     * Check what item are unique in system
     */
    protected boolean isUnique(Project item) {
        List same = dataManager.loadList(LoadContext.create(Project.class)
                .setQuery(new LoadContext.Query("select e from plstp$Project e where " +
                        "e.name = :name and e.id <> :id")
                        .setParameter("name", item.getName())
                        .setParameter("id", item.getId())
                        .setMaxResults(1))
                .setView(View.MINIMAL));
        return CollectionUtils.isEmpty(same);
    }

    protected boolean isCompaniesSpecified(Project item) {
        return !CollectionUtils.isEmpty(item.getCompanies());
    }
}