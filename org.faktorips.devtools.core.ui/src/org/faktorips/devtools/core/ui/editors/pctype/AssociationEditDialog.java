/*******************************************************************************
 * Copyright (c) 2005-2009 Faktor Zehn AG und andere.
 * 
 * Alle Rechte vorbehalten.
 * 
 * Dieses Programm und alle mitgelieferten Sachen (Dokumentationen, Beispiele, Konfigurationen,
 * etc.) duerfen nur unter den Bedingungen der Faktor-Zehn-Community Lizenzvereinbarung - Version
 * 0.1 (vor Gruendung Community) genutzt werden, die Bestandteil der Auslieferung ist und auch unter
 * http://www.faktorzehn.org/f10-org:lizenzen:community eingesehen werden kann.
 * 
 * Mitwirkende: Faktor Zehn AG - initial API and implementation - http://www.faktorzehn.de
 *******************************************************************************/

package org.faktorips.devtools.core.ui.editors.pctype;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.faktorips.devtools.core.IpsPlugin;
import org.faktorips.devtools.core.model.ipsobject.IExtensionPropertyDefinition;
import org.faktorips.devtools.core.model.ipsproject.IIpsProject;
import org.faktorips.devtools.core.model.pctype.AssociationType;
import org.faktorips.devtools.core.model.pctype.IPersistentAssociationInfo;
import org.faktorips.devtools.core.model.pctype.IPersistentTypeInfo;
import org.faktorips.devtools.core.model.pctype.IPolicyCmptType;
import org.faktorips.devtools.core.model.pctype.IPolicyCmptTypeAssociation;
import org.faktorips.devtools.core.model.pctype.IPersistentAssociationInfo.FetchType;
import org.faktorips.devtools.core.model.productcmpttype.IProductCmptType;
import org.faktorips.devtools.core.model.productcmpttype.IProductCmptTypeAssociation;
import org.faktorips.devtools.core.model.type.IAssociation;
import org.faktorips.devtools.core.ui.CompletionUtil;
import org.faktorips.devtools.core.ui.ExtensionPropertyControlFactory;
import org.faktorips.devtools.core.ui.binding.ButtonTextBinding;
import org.faktorips.devtools.core.ui.binding.ControlPropertyBinding;
import org.faktorips.devtools.core.ui.binding.IpsObjectPartPmo;
import org.faktorips.devtools.core.ui.controller.fields.CardinalityField;
import org.faktorips.devtools.core.ui.controller.fields.ComboField;
import org.faktorips.devtools.core.ui.controller.fields.EnumField;
import org.faktorips.devtools.core.ui.controls.Checkbox;
import org.faktorips.devtools.core.ui.controls.PcTypeRefControl;
import org.faktorips.devtools.core.ui.editors.IpsPartEditDialog2;
import org.faktorips.devtools.core.ui.editors.type.DerivedUnionCompletionProcessor;
import org.faktorips.devtools.core.util.QNameUtil;

/**
 * A dialog to edit an association.
 */
public class AssociationEditDialog extends IpsPartEditDialog2 {

    private IIpsProject ipsProject;
    private IPolicyCmptTypeAssociation association;
    private PmoAssociation pmoAssociation;

    private ExtensionPropertyControlFactory extFactory;

