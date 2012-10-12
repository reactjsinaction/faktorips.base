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

package org.faktorips.devtools.core.ui.controls.contentproposal;

import org.faktorips.devtools.core.model.ipsobject.IIpsSrcFile;

/**
 * This subclass of the {@link AbstractIpsSrcFileContentProposalProvider} uses an
 * {@link IpsSrcFileProvider} to provide IIpsSrcFiles for the proposal.
 * 
 * @author dicker
 */
public class ProvidedIpsSrcFileContentProposalProvider extends AbstractIpsSrcFileContentProposalProvider {

    private final IpsSrcFileProvider ipsSrcFileProvider;

    /**
     * Constructor with the {@link IpsSrcFileProvider}
     */
    public ProvidedIpsSrcFileContentProposalProvider(IpsSrcFileProvider ipsSrcFileProvider) {
        this.ipsSrcFileProvider = ipsSrcFileProvider;
    }

    @Override
    protected IIpsSrcFile[] getIpsSrcFiles() {
        return ipsSrcFileProvider.getProvidedIpsSrcFiles();
    }

}
