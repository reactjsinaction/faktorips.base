/*******************************************************************************
 * Copyright (c) Faktor Zehn AG. <http://www.faktorzehn.org>
 * 
 * This source code is available under the terms of the AGPL Affero General Public License version
 * 3.
 * 
 * Please see LICENSE.txt for full license terms, including the additional permissions and
 * restrictions as well as the possibility of alternative license terms.
 *******************************************************************************/

package org.faktorips.runtime;

import java.util.Calendar;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBContext;

import org.faktorips.runtime.formula.IFormulaEvaluatorFactory;
import org.faktorips.runtime.internal.AbstractTocBasedRuntimeRepository;
import org.faktorips.runtime.internal.ProductConfiguration;
import org.faktorips.runtime.internal.toc.CustomTocEntryObject;
import org.faktorips.runtime.model.Models;
import org.faktorips.runtime.modeltype.IModelType;
import org.faktorips.runtime.test.IpsTest2;
import org.faktorips.runtime.test.IpsTestCaseBase;
import org.faktorips.runtime.test.IpsTestSuite;

/**
 * The runtime repository gives access to the information about products, enums and tables.
 * 
 * @author Jan Ortmann
 */
public interface IRuntimeRepository {

    /**
     * Returns the repository's name.
     */
    public String getName();

    /**
     * Adds a repository this repository depends on because the one to add contains product data
     * that is referenced from this repository. Access methods like
     * <code>getProductComponent(..)</code> include all repositories this one depends on in their
     * search.
     * 
     * @param repository The repository to add.
     * 
     * @throws NullPointerException if repository is <code>null</code>.
     */
    public void addDirectlyReferencedRepository(IRuntimeRepository repository);

    /**
     * Returns the runtime repositories this one directly depends on.
     */
    public List<IRuntimeRepository> getDirectlyReferencedRepositories();

    /**
     * Returns all repositories this one depends on directly or indirectly. The order is defined by
     * a breadth first search starting with this repository's direct dependencies. Each repository
     * is only included once even if it is referenced from two others. The list is computed lazy on
     * the first request.
     */
    public List<IRuntimeRepository> getAllReferencedRepositories();

    /**
     * Returns the product component identified by the given id. Returns <code>null</code> if the id
     * is <code>null</code> or no component with the indicated id can be found.
     * <p>
     * Note
     * <p>
     * How the product component id is structured, is defined as part of the ips project. The
     * definition is called a product component naming (and identification) strategy. The standard
     * strategy is to use the id of the product component kind followed by a separator followed by
     * component's versionId. However it is possible to use completly different strategies for
     * example to create numeric identifiers to reduce the size of indices in a datatbase. In the
     * latter case the product component id can't be derived from the product component kind id and
     * it's version id.
     * 
     * @param id The id of the product component to find.
     * 
     * @return The product component identified by the id or <code>null</code>.
     */
    public IProductComponent getProductComponent(String id);

    /**
     * Returns the product component identified by the given id. Same as getProductComponent(String
     * id) but throws an exception if the product component is not found. This method never returns
     * null.
     * 
     * @param id The id of the product component to find.
     * @return The product component identified by the id
     * @throws ProductCmptNotFoundException if no product component with the given id exists.
     * 
     * @see #getProductComponent(String)
     */
    public IProductComponent getExistingProductComponent(String id);

    /**
     * Returns the product component identified by the given KindId and versionId. If versionId is
     * <code>null</code> the most recent version is returned. Returns <code>null</code> if the
     * kindId is <code>null</code> or no component with the indicated kindId and versionId can be
     * found.
     * 
     * @param kindId The product component kind id, e.g. CollisionCoverage
     * @param versionId The versionId to find, e.g. 2005-01
     * 
     * @return The product component identified by the id or <code>null</code>.
     */
    public IProductComponent getProductComponent(String kindId, String versionId);

    /**
     * Returns all product components that belong to the indicated product component kind. Returns
     * an empty array if either kindId is <code>null</code> or no component with the indicated kind
     * is found.
     * 
     * @param kindId The product component kind id, e.g. CollisionCoverage
     */
    public List<IProductComponent> getAllProductComponents(String kindId);

    /**
     * Returns the type safe {@link List} of enumeration values of the provided faktor ips generated
     * enumeration class. This method is only relevant for faktor ips enumerations whose values are
     * deferred to a content that is hold by this repository.
     * 
     * @param clazz the enumeration class upon which the list of enumeration values is returned
     * 
     * @return The UNMODIFIABLE list of enum values.
     */
    public <T> List<T> getEnumValues(Class<T> clazz);

