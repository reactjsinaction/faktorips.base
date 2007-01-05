package org.faktorips.devtools.core.ui.editors.testcase;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.osgi.util.NLS;
import org.faktorips.devtools.core.IpsPlugin;
import org.faktorips.devtools.core.model.testcase.ITestCase;
import org.faktorips.devtools.core.ui.editors.DescriptionPage;
import org.faktorips.devtools.core.ui.editors.IpsObjectEditor;
import org.faktorips.devtools.core.ui.editors.testcase.deltapresentation.TestCaseDeltaDialog;

/**
 * The editor to edit test cases based on test case types.
 * 
 * @author Joerg Ortmann
 */
public class TestCaseEditor extends IpsObjectEditor {

    TestCaseEditorPage editorPage;
    
    public TestCaseEditor() {
        super();
    }

    /**
     * {@inheritDoc}
     */
    public void doSave(IProgressMonitor monitor) {
        super.doSave(monitor);
    }

    /**
     * (@inheritDoc)
     */
    protected void addPagesForParsableSrcFile() throws CoreException {
        // open the select template dialog if the templ. is missing and the data is changeable
        if (getTestCase().findTestCaseType() == null 
                && couldDateBeChangedIfTestCaseTypeWasntMissing()
                && !IpsPlugin.getDefault().isTestMode()) {
            String msg = NLS
                    .bind(Messages.TestCaseEditor_Information_TemplateNotFound, getTestCase().getTestCaseType());
            postOpenDialogInUiThread(new SetTemplateDialog(getTestCase(), getSite().getShell(), msg));
        }
        TestCaseContentProvider contentProviderInput = new TestCaseContentProvider(TestCaseContentProvider.COMBINED,
                getTestCase());

        editorPage = new TestCaseEditorPage(this, Messages.TestCaseEditor_Combined_Title,
                contentProviderInput, Messages.TestCaseEditor_Combined_SectionTitle,
                Messages.TestCaseEditor_Combined_Description);
        
        addPage(editorPage);
        addPage(new DescriptionPage(this));
    }

    /**
     * Returns the test case the editor belongs to.
     */
    ITestCase getTestCase() {
        try {
            return (ITestCase)getIpsSrcFile().getIpsObject();
        } catch (Exception e) {
            IpsPlugin.logAndShowErrorDialog(e);
            throw new RuntimeException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    protected String getUniformPageTitle() {
        return NLS.bind(Messages.TestCaseEditor_Title, getTestCase().getName(), getTestCase().getTestCaseType());
    }

    /**
     * {@inheritDoc}
     */
    protected Dialog createDialogToFixDifferencesToModel() throws CoreException {
        return new TestCaseDeltaDialog(getTestCase().computeDeltaToTestCaseType(), getSite().getShell());
    }
    
    /**
     * {@inheritDoc}
     */
    protected boolean computeDataChangeableState() {
        if (!couldDateBeChangedIfTestCaseTypeWasntMissing()) {
            return false;
        }
        try {
            return getTestCase().findTestCaseType() != null;
        } catch (CoreException e) {
            IpsPlugin.log(e);
            return false;
        }
    }  
    
    private boolean couldDateBeChangedIfTestCaseTypeWasntMissing() {
        return super.computeDataChangeableState();
    }
    
}
