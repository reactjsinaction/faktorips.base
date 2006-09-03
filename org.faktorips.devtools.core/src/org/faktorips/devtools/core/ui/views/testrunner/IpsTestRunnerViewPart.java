/*******************************************************************************
 * Copyright (c) 2005,2006 Faktor Zehn GmbH und andere.
 *
 * Alle Rechte vorbehalten.
 *
 * Dieses Programm und alle mitgelieferten Sachen (Dokumentationen, Beispiele,
 * Konfigurationen, etc.) dürfen nur unter den Bedingungen der 
 * Faktor-Zehn-Community Lizenzvereinbarung – Version 0.1 (vor Gründung Community) 
 * genutzt werden, die Bestandteil der Auslieferung ist und auch unter
 *   http://www.faktorips.org/legal/cl-v01.html
 * eingesehen werden kann.
 *
 * Mitwirkende:
 *   Faktor Zehn GmbH - initial API and implementation 
 *
 *******************************************************************************/

package org.faktorips.devtools.core.ui.views.testrunner;

import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ViewForm;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.progress.UIJob;
import org.faktorips.devtools.core.IpsPlugin;
import org.faktorips.devtools.core.model.testcase.IIpsTestRunListener;
import org.faktorips.devtools.core.model.testcase.IIpsTestRunner;
import org.faktorips.util.StringUtil;

/**
 * A ViewPart that shows the results of a ips test run.
 * 
 * @author Joerg Ortmann
 */
public class IpsTestRunnerViewPart extends ViewPart implements IIpsTestRunListener {
	public static final String EXTENSION_ID = "org.faktorips.devtools.core.ui.views.testRunner"; //$NON-NLS-1$
	
	static final int REFRESH_INTERVAL = 200;
	
	/* Ui components */
	private Composite fCounterComposite;
	private IpsTestCounterPanel fCounterPanel;
	private IpsTestProgressBar fProgressBar;
	private SashForm fSashForm;
	private FailurePane fFailurePane;
	private TestRunPane fTestRunPane;
	private Composite fParent;
	
    // Contains the status message
	protected volatile String fStatus = ""; //$NON-NLS-1$
	
	/*
	 * The current orientation; either <code>VIEW_ORIENTATION_HORIZONTAL</code>
	 * <code>VIEW_ORIENTATION_VERTICAL</code>, or <code>VIEW_ORIENTATION_AUTOMATIC</code>.
	 */
	private int fOrientation= VIEW_ORIENTATION_AUTOMATIC;
	private int fCurrentOrientation;
	
	/* Actions */
    private Action fStopTestRunAction;
	private Action fRerunLastTestAction;
	private Action fNextAction;
	private Action fPreviousAction;
	private ToggleOrientationAction[] fToggleOrientationActions;
    
	/* Sash form orientations */
	static final int VIEW_ORIENTATION_VERTICAL = 0;
	static final int VIEW_ORIENTATION_HORIZONTAL = 1;
	static final int VIEW_ORIENTATION_AUTOMATIC = 2;
	
    // Persistence tags.
    static final String TAG_PAGE= "page"; //$NON-NLS-1$
    static final String TAG_RATIO= "ratio"; //$NON-NLS-1$
    static final String TAG_TRACEFILTER= "tracefilter"; //$NON-NLS-1$ 
    static final String TAG_ORIENTATION= "orientation"; //$NON-NLS-1$
    static final String TAG_SCROLL= "scroll"; //$NON-NLS-1$
    
    private IMemento fMemento;  
    
	/* Queue used for processing Tree Entries */
	private List fTableEntryQueue = new ArrayList();
	
	/* Is the UI disposed */
	private boolean fIsDisposed= false;
	
	/* Indicates an instance of TreeEntryQueueDrainer is already running, or scheduled to */
	private boolean fQueueDrainRequestOutstanding;
	
 	/*
 	 * Number of executed tests during a test run
 	 */
	private volatile int fExecutedTests;
	/*
	 * Number of errors during this test run
	 */
	private volatile int fErrorCount;
	/*
	 * Number of failures during this test run
	 */
	private volatile int fFailureCount;
	
	/* Indicates that there was an failure */
	private boolean isFailure;
	
	private UpdateUIJob fUpdateJob;
	
