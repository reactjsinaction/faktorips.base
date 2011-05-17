/*******************************************************************************
 * Copyright (c) 2005-2011 Faktor Zehn AG und andere.
 * 
 * Alle Rechte vorbehalten.
 * 
 * Dieses Programm und alle mitgelieferten Sachen (Dokumentationen, Beispiele, Konfigurationen,
 * etc.) duerfen nur unter den Bedingungen der Faktor-Zehn-Community Lizenzvereinbarung - Version
 * 0.1 (vor Gruendung Community) genutzt werden, die Bestandteil der Auslieferung ist und auch unter
 * http://www.faktorzehn.org/fips:lizenz eingesehen werden kann.
 * 
 * Mitwirkende: Faktor Zehn AG - initial API and implementation - http://www.faktorzehn.de
 *******************************************************************************/

package org.faktorips.devtools.core.model.type;

import org.faktorips.devtools.core.internal.model.productcmpttype.ProductCmptTypeAssociation;
import org.faktorips.devtools.core.model.ipsobject.IDescribedElement;
import org.faktorips.devtools.core.model.ipsobject.ILabeledElement;
import org.faktorips.devtools.core.model.productcmpt.IProductCmpt;
import org.faktorips.devtools.core.model.productcmpt.IProductCmptLink;
import org.faktorips.devtools.core.model.productcmpt.IPropertyValue;

/**
 * An abstraction of properties (defined by a type) that are configured by product components. Such
 * properties have exactly one value-instance ({@link IPropertyValue}).
 * <p>
 * As of yet, not all aspects that are configured by a product component are
 * {@link IProductCmptProperty}s. e.g. {@link ProductCmptTypeAssociation} as there are multiple
 * {@link IProductCmptLink} instances for a single association.
 * 
 * @author Jan Ortmann
 * @author Stefan Widmaier
 */
public interface IProductCmptProperty extends IDescribedElement, ILabeledElement {

    /**
     * Returns the type of the property. The different types of product definition properties are
     * defined by {@link ProductCmptPropertyType}. The type represents the different elements in the
     * model that implement this interface. Each type corresponds to one element.
     * <p>
     * See {@link ProductCmptPropertyType} for safe casts to a specific model element.
     * 
     * @see ProductCmptPropertyType
     */
    public ProductCmptPropertyType getProdDefPropertyType();

    /**
     * Returns the name of the property. That name is unique in the corresponding
     * {@link IProductCmpt}.
     */
    public String getPropertyName();
}
