package com.groupstp.platform.web.filter;

import com.haulmont.cuba.gui.components.AbstractAction;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.components.filter.ConditionsTree;
import com.haulmont.cuba.gui.components.filter.FilterDelegateImpl;
import com.haulmont.cuba.gui.icons.CubaIcon;

/**
 * Extended generic filter with auto applicable reset button
 *
 * @author adiatullin
 */
public class ExtFilterDelegateImpl extends FilterDelegateImpl {

    protected Button resetAll;

    @Override
    protected void createControlsLayoutForGeneric() {
        super.createControlsLayoutForGeneric();
        resetAll = componentsFactory.createComponent(Button.class);
        resetAll.setCaption(messages.getMessage(getClass(), "resetFilter"));
        resetAll.setIcon(CubaIcon.CANCEL.source());
        resetAll.setAction(new AbstractAction("reset") {
            @Override
            public void actionPerform(Component component) {
                reset();
            }
        });
        controlsLayout.add(resetAll, 1);
    }

    protected void reset() {
        conditions = new ConditionsTree();
        setFilterEntity(adHocFilter);

        apply(true);
    }

    @Override
    public void setEditable(boolean editable) {
        super.setEditable(editable);
        resetAll.setVisible(editable && userCanEditFilers());
    }
}
