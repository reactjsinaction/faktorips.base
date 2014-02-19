/*******************************************************************************
 * Copyright (c) Faktor Zehn AG. <http://www.faktorzehn.org>
 * 
 * This source code is available under the terms of the AGPL Affero General Public License version
 * 3.
 * 
 * Please see LICENSE.txt for full license terms, including the additional permissions and
 * restrictions as well as the possibility of alternative license terms.
 *******************************************************************************/
package org.faktorips.devtools.core.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.commons.lang.StringUtils;
import org.faktorips.devtools.core.util.TextRegion;
import org.junit.Test;

public class TextRegionTest {

    private String completeIdentifierString = "oldString.oldPartOfString";
    private String newString = "thisIsTheNew";
    private TextRegion region;

    @Test
    public void testReplaceTextRegion() {
        region = new TextRegion(0, 3);
        String refactoredString = region.replaceTextRegion(completeIdentifierString, newString);

        assertEquals("thisIsTheNewString.oldPartOfString", refactoredString);
    }

    @Test
    public void testReplaceTextRegionEmptyString() {
        region = new TextRegion(10, 17);
        String refactoredString = region.replaceTextRegion(completeIdentifierString, StringUtils.EMPTY);

        assertEquals("oldString.OfString", refactoredString);
    }

    @Test
    public void testReplaceTextRegionInvalidStartEndPoints() {
        region = new TextRegion(-1, -8);
        String refactoredString = region.replaceTextRegion(completeIdentifierString, StringUtils.EMPTY);

        assertEquals(completeIdentifierString, refactoredString);
    }

    @Test
    public void testOffset() throws Exception {
        TextRegion textRegion = new TextRegion(13, 57);

        TextRegion offsetTextRegion = textRegion.offset(42);

        assertEquals(55, offsetTextRegion.getStart());
        assertEquals(99, offsetTextRegion.getEnd());
    }

    @Test
    public void testStartOffset() throws Exception {
        TextRegion textRegion = new TextRegion(13, 57);

        TextRegion offsetTextRegion = textRegion.startOffset(42);

        assertEquals(55, offsetTextRegion.getStart());
        assertEquals(57, offsetTextRegion.getEnd());
    }

    @Test
    public void testEndOffset() throws Exception {
        TextRegion textRegion = new TextRegion(13, 57);

        TextRegion offsetTextRegion = textRegion.endOffset(42);

        assertEquals(13, offsetTextRegion.getStart());
        assertEquals(99, offsetTextRegion.getEnd());
    }

    @Test
    public void testCompareTo() throws Exception {
        TextRegion textRegion1 = new TextRegion(2, 10);
        TextRegion textRegion2 = new TextRegion(5, 12);
        TextRegion textRegion3 = new TextRegion(2, 5);

        assertTrue(textRegion1.compareTo(textRegion1) == 0);
        assertTrue(textRegion2.compareTo(textRegion2) == 0);
        assertTrue(textRegion3.compareTo(textRegion3) == 0);
        assertTrue(textRegion1.compareTo(textRegion2) < 0);
        assertTrue(textRegion1.compareTo(textRegion3) > 0);
        assertTrue(textRegion2.compareTo(textRegion3) > 0);
    }

    @Test
    public void testGetSubstring() throws Exception {
        TextRegion region = new TextRegion(1, 5);

        String substring = region.getSubstring("abc123");

        assertEquals("bc12", substring);
    }

    @Test
    public void testGetSubstringIllegal() throws Exception {
        TextRegion region = new TextRegion(1, -5);

        String substring = region.getSubstring("abc123");

        assertEquals("abc123", substring);
    }
}
