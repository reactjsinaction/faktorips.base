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

package org.faktorips.devtools.core.ui.editors;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.faktorips.devtools.core.model.IIpsObject;
import org.faktorips.devtools.core.ui.UIToolkit;
import org.faktorips.devtools.core.ui.forms.IpsSection;


/**
 *
 */
public abstract class IpsObjectEditorPage extends FormPage {
    
    // the space between two sections 
    public final static int HORIZONTAL_SECTION_SPACE = 15;
    public final static int VERTICAL_SECTION_SPACE = 10;

    private boolean contentChangeable = true;
    
    /**
     * @param editor	The editor the page belongs to.
     * @param id		Page id used to identify the page.
     * @param title		The title shown at the top of the page when the page is selected.
     * @param title	The page name shown at the bottom of the editor as tab page. 
     */
    public IpsObjectEditorPage(
            IpsObjectEditor editor, 
            String id, 
            String tabPageName) {
        super(editor, id, tabPageName);
    }
    
    protected IpsObjectEditor getIpsObjectEditor() {
        return (IpsObjectEditor)getEditor();
    }
    
    /**
     * Return the ips object of the ips src file beeing edited.
     * Returns <code>null</code> if the src file couldn't determine the ips object
     * (e.g. if the src file is stored outside an ips package)
     */
    protected IIpsObject getIpsObject() {
        if (getIpsObjectEditor().getIpsSrcFile().exists()){
            return getIpsObjectEditor().getIpsObject();
        } else {
            return null;
        }
    }
    
	protected final void createFormContent(IManagedForm managedForm) {
		super.createFormContent(managedForm);
		ScrolledForm form = managedForm.getForm();
        if (getIpsObject() == null){
            // no valid ips src file, create nothing
            return;
        }
		form.setText(getIpsObjectEditor().getUniformPageTitle());
		FormToolkit toolkit = managedForm.getToolkit();
		createPageContent(form.getBody(), new UIToolkit(toolkit));
		form.setExpandHorizontal(true);
		form.setExpandVertical(true);
		form.reflow(true);
        registerSelectionProviderActivation(getPartControl());
	}
	
    protected final void registerSelectionProviderActivation(Control container){
        if(container instanceof ISelectionProviderActivation){
            getIpsObjectEditor().getSelectionProviderDispatcher().addSelectionProviderActivation((ISelectionProviderActivation)container);
        }
        if(!(container instanceof Composite)){
            return;
        }
        Control[] childs = ((Composite)container).getChildren();
        for (int i = 0; i < childs.length; i++) {
            if(childs[i] instanceof Composite){
                registerSelectionProviderActivation(childs[i]);
            }
        }
        
    }

	protected abstract void createPageContent(Composite formBody, UIToolkit toolkit);
	
	/**
	 * Creates a grid layout for the page with the indicated number of columns
	 * and the default margins.
	 * 
	 * @param numOfColumns	Number of columns in the grid.
	 * @param equalSize		True if the columns should have the same size
	 */
	protected GridLayout createPageLayout(int numOfColumns, boolean equalSize) {
	    GridLayout layout = new GridLayout(numOfColumns, equalSize);
	    layout.marginHeight = 10;
	    layout.marginWidth = 10;
	    layout.verticalSpacing = VERTICAL_SECTION_SPACE;
	    layout.horizontalSpacing = HORIZONTAL_SECTION_SPACE;
	    return layout;
	}
	
	/**
	 * Creates a grid composite for the inner page structure. The composite
	 * has no margins but the default spacing settings.
	 * 
	 * @param numOfColumns	Number of columns in the grid.
	 * @param equalSize		True if the columns should have the same size
	 */
	protected Composite createGridComposite(
	        UIToolkit toolkit, 
	        Composite parent, 
	        int numOfColumns, 
	        boolean equalSize,
	        int gridData) {
	    Composite composite = toolkit.getFormToolkit().createComposite(parent);
	    GridLayout layout = new GridLayout(numOfColumns, equalSize);
	    layout.marginHeight = 0;
	    layout.marginWidth = 0;
	    layout.verticalSpacing = VERTICAL_SECTION_SPACE;
	    layout.horizontalSpacing = HORIZONTAL_SECTION_SPACE;
	    composite.setLayout(layout);
	    composite.setLayoutData(new GridData(gridData));
	    return composite;
	}
	
    /**
     * Refreshes the page with the data from the model.
     * 
     * Default implementation refreshs all ancestors that are instances
     * of <code>IpsSection</code>. By ancestors we mean the children of the
     * composite that represents this page and their children.
     */
    protected void refresh() {
        if (!(getPartControl() instanceof Composite)) {
            return;
        }
        refresh((Composite)getPartControl());
    }
    
    private void refresh(Composite composite) {
        Control[] children =composite.getChildren();
        for (int i=0; i<children.length; i++) {
            if (children[i] instanceof IpsSection) {
                ((IpsSection)children[i]).refresh();
            }
            else if (children[i] instanceof Composite) {
                refresh((Composite)children[i]);
            }
        }
        
    }
    
    /**
     * Returns <code>true</code> if the content shown on this page is changeable,
     * otherwise false.
     */
    public boolean isContentChangeable() {
        return contentChangeable;
    }
    
    /**
     * Evaluates the new content changeable state and updates it, if it has changed.
     * If the user can't change the the editor's content at all, he also can't change the
     * content shown on this page. If the user can change editor's content in general,
     * the computeContentChangeableState() is called to evaluate if the content shown on
     * this page can be changed. 
     */
    public void updateContentChangeableState() {
        if (getIpsObjectEditor().isContentChangeable()==null || !getIpsObjectEditor().isContentChangeable().booleanValue()) {
            setContentChangeable(false);
        } else {
            setContentChangeable(computeContentChangeableState());
        }
    }

    /**
     * Evaluates if if the content shown on this page is changeable by the user. 
     * This method does not consider the state of the ips object editor. 
     * 
     * The default implementation returns <code>true</code>, subclasses may override.
     */
    protected boolean computeContentChangeableState() {
        return true;
    }
    
    /**
     * Resets the content changeable state to it's default, which is <code>true</code>,
     * so that it maches the initial state of controls which are be default enabled and editable.
     */
    protected void resetContentChangeableState() {
        contentChangeable = true;
    }
        
    private void setContentChangeable(boolean changeable) {
        if (changeable==this.contentChangeable) {
            return;
        }
        this.contentChangeable = changeable;
        setContentChangeable((Composite)getPartControl(), changeable);
    }
    
    private void setContentChangeable(Composite composite, boolean editable) {
        Control[] children =composite.getChildren();
        for (int i=0; i<children.length; i++) {
            if (children[i] instanceof IpsSection) {
                ((IpsSection)children[i]).setContentChangeable(editable);
            }
            else if (children[i] instanceof Composite) {
                setContentChangeable((Composite)children[i], editable);
            }
        }
    }

}
