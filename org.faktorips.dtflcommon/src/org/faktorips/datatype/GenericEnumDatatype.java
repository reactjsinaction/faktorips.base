/*******************************************************************************
 * Copyright (c) Faktor Zehn AG. <http://www.faktorzehn.org>
 * 
 * This source code is available under the terms of the AGPL Affero General Public License version
 * 3.
 * 
 * Please see LICENSE.txt for full license terms, including the additional permissions and
 * restrictions as well as the possibility of alternative license terms.
 *******************************************************************************/

package org.faktorips.datatype;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.lang.ObjectUtils;

/**
 * Generic enum datatype. See the superclass for more Details.
 * 
 * @author Jan Ortmann
 */
public abstract class GenericEnumDatatype extends GenericValueDatatype implements EnumDatatype {

    private Method getAllValuesMethod;

    private Method getNameMethod;

    private String getAllValuesMethodName = "getAllValues"; //$NON-NLS-1$

    private String getNameMethodName = "getName"; //$NON-NLS-1$

    private boolean isSupportingNames = false;
    private boolean cacheData = false;
    private String[] cachedValueIds = null;
    private String[] cachedValueNames = null;

    public GenericEnumDatatype() {
        super();
    }

    /**
     * Returns <code>true</code> if the values' id and names are cached, <code>false</code>
     * otherwise.
     */
    public boolean isCacheData() {
        return cacheData;
    }

    /**
     * Sets to <code>true</code> if the values' ids and names should be cached, otherwise
     * <code>false</code>. Setting <code>false</code> also clears the cache.
     */
    public void setCacheData(boolean cacheData) {
        this.cacheData = cacheData;
        if (!cacheData) {
            clearCache();
        }
    }

    public String getGetAllValuesMethodName() {
        return getAllValuesMethodName;
    }

    public void setGetAllValuesMethodName(String getAllValuesMethodName) {
        this.getAllValuesMethodName = getAllValuesMethodName;
        getAllValuesMethod = null;
        clearCache();
    }

    /**
     * Returns the name of the getName(String) method.
     */
    public String getGetNameMethodName() {
        return getNameMethodName;
    }

