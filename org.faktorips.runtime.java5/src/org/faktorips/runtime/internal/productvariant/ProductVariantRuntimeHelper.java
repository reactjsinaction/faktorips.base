/*******************************************************************************
 * Copyright (c) 2005-2011 Faktor Zehn AG und andere.
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

package org.faktorips.runtime.internal.productvariant;

import org.faktorips.runtime.internal.AbstractClassLoadingRuntimeRepository;
import org.faktorips.runtime.internal.ProductComponent;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Helper class for loading product variants or varied product components respectively.
 * 
 * @author Stefan Widmaier, FaktorZehn AG
 */
public class ProductVariantRuntimeHelper {

    protected static final String ATTRIBUTE_NAME_VARIED_PRODUCT_CMPT = "variedProductCmpt";
    protected static final String ATTRIBUTE_NAME_RUNTIME_ID = "runtimeID";
    protected static final String ATTRIBUTE_NAME_VALID_TO = "validTo";

    public void loadProductComponentVariation(AbstractClassLoadingRuntimeRepository runtimeRepository,
            Element variationElement,
            ProductComponent productCmpt) {
        String originalRuntimeID = variationElement.getAttribute(ATTRIBUTE_NAME_VARIED_PRODUCT_CMPT);
        ProductComponent originalCmpt = (ProductComponent)runtimeRepository.getProductComponent(originalRuntimeID);
        Document document = (Document)variationElement.getOwnerDocument().cloneNode(false);
        Element originalElement = originalCmpt.toXml(document, false);

        productCmpt.initFromXml(originalElement);
        productCmpt.initFromXml(variationElement);
    }

    public boolean isProductVariantXML(Element productVariantElement) {
        // oder genuegt das attribut variedProdCmpt?
        return "ProductVariant".equals(productVariantElement.getNodeName());
    }

    /**
     * Searches the product variant XML for a variant component with the given runtime id. This
     * method does not return <code>null</code>. Instead a {@link RuntimeException} is thrown in
     * case no variant component XML could be found.
     * 
     * @param productVariantElement the document element of the product variant XML file
     * @param variationRuntimeID the searched runtime id. This is the runtime id a product component
     *            was requested for.
     * @return the variant component XML element with the given runtime id.
     */
    protected Element findVariationElement(Element productVariantElement, String variationRuntimeID) {
        Node rootCmpt = productVariantElement.getFirstChild();
        if ("ProductVariantCmpt".equals(rootCmpt.getNodeName())) {
            Element variationElement = recursiveFindProductComponentElement(rootCmpt, variationRuntimeID);
            if (variationElement == null) {
                throw new RuntimeException("No variant component with runtime id \"" + variationRuntimeID
                        + "\" could be found in XML.");
            }
            return variationElement;
        } else {
            throw new RuntimeException("No variant components defined in XML.");
        }
    }

    private Element recursiveFindProductComponentElement(Node variantCmptNode, String variationRuntimeID) {
        if (!(variantCmptNode instanceof Element)) {
            return null;
        }
        Element variantCmptElement = (Element)variantCmptNode;
        if (variationRuntimeID.equals(variantCmptElement.getAttribute(ATTRIBUTE_NAME_RUNTIME_ID))) {
            return variantCmptElement;
        } else {
            NodeList children = variantCmptElement.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                Element foundElement = recursiveFindProductComponentElement(child, variationRuntimeID);
                if (foundElement != null) {
                    return foundElement;
                }
            }
            return null;
        }
    }
}