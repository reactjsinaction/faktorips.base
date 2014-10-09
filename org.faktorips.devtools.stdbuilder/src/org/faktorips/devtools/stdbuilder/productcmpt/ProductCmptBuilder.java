/*******************************************************************************
 * Copyright (c) Faktor Zehn AG. <http://www.faktorzehn.org>
 * 
 * This source code is available under the terms of the AGPL Affero General Public License version
 * 3.
 * 
 * Please see LICENSE.txt for full license terms, including the additional permissions and
 * restrictions as well as the possibility of alternative license terms.
 *******************************************************************************/

package org.faktorips.devtools.stdbuilder.productcmpt;

import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.MultiStatus;
import org.faktorips.devtools.core.builder.AbstractArtefactBuilder;
import org.faktorips.devtools.core.model.ipsobject.IIpsSrcFile;
import org.faktorips.devtools.core.model.ipsobject.IpsObjectType;
import org.faktorips.devtools.core.model.ipsproject.IIpsProject;
import org.faktorips.devtools.core.model.productcmpt.IProductCmpt;
import org.faktorips.devtools.core.model.productcmpt.IProductCmptGeneration;
import org.faktorips.devtools.core.model.productcmpt.IPropertyValueContainer;
import org.faktorips.devtools.stdbuilder.StandardBuilderSet;
import org.faktorips.devtools.stdbuilder.xpand.productcmpt.ProductCmptClassBuilder;
import org.faktorips.devtools.stdbuilder.xpand.productcmpt.ProductCmptGenerationClassBuilder;

/**
 * 
 */
public class ProductCmptBuilder extends AbstractArtefactBuilder {

    private MultiStatus buildStatus;
    private ProductCmptGenerationCuBuilder generationBuilder;
    private ProductCmptCuBuilder productCmptCuBuilder;

    public ProductCmptBuilder(StandardBuilderSet builderSet) {
        super(builderSet);
        productCmptCuBuilder = new ProductCmptCuBuilder(builderSet);
        generationBuilder = new ProductCmptGenerationCuBuilder(builderSet, productCmptCuBuilder);
    }

    @Override
    public StandardBuilderSet getBuilderSet() {
        return (StandardBuilderSet)super.getBuilderSet();
    }

    public void setProductCmptImplBuilder(ProductCmptClassBuilder builder) {
        productCmptCuBuilder.setProductCmptImplBuilder(builder);
        generationBuilder.setProductCmptImplBuilder(builder);
    }

    public void setProductCmptGenImplBuilder(ProductCmptGenerationClassBuilder builder) {
        generationBuilder.setProductCmptGenImplBuilder(builder);
    }

    public ProductCmptGenerationCuBuilder getGenerationBuilder() {
        return generationBuilder;
    }

    @Override
    public String getName() {
        return "ProductCmptBuilder"; //$NON-NLS-1$
    }

    @Override
    public void beforeBuildProcess(IIpsProject project, int buildKind) throws CoreException {
        super.beforeBuildProcess(project, buildKind);
        productCmptCuBuilder.beforeBuildProcess(project, buildKind);
        generationBuilder.beforeBuildProcess(project, buildKind);
    }

    @Override
    public void afterBuildProcess(IIpsProject project, int buildKind) throws CoreException {
        super.afterBuildProcess(project, buildKind);
        productCmptCuBuilder.afterBuildProcess(project, buildKind);
        generationBuilder.afterBuildProcess(project, buildKind);
    }

    @Override
    public boolean isBuilderFor(IIpsSrcFile ipsSrcFile) throws CoreException {
        return IpsObjectType.PRODUCT_CMPT.equals(ipsSrcFile.getIpsObjectType());
    }

    @Override
    public void beforeBuild(IIpsSrcFile ipsSrcFile, MultiStatus status) throws CoreException {
        super.beforeBuild(ipsSrcFile, status);
        buildStatus = status;
    }

    @Override
    public void build(IIpsSrcFile ipsSrcFile) throws CoreException {
        IProductCmpt productCmpt = (IProductCmpt)ipsSrcFile.getIpsObject();
        if (productCmpt.isValid(getIpsProject())) {
            if (requiresJavaCompilationUnit(productCmpt)) {
                build(productCmpt);
            }
            List<IProductCmptGeneration> generations = productCmpt.getProductCmptGenerations();
            for (IProductCmptGeneration generation : generations) {
                if (requiresJavaCompilationUnit(generation)) {
                    build(generation);
                }
            }
        }
    }

    private void build(IProductCmpt productCmpt) throws CoreException {
        productCmptCuBuilder.callBuildProcess(productCmpt, buildStatus);
    }

    private void build(IProductCmptGeneration generation) throws CoreException {
        generationBuilder.callBuildProcess(generation, buildStatus);
    }

    public String getImplementationClass(IProductCmpt productCmpt) throws CoreException {
        return generationBuilder.getImplementationClassProductCmpt(productCmpt);
    }

    public String getQualifiedClassName(IProductCmptGeneration generation) throws CoreException {
        return generationBuilder.getQualifiedClassNameOfProductCmpt(generation);
    }

    public String getQualifiedClassName(IProductCmpt productCmpt) throws CoreException {
        return productCmptCuBuilder.getQualifiedClassNameOfProductCmpt(productCmpt);
    }

    /**
     * Returns the Java sourcefile that is generated for the given {@link IPropertyValueContainer}
     * or <code>null</code> if no sourcefile is generated. In this case the product component or the
     * respective generation contain no formula.
     */
    public IFile getGeneratedJavaFile(IPropertyValueContainer container) throws CoreException {
        if (!requiresJavaCompilationUnit(container)) {
            return null;
        }
        return generationBuilder.getGeneratedJavaFile((IProductCmptGeneration)container);
    }

    private boolean requiresJavaCompilationUnit(IPropertyValueContainer container) throws CoreException {
        if (!container.isContainingAvailableFormula()) {
            return false;
        }
        if (container.findProductCmptType(container.getIpsProject()) == null) {
            return false;
        }
        return true;
    }

    @Override
    public void delete(IIpsSrcFile deletedFile) throws CoreException {
        // the problem here, is that the file is deleted and so we can't access the generations.
        // so we can get the exact file names, as the generation's valid from is part of the file
        // name
        // instead we delete all file that start with the common prefix.
        String prefix = generationBuilder.getJavaSrcFilePrefix(deletedFile);
        // get a file handle in the
        IFile file = generationBuilder.getJavaFile(deletedFile);
        // target folder
        IContainer folder = file.getParent();
        // now delete all files that start with the common
        IResource[] members = folder.members();
        // prefix
        for (IResource member : members) {
            if (member.getType() == IResource.FILE && member.getName().startsWith(prefix)) {
                member.delete(true, null);
            }
        }
    }

    @Override
    public boolean buildsDerivedArtefacts() {
        return true;
    }

    @Override
    public boolean isBuildingInternalArtifacts() {
        return getBuilderSet().isGeneratePublishedInterfaces();
    }

}
