/*******************************************************************************
 * Copyright (c) Faktor Zehn AG. <http://www.faktorzehn.org>
 * 
 * This source code is available under the terms of the AGPL Affero General Public License version
 * 3.
 * 
 * Please see LICENSE.txt for full license terms, including the additional permissions and
 * restrictions as well as the possibility of alternative license terms.
 *******************************************************************************/

package org.faktorips.runtime.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Utility methods for initFromXml methods used by {@link ProductComponent} and
 * {@link ProductComponentGeneration}.
 * 
 * 
 * @author dirmeier
 */
final class ProductComponentXmlUtil {

    /*
     * Package private so ProductComponentGeneration can access the constants.
     */
    static final String XML_TAG_FORMULA = "Formula";
    static final String XML_TAG_EXPRESSION = "Expression";
    static final String XML_ATTRIBUTE_FORMULA_SIGNATURE = "formulaSignature";

    private ProductComponentXmlUtil() {
        // do not instantiate
    }

    /**
     * Returns a map containing the xml elements representing relations found in the indicated
     * generation's xml element. For each policy component type relation (pcTypeRelation) the map
     * contains an entry with the pcTypeRelation as key. The value is an array list containing all
     * relation elements for the pcTypeRelation.
     * 
     * @param genElement An xml element containing a product component generation's data.
     * @throws NullPointerException if genElement is <code>null</code>.
     */
    static final Map<String, List<Element>> getLinkElements(Element genElement) {
        Map<String, List<Element>> elementMap = new HashMap<String, List<Element>>();
        NodeList nl = genElement.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE && "Link".equals(node.getNodeName())) {
                Element childElement = (Element)nl.item(i);
                String association = childElement.getAttribute("association");
                List<Element> associationElements = elementMap.get(association);
                if (associationElements == null) {
                    associationElements = new ArrayList<Element>(1);
                    elementMap.put(association, associationElements);
                }
                associationElements.add(childElement);
            }

        }
        return elementMap;
    }

    /**
     * Returns a map containing the xml elements representing config elements found in the indicated
     * generation's xml element. For each config element the map contains an entry with the
     * pcTypeAttribute's name as key and the xml element containing the config element data as
     * value.
     * 
     * @param element An xml element containing a product component generation's data.
     * @throws NullPointerException if genElement is <code>null</code>.
     */
    // note: not private to allow access by test case
    static final Map<String, Element> getPropertyElements(Element element) {
        Map<String, Element> elementMap = new HashMap<String, Element>();
        NodeList nl = element.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                if (ValueToXmlHelper.XML_TAG_CONFIG_ELEMENT.equals(node.getNodeName())) {
                    Element childElement = (Element)nl.item(i);
                    elementMap.put(childElement.getAttribute(ValueToXmlHelper.XML_ATTRIBUTE_ATTRIBUTE), childElement);
                } else if (ValueToXmlHelper.XML_TAG_ATTRIBUTE_VALUE.equals(node.getNodeName())) {
                    Element childElement = (Element)nl.item(i);
                    elementMap.put(childElement.getAttribute(ValueToXmlHelper.XML_ATTRIBUTE_ATTRIBUTE), childElement);
                } else if (ValueToXmlHelper.XML_TAG_TABLE_CONTENT_USAGE.equals(node.getNodeName())) {
                    Element childElement = (Element)nl.item(i);
                    String structureUsage = childElement.getAttribute(ValueToXmlHelper.XML_ATTRIBUTE_STRUCTURE_USAGE);
                    elementMap.put(structureUsage, childElement);
                }

            }
        }
        return elementMap;
    }

    /**
     * Returns a set containing the formulaSignatures and expression of all available formulas (with
     * a not empty expression) found in the indicated xml element.
     * 
     * @param element An xml element containing the data.
     * @throws NullPointerException if element is <code>null</code>.
     */
    static final Map<String, String> getAvailableFormulars(Element element) {
        Map<String, String> availableFormulas = new LinkedHashMap<String, String>();
        NodeList nl = element.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element aFormula = (Element)node;
                if (XML_TAG_FORMULA.equals(node.getNodeName())) {
                    String name = aFormula.getAttribute(XML_ATTRIBUTE_FORMULA_SIGNATURE);
                    NodeList nodeList = aFormula.getElementsByTagName(XML_TAG_EXPRESSION);
                    Element expressionElement = (Element)nodeList.item(0);

                    String content = expressionElement.getTextContent();
                    availableFormulas.put(name, content.trim());
                }
            }
        }
        return availableFormulas;
    }
}