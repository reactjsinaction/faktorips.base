/*******************************************************************************
 * Copyright (c) Faktor Zehn AG. <http://www.faktorzehn.org>
 * 
 * This source code is available under the terms of the AGPL Affero General Public License version
 * 3.
 * 
 * Please see LICENSE.txt for full license terms, including the additional permissions and
 * restrictions as well as the possibility of alternative license terms.
 *******************************************************************************/
package org.faktorips.devtools.core.ui.wizards.productcmpt;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.faktorips.devtools.core.IpsPlugin;
import org.faktorips.devtools.core.model.ipsobject.IIpsSrcFile;
import org.faktorips.devtools.core.model.productcmpt.IProductCmpt;
import org.faktorips.devtools.core.model.productcmpt.IProductCmptGeneration;

public class InferTemplateOperation extends NewProductCmptOperation {

    protected InferTemplateOperation(InferTemplatePmo pmo) {
        super(pmo);
    }

    @Override
    protected InferTemplatePmo getPmo() {
        return (InferTemplatePmo)super.getPmo();
    }

    @Override
    protected void postProcess(IIpsSrcFile ipsSrcFile, IProgressMonitor monitor) {
        IProductCmpt template = (IProductCmpt)ipsSrcFile.getIpsObject();
        IProductCmptGeneration templateGeneration = template.getLatestProductCmptGeneration();
        List<IProductCmpt> productCmpts = getPmo().getProductCmptsToInferTemplate();

        InferTemplateProcessor inferTemplateProzessor = new InferTemplateProcessor(templateGeneration, productCmpts);
        IpsPlugin.getDefault().getIpsModel().runAndQueueChangeEvents(inferTemplateProzessor, monitor);
    }

}
