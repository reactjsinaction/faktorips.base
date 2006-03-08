/*******************************************************************************
 * Copyright (c) 2005,2006 Faktor Zehn GmbH und andere.
 *
 * Alle Rechte vorbehalten.
 *
 * Dieses Programm und alle mitgelieferten Sachen (Dokumentationen, Beispiele,
 * Konfigurationen, etc.) duerfen nur unter den Bedingungen der 
 * Faktor-Zehn-Community Lizenzvereinbarung - Version 0.1 (vor Gruendung Community) 
 * genutzt werden, die Bestandteil der Auslieferung ist und auch unter
 *   http://www.faktorips.org/legal/cl-v01.html
 * eingesehen werden kann.
 *
 * Mitwirkende:
 *   Faktor Zehn GmbH - initial API and implementation - http://www.faktorzehn.de
 *
 *******************************************************************************/

package org.faktorips.devtools.core.ui.editors.pctype;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.faktorips.devtools.core.IpsPlugin;
import org.faktorips.util.message.Message;
import org.faktorips.util.message.MessageList;


/**
 *
 */
public class ValidationDecorator extends Composite {
    
	protected final static Point IMAGE_SIZE = new Point(8, 8);

    private Label imageLabel;
    private Control control;
    private String property;
    private Object object;

    /**
     * @param parent
     * @param style
     */
    public ValidationDecorator(Composite parent) {
        super(parent, SWT.NONE);
        GridLayout layout = new GridLayout(2, false);
        layout.horizontalSpacing = 2;
        layout.verticalSpacing = 0;
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        setLayout(layout);
        Label imageLabel = new Label(this, SWT.CENTER);
		imageLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_BEGINNING));
    }
    
	/**
	 * @see org.eclipse.swt.widgets.Control#setBackground(org.eclipse.swt.graphics.Color)
	 */
	public void setBackground(Color color) {
		super.setBackground(color);
		imageLabel.setBackground(color);
		control.setBackground(color);
	}
	
	public void setMessageList(MessageList list) {
	    String imageName;
	    switch (list.getSeverity()) {
	    	case Message.ERROR: {
	    	    imageName = "size8/Error.gif"; //$NON-NLS-1$
	    	    break;
	    	}
	    	case Message.WARNING: {
	    	    imageName = "size8/Warning.gif"; //$NON-NLS-1$
	    	    break;
	    	}
	    	case Message.INFO: {
	    	    imageName = "info.gif"; //$NON-NLS-1$
	    	    break;
	    	}
	    	default:
	    	    imageName = "empty.gif"; //$NON-NLS-1$
	    }
        imageLabel.setImage(IpsPlugin.getDefault().getImage(imageName));
	}
	
}
