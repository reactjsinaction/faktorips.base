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

package org.faktorips.devtools.stdbuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.IJavaElement;
import org.faktorips.codegen.DatatypeHelper;
import org.faktorips.codegen.JavaCodeFragment;
import org.faktorips.datatype.Datatype;
import org.faktorips.devtools.core.ExtensionPoints;
import org.faktorips.devtools.core.IpsStatus;
import org.faktorips.devtools.core.builder.AbstractParameterIdentifierResolver;
import org.faktorips.devtools.core.builder.DefaultBuilderSet;
import org.faktorips.devtools.core.builder.ExtendedExprCompiler;
import org.faktorips.devtools.core.builder.GenericBuilderKindId;
import org.faktorips.devtools.core.builder.JavaSourceFileBuilder;
import org.faktorips.devtools.core.builder.naming.BuilderAspect;
import org.faktorips.devtools.core.exception.CoreRuntimeException;
import org.faktorips.devtools.core.model.enums.EnumTypeDatatypeAdapter;
import org.faktorips.devtools.core.model.ipsobject.IIpsObjectPartContainer;
import org.faktorips.devtools.core.model.ipsobject.IIpsSrcFile;
import org.faktorips.devtools.core.model.ipsobject.IpsObjectType;
import org.faktorips.devtools.core.model.ipsproject.IBuilderKindId;
import org.faktorips.devtools.core.model.ipsproject.IIpsArtefactBuilder;
import org.faktorips.devtools.core.model.ipsproject.IIpsArtefactBuilderSetConfig;
import org.faktorips.devtools.core.model.ipsproject.IIpsSrcFolderEntry;
import org.faktorips.devtools.core.model.pctype.IPolicyCmptType;
import org.faktorips.devtools.core.model.pctype.IPolicyCmptTypeAttribute;
import org.faktorips.devtools.core.model.productcmpt.IExpression;
import org.faktorips.devtools.core.model.productcmpttype.IProductCmptType;
import org.faktorips.devtools.core.model.productcmpttype.IProductCmptTypeAttribute;
import org.faktorips.devtools.core.model.tablestructure.ITableAccessFunction;
import org.faktorips.devtools.core.model.tablestructure.ITableStructure;
import org.faktorips.devtools.core.model.type.IAssociation;
import org.faktorips.devtools.core.model.type.IAttribute;
import org.faktorips.devtools.core.model.type.IType;
import org.faktorips.devtools.stdbuilder.bf.BusinessFunctionBuilder;
import org.faktorips.devtools.stdbuilder.enumtype.EnumContentBuilder;
import org.faktorips.devtools.stdbuilder.enumtype.EnumPropertyBuilder;
import org.faktorips.devtools.stdbuilder.enumtype.EnumTypeBuilder;
import org.faktorips.devtools.stdbuilder.enumtype.EnumXmlAdapterBuilder;
import org.faktorips.devtools.stdbuilder.persistence.EclipseLink1PersistenceProvider;
import org.faktorips.devtools.stdbuilder.persistence.GenericJPA2PersistenceProvider;
import org.faktorips.devtools.stdbuilder.persistence.IPersistenceProvider;
import org.faktorips.devtools.stdbuilder.policycmpttype.PolicyCmptImplClassJaxbAnnGenFactory;
import org.faktorips.devtools.stdbuilder.policycmpttype.persistence.PolicyCmptImplClassJpaAnnGenFactory;
import org.faktorips.devtools.stdbuilder.policycmpttype.validationrule.ValidationRuleMessagesPropertiesBuilder;
import org.faktorips.devtools.stdbuilder.productcmpt.ProductCmptBuilder;
import org.faktorips.devtools.stdbuilder.productcmpt.ProductCmptXMLBuilder;
import org.faktorips.devtools.stdbuilder.table.TableContentBuilder;
import org.faktorips.devtools.stdbuilder.table.TableImplBuilder;
import org.faktorips.devtools.stdbuilder.table.TableRowBuilder;
import org.faktorips.devtools.stdbuilder.testcase.TestCaseBuilder;
import org.faktorips.devtools.stdbuilder.testcasetype.TestCaseTypeClassBuilder;
import org.faktorips.devtools.stdbuilder.xpand.GeneratorModelContext;
import org.faktorips.devtools.stdbuilder.xpand.XpandBuilder;
import org.faktorips.devtools.stdbuilder.xpand.model.AbstractGeneratorModelNode;
import org.faktorips.devtools.stdbuilder.xpand.model.ModelService;
import org.faktorips.devtools.stdbuilder.xpand.policycmpt.PolicyCmptClassBuilder;
import org.faktorips.devtools.stdbuilder.xpand.policycmpt.model.XPolicyAssociation;
import org.faktorips.devtools.stdbuilder.xpand.policycmpt.model.XPolicyAttribute;
import org.faktorips.devtools.stdbuilder.xpand.policycmpt.model.XPolicyCmptClass;
import org.faktorips.devtools.stdbuilder.xpand.productcmpt.ProductCmptClassBuilder;
import org.faktorips.devtools.stdbuilder.xpand.productcmpt.ProductCmptGenerationClassBuilder;
import org.faktorips.devtools.stdbuilder.xpand.productcmpt.model.XProductAttribute;
import org.faktorips.devtools.stdbuilder.xpand.productcmpt.model.XProductCmptClass;
import org.faktorips.devtools.stdbuilder.xpand.productcmpt.model.XProductCmptGenerationClass;
import org.faktorips.fl.CompilationResult;
import org.faktorips.fl.ExprCompiler;
import org.faktorips.fl.IdentifierResolver;
import org.faktorips.fl.CompilationResultImpl;
import org.faktorips.runtime.ICopySupport;
import org.faktorips.runtime.IDeltaSupport;
import org.faktorips.runtime.internal.MethodNames;
import org.faktorips.util.ArgumentCheck;

