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

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.osgi.util.NLS;
import org.faktorips.devtools.core.IpsPlugin;
import org.faktorips.devtools.core.builder.flidentifier.ast.AttributeNode;
import org.faktorips.devtools.core.builder.flidentifier.ast.IdentifierNode;
import org.faktorips.devtools.core.fl.IdentifierKind;
import org.faktorips.devtools.core.internal.fl.IdentifierFilter;
import org.faktorips.devtools.core.model.ipsproject.IIpsProject;
import org.faktorips.devtools.core.model.pctype.IPolicyCmptType;
import org.faktorips.devtools.core.model.productcmpt.IExpression;
import org.faktorips.devtools.core.model.type.IAttribute;
import org.faktorips.fl.ExprCompiler;
import org.faktorips.util.message.Message;

/**
 * This parser tries to match the identifier part to an attribute of the context type. In case of
 * success it will return an {@link AttributeNode} otherwise it returns <code>null</code>.
 * 
 * @author dirmeier
 */
public class AttributeParser extends TypeBasedIdentifierParser {

    public static final char VALUE_SUFFIX_SEPARATOR_CHAR = '@';
    public static final String DEFAULT_VALUE_SUFFIX = VALUE_SUFFIX_SEPARATOR_CHAR + "default"; //$NON-NLS-1$
    private final IdentifierFilter identifierFilter;

    public AttributeParser(IExpression expression, IIpsProject ipsProject, IdentifierFilter identifierFilter) {
        super(expression, ipsProject);
        this.identifierFilter = identifierFilter;
    }

    @Override
    public IdentifierNode parseInternal() {
        boolean defaultValueAccess = isDefaultValueAccess(getIdentifierPart());
        List<IAttribute> attributes;
        try {
            attributes = findAttributes();
        } catch (CoreException e) {
            IpsPlugin.log(e);
            return nodeFactory().createInvalidIdentifier(
                    Message.newInfo(ExprCompiler.UNDEFINED_IDENTIFIER,
                            Messages.AbstractParameterIdentifierResolver_msgErrorRetrievingAttribute));
        }
        for (IAttribute anAttribute : attributes) {
            String attributeName = getAttributeName(getIdentifierPart(), defaultValueAccess);
            if (attributeName.equals(anAttribute.getName())) {
                if (isAllowd(anAttribute, defaultValueAccess)) {
                    return nodeFactory().createAttributeNode(anAttribute, defaultValueAccess, isListOfTypeContext());
                } else {
                    return createInvalidIdentifierNode();
                }
            }
        }
        return null;
    }

    private boolean isDefaultValueAccess(String attributeName) {
        return getContextType() instanceof IPolicyCmptType && attributeName.endsWith(DEFAULT_VALUE_SUFFIX);
    }

    private String getAttributeName(String identifier, boolean defaultValueAccess) {
        if (defaultValueAccess) {
            return identifier.substring(0, identifier.lastIndexOf(VALUE_SUFFIX_SEPARATOR_CHAR));
        } else {
            return identifier;
        }
    }

    private List<IAttribute> findAttributes() throws CoreException {
        List<IAttribute> attributes;
        if (isContextTypeFormulaType()) {
            attributes = getExpression().findMatchingProductCmptTypeAttributes();
        } else {
            attributes = getContextType().findAllAttributes(getIpsProject());
        }
        return attributes;
    }

    private boolean isAllowd(IAttribute anAttribute, boolean isDefaultIdentifier) {
        return identifierFilter.isIdentifierAllowed(anAttribute,
                IdentifierKind.getDefaultIdentifierOrAttribute(isDefaultIdentifier));
    }

    private IdentifierNode createInvalidIdentifierNode() {
        return nodeFactory().createInvalidIdentifier(
                Message.newError(ExprCompiler.UNDEFINED_IDENTIFIER, NLS.bind(
                        Messages.AbstractParameterIdentifierResolver_msgIdentifierNotAllowed, getIdentifierPart())));
    }

}