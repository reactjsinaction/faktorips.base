/***************************************************************************************************
 *  * Copyright (c) 2005,2006 Faktor Zehn GmbH und andere.  *  * Alle Rechte vorbehalten.  *  *
 * Dieses Programm und alle mitgelieferten Sachen (Dokumentationen, Beispiele,  * Konfigurationen,
 * etc.) duerfen nur unter den Bedingungen der  * Faktor-Zehn-Community Lizenzvereinbarung - Version
 * 0.1 (vor Gruendung Community)  * genutzt werden, die Bestandteil der Auslieferung ist und auch
 * unter  *   http://www.faktorips.org/legal/cl-v01.html  * eingesehen werden kann.  *  *
 * Mitwirkende:  *   Faktor Zehn GmbH - initial API and implementation - http://www.faktorzehn.de  *  
 **************************************************************************************************/

package org.faktorips.devtools.core.builder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.faktorips.devtools.core.IpsPlugin;
import org.faktorips.devtools.core.IpsStatus;
import org.faktorips.devtools.core.internal.model.IpsModel;
import org.faktorips.devtools.core.internal.model.ipsobject.IpsSrcFile;
import org.faktorips.devtools.core.internal.model.ipsproject.IpsProject;
import org.faktorips.devtools.core.model.Dependency;
import org.faktorips.devtools.core.model.IIpsElement;
import org.faktorips.devtools.core.model.ipsobject.IIpsObject;
import org.faktorips.devtools.core.model.ipsobject.IIpsSrcFile;
import org.faktorips.devtools.core.model.ipsobject.QualifiedNameType;
import org.faktorips.devtools.core.model.ipsproject.IIpsArchiveEntry;
import org.faktorips.devtools.core.model.ipsproject.IIpsArtefactBuilder;
import org.faktorips.devtools.core.model.ipsproject.IIpsArtefactBuilderSet;
import org.faktorips.devtools.core.model.ipsproject.IIpsPackageFragment;
import org.faktorips.devtools.core.model.ipsproject.IIpsPackageFragmentRoot;
import org.faktorips.devtools.core.model.ipsproject.IIpsProject;
import org.faktorips.util.message.Message;
import org.faktorips.util.message.MessageList;

/**
 * The ips builder generates Java sourcecode and xml files based on the ips objects contained in the
 * ips project. It runs before the Java builder, so that first the Java sourcecode is generated by
 * the ips builder and then the Java builder compiles the Java sourcecode into classfiles.
 */
public class IpsBuilder extends IncrementalProjectBuilder {

    /**
     * The builders extension id.
     */
    public final static String BUILDER_ID = IpsPlugin.PLUGIN_ID + ".ipsbuilder"; //$NON-NLS-1$

    public final static boolean TRACE_BUILDER_TRACE;

    static {
        TRACE_BUILDER_TRACE = Boolean
                .valueOf(Platform.getDebugOption("org.faktorips.devtools.core/trace/builder")).booleanValue(); //$NON-NLS-1$
    }

    public IpsBuilder() {
        super();
    }

    private MultiStatus createInitialMultiStatus() {
        return new MultiStatus(IpsPlugin.PLUGIN_ID, 0, Messages.IpsBuilder_msgBuildResults, null);
    }

