/*******************************************************************************
 * Copyright (c) 2005-2012 Faktor Zehn AG und andere.
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

package org.faktorips.devtools.core.internal.model.enums;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Locale;

import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.runtime.CoreException;
import org.faktorips.datatype.Datatype;
import org.faktorips.devtools.core.internal.model.value.InternationalStringValue;
import org.faktorips.devtools.core.internal.model.value.StringValue;
import org.faktorips.devtools.core.model.IIpsModel;
import org.faktorips.devtools.core.model.IValidationMsgCodesForInvalidValues;
import org.faktorips.devtools.core.model.enums.IEnumAttribute;
import org.faktorips.devtools.core.model.enums.IEnumAttributeValue;
import org.faktorips.devtools.core.model.enums.IEnumContent;
import org.faktorips.devtools.core.model.enums.IEnumType;
import org.faktorips.devtools.core.model.enums.IEnumValue;
import org.faktorips.devtools.core.model.value.ValueFactory;
import org.faktorips.devtools.core.model.value.ValueType;
import org.faktorips.util.message.MessageList;
import org.faktorips.values.LocalizedString;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Element;

public class EnumAttributeValueTest extends AbstractIpsEnumPluginTest {

    private IEnumAttributeValue maleIdAttributeValue;
    private IEnumAttributeValue maleNameAttributeValue;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        maleIdAttributeValue = genderEnumValueMale.getEnumAttributeValues().get(0);
        maleNameAttributeValue = genderEnumValueMale.getEnumAttributeValues().get(1);
    }

    @Test
    public void testFindEnumAttribute() throws CoreException {
        try {
            maleIdAttributeValue.findEnumAttribute(null);
            fail();
        } catch (NullPointerException e) {
        }

        assertEquals(genderEnumAttributeId, maleIdAttributeValue.findEnumAttribute(ipsProject));
        assertEquals(genderEnumAttributeName, maleNameAttributeValue.findEnumAttribute(ipsProject));

        genderEnumContent.setEnumType("");
        assertNull(maleIdAttributeValue.findEnumAttribute(ipsProject));
        genderEnumContent.setEnumType(genderEnumType.getQualifiedName());

        genderEnumType.deleteEnumAttributeWithValues(genderEnumAttributeId);
        assertNull(maleIdAttributeValue.findEnumAttribute(ipsProject));

        genderEnumType.deleteEnumAttributeWithValues(genderEnumAttributeName);
        assertNull(maleNameAttributeValue.findEnumAttribute(ipsProject));
    }

    @Test
    public void testIsEnumLiteralNameAttributeValue() {
        assertFalse(maleNameAttributeValue.isEnumLiteralNameAttributeValue());
        IEnumValue enumValue = paymentMode.getEnumValues().get(0);
        IEnumAttributeValue literalNameValue = enumValue.getEnumAttributeValues().get(0);
        assertTrue(literalNameValue.isEnumLiteralNameAttributeValue());
        assertFalse(enumValue.getEnumAttributeValues().get(1).isEnumLiteralNameAttributeValue());
    }

    @Test
    public void testXml() throws ParserConfigurationException, CoreException {
        Element xmlElement = maleIdAttributeValue.toXml(createXmlDocument(IEnumAttributeValue.XML_TAG));
        assertEquals(GENDER_ENUM_LITERAL_MALE_ID, xmlElement.getTextContent());

        IEnumAttributeValue loadedAttributeValue = genderEnumValueMale.newEnumAttributeValue();
        loadedAttributeValue.initFromXml(xmlElement);
        assertEquals(GENDER_ENUM_LITERAL_MALE_ID, loadedAttributeValue.getStringValue());
    }

    @Test
    public void testPropertiesToXml() throws Exception {
        IEnumType enumType = newEnumType(ipsProject, "AnEnum");
        IEnumAttribute enumAttr = enumType.newEnumAttribute();
        enumAttr.setDatatype(Datatype.STRING.getQualifiedName());
        enumAttr.setName("a");
        enumAttr.setUnique(true);

        IEnumAttribute enumAttr2 = enumType.newEnumAttribute();
        enumAttr2.setDatatype(Datatype.INTEGER.getQualifiedName());
        enumAttr2.setName("b");
        enumAttr2.setUnique(true);

        IEnumValue enumValue = enumType.newEnumValue();
        List<IEnumAttributeValue> enumAttrList = enumValue.getEnumAttributeValues();
        enumAttrList.get(0).setValue(ValueFactory.createStringValue(null));
        enumAttrList.get(1).setValue(ValueFactory.createStringValue(null));

        Element enumTypeEl = enumType.toXml(createXmlDocument(IEnumContent.XML_TAG));
        IEnumType enumType2 = newEnumType(ipsProject, "AnEnum2");
        enumType2.initFromXml(enumTypeEl);
        assertNull(enumType2.getEnumValues().get(0).getEnumAttributeValues().get(0).getStringValue());
        assertNull(enumType2.getEnumValues().get(0).getEnumAttributeValues().get(1).getStringValue());

        List<IEnumAttributeValue> enumAttrList1 = enumValue.getEnumAttributeValues();
        enumAttrList1.get(0).setValue(ValueFactory.createStringValue("foo"));
        enumAttrList1.get(1).setValue(ValueFactory.createStringValue("bar"));

        Element enumTypeEl3 = enumType.toXml(createXmlDocument(IEnumContent.XML_TAG));
        IEnumType enumType3 = newEnumType(ipsProject, "AnEnum3");
        enumType3.initFromXml(enumTypeEl3);
        assertEquals("foo", enumType3.getEnumValues().get(0).getEnumAttributeValues().get(0).getStringValue());
        assertEquals("bar", enumType3.getEnumValues().get(0).getEnumAttributeValues().get(1).getStringValue());
    }

    @Test
    public void testValidateParsable() throws CoreException {
        IEnumAttribute stringAttribute = genderEnumType.newEnumAttribute();
        stringAttribute.setDatatype(Datatype.STRING.getQualifiedName());
        stringAttribute.setName("StringAttribute");

        IEnumAttribute integerAttribute = genderEnumType.newEnumAttribute();
        integerAttribute.setDatatype(Datatype.INTEGER.getQualifiedName());
        integerAttribute.setName("IntegerAttribute");

        IEnumAttribute booleanAttribute = genderEnumType.newEnumAttribute();
        booleanAttribute.setDatatype(Datatype.BOOLEAN.getQualifiedName());
        booleanAttribute.setName("BooleanAttribute");

        genderEnumType.setContainingValues(true);
        IEnumValue newEnumValue = genderEnumType.newEnumValue();

        IEnumAttributeValue stringNewAttributeValue = newEnumValue.getEnumAttributeValues().get(2);
        IEnumAttributeValue integerNewAttributeValue = newEnumValue.getEnumAttributeValues().get(3);
        IEnumAttributeValue booleanNewAttributeValue = newEnumValue.getEnumAttributeValues().get(4);

        stringNewAttributeValue.setValue(ValueFactory.createStringValue("String"));
        integerNewAttributeValue.setValue(ValueFactory.createStringValue("4"));
        booleanNewAttributeValue.setValue(ValueFactory.createStringValue("false"));

        assertTrue(stringNewAttributeValue.isValid(ipsProject));
        assertTrue(integerNewAttributeValue.isValid(ipsProject));
        assertTrue(booleanNewAttributeValue.isValid(ipsProject));

        IIpsModel ipsModel = getIpsModel();

        // Test value parsable with data type Integer.
        ipsModel.clearValidationCache();
        integerNewAttributeValue.setValue(ValueFactory.createStringValue("fooBar"));
        MessageList validationMessageList = integerNewAttributeValue.validate(ipsProject);
        assertOneValidationMessage(validationMessageList);
        assertNotNull(validationMessageList
                .getMessageByCode(IValidationMsgCodesForInvalidValues.MSGCODE_VALUE_IS_NOT_INSTANCE_OF_VALUEDATATYPE));
        integerNewAttributeValue.setValue(ValueFactory.createStringValue("4"));

        // Test value parsable with data type Boolean.
        ipsModel.clearValidationCache();
        booleanNewAttributeValue.setValue(ValueFactory.createStringValue("fooBar"));
        validationMessageList = booleanNewAttributeValue.validate(ipsProject);
        assertOneValidationMessage(validationMessageList);
        assertNotNull(validationMessageList
                .getMessageByCode(IValidationMsgCodesForInvalidValues.MSGCODE_VALUE_IS_NOT_INSTANCE_OF_VALUEDATATYPE));
        booleanNewAttributeValue.setValue(ValueFactory.createStringValue("false"));
    }

    @Test
    public void testValidateUniqueIdentifierValueEmpty() throws CoreException {
        IIpsModel ipsModel = getIpsModel();
        IEnumAttributeValue uniqueIdentifierEnumAttributeValue = genderEnumValueFemale.getEnumAttributeValues().get(0);

        uniqueIdentifierEnumAttributeValue.setValue(ValueFactory.createStringValue(""));
        MessageList validationMessageList = genderEnumValueFemale.validate(ipsProject);
        assertOneValidationMessage(validationMessageList);
        assertNotNull(validationMessageList
                .getMessageByCode(IEnumAttributeValue.MSGCODE_ENUM_ATTRIBUTE_VALUE_UNIQUE_IDENTIFIER_VALUE_EMPTY));

        ipsModel.clearValidationCache();
        uniqueIdentifierEnumAttributeValue.setValue(ValueFactory.createStringValue(null));
        validationMessageList = genderEnumValueFemale.validate(ipsProject);
        assertOneValidationMessage(validationMessageList);
        assertNotNull(validationMessageList
                .getMessageByCode(IEnumAttributeValue.MSGCODE_ENUM_ATTRIBUTE_VALUE_UNIQUE_IDENTIFIER_VALUE_EMPTY));
    }

    @Test
    public void testValidateUniqueIdentifierValueNotUnique() throws CoreException {
        IEnumAttributeValue uniqueIdentifierEnumAttributeValueMale = genderEnumValueMale.getEnumAttributeValues()
                .get(0);
        IEnumAttributeValue uniqueIdentifierEnumAttributeValueFemale = genderEnumValueFemale.getEnumAttributeValues()
                .get(0);

        uniqueIdentifierEnumAttributeValueMale.setValue(ValueFactory.createStringValue("foo"));
        uniqueIdentifierEnumAttributeValueFemale.setValue(ValueFactory.createStringValue("foo"));

        MessageList validationMessageList = uniqueIdentifierEnumAttributeValueMale.validate(ipsProject);
        assertOneValidationMessage(validationMessageList);
        assertNotNull(validationMessageList
                .getMessageByCode(IEnumAttributeValue.MSGCODE_ENUM_ATTRIBUTE_VALUE_UNIQUE_IDENTIFIER_NOT_UNIQUE));

        validationMessageList = uniqueIdentifierEnumAttributeValueFemale.validate(ipsProject);
        assertOneValidationMessage(validationMessageList);
        assertNotNull(validationMessageList
                .getMessageByCode(IEnumAttributeValue.MSGCODE_ENUM_ATTRIBUTE_VALUE_UNIQUE_IDENTIFIER_NOT_UNIQUE));
    }

    @Test
    public void testGetEnumValue() {
        assertEquals(genderEnumValueMale, genderEnumValueMale.getEnumAttributeValues().get(0).getEnumValue());
    }

    @Test
    public void testGetSetValue() {
        InternationalStringValue internationalStringValue = new InternationalStringValue();
        internationalStringValue.getContent().add(new LocalizedString(Locale.GERMAN, "foo"));
        maleNameAttributeValue.setValue(internationalStringValue);
        assertEquals(internationalStringValue, maleNameAttributeValue.getValue());

        StringValue stringValue = new StringValue("foo");
        maleNameAttributeValue.setValue(stringValue);
        assertEquals(stringValue, maleNameAttributeValue.getValue());
    }

    @Test
    public void testGetValueType() {
        InternationalStringValue internationalStringValue = new InternationalStringValue();
        internationalStringValue.getContent().add(new LocalizedString(Locale.GERMAN, "foo"));
        maleNameAttributeValue.setValue(internationalStringValue);
        assertEquals(ValueType.INTERNATIONAL_STRING, maleNameAttributeValue.getValueType());

        maleNameAttributeValue.setValue(new StringValue("foo"));
        assertEquals(ValueType.STRING, maleNameAttributeValue.getValueType());
    }

    @Test
    public void testGetSringValue() {
        InternationalStringValue internationalStringValue = new InternationalStringValue();
        internationalStringValue.getContent().add(new LocalizedString(Locale.GERMAN, "foo"));
        maleNameAttributeValue.setValue(internationalStringValue);
        assertEquals("de=foo", maleNameAttributeValue.getStringValue());

        maleNameAttributeValue.setValue(new StringValue("foo"));
        assertEquals("foo", maleNameAttributeValue.getStringValue());
    }

    @Test
    public void testIsNull() {
        InternationalStringValue internationalStringValue = new InternationalStringValue();
        maleNameAttributeValue.setValue(internationalStringValue);
        assertFalse(maleNameAttributeValue.isNullValue());

        maleNameAttributeValue.setValue(new StringValue(null));
        assertTrue(maleNameAttributeValue.isNullValue());
    }

    @Test
    public void testValidateMultilingual() throws CoreException {
        InternationalStringValue internationalStringValue = new InternationalStringValue();
        internationalStringValue.getContent().add(new LocalizedString(Locale.GERMAN, "foo"));
        maleNameAttributeValue.setValue(internationalStringValue);
        assertEquals(ValueType.INTERNATIONAL_STRING, maleNameAttributeValue.getValueType());
        maleNameAttributeValue.validate(ipsProject);
        assertFalse(maleNameAttributeValue.findEnumAttribute(ipsProject).isMultilingual());

        maleNameAttributeValue.setValue(new StringValue("foo"));
        maleNameAttributeValue.findEnumAttribute(ipsProject).setMultilingual(true);
        assertEquals(ValueType.STRING, maleNameAttributeValue.getValueType());
        maleNameAttributeValue.validate(ipsProject);
        assertTrue(maleNameAttributeValue.findEnumAttribute(ipsProject).isMultilingual());

    }

    @Test
    public void testFixValueType() {
        InternationalStringValue internationalStringValue = new InternationalStringValue();
        internationalStringValue.getContent().add(new LocalizedString(Locale.GERMAN, "foo"));
        maleNameAttributeValue.setValue(internationalStringValue);
        assertEquals(ValueType.INTERNATIONAL_STRING, maleNameAttributeValue.getValueType());
        maleNameAttributeValue.fixValueType(false);
        assertEquals(ValueType.STRING, maleNameAttributeValue.getValueType());

        maleNameAttributeValue.setValue(new StringValue("foo"));
        assertEquals(ValueType.STRING, maleNameAttributeValue.getValueType());
        maleNameAttributeValue.fixValueType(true);
        assertEquals(ValueType.INTERNATIONAL_STRING, maleNameAttributeValue.getValueType());
    }

}
