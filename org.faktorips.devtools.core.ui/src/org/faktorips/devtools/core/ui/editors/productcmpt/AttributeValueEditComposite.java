/*******************************************************************************
 * Copyright (c) Faktor Zehn AG. <http://www.faktorzehn.org>
 * 
 * This source code is available under the terms of the AGPL Affero General Public License version
 * 3.
 * 
 * Please see LICENSE.txt for full license terms, including the additional permissions and
 * restrictions as well as the possibility of alternative license terms.
 *******************************************************************************/

package org.faktorips.devtools.core.ui.editors.productcmpt;

import java.util.List;
import java.util.Locale;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.faktorips.datatype.ValueDatatype;
import org.faktorips.devtools.core.IpsPlugin;
import org.faktorips.devtools.core.internal.model.productcmpt.MultiValueHolder;
import org.faktorips.devtools.core.internal.model.productcmpt.SingleValueHolder;
import org.faktorips.devtools.core.model.IInternationalString;
import org.faktorips.devtools.core.model.ipsobject.IExtensionPropertyDefinition;
import org.faktorips.devtools.core.model.productcmpt.IAttributeValue;
import org.faktorips.devtools.core.model.productcmpt.IValueHolder;
import org.faktorips.devtools.core.model.productcmpttype.IProductCmptTypeAttribute;
import org.faktorips.devtools.core.model.value.IValue;
import org.faktorips.devtools.core.model.value.ValueFactory;
import org.faktorips.devtools.core.model.value.ValueType;
import org.faktorips.devtools.core.model.valueset.IValueSet;
import org.faktorips.devtools.core.ui.ExtensionPropertyControlFactory;
import org.faktorips.devtools.core.ui.IpsUIPlugin;
import org.faktorips.devtools.core.ui.UIToolkit;
import org.faktorips.devtools.core.ui.ValueDatatypeControlFactory;
import org.faktorips.devtools.core.ui.binding.BindingContext;
import org.faktorips.devtools.core.ui.binding.IpsObjectPartPmo;
import org.faktorips.devtools.core.ui.binding.PropertyChangeBinding;
import org.faktorips.devtools.core.ui.controller.EditField;
import org.faktorips.devtools.core.ui.controller.fields.LocalizedStringEditField;
import org.faktorips.devtools.core.ui.controller.fields.TextButtonField;
import org.faktorips.devtools.core.ui.controls.InternationalStringControl;
import org.faktorips.devtools.core.ui.controls.InternationalStringDialogHandler;
import org.faktorips.devtools.core.ui.controls.MultiValueAttributeControl;
import org.faktorips.devtools.core.ui.forms.IpsSection;
import org.faktorips.values.LocalizedString;

/**
 * Provides controls that allow the user to edit the an {@link IAttributeValue}.
 * <p>
 * For attributes that do not change over time, a decoration marker is attached to the edit control.
 * 
 * @since 3.6
 * 
 * @author Alexander Weickmann, Faktor Zehn AG
 * 
 * @see IAttributeValue
 */
public class AttributeValueEditComposite extends EditPropertyValueComposite<IProductCmptTypeAttribute, IAttributeValue> {

    private ExtensionPropertyControlFactory extProContFact;

    public AttributeValueEditComposite(IProductCmptTypeAttribute property, IAttributeValue propertyValue,
            IpsSection parentSection, Composite parent, BindingContext bindingContext, UIToolkit toolkit) {
        super(property, propertyValue, parentSection, parent, bindingContext, toolkit);
        extProContFact = new ExtensionPropertyControlFactory(propertyValue);
        initControls();
    }

    @Override
    protected void setLayout() {
        super.setLayout();
        GridLayout clientLayout = (GridLayout)getLayout();
        clientLayout.numColumns = 2;
    }

    @Override
    protected void createEditFields(List<EditField<?>> editFields) throws CoreException {
        createValueEditField(editFields);
        if (showTemplateButton()) {
            createTemplateStatusButton();
        }
        createControlForExtensionProperty();
    }