	private int testRuns=0;
	private int testId;
	
	// Test last test run context
	private String repositoryPackage;
	private String testPackage;
	
	// The project which contains the runned tests 
	private IJavaProject fTestProject;
	
    // Contains the map to do the mapping between the test case ids (unique id in the table run pane
    // table) and the test case qualified name
    private HashMap testId2TestQualifiedNameMap = new HashMap();
    
    /*
     * Action class to stop the currently running test.
     */
    private class StopTestRunAction extends Action {
        public StopTestRunAction() {
            setText(Messages.IpsTestRunnerViewPart_Action_StopTest); 
            setToolTipText(Messages.IpsTestRunnerViewPart_Action_StopTest_ToolTip); 
            setDisabledImageDescriptor(IpsPlugin.getDefault().getImageDescriptor("dlcl16/stop.gif")); //$NON-NLS-1$
            setHoverImageDescriptor(IpsPlugin.getDefault().getImageDescriptor("elcl16/stop.gif")); //$NON-NLS-1$
            setImageDescriptor(IpsPlugin.getDefault().getImageDescriptor("elcl16/stop.gif")); //$NON-NLS-1$
            setEnabled(false);
        }
        
        public void run(){
            try {
                IpsPlugin.getDefault().getIpsTestRunner().terminate();
            } catch (CoreException e) {
                IpsPlugin.logAndShowErrorDialog(e);
            }
        }
    }
    
	/*
	 * Action class to rerun a test.
	 */
	private class RerunLastAction extends Action {
		public RerunLastAction() {
			setText(Messages.IpsTestRunnerViewPart_Action_RerunLastTest_Text); 
			setToolTipText(Messages.IpsTestRunnerViewPart_Action_RerunLastTest_ToolTip); 
			setDisabledImageDescriptor(IpsPlugin.getDefault().getImageDescriptor("dlcl16/relaunch.gif")); //$NON-NLS-1$
			setHoverImageDescriptor(IpsPlugin.getDefault().getImageDescriptor("elcl16/relaunch.gif")); //$NON-NLS-1$
			setImageDescriptor(IpsPlugin.getDefault().getImageDescriptor("elcl16/relaunch.gif")); //$NON-NLS-1$
			setEnabled(false);
		}
		
		public void run(){
			rerunTestRun();
		}
	}
    
    /*
     * Action class to select the next error or failure
     */
    private class ShowNextErrorAction extends Action {
        public ShowNextErrorAction() {
            setText(Messages.IpsTestRunnerViewPart_Action_NextFailure); 
            setToolTipText(Messages.IpsTestRunnerViewPart_Action_NextFailureToolTip); 
            setDisabledImageDescriptor(IpsPlugin.getDefault().getImageDescriptor("dlcl16/select_next.gif")); //$NON-NLS-1$
            setHoverImageDescriptor(IpsPlugin.getDefault().getImageDescriptor("elcl16/select_next.gif")); //$NON-NLS-1$
            setImageDescriptor(IpsPlugin.getDefault().getImageDescriptor("elcl16/select_next.gif")); //$NON-NLS-1$
            setEnabled(false);
        }
        public void run(){
            fTestRunPane.selectNextFailureOrError();
        }
    }    
	
    /*
     * Action class to select the previous error or failure
     */
    private class ShowPreviousErrorAction extends Action {
        public ShowPreviousErrorAction() {
            setText(Messages.IpsTestRunnerViewPart_Action_PrevFailure); 
            setToolTipText(Messages.IpsTestRunnerViewPart_Action_PrevFailureToolTip); 
            setDisabledImageDescriptor(IpsPlugin.getDefault().getImageDescriptor("dlcl16/select_prev.gif")); //$NON-NLS-1$
            setHoverImageDescriptor(IpsPlugin.getDefault().getImageDescriptor("elcl16/select_prev.gif")); //$NON-NLS-1$
            setImageDescriptor(IpsPlugin.getDefault().getImageDescriptor("elcl16/select_prev.gif")); //$NON-NLS-1$
            setEnabled(false);
        }
        public void run(){
            fTestRunPane.selectPreviousFailureOrError();
        }
    }    
    
    /*
     * Action to toggle the orientation of the view
     */
    private class ToggleOrientationAction extends Action {
        private final int fActionOrientation;
        
