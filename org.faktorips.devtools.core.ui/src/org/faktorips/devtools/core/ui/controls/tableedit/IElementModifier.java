/*******************************************************************************
 * Copyright (c) 2005-2012 Faktor Zehn AG und andere.
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

package org.faktorips.devtools.core.ui.controls.tableedit;

/**
 * Interface for classes that can access and modify a {@link IEditTabelModel table model's}
 * elements.
 * 
 * @author Stefan Widmaier
 */
public interface IElementModifier {
    /**
     * Returns the value for the given element.
     * 
     * @param element the value to access
     */
    public Object getValue(Object element);

    /**
     * Sets the given value as value for the given element.
     * 
     * @param element the value to modify
     */
    public void setValue(Object element, Object value);
}