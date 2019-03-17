package com.groupstp.platform.service;

import com.haulmont.bali.util.Preconditions;
import com.haulmont.cuba.core.entity.Entity;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * This service provide importing entities from CSV
 *
 * @author adiatullin
 */
public interface CsvImportExportService {
    String NAME = "plstp_CsvImportExportService";

    /**
     * Import and persist entities from provided csv context
     *
     * @param ctx entities importing csv context
     * @return count of updated entities
     */
    int importEntities(CsvDataContext ctx);

    /**
     * Export entities to csv and return bytes of this csv.
     *
     * @param ctx csv data context. Context should contains entities list to export.
     * @return exported entities csv bytes.
     */
    byte[] exportEntities(CsvDataContext ctx);


    /**
     * Importing/Exporting CSV context
     */
    @SuppressWarnings("all")
    class CsvDataContext implements Serializable {
        private static final long serialVersionUID = -5202330453258309103L;

        protected final byte[] content;
        protected final Set<Entity> exportingEntities;
        protected final Class<? extends Entity> entityClass;

        protected String pattern;

        protected final Map<String, String> propertiesCaptions = new LinkedHashMap<>();
        protected final Map<String, SimpleDateFormat> propertiesDateFormats = new HashMap<>();
        protected final Set<String> uniqueKeyProperties = new LinkedHashSet<>();

        /**
         * @param content     csv bytes
         * @param entityClass importing entity java class
         */
        public CsvDataContext(byte[] content, Class<? extends Entity> entityClass) {
            Preconditions.checkNotNullArgument(content);
            Preconditions.checkNotNullArgument(entityClass);

            this.content = content;
            this.exportingEntities = null;
            this.entityClass = entityClass;
        }

        public CsvDataContext(Set<Entity> exportingEntities) {
            Preconditions.checkNotNullArgument(exportingEntities);

            this.content = null;
            this.exportingEntities = exportingEntities;
            this.entityClass = null;
        }

        /**
         * @param pattern CSV entity property pattern
         * @return this context object
         */
        public CsvDataContext setPattern(String pattern) {
            this.pattern = pattern;
            return this;
        }

        /**
         * @param caption  property caption in csv
         * @param property entity property
         * @return this context object
         */
        public CsvDataContext setPropertyCaption(String caption, String property) {
            propertiesCaptions.put(caption, property);
            return this;
        }

        /**
         * @param dateFormat date formatter
         * @param properties properties where should using this date formatter
         * @return this context object
         */
        public CsvDataContext setPropertyDateFormat(SimpleDateFormat dateFormat, String... properties) {
            if (properties.length > 0) {
                for (String property : properties) {
                    propertiesDateFormats.put(property, dateFormat);
                }
            }
            return this;
        }

        public CsvDataContext setUniqueKeyProperties(String... properties) {
            if (properties.length > 0) {
                for (String property : properties) {
                    uniqueKeyProperties.add(property);
                }
            }
            return this;
        }
    }
}
