/*******************************************************************************
 * Copyright (c) Faktor Zehn AG. <http://www.faktorzehn.org>
 * 
 * This source code is available under the terms of the AGPL Affero General Public License version
 * 3.
 * 
 * Please see LICENSE.txt for full license terms, including the additional permissions and
 * restrictions as well as the possibility of alternative license terms.
 *******************************************************************************/

package org.faktorips.runtime;

import org.faktorips.runtime.internal.ProductComponent;

/**
 * A product component generation represents the state of a product component valid for a period of
 * time. The period's begins is defined by the generation's valid from date. The period ends at the
 * next generation's valid from date. A product component's generation periods are none overlapping.
 * For a given point in time exactly one (or none) generation is found.
 * 
 * @author Jan Ortmann
 */
public interface IProductComponentGeneration extends IRuntimeObject, IProductObject {

    /**
     * Returns the repository this product component generation belongs to. This method never
     * returns <code>null</code>.
     */
    @Override
    public IRuntimeRepository getRepository();

    /**
     * Returns the product component this generation belongs to. This method never returns
     * <code>null</code>.
     */
    IProductComponent getProductComponent();

    /**
     * Returns the previous generation if available if not <code>null</code> will be returned.
     */
    public IProductComponentGeneration getPreviousGeneration();

    /**
     * Returns the next generation if available if not <code>null</code> will be returned.
     */
    public IProductComponentGeneration getNextGeneration();

    /**
     * Returns whether the validation rule with the given name is configured as active in this
     * {@link ProductComponent}.
     * 
     * @param ruleName the name of the rule in question
     * @return <code>true</code> if the rule was activated, <code>false</code> else.
     */
    public boolean isValidationRuleActivated(String ruleName);

    /**
     * Enables or disables validation for a specific rule.
     * 
     * @param ruleName the name of the rule in question
     * @param active indicating whether the validation rule is configured as active
     */
    public void setValidationRuleActivated(String ruleName, boolean active);

}