    /**
     * @param parentShell
     * @param title
     */
    public AssociationEditDialog(IPolicyCmptTypeAssociation relation2, Shell parentShell) {
        super(relation2, parentShell, Messages.AssociationEditDialog_title, true);
        association = relation2;
        ipsProject = association.getIpsProject();
        pmoAssociation = new PmoAssociation(association);
        extFactory = new ExtensionPropertyControlFactory(association.getClass());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Composite createWorkArea(Composite parent) throws CoreException {
        TabFolder folder = (TabFolder)parent;

        TabItem firstPage = new TabItem(folder, SWT.NONE);
        firstPage.setText(Messages.AssociationEditDialog_textFirstPage);
        firstPage.setControl(createFirstPage(folder));

        createPersistenceTabItemIfNecessary(folder);
        createDescriptionTabItem(folder);
        return folder;
    }

    /**
     * Creates the first tab page. With the following goups: general, policy side, and product side.
     */
    private Control createFirstPage(TabFolder folder) {

        Composite c = createTabItemComposite(folder, 1, false);

        Group groupGeneral = uiToolkit.createGroup(c, Messages.AssociationEditDialog_generalGroup);
        createGeneralControls(groupGeneral);

        uiToolkit.createVerticalSpacer(c, 12);
        createQualificationGroup(uiToolkit.createGroup(c, Messages.AssociationEditDialog_qualificationGroup));

        uiToolkit.createVerticalSpacer(c, 12);
        createDerivedUnionGroup(uiToolkit.createGroup(c, Messages.AssociationEditDialog_derivedUnionGroup));

        return c;
    }

    private void createGeneralControls(Composite c) {
        Composite workArea = uiToolkit.createLabelEditColumnComposite(c);
        workArea.setLayoutData(new GridData(GridData.FILL_BOTH));

        // top extensions
        extFactory.createControls(workArea, uiToolkit, association, IExtensionPropertyDefinition.POSITION_TOP);

        // target
        uiToolkit.createFormLabel(workArea, Messages.AssociationEditDialog_targetLabel);
        PcTypeRefControl targetControl = uiToolkit.createPcTypeRefControl(association.getIpsProject(), workArea);
        bindingContext.bindContent(targetControl, association, IAssociation.PROPERTY_TARGET);

        // type
        uiToolkit.createFormLabel(workArea, Messages.AssociationEditDialog_associationTypeLabel);
        final Combo typeCombo = uiToolkit
                .createCombo(workArea, IPolicyCmptTypeAssociation.APPLICABLE_ASSOCIATION_TYPES);
        bindingContext.bindContent(typeCombo, association, IAssociation.PROPERTY_ASSOCIATION_TYPE, AssociationType
                .getEnumType());
        typeCombo.setFocus();

        // role singular
        uiToolkit.createFormLabel(workArea, Messages.AssociationEditDialog_targetRoleSingularLabel);
        final Text targetRoleSingularText = uiToolkit.createText(workArea);
        bindingContext.bindContent(targetRoleSingularText, association, IAssociation.PROPERTY_TARGET_ROLE_SINGULAR);
        targetRoleSingularText.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (StringUtils.isEmpty(association.getTargetRoleSingular())) {
                    association.setTargetRoleSingular(association.getDefaultTargetRoleSingular());
                }
            }
        });

        // role plural
        uiToolkit.createFormLabel(workArea, Messages.AssociationEditDialog_targetRolePluralLabel);
        final Text targetRolePluralText = uiToolkit.createText(workArea);
        bindingContext.bindContent(targetRolePluralText, association, IAssociation.PROPERTY_TARGET_ROLE_PLURAL);
        targetRolePluralText.addFocusListener(new FocusAdapter() {

            @Override
            public void focusGained(FocusEvent e) {
                if (StringUtils.isEmpty(targetRolePluralText.getText()) && association.isTargetRolePluralRequired()) {
                    association.setTargetRolePlural(association.getDefaultTargetRolePlural());
                }
            }
        });

        // min cardinality
        uiToolkit.createFormLabel(workArea, Messages.AssociationEditDialog_minimumCardinality);
        Text minCardinalityText = uiToolkit.createText(workArea);
        CardinalityField cardinalityField = new CardinalityField(minCardinalityText);
        cardinalityField.setSupportsNull(false);
        bindingContext.bindContent(cardinalityField, association, IAssociation.PROPERTY_MIN_CARDINALITY);

        // max cardinality
        uiToolkit.createFormLabel(workArea, Messages.AssociationEditDialog_maximumCardinality);
        Text maxCardinalityText = uiToolkit.createText(workArea);
        cardinalityField = new CardinalityField(maxCardinalityText);
        cardinalityField.setSupportsNull(false);
        bindingContext.bindContent(cardinalityField, association, IAssociation.PROPERTY_MAX_CARDINALITY);

        // inverse relation
        uiToolkit.createFormLabel(workArea, Messages.AssociationEditDialog_inverseAssociationLabel);
        Text reverseRelationText = uiToolkit.createText(workArea);
        bindingContext.bindContent(reverseRelationText, association,
                IPolicyCmptTypeAssociation.PROPERTY_INVERSE_ASSOCIATION);
        InverseAssociationCompletionProcessor inverseAssociationCompletionProcessor = new InverseAssociationCompletionProcessor(
                association);
        inverseAssociationCompletionProcessor.setComputeProposalForEmptyPrefix(true);
        CompletionUtil.createHandlerForText(reverseRelationText, inverseAssociationCompletionProcessor);

        Composite info = uiToolkit.createGridComposite(c, 1, true, false);
        Label note = uiToolkit.createLabel(info, pmoAssociation.getConstrainedNote());
        bindingContext.bindContent(note, pmoAssociation, PmoAssociation.PROPERTY_CONSTRAINED_NOTE);

        // bottom extensions
        extFactory.createControls(workArea, uiToolkit, association, IExtensionPropertyDefinition.POSITION_BOTTOM);
        extFactory.bind(bindingContext);
    }

    private void createDerivedUnionGroup(Composite c) {

        // derived union checkbox
        Checkbox containerCheckbox = uiToolkit.createCheckbox(c,
                Messages.AssociationEditDialog_associationIsADerivedUnion);
        bindingContext.bindContent(containerCheckbox, association, IAssociation.PROPERTY_DERIVED_UNION);
        bindingContext.bindEnabled(containerCheckbox, association,
                IPolicyCmptTypeAssociation.PROPERTY_SUBSETTING_DERIVED_UNION_APPLICABLE);

        // is subset checkbox
        Checkbox subsetCheckbox = uiToolkit.createCheckbox(c, Messages.AssociationEditDialog_associationDefinesSubset);
        bindingContext.bindContent(subsetCheckbox, pmoAssociation, PmoAssociation.PROPERTY_SUBSET);
        bindingContext.bindEnabled(subsetCheckbox, association,
                IPolicyCmptTypeAssociation.PROPERTY_SUBSETTING_DERIVED_UNION_APPLICABLE);

        Composite workArea = uiToolkit.createLabelEditColumnComposite(c);
        workArea.setLayoutData(new GridData(GridData.FILL_BOTH));
        uiToolkit.createFormLabel(workArea, Messages.AssociationEditDialog_derivedUnionLabel);
        Text derivedUnion = uiToolkit.createText(workArea);
        bindingContext.bindContent(derivedUnion, association, IAssociation.PROPERTY_SUBSETTED_DERIVED_UNION);
        bindingContext.bindEnabled(derivedUnion, pmoAssociation, PmoAssociation.PROPERTY_SUBSET);
        DerivedUnionCompletionProcessor completionProcessor = new DerivedUnionCompletionProcessor(association);
        completionProcessor.setComputeProposalForEmptyPrefix(true);
        CompletionUtil.createHandlerForText(derivedUnion, completionProcessor);
    }

    private void createQualificationGroup(Composite c) {
        Composite workArea = uiToolkit.createGridComposite(c, 1, true, true);
        workArea.setLayoutData(new GridData(GridData.FILL_BOTH));

        Checkbox qualifiedCheckbox = uiToolkit.createCheckbox(workArea);
        bindingContext.bindContent(qualifiedCheckbox, association, IAssociation.PROPERTY_QUALIFIED);
        bindingContext.bindEnabled(qualifiedCheckbox, pmoAssociation, PmoAssociation.PROPERTY_QUALIFICATION_POSSIBLE);
        Label note = uiToolkit.createFormLabel(workArea, StringUtils.rightPad("", 120)); //$NON-NLS-1$
        bindingContext.bindContent(note, pmoAssociation, PmoAssociation.PROPERTY_QUALIFICATION_NOTE);
        bindingContext.add(new ButtonTextBinding(qualifiedCheckbox, pmoAssociation,
                PmoAssociation.PROPERTY_QUALIFICATION_LABEL));
    }

    private void createPersistenceTabItemIfNecessary(TabFolder tabFolder) {
        if (!ipsProject.getProperties().isPersistenceSupportEnabled()) {
            return;
        }
        final TabItem persistencePage = new TabItem(tabFolder, SWT.NONE);
        persistencePage.setText("Persistence");

        Composite c = createTabItemComposite(tabFolder, 1, false);
        persistencePage.setControl(c);

        Checkbox checkTransient = uiToolkit.createCheckbox(c, "The association is transient");
        bindingContext.bindContent(checkTransient, association.getPersistenceAssociatonInfo(),
                IPersistentAssociationInfo.PROPERTY_TRANSIENT);

        final Composite allPersistentProps = uiToolkit.createGridComposite(c, 1, true, true);

        uiToolkit.createVerticalSpacer(allPersistentProps, 12);

        final Group joinTable = createGroupJoinTable(allPersistentProps);

        uiToolkit.createVerticalSpacer(allPersistentProps, 12);

        createGroupOtherPersistentProps(allPersistentProps);

        // disable all tab page controls if policy component type shouldn't persist
        bindingContext.add(new ControlPropertyBinding(c, association.getPolicyCmptType().getPersistenceTypeInfo(),
                IPersistentTypeInfo.PROPERTY_ENABLED, Boolean.TYPE) {
            @Override
            public void updateUiIfNotDisposed() {
                uiToolkit.setDataChangeable(getControl(), ((IPersistentTypeInfo)getObject()).isEnabled());
                uiToolkit.setDataChangeable(getControl(), ((IPersistentTypeInfo)getObject()).isEnabled());
            }
        });
        // disable all persistent controls if attribute is marked as transient
        bindingContext.add(new ControlPropertyBinding(allPersistentProps, association.getPersistenceAssociatonInfo(),
                IPersistentAssociationInfo.PROPERTY_TRANSIENT, Boolean.TYPE) {
            @Override
            public void updateUiIfNotDisposed() {
                IPersistentAssociationInfo associationInfo = (IPersistentAssociationInfo)getObject();
                boolean persistEnabled = associationInfo.getPolicyComponentTypeAssociation().getPolicyCmptType()
                        .isPersistentEnabled();
                if (!persistEnabled) {
                    return;
                }
                uiToolkit.setDataChangeable(getControl(), !associationInfo.isTransient());
            }
        });
        // disable join table controls if join table is not required
        bindingContext.add(new ControlPropertyBinding(joinTable, association.getPersistenceAssociatonInfo(),
                IPersistentAssociationInfo.PROPERTY_TRANSIENT, Boolean.TYPE) {
            @Override
            public void updateUiIfNotDisposed() {
                IPersistentAssociationInfo associationInfo = (IPersistentAssociationInfo)getObject();
                boolean persistEnabled = associationInfo.getPolicyComponentTypeAssociation().getPolicyCmptType()
                        .isPersistentEnabled();
                if (!persistEnabled) {
                    return;
                }
                if (associationInfo.isTransient()) {
                    return;
                }
                try {
                    uiToolkit.setDataChangeable(getControl(), associationInfo.isJoinTableRequired());
                } catch (CoreException e) {
                    IpsPlugin.logAndShowErrorDialog(e);
                }
            }
        });
    }

    private Group createGroupOtherPersistentProps(Composite allPersistentProps) {
        Group groupOtherProps = uiToolkit.createGroup(allPersistentProps, "Persistent Properties");
        GridData layoutData = (GridData)groupOtherProps.getLayoutData();
        layoutData.grabExcessVerticalSpace = false;

        Composite otherPropsComposite = uiToolkit.createLabelEditColumnComposite(groupOtherProps);
        otherPropsComposite.setLayoutData(new GridData(GridData.FILL_BOTH));

        uiToolkit.createFormLabel(otherPropsComposite, "Fetch Type:");
        Combo fetchTypeCombo = uiToolkit.createCombo(otherPropsComposite);
        setComboItemsForEnum(fetchTypeCombo, FetchType.class);
        ComboField fetchTypeField = new EnumField(fetchTypeCombo, FetchType.class);
        bindingContext.bindContent(fetchTypeField, association.getPersistenceAssociatonInfo(),
                IPersistentAssociationInfo.PROPERTY_FETCH_TYPE);
        return groupOtherProps;
    }

    private Group createGroupJoinTable(Composite allPersistentProps) {
        Group groupJoinTable = uiToolkit.createGroup(allPersistentProps, "Database Schema Properties");
        GridData layoutData = (GridData)groupJoinTable.getLayoutData();
        layoutData.grabExcessVerticalSpace = false;

        Composite workArea = uiToolkit.createLabelEditColumnComposite(groupJoinTable);
        workArea.setLayoutData(new GridData(GridData.FILL_BOTH));

        uiToolkit.createFormLabel(workArea, "Join Table Name:");
        Text joinTableNameText = uiToolkit.createText(workArea);
        bindingContext.bindContent(joinTableNameText, association.getPersistenceAssociatonInfo(),
                IPersistentAssociationInfo.PROPERTY_JOIN_TABLE_NAME);

        uiToolkit.createFormLabel(workArea, "Source Column Name:");
        Text sourceColumnNameText = uiToolkit.createText(workArea);
        bindingContext.bindContent(sourceColumnNameText, association.getPersistenceAssociatonInfo(),
                IPersistentAssociationInfo.PROPERTY_SOURCE_COLUMN_NAME);

        // TODO Joerg JPA wollen wir die JoinColumn bei der one-to-many so definieren?
        // wenn ja, disable state fixen
        uiToolkit.createFormLabel(workArea, "Target Column Name:");
        Text targetColumnNameText = uiToolkit.createText(workArea);
        bindingContext.bindContent(targetColumnNameText, association.getPersistenceAssociatonInfo(),
                IPersistentAssociationInfo.PROPERTY_TARGET_COLUMN_NAME);

        return groupJoinTable;
    }

    public class PmoAssociation extends IpsObjectPartPmo {
        public final static String PROPERTY_SUBSET = "subset"; //$NON-NLS-1$
        public final static String PROPERTY_QUALIFICATION_LABEL = "qualificationLabel"; //$NON-NLS-1$
        public final static String PROPERTY_QUALIFICATION_NOTE = "qualificationNote"; //$NON-NLS-1$
        public final static String PROPERTY_QUALIFICATION_POSSIBLE = "qualificationPossible"; //$NON-NLS-1$
        public final static String PROPERTY_CONSTRAINED_NOTE = "constrainedNote"; //$NON-NLS-1$

        private boolean subset;

        public PmoAssociation(IPolicyCmptTypeAssociation association) {
            super(association);
            subset = association.isSubsetOfADerivedUnion();
        }

        public boolean isSubset() {
            return subset;
        }

        public void setSubset(boolean newValue) {
            subset = newValue;
            if (!subset) {
                association.setSubsettedDerivedUnion(""); //$NON-NLS-1$
            }
            notifyListeners();
        }

        public String getQualificationLabel() {
            String label = Messages.AssociationEditDialog_associationIsQualified;
            try {
                String productCmptType = QNameUtil.getUnqualifiedName(association.findQualifierCandidate(ipsProject));
                if (StringUtils.isNotEmpty(productCmptType)) {
                    label = label + NLS.bind(Messages.AssociationEditDialog_qualifiedByType, productCmptType);
                }
            } catch (CoreException e) {
                IpsPlugin.log(e);
            }
            return StringUtils.rightPad(label, 80);
        }

        public String getQualificationNote() {
            String note = Messages.AssociationEditDialog_note;
            if (!association.isCompositionMasterToDetail()) {
                note = note + Messages.AssociationEditDialog_qualificationOnlyForMasterDetail;
            } else {
                try {
                    if (!association.isQualificationPossible(ipsProject)) {
                        note = note + Messages.AssociationEditDialog_qualificationOnlyIfTheTargetTypeIsConfigurable;
                    } else {
                        note = note + Messages.AssociationEditDialog_multiplicityIsDefineddPerQualifier;
                    }
                } catch (CoreException e) {
                    IpsPlugin.log(e);
                }
            }
            return StringUtils.rightPad(note, 90);
        }

        public boolean isQualificationPossible() {
            try {
                return association.isQualificationPossible(ipsProject);
            } catch (CoreException e) {
                IpsPlugin.log(e);
                return false;
            }
        }

        public String getConstrainedNote() {
            try {
                if (association.isCompositionDetailToMaster()) {
                    return StringUtils.rightPad("", 120) + StringUtils.rightPad("\n", 120) + StringUtils.right("\n", 120); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                }
                IProductCmptTypeAssociation matchingAss = association
                        .findMatchingProductCmptTypeAssociation(ipsProject);
                if (matchingAss != null) {
                    String type = matchingAss.getProductCmptType().getName();
                    return NLS.bind(Messages.AssociationEditDialog_noteAssociationIsConstrainedByProductStructure,
                            type, matchingAss.getTargetRoleSingular())
                            + StringUtils.rightPad("\n", 120); //$NON-NLS-1$
                } else {
                    String note = Messages.AssociationEditDialog_noteAssociationNotConstrainedByProductStructure;
                    IProductCmptType sourceProductType = association.getPolicyCmptType()
                            .findProductCmptType(ipsProject);
                    IPolicyCmptType targetType = association.findTargetPolicyCmptType(ipsProject);
                    if (sourceProductType != null && targetType != null) {
                        IProductCmptType targetProductType = targetType.findProductCmptType(ipsProject);
                        if (targetProductType != null) {
                            return note
                                    + NLS.bind(Messages.AssociationEditDialog_toConstraintTheAssociation,
                                            sourceProductType.getName(), targetProductType.getName());
                        }
                    }
                    return note + StringUtils.rightPad("\n", 120) + StringUtils.rightPad("\n", 120); //$NON-NLS-1$ //$NON-NLS-2$
                }
            } catch (CoreException e) {
                IpsPlugin.log(e);
                return ""; //$NON-NLS-1$
            }

        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void partHasChanged() {
            if (association.isCompositionDetailToMaster()) {
                subset = false;
            }
        }

    }
}
