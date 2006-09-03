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

package org.faktorips.devtools.core.ui.editors.testcase;

import org.eclipse.core.runtime.CoreException;
import org.faktorips.devtools.core.model.testcase.ITestCase;
import org.faktorips.devtools.core.model.testcase.ITestPolicyCmpt;
import org.faktorips.devtools.core.model.testcase.ITestPolicyCmptRelation;

/**
 * Class to evalulate and navigate a hierarchy path for test case or test case types.
 * 
 * @author Joerg Ortmann
 */
public class TestCaseHierarchyPath{
	// Seperator between each hierarchy element
	private static final String separator = "//"; //$NON-NLS-1$
	    
	// Contains the complete hierarchy path
	private String hierarchyPath = ""; //$NON-NLS-1$
	
    /**
     * Removes the folder information from the beginning.
     */
	public static String unqualifiedName(String hierarchyPath){
        int index = hierarchyPath.lastIndexOf(separator); //$NON-NLS-1$
        if (index == -1) {
            return hierarchyPath;
        }
        return hierarchyPath.substring(index + separator.length());
	}
	
    /**
     * Evaluate the test policy component type parameter path of the given test policy component.
     * An offset concatenated to the name of the test policy component type parameter indicates the
     * unique path if there are more instances with the same name.
     */
    public static String evalTestPolicyCmptParamPath(ITestPolicyCmpt testPolicyCmpt) throws CoreException {
        String pathWithOffset = ""; //$NON-NLS-1$
        
        while (!testPolicyCmpt.isRoot()){
            int offset = 0;
            ITestPolicyCmpt parent = testPolicyCmpt.getParentPolicyCmpt();
            ITestPolicyCmptRelation[] relations = parent.getTestPolicyCmptRelations();
            for (int i = 0; i < relations.length; i++) {
                if (relations[i].findTarget().equals(testPolicyCmpt))
                    break;
                // check for same parameter and increment offset if necessary
                if (relations[i].getTestPolicyCmptTypeParameter().equals(testPolicyCmpt.getTestPolicyCmptTypeParameter()))
                    offset ++;
            }
            pathWithOffset = testPolicyCmpt.getTestPolicyCmptTypeParameter() + offset + (pathWithOffset.length()>0? "." + pathWithOffset: ""); //$NON-NLS-1$ //$NON-NLS-2$
            testPolicyCmpt = parent;
        }

        // get the offset of the test policy cmpt
        //   by searching test policy cmpt with the same test policy cmpt type param
        ITestCase testCase = testPolicyCmpt.getTestCase();
        ITestPolicyCmpt[] tpcs = testCase.getTestPolicyCmpts();
        int offset = 0;
        for (int i = 0; i < tpcs.length; i++) {
            if (testPolicyCmpt.equals(tpcs[i]))
                break;
            // check for same parameter and increment offset if necessary
            if (testPolicyCmpt.getTestPolicyCmptTypeParameter().equals(tpcs[i]))
                offset ++;
        }
        pathWithOffset = testPolicyCmpt.getTestPolicyCmptTypeParameter() + offset + "." + pathWithOffset; //$NON-NLS-1$
        return pathWithOffset;
    }
    
	public TestCaseHierarchyPath(String hierarchyPath){
        this.hierarchyPath = hierarchyPath;
	}

    /**
     * Creates a test case hierarchy path for a given test policy component.
     */
    public TestCaseHierarchyPath(ITestPolicyCmpt currTestPolicyCmpt){
        this.hierarchyPath = evalHierarchyPathForTestCase(currTestPolicyCmpt, ""); //$NON-NLS-1$
    }
    
	/**
	 * Creates a test case or test case type hierarchy path for a given test policy component.
	 * 
	 * @param currTestPolicyCmpt The test policy compcomponentonengt for which the path will be created.
	 * @param evalForTestCase <code>true</code> if the hierarchy path will be evaluated for a test case
	 *                        <code>false</code> if the hierarchy path will be evaluated for a test case type.
	 */
	public TestCaseHierarchyPath(ITestPolicyCmpt currTestPolicyCmpt, boolean evalForTestCase){
		if (evalForTestCase){
			this.hierarchyPath = evalHierarchyPathForTestCase(currTestPolicyCmpt, ""); //$NON-NLS-1$
		}else{
			this.hierarchyPath = evalHierarchyPathForTestCaseType(currTestPolicyCmpt, ""); //$NON-NLS-1$
		}
	}
	
