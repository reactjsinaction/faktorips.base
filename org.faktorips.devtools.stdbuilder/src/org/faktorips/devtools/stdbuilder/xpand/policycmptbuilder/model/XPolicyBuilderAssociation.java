/*******************************************************************************
 * Copyright (c) Faktor Zehn AG. <http://www.faktorzehn.org>
 * 
 * This source code is available under the terms of the AGPL Affero General Public License version
 * 3.
 * 
 * Please see LICENSE.txt for full license terms, including the additional permissions and
 * restrictions as well as the possibility of alternative license terms.
 *******************************************************************************/
package org.faktorips.devtools.stdbuilder.xpand.policycmptbuilder.model;

import org.apache.commons.lang.StringUtils;
import org.faktorips.devtools.core.model.pctype.IPolicyCmptTypeAssociation;
import org.faktorips.devtools.stdbuilder.xpand.GeneratorModelContext;
import org.faktorips.devtools.stdbuilder.xpand.model.ModelService;
import org.faktorips.devtools.stdbuilder.xpand.policycmpt.model.XPolicyAssociation;

public class XPolicyBuilderAssociation extends XPolicyAssociation {

    public XPolicyBuilderAssociation(IPolicyCmptTypeAssociation association, GeneratorModelContext context,
            ModelService modelService) {
        super(association, context, modelService);
    }

    public XPolicyBuilder getTargetBuilderClass() {
        return getModelNode(getTargetPolicyCmptClass().getType(), XPolicyBuilder.class);
    }

    /**
     * Checks if override is needed for the association setter. The method in the super class
     * excludes associations with isMasterToDetail. Associations with a non-abstract target are
     * included here.
     * 
     * {@inheritDoc}
     */
    @Override
    public boolean isNeedOverrideForConstrainNewChildMethod() {
        if (isConstrain()) {
            XPolicyAssociation superAsso = getSuperAssociationWithSameName();
            if ((superAsso.isTypeAssociation() && !superAsso.isAbstractTarget())
                    || getSuperAssociationWithSameName().isGenerateNewChildMethods()) {
                return true;
            } else {
                return getSuperAssociationWithSameName().isNeedOverrideForConstrainNewChildMethod();
            }
        } else {
            return false;
        }
    }

    public boolean isOverwrittenTargetNotAbstract() {
        return !getSuperAssociationWithSameName().isAbstractTarget();
    }

    public boolean isOverwrittenNotAbstractConfigured() {
        return isOverwrittenTargetNotAbstract()
                && getSuperAssociationWithSameName().getTargetPolicyCmptClass().isConfigured();
    }

    public String getMethodName() {
        return StringUtils.uncapitalize(getName());
    }

    // //For creatDefault
    //
    // public boolean isTargetProductChangingOverTime() throws CoreException {
    // IProductCmptTypeAssociation findMatchingProductCmptTypeAssociation = getAssociation()
    // .findMatchingProductCmptTypeAssociation(getIpsProject());
    // return (findMatchingProductCmptTypeAssociation != null)
    // && findMatchingProductCmptTypeAssociation.isChangingOverTime();
    // }

    // public ProductCmptTypeAssociation getMatchingAssociation() throws CoreException {
    // return
    // (ProductCmptTypeAssociation)getAssociation().findMatchingProductCmptTypeAssociation(getIpsProject());
    // }

    // public String getMethodNameCreateDefaultAssociation() {
    // return "createDefault" + StringUtils.capitalize(getName());
    // }
}