        public ToggleOrientationAction(IpsTestRunnerViewPart v, int orientation) {
            super("", AS_RADIO_BUTTON); //$NON-NLS-1$
            if (orientation == IpsTestRunnerViewPart.VIEW_ORIENTATION_HORIZONTAL) {
                setText(Messages.IpsTestRunnerViewPart_Menu_HorizontalOrientation); 
                setImageDescriptor(IpsPlugin.getDefault().getImageDescriptor("elcl16/th_horizontal.gif")); //$NON-NLS-1$                
            } else if (orientation == IpsTestRunnerViewPart.VIEW_ORIENTATION_VERTICAL) {
                setText(Messages.IpsTestRunnerViewPart_Menu_VerticalOrientation); 
                setImageDescriptor(IpsPlugin.getDefault().getImageDescriptor("elcl16/th_vertical.gif")); //$NON-NLS-1$              
            } else if (orientation == IpsTestRunnerViewPart.VIEW_ORIENTATION_AUTOMATIC) {
                setText(Messages.IpsTestRunnerViewPart_Menu_AutomaticOrientation);  
                setImageDescriptor(IpsPlugin.getDefault().getImageDescriptor("elcl16/th_automatic.gif")); //$NON-NLS-1$             
            }
            fActionOrientation = orientation;
        }
        
        public int getOrientation() {
            return fActionOrientation;
        }
        
        public void run() {
            if (isChecked()) {
                fOrientation = fActionOrientation;
                computeOrientation();
            }
        }       
    } 
    
	/*
	 * Runs the last runned test.
	 */
	private void rerunTestRun()  {
        IpsPlugin.getDefault().getIpsTestRunner().startTestRunnerJob(repositoryPackage, testPackage);
	}
	
	/*
	 * UIJob to refresh the counter in th user interface.
	 */
	class UpdateUIJob extends UIJob {
		private boolean fRunning= true; 
		
		public UpdateUIJob(String name) {
			super(name);
			setSystem(true);
		}
		public IStatus runInUIThread(IProgressMonitor monitor) {
			if (!isDisposed()) { 
				doShowStatus();
				refreshCounters();
			}
			schedule(REFRESH_INTERVAL);
			return Status.OK_STATUS;
		}
		
		public void stop() {
			fRunning= false;
		}
		public boolean shouldSchedule() {
			return fRunning;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	 public void setFocus() {
	 }
	 
	/**
	 * {@inheritDoc}
	 */
	public void createPartControl(Composite parent) {
		fParent = parent;
        addResizeListener(parent);
        
		GridLayout gridLayout= new GridLayout(); 
		gridLayout.marginWidth= 0;
		gridLayout.marginHeight= 0;
		parent.setLayout(gridLayout);

		configureToolBar();
		
		fCounterComposite = createProgressCountPanel(parent);
		fCounterComposite.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
		
		SashForm sashForm = createSashForm(parent);
		sashForm.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		registerAsIpsTestRunListener();
        
        if (fMemento != null) {
            restoreLayoutState(fMemento);
        }
        fMemento= null;        
	}

    private void restoreLayoutState(IMemento memento) {
        Integer ratio= memento.getInteger(TAG_RATIO);
        if (ratio != null) 
            fSashForm.setWeights(new int[] { ratio.intValue(), 1000 - ratio.intValue()} );
        Integer orientation= memento.getInteger(TAG_ORIENTATION);
        if (orientation != null)
            fOrientation= orientation.intValue();
        computeOrientation();
    }
    
	/**
	 * Returns the failure pane.
	 */
	public FailurePane getFailurePane() {
		return fFailurePane;
	}

	/**
	 * Returns the test run pane.
	 */
	public TestRunPane getTestRunPane() {
		return fTestRunPane;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);
        fMemento= memento;        
	}
	
	private Composite createProgressCountPanel(Composite parent) {
		Composite composite= new Composite(parent, SWT.NONE);
		GridLayout layout= new GridLayout();
		composite.setLayout(layout);
		layout.numColumns = 1;
		
		fCounterPanel = new IpsTestCounterPanel(composite);
		fCounterPanel.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
		
		fProgressBar = new IpsTestProgressBar(composite);
		fProgressBar.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
		return composite;
	}
	
