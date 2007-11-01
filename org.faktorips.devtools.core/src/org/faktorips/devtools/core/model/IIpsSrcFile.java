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

package org.faktorips.devtools.core.model;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * A handle for files containing sourcecode for ips objects.
 */
public interface IIpsSrcFile extends IIpsElement {
    
    /**
     * Returns the package fragment the file belongs to.
     */
    public IIpsPackageFragment getIpsPackageFragment();
    
    /**
     * Returns the corresponding platform file.
     */
    public IFile getCorrespondingFile();
    
    /**
     * Returns true if the file contains unsaved changes to it's contents. 
     * @throws CoreException
     */
    public boolean isDirty();
    
    /**
     * Marks the file as containing no unsaved changed.
     */
    public void markAsClean();
    
    /**
     * Marks the file as containing unsaved changes.
     */
    public void markAsDirty();
    
    /**
     * Discard all changes that haven't been saved yet. If the file
     * is not dirty, nothing happens.
     */
    public void discardChanges();
    
    /**
     * Returns a new memento with the file's content and dirty state.
     */
    public IIpsSrcFileMemento newMemento() throws CoreException;
    
    /**
     * Update the file's state with the informaiton from the memento.
     * 
     * @throws CoreException if the memento wasn't taken from this file.
     */
    public void setMemento(IIpsSrcFileMemento memento) throws CoreException;
    
    /**
     * Saves the file's content to the file system.
     *  
     * @throws CoreException
     */
    public void save(boolean force, IProgressMonitor monitor) throws CoreException;
    
    /**
     * Returns true if the content can be parsed and a IpsObject can be created
     * based on the content. Returns false, if the contents can't be parsed
     * (e.g. the XML isn't properly formatted).
     * 
     * @throws CoreException if an error occurs while reading the contents.
     */
    public boolean isContentParsable() throws CoreException;
    
    /**
     * Returns <code>true</code> if the file has been read from the resource history.
     */
    public boolean isHistoric();
    
    /**
     * Returns the IPS object stored in the file.
     * 
     * @throws CoreException if the file can't be read or it's contents can't be
     * parsed.
     */
    public IIpsObject getIpsObject() throws CoreException;
    
    /**
     * Returns the given property of the source file.Returns <code>null</code> if the given property 
     * wasn't found.<br>
     * Performance hint: this method reads only the properties of the ips source file (attributes of
     * the first xml node) and returns the value of the given property, the ips object is not
     * completely read until {@link #getIpsObject()} method is called.
     * 
     * @throws CoreException if the file can't be read or it's contents can't be parsed.
     */
    public String getPropertyValue(String name) throws CoreException;
    
    /**
     * Returns the IpsObjectType that is contain in this IpsSrcFile. 
     */
    public IpsObjectType getIpsObjectType();
    
    /**
     * Returns the qualified name type of the ips object contained within this IpsSrcFile. 
     */
    public QualifiedNameType getQualifiedNameType();
    
    /**
     * Returns the name of the ips object stored in the file.
     */
    public String getIpsObjecName();
    
    /**
     * Returns whether this file is mutable or not.
     */
    public boolean isMutable();
    
    /**
     * Reads the content from the enclosing resource and returns it as string 
     * The bytes read from disk are transformed into a string with the encoding
     * defined in the ips project.
     * If this is a source file from an archive, the data is read from the archive,
     * otherwise it is read from the correspondig file.
     * 
     * @throws CoreException if an error occurs while reading the contents.
     * 
     * @see org.faktorips.devtools.core.model.IIpsProject#getXmlFileCharset()
     */
    public String getContentFromEnclosingResource() throws CoreException;
    
    /**
     * Returns the name of the base package for the generated Java source files.
     * All generated Java types are contained in this package or one of the child packages.
     */
    public String getBasePackageNameForGeneratedJavaClass() throws CoreException;

    /**
     * Returns the name of the base package for the extension Java source files. All generated Java types
     * are contained in this package or one of the child packages.
     * Extension Java files are the files where the developer adds it's own code.
     */
    public String getBasePackageNameForExtensionJavaClass() throws CoreException;

    
}
