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

package org.faktorips.devtools.stdbuilder.policycmpttype;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.CoreException;
import org.faktorips.codegen.DatatypeHelper;
import org.faktorips.codegen.JavaCodeFragment;
import org.faktorips.codegen.JavaCodeFragmentBuilder;
import org.faktorips.datatype.Datatype;
import org.faktorips.datatype.EnumDatatype;
import org.faktorips.datatype.ValueDatatype;
import org.faktorips.devtools.core.builder.BuilderHelper;
import org.faktorips.devtools.core.builder.JavaSourceFileBuilder;
import org.faktorips.devtools.core.builder.MessageFragment;
import org.faktorips.devtools.core.model.IIpsArtefactBuilderSet;
import org.faktorips.devtools.core.model.IIpsProject;
import org.faktorips.devtools.core.model.IIpsSrcFile;
import org.faktorips.devtools.core.model.IpsObjectType;
import org.faktorips.devtools.core.model.ValueSetType;
import org.faktorips.devtools.core.model.pctype.AssociationType;
import org.faktorips.devtools.core.model.pctype.AttributeType;
import org.faktorips.devtools.core.model.pctype.IPolicyCmptType;
import org.faktorips.devtools.core.model.pctype.IPolicyCmptTypeAssociation;
import org.faktorips.devtools.core.model.pctype.IPolicyCmptTypeAttribute;
import org.faktorips.devtools.core.model.pctype.IValidationRule;
import org.faktorips.devtools.core.model.productcmpttype.IProductCmptTypeAttribute;
import org.faktorips.devtools.core.model.productcmpttype.IProductCmptTypeMethod;
import org.faktorips.devtools.core.model.type.IMethod;
import org.faktorips.devtools.core.model.type.IParameter;
import org.faktorips.devtools.stdbuilder.StdBuilderHelper;
import org.faktorips.devtools.stdbuilder.productcmpttype.ProductCmptGenImplClassBuilder;
import org.faktorips.devtools.stdbuilder.productcmpttype.ProductCmptGenInterfaceBuilder;
import org.faktorips.devtools.stdbuilder.productcmpttype.ProductCmptInterfaceBuilder;
import org.faktorips.runtime.DefaultUnresolvedReference;
import org.faktorips.runtime.IConfigurableModelObject;
import org.faktorips.runtime.IModelObject;
import org.faktorips.runtime.IModelObjectChangedEvent;
import org.faktorips.runtime.IUnresolvedReference;
import org.faktorips.runtime.Message;
import org.faktorips.runtime.MessageList;
import org.faktorips.runtime.MsgReplacementParameter;
import org.faktorips.runtime.ObjectProperty;
import org.faktorips.runtime.internal.AbstractConfigurableModelObject;
import org.faktorips.runtime.internal.AbstractModelObject;
import org.faktorips.runtime.internal.DependantObject;
import org.faktorips.runtime.internal.MethodNames;
import org.faktorips.runtime.internal.ModelObjectChangedEvent;
import org.faktorips.util.LocalizedStringsSet;
import org.faktorips.util.StringUtil;
import org.w3c.dom.Element;

public class PolicyCmptImplClassBuilder extends BasePolicyCmptTypeBuilder {

    private final static String FIELD_PARENT_MODEL_OBJECT = "parentModelObject";
    
    private PolicyCmptInterfaceBuilder interfaceBuilder;
    private ProductCmptInterfaceBuilder productCmptInterfaceBuilder;
    private ProductCmptGenInterfaceBuilder productCmptGenInterfaceBuilder;
    private ProductCmptGenImplClassBuilder productCmptGenImplBuilder;

    public PolicyCmptImplClassBuilder(IIpsArtefactBuilderSet builderSet, String kindId, boolean changeListenerSupportActive) {
        super(builderSet, kindId, new LocalizedStringsSet(PolicyCmptImplClassBuilder.class), changeListenerSupportActive);
        setMergeEnabled(true);
    }

    public void setInterfaceBuilder(PolicyCmptInterfaceBuilder policyCmptTypeInterfaceBuilder) {
        this.interfaceBuilder = policyCmptTypeInterfaceBuilder;
    }

    public PolicyCmptInterfaceBuilder getInterfaceBuilder() {
        return interfaceBuilder;
    }

    public void setProductCmptInterfaceBuilder(ProductCmptInterfaceBuilder productCmptInterfaceBuilder) {
        this.productCmptInterfaceBuilder = productCmptInterfaceBuilder;
    }
    
    public void setProductCmptGenInterfaceBuilder(ProductCmptGenInterfaceBuilder builder) {
        this.productCmptGenInterfaceBuilder = builder;
    }
    
    public void setProductCmptGenImplBuilder(ProductCmptGenImplClassBuilder builder) {
        this.productCmptGenImplBuilder = builder;
    }

    public IPolicyCmptType getPolicyCmptType() {
        return (IPolicyCmptType)getIpsObject();
    }
    
    /**
     * {@inheritDoc}
     */
    public boolean isBuilderFor(IIpsSrcFile ipsSrcFile) {
        return IpsObjectType.POLICY_CMPT_TYPE.equals(ipsSrcFile.getIpsObjectType());
    }