/**
 * An <code>IpsArtefactBuilderSet</code> implementation that assembles the standard Faktor-IPS
 * <tt>IIpsArtefactBuilder</tt>s.
 * 
 * @author Peter Erzberger
 */
public class StandardBuilderSet extends DefaultBuilderSet {

    private static final String EXTENSION_POINT_ARTEFACT_BUILDER_FACTORY = "artefactBuilderFactory";

    public final static String ID = "org.faktorips.devtools.stdbuilder.ipsstdbuilderset";

    /**
     * Configuration property that enables/disables the generation of a copy method.
     * 
     * @see ICopySupport
     */
    public final static String CONFIG_PROPERTY_GENERATE_COPY_SUPPORT = "generateCopySupport"; //$NON-NLS-1$

    /**
     * Configuration property that enables/disables the generation of delta computation.
     * 
     * @see IDeltaSupport
     */
    public final static String CONFIG_PROPERTY_GENERATE_DELTA_SUPPORT = "generateDeltaSupport"; //$NON-NLS-1$

    /**
     * Configuration property that enables/disables the generation of the visitor support.
     * 
     * @see IDeltaSupport
     */
    public final static String CONFIG_PROPERTY_GENERATE_VISITOR_SUPPORT = "generateVisitorSupport"; //$NON-NLS-1$

    /**
     * Configuration property that is supposed to be used to read a configuration value from the
     * IIpsArtefactBuilderSetConfig object provided by the initialize method of an
     * IIpsArtefactBuilderSet instance.
     */
    public final static String CONFIG_PROPERTY_GENERATE_CHANGELISTENER = "generateChangeListener"; //$NON-NLS-1$

    /**
     * Configuration property that enables/disables the use of enums, if supported by the target
     * java version.
     */
    public final static String CONFIG_PROPERTY_USE_ENUMS = "useJavaEnumTypes"; //$NON-NLS-1$

    /**
     * Configuration property that enables/disables the generation of JAXB support.
     */
    public final static String CONFIG_PROPERTY_GENERATE_JAXB_SUPPORT = "generateJaxbSupport"; //$NON-NLS-1$

    /**
     * Configuration property contains the persistence provider implementation.
     */
    public final static String CONFIG_PROPERTY_PERSISTENCE_PROVIDER = "persistenceProvider"; //$NON-NLS-1$

    /**
     * Configuration property contains the kind of formula compiling.
     */
    public final static String CONFIG_PROPERTY_FORMULA_COMPILING = "formulaCompiling"; //$NON-NLS-1$

    /**
     * Name of the configuration property that indicates whether toXml() methods should be
     * generated.
     */
    public final static String CONFIG_PROPERTY_TO_XML_SUPPORT = "toXMLSupport"; //$NON-NLS-1$

