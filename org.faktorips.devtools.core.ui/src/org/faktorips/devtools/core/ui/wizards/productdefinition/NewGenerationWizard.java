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

package org.faktorips.devtools.core.ui.wizards.productdefinition;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationTargetException;
import java.util.GregorianCalendar;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.faktorips.devtools.core.IpsPlugin;
import org.faktorips.devtools.core.model.ipsobject.IIpsObjectGeneration;
import org.faktorips.devtools.core.model.ipsobject.ITimedIpsObject;
import org.faktorips.devtools.core.ui.IpsUIPlugin;
import org.faktorips.devtools.core.ui.UIToolkit;
import org.faktorips.devtools.core.ui.binding.BindingContext;
import org.faktorips.devtools.core.ui.controller.fields.FormattingTextField;
import org.faktorips.devtools.core.ui.controller.fields.GregorianCalendarFormat;
import org.faktorips.devtools.core.ui.controls.Checkbox;
import org.faktorips.devtools.core.ui.controls.DateControl;

/**
 * Allows creation of new {@linkplain IIpsObjectGeneration IPS Object Generations} to one or many
 * {@linkplain ITimedIpsObject Timed IPS Objects}.
 */
public class NewGenerationWizard extends Wizard {

    private final NewGenerationPMO pmo = new NewGenerationPMO();

    private final List<ITimedIpsObject> timedIpsObjects;

    /**
     * @param timedIpsObjects a list containing all {@linkplain ITimedIpsObject Timed IPS Objects}
     *            to create new {@linkplain IIpsObjectGeneration IPS Object Generations} for
     */
    public NewGenerationWizard(List<ITimedIpsObject> timedIpsObjects) {
        this.timedIpsObjects = timedIpsObjects;
        setWindowTitle(Messages.NewGenerationWizard_title);
        setDefaultPageImageDescriptor(IpsUIPlugin.getImageHandling().createImageDescriptor(
                "wizards/NewGenerationWizard.png")); //$NON-NLS-1$
        setNeedsProgressMonitor(true);
    }

    @Override
    public void addPages() {
        addPage(new ChooseValidityDatePage(pmo, timedIpsObjects));
    }

    @Override
    public boolean performFinish() {
        // Execute runnable
        try {
            getContainer().run(true, true, new NewGenerationRunnable(pmo, timedIpsObjects));
        } catch (InvocationTargetException e) {
            IpsPlugin.logAndShowErrorDialog(e);
        } catch (InterruptedException e) {
            IpsPlugin.logAndShowErrorDialog(e);
        }

        // Update default validity date
        IpsUIPlugin.getDefault().setDefaultValidityDate(pmo.getValidFrom());

        return true;
    }

    /**
     * Allows the user to select a validity date (valid-from) for the new
     * {@linkplain IIpsObjectGeneration IPS Object Generations} to create.
     */
    private static class ChooseValidityDatePage extends WizardPage {

        private final BindingContext bindingContext = new BindingContext();

        private final NewGenerationPMO pmo;

        protected ChooseValidityDatePage(NewGenerationPMO pmo, List<ITimedIpsObject> timedIpsObjects) {
            super("ChooseValidityDate"); //$NON-NLS-1$
            this.pmo = pmo;

            // Set page title
            setTitle(Messages.ChooseValidityDatePage_pageTitle);

            // Set info message
            if (timedIpsObjects.size() == 1) {
                String objectName = timedIpsObjects.get(0).getName();
                setMessage(NLS.bind(Messages.ChooseValidityDatePage_msgPageInfoSingular, objectName));
            } else {
                setMessage(NLS.bind(Messages.ChooseValidityDatePage_msgPageInfoPlural, timedIpsObjects.size()));
            }

            addValidationListener();
        }

        private void addValidationListener() {
            pmo.addPropertyChangeListener(new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    setErrorMessage(null); // Reset error message
                    if (pmo.getValidFrom() == null) {
                        setErrorMessage(Messages.ChooseValidityDatePage_msgValidFromInvalid);
                    }
                }
            });
        }

        @Override
        public void createControl(Composite parent) {
            UIToolkit toolkit = new UIToolkit(null);
            Composite pageControl = toolkit.createLabelEditColumnComposite(parent);
            setControl(pageControl);

            // Valid from
            toolkit.createLabel(pageControl, Messages.ChooseValidityDatePage_labelValidFrom);
            DateControl validFromDateControl = new DateControl(pageControl, toolkit);
            Text validFromTextControl = validFromDateControl.getTextControl();
            bindingContext.bindContent(new FormattingTextField<GregorianCalendar>(validFromTextControl,
                    GregorianCalendarFormat.newInstance()), pmo, NewGenerationPMO.PROPERTY_VALID_FROM);

            // Skip existing generations
            toolkit.createLabel(pageControl, StringUtils.EMPTY);
            Checkbox skipExistingGenerationsCheckbox = toolkit.createCheckbox(pageControl,
                    Messages.ChooseValidityDatePage_labelSkipExistingGenerations);
            bindingContext.bindContent(skipExistingGenerationsCheckbox, pmo,
                    NewGenerationPMO.PROPERTY_SKIP_EXISTING_GENERATIONS);

            bindingContext.updateUI();
        }

        @Override
        public void dispose() {
            super.dispose();
            bindingContext.dispose();
        }

    }

}