	private SashForm createSashForm(Composite parent) {
		fSashForm= new SashForm(parent, SWT.VERTICAL);
		
		ViewForm top= new ViewForm(fSashForm, SWT.NONE);		
		CLabel label= new CLabel(top, SWT.NONE);
		label.setText(Messages.IpsTestRunnerViewPart_TestRunPane_Text); 
		label.setImage(IpsPlugin.getDefault().getImage("TestCaseRun.gif"));		 //$NON-NLS-1$
		top.setTopLeft(label);
		fTestRunPane = new TestRunPane(top, this);
		top.setContent(fTestRunPane.getComposite()); 
		
		ViewForm bottom= new ViewForm(fSashForm, SWT.NONE);
		label= new CLabel(bottom, SWT.NONE);
		label.setText(Messages.IpsTestRunnerViewPart_TestFailurePane_Text); 
		label.setImage(IpsPlugin.getDefault().getImage("failures.gif")); //$NON-NLS-1$
		bottom.setTopLeft(label);

        ToolBar failureToolBar= new ToolBar(bottom, SWT.FLAT | SWT.WRAP);
        bottom.setTopCenter(failureToolBar);
		fFailurePane = new FailurePane(bottom, failureToolBar);
		bottom.setContent(fFailurePane.getComposite()); 
		
		fSashForm.setWeights(new int[]{50, 50});
		return fSashForm;
	}

	private void configureToolBar() {
		IActionBars actionBars= getViewSite().getActionBars();
		IToolBarManager toolBar= actionBars.getToolBarManager();
		IMenuManager viewMenu = actionBars.getMenuManager();
		
        fStopTestRunAction= new StopTestRunAction();
		fRerunLastTestAction= new RerunLastAction();
		fNextAction= new ShowNextErrorAction();
		fPreviousAction= new ShowPreviousErrorAction();

        fToggleOrientationActions =
			new ToggleOrientationAction[] {
				new ToggleOrientationAction(this, VIEW_ORIENTATION_VERTICAL),
				new ToggleOrientationAction(this, VIEW_ORIENTATION_HORIZONTAL),
				new ToggleOrientationAction(this, VIEW_ORIENTATION_AUTOMATIC)};
        
        fToggleOrientationActions[2].setChecked(true);
        
        toolBar.add(fNextAction);
        toolBar.add(fPreviousAction);
        toolBar.add(new Separator());
        toolBar.add(fStopTestRunAction);
		toolBar.add(fRerunLastTestAction);
		
		for (int i = 0; i < fToggleOrientationActions.length; ++i)
			viewMenu.add(fToggleOrientationActions[i]);		
        
        
		actionBars.updateActionBars();
		
        setRunToolBarButtonsStatus(false);
        setNextPrevToolBarButtonsStatus(false);
	}
	
    private void addResizeListener(Composite parent) {
        parent.addControlListener(new ControlListener() {
            public void controlMoved(ControlEvent e) {
            }
            public void controlResized(ControlEvent e) {
                computeOrientation();
            }
        });
    }
    
	private void computeOrientation() {
		if (fOrientation != VIEW_ORIENTATION_AUTOMATIC) {
			fCurrentOrientation = fOrientation;
			setOrientation(fCurrentOrientation);
		}
		else {
			Point size= fParent.getSize();
			if (size.x != 0 && size.y != 0) {
				if (size.x > size.y) 
					setOrientation(VIEW_ORIENTATION_HORIZONTAL);
				else 
					setOrientation(VIEW_ORIENTATION_VERTICAL);
			}
		}
	}
	
	private void setOrientation(int orientation) {
		if ((fSashForm == null) || fSashForm.isDisposed())
			return;
		boolean horizontal = orientation == VIEW_ORIENTATION_HORIZONTAL;
		fSashForm.setOrientation(horizontal ? SWT.HORIZONTAL : SWT.VERTICAL);
		for (int i = 0; i < fToggleOrientationActions.length; ++i)
			fToggleOrientationActions[i].setChecked(fOrientation == fToggleOrientationActions[i].getOrientation());
		fCurrentOrientation = orientation;
		GridLayout layout= (GridLayout) fCounterComposite.getLayout();
		setCounterColumns(layout); 
		fParent.layout();
	}
	
