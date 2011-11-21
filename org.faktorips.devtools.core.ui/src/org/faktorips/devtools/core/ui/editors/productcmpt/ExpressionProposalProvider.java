/*******************************************************************************
 * Copyright (c) 2005-2011 Faktor Zehn AG und andere.
 * 
 * Alle Rechte vorbehalten.
 * 
 * Dieses Programm und alle mitgelieferten Sachen (Dokumentationen, Beispiele, Konfigurationen,
 * etc.) duerfen nur unter den Bedingungen der Faktor-Zehn-Community Lizenzvereinbarung - Version
 * 0.1 (vor Gruendung Community) genutzt werden, die Bestandteil der Auslieferung ist und auch unter
 * http://www.faktorzehn.org/fips:lizenz eingesehen werden kann.
 * 
 * Mitwirkende: Faktor Zehn AG - initial API and implementation - http://www.faktorzehn.de
 *******************************************************************************/

package org.faktorips.devtools.core.ui.editors.productcmpt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.faktorips.datatype.Datatype;
import org.faktorips.datatype.EnumDatatype;
import org.faktorips.devtools.core.IpsPlugin;
import org.faktorips.devtools.core.exception.CoreRuntimeException;
import org.faktorips.devtools.core.model.ipsobject.IIpsObjectPart;
import org.faktorips.devtools.core.model.ipsproject.IIpsProject;
import org.faktorips.devtools.core.model.pctype.IPolicyCmptType;
import org.faktorips.devtools.core.model.pctype.IPolicyCmptTypeAttribute;
import org.faktorips.devtools.core.model.productcmpt.IExpression;
import org.faktorips.devtools.core.model.type.IAttribute;
import org.faktorips.devtools.core.model.type.IMethod;
import org.faktorips.devtools.core.model.type.IParameter;
import org.faktorips.devtools.core.model.type.IType;
import org.faktorips.fl.ExprCompiler;
import org.faktorips.fl.FlFunction;
import org.faktorips.util.ArgumentCheck;

/**
 * A {@link IContentProposalProvider} for {@link IExpression}s for use in a
 * {@link ContentProposalAdapter}.
 * 
 * @author schwering
 */
public class ExpressionProposalProvider implements IContentProposalProvider {

    private static final String DEFAULT_VALUE = "@default"; //$NON-NLS-1$

    private IMethod signature;
    private IExpression expression;
    private IIpsProject ipsProject;

    public ExpressionProposalProvider(IExpression expression) {
        super();
        ArgumentCheck.notNull(expression);
        this.expression = expression;
        this.ipsProject = expression.getIpsProject();
        this.signature = expression.findFormulaSignature(ipsProject);
    }

    @Override
    public IContentProposal[] getProposals(String contents, int position) {
        String prefix = contents.substring(0, position);
        String identifier = getLastIdentifier(prefix);
        int pos = identifier.lastIndexOf('.');
        List<IContentProposal> result = new LinkedList<IContentProposal>();
        if (pos > 0) {
            String paramName = identifier.substring(0, pos);
            String attributePrefix = identifier.substring(pos + 1);
            addMatchingAttributes(result, paramName, attributePrefix);
            addDefaultValuesToResult(result, paramName, attributePrefix);
            addMatchingEnumValues(result, paramName, attributePrefix);
            addAdditionalProposals(result, getAdditionalProposals(paramName, attributePrefix));
        } else {
            addMatchingProductCmptTypeAttributes(result, identifier);
            addMatchingParameters(result, identifier);
            addMatchingFunctions(result, identifier);
            addMatchingEnumTypes(result, identifier);
            addAdditionalProposals(result, getAdditionalProposals(identifier));
        }
        return result.toArray(new IContentProposal[result.size()]);
    }

    private void addAdditionalProposals(final List<IContentProposal> result,
            final List<IContentProposal> additionalProposals) {
        for (IContentProposal proposal : additionalProposals) {
            if (!result.contains(proposal)) {
                result.add(proposal);
            }
        }
    }

    private void addMatchingEnumTypes(List<IContentProposal> result, String enumTypePrefix) {
        EnumDatatype[] enumTypes = expression.getEnumDatatypesAllowedInFormula();
        for (EnumDatatype enumType : enumTypes) {
            if (enumType.getName().startsWith(enumTypePrefix)) {
                IContentProposal proposal = new ContentProposal(enumType.getName(), enumType.getName(),
                        enumType.getName());
                result.add(proposal);
            }
        }
    }

