/*******************************************************************************
 * Copyright (c) 2005-2012 Faktor Zehn AG und andere.
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

package org.faktorips.abstracttest.builder;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.faktorips.codegen.DatatypeHelper;
import org.faktorips.codegen.JavaCodeFragment;
import org.faktorips.devtools.core.builder.DefaultBuilderSet;
import org.faktorips.devtools.core.builder.GenericBuilderKindId;
import org.faktorips.devtools.core.model.enums.EnumTypeDatatypeAdapter;
import org.faktorips.devtools.core.model.ipsproject.IBuilderKindId;
import org.faktorips.devtools.core.model.ipsproject.IIpsArtefactBuilder;
import org.faktorips.devtools.core.model.ipsproject.IIpsPackageFragmentRoot;
import org.faktorips.devtools.core.model.productcmpt.IExpression;
import org.faktorips.devtools.core.model.tablestructure.ITableAccessFunction;
import org.faktorips.fl.CompilationResult;
import org.faktorips.fl.ExprCompiler;
import org.faktorips.fl.IdentifierResolver;

public class TestIpsArtefactBuilderSet extends DefaultBuilderSet {

    public final static String ID = "testbuilderset";

    private boolean inverseRelationLinkRequiredFor2WayCompositions;

    private boolean roleNamePluralRequiredForTo1Relations;

    private boolean isAggregateRootBuilder;

    private final LinkedHashMap<IBuilderKindId, IIpsArtefactBuilder> ipsArtefactBuilders;

    /**
     * You can put any object for any key into this map. Some of the test methods in this test
     * builder set use this map to return test results instead of null. Just put the expected method
     * parameter into this map and the method would return the corresponding value.
     * 
     */
    public Map<Object, Object> testObjectsMap = new HashMap<Object, Object>();

    public TestIpsArtefactBuilderSet() throws CoreException {
        this(new IIpsArtefactBuilder[0]);
    }

    public TestIpsArtefactBuilderSet(IIpsArtefactBuilder[] builders) throws CoreException {
        super();
        ipsArtefactBuilders = new LinkedHashMap<IBuilderKindId, IIpsArtefactBuilder>();
        for (IIpsArtefactBuilder ipsArtefactBuilder : builders) {
            ipsArtefactBuilders.put(new GenericBuilderKindId(), ipsArtefactBuilder);
        }
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(CONFIG_PROPERTY_GENERATOR_LOCALE, Locale.GERMAN.getLanguage());
        TestBuilderSetConfig config = new TestBuilderSetConfig(properties);
        initialize(config);
    }

    @Override
    public TestBuilderSetConfig getConfig() {
        return (TestBuilderSetConfig)super.getConfig();
    }

    @Override
    protected LinkedHashMap<IBuilderKindId, IIpsArtefactBuilder> createBuilders() throws CoreException {
        return ipsArtefactBuilders;
    }

    public void setAggregateRootBuilder(boolean enable) {
        isAggregateRootBuilder = enable;
    }

    @Override
    public boolean containsAggregateRootBuilder() {
        return isAggregateRootBuilder;
    }

    @Override
    public boolean isInverseRelationLinkRequiredFor2WayCompositions() {
        return inverseRelationLinkRequiredFor2WayCompositions;
    }

    public void setInverseRelationLinkRequiredFor2WayCompositions(boolean inverseRelationLinkRequiredFor2WayCompositions) {
        this.inverseRelationLinkRequiredFor2WayCompositions = inverseRelationLinkRequiredFor2WayCompositions;
    }

    @Override
    public boolean isRoleNamePluralRequiredForTo1Relations() {
        return roleNamePluralRequiredForTo1Relations;
    }

    public void setRoleNamePluralRequiredForTo1Relations(boolean roleNamePluralRequiredForTo1Relations) {
        this.roleNamePluralRequiredForTo1Relations = roleNamePluralRequiredForTo1Relations;
    }

    @Override
    public void setId(String id) {

    }

    @Override
    public void setLabel(String label) {

    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getLabel() {
        return getId();
    }

    @Override
    public boolean isSupportTableAccess() {
        return false;
    }

    @Override
    public CompilationResult<JavaCodeFragment> getTableAccessCode(String tableContentsQualifiedName,
            ITableAccessFunction fct,
            CompilationResult<JavaCodeFragment>[] argResults) throws CoreException {
        return null;
    }

    @Override
    public boolean isSupportFlIdentifierResolver() {
        return false;
    }

    @Override
    public IdentifierResolver<JavaCodeFragment> createFlIdentifierResolver(IExpression formula,
            ExprCompiler<JavaCodeFragment> exprCompiler) throws CoreException {
        return new TestParameterIdentifierResolver(formula, exprCompiler);
    }

    @Override
    public IFile getRuntimeRepositoryTocFile(IIpsPackageFragmentRoot root) {
        return (IFile)testObjectsMap.get(root);
    }

    public String getTocFilePackageName(IIpsPackageFragmentRoot root) {
        return (String)testObjectsMap.get(root);
    }

    @Override
    public String getRuntimeRepositoryTocResourceName(IIpsPackageFragmentRoot root) {
        return (String)testObjectsMap.get(root);
    }

    @Override
    public String getVersion() {
        return null;
    }

    @Override
    public DatatypeHelper getDatatypeHelperForEnumType(EnumTypeDatatypeAdapter datatypeAdapter) {
        return (DatatypeHelper)testObjectsMap.get(datatypeAdapter);
    }

    private final class TestParameterIdentifierResolver extends AbstractParameterIdentifierResolver {
        private TestParameterIdentifierResolver(IExpression formula2, ExprCompiler<JavaCodeFragment> exprCompiler) {
            super(formula2, exprCompiler);
        }

        @Override
        protected String getParameterAttributGetterName(IAttribute attribute, Datatype datatype) {
            return attribute.getIpsProject().getJavaNamingConvention()
                    .getGetterMethodName(attribute.getName(), datatype);
        }

        @Override
        protected String getParameterAttributDefaultValueGetterName(IAttribute attribute, Datatype datatype) {
            return "getDefaultValue" + StringUtils.capitalize(attribute.getName()); //$NON-NLS-1$
        }

        @Override
        protected String getAssociationTargetGetterName(IAssociation association, IPolicyCmptType policyCmptType) {
            return association.getIpsProject().getJavaNamingConvention()
                    .getGetterMethodName(association.getName(), policyCmptType);
        }

        @Override
        protected String getAssociationTargetAtIndexGetterName(IAssociation association, IPolicyCmptType policyCmptType) {
            return association.getIpsProject().getJavaNamingConvention()
                    .getGetterMethodName(association.getName(), policyCmptType);
        }

        @Override
        protected String getAssociationTargetsGetterName(IAssociation association, IPolicyCmptType policyCmptType) {
            return association.getIpsProject().getJavaNamingConvention()
                    .getGetterMethodName(association.getTargetRolePlural(), policyCmptType);
        }

        @Override
        protected String getJavaClassName(IType type) {
            return type.getQualifiedName();
        }
    }

}
