/***************************************************************************************************
 *  * Copyright (c) 2005,2006 Faktor Zehn GmbH und andere.  *  * Alle Rechte vorbehalten.  *  *
 * Dieses Programm und alle mitgelieferten Sachen (Dokumentationen, Beispiele,  * Konfigurationen,
 * etc.) duerfen nur unter den Bedingungen der  * Faktor-Zehn-Community Lizenzvereinbarung - Version
 * 0.1 (vor Gruendung Community)  * genutzt werden, die Bestandteil der Auslieferung ist und auch
 * unter  *   http://www.faktorips.org/legal/cl-v01.html  * eingesehen werden kann.  *  *
 * Mitwirkende:  *   Faktor Zehn GmbH - initial API and implementation - http://www.faktorzehn.de  *  
 **************************************************************************************************/

package org.faktorips.devtools.core.ui.actions;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.part.ResourceTransfer;
import org.faktorips.devtools.core.IpsPlugin;
import org.faktorips.devtools.core.internal.model.IpsObjectPartState;
import org.faktorips.devtools.core.model.IIpsElement;
import org.faktorips.devtools.core.model.IIpsObject;
import org.faktorips.devtools.core.model.IIpsObjectPartContainer;
import org.faktorips.devtools.core.model.IIpsPackageFragment;
import org.faktorips.devtools.core.model.IIpsPackageFragmentRoot;
import org.faktorips.devtools.core.model.IIpsProject;
import org.faktorips.devtools.core.model.IIpsSrcFile;
import org.faktorips.devtools.core.model.IpsObjectType;
import org.faktorips.devtools.core.model.product.IProductCmpt;

/**
 * Action to paste IpsElements or resources.
 * 
 * @author Thorsten Guenther
 */
public class IpsPasteAction extends IpsAction {

    /**
     * The clipboard used to transfer the data
     */
    private Clipboard clipboard;

    /**
     * The shell for this session
     */
    private Shell shell;

    /**
     * Creates a new action to paste <code>IIpsElement</code>s or resources.
     * 
     * @param selectionProvider The provider for the selection to get the target from.
     * @param shell The shell for this session.
     */
    public IpsPasteAction(ISelectionProvider selectionProvider, Shell shell) {
        super(selectionProvider);
        clipboard = new Clipboard(shell.getDisplay());
        this.shell = shell;
    }

    /**
     * {@inheritDoc}
     */
    public void run(IStructuredSelection selection) {
        Object selected = selection.getFirstElement();
        if (selected instanceof IIpsObjectPartContainer) {
            paste((IIpsObjectPartContainer)selected);
        } else if (selected instanceof IIpsProject) {
            paste(((IIpsProject)selected).getProject());
        } else if (selected instanceof IIpsPackageFragmentRoot) {
            paste(((IIpsPackageFragmentRoot)selected).getDefaultIpsPackageFragment());
        } else if (selected instanceof IIpsPackageFragment) {
            paste((IIpsPackageFragment)selected);
        } else if (selected instanceof IContainer) {
            paste((IContainer)selected);
        }
    }

    /**
     * Try to paste an <code>IIpsObject</code> to an <code>IIpsObjectPartContainer</code>. If
     * it is not possible because the stored data does not support this (e.g. is a resource and not
     * a string) paste(IIpsPackageFragement) is called.
     * 
     * @param parent The parent to paste to.
     */
    private void paste(IIpsObjectPartContainer parent) {
        String stored = (String)clipboard.getContents(TextTransfer.getInstance());
        
        // obtain the package fragment of the given part container
        IIpsPackageFragment parentPackageFrgmt = null;
        IIpsElement pack = parent.getParent();
        while (pack != null && !(pack instanceof IIpsPackageFragment)) {
            pack = pack.getParent();
        }
        if (pack != null){
            parentPackageFrgmt = (IIpsPackageFragment)pack;
        }
        
        if (stored == null && parentPackageFrgmt != null) {
            // the clipboard contains no string, try to paste resources
            paste(parentPackageFrgmt);
        } else {  
            // try to paste resource links
            if (parentPackageFrgmt != null && pasteResourceLinks(parentPackageFrgmt, stored)){
                // the copied text contains links, paste is finished
                return;
            }
            // no links in string try to paste ips object parts
            try {
                IpsObjectPartState state = new IpsObjectPartState(stored);
                state.newPart(parent);
            } catch (RuntimeException e) {
                IpsPlugin.log(e);
            }
        }
    }