    private void addMatchingEnumValues(List<IContentProposal> result, String enumTypeName, String enumValuePrefix) {
        EnumDatatype[] enumTypes = expression.getEnumDatatypesAllowedInFormula();
        for (EnumDatatype enumType : enumTypes) {
            if (enumType.getName().equals(enumTypeName)) {
                String[] valueIds = enumType.getAllValueIds(false);
                for (String valueId : valueIds) {
                    if (valueId.startsWith(enumValuePrefix)) {
                        addEnumValueToResult(result, valueId, enumValuePrefix);
                    }
                }
                return;
            }
        }
    }

    private void addEnumValueToResult(List<IContentProposal> result, String enumValue, String prefix) {
        IContentProposal proposal = new ContentProposal(removePrefix(enumValue, prefix), enumValue, null);
        result.add(proposal);
    }

    private void addMatchingProductCmptTypeAttributes(List<IContentProposal> result, String prefix) {
        List<IAttribute> attributes = expression.findMatchingProductCmptTypeAttributes();
        for (IAttribute attribute : attributes) {
            if (attribute.getName().startsWith(prefix)) {
                addPartToResult(result, attribute, attribute.getDatatype(), prefix);
            }
        }
    }

    private void addPartToResult(List<IContentProposal> result, IIpsObjectPart part, String datatype, String prefix) {
        String name = part.getName();
        String displayText = name + " - " + datatype; //$NON-NLS-1$
        IContentProposal proposal = new ContentProposal(removePrefix(name, prefix), displayText, null);
        result.add(proposal);
    }

    private void addMatchingParameters(List<IContentProposal> result, String prefix) {
        IParameter[] params = getSignature().getParameters();
        for (IParameter param : params) {
            if (param.getName().toLowerCase().startsWith(prefix.toLowerCase())) {
                addPartToResult(result, param, param.getDatatype(), prefix);
            }
        }
    }

    /**
     * This method should be used by subclasses that want to provide additional
     * {@link IContentProposal}s.
     * 
     * @param prefix the prefix for the proposals
     * @return additional {@link IContentProposal}s
     */
    protected List<IContentProposal> getAdditionalProposals(final String prefix) {
        return Collections.emptyList();
    }

    /**
     * This method should be used by subclasses that want to provide additional
     * {@link IContentProposal}s.
     * 
     * @param paramName the name of the parameter on which the additional proposals are based
     * @param prefix the prefix for the proposals
     * @return additional {@link IContentProposal}s
     */
    protected List<IContentProposal> getAdditionalProposals(final String paramName, final String prefix) {
        return Collections.emptyList();
    }

    private void addMatchingFunctions(List<IContentProposal> result, String prefix) {
        ExprCompiler compiler = expression.newExprCompiler(expression.getIpsProject());
        FlFunction[] functions = compiler.getFunctions();
        Arrays.sort(functions, new Comparator<FlFunction>() {

            @Override
            public int compare(FlFunction o1, FlFunction o2) {
                FlFunction f1 = o1;
                FlFunction f2 = o2;
                return f1.getName().compareTo(f2.getName());
            }
        });
        for (FlFunction function : functions) {
            if (function.getName().toLowerCase().startsWith(prefix.toLowerCase())) {
                addFunctionToResult(result, function, prefix);
            }
        }
    }

    private void addFunctionToResult(List<IContentProposal> result, FlFunction function, String prefix) {
        String name = function.getName();
        StringBuffer displayText = new StringBuffer(name);
        displayText.append('(');
        Datatype[] argTypes = function.getArgTypes();
        for (int i = 0; i < argTypes.length; i++) {
            if (i > 0) {
                displayText.append(", "); //$NON-NLS-1$
            }
            displayText.append(argTypes[i].getName());
        }
        displayText.append(')');
        displayText.append(" - "); //$NON-NLS-1$
        displayText.append(function.getType().getName());
        String description = function.getDescription();
        description = description.replaceAll("\r\n", "<br>"); //$NON-NLS-1$ //$NON-NLS-2$
        description = description.replaceAll("\n", "<br>"); //$NON-NLS-1$ //$NON-NLS-2$
        IContentProposal proposal = new ContentProposal(removePrefix(name, prefix), displayText.toString(), description);
        result.add(proposal);
    }

    protected String removePrefix(String name, String prefix) {
        return name.startsWith(prefix) ? name.substring(prefix.length()) : name;
    }

    private void addMatchingAttributes(List<IContentProposal> result, String paramName, String attributePrefix) {
        try {
            Datatype datatype = findParamDatatype(paramName);
            if (!(datatype instanceof IType)) {
                return;
            }
            List<IAttribute> attributes = ((IType)datatype).findAllAttributes(getIpsProject());
            List<String> attributeNames = new ArrayList<String>();
            for (IAttribute attribute : attributes) {
                if (attribute.getName().startsWith(attributePrefix)) {
                    if (!attributeNames.contains(attribute.getName())) {
                        addAttributeToResult(result, attribute, attributePrefix);
                        attributeNames.add(attribute.getName());
                    }
                }
            }
        } catch (CoreException e) {
            throw new CoreRuntimeException(e.getMessage(), e);
        }
    }

