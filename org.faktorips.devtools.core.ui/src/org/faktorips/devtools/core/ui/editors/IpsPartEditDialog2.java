/*******************************************************************************
 * Copyright (c) 2005-2011 Faktor Zehn AG und andere.
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

package org.faktorips.devtools.core.ui.editors;

import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.faktorips.devtools.core.IpsPlugin;
import org.faktorips.devtools.core.model.ContentChangeEvent;
import org.faktorips.devtools.core.model.ContentsChangeListener;
import org.faktorips.devtools.core.model.ipsobject.IDescribedElement;
import org.faktorips.devtools.core.model.ipsobject.IIpsObjectGeneration;
import org.faktorips.devtools.core.model.ipsobject.IIpsObjectPart;
import org.faktorips.devtools.core.model.ipsobject.ILabeledElement;
import org.faktorips.devtools.core.ui.binding.BindingContext;
import org.faktorips.util.memento.Memento;
import org.faktorips.util.message.Message;
import org.faktorips.util.message.MessageList;

/**
 * Base class for dialogs that allows to edit an ips object part. In contrast to the original
 * <code>IpsPartEditDialog</code> this version uses the new databinding.
 * 
 * @since 2.0
 * 
 * @see BindingContext
 * 
 * @author Jan Ortmann
 */
public abstract class IpsPartEditDialog2 extends EditDialog implements ContentsChangeListener {

    /**
     * The binding context to use to bind model object properties to ui controls.
     * 
     * @deprecated use {@link IpsPartEditDialog2#getBindingContext()} instead
     */
    @Deprecated
    protected BindingContext bindingContext = new BindingContext();

    /**
     * Returns the binding context to use to bind model object properties to UI controls.
     * 
     * @return the binding context to use to bind model object properties to UI controls
     */
    protected BindingContext getBindingContext() {
        return bindingContext;
    }

    private IIpsObjectPart part;
    private Memento oldState;
    private boolean dirty = false;

    private LabelEditComposite labelEditComposite;

    /**
     * Creates a new <code>IpsPartEditDialog2</code>.
     * 
     * @param part The ips object part to edit.
     * @param windowTitle The window title of the dialog.
     */
    public IpsPartEditDialog2(IIpsObjectPart part, Shell parentShell, String windowTitle) {
        this(part, parentShell, windowTitle, false);
    }

    /**
     * Creates a new <code>IpsPartEditDialog2</code>.
     * 
     * @param part The ips object part to edit.
     * @param windowTitle The window title of the dialog.
     */
    public IpsPartEditDialog2(IIpsObjectPart part, Shell parentShell, String windowTitle, boolean useTabFolder) {
        super(parentShell, windowTitle, useTabFolder);

        this.part = part;
        oldState = part.getIpsObject().newMemento();
        dirty = part.getIpsObject().getIpsSrcFile().isDirty();
        IpsPlugin.getDefault().getIpsModel().addChangeListener(this);
    }