	private void setCounterColumns(GridLayout layout) {
		if (fCurrentOrientation == VIEW_ORIENTATION_HORIZONTAL)
			layout.numColumns= 2; 
		else
			layout.numColumns= 1;
	}

	public synchronized void dispose(){
		fIsDisposed = true;
		IIpsTestRunner testRunner = IpsPlugin.getDefault().getIpsTestRunner();
		testRunner.removeIpsTestRunListener(this);
	}
	
	private boolean isDisposed() {
		return fIsDisposed || fCounterPanel.isDisposed();
	}
	
	private void doShowStatus() {
		setContentDescription(fStatus);
	}

	public void setInfoMessage(final String message) {
		fStatus= message;
	}
	
	private class TableEntryQueueDrainer implements Runnable {
		public void run() {
			while (true) {
				TestCaseEntry testCaseEntry;
				synchronized (fTableEntryQueue) {
					if (fTableEntryQueue.isEmpty() || isDisposed()) {
						fQueueDrainRequestOutstanding = false;
						return;
					}
					testCaseEntry = (TestCaseEntry) fTableEntryQueue.remove(0);
				}
				fTestRunPane.newTableEntry(testCaseEntry.getTestId(), testCaseEntry.getQualifiedName(), testCaseEntry.fullPath);
                fTestRunPane.checkMissingEntries();
			}
		}
	}
	
	// inner class to represent an ips test case
	private class TestCaseEntry {
		private String qualifiedName;
		private String fullPath;
        private String testId;
		public TestCaseEntry(String testId, String qualifiedName, String fullPath){
            this.testId = testId;
			this.qualifiedName = qualifiedName;
			this.fullPath = fullPath;
		}
		public String getFullPath() {
			return fullPath;
		}
		public String getQualifiedName() {
			return qualifiedName;
		}
        public String getTestId() {
            return testId;
        }
	}
	
	public Display getDisplay() {
		return getViewSite().getShell().getDisplay();
	}
	 
	private void stopUpdateJobs() {
		if (fUpdateJob != null) {
			fUpdateJob.stop();
			fUpdateJob= null;
		}
	}
	
	private void reset(final int testCount) {
		postSyncRunnable(new Runnable() {
			public void run() {
				if (isDisposed()) 
					return;
				fCounterPanel.reset();
				fProgressBar.reset();
				start(testCount);
			}
		});
        
		fExecutedTests= 0;
		fFailureCount= 0;
		fErrorCount= 0;
        fStatus= ""; //$NON-NLS-1$
		resetTestId();
		aboutToStart();
	}
	
	private void resetProgressBar(final int total) {
		fProgressBar.reset();
		fProgressBar.setMaximum(total);
	}

	private void handleEndTest() {
		fProgressBar.step(fFailureCount + fErrorCount);
	}
	
	private void refreshCounters() {
		fCounterPanel.setErrorValue(fErrorCount);
		fCounterPanel.setFailureValue(fFailureCount);
		fCounterPanel.setRunValue(fExecutedTests);
		fProgressBar.refresh(fErrorCount+fFailureCount > 0);
	}	
	
	private void start(final int total) {
		resetProgressBar(total);
		fCounterPanel.setTotal(total);
		fCounterPanel.setRunValue(0);	
	}
	
	/*
	 * Register self as ips test run listener. 
	 */
	private void registerAsIpsTestRunListener() {
		IIpsTestRunner testRunner = IpsPlugin.getDefault().getIpsTestRunner();
		testRunner.addIpsTestRunListener(this);
	}

	//
	// Inform ui panes about test runs
	//
	
	private void aboutToStart() {
		postSyncRunnable(new Runnable() {
			public void run() {
				if (!isDisposed())
					fTestRunPane.aboutToStart();
					fFailurePane.aboutToStart();
				}
		});
	}
	
	private void postSyncRunnable(Runnable r) {
		if (!isDisposed())
			getDisplay().syncExec(r);
	}

	private void postStartTest(final String testId, final String qualifiedTestName) {
		postSyncRunnable(new Runnable() {
			public void run() {
				if(isDisposed()) 
					return;
				fTestRunPane.startTest(testId, qualifiedTestName);
			}
		});
	}
	