    /**
     * {@inheritDoc}
     */
    protected IProject[] build(int kind, Map args, IProgressMonitor monitor) throws CoreException {
        MultiStatus buildStatus = createInitialMultiStatus();
        try {
            monitor.beginTask("build", 100000); //$NON-NLS-1$
            monitor.subTask(Messages.IpsBuilder_validatingProject);
            // we have to clear the validation cache as for example the deletion of ips source files
            // migth still be
            // undetected as the validation result cache gets cleared in a resource change listener.
            // it is not guaranteed that the listener is notified before the build starts!
            getIpsProject().getIpsModel().clearValidationCache();
            getProject().deleteMarkers(IpsPlugin.PROBLEM_MARKER, true, 0);
            MessageList list = getIpsProject().validate();
            createMarkersFromMessageList(getProject(), list, IpsPlugin.PROBLEM_MARKER);
            monitor.worked(100);
            if (!getIpsProject().canBeBuild()) {
                IMarker marker = getProject().createMarker(IpsPlugin.PROBLEM_MARKER);
                String msg = Messages.IpsBuilder_msgInvalidProperties;
                updateMarker(marker, msg, IMarker.SEVERITY_ERROR);
                return getProject().getReferencedProjects();
            }
            monitor.subTask(Messages.IpsBuilder_preparingBuild);
            IIpsArtefactBuilderSet ipsArtefactBuilderSet = getIpsProject().getIpsArtefactBuilderSet();
            boolean isFullBuildRequired = isFullBuildRequired(kind);
            if (isFullBuildRequired) {
                kind = IncrementalProjectBuilder.FULL_BUILD;
            }
            applyBuildCommand(ipsArtefactBuilderSet, buildStatus, new BeforeBuildProcessCommand(kind), monitor);
            monitor.worked(100);
            if (isFullBuildRequired) {
                kind = IncrementalProjectBuilder.FULL_BUILD;
                monitor.subTask(Messages.IpsBuilder_startFullBuild);
                fullBuild(ipsArtefactBuilderSet, buildStatus, new SubProgressMonitor(monitor, 99700));
            } else {
                monitor.subTask(Messages.IpsBuilder_startIncrementalBuild);
                incrementalBuild(ipsArtefactBuilderSet, buildStatus, new SubProgressMonitor(monitor, 99700));
            }
            monitor.subTask(Messages.IpsBuilder_finishBuild);
            applyBuildCommand(ipsArtefactBuilderSet, buildStatus, new AfterBuildProcessCommand(kind), monitor);
            monitor.worked(100);
            if (buildStatus.getSeverity() == IStatus.OK) {
                return getProject().getReferencedProjects();
            }

            // reinitialize the builders of the current builder set if an error
            // occurs
            getIpsProject().reinitializeIpsArtefactBuilderSet();
            throw new CoreException(buildStatus);

        } catch (OperationCanceledException e) {
            getIpsProject().reinitializeIpsArtefactBuilderSet();
        } catch (CoreException e) {
            throw e;
        } catch (Exception e) {
            throw new CoreException(new IpsStatus(e));
        } finally {
            monitor.done();
        }
        return getProject().getReferencedProjects();
    }

    private boolean beforeBuild(IProject project, IIpsProject ipsProject, IProgressMonitor monitor, MultiStatus buildStatus){
//        project.deleteMarkers(IpsPlugin.PROBLEM_MARKER, true, 0);
//        MessageList list = ipsProject.validate();
//        createMarkersFromMessageList(project, list, IpsPlugin.PROBLEM_MARKER);
//        monitor.worked(100);
//        if (!ipsProject.canBeBuild()) {
//            IMarker marker = project.createMarker(IpsPlugin.PROBLEM_MARKER);
//            String msg = Messages.IpsBuilder_msgInvalidProperties;
//            updateMarker(marker, msg, IMarker.SEVERITY_ERROR);
//            return false;
//        }
//        monitor.subTask(Messages.IpsBuilder_preparingBuild);
//        IIpsArtefactBuilderSet ipsArtefactBuilderSet = getIpsProject().getIpsArtefactBuilderSet();
//        boolean isFullBuildRequired = isFullBuildRequired(kind);
//        if (isFullBuildRequired) {
//            kind = IncrementalProjectBuilder.FULL_BUILD;
//        }
//        applyBuildCommand(ipsArtefactBuilderSet, buildStatus, new BeforeBuildProcessCommand(kind), monitor);
//        monitor.worked(100);
        return true;
    }
    
    private void afterBuild(){
        
    }
    
    private boolean isFullBuildRequired(int kind) throws CoreException {
        if (kind == FULL_BUILD || kind == CLEAN_BUILD) {
            return true;
        }
        IResourceDelta delta = getDelta(getProject());
        if (delta == null) {
            return true;
        }
        IIpsProject ipsProject = getIpsProject();
        if (delta.findMember(ipsProject.getIpsProjectPropertiesFile().getProjectRelativePath()) != null) {
            return true;
        }
        IIpsArchiveEntry[] entries = ipsProject.getIpsObjectPath().getArchiveEntries();
        for (int i = 0; i < entries.length; i++) {
            IFile archiveFile = entries[i].getArchiveFile();
            if (archiveFile != null && delta.findMember(archiveFile.getProjectRelativePath()) != null) {
                return true;
            }
        }
        return false;
    }

