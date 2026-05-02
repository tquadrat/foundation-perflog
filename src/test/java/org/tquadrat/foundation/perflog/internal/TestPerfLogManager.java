/*
 * ============================================================================
 * Copyright © 2002-2026 by Thomas Thrien.
 * All Rights Reserved.
 * ============================================================================
 *
 * Licensed to the public under the agreements of the GNU Lesser General Public
 * License, version 3.0 (the "License"). You may obtain a copy of the License at
 *
 *      http://www.gnu.org/licenses/lgpl.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.tquadrat.foundation.perflog.internal;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static javax.management.MBeanServerFactory.createMBeanServer;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.tquadrat.foundation.lang.CommonConstants.EMPTY_STRING;
import static org.tquadrat.foundation.perflog.PerfLogUtils.DOMAIN_NAME;
import static org.tquadrat.foundation.perflog.PerfLogUtils.createPerformanceSectionName;
import static org.tquadrat.foundation.perflog.PerfLogUtils.getPerfLogMBeanObjectName;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.tquadrat.foundation.annotation.ClassVersion;
import org.tquadrat.foundation.exception.BlankArgumentException;
import org.tquadrat.foundation.exception.EmptyArgumentException;
import org.tquadrat.foundation.exception.NullArgumentException;
import org.tquadrat.foundation.perflog.PerfLogMBean;
import org.tquadrat.foundation.perflog.PerformanceSection;
import org.tquadrat.foundation.perflog.PerformanceSectionName;
import org.tquadrat.foundation.testutil.TestBaseClass;

/**
 *  Some tests for the interface
 *  {@link org.tquadrat.foundation.perflog.PerfLogManager}
 *  and its implementation class
 *  {@link PerfLogManagerImpl}.
 *
 *  @extauthor Thomas Thrien - thomas.thrien@tquadrat.org
 *  @version $Id: TestPerfLogManager.java 1211 2026-05-01 15:24:10Z tquadrat $
 */
@ClassVersion( sourceVersion = "$Id: TestPerfLogManager.java 1211 2026-05-01 15:24:10Z tquadrat $" )
@DisplayName( "org.tquadrat.foundation.perflog.internal.TestPerfLogManager" )
public class TestPerfLogManager extends TestBaseClass
{
        /*---------*\
    ====** Methods **==========================================================
        \*---------*/
    /**
     *  Unregisters the MBean after each test.
     */
    @AfterEach
    final void afterEach() throws InstanceNotFoundException, MBeanRegistrationException
    {
        final var mbeanServer = getMBeanServer();
        final var objectName = getPerfLogMBeanObjectName();
        if( mbeanServer.isRegistered( objectName ) )
        {
            mbeanServer.unregisterMBean( objectName );
        }
    }   //  afterEach()

