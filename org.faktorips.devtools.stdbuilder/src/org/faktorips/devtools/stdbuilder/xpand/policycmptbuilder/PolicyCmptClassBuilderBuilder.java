/*******************************************************************************
 * Copyright (c) Faktor Zehn AG. <http://www.faktorzehn.org>
 * 
 * This source code is available under the terms of the AGPL Affero General Public License version
 * 3.
 * 
 * Please see LICENSE.txt for full license terms, including the additional permissions and
 * restrictions as well as the possibility of alternative license terms.
 *******************************************************************************/
package org.faktorips.devtools.stdbuilder.xpand.policycmptbuilder;

import org.eclipse.core.runtime.CoreException;
import org.faktorips.devtools.core.builder.naming.IJavaClassNameProvider;
import org.faktorips.devtools.core.exception.CoreRuntimeException;
import org.faktorips.devtools.core.model.ipsobject.IIpsObject;
import org.faktorips.devtools.core.model.ipsobject.IIpsObjectPartContainer;
import org.faktorips.devtools.core.model.ipsobject.IIpsSrcFile;
import org.faktorips.devtools.core.model.ipsobject.IpsObjectType;
import org.faktorips.devtools.core.model.pctype.IPolicyCmptType;
import org.faktorips.devtools.stdbuilder.StandardBuilderSet;
import org.faktorips.devtools.stdbuilder.xpand.GeneratorModelContext;
import org.faktorips.devtools.stdbuilder.xpand.TypeBuilder;
import org.faktorips.devtools.stdbuilder.xpand.model.ModelService;
import org.faktorips.devtools.stdbuilder.xpand.policycmptbuilder.model.XPolicyBuilder;
import org.faktorips.util.LocalizedStringsSet;

public class PolicyCmptClassBuilderBuilder extends TypeBuilder<XPolicyBuilder> {

    private IJavaClassNameProvider javaClassNameProvider;

    public PolicyCmptClassBuilderBuilder(StandardBuilderSet builderSet, GeneratorModelContext modelContext,
            ModelService modelService) {
        super(false, builderSet, modelContext, modelService, new LocalizedStringsSet(
                PolicyCmptClassBuilderBuilder.class));
        javaClassNameProvider = new BuilderJavaClassNameProvider();
    }

    @Override
    public IJavaClassNameProvider getJavaClassNameProvider() {
        return javaClassNameProvider;
    }

    @Override
    public boolean isBuilderFor(IIpsSrcFile ipsSrcFile) throws CoreException {
        String configProperty = getBuilderSet().getConfig().getPropertyValueAsString(
                StandardBuilderSet.CONFIG_PROPERTY_BUILDER_GENERATOR);
        if (StandardBuilderSet.CONFIG_PROPERTY_BUILDER_GENERATOR_ALL.equals(configProperty)
                || StandardBuilderSet.CONFIG_PROPERTY_BUILDER_GENERATOR_POLICY.equals(configProperty)) {
            return IpsObjectType.POLICY_CMPT_TYPE.equals(ipsSrcFile.getIpsObjectType());
        } else {
            return false;
        }
    }

    @Override
    protected String getTemplate() {
        return "org::faktorips::devtools::stdbuilder::xpand::policycmptbuilder::template::PolicyBuilder::main";
    }

    @Override
    protected Class<XPolicyBuilder> getGeneratorModelNodeClass() {
        return XPolicyBuilder.class;
    }

    @Override
    public boolean isGenerateingArtifactsFor(IIpsObjectPartContainer ipsObjectPartContainer) {
        try {
            return isBuilderFor(ipsObjectPartContainer.getIpsSrcFile());
        } catch (CoreException e) {
            throw new CoreRuntimeException(e);
        }
    }

    @Override
    protected IIpsObject getSupportedIpsObject(IIpsObjectPartContainer ipsObjectPartContainer) {
        IIpsObject ipsObject = ipsObjectPartContainer.getIpsObject();
        if (ipsObject instanceof IPolicyCmptType) {
            return ipsObject;
        } else {
            return null;
        }
    }

    @Override
    public boolean isBuildingPublishedSourceFile() {
        return true;
    }

}
