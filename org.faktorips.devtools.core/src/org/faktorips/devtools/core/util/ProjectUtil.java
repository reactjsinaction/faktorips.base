/*******************************************************************************
 * Copyright (c) 2005-2012 Faktor Zehn AG und andere.
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

package org.faktorips.devtools.core.util;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.faktorips.devtools.core.IpsClasspathContainerInitializer;
import org.faktorips.devtools.core.IpsPlugin;
import org.faktorips.devtools.core.Util;
import org.faktorips.devtools.core.internal.model.productcmpt.DateBasedProductCmptNamingStrategy;
import org.faktorips.devtools.core.model.IIpsModel;
import org.faktorips.devtools.core.model.ipsproject.IIpsArtefactBuilderSetConfigModel;
import org.faktorips.devtools.core.model.ipsproject.IIpsArtefactBuilderSetInfo;
import org.faktorips.devtools.core.model.ipsproject.IIpsObjectPath;
import org.faktorips.devtools.core.model.ipsproject.IIpsProject;
import org.faktorips.devtools.core.model.ipsproject.IIpsProjectProperties;
import org.faktorips.devtools.core.model.ipsproject.IIpsSrcFolderEntry;
import org.faktorips.devtools.core.model.ipsproject.ISupportedLanguage;

/**
 * Utilities for the creation and modification of projects.
 * 
 * @author Thorsten Günther
 */
public class ProjectUtil {

    // TODO CODE-REVIEW PA-559: nicht verwendete Konstante
    // character and numbers without special character, white space and umlauts
    private static final Pattern NAME_PATTERN = Pattern.compile("\\w+"); //$NON-NLS-1$

    /**
     * Adds the Faktor-IPS nature to the given project.
     * 
     * @param project A platform project.
     * 
     * @throws NullPointerException If project is <code>null</code>.
     */
    public final static void addIpsNature(IProject project) throws CoreException {
        Util.addNature(project, IIpsProject.NATURE_ID);
    }

    /**
     * Returns <code>true</code> if the given Java project has the Faktor-IPS Nature, otherwise
     * <code>false</code>.
     * 
     * @param project A platform project.
     */
    public final static boolean hasIpsNature(IJavaProject project) throws CoreException {
        return hasIpsNature(project.getProject());
    }

    /**
     * Returns <code>true</code> if the given project has the Faktor-IPS Nature, otherwise
     * <code>false</code>.
     * 
     * @param project A platform project.
     */
    public final static boolean hasIpsNature(IProject project) throws CoreException {
        return project.getDescription().hasNature(IIpsProject.NATURE_ID);
    }

    /**
     * Disable a feature of the default IPS builder.
     * 
     * @param ipsProject The project to disable the feature at.
     * @param featureName The name of the feature to disable.
     * 
     * @throws CoreException if an error occurs while saving the properties to the file.
     */
    public static void disableBuilderFeature(IIpsProject ipsProject, String featureName) throws CoreException {
        IIpsProjectProperties props = ipsProject.getProperties();
        IIpsArtefactBuilderSetConfigModel configModel = props.getBuilderSetConfig();
        String desc = configModel.getPropertyDescription(featureName);
        configModel.setPropertyValue(featureName, Boolean.FALSE.toString(), desc);
        props.setBuilderSetConfig(configModel);
        ipsProject.setProperties(props);
    }

    /**
     * Creates and returns an IPS project based on the given Java project. This project does not use
     * the persistence feature.
     * 
     * @deprecated As of release 2.6, replaced by
     *             {@link #createIpsProject(IJavaProject, boolean, boolean, boolean, String, IFolder, IFolder, IFolder)}
     * 
     * @param javaProject The Java project to use as base for the IPS project
     * @param runtimeIdPrefix The prefix for the runtime IDs to be used in the new project.
     * @param mergableFolder The source folder for mergable Java files.
     * @param derivedFolder The source folder for derived Java files.
     * @param srcFolder The source folder for IPS objects.
     * @param isProductDefinitionProject <code>true</code> to create a project which is capable of
     *            product definitions.
     * @param isModelProject <code>true</code> to create a project which is capable of model
     *            objects.
     * 
     * @throws CoreException In case of any errors.
     */
    @Deprecated
    public static IIpsProject createIpsProject(IJavaProject javaProject,
            boolean isProductDefinitionProject,
            boolean isModelProject,
            String runtimeIdPrefix,
            IFolder mergableFolder,
            IFolder derivedFolder,
            IFolder srcFolder) throws CoreException {

        return createIpsProject(javaProject, isProductDefinitionProject, isModelProject, false, runtimeIdPrefix,
                mergableFolder, derivedFolder, srcFolder);
    }