    private void applyBuildCommand(IIpsArtefactBuilderSet currentBuilderSet,
            MultiStatus buildStatus,
            BuildCommand command,
            IProgressMonitor monitor) throws CoreException {
        // Despite the fact that generating is disabled in the faktor ips
        // preferences the
        // validation of the modell class instances and marker updating of the
        // regarding resource files still takes place
        if (!IpsPlugin.getDefault().getIpsPreferences().getEnableGenerating()) {
            return;
        }
        IIpsArtefactBuilder[] artefactBuilders = currentBuilderSet.getArtefactBuilders();
        for (int i = 0; i < artefactBuilders.length; i++) {
            try {
                command.build(artefactBuilders[i], buildStatus);
            } catch (Exception e) {
                addIpsStatus(artefactBuilders[i], command, buildStatus, e);
            }
        }
        if (monitor.isCanceled()) {
            throw new OperationCanceledException();
        }
    }

    private void addIpsStatus(IIpsArtefactBuilder builder, BuildCommand command, MultiStatus buildStatus, Exception e) {
        String text = builder.getName() + ": Error during: " + command + "."; //$NON-NLS-1$ //$NON-NLS-2$
        buildStatus.add(new IpsStatus(text, e));
    }

    private DependencyGraph getDependencyGraph() throws CoreException {
        IpsModel model = ((IpsModel)getIpsProject().getIpsModel());
        return model.getDependencyGraph(getIpsProject());
    }

    /*
     * Returns the ips project the build is currently building.
     */
    private IIpsProject getIpsProject() {
        return IpsPlugin.getDefault().getIpsModel().getIpsProject(getProject());
    }

    private void collectIpsSrcFilesForFullBuild(List allIpsSrcFiles) throws CoreException {
        IIpsPackageFragmentRoot[] roots = getIpsProject().getIpsPackageFragmentRoots();
        for (int i = 0; i < roots.length; i++) {
            if (!roots[i].isBasedOnSourceFolder()) {
                continue;
            }
            IIpsPackageFragment[] packs = roots[i].getIpsPackageFragments();
            for (int j = 0; j < packs.length; j++) {
                IIpsElement[] elements = packs[j].getChildren();
                for (int k = 0; k < elements.length; k++) {
                    if (elements[k] instanceof IIpsSrcFile) {
                        allIpsSrcFiles.add(elements[k]);
                    }
                }
            }
        }
    }

    private void removeEmptyFolders() throws CoreException {
        IIpsPackageFragmentRoot[] roots = getIpsProject().getIpsPackageFragmentRoots();
        for (int i = 0; i < roots.length; i++) {
            if (roots[i].isBasedOnSourceFolder()) {
                removeEmptyFolders(roots[i].getArtefactDestination(false), false);
            }
        }
    }

    /**
     * Full build generates Java source files for all IPS objects.
     */
    private MultiStatus fullBuild(IIpsArtefactBuilderSet ipsArtefactBuilderSet,
            MultiStatus buildStatus,
            IProgressMonitor monitor) {
        if (TRACE_BUILDER_TRACE) {
            System.out.println("Full build started."); //$NON-NLS-1$
        }
        long begin = System.currentTimeMillis();

        try {
            ArrayList allIpsSrcFiles = new ArrayList();
            collectIpsSrcFilesForFullBuild(allIpsSrcFiles);
            monitor.beginTask("full build", 2 * allIpsSrcFiles.size()); //$NON-NLS-1$
            getDependencyGraph().reInit();
            monitor.worked(allIpsSrcFiles.size());
            removeEmptyFolders();

            for (Iterator it = allIpsSrcFiles.iterator(); it.hasNext();) {
                try {
                    IIpsSrcFile ipsSrcFile = (IIpsSrcFile)it.next();
                    monitor.subTask(Messages.IpsBuilder_building + ipsSrcFile.getName());
                    buildIpsSrcFile(ipsArtefactBuilderSet, getIpsProject(), ipsSrcFile, buildStatus, monitor);
                    monitor.worked(1);
                } catch (Exception e) {
                    buildStatus.add(new IpsStatus(e));
                }
            }
        } catch (CoreException e) {
            buildStatus.add(new IpsStatus(e));
        } finally {
            monitor.done();
        }
        long end = System.currentTimeMillis();
        if (TRACE_BUILDER_TRACE) {
            System.out.println("Full build finished. Duration: " + (end - begin)); //$NON-NLS-1$
        }
        return buildStatus;
    }

