package com.groupstp.platform.service;

import com.groupstp.platform.core.bean.MessageableBean;
import com.groupstp.platform.util.ParsingUtil;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.chile.core.model.MetaProperty;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.*;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * CSV import exporter bean
 *
 * @author adiatullin
 */
@Service(CsvImportExportService.NAME)
public class CsvImportExportServiceBean extends MessageableBean implements CsvImportExportService {
    private static final Logger log = LoggerFactory.getLogger(CsvImportExportServiceBean.class);

    @Inject
    protected DataManager dataManager;
    @Inject
    protected ParsingUtil parsingUtil;
    @Inject
    protected Metadata metadata;

    @Override
    public int importEntities(CsvDataContext ctx) {
        if (ctx == null) {
            throw new IllegalArgumentException(getMessage("CsvImportServiceBean.emptyContext"));
        }
        if (ctx.content == null) {
            throw new IllegalArgumentException(getMessage("CsvImportServiceBean.contentNotSet"));
        }
        if (ctx.entityClass == null) {
            throw new IllegalArgumentException(getMessage("CsvImportServiceBean.entityClassNotSet"));
        }

        int count = 0;
        setupContextFromPatternIfNeed(ctx);
        try {
            CSVReadingResult result = readCSV(new String(ctx.content, StandardCharsets.UTF_8), ctx);
            if (!CollectionUtils.isEmpty(result.getEntities())) {
                CommitContext toCommit = new CommitContext();

                if (!CollectionUtils.isEmpty(ctx.uniqueKeyProperties)) {
                    List<Entity> exists = loadExists(ctx.entityClass);
                    if (!CollectionUtils.isEmpty(exists)) {
                        Map<String, Entity> key2Entity = exists.stream()
                                .collect(Collectors.toMap(e -> getKey(ctx, e), Function.identity()));

                        for (Entity entity : result.getEntities()) {
                            Entity existEntity = key2Entity.get(getKey(ctx, entity));
                            if (existEntity != null) {
                                for (String property : result.getProperties()) {
                                    existEntity.setValue(property, entity.getValue(property));
                                }
                                toCommit.addInstanceToCommit(existEntity);
                            } else {
                                toCommit.addInstanceToCommit(entity);
                            }
                        }
                    } else {
                        Map<String, Entity> key2Entity = result.getEntities().stream()
                                .collect(Collectors.toMap(e -> getKey(ctx, e), Function.identity()));
                        toCommit.getCommitInstances().addAll(key2Entity.values());
                    }
                } else {
                    toCommit.getCommitInstances().addAll(result.getEntities());
                }

                if (toCommit.getCommitInstances().size() > 0) {
                    dataManager.commit(toCommit);
                    count = toCommit.getCommitInstances().size();
                }
            }
        } catch (Exception e) {
            log.error("Failed to import entities", e);

            throw new RuntimeException(String.format(getMessage("CsvImportServiceBean.failedToParseEntities"), e.getMessage()));
        }
        return count;
    }

    @Override
    public byte[] exportEntities(CsvDataContext ctx) {
        if (ctx == null) {
            throw new IllegalArgumentException(getMessage("CsvImportServiceBean.emptyContext"));
        }
        if (CollectionUtils.isEmpty(ctx.exportingEntities)) {
            throw new IllegalArgumentException(getMessage("CsvImportServiceBean.entitiesNotSet"));
        }

        setupContextFromPatternIfNeed(ctx);
        List<Entity> reloadedEntities = reloadEntities(ctx.exportingEntities);
        try {
            return writeCSV(ctx, reloadedEntities);
        } catch (Exception e) {
            log.error("Failed to export entities", e);

            throw new RuntimeException(String.format(getMessage("CsvImportServiceBean.failedToExportEntities"), e.getMessage()));
        }
    }


    @Nullable
    protected String getKey(CsvDataContext ctx, Entity entity) {
        if (entity == null || CollectionUtils.isEmpty(ctx.uniqueKeyProperties)) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (String property : ctx.uniqueKeyProperties) {
            Object value = entity.getValue(property);
            sb.append(value == null ? StringUtils.EMPTY : value.toString());
        }
        return sb.toString();
    }

    protected void setupContextFromPatternIfNeed(CsvDataContext ctx) {
        if (!StringUtils.isEmpty(ctx.pattern)) {
            try {
                setupContextFromPattern(ctx, ctx.pattern);
            } catch (Exception e) {
                log.error(String.format("Failed to parse csv pattern '%s'", ctx.pattern), e);
                throw new IllegalArgumentException(getMessage("CsvImportServiceBean.failedToParsePattern"), e);
            }
        }
    }

    /**
     * Prepare and fill csv context from pattern text.
     * Pattern contains text like caption=property[dateformat][unique]
     * <p>
     * For example entity has next properties: x- text value, y - date value. x is unique property.
     * <p>
     * X Caption=x[unique],Y Date Caption=y[dd/MM/YY]
     *
     * @param ctx     csv content to fill
     * @param pattern properties pattern
     */
    protected void setupContextFromPattern(CsvDataContext ctx, String pattern) {
        String[] properties = pattern.split(",");
        for (String property : properties) {
            String[] parts = property.split("→");
            if (parts.length != 2) {
                throw new IllegalArgumentException(getMessage("CsvImportServiceBean.csvPatternIncorrectParts"));
            }
            String propertyCaption = parts[0].trim();
            String propertyName = parts[1];

            boolean unique = false;
            SimpleDateFormat df = null;

            int metaPropertyStart;
            int metaPropertyEnd;
            while ((metaPropertyStart = propertyName.indexOf("[")) != -1 && (metaPropertyEnd = propertyName.indexOf("]")) != -1) {
                String metaProperty = propertyName.substring(metaPropertyStart, metaPropertyEnd + 1);
                propertyName = propertyName.substring(0, metaPropertyStart) + propertyName.substring(metaPropertyEnd + 1, propertyName.length());

                metaProperty = metaProperty.replaceAll("\\[", StringUtils.EMPTY).replaceAll("\\]", StringUtils.EMPTY);
                if ("unique".equals(metaProperty)) {
                    unique = true;
                } else {
                    df = new SimpleDateFormat(metaProperty);
                }
            }
            if (unique) {
                ctx.setUniqueKeyProperties(propertyName);
            }
            if (df != null) {
                ctx.setPropertyDateFormat(df, propertyName);
            }

            ctx.setPropertyCaption(propertyCaption, propertyName);
        }
    }

