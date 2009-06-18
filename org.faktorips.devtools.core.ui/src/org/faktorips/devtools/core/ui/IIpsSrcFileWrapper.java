/*******************************************************************************
 * Copyright (c) 2005-2009 Faktor Zehn AG und andere.
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
package org.faktorips.devtools.core.ui;

import org.faktorips.devtools.core.model.ipsobject.IIpsSrcFile;
import org.faktorips.devtools.core.ui.actions.IpsAction;
import org.faktorips.devtools.core.ui.views.IpsElementDragListener;

/**
 * This interface is used to wrap a <code>IpsSrcFile</code> in another object. It provides methods
 * for getting and setting the internal <code>IpsSrcFile</code>. You can use such a wrapper object
 * to put some additional information for viewing an source file object in an TreeViewer or TableViewer.
 * The components {@link IpsElementDragListener} and {@link IpsAction} are able to handle this instances
 * of this wrapper as <code>IpsSrcFile</code>-Objects by calling the getIpsSrcFile method.
 * 
 * @author dirmeier
 *
 */
public interface IIpsSrcFileWrapper {

	/**
	 * To get the internal <code>IpsSrcFile</code> of this wrapper
	 * @return the internal <code>IpsSrcFile</code>
	 */
	public IIpsSrcFile getIpsSrcFile();
	
	/**
	 * To set the internal <code>IpsSrcFile</code>
	 * @param ipsSrcFile
	 */
	public void setIpsSrcFile(IIpsSrcFile ipsSrcFile);
	
}