    /**
     * Sets the name of the getName(String) method.
     */
    public void setGetNameMethodName(String getNameMethodName) {
        getNameMethod = null;
        this.getNameMethodName = getNameMethodName;
        clearCache();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSupportingNames() {
        return isSupportingNames;
    }

    public void setIsSupportingNames(boolean isSupportingNames) {
        this.isSupportingNames = isSupportingNames;
        clearCache();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getAllValueIds(boolean includeNull) {
        try {
            String[] ids = getAllValueIdsFromCache();
            // caching disabled
            if (ids == null) {
                ids = getAllValueIdsFromClass();
            }
            int indexOfNull = getIndeoxOfNullOrNullObject(ids);
            ArrayList<String> result = new ArrayList<String>();
            result.addAll(Arrays.asList(ids));
            if (!includeNull && indexOfNull >= 0) {
                result.remove(indexOfNull);
            } else if (includeNull && indexOfNull == -1) {
                if (hasNullObject()) {
                    result.add(getNullObjectId());
                } else {
                    result.add(null);
                }
            }
            return result.toArray(new String[result.size()]);
            // CSOFF: Illegal Catch
        } catch (Exception e) {
            // CSON: Illegal Catch
            throw new RuntimeException("Error invoking method " + getAllValuesMethodName, e); //$NON-NLS-1$
        }
    }

    /**
     * Returns the value id's from the underlying enum class' via it's getAllValuesMethod().
     * 
     * throws IllegalArgumentException
     */
    private String[] getAllValueIdsFromClass() throws IllegalAccessException, InvocationTargetException {
        Object[] values = (Object[])getGetAllValuesMethod().invoke(null, new Object[0]);
        String[] ids = new String[values.length];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = valueToString(values[i]);
        }
        return ids;
    }

    private int getIndeoxOfNullOrNullObject(String[] valueIds) {
        for (int i = 0; i < valueIds.length; i++) {
            if (valueIds[i] == null) {
                return i;
            }
            if (hasNullObject() && ObjectUtils.equals(valueIds[i], getNullObjectId())) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns the method to get all enum values from the adapted class.
     * 
     * @hrows RuntimeException if the method can't be found.
     */
    public Method getGetAllValuesMethod() {
        if (getAllValuesMethod == null && getAllValuesMethodName != null) {
            try {
                getAllValuesMethod = getAdaptedClass().getMethod(getAllValuesMethodName, new Class[0]);
                if (getAllValuesMethod == null) {
                    throw new NullPointerException();
                }
                // CSOFF: Illegal Catch
            } catch (Exception e) {
                // CSON: Illegal Catch
                throw new RuntimeException("Can't get method getAllValues(), Class: " + getAdaptedClass() //$NON-NLS-1$
                        + ", Methodname: " + getAllValuesMethodName); //$NON-NLS-1$
            }
        }
        return getAllValuesMethod;
    }

    /**
     * Returns the method to get the name for a given valueId from the adapted class.
     * 
     * @hrows RuntimeException if the method can't be found.
     */
    public Method getGetNameMethod() {
        if (getNameMethod == null && getNameMethodName != null) {
            try {
                getNameMethod = getAdaptedClass().getMethod(getNameMethodName, new Class[0]);
                // CSOFF: Illegal Catch
            } catch (Exception e) {
                // CSON: Illegal Catch
                throw new RuntimeException("Unable to access the method " + getNameMethodName //$NON-NLS-1$
                        + " on the adapted class " + getAdaptedClass(), e); //$NON-NLS-1$
            }
        }
        return getNameMethod;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getValueName(String id) {
        if (!isSupportingNames) {
            throw new UnsupportedOperationException(
                    "This enumeration type does not support a getName(String) method, enumeration type class: " //$NON-NLS-1$
                            + getAdaptedClass());
        }
        String[] ids = getAllValueIdsFromCache();
        if (ids != null) {
            for (int i = 0; i < ids.length; i++) {
                if (ObjectUtils.equals(id, ids[i])) {
                    String[] names = getAllValueNamesFromCache();
                    return names[i];
                }
            }
        }
        return getValueNameFromClass(id);
    }

    private String getValueNameFromClass(String id) {
        try {
            Object value = getValue(id);
            if (value == null) {
                return null;
            }
            return (String)getGetNameMethod().invoke(value, new Object[0]);

            // CSOFF: Illegal Catch
        } catch (Exception e) {
            // CSON: Illegal Catch
            throw new RuntimeException("Unable to invoke the method to get the value name " + getNameMethodName //$NON-NLS-1$
                    + " on the class: " + getAdaptedClass(), e); //$NON-NLS-1$
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isParsable(String value) {
        if (value == null) {
            return true;
        }
        String[] ids = getAllValueIdsFromCache();
        if (ids == null) {
            return super.isParsable(value);
        }
        for (String id : ids) {
            if (value.equals(id)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns all value ids from the cache. If caching is enabled, but the cache is empty, the
     * cahce is populated with the date. If caching is disabled, the method returns
     * <code>null</code>.
     * <p>
     * Package private to allows testing.
     */
    String[] getAllValueIdsFromCache() {
        if (!cacheData) {
            return null;
        }
        if (cachedValueIds == null) {
            initCache();
        }
        return cachedValueIds;
    }

    /**
     * Returns all value names from the cache. If caching is enabled, but the cache is empty, the
     * cahce is populated with the date. If caching is disabled, the method returns
     * <code>null</code>.
     * <p>
     * Package private to allows testing.
     */
    String[] getAllValueNamesFromCache() {
        if (!isSupportingNames) {
            throw new RuntimeException("Datatype " + this + " does not support names."); //$NON-NLS-1$ //$NON-NLS-2$
        }
        if (!cacheData) {
            return null;
        }
        if (cachedValueNames == null) {
            initCache();
        }
        return cachedValueNames;
    }

    /**
     * Initializes the cache with the data from the underlying class.
     */
    public void initCache() {
        String[] ids;
        try {
            ids = getAllValueIdsFromClass();
            // CSOFF: Illegal Catch
        } catch (Exception e) {
            // CSON: Illegal Catch
            throw new RuntimeException("Error initializing cache for datatype " + this, e); //$NON-NLS-1$
        }
        cachedValueIds = new String[ids.length];
        if (isSupportingNames) {
            cachedValueNames = new String[ids.length];
        }
        for (int i = 0; i < ids.length; i++) {
            cachedValueIds[i] = ids[i];
            if (isSupportingNames) {
                cachedValueNames[i] = getValueNameFromClass(ids[i]);
            }
        }
    }

    @Override
    protected void clearCache() {
        super.clearCache();
        getAllValuesMethod = null;
        cachedValueIds = null;
        cachedValueNames = null;
    }

}
