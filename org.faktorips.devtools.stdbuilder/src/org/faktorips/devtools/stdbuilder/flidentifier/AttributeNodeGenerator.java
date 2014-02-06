/*******************************************************************************
 * Copyright (c) Faktor Zehn AG. <http://www.faktorzehn.org>
 * 
 * This source code is available under the terms of the AGPL Affero General Public License version
 * 3.
 * 
 * Please see LICENSE.txt for full license terms, including the additional permissions and
 * restrictions as well as the possibility of alternative license terms.
 *******************************************************************************/

package org.faktorips.devtools.stdbuilder.flidentifier;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.CoreException;
import org.faktorips.codegen.JavaCodeFragment;
import org.faktorips.datatype.Datatype;
import org.faktorips.datatype.ListOfTypeDatatype;
import org.faktorips.datatype.ValueDatatype;
import org.faktorips.devtools.core.builder.flidentifier.IdentifierNodeGeneratorFactory;
import org.faktorips.devtools.core.builder.flidentifier.ast.AttributeNode;
import org.faktorips.devtools.core.builder.flidentifier.ast.IdentifierNode;
import org.faktorips.devtools.core.exception.CoreRuntimeException;
import org.faktorips.devtools.core.model.pctype.IPolicyCmptType;
import org.faktorips.devtools.core.model.pctype.IPolicyCmptTypeAttribute;
import org.faktorips.devtools.core.model.productcmpttype.IProductCmptTypeAttribute;
import org.faktorips.devtools.core.model.type.IAttribute;
import org.faktorips.devtools.stdbuilder.GeneratorRuntimeException;
import org.faktorips.devtools.stdbuilder.StandardBuilderSet;
import org.faktorips.devtools.stdbuilder.xpand.policycmpt.model.XPolicyAttribute;
import org.faktorips.devtools.stdbuilder.xpand.policycmpt.model.XPolicyCmptClass;
import org.faktorips.devtools.stdbuilder.xpand.productcmpt.model.XProductAttribute;
import org.faktorips.devtools.stdbuilder.xpand.productcmpt.model.XProductCmptClass;
import org.faktorips.fl.CompilationResult;
import org.faktorips.fl.CompilationResultImpl;

/**
 * JavaGenerator for an {@link AttributeNode}. Supports both policy- and product-attributes.
 * Examples in the formula language: "policy.premium" (gets the value of attribute "premium" from
 * policy) and "policy.paymentMode" (gets the value "paymentMode" from the configuring product
 * component).
 * 
 * @author widmaier
 * @since 3.11.0
 */
public class AttributeNodeGenerator extends StdBuilderIdentifierNodeGenerator {

    public AttributeNodeGenerator(IdentifierNodeGeneratorFactory<JavaCodeFragment> nodeBuilderFactory,
            StandardBuilderSet builderSet) {
        super(nodeBuilderFactory, builderSet);
    }

    @Override
    protected CompilationResult<JavaCodeFragment> getCompilationResultForCurrentNode(IdentifierNode identifierNode,
            CompilationResult<JavaCodeFragment> contextCompilationResult) {
        AttributeNode node = (AttributeNode)identifierNode;
        if (isListOfTypeDatatype(contextCompilationResult)) {
            return createListCompilationResult(node, contextCompilationResult);
        } else {
            return createNormalCompilationResult(node, contextCompilationResult);
        }
    }

    private CompilationResult<JavaCodeFragment> createNormalCompilationResult(final AttributeNode node,
            CompilationResult<JavaCodeFragment> contextCompilationResult) {
        Datatype contextDatatype = contextCompilationResult.getDatatype();
        String attributGetterName = getAttributeGetterName(node.getAttribute(), node.isDefaultValueAccess(),
                contextDatatype);
        JavaCodeFragment attributeFragment = createCodeFragment(attributGetterName,
                contextCompilationResult.getCodeFragment());
        return new CompilationResultImpl(attributeFragment, node.getDatatype());
    }

    private boolean isListOfTypeDatatype(CompilationResult<JavaCodeFragment> compilationResult) {
        return compilationResult.getDatatype() instanceof ListOfTypeDatatype;
    }

    private CompilationResult<JavaCodeFragment> createListCompilationResult(AttributeNode node,
            CompilationResult<JavaCodeFragment> contextCompilationResult) {

        if (!node.isListOfTypeDatatype()) {
            throw new GeneratorRuntimeException("The datatype of this node is not a ListOfTypeDatatype: " + node); //$NON-NLS-1$
        }

        Datatype conextDatatype = getBasicDatatype(contextCompilationResult);
        IAttribute attribute = node.getAttribute();
        String attributeDatatypeClassName = getDatatypeClassname(attribute);
        String parameterAttributGetterName = getAttributeGetterName(attribute, node.isDefaultValueAccess(),
                conextDatatype);

        JavaCodeFragment getTargetCode = new JavaCodeFragment("new "); //$NON-NLS-1$
        getTargetCode.appendClassName(org.faktorips.runtime.formula.FormulaEvaluatorUtil.AttributeAccessorHelper.class);
        getTargetCode.append("<"); //$NON-NLS-1$
        getTargetCode.appendClassName(getJavaClassName(conextDatatype));
        getTargetCode.append(", "); //$NON-NLS-1$
        getTargetCode.appendClassName(attributeDatatypeClassName);
        getTargetCode.append(">(){\n@Override protected "); //$NON-NLS-1$
        getTargetCode.appendClassName(attributeDatatypeClassName);
        getTargetCode.append(" getValueInternal("); //$NON-NLS-1$
        getTargetCode.appendClassName(getJavaClassName(conextDatatype));
        getTargetCode.append(" sourceObject){return sourceObject." + parameterAttributGetterName); //$NON-NLS-1$
        getTargetCode.append("();}}.getAttributeValues("); //$NON-NLS-1$
        getTargetCode.append(contextCompilationResult.getCodeFragment());
        getTargetCode.append(")"); //$NON-NLS-1$

        return new CompilationResultImpl(getTargetCode, node.getDatatype());
    }