    /**
     * {@inheritDoc}
     */
    protected void clean(IProgressMonitor monitor) throws CoreException {
        IIpsPackageFragmentRoot[] roots = getIpsProject().getIpsPackageFragmentRoots();
        for (int i = 0; i < roots.length; i++) {

            if (!roots[i].isBasedOnSourceFolder()) {
                continue;
            }
            IFolder destination = roots[i].getArtefactDestination(true);
            if (destination == null) {
                continue;
            }
            if (destination.exists()) {
                removeDerivedResources(destination, monitor);
            }
        }
    }

    /*
     * Only the resource (file, folder) that is actually derived will be deleted. So if a user chooses to place a
     * non derived resource in the destination folder it will not be deleted. Accordingly all
     * folders in the folder hierarchy starting from the folder that contains the derived resource
     * will not be deleted.
     */
    private void removeDerivedResources(IFolder folder, IProgressMonitor monitor) throws CoreException {
        IResource[] members = folder.members();
        for (int i = 0; i < members.length; i++) {
            if (members[i].exists()) {
                if (members[i].getType() == IResource.FILE && members[i].isDerived()) {
                    members[i].delete(true, monitor);
                    continue;
                }
                if (members[i].getType() == IResource.FOLDER) {
                    IFolder folderMember = (IFolder)members[i];
                    removeDerivedResources(folderMember, monitor);
                    if (folderMember.members().length == 0 && folderMember.isDerived()) {
                        folderMember.delete(true, monitor);
                    }
                }
            }
        }
    }

    private void removeEmptyFolders(IFolder parent, boolean removeThisParent) throws CoreException {
        if (!parent.exists()) {
            return;
        }
        IResource[] members = parent.members();
        if (removeThisParent && members.length == 0) {
            parent.delete(true, null);
            return;
        }
        for (int i = 0; i < members.length; i++) {
            if (members[i].getType() == IResource.FOLDER) {
                removeEmptyFolders((IFolder)members[i], true);
            }
        }
    }

    private Set getDependencySetForProject(IIpsProject project, Map buildCandidatesForProjectMap) {
        Set buildCandidatesSet = (Set)buildCandidatesForProjectMap.get(project);
        if (buildCandidatesSet == null) {
            buildCandidatesSet = new HashSet(1000);
            buildCandidatesForProjectMap.put(project, buildCandidatesSet);
        }
        return buildCandidatesSet;
    }

