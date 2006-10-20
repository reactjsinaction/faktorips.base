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

package org.faktorips.devtools.core.builder;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.faktorips.codegen.DatatypeHelper;
import org.faktorips.devtools.core.AbstractIpsPluginTest;
import org.faktorips.devtools.core.internal.model.TableStructureEnumDatatypeAdapter;
import org.faktorips.devtools.core.model.IIpsArtefactBuilder;
import org.faktorips.devtools.core.model.IIpsArtefactBuilderSetConfig;
import org.faktorips.devtools.core.model.IIpsPackageFragmentRoot;
import org.faktorips.devtools.core.model.IIpsProject;
import org.faktorips.devtools.core.model.IParameterIdentifierResolver;
import org.faktorips.devtools.core.model.tablestructure.ITableStructure;

/**
 * 
 * @author Jan Ortmann
 */
public class DefaultBuilderSetTest extends AbstractIpsPluginTest {

	/*
	 * @see IpsPluginTest#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
	}

	/*
	 * Test method for 'org.faktorips.devtools.core.builder.DefaultBuilderSet.getRuntimeRepositoryTocFile(IIpsPackageFragmentRoot)'
	 */
	public void testGetRuntimeRepositoryTocFile() throws CoreException {
		IIpsProject project = newIpsProject("TestProject");
		DefaultBuilderSet builderSet = new TestBuilderSet();
		IIpsPackageFragmentRoot root = project.getIpsPackageFragmentRoots()[0];
		IFile file = builderSet.getRuntimeRepositoryTocFile(root);
		assertNotNull(file);
	}

	class TestBuilderSet extends DefaultBuilderSet {

		/**
		 * {@inheritDoc}
		 */
		public IIpsArtefactBuilder[] getArtefactBuilders() {
			return null;
		}

		/**
		 * {@inheritDoc}
		 */
		public boolean isSupportFlIdentifierResolver() {
			return false;
		}

		/**
		 * {@inheritDoc}
		 */
		public IParameterIdentifierResolver getFlParameterIdentifierResolver() {
			return null;
		}

        public void clean() throws CoreException {
            
        }
	
        /**
         * {@inheritDoc}
         */
        public String getClassNameForTableBasedEnum(ITableStructure structure) {
            return null;
        }

        public DatatypeHelper getDatatypeHelperForTableBasedEnum(TableStructureEnumDatatypeAdapter datatype) {
            return null;
        }

        public void initialize(IIpsArtefactBuilderSetConfig config) throws CoreException {
        }
        
	}
}
