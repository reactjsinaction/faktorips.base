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

package org.faktorips.devtools.core.ui.views.productstructureexplorer;

import java.util.GregorianCalendar;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ViewerLabel;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;
import org.faktorips.devtools.core.IpsPlugin;
import org.faktorips.devtools.core.model.ipsobject.IIpsObjectGeneration;
import org.faktorips.devtools.core.model.productcmpt.IProductCmpt;
import org.faktorips.devtools.core.model.productcmpt.ITableContentUsage;
import org.faktorips.devtools.core.model.productcmpt.treestructure.IProductCmptReference;
import org.faktorips.devtools.core.model.productcmpt.treestructure.IProductCmptStructureTblUsageReference;
import org.faktorips.devtools.core.model.productcmpt.treestructure.IProductCmptTypeRelationReference;
import org.faktorips.devtools.core.model.productcmpttype.IProductCmptTypeAssociation;
import org.faktorips.devtools.core.ui.IpsUIPlugin;
import org.faktorips.devtools.core.ui.internal.adjustmentdate.AdjustmentDate;
import org.faktorips.util.StringUtil;

public class ProductStructureLabelProvider extends LabelProvider {

    private AdjustmentDate adjustmentDate;

    private boolean showTableStructureUsageName = false;

    /**
     * {@inheritDoc}
     */
    @Override
    public Image getImage(Object element) {
        if (element instanceof IProductCmptReference) {
            return IpsUIPlugin.getImageHandling().getImage(((IProductCmptReference)element).getProductCmpt());
        } else if (element instanceof IProductCmptTypeRelationReference) {
            return IpsUIPlugin.getImageHandling().getImage(((IProductCmptTypeRelationReference)element).getRelation());
        } else if (element instanceof IProductCmptStructureTblUsageReference) {
            return IpsUIPlugin.getImageHandling().getImage(
                    ((IProductCmptStructureTblUsageReference)element).getTableContentUsage());
        } else if (element instanceof ViewerLabel) {
            return ((ViewerLabel)element).getImage();
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getText(Object element) {
        if (element instanceof IProductCmptReference) {
            return getProductCmptLabel(((IProductCmptReference)element).getProductCmpt());
        } else if (element instanceof IProductCmptTypeRelationReference) {
            IProductCmptTypeAssociation association = ((IProductCmptTypeRelationReference)element).getRelation();
            // if the cardinality of the association is "toMany" then show the name (target role
            // name) in plural
            // otherwise show the default name, which normally is the singular target role name
            if (association.is1ToMany()) {
                return association.getTargetRolePlural();
            } else {
                return association.getName();
            }
        } else if (element instanceof IProductCmptStructureTblUsageReference) {
            ITableContentUsage tcu = ((IProductCmptStructureTblUsageReference)element).getTableContentUsage();
            String tableUsageLabelText = showTableStructureUsageName ? tcu.getStructureUsage() + ": " : ""; //$NON-NLS-1$ //$NON-NLS-2$
            return StringUtils.capitalize(tableUsageLabelText) + StringUtil.unqualifiedName(tcu.getTableContentName());
        } else if (element instanceof ViewerLabel) {
            return ((ViewerLabel)element).getText();
        }
        return Messages.ProductStructureLabelProvider_undefined;
    }

    public String getProductCmptLabel(IProductCmpt productCmpt) {
        String label = productCmpt.getName();

        GregorianCalendar date = null;
        if (getAdjustmentDate() != null) {
            date = getAdjustmentDate().getValidFrom();
        }
        IIpsObjectGeneration generation = productCmpt.findGenerationEffectiveOn(date);

        if (generation == null) {
            // no generations avaliable,
            // show additional text to inform that no generations exists
            String generationText = IpsPlugin.getDefault().getIpsPreferences().getChangesOverTimeNamingConvention()
                    .getGenerationConceptNameSingular();
            label = NLS.bind(Messages.ProductStructureExplorer_label_NoGenerationForCurrentWorkingDate, label,
                    generationText);
        } else {
            AdjustmentDate genAdjDate = new AdjustmentDate(generation.getValidFrom(), generation.getValidTo());
            if (!genAdjDate.equals(adjustmentDate)) {
                label += " (" + genAdjDate.getText() + ")";
            }
        }
        return label;
    }

    /**
     * Definines if the table content usage role name will be displayed beside the referenced table
     * content (<code>true</code>), or if the corresponding table structure usage name will be
     * hidden (<code>false</code>).
     */
    public void setShowTableStructureUsageName(boolean showTableStructureUsageName) {
        this.showTableStructureUsageName = showTableStructureUsageName;
    }

    /**
     * Returns <code>true</code> if the table structure usage role name will be displayed or not.
     */
    public boolean isShowTableStructureUsageName() {
        return showTableStructureUsageName;
    }

    public void setAdjustmentDate(AdjustmentDate adjustmentDate) {
        this.adjustmentDate = adjustmentDate;
    }

    public AdjustmentDate getAdjustmentDate() {
        return adjustmentDate;
    }
}