    /**
     * Incremental build generates Java source files for all PdObjects that have been changed.
     */
    private void incrementalBuild(IIpsArtefactBuilderSet ipsArtefactBuilderSet,
            MultiStatus buildStatus,
            IProgressMonitor monitor) {
        if (TRACE_BUILDER_TRACE) {
            System.out.println("Incremental build started."); //$NON-NLS-1$
        }

        try {
            IResourceDelta delta = getDelta(getProject());
            IncBuildVisitor visitor = new IncBuildVisitor();
            delta.accept(visitor);
            Map dependenciesForProjectsMap = new HashMap(10);
            int numberOfBuildCandidates = collectDependenciesForIncrementalBuild(visitor.changedAndAddedIpsSrcFiles,
                    visitor.removedIpsSrcFiles, dependenciesForProjectsMap)
                    + visitor.removedIpsSrcFiles.size() + visitor.changedAndAddedIpsSrcFiles.size();
            monitor.beginTask("build incremental", numberOfBuildCandidates); //$NON-NLS-1$
            for (Iterator it = visitor.removedIpsSrcFiles.iterator(); it.hasNext();) {
                IpsSrcFile ipsSrcFile = (IpsSrcFile)it.next();
                monitor.subTask(Messages.IpsBuilder_deleting + ipsSrcFile.getName());
                applyBuildCommand(ipsArtefactBuilderSet, buildStatus, new DeleteArtefactBuildCommand(ipsSrcFile),
                        monitor);
                updateDependencyGraph(buildStatus, ipsSrcFile);
                monitor.worked(1);
            }

            for (Iterator it = visitor.changedAndAddedIpsSrcFiles.iterator(); it.hasNext();) {
                IpsSrcFile ipsSrcFile = (IpsSrcFile)it.next();
                monitor.subTask(Messages.IpsBuilder_building + ipsSrcFile.getName());
                buildIpsSrcFile(ipsArtefactBuilderSet, getIpsProject(), ipsSrcFile, buildStatus, monitor);
                updateDependencyGraph(buildStatus, ipsSrcFile);
                monitor.worked(1);
            }

            for (Iterator it = dependenciesForProjectsMap.keySet().iterator(); it.hasNext();) {
                IIpsProject ipsProject = (IIpsProject)it.next();
                Set dependencySet = (Set)dependenciesForProjectsMap.get(ipsProject);
                
                //dependent ips object can be located in a different project which can have a differen artefact builder set
                //therefor the builder set needs to be determined for each project at this point
                ipsArtefactBuilderSet = ipsProject.getIpsArtefactBuilderSet();
                Set alreadyBuild = new HashSet(dependencySet.size());
                for (Iterator it2 = dependencySet.iterator(); it2.hasNext();) {
                    Dependency dependency = (Dependency)it2.next();
                    QualifiedNameType buildCandidate = dependency.getSource();
                    if (alreadyBuild.contains(buildCandidate)) {
                        continue;
                    }
                    alreadyBuild.add(buildCandidate);
                    IIpsObject ipsObject = ipsProject.findIpsObject(buildCandidate);
                    if (ipsObject == null) {
                        continue;
                    }
                    monitor.subTask(Messages.IpsBuilder_building + dependency);
                    buildIpsSrcFile(ipsArtefactBuilderSet, ipsProject, ipsObject.getIpsSrcFile(), buildStatus, monitor);
                    updateDependencyGraph(buildStatus, ipsObject.getIpsSrcFile());
                    monitor.worked(1);
                }
            }
        } catch (Exception e) {
            buildStatus.add(new IpsStatus(e));
        } finally {
            monitor.done();
            if (TRACE_BUILDER_TRACE) {
                System.out.println("Incremental build finished."); //$NON-NLS-1$
            }
        }
    }

    private void updateMarkers(MultiStatus buildStatus, IIpsObject object) {
        if (object == null) {
            return;
        }
        IResource resource = object.getEnclosingResource();
        if (!resource.exists()) {
            return;
        }
        try {

            MessageList list = object.validate(object.getIpsProject());
            createMarkersFromMessageList(resource, list, IpsPlugin.PROBLEM_MARKER);
        } catch (Exception e) {
            buildStatus.add(new IpsStatus("An exception occured during marker updating for " + object, e)); //$NON-NLS-1$
        }
    }

    private void createMarkersFromMessageList(IResource resource, MessageList list, String markerType)
            throws CoreException {
        resource.deleteMarkers(IpsPlugin.PROBLEM_MARKER, true, IResource.DEPTH_ZERO);
        for (int i = 0; i < list.getNoOfMessages(); i++) {
            Message msg = list.getMessage(i);
            IMarker marker = resource.createMarker(markerType);
            updateMarker(marker, msg.getText(), getMarkerSeverity(msg));
        }
    }

    private void updateMarker(IMarker marker, String text, int severity) throws CoreException {
        marker.setAttributes(new String[] { IMarker.MESSAGE, IMarker.SEVERITY }, new Object[] { text,
                new Integer(severity) });
    }

    private int getMarkerSeverity(Message msg) {
        int msgSeverity = msg.getSeverity();
        if (msgSeverity == Message.ERROR) {
            return IMarker.SEVERITY_ERROR;
        } else if (msgSeverity == Message.WARNING) {
            return IMarker.SEVERITY_WARNING;
        } else if (msgSeverity == Message.INFO) {
            return IMarker.SEVERITY_INFO;
        }
        throw new RuntimeException("Unknown severity " + msgSeverity); //$NON-NLS-1$
    }

