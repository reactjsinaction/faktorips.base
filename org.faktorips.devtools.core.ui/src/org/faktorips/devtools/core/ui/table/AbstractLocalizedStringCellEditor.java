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

package org.faktorips.devtools.core.ui.table;

import java.util.Locale;

import org.apache.commons.lang.StringUtils;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.faktorips.values.LocalizedString;

/**
 * Abstract implementation for cell editors providing {@link LocalizedString localized strings}. The
 * object returned by this cell editor is always a {@link LocalizedString} in a predefined locale
 * and it handles only {@link LocalizedString} as input objects. The abstraction is needed because
 * the used control may not always be a single {@link Text} control (for example in
 * {@link InternationalStringCellEditor}. But every implementation of this cell editor have to
 * provide a {@link Text} control to get the input text from.
 * 
 * @author dirmeier
 */
public abstract class AbstractLocalizedStringCellEditor extends IpsCellEditor {

    private Locale locale;

    public AbstractLocalizedStringCellEditor(Control control) {
        super(control);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation always creates {@link LocalizedString} using the predefined locale.
     */
    @Override
    protected LocalizedString doGetValue() {
        return new LocalizedString(locale, getTextControl().getText());
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation only supports {@link LocalizedString}. The locale is simply ignored but
     * should be the locale predefined for this cell editor.
     */
    @Override
    protected void doSetValue(Object value) {
        if (value instanceof LocalizedString) {
            final LocalizedString localizedString = (LocalizedString)value;
            locale = localizedString.getLocale();
            String textValue = localizedString.getValue();
            getTextControl().setText(textValue == null ? StringUtils.EMPTY : textValue);
        } else {
            throw new IllegalArgumentException(
                    "The value of type " + value.getClass() + " is not not supported by AbstractLocalizedStringCellEditor"); //$NON-NLS-1$//$NON-NLS-2$
        }
    }

    @Override
    protected void doSetFocus() {
        getTextControl().selectAll();
        getControl().setFocus();
    }

    /**
     * Returns the {@link Text} control used to input the text in the predifned locale.
     */
    protected abstract Text getTextControl();

    @Override
    public boolean isMappedValue() {
        return false;
    }
}