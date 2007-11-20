/***************************************************************************************************
 *  * Copyright (c) 2005,2006 Faktor Zehn GmbH und andere.  *  * Alle Rechte vorbehalten.  *  *
 * Dieses Programm und alle mitgelieferten Sachen (Dokumentationen, Beispiele,  * Konfigurationen,
 * etc.) duerfen nur unter den Bedingungen der  * Faktor-Zehn-Community Lizenzvereinbarung - Version
 * 0.1 (vor Gruendung Community)  * genutzt werden, die Bestandteil der Auslieferung ist und auch
 * unter  *   http://www.faktorips.org/legal/cl-v01.html  * eingesehen werden kann.  *  *
 * Mitwirkende:  *   Faktor Zehn GmbH - initial API and implementation - http://www.faktorzehn.de  *  
 **************************************************************************************************/

package org.faktorips.devtools.core.ui.editors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IPartService;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.IFormPage;
import org.faktorips.devtools.core.IpsPlugin;
import org.faktorips.devtools.core.IpsPreferences;
import org.faktorips.devtools.core.internal.model.ipsobject.IpsSrcFileImmutable;
import org.faktorips.devtools.core.model.ContentChangeEvent;
import org.faktorips.devtools.core.model.ContentsChangeListener;
import org.faktorips.devtools.core.model.IIpsModel;
import org.faktorips.devtools.core.model.IModificationStatusChangeListener;
import org.faktorips.devtools.core.model.ModificationStatusChangedEvent;
import org.faktorips.devtools.core.model.ipsobject.IFixDifferencesToModelSupport;
import org.faktorips.devtools.core.model.ipsobject.IIpsObject;
import org.faktorips.devtools.core.model.ipsobject.IIpsSrcFile;
import org.faktorips.devtools.core.model.ipsobject.IpsObjectType;
import org.faktorips.devtools.core.model.ipsproject.IIpsProject;

/**
 * Base class for all editors to edit ips objects.
 * 
 * <p>This editor uses an implementation of ISelectionProvider where ISelectionProviders
 * used on the different pages of this editor can be registered. The ISelectionProvider of this
 * editor is registered at the selection service of the workbench so that only this selection
 * provider is the active one within the workbench when this editor is active. Implementations of
 * ISelectionProvider that are used on the pages of this editor have to be registered at the
 * SelectionProviderDispatcher the ISelectionProvider of this editor. The dispatcher finds the
 * currently active of all registered selection providers and forwards requests to it. There are two
 * ways of registering with the SelectionProviderDispatcher.
 * <ol>
 * <li>The <code>Composite</code> where the control of the ISelectionProvider implementation e.g. a
 * TreeViewer is added to has to implement the {@link ISelectionProviderActivation} interface. The
 * editor will track all the implementations of this interface at initialization time and register
 * them with the dispatcher. 
 * </li> 
 * <li>The dispatcher can be retrieved by the
 * getSelectionProviderDispatcher() method of this editor and an
 * {@link ISelectionProviderActivation} can be registered manually.
 * </li>
 * </ol>
 */
