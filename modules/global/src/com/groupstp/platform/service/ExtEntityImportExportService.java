package com.groupstp.platform.service;

import com.haulmont.cuba.core.app.importexport.EntityImportExportService;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.View;

import java.util.Collection;

/**
 * Extended entities import export service
 *
 * @author adiatullin
 */
public interface ExtEntityImportExportService extends EntityImportExportService {
    String NAME = EntityImportExportService.NAME;

    /**
     * Export entities separately to zip archive
     *
     * @param entities entities to export
     * @param view     import entities
     * @return zip archive bytes
     */
    byte[] exportEntitiesSeparatelyToZIP(Collection<? extends Entity> entities, View view);
}