    protected Datatype findParamDatatype(String paramName) {
        IParameter param = getParameter(paramName);
        if (param == null) {
            return null;
        }
        try {
            return param.findDatatype(getIpsProject());
        } catch (CoreException e) {
            throw new CoreRuntimeException(e.getMessage(), e);
        }
    }

    private void addAttributeToResult(List<IContentProposal> result, IAttribute attribute, String prefix) {
        String name = attribute.getName();
        String displayText = name + " - " + attribute.getDatatype(); //$NON-NLS-1$
        String localizedDescription = IpsPlugin.getMultiLanguageSupport().getLocalizedDescription(attribute);
        IContentProposal proposal = new ContentProposal(removePrefix(name, prefix), displayText, localizedDescription);
        result.add(proposal);
    }

    private static class ContentProposal implements IContentProposal {
        // FIXME in Eclipse 3.6: use org.eclipse.jface.fieldassist.ContentProposal
        private final String content;
        private final String label;
        private final String description;

        public ContentProposal(final String content, final String label, final String description) {
            this.content = content;
            this.label = label;
            this.description = description;

        }

        @Override
        public String getLabel() {
            return label;
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public int getCursorPosition() {
            return content.length();
        }

        @Override
        public String getContent() {
            return content;
        }

    }

    protected IParameter getParameter(String name) {
        IParameter[] params = getSignature().getParameters();
        for (IParameter param : params) {
            if (param.getName().equals(name)) {
                return param;
            }
        }
        return null;
    }

    /**
     * The characters that are checked within this method have to be in synch with the identifier
     * tokens defined in the ffl.jjt grammar
     */
    private String getLastIdentifier(String s) {
        if (StringUtils.isEmpty(s)) {
            return ""; //$NON-NLS-1$
        }
        int i = s.length() - 1;
        while (i >= 0) {
            if (!Character.isLetterOrDigit(s.charAt(i)) && s.charAt(i) != '.' && s.charAt(i) != '_'
                    && s.charAt(i) != '-') {
                break;
            }
            i--;
        }
        return s.substring(i + 1);
    }

    public IIpsProject getIpsProject() {
        return ipsProject;
    }

    public IMethod getSignature() {
        return signature;
    }

    private void addDefaultValuesToResult(final List<IContentProposal> result,
            final String paramName,
            final String attributePrefix) {
        try {
            final Datatype datatype = findParamDatatype(paramName);
            if (!(datatype instanceof IPolicyCmptType)) {
                return;
            }
            String prefix = attributePrefix;
            if (prefix.indexOf('@') > 0) {
                prefix = prefix.substring(0, prefix.indexOf('@'));
            }
            final IIpsProject ipsProject = getIpsProject();
            final List<IAttribute> attributes = findProductRelevantAttributes((IPolicyCmptType)datatype, ipsProject);
            final List<String> attributeNames = new ArrayList<String>();
            for (final IAttribute attribute : attributes) {
                if (attribute.getName().startsWith(prefix) && !attributeNames.contains(attribute.getName())) {
                    addDefaultValueToResult(result, attribute, attributePrefix);
                    attributeNames.add(attribute.getName());
                }
            }

        } catch (final CoreException e) {
            throw new CoreRuntimeException(e.getMessage(), e);
        }
    }

    private void addDefaultValueToResult(final List<IContentProposal> result,
            final IAttribute attribute,
            final String prefix) {
        String name = attribute.getName() + DEFAULT_VALUE;
        final String displayText = name
                + " - " + attribute.getDatatype() + Messages.ExpressionProposalProvider_defaultValue; //$NON-NLS-1$
        final String localizedDescription = IpsPlugin.getMultiLanguageSupport().getLocalizedDescription(attribute);
        final IContentProposal proposal = new ContentProposal(removePrefix(name, prefix), displayText,
                localizedDescription);
        result.add(proposal);
    }

    private List<IAttribute> findProductRelevantAttributes(final IPolicyCmptType datatype, final IIpsProject ipsProject)
            throws CoreException {
        final List<IAttribute> attributes = datatype.findAllAttributes(ipsProject);
        for (Iterator<IAttribute> iterator = attributes.iterator(); iterator.hasNext();) {
            IPolicyCmptTypeAttribute attribute = (IPolicyCmptTypeAttribute)iterator.next();
            if (!attribute.isProductRelevant()) {
                iterator.remove();
            }
        }
        return attributes;
    }
}