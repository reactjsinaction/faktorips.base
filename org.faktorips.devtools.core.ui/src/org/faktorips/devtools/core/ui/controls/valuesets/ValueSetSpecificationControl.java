/*******************************************************************************
 * Copyright (c) 2005-2009 Faktor Zehn AG und andere.
 * 
 * Alle Rechte vorbehalten.
 * 
 * Dieses Programm und alle mitgelieferten Sachen (Dokumentationen, Beispiele, Konfigurationen,
 * etc.) duerfen nur unter den Bedingungen der Faktor-Zehn-Community Lizenzvereinbarung - Version
 * 0.1 (vor Gruendung Community) genutzt werden, die Bestandteil der Auslieferung ist und auch unter
 * http://www.faktorzehn.org/f10-org:lizenzen:community eingesehen werden kann.
 * 
 * Mitwirkende: Faktor Zehn AG - initial API and implementation - http://www.faktorzehn.de
 *******************************************************************************/

package org.faktorips.devtools.core.ui.controls.valuesets;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.faktorips.datatype.ValueDatatype;
import org.faktorips.devtools.core.model.valueset.IValueSet;
import org.faktorips.devtools.core.model.valueset.IValueSetOwner;
import org.faktorips.devtools.core.model.valueset.ValueSetType;
import org.faktorips.devtools.core.ui.IDataChangeableReadWriteAccess;
import org.faktorips.devtools.core.ui.UIToolkit;
import org.faktorips.devtools.core.ui.controller.DefaultUIController;
import org.faktorips.devtools.core.ui.controller.fields.CheckboxField;
import org.faktorips.devtools.core.ui.controller.fields.ComboField;
import org.faktorips.devtools.core.ui.controller.fields.FieldValueChangedEvent;
import org.faktorips.devtools.core.ui.controller.fields.ValueChangeListener;
import org.faktorips.devtools.core.ui.controls.Checkbox;
import org.faktorips.devtools.core.ui.controls.ControlComposite;
import org.faktorips.devtools.core.ui.controls.Messages;

/**
 * A control to edit a value set including it's type.
 */
public class ValueSetSpecificationControl extends ControlComposite implements IDataChangeableReadWriteAccess {

    private ValueSetControlEditMode editMode = ValueSetControlEditMode.ALL_KIND_OF_SETS;
    private IValueSetOwner valueSetOwner;

    private ValueSetEditControlFactory valueSetEditControlFactory = new ValueSetEditControlFactory();

    private List<ValueSetType> allowedValueSetTypes = new ArrayList<ValueSetType>();
    // combo showing the allowed value set types
    private Combo valueSetTypesCombo;
    private ComboField valueSetTypeField;

    private Checkbox concreteValueSetCheckbox = null;
    private CheckboxField concreteValueSetField = null;

    private Control valueSetEditControl; // control showing the value set
    // can be safely casted to IValueSetEditControl

    private Composite valueSetArea; // area around the value set, used to change the layout

    // The last selected value set that is not an unrestricted value set.
    private IValueSet lastRestrictedValueSet;

    private UIToolkit toolkit;
    private DefaultUIController uiController;

    private Label valueSetTypeLabel;

    private boolean dataChangeable;

    /**
     * Creates a new control which contains a combo box and depending on the value of the box a
     * EnumValueSetEditControl or a RangeEditControl. the following general layout is used: the main
     * layout is a gridlayout with one column. In the first row a composite with a 2 column
     * gridlayout is created. In the second row a stacklayout is used to swap the
     * EnumValueSetEditControl and RangeEditControl dynamically.
     */
    public ValueSetSpecificationControl(Composite parent, UIToolkit toolkit, DefaultUIController uiController,
            IValueSetOwner valueSetOwner, List<ValueSetType> allowedValueSetTypes, ValueSetControlEditMode editMode) {
        super(parent, SWT.NONE);
        this.valueSetOwner = valueSetOwner;
        this.toolkit = toolkit;
        this.uiController = uiController;
        this.editMode = editMode;

        initControls(toolkit);
        setAllowedValueSetTypes(allowedValueSetTypes);
    }

