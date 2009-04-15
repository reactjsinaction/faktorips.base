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

package org.faktorips.devtools.core.ui.binding;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.faktorips.devtools.core.IpsPlugin;
import org.faktorips.devtools.core.IpsStatus;
import org.faktorips.devtools.core.enums.DefaultEnumValue;
import org.faktorips.devtools.core.enums.EnumType;
import org.faktorips.devtools.core.model.ContentChangeEvent;
import org.faktorips.devtools.core.model.ContentsChangeListener;
import org.faktorips.devtools.core.model.ipsobject.IExtensionPropertyAccess;
import org.faktorips.devtools.core.model.ipsobject.IExtensionPropertyDefinition;
import org.faktorips.devtools.core.model.ipsobject.IIpsObject;
import org.faktorips.devtools.core.model.ipsobject.IIpsObjectPartContainer;
import org.faktorips.devtools.core.ui.IpsUIPlugin;
import org.faktorips.devtools.core.ui.controller.EditField;
import org.faktorips.devtools.core.ui.controller.FieldExtensionPropertyMapping;
import org.faktorips.devtools.core.ui.controller.FieldPropertyMapping;
import org.faktorips.devtools.core.ui.controller.FieldPropertyMappingByPropertyDescriptor;
import org.faktorips.devtools.core.ui.controller.Messages;
import org.faktorips.devtools.core.ui.controller.fields.CheckboxField;
import org.faktorips.devtools.core.ui.controller.fields.EnumValueField;
import org.faktorips.devtools.core.ui.controller.fields.FieldValueChangedEvent;
import org.faktorips.devtools.core.ui.controller.fields.IntegerField;
import org.faktorips.devtools.core.ui.controller.fields.LabelField;
import org.faktorips.devtools.core.ui.controller.fields.TextButtonField;
import org.faktorips.devtools.core.ui.controller.fields.TextField;
import org.faktorips.devtools.core.ui.controller.fields.ValueChangeListener;
import org.faktorips.devtools.core.ui.controls.AbstractCheckbox;
import org.faktorips.devtools.core.ui.controls.TextButtonControl;
import org.faktorips.devtools.core.util.BeanUtil;
import org.faktorips.util.message.Message;
import org.faktorips.util.message.MessageList;

/**
 * <p>
 * A <code>BindingContext</code> provides binding between the user interface and a (domain or
 * presentation) model.
 * </p>
 * <p>
 * Currently the context provides the following types of binding methods:
 * </p>
 * <ul>
 * <li><strong>bindContent</strong> Binds the content shown in a control to a model object property.
 * </li>
 * <li><strong>bindEnable</strong> Binds a control's enabled property to a model object property of
 * type boolean.</li>
 * <li><strong>bindVisible</strong> Binds a control's visible property to a model object property of
 * type boolean.</li>
 * </ul>
 * 
 * @author Jan Ortmann
 */
public class BindingContext {

    // listener for changes and focus losts. Instance of an inner class is used to avoid poluting
    // this class' interface.
    private Listener listener = new Listener();

    // list of mappings between edit fields and properties of model objects.
    private List<FieldPropertyMapping> mappings = new ArrayList<FieldPropertyMapping>();

    // a list of the ips objects containing at least one binded ips part container
    // each container is contained in the list only once, so it is actually used as a set, not
    // we still use the list, because once binded, we need to access all binded containers, and
    // this is faster with a list, than a hashset or treeset.
    private List<IIpsObject> ipsObjects = new ArrayList<IIpsObject>(1);

    private List<ControlPropertyBinding> controlBindings = new ArrayList<ControlPropertyBinding>(2);

    /**
     * Updates the UI with information from the model.
     */
    public void updateUI() {
        // defensive copy to avoid concurrent modification
        List<FieldPropertyMapping> copy = new ArrayList<FieldPropertyMapping>(mappings);

        // exceptions
        for (Iterator<FieldPropertyMapping> it = copy.iterator(); it.hasNext();) {
            FieldPropertyMapping mapping = it.next();
            try {
                mapping.setControlValue();
            } catch (Exception e) {
                IpsPlugin.log(new IpsStatus("Error updating control for property " + mapping.getPropertyName() //$NON-NLS-1$
                        + " of object " + mapping.getObject(), e)); //$NON-NLS-1$
            }
        }

        showValidationStatus(copy);
        applyControlBindings();
    }