    /**
     * Creates and returns an IPS project based on the given Java project.
     * 
     * @param javaProject The Java project to use as base for the IPS project.
     * @param runtimeIdPrefix The prefix for the runtime IDs to be used in the new project.
     * @param mergableFolder The source folder for mergable Java files.
     * @param derivedFolder The source folder for derived Java files.
     * @param srcFolder The source folder for IPS objects.
     * @param isProductDefinitionProject <code>true</code> to create a project which is capable of
     *            product definitions.
     * @param isModelProject <code>true</code> to create a project which is capable of model
     *            objects.
     * 
     * @throws CoreException In case of any errors.
     * 
     * @since 2.6
     */
    public static IIpsProject createIpsProject(IJavaProject javaProject,
            boolean isProductDefinitionProject,
            boolean isModelProject,
            boolean isPersistentProject,
            String runtimeIdPrefix,
            IFolder mergableFolder,
            IFolder derivedFolder,
            IFolder srcFolder) throws CoreException {

        IIpsProject ipsProject = createIpsProject(javaProject, runtimeIdPrefix, isProductDefinitionProject,
                isModelProject, isPersistentProject);

        createIpsSourceFolderEntry(ipsProject, srcFolder.getName(), mergableFolder, derivedFolder);

        return ipsProject;
    }

    /**
     * Creates and returns a new project.
     * 
     * @param projectName The name for the new project.
     * 
     * @throws CoreException if the creation of the project fails. See
     *             {@link IProject#create(org.eclipse.core.runtime.IProgressMonitor)} for details.
     */
    public static IProject createPlatformProject(String projectName) throws CoreException {
        IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
        project.create(new NullProgressMonitor());
        project.open(new NullProgressMonitor());
        return project;
    }

    /**
     * Creates a new folder root folder and returns a handle to it.
     * 
     * @param project The project to create the folder in.
     * @param folderName The name of the folder.
     * 
     * @throws CoreException If the creation of the folder fails. See
     *             {@link IFolder#create(boolean, boolean, org.eclipse.core.runtime.IProgressMonitor)}
     *             for details.
     */
    public static IFolder createFolder(IProject project, String folderName) throws CoreException {
        IFolder folder = project.getFolder(folderName);
        if (!folder.exists()) {
            folder.create(true, true, new NullProgressMonitor());
        }
        return folder;
    }

    /**
     * Create a new folder and adds it as additional source folder entry to the IPS project.
     * 
     * @param ipsProject The project to create the folder in and to add the source folder entry to.
     * @param folderName The name of the new folder.
     * @param outputFolderForMergableJavaFiles The folder for mergable Java files.
     * @param outputFolderForDerivedJavaFiles The folder for derived Java files.
     * 
     * @throws CoreException if the creation of the folder fails or if the IPSObjectPath could not
     *             be set.
     */
    public static IFolder createIpsSourceFolderEntry(IIpsProject ipsProject,
            String folderName,
            IFolder outputFolderForMergableJavaFiles,
            IFolder outputFolderForDerivedJavaFiles) throws CoreException {

        IFolder srcFolder = createFolder(ipsProject.getProject(), folderName);

        IIpsObjectPath path = ipsProject.getIpsObjectPath();
        path.setOutputDefinedPerSrcFolder(true);

        if (path.containsSrcFolderEntry(srcFolder)) {
            path.removeSrcFolderEntry(srcFolder);
        }
        String packageName = getValidProjectName(ipsProject.getName()) + "." + folderName; //$NON-NLS-1$

        IIpsSrcFolderEntry entry = path.newSourceFolderEntry(srcFolder);
        entry.setSpecificBasePackageNameForMergableJavaClasses(packageName);
        entry.setSpecificOutputFolderForMergableJavaFiles(outputFolderForMergableJavaFiles);
        entry.setSpecificBasePackageNameForDerivedJavaClasses(packageName);
        entry.setSpecificOutputFolderForDerivedJavaFiles(outputFolderForDerivedJavaFiles);

        ipsProject.setIpsObjectPath(path);

        return srcFolder;
    }