    /**
     * Try to paste an <code>IFolder</code> or <code>IFile</code> stored in the clipboard into
     * the given <code>IContainer</code>.
     */
    private void paste(IContainer parent) {
        Object stored = clipboard.getContents(ResourceTransfer.getInstance());
        if (stored instanceof IResource[]) {
            IResource[] res = (IResource[])stored;
            for (int i = 0; i < res.length; i++) {
                try {
                    IPath targetPath = parent.getFullPath();
                    copy(targetPath, res[i]);
                } catch (CoreException e) {
                    IpsPlugin.logAndShowErrorDialog(e);
                }
            }
        }
    }

    /**
     * Try to paste the <code>IResource</code> stored on the clipboard to the given parent.
     * 
     * @param parent
     */
    private void paste(IIpsPackageFragment parent) {
        Object stored = clipboard.getContents(ResourceTransfer.getInstance());
        if (stored instanceof IResource[]) {
            IResource[] res = (IResource[])stored;
            for (int i = 0; i < res.length; i++) {
                try {
                    IResource resource = ((IIpsElement)parent).getCorrespondingResource();
                    if (resource != null) {
                        IPath targetPath = resource.getFullPath();
                        copy(targetPath, res[i]);
                    } else {
                        showPasteNotSupportedError();
                    }
                } catch (CoreException e) {
                    IpsPlugin.logAndShowErrorDialog(e);
                }
            }
        }
        // Paste objects by resource links (e.g. files inside an ips archive)
        String storedText = (String)clipboard.getContents(TextTransfer.getInstance());
        pasteResourceLinks(parent, storedText);
    }

    /*
     * Try to paste resource links, if the given text contains no such links do nothing.
     * Rerurns true if the text contains resource links otherwise return false.
     */
    private boolean pasteResourceLinks(IIpsPackageFragment parent, String storedText) {
        boolean result = false;
        Object[] resourceLinks = getObjectsFromResourceLinks(storedText);
        try {
            if (resourceLinks.length > 0){
                result = true;
            }
            for (int i = 0; i < resourceLinks.length; i++) {
                if (resourceLinks[i] instanceof IIpsObject) {
                    createFile(parent, (IIpsObject)resourceLinks[i]);
                } else if (resourceLinks[i] instanceof IIpsPackageFragment) {
                    IIpsPackageFragment packageFragment = (IIpsPackageFragment)resourceLinks[i];
                    createPackageFragment(parent, packageFragment);
                } else {
                    showPasteNotSupportedError();
                }
            }
        } catch (Exception e) {
            IpsPlugin.log(e);
        }
        return result;
    }

    private void createPackageFragment(IIpsPackageFragment parent, IIpsPackageFragment packageFragment) throws CoreException {
        String packageName = packageFragment.getLastSegmentName();
        IIpsPackageFragment destination = parent.createSubPackage(packageName, true, null);
        IIpsElement[] children = packageFragment.getChildren();
        for (int i = 0; i < children.length; i++) {
            if (children[i] instanceof IIpsSrcFile){
                IIpsObject ipsObject = ((IIpsSrcFile)children[i]).getIpsObject();
                createFile(destination, ipsObject);
            }
        }
        IIpsPackageFragment[] childPackages = packageFragment.getChildIpsPackageFragments();
        for (int i = 0; i < childPackages.length; i++) {
            createPackageFragment(destination, childPackages[i]);
        }
    }

    private void createFile(IIpsPackageFragment parent, IIpsObject ipsObject) throws CoreException {
        String content = ipsObject.toXml(IpsPlugin.getDefault().newDocumentBuilder().newDocument())
                .toString();
        IIpsSrcFile srcFile = ipsObject.getIpsSrcFile();
        parent.createIpsFile(srcFile.getName(), content, true, null);
    }

