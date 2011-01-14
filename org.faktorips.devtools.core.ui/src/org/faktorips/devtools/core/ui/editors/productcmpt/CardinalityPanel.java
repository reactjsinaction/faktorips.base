/*******************************************************************************
 * Copyright (c) 2005-2010 Faktor Zehn AG und andere.
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

package org.faktorips.devtools.core.ui.editors.productcmpt;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.faktorips.devtools.core.IpsPlugin;
import org.faktorips.devtools.core.model.productcmpt.IProductCmptLink;
import org.faktorips.devtools.core.ui.IDataChangeableReadWriteAccess;
import org.faktorips.devtools.core.ui.UIToolkit;
import org.faktorips.devtools.core.ui.controller.IpsObjectUIController;
import org.faktorips.devtools.core.ui.controller.fields.CardinalityField;

/**
 * Panel to display cardinality. Note that this is <strong>NOT</strong> a control.
 * <p>
 * To edit a {@link IProductCmptLink} call {@link #setProductCmptLinkToEdit(IProductCmptLink)} and
 * this panel will be updated with the links values. If <code>null</code> is passed to the method,
 * this panel resets and disables itself.
 * 
 * 
 * @author Thorsten Guenther
 * @author Stefan Widmaier
 */
public class CardinalityPanel implements IDataChangeableReadWriteAccess {

    private Text minCard;
    private Text maxCard;
    private Text defaultCard;
    private Label minMaxCardLabel;
    private Label defaultCardLabel;
    private Button optional;
    private Button mandatory;
    private Button other;
    private Composite root;

    private IpsObjectUIController uiController;
    private CardinalityField minCardField;
    private CardinalityField maxCardField;
    private CardinalityField defaultCardField;

    private IProductCmptLink currentLink;

    private boolean dataChangeable;