    /**
     * <p>
     * Any invalid character will be transformed to an underscore.
     * <p>
     * Note that this is not part of an official naming convention. We defined this because code
     * generator generated project name than package name part.
     */
    /*
     * TODO CODE-REVIEW PA-559: Es geht hier eigentlich nicht um einen validen Projektnamen. Als
     * Projektnamen ist nach wie vor alles erlaubt. Der Code ist dupliziert aus
     * JavaNamingConvention#getEnumLiteral. Diese Methode sollte verschoben werden nach
     * JavaNamingConvention und dort heißen: getValidJavaIdentifier(String). Bitte noch in den
     * JavaDoc ein @see ValidationUtils#validateJavaIdentifier(String, IIpsProject) aufnehmen. Die
     * Methode dann auch gleich in JavaNamingConvention#getEnumLiteral verwenden.
     */
    protected static String getValidProjectName(String projectName) {

        // Replace characters that are not valid for Java packages name
        char[] characters = projectName.toCharArray();
        for (int i = 0; i < characters.length; i++) {
            if ((i == 0 && !Character.isJavaIdentifierStart(characters[i]))
                    || (i > 0 && !Character.isJavaIdentifierPart(characters[i]))) {
                characters[i] = '_';
            }
        }

        return String.valueOf(characters);
    }

    /**
     * Add a reference between the two projects.
     * 
     * @param referringProject The project referring the other.
     * @param referencedProject The project referred by the first one.
     * 
     * @throws CoreException If the IPS object path could not be set accordingly.
     */
    public static void addProjectReference(IIpsProject referringProject, IIpsProject referencedProject)
            throws CoreException {

        IIpsObjectPath ipsObjPath = referringProject.getIpsObjectPath();
        ipsObjPath.newIpsProjectRefEntry(referencedProject);
        referringProject.setIpsObjectPath(ipsObjPath);
    }

    /**
     * Creates and returns an <tt>IIpsProject</tt> based on the given <tt>IJavaProject</tt>. This
     * project does not use the persistence feature.
     * 
     * @deprecated As of release 2.6, replaced by
     *             {@link #createIpsProject(IJavaProject, String, boolean, boolean, boolean)}.
     * 
     * @param javaProject The <tt>IJavaProject</tt> which is to be extended with IPS capabilities.
     * @param runtimeIdPrefix The prefix for runtime IDs to be used in this project.
     * @param isProductDefinitionProject Must be <code>true</code> if this is a product definition
     *            project.
     * @param isModelProject Must be <code>true</code> if this is a model project.
     * 
     * @throws CoreException In case of any errors.
     */
    @Deprecated
    public static IIpsProject createIpsProject(IJavaProject javaProject,
            String runtimeIdPrefix,
            boolean isProductDefinitionProject,
            boolean isModelProject) throws CoreException {
        return createIpsProject(javaProject, runtimeIdPrefix, isProductDefinitionProject, isModelProject, false);
    }

    /**
     * Creates and returns an <tt>IIpsProject</tt> based on the given <tt>IJavaProject</tt>.
     * 
     * @param javaProject The <tt>IJavaProject</tt> which is to be extended with IPS capabilities.
     * @param runtimeIdPrefix The prefix for runtime IDs to be used in this project.
     * @param isProductDefinitionProject Must be <code>true</code> if this is a product definition
     *            project.
     * @param isModelProject Must be <code>true</code> if this is a model project.
     * 
     * @throws CoreException In case of any Errors.
     * 
     * @since 2.6
     * 
     * @deprecated Use
     *             {@link #createIpsProject(IJavaProject, String, boolean, boolean, boolean, List)}
     *             instead.
     */
    // Deprecated since 3.1
    @Deprecated
    public static IIpsProject createIpsProject(IJavaProject javaProject,
            String runtimeIdPrefix,
            boolean isProductDefinitionProject,
            boolean isModelProject,
            boolean isPersistentProject) throws CoreException {

        List<Locale> supportedLocales = new ArrayList<Locale>(1);
        supportedLocales.add(IpsPlugin.getMultiLanguageSupport().getLocalizationLocale());
        return createIpsProject(javaProject, runtimeIdPrefix, isProductDefinitionProject, isModelProject,
                isPersistentProject, supportedLocales);
    }

