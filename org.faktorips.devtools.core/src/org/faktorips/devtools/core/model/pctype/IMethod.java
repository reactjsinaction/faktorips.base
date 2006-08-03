/*******************************************************************************
 * Copyright (c) 2005,2006 Faktor Zehn GmbH und andere.
 *
 * Alle Rechte vorbehalten.
 *
 * Dieses Programm und alle mitgelieferten Sachen (Dokumentationen, Beispiele,
 * Konfigurationen, etc.) duerfen nur unter den Bedingungen der 
 * Faktor-Zehn-Community Lizenzvereinbarung - Version 0.1 (vor Gruendung Community) 
 * genutzt werden, die Bestandteil der Auslieferung ist und auch unter
 *   http://www.faktorips.org/legal/cl-v01.html
 * eingesehen werden kann.
 *
 * Mitwirkende:
 *   Faktor Zehn GmbH - initial API and implementation - http://www.faktorzehn.de
 *
 *******************************************************************************/

package org.faktorips.devtools.core.model.pctype;


/**
 * Method.
 */
public interface IMethod extends IMember, IParameterContainer {

    public final static String PROPERTY_DATATYPE = "datatype"; //$NON-NLS-1$
    public final static String PROPERTY_MODIFIER = "modifier"; //$NON-NLS-1$
    public final static String PROPERTY_ABSTRACT = "abstract"; //$NON-NLS-1$
    public final static String PROPERTY_PARAMETERS = "parameters"; //$NON-NLS-1$
    public final static String PROPERTY_BODY = "body"; //$NON-NLS-1$
    
    public final static String PROPERTY_PARAM_NAME = "param.name"; //$NON-NLS-1$
    public final static String PROPERTY_PARAM_DATATYPE = "param.datatype"; //$NON-NLS-1$
    
    /**
     * Returns the policy component type this method belongs to.
     */
    public IPolicyCmptType getPolicyCmptType();
    
    public String getDatatype();
    
    public void setDatatype(String newDatatype);
    
    public Modifier getModifier();
    
    public void setModifier(Modifier newModifier);
    
    public boolean isAbstract();
    
    public void setAbstract(boolean newValue);
    
    /**
     * Returns the Java modifier. Determined from the ips modifier and the abstract flag.
     * 
     * @see java.lang.reflect.Modifier
     */
    public int getJavaModifier();

    /**
     * Returns the parameter names.
     */
    public String[] getParameterNames();
    
    /**
     * Returns the parameter types.
     */
    public String[] getParameterTypes();
    
    /**
     * Returns <code>true</code> if the other method has the same name, the same numer of parameters
     * and each parameter has the same datatype as the parameter in this method. Returns <code>false</code> otherwise.
     * Note that the return type is not checked. 
     */
    public boolean isSame(IMethod method);
    
    
}