    /**
     * Builds the indicated file and updates its markers.
     */
    private IIpsObject buildIpsSrcFile(IIpsArtefactBuilderSet ipsArtefactBuilderSet,
            IIpsProject ipsProject,
            IIpsSrcFile file,
            MultiStatus buildStatus,
            IProgressMonitor monitor) throws CoreException {
        if (!file.isContentParsable()) {
            IMarker marker = file.getCorrespondingResource().createMarker(IpsPlugin.PROBLEM_MARKER);
            marker.setAttribute(IMarker.MESSAGE, Messages.IpsBuilder_ipsSrcFileNotParsable);
            marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
            return null;
        }
        IIpsObject ipsObject = file.getIpsObject();
        MultiStatus newStatus = createInitialMultiStatus();
        applyBuildCommand(ipsArtefactBuilderSet, newStatus, new BuildArtefactBuildCommand(file), monitor);
        if (!newStatus.isOK()) {
            fillMultiStatusWithMessageList(newStatus, ipsObject.validate(ipsProject));
        }
        buildStatus.add(newStatus);
        updateMarkers(buildStatus, ipsObject);
        return ipsObject;
    }

    private void fillMultiStatusWithMessageList(MultiStatus status, MessageList list) throws CoreException {
        for (int i = 0; i < list.getNoOfMessages(); i++) {
            Message msg = list.getMessage(i);
            status.add(new IpsStatus(getMarkerSeverity(msg), msg.getText(), null));
        }
    }

    private void updateDependencyGraph(MultiStatus buildStatus, IIpsSrcFile ipsSrcFile) {
        try {
            DependencyGraph graph = ((IpsModel)ipsSrcFile.getIpsProject().getIpsModel()).getDependencyGraph(ipsSrcFile
                    .getIpsProject());
            graph.update(ipsSrcFile.getQualifiedNameType());
        } catch (CoreException e) {
            buildStatus.add(new IpsStatus("An error occured while trying to update the " + //$NON-NLS-1$
                    "dependency graph for the IpsSrcFile: " + ipsSrcFile, e)); //$NON-NLS-1$
        }
    }
    
    private int collectDependenciesForIncrementalBuild(List addedOrChangesIpsSrcFiles,
            List removedIpsSrcFiles,
            Map dependenciesForProjectsMap) throws CoreException {

        IIpsProject ipsProject = getIpsProject();
        Counter counter = new Counter(0);
        for (Iterator it = addedOrChangesIpsSrcFiles.iterator(); it.hasNext();) {
            IpsSrcFile ipsSrcFile = (IpsSrcFile)it.next();
            collectDependenciesForProject(ipsSrcFile.getQualifiedNameType(), ipsProject, new HashSet(),
                    dependenciesForProjectsMap, counter, false);
        }
        for (Iterator it = removedIpsSrcFiles.iterator(); it.hasNext();) {
            IpsSrcFile ipsSrcFile = (IpsSrcFile)it.next();
            // TODO do we still need to find out if the file is in the ips projects roots?
            IIpsPackageFragmentRoot[] roots = ipsSrcFile.getIpsProject().getSourceIpsPackageFragmentRoots();
            for (int i = 0; i < roots.length; i++) {
                if (ipsSrcFile.getIpsPackageFragment().getRoot().equals(roots[i])) {
                    collectDependenciesForProject(ipsSrcFile.getQualifiedNameType(),
                            ipsProject, new HashSet(), dependenciesForProjectsMap, counter, false);
                }
            }
        }
        return counter.getCounts();
    }

    private void collectDependenciesForProject(QualifiedNameType root,
            IIpsProject ipsProject,
            Set visitedProjects,
            Map dependenciesForProjectMap,
            Counter counter,
            boolean searchInstanceOfDependencyOnly) throws CoreException {

        // build object of dependant projects only if the dependant project can be build...
        if (!ipsProject.canBeBuild()) {
            return;
        }

        IpsModel model = (IpsModel)IpsPlugin.getDefault().getIpsModel();
        DependencyGraph graph = model.getDependencyGraph(ipsProject);
        if (graph == null) {
            return;
        }
        // when collecting dependencies it is necessary to know if the builder set has any
        // properties that influences the dependency management
        IIpsArtefactBuilderSet ipsArtefactBuilderSet = model.getIpsArtefactBuilderSet(ipsProject, false);
        collectDependencies(ipsArtefactBuilderSet, graph, dependenciesForProjectMap, root, counter, searchInstanceOfDependencyOnly);
        collectDependenciesWithinDependantProjects(root, ipsProject, visitedProjects, dependenciesForProjectMap,
                counter, searchInstanceOfDependencyOnly);
    }

