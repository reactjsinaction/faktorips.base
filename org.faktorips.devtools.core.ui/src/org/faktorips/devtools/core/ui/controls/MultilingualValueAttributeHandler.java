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

package org.faktorips.devtools.core.ui.controls;

import org.eclipse.swt.widgets.Shell;
import org.faktorips.devtools.core.ui.dialogs.MultilingualValueDialog;

public class MultilingualValueAttributeHandler {
    private final Shell shell;
    private final ISingleValueHolderProvider valueHolderProvider;

    public MultilingualValueAttributeHandler(Shell shell, ISingleValueHolderProvider valueHolderProvider) {
        this.shell = shell;
        this.valueHolderProvider = valueHolderProvider;
    }

    public void editValues() {
        openMultilingualValueDialog();
    }

    protected void openMultilingualValueDialog() {
        MultilingualValueDialog multilingualValueDialog = new MultilingualValueDialog(shell, valueHolderProvider);
        multilingualValueDialog.open();
    }
}