/*******************************************************************************
 * Copyright (c) Faktor Zehn AG. <http://www.faktorzehn.org>
 * 
 * This source code is available under the terms of the AGPL Affero General Public License version
 * 3.
 * 
 * Please see LICENSE.txt for full license terms, including the additional permissions and
 * restrictions as well as the possibility of alternative license terms.
 *******************************************************************************/

package org.faktorips.devtools.core.internal.model.ipsproject;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.faktorips.devtools.core.model.ipsproject.IIpsArtefactBuilderSetConfig;
import org.faktorips.devtools.core.model.ipsproject.IIpsArtefactBuilderSetConfigModel;
import org.faktorips.devtools.core.model.ipsproject.IIpsArtefactBuilderSetInfo;
import org.faktorips.devtools.core.model.ipsproject.IIpsBuilderSetPropertyDef;
import org.faktorips.devtools.core.model.ipsproject.IIpsProject;
import org.faktorips.util.ArgumentCheck;
import org.faktorips.util.message.MessageList;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * The default implementation of the IIpsArtefactBuilderSetConfig interface
 * 
 * @author Peter Erzberger
 */
public class IpsArtefactBuilderSetConfigModel implements IIpsArtefactBuilderSetConfigModel {

    private static final String PROPERTY_XML_TAG = "Property"; //$NON-NLS-1$

    private static final String NAME_XML_ATTR = "name"; //$NON-NLS-1$

    private static final String VALUE_XML_ATTR = "value"; //$NON-NLS-1$

    private Map<String, String> properties;
    private Map<String, String> propertiesDescription;

    /**
     * Creates an empty IPS artifact builder set configuration instances.
     */
    public IpsArtefactBuilderSetConfigModel() {
        properties = new LinkedHashMap<String, String>();
        propertiesDescription = new LinkedHashMap<String, String>();
    }

    /**
     * This constructor is for test purposes only.
     */
    public IpsArtefactBuilderSetConfigModel(Map<String, String> properties) {
        ArgumentCheck.notNull(properties);
        this.properties = properties;
        propertiesDescription = new LinkedHashMap<String, String>();
    }

    /**
     * Creates and returns an IPS artifact builder set configuration instance from the provided dom
     * element.
     */
    @Override
    public final void initFromXml(Element el) {
        properties = new LinkedHashMap<String, String>();
        propertiesDescription = new LinkedHashMap<String, String>();
        NodeList nl = el.getElementsByTagName(PROPERTY_XML_TAG);
        for (int i = 0; i < nl.getLength(); i++) {
            Element propertyEl = (Element)nl.item(i);
            String key = propertyEl.getAttribute(NAME_XML_ATTR);
            String value = propertyEl.getAttribute(VALUE_XML_ATTR);
            NodeList propertyElNodeList = propertyEl.getChildNodes();
            for (int j = 0; j < propertyElNodeList.getLength(); j++) {
                Node node = propertyElNodeList.item(j);
                if (node instanceof Comment) {
                    Comment comment = (Comment)node;
                    propertiesDescription.put(key, comment.getData());
                }
            }
            properties.put(key, value);
        }
    }

    @Override
    public String getPropertyDescription(String propertyName) {
        return propertiesDescription.get(propertyName);
    }

    @Override
    public String getPropertyValue(String propertyName) {
        return properties.get(propertyName);
    }

    /**
     * Sets the value of a property
     * 
     * @param propertyName the name of the property
     * @param value the value of the property
     */
    @Override
    public void setPropertyValue(String propertyName, String value, String description) {
        ArgumentCheck.notNull(propertyName);
        ArgumentCheck.notNull(value);
        properties.put(propertyName, value);
        if (description != null) {
            propertiesDescription.put(propertyName, description);
        }
    }

    public void removeProperty(String propertyName) {
        properties.remove(propertyName);
    }

    @Override
    public final Element toXml(Document doc) {
        Element root = doc.createElement(XML_ELEMENT);
        Set<String> keys = properties.keySet();
        for (String key : keys) {
            String value = properties.get(key);
            Element prop = doc.createElement(PROPERTY_XML_TAG);
            String description = propertiesDescription.get(key);
            if (description != null) {
                Comment comment = doc.createComment(description);
                prop.appendChild(comment);
            }
            root.appendChild(prop);
            prop.setAttribute(NAME_XML_ATTR, key);
            prop.setAttribute(VALUE_XML_ATTR, value);
        }
        return root;
    }

    @Override
    public String[] getPropertyNames() {
        return properties.keySet().toArray(new String[properties.size()]);
    }

    @Override
    public IIpsArtefactBuilderSetConfig create(IIpsProject ipsProject, IIpsArtefactBuilderSetInfo builderSetInfo) {
        Map<String, Object> parsedValueMap = new LinkedHashMap<String, Object>();
        for (String name : this.properties.keySet()) {
            IIpsBuilderSetPropertyDef propertyDef = builderSetInfo.getPropertyDefinition(name);
            if (propertyDef == null) {
                /*
                 * Ignore properties without property definition. Let a migration remove them as
                 * necessary.
                 */
                continue;
            }
            String valueAsString = this.properties.get(name);
            if (!propertyDef.isAvailable(ipsProject)) {
                parsedValueMap.put(name, propertyDef.parseValue(propertyDef.getDisableValue(ipsProject)));
                continue;
            }
            Object value = propertyDef.parseValue(valueAsString);
            parsedValueMap.put(name, value);
        }
        IIpsBuilderSetPropertyDef[] propertyDefs = builderSetInfo.getPropertyDefinitions();
        for (IIpsBuilderSetPropertyDef propertyDef : propertyDefs) {
            Object value = parsedValueMap.get(propertyDef.getName());
            if (value == null) {
                parsedValueMap.put(propertyDef.getName(),
                        propertyDef.parseValue(propertyDef.getDisableValue(ipsProject)));
            }
        }
        return new IpsArtefactBuilderSetConfig(parsedValueMap);
    }

    @Override
    public MessageList validate(IIpsProject ipsProject, IpsArtefactBuilderSetInfo builderSetInfo) {
        return builderSetInfo.validateIpsArtefactBuilderSetConfig(ipsProject, this);
    }

}