    /**
     * Returns the enumeration value for the provided enumeration class and string representation of
     * the value. If no value is found in the enumeration of the provided enumeration class
     * <code>null</code> will be returned. If the provided class cannot be recognized as a faktor
     * ips enumeration <code>null</code> will be returned. This method is only relevant for faktor
     * ips enumerations whose values are deferred to a content that is hold by this repository.
     * 
     * @param clazz The enumeration class upon which the enumeration value is returned
     * @param id The enum value's identification
     */
    public <T> T getEnumValue(Class<T> clazz, Object id);

    /**
     * Returns the enumeration value for the provided unique Id. The unique Id is specified as
     * follows <i>qualifiedClassName'#'valueId</i>.
     * 
     * @deprecated This method does only return valid enums if the id attribute of the enum is of
     *             type {@link String}. You should never use this method! Use
     *             {@link #getEnumValue(Class, Object)} instead. This method may be returned in
     *             future releases.
     */
    @Deprecated
    public Object getEnumValue(String uniqueId);

    /**
     * Adds the service to lookup enumeration values for the enumeration type specified by
     * {@link IEnumValueLookupService#getEnumTypeClass()}. If a service is already registered for
     * the enumeration type, the new service replaces the old one.
     * 
     * @param lookupService The new lookup service.
     */
    public void addEnumValueLookupService(IEnumValueLookupService<?> lookupService);

    /**
     * Returns the lookup service for the given enumeration type. Returns <code>null</code> if no
     * service is registered for the given type.
     */
    public <T> IEnumValueLookupService<T> getEnumValueLookupService(Class<T> enumClazz);

    /**
     * Removes the lookup service registered for the given enumeration type. Does nothing if no such
     * service has been registered.
     */
    public void removeEnumValueLookupService(IEnumValueLookupService<?> lookupService);

    /**
     * Returns the product component generation identified by the id and the effective date. Returns
     * <code>null</code> if either the id is <code>null</code>, the effectiveDate is
     * <code>null</code>or no generation with the indicated id can be found or the product component
     * hasn't got a generation that is effective on the given date.
     * 
     * @param id The product component's id.
     * @param effectiveDate The process' effective date
     * 
     * @return The product component generation or <code>null</code>.
     */
    public IProductComponentGeneration getProductComponentGeneration(String id, Calendar effectiveDate);

    /**
     * Returns the product component generation identified by the id and the effective date. Same as
     * getProductComponentGeneration(String id, Calendar effectiveDate) but throws an exception if
     * the product component generation is not found. This method never returns null.
     * 
     * @param id The product component's id.
     * @param effectiveDate The process' effective date
     * @return The product component generation
     * @throws ProductCmptGenerationNotFoundException if no generation for the given effectiveDate.
     * 
     * @see #getProductComponentGeneration(String, Calendar)
     */
    public IProductComponentGeneration getExistingProductComponentGeneration(String id, Calendar effectiveDate);

    /**
     * Returns all product components that are instances of the indicated class. Returns an empty
     * list if no such component exists.
     */
    public <T extends IProductComponent> List<T> getAllProductComponents(Class<T> productComponentType);

    /**
     * Returns all product components available in this repository. Returns an empty list if no
     * component is available.
     * <p>
     * Note that this is an expensive operation as all components have to be loaded into memory.
     */
    public List<IProductComponent> getAllProductComponents();

    /**
     * Returns all product component generations for the given product component. Returns an empty
     * list if no generation is available.
     * <p>
     * The generations are ordered by valid from date in reverse order that means the latest
     * generation (latest valid from date) is the first one, the oldest generation is the last one.
     * 
     * @return The list of product component generations ordered by the valid from date in reverse
     *         order
     */
    public List<IProductComponentGeneration> getProductComponentGenerations(IProductComponent productCmpt);

    /**
     * Returns the number of product component generations of the provided product component.
     */
    public int getNumberOfProductComponentGenerations(IProductComponent productCmpt);

    /**
     * Returns the product component generation that follows the provided generation with respect to
     * its valid from date.
     * <p>
     * If there is no further generation this method returns <code>null</code>.
     * 
     * @return The next generation with respect to the valid from date.
     * @throws IllegalArgumentException if the given product component generation could not be found
     *             in this repository or in any dependent repository.
     */
    public IProductComponentGeneration getNextProductComponentGeneration(IProductComponentGeneration generation);

