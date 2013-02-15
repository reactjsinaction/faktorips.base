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

package org.faktorips.devtools.core.ui.wizards.enumcontent;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {

    private static final String BUNDLE_NAME = "org.faktorips.devtools.core.ui.wizards.enumcontent.messages"; //$NON-NLS-1$

    static {
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
        // Messages bundles shall not be initialized.
    }

    public static String EnumContentPage_msgEnumContentAlreadyExists;
    public static String EnumContentPage_msgEnumContentExistsForNameExistsAlready;
    public static String EnumContentPage_msgEnumContentNameOfEnumTypeMissing;
    public static String EnumContentPage_msgEnumTypeMissing;
    public static String EnumContentPage_msgEnumTypeNotExisting;
    public static String Page_Title;
    public static String Fields_EnumType;

    public static String CreateMissingEnumContentsWizard_title;
    public static String CreateMissingEnumContentsWizard_labelOperation;
    public static String SelectEnumContentsPage_title;
    public static String SelectEnumContentsPage_prompt;
    public static String SelectEnumContentsPage_labelTargetSourceFolder;
    public static String SelectEnumContentsPage_buttonSelectAll;
    public static String SelectEnumContentsPage_buttonDeselectAll;
    public static String SelectEnumContentsPage_msgTargetSourceFolderNotSpecified;
    public static String SelectEnumContentsPage_msgTargetSourceFolderDoesNotExist;

    public static String FixEnumContentWizard_title;
    public static String FixEnumContentWizard_labelNewEnumType;
    public static String FixEnumContentWizard_msgChooseEnumType;
    public static String FixEnumContentWizard_chooseEnumTypePageTitle;
    public static String FixEnumContentWizard_chosenEnumTypeAbstract;
    public static String FixEnumContentWizard_chosenEnumTypeValuesArePartOfModel;
    public static String FixEnumContentWizard_chosenEnumTypeEmpty;
    public static String FixEnumContentWizard_chosenEnumTypeDoesNotExist;
    public static String FixEnumContentWizard_msgAssignEnumAttributes;
    public static String FixEnumContentWizard_assignEnumAttributesPageTitle;
    public static String FixEnumContentWizard_assignEnumAttributesCreateNewColumn;
    public static String FixEnumContentWizard_assignEnumAttributesDuplicateColumnAssigned;
    public static String FixEnumContentWizard_assignEnumAttributesAttributeNotAssigned;
    public static String FixEnumContentWizard_assignEnumAttributesDeleteColumnsConfirmationTitle;
    public static String FixEnumContentWizard_assignEnumAttributesDeleteColumnsConfirmationMessageSingular;
    public static String FixEnumContentWizard_assignEnumAttributesDeleteColumnsConfirmationMessagePlural;
    public static String FixEnumContentWizard_assignEnumAttributesGroup;
    public static String FixEnumContentWizard_assignEnumAttributeMismatchPageTitle;

    public static String FixEnumContentWizard_messageMultilingual;
    public static String FixEnumContentWizard_messageNoMultilingual;
}