    /**
     * Name of the configuration property that indicates whether to generate camel case constant
     * names with underscore separator or without. For example if this property is true, the
     * constant for the name checkAnythingRule would be generated as CHECK_ANYTHING_RULE, if the
     * property is false the constant name would be CHECKANYTHINGRUL.
     */
    public final static String CONFIG_PROPERTY_CAMELCASE_SEPARATED = "camelCaseSeparated"; //$NON-NLS-1$

    private ModelService modelService;

    private GeneratorModelContext generatorModelContext;

    private Map<String, CachedPersistenceProvider> allSupportedPersistenceProvider;

    private final String version;

    private final AnnotationGeneratorFactory[] annotationGeneratorFactories;

    private Map<AnnotatedJavaElementType, List<IAnnotationGenerator>> annotationGeneratorsMap;

    public StandardBuilderSet() {
        annotationGeneratorFactories = new AnnotationGeneratorFactory[] { new PolicyCmptImplClassJpaAnnGenFactory(), // JPA
                                                                                                                     // support
                new PolicyCmptImplClassJaxbAnnGenFactory() }; // Jaxb support

        initSupportedPersistenceProviderMap();

        version = "3.0.0"; //$NON-NLS-1$
        // Following code sections sets the version to the stdbuilder-plugin/bundle version.
        // Most of the time we hardwire the version of the generated code here, but from time to
        // time
        // we want to sync it with the plugin version, so the code remains here.
        //
        // Version versionObj =
        // Version.parseVersion((String)StdBuilderPlugin.getDefault().getBundle(
        // ).getHeaders().get(org
        // .osgi.framework.Constants.BUNDLE_VERSION));
        // StringBuffer buf = new StringBuffer();
        // buf.append(versionObj.getMajor());
        // buf.append('.');
        // buf.append(versionObj.getMinor());
        // buf.append('.');
        // buf.append(versionObj.getMicro());
        // version = buf.toString();

    }

    @Override
    public void clean(IProgressMonitor monitor) {
        super.clean(monitor);
        modelService = new ModelService();
    }

    @Override
    public boolean isSupportTableAccess() {
        return true;
    }

    @Override
    public CompilationResult<JavaCodeFragment> getTableAccessCode(String tableContentsQualifiedName,
            ITableAccessFunction fct,
            CompilationResult<JavaCodeFragment>[] argResults) throws CoreException {

        Datatype returnType = fct.getIpsProject().findDatatype(fct.getType());
        JavaCodeFragment code = new JavaCodeFragment();
        ITableStructure tableStructure = fct.getTableStructure();

        CompilationResultImpl result = new CompilationResultImpl(code, returnType);
        code.appendClassName(getTableImplBuilder().getQualifiedClassName(tableStructure.getIpsSrcFile()));
        // create get instance method by using the qualified name of the table content
        code.append(".getInstance(" + MethodNames.GET_THIS_REPOSITORY + "(), \"" + tableContentsQualifiedName //$NON-NLS-1$ //$NON-NLS-2$
                + "\").findRowNullRowReturnedForEmtpyResult("); //$NON-NLS-1$

        for (int i = 0; i < argResults.length; i++) {
            if (i > 0) {
                code.append(", "); //$NON-NLS-1$
            }
            code.append(argResults[i].getCodeFragment());
            result.addMessages(argResults[i].getMessages());
        }
        code.append(").get"); //$NON-NLS-1$
        code.append(StringUtils.capitalize(fct.findAccessedColumn().getName()));
        code.append("()"); //$NON-NLS-1$

        return result;
    }

    @Override
    public IdentifierResolver<JavaCodeFragment> createFlIdentifierResolver(IExpression formula,
            ExprCompiler<JavaCodeFragment> exprCompiler) throws CoreException {
        return new StandardParameterIdentifierResolver(formula, exprCompiler);
    }

    @Override
    public boolean isSupportFlIdentifierResolver() {
        return true;
    }

    @Override
    public void initialize(IIpsArtefactBuilderSetConfig config) throws CoreException {
        createAnnotationGeneratorMap();
        modelService = new ModelService();
        generatorModelContext = new GeneratorModelContext(config, this, getAnnotationGenerators());
        super.initialize(config);
    }

