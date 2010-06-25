package org.faktorips.devtools.htmlexport.helper.path;

import org.faktorips.devtools.core.model.ipsproject.IIpsPackageFragment;
import org.faktorips.devtools.core.ui.IpsUIPlugin;
import org.faktorips.devtools.htmlexport.helper.DocumentorUtil;

/**
 * {@link IpsElementPathUtil} for an {@link IIpsPackageFragment}
 * 
 * @author dicker
 * 
 */
public class IpsPackageFragmentPathUtil extends AbstractIpsElementPathUtil<IIpsPackageFragment> {
    private static String PACKAGE_INDEX_FILE_NAME = "package_index"; //$NON-NLS-1$

    public IpsPackageFragmentPathUtil(IIpsPackageFragment ipsElement) {
        super(ipsElement);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.faktorips.devtools.htmlexport.helper.path.AbstractIpsElementPathUtil#getPathFromRoot(
     * org.faktorips.devtools.htmlexport.helper.path.LinkedFileType)
     */
    @Override
    public String getPathFromRoot(LinkedFileType linkedFileType) {
        if (getIpsElement().isDefaultPackage()) {
            return PACKAGE_INDEX_FILE_NAME;
        }
        return super.getPathFromRoot(linkedFileType);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.faktorips.devtools.htmlexport.helper.path.IpsElementPathUtil#getPathToRoot()
     */
    public String getPathToRoot() {
        if (getIpsElement().isDefaultPackage()) {
            return ""; //$NON-NLS-1$
        }
        return getPackageFragmentPathToRoot(getIpsElement());
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.faktorips.devtools.htmlexport.helper.path.AbstractIpsElementPathUtil#getLinkText(boolean)
     */
    @Override
    public String getLinkText(boolean withImage) {
        return IpsUIPlugin.getLabel(getIpsElement());
        /*
         * if (ipsPackageFragment.isDefaultPackage()) { return DEFAULT_PACKAGE_NAME; } return
         * ipsPackageFragment.getName();
         */
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.faktorips.devtools.htmlexport.helper.path.AbstractIpsElementPathUtil#getIpsPackageFragment
     * ()
     */
    @Override
    protected IIpsPackageFragment getIpsPackageFragment() {
        return getIpsElement();
    }
}
