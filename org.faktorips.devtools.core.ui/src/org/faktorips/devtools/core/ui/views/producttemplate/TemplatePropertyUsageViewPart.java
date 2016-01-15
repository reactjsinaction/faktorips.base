/*******************************************************************************
 * Copyright (c) Faktor Zehn AG. <http://www.faktorzehn.org>
 * 
 * This source code is available under the terms of the AGPL Affero General Public License version
 * 3.
 * 
 * Please see LICENSE.txt for full license terms, including the additional permissions and
 * restrictions as well as the possibility of alternative license terms.
 *******************************************************************************/
package org.faktorips.devtools.core.ui.views.producttemplate;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;
import org.faktorips.devtools.core.model.productcmpt.IPropertyValue;

public class TemplatePropertyUsageViewPart extends ViewPart {

    private TemplatePropertyUsageView view;

    public TemplatePropertyUsageViewPart() {
    }

    @Override
    public void createPartControl(Composite parent) {
        view = new TemplatePropertyUsageView(parent);
        view.setUpToolbar(this.getViewSite());
    }

    @Override
    public void setFocus() {
        view.setFocus();
    }

    @Override
    public void dispose() {
        super.dispose();
        view.dispose();

    }

    public void setPropertyValue(IPropertyValue v) {
        view.setPropertyValue(v);
    }

}