	/**
	 * Creates a test case hierarchy path for a given test policy component relation.
	 * 
	 * @param currTestPolicyCmpt The test policy component relation for which the path will be created.
	 * @param evalForTestCase <code>true</code> if the hierarchy path will be evaluated for a test case
	 *                        <code>false</code> if the hierarchy path will be evaluated for a test case type.
	 */	
	public TestCaseHierarchyPath(ITestPolicyCmptRelation relation, boolean evalforTestCase){
		String relationPath = relation.getTestPolicyCmptTypeParameter();
		if (evalforTestCase){	
			this.hierarchyPath = evalHierarchyPathForTestCase((ITestPolicyCmpt) relation.getParent(), relationPath);
		}else{
			this.hierarchyPath = evalHierarchyPathForTestCaseType((ITestPolicyCmpt) relation.getParent(), relationPath);
		}
	}
	
	/**
	 * Returns the hierarchy path.
	 */
	public String getHierarchyPath() {
		return hierarchyPath;
	}

	/**
	 * Returns <code>true</code> if there is a next path element.
	 */
	public boolean hasNext(){
		return hierarchyPath.length() > 0;
	}
			
	/**
	 * Returns the current path element and sets the navigation pointer one element forward.
	 */
	public String next(){
		String next = ""; //$NON-NLS-1$
		
		if (hierarchyPath.indexOf(separator)>=0){
			next = hierarchyPath.substring(0, hierarchyPath.indexOf(separator));
			hierarchyPath = hierarchyPath.substring(hierarchyPath.indexOf(separator) + separator.length());
			return next;
		}else{
			next = hierarchyPath;
			hierarchyPath = ""; //$NON-NLS-1$
		}
		return next;
	}
	
	/**
	 * Returns the string representation of this object.
	 */
	public String toString(){
		return hierarchyPath; //$NON-NLS-1$
	}

	/**
	 * Returns the count of path elements.
	 */
	public int count() {
		int count = 0;
		TestCaseHierarchyPath tempHierarchyPath = new TestCaseHierarchyPath(hierarchyPath);
		if (!(hierarchyPath.length() > 0)){
			return 0;
		}
		while(tempHierarchyPath.hasNext()){
			tempHierarchyPath.next();
			count ++;
		}
		return count;
	}
	
    /**
     * Returns the folder name for a given hierarchy path.
     */	
	public static String getFolderName(String hierarchyPath){
        int index = hierarchyPath.lastIndexOf(separator); //$NON-NLS-1$
        if (index == -1){
            return ""; //$NON-NLS-1$
        }
        return hierarchyPath.substring(0, index);
	}
	
	private String evalHierarchyPathForTestCaseType(ITestPolicyCmpt currTestPolicyCmpt, String hierarchyPath){
		while (!currTestPolicyCmpt.isRoot()){
			if (hierarchyPath.length()>0)
				hierarchyPath = separator + hierarchyPath ;
			ITestPolicyCmptRelation testPcTypeRelation = (ITestPolicyCmptRelation) currTestPolicyCmpt.getParent();
			hierarchyPath = testPcTypeRelation.getTestPolicyCmptTypeParameter() + hierarchyPath;
			currTestPolicyCmpt = (ITestPolicyCmpt) testPcTypeRelation.getParent();
		}
		hierarchyPath = currTestPolicyCmpt.getTestPolicyCmptTypeParameter() + (hierarchyPath.length() > 0 ? separator + hierarchyPath : ""); //$NON-NLS-1$
		return hierarchyPath;
	}
	
	private String evalHierarchyPathForTestCase(ITestPolicyCmpt currTestPolicyCmpt, String hierarchyPath){
		while (!currTestPolicyCmpt.isRoot()){
			if (hierarchyPath.length()>0)
				hierarchyPath = separator + hierarchyPath ;
			hierarchyPath = separator + currTestPolicyCmpt.getName() + hierarchyPath;
			ITestPolicyCmptRelation testPcTypeRelation = (ITestPolicyCmptRelation) currTestPolicyCmpt.getParent();
			hierarchyPath = testPcTypeRelation.getTestPolicyCmptTypeParameter() + hierarchyPath;
			currTestPolicyCmpt = (ITestPolicyCmpt) testPcTypeRelation.getParent();
		}
		hierarchyPath = currTestPolicyCmpt.getName() + (hierarchyPath.length() > 0 ? separator + hierarchyPath : ""); //$NON-NLS-1$
		return hierarchyPath;
	}
}
