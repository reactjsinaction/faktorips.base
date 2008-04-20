/***************************************************************************************************
 * Copyright (c) 2005,2006 Faktor Zehn GmbH und andere.
 * 
 * Alle Rechte vorbehalten.
 * 
 * Dieses Programm und alle mitgelieferten Sachen (Dokumentationen, Beispiele, Konfigurationen,
 * etc.) dürfen nur unter den Bedingungen der Faktor-Zehn-Community Lizenzvereinbarung – Version 0.1
 * (vor Gründung Community) genutzt werden, die Bestandteil der Auslieferung ist und auch unter
 * http://www.faktorips.org/legal/cl-v01.html eingesehen werden kann.
 * 
 * Mitwirkende: Faktor Zehn GmbH - initial API and implementation
 * 
 **************************************************************************************************/

package org.faktorips.devtools.stdbuilder.policycmpttype.attribute;

import org.eclipse.core.runtime.CoreException;
import org.faktorips.codegen.DatatypeHelper;
import org.faktorips.codegen.JavaCodeFragment;
import org.faktorips.codegen.JavaCodeFragmentBuilder;
import org.faktorips.datatype.Datatype;
import org.faktorips.datatype.ValueDatatype;
import org.faktorips.devtools.core.builder.BuilderHelper;
import org.faktorips.devtools.core.builder.JavaSourceFileBuilder;
import org.faktorips.devtools.core.model.ipsproject.IIpsProject;
import org.faktorips.devtools.core.model.pctype.AttributeType;
import org.faktorips.devtools.core.model.pctype.IPolicyCmptTypeAttribute;
import org.faktorips.devtools.core.model.productcmpttype.IProductCmptTypeMethod;
import org.faktorips.devtools.core.model.type.IParameter;
import org.faktorips.devtools.stdbuilder.StdBuilderHelper;
import org.faktorips.devtools.stdbuilder.policycmpttype.GenPolicyCmptType;
import org.faktorips.util.ArgumentCheck;
import org.faktorips.util.LocalizedStringsSet;

/**
 * Code generator for a derived attribute.
 * 
 * @author Jan Ortmann
 */
public class GenDerivedAttribute extends GenAttribute {

    public GenDerivedAttribute(GenPolicyCmptType genPolicyCmptType, IPolicyCmptTypeAttribute a,
            LocalizedStringsSet stringsSet) throws CoreException {

        super(genPolicyCmptType, a, stringsSet);
        ArgumentCheck.isTrue(a.isDerived());
    }

    /**
     * {@inheritDoc}
     */
    protected void generateConstants(JavaCodeFragmentBuilder builder, IIpsProject ipsProject, boolean generatesInterface) throws CoreException {
        if (generatesInterface) {
            if (!isOverwritten()) {
                generateAttributeNameConstant(builder);
            }
        } else {
            if (isNotPublished()) {
                generateAttributeNameConstant(builder);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void generateMemberVariables(JavaCodeFragmentBuilder builder, IIpsProject ipsProject, boolean generatesInterface)
            throws CoreException {
        if (!generatesInterface) {
            if (getPolicyCmptTypeAttribute().getAttributeType() == AttributeType.DERIVED_BY_EXPLICIT_METHOD_CALL
                    && !isOverwritten()) {
                generateField(builder);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void generateMethods(JavaCodeFragmentBuilder builder, IIpsProject ipsProject, boolean generatesInterface) throws CoreException {
        if (generatesInterface) {
            if (!isOverwritten()) {
                generateGetterInterface(builder);
            }
        } else {
            if (getPolicyCmptTypeAttribute().getAttributeType() == AttributeType.DERIVED_ON_THE_FLY) {
                generateGetterImplementationForOnTheFlyComputation(builder, ipsProject);
            } else {
                if (!isOverwritten()) {
                    generateGetterImplementation(builder);
                }
            }
        }
    }

    private void generateGetterImplementationForOnTheFlyComputation(JavaCodeFragmentBuilder builder, IIpsProject ipsProject)
            throws CoreException {
        builder.javaDoc(getJavaDocCommentForOverriddenMethod(), JavaSourceFileBuilder.ANNOTATION_GENERATED);
        generateGetterSignature(builder);
        builder.openBracket();

        IProductCmptTypeMethod formulaSignature = getPolicyCmptTypeAttribute().findComputationMethod(ipsProject);
        if (!getPolicyCmptTypeAttribute().isProductRelevant() || formulaSignature == null
                || formulaSignature.validate(ipsProject).containsErrorMsg()) {
            builder.append("return ");
            builder.append(datatypeHelper.newInstance(attribute.getDefaultValue()));
            builder.appendln(";");
        } else {
            IParameter[] parameters = formulaSignature.getParameters();
            boolean resolveTypesToPublishedInterface = formulaSignature.getModifier().isPublished();
            String[] paramNames = BuilderHelper.extractParameterNames(parameters);
            String[] paramTypes = StdBuilderHelper.transformParameterTypesToJavaClassNames(parameters,
                    resolveTypesToPublishedInterface, getGenPolicyCmptType().getBuilderSet(), ipsProject);

            builder.appendln("// TODO Belegung der Berechnungsparameter implementieren");
            JavaCodeFragment paramFragment = new JavaCodeFragment();
            paramFragment.append('(');
            for (int i = 0; i < paramNames.length; i++) {
                builder.appendClassName(paramTypes[i]);
                builder.append(' ');
                builder.append(paramNames[i]);
                builder.append(" = ");
                Datatype paramDataype = ipsProject.findDatatype(parameters[i].getDatatype());
                DatatypeHelper helper = ipsProject.getDatatypeHelper(paramDataype);
                if (paramDataype.isPrimitive()) {
                    builder.append(((ValueDatatype)paramDataype).getDefaultValue());
                } else {
                    if (helper != null) {
                        JavaCodeFragment nullExpressionFragment = helper.nullExpression();
                        builder.append(nullExpressionFragment);
                    } else {
                        builder.append("null");
                    }
                }
                builder.appendln(";");
                if (i > 0) {
                    paramFragment.append(", ");
                }
                paramFragment.append(paramNames[i]);
            }
            paramFragment.append(")");
            builder.append(" return ((");
            builder.appendClassName(getGenPolicyCmptType().getBuilderSet().getGenerator(getProductCmptType(ipsProject)).getQualifiedName(false));
            builder.append(')');
            builder.append(getGenPolicyCmptType().getBuilderSet().getGenerator(getProductCmptType(ipsProject))
                    .getMethodNameGetProductCmptGeneration());
            builder.append("()).");
            builder.append(formulaSignature.getName());
            builder.append(paramFragment);
            builder.append(";");
        }
        builder.closeBracket();
    }

}