    // Overwritten to be sure to get the cancel-button as soon as possible ...
    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        super.createButtonsForButtonBar(parent);
        super.getButton(Window.CANCEL).addSelectionListener(new SelectionListener() {

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }

            @Override
            public void widgetSelected(SelectionEvent e) {
                handleAbortion();
            }
        });
    }

    @Override
    protected void handleShellCloseEvent() {
        handleAbortion();
        super.handleShellCloseEvent();
    }

    @Override
    public boolean close() {
        if (getBindingContext() != null) {
            getBindingContext().dispose();
        }

        IpsPlugin.getDefault().getIpsModel().removeChangeListener(this);

        return super.close();
    }

    private void handleAbortion() {
        part.getIpsObject().setState(oldState);
        if (!dirty) {
            part.getIpsObject().getIpsSrcFile().markAsClean();
        }
    }

    @Override
    protected Control createContents(Composite parent) {
        Control control = super.createContents(parent);
        getBindingContext().updateUI();
        updateTitleInTitleArea();
        // updateMessageArea should be called after the size of the dialog has been calculated -->
        // @see method create() (MTB#142)
        // at least an empty message must be set, otherwise the height of the dialog will be
        // to short (MTB#0291 )
        setMessage(null);
        return control;
    }

    @Override
    public void create() {
        super.create();
        // updateMessageArea have to be called after the size of the dialog is set. (MTB#142)
        updateMessageArea();
    }

    @Override
    protected final Composite createWorkArea(Composite parent) {
        Composite composite = createWorkAreaThis(parent);
        if (isTabFolderUsed()) {
            TabFolder tabFolder = (TabFolder)parent;
            if (part instanceof IDescribedElement && part instanceof ILabeledElement) {
                createLabelAndDescriptionTabItem(tabFolder);
            } else if (part instanceof IDescribedElement) {
                createDescriptionTabItem(tabFolder);
            } else if (part instanceof ILabeledElement) {
                createLabelTabItem(tabFolder);
            }
        }
        return composite;
    }

    protected abstract Composite createWorkAreaThis(Composite parent);

    private TabItem createLabelAndDescriptionTabItem(TabFolder folder) {
        Composite composite = getToolkit().createGridComposite(folder, 1, true, true);
        Group labelGroup = getToolkit().createGroup(composite, Messages.IpsPartEditDialog_groupLabel);
        labelEditComposite = new LabelEditComposite(labelGroup, (ILabeledElement)part, getToolkit());
        Group descriptionGroup = getToolkit().createGroup(composite, Messages.IpsPartEditDialog_groupDescription);
        new DescriptionEditComposite(descriptionGroup, (IDescribedElement)part, getToolkit());

        TabItem item = new TabItem(folder, SWT.NONE);
        item.setText(Messages.IpsPartEditDialog_tabItemLabelAndDescription);
        item.setControl(composite);

        return item;
    }

    private TabItem createDescriptionTabItem(TabFolder folder) {
        IDescribedElement describedElement = (IDescribedElement)part;
        Composite editComposite = new DescriptionEditComposite(folder, describedElement, getToolkit());

        TabItem item = new TabItem(folder, SWT.NONE);
        item.setText(Messages.IpsPartEditDialog_tabItemDescription);
        item.setControl(editComposite);

        return item;
    }

    private TabItem createLabelTabItem(TabFolder folder) {
        labelEditComposite = new LabelEditComposite(folder, (ILabeledElement)part, getToolkit());

        TabItem item = new TabItem(folder, SWT.NONE);
        item.setText(Messages.IpsPartEditDialog_tabItemLabel);
        item.setControl(labelEditComposite);

        return item;
    }

    public IIpsObjectPart getIpsPart() {
        return part;
    }

    @Override
    protected void updateTitleInTitleArea() {
        setTitle(buildTitle());
    }

    /**
     * Creates the title of this dialog. The title will include the name of the ips object the ips
     * object part belongs to and the name of the ips object part itself.
     */
    protected String buildTitle() {
        IIpsObjectPart part = getIpsPart();
        if (part.getParent() instanceof IIpsObjectGeneration) {
            return part.getIpsObject().getName() + ' ' + part.getParent().getName() + '.' + part.getName();
        }
        return part.getIpsObject().getName() + '.' + part.getName();
    }

    @Override
    public void contentsChanged(ContentChangeEvent event) {
        if (event.getIpsSrcFile().equals(getIpsPart().getIpsSrcFile())) {
            updateTitleInTitleArea();
            updateMessageArea();
            contentsChangedInternal(event);
        }
    }

    /**
     * Updates the message area at the top of the dialog beneath the title with up-to-date
     * validation information.
     */
    protected void updateMessageArea() {
        MessageList msgList;
        try {
            msgList = part.validate(part.getIpsProject());
            MessageList objMsgList = part.getIpsObject().validate(part.getIpsProject());
            msgList.add(objMsgList.getMessagesFor(part));
            addAdditionalDialogMessages(msgList);
        } catch (CoreException e) {
            IpsPlugin.log(e);
            return;
        }

        if (msgList.size() > 0) {
            Message firstErrorMsg = getFirstMessage(msgList, Message.ERROR, bindingContext.getIgnoredMessageCodes());
            if (firstErrorMsg != null) {
                setMessage(firstErrorMsg.getText(), IMessageProvider.ERROR);
                return;
            }

            Message firstWarningMsg = getFirstMessage(msgList, Message.WARNING, bindingContext.getIgnoredMessageCodes());
            if (firstWarningMsg != null) {
                setMessage(firstWarningMsg.getText(), IMessageProvider.WARNING);
                return;
            }

            Message firstInfoMsg = getFirstMessage(msgList, Message.INFO, bindingContext.getIgnoredMessageCodes());
            if (firstInfoMsg != null) {
                setMessage(firstInfoMsg.getText(), IMessageProvider.INFORMATION);
                return;
            }
        }

        if (part instanceof IDescribedElement) {
            String localizedDescription = IpsPlugin.getMultiLanguageSupport().getLocalizedDescription(
                    (IDescribedElement)part);
            if (!(StringUtils.isEmpty(localizedDescription.trim()))) {
                setMessage(localizedDescription, IMessageProvider.INFORMATION);
                return;
            }
        }

        setMessage(null);
    }

    /**
     * Overwrite this method to add additional messages to the dialog message area.
     * 
     * @param messageList Add your additional messages to this {@link MessageList}.
     * 
     */
    protected void addAdditionalDialogMessages(MessageList messageList) {
        // default implementation does nothing
    }

    private Message getFirstMessage(MessageList messageList, int severity, Set<String> ignoredMessageCodes) {
        for (int i = 0; i < messageList.size(); i++) {
            Message message = messageList.getMessage(i);
            if (message.getSeverity() == severity && !(ignoredMessageCodes.contains(message.getCode()))) {
                return message;
            }
        }
        return null;
    }

    /**
     * Method for sub classes to hook into the contents changed notification.
     * 
     * @param event The event that caused the content change
     */
    protected void contentsChangedInternal(ContentChangeEvent event) {
        // Empty default implementation
    }

    /**
     * Allows subclasses to set whether the composite that allows to edit labels is enabled in case
     * the part being edited is an {@link ILabeledElement}.
     * 
     * @throws RuntimeException If the part being edited is not an {@link ILabeledElement}
     */
    protected final void setLabelCompositeEnabled(boolean enabled) {
        if (!(part instanceof ILabeledElement)) {
            throw new RuntimeException();
        }
        labelEditComposite.setEnabled(enabled);
    }

}