    /**
     * Creates a new Cardinality panel
     */
    public CardinalityPanel(Composite parent, UIToolkit toolkit) {
        // first create a composite to fill the complete space
        root = toolkit.createComposite(parent);
        GridLayout layout = new GridLayout(1, false);
        layout.marginHeight = 1;
        root.setLayout(layout);

        GridData layoutData = new GridData(SWT.FILL, SWT.FILL, false, false);
        root.setLayoutData(layoutData);

        // then, create a composite to be put at to and this
        // composite becomes the border!
        Composite kardinalityPane = toolkit.createLabelEditColumnComposite(root);
        layoutData = new GridData(SWT.FILL, SWT.FILL, false, false);
        layoutData.verticalAlignment = SWT.TOP;
        kardinalityPane.setLayoutData(layoutData);
        GridLayout kardinalityPaneLayout = (GridLayout)kardinalityPane.getLayout();
        kardinalityPaneLayout.numColumns = 2;
        kardinalityPaneLayout.horizontalSpacing = 1;

        // create header
        Label headerLabel = toolkit.createLabel(kardinalityPane, Messages.RelationsSection_cardinality);
        toolkit.setHorizontalSpan(headerLabel, 3);
        ((GridData)headerLabel.getLayoutData()).horizontalAlignment = SWT.CENTER;

        // create radio buttons
        KardinalitySelectionListener listener = new KardinalitySelectionListener();
        optional = toolkit.createRadioButton(kardinalityPane, Messages.CardinalityPanel_labelOptional);
        optional.addSelectionListener(listener);
        optional.setLayoutData(new GridData());
        toolkit.setHorizontalSpan(optional, 2);
        mandatory = toolkit.createRadioButton(kardinalityPane, Messages.CardinalityPanel_labelMandatory);
        mandatory.addSelectionListener(listener);
        mandatory.setLayoutData(new GridData());
        toolkit.setHorizontalSpan(mandatory, 2);
        other = toolkit.createRadioButton(kardinalityPane, Messages.CardinalityPanel_labelOther);
        other.addSelectionListener(listener);

        // Min/Max bei Other, Default zentriert darunter
        Composite otherPane = toolkit.createLabelEditColumnComposite(kardinalityPane);
        GridLayout specialPaneLayout = (GridLayout)otherPane.getLayout();
        specialPaneLayout.marginLeft = 4;
        specialPaneLayout.numColumns = 3;
        specialPaneLayout.horizontalSpacing = 2;
        specialPaneLayout.marginHeight = 0;
        minCard = toolkit.createText(otherPane);
        GridData gd = new GridData(GridData.FILL, GridData.BEGINNING, true, false);
        gd.widthHint = 20;
        minCard.setLayoutData(gd);
        minMaxCardLabel = toolkit.createLabel(otherPane, ".."); //$NON-NLS-1$
        maxCard = toolkit.createText(otherPane);
        gd = new GridData(GridData.FILL, GridData.BEGINNING, true, false);
        gd.widthHint = 20;
        maxCard.setLayoutData(gd);

        Composite defaultPane = toolkit.createLabelEditColumnComposite(kardinalityPane);
        GridLayout defaultPaneLayout = (GridLayout)defaultPane.getLayout();
        defaultPaneLayout.marginWidth = 2;
        GridData defaultGridData = new GridData(GridData.FILL, GridData.BEGINNING, true, true);
        defaultGridData.horizontalSpan = 3;
        defaultPane.setLayoutData(defaultGridData);
        defaultCardLabel = toolkit.createLabel(defaultPane, Messages.CardinalityPanel_LabelDefaultCardinality);
        defaultCard = toolkit.createText(defaultPane);
        gd = new GridData(GridData.FILL, GridData.BEGINNING, true, false);
        /*
         * Layout optimization for GTK Linux. Indent makes the textField one pixel smaller on the
         * left side to align it with the min-cardinality field above. TODO Test Layout in Windows.
         */
        gd.horizontalIndent = 1;
        defaultCard.setLayoutData(gd);

        toolkit.createVerticalSpacer(kardinalityPane, 3).setBackground(kardinalityPane.getBackground());

        kardinalityPane.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TREE_BORDER);
        toolkit.getFormToolkit().paintBordersFor(root);
    }

    /**
     * Configures this {@link CardinalityPanel} to change the cardinality values of the given link
     * when the user modifies the text controls. If the given {@link IProductCmptLink} is
     * <code>null</code> this panel will be deactivated.
     * 
     * @param link the {@link IProductCmptLink} currently edited by this panel or <code>null</code>
     *            if none.
     */
    public void setProductCmptLinkToEdit(IProductCmptLink link) {
        currentLink = link;
        if (currentLink == null) {
            deactivate();
        } else {

            boolean cardinalityPanelEnabled;
            try {
                cardinalityPanelEnabled = currentLink.constrainsPolicyCmptTypeAssociation(currentLink.getIpsProject());
            } catch (CoreException e) {
                IpsPlugin.log(e);
                cardinalityPanelEnabled = false;
            }

            if (!cardinalityPanelEnabled) {
                deactivate();
                return;
            } else {
                if (uiController != null) {
                    removeFields();
                }
                if (uiController == null || !uiController.getIpsObjectPartContainer().equals(currentLink)) {
                    uiController = new IpsObjectUIController(currentLink);
                }
                addFields(currentLink);

                uiController.updateUI();
                setEnabled(true);
            }
        }
    }

    /**
     * Disables this panel (as in setEnabled(false)) and removes all values and selection from its
     * controls. The panel then displays no data.
     */
    public void deactivate() {
        setEnabled(false);

        removeFields();
        mandatory.setSelection(false);
        optional.setSelection(false);
        other.setSelection(false);
        minCard.setText(""); //$NON-NLS-1$
        maxCard.setText(""); //$NON-NLS-1$
        defaultCard.setText(""); //$NON-NLS-1$
    }

    private void removeFields() {
        if (uiController != null) {
            uiController.remove(minCardField);
            uiController.remove(maxCardField);
            uiController.remove(defaultCardField);
        }
    }

    private void addFields(IProductCmptLink link) {
        minCardField = new CardinalityField(getMinCardinalityTextControl());
        maxCardField = new CardinalityField(getMaxCardinalityTextControl());
        defaultCardField = new CardinalityField(getDefaultCardinalityTextControl());
        uiController.add(minCardField, link, IProductCmptLink.PROPERTY_MIN_CARDINALITY);
        uiController.add(maxCardField, link, IProductCmptLink.PROPERTY_MAX_CARDINALITY);
        uiController.add(defaultCardField, link, IProductCmptLink.PROPERTY_DEFAULT_CARDINALITY);
    }

    public void refresh() {
        if (uiController != null) {
            uiController.updateUI();
        }
    }

    /**
     * Method to enable or disable this panel. If no checkbox (optional, mandatory, other) is
     * selected all controls will be disabled, regardless of the given value. This is the case if no
     * ProdCmptLink is selected in the {@link LinksSection}s tree, thus this panel is disabled
     * completely.
     */
    public void setEnabled(boolean enabled) {

        boolean doEnable = enabled & isDataChangeable();

        mandatory.setEnabled(doEnable);
        optional.setEnabled(doEnable);
        other.setEnabled(doEnable);

        minCard.setEnabled(false);
        maxCard.setEnabled(false);
        defaultCard.setEnabled(false);
        minMaxCardLabel.setEnabled(false);
        defaultCardLabel.setEnabled(false);

        String min = minCard.getText();
        String max = maxCard.getText();
        String def = defaultCard.getText();

        if (min.equals("1") && max.equals("1") && def.equals("1")) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            mandatory.setSelection(true);
            optional.setSelection(false);
            other.setSelection(false);
        } else if (min.equals("0") && max.equals("1")) { //$NON-NLS-1$ //$NON-NLS-2$ 
            optional.setSelection(true);
            mandatory.setSelection(false);
            other.setSelection(false);
            defaultCard.setEnabled(doEnable);
            defaultCardLabel.setEnabled(doEnable);
        } else {
            other.setSelection(true);
            mandatory.setSelection(false);
            optional.setSelection(false);

            minCard.setEnabled(doEnable);
            maxCard.setEnabled(doEnable);
            minMaxCardLabel.setEnabled(doEnable);
            defaultCard.setEnabled(doEnable);
            defaultCardLabel.setEnabled(doEnable);
        }
    }

    /**
     * Listener to update on cardinality modifications.
     * 
     * @author Thorsten Guenther
     * @author Stefan Widmaier
     */
    private class KardinalitySelectionListener implements SelectionListener {

        @Override
        public void widgetSelected(SelectionEvent e) {

            boolean otherSelected = e.getSource() == other;
            boolean optionalSelected = e.getSource() == optional;
            minCard.setEnabled(otherSelected);
            maxCard.setEnabled(otherSelected);
            minMaxCardLabel.setEnabled(otherSelected);
            defaultCard.setEnabled(otherSelected | optionalSelected);
            defaultCardLabel.setEnabled(otherSelected | optionalSelected);

            /*
             * Setting the cardinality values using their fields causes an inconsistent GUI state
             * and incorrect model data. This is due to the fact that edit fields update the model
             * asynchronously (see EventBroadcaster). Following calls of setValue on other fields
             * might be "too early" as the Text controls will also be updated by triggered
             * modelChangeEvents that then write possibly incorrect or outdated data to the GUI.
             * 
             * Calling the model-setters directly forces synchronous processing and works around
             * those problems.
             */
            if (e.getSource() == optional) {
                currentLink.setMinCardinality(0);
                currentLink.setMaxCardinality(1);
            } else if (e.getSource() == mandatory) {
                currentLink.setMinCardinality(1);
                currentLink.setMaxCardinality(1);
                currentLink.setDefaultCardinality(1);
            }

        }

        @Override
        public void widgetDefaultSelected(SelectionEvent e) {
            widgetSelected(e);
        }

    }

    protected void setFieldValue(CardinalityField field, Integer integer, boolean triggerValueChanged) {
        if (field != null) {
            field.setValue(integer, triggerValueChanged);
        }
    }

    /**
     * Returns the Text control which displays the min cardinality
     */
    private Text getMinCardinalityTextControl() {
        return minCard;
    }

    /**
     * Returns the Text control which displays the max cardinality
     */
    private Text getMaxCardinalityTextControl() {
        return maxCard;
    }

    /**
     * Returns the control which displays the default cardinality
     */
    private Text getDefaultCardinalityTextControl() {
        return defaultCard;
    }

    @Override
    public boolean isDataChangeable() {
        return dataChangeable;
    }

    @Override
    public void setDataChangeable(boolean changeable) {
        dataChangeable = changeable;
        setEnabled(changeable);
    }

}
