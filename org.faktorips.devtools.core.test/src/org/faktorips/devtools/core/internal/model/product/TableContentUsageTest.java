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

package org.faktorips.devtools.core.internal.model.product;

import java.util.GregorianCalendar;

import org.faktorips.devtools.core.AbstractIpsPluginTest;
import org.faktorips.devtools.core.model.IIpsProject;
import org.faktorips.devtools.core.model.IpsObjectType;
import org.faktorips.devtools.core.model.pctype.IPolicyCmptType;
import org.faktorips.devtools.core.model.product.IProductCmpt;
import org.faktorips.devtools.core.model.product.IProductCmptGeneration;
import org.faktorips.devtools.core.model.product.ITableContentUsage;
import org.faktorips.devtools.core.model.productcmpttype.ITableStructureUsage;
import org.faktorips.devtools.core.model.tablecontents.ITableContents;
import org.faktorips.devtools.core.model.tablestructure.ITableStructure;
import org.faktorips.util.message.MessageList;

/**
 * Tests for product component structure.
 * 
 * @author Thorsten Guenther
 */
public class TableContentUsageTest extends AbstractIpsPluginTest {

    private IIpsProject project;
    private ITableStructureUsage structUsage;
    private ITableContentUsage contentUsage;
    private ITableContents content;
    private ITableStructure structure;
    private IPolicyCmptType type;
    private IProductCmpt cmpt;
    
    final private String STRUCTURE_ROLENAME = "StructUsageRole";
	/**
	 * {@inheritDoc}
	 */
    protected void setUp() throws Exception {
        super.setUp();
        project = newIpsProject("TestProject");
        type = newPolicyCmptType(project, "Type");
        cmpt = newProductCmpt(project, "Cmpt");
        cmpt.setPolicyCmptType(type.getQualifiedName());
        structure = (ITableStructure)newIpsObject(project, IpsObjectType.TABLE_STRUCTURE, "Structure");
        content = (ITableContents)newIpsObject(project, IpsObjectType.TABLE_CONTENTS, "Contents");
        structUsage = cmpt.findProductCmptType().newTableStructureUsage();
        structUsage.addTableStructure(structure.getQualifiedName());
        structUsage.setRoleName(STRUCTURE_ROLENAME);
        
        cmpt.newGeneration(new GregorianCalendar());
        contentUsage = ((IProductCmptGeneration)cmpt.getFirstGeneration()).newTableContentUsage();
    }
    
    public void testValidateUnknownStructure() throws Exception {
        MessageList ml = contentUsage.validate();
        assertNotNull(ml.getMessageByCode(ITableContentUsage.MSGCODE_UNKNOWN_STRUCTURE_USAGE));
        
        contentUsage.setStructureUsage("StructUsageRole");
        ml = contentUsage.validate();
        assertNull(ml.getMessageByCode(ITableContentUsage.MSGCODE_UNKNOWN_STRUCTURE_USAGE));
    }
    
    public void testValidateUnknownContent() throws Exception {
        structUsage.setMandatoryTableContent(true);
        contentUsage.setStructureUsage("StructUsageRole");
        contentUsage.setTableContentName("unknown");
        MessageList ml = contentUsage.validate();
        assertNotNull(ml.getMessageByCode(ITableContentUsage.MSGCODE_UNKNOWN_TABLE_CONTENT));
        
        contentUsage.setTableContentName(content.getQualifiedName());
        ml = contentUsage.validate();
        assertNull(ml.getMessageByCode(ITableContentUsage.MSGCODE_UNKNOWN_TABLE_CONTENT));
        
        contentUsage.setTableContentName("");
        ml = contentUsage.validate();
        assertNotNull(ml.getMessageByCode(ITableContentUsage.MSGCODE_UNKNOWN_TABLE_CONTENT));   
        
        contentUsage.setTableContentName(null);
        ml = contentUsage.validate();
        assertNotNull(ml.getMessageByCode(ITableContentUsage.MSGCODE_UNKNOWN_TABLE_CONTENT));
        
        structUsage.setMandatoryTableContent(false);
        contentUsage.setTableContentName("");
        ml = contentUsage.validate();
        assertNull(ml.getMessageByCode(ITableContentUsage.MSGCODE_UNKNOWN_TABLE_CONTENT));   
        
        contentUsage.setTableContentName(null);
        ml = contentUsage.validate();
        assertNull(ml.getMessageByCode(ITableContentUsage.MSGCODE_UNKNOWN_TABLE_CONTENT));        
    }
    
    public void testValidateInvalidContent() throws Exception {
        content.setTableStructure("unknown");
        contentUsage.setStructureUsage("StructUsageRole");
        contentUsage.setTableContentName(content.getQualifiedName());
        MessageList ml = contentUsage.validate();
        assertNotNull(ml.getMessageByCode(ITableContentUsage.MSGCODE_INVALID_TABLE_CONTENT));
        
        content.setTableStructure(structUsage.getTableStructures()[0]);
        ml = contentUsage.validate();
        assertNull(ml.getMessageByCode(ITableContentUsage.MSGCODE_INVALID_TABLE_CONTENT));
    }
    
    public void testFindTableContents() throws Exception{
        contentUsage.setTableContentName("none");
        assertNull(contentUsage.findTableContents());
        
        contentUsage.setTableContentName(content.getQualifiedName());
        assertSame(content, contentUsage.findTableContents());
    }
    
    public void testFindTableStructureUsage() throws Exception {
        contentUsage.setStructureUsage("none");
        assertNull(contentUsage.findTableStructureUsage());
        
        
        contentUsage.setStructureUsage(STRUCTURE_ROLENAME);
        ITableStructureUsage structureUsage = contentUsage.findTableStructureUsage();
        assertNotNull(structureUsage);
        // TODO
        // assertEquals(structureUsage, contentUsage.findTableStructureUsage());
   	
    }
}