    @Override
    protected LinkedHashMap<IBuilderKindId, IIpsArtefactBuilder> createBuilders() throws CoreException {
        // create policy component type builders
        LinkedHashMap<IBuilderKindId, IIpsArtefactBuilder> builders = new LinkedHashMap<IBuilderKindId, IIpsArtefactBuilder>();
        builders.put(BuilderKindIds.POLICY_CMPT_TYPE_INTERFACE, new PolicyCmptClassBuilder(true, this,
                generatorModelContext, modelService));
        PolicyCmptClassBuilder policyCmptClassBuilder = new PolicyCmptClassBuilder(false, this, generatorModelContext,
                modelService);
        builders.put(BuilderKindIds.POLICY_CMPT_TYPE_IMPLEMEMENTATION, policyCmptClassBuilder);

        // create product component type builders
        builders.put(BuilderKindIds.PRODUCT_CMPT_TYPE_INTERFACE, new ProductCmptClassBuilder(true, this,
                generatorModelContext, modelService));
        ProductCmptClassBuilder productCmptClassBuilder = new ProductCmptClassBuilder(false, this,
                generatorModelContext, modelService);
        builders.put(BuilderKindIds.PRODUCT_CMPT_TYPE_IMPLEMEMENTATION, productCmptClassBuilder);
        builders.put(BuilderKindIds.PRODUCT_CMPT_TYPE_GENERATION_INTERFACE, new ProductCmptGenerationClassBuilder(true,
                this, generatorModelContext, modelService));
        ProductCmptGenerationClassBuilder productCmptGenerationClassBuilder = new ProductCmptGenerationClassBuilder(
                false, this, generatorModelContext, modelService);
        builders.put(BuilderKindIds.PRODUCT_CMPT_TYPE_GENERATION_IMPLEMEMENTATION, productCmptGenerationClassBuilder);

        // table structure builders
        TableImplBuilder tableImplBuilder = new TableImplBuilder(this);
        builders.put(BuilderKindIds.TABLE, tableImplBuilder);
        TableRowBuilder tableRowBuilder = new TableRowBuilder(this);
        builders.put(BuilderKindIds.TABLE_ROW, tableRowBuilder);
        tableImplBuilder.setTableRowBuilder(tableRowBuilder);

        // table content builders
        builders.put(BuilderKindIds.TABLE_CONTENT, new TableContentBuilder(this));

        // test case type builders
        builders.put(BuilderKindIds.TEST_CASE_TYPE, new TestCaseTypeClassBuilder(this));

        // test case builder
        TestCaseBuilder testCaseBuilder = new TestCaseBuilder(this);
        builders.put(BuilderKindIds.TEST_CASE, testCaseBuilder);

        // toc file builder
        TocFileBuilder tocFileBuilder = new TocFileBuilder(this);
        builders.put(BuilderKindIds.TOC_FILE, tocFileBuilder);

        builders.put(BuilderKindIds.BUSINESS_FUNCTION, new BusinessFunctionBuilder(this));
        // New enum type builder
        EnumTypeBuilder enumTypeBuilder = new EnumTypeBuilder(this);
        builders.put(BuilderKindIds.ENUM_TYPE, enumTypeBuilder);
        builders.put(BuilderKindIds.ENUM_XML_ADAPTER, new EnumXmlAdapterBuilder(this, enumTypeBuilder));
        builders.put(BuilderKindIds.ENUM_CONTENT, new EnumContentBuilder(this));
        builders.put(BuilderKindIds.ENUM_PROPERTY, new EnumPropertyBuilder(this));

        // product component builders
        ProductCmptBuilder productCmptBuilder = new ProductCmptBuilder(this);
        builders.put(BuilderKindIds.PRODUCT_CMPT_IMPLEMENTATION, productCmptBuilder);
        IIpsArtefactBuilder productCmptXmlBuilder = new ProductCmptXMLBuilder(IpsObjectType.PRODUCT_CMPT, this);
        builders.put(BuilderKindIds.PRODUCT_CMPT_XML, productCmptXmlBuilder);

        productCmptBuilder.setProductCmptImplBuilder(productCmptClassBuilder);
        productCmptBuilder.setProductCmptGenImplBuilder(productCmptGenerationClassBuilder);

        // test case builder
        testCaseBuilder.setJavaSourceFileBuilder(policyCmptClassBuilder);

        builders.put(BuilderKindIds.VALIDATION_RULE_MESSAGES, new ValidationRuleMessagesPropertiesBuilder(this));

        List<IIpsArtefactBuilder> extendingBuilders = getExtendingArtefactBuilders();
        for (IIpsArtefactBuilder ipsArtefactBuilder : extendingBuilders) {
            GenericBuilderKindId id = new GenericBuilderKindId(ipsArtefactBuilder.getName());
            if (builders.containsKey(id)) {
                id = new GenericBuilderKindId();
            }
            builders.put(id, ipsArtefactBuilder);
        }

        builders.put(BuilderKindIds.POLICY_CMPT_MODEL_TYPE, new ModelTypeXmlBuilder(IpsObjectType.POLICY_CMPT_TYPE,
                this));
        builders.put(BuilderKindIds.PRODUCT_CMPT_MODEL_TYPE, new ModelTypeXmlBuilder(IpsObjectType.PRODUCT_CMPT_TYPE,
                this));
        tocFileBuilder.setGenerateEntriesForModelTypes(true);

        return builders;
    }