public abstract class IpsObjectEditor extends FormEditor 
    implements ContentsChangeListener, IModificationStatusChangeListener,
        IResourceChangeListener, IPropertyChangeListener {

    public final static boolean TRACE = IpsPlugin.TRACE_UI;

    /*
     * Setting key for user's decision not to fix the differences between the
     * product definition structure and the model structure
     */
    private final static String SETTING_DONT_FIX_DIFFERENCES = "dontFixDifferences"; //$NON-NLS-1$

    // the file that's being edited (if any)
    private IIpsSrcFile ipsSrcFile;

    // dirty flag
    private boolean dirty = false;

    private Boolean contentChangeable = null;
    
    // the editor's ISelectionProvider 
    private SelectionProviderDispatcher selectionProviderDispatcher;

    /*
     * Storage for the user's decision not to load the changes made directly in the
     * file system.
     */
    private boolean dontLoadChanges = false;
    
    private boolean isCheckingForChangesMadeOutsideEclipse = false;
    
    /*
     * True if the editor contains the pages that are shown for a parsable ips source file,
     * false if an error page is shown.
     */
    private boolean pagesForParsableSrcFileShown;
    
    private boolean updatingPageStructure = false;
    
    private ActivationListener activationListener;

    /* Updates the title image if there are ips marker changes on the editor's input */
    private IpsObjectEditorErrorTickUpdater errorTickupdater;
    
    public IpsObjectEditor() {
        super();
        errorTickupdater = new IpsObjectEditorErrorTickUpdater(this);
    }

    /**
     * Returns the file being edited.
     */
    public IIpsSrcFile getIpsSrcFile() {
        return ipsSrcFile;
    }
    
    /**
     * Shortcut for getIpsSrcFile().getIpsProject().
     */
    public IIpsProject getIpsProject() {
        return ipsSrcFile.getIpsProject();
    }

    /**
     * Returns the ips object of the ips src file currently edited, returns <code>null</code> if
     * the ips object not exists (e.g. if the ips src file is outside an ips package.
     */
    public IIpsObject getIpsObject() {
        try {
            if (getIpsSrcFile().exists()) {
                return getIpsSrcFile().getIpsObject();
            }
            else {
                return null;
            }
        } catch (Exception e) {
            IpsPlugin.logAndShowErrorDialog(e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the title that is shown on every page.
     */
    protected abstract String getUniformPageTitle();

    /**
     * {@inheritDoc}
     */
    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
        if (TRACE) {
            logMethodStarted("init"); //$NON-NLS-1$
        }
        super.init(site, input);
        IIpsModel model = IpsPlugin.getDefault().getIpsModel();

        if (input instanceof IFileEditorInput) {
            IFile file = ((IFileEditorInput)input).getFile();
            ipsSrcFile = (IIpsSrcFile)model.getIpsElement(file);
        } else if (input instanceof IpsArchiveEditorInput) {
            ipsSrcFile = ((IpsArchiveEditorInput)input).getIpsSrcFile();
        } else if (input instanceof IStorageEditorInput) {
            initFromStorageEditorInput((IStorageEditorInput)input);
            setPartName(((IStorageEditorInput)input).getName());
        }

        if (ipsSrcFile == null) {
            throw new PartInitException("Unsupported editor input type " + input.getClass().getName()); //$NON-NLS-1$
        } 
        
        if(!(input instanceof IStorageEditorInput)){
            setPartName(ipsSrcFile.getName());
        }

        if (ipsSrcFile.isMutable() && !ipsSrcFile.getEnclosingResource().isSynchronized(0)) {
            try {
                ipsSrcFile.getEnclosingResource().refreshLocal(0, null);
            } catch (CoreException e) {
                throw new PartInitException("Error refreshing resource " + ipsSrcFile.getEnclosingResource()); //$NON-NLS-1$
            }
        }
        
        // check if the ips src file is valid and could be edited in the editor,
        // if the ips src file doesn't exists (e.g. ips src file outside ips package)
        // close the editor and open the current file in the default text editor
        if (!ipsSrcFile.exists()) {
            Runnable closeRunnable = new Runnable() {
                public void run() {
                    IpsObjectEditor.this.close(false);
                    IpsPlugin.getDefault().openEditor(ipsSrcFile.getCorrespondingFile());
                }
            };
            getSite().getShell().getDisplay().syncExec(closeRunnable);
        } else {
            activationListener = new ActivationListener(site.getPage());
            selectionProviderDispatcher = new SelectionProviderDispatcher();
            site.setSelectionProvider(selectionProviderDispatcher);
        }
        
        setDataChangeable(computeDataChangeableState());

        ResourcesPlugin.getWorkspace().addResourceChangeListener(IpsObjectEditor.this);
        IpsPlugin.getDefault().getIpsModel().addChangeListener(IpsObjectEditor.this);
        IpsPlugin.getDefault().getIpsModel().addModifcationStatusChangeListener(IpsObjectEditor.this);
        IpsPlugin.getDefault().getIpsPreferences().addChangeListener(IpsObjectEditor.this);
        if (TRACE) {
            logMethodFinished("init"); //$NON-NLS-1$
        }
    }

    private void initFromStorageEditorInput(IStorageEditorInput input) throws PartInitException {
        if (TRACE) {
            logMethodStarted("initFromStorageEditorInput"); //$NON-NLS-1$
        }
        try {
            IStorage storage = input.getStorage();
            IPath path = storage.getFullPath();
            if (path == null) {
                return;
            }

            String extension = IpsObjectType.PRODUCT_CMPT.getFileExtension();
            int nameIndex = path.lastSegment().indexOf(extension);

            IpsObjectType[] types = IpsObjectType.ALL_TYPES;
            for (int i = 0; i < types.length; i++) {
                extension = types[i].getFileExtension();
                nameIndex = path.lastSegment().indexOf(extension);
                if (nameIndex != -1) {
                    break;
                }
            }

            if (nameIndex == -1) {
                return;
            }
            String name = path.lastSegment().substring(0, nameIndex) + extension;
            ipsSrcFile = new IpsSrcFileImmutable(name, storage.getContents());
            if (TRACE) {
                logMethodFinished("initFromStorageEditorInput"); //$NON-NLS-1$
            }
        } catch (CoreException e) {
            throw new PartInitException(e.getStatus());
        } catch (Exception e) {
            IpsPlugin.log(e);
            throw new PartInitException(e.getMessage());
        }
    }
    
    /**
     * {@inheritDoc}
     */
    final protected void addPages() {
        if (TRACE) {
            logMethodStarted("addPages"); //$NON-NLS-1$
        }        
        pagesForParsableSrcFileShown = false;
        try {
            if (!getIpsSrcFile().isContentParsable()) {
                if (TRACE) {
                    log("addPages(): Page for unparsable files created."); //$NON-NLS-1$
                }
                addPage(new UnparsableFilePage(this));
                return;
            }
            if (!ipsSrcFile.exists()) {
                if (TRACE) {
                    log("addPages(): Page for missing files created."); //$NON-NLS-1$
                }
                addPage(new MissingResourcePage(this));
                return;
            }
            if (TRACE) {
                logMethodStarted("addPagesForParsableSrcFile()"); //$NON-NLS-1$
            }
            addPagesForParsableSrcFile();
            if (TRACE) {
                logMethodFinished("addPagesForParsableSrcFile()"); //$NON-NLS-1$
            }
            pagesForParsableSrcFileShown = true;
            if (TRACE) {
                logMethodFinished("addPages"); //$NON-NLS-1$
            }        
        } catch (Exception e) {
            IpsPlugin.log(e);
        }
    }
    
    protected abstract void addPagesForParsableSrcFile() throws PartInitException, CoreException;
    
    protected void updatePageStructure() {
        if (TRACE) {
            logMethodStarted("updatePageStructure"); //$NON-NLS-1$
        }        
        try {
            if (getIpsSrcFile().isContentParsable()==pagesForParsableSrcFileShown) {
                return;
            }
            updatingPageStructure = true;
            ipsSrcFile.getIpsObject();
            // remove all pages
            for (int i=getPageCount(); i>0; i--) {
                removePage(0);
            }
            if (TRACE) {
                System.out.println("updatePageStructure(): Existing pages removed. Must recreate."); //$NON-NLS-1$
            }
            addPages();
            updatingPageStructure = false;
            super.setActivePage(0); // also triggers the refresh
            if (TRACE) {
                logMethodFinished("updatePageStructure"); //$NON-NLS-1$
            }        
        } catch (CoreException e) {
            updatingPageStructure = false;
            IpsPlugin.log(e);
            return;
        } 
    }

    /**
     * {@inheritDoc}
     */
    protected void setActivePage(int pageIndex) {
        super.setActivePage(pageIndex);
        refresh();
    }

    /**
     * Returns the active IpsObjectEditorPage. If the active page is not an instance of IpsObjectEditorPage 
     * <code>null</code> will be returned.
     */
    public IpsObjectEditorPage getActiveIpsObjectEditorPage(){
        IFormPage page = getActivePageInstance();
        if(page instanceof IpsObjectEditorPage){
            return (IpsObjectEditorPage)getActivePageInstance();
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    protected void pageChange(int newPageIndex) {
        if (TRACE) {
            logMethodStarted("pageChange(): newPage=" + newPageIndex); //$NON-NLS-1$
        }
        super.pageChange(newPageIndex); // must be called even if the file isn't parsable, 
        // (otherwise the unparsable file page wouldn't be shown)
        refresh();
        if (TRACE) {
            logMethodFinished("pageChange(): newPage=" + newPageIndex); //$NON-NLS-1$
        }
    }

    /**
     * Refreshes the controls on the active page with the data from the model.<br>
     * Calls to this refresh method are ignored if the activate attribute is set to
     * <code>false</code>.
     */
    protected void refresh() {
        if (updatingPageStructure) {
            return;
        }
        if (!ipsSrcFile.exists()) {
            return;
        }
        try {
            if (!ipsSrcFile.isContentParsable()) {
                return;
            }
            // here we have to request the ips object once, to make sure that 
            // it's state is is synchronized with the enclosing resource.
            // otherwise if some part of the ui keeps a reference to the ips object, it won't contain
            // the correct state.
            ipsSrcFile.getIpsObject(); 
        } catch (CoreException e) {
            IpsPlugin.log(e);
        }
        if (TRACE) {
            logMethodStarted("refresh"); //$NON-NLS-1$
        }        
        IEditorPart editor = getActivePageInstance();
        if (editor instanceof IpsObjectEditorPage) {
            IpsObjectEditorPage page = (IpsObjectEditorPage)editor;
            page.refresh();
        }
        updateDataChangeableState();
        if (TRACE) {
            logMethodFinished("refresh"); //$NON-NLS-1$
        }        
    }
    
    /**
     * Evaluates the new data changeable state and updates it, if it has changed.
     */
    public void updateDataChangeableState() {
        if (TRACE) {
            logMethodStarted("updateDataChangeable"); //$NON-NLS-1$
        }
        boolean newState = computeDataChangeableState();
        if (TRACE) {
            log("Next data changeable state=" + newState + ", oldState=" + isDataChangeable()); //$NON-NLS-1$ //$NON-NLS-2$
        }        
        setDataChangeable(newState);
        IEditorPart editor = getActivePageInstance();
        if (editor instanceof IpsObjectEditorPage) {
            IpsObjectEditorPage page = (IpsObjectEditorPage)editor;
            page.updateDataChangeableState();
        }
        if (TRACE) {
            logMethodFinished("updateDataChangeable"); //$NON-NLS-1$
        }        
    }
    
    /**
     * Evaluates if if the data shown in this editor is changeable by the user. 
     * The data is changeable if the the ips source file shown
     * in the editor is mutable and the working mode preference is set to edit mode.
     * 
     * Subclasses may override this method.
     */
    protected boolean computeDataChangeableState() {
        return ipsSrcFile.isMutable() && IpsPlugin.getDefault().getIpsPreferences().isWorkingModeEdit();
    }
    
    /**
     * Returns <code>true</code> if the data shown in this editor is changeable by the user, 
     * otherwise <code>false</code>. 
     */
    public final Boolean isDataChangeable() {
        return contentChangeable;
    }
    
    /**
     * Sets the content changeable state. This method is final. If you want to change an editor's
     * data changeable behaviour override {@link #computeDataChangeableState()}.
     */
    final protected void setDataChangeable(boolean changeable) {
        this.contentChangeable = Boolean.valueOf(changeable);
        if (getIpsSrcFile()!=null) {
            this.setTitleImage(errorTickupdater.getDecoratedImage());
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public void contentsChanged(final ContentChangeEvent event) {
        if (!event.getIpsSrcFile().equals(ipsSrcFile)) {
            return;
        }
        Display display = IpsPlugin.getDefault().getWorkbench().getDisplay();
        display.syncExec(new Runnable() {

            public void run() {
                if (TRACE) {
                    logMethodStarted("contentsChanged(): Received content changed event for the file being edited." + event.getEventType()); //$NON-NLS-1$
                }        
                if (event.getEventType()==ContentChangeEvent.TYPE_WHOLE_CONTENT_CHANGED) {
                    updatePageStructure();
                } else {
                    refresh();
                }
                if (TRACE) {
                    logMethodFinished("contentChanged()"); //$NON-NLS-1$
                }
            }
        });
    }
    
    /**
     * {@inheritDoc}
     */
    public void modificationStatusHasChanged(ModificationStatusChangedEvent event) {
        if (!ipsSrcFile.equals(event.getIpsSrcFile())) {
            return;
        }
        setDirty(ipsSrcFile.isDirty());
    }

    protected void setDirty(boolean newValue) {
        if (dirty == newValue) {
            return;
        }
        dirty = newValue;
        firePropertyChange(IEditorPart.PROP_DIRTY);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isDirty() {
        return dirty;
    }

    /**
     * {@inheritDoc}
     */
    public void doSave(IProgressMonitor monitor) {
        try {
            ipsSrcFile.save(true, monitor);
        } catch (Exception e) {
            IpsPlugin.logAndShowErrorDialog(e);
        }
        setDirty(ipsSrcFile.isDirty());
    }

    /**
     * {@inheritDoc}
     */
    public void doSaveAs() {
    }

    /**
     * {@inheritDoc}
     */
    public boolean isSaveAsAllowed() {
        return false;
    }

    /**
     * We have to close the editor if the underlying resource is removed. 
     * 
     * {@inheritDoc}
     */
    public void resourceChanged(IResourceChangeEvent event) {
        IResource enclResource = ipsSrcFile.getEnclosingResource();
        if (enclResource == null || event.getDelta() == null
                || event.getDelta().findMember(enclResource.getFullPath()) == null) {
            return;
        }
        if (TRACE) {
            logMethodStarted("resourceChanged(): Received resource changed event for the file being edited."); //$NON-NLS-1$
        }
        if (!ipsSrcFile.exists()) {
            this.close(false);
        }
        if (TRACE) {
            logMethodFinished("resourceChanged()"); //$NON-NLS-1$
        }
    }

    /**
     * Returns <code>true</code> if the <code>IIpsSrcFile</code> this editor is based on exists
     * and is in sync.
     */
    protected boolean isSrcFileUsable() {
        return ipsSrcFile != null && ipsSrcFile.exists()
                && ipsSrcFile.getEnclosingResource().isSynchronized(IResource.DEPTH_ONE);
    }
    
    /**
     * Returns <code>true</code> if this is the active editor, otherwise <code>false</code>. 
     */
    protected boolean isActive() {
        return this==getSite().getPage().getActiveEditor();
    }

    protected void handleEditorActivation() {
        if (TRACE) {
            logMethodStarted("handleEditorActivation()"); //$NON-NLS-1$
        }
        checkForChangesMadeOutsideEclipse();
        editorActivated();
        refresh();
        if (TRACE) {
            logMethodFinished("handleEditorActivation()"); //$NON-NLS-1$
        }
    }

    private void checkForChangesMadeOutsideEclipse() {
        if (dontLoadChanges || isCheckingForChangesMadeOutsideEclipse) {
            return;
        }
        try {
            isCheckingForChangesMadeOutsideEclipse = true;
            if (TRACE) {
                logMethodStarted("checkForChangesMadeOutsideEclipse()"); //$NON-NLS-1$
            }
            if (getIpsSrcFile().isMutable() && !getIpsSrcFile().getEnclosingResource().isSynchronized(0)) {
                MessageDialog dlg = new MessageDialog(Display.getCurrent().getActiveShell(), Messages.IpsObjectEditor_fileHasChangesOnDiskTitle, (Image)null, 
                        Messages.IpsObjectEditor_fileHasChangesOnDiskMessage, MessageDialog.QUESTION,
                        new String[]{Messages.IpsObjectEditor_fileHasChangesOnDiskYesButton, Messages.IpsObjectEditor_fileHasChangesOnDiskNoButton}, 0);
                dlg.open();
                if (dlg.getReturnCode()==0) {
                    try {
                        if (TRACE) {
                            log("checkForChangesMadeOutsideEclipse(): Change found, sync file with filesystem (refreshLocal)"); //$NON-NLS-1$
                        }
                        getIpsSrcFile().getEnclosingResource().refreshLocal(0, null);
                        updatePageStructure();
                    } catch (CoreException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    dontLoadChanges = true;
                }
                
            }
            if (TRACE) {
                logMethodFinished("checkForChangesMadeOutsideEclipse()"); //$NON-NLS-1$
            }
        } finally {
            isCheckingForChangesMadeOutsideEclipse = false;
        }
    }

    /**
     * Called when the editor is activated (e.g. by clicking in it).
     */
    protected void editorActivated() {
        if (TRACE) {
            logMethodStarted("editorActivated()"); //$NON-NLS-1$
        }
        checkForInconsistenciesToModel();
        if (TRACE) {
            logMethodFinished("editorActivated()"); //$NON-NLS-1$
        }
    }
    
    /**
     * Does what the methodname says :-)
     */
    protected void checkForInconsistenciesToModel() {
        if (TRACE) {
            logMethodStarted("checkForInconsistenciesToModel"); //$NON-NLS-1$
        }
        if (isDataChangeable()==null || !isDataChangeable().booleanValue()) {
            if (TRACE) {
                logMethodFinished("checkForInconsistenciesToModel - no need to check, content is read-only."); //$NON-NLS-1$
            }
            return;
        }
        if (!getIpsSrcFile().exists()){
            if (TRACE) {
                logMethodFinished("checkForInconsistenciesToModel - no need to check, file does not exists."); //$NON-NLS-1$
            }
            return;
        }
        if (getSettings().getBoolean(getIpsSrcFile(), SETTING_DONT_FIX_DIFFERENCES)) {
            if (TRACE) {
                logMethodFinished("checkForInconsistenciesToModel - no need to check, user decided no to fix."); //$NON-NLS-1$
            }
            return;
        }           
        if (getContainer() == null) {
            // do nothing, we will be called again later. This avoids that the user
            // is shown the differences-dialog twice if openening the editor...
            return;
        }
        if (!(getIpsObject() instanceof IFixDifferencesToModelSupport)) {
            return;
        }
        final IFixDifferencesToModelSupport toFixIpsObject = (IFixDifferencesToModelSupport)getIpsObject();
        try {
            if (!toFixIpsObject.containsDifferenceToModel(getIpsProject())){
                if (TRACE) {
                    logMethodFinished("checkForInconsistenciesToModel - no differences found."); //$NON-NLS-1$
                }
                return;
            }
            Dialog dialog = createDialogToFixDifferencesToModel();
            if (dialog.open() == Dialog.OK) {
                if (TRACE) {
                    log("checkForInconsistenciesToModel - differences found, start fixing differenced."); //$NON-NLS-1$
                }
                IWorkspaceRunnable fix = new IWorkspaceRunnable(){
                    public void run(IProgressMonitor monitor) throws CoreException {
                        toFixIpsObject.fixAllDifferencesToModel(getIpsProject());
                    }
                };
                IpsPlugin.getDefault().getIpsModel().runAndQueueChangeEvents(fix, null);
                refreshInclStructuralChanges();
            } else {
                getSettings().put(getIpsSrcFile(), SETTING_DONT_FIX_DIFFERENCES, true);
            }
            if (TRACE) {
                logMethodFinished("checkForInconsistenciesToModel"); //$NON-NLS-1$
            }
        } catch (CoreException e) {
            IpsPlugin.logAndShowErrorDialog(e);
            return;
        }
    }    
    
    /**
     * Creates a dialog to disblay the differences to the model and ask the user if the
     * inconsistencies should be fixed. Specific logic has to be implemented in subclasses.
     * 
     * @throws CoreException Throws in case of an error
     */
    protected Dialog createDialogToFixDifferencesToModel() throws CoreException{
        throw new UnsupportedOperationException();
    }
    
    /**
     * Refreshes the UI and can handle structural changes which means not only the content of the
     * controls is updated but also new controls are created or existing ones are disposed if
     * neccessary.
     */
    protected void refreshInclStructuralChanges(){
        if (updatingPageStructure) {
            return;
        }
        refresh();
    }
    
    /**
     * Returns the SelectionProviderDispatcher which is the ISelectionProvider for this IEditorPart.
     */
    public SelectionProviderDispatcher getSelectionProviderDispatcher() {
        return selectionProviderDispatcher;
    }
    
    /**
     * {@inheritDoc}
     */
    public final void dispose() {
        super.dispose();
        if (selectionProviderDispatcher!=null) {
            selectionProviderDispatcher.dispose();
        }
        if (activationListener!=null) {
            activationListener.dispose();
        }
        ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
        if (errorTickupdater!=null) {
            errorTickupdater.dispose();
        }
        disposeInternal();
        if (TRACE) {
            log("disposed."); //$NON-NLS-1$
        }
    }

    /**
     * Empty. Can be overridden by subclasses for dispose purposes.
     */
    protected void disposeInternal() {
    }

    /**
     * {@inheritDoc}
     */
    public void propertyChange(PropertyChangeEvent event) {
        if (TRACE) {
            logMethodStarted("propertyChange(): Received property changed event " + event); //$NON-NLS-1$
        }
        if (!isActive()) {
            return;
        }
        if (event.getProperty().equals(IpsPreferences.WORKING_MODE)) {
            refresh();        
        }
        if (TRACE) {
            logMethodFinished("propertyChange()"); //$NON-NLS-1$
        }
    }
    
    /**
     * Returns the settings for ips object editors. This method never returns <code>null</code>.
     */
    protected IIpsObjectEditorSettings getSettings() {
        return IpsPlugin.getDefault().getIpsEditorSettings();
    }
    
    public String toString() {
        return "Editor for " + getIpsSrcFile(); //$NON-NLS-1$
    }
    
    /**
     * Internal part and shell activation listener.
     * 
     * 
     * Copied from AbstractTextEditor.
     */
    class ActivationListener implements IPartListener, IWindowListener {

        private IPartService partService;
        
        /**
         * Creates this activation listener.
         *
         * @param partService the part service on which to add the part listener
         * @since 3.1
         */
        public ActivationListener(IPartService partService) {
            this.partService = partService;
            partService.addPartListener(this);
            PlatformUI.getWorkbench().addWindowListener(this);
        }

        /**
         * Disposes this activation listener.
         *
         * @since 3.1
         */
        public void dispose() {
            partService.removePartListener(this);
            PlatformUI.getWorkbench().removeWindowListener(this);
            partService= null;
        }

        public void partActivated(IWorkbenchPart part) {
            if (part!=IpsObjectEditor.this) {
                return;
            }
            handleEditorActivation();
        }

        public void partBroughtToTop(IWorkbenchPart part) {
        }

        public void partClosed(IWorkbenchPart part) {
            if (part!=IpsObjectEditor.this) {
                return;
            }
            ipsSrcFile.discardChanges();
            removeListeners();
            if (!IpsPlugin.getDefault().getWorkbench().isClosing()) {
                IIpsObjectEditorSettings settings = IpsPlugin.getDefault().getIpsEditorSettings();
                settings.remove(ipsSrcFile);
            }
        }

        public void partDeactivated(IWorkbenchPart part) {
        }
        
        private void removeListeners() {
            IpsPlugin.getDefault().getIpsModel().removeChangeListener(IpsObjectEditor.this);
            IpsPlugin.getDefault().getIpsModel().removeModificationStatusChangeListener(IpsObjectEditor.this);
            IpsPlugin.getDefault().getIpsPreferences().removeChangeListener(IpsObjectEditor.this);
        }
        
        public void partOpened(IWorkbenchPart part) {
        }

        public void windowActivated(IWorkbenchWindow window) {
            if (window == getEditorSite().getWorkbenchWindow()) {
                checkForChangesMadeOutsideEclipse();
            }
        }

        public void windowDeactivated(IWorkbenchWindow window) {
        }

        public void windowClosed(IWorkbenchWindow window) {
        }

        public void windowOpened(IWorkbenchWindow window) {
        }
    }

    private void logMethodStarted(String msg) {
        logInternal("." + msg + " - started"); //$NON-NLS-1$ //$NON-NLS-2$ $NON-NLS-2$
    }
    
    private void logMethodFinished(String msg) {
        logInternal("." + msg + " - finished"); //$NON-NLS-1$ //$NON-NLS-2$ $NON-NLS-2$
    }
    
    private void log(String msg) {
        logInternal(": " + msg); //$NON-NLS-1$
    }

    private void logInternal(String msg) {
        String file = ipsSrcFile==null ? "null" : ipsSrcFile.getName(); // $NON-NLS-1$ //$NON-NLS-1$
        System.out.println(getLogPrefix() + msg + ", IpsSrcFile=" + file + ", Thread=" + Thread.currentThread().getName()); //$NON-NLS-1$ //$NON-NLS-2$ $NON-NLS-2$
    }
    
    private String getLogPrefix() {
        return "IpsObjectEditor"; //$NON-NLS-1$
    }

    /**
     * Updates the title image with the given image.
     */
    public void updatedTitleImage(Image newImage) {
        setTitleImage(newImage);
    }

}
