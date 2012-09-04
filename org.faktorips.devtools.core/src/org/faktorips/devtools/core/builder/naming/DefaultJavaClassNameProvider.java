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

package org.faktorips.devtools.core.builder.naming;

import org.faktorips.devtools.core.model.ipsobject.IIpsSrcFile;
import org.faktorips.devtools.core.model.ipsproject.IJavaNamingConvention;

/**
 * The default java name provider. Simply use the the configured {@link IJavaNamingConvention} and
 * the name of the {@link IIpsSrcFile} to get the implementation or interface name.
 * 
 * @author dirmeier
 */
public class DefaultJavaClassNameProvider implements IJavaClassNameProvider {

    private final boolean isGeneratePublishedInterface;

    public DefaultJavaClassNameProvider(boolean isGeneratePublishedInterface) {
        this.isGeneratePublishedInterface = isGeneratePublishedInterface;
    }

    @Override
    public String getImplClassName(IIpsSrcFile ipsSrcFile) {
        return ipsSrcFile.getIpsProject().getJavaNamingConvention()
                .getImplementationClassName(ipsSrcFile.getIpsObjectName());
    }

    @Override
    public boolean isImplClassPublishedArtifact() {
        return !isGeneratePublishedInterface;
    }

    @Override
    public final String getInterfaceName(IIpsSrcFile ipsSrcFile) {
        if (!isGeneratePublishedInterface) {
            return getImplClassName(ipsSrcFile);
        } else {
            return getInterfaceNameInternal(ipsSrcFile);
        }
    }

    /**
     * Returns the name of the generated interface and does not check if interfaces are generated at
     * all.
     * 
     * @param ipsSrcFile The {@link IIpsSrcFile} you want to get the generated interface name for
     * 
     * @return The name of the generated interface
     */
    protected String getInterfaceNameInternal(IIpsSrcFile ipsSrcFile) {
        return ipsSrcFile.getIpsProject().getJavaNamingConvention()
                .getPublishedInterfaceName(ipsSrcFile.getIpsObjectName());
    }

    @Override
    public boolean isInterfacePublishedArtifact() {
        return true;
    }

}
