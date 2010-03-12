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

package org.faktorips.devtools.core.internal.model.productcmpt.treestructure;

import org.faktorips.devtools.core.model.ipsobject.IIpsObject;
import org.faktorips.devtools.core.model.ipsobject.IIpsObjectPartContainer;
import org.faktorips.devtools.core.model.productcmpt.treestructure.CycleInProductStructureException;
import org.faktorips.devtools.core.model.productcmpt.treestructure.IProductCmptTreeStructure;
import org.faktorips.devtools.core.model.productcmpt.treestructure.IProductCmptTypeAssociationReference;
import org.faktorips.devtools.core.model.productcmpttype.IProductCmptTypeAssociation;

/**
 * A reference to a {@link IProductCmptTypeAssociation}. Used by <code>ProductCmptStructure</code> .
 * 
 * @author Thorsten Guenther
 */
public class ProductCmptTypeAssociationReference extends ProductCmptStructureReference implements
        IProductCmptTypeAssociationReference {

    private IProductCmptTypeAssociation association;

    /**
     * @param structure
     * @param parent
     * @throws CycleInProductStructureException
     */
    public ProductCmptTypeAssociationReference(IProductCmptTreeStructure structure,
            ProductCmptStructureReference parent, IProductCmptTypeAssociation association)
            throws CycleInProductStructureException {
        super(structure, parent);
        this.association = association;
    }

    /**
     * {@inheritDoc}
     */
    public IProductCmptTypeAssociation getAssociation() {
        return association;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected IIpsObjectPartContainer getWrapped() {
        return association;
    }

    /**
     * {@inheritDoc}
     */
    public IIpsObject getWrappedIpsObject() {
        if (association == null) {
            return null;
        }
        return association.getIpsObject();
    }
}