    /**
     * Returns all builders registered with the standard builder set through the extension point
     * "artefactBuilder".
     * 
     * @return a list containing all builders that extend this builder set.
     */
    private List<IIpsArtefactBuilder> getExtendingArtefactBuilders() {
        List<IIpsArtefactBuilder> builders = new ArrayList<IIpsArtefactBuilder>();

        ExtensionPoints extensionPoints = new ExtensionPoints(StdBuilderPlugin.PLUGIN_ID);
        IExtension[] extensions = extensionPoints.getExtension(EXTENSION_POINT_ARTEFACT_BUILDER_FACTORY);
        for (IExtension extension : extensions) {
            IConfigurationElement[] configurationElements = extension.getConfigurationElements();
            for (IConfigurationElement configElement : configurationElements) {
                if (EXTENSION_POINT_ARTEFACT_BUILDER_FACTORY.equals(configElement.getName())) {
                    IIpsArtefactBuilderFactory builderFactory = ExtensionPoints.createExecutableExtension(extension,
                            configElement, "class", IIpsArtefactBuilderFactory.class); //$NON-NLS-1$
                    IIpsArtefactBuilder builder = builderFactory.createBuilder(this);
                    builders.add(builder);
                }
            }
        }
        return builders;
    }

    private void createAnnotationGeneratorMap() throws CoreException {
        annotationGeneratorsMap = new HashMap<AnnotatedJavaElementType, List<IAnnotationGenerator>>();
        List<AnnotationGeneratorFactory> factories = getAnnotationGeneratorFactoriesRequiredForProject();

        for (AnnotatedJavaElementType type : AnnotatedJavaElementType.values()) {
            ArrayList<IAnnotationGenerator> annotationGenerators = new ArrayList<IAnnotationGenerator>();
            for (AnnotationGeneratorFactory annotationGeneratorFactory : factories) {
                IAnnotationGenerator annotationGenerator = annotationGeneratorFactory.createAnnotationGenerator(type);
                if (annotationGenerator == null) {
                    continue;
                }
                annotationGenerators.add(annotationGenerator);
            }
            annotationGeneratorsMap.put(type, annotationGenerators);
        }
    }

    private List<AnnotationGeneratorFactory> getAnnotationGeneratorFactoriesRequiredForProject() {
        List<AnnotationGeneratorFactory> factories = new ArrayList<AnnotationGeneratorFactory>();
        for (AnnotationGeneratorFactory annotationGeneratorFactorie : annotationGeneratorFactories) {
            if (annotationGeneratorFactorie.isRequiredFor(getIpsProject())) {
                factories.add(annotationGeneratorFactorie);
            }
        }
        return factories;
    }

    /**
     * Returns the map of annotation generators used to provide annotations to generated elements.
     * 
     * @return The annotation generator map.
     */
    public Map<AnnotatedJavaElementType, List<IAnnotationGenerator>> getAnnotationGenerators() {
        return annotationGeneratorsMap;
    }

    @Override
    public DatatypeHelper getDatatypeHelperForEnumType(EnumTypeDatatypeAdapter datatypeAdapter) {
        return new EnumTypeDatatypeHelper(getEnumTypeBuilder(), datatypeAdapter);
    }

    /**
     * Returns the standard builder plugin version in the format [major.minor.micro]. The version
     * qualifier is not included in the version string.
     */
    @Override
    public String getVersion() {
        return version;
    }

