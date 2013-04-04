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

package org.faktorips.devtools.core.ui.views.modelexplorer;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.faktorips.devtools.core.internal.model.ipsproject.LibraryIpsPackageFragmentRoot;
import org.faktorips.devtools.core.internal.model.ipsproject.bundle.IpsBundleEntry;
import org.faktorips.devtools.core.model.ipsproject.IIpsObjectPathContainer;
import org.faktorips.devtools.core.model.ipsproject.IIpsObjectPathEntry;
import org.faktorips.devtools.core.model.ipsproject.IIpsProject;
import org.faktorips.devtools.core.model.ipsproject.IIpsProjectRefEntry;
import org.junit.Before;
import org.junit.Test;

public class IpsObjectPathContainerChildrenProviderTest {

    private IpsObjectPathContainerChildrenProvider childrenProvider;
    private IIpsProject referencedProject;
    private IIpsProject referencedProject2;
    private IIpsObjectPathContainer container;
    private LibraryIpsPackageFragmentRoot jarBundleFragmentRoot;

    @Before
    public void setUp() {
        childrenProvider = new IpsObjectPathContainerChildrenProvider();

        IIpsProjectRefEntry projectEntry = mock(IIpsProjectRefEntry.class);
        referencedProject = mock(IIpsProject.class);
        when(projectEntry.getReferencedIpsProject()).thenReturn(referencedProject);

        IIpsProjectRefEntry projectEntry2 = mock(IIpsProjectRefEntry.class);
        referencedProject2 = mock(IIpsProject.class);
        when(projectEntry2.getReferencedIpsProject()).thenReturn(referencedProject2);

        IpsBundleEntry jarBundleEntry = mock(IpsBundleEntry.class);
        jarBundleFragmentRoot = mock(LibraryIpsPackageFragmentRoot.class);
        when(jarBundleEntry.getIpsPackageFragmentRoot()).thenReturn(jarBundleFragmentRoot);

        List<IIpsObjectPathEntry> values = Arrays.asList(projectEntry, jarBundleEntry, projectEntry2);

        container = mock(IIpsObjectPathContainer.class);
        when(container.resolveEntries()).thenReturn(values);

    }

    @Test
    public void testGetChildren() throws CoreException {

        Object[] children = childrenProvider.getChildren(container);

        assertEquals(3, children.length);
        assertEquals(referencedProject, ((ReferencedIpsProjectViewItem)children[0]).getIpsProject());
        assertEquals(jarBundleFragmentRoot, children[1]);
        assertEquals(referencedProject2, ((ReferencedIpsProjectViewItem)children[2]).getIpsProject());
    }

}
