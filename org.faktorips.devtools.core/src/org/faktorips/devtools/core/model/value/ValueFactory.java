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

package org.faktorips.devtools.core.model.value;

import org.faktorips.devtools.core.internal.model.InternationalStringXmlHelper;
import org.faktorips.devtools.core.internal.model.value.InternationalStringValue;
import org.faktorips.devtools.core.internal.model.value.StringValue;
import org.faktorips.devtools.core.util.XmlUtil;
import org.faktorips.runtime.internal.ValueToXmlHelper;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

/**
 * Factory to build StringValue or InternationalStringValue
 * 
 * @author frank
 * @since 3.9
 */
public final class ValueFactory {

    private ValueFactory() {
        // only static
    }

    /**
     * Read the xml and creates a new IValue<?>.
     * 
     * @param valueEl Element
     */
    public static IValue<?> createValue(Element valueEl) {
        if (valueEl == null || Boolean.parseBoolean(valueEl.getAttribute(ValueToXmlHelper.XML_ATTRIBUTE_IS_NULL))) {
            return new StringValue(null);
        }
        if (InternationalStringXmlHelper.isInternationalStringElement(valueEl)) {
            return InternationalStringValue.createFromXml(valueEl);
        }
        CDATASection cdata = XmlUtil.getFirstCDataSection(valueEl);
        // if no cdata-section was found, the value stored was an empty string.
        // In this case, the cdata-section get lost during transformation of the
        // xml-document to a string.
        String result = ""; //$NON-NLS-1$
        if (cdata != null) {
            result = cdata.getData();
        } else {
            NodeList childNodes = valueEl.getChildNodes();
            if (childNodes.getLength() > 0) {
                Node node = childNodes.item(0);
                if (node instanceof Text) {
                    result = node.getNodeValue();
                }
            }
        }
        return new StringValue(result);
    }

    /**
     * Return the new {@link IValue}. If isMultilingual is <code>true</code>, then {@link IValue} is
     * {@link InternationalStringValue}. If <code>false</code>, then {@link IValue} is
     * {@link StringValue}.
     * 
     * @param isMultilingual <code>true</code> or <code>false</code>
     * @param value the value to set only in {@link StringValue}
     */
    public static IValue<?> createValue(boolean isMultilingual, String value) {
        if (isMultilingual) {
            return new InternationalStringValue();
        } else {
            return new StringValue(value);
        }
    }

    /**
     * Returns a new {@link StringValue}
     * 
     * @param value the value to set
     */
    public static IValue<String> createStringValue(String value) {
        return new StringValue(value);
    }
}