	private void postErrorInTest(final String testId, final String qualifiedTestName, final String[] errorDetails) {
		postSyncRunnable(new Runnable() {
			public void run() {
				if(isDisposed()) 
					return;
				fTestRunPane.errorInTest(testId, qualifiedTestName, errorDetails);
			}
		});
	}
	
	private void postEndTest(final String testId, final String qualifiedTestName) {
		postSyncRunnable(new Runnable() {
			public void run() {
				if(isDisposed()) 
					return;
				handleEndTest();
				fTestRunPane.endTest(testId, qualifiedTestName);
			}
		});	
	}

	private void postFailureTest(final String testId, final String[] failureDetails) {
		postSyncRunnable(new Runnable() {
			public void run() {
				if(isDisposed()) 
					return;
				fTestRunPane.failureTest(testId, failureDetailsToString(failureDetails));
                fTestRunPane.selectFirstFailureOrError();
			}
		});
	}

	private void postEndTestRun() {
		postSyncRunnable(new Runnable() {
			public void run() {
				if(isDisposed()) 
					return;
				fTestRunPane.selectFirstFailureOrError();
			}
		});
	}
	
	/*
	 * Converts the given failure details to one failure detail row.
	 */
	private String failureDetailsToString(String[] failureDetails){
		String failureFormat= Messages.IpsTestRunnerViewPart_FailureFormat_FailureIn;
		String failureActual = Messages.IpsTestRunnerViewPart_FailureFormat_Actual;
		String failureExpected = Messages.IpsTestRunnerViewPart_FailureFormat_Expected;
		String failureFormatAttribute= Messages.IpsTestRunnerViewPart_FailureFormat_Attribute;
		String failureFormatObject= Messages.IpsTestRunnerViewPart_FailureFormat_Object;
        String failureFormatMessage = Messages.IpsTestRunnerViewPart_FailureFormat_Message;
        
		if (failureDetails.length>3)
			failureFormat= failureFormat + (failureExpected); //$NON-NLS-1$
		if (failureDetails.length>4)
			failureFormat= failureFormat + (failureActual); //$NON-NLS-1$
		if (failureDetails.length>1)
			failureFormat= failureFormat + (!"<null>".equals(failureDetails[1])?failureFormatObject:""); //$NON-NLS-1$ //$NON-NLS-2$
		if (failureDetails.length>2)
			failureFormat= failureFormat + (!"<null>".equals(failureDetails[2])?failureFormatAttribute:""); //$NON-NLS-1$ //$NON-NLS-2$
		if (failureDetails.length>5)
		    failureFormat= failureFormat + (!"<null>".equals(failureDetails[5])?failureFormatMessage:""); //$NON-NLS-1$ //$NON-NLS-2$
		return MessageFormat.format(failureFormat, failureDetails); 
	}
	
	//
	// Helper functions to generate unique test id's to identify the test in the test run ui control.
	//
	
    private void resetTestId(){
		testRuns++;
		testId = 0;
	}
    
	private String nextTestId(){
		return "" + testRuns + "." + ++testId; //$NON-NLS-1$ //$NON-NLS-2$
	}
    
	private String getTestId(String qualifiedTestName){
        return (String) testId2TestQualifiedNameMap.get(qualifiedTestName);
	}
	
	//
	// Listener methods
	//

	/**
	 * {@inheritDoc}
	 */
	public void testFailureOccured(String qualifiedTestName, String[] failureDetails) {
	    isFailure = true;
	    postFailureTest(getTestId(qualifiedTestName), failureDetails);
        setNextPrevToolBarButtonsStatus(true);
	}

	/**
	 * {@inheritDoc}
	 */
	public void testFinished(String qualifiedTestName) {
		fExecutedTests++;
		if (isFailure)
			fFailureCount++;
		postEndTest(getTestId(qualifiedTestName), qualifiedTestName);
	}
    
	/**
	 * {@inheritDoc}
	 */
	public void testStarted(String qualifiedTestName) {
        setInfoMessage(StringUtil.unqualifiedName(qualifiedTestName));
        isFailure = false;
        setRunToolBarButtonsStatus(true);
		postStartTest(getTestId(qualifiedTestName), qualifiedTestName);
	}
    
