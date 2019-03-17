package com.groupstp.platform.web.supplier;

import com.groupstp.platform.entity.Supplier;
import com.groupstp.platform.service.CsvImportExportService;
import com.groupstp.platform.web.config.ProjectWebConfig;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.components.EntityCombinedScreen;
import com.haulmont.cuba.gui.components.FileUploadField;
import com.haulmont.cuba.gui.components.actions.BaseAction;
import com.haulmont.cuba.gui.export.ByteArrayDataProvider;
import com.haulmont.cuba.gui.export.ExportDisplay;
import com.haulmont.cuba.gui.export.ExportFormat;
import com.haulmont.cuba.gui.upload.FileUploadingAPI;
import com.haulmont.cuba.security.entity.EntityOp;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.nio.file.Files;
import java.util.*;

/**
 * @author adiatullin
 */
public class SupplierBrowse extends EntityCombinedScreen {

    private static final Logger log = LoggerFactory.getLogger(SupplierBrowse.class);

    @Inject
    private DataManager dataManager;
    @Inject
    private FileUploadingAPI uploadingAPI;
    @Inject
    private CsvImportExportService csvService;
    @Inject
    private Security security;
    @Inject
    private ExportDisplay exportDisplay;

    @Inject
    private ProjectWebConfig config;

    @Inject
    private FileUploadField csvImportBtn;

    @Override
    public void init(Map<String, Object> params) {
        super.init(params);

        initImport();
        initExport();
    }

    protected void initImport() {
        csvImportBtn.addFileUploadErrorListener(e -> {
            showNotification(getMessage("supplierBrowse.importFailed"), NotificationType.ERROR);
            log.error("Failed to upload suppliers from csv", e.getCause());
        });
        csvImportBtn.addFileUploadSucceedListener(e -> {
            final UUID fileId = csvImportBtn.getFileId();
            try {
                File file = uploadingAPI.getFile(fileId);
                if (file != null) {
                    byte[] data = Files.readAllBytes(file.toPath());

                    int count = csvService.importEntities(new CsvImportExportService.CsvDataContext(data, Supplier.class)
                            .setPattern(config.getCsvSupplierPattern())
                    );

                    showNotification(String.format(getMessage("supplierBrowse.importedSuccess"), count), NotificationType.HUMANIZED);
                    getTable().getDatasource().refresh();
                } else {
                    showNotification(getMessage("supplierBrowse.fileNotFound"), NotificationType.WARNING);
                    log.error("Upload file not found");
                }
            } catch (Exception ee) {
                showNotification(getMessage("supplierBrowse.importFailed"), ee.getMessage(), NotificationType.ERROR);
                log.error("Suppliers import failed", ee);
            } finally {
                try {
                    uploadingAPI.deleteFile(fileId);
                } catch (FileStorageException ee) {
                    log.error(String.format("Unable to delete temp file '%s'", fileId), ee);
                }
            }
        });
        csvImportBtn.setEnabled(security.isEntityOpPermitted(Supplier.class, EntityOp.CREATE));
    }

    protected void initExport() {
        getTable().addAction(new BaseAction("csvExport") {
            @Override
            public String getCaption() {
                return getMessage("supplierBrowse.csvExport");
            }

            @Override
            public void actionPerform(Component component) {
                //noinspection unchecked
                Collection<Supplier> items = getTable().getSelected();
                if (!CollectionUtils.isEmpty(items)) {
                    try {
                        byte[] data = csvService.exportEntities(new CsvImportExportService.CsvDataContext(new LinkedHashSet<>(items))
                                .setPattern(config.getCsvSupplierPattern())
                        );
                        exportDisplay.show(new ByteArrayDataProvider(data), getMessage("supplierBrowse.csvFileName"), ExportFormat.CSV);
                    } catch (Exception e) {
                        showNotification(getMessage("supplierBrowse.exportFailed"), e.getMessage(), NotificationType.ERROR);
                        log.error("Suppliers export failed", e);
                    }
                }
            }

            @Override
            public boolean isPermitted() {
                return super.isPermitted() &&
                        !CollectionUtils.isEmpty(getTable().getSelected()) &&
                        security.isEntityOpPermitted(Supplier.class, EntityOp.UPDATE);
            }
        });
    }

    /**
     * Before saving check item
     */
    @Override
    public boolean validate(List<Validatable> fields) {
        if (super.validate(fields)) {
            Supplier item = (Supplier) getFieldGroup().getDatasource().getItem();
            if (!isUnique(item)) {
                showNotification(getMessage("supplierBrowse.sameSupplierAlreadyExist"), NotificationType.WARNING);
                return false;
            }
            return true;
        }
        return false;
    }

    /**
     * Check what item are unique in system
     */
    protected boolean isUnique(Supplier item) {
        List same = dataManager.loadList(LoadContext.create(Supplier.class)
                .setQuery(new LoadContext.Query("select e from plstp$Supplier e where e.name = :name and e.id <> :id")
                        .setParameter("name", item.getName())
                        .setParameter("id", item.getId())
                        .setMaxResults(1))
                .setView(View.MINIMAL));
        return CollectionUtils.isEmpty(same);
    }
}