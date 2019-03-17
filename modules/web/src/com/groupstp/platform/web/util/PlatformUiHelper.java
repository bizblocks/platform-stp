package com.groupstp.platform.web.util;

import com.groupstp.platform.web.util.data.PlatformColumnGenerator;
import com.haulmont.bali.util.Dom4j;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.chile.core.model.MetaProperty;
import com.haulmont.chile.core.model.MetaPropertyPath;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.gui.WindowManager;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.actions.BaseAction;
import com.haulmont.cuba.gui.components.formatters.DateFormatter;
import com.haulmont.cuba.gui.data.CollectionDatasource;
import com.haulmont.cuba.gui.xml.layout.ComponentsFactory;
import com.haulmont.cuba.security.entity.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.lang.StringUtils;
import org.dom4j.Element;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;

/**
 * Web module helpful class to create a flexible screens
 *
 * @author adiatullin
 */
@SuppressWarnings("all")
@org.springframework.stereotype.Component(PlatformUiHelper.NAME)
public class PlatformUiHelper {

    public static final String NAME = "PlatformUiHelper";

    protected static Element CREATE_TS_ELEMENT = Dom4j.readDocument("<createTs format=\"dd.MM.yyyy\" useUserTimezone=\"true\"/>").getRootElement();
    protected static String DATE_FORMAT = "dd.MM.yyyy";
    protected static SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat(DATE_FORMAT);
    protected static DecimalFormat DECIMAL_FORMAT;

    static {
        DecimalFormatSymbols formatSymbols = new DecimalFormatSymbols();
        formatSymbols.setDecimalSeparator('.');
        formatSymbols.setGroupingSeparator(' ');
        DECIMAL_FORMAT = new DecimalFormat("#,###.##", formatSymbols);
    }

    @Inject
    protected ComponentsFactory componentsFactory;
    @Inject
    protected Security security;
    @Inject
    protected MessageTools messageTools;
    @Inject
    protected Messages messages;
    @Inject
    protected DataManager dataManager;
    @Inject
    protected Metadata metadata;
    @Inject
    protected UserSessionSource userSessionSource;

    /**
     * Show related entity in table as link
     *
     * @param table          master entity table
     * @param entityProperty slave entity property name
     */
    public static void showLinkOnTable(Table table, String entityProperty) {
        ((PlatformUiHelper) AppBeans.get(NAME)).showLinkOnTableInner(table, entityProperty);
    }

    protected void showLinkOnTableInner(Table table, String entityProperty) {
        showLinkOnTableInternal(table, entityProperty, null);
    }

    /**
     * Show related entity in table as link
     *
     * @param table           master entity table
     * @param entityProperty  slave entity property name
     * @param captionFunction function to generate related entity link caption
     */
    public static void showLinkOnTable(Table table, String entityProperty, Function<Entity, String> captionFunction) {
        ((PlatformUiHelper) AppBeans.get(NAME)).showLinkOnTableInternal(table, entityProperty, captionFunction);
    }

    protected void showLinkOnTableInternal(Table table, String entityProperty, Function<Entity, String> captionFunction) {
        table.addGeneratedColumn(entityProperty, new Table.ColumnGenerator<Entity>() {
            @Override
            public Component generateCell(Entity entity) {
                LinkButton link = componentsFactory.createComponent(LinkButton.class);
                final Entity nested = entity.getValue(entityProperty);
                if (nested != null) {
                    link.setAction(new BaseAction(entityProperty + "Link") {
                        @Override
                        public void actionPerform(Component component) {
                            Window.Editor editor = table.getFrame().openEditor(nested, WindowManager.OpenType.THIS_TAB);
                            editor.addCloseListener(actionId -> table.getDatasource().refresh());
                        }

                        @Override
                        public boolean isPermitted() {
                            return super.isPermitted() && security.isPermitted(nested, ConstraintOperationType.READ);
                        }
                    });
                    link.setCaption(captionFunction == null ? nested.getInstanceName() : captionFunction.apply(nested));
                } else {
                    link.setCaption(StringUtils.EMPTY);
                }
                return link;
            }
        });
    }

    /**
     * Hide up all lookup actions from screen if user do not have permission to reach them
     *
     * @param screen user opened window
     */
    public static void hideLookupActionInFields(Frame screen) {
        ((PlatformUiHelper) AppBeans.get(NAME)).hideLookupActionInFieldsInternal(screen);
    }

    protected void hideLookupActionInFieldsInternal(Frame screen) {
        Collection<Component> components = screen.getComponents();
        if (!CollectionUtils.isEmpty(components)) {
            for (Component component : components) {
                if (component instanceof LookupPickerField) {
                    LookupPickerField field = (LookupPickerField) component;
                    if (field.getAction(LookupPickerField.LookupAction.NAME) != null) {
                        MetaClass metaClass = field.getMetaClass();
                        if (!security.isEntityOpPermitted(metaClass, EntityOp.READ)) {
                            field.removeAction(LookupPickerField.LookupAction.NAME);
                        }
                    }
                }
            }
        }
    }