    /**
     * Returns whether Java5 enums shall be used in the code generated by this builder.
     */
    public boolean isUseEnums() {
        return getConfig().getPropertyValueAsBoolean(StandardBuilderSet.CONFIG_PROPERTY_USE_ENUMS).booleanValue();
    }

    /**
     * Returns if Java 5 typesafe collections shall be used in the code generated by this builder.
     */
    public boolean isUseTypesafeCollections() {
        return true;
    }

    /**
     * Returns whether JAXB support is to be generated by this builder.
     */
    public boolean isGenerateJaxbSupport() {
        return getConfig().getPropertyValueAsBoolean(CONFIG_PROPERTY_GENERATE_JAXB_SUPPORT);
    }

    /**
     * Returns whether toXml() methods are to be generated.
     */
    public boolean isGenerateToXmlSupport() {
        return generatorModelContext.isGenerateToXmlSupport();
    }

    /**
     * Returns whether to generate camel case constant names with underscore separator or without.
     * For example if this property is true, the constant for the property
     * checkAnythingAndDoSomething would be generated as CHECK_ANYTHING_AND_DO_SOMETHING, if the
     * property is false the constant name would be CHECKANYTHINGANDDOSOMETHING.
     */
    public boolean isGenerateSeparatedCamelCase() {
        return generatorModelContext.isGenerateSeparatedCamelCase();
    }

    public FormulaCompiling getFormulaCompiling() {
        String kind = getConfig().getPropertyValueAsString(CONFIG_PROPERTY_FORMULA_COMPILING);
        try {
            return FormulaCompiling.valueOf(kind);
        } catch (Exception e) {
            // if value is not set correctly we use Both as default value
            return FormulaCompiling.Both;
        }
    }

    private void initSupportedPersistenceProviderMap() {
        allSupportedPersistenceProvider = new HashMap<String, CachedPersistenceProvider>(2);
        allSupportedPersistenceProvider.put(IPersistenceProvider.PROVIDER_IMPLEMENTATION_ECLIPSE_LINK_1_1,
                CachedPersistenceProvider.create(EclipseLink1PersistenceProvider.class));
        allSupportedPersistenceProvider.put(IPersistenceProvider.PROVIDER_IMPLEMENTATION_GENERIC_JPA_2_0,
                CachedPersistenceProvider.create(GenericJPA2PersistenceProvider.class));
    }

    @Override
    public boolean isPersistentProviderSupportConverter() {
        IPersistenceProvider persistenceProviderImpl = getPersistenceProviderImplementation();
        return persistenceProviderImpl != null && getPersistenceProviderImplementation().isSupportingConverters();
    }

    @Override
    public boolean isPersistentProviderSupportOrphanRemoval() {
        IPersistenceProvider persistenceProviderImpl = getPersistenceProviderImplementation();
        return persistenceProviderImpl != null && getPersistenceProviderImplementation().isSupportingOrphanRemoval();
    }