    protected List<Entity> loadExists(Class<? extends Entity> javaClass) {
        MetaClass metaClass = metadata.getClassNN(javaClass);
        //noinspection unchecked
        List<Entity> list = dataManager.loadList(LoadContext.create(metaClass.getJavaClass())
                .setQuery(new LoadContext.Query("select e from " + metaClass.getName() + " e"))
                .setView(View.LOCAL));
        if (!CollectionUtils.isEmpty(list)) {
            return list;
        }
        return Collections.emptyList();
    }

    private List<Entity> reloadEntities(Set<Entity> entities) {
        if (!CollectionUtils.isEmpty(entities)) {
            MetaClass metaClass = metadata.getClassNN(IterableUtils.get(entities, 0).getClass());
            //noinspection unchecked
            List<Entity> list = dataManager.loadList(LoadContext.create(metaClass.getJavaClass())
                    .setQuery(new LoadContext.Query("select e from " + metaClass.getName() + " e where e.id in :ids")
                            .setParameter("ids", entities.stream().map(Entity::getId).collect(Collectors.toList())))
                    .setView(View.LOCAL));
            if (!CollectionUtils.isEmpty(list)) {
                return list;
            }
        }
        return Collections.emptyList();
    }

    /**
     * Parse entities from csv text
     *
     * @param csv csv text.
     * @param ctx csv data context object
     * @return parsed result from csv text.
     * @throws IOException IOException in case of an I/O error.
     */
    protected CSVReadingResult readCSV(String csv, CsvDataContext ctx) throws IOException {
        List<Entity> entities = null;
        Set<String> properties = null;

        if (!StringUtils.isEmpty(csv)) {
            CSVReader reader = new CSVReader(new StringReader(csv));

            String[] header = null;
            String[] nextLine;
            while ((nextLine = reader.readNext()) != null) {
                if (header == null) {
                    header = nextLine;
                    continue;
                }
                if (entities == null) {
                    entities = new ArrayList<>();
                }

                Entity item = metadata.create(ctx.entityClass);
                for (int i = 0; i < header.length; i++) {
                    String propertyName = ctx.propertiesCaptions.get(header[i]);
                    if (StringUtils.isEmpty(propertyName)) {
                        propertyName = header[i];
                    }
                    String propertyValue = nextLine[i];

                    MetaProperty property = item.getMetaClass().getPropertyNN(propertyName);
                    if (properties == null) {
                        properties = new LinkedHashSet<>();
                    }
                    properties.add(propertyName);

                    SimpleDateFormat dateFormat = ctx.propertiesDateFormats.get(propertyName);
                    if (dateFormat == null) {
                        dateFormat = ParsingUtil.DEFAULT_DATE_FORMAT;
                    }

                    item.setValue(propertyName, parsingUtil.toSimpleValue(propertyValue, property.getJavaType(), dateFormat));
                }
                entities.add(item);
            }
        }
        return new CSVReadingResult(entities, properties);
    }

    /**
     * Write provided entities to CSV bytes.
     *
     * @param ctx      csv data context.
     * @param entities entities to export.
     * @return CSV of serialized entities.
     * @throws IOException IOException in case of an I/O error.
     */
    protected byte[] writeCSV(CsvDataContext ctx, List<Entity> entities) throws IOException {
        StringWriter stringWriter = new StringWriter();
        CSVWriter csvWriter = new CSVWriter(stringWriter);

        List<String> captions = new ArrayList<>(ctx.propertiesCaptions.keySet());
        csvWriter.writeNext(captions.toArray(new String[captions.size()]), false);

        if (!CollectionUtils.isEmpty(entities)) {
            for (Entity entity : entities) {
                String[] line = new String[captions.size()];
                for (int i = 0; i < captions.size(); i++) {
                    String property = ctx.propertiesCaptions.get(captions.get(i));
                    Object value = entity.getValue(property);

                    SimpleDateFormat dateFormat = ctx.propertiesDateFormats.get(property);
                    if (dateFormat == null) {
                        dateFormat = ParsingUtil.DEFAULT_DATE_FORMAT;
                    }

                    line[i] = parsingUtil.toText(value, dateFormat);
                }
                csvWriter.writeNext(line, false);
            }
        }
        return stringWriter.toString().getBytes(StandardCharsets.UTF_8);
    }

    protected static class CSVReadingResult {
        private final List<Entity> entities;
        private final Set<String> properties;

        CSVReadingResult(List<Entity> entities, Set<String> properties) {
            this.entities = entities == null ? Collections.emptyList() : entities;
            this.properties = properties == null ? Collections.emptySet() : properties;
        }

        List<Entity> getEntities() {
            return entities;
        }

        Set<String> getProperties() {
            return properties;
        }
    }
}