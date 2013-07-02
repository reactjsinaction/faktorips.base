/*******************************************************************************
 * Copyright (c) 2005-2012 Faktor Zehn AG und andere.
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

package org.faktorips.devtools.core.internal.model.ipsproject;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.faktorips.devtools.core.IpsPlugin;
import org.faktorips.devtools.core.model.ipsobject.IIpsSrcFile;
import org.faktorips.devtools.core.model.ipsobject.IpsObjectType;
import org.faktorips.devtools.core.model.ipsobject.QualifiedNameType;
import org.faktorips.devtools.core.model.ipsproject.IIpsLibraryEntry;
import org.faktorips.devtools.core.model.ipsproject.IIpsObjectPathEntry;
import org.faktorips.devtools.core.model.ipsproject.IIpsStorage;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public abstract class IpsLibraryEntry extends IpsObjectPathEntry implements IIpsLibraryEntry {

    private LibraryIpsPackageFragmentRoot packageFragmentRoot;

    public IpsLibraryEntry(IpsObjectPath ipsObjectPath) {
        super(ipsObjectPath);
    }

    @Override
    public void findIpsSrcFilesStartingWithInternal(IpsObjectType type,
            String prefixParam,
            boolean ignoreCase,
            List<IIpsSrcFile> result,
            Set<IIpsObjectPathEntry> visitedEntries) throws CoreException {
        String prefix = prefixParam;
        if (ignoreCase) {
            prefix = prefixParam.toLowerCase();
        }

        for (QualifiedNameType qnt : getIpsStorage().getQNameTypes()) {
            String name = qnt.getUnqualifiedName();
            if (ignoreCase) {
                name = name.toLowerCase();
            }
            if (name.startsWith(prefix)) {
                IIpsSrcFile file = getIpsSrcFile(qnt);
                if (file.exists()) {
                    result.add(file);
                }
            }
        }
    }

    protected abstract IIpsSrcFile getIpsSrcFile(QualifiedNameType qnt) throws CoreException;

    protected abstract IIpsStorage getIpsStorage();

    @Override
    protected IIpsSrcFile findIpsSrcFileInternal(QualifiedNameType nameType, Set<IIpsObjectPathEntry> visitedEntries)
            throws CoreException {
        if (getIpsStorage().contains(nameType)) {
            return getIpsPackageFragmentRoot().getIpsPackageFragment(nameType.getPackageName()).getIpsSrcFile(
                    nameType.getFileName());
        } else {
            return null;
        }
    }

    @Override
    public Element toXml(Document doc) {
        Element element = doc.createElement(XML_ELEMENT);
        element.setAttribute("type", getType()); //$NON-NLS-1$
        element.setAttribute(getXmlAttributePathName(), getXmlPathRepresentation());
        return element;
    }

    @Override
    public void initFromXml(Element element, IProject project) {
        String path = element.getAttribute(getXmlAttributePathName());
        try {
            if (StringUtils.isEmpty(path)) {
                initStorage(null);
            } else {
                initStorage(new Path(path));
            }
        } catch (IOException e) {
            IpsPlugin.log(e);
        }
    }

    protected abstract String getXmlPathRepresentation();

    protected abstract String getXmlAttributePathName();

    @Override
    public abstract void initStorage(IPath path) throws IOException;

    @Override
    public LibraryIpsPackageFragmentRoot getIpsPackageFragmentRoot() {
        return packageFragmentRoot;
    }

    protected void setIpsPackageFragmentRoot(LibraryIpsPackageFragmentRoot packageFragmentRoot) {
        this.packageFragmentRoot = packageFragmentRoot;
    }
}