    /**
     * Binds the given text control to the given ips object's property.
     * 
     * @return The edit field created to access the value in the text control.
     * 
     * @throws IllegalArgumentException If the property is not of type String.
     * @throws NullPointerException If any argument is <code>null</code>.
     */
    public EditField bindContent(Text text, Object object, String propertyName) {
        EditField field = null;
        PropertyDescriptor property = BeanUtil.getPropertyDescriptor(object.getClass(), propertyName);

        if (String.class == property.getPropertyType()) {
            field = new TextField(text);
        } else if (Integer.class == property.getPropertyType() || Integer.TYPE == property.getPropertyType()) {
            field = new IntegerField(text);
        }

        if (field == null) {
            throwWrongPropertyTypeException(property, new Class[] { String.class, Integer.class });
        }

        bindContent(field, object, propertyName);
        return field;
    }

    /**
     * Binds the given label to the given ips object's property.
     * 
     * @return the edit field created to access the value in the label.
     * 
     * @throws IllegalArgumentException if the property is not of type String.
     * @throws NullPointerException if any argument is <code>null</code>.
     */
    public EditField bindContent(Label label, Object object, String property) {
        checkPropertyType(object, property, String.class);
        EditField field = new LabelField(label);
        bindContent(field, object, property);
        return field;
    }

    /**
     * Binds the given checkbox to the given ips object's property.
     * 
     * @return the edit field created to access the value in the text control.
     * 
     * @throws IllegalArgumentException if the property is not of type Boolean or boolean.
     * @throws NullPointerException if any argument is <code>null</code>.
     */
    public EditField bindContent(AbstractCheckbox checkbox, Object object, String propertyName) {
        PropertyDescriptor property = BeanUtil.getPropertyDescriptor(object.getClass(), propertyName);
        if (Boolean.class != property.getPropertyType() && Boolean.TYPE != property.getPropertyType()) {
            throwWrongPropertyTypeException(property, new Class[] { Boolean.class, Boolean.TYPE });
        }

        EditField field = new CheckboxField(checkbox);
        bindContent(field, object, propertyName);
        return field;
    }

    /**
     * Binds the given text-button control to the given ips object's property.
     * 
     * @return the edit field created to access the value in the text control.
     * 
     * @throws IllegalArgumentException if the property is not of type String.
     * @throws NullPointerException if any argument is <code>null</code>.
     */
    public EditField bindContent(TextButtonControl control, Object object, String property) {
        checkPropertyType(object, property, String.class);
        EditField field = new TextButtonField(control);
        bindContent(field, object, property);

        return field;
    }

    /**
     * Binds the given combo to the given ips object's property.
     * 
     * @return the edit field created to access the value in the text control.
     * 
     * @throws IllegalArgumentException if the property's type is not a subclass of
     *             DefaultEnumValue.
     * @throws NullPointerException if any argument is <code>null</code>.
     * 
     * @see DefaultEnumValue
     */
    public EnumValueField bindContent(Combo combo, Object object, String property, EnumType enumType) {
        checkPropertyType(object, property, DefaultEnumValue.class);
        EnumValueField field = new EnumValueField(combo, enumType);
        bindContent(field, object, property);

        return field;
    }

    /**
     * Binds the given edit field to the given ips object's property.
     * 
     * @throws NullPointerException if any argument is <code>null</code>.
     */
    public void bindContent(EditField field, Object object, String property) {
        add(createMapping(field, object, property));
    }