    private void collectDependenciesWithinDependantProjects(QualifiedNameType root,
            IIpsProject ipsProject,
            Set visitedProjects,
            Map dependenciesForProjectMap,
            Counter counter,
            boolean searchInstanceOfDependencyOnly) throws CoreException {

        visitedProjects.add(ipsProject);
        IIpsProject[] dependantProjects = ipsProject.getReferencingProjects(false);
        for (int i = 0; i < dependantProjects.length && !visitedProjects.contains(dependantProjects[i]); i++) {
            collectDependenciesForProject(root, dependantProjects[i], visitedProjects, dependenciesForProjectMap,
                    counter, searchInstanceOfDependencyOnly);
        }
        return;
    }    
    
    private void collectDependencies(IIpsArtefactBuilderSet ipsArtefactBuilderSet,
            DependencyGraph graph,
            Map dependenciesForProjectMap,
            QualifiedNameType nameType,
            Counter counter,
            boolean searchInstanceOfDependencyOnly) throws CoreException {

        Set dependencySet = getDependencySetForProject(graph.getIpsProject(), dependenciesForProjectMap);
        Dependency[] dependencies = graph.getDependants(nameType);

        for (int i = 0; i < dependencies.length; i++) {
            if (!dependencySet.contains(dependencies[i])) {
                
                if(searchInstanceOfDependencyOnly){
                    if (dependencies[i].isInstanceOf()) {
                        dependencySet.add(dependencies[i]);
                        counter.increment();
                    }
                    continue;
                }
                
                dependencySet.add(dependencies[i]);
                counter.increment();
                if (dependencies[i].isSubtype()) {
                    collectDependencies(ipsArtefactBuilderSet, graph, dependenciesForProjectMap, dependencies[i]
                            .getSource(), counter, false);
                    collectDependenciesWithinDependantProjects(dependencies[i].getSource(), graph.getIpsProject(),
                            new HashSet(100), dependenciesForProjectMap, counter, false);
                    continue;
                }

                if (dependencies[i].isCompositionMasterDetail() && ipsArtefactBuilderSet.containsAggregateRootBuilder()) {
                    collectDependencies(ipsArtefactBuilderSet, graph, dependenciesForProjectMap, dependencies[i]
                            .getSource(), counter, false);
                    collectDependenciesWithinDependantProjects(dependencies[i].getSource(), graph.getIpsProject(),
                            new HashSet(100), dependenciesForProjectMap, counter, false);
                    continue;
                }
                collectDependencies(ipsArtefactBuilderSet, graph, dependenciesForProjectMap, dependencies[i]
                        .getSource(), counter, true);
                collectDependenciesWithinDependantProjects(dependencies[i].getSource(), graph.getIpsProject(),
                        new HashSet(100), dependenciesForProjectMap, counter, true);
            }
        }
        return;
    }
    
    private static class Counter{
        
        private int counts = 0;
        
        private Counter(int offSet){
            this.counts = offSet;
        }
        
        private void increment(){
            counts++;
        }
        
        private int getCounts(){
            return counts;
        }
    }
    
    /**
     * ResourceDeltaVisitor for the incremental build.
     */
    private class IncBuildVisitor implements IResourceDeltaVisitor {

        private IFolder[] outputFolders;
        private List removedIpsSrcFiles = new ArrayList(100);
        private List changedAndAddedIpsSrcFiles = new ArrayList(100);

        private IncBuildVisitor() throws CoreException {
            outputFolders = getIpsProject().getOutputFolders();
        }

        public List getRemovedIpsSrcFiles() {
            return removedIpsSrcFiles;
        }

        public List getChangedOrAddedIpsSrcFiles() {
            return changedAndAddedIpsSrcFiles;
        }

        /**
         * Checks if the provided resource is the java output folder resource or the IpsProject
         * output folder resource.
         * 
         * @throws CoreException
         */
        private boolean ignoredResource(IResource resource) throws CoreException {
            IPath outPutLocation = getIpsProject().getJavaProject().getOutputLocation();
            IPath resourceLocation = resource.getFullPath();
            if (outPutLocation.equals(resourceLocation)) {
                return true;
            }
            for (int i = 0; i < outputFolders.length; i++) {
                if (outputFolders[i].getFullPath().equals(resourceLocation)) {
                    return true;
                }
            }
            return false;
        }

