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

package org.faktorips.devtools.core.model;




/**
 *
 */
public interface IIpsObjectPart extends IIpsObjectPartContainer {
    
    public final static String PROPERTY_DESCRIPTION = "description"; //$NON-NLS-1$
    public final static String PROPERTY_ID = "id"; //$NON-NLS-1$
    
    /**
     * Returns the object this part belongs to.
     */
    public IIpsObject getIpsObject();
    
    /**
     * The part's id that uniquely identifies it in it's parent.
     */
    public int getId();

    /**
     * Deletes the part. 
     */
    public void delete();
    
    /**
     * Returns whether the part was deleted (<code>true</code>) or not. 
     */
    public boolean isDeleted();
    
}