    /**
     * Returns the product component generation that is prior to the provided generation with
     * respect to its valid from date.
     * <p>
     * If there is no previous generation this method returns <code>null</code>.
     * 
     * @return The previous generation with respect to the valid from date.
     * @throws IllegalArgumentException if the given product component generation could not be found
     *             in this repository or in any dependent repository.
     */
    public IProductComponentGeneration getPreviousProductComponentGeneration(IProductComponentGeneration generation);

    /**
     * Returns the latest product component generation of the provided product component.
     * 
     * @return The generation with the latest valid from date
     */
    public IProductComponentGeneration getLatestProductComponentGeneration(IProductComponent productCmpt);

    /**
     * Returns a list of the IDs of all product components held by this repository or any dependent
     * repository.
     * 
     * @return All valid product component IDs that are accessible by this repository.
     */
    public List<String> getAllProductComponentIds();

    /**
     * Returns all tables available in this repository. Returns an empty list if no table is
     * available.
     * <p>
     * Note that this is an expensive operation as all tables have to be loaded into memory.
     */
    public List<ITable> getAllTables();

    /**
     * Returns the table contents for the given table class.
     */
    public <T extends ITable> T getTable(Class<T> tableClass);

    /**
     * Returns the table contents for the given qualified table name.
     */
    public ITable getTable(String qualifiedTableName);

    /**
     * Returns a list of all test cases stored in the repository and all repositories this one
     * references. Returns an empty list if none is found.
     */
    public List<IpsTest2> getAllIpsTestCases(IRuntimeRepository runtimeRepository);

    /**
     * Returns a list of test cases starting with the given qualified name prefix stored in the
     * repository and all repositories this one references. Returns an empty list if none is found.
     */
    public List<IpsTest2> getIpsTestCasesStartingWith(String qNamePrefix, IRuntimeRepository runtimeRepository);

    /**
     * Returns the test (either test case or suite) for the given qualified name. If a test is found
     * for the given qualified name, the test is returned. Otherwise a test suite containing all
     * tests that starts with the given qualified name is returned. Returns an empty test suite if
     * no tests are found for the given qualified name.
     * 
     * @throws NullPointerException if qName is <code>null</code>.
     */
    public IpsTest2 getIpsTest(String qName);

    /**
     * Returns the test (either test case or suite) for the given qualified name. If a test is found
     * for the given qualified name, the test is returned. Otherwise a test suite containing all
     * tests that starts with the given qualified name is returned. Returns an empty test suite if
     * no tests are found for the given qualified name. The given runtime repository is the
     * repository which will be used to instantiate the test cases.
     * 
     * @see IRuntimeRepository#getIpsTestCase
     * 
     * @throws NullPointerException if qName is <code>null</code>.
     */
    public IpsTest2 getIpsTest(String qName, IRuntimeRepository runtimeRepository);

    /**
     * Returns the test case for the given qualified name.
     * 
     * @throws NullPointerException if qName is <code>null</code>.
     */
    public IpsTestCaseBase getIpsTestCase(String qName);

    /**
     * Returns the test case for the given qualified name. The given runtimeRepository will be used
     * to instantiate the test case (this repository is used to search for test cases).<br>
     * Remark this runtime repository which will be used to search for the given test case can
     * differ from the runtime repository which will be used to instantiate the test case during
     * runtime.<br>
     * Normaly the runtime repository contains all repositories which are referenced by the project.
     * 
     * @throws NullPointerException if qName is <code>null</code>.
     */
    public IpsTestCaseBase getIpsTestCase(String qName, IRuntimeRepository runtimeRepository);

    /**
     * Returns a test suite that contains all tests that have qualified names starting with the
     * given prefix. Note that if test cases belong to different package fragments the returned test
     * suite contains other testsuites. One suite for each package fragment.
     * 
     * @throws NullPointerException if qNamePrefix is <code>null</code>.
     */
    public IpsTestSuite getIpsTestSuite(String qNamePrefix);

    /**
     * Returns a test suite that contains all tests that have qualified names starting with the
     * given prefix. Note that if test cases belong to different package fragments the returned test
     * suite contains other test suites. One suite for each package fragment. The given runtime
     * repository is the repository which will be used to instantiate the test cases.
     * 
     * @see IRuntimeRepository#getIpsTestCase
     * 
     * @throws NullPointerException if qNamePrefix is <code>null</code>.
     */
    public IpsTestSuite getIpsTestSuite(String qNamePrefix, IRuntimeRepository runtimeRepository);

