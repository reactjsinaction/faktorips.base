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

package org.faktorips.devtools.core.internal.model.product;

import org.eclipse.osgi.util.NLS;

/**
 * 
 * @author Thorsten Guenther
 */
public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.faktorips.devtools.core.internal.model.product.messages"; //$NON-NLS-1$

	private Messages() {
	}

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	public static String ConfigElement_valueIsNotInTheValueSetDefinedInTheModel;

    public static String ConfigElement_valueSetIsNotASubset;

    public static String TableAccessFunctionFlFunctionAdapter_msgNoTableAccess;

	public static String TableAccessFunctionFlFunctionAdapter_msgErrorDuringCodeGeneration;

	public static String ProductCmptGeneration_msgTemplateNotFound;

	public static String ProductCmptGeneration_msgNotEnoughRelations;

	public static String ProductCmptGeneration_msgTooManyRelations;

	public static String ConfigElement_msgAttrNotDefined;

	public static String ConfigElement_msgFormulaNotDefined;

	public static String ConfigElement_msgDatatypeMissing;

	public static String ConfigElement_msgReturnTypeMissmatch;

	public static String ConfigElement_msgUndknownDatatype;

	public static String ConfigElement_msgInvalidDatatype;

	public static String ConfigElement_msgValueNotParsable;

	public static String ConfigElement_msgValueNotInValueset;

	public static String ProductCmptRelation_msgNoRelationDefined;

	public static String ProductCmptRelation_msgMaxCardinalityIsLessThan1;

	public static String ProductCmptRelation_msgMaxCardinalityIsLessThanMin;

	public static String ProductCmptRelation_msgMaxCardinalityExceedsModelMax;

	public static String ProductCmpt_msgUnknownTemplate;

	public static String DeepCopyOperation_taskTitle;

	public static String ConfigElement_msgValueIsEmptyString;

	public static String ProductCmptGeneration_msgDuplicateTarget;

	public static String AbstractProductCmptNamingStrategy_msgNoVersionSeparator;

	public static String AbstractProductCmptNamingStrategy_msgIllegalChar;

	public static String DateBasedProductCmptNamingStrategy_msgWrongFormat;

	public static String ProductCmpt_msgInvalidTypeHierarchy;

	public static String AbstractProductCmptNamingStrategy_emptyKindId;

	public static String ProductCmptRelation_msgInvalidTarget;

    public static String ConfigElement_msgInvalidAttributeValueset;

    public static String FormulaTestInputValue_CoreException_WrongIdentifierForParameter;

    public static String FormulaTestInputValue_CoreException_AttributeOfParameterNotFound;

    public static String FormulaTestInputValue_ValidationMessage_FormulaParameterNotFound;

    public static String FormulaTestInputValue_ValidationMessage_UnsupportedDatatype;

    public static String ConfigElement_msgFormulaTestCaseNotAllowedIfTypeOfConfigElemIs;

    public static String FormulaTestCase_CoreException_DatatypeNotFoundOrWrongConfigured;

    public static String FormulaTestCase_ValidationMessage_DuplicateFormulaTestCaseName;

    public static String FormulaTestCase_ValidationMessage_MismatchBetweenFormulaInputValuesAndIdentifierInFormula;

    public static String TableContentUsage_msgNoType;

    public static String TableContentUsage_msgUnknownStructureUsage;

    public static String TableContentUsage_msgUnknownTableContent;

    public static String TableContentUsage_msgInvalidTableContent;

    public static String DefaultRuntimeIdStrategy_msgRuntimeIdNotValid;
}
