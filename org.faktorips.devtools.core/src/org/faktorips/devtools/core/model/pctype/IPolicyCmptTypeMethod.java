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

package org.faktorips.devtools.core.model.pctype;

import org.faktorips.devtools.core.model.type.IMethod;

/**
 * An {@link IMethod} that is part of an {@link IPolicyCmptType}.
 * 
 * @since 3.6
 * 
 * @author Alexander Weickmann
 */
public interface IPolicyCmptTypeMethod extends IMethod {

    /**
     * Returns the {@link IPolicyCmptType} this {@link IPolicyCmptTypeMethod} belongs to.
     */
    public IPolicyCmptType getPolicyCmptType();

}