    private void createValueEditField(List<EditField<?>> editFields) throws CoreException {
        ValueDatatype datatype = getProperty() == null ? null : getProperty().findDatatype(
                getPropertyValue().getIpsProject());
        createEditField(datatype, editFields);
    }

    protected void createEditField(ValueDatatype datatype, List<EditField<?>> editFields) {
        EditField<?> editField;

        if (getPropertyValue().getValueHolder() instanceof MultiValueHolder) {
            editField = createMultiValueField(datatype);
        } else if (getPropertyValue().getValueHolder() instanceof SingleValueHolder) {
            editField = createSingleValueField(datatype);
        } else {
            throw new RuntimeException("Illegal value holder instance in attribute " + getProperty().getName()); //$NON-NLS-1$
        }
        getBindingContext().bindProblemMarker(editField, getPropertyValue(), IAttributeValue.PROPERTY_ATTRIBUTE);
        getBindingContext().bindProblemMarker(editField, getPropertyValue(), IAttributeValue.PROPERTY_VALUE_HOLDER);
        getBindingContext().bindProblemMarker(editField, getPropertyValue(),
                IAttributeValue.PROPERTY_TEMPLATE_VALUE_STATUS);
        editFields.add(editField);
        addChangingOverTimeDecorationIfRequired(editField);
    }

    private EditField<?> createMultiValueField(ValueDatatype datatype) {
        EditField<?> editField;
        MultiValueAttributeControl control = new MultiValueAttributeControl(this, getToolkit(), getProperty(),
                getPropertyValue(), datatype);
        editField = new TextButtonField(control);
        ValueHolderToFormattedStringWrapper wrapper = ValueHolderToFormattedStringWrapper
                .createWrapperFor(getPropertyValue());
        getBindingContext().bindContent(editField, wrapper,
                ValueHolderToFormattedStringWrapper.PROPERTY_FORMATTED_VALUE);
        return editField;
    }

    private EditField<?> createSingleValueField(ValueDatatype datatype) {
        IValueSet valueSet = getProperty() == null ? null : getProperty().getValueSet();
        SingleValueHolder singleValueHolder = (SingleValueHolder)getPropertyValue().getValueHolder();

        if (singleValueHolder.getValueType() == ValueType.STRING) {
            return createSimpleField(datatype, valueSet);
        } else if (singleValueHolder.getValueType() == ValueType.INTERNATIONAL_STRING) {
            return createInternationalStringField();
        } else {
            throw new RuntimeException("Illegal value type in attribute " + getProperty().getName()); //$NON-NLS-1$
        }
    }

    private EditField<?> createSimpleField(ValueDatatype datatype, IValueSet valueSet) {
        ValueHolderPmo valueHolderPMO = new ValueHolderPmo(getPropertyValue());
        ValueDatatypeControlFactory controlFactory = IpsUIPlugin.getDefault().getValueDatatypeControlFactory(datatype);
        EditField<?> editField = controlFactory.createEditField(getToolkit(), this, datatype, valueSet,
                getPropertyValue().getIpsProject());
        getBindingContext().bindContent(editField, valueHolderPMO, ValueHolderPmo.PROPERTY_STRING_VALUE);
        return editField;
    }

    private EditField<?> createInternationalStringField() {
        final Locale localizationLocale = IpsPlugin.getMultiLanguageSupport().getLocalizationLocaleOrDefault(
                getPropertyValue().getIpsProject());
        MultilingualValueHolderPmo valueHolderPMO = new MultilingualValueHolderPmo(getPropertyValue(),
                localizationLocale);
        InternationalStringDialogHandler handler = new MyMultilingualValueAttributeHandler(getShell(),
                getPropertyValue());
        InternationalStringControl control = new InternationalStringControl(this, getToolkit(), handler);
        LocalizedStringEditField editField = new LocalizedStringEditField(control.getTextControl());
        getBindingContext().bindContent(editField, valueHolderPMO,
                MultilingualValueHolderPmo.PROPERTY_LOCALIZED_STRING_VALUE);
        return editField;
    }

