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

import org.faktorips.devtools.core.builder.flidentifier.ast.IdentifierNode;
import org.faktorips.devtools.core.builder.flidentifier.ast.ParameterNode;
import org.faktorips.devtools.core.model.ipsproject.IIpsProject;
import org.faktorips.devtools.core.model.method.IParameter;
import org.faktorips.devtools.core.model.productcmpt.IExpression;

/**
 * The {@link ParameterParser} parses an identifier part to an {@link ParameterNode}. It checks
 * whether the identifier part matches any parameter of the expression signature. In this case it
 * will return an {@link ParameterNode} otherwise <code>null</code>.
 * 
 * @author dirmeier
 */
public class ParameterParser extends AbstractIdentifierNodeParser {

    public ParameterParser(IExpression expression, IIpsProject ipsProject) {
        super(expression, ipsProject);
    }

    @Override
    protected IdentifierNode parse() {
        if (isContextTypeFormulaType()) {
            IParameter[] params = getParameters();
            for (IParameter param : params) {
                if (param.getName().equals(getIdentifierPart())) {
                    return nodeFactory().createParameterNode(param);
                }
            }
        }
        return null;
    }

    /* private */protected IParameter[] getParameters() {
        return getExpression().findFormulaSignature(getIpsProject()).getParameters();
    }

}