    /**
     * Returns <code>true</code> if the repository's content is modifiable. This feature is mainly
     * targeted for writing test cases that need to setup a repository with a test specific content
     * programmatically. Returns <code>false</code> otherwise.
     */
    public boolean isModifiable();

    /**
     * Returns the <code>IModelType</code> containing the meta information for the given model
     * object class.
     * 
     * @deprecated use {@link Models#getModelType(Class)}
     */
    @Deprecated
    public IModelType getModelType(Class<?> modelObjectClass);

    /**
     * Returns the <code>IModelType</code> containing the meta information for the given model
     * object. This is a convenience method calling <code>getModelType</code> with the model
     * object's class.
     * 
     * @deprecated use {@link Models#getPolicyModel(IModelObject)}
     */
    @Deprecated
    public IModelType getModelType(IModelObject modelObject);

    /**
     * Returns the <code>IModelType</code> containing the meta information for the given product
     * component. This is a convenience method calling <code>getModelType</code> with the product
     * component class.
     * 
     * @deprecated use {@link Models#getProductModel(IProductComponent)}
     */
    @Deprecated
    public IModelType getModelType(IProductComponent productComponent);

    /**
     * Returns a set containing the Java Class names of the implementation classes for all model
     * types available in this repository (either directly or via a referenced repository). For
     * product component types the implementation class for the part that remains unchanged over
     * time is returned. Currently there is no way to get get the implementation class for the
     * product component generation (the part that changed over time).
     * <p>
     * Returns an empty set if no type is available.
     */
    public Set<String> getAllModelTypeImplementationClasses();

    /**
     * Creates a new JAXBContext that can marshall / unmarshall all modell classes defined in the
     * given repository. If the repository references other repositories (directly or indirectly),
     * the context can also handle the classes defined in these other repositories.
     */
    public JAXBContext newJAXBContext();

    /**
     * Getting a formula evaluator factory to create a new formula evaluator. If formula evaluation
     * is not supported, this method should return null.
     * 
     * @return The configured formula evaluator or null if no evaluation is supported
     */
    public IFormulaEvaluatorFactory getFormulaEvaluatorFactory();

    /**
     * Returns the class loader that is used to load Java classes by this repository.
     */
    public ClassLoader getClassLoader();

    /**
     * Returns a object of type {@code T}, identified by it's qualified name, or {@code null} if no
     * such object exists in this repository or it's referenced repositories. It is up to extensions
     * to define which types can be found in the repository (e.g. a
     * {@link AbstractTocBasedRuntimeRepository} could define {@link CustomTocEntryObject}s for new
     * types. If there are no objects of the given type, {@code null} is returned. If the
     * {@link IRuntimeRepository} implementation does not allow custom types, {@code null} is
     * returned.
     * 
     * @param type a class supported by the {@link IRuntimeRepository} implementation
     * @param qName the qualified name of the object
     * @return the object identified by qName or {@code null}
     */
    public <T> T getCustomRuntimeObject(Class<T> type, String qName);

    /**
     * Returns the {@link IRuntimeRepositoryLookup} that was previously set using
     * {@link #setRuntimeRepositoryLookup(IRuntimeRepositoryLookup)}. The
     * {@link IRuntimeRepositoryLookup} is used for serialization of policy components especially
     * for the {@link ProductConfiguration} in configured policy components.
     * 
     * @return A previously set {@link IRuntimeRepositoryLookup} that is serialized by a
     *         {@link ProductConfiguration} and used to load the product component and its
     *         generation after deserialization.
     * 
     * @see IRuntimeRepositoryLookup
     * @see ProductConfiguration
     */
    public IRuntimeRepositoryLookup getRuntimeRepositoryLookup();

    /**
     * Setting a {@link IRuntimeRepositoryLookup} is needed to enable serialization of policy
     * components. You need to set a {@link IRuntimeRepositoryLookup} before you could serialize any
     * product configured policy component.
     * 
     * @param repositoryLookup The {@link IRuntimeRepositoryLookup} will provide an instance of this
     *            {@link IRuntimeRepository} when deserializing a policy component.
     * 
     * @see IRuntimeRepositoryLookup
     */
    public void setRuntimeRepositoryLookup(IRuntimeRepositoryLookup repositoryLookup);

}