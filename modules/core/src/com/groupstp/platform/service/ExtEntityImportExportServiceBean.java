package com.groupstp.platform.service;

import com.groupstp.platform.core.bean.ExtEntityImportExportAPI;
import com.haulmont.cuba.core.app.importexport.EntityImportExportServiceBean;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.View;

import javax.inject.Inject;
import java.util.Collection;

/**
 * Extended base entity import export service
 *
 * @author adiatullin
 */
public class ExtEntityImportExportServiceBean extends EntityImportExportServiceBean implements ExtEntityImportExportService {

    @Inject
    protected ExtEntityImportExportAPI api;

    @Override
    public byte[] exportEntitiesSeparatelyToZIP(Collection<? extends Entity> entities, View view) {
        return api.exportEntitiesSeparatelyToZIP(entities, view);
    }
}