    protected FieldPropertyMapping createMapping(EditField editField, Object object, String propertyName) {
        if (object instanceof IExtensionPropertyAccess) {
            IExtensionPropertyDefinition extProperty = IpsPlugin.getDefault().getIpsModel()
                    .getExtensionPropertyDefinition(object.getClass(), propertyName, true);
            if (extProperty != null) {
                return new FieldExtensionPropertyMapping(editField, (IExtensionPropertyAccess)object, propertyName);
            }
        }

        PropertyDescriptor property = BeanUtil.getPropertyDescriptor(object.getClass(), propertyName);
        return new FieldPropertyMappingByPropertyDescriptor(editField, object, property);
    }

    private void checkPropertyType(Object object, String propertyName, Class<?> expectedType) {
        PropertyDescriptor property = BeanUtil.getPropertyDescriptor(object.getClass(), propertyName);
        if (!expectedType.isAssignableFrom(property.getPropertyType())) {
            throw new IllegalArgumentException(
                    "Expected property " + property.getName() + " to be of type " + expectedType //$NON-NLS-1$ //$NON-NLS-2$
                            + ", but is of type " + property.getPropertyType()); //$NON-NLS-1$
        }
    }

    private void throwWrongPropertyTypeException(PropertyDescriptor property, Class<?>[] expectedTypes) {
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < expectedTypes.length; i++) {
            if (i > 0) {
                buffer.append(", "); //$NON-NLS-1$
            }
            buffer.append(expectedTypes[i]);
        }

