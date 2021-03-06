/*******************************************************************************
 * Copyright (c) Faktor Zehn AG. <http://www.faktorzehn.org>
 * 
 * This source code is available under the terms of the AGPL Affero General Public License version
 * 3.
 * 
 * Please see LICENSE.txt for full license terms, including the additional permissions and
 * restrictions as well as the possibility of alternative license terms.
 *******************************************************************************/

package org.faktorips.runtime.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.anySetOf;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.faktorips.runtime.IRuntimeRepository;
import org.faktorips.runtime.IRuntimeRepositoryLookup;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AbstractRuntimeRepositoryTest {

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private AbstractRuntimeRepository repositoryA;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private AbstractRuntimeRepository repositoryB;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private AbstractRuntimeRepository repositoryC;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private AbstractRuntimeRepository repositoryD;

    @Test
    public void testGetEnumValuesDefinedInType() {
        AbstractRuntimeRepository abstractRuntimeRepository = mock(AbstractRuntimeRepository.class, CALLS_REAL_METHODS);

        List<EnumTestClass> enumValues = abstractRuntimeRepository.getEnumValuesDefinedInType(EnumTestClass.class);

        assertEquals(EnumTestClass.VALUES, enumValues);
    }

    @Test
    public void testGetEnumValuesReferencedContent() throws Exception {
        EnumTestClass myEnum = mock(EnumTestClass.class);
        List<EnumTestClass> list = new ArrayList<EnumTestClass>();
        list.add(myEnum);
        initRepositoryReferences(repositoryA, repositoryB, repositoryC, repositoryD);
        doReturn(list).when(repositoryD).getEnumValuesInternal(EnumTestClass.class);
        List<EnumTestClass> expected = new ArrayList<EnumTestClass>(EnumTestClass.VALUES);
        expected.addAll(list);

        List<EnumTestClass> enumValues = repositoryA.getEnumValues(EnumTestClass.class);

        assertEquals(expected, enumValues);
    }

    @Test
    public void testGetEnumValuesNoContent() throws Exception {
        initRepositoryReferences(repositoryA, repositoryB, repositoryC, repositoryD);
        List<EnumTestClass> expected = new ArrayList<EnumTestClass>(EnumTestClass.VALUES);

        List<EnumTestClass> enumValues = repositoryA.getEnumValues(EnumTestClass.class);

        assertEquals(expected, enumValues);
    }

    @Test
    public void testGetAllModelTypeImplementationClasses() throws Exception {
        initRepositoryReferences(repositoryA, repositoryB);

        Set<String> modelTypeImplementationClasses = repositoryA.getAllModelTypeImplementationClasses();

        assertNotNull(modelTypeImplementationClasses);
        verify(repositoryA).getAllModelTypeImplementationClasses(anySetOf(String.class));
        verify(repositoryB).getAllModelTypeImplementationClasses(anySetOf(String.class));
    }

    private void initRepositoryReferences(AbstractRuntimeRepository referencingRepository,
            AbstractRuntimeRepository... referencedRepositories) throws Exception {
        Field declaredField = AbstractRuntimeRepository.class.getDeclaredField("repositories");
        declaredField.setAccessible(true);
        declaredField.set(referencingRepository, new ArrayList<IRuntimeRepository>());
        mockRepository(referencingRepository);

        for (AbstractRuntimeRepository referencedRepository : referencedRepositories) {
            declaredField.set(referencedRepository, new ArrayList<IRuntimeRepository>());
            referencingRepository.addDirectlyReferencedRepository(referencedRepository);
            mockRepository(referencedRepository);
        }

    }

    private void mockRepository(AbstractRuntimeRepository repository) {
        doReturn(null).when(repository).getEnumValueLookupService(EnumTestClass.class);
        doReturn(null).when(repository).getEnumValuesInternal(EnumTestClass.class);
        doNothing().when(repository).getAllModelTypeImplementationClasses(anySetOf(String.class));
    }

    @Test
    public void testSetGetRuntimeRepositoryLookup() {
        IRuntimeRepositoryLookup repositoryLookupMock = mock(IRuntimeRepositoryLookup.class);
        repositoryA.setRuntimeRepositoryLookup(repositoryLookupMock);

        IRuntimeRepositoryLookup runtimeRepositoryLookup = repositoryA.getRuntimeRepositoryLookup();

        assertSame(repositoryLookupMock, runtimeRepositoryLookup);
    }

    public static class EnumTestClass {

        public static final EnumTestClass VALUE1 = new EnumTestClass();

        public static final EnumTestClass VALUE2 = new EnumTestClass();

        public static final List<EnumTestClass> VALUES = Arrays.asList(VALUE1, VALUE2);

    }

}
