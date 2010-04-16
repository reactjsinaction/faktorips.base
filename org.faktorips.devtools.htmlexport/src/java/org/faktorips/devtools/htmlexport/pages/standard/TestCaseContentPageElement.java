package org.faktorips.devtools.htmlexport.pages.standard;

import org.eclipse.core.runtime.CoreException;
import org.faktorips.devtools.core.model.ipsobject.IpsObjectType;
import org.faktorips.devtools.core.model.testcase.ITestAttributeValue;
import org.faktorips.devtools.core.model.testcase.ITestCase;
import org.faktorips.devtools.core.model.testcase.ITestObject;
import org.faktorips.devtools.core.model.testcase.ITestPolicyCmpt;
import org.faktorips.devtools.core.model.testcase.ITestRule;
import org.faktorips.devtools.core.model.testcase.ITestValue;
import org.faktorips.devtools.core.model.testcasetype.ITestCaseType;
import org.faktorips.devtools.htmlexport.documentor.DocumentorConfiguration;
import org.faktorips.devtools.htmlexport.generators.WrapperType;
import org.faktorips.devtools.htmlexport.pages.elements.core.ImagePageElement;
import org.faktorips.devtools.htmlexport.pages.elements.core.LinkPageElement;
import org.faktorips.devtools.htmlexport.pages.elements.core.PageElement;
import org.faktorips.devtools.htmlexport.pages.elements.core.Style;
import org.faktorips.devtools.htmlexport.pages.elements.core.TextPageElement;
import org.faktorips.devtools.htmlexport.pages.elements.core.TextType;
import org.faktorips.devtools.htmlexport.pages.elements.core.TreeNodePageElement;
import org.faktorips.devtools.htmlexport.pages.elements.core.WrapperPageElement;
import org.faktorips.devtools.htmlexport.pages.elements.types.KeyValueTablePageElement;

/**
 * a page representing an {@link ITestCase}
 * @author dicker
 *
 */
public class TestCaseContentPageElement extends AbstractObjectContentPageElement<ITestCase> {

	private ITestCaseType testCaseType;

	/**
	 * creates a page for the given {@link ITestCase} with the given config
	 * @param object
	 * @param config
	 */
	protected TestCaseContentPageElement(ITestCase object, DocumentorConfiguration config) {
		super(object, config);
		try {
			testCaseType = object.findTestCaseType(config.getIpsProject());
		} catch (CoreException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void build() {
		super.build();

		addPageElements(new WrapperPageElement(WrapperType.BLOCK).addPageElements(new TextPageElement(IpsObjectType.TEST_CASE_TYPE.getDisplayName() + ": ")) //$NON-NLS-1$
				.addPageElements(new LinkPageElement(testCaseType, "content", testCaseType.getQualifiedName(), true))); //$NON-NLS-1$

		addTestCaseTypeParameters();
	}

	/**
	 * adds a treeview of the parameters of the testcase
	 */
	private void addTestCaseTypeParameters() {
		addPageElements(new TextPageElement(Messages.TestCaseContentPageElement_parameters, TextType.HEADING_2));
		TreeNodePageElement root = new TreeNodePageElement(new WrapperPageElement(WrapperType.NONE).addPageElements(
				new ImagePageElement(getDocumentedIpsObject())).addPageElements(new TextPageElement(getDocumentedIpsObject().getQualifiedName())));
		
		ITestObject[] testObjects = getDocumentedIpsObject().getTestObjects();
		for (ITestObject testObject : testObjects) {
			root.addPageElements(createTestObjectPageElement(testObject));
		}
		addPageElements(root);
	}

	/**
	 * creates a {@link PageElement} for an {@link ITestObject}
	 * @param testObject
	 * @return
	 */
	private PageElement createTestObjectPageElement(ITestObject testObject) {
		if (testObject instanceof ITestValue)
			return createTestValuePageElement((ITestValue) testObject);
		if (testObject instanceof ITestRule)
			return createTestRulePageElement((ITestRule) testObject);
		if (testObject instanceof ITestPolicyCmpt)
			return createTestPolicyCmptPageElement((ITestPolicyCmpt) testObject);

		return TextPageElement.createParagraph(testObject.getName() + " " + testObject.getClass()).addStyles(Style.BIG) //$NON-NLS-1$
				.addStyles(Style.BOLD);
	}

	private PageElement createTestPolicyCmptPageElement(ITestPolicyCmpt testObject) {
		TreeNodePageElement testObjectPageElement = new TreeNodePageElement(new WrapperPageElement(WrapperType.BLOCK)
				.addPageElements(new ImagePageElement(testObject)).addPageElements(
						new TextPageElement(testObject.getTestParameterName())));

		ITestAttributeValue[] testAttributeValues = testObject.getTestAttributeValues();

		KeyValueTablePageElement keyValueTable = new KeyValueTablePageElement();
		for (ITestAttributeValue testAttributeValue : testAttributeValues) {
			keyValueTable.addKeyValueRow(testAttributeValue.getName(), testAttributeValue.getValue());
		}

		testObjectPageElement.addPageElements(keyValueTable);
		
		return testObjectPageElement;
	}

	private PageElement createTestRulePageElement(ITestRule testObject) {
		TreeNodePageElement testObjectPageElement = new TreeNodePageElement(new WrapperPageElement(WrapperType.BLOCK)
				.addPageElements(new ImagePageElement(testObject)).addPageElements(
						new TextPageElement(testObject.getTestParameterName())));

		KeyValueTablePageElement keyValueTable = new KeyValueTablePageElement();
		keyValueTable.addKeyValueRow(Messages.TestCaseContentPageElement_name, testObject.getValidationRule());
		keyValueTable.addKeyValueRow(Messages.TestCaseContentPageElement_violationType, testObject.getViolationType().getName());

		testObjectPageElement.addPageElements(keyValueTable);

		return testObjectPageElement;
	}

	private PageElement createTestValuePageElement(ITestValue testObject) {
		TreeNodePageElement testObjectPageElement = new TreeNodePageElement(new WrapperPageElement(WrapperType.BLOCK)
				.addPageElements(new ImagePageElement(testObject)).addPageElements(
						new TextPageElement(testObject.getTestParameterName())));

		KeyValueTablePageElement keyValueTable = new KeyValueTablePageElement();
		keyValueTable.addKeyValueRow(Messages.TestCaseContentPageElement_name, testObject.getName()); //$NON-NLS-1$
		keyValueTable.addKeyValueRow(Messages.TestCaseContentPageElement_value, testObject.getValue());
		try {
			keyValueTable.addKeyValueRow(Messages.TestCaseContentPageElement_testParameterType, testObject
					.findTestValueParameter(getConfig().getIpsProject()).getTestParameterType().getName());
		} catch (CoreException e) {
			throw new RuntimeException(e);
		}

		testObjectPageElement.addPageElements(keyValueTable);

		return testObjectPageElement;
	}
}