        public boolean visit(IResourceDelta delta) throws CoreException {
            IResource resource = delta.getResource();
            if (resource == null || resource.getType() == IResource.PROJECT) {
                return true;
            }
            // resources in the output folders of the ipsProject and the
            // assigned java project are
            // ignored
            if (ignoredResource(resource)) {
                return false;
            }

            // only interested in IpsSrcFile changes
            IIpsElement element = IpsPlugin.getDefault().getIpsModel().getIpsElement(resource);
            if (!(element instanceof IIpsSrcFile)) {
                return true;
            }

            switch (delta.getKind()) {
                case IResourceDelta.ADDED:
                    if (element.exists()) {
                        changedAndAddedIpsSrcFiles.add(element);
                    }
                    return true;
                case IResourceDelta.REMOVED:
                    removedIpsSrcFiles.add(element);
                case IResourceDelta.CHANGED: {
                    // skip changes, not caused by content changes,
                    if (delta.getFlags() != 0 && element.exists()) {
                        changedAndAddedIpsSrcFiles.add(element);
                        return true;
                    }
                }
                    break;
            }
            return true;
        }
    }

    /*
     * The applyBuildCommand method of this class uses this interface.
     */
    private interface BuildCommand {
        public void build(IIpsArtefactBuilder builder, MultiStatus status) throws CoreException;
    }

    private class BeforeBuildProcessCommand implements BuildCommand {

        private int buildKind;

        public BeforeBuildProcessCommand(int buildKind) {
            this.buildKind = buildKind;
        }

        public void build(IIpsArtefactBuilder builder, MultiStatus status) throws CoreException {
            builder.beforeBuildProcess(getIpsProject(), buildKind);
        }

        public String toString() {
            return "BeforeBuildProcessCmd[kind=" + buildKind + "]"; //$NON-NLS-1$ //$NON-NLS-2$
        }

    }

    private class AfterBuildProcessCommand implements BuildCommand {

        private int buildKind;

        public AfterBuildProcessCommand(int buildKind) {
            this.buildKind = buildKind;
        }

        public void build(IIpsArtefactBuilder builder, MultiStatus status) throws CoreException {
            builder.afterBuildProcess(getIpsProject(), buildKind);
        }

        public String toString() {
            return "AfterBuildProcessCmd[kind=" + buildKind + "]"; //$NON-NLS-1$ //$NON-NLS-2$
        }
    }

    private static class BuildArtefactBuildCommand implements BuildCommand {

        private IIpsSrcFile ipsSrcFile;

        public BuildArtefactBuildCommand(IIpsSrcFile ipsSrcFile) {
            this.ipsSrcFile = ipsSrcFile;
        }

        public void build(IIpsArtefactBuilder builder, MultiStatus status) throws CoreException {
            if (builder.isBuilderFor(ipsSrcFile)) {
                long begin = 0;
                try {
                    if (TRACE_BUILDER_TRACE) {
                        begin = System.currentTimeMillis();
                        System.out.println(builder.getName() + ": Start building " + ipsSrcFile); //$NON-NLS-1$
                    }
                    builder.beforeBuild(ipsSrcFile, status);
                    builder.build(ipsSrcFile);
                } finally {
                    builder.afterBuild(ipsSrcFile);
                    if (TRACE_BUILDER_TRACE) {
                        System.out
                                .println(builder.getName()
                                        + ": Finished building " + ipsSrcFile + ". Duration: " + (System.currentTimeMillis() - begin)); //$NON-NLS-1$ //$NON-NLS-2$
                    }
                }
            }
        }

        public String toString() {
            return "Build file " + ipsSrcFile; //$NON-NLS-1$
        }
    }

    private static class DeleteArtefactBuildCommand implements BuildCommand {

        private IIpsSrcFile toDelete;

        public DeleteArtefactBuildCommand(IIpsSrcFile toDelete) {
            this.toDelete = toDelete;
        }

        public void build(IIpsArtefactBuilder builder, MultiStatus status) throws CoreException {
            if (builder.isBuilderFor(toDelete)) {
                builder.delete(toDelete);
            }
        }

        public String toString() {
            return "Delete file " + toDelete; //$NON-NLS-1$
        }

    }
}