    /**
     * Creates and returns an <tt>IIpsProject</tt> based on the given <tt>IJavaProject</tt>.
     * 
     * @param javaProject The <tt>IJavaProject</tt> which is to be extended with IPS capabilities.
     * @param runtimeIdPrefix The prefix for runtime IDs to be used in this project.
     * @param isProductDefinitionProject Must be <code>true</code> if this is a product definition
     *            project.
     * @param isModelProject Must be <code>true</code> if this is a model project.
     * @param isPersistentProject Must be true if persistence support should be enabled for the
     *            project
     * @param supportedLocales List of locales that will reflect the languages supported by the
     *            project
     * 
     * @throws CoreException In case of any Errors.
     * 
     * @since 3.1
     */
    public static IIpsProject createIpsProject(IJavaProject javaProject,
            String runtimeIdPrefix,
            boolean isProductDefinitionProject,
            boolean isModelProject,
            boolean isPersistentProject,
            List<Locale> supportedLocales) throws CoreException {

        addIpsRuntimeLibraries(javaProject);
        IIpsModel ipsModel = IpsPlugin.getDefault().getIpsModel();
        IIpsProject ipsProject = ipsModel.createIpsProject(javaProject);
        IIpsProjectProperties props = ipsProject.getProperties();
        props.setRuntimeIdPrefix(runtimeIdPrefix);
        props.setProductDefinitionProject(isProductDefinitionProject);
        props.setModelProject(isModelProject);
        props.setPersistenceSupport(isPersistentProject);

        // use the first registered builder set info as default
        IIpsArtefactBuilderSetInfo[] builderSetInfos = ipsModel.getIpsArtefactBuilderSetInfos();
        props.setBuilderSetId(builderSetInfos.length > 0 ? builderSetInfos[0].getBuilderSetId() : ""); //$NON-NLS-1$

        props.setPredefinedDatatypesUsed(ipsModel.getPredefinedValueDatatypes());
        DateBasedProductCmptNamingStrategy namingStrategy = new DateBasedProductCmptNamingStrategy(" ", "yyyy-MM", true); //$NON-NLS-1$ //$NON-NLS-2$
        props.setProductCmptNamingStrategy(namingStrategy);
        props.setMinRequiredVersionNumber(
                "org.faktorips.feature", (String)Platform.getBundle("org.faktorips.devtools.core").getHeaders().get("Bundle-Version")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        props.setChangesOverTimeNamingConventionIdForGeneratedCode(IpsPlugin.getDefault().getIpsPreferences()
                .getChangesOverTimeNamingConvention().getId());
        IIpsArtefactBuilderSetInfo builderSetInfo = ipsModel.getIpsArtefactBuilderSetInfo(props.getBuilderSetId());
        if (builderSetInfo != null) {
            props.setBuilderSetConfig(builderSetInfo.createDefaultConfiguration(ipsProject));
        }

        for (int i = 0; i < supportedLocales.size(); i++) {
            Locale locale = supportedLocales.get(i);
            props.addSupportedLanguage(locale);
            if (i == 0) {
                props.setDefaultLanguage(locale);
            }
        }

        setDefaultFunctionsLanguageLocale(props);

        ipsProject.setProperties(props);

        return ipsProject;
    }

    private static void setDefaultFunctionsLanguageLocale(IIpsProjectProperties properties) {
        ISupportedLanguage defaultLanguage = properties.getDefaultLanguage();
        if (defaultLanguage != null && defaultLanguage.getLocale().getLanguage().equals(Locale.GERMAN.getLanguage())) {
            properties.setFormulaLanguageLocale(Locale.GERMAN);
        } else {
            properties.setFormulaLanguageLocale(Locale.ENGLISH);
        }
    }

    private static void addIpsRuntimeLibraries(IJavaProject javaProject) throws JavaModelException {
        IClasspathEntry[] oldEntries = javaProject.getRawClasspath();
        if (targetVersionIsAtLeast5(javaProject)) {
            IClasspathEntry[] entries = new IClasspathEntry[oldEntries.length + 1];
            System.arraycopy(oldEntries, 0, entries, 0, oldEntries.length);
            entries[oldEntries.length] = JavaCore.newContainerEntry(IpsClasspathContainerInitializer.ENTRY_PATH);
            javaProject.setRawClasspath(entries, null);
        }
    }

    private static boolean targetVersionIsAtLeast5(IJavaProject javaProject) {
        String[] targetVersion = javaProject.getOption(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, true).split("\\."); //$NON-NLS-1$
        return (Integer.parseInt(targetVersion[0]) == 1 && Integer.parseInt(targetVersion[1]) >= 5)
                || Integer.parseInt(targetVersion[0]) > 1;
    }

    /**
     * Creates a hidden file <code>.keepme</code> in the given folder. Returns <tt>true</tt> if done
     * successfully, <tt>false</tt> otherwise.
     * 
     * @param folder parent folder
     */
    public static boolean createKeepMeFile(IFolder folder) {
        IFile outFile = folder.getFile(".keepme"); //$NON-NLS-1$
        String content = "// force e.g. CVS to keep empty folders"; //$NON-NLS-1$
        byte[] stringBytes = content.getBytes();
        ByteArrayInputStream bais = new ByteArrayInputStream(stringBytes);
        try {
            outFile.create(bais, true, new NullProgressMonitor());
        } catch (CoreException e) {
            IpsPlugin.log(e);
            return false;
        }
        return true;
    }

    private ProjectUtil() {
        // Utility class not to be instantiated.
    }

}