    /**
     *  Returns an MBean server that can be used to register the MBean.
     *
     *  @return The MBean server.
     */
    private final MBeanServer getMBeanServer()
    {
        final var retValue = MBeanServerFactory.findMBeanServer( null )
            .stream()
            .filter( mbs -> mbs.getDefaultDomain().equals( DOMAIN_NAME ) )
            .findFirst()
            .orElseGet( () -> createMBeanServer( DOMAIN_NAME ) );

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  getMBeanServer()

    /**
     *  Some tests for the
     *  {@linkplain PerfLogManagerImpl#PerfLogManagerImpl(javax.management.MBeanServer,javax.management.ObjectName) constructor}
     *  of
     *  {@link PerfLogManagerImpl}.
     *
     *  @throws Exception   Something went awfully wrong.
     */
    @SuppressWarnings( "resource" )
    @Test
    final void testConstructor() throws Exception
    {
        skipThreadTest();

        final var mbeanServer = getMBeanServer();
        final var objectName = getPerfLogMBeanObjectName();

        assertThrows( NullArgumentException.class, () -> new PerfLogManagerImpl( null, objectName ) );
        assertThrows( NullArgumentException.class, () -> new PerfLogManagerImpl( mbeanServer, null ) );

        final var candidate = new PerfLogManagerImpl( mbeanServer, objectName );
        assertNotNull( candidate );
    }   //  testConstructor()

    /**
     *  Some tests for
     *  {@link org.tquadrat.foundation.perflog.PerfLogManager#createPerformanceTracker(String)}
     *  and
     *  {@link org.tquadrat.foundation.perflog.PerfLogManager#createPerformanceTracker(PerformanceSectionName)}.
     *
     *  @throws Exception   Something went awfully wrong.
     */
    @Test
    final void testCreatePerformanceTracker() throws Exception
    {
        skipThreadTest();

        final var nameString = "PerformanceSection";
        final var name = createPerformanceSectionName( nameString );

        final var candidate = new PerfLogManagerImpl( getMBeanServer(), getPerfLogMBeanObjectName() );
        assertNotNull( candidate );

        assertThrows( NullArgumentException.class, () -> candidate.createPerformanceTracker( (PerformanceSectionName) null ) );

        assertThrows( NullArgumentException.class, () -> candidate.createPerformanceTracker( (String) null ) );
        assertThrows( EmptyArgumentException.class, () -> candidate.createPerformanceTracker( EMPTY_STRING ) );
        assertThrows( BlankArgumentException.class, () -> candidate.createPerformanceTracker( " " ) );

        assertTrue( candidate.getPerformanceSection( name ).isEmpty() );

        final var tracker = candidate.createPerformanceTracker( nameString );
        assertNotNull( tracker );

        /*
         * The section is created on the fly and is not ignored!
         */
        assertTrue( tracker.isPresent() );

        final var section = candidate.getPerformanceSection( name );
        assertTrue( section.isPresent() );

        /*
         * We now ignore the section.
         */
        section.get().setIgnoreFlag( true );
        assertTrue( section.get().isIgnored() );
        assertTrue( candidate.getPerformanceSection( name ).get().isIgnored() );

        assertTrue( candidate.createPerformanceTracker( name ).isEmpty() );
    }   //  testCreatePerformanceTracker()

    /**
     *  Some tests for
     *  {@link PerfLogManagerImpl#loadPerformanceSectionDefinitions(Iterable)}.
     *
     *  @throws Exception   Something went awfully wrong.
     */
    @Test
    final void testLoadPerformanceSectionDefinitions() throws Exception
    {
        skipThreadTest();

        final var candidate = new PerfLogManagerImpl( getMBeanServer(), getPerfLogMBeanObjectName() );
        assertNotNull( candidate );

        assertThrows( NullArgumentException.class, () -> candidate.loadPerformanceSectionDefinitions( (Iterable<PerformanceSection>) null ) );
        assertThrows( NullArgumentException.class, () -> candidate.loadPerformanceSectionDefinitions( (PerformanceSection[]) null ) );

        candidate.loadPerformanceSectionDefinitions( emptyList() );
        candidate.loadPerformanceSectionDefinitions( emptySet() );
        candidate.loadPerformanceSectionDefinitions( new PerformanceSection [0] );
    }   //  testLoadPerformanceSectionDefinitions()

    /**
     *  Some tests for
     *  {@link PerfLogManagerImpl#getMBean()}.
     *
     *  @throws Exception   Something went awfully wrong.
     */
    @Test
    final void testGetMBean() throws Exception
    {
        skipThreadTest();

        final var candidate = new PerfLogManagerImpl( getMBeanServer(), getPerfLogMBeanObjectName() );
        assertNotNull( candidate );

        final var mBean = candidate.getMBean();
        assertNotNull( mBean );
        assertInstanceOf( PerfLogMBean.class, mBean );
        assertFalse( mBean instanceof PerfLogMBeanImpl );
        assertTrue( mBean.getClass().getPackageName().startsWith( "jdk.proxy" ) );
    }   //  testGetMBean()

    /**
     *  Some tests for
     *  {@link PerfLogManagerImpl#getPerformanceSection(PerformanceSectionName)}.
     *
     *  @throws Exception   Something went awfully wrong.
     */
    @Test
    final void testGetPerformanceSection() throws Exception
    {
        skipThreadTest();

        final var name = createPerformanceSectionName( "PerformanceSection" );
        final var candidate = new PerfLogManagerImpl( getMBeanServer(), getPerfLogMBeanObjectName() );
        assertNotNull( candidate );

        assertThrows( NullArgumentException.class, () -> candidate.getPerformanceSection( null ) );

        assertTrue( candidate.getPerformanceSection( name ).isEmpty() );

        final var definitions = List.of( new PerformanceSection( name, "Description", null, null ) );
        candidate.loadPerformanceSectionDefinitions( definitions );
        assertTrue( candidate.getPerformanceSection( name ).isPresent() );
    }   //  testGetPerformanceSection()
}
//  class TestPerfLogManager

/*
 *  End of File
 */