    /**
     * {@inheritDoc}
     */
    protected boolean generatesInterface() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    protected String getSuperclass() throws CoreException {
        IPolicyCmptType supertype = getPcType().findSupertype();
        if (supertype != null) {
            return getQualifiedClassName(supertype);
        }
        if (getPcType().isConfigurableByProductCmptType()) {
            return AbstractConfigurableModelObject.class.getName(); 
        } else {
            return AbstractModelObject.class.getName();
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getUnqualifiedClassName(IIpsSrcFile ipsSrcFile) throws CoreException {
        String name = StringUtil.getFilenameWithoutExtension(ipsSrcFile.getName());
        return getJavaNamingConvention().getImplementationClassName(StringUtils.capitalise(name));
    }

    /**
     * {@inheritDoc}
     */
    protected String[] getExtendedInterfaces() throws CoreException {
        String publishedInterface = interfaceBuilder.getQualifiedClassName(getPcType());
        if (isFirstDependantTypeInHierarchy(getPcType())) {
            return new String[]{publishedInterface, DependantObject.class.getName()};
        }
        return new String[] { publishedInterface };
    }

    /**
     * {@inheritDoc}
     */
    protected void generateTypeJavadoc(JavaCodeFragmentBuilder builder) {
        builder.javaDoc(null, ANNOTATION_GENERATED);
    }

    /**
     * {@inheritDoc}
     */
    protected void generateConstructors(JavaCodeFragmentBuilder builder) throws CoreException {
        generateConstructorDefault(builder);
        if (getProductCmptType()!=null) {
            generateConstructorWithProductCmptArg(builder);
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void generateOther(JavaCodeFragmentBuilder memberVarsBuilder,
            JavaCodeFragmentBuilder methodsBuilder) throws CoreException {
        
        IPolicyCmptType type = getPcType();
        generateMethodInitialize(methodsBuilder);
        if (getProductCmptType()!=null) {
            if(hasValidProductCmptTypeName()){
                generateMethodGetProductCmpt(methodsBuilder);
                generateMethodGetProductCmptGeneration(methodsBuilder);
                generateMethodSetProductCmpt(methodsBuilder);
            }
            generateMethodEffectiveFromHasChanged(methodsBuilder);
        }
        if (type.isAggregateRoot()) {
            if (type.isConfigurableByProductCmptType()) {
                generateMethodGetEffectiveFromAsCalendarForAggregateRoot(methodsBuilder);
            }
        } else if (isFirstDependantTypeInHierarchy(type)){
            generateCodeForDependantObjectBaseClass(memberVarsBuilder, methodsBuilder);
        }
        generateMethodRemoveChildModelObjectInternal(methodsBuilder);
        buildValidation(methodsBuilder);
        generateMethodInitPropertiesFromXml(methodsBuilder);
        generateMethodCreateChildFromXml(methodsBuilder);
        boolean hasAssociation = false;
        IPolicyCmptTypeAssociation[] relations = getPcType().getPolicyCmptTypeAssociations();
        for (int i = 0; i < relations.length; i++) {
            if (relations[i].isAssoziation()) {
                hasAssociation = true;
                break;
            }
        } 
        if (hasAssociation) {
            generateMethodCreateUnresolvedReference(methodsBuilder);
        }
    }
    
    protected void generateMethodGetEffectiveFromAsCalendarForAggregateRoot(JavaCodeFragmentBuilder methodsBuilder) throws CoreException {
        methodsBuilder.javaDoc(getJavaDocCommentForOverriddenMethod(), ANNOTATION_GENERATED);
        methodsBuilder.methodBegin(java.lang.reflect.Modifier.PUBLIC, Calendar.class, MethodNames.GET_EFFECTIVE_FROM_AS_CALENDAR, new String[0], new Class[0]);
        if (getPolicyCmptType().hasSupertype()) {
            methodsBuilder.appendln("return super." + MethodNames.GET_EFFECTIVE_FROM_AS_CALENDAR + "();");
        } else {
            String todoText = getLocalizedText(getPcType(), "METHOD_GET_EFFECTIVE_FROM_TODO");
            methodsBuilder.appendln("return null; // " + getJavaNamingConvention().getToDoMarker() + " " + todoText);
        }
        methodsBuilder.methodEnd();
    }
    
    protected void generateMethodEffectiveFromHasChanged(JavaCodeFragmentBuilder methodsBuilder) throws CoreException {
        methodsBuilder.javaDoc(getJavaDocCommentForOverriddenMethod(), ANNOTATION_GENERATED);
        methodsBuilder.methodBegin(java.lang.reflect.Modifier.PUBLIC, Void.TYPE, MethodNames.EFFECTIVE_FROM_HAS_CHANGED, new String[0], new Class[0]);
        methodsBuilder.appendln("super." + MethodNames.EFFECTIVE_FROM_HAS_CHANGED + "();");

        IPolicyCmptTypeAssociation[] relations = getPolicyCmptType().getPolicyCmptTypeAssociations();
        for (int i = 0; i < relations.length; i++) {
            IPolicyCmptTypeAssociation r = relations[i];
            if (r.isValid() && r.isCompositionMasterToDetail() && !r.isDerivedUnion()) {
                IPolicyCmptType target = r.findTarget();
                if (!target.isConfigurableByProductCmptType()) {
                    continue;
                }
                methodsBuilder.appendln();
                String field = getFieldNameForRelation(r);
                if (r.is1ToMany()) {
                    methodsBuilder.append("for (");
                    methodsBuilder.appendClassName(Iterator.class);
                    methodsBuilder.append(" it=" + field + ".iterator(); it.hasNext();) {");
                    methodsBuilder.appendClassName(AbstractConfigurableModelObject.class);
                    methodsBuilder.append(" child = (");
                    methodsBuilder.appendClassName(AbstractConfigurableModelObject.class);
                    methodsBuilder.append(")it.next();");
                    methodsBuilder.append("child." + MethodNames.EFFECTIVE_FROM_HAS_CHANGED + "();");
                    methodsBuilder.append("}");
                } else {
                    methodsBuilder.append("if (" + field + "!=null) {");
                    methodsBuilder.append("((");
                    methodsBuilder.appendClassName(AbstractConfigurableModelObject.class);
                    methodsBuilder.append(")" + field + ")." + MethodNames.EFFECTIVE_FROM_HAS_CHANGED + "();");
                    methodsBuilder.append("}");
                }
            }
        }
        methodsBuilder.methodEnd();
    }
    
    protected void generateMethodRemoveChildModelObjectInternal(JavaCodeFragmentBuilder methodsBuilder) throws CoreException {
        methodsBuilder.javaDoc(getJavaDocCommentForOverriddenMethod(), ANNOTATION_GENERATED);
        String paramName = "childToRemove";
        methodsBuilder.methodBegin(java.lang.reflect.Modifier.PUBLIC, Void.TYPE, MethodNames.REMOVE_CHILD_MODEL_OBJECT_INTERNAL, 
                new String[] {paramName}, new Class[]{IModelObject.class});
        methodsBuilder.appendln("super." + MethodNames.REMOVE_CHILD_MODEL_OBJECT_INTERNAL + "(" + paramName + ");");
        IPolicyCmptTypeAssociation[] relations = getPcType().getPolicyCmptTypeAssociations();
        for (int i = 0; i < relations.length; i++) {
            if (relations[i].isValid() && relations[i].getAssociationType().isCompositionMasterToDetail() && !relations[i].isDerivedUnion()) {
                String fieldName = getFieldNameForRelation(relations[i]);
                if (relations[i].is1ToMany()) {
                    methodsBuilder.appendln(fieldName + ".remove(" + paramName + ");");
                } else {
                    methodsBuilder.appendln("if (" + fieldName + "==" + paramName + ") {");
                    methodsBuilder.appendln(fieldName + " = null;");
                    methodsBuilder.appendln("}");
                }
            }
        }
        methodsBuilder.methodEnd();
    }

    protected void generateCodeForAttribute(IPolicyCmptTypeAttribute attribute,
            DatatypeHelper datatypeHelper,
            JavaCodeFragmentBuilder constantBuilder,
            JavaCodeFragmentBuilder memberVarsBuilder, 
            JavaCodeFragmentBuilder methodsBuilder) throws CoreException {
        
        if(org.faktorips.devtools.core.model.Modifier.PUBLIC.equals(attribute.getModifier())){
            interfaceBuilder.generateFieldConstantForProperty(attribute, datatypeHelper.getDatatype(), constantBuilder);
        }
        super.generateCodeForAttribute(attribute, datatypeHelper, constantBuilder, memberVarsBuilder, methodsBuilder);
    }
    
    protected void generateCodeForProductCmptTypeAttribute(
            IProductCmptTypeAttribute attribute,
            DatatypeHelper datatypeHelper,
            JavaCodeFragmentBuilder constantBuilder,
            JavaCodeFragmentBuilder memberVarsBuilder, 
            JavaCodeFragmentBuilder methodsBuilder) throws CoreException {

        String javaDoc = null; // getLocalizedText(null, a.getName()); // TODO
        methodsBuilder.javaDoc(javaDoc, ANNOTATION_GENERATED);
        interfaceBuilder.generateSignatureGetPropertyValue(attribute.getName(), datatypeHelper, methodsBuilder);
        methodsBuilder.openBracket();
        methodsBuilder.append("return ");
        methodsBuilder.append(interfaceBuilder.getMethodNameGetProductCmptGeneration(getProductCmptType()));
        methodsBuilder.append("().");
        methodsBuilder.append(this.productCmptGenInterfaceBuilder.getMethodNameGetValue(attribute, datatypeHelper));
        methodsBuilder.append("();");
        methodsBuilder.closeBracket();
    }
    
    /**
     * {@inheritDoc}
     */
    protected void generateCodeForConstantAttribute(IPolicyCmptTypeAttribute attribute,
            DatatypeHelper datatypeHelper,
            JavaCodeFragmentBuilder constantBuilder,
            JavaCodeFragmentBuilder memberVarsBuilder, JavaCodeFragmentBuilder methodsBuilder) throws CoreException {
     
        if (attribute.getModifier() == org.faktorips.devtools.core.model.Modifier.PUBLISHED) {
            return;
        }
        interfaceBuilder.generateFieldConstPropertyValue(attribute, datatypeHelper, constantBuilder);
    }

    /**
     * {@inheritDoc}
     */
    protected void generateCodeForChangeableAttribute(IPolicyCmptTypeAttribute attribute,
            DatatypeHelper datatypeHelper,
            JavaCodeFragmentBuilder memberVarsBuilder,
            JavaCodeFragmentBuilder methodsBuilder) throws CoreException {
        
        DatatypeHelper nonPrimitiveDatatypeHelper = StdBuilderHelper.getDatatypeHelperForValueSet(getIpsSrcFile().getIpsProject(), datatypeHelper);

        if(ValueSetType.RANGE.equals(attribute.getValueSet().getValueSetType())){
            if(org.faktorips.devtools.core.model.Modifier.PUBLIC.equals(attribute.getModifier())){
                interfaceBuilder.generateFieldMaxRangeFor(attribute, nonPrimitiveDatatypeHelper, memberVarsBuilder);
            }
            generateMethodGetRangeFor(attribute, nonPrimitiveDatatypeHelper, methodsBuilder);
        }
        if(ValueSetType.ENUM.equals(attribute.getValueSet().getValueSetType()) ||
                datatypeHelper.getDatatype() instanceof EnumDatatype){
            if(org.faktorips.devtools.core.model.Modifier.PUBLIC.equals(attribute.getModifier())){
                interfaceBuilder.generateFieldMaxAllowedValuesFor(attribute, nonPrimitiveDatatypeHelper, memberVarsBuilder);
            }
            generateMethodGetAllowedValuesFor(attribute, nonPrimitiveDatatypeHelper, methodsBuilder);
        }
        generateFieldForAttribute(attribute, datatypeHelper, memberVarsBuilder);
        generateMethodAttributeGetterFromMemberVar(attribute, datatypeHelper, methodsBuilder);
        generateMethodAttributeSetter(attribute, datatypeHelper, methodsBuilder);
    }
    
    /**
     * {@inheritDoc}
     */
    protected void generateCodeForDerivedAttribute(IPolicyCmptTypeAttribute attribute,
            DatatypeHelper datatypeHelper,
            JavaCodeFragmentBuilder memberVarsBuilder,
            JavaCodeFragmentBuilder methodsBuilder) throws CoreException {
        
        generateMethodAttributeDerivedGetter(attribute, datatypeHelper, methodsBuilder);
    }
    
    /**
     * {@inheritDoc}
     */
    protected void generateCodeForComputedAttribute(IPolicyCmptTypeAttribute attribute,
            DatatypeHelper datatypeHelper,
            JavaCodeFragmentBuilder memberVarsBuilder,
            JavaCodeFragmentBuilder methodsBuilder) throws CoreException {
        
        generateFieldForAttribute(attribute, datatypeHelper, memberVarsBuilder);
        generateMethodAttributeGetterFromMemberVar(attribute, datatypeHelper, methodsBuilder);
    }

    // TODO v2 - wollen wir die noch generieren.
    void generateMethodAttributeGetterFromProductCmpt(
            IProductCmptTypeAttribute a,
            DatatypeHelper datatypeHelper,
            JavaCodeFragmentBuilder methodsBuilder) throws CoreException {
        
        String javaDoc = null; // getLocalizedText(null, a.getName()); // TODO
        methodsBuilder.javaDoc(javaDoc, ANNOTATION_GENERATED);
        interfaceBuilder.generateSignatureGetPropertyValue(a.getName(), datatypeHelper, methodsBuilder);
        methodsBuilder.openBracket();
        methodsBuilder.append("return ");
        methodsBuilder.append(interfaceBuilder.getMethodNameGetProductCmptGeneration(getProductCmptType()));
        methodsBuilder.append("().");
        methodsBuilder.append(this.productCmptGenInterfaceBuilder.getMethodNameGetValue(a, datatypeHelper));
        methodsBuilder.append("();");
        methodsBuilder.closeBracket();
    }
    
    protected void generateFieldForAttribute(            
            IPolicyCmptTypeAttribute a,
            DatatypeHelper datatypeHelper,
            JavaCodeFragmentBuilder memberVarsBuilders) throws CoreException {
        
        JavaCodeFragment initialValueExpression = datatypeHelper.newInstance(a.getDefaultValue());
        String comment = getLocalizedText(a, "FIELD_ATTRIBUTE_VALUE_JAVADOC", a.getName());
        String fieldName = getFieldNameForAttribute(a);

        memberVarsBuilders.javaDoc(comment, ANNOTATION_GENERATED);
        memberVarsBuilders.varDeclaration(java.lang.reflect.Modifier.PRIVATE, datatypeHelper.getJavaClassName(), fieldName,
            initialValueExpression);
    }
    
    private void generateMethodAttributeDerivedGetter(
            IPolicyCmptTypeAttribute a,
            DatatypeHelper datatypeHelper,
            JavaCodeFragmentBuilder builder) throws CoreException {
        
        IIpsProject ipsProject = getIpsProject();
        builder.javaDoc(getJavaDocCommentForOverriddenMethod(), ANNOTATION_GENERATED);
        interfaceBuilder.generateSignatureGetPropertyValue(a.getName(), datatypeHelper, builder);
        builder.openBracket();

        IProductCmptTypeMethod formulaSignature = a.findComputationMethod(ipsProject);
        if (!a.isProductRelevant() || formulaSignature==null) {
            builder.append("return ");
            builder.append(datatypeHelper.newInstance(a.getDefaultValue()));
            builder.appendln(";");
        } else {
            IParameter[] parameters = formulaSignature.getParameters();
            boolean resolveTypesToPublishedInterface = formulaSignature.getModifier().isPublished();
            String[] paramNames = BuilderHelper.extractParameterNames(parameters);
            String[] paramTypes = StdBuilderHelper.transformParameterTypesToJavaClassNames(
                    parameters, resolveTypesToPublishedInterface, getIpsProject(), this, productCmptGenImplBuilder);

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
                if(paramDataype.isPrimitive()){
                    builder.append(((ValueDatatype)paramDataype).getDefaultValue());
                }
                else{
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
            builder.appendClassName(productCmptGenImplBuilder.getQualifiedClassName(getProductCmptType()));
            builder.append(')');
            builder.append(interfaceBuilder.getMethodNameGetProductCmptGeneration(getProductCmptType()));
            builder.append("()).");
            builder.append(formulaSignature.getName());
            builder.append(paramFragment);
            builder.append(";");
        } 
        builder.closeBracket();
    }

    /**
     * Returns the name of the field/member variable that stores the values
     * for the property/attribute.
     */
    public String getFieldNameForAttribute(IPolicyCmptTypeAttribute a) throws CoreException {
        return getJavaNamingConvention().getMemberVarName(a.getName());
    }
    
    /**
     * Code sample:
     * <pre>
     * public Money getPremium() {
     *     return premium;
     * }
     * </pre>
     */
    private void generateMethodAttributeGetterFromMemberVar(
            IPolicyCmptTypeAttribute a,
            DatatypeHelper datatypeHelper,
            JavaCodeFragmentBuilder methodsBuilder) throws CoreException {
        
        methodsBuilder.javaDoc(getJavaDocCommentForOverriddenMethod(), ANNOTATION_GENERATED);
        interfaceBuilder.generateSignatureGetPropertyValue(a.getName(), datatypeHelper, methodsBuilder);
        methodsBuilder.openBracket();
        methodsBuilder.append("return ");
        methodsBuilder.append(getFieldNameForAttribute(a));
        methodsBuilder.append(";");
        methodsBuilder.closeBracket();
    }
    
    /**
     * Code sample:
     * 
     * <pre>
     * public void setPremium(Money newValue) {
     *     this.premium = newValue;
     * }
     * </pre>
     */
    protected void generateMethodAttributeSetter(
            IPolicyCmptTypeAttribute a,
            DatatypeHelper datatypeHelper,
            JavaCodeFragmentBuilder methodsBuilder) throws CoreException {
        
        methodsBuilder.javaDoc(getJavaDocCommentForOverriddenMethod(), ANNOTATION_GENERATED);
        interfaceBuilder.generateSignatureSetPropertyValue(a, datatypeHelper, methodsBuilder);
        methodsBuilder.openBracket();
        methodsBuilder.append("this.");
        methodsBuilder.append(getFieldNameForAttribute(a));
        methodsBuilder.appendln(" = " + interfaceBuilder.getParamNameForSetPropertyValue(a) + ";");
        generateChangeListenerSupport( methodsBuilder, IModelObjectChangedEvent.class.getName(), "MUTABLE_PROPERTY_CHANGED", getFieldNameForAttribute(a) );
        
        methodsBuilder.closeBracket();
    }

    protected void generateChangeListenerSupport(JavaCodeFragmentBuilder methodsBuilder, String eventClassName, String eventConstant, String fieldName) {
    	generateChangeListenerSupport(methodsBuilder, eventClassName, eventConstant, fieldName, null);
	}
    
    protected void generateChangeListenerSupport(JavaCodeFragmentBuilder methodsBuilder, String eventClassName, String eventConstant, String fieldName, String paramName) {
    	if (isChangeListenerSupportActive()) {
            methodsBuilder.appendln("if (" + MethodNames.EXISTS_CHANGE_LISTENER_TO_BE_INFORMED + "()) {");
            methodsBuilder.append(MethodNames.NOTIFIY_CHANGE_LISTENERS + "(new ");
            methodsBuilder.appendClassName(ModelObjectChangedEvent.class);
            methodsBuilder.append("(this, ");
            methodsBuilder.appendClassName(eventClassName);
            methodsBuilder.append('.');
            methodsBuilder.append(eventConstant);
            methodsBuilder.append(", ");
            methodsBuilder.appendQuoted(fieldName);
            if(paramName != null) {
            	methodsBuilder.append(", ");
            	methodsBuilder.append(paramName);
            }
            methodsBuilder.appendln("));");
            methodsBuilder.appendln("}");
        }
	}

	/**
     * {@inheritDoc}
     */
    protected void generateCodeForRelationInCommon(IPolicyCmptTypeAssociation relation, JavaCodeFragmentBuilder fieldsBuilder, JavaCodeFragmentBuilder methodsBuilder) throws Exception {
    }

    /**
     * {@inheritDoc}
     */
    protected void generateCodeFor1To1Relation(IPolicyCmptTypeAssociation relation, JavaCodeFragmentBuilder fieldsBuilder, JavaCodeFragmentBuilder methodsBuilder) throws Exception {
        if (relation.isCompositionDetailToMaster()) {
            generateMethodGetTypesafeParentObject(relation, methodsBuilder);
            return; 
        }
        if (!relation.isDerivedUnion()) {
            IPolicyCmptType target = relation.findTarget();
            generateFieldForRelation(relation, target, fieldsBuilder);
            generateMethodGetRefObjectBasedOnMemberVariable(relation, methodsBuilder);
            if (relation.isAssoziation()) {
                generateMethodSetRefObjectForAssociation(relation, methodsBuilder);
            } else if (relation.isCompositionMasterToDetail()) { 
                generateMethodSetRefObjectForComposition(relation, methodsBuilder);
            }
            generateNewChildMethodsIfApplicable(relation, target, methodsBuilder);
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void generateCodeFor1ToManyRelation(IPolicyCmptTypeAssociation relation, JavaCodeFragmentBuilder fieldsBuilder, JavaCodeFragmentBuilder methodsBuilder) throws Exception {
        IPolicyCmptType target = relation.findTarget();
        if (relation.isDerivedUnion()) {
            generateMethodContainsObjectForContainerRelation(relation, methodsBuilder);
        } else {
            generateFieldForRelation(relation, target, fieldsBuilder);
            generateMethodGetNumOfForNoneContainerRelation(relation, methodsBuilder);
            generateMethodContainsObjectForNoneContainerRelation(relation, methodsBuilder);
            generateMethodGetAllRefObjectsForNoneContainerRelation(relation, methodsBuilder);
            generateMethodGetRefObjectAtIndex(relation, methodsBuilder);
            generateNewChildMethodsIfApplicable(relation, target, methodsBuilder);
            generateMethodAddObject(relation, methodsBuilder);
            generateMethodRemoveObject(relation, methodsBuilder);
        }
    }
    
    protected void generateFieldForRelation(
            IPolicyCmptTypeAssociation relation,
            IPolicyCmptType target,
            JavaCodeFragmentBuilder methodsBuilder) throws CoreException {
        
        String javaClassname = relation.is1ToMany() ? List.class.getName()
                : getQualifiedClassName(target);
        JavaCodeFragment initialValueExpression = new JavaCodeFragment();
        if (relation.is1ToMany()) {
            initialValueExpression.append("new ");
            initialValueExpression.appendClassName(ArrayList.class);
            initialValueExpression.append("()");
        } else {
            initialValueExpression.append("null");
        }
        String comment = getLocalizedText(relation, "FIELD_RELATION_JAVADOC", relation.getName());
        methodsBuilder.javaDoc(comment, JavaSourceFileBuilder.ANNOTATION_GENERATED);
        methodsBuilder.varDeclaration(java.lang.reflect.Modifier.PRIVATE, javaClassname, getFieldNameForRelation(relation),
            initialValueExpression);
    }

    /**
     * Code sample:
     * <pre>
     * [Javadoc]
     * public int getNumOfCoverages() {
     *     return coverages.size();
     * }
     * </pre>
     */
    protected void generateMethodGetNumOfForNoneContainerRelation(IPolicyCmptTypeAssociation relation, JavaCodeFragmentBuilder methodsBuilder) throws Exception {
        methodsBuilder.javaDoc(getJavaDocCommentForOverriddenMethod(), ANNOTATION_GENERATED);
        interfaceBuilder.generateSignatureGetNumOfRefObjects(relation, methodsBuilder);
        methodsBuilder.openBracket();
        String field = getFieldNameForRelation(relation);
        if (relation.is1ToMany()) {
            methodsBuilder.appendln("return " + field + ".size();");
        } else {
            methodsBuilder.appendln("return " + field + "==null ? 0 : 1;");
        }
        methodsBuilder.closeBracket();
    }
    
    /**
     * Code sample:
     * <pre>
     * [Javadoc]
     * public int getNumOfCoveragesInternal() {
     *     int num = 0; 
     *     num += super.getNumOfCollisionCoverages(); // generated only if class has none abstract superclass
     *     num += getNumOfCollisionsCoverages(); 
     *     num += tplCoverage==null ? 0 : 1;
     *     return num;
     * }
     * </pre>
     */
    protected void generateMethodGetNumOfInternalForContainerRelationImplementation(
            IPolicyCmptTypeAssociation containerRelation, 
            List implRelations,
            JavaCodeFragmentBuilder methodsBuilder) throws Exception {
    
        methodsBuilder.javaDoc(null, ANNOTATION_GENERATED);
        String methodName = getMethodNameGetNumOfRefObjectsInternal(containerRelation);
        methodsBuilder.signature(java.lang.reflect.Modifier.PRIVATE, "int", methodName, new String[]{}, new String[]{});
        methodsBuilder.openBracket();
        methodsBuilder.append("int num = 0;");
        IPolicyCmptType supertype = getPcType().findSupertype();
        if (supertype!=null && !supertype.isAbstract()) {
            String methodName2 = interfaceBuilder.getMethodNameGetNumOfRefObjects(containerRelation);
            methodsBuilder.appendln("num += super." + methodName2 + "();");
        }
        for (int i = 0; i < implRelations.size(); i++) {
            methodsBuilder.appendln();
            IPolicyCmptTypeAssociation relation = (IPolicyCmptTypeAssociation)implRelations.get(i);
            methodsBuilder.append("num += ");
            if (relation.is1ToMany()) {
                methodsBuilder.append(interfaceBuilder.getMethodNameGetNumOfRefObjects(relation) + "();");
            } else {
                String field = getFieldNameForRelation(relation);
                methodsBuilder.append(field + "==null ? 0 : 1;");
            }
        }
        methodsBuilder.append("return num;");
        methodsBuilder.closeBracket();
    }
    
    /*
     * Returns the name of the internal method returning the number of referenced objects,
     * e.g. getNumOfCoveragesInternal()
     */
    private String getMethodNameGetNumOfRefObjectsInternal(IPolicyCmptTypeAssociation relation) {
        return getLocalizedText(relation, "METHOD_GET_NUM_OF_INTERNAL_NAME", StringUtils.capitalise(relation.getTargetRolePlural()));
    }
    
    
    /**
     * Code sample:
     * <pre>
     * [Javadoc]
     * public int getNumOfCoverages() {
     *     return getNumOfCoveragesInternal();
     * }
     * </pre>
     */
    protected void generateMethodGetNumOfForContainerRelationImplementation(
            IPolicyCmptTypeAssociation containerRelation, 
            List implRelations,
            JavaCodeFragmentBuilder methodsBuilder) throws Exception {
        
        methodsBuilder.javaDoc(getJavaDocCommentForOverriddenMethod(), ANNOTATION_GENERATED);
        interfaceBuilder.generateSignatureGetNumOfRefObjects(containerRelation, methodsBuilder);
        methodsBuilder.openBracket();
        String methodName = getMethodNameGetNumOfRefObjectsInternal(containerRelation);
        methodsBuilder.append("return " + methodName + "();");
        methodsBuilder.closeBracket();
    }
    
    /**
     * Code sample:
     * <pre>
     * [Javadoc]
     * public boolean containsCoverage(ICoverage objectToTest) {
     *     return coverages.contains(objectToTest);
     * }
     * </pre>
     */
    protected void generateMethodContainsObjectForNoneContainerRelation(
            IPolicyCmptTypeAssociation relation, 
            JavaCodeFragmentBuilder methodsBuilder) throws Exception {
        
        String paramName = interfaceBuilder.getParamNameForContainsObject(relation);
        
        methodsBuilder.javaDoc(getJavaDocCommentForOverriddenMethod(), ANNOTATION_GENERATED);
        interfaceBuilder.generateSignatureContainsObject(relation, methodsBuilder);
        
        methodsBuilder.openBracket();
        String field = getFieldNameForRelation(relation);
        methodsBuilder.appendln("return " + field + ".contains(" + paramName + ");");
        methodsBuilder.closeBracket();
    }
    
    /**
     * Code sample:
     * <pre>
     * [Javadoc]
     * public boolean containsCoverage(ICoverage objectToTest) {
     *     ICoverage[] targets = getCoverages();
     *     for (int i = 0; i < targets.length; i++) {
     *         if (targets[i] == objectToTest)
     *             return true;
     *     }
     *     return false;
     * }
     * </pre>
     */
    protected void generateMethodContainsObjectForContainerRelation(
            IPolicyCmptTypeAssociation relation, 
            JavaCodeFragmentBuilder methodsBuilder) throws Exception {
        
        String paramName = interfaceBuilder.getParamNameForContainsObject(relation);
        
        methodsBuilder.javaDoc(getJavaDocCommentForOverriddenMethod(), ANNOTATION_GENERATED);
        interfaceBuilder.generateSignatureContainsObject(relation, methodsBuilder);
        
        methodsBuilder.openBracket();
        methodsBuilder.appendClassName(interfaceBuilder.getQualifiedClassName(relation.findTarget()));
        methodsBuilder.append("[] targets = ");
        methodsBuilder.append(interfaceBuilder.getMethodNameGetAllRefObjects(relation));
        methodsBuilder.append("();");
        methodsBuilder.append("for(int i=0;i < targets.length;i++) {");
        methodsBuilder.append("if(targets[i] == " + paramName + ") return true; }");
        methodsBuilder.append("return false;");
        methodsBuilder.closeBracket();
    }
    
    /**
     * Code sample:
     * <pre>
     * [Javadoc]
     * public ICoverage[] getCoverages() {
     *     return (ICoverage[])coverages.toArray(new ICoverage[coverages.size()]);
     * }
     * </pre>
     */
    protected void generateMethodGetAllRefObjectsForNoneContainerRelation(
            IPolicyCmptTypeAssociation relation, 
            JavaCodeFragmentBuilder methodsBuilder) throws Exception {
        methodsBuilder.javaDoc(getJavaDocCommentForOverriddenMethod(), ANNOTATION_GENERATED);
        interfaceBuilder.generateSignatureGetAllRefObjects(relation, methodsBuilder);
        String className = getQualifiedClassName(relation.findTarget());
        String field = getFieldNameForRelation(relation);
        methodsBuilder.openBracket();
        methodsBuilder.appendln("return (");
        methodsBuilder.appendClassName(className);
        methodsBuilder.append("[])");
        methodsBuilder.append(field);
        methodsBuilder.append(".toArray(new ");
        methodsBuilder.appendClassName(className);
        methodsBuilder.append('[');
        methodsBuilder.append(field);
        methodsBuilder.append(".size()]);");
        methodsBuilder.closeBracket();
    }
    
    /**
     * Code sample:
     * <pre>
     * [Javadoc]
     * public ICoverage[] getCoverages() {
     *     ICoverage[] result = new ICoverage[getNumOfCoveragesInternal()];
     *     ICoverage[] elements;
     *     counter = 0;
     *     elements = getTplCoverages();
     *     for (int i = 0; i < elements.length; i++) {
     *         result[counter] = elements[i];
     *         counter++;
     *     }
     *     return result;
     * }
     * </pre>
     */
    protected void generateMethodGetAllRefObjectsForContainerRelationImplementation(
            IPolicyCmptTypeAssociation relation,
            List subRelations,
            JavaCodeFragmentBuilder methodsBuilder) throws Exception {
        
        methodsBuilder.javaDoc(getJavaDocCommentForOverriddenMethod(), ANNOTATION_GENERATED);
        interfaceBuilder.generateSignatureGetAllRefObjects(relation, methodsBuilder);
        String classname = interfaceBuilder.getQualifiedClassName(relation.findTarget());
        
        methodsBuilder.openBracket();
        methodsBuilder.appendClassName(classname);
        methodsBuilder.append("[] result = new ");       
        methodsBuilder.appendClassName(classname);
        methodsBuilder.append("[" + getMethodNameGetNumOfRefObjectsInternal(relation) + "()];");       

        IPolicyCmptType supertype = getPcType().findSupertype();
        if (supertype!=null && !supertype.isAbstract()) {
            // ICoverage[] superResult = super.getCoverages();
            // System.arraycopy(superResult, 0, result, 0, superResult.length);
            // int counter = superResult.length;
            methodsBuilder.appendClassName(classname);
            methodsBuilder.append("[] superResult = super.");       
            methodsBuilder.appendln(interfaceBuilder.getMethodNameGetAllRefObjects(relation) + "();");
            
            methodsBuilder.appendln("System.arraycopy(superResult, 0, result, 0, superResult.length);");
            methodsBuilder.appendln("int counter = superResult.length;");
        } else {
            methodsBuilder.append("int counter = 0;");
        }
        
        boolean elementsVarDefined = false;
        for (int i = 0; i < subRelations.size(); i++) {
            IPolicyCmptTypeAssociation subrel = (IPolicyCmptTypeAssociation)subRelations.get(i);
            if (subrel.is1ToMany()) {
                if (!elementsVarDefined) {
                    methodsBuilder.appendClassName(classname);   
                    methodsBuilder.append("[] ");
                    elementsVarDefined = true;
                }
                String method = interfaceBuilder.getMethodNameGetAllRefObjects(subrel);
                methodsBuilder.appendln("elements = " + method + "();");
                methodsBuilder.appendln("for (int i=0; i<elements.length; i++) {");
                methodsBuilder.appendln("result[counter++] = elements[i];");
                methodsBuilder.appendln("}");
            } else {
                String method = interfaceBuilder.getMethodNameGetRefObject(subrel);
                methodsBuilder.appendln("if (" + method + "()!=null) {");
                methodsBuilder.appendln("result[counter++] = " + method + "();");
                methodsBuilder.appendln("}");    
            }
        }
        methodsBuilder.append("return result;");
        methodsBuilder.closeBracket();
    }
    
    /**
     * Code sample:
     * <pre>
     * [Javadoc]
     * public ICoverage getCoverage() {
     *     return coverage;
     * }
     * </pre>
     */
    protected void generateMethodGetRefObjectBasedOnMemberVariable(
            IPolicyCmptTypeAssociation relation, 
            JavaCodeFragmentBuilder methodsBuilder) throws Exception {
        
        methodsBuilder.javaDoc(getJavaDocCommentForOverriddenMethod(), ANNOTATION_GENERATED);
        interfaceBuilder.generateSignatureGetRefObject(relation, methodsBuilder);
        methodsBuilder.openBracket();
        if (!relation.isCompositionDetailToMaster()) {
            String field = getFieldNameForRelation(relation);
            methodsBuilder.appendln("return " + field + ";");
        } else {
            IPolicyCmptType target = relation.findTarget();
            methodsBuilder.append("return (");
            methodsBuilder.appendClassName(interfaceBuilder.getQualifiedClassName(target));
            methodsBuilder.append(")" + MethodNames.GET_PARENT + "();");
        }
        methodsBuilder.closeBracket();
    }
    
    /**
     * Code sample:
     * <pre>
     * [Javadoc]
     * public ICoverage getCoverage() {
     *     return (ICoverage)getParentModelObject();
     * }
     * </pre>
     */
    protected void generateMethodGetTypesafeParentObject(
            IPolicyCmptTypeAssociation relation, 
            JavaCodeFragmentBuilder methodsBuilder) throws Exception {
        
        methodsBuilder.javaDoc(getJavaDocCommentForOverriddenMethod(), ANNOTATION_GENERATED);
        interfaceBuilder.generateSignatureGetRefObject(relation, methodsBuilder);
        methodsBuilder.openBracket();
        IPolicyCmptType target = relation.findTarget();
        methodsBuilder.append("return (");
        methodsBuilder.appendClassName(interfaceBuilder.getQualifiedClassName(target));
        methodsBuilder.append(")" + MethodNames.GET_PARENT + "();");
        methodsBuilder.closeBracket();
    }
    
    /**
     * Code sample:
     * <pre>
     * [Javadoc]
     * public ICoverage getCoverage() {
     *     if(getNumOfTplCoverage() > 0) { 
     *         return getTplCoverage(); 
     *     } 
     *     if (getNumOfCollisionCoverage() > 0) { 
     *         return getCollisionCoverage(); 
     *     } 
     *     return null;
     * }
     * </pre>
     */
    protected void generateMethodGetRefObjectForContainerRelationImplementation(
            IPolicyCmptTypeAssociation relation,
            List subRelations,
            JavaCodeFragmentBuilder methodsBuilder) throws Exception {
        methodsBuilder.javaDoc(getJavaDocCommentForOverriddenMethod(), ANNOTATION_GENERATED);
        interfaceBuilder.generateSignatureGetRefObject(relation, methodsBuilder);
        methodsBuilder.openBracket();
        for (int i = 0; i < subRelations.size(); i++) {
            IPolicyCmptTypeAssociation subrel = (IPolicyCmptTypeAssociation)subRelations.get(i);
            String accessCode;
            accessCode = interfaceBuilder.getMethodNameGetRefObject(subrel) + "()";
            methodsBuilder.appendln("if (" + accessCode + "!=null) {");
            methodsBuilder.appendln("return " + accessCode + ";");
            methodsBuilder.appendln("}");
        }
        methodsBuilder.append("return null;");
        methodsBuilder.closeBracket();
    }
    
    /**
     * Code sample:
     * <pre>
     * [Javadoc]
     * public void addCoverage(ICoverage objectToAdd) {
     *     if(objectToAdd == null) { 
     *         throw new IllegalArgumentException("Can't add null to ...");
     *     }
     *     if (coverages.contains(objectToAdd)) { 
     *         return; 
     *     }
     *     coverages.add(objectToAdd);
     * }
     * </pre>
     */
    protected void generateMethodAddObject (
            IPolicyCmptTypeAssociation relation, 
            JavaCodeFragmentBuilder methodsBuilder) throws CoreException {
        
        methodsBuilder.javaDoc(getJavaDocCommentForOverriddenMethod(), ANNOTATION_GENERATED);
        interfaceBuilder.generateSignatureAddObject(relation, methodsBuilder);
        String fieldname = getFieldNameForRelation(relation);
        String paramName = interfaceBuilder.getParamNameForAddObject(relation);
        IPolicyCmptTypeAssociation reverseRelation = relation.findInverseRelation();
        methodsBuilder.openBracket();
        methodsBuilder.append("if (" + paramName + " == null) {");
        methodsBuilder.append("throw new ");
        methodsBuilder.appendClassName(NullPointerException.class);
        methodsBuilder.append("(\"Can't add null to relation " + relation.getName() + " of \" + this); }");
        methodsBuilder.append("if(");
        methodsBuilder.append(fieldname);
        methodsBuilder.append(".contains(" + paramName + ")) { return; }");
        if (relation.isCompositionMasterToDetail()) {
            IPolicyCmptType target = relation.findTarget();
            if (target!=null && target.isDependantType()) {
                methodsBuilder.append(generateCodeToSynchronizeReverseComposition(paramName, "this"));
            }
        }
        methodsBuilder.append(fieldname);
        methodsBuilder.append(".add(" + paramName + ");");
        if (relation.isAssoziation() && reverseRelation!=null) {
            String targetClass = interfaceBuilder.getQualifiedClassName(relation.findTarget());
            methodsBuilder.append(generateCodeToSynchronizeReverseAssoziation(paramName, targetClass, relation, reverseRelation));
        }
        generateChangeListenerSupport(methodsBuilder, IModelObjectChangedEvent.class.getName(), "RELATION_OBJECT_ADDED" , fieldname, paramName);
        methodsBuilder.closeBracket();
    }
    
    

	/**
     * Code sample:
     * <pre>
     * [Javadoc]
     * public ICoverage newCoverage() {
     *     ICoverage newCoverage = new Coverage();
     *     return newCoverage;
     * }
     * </pre>
     */
    public void generateMethodNewChild(
            IPolicyCmptTypeAssociation relation, 
            IPolicyCmptType target,
            boolean inclProductCmptArg,
            JavaCodeFragmentBuilder methodsBuilder) throws CoreException {
        
        methodsBuilder.javaDoc(getJavaDocCommentForOverriddenMethod(), ANNOTATION_GENERATED);
        interfaceBuilder.generateSignatureNewChild(relation, target, inclProductCmptArg, methodsBuilder);
        String addMethod = relation.is1ToMany() ? interfaceBuilder.getMethodNameAddObject(relation) :
            interfaceBuilder.getMethodNameSetObject(relation);
        String varName = "new" + relation.getTargetRoleSingular();
        methodsBuilder.openBracket();
        methodsBuilder.appendClassName(getQualifiedClassName(target));
        methodsBuilder.append(" " + varName + " = new ");
        methodsBuilder.appendClassName(getQualifiedClassName(target));
        if (inclProductCmptArg) {
            methodsBuilder.appendln("(" + interfaceBuilder.getParamNameForProductCmptInNewChildMethod(target.findProductCmptType(getIpsProject())) + ");");  
        } else {
            methodsBuilder.appendln("();");
        }
        methodsBuilder.appendln(addMethod + "(" + varName + ");");
        methodsBuilder.appendln(varName + "." + getMethodNameInitialize() + "();");
        methodsBuilder.appendln("return " + varName + ";");
        methodsBuilder.closeBracket();
    }
    
    /**
     * Code sample:
     * <pre>
     * [Javadoc]
     * public void removeMotorCoverage(IMotorCoverage objectToRemove) {
     *     if (objectToRemove == null) {
     *          return;
     *      }
     *      if (motorCoverages.remove(objectToRemove)) {
     *          objectToRemove.setMotorPolicy(null);
     *      }
     *  }
     * </pre>
     */
    protected void generateMethodRemoveObject(
            IPolicyCmptTypeAssociation relation, 
            JavaCodeFragmentBuilder methodsBuilder) throws CoreException {

        String fieldname = getFieldNameForRelation(relation);
        String paramName = interfaceBuilder.getParamNameForRemoveObject(relation);
        IPolicyCmptTypeAssociation reverseRelation = relation.findInverseRelation();
        IPolicyCmptType target = relation.findTarget();
        
        methodsBuilder.javaDoc(getJavaDocCommentForOverriddenMethod(), ANNOTATION_GENERATED);
        interfaceBuilder.generateSignatureRemoveObject(relation, methodsBuilder);

        methodsBuilder.openBracket();
        methodsBuilder.append("if(" + paramName + "== null) {return;}");
        
        if (reverseRelation != null || (relation.isComposition() && target!=null && target.isDependantType())) {
            methodsBuilder.append("if(");
        }
        methodsBuilder.append(fieldname);
        methodsBuilder.append(".remove(" + paramName + ")");
        if (reverseRelation != null || (relation.isComposition() && target!=null && target.isDependantType())) {
            methodsBuilder.append(") {");
            if (relation.isAssoziation()) {
                methodsBuilder.append(generateCodeToCleanupOldReference(relation, reverseRelation, paramName));
            } else {
                methodsBuilder.append(generateCodeToSynchronizeReverseComposition(paramName, "null"));
            }
            methodsBuilder.append(" }");
        } else {
            methodsBuilder.append(';');
        }
        generateChangeListenerSupport(methodsBuilder, IModelObjectChangedEvent.class.getName(), "RELATION_OBJECT_REMOVED", fieldname, paramName);
        methodsBuilder.closeBracket();
    }
    
    /**
     * Code sample:
     * <pre>
     * [Javadoc]
     * public void setCoverage(ICoverage newObject) {
     *     if (homeContract!=null) {
     *         ((DependantObject)homeContract).setParentModelObjectInternal(null);
     *     }
     *     homeContract = (HomeContract)newObject;
     *     if (homeContract!=null) {
     *         ((DependantObject)homeContract).setParentModelObjectInternal(this);
     *     }
     * }
     * </pre>
     */
    protected void generateMethodSetRefObjectForComposition(
            IPolicyCmptTypeAssociation relation, 
            JavaCodeFragmentBuilder methodsBuilder) throws Exception {
        
        if (relation.isCompositionDetailToMaster()) {
            return; // setter defined in base class.
        }
        String fieldname = getFieldNameForRelation(relation);
        String paramName = interfaceBuilder.getParamNameForSetObject(relation);
        IPolicyCmptType target = relation.findTarget();

        methodsBuilder.javaDoc(getJavaDocCommentForOverriddenMethod(), ANNOTATION_GENERATED);
        interfaceBuilder.generateSignatureSetObject(relation, methodsBuilder);
        
        methodsBuilder.openBracket();
        if (target.isDependantType()) {
            methodsBuilder.appendln("if(" + fieldname + " != null) {");
            methodsBuilder.append(generateCodeToSynchronizeReverseComposition(fieldname, "null"));;
            methodsBuilder.appendln("}");
        }

        if (target.isDependantType()) {
            methodsBuilder.appendln("if(" + paramName + " != null) {");
            methodsBuilder.append(generateCodeToSynchronizeReverseComposition(paramName, "this"));;
            methodsBuilder.appendln("}");
        }
        
        methodsBuilder.append(fieldname);
        methodsBuilder.append(" = (");
        methodsBuilder.appendClassName(getQualifiedClassName(target));
        methodsBuilder.append(")" + paramName +";");

        generateChangeListenerSupport(methodsBuilder, IModelObjectChangedEvent.class.getName(), "RELATION_OBJECT_CHANGED", fieldname, paramName);
        methodsBuilder.closeBracket();
    }
    
    /**
     * Code sample:
     * <pre>
     * [Javadoc]
     * public void setCoverage(ICoverage newObject) {
     *     if (refObject == homeContract)
     *         return;
     *     IHomeContract oldRefObject = homeContract;
     *     homeContract = null;
     *     if (oldRefObject != null) {
     *          oldRefObject.setHomePolicy(null);
     *     }
     *     homeContract = (HomeContract) refObject;
     *     if (refObject != null && refObject.getHomePolicy() != this) {
     *         refObject.setHomePolicy(this);
     *     }
     * }
     * </pre>
     */
    protected void generateMethodSetRefObjectForAssociation(
            IPolicyCmptTypeAssociation relation, 
            JavaCodeFragmentBuilder methodsBuilder) throws Exception {
        
        String fieldname = getFieldNameForRelation(relation);
        String paramName = interfaceBuilder.getParamNameForSetObject(relation);
        IPolicyCmptType target = relation.findTarget();
        IPolicyCmptTypeAssociation reverseRelation = relation.findInverseRelation();

        methodsBuilder.javaDoc(getJavaDocCommentForOverriddenMethod(), ANNOTATION_GENERATED);
        interfaceBuilder.generateSignatureSetObject(relation, methodsBuilder);
        
        methodsBuilder.openBracket();
        methodsBuilder.append("if(" + paramName + " == ");
        methodsBuilder.append(fieldname);
        methodsBuilder.append(") return;");
        if (reverseRelation != null) {
            methodsBuilder.appendClassName(getQualifiedClassName(target));
            methodsBuilder.append(" oldRefObject = ");
            methodsBuilder.append(fieldname);
            methodsBuilder.append(';');
            methodsBuilder.append(fieldname);
            methodsBuilder.append(" = null;");
            methodsBuilder.append(generateCodeToCleanupOldReference(relation, reverseRelation, "oldRefObject"));
        }
        methodsBuilder.append(fieldname);
        methodsBuilder.append(" = (");
        methodsBuilder.appendClassName(getQualifiedClassName(target));
        methodsBuilder.append(")" + paramName +";");
        if (reverseRelation != null) {
            methodsBuilder.append(generateCodeToSynchronizeReverseAssoziation(fieldname, 
                    getQualifiedClassName(target), relation, reverseRelation));
        }
        generateChangeListenerSupport(methodsBuilder, IModelObjectChangedEvent.class.getName(), "RELATION_OBJECT_CHANGED", fieldname, paramName);
        methodsBuilder.closeBracket();
    }
    
    private JavaCodeFragment generateCodeToSynchronizeReverseAssoziation(
            String varName,
            String varClassName,
            IPolicyCmptTypeAssociation relation,
            IPolicyCmptTypeAssociation reverseRelation) throws CoreException {
        JavaCodeFragment code = new JavaCodeFragment();
        code.append("if(");
        if (!relation.is1ToMany()) {
            code.append(varName + " != null && ");
        }
        if (reverseRelation.is1ToMany()) {
            code.append("! " + varName + ".");
            code.append(interfaceBuilder.getMethodNameContainsObject(reverseRelation) + "(this)");
        } else {
            code.append(varName + ".");
            code.append(interfaceBuilder.getMethodNameGetRefObject(reverseRelation));
            code.append("() != this");
        }
        code.append(") {");
        if (reverseRelation.is1ToMany()) {
            code.append(varName + "." + interfaceBuilder.getMethodNameAddObject(reverseRelation));
        } else {
            String targetClass = getQualifiedClassName(relation.findTarget());
            if (!varClassName.equals(targetClass)) {
                code.append("((");
                code.appendClassName(targetClass);
                code.append(")" + varName + ").");
            } else {
                code.append(varName + ".");
            }
            code.append(interfaceBuilder.getMethodNameSetObject(reverseRelation));
        }
        code.appendln("(this);");
        code.appendln("}");
        return code;
    }
    
    /**
     * <pre>
     * ((DependantObject)parentModelObject).setParentModelObjectInternal(this);
     * </pre>
     */
    private JavaCodeFragment generateCodeToSynchronizeReverseComposition(
            String varName, String newValue) throws CoreException {
        
        JavaCodeFragment code = new JavaCodeFragment();
        code.append("((");
        code.appendClassName(DependantObject.class);
        code.append(')');
        code.append(varName);
        code.append(")." + MethodNames.SET_PARENT);
        code.append('(');
        code.append(newValue);
        code.appendln(");");
        return code;
    }
    
    private JavaCodeFragment generateCodeToCleanupOldReference(
            IPolicyCmptTypeAssociation relation, 
            IPolicyCmptTypeAssociation reverseRelation,
            String varToCleanUp) throws CoreException {
        
        JavaCodeFragment body = new JavaCodeFragment();
        if (!relation.is1ToMany()) {
            body.append("if (" + varToCleanUp + "!=null) {");
        }
        if (reverseRelation.is1ToMany()) {
            String removeMethod = interfaceBuilder.getMethodNameRemoveObject(reverseRelation);
            body.append(varToCleanUp + "." + removeMethod + "(this);");
        } else {
            String targetClass = getQualifiedClassName(relation.findTarget());
            String setMethod = interfaceBuilder.getMethodNameSetObject(reverseRelation);
            body.append("((");
            body.appendClassName(targetClass);
            body.append(")" + varToCleanUp + ")." + setMethod + "(null);");
        }
        if (!relation.is1ToMany()) {
            body.append(" }");
        }
        return body;
    }

    /**
     * Returns the name of field/member var for the relation.
     */
    public String getFieldNameForRelation(IPolicyCmptTypeAssociation relation) throws CoreException {
        if (relation.is1ToMany()) {
            return getJavaNamingConvention().getMemberVarName(relation.getTargetRolePlural());
        } else {
            return getJavaNamingConvention().getMemberVarName(relation.getTargetRoleSingular());
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void generateCodeForContainerRelationImplementation(
            IPolicyCmptTypeAssociation containerRelation,
            List relations,
            JavaCodeFragmentBuilder memberVarsBuilder,
            JavaCodeFragmentBuilder methodsBuilder) throws Exception {

        if (containerRelation.is1ToMany()) {
            generateMethodGetNumOfForContainerRelationImplementation(containerRelation, relations, methodsBuilder);
            generateMethodGetAllRefObjectsForContainerRelationImplementation(containerRelation, relations, methodsBuilder);
            generateMethodGetNumOfInternalForContainerRelationImplementation(containerRelation, relations, methodsBuilder);
        } else {
            generateMethodGetRefObjectForContainerRelationImplementation(containerRelation, relations, methodsBuilder);
        }
    }

    private void buildValidation(JavaCodeFragmentBuilder builder) throws CoreException {
        generateMethodValidateSelf(builder, getPcType().getPolicyCmptTypeAttributes());
        createMethodValidateDependants(builder);
        IValidationRule[] rules = getPcType().getRules();
        for (int i = 0; i < rules.length; i++) {
            IValidationRule r = rules[i];
            if(r.validate().containsErrorMsg()){
                continue;
           }
            generateMethodExecRule(r, builder);
            generateMethodCreateMessageForRule(r, builder);
        }
    }

    private void createMethodValidateDependants(JavaCodeFragmentBuilder builder)
            throws CoreException {
        /*
         * public void validateDependants(MessageList ml) { if(NumOfRelToMany() > 0) { TargetType[]
         * rels = GetAllRelationToMany(); for (int i = 0; i < rels.length; i++) {
         * ml.add(rels[i].validate()); } if (NumOfRelTo1() > 0) {
         * ml.add(GetRelationTo1().validate()); } }
         */

        String methodName = "validateDependants";
        IPolicyCmptTypeAssociation[] relations = getPcType().getPolicyCmptTypeAssociations();

        JavaCodeFragment body = new JavaCodeFragment();
        body.append("super.");
        body.append(methodName);
        body.append("(ml, businessFunction);");
        for (int i = 0; i < relations.length; i++) {
            IPolicyCmptTypeAssociation r = relations[i];
            if (!r.validate().containsErrorMsg()) {
                if (r.getAssociationType() == AssociationType.COMPOSITION_MASTER_TO_DETAIL
                        && StringUtils.isEmpty(r.getSubsettedDerivedUnion())) {
                    body.appendln();
                    if (r.is1ToMany()) {
                        IPolicyCmptType target = r.getIpsProject().findPolicyCmptType(r.getTarget());
                        body.append("if(");
                        body.append(interfaceBuilder.getMethodNameGetNumOfRefObjects(r));
                        body.append("() > 0) { ");
                        body.appendClassName(interfaceBuilder
                                .getQualifiedClassName(target.getIpsSrcFile()));
                        body.append("[] rels = ");
                        body.append(interfaceBuilder.getMethodNameGetAllRefObjects(r));
                        body.append("();");
                        body.append("for (int i = 0; i < rels.length; i++)");
                        body.append("{ ml.add(rels[i].validate(businessFunction)); } }");
                    } else {
                        String field = getFieldNameForRelation(r);
                        body.append("if (" + field + "!=null) {");
                        body.append("ml.add(" + field + ".validate(businessFunction));");
                        body.append("}");
                    }
                }
            }
        }

        String javaDoc = getLocalizedText(getPcType(), "VALIDATE_DEPENDANTS_JAVADOC", getPcType()
                .getName());

        builder.method(java.lang.reflect.Modifier.PUBLIC, Datatype.VOID.getJavaClassName(),
            methodName, new String[] { "ml", "businessFunction" }, 
            new String[] { MessageList.class.getName(), String.class.getName() }, body,
            javaDoc, ANNOTATION_GENERATED);
    }

    private void generateMethodValidateSelf(JavaCodeFragmentBuilder builder, IPolicyCmptTypeAttribute[] attributes)
            throws CoreException {
        /*
         * public void validateSelf(MessageList ml, String businessFunction) { super.validateSelf(ml, businessFunction); }
         */
        String methodName = "validateSelf";
        String javaDoc = getLocalizedText(getIpsObject(), "VALIDATE_SELF_JAVADOC", getPcType()
                .getName());

        JavaCodeFragment body = new JavaCodeFragment();
        body.append("if(!");
        body.append("super.");
        body.append(methodName);
        body.append("(ml, businessFunction))");
        body.appendOpenBracket();
        body.append(" return false;");
        body.appendCloseBracket();
        IValidationRule[] rules = getPcType().getRules();
        for (int i = 0; i < rules.length; i++) {
            IValidationRule r = rules[i];
            if(r.validate().isEmpty()){
                body.append("if(!");
                body.append(getMethodExpressionExecRule(r, "ml", "businessFunction"));
                body.append(')');
                body.appendOpenBracket();
                body.append(" return false;");
                body.appendCloseBracket();
            }
        }
        body.appendln(" return true;");
        // buildValidationValueSet(body, attributes); wegschmeissen ??
        builder.method(java.lang.reflect.Modifier.PUBLIC, Datatype.PRIMITIVE_BOOLEAN.getJavaClassName(),
            methodName, new String[] { "ml", "businessFunction" }, 
            new String[] { MessageList.class.getName(), String.class.getName() }, body,
            javaDoc, ANNOTATION_GENERATED);
    }

    /**
     * Code sample:
     * <pre>
     * [Javadoc]
     *   protected Message createMessageForRuleARule(String p0, String p1, String p2) {
     *      ObjectProperty[] objectProperties = new ObjectProperty[] { new ObjectProperty(this, PROPERTY_NAME_A),
     *              new ObjectProperty(this, PROPERTY_NAME_B) };
     *      StringBuffer text = new StringBuffer();
     *      text.append("Check parameters ");
     *      text.append(p0);
     *      text.append(", check if line break works in generated code\n");
     *      text.append(p1);
     *      text.append(" and ");
     *      text.append(p2);
     *      return new Message(MSG_CODE_ARULE, text.toString(), Message.ERROR, objectProperties);
     *  }
     * </pre>
     */
    private void generateMethodCreateMessageForRule(IValidationRule rule, JavaCodeFragmentBuilder builder) throws CoreException {
        String localVarObjectProperties = "invalidObjectProperties";
        String localVarReplacementParams = "replacementParameters";
        MessageFragment msgFrag = MessageFragment.createMessageFragment(rule.getMessageText(), MessageFragment.VALUES_AS_PARAMETER_NAMES);

        // determine method parameters (name and type)
        String[] methodParamNames;
        String[] methodParamTypes;
        if (!rule.isValidatedAttrSpecifiedInSrc()) {
            methodParamNames = msgFrag.getParameterNames();
            methodParamTypes = msgFrag.getParameterClasses();
        } else {
            int numberOfMethodParams = msgFrag.getNumberOfParameters() + 1;
            methodParamNames = new String[numberOfMethodParams];
            methodParamTypes = new String[numberOfMethodParams];
            System.arraycopy(msgFrag.getParameterNames(), 0, methodParamNames, 0, msgFrag.getNumberOfParameters());
            System.arraycopy(msgFrag.getParameterClasses(), 0, methodParamTypes, 0, msgFrag.getNumberOfParameters());
            methodParamNames[methodParamNames.length-1] = localVarObjectProperties;
            methodParamTypes[methodParamTypes.length-1] = ObjectProperty.class.getName() + "[]";
        }
        
        // code for objectProperties
        JavaCodeFragment body = new JavaCodeFragment();
        String[] validatedAttributes = rule.getValidatedAttributes();
        if(!rule.isValidatedAttrSpecifiedInSrc()){
            body.append(generateCodeForInvalidObjectProperties(localVarObjectProperties, validatedAttributes));
        }
        // code for replacement parameters
        if (msgFrag.hasParameters()) {
            body.append(generateCodeForMsgReplacementParameters(localVarReplacementParams, msgFrag.getParameterNames()));
        }
        
        // code to construct the message's text
        body.append(msgFrag.getFrag());

        // code to create the message and return it.
        body.append("return new ");
        body.appendClassName(Message.class);
        body.append('(');
        body.append(interfaceBuilder.getFieldNameForMsgCode(rule));
        body.append(", ");
        body.append(msgFrag.getMsgTextExpression());
        body.append(", ");
        body.append(rule.getMessageSeverity().getJavaSourcecode());
        body.append(", ");
        body.append(localVarObjectProperties);
        if (msgFrag.hasParameters()) {
            body.append(", ");
            body.append(localVarReplacementParams);
        }
        body.append(");");

        String javaDoc = getLocalizedText(rule, "CREATE_MESSAGE_JAVADOC", rule.getName());
        builder.method(java.lang.reflect.Modifier.PROTECTED, Message.class.getName(),
                getMethodNameCreateMessageForRule(rule), methodParamNames, methodParamTypes, body, javaDoc, ANNOTATION_GENERATED);
    }
    
    private String getMethodNameCreateMessageForRule(IValidationRule rule) {
        return "createMessageForRule" + StringUtils.capitalise(rule.getName());
    }
    
    private JavaCodeFragment generateCodeForInvalidObjectProperties(String pObjectProperties, String[] validatedAttributes) throws CoreException {
        JavaCodeFragment code = new JavaCodeFragment();
        if(validatedAttributes.length > 0){
            code.appendClassName(ObjectProperty.class);
            code.append("[] ");
            code.append(pObjectProperties);
            code.append(" = new ");
            code.appendClassName(ObjectProperty.class);
            code.append("[]{");
            for (int j = 0; j < validatedAttributes.length; j++) {
                IPolicyCmptTypeAttribute attr = getPcType().findAttributeInSupertypeHierarchy(validatedAttributes[j]);
                String propertyConstName = interfaceBuilder.getPropertyName(attr, attr.findDatatype());
                code.append(" new ");
                code.appendClassName(ObjectProperty.class);
                code.append("(this, ");
                code.append(propertyConstName);
                code.append(")");
                if(j < validatedAttributes.length -1){
                    code.append(',');
                }
            }
            code.appendln("};");
        }
        else{
            code.appendClassName(ObjectProperty.class);
            code.append(" ");
            code.append(pObjectProperties);
            code.append(" = new ");
            code.appendClassName(ObjectProperty.class);
            code.appendln("(this);");
        }
        return code;
    }
    
    /**
     * Code sample:
     * <pre>
     *   MsgReplacementParameter[] replacementParameters = new MsgReplacementParameter[] {
     *       new MsgReplacementParameter("maxVs", maxVs),
     *   };
     * 
     * </pre>
     */
    private JavaCodeFragment generateCodeForMsgReplacementParameters(String localVar, String[] parameterNames) {
        JavaCodeFragment code = new JavaCodeFragment();
        // MsgReplacementParameter[] replacementParameters = new MsgReplacementParameter[] {
        code.appendClassName(MsgReplacementParameter.class);
        code.append("[] " + localVar + " = new ");
        code.appendClassName(MsgReplacementParameter.class);
        code.appendln("[] {");

        for (int i = 0; i < parameterNames.length; i++) {

            //     new MsgReplacementParameter("paramName", paramName),
            code.append("new ");
            code.appendClassName(MsgReplacementParameter.class);
            code.append("(");
            code.appendQuoted(parameterNames[i]);
            code.append(", ");
            code.append(parameterNames[i]);
            code.append(")");
            if (i!=parameterNames.length-1) {
                code.append(", ");
            }
            code.appendln();
        }
        
        code.appendln("};");
        return code;
    }

    /**
     * Code sample:
     * <pre>
     * [Javadoc]
     * public Policy(Product productCmpt) {
     *     super(productCmpt);
     * }
     * </pre>
     */
    protected void generateConstructorWithProductCmptArg(JavaCodeFragmentBuilder builder)
            throws CoreException {

        appendLocalizedJavaDoc("CONSTRUCTOR", getUnqualifiedClassName(), getPcType(), builder);
        String[] paramNames = new String[] { "productCmpt" };
        String[] paramTypes = new String[] { 
                productCmptInterfaceBuilder.getQualifiedClassName(getProductCmptType())};
        builder.methodBegin(java.lang.reflect.Modifier.PUBLIC, null, getUnqualifiedClassName(),
                paramNames, paramTypes);
        builder.append("super(productCmpt);");
        generateInitializationForOverrideAttributes(builder);
        builder.methodEnd();
    }
    
    /**
     * Code sample:
     * <pre>
     * [Javadoc]
     * public Policy(Product productCmpt, Date effectiveDate) {
     *     super(productCmpt, effectiveDate);
     *     initialize();
     * }
     * </pre>
     */
    protected void generateConstructorDefault(JavaCodeFragmentBuilder builder)
        throws CoreException {

        appendLocalizedJavaDoc("CONSTRUCTOR", getUnqualifiedClassName(), getPcType(), builder);
        builder.methodBegin(java.lang.reflect.Modifier.PUBLIC, null, getUnqualifiedClassName(),
                new String[0], new String[0]);
        builder.append("super();");
        generateInitializationForOverrideAttributes(builder);
        builder.methodEnd();
    }

    private void generateInitializationForOverrideAttributes(JavaCodeFragmentBuilder builder) throws CoreException{
        IPolicyCmptTypeAttribute[] attributes = getPcType().getPolicyCmptTypeAttributes();
        for (int i = 0; i < attributes.length; i++) {
            if(attributes[i].isChangeable() && attributes[i].getOverwrites() && attributes[i].validate().isEmpty()){
                DatatypeHelper helper = getPcType().getIpsProject().getDatatypeHelper(attributes[i].getValueDatatype());
                JavaCodeFragment initialValueExpression = helper.newInstance(attributes[i].getDefaultValue());
                interfaceBuilder.generateCallToMethodSetPropertyValue(attributes[i], helper, initialValueExpression, builder);
            }
        }
    }
    
    /**
     * Code sample:
     * <pre>
     * [Javadoc]
     * protected void initialize() {
     *     super.initialize();
     *     paymentMode = getProductGen().getDefaultPaymentMode();
     *     ... and so for other properties
     * }
     * </pre>
     */
    protected void generateMethodInitialize(JavaCodeFragmentBuilder builder) throws CoreException {
        IPolicyCmptTypeAttribute[] attributes = getPcType().getPolicyCmptTypeAttributes();
        ArrayList selectedValues = new ArrayList();
        for (int i = 0; i < attributes.length; i++) {
            IPolicyCmptTypeAttribute a = attributes[i];
            if (!a.validate().containsErrorMsg()) {
                if (a.isProductRelevant() && a.isChangeable() && !a.getOverwrites()) {
                    selectedValues.add(a);
                }
            }
        }
        appendLocalizedJavaDoc("METHOD_INITIALIZE", getPcType(), builder);
        builder.methodBegin(java.lang.reflect.Modifier.PUBLIC, Datatype.VOID.getJavaClassName(),
                getMethodNameInitialize(), new String[0], new String[0]);
        if (StringUtils.isNotEmpty(getPcType().getSupertype())) {
            builder.append("super." + getMethodNameInitialize() + "();");
        }
        if(selectedValues.isEmpty()){
            builder.methodEnd();
            return;
        }
        if (getProductCmptType()==null) {
            builder.methodEnd();
            return;
        }
        String method = interfaceBuilder.getMethodNameGetProductCmptGeneration(getProductCmptType());
        builder.appendln("if (" + method + "()==null) {");
        builder.appendln("return;");
        builder.appendln("}");
        for (Iterator it = selectedValues.iterator(); it.hasNext();) {
            IPolicyCmptTypeAttribute a = (IPolicyCmptTypeAttribute)it.next();
            DatatypeHelper datatype = a.getIpsProject().findDatatypeHelper(a.getDatatype());
            builder.append(getFieldNameForAttribute(a));
            builder.append(" = ");
            builder.append(getMethodNameGetDefaultValueFromProductCmpt(a, datatype));
            builder.append(";");
        }
        builder.methodEnd();
    }
    
    /**
     * Returns the method name to initialize the policy component with the default data from
     * the product component.
     */
    public String getMethodNameInitialize() {
        return "initialize";
    }

    /**
     * Code sample:
     * <pre>
     * [Javadoc]
     * public IProduct getProduct() {
     *     return (IProduct) getProductComponent();
     * }
     * </pre>
     */
    protected void generateMethodGetProductCmpt(JavaCodeFragmentBuilder builder) throws CoreException {
        builder.javaDoc(getJavaDocCommentForOverriddenMethod(), ANNOTATION_GENERATED);
        interfaceBuilder.generateSignatureGetProductCmpt(getProductCmptType(), builder);
        builder.openBracket();
        String productCmptInterfaceQualifiedName = productCmptInterfaceBuilder.getQualifiedClassName(getProductCmptType());
        builder.append("return (");
        builder.appendClassName(productCmptInterfaceQualifiedName);
        builder.append(")getProductComponent();"); // don't use getMethodNameGetProductComponent() as this results in a recursive call
        // we have to call the generic superclass method here
        builder.closeBracket();
    }

    /**
     * Code sample:
     * <pre>
     * [Javadoc]
     * public IProductGen getProductGen() {
     *     return (IProductGen) getProduct().getProductGen(getEffectiveFromAsCalendar());
     * }
     * </pre>
     */
    protected void generateMethodGetProductCmptGeneration(JavaCodeFragmentBuilder builder) throws CoreException {
        builder.javaDoc(getJavaDocCommentForOverriddenMethod(), ANNOTATION_GENERATED);
        interfaceBuilder.generateSignatureGetProductCmptGeneration(getProductCmptType(), builder);
        builder.openBracket();
        builder.appendln("if (getProductComponent()==null) {");
        builder.appendln("return null;");
        builder.appendln("}");
        
        builder.append("return (");
        builder.appendClassName(productCmptGenInterfaceBuilder.getQualifiedClassName(getProductCmptType()));
        builder.append(")");
        builder.append(interfaceBuilder.getMethodNameGetProductCmpt(getProductCmptType()));
        builder.append("().");
        builder.append(productCmptInterfaceBuilder.getMethodNameGetGeneration(getProductCmptType()));
        builder.append('(');
        builder.append(MethodNames.GET_EFFECTIVE_FROM_AS_CALENDAR);
        builder.appendln("());");
        builder.closeBracket();
    }
    
    private void generateMethodSetProductCmpt(JavaCodeFragmentBuilder builder) throws CoreException {
        builder.javaDoc(getJavaDocCommentForOverriddenMethod(), ANNOTATION_GENERATED);
        interfaceBuilder.generateSignatureSetProductCmpt(getProductCmptType(), builder);
        String[] paramNames = interfaceBuilder.getMethodParamNamesSetProductCmpt(getProductCmptType());
        builder.openBracket();
        builder.appendln("setProductCmpt(" + paramNames[0] + ");");
        builder.appendln("if(" + paramNames[1] + ") { initialize(); }");
        builder.closeBracket();
    }

    private String getMethodNameGetDefaultValueFromProductCmpt(IPolicyCmptTypeAttribute a, DatatypeHelper datatype) throws CoreException {
        String methodName = productCmptGenInterfaceBuilder.getMethodNameGetDefaultValue(a, datatype);
        return interfaceBuilder.getMethodNameGetProductCmptGeneration(getProductCmptType()) + "()." + methodName + "()";
    }
    
    private String getMethodNameExecRule(IValidationRule r){
        return "execRule" + StringUtils.capitalise(r.getName());
    }
    
    private String getMethodExpressionExecRule(IValidationRule r, String messageList, String businessFunction){
        StringBuffer buf = new StringBuffer();
        buf.append(getMethodNameExecRule(r));
        buf.append('(');
        buf.append(messageList);
        buf.append(", ");
        buf.append(businessFunction);
        buf.append(")");
        return  buf.toString();
    }
    
    /**
     * Code sample:
     * <pre>
     * [Javadoc]
     *   if ("rules.businessProcess1".equals(businessFunction) || "rules.businessProcess2".equals(businessFunction)) {
     *      //begin-user-code
     *      boolean condition = getA().equals(new Integer(1));
     *      if (condition) {
     *          ml.add(createMessageForRuleARule(String.valueOf(getA()), String.valueOf(getB()), String.valueOf(getHallo())));
     *          return false;
     *      }
     *      return true;
     *      //end-user-code
     *  }
     *  return true;
     * </pre>
     */
    private void generateMethodExecRule(IValidationRule rule, JavaCodeFragmentBuilder builder) throws CoreException {
        String parameterBusinessFunction = "businessFunction";
        String javaDoc = getLocalizedText(getIpsObject(), "EXEC_RULE_JAVADOC", rule.getName());
        JavaCodeFragment body = new JavaCodeFragment();
        body.appendln();
        String[] businessFunctions = rule.getBusinessFunctions();
        if(!rule.isAppliedForAllBusinessFunctions()){
            if(businessFunctions.length > 0){
                body.append("if(");
                for (int j = 0; j < businessFunctions.length; j++) {
                    body.append("\"");
                    body.append(businessFunctions[j]);
                    body.append("\"");
                    body.append(".equals(");
                    body.append(parameterBusinessFunction);
                    body.append(")");
                    if(j < businessFunctions.length - 1){
                        body.appendln(" || ");
                    }
                }
                body.append(")");
                body.appendOpenBracket();
            }
        }
        if(!rule.isCheckValueAgainstValueSetRule()) {
            body.appendln("//begin-user-code");
            body.appendln(getLocalizedToDo(rule, "EXEC_RULE_IMPLEMENT", rule.getName()));
        }
        
        body.append("if(");
        String[] javaDocAnnotation = ANNOTATION_RESTRAINED_MODIFIABLE;
        if(rule.isCheckValueAgainstValueSetRule()){
            javaDocAnnotation = ANNOTATION_GENERATED;
            IPolicyCmptTypeAttribute attr = getPcType().getPolicyCmptTypeAttribute(rule.getValidatedAttributeAt(0));
            Datatype attrDatatype = attr.findDatatype();
            body.append('!');
            if(attr.getValueSet().getValueSetType().equals(ValueSetType.ENUM)){
                body.append(productCmptGenInterfaceBuilder.getMethodNameGetAllowedValuesFor(attr, attr.findDatatype()));
            }
            else if(attr.getValueSet().getValueSetType().equals(ValueSetType.RANGE)){
                body.append(productCmptGenInterfaceBuilder.getMethodNameGetRangeFor(attr, attrDatatype));
            }
            body.append("(");
            body.append(parameterBusinessFunction);
            body.append(").contains(");
            body.append(interfaceBuilder.getMethodNameGetPropertyValue(attr.getName(), attrDatatype));
            body.append("()))");
        }
        else{
            body.append("true) ");
        }
        body.appendOpenBracket();
        boolean generateToDo = false;
        body.append("ml.add(");
        body.append(getMethodNameCreateMessageForRule(rule));
        MessageFragment msgFrag = MessageFragment.createMessageFragment(rule.getMessageText(), MessageFragment.VALUES_AS_PARAMETER_NAMES);
        body.append('(');
        if(msgFrag.hasParameters()){
            String[] parameterNames = msgFrag.getParameterNames();
            for (int j = 0; j < parameterNames.length; j++) {
                body.append("null");
                generateToDo = true;
                if(j < parameterNames.length - 1){
                    body.append(", ");
                }
            }
        }

        if(rule.isValidatedAttrSpecifiedInSrc()){
            generateToDo = true;
            if(msgFrag.hasParameters()){
                body.append(", ");
            }
            body.append("new ");
            body.appendClassName(ObjectProperty.class);
            body.append("[0]");
        }
        
        body.append("));");
        if (generateToDo) {
            body.append(getLocalizedToDo(rule, "EXEC_RULE_COMPLETE_CALL_CREATE_MSG", rule.getName()));
        }
        body.appendln();
        body.appendCloseBracket();
        body.appendln(" return true;");
        if(!rule.isCheckValueAgainstValueSetRule()) {
            body.appendln("//end-user-code");
        }
        if(!rule.isAppliedForAllBusinessFunctions()){
            if(businessFunctions.length > 0){
                body.appendCloseBracket();
                body.appendln(" return true;");
            }
        }
        builder.method(java.lang.reflect.Modifier.PROTECTED, Datatype.PRIMITIVE_BOOLEAN.getJavaClassName(),
            getMethodNameExecRule(rule), new String[] { "ml", parameterBusinessFunction },
            new String[] { MessageList.class.getName(), String.class.getName() }, body, javaDoc, javaDocAnnotation);
    }

    /**
     * {@inheritDoc}
     */
    protected void generateCodeForMethodDefinedInModel(IMethod method, Datatype returnType, Datatype[] paramTypes, JavaCodeFragmentBuilder methodsBuilder) throws CoreException {
        if (method.getModifier()==org.faktorips.devtools.core.model.Modifier.PUBLISHED) {
            methodsBuilder.javaDoc(getJavaDocCommentForOverriddenMethod(), ANNOTATION_GENERATED);
        } else {
            methodsBuilder.javaDoc(method.getDescription(), ANNOTATION_GENERATED);
        }
        interfaceBuilder.generateSignatureForMethodDefinedInModel(method, method.getJavaModifier(),
                returnType, paramTypes, methodsBuilder);
        if (method.isAbstract()) {
            methodsBuilder.appendln(";");
            return;
        }
        methodsBuilder.openBracket();
        methodsBuilder.appendln("// TODO implement model method.");
        methodsBuilder.append("throw new RuntimeException(\"Not implemented yet!\");");
        methodsBuilder.closeBracket();
    }
    
    /**
     * protected void initPropertiesFromXml(HashMap propMap) {
     *     if (propMap.containsKey("prop0")) {
     *         prop0 = (String)propMap.get("prop0");
     *     }
     *     if (propMap.containsKey("prop1")) {
     *         prop1 = (String)propMap.get("prop1");
     *     }
     * }
     */
    private void generateMethodInitPropertiesFromXml(JavaCodeFragmentBuilder builder) throws CoreException {

        IPolicyCmptTypeAttribute[] attributes = getPolicyCmptType().getPolicyCmptTypeAttributes();
        ArrayList selectedAttributes = new ArrayList();
        for (int i = 0; i < attributes.length; i++) {
            IPolicyCmptTypeAttribute a = attributes[i];
            if (a.getAttributeType()==AttributeType.DERIVED_ON_THE_FLY 
                    || a.getAttributeType()==AttributeType.CONSTANT
                    || a.getOverwrites()
                    || !a.isValid()) {
                continue;
            }
            selectedAttributes.add(a);
        }
        if(selectedAttributes.isEmpty()){
            return;
        }
        builder.javaDoc(getJavaDocCommentForOverriddenMethod(), ANNOTATION_GENERATED);
        builder.methodBegin(java.lang.reflect.Modifier.PROTECTED, Void.TYPE, MethodNames.INIT_PROPERTIES_FROM_XML, new String[]{"propMap"}, new Class[]{HashMap.class});
        builder.appendln("super." + MethodNames.INIT_PROPERTIES_FROM_XML + "(propMap);");
        for (Iterator it = selectedAttributes.iterator(); it.hasNext();) {
            IPolicyCmptTypeAttribute a = (IPolicyCmptTypeAttribute)it.next();
            Datatype datatype = a.findDatatype();
            DatatypeHelper helper = a.getIpsProject().getDatatypeHelper(datatype);
            builder.append("if (propMap.containsKey(");
            builder.appendQuoted(a.getName());
            builder.appendln(")) {");
            String expr = "(String)propMap.get(\"" + a.getName() + "\")";
            builder.append(getFieldNameForAttribute(a) + " = ");
            builder.append(helper.newInstanceFromExpression(expr));
            builder.appendln(";");
            builder.appendln("}");
            
        }
        builder.appendln(MARKER_BEGIN_USER_CODE);
        builder.appendln(MARKER_END_USER_CODE);
        builder.methodEnd();
    }
    
    /**
     * <pre>
     * protected AbstractPolicyComponent createChildFromXml(Element childEl) {
     *     AbstractPolicyComponent newChild ) super.createChildFromXml(childEl);
     *     if (newChild!=null) {
     *         return newChild;
     *     }
     *     String className = childEl.getAttribute("class");
     *     if (className.length>0) {
     *         try {
     *             AbstractCoverage abstractCoverage = (AbstractCoverage)Class.forName(className).newInstance();
     *             addAbstractCoverage(abstractCoverage);
     *             initialize();
     *         } catch (Exception e) {
     *             throw new RuntimeException(e);
     *         }
     *     }
     *     if ("Coverage".equals(childEl.getNodeName())) {
     *         (AbstractPolicyComponent)return newCovergae();
     *     }
     *     return null;    
     * }
     * </pre>
     */
    private void generateMethodCreateChildFromXml(JavaCodeFragmentBuilder builder) throws CoreException {
        builder.javaDoc(getJavaDocCommentForOverriddenMethod(), ANNOTATION_GENERATED);
        builder.methodBegin(java.lang.reflect.Modifier.PROTECTED, 
                AbstractModelObject.class, 
                MethodNames.CREATE_CHILD_FROM_XML, 
                new String[]{"childEl"}, 
                new Class[]{Element.class});
        
        builder.appendClassName(AbstractModelObject.class);
        builder.append(" newChild = super." + MethodNames.CREATE_CHILD_FROM_XML + "(childEl);");
        builder.appendln("if (newChild!=null) {");
        builder.appendln("return newChild;");
        builder.appendln("}");

        IPolicyCmptTypeAssociation[] relations = getPolicyCmptType().getPolicyCmptTypeAssociations();
        for (int i = 0; i < relations.length; i++) {
            IPolicyCmptTypeAssociation relation = relations[i];
            if (!relation.isCompositionMasterToDetail() 
                    || relation.isDerivedUnion()
                    || !relation.isValid()) {
                continue;
            }
            builder.append("if (");
            builder.appendQuoted(relation.getTargetRoleSingular());
            builder.appendln(".equals(childEl.getNodeName())) {");
            IPolicyCmptType target = relation.findTarget();
            builder.appendln("String className = childEl.getAttribute(\"class\");");
            builder.appendln("if (className.length()>0) {");
            builder.appendln("try {");
            builder.appendClassName(getQualifiedClassName(target));
            String varName = StringUtils.uncapitalise(relation.getTargetRoleSingular());
            builder.append(" " + varName + "=(");
            builder.appendClassName(getQualifiedClassName(target));
            builder.appendln(")Class.forName(className).newInstance();");
            builder.append(interfaceBuilder.getMethodNameAddOrSetObject(relation) + "(" + varName + ");");
            builder.appendln("return " + varName + ";");
            builder.appendln("} catch (Exception e) {");
            builder.appendln("throw new RuntimeException(e);");
            builder.appendln("}"); // catch
            builder.appendln("}"); // if
            if (!target.isAbstract()) {
                builder.append("return (");
                builder.appendClassName(AbstractModelObject.class);
                builder.append(")");
                builder.append(interfaceBuilder.getMethodNameNewChild(relation));
                builder.appendln("();");
            } else {
                builder.appendln("throw new RuntimeException(childEl.toString() + \": Attribute className is missing.\");");
            }
            builder.appendln("}");
        }
        builder.appendln("return null;");
        builder.methodEnd();
    }
    
    /**
     * <pre>
     * protected abstract IUnresolvedReference createUnresolvedReference(
     *     Object objectId,
     *     String targetRole,
     *     String targetId) throws Exception {
     * 
     *     if ("InsuredPeson".equals(targetRole)) {
     *         return new DefaultUnresolvedReference(this, objectId, "setInsuredPerson", IInsuredPerson.class, targetId);
     *     }
     *     return super.createUnresolvedReference(objectId, targetRole, targetId);
     * }
     * </pre>
     */
    private void generateMethodCreateUnresolvedReference(JavaCodeFragmentBuilder builder) throws CoreException {
        builder.javaDoc(getJavaDocCommentForOverriddenMethod(), ANNOTATION_GENERATED);
        String[] argNames = new String[]{"objectId", "targetRole", "targetId"};
        Class[] argClasses= new Class[]{Object.class, String.class, String.class};
        builder.methodBegin(java.lang.reflect.Modifier.PROTECTED, 
                IUnresolvedReference.class, 
                MethodNames.CREATE_UNRESOLVED_REFERENCE, 
                argNames, 
                argClasses, 
                new Class[]{Exception.class});

        IPolicyCmptTypeAssociation[] relations = getPcType().getPolicyCmptTypeAssociations();
        for (int i = 0; i < relations.length; i++) {
            if (!relations[i].isValid() || !relations[i].isAssoziation()) {
                continue;
            }
            IPolicyCmptTypeAssociation association = relations[i];
            String targetClass = interfaceBuilder.getQualifiedClassName(association.findTarget());
            builder.append("if (");
            builder.appendQuoted(association.getTargetRoleSingular());
            builder.append(".equals(targetRole)) {");
            builder.append("return new ");
            builder.appendClassName(DefaultUnresolvedReference.class);
            builder.append("(this, objectId, ");
            builder.appendQuoted(interfaceBuilder.getMethodNameAddOrSetObject(association));
            builder.append(", ");
            builder.appendClassName(targetClass);
            builder.append(".class, targetId);");
            builder.append("}");
        }
        builder.appendln("return super." + MethodNames.CREATE_UNRESOLVED_REFERENCE + "(objectId, targetRole, targetId);");
        builder.methodEnd();
    }
    
    private void generateMethodGetRangeFor(IPolicyCmptTypeAttribute a, DatatypeHelper helper, JavaCodeFragmentBuilder methodBuilder) throws CoreException{
        methodBuilder.javaDoc("{@inheritDoc}", ANNOTATION_GENERATED);
        productCmptGenInterfaceBuilder.generateSignatureGetRangeFor(a, helper, methodBuilder);
        JavaCodeFragment body = new JavaCodeFragment();
        body.appendOpenBracket();
        body.append("return ");
        if(a.isProductRelevant() && getProductCmptType()!=null){
            
            body.append(interfaceBuilder.getMethodNameGetProductCmptGeneration(getProductCmptType()));
            body.append("().");
            body.append(productCmptGenInterfaceBuilder.getMethodNameGetRangeFor(a, helper.getDatatype()));
            body.appendln("(businessFunction);");
        }
        else{
            body.append(interfaceBuilder.getFieldNameMaxRangeFor(a));
            body.appendln(";");
            
        }
        body.appendCloseBracket();
        methodBuilder.append(body);
    }
    
    private void generateMethodGetAllowedValuesFor(IPolicyCmptTypeAttribute a, DatatypeHelper helper, JavaCodeFragmentBuilder methodBuilder) throws CoreException{
        methodBuilder.javaDoc("{@inheritDoc}", ANNOTATION_GENERATED);
        productCmptGenInterfaceBuilder.generateSignatureGetAllowedValuesFor(a, helper.getDatatype(), methodBuilder);
        JavaCodeFragment body = new JavaCodeFragment();
        body.appendOpenBracket();
        body.append("return ");
        if(a.isProductRelevant() && getProductCmptType()!=null) {
            
            body.append(interfaceBuilder.getMethodNameGetProductCmptGeneration(getProductCmptType()));
            body.append("().");
            body.append(productCmptGenInterfaceBuilder.getMethodNameGetAllowedValuesFor(a, helper.getDatatype()));
            body.appendln("(businessFunction);");
        }
        else{
            body.append(interfaceBuilder.getFieldNameMaxAllowedValuesFor(a));
            body.appendln(";");
            
        }
        body.appendCloseBracket();
        methodBuilder.append(body);
    }
    
    private void generateCodeForDependantObjectBaseClass(JavaCodeFragmentBuilder memberVarsBuilder, JavaCodeFragmentBuilder methodBuilder) throws CoreException {
        generateFieldForParent(memberVarsBuilder);
        if (getPcType().isConfigurableByProductCmptType()) {
            generateMethodGetEffectiveFromAsCalendarForDependantObjectBaseClass(methodBuilder);
        }
        generateMethodGetParentModelObject(methodBuilder);
        generateMethodSetParentModelObjectInternal(methodBuilder);
        if (isChangeListenerSupportActive()) {
            generateMethodExistsChangeListenerToBeInformed(methodBuilder);
            generateMethodNotifyChangeListeners(methodBuilder);
        }
    }
    
    /**
     * <pre>
     * private AbstractModelObject parentModelObject;
     * </pre>
     */
    private void generateFieldForParent(JavaCodeFragmentBuilder memberVarsBuilder) {
        String javadoc = getLocalizedText(getPolicyCmptType(), "FIELD_PARENT_JAVADOC");
        memberVarsBuilder.javaDoc(javadoc, ANNOTATION_GENERATED);
        memberVarsBuilder.append("private ");
        memberVarsBuilder.appendClassName(AbstractModelObject.class);
        memberVarsBuilder.append(' ');
        memberVarsBuilder.append(FIELD_PARENT_MODEL_OBJECT);
        memberVarsBuilder.appendln(";");
    }
    
    /**
     * <pre>
     * public IModelObject getParentModelObject() {
     *     return parentModelObject;
     * }
     * </pre>
     */
    private void generateMethodGetParentModelObject(JavaCodeFragmentBuilder methodBuilder) {
        methodBuilder.javaDoc(getJavaDocCommentForOverriddenMethod(), ANNOTATION_GENERATED);
        methodBuilder.methodBegin(Modifier.PUBLIC, IModelObject.class, MethodNames.GET_PARENT, new String[0], new Class[0]);
        methodBuilder.appendln("return " + FIELD_PARENT_MODEL_OBJECT + ";");
        methodBuilder.methodEnd();
    }
    
    /**
     * <pre>
     * public IModelObject getParentModelObject() {
     *     if (parentModelObject!=null && parentModelObject!=newParent) {
     *         parentModelObject.removeChildModelObjectInternal(this);
     *     }
     *     return parentModelObject;
     * }
     * </pre>
     */
    private void generateMethodSetParentModelObjectInternal(JavaCodeFragmentBuilder methodBuilder) {
        methodBuilder.javaDoc(getJavaDocCommentForOverriddenMethod(), ANNOTATION_GENERATED);
        methodBuilder.methodBegin(Modifier.PUBLIC, Void.TYPE, MethodNames.SET_PARENT, 
                new String[]{"newParent"}, new Class[]{AbstractModelObject.class});
        methodBuilder.appendln("if (" + FIELD_PARENT_MODEL_OBJECT + "!=null) {");
        methodBuilder.appendln(FIELD_PARENT_MODEL_OBJECT + "." + MethodNames.REMOVE_CHILD_MODEL_OBJECT_INTERNAL + "(this);");
        methodBuilder.appendln("}");
        methodBuilder.appendln(FIELD_PARENT_MODEL_OBJECT + "=newParent;");
        methodBuilder.methodEnd();
    }
    
    /**
     * <pre>
     * protected boolean existsChangeListenerToBeInformed() {
     *     if (super.existsChangeListenerToBeInformed()) {
     *         return true;
     *     }
     *     if (parentModelObject==null) {
     *         return false;
     *     }
     *     return parentModelObject.existsChangeListenerToBeInformed();
     * }
     * </pre>
     */
    private void generateMethodExistsChangeListenerToBeInformed(JavaCodeFragmentBuilder methodBuilder) {
        methodBuilder.javaDoc(getJavaDocCommentForOverriddenMethod(), ANNOTATION_GENERATED);
        methodBuilder.methodBegin(Modifier.PUBLIC, Boolean.TYPE, MethodNames.EXISTS_CHANGE_LISTENER_TO_BE_INFORMED, 
                new String[]{}, new Class[]{});
        methodBuilder.appendln("if (super." + MethodNames.EXISTS_CHANGE_LISTENER_TO_BE_INFORMED + "()) {");
        methodBuilder.appendln("return true;");
        methodBuilder.appendln("}");
        methodBuilder.appendln("if (" + FIELD_PARENT_MODEL_OBJECT + "==null) {");
        methodBuilder.appendln("return false;");
        methodBuilder.appendln("}");
        methodBuilder.appendln("return " + FIELD_PARENT_MODEL_OBJECT + "." + MethodNames.EXISTS_CHANGE_LISTENER_TO_BE_INFORMED+ "();");
        methodBuilder.methodEnd();
    }
    
    /**
     * <pre>
     * public void notifyChangeListeners(ModelObjectChangedEvent event) {
     *     super.notifyChangeListeners(event);
     *     if (parentModelObject != null) {
     *         parentModelObject.notifyChangeListeners(event);
     *     }
     * }
     * </pre>
     */
    private void generateMethodNotifyChangeListeners(JavaCodeFragmentBuilder methodBuilder) {
        methodBuilder.javaDoc(getJavaDocCommentForOverriddenMethod(), ANNOTATION_GENERATED);
        methodBuilder.methodBegin(Modifier.PUBLIC, Void.TYPE, MethodNames.NOTIFIY_CHANGE_LISTENERS, 
                new String[]{"event"}, new Class[]{IModelObjectChangedEvent.class});
        
        methodBuilder.appendln("super." + MethodNames.NOTIFIY_CHANGE_LISTENERS+ "(event);");
        methodBuilder.appendln("if (" + FIELD_PARENT_MODEL_OBJECT + "!=null) {");
        methodBuilder.appendln(FIELD_PARENT_MODEL_OBJECT + "." + MethodNames.NOTIFIY_CHANGE_LISTENERS + "(event);");
        methodBuilder.appendln("}");
        methodBuilder.methodEnd();
    }

    /**
     * <pre>
     * public Calendar getEffectiveFromAsCalendar() {
     *    IModelObject parent = getParentModelObject();
     *    if (parent instanceof IConfigurableModelObject) {
     *        return ((IConfigurableModelObject)parent).getEffectiveFromAsCalendar();
     *    }
     *    return null;
     * }
     * </pre>
     */
    protected void generateMethodGetEffectiveFromAsCalendarForDependantObjectBaseClass(JavaCodeFragmentBuilder methodsBuilder) throws CoreException {
        methodsBuilder.javaDoc(getJavaDocCommentForOverriddenMethod(), ANNOTATION_GENERATED);
        methodsBuilder.methodBegin(java.lang.reflect.Modifier.PUBLIC, Calendar.class, MethodNames.GET_EFFECTIVE_FROM_AS_CALENDAR, new String[0], new Class[0]);
        methodsBuilder.append("if (" + FIELD_PARENT_MODEL_OBJECT + " instanceof ");
        methodsBuilder.appendClassName(IConfigurableModelObject.class);
        methodsBuilder.append(") {");
        methodsBuilder.append("return ((");
        methodsBuilder.appendClassName(IConfigurableModelObject.class);
        methodsBuilder.appendln(")" + FIELD_PARENT_MODEL_OBJECT + ")." + MethodNames.GET_EFFECTIVE_FROM_AS_CALENDAR + "();");
        methodsBuilder.appendln("}");
        methodsBuilder.appendln("return null;");
        methodsBuilder.methodEnd();
    }

    /**
     * <pre>
     * public IMotorCoverage getMotorCoverage(int index) {
     *      return (IMotorCoverage)motorCoverages.get(index);
     * }
     * </pre>
     */
    protected void generateMethodGetRefObjectAtIndex(IPolicyCmptTypeAssociation relation, JavaCodeFragmentBuilder methodBuilder) throws CoreException{
        String className = interfaceBuilder.getQualifiedClassName(relation.findTarget());
        String field = getFieldNameForRelation(relation);
        interfaceBuilder.generateSignatureGetRefObjectAtIndex(relation, methodBuilder);
        methodBuilder.openBracket();
        methodBuilder.append("return (");
        methodBuilder.appendClassName(className);
        methodBuilder.append(')');
        methodBuilder.append(field);
        methodBuilder.append(".get(index);");
        methodBuilder.closeBracket();
    }
   
}