    private void showPasteNotSupportedError() {
        MessageDialog.openError(Display.getCurrent().getActiveShell(), Messages.IpsPasteAction_errorTitle,
                Messages.IpsPasteAction_Error_CannotPasteIntoSelectedElement);
    }

    /**
     * Copy the given resource to the given target path.
     * 
     * @throws CoreException If copy failed.
     */
    private void copy(IPath targetPath, IResource resource) throws CoreException {
        if (targetPath == null) {
            return;
        }

        String name = resource.getName();
        String extension = ""; //$NON-NLS-1$
        String suggestedName = name;

        if (resource.getType() == IResource.FOLDER) {
            if (((IFolder)resource).getFullPath().equals(targetPath)) {
                MessageDialog.openError(shell, Messages.IpsPasteAction_errorTitle,
                        Messages.IpsPasteAction_msgSrcAndTargetSame);
                return;
            }
        } else {
            int index = name.lastIndexOf("."); //$NON-NLS-1$
            if (index == -1) {
                suggestedName = name;
            } else {
                suggestedName = name.substring(0, index);
                extension = name.substring(index);
            }
        }

        String nameWithoutExtension = suggestedName;
        Validator validator = new Validator(targetPath, resource, extension);

        int doCopy = InputDialog.OK;
        boolean nameChangeRequired = validator.isValid(suggestedName) != null;

        if (nameChangeRequired) {
            for (int count = 0; validator.isValid(suggestedName) != null; count++) {
                if (count == 0) {
                    suggestedName = Messages.IpsPasteAction_suggestedNamePrefixSimple + nameWithoutExtension;
                } else {
                    suggestedName = NLS.bind(Messages.IpsPasteAction_suggestedNamePrefixComplex, new Integer(count),
                            nameWithoutExtension);
                }
            }

            InputDialog dialog = new InputDialog(shell, Messages.IpsPasteAction_titleNamingConflict, NLS.bind(
                    Messages.IpsPasteAction_msgNamingConflict, nameWithoutExtension), suggestedName, validator);
            dialog.setBlockOnOpen(true);
            doCopy = dialog.open();
            nameWithoutExtension = dialog.getValue();
        }
        if (doCopy == InputDialog.OK) {
            IPath finalTargetPath = targetPath.append(nameWithoutExtension + extension);
            resource.copy(finalTargetPath, true, null);

            if (nameChangeRequired) {
                // The name of the resource was changed - if the copied object is a product
                // component, the runtime-id has to be updated.
                IFile newFile = ResourcesPlugin.getWorkspace().getRoot().getFile(finalTargetPath);
                IIpsElement target = IpsPlugin.getDefault().getIpsModel().getIpsElement(newFile);
                if (target instanceof IIpsSrcFile
                        && ((IIpsSrcFile)target).getIpsObjectType() == IpsObjectType.PRODUCT_CMPT) {
                    IProductCmpt cmpt = (IProductCmpt)((IIpsSrcFile)target).getIpsObject();
                    cmpt.setRuntimeId(nameWithoutExtension);
                    ((IIpsSrcFile)target).save(true, null);
                }
            }
        }

    }

    /**
     * Validator for new resource name.
     * 
     * @author Thorsten Guenther
     */
    private class Validator implements IInputValidator {
        IPath root;
        IResource resource;
        String extension;

        public Validator(IPath root, IResource resource, String extension) {
            this.root = root;
            this.resource = resource;
            this.extension = extension;
        }

        /**
         * {@inheritDoc}
         */
        public String isValid(String newText) {
            IWorkspaceRoot wsRoot = ResourcesPlugin.getWorkspace().getRoot();
            IResource test = null;
            if (resource.getType() == IResource.FILE) {
                test = wsRoot.getFile(root.append(newText + extension));
            } else if (resource.getType() == IResource.FOLDER) {
                test = wsRoot.getFolder(root.append(newText));
            }
            if (test != null && test.exists()) {
                return newText + extension + Messages.IpsPasteAction_msgFileAllreadyExists;
            }

            return null;
        }

    }
}
