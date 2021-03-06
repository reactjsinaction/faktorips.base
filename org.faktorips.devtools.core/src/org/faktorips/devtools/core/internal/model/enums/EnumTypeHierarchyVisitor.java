/*******************************************************************************
 * Copyright (c) Faktor Zehn AG. <http://www.faktorzehn.org>
 * 
 * This source code is available under the terms of the AGPL Affero General Public License version
 * 3.
 * 
 * Please see LICENSE.txt for full license terms, including the additional permissions and
 * restrictions as well as the possibility of alternative license terms.
 *******************************************************************************/

package org.faktorips.devtools.core.internal.model.enums;

import org.faktorips.devtools.core.model.HierarchyVisitor;
import org.faktorips.devtools.core.model.enums.IEnumType;
import org.faktorips.devtools.core.model.ipsproject.IIpsProject;

/**
 * A specialization of <tt>HierarchyVisitor</tt> for <tt>IEnumType</tt>.
 * 
 * @author Peter Kuntz
 */
public abstract class EnumTypeHierarchyVisitor extends HierarchyVisitor<IEnumType> {

    /**
     * Creates a new <tt>EnumTypeHierachyVisitor</tt>.
     * 
     * @param ipsProject The IPS project which IPS object path is used to search for
     *            <tt>IEnumType</tt>s.
     */
    public EnumTypeHierarchyVisitor(IIpsProject ipsProject) {
        super(ipsProject);
    }

    @Override
    protected IEnumType findSupertype(IEnumType currentType, IIpsProject ipsProject) {
        return currentType.findSuperEnumType(ipsProject);
    }

}
