package com.groupstp.platform.web.company;

import com.groupstp.platform.entity.Company;
import com.groupstp.platform.service.CsvImportExportService;
import com.groupstp.platform.service.ExtEntityImportExportService;
import com.groupstp.platform.web.config.ProjectWebConfig;
import com.haulmont.cuba.core.app.importexport.EntityImportView;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/**
 * @author adiatullin
 */
public class CompanyBrowse extends EntityCombinedScreen {
    private static final Logger log = LoggerFactory.getLogger(CompanyBrowse.class);

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

    @Override
    public void init(Map<String, Object> params) {
        super.init(params);

        initImport();
        initExport();

        exportBtn.setEnabled(security.isEntityOpPermitted(Company.class, EntityOp.UPDATE));
    }

    //company import behaviour
    protected void initImport() {
        importBtn.addFileUploadErrorListener(e -> {
            showNotification(getMessage("companyBrowse.importFailed"), NotificationType.ERROR);
            log.error("Failed to upload companies", e.getCause());
        });
        importBtn.addFileUploadSucceedListener(e -> {
            final UUID fileId = importBtn.getFileId();
            try {
                File file = uploadingAPI.getFile(fileId);
                if (file != null) {
                    byte[] data = Files.readAllBytes(file.toPath());

                    long count;
                    String ext = FilenameUtils.getExtension(importBtn.getFileName());
                    if ("json".equalsIgnoreCase(ext)) {
                        Collection<Entity> importedEntities = entityImportExportService.importEntitiesFromJSON(
                                new String(data, StandardCharsets.UTF_8), getImportingView());
                        count = importedEntities.stream()
                                .filter(entity -> entity instanceof Company)
                                .count();
                    } else if ("zip".equalsIgnoreCase(ext)) {
                        Collection<Entity> importedEntities = entityImportExportService.importEntitiesFromZIP(
                                data, getImportingView());
                        count = importedEntities.stream()
                                .filter(entity -> entity instanceof Company)
                                .count();
                    } else {
                        count = csvService.importEntities(new CsvImportExportService.CsvDataContext(data, Company.class)
                                .setPattern(config.getCsvCompanyPattern()));
                    }

                    showNotification(String.format(getMessage("companyBrowse.importedSuccess"), count), NotificationType.HUMANIZED);
                    getTable().getDatasource().refresh();
                } else {
                    showNotification(getMessage("companyBrowse.fileNotFound"), NotificationType.WARNING);
                    log.error("Upload file not found");
                }
            } catch (Exception ee) {
                showNotification(getMessage("companyBrowse.importError"), ee.getMessage(), NotificationType.ERROR);
                log.error("Companies import failed", ee);
            } finally {
                try {
                    uploadingAPI.deleteFile(fileId);
                } catch (FileStorageException ee) {
                    log.error(String.format("Unable to delete temp file '%s'", fileId), ee);
                }
            }
        });
        importBtn.setEnabled(security.isEntityOpPermitted(Company.class, EntityOp.CREATE));
    }

    //company export behaviour
    protected void initExport() {
        exportBtn.addAction(new ItemTrackingAction("zipExport") {
            @Override
            public String getCaption() {
                return getMessage("companyBrowse.zipExport");
            }

            @Override
            public void actionPerform(Component component) {
                //noinspection unchecked
                Collection<Company> items = getTable().getSelected();
                if (!CollectionUtils.isEmpty(items)) {
                    try {
                        byte[] data = entityImportExportService.exportEntitiesSeparatelyToZIP(items, getExportingView());
                        exportDisplay.show(new ByteArrayDataProvider(data), getMessage("companyBrowse.csvFileName"), ExportFormat.ZIP);
                    } catch (Exception e) {
                        showNotification(getMessage("companyBrowse.exportFailed"), e.getMessage(), NotificationType.ERROR);
                        log.error("Companies export failed", e);
                    }
                }
            }

            @Override
            public boolean isPermitted() {
                return super.isPermitted() &&
                        !CollectionUtils.isEmpty(getTable().getSelected()) &&
                        security.isEntityOpPermitted(Company.class, EntityOp.READ);
            }
        });
        exportBtn.addAction(new ItemTrackingAction("csvExport") {
            @Override
            public String getCaption() {
                return getMessage("companyBrowse.csvExport");
            }

            @Override
            public void actionPerform(Component component) {
                //noinspection unchecked
                Collection<Company> items = getTable().getSelected();
                if (!CollectionUtils.isEmpty(items)) {
                    try {
                        byte[] data = csvService.exportEntities(new CsvImportExportService.CsvDataContext(new LinkedHashSet<>(items))
                                .setPattern(config.getCsvCompanyPattern())
                        );
                        exportDisplay.show(new ByteArrayDataProvider(data), getMessage("companyBrowse.csvFileName"), ExportFormat.CSV);
                    } catch (Exception e) {
                        showNotification(getMessage("companyBrowse.exportFailed"), e.getMessage(), NotificationType.ERROR);
                        log.error("Companies export failed", e);
                    }
                }
            }

            @Override
            public boolean isPermitted() {
                return super.isPermitted() &&
                        !CollectionUtils.isEmpty(getTable().getSelected()) &&
                        security.isEntityOpPermitted(Company.class, EntityOp.READ);
            }
        });
    }

    protected EntityImportView getImportingView() {
        return new EntityImportView(Company.class)
                .addLocalProperties();
    }

    protected View getExportingView() {
        View view = viewRepository.getView(Company.class, "company-export");
        if (view == null) {
            throw new DevelopmentException("View 'company-export' for plstp$Company not found");
        }
        return view;
    }

    /**
     * Before saving check item
     */
    @Override
    public boolean validate(List<Validatable> fields) {
        if (super.validate(fields)) {
            Company item = (Company) getFieldGroup().getDatasource().getItem();
            if (!isNameUnique(item)) {
                showNotification(getMessage("companyBrowse.sameNameCompanyAlreadyExist"), NotificationType.WARNING);
                getFieldGroup().getComponentNN("name").requestFocus();
                return false;
            }
            if (!isCodeUnique(item)) {
                showNotification(getMessage("companyBrowse.sameCodeCompanyAlreadyExist"), NotificationType.WARNING);
                getFieldGroup().getComponentNN("code").requestFocus();
                return false;
            }
            return true;
        }
        return false;
    }

    private boolean isNameUnique(Company item) {
        List same = dataManager.loadList(LoadContext.create(Company.class)
                .setQuery(new LoadContext.Query("select e from plstp$Company e where e.name = :name and e.id <> :id")
                        .setParameter("name", item.getName())
                        .setParameter("id", item.getId())
                        .setMaxResults(1))
                .setView(View.MINIMAL));
        return CollectionUtils.isEmpty(same);
    }

    private boolean isCodeUnique(Company item) {
        List same = dataManager.loadList(LoadContext.create(Company.class)
                .setQuery(new LoadContext.Query("select e from plstp$Company e where e.code = :code and e.id <> :id")
                        .setParameter("code", item.getCode())
                        .setParameter("id", item.getId())
                        .setMaxResults(1))
                .setView(View.MINIMAL));
        return CollectionUtils.isEmpty(same);
    }
}