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

package org.faktorips.devtools.core.ui.table;

import java.util.Locale;

import org.faktorips.devtools.core.internal.model.LocalizedString;
import org.faktorips.devtools.core.model.ILocalizedString;
import org.faktorips.devtools.core.ui.controls.MultilingualValueAttributeControl;

/**
 * A cell editor using the {@link MultilingualValueAttributeControl} to enter values in different
 * languages.
 */
public class MultilingualTextCellEditor extends IpsCellEditor {
    private final Locale locale;

    public MultilingualTextCellEditor(Locale locale, MultilingualValueAttributeControl control) {
        super(control);
        this.locale = locale;
    }

    @Override
    public MultilingualValueAttributeControl getControl() {
        return (MultilingualValueAttributeControl)super.getControl();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Object doGetValue() {
        return new LocalizedString(locale, getControl().getText());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doSetFocus() {
        getControl().getTextControl().selectAll();
        getControl().setFocus();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doSetValue(Object value) {
        if (value instanceof ILocalizedString) {
            getControl().setText(((ILocalizedString)value).getValue());
        }
    }

    @Override
    public boolean isMappedValue() {
        return false;
    }
}