    private void initControls(UIToolkit toolkit) {
        initLayout();
        Composite parentArea = createParentArea(toolkit);
        createValueSetTypesCombo(toolkit, parentArea);
        createConcreteValueSetCheckbox(toolkit, parentArea);

        valueSetArea = createValueSetArea(toolkit, parentArea);
        showControlForValueSet();
        if (toolkit.getFormToolkit() != null) {
            toolkit.getFormToolkit().adapt(this); // has to be done after the text control is
            // created!
        }
    }

    private Composite createParentArea(UIToolkit toolkit) {
        Composite parentArea;
        if (toolkit.getFormToolkit() == null) {
            parentArea = this;
        } else {
            parentArea = toolkit.getFormToolkit().createComposite(this);
            GridLayout formAreaLayout = new GridLayout(1, false);
            formAreaLayout.marginHeight = 3;
            formAreaLayout.marginWidth = 1;
            parentArea.setLayout(formAreaLayout);
        }
        parentArea.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_END | GridData.FILL_HORIZONTAL));
        return parentArea;
    }

    private Composite createValueSetArea(UIToolkit toolkit, Composite parentArea) {
        Composite valueArea = toolkit.createComposite(parentArea);
        GridData stackData = new GridData(GridData.VERTICAL_ALIGN_END | GridData.FILL_HORIZONTAL);
        stackData.horizontalSpan = 2;
        valueArea.setLayoutData(stackData);
        valueArea.setLayout(new StackLayout());
        return valueArea;
    }

    /**
     * Returns the value set being edited.
     */
    public IValueSet getValueSet() {
        return valueSetOwner.getValueSet();
    }

    /**
     * Returns the type of the value set being edited.
     */
    public ValueSetType getValueSetType() {
        return valueSetOwner.getValueSet().getValueSetType();
    }

    public ValueDatatype getValueDatatype() {
        try {
            return valueSetOwner.getValueDatatype();
        } catch (CoreException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Selects the given value set type. Or the first item if the value set type is not in the list
     * of available item.
     */
    public void setValueSetType(ValueSetType valueSetType) {
        valueSetTypeField.setText(valueSetType.getName());
        // value set in the valueSetOwner is updated through event handling
    }

    /**
     * Returns the edit mode being used.
     */
    public ValueSetControlEditMode getEditMode() {
        return editMode;
    }

    /**
     * Sets the new edit mode. If the new edit mode does not allow to edit abstract value sets, but
     * the current value set being edited is abstract, it is set to not abstract.
     */
    public void setEditMode(ValueSetControlEditMode newEditMode) {
        if (editMode == newEditMode) {
            return;
        }
        editMode = newEditMode;
        IValueSet valueSet = getValueSet();
        if (!editMode.canDefineAbstractSets() && valueSet.isAbstract() && !valueSet.isUnrestricted()) {
            valueSet.setAbstract(false);
        }
        updateUI();
    }

    private Control showControlForValueSet() {
        StackLayout layout = (StackLayout)valueSetArea.getLayout();
        layout.topControl = updateControlWithCurrentValueSetOrCreateNewIfNeccessary(valueSetArea);
        // layout.topControl.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
        setDataChangeable(isDataChangeable()); // set data changeable state of controls
        return layout.topControl;
    }

    private Control updateControlWithCurrentValueSetOrCreateNewIfNeccessary(Composite parent) {
        IValueSet valueSet = getValueSet();
        if (valueSet.isAbstract() || valueSet.isUnrestricted()) {
            // no further editing possible, return empty composite
            return toolkit.createComposite(parent);
        }
        ValueDatatype valueDatatype = getValueDatatype();
        if (getValueSetEditControl() != null && getValueSetEditControl().canEdit(valueSet, valueDatatype)) {
            // the current composite can be reused to edit the current value set
            getValueSetEditControl().setValueSet(valueSet, valueDatatype);
            return valueSetEditControl.getParent();
        }
        // Creates a new composite to edit the current value set
        Group group = createGroupAroundValueSet(parent, valueSet.getValueSetType().getName());
        Control c = valueSetEditControlFactory.newControl(valueSet, valueDatatype, group, toolkit, uiController);
        c.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_END | GridData.FILL_HORIZONTAL));
        setValueSetEditControl(c);
        return group;
    }

    /**
     * Returns the value set control casted to {@link IValueSetEditControl}. Neccessary as the SWT
     * type Control is a class and not an interface.
     */
    private IValueSetEditControl getValueSetEditControl() {
        return (IValueSetEditControl)valueSetEditControl;
    }

    /**
     * Sets the value set control. Neccessary as the SWT type Control is a class and not an
     * interface.
     */
    private void setValueSetEditControl(Control newValueSetEditControl) {
        valueSetEditControl = newValueSetEditControl;
    }

    private Group createGroupAroundValueSet(Composite parent, String title) {
        Group group = toolkit.createGroup(parent, title);
        GridLayout layout = new GridLayout(1, false);
        layout.marginHeight = 10;
        group.setLayout(layout);
        return group;
    }

    private void initLayout() {
        GridLayout mainAreaLayout = new GridLayout(2, false);
        mainAreaLayout.marginHeight = 0;
        mainAreaLayout.marginWidth = 0;
        setLayout(mainAreaLayout);
    }

    /**
     * Sets the width of the type label. The method could be used to align the control in the second
     * column with a control in one position (row) above.
     */
    public void setLabelWidthHint(int widthHint) {
        Object layoutData = valueSetTypeLabel.getLayoutData();
        if (layoutData instanceof GridData) {
            ((GridData)layoutData).widthHint = widthHint;
        }
    }

    public Label getValueSetTypeLabel() {
        return valueSetTypeLabel;
    }

    private void createValueSetTypesCombo(UIToolkit toolkit, Composite parentArea) {
        valueSetTypeLabel = toolkit.createFormLabel(this, Messages.ValueSetEditControl_labelType);
        GridData labelGridData = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
        valueSetTypeLabel.setLayoutData(labelGridData);

        valueSetTypesCombo = toolkit.createCombo(this);
        valueSetTypesCombo.setText(getValueSetType().getName());
        valueSetTypeField = new ComboField(valueSetTypesCombo);
        valueSetTypeField.addChangeListener(new ValueSetTypeModifyListener());

        toolkit.setDataChangeable(valueSetTypesCombo, isDataChangeable());
    }

    private void createConcreteValueSetCheckbox(UIToolkit toolkit, Composite parent) {
        if (!editMode.canDefineAbstractSets()) {
            return; // the user has no choice, so need to create a checkbox
        }
        toolkit.createLabel(parent, "Specify Bounds/Values:");
        concreteValueSetCheckbox = toolkit.createCheckbox(parent);
        concreteValueSetField = new CheckboxField(concreteValueSetCheckbox);
        updateConcreteValueSetCheckbox();
        concreteValueSetField.addChangeListener(new ValueChangeListener() {

            public void valueChanged(FieldValueChangedEvent e) {
                boolean checked = ((Boolean)e.field.getValue());
                getValueSet().setAbstract(!checked);
                updateUI();
            }

        });
    }

    @Override
    public boolean setFocus() {
        return valueSetTypesCombo.setFocus();
    }

    /**
     * Sets the list of value set types the user can select in the Combo box.
     */
    public void setAllowedValueSetTypes(List<ValueSetType> valueSetTypes) {
        allowedValueSetTypes.clear();
        allowedValueSetTypes.addAll(valueSetTypes);
        ValueSetType oldType = getValueSetType();
        ValueSetType newType = valueSetTypes.get(0);

        valueSetTypesCombo.removeAll();
        for (ValueSetType type : valueSetTypes) {
            valueSetTypesCombo.add(type.getName());
            if (oldType == type) {
                newType = oldType;
            }
        }
        valueSetTypeField.setText(newType.getName());
        valueSetTypesCombo.setEnabled(valueSetTypes.size() > 1);
    }

    /**
     * Returns the list of value set types the user can select in the Combo box.
     */
    public List<ValueSetType> getAllowedValueSetTypes() {
        List<ValueSetType> types = new ArrayList<ValueSetType>();
        types.addAll(allowedValueSetTypes);
        return types;
    }

    /**
     * Sets the available value set types.
     * 
     * @deprecated use {@link #setAllowedValueSetTypes(List)
     */
    @Deprecated
    public void setAllowedValueSetTypes(ValueSetType[] valueSetTypes) {
        List<ValueSetType> types = new ArrayList<ValueSetType>();
        for (int i = 0; i < valueSetTypes.length; i++) {
            types.add(valueSetTypes[0]);
        }
        setAllowedValueSetTypes(types);
    }

    private class ValueSetTypeModifyListener implements ValueChangeListener {
        /**
         * {@inheritDoc}
         */
        public void valueChanged(FieldValueChangedEvent e) {
            String selectedText = e.field.getText();
            ValueSetType newValueSetType = ValueSetType.getValueSetTypeByName(selectedText);
            changeValueSetType(newValueSetType);
        }

    }

    private void changeValueSetType(ValueSetType newValueSetType) {
        IValueSet oldValueSet = valueSetOwner.getValueSet();
        if (!oldValueSet.isUnrestricted()) {
            lastRestrictedValueSet = oldValueSet;
        }
        if (oldValueSet.getValueSetType().equals(newValueSetType)) {
            return; // unchanged
        }
        valueSetOwner.setValueSetType(newValueSetType);
        IValueSet newValueSet = valueSetOwner.getValueSet();
        if (lastRestrictedValueSet == null) {
            newValueSet.setAbstract(getDefaultForAbstractProperty());
        } else {
            newValueSet.setValuesOf(lastRestrictedValueSet);
        }
        updateUI();
    }

    private void updateUI() {
        showControlForValueSet();
        valueSetArea.layout(); // show the new top control
        valueSetArea.getParent().layout(); // parent has to resize
        valueSetArea.getParent().getParent().layout(); // parent has to resize

        uiController.updateUI();
        updateConcreteValueSetCheckbox();
    }

    private void updateConcreteValueSetCheckbox() {
        if (concreteValueSetCheckbox != null) {
            IValueSet valueSet = valueSetOwner.getValueSet();
            boolean checked = !valueSet.isAbstract() && !valueSet.isUnrestricted();
            concreteValueSetCheckbox.setChecked(checked);
            boolean enabled = !valueSet.isUnrestricted() && editMode.canDefineAbstractSets();
            concreteValueSetCheckbox.setEnabled(enabled);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setDataChangeable(boolean changeable) {
        dataChangeable = changeable;
        toolkit.setDataChangeable(valueSetTypesCombo, changeable);
        toolkit.setDataChangeable(valueSetEditControl, changeable);
        toolkit.setDataChangeable(concreteValueSetCheckbox, changeable);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isDataChangeable() {
        return dataChangeable;
    }

    @Override
    public void setEnabled(boolean enable) {
        setEnabledIfExistent(valueSetTypeLabel, enable);
        setEnabledIfExistent(valueSetTypesCombo, enable);
        setEnabledIfExistent(concreteValueSetCheckbox, enable);
        setEnabledIfExistent(valueSetEditControl, enable);
        // TODO RangeEditControl#setEnabled() sauber implementieren
    }

    private void setEnabledIfExistent(Control control, boolean enable) {
        if (control != null && !control.isDisposed()) {
            control.setEnabled(enable);
        }
    }

    private boolean getDefaultForAbstractProperty() {
        return editMode.canDefineAbstractSets();
    }

}
