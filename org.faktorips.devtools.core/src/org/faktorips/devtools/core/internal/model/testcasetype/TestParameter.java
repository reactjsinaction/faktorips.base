/***************************************************************************************************
 * Copyright (c) 2005,2006 Faktor Zehn GmbH und andere.
 * 
 * Alle Rechte vorbehalten.
 * 
 * Dieses Programm und alle mitgelieferten Sachen (Dokumentationen, Beispiele, Konfigurationen,
 * etc.) dürfen nur unter den Bedingungen der Faktor-Zehn-Community Lizenzvereinbarung – Version 0.1
 * (vor Gründung Community) genutzt werden, die Bestandteil der Auslieferung ist und auch unter
 * http://www.faktorips.org/legal/cl-v01.html eingesehen werden kann.
 * 
 * Mitwirkende: Faktor Zehn GmbH - initial API and implementation
 * 
 **************************************************************************************************/

package org.faktorips.devtools.core.internal.model.testcasetype;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;
import org.faktorips.devtools.core.internal.model.IpsObjectPart;
import org.faktorips.devtools.core.model.IIpsElement;
import org.faktorips.devtools.core.model.IIpsObject;
import org.faktorips.devtools.core.model.IIpsObjectPart;
import org.faktorips.devtools.core.model.testcasetype.ITestCaseType;
import org.faktorips.devtools.core.model.testcasetype.ITestParameter;
import org.faktorips.devtools.core.model.testcasetype.TestParameterType;
import org.faktorips.util.message.Message;
import org.faktorips.util.message.MessageList;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Test parameter class. Superclass for all test parameter.
 * 
 * @author Joerg Ortmann
 */
public abstract class TestParameter extends IpsObjectPart implements ITestParameter {

    private boolean deleted = false;

    protected TestParameterType type = TestParameterType.COMBINED;

    public TestParameter(IIpsObject parent, int id) {
        super(parent, id);
    }

    public TestParameter(IIpsObjectPart parent, int id) {
        super(parent, id);
    }

    /**
     * {@inheritDoc}
     */
    public abstract boolean isRoot();
    
    /**
     * {@inheritDoc}
     */
    public abstract ITestParameter getRootParameter();
    
    /**
     * {@inheritDoc}
     */
    public void setName(String newName) {
        String oldName = this.name;
        this.name = newName;
        valueChanged(oldName, newName);
    }
    
    /**
     * {@inheritDoc}
     */
    protected Element createElement(Document doc) {
        throw new RuntimeException("Not implemented!"); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    public void delete() {
        if (isRoot())
            ((TestCaseType)getIpsObject()).removeTestParameter(this);
        objectHasChanged();
        deleted = true;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isDeleted() {
        return deleted;
    }

    /**
     * {@inheritDoc}
     */
    public IIpsObjectPart newPart(Class partType) {
        throw new IllegalArgumentException("Unknown part type: " + partType); //$NON-NLS-1$
    }

    /**
     * {@inheritDoc}
     */
    public Image getImage() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    protected void initPropertiesFromXml(Element element, Integer id) {
        super.initPropertiesFromXml(element, id);
        setName(element.getAttribute(PROPERTY_NAME));
        TestParameterType oldType = type;
        type = TestParameterType.getTestParameterType(element.getAttribute(PROPERTY_TEST_PARAMETER_TYPE));
        valueChanged(oldType, type);
    }

    /**
     * {@inheritDoc}
     */
    protected void propertiesToXml(Element element) {
        super.propertiesToXml(element);
        element.setAttribute(PROPERTY_NAME, name);
        element.setAttribute(PROPERTY_TEST_PARAMETER_TYPE, type.getId());
    }
    
    /**
     * {@inheritDoc}
     */
    public boolean isInputParameter() {
        return type.equals(TestParameterType.INPUT) || type.equals(TestParameterType.COMBINED);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isExpextedResultParameter() {
        return type.equals(TestParameterType.EXPECTED_RESULT) || type.equals(TestParameterType.COMBINED);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isCombinedParameter() {
        return type.equals(TestParameterType.COMBINED);
    }

    /**
     *  {@inheritDoc}
     */
    public TestParameterType getTestParameterType(){
        return type;
    }
    
    /**
     * {@inheritDoc}
     */
    public abstract void setTestParameterType(TestParameterType testParameterType);
    
    /**
     * {@inheritDoc}
     */
    protected void validateThis(MessageList list) throws CoreException {
        super.validateThis(list);
    
        // check for duplicate test parameter names
        ITestParameter[] testParameters = null;
        if (isRoot()) {
            testParameters = ((ITestCaseType)getParent()).getTestParameters();
        } else {
            // get all elements on the same level (all childrens of the parent object)
            IIpsElement[] childrenOfParent = ((ITestParameter)getParent()).getChildren();
            List testParameterChildrenOfParent = new ArrayList(childrenOfParent.length);
            for (int i = 0; i < childrenOfParent.length; i++) {
                if (childrenOfParent[i] instanceof ITestParameter){
                    testParameterChildrenOfParent.add(childrenOfParent[i]);
                }
            }
            testParameters = (ITestParameter[]) testParameterChildrenOfParent.toArray(new ITestParameter[0]);
        }

        if (testParameters != null){
            for (int i = 0; i < testParameters.length; i++) {
                if (testParameters[i] != this && testParameters[i].getName().equals(name)) {
                    String text = NLS.bind(Messages.TestParameter_ValidationError_DuplicateName, name);
                    Message msg = new Message(MSGCODE_DUPLICATE_NAME, text, Message.ERROR, this, PROPERTY_NAME);
                    list.add(msg);
                    break;
                }
            }
        }
        
        // check the correct name format
        IStatus status = JavaConventions.validateFieldName(name);
        if (!status.isOK()){
            String text = NLS.bind(Messages.TestParameter_ValidateError_InvalidTestParamName, name);
            Message msg = new Message(MSGCODE_INVALID_NAME, text, Message.ERROR, this, PROPERTY_NAME);
            list.add(msg);
        }
    }
}
