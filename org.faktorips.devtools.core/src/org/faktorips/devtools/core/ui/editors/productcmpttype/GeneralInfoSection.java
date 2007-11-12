/*******************************************************************************
 * Copyright (c) 2005,2006 Faktor Zehn GmbH und andere.
 *
 * Alle Rechte vorbehalten.
 *
 * Dieses Programm und alle mitgelieferten Sachen (Dokumentationen, Beispiele,
 * Konfigurationen, etc.) d�rfen nur unter den Bedingungen der 
 * Faktor-Zehn-Community Lizenzvereinbarung � Version 0.1 (vor Gr�ndung Community) 
 * genutzt werden, die Bestandteil der Auslieferung ist und auch unter
 *   http://www.faktorips.org/legal/cl-v01.html
 * eingesehen werden kann.
 *
 * Mitwirkende:
 *   Faktor Zehn GmbH - initial API and implementation 
 *
 *******************************************************************************/

package org.faktorips.devtools.core.ui.editors.productcmpttype;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.Section;
import org.faktorips.devtools.core.IpsPlugin;
import org.faktorips.devtools.core.model.pctype.IPolicyCmptType;
import org.faktorips.devtools.core.model.productcmpttype.IProductCmptType;
import org.faktorips.devtools.core.ui.UIToolkit;
import org.faktorips.devtools.core.ui.controls.Checkbox;
import org.faktorips.devtools.core.ui.controls.PcTypeRefControl;
import org.faktorips.devtools.core.ui.controls.ProductCmptType2RefControl;
import org.faktorips.devtools.core.ui.forms.IpsSection;

/**
 * Section to edit supertype, abstract flag and configured policy component type.
 * 
 * @author Jan Ortmann
 */
public class GeneralInfoSection extends IpsSection {

    private IProductCmptType type;
    
    /**
     * @param parent
     * @param style
     * @param layoutData
     * @param toolkit
     */
    public GeneralInfoSection(IProductCmptType type, Composite parent, UIToolkit toolkit) {
        super(parent, Section.TITLE_BAR, GridData.FILL_HORIZONTAL, toolkit);
        this.type = type;
        initControls();
        setText(Messages.GeneralInfoSection_title);
    }

    /**
     * {@inheritDoc}
     */
    protected void initClientComposite(Composite client, UIToolkit toolkit) {
        client.setLayout(new GridLayout(1, false));
        Composite composite = toolkit.createLabelEditColumnComposite(client);

        // super type
        Hyperlink link = toolkit.createHyperlink(composite, Messages.GeneralInfoSection_supertypeLabel);
        link.addHyperlinkListener(new HyperlinkAdapter() {
    
            public void linkActivated(HyperlinkEvent event) {
                try {
                    IpsPlugin.getDefault().openEditor(type.findSupertype(type.getIpsProject()));
                } catch (Exception e) {
                    IpsPlugin.logAndShowErrorDialog(e);
                }
                
            }
            
        });
        
        ProductCmptType2RefControl supertypeRefControl = new ProductCmptType2RefControl(type.getIpsProject(), composite, toolkit, false);
        bindingContext.bindContent(supertypeRefControl, type, IProductCmptType.PROPERTY_SUPERTYPE); 

        // abstract flag
        toolkit.createLabel(composite, Messages.GeneralInfoSection_abstractLabel);
        Checkbox abstractCheckbox = toolkit.createCheckbox(composite);
        bindingContext.bindContent(abstractCheckbox, type, IProductCmptType.PROPERTY_ABSTRACT);
        
        // reference to policy component type
        toolkit.createFormLabel(composite, Messages.GeneralInfoSection_configuresLabel);
        Checkbox configuratedCheckbox = toolkit.createCheckbox(composite);
        bindingContext.bindContent(configuratedCheckbox, type, IProductCmptType.PROPERTY_CONFIGURATION_FOR_POLICY_CMPT_TYPE);
        
        link = toolkit.createHyperlink(composite, Messages.GeneralInfoSection_configuredTypeLabel);
        link.addHyperlinkListener(new HyperlinkAdapter() {
    
            public void linkActivated(HyperlinkEvent event) {
                try {
                    IPolicyCmptType policyCmptType = type.findPolicyCmptType(type.getIpsProject());
                    if (policyCmptType!=null) {
                        IpsPlugin.getDefault().openEditor(policyCmptType);
                    }
                } catch (Exception e) {
                    IpsPlugin.logAndShowErrorDialog(e);
                }
                
            }
            
        });
        
        PcTypeRefControl control = toolkit.createPcTypeRefControl(type.getIpsProject(), composite);
        bindingContext.bindContent(control, type, IProductCmptType.PROPERTY_POLICY_CMPT_TYPE); 
        bindingContext.bindEnabled(control, type, IProductCmptType.PROPERTY_CONFIGURATION_FOR_POLICY_CMPT_TYPE); 
    }

    /**
     * {@inheritDoc}
     */
    protected void performRefresh() {
        bindingContext.updateUI();
    }

}
