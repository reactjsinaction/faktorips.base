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

import org.eclipse.core.runtime.CoreException;
import org.faktorips.abstracttest.AbstractIpsPluginTest;
import org.faktorips.datatype.Datatype;
import org.faktorips.devtools.core.internal.model.value.StringValue;
import org.faktorips.devtools.core.model.ContentChangeEvent;
import org.faktorips.devtools.core.model.ContentsChangeListener;
import org.faktorips.devtools.core.model.enums.IEnumAttribute;
import org.faktorips.devtools.core.model.enums.IEnumAttributeValue;
import org.faktorips.devtools.core.model.enums.IEnumContent;
import org.faktorips.devtools.core.model.enums.IEnumLiteralNameAttribute;
import org.faktorips.devtools.core.model.enums.IEnumType;
import org.faktorips.devtools.core.model.enums.IEnumValue;
import org.faktorips.devtools.core.model.ipsproject.IIpsProject;
import org.faktorips.devtools.core.model.value.ValueFactory;
import org.faktorips.util.message.MessageList;
import org.junit.Before;

/**
 * Base test for all enumeration tests providing a simple enumeration model with a gender
 * enumeration and a payment mode enumeration.
 * <p>
 * There is a gender <tt>IEnumType</tt>, the values are stored separated from the <tt>IEnumType</tt>
 * in a gender <tt>IEnumContent</tt>.
 * <p>
 * The payment mode <tt>IEnumType</tt> stores its values directly.
 * <p>
 * Utility methods and helpful string constants are provided.
 * 
 * @author Alexander Weickmann
 * 
 * @since 2.3
 */
public abstract class AbstractIpsEnumPluginTest extends AbstractIpsPluginTest {

    protected final String GENDER_ENUM_TYPE_NAME = "GenderEnumType";
    protected final String GENDER_ENUM_ATTRIBUTE_ID_NAME = "Id";
    protected final String GENDER_ENUM_ATTRIBUTE_NAME_NAME = "Name";
    protected final String GENDER_ENUM_CONTENT_NAME = "GenderEnumContent";

    protected final String GENDER_ENUM_LITERAL_MALE_ID = "m";
    protected final String GENDER_ENUM_LITERAL_FEMALE_ID = "w";
    protected final String GENDER_ENUM_LITERAL_MALE_NAME = "male";
    protected final String GENDER_ENUM_LITERAL_FEMALE_NAME = "female";

    protected final String ENUMCONTENTS_NAME = "enumcontents.GenderEnumContent";

    protected IIpsProject ipsProject;

    protected IEnumType genderEnumType;
    protected IEnumAttribute genderEnumAttributeId;
    protected IEnumAttribute genderEnumAttributeName;
    protected IEnumContent genderEnumContent;
    protected IEnumValue genderEnumValueMale;
    protected IEnumValue genderEnumValueFemale;

    protected EnumType paymentMode;

    protected ContentsChangeCounter contentsChangeCounter;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        ipsProject = newIpsProject();
        contentsChangeCounter = new ContentsChangeCounter();
        getIpsModel().addChangeListener(contentsChangeCounter);
        createGenderEnum();
        initGenderEnumValues();
        createPaymentModeEnum();
    }

    private void createPaymentModeEnum() throws Exception {
        paymentMode = newEnumType(ipsProject, "PaymentMode");
        paymentMode.setAbstract(false);
        paymentMode.setContainingValues(true);
        IEnumLiteralNameAttribute literalNameAttribute = paymentMode.newEnumLiteralNameAttribute();
        literalNameAttribute.setDefaultValueProviderAttribute("name");

        IEnumAttribute id = paymentMode.newEnumAttribute();
        id.setDatatype(Datatype.STRING.getQualifiedName());
        id.setInherited(false);
        id.setUnique(true);
        id.setName("id");
        id.setIdentifier(true);
        IEnumAttribute name = paymentMode.newEnumAttribute();
        name.setName("name");
        name.setUsedAsNameInFaktorIpsUi(true);
        name.setUnique(true);
        name.setDatatype(Datatype.STRING.getQualifiedName());

        IEnumValue value1 = paymentMode.newEnumValue();
        value1.setEnumAttributeValue(0, ValueFactory.createStringValue("MONTHLY"));
        value1.setEnumAttributeValue(1, ValueFactory.createStringValue("P1"));
        value1.setEnumAttributeValue(2, ValueFactory.createStringValue("monthly"));
        IEnumValue value2 = paymentMode.newEnumValue();
        value2.setEnumAttributeValue(0, ValueFactory.createStringValue("ANNUALLY"));
        value2.setEnumAttributeValue(1, ValueFactory.createStringValue("P2"));
        value2.setEnumAttributeValue(2, ValueFactory.createStringValue("annually"));
    }

    private void createGenderEnum() throws CoreException {
        genderEnumType = newEnumType(ipsProject, GENDER_ENUM_TYPE_NAME);
        genderEnumType.setAbstract(false);
        genderEnumType.setContainingValues(false);
        genderEnumType.setSuperEnumType("");
        genderEnumType.setEnumContentName(ENUMCONTENTS_NAME);

        genderEnumAttributeId = genderEnumType.newEnumAttribute();
        genderEnumAttributeId.setName(GENDER_ENUM_ATTRIBUTE_ID_NAME);
        genderEnumAttributeId.setDatatype(Datatype.STRING.getQualifiedName());
        genderEnumAttributeId.setUnique(true);
        genderEnumAttributeId.setIdentifier(true);
        genderEnumAttributeName = genderEnumType.newEnumAttribute();
        genderEnumAttributeName.setName(GENDER_ENUM_ATTRIBUTE_NAME_NAME);
        genderEnumAttributeName.setDatatype(Datatype.STRING.getQualifiedName());
        genderEnumAttributeName.setUsedAsNameInFaktorIpsUi(true);
        genderEnumAttributeName.setUnique(true);

        genderEnumContent = newEnumContent(ipsProject, ENUMCONTENTS_NAME);
        genderEnumContent.setEnumType(genderEnumType.getQualifiedName());

        genderEnumValueMale = genderEnumContent.newEnumValue();
        genderEnumValueFemale = genderEnumContent.newEnumValue();
    }

    private void initGenderEnumValues() {
        IEnumAttributeValue tempAttributeValueRef;

        tempAttributeValueRef = genderEnumValueMale.getEnumAttributeValues().get(0);
        tempAttributeValueRef.setValue(new StringValue(GENDER_ENUM_LITERAL_MALE_ID));
        tempAttributeValueRef = genderEnumValueMale.getEnumAttributeValues().get(1);
        tempAttributeValueRef.setValue(new StringValue(GENDER_ENUM_LITERAL_MALE_NAME));

        tempAttributeValueRef = genderEnumValueFemale.getEnumAttributeValues().get(0);
        tempAttributeValueRef.setValue(new StringValue(GENDER_ENUM_LITERAL_FEMALE_ID));
        tempAttributeValueRef = genderEnumValueFemale.getEnumAttributeValues().get(1);
        tempAttributeValueRef.setValue(new StringValue(GENDER_ENUM_LITERAL_FEMALE_NAME));
    }

    protected void assertOneValidationMessage(MessageList validationMessageList) {
        assertEquals(1, validationMessageList.size());
    }

    public class ContentsChangeCounter implements ContentsChangeListener {

        private int counter = 0;

        public int getCounts() {
            return counter;
        }

        public void reset() {
            counter = 0;
        }

        @Override
        public void contentsChanged(ContentChangeEvent event) {
            counter++;
        }
    }
}