        throw new IllegalArgumentException(
                "Property " + property.getName() + " is of type " + property.getPropertyType() //$NON-NLS-1$ //$NON-NLS-2$
                        + ", but is expected to of one of the types " + buffer.toString()); //$NON-NLS-1$
    }

    /**
     * Binds the control's enabled property to the given part container's property.
     * 
     * @param control The control which enabled property is bound
     * @param object The object the control is bound to
     * @param property The name of the object's property the control is bound to.
     * 
     * @throws IllegalArgumentException if the object's property is not of type boolean.
     * @throws NullPointerException if any argument is <code>null</code>.
     */
    public void bindEnabled(Control control, Object object, String property) {
        add(new EnableBinding(control, object, property, true));
    }

    /**
     * Binds the control's enabled property to the given part container's property.
     * 
     * @param control The control which enabled property is bound
     * @param object The object the control is bound to
     * @param property The name of the object's property the control is bound to.
     * @param enabledIfTrue <code>true</code> if the control should be enabled if the object's
     *            property is <code>true</code>, <code>false</code> if it should be enabled if the
     *            object's property is <code>false</code>.
     * 
     * @throws IllegalArgumentException if the object's property is not of type boolean.
     * @throws NullPointerException if any argument is <code>null</code>.
     */
    public void bindEnabled(Control control, Object object, String property, boolean enabledIfTrue) {
        add(new EnableBinding(control, object, property, enabledIfTrue));
    }

    /**
     * Binds the control's visible property to the given part container's property.
     * 
     * @throws IllegalArgumentException if the object's property is not of type boolean.
     * @throws NullPointerException if any argument is <code>null</code>.
     */
    public void bindVisible(Control control, Object object, String property) {
        add(new VisibleBinding(control, object, property));
    }

    private void add(FieldPropertyMapping mapping) {
        registerIpsModelChangeListener();
        mapping.getField().addChangeListener(listener);
        mapping.getField().getControl().addFocusListener(listener);
        mappings.add(mapping);
        if (mapping.getObject() instanceof IIpsObjectPartContainer) {
            IIpsObjectPartContainer container = (IIpsObjectPartContainer)mapping.getObject();
            IIpsObject ipsObject = container.getIpsObject();
            if (!ipsObjects.contains(ipsObject)) {
                ipsObjects.add(ipsObject);
            }
        } else if (mapping.getObject() instanceof PresentationModelObject) {
            PresentationModelObject pmo = (PresentationModelObject)mapping.getObject();
            pmo.addPropertyChangeListener(listener);
        }
    }

    public void add(ControlPropertyBinding binding) {
        registerIpsModelChangeListener();
        controlBindings.add(binding);
    }

    private void registerIpsModelChangeListener() {
        if (mappings.size() == 0 && controlBindings.size() == 0) {
            IpsPlugin.getDefault().getIpsModel().addChangeListener(listener);
        }
    }

    /**
     * Removes all bindings for the given control.
     */
    /*
     * TODO aw: it would be nice to have this more fine granular - remove enabledState binding,
     * content binding
     */
    public void removeBindings(Control control) {
        for (Iterator<ControlPropertyBinding> it = controlBindings.iterator(); it.hasNext();) {
            ControlPropertyBinding binding = it.next();
            if (binding.getControl() == control) {
                it.remove();
            }
        }

        for (Iterator<FieldPropertyMapping> it = this.mappings.iterator(); it.hasNext();) {
            FieldPropertyMapping binding = it.next();
            if (binding.getField().getControl() == control) {
                it.remove();
            }
        }
    }

    /**
     * Removes the registered listener.
     */
    public void dispose() {
        IpsPlugin.getDefault().getIpsModel().removeChangeListener(listener);
        // defensive copy to avoid concurrent modification
        List<FieldPropertyMapping> mappingsCopy = new ArrayList<FieldPropertyMapping>(mappings);

        // exceptions
        Set<Object> disposedPmos = new HashSet<Object>();
        for (Iterator<FieldPropertyMapping> it = mappingsCopy.iterator(); it.hasNext();) {
            FieldPropertyMapping mapping = it.next();
            if (mapping.getField().removeChangeListener(listener))
                ;
            if (!mapping.getField().getControl().isDisposed()) {
                mapping.getField().getControl().removeFocusListener(listener);
            }
            disposeObjectIfNeccessary(disposedPmos, mapping);
        }

        // defensive copy to avoid concurrent
        List<ControlPropertyBinding> controlsCopy = new ArrayList<ControlPropertyBinding>(this.controlBindings);

        // modification exceptions
        for (Iterator<ControlPropertyBinding> it = controlsCopy.iterator(); it.hasNext();) {
            ControlPropertyBinding mapping = (ControlPropertyBinding)it.next();
            disposeObjectIfNeccessary(disposedPmos, mapping);
        }
    }

    private void disposeObjectIfNeccessary(Set<Object> disposedPmos, Object object) {
        if (object instanceof IpsObjectPartPmo) {
            if (!disposedPmos.contains(object)) {
                ((IpsObjectPartPmo)object).dispose();
                disposedPmos.add(object);
            }
        }
    }

    /**
     * Validates all binded the part containers and updates the fields that are associated with
     * properties of the IpsPartContainer. It returns the MessageList which is the result of the
     * validation. This return value can be evaluated when overriding this method.
     * 
     * @return the validation message list. Never returns <code>null</code>.
     */
    protected void showValidationStatus(List<FieldPropertyMapping> propertyMappings) {
        ArrayList<IIpsObject> copy = new ArrayList<IIpsObject>(ipsObjects);
        for (Iterator<IIpsObject> it = copy.iterator(); it.hasNext();) {
            showValidationStatus(it.next(), propertyMappings);
        }
    }

    /**
     * Validates the part container and updates the fields that are associated with attributes of
     * the IpsPartContainer. It returns the MessageList which is the result of the validation. This
     * return value can be evaluated when overriding this method.
     * 
     * @return the validation message list. Never returns <code>null</code>.
     */
    protected MessageList showValidationStatus(IIpsObject ipsObject, List<FieldPropertyMapping> propertyMappings) {
        try {
            MessageList list = ipsObject.validate(ipsObject.getIpsProject());
            for (Iterator<FieldPropertyMapping> it = propertyMappings.iterator(); it.hasNext();) {
                FieldPropertyMapping mapping = it.next();
                Control c = mapping.getField().getControl();
                if (c == null || c.isDisposed()) {
                    continue;
                }

                MessageList fieldMessages;
                if (mapping.getField().isTextContentParsable()) {
                    fieldMessages = list.getMessagesFor(mapping.getObject(), mapping.getPropertyName());
                } else {
                    fieldMessages = new MessageList();
                    fieldMessages.add(Message.newError(EditField.INVALID_VALUE,
                            Messages.IpsObjectPartContainerUIController_invalidValue));
                }

                mapping.getField().setMessages(fieldMessages);
            }

            return list;

        } catch (CoreException e) {
            IpsPlugin.log(e);
            return new MessageList();
        }
    }

    private void applyControlBindings() {
        List<ControlPropertyBinding> copy = new ArrayList<ControlPropertyBinding>(controlBindings);
        for (Iterator<ControlPropertyBinding> it = copy.iterator(); it.hasNext();) {
            ControlPropertyBinding binding = it.next();
            try {
                binding.updateUI();
            } catch (Exception e) {
                IpsPlugin.log(new IpsStatus("Error updating ui with control binding " + binding)); //$NON-NLS-1$
            }
        }

    }

    class Listener implements ContentsChangeListener, ValueChangeListener, FocusListener, PropertyChangeListener {

        public void valueChanged(FieldValueChangedEvent e) {
            // defensive copy to avoid concurrent modification
            List<FieldPropertyMapping> copy = new ArrayList<FieldPropertyMapping>(mappings);

            // exceptions
            for (Iterator<FieldPropertyMapping> it = copy.iterator(); it.hasNext();) {
                FieldPropertyMapping mapping = it.next();
                if (e.field == mapping.getField()) {
                    try {
                        mapping.setPropertyValue();
                    } catch (Exception ex) {
                        IpsPlugin.log(new IpsStatus("Error updating model property " + mapping.getPropertyName() //$NON-NLS-1$
                                + " of object " + mapping.getObject(), ex)); //$NON-NLS-1$
                    }
                }
            }
        }

        public void focusGained(FocusEvent e) {
            // nothing to do
        }

        public void focusLost(FocusEvent e) {
            // broadcast outstanding change events
            IpsUIPlugin.getDefault().getEditFieldChangeBroadcaster().broadcastLastEvent();
        }

        public void contentsChanged(ContentChangeEvent event) {
            // defensive copy to avoid concurrent modification
            List<FieldPropertyMapping> copy = new ArrayList<FieldPropertyMapping>(mappings);

            // exceptions
            for (Iterator<FieldPropertyMapping> it = copy.iterator(); it.hasNext();) {
                FieldPropertyMapping mapping = it.next();
                if (mapping.getObject() instanceof IIpsObjectPartContainer) {
                    if (event.isAffected((IIpsObjectPartContainer)mapping.getObject())) {
                        try {
                            mapping.setControlValue();
                        } catch (Exception ex) {
                            IpsPlugin.log(new IpsStatus("Error updating model property " + mapping.getPropertyName() //$NON-NLS-1$
                                    + " of object " + mapping.getObject(), ex)); //$NON-NLS-1$
                        }
                    }
                }
            }

            showValidationStatus(copy);
            applyControlBindings();
        }

        /**
         * {@inheritDoc}
         */
        public void propertyChange(PropertyChangeEvent evt) {
            // defensive copy to avoid concurrent modification
            List<FieldPropertyMapping> copy = new ArrayList<FieldPropertyMapping>(mappings);

            // exceptions
            for (Iterator<FieldPropertyMapping> it = copy.iterator(); it.hasNext();) {
                FieldPropertyMapping mapping = it.next();
                if (mapping.getObject() == evt.getSource()) {
                    try {
                        mapping.setControlValue();
                    } catch (Exception ex) {
                        IpsPlugin.log(new IpsStatus("Error updating model property " + mapping.getPropertyName() //$NON-NLS-1$
                                + " of object " + mapping.getObject(), ex)); //$NON-NLS-1$
                    }
                }
            }

            applyControlBindings();
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer("Ctx["); //$NON-NLS-1$
        for (int i = 0; i < ipsObjects.size(); i++) {
            if (i > 0) {
                sb.append(", "); //$NON-NLS-1$
            }
            IIpsObject ipsObject = (IIpsObject)ipsObjects.get(i);
            sb.append(ipsObject.getName());
        }
        sb.append(']');

        return sb.toString();
    }

}