    protected void createTemplateStatusButton() {
        final ToolBar toolBar = new ToolBar(this, SWT.FLAT);
        final ToolItem toolItem = new ToolItem(toolBar, SWT.PUSH);

        final TemplateValuePmo pmo = new TemplateValuePmo(getPropertyValue());
        toolItem.setImage(pmo.getTemplateValueStatus().getIcon());
        getBindingContext().add(
                new PropertyChangeBinding<TemplateValueUiStatus>(toolBar, pmo,
                        TemplateValuePmo.PROPERTY_TEMPLATE_VALUE_STATUS, TemplateValueUiStatus.class) {

                    @Override
                    protected void propertyChanged(TemplateValueUiStatus oldValue, TemplateValueUiStatus newValue) {
                        toolItem.setImage(newValue.getIcon());
                    }
                });

        toolItem.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                pmo.onClick();
            }

        });
    }

    private void createControlForExtensionProperty() {
        extProContFact
                .createControls(this, getToolkit(), getPropertyValue(), IExtensionPropertyDefinition.POSITION_TOP);
        extProContFact.createControls(this, getToolkit(), getPropertyValue(),
                IExtensionPropertyDefinition.POSITION_BOTTOM);
        extProContFact.bind(getBindingContext());
    }

    private static class MyMultilingualValueAttributeHandler extends InternationalStringDialogHandler {

        private final IAttributeValue attributeValue;

        private MyMultilingualValueAttributeHandler(Shell shell, IAttributeValue part) {
            super(shell, part);
            this.attributeValue = part;
        }

        @Override
        protected IInternationalString getInternationalString() {
            IValueHolder<?> valueHolder = attributeValue.getValueHolder();
            if (valueHolder instanceof SingleValueHolder) {
                SingleValueHolder singleValueHolder = (SingleValueHolder)valueHolder;
                IValue<?> value = singleValueHolder.getValue();
                Object content = value.getContent();
                if (content instanceof IInternationalString) {
                    IInternationalString internationalString = (IInternationalString)content;
                    return internationalString;
                }
            }
            throw new IllegalArgumentException("The object provided to the InternationalStringDialog is not supported."); //$NON-NLS-1$
        }
    }

    public static class ValueHolderPmo extends IpsObjectPartPmo {

        public static final String PROPERTY_STRING_VALUE = "stringValue"; //$NON-NLS-1$

        public ValueHolderPmo(IAttributeValue attributeValue) {
            super(attributeValue);
        }

        public String getStringValue() {
            return ((SingleValueHolder)getValueHolder()).getValue().getContentAsString();
        }

        public IValueHolder<?> getValueHolder() {
            return getIpsObjectPartContainer().getValueHolder();
        }

        @Override
        public IAttributeValue getIpsObjectPartContainer() {
            return (IAttributeValue)super.getIpsObjectPartContainer();
        }

        public void setStringValue(String value) {
            ((SingleValueHolder)getValueHolder()).setValue(ValueFactory.createStringValue(value));
            notifyListeners();
        }
    }

    public static class MultilingualValueHolderPmo extends IpsObjectPartPmo {

        public static final String PROPERTY_LOCALIZED_STRING_VALUE = "localizedStringValue"; //$NON-NLS-1$

        private final Locale locale;

        public MultilingualValueHolderPmo(IAttributeValue attributeValue, Locale locale) {
            super(attributeValue);
            this.locale = locale;
        }

        @Override
        public IAttributeValue getIpsObjectPartContainer() {
            return (IAttributeValue)super.getIpsObjectPartContainer();
        }

        public SingleValueHolder getSingleValueHolder() {
            return (SingleValueHolder)getIpsObjectPartContainer().getValueHolder();
        }

        public LocalizedString getLocalizedStringValue() {
            IValue<?> value = getSingleValueHolder().getValue();
            return value == null || value.getContent() == null ? null : ((IInternationalString)value.getContent())
                    .get(locale);
        }

        public void setLocalizedStringValue(LocalizedString newValue) {
            IValue<?> value = getSingleValueHolder().getValue();
            if (value != null) {
                IInternationalString currentString = (IInternationalString)value.getContent();
                currentString.add(newValue);
            }
            notifyListeners();
        }

    }

}