    private String getDatatypeClassname(IAttribute attribute) {
        try {
            ValueDatatype datatype = attribute.findDatatype(getIpsProject());
            if (datatype.isPrimitive()) {
                datatype = datatype.getWrapperType();
            }
            return getJavaClassName(datatype);
        } catch (CoreException e) {
            throw new CoreRuntimeException(e);
        }
    }

    private Datatype getBasicDatatype(CompilationResult<JavaCodeFragment> contextCompilationResult) {
        ListOfTypeDatatype contextListofTypeDatatype = (ListOfTypeDatatype)contextCompilationResult.getDatatype();
        Datatype conextDatatype = contextListofTypeDatatype.getBasicDatatype();
        return conextDatatype;
    }

    private JavaCodeFragment createCodeFragment(final String parameterAttributGetterName,
            JavaCodeFragment contextCodeFragment) {
        JavaCodeFragment javaCodeFragment = new JavaCodeFragment();
        javaCodeFragment.append(contextCodeFragment);
        javaCodeFragment.append('.' + parameterAttributGetterName + "()"); //$NON-NLS-1$
        return javaCodeFragment;
    }

    protected String getAttributeGetterName(IAttribute attribute, boolean isDefaultValueAccess, Datatype contextDatatype) {
        String parameterAttributGetterName = isDefaultValueAccess ? getParameterAttributDefaultValueGetterName(attribute)
                : getParameterAttributGetterName(attribute, contextDatatype);
        return parameterAttributGetterName;
    }

    private String getParameterAttributGetterName(IAttribute attribute, Datatype contextDatatype) {
        if (attribute instanceof IPolicyCmptTypeAttribute) {
            return getPolicyAttributeGetterName((IPolicyCmptTypeAttribute)attribute);
        } else if (attribute instanceof IProductCmptTypeAttribute) {
            return getProductAttributeAccessCode((IProductCmptTypeAttribute)attribute, contextDatatype);
        }
        throw new GeneratorRuntimeException("This type of attribute is not supported: " + attribute.getClass()); //$NON-NLS-1$
    }

    private String getPolicyAttributeGetterName(IPolicyCmptTypeAttribute attribute) {
        XPolicyAttribute xPolicyAttribute = getModelNode(attribute, XPolicyAttribute.class);
        return xPolicyAttribute.getMethodNameGetter();
    }

    private String getProductAttributeAccessCode(IProductCmptTypeAttribute attribute, Datatype contextDatatype) {
        StringBuffer contextAccessCode = new StringBuffer();
        contextAccessCode.append(getProductCmptContextCode(attribute, contextDatatype));
        contextAccessCode.append(getProductAttributeGetterName(attribute));
        return contextAccessCode.toString();
    }

    private String getProductCmptContextCode(IProductCmptTypeAttribute attribute, Datatype contextDatatype) {
        if (contextDatatype instanceof IPolicyCmptType) {
            return getProductCmptOrGenerationGetterCode(attribute, (IPolicyCmptType)contextDatatype);
        } else {
            return getProductCmptGetterCodeIfRequired(attribute);
        }
    }

    /**
     * Based on a policy component, returns the code for getting the configuring product component
     * generation if the requested attribute is changing over time or the product component if the
     * attribute is static.
     */
    private String getProductCmptOrGenerationGetterCode(IProductCmptTypeAttribute attribute, IPolicyCmptType policyType) {
        XPolicyCmptClass xPolicyCmptClass = getModelNode(policyType, XPolicyCmptClass.class);
        if (isChangingOverTime(attribute)) {
            return xPolicyCmptClass.getMethodNameGetProductCmptGeneration() + "().";
        } else {
            return xPolicyCmptClass.getMethodNameGetProductCmpt() + "().";
        }
    }

    private boolean isChangingOverTime(IProductCmptTypeAttribute attribute) {
        XProductAttribute xProductAttribute = getModelNode(attribute, XProductAttribute.class);
        return xProductAttribute.isChangingOverTime();
    }

    /**
     * Based on a product component generation, returns an empty string if the requested attribute
     * is changing over time or the code for getting the product component if the attribute is
     * static.
     */
    private String getProductCmptGetterCodeIfRequired(IProductCmptTypeAttribute attribute) {
        if (!isChangingOverTime(attribute)) {
            XProductCmptClass xProductCmptClass = getModelNode(attribute.getType(), XProductCmptClass.class);
            return xProductCmptClass.getMethodNameGetProductCmpt() + "().";
        } else {
            return StringUtils.EMPTY;
        }
    }

    private String getProductAttributeGetterName(IProductCmptTypeAttribute attribute) {
        XProductAttribute xProductAttribute = getModelNode(attribute, XProductAttribute.class);
        return xProductAttribute.getMethodNameGetter();
    }

    private String getParameterAttributDefaultValueGetterName(IAttribute attribute) {
        XPolicyCmptClass xPolicyCmptClass = getModelNode(attribute.getType(), XPolicyCmptClass.class);
        XPolicyAttribute xPolicyAttribute = getModelNode(attribute, XPolicyAttribute.class);
        return xPolicyCmptClass.getMethodNameGetProductCmptGeneration() + "()." //$NON-NLS-1$
                + xPolicyAttribute.getMethodNameGetDefaultValue();
    }
}