	/**
	 * {@inheritDoc}
	 */
	public void testTableEntry(final String qualifiedName, final String fullPath) {
		// get a new or a cached test id 
        String testId = getTestId(qualifiedName);
        if (testId == null){
            testId = nextTestId();
            testId2TestQualifiedNameMap.put(qualifiedName, testId);
        }
        
        synchronized(fTableEntryQueue) {
			fTableEntryQueue.add(new TestCaseEntry(testId, qualifiedName, fullPath));
			if (!fQueueDrainRequestOutstanding) {
				fQueueDrainRequestOutstanding = true;
				if (!isDisposed())
					getDisplay().asyncExec(new TableEntryQueueDrainer());
			}
		}
	}

    /**
     * {@inheritDoc}
     */
    public void testTableEntries(final String[] qualifiedNames, final String[] fullPaths) {
        // get a new or a cached test id 

        
        synchronized(fTableEntryQueue) {
            for (int i = 0; i < fullPaths.length; i++) {
                String testId = getTestId(qualifiedNames[i]);
                if (testId == null){
                    testId = nextTestId();
                    testId2TestQualifiedNameMap.put(qualifiedNames[i], testId);
                }
                fTableEntryQueue.add(new TestCaseEntry(testId, qualifiedNames[i], fullPaths[i]));
            }
            if (!fQueueDrainRequestOutstanding) {
                fQueueDrainRequestOutstanding = true;
                if (!isDisposed())
                    getDisplay().asyncExec(new TableEntryQueueDrainer());
            }
        }
    }
    
	/**
	 * {@inheritDoc}
	 */
	public void testRunStarted(int testCount, String repositoryPackage, String testPackage){
		this.repositoryPackage = repositoryPackage;
		this.testPackage = testPackage;
		
        testId2TestQualifiedNameMap.clear();
        
		reset(testCount);
		fExecutedTests++;
        setRunToolBarButtonsStatus(true);
        setNextPrevToolBarButtonsStatus(false);
        
		stopUpdateJobs();
		fUpdateJob = new UpdateUIJob(Messages.IpsTestRunnerViewPart_Job_UpdateUiTitle); 
		fUpdateJob.schedule(0);
		
		// store the project which contains the tests, will be used to open the test in the editor
		fTestProject = IpsPlugin.getDefault().getIpsTestRunner().getJavaProject();
	}

	/**
	 * {@inheritDoc}
	 */
	public void testRunEnded(String elapsedTime){
		fExecutedTests--;
		stopUpdateJobs();
		postEndTestRun();
        long elapsedTimeLong = 0;
        try{
            elapsedTimeLong = Long.parseLong(elapsedTime);
        }catch(NumberFormatException e){
            // ignore exception of wrong number format
        }
        fStatus = NLS.bind(Messages.IpsTestRunnerViewPart_Message_TestFinishedAfterNSeconds, elapsedTimeAsString(elapsedTimeLong));
        
        fStopTestRunAction.setEnabled(false);
        if (fErrorCount+fFailureCount > 0){
            setNextPrevToolBarButtonsStatus(true);
        }
	}
    
    /*
     * Returns the string representation in second of the given time in milliseconds
     */
    private String elapsedTimeAsString(long elapsedTime) {
        return NumberFormat.getInstance().format((double)elapsedTime/1000);
    }    

	/**
	 * {@inheritDoc}
	 */
    public void testErrorOccured(String qualifiedTestName, String[] errorDetails){
    	fErrorCount ++;
    	postErrorInTest(getTestId(qualifiedTestName), qualifiedTestName, errorDetails);
    }
    
	/**
	 * Informs that the selection of a test result changed.
	 * 
	 * @param testCaseDetails contains the details of the selcted test result.
	 */
	public void selectionOfTestCaseChanged(String[] testCaseFailures) {
		fFailurePane.showFailureDetails(testCaseFailures);
	}

	public IJavaProject getLaunchedProject() {
		return fTestProject;
	}
    
    /*
     * Enables or disables the run and stop toolbar actions
     */
    private void setRunToolBarButtonsStatus(boolean enabled){
        fRerunLastTestAction.setEnabled(enabled);
        fStopTestRunAction.setEnabled(enabled);
    }
    
    /*
     * Enables or disables the next and previous toolbar actions
     */
    private void setNextPrevToolBarButtonsStatus(boolean enabled){
        fNextAction.setEnabled(enabled);
        fPreviousAction.setEnabled(enabled);
    }
}