    /**
     * Setup table columns from specified set
     *
     * @param table      UI table
     * @param columns    which properties are should shown
     * @param generators custom column generators map
     */
    public static void showColumns(Table table, List<String> columns, Map<String, PlatformColumnGenerator> generators) {
        ((PlatformUiHelper) AppBeans.get(NAME)).showColumnsInternal(table, columns, generators);
    }

    protected void showColumnsInternal(Table table, List<String> columns, Map<String, PlatformColumnGenerator> generators) {
        clearColumns(table);

        if (generators == null) {
            generators = Collections.emptyMap();
        }

        if (!CollectionUtils.isEmpty(columns)) {
            MetaClass metaClass = table.getDatasource().getMetaClass();

            for (String property : columns) {
                PlatformColumnGenerator custom = generators.get(property);
                if (custom != null && custom.getReadGenerator() != null) {
                    table.addGeneratedColumn(property, custom.getReadGenerator());
                } else {
                    MetaProperty metaProperty = metaClass.getPropertyNN(property);
                    MetaPropertyPath path = metaClass.getPropertyPath(property);
                    assert path != null;

                    Table.Column column = new Table.Column(path, property);
                    column.setType(path.getRangeJavaClass());
                    column.setCaption(messageTools.getPropertyCaption(metaProperty));
                    table.addColumn(column);
                }
            }
        }
    }

    /**
     * Prepare table to show specified columns and mark some of them as editable with saving grouping and sorting feature
     *
     * @param table           UI table to prepare
     * @param columns         all showing table columns
     * @param editableColumns editable table columns
     * @param generators      custom column generators for showing the columns
     */
    public static void showColumns(Table table, List<String> columns, List<String> editableColumns, Map<String, PlatformColumnGenerator> generators) {
        ((PlatformUiHelper) AppBeans.get(NAME)).showColumnsInternal(table, columns, editableColumns, generators);
    }

    protected void showColumnsInternal(Table table, List<String> columns, List<String> editableColumns, Map<String, PlatformColumnGenerator> generators) {
        clearColumns(table);

        if (columns == null) {
            columns = Collections.emptyList();
        }
        if (editableColumns == null) {
            editableColumns = Collections.emptyList();
        }

        final List editing = new ArrayList<>();
        final CollectionDatasource ds = table.getDatasource();
        final MetaClass metaClass = ds.getMetaClass();

        for (String property : columns) {
            final MetaPropertyPath path = metaClass.getPropertyPath(property);
            final MetaProperty metaProperty = metaClass.getProperty(property);

            Table.Column column;

            boolean currentEditable = editableColumns.contains(property) && security.isEntityAttrPermitted(metaClass, property, EntityAttrAccess.MODIFY);
            if (currentEditable) {
                table.addGeneratedColumn(property, entity -> {
                    PlatformColumnGenerator generator = generators.get(property);
                    if (editing.contains(entity.getId())) {
                        if (generator != null && generator.getEditGenerator() != null) {
                            return generator.getEditGenerator().generateCell(entity);
                        }

                        assert path != null;
                        return getEditableComponent(table, path, entity);
                    } else {
                        if (generator != null && generator.getReadGenerator() != null) {
                            return generator.getReadGenerator().generateCell(entity);
                        }

                        assert path != null;
                        return getNotEditableComponent(path, entity);
                    }
                });
                column = table.getColumn(property);
            } else {
                PlatformColumnGenerator custom = generators.get(property);
                if (custom != null && custom.getReadGenerator() != null) {
                    table.addGeneratedColumn(property, custom.getReadGenerator());
                    column = table.getColumn(property);
                } else {
                    assert path != null;
                    column = new Table.Column(path, property);
                }
            }

            if (path != null && metaProperty != null) {
                column.setType(path.getRangeJavaClass());
                column.setCaption(messageTools.getPropertyCaption(metaProperty));
            }

            table.addColumn(column);
        }
        //listening items removing
        ds.addCollectionChangeListener(e -> {
            Collection<Entity> items = ds.getItems();
            if (!CollectionUtils.isEmpty(items)) {
                List remove = new ArrayList<>(editing);
                for (Entity item : items) {
                    remove.remove(item.getId());
                }
                editing.removeAll(remove);
            } else {
                editing.clear();
            }
        });
        AbstractAction action = new AbstractAction("inlineEdit") {
            @Override
            public String getCaption() {
                return messages.getMainMessage("action.inlineEdit");
            }

            @Override
            public void actionPerform(Component component) {
                Set<Entity> selectedSet = getSelected();
                Entity selected = CollectionUtils.isEmpty(selectedSet) ? null : IterableUtils.get(selectedSet, selectedSet.size() - 1);
                Object id = selected == null ? null : selected.getId();
                if (id != null) {
                    if (editing.contains(id)) {//disable inline editing mode
                        editing.remove(id);
                        if (ds.isModified()) {
                            ds.commit();
                            setSelected(selected);
                        } else {
                            table.repaint();
                        }
                    } else {
                        if (!CollectionUtils.isEmpty(editing)) {
                            if (ds.isModified()) {
                                ds.commit();
                            }
                            editing.clear();
                        }

                        editing.add(id);//activate inline editing mode
                        table.repaint();
                    }
                }
            }

            private Set<Entity> getSelected() {
                return table.getSelected();
            }

            private void setSelected(Entity entity) {
                table.setSelected(entity);
            }
        };
        table.setItemClickAction(action);
        table.setEnterPressAction(action);
    }

    protected Component getEditableComponent(Table table, MetaPropertyPath path, Entity entity) {
        Field result;
        if (Boolean.class.isAssignableFrom(path.getRangeJavaClass())) {
            result = componentsFactory.createComponent(CheckBox.class);
        } else if (Entity.class.isAssignableFrom(path.getRangeJavaClass())) {
            LookupField field = componentsFactory.createComponent(LookupField.class);
            field.setOptionsList(getItemsList(metadata.getClassNN(path.getRangeJavaClass())));
            result = field;
            result.setWidth("100%");
        } else if (Enum.class.isAssignableFrom(path.getRangeJavaClass())) {
            LookupField field = componentsFactory.createComponent(LookupField.class);
            field.setOptionsEnum(path.getRangeJavaClass());
            result = field;
            result.setWidth("100%");
        } else if (Date.class.isAssignableFrom(path.getRangeJavaClass())) {
            DateField field = componentsFactory.createComponent(DateField.class);
            field.setDateFormat(DATE_FORMAT);
            result = field;
            result.setWidth("100%");
        } else {
            TextField field = componentsFactory.createComponent(TextField.class);
            field.setDatatype(path.getRange().asDatatype());
            result = field;
            result.setWidth("100%");
        }

        result.setDatasource(table.getItemDatasource(entity), path.getMetaProperty().getName());

        return result;
    }

    protected Component getNotEditableComponent(MetaPropertyPath path, Entity entity) {
        final Object value = entity.getValue(path.getMetaProperty().getName());

        if (Boolean.class.isAssignableFrom(path.getRangeJavaClass())) {
            CheckBox checkBox = componentsFactory.createComponent(CheckBox.class);
            checkBox.setEditable(false);
            checkBox.setValue(value);
            return checkBox;
        } else {
            String text = StringUtils.EMPTY;
            if (value != null) {
                if (Date.class.isAssignableFrom(path.getRangeJavaClass())) {
                    text = SIMPLE_DATE_FORMAT.format((Date) value);
                } else if (Entity.class.isAssignableFrom(path.getRangeJavaClass())) {
                    text = ((Entity) value).getInstanceName();
                } else if (Enum.class.isAssignableFrom(path.getRangeJavaClass())) {
                    text = messages.getMessage((Enum) value);
                } else if (BigDecimal.class.isAssignableFrom(path.getRangeJavaClass())) {
                    text = DECIMAL_FORMAT.format(((BigDecimal) value).doubleValue());
                } else {
                    text = value.toString();
                }
            }
            return new Table.PlainTextCell(text);
        }
    }

    protected List getItemsList(MetaClass metaClass) {
        Collection<MetaProperty> namePatternProperties = metadata.getTools().getNamePatternProperties(metaClass, true);
        if (CollectionUtils.isEmpty(namePatternProperties)) {
            throw new DevelopmentException(String.format("Unknown entity '%s' name pattern", metaClass.getName()));
        }
        String orderProperty = IterableUtils.get(namePatternProperties, 0).getName();

        List list = dataManager.loadList(LoadContext.create(metaClass.getJavaClass())
                .setQuery(LoadContext.createQuery("select e from " + metaClass.getName() + " e order by e." + orderProperty))
                .setView(View.MINIMAL));
        if (CollectionUtils.isEmpty(list)) {
            return Collections.emptyList();
        }
        return list;
    }

    protected void clearColumns(Table table) {
        List<Table.Column> columns = table.getColumns();
        if (!CollectionUtils.isEmpty(columns)) {
            columns = new ArrayList<>(columns);
            for (Table.Column column : columns) {
                table.removeColumn(column);
            }
        }
    }

    /**
     * Correct CreateTS column in table to show it without the time part
     *
     * @param table UI table with CreateTS column
     */
    public static void createTsWithoutTime(Table table) {
        ((PlatformUiHelper) AppBeans.get(NAME)).createTsWithoutTimeInternal(table);
    }

    protected void createTsWithoutTimeInternal(Table table) {
        Table.Column column = table.getColumn("createTs");
        if (column != null) {
            column.setFormatter(new DateFormatter(CREATE_TS_ELEMENT));
        }
    }
}
