/*******************************************************************************
 * Copyright (c) Faktor Zehn AG. <http://www.faktorzehn.org>
 * 
 * This source code is available under the terms of the AGPL Affero General Public License version
 * 3.
 * 
 * Please see LICENSE.txt for full license terms, including the additional permissions and
 * restrictions as well as the possibility of alternative license terms.
 *******************************************************************************/

package org.faktorips.devtools.stdbuilder.xpand.productcmpt.model;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.CoreException;
import org.faktorips.devtools.core.builder.naming.BuilderAspect;
import org.faktorips.devtools.core.exception.CoreRuntimeException;
import org.faktorips.devtools.core.model.productcmpttype.IProductCmptType;
import org.faktorips.devtools.core.model.productcmpttype.IProductCmptTypeAssociation;
import org.faktorips.devtools.core.model.type.IType;
import org.faktorips.devtools.stdbuilder.AnnotatedJavaElementType;
import org.faktorips.devtools.stdbuilder.xpand.GeneratorModelContext;
import org.faktorips.devtools.stdbuilder.xpand.model.ModelService;
import org.faktorips.devtools.stdbuilder.xpand.model.XAssociation;
import org.faktorips.devtools.stdbuilder.xpand.model.XClass;
import org.faktorips.devtools.stdbuilder.xpand.policycmpt.model.XPolicyAssociation;
import org.faktorips.util.StringUtil;

public class XProductAssociation extends XAssociation {

    public XProductAssociation(IProductCmptTypeAssociation association, GeneratorModelContext context,
            ModelService modelService) {
        super(association, context, modelService);
    }

    @Override
    public IProductCmptTypeAssociation getAssociation() {
        return (IProductCmptTypeAssociation)super.getAssociation();
    }

    @Override
    protected IProductCmptType getTargetType() {
        return (IProductCmptType)super.getTargetType();
    }

    public String getTargetClassGenerationName() {
        IType target = getTargetType();
        XClass modelNode = getModelNode(target, XProductCmptGenerationClass.class);
        return modelNode.getSimpleName(BuilderAspect.INTERFACE);
    }

    public String getMethodNameGetTargetGeneration() {
        IType target = getTargetType();
        XProductCmptGenerationClass modelNode = getModelNode(target, XProductCmptGenerationClass.class);
        return modelNode.getMethodNameGetProductComponentGeneration();
    }

    public String getMethodNameGetLinksFor() {
        return getMethodNameGetLinksFor(isOneToMany());
    }

    public String getMethodNameGetLinkFor() {
        return getMethodNameGetLinksFor(false);
    }

    private String getMethodNameGetLinksFor(boolean plural) {
        return getJavaNamingConvention().getMultiValueGetterMethodName(
                "Link" + (plural ? "s" : "") + "For" + StringUtils.capitalize(getName(plural)));
    }

    public String getMethodNameGetCardinalityFor() {
        String matchingSingularName;
        matchingSingularName = StringUtils.capitalize(getNameOfMatchingAssociation());
        return getJavaNamingConvention().getGetterMethodName("CardinalityFor" + matchingSingularName);
    }

    public String getNameOfMatchingAssociation() {
        return getAssociation().findMatchingPolicyCmptTypeAssociation(getIpsProject()).getTargetRoleSingular();
    }

    public String getConstantNameXmlTag() {
        return "XML_TAG_" + StringUtil.camelCaseToUnderscore(getName(isOneToMany())).toUpperCase();
    }

    public boolean hasMatchingAssociation() {
        try {
            return getAssociation().constrainsPolicyCmptTypeAssociation(getIpsProject());
        } catch (CoreException e) {
            throw new CoreRuntimeException(e);
        }
    }

    /**
     * Returns the key used to localize java doc. The parameter gives the prefix for the key
     * specifying the scope of generated code. This method adds "ONE" or "MANY" depending on the
     * kind of the association to differ between one to many and one to one associations.
     * 
     * @param prefix The prefix defining the scope of the generated code, for example
     *            "METHOD_GET_CMPT"
     * @return The key used to localize the java doc for example "METHOD_GET_CMPT_ONE" or
     *         "METHOD_GET_CMPT_MANY"
     */
    public String getJavadocKey(String prefix) {
        return prefix + (isOneToMany() ? "_MANY" : "_ONE");
    }

    protected XProductAssociation getSuperAssociationWithSameName() {
        IProductCmptTypeAssociation superAssociationWithSameName = (IProductCmptTypeAssociation)getAssociation()
                .findSuperAssociationWithSameName(getIpsProject());
        if (superAssociationWithSameName != null) {
            return getModelNode(superAssociationWithSameName, XProductAssociation.class);
        } else {
            return null;
        }
    }

    public XProductCmptClass getTargetProductCmptClass() {
        IProductCmptType proCmptType = getAssociation().findTargetProductCmptType(getIpsProject());
        return getModelNode(proCmptType, XProductCmptClass.class);
    }

    public boolean isGenerateNewChildMethods() {
        return isMasterToDetail() && !getTargetProductCmptClass().isAbstract() && !isDerivedUnion();
    }

    /**
     * This method checks if any association of the super class has the same name as the given one.
     * 
     * @return true if '@Override is needed for the association, false is not.
     */
    public boolean isNeedOverrideForConstrainNewChildMethod() {
        if (isConstrain()) {
            if (getSuperAssociationWithSameName().isGenerateNewChildMethods()) {
                return true;
            } else {
                return getSuperAssociationWithSameName().isNeedOverrideForConstrainNewChildMethod();
            }
        } else {
            return false;
        }
    }

    /**
     * Returns true if the changing over time flag of association target product component type is
     * enabled.
     */
    public boolean isGenerateGenerationAccessMethods() {
        return getTargetType().isChangingOverTime();
    }

    @Override
    protected Class<? extends XAssociation> getMatchingClass() {
        return XPolicyAssociation.class;
    }

    @Override
    public AnnotatedJavaElementType getAnnotatedJavaElementTypeForGetter() {
        return AnnotatedJavaElementType.PRODUCT_CMPT_DECL_CLASS_ASSOCIATION_GETTER;
    }
}
