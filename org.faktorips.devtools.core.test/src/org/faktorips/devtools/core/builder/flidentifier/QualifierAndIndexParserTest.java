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

package org.faktorips.devtools.core.builder.flidentifier;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.faktorips.datatype.ListOfTypeDatatype;
import org.faktorips.devtools.core.builder.flidentifier.ast.AssociationNode;
import org.faktorips.devtools.core.builder.flidentifier.ast.IdentifierNode;
import org.faktorips.devtools.core.builder.flidentifier.ast.IdentifierNodeFactory;
import org.faktorips.devtools.core.builder.flidentifier.ast.IndexNode;
import org.faktorips.devtools.core.builder.flidentifier.ast.InvalidIdentifierNode;
import org.faktorips.devtools.core.builder.flidentifier.ast.QualifierNode;
import org.faktorips.devtools.core.model.ipsobject.IIpsSrcFile;
import org.faktorips.devtools.core.model.pctype.IPolicyCmptType;
import org.faktorips.devtools.core.model.pctype.IPolicyCmptTypeAssociation;
import org.faktorips.devtools.core.model.productcmpt.IProductCmpt;
import org.faktorips.fl.ExprCompiler;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class QualifierAndIndexParserTest extends AbstractParserTest {

    private static final String RUNTIME_ID = "RuntimeID";

    private static final int MY_INDEX = 12;

    private static final String MY_QUALIFIER = "myQualifier";

    private static final String INDEX = MY_INDEX + "]";

    private static final String QUALIFIER = "\"" + MY_QUALIFIER + "\"]";

    @Mock
    private IPolicyCmptTypeAssociation association;

    @Mock
    private IPolicyCmptType targetSubType;

    @Mock
    private IPolicyCmptType targetType;

    @Mock
    private IProductCmpt productCmpt;

    private QualifierAndIndexParser qualifierAndIndexParser;

    @Before
    public void initParser() {
        qualifierAndIndexParser = new QualifierAndIndexParser(getExpression(), getIpsProject());
    }

    @Test
    public void testParse_noIndexFor1To1Association() throws Exception {
        when(association.is1ToMany()).thenReturn(false);
        initSourceFile();

        InvalidIdentifierNode node = (InvalidIdentifierNode)qualifierAndIndexParser.parse(INDEX,
                createAssociationNode(association, false));

        assertEquals(ExprCompiler.NO_INDEX_FOR_1TO1_ASSOCIATION, node.getMessage().getCode());
    }

    @Test
    public void testParse_findAssociationQualified1To1IgnoringQualifier() throws Exception {
        when(association.is1ToMany()).thenReturn(true);
        when(association.is1ToManyIgnoringQualifier()).thenReturn(false);
        initSourceFile();

        QualifierNode node = (QualifierNode)qualifierAndIndexParser.parse(QUALIFIER,
                createAssociationNode(association, false));

        assertEquals(RUNTIME_ID, node.getRuntimeId());
        assertEquals(targetSubType, node.getDatatype());
    }

    @Test
    public void testParse_findAssociationQualified1ToMany() throws Exception {
        when(association.is1ToMany()).thenReturn(true);
        when(association.is1ToManyIgnoringQualifier()).thenReturn(true);
        initSourceFile();

        QualifierNode node = (QualifierNode)qualifierAndIndexParser.parse(QUALIFIER,
                createAssociationNode(association, false));

        assertEquals(RUNTIME_ID, node.getRuntimeId());
        assertEquals(new ListOfTypeDatatype(targetSubType), node.getDatatype());
    }

    @Test
    public void testParse_findAssociationQualified1To1FromMany() throws Exception {
        when(association.is1ToManyIgnoringQualifier()).thenReturn(false);
        initSourceFile();

        QualifierNode node = (QualifierNode)qualifierAndIndexParser.parse(QUALIFIER,
                createAssociationNode(association, true));

        assertEquals(RUNTIME_ID, node.getRuntimeId());
        assertEquals(new ListOfTypeDatatype(targetSubType), node.getDatatype());
    }

    @Test
    public void testParse_findAssociationQualified_NoRuntimeID() throws Exception {
        when(association.is1ToMany()).thenReturn(true);
        when(association.is1ToManyIgnoringQualifier()).thenReturn(false);
        initSourceFileNoRuntimeID();

        InvalidIdentifierNode node = (InvalidIdentifierNode)qualifierAndIndexParser.parse(QUALIFIER,
                createAssociationNode(association, false));
        assertEquals(ExprCompiler.UNKNOWN_QUALIFIER, node.getMessage().getCode());
    }

    @Test
    public void testParse_findAssociationIndex() throws Exception {
        when(association.is1ToMany()).thenReturn(true);
        initSourceFileNoRuntimeID();

        IndexNode node = (IndexNode)qualifierAndIndexParser.parse(INDEX, createAssociationNode(association, false));

        assertEquals(MY_INDEX, node.getIndex());
        assertEquals(targetType, node.getDatatype());
    }

    @Test
    public void testParse_IndexAtTo1AssociationButListContext() throws Exception {
        when(association.is1ToMany()).thenReturn(false);
        initSourceFileNoRuntimeID();

        IndexNode node = (IndexNode)qualifierAndIndexParser.parse(INDEX, createAssociationNode(association, true));

        assertEquals(MY_INDEX, node.getIndex());
        assertEquals(targetType, node.getDatatype());
    }

    @Test
    public void testParse_invalidAssociationTo1Index() throws Exception {
        when(association.is1ToMany()).thenReturn(false);
        initSourceFileNoRuntimeID();

        InvalidIdentifierNode node = (InvalidIdentifierNode)qualifierAndIndexParser.parse(INDEX,
                createAssociationNode(association, false));

        assertEquals(ExprCompiler.NO_INDEX_FOR_1TO1_ASSOCIATION, node.getMessage().getCode());
    }

    @Test
    public void testParse_associationInvalidIndex() throws Exception {
        when(association.is1ToMany()).thenReturn(true);
        initSourceFileNoRuntimeID();

        InvalidIdentifierNode node = (InvalidIdentifierNode)qualifierAndIndexParser.parse("[asd]",
                createAssociationNode(association, true));

        assertEquals(ExprCompiler.UNKNOWN_QUALIFIER, node.getMessage().getCode());
    }

    @Test
    public void testParse_findAssociation1ToManyIndexedFromList() throws Exception {
        when(association.is1ToMany()).thenReturn(true);
        when(association.is1ToManyIgnoringQualifier()).thenReturn(true);
        initSourceFileNoRuntimeID();

        IndexNode node = (IndexNode)qualifierAndIndexParser.parse(INDEX, createAssociationNode(association, true));

        assertEquals(MY_INDEX, node.getIndex());
        assertEquals(targetType, node.getDatatype());
    }

    @Test
    public void testParse_findIndexAfterQualifierOnList() throws Exception {
        initSourceFile(RUNTIME_ID);

        IndexNode node = (IndexNode)qualifierAndIndexParser.parse(INDEX, createQualifierNode(true));

        assertEquals(MY_INDEX, node.getIndex());
        assertEquals(targetSubType, node.getDatatype());
    }

    @Test
    public void testParse_noIndexAfterQualifierOnOneElement() throws Exception {
        initSourceFile(RUNTIME_ID);

        InvalidIdentifierNode node = (InvalidIdentifierNode)qualifierAndIndexParser.parse(INDEX,
                createQualifierNode(false));

        assertEquals(ExprCompiler.NO_INDEX_FOR_1TO1_ASSOCIATION, node.getMessage().getCode());
    }

    private IPolicyCmptType initSourceFile() throws Exception {
        return initSourceFile(RUNTIME_ID);
    }

    private IPolicyCmptType initSourceFileNoRuntimeID() throws Exception {
        return initSourceFile(null);
    }

    private IPolicyCmptType initSourceFile(String runtimeID) throws Exception {
        when(association.findTarget(getIpsProject())).thenReturn(targetType);
        when(targetType.findProductCmptType(getIpsProject())).thenReturn(getProductCmptType());
        IIpsSrcFile sourceFile = mock(IIpsSrcFile.class);
        IIpsSrcFile[] ipsSourceFiles = new IIpsSrcFile[] { sourceFile };
        when(getIpsProject().findAllProductCmptSrcFiles(getProductCmptType(), true)).thenReturn(ipsSourceFiles);
        when(sourceFile.getIpsObjectName()).thenReturn(MY_QUALIFIER);
        when(sourceFile.getIpsObject()).thenReturn(productCmpt);
        when(productCmpt.getRuntimeId()).thenReturn(runtimeID);
        when(productCmpt.findPolicyCmptType(getIpsProject())).thenReturn(targetSubType);
        return targetType;
    }

    private AssociationNode createAssociationNode(IPolicyCmptTypeAssociation association, boolean listContext) {
        IdentifierNodeFactory nodeFactory = new IdentifierNodeFactory("", getIpsProject());
        return (AssociationNode)nodeFactory.createAssociationNode(association, listContext);
    }

    private IdentifierNode createQualifierNode(boolean listOfTypes) {
        IdentifierNodeFactory nodeFactory = new IdentifierNodeFactory("", getIpsProject());
        return nodeFactory.createQualifierNode(productCmpt, QUALIFIER, listOfTypes);
    }

}