    /**
     * Returns the persistence provider or <code>null</code> if no
     */
    public IPersistenceProvider getPersistenceProviderImplementation() {
        String persistenceProviderKey = (String)getConfig().getPropertyValue(CONFIG_PROPERTY_PERSISTENCE_PROVIDER);
        if (StringUtils.isEmpty(persistenceProviderKey) || "none".equalsIgnoreCase(persistenceProviderKey)) {
            return null;
        }
        CachedPersistenceProvider pProviderCached = allSupportedPersistenceProvider.get(persistenceProviderKey);
        if (pProviderCached == null) {
            StdBuilderPlugin.log(new IpsStatus(IStatus.WARNING,
                    "Unknow persistence provider  \"" + persistenceProviderKey //$NON-NLS-1$
                            + "\". Supported provider are: " + allSupportedPersistenceProvider.keySet().toString()));
            return null;
        }

        if (pProviderCached.cachedProvider == null) {
            try {
                pProviderCached.cachedProvider = pProviderCached.persistenceProviderClass.newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return pProviderCached.cachedProvider;
    }

    public String getJavaClassName(Datatype datatype) {
        return getJavaClassName(datatype, true);
    }

    public String getJavaClassName(Datatype datatype, boolean interfaces) {
        if (datatype instanceof IPolicyCmptType) {
            return getModelNode((IPolicyCmptType)datatype, XPolicyCmptClass.class).getQualifiedName(
                    BuilderAspect.getValue(interfaces));
        } else

        if (datatype instanceof IProductCmptType) {
            return modelService.getModelNode((IProductCmptType)datatype, XProductCmptGenerationClass.class,
                    generatorModelContext).getQualifiedName(BuilderAspect.getValue(interfaces));
        } else {
            return datatype.getJavaClassName();
        }
    }

    /**
     * Returns a list containing all <tt>IJavaElement</tt>s this builder set generates for the given
     * <tt>IIpsObjectPartContainer</tt>.
     * <p>
     * Returns an empty list if no <tt>IJavaElement</tt>s are generated for the provided
     * <tt>IIpsObjectPartContainer</tt>.
     * <p>
     * The IPS model should be completely valid if calling this method or else the results may not
     * be exhaustive.
     * 
     * @param ipsObjectPartContainer The <tt>IIpsObjectPartContainer</tt> to obtain the generated
     *            <tt>IJavaElement</tt>s for.
     * 
     * @throws NullPointerException If the parameter is null
     */
    public List<IJavaElement> getGeneratedJavaElements(IIpsObjectPartContainer ipsObjectPartContainer) {
        ArgumentCheck.notNull(ipsObjectPartContainer);

        List<IJavaElement> javaElements = new ArrayList<IJavaElement>();
        for (IIpsArtefactBuilder builder : getArtefactBuilders()) {
            if (builder instanceof ProductCmptBuilder) {
                builder = ((ProductCmptBuilder)builder).getGenerationBuilder();
            }
            if (!(builder instanceof JavaSourceFileBuilder)) {
                continue;
            }
            JavaSourceFileBuilder javaBuilder = (JavaSourceFileBuilder)builder;
            IIpsSrcFile ipsSrcFile = (IIpsSrcFile)ipsObjectPartContainer.getAdapter(IIpsSrcFile.class);
            try {
                if (javaBuilder.isBuilderFor(ipsSrcFile)) {
                    javaElements.addAll(javaBuilder.getGeneratedJavaElements(ipsObjectPartContainer));
                } else if (javaBuilder instanceof XpandBuilder<?>) {
                    XpandBuilder<?> xpandBuilder = (XpandBuilder<?>)javaBuilder;
                    if (xpandBuilder.isGenerateingArtifactsFor(ipsObjectPartContainer)) {
                        javaElements.addAll(xpandBuilder.getGeneratedJavaElements(ipsObjectPartContainer));
                    }
                }

            } catch (CoreException e) {
                throw new CoreRuntimeException(e);
            }
        }

        return javaElements;
    }

    /**
     * Returns the <tt>ProductCmptGenImplClassBuilder</tt> or <tt>null</tt> if non has been
     * assembled yet.
     */
    public final ProductCmptGenerationClassBuilder getProductCmptGenImplClassBuilder() {
        return getBuilderById(BuilderKindIds.PRODUCT_CMPT_TYPE_GENERATION_IMPLEMEMENTATION,
                ProductCmptGenerationClassBuilder.class);
    }

    public final ProductCmptBuilder getProductCmptBuilder() {
        return getBuilderById(BuilderKindIds.PRODUCT_CMPT_IMPLEMENTATION, ProductCmptBuilder.class);
    }

    /**
     * Returns the <tt>PolicyCmptClassBuilder</tt> or <tt>null</tt> if non has been assembled yet.
     */
    public final PolicyCmptClassBuilder getPolicyCmptImplClassBuilder() {
        return getBuilderById(BuilderKindIds.POLICY_CMPT_TYPE_IMPLEMEMENTATION, PolicyCmptClassBuilder.class);
    }

    /**
     * Returns the <tt>ProductCmptClassBuilder</tt> or <tt>null</tt> if non has been assembled yet.
     */
    public final ProductCmptClassBuilder getProductCmptImplClassBuilder() {
        return getBuilderById(BuilderKindIds.PRODUCT_CMPT_TYPE_IMPLEMEMENTATION, ProductCmptClassBuilder.class);
    }

    public TableImplBuilder getTableImplBuilder() {
        return getBuilderById(BuilderKindIds.TABLE, TableImplBuilder.class);
    }

    public TableRowBuilder getTableRowBuilder() {
        return getBuilderById(BuilderKindIds.TABLE_ROW, TableRowBuilder.class);
    }

    public EnumTypeBuilder getEnumTypeBuilder() {
        return getBuilderById(BuilderKindIds.ENUM_TYPE, EnumTypeBuilder.class);
    }

    public String getValidationMessageBundleBaseName(IIpsSrcFolderEntry entry) {
        return generatorModelContext.getValidationMessageBundleBaseName(entry);
    }

    public <T extends AbstractGeneratorModelNode> T getModelNode(IIpsObjectPartContainer object, Class<T> type) {
        return modelService.getModelNode(object, type, generatorModelContext);
    }

    public ModelService getModelService() {
        return modelService;
    }

    public GeneratorModelContext getGeneratorModelContext() {
        return generatorModelContext;
    }

    private class StandardParameterIdentifierResolver extends AbstractParameterIdentifierResolver {
        private StandardParameterIdentifierResolver(IExpression formula2, ExprCompiler<JavaCodeFragment> exprCompiler) {
            super(formula2, exprCompiler);
        }

        @Override
        protected void addNewInstanceForEnumType(JavaCodeFragment fragment,
                EnumTypeDatatypeAdapter datatype,
                ExprCompiler<JavaCodeFragment> exprCompiler,
                String value) throws CoreException {
            getEnumTypeBuilder().setExtendedExprCompiler((ExtendedExprCompiler)exprCompiler);
            fragment.append(getEnumTypeBuilder().getNewInstanceCodeFragement(datatype, value));
        }

        @Override
        protected String getParameterAttributGetterName(IAttribute attribute, Datatype datatype) {
            if (attribute instanceof IPolicyCmptTypeAttribute) {
                XPolicyAttribute xPolicyAttribute = getModelNode(attribute, XPolicyAttribute.class);
                return xPolicyAttribute.getMethodNameGetter();
            }
            if (attribute instanceof IProductCmptTypeAttribute) {
                XProductAttribute xProductAttribute = getModelNode(attribute, XProductAttribute.class);
                if (xProductAttribute.isChangingOverTime()) {
                    return xProductAttribute.getMethodNameGetter();
                } else {
                    XProductCmptClass xProductCmptClass = getModelNode(attribute.getType(), XProductCmptClass.class);
                    return xProductCmptClass.getMethodNameGetProductCmpt() + "()."
                            + xProductAttribute.getMethodNameGetter();
                }
            }
            return null;
        }

        @Override
        protected String getParameterAttributDefaultValueGetterName(IAttribute attribute, Datatype datatype) {
            XPolicyCmptClass xPolicyCmptClass = getModelNode(attribute.getType(), XPolicyCmptClass.class);
            XPolicyAttribute xPolicyAttribute = getModelNode(attribute, XPolicyAttribute.class);
            return xPolicyCmptClass.getMethodNameGetProductCmptGeneration() + "()."
                    + xPolicyAttribute.getMethodNameGetDefaultValue();
        }

        @Override
        protected String getAssociationTargetGetterName(IAssociation association, IPolicyCmptType policyCmptType) {
            XPolicyAssociation xPolicyAssociation = getModelNode(association, XPolicyAssociation.class);
            return xPolicyAssociation.getMethodNameGetter();
        }

        @Override
        protected String getAssociationTargetAtIndexGetterName(IAssociation association, IPolicyCmptType policyCmptType) {
            XPolicyAssociation xPolicyAssociation = getModelNode(association, XPolicyAssociation.class);
            return xPolicyAssociation.getMethodNameGetSingle();
        }

        @Override
        protected String getAssociationTargetsGetterName(IAssociation association, IPolicyCmptType policyCmptType) {
            XPolicyAssociation xPolicyAssociation = getModelNode(association, XPolicyAssociation.class);
            return xPolicyAssociation.getMethodNameGetter();
        }

        @Override
        protected String getJavaClassName(IType type) {
            return StandardBuilderSet.this.getJavaClassName(type);
        }
    }

    private static class CachedPersistenceProvider {
        Class<? extends IPersistenceProvider> persistenceProviderClass;
        IPersistenceProvider cachedProvider = null;

        private static CachedPersistenceProvider create(Class<? extends IPersistenceProvider> pPClass) {
            CachedPersistenceProvider providerCache = new CachedPersistenceProvider();
            providerCache.persistenceProviderClass = pPClass;
            return providerCache;
        }
    }

    public enum FormulaCompiling {

        Subclass,
        XML,
        Both;

        public boolean isCompileToSubclass() {
            return this == Subclass || this == Both;
        }

        public boolean isCompileToXml() {
            return this == XML || this == Both;
        }
    }
}
