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

import static javax.management.JMX.newMBeanProxy;
import static javax.management.MBeanServerFactory.createMBeanServer;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.tquadrat.foundation.perflog.PerfLogMBean.DESCRIPTION;
import static org.tquadrat.foundation.perflog.PerfLogUtils.DOMAIN_NAME;
import static org.tquadrat.foundation.perflog.PerfLogUtils.createPerformanceSectionName;
import static org.tquadrat.foundation.perflog.PerfLogUtils.getPerfLogMBeanObjectName;
import static org.tquadrat.foundation.perflog.PerformanceSection.PerformanceSectionFlags.IGNORED;
import static org.tquadrat.foundation.perflog.PerformanceSection.PerformanceSectionFlags.SEND_REPORT_FOR_ABORT;
import static org.tquadrat.foundation.value.Time.MILLISECOND;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import java.util.OptionalLong;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.tquadrat.foundation.annotation.ClassVersion;
import org.tquadrat.foundation.exception.NullArgumentException;
import org.tquadrat.foundation.perflog.PerfLogMBean;
import org.tquadrat.foundation.perflog.PerformanceSection;
import org.tquadrat.foundation.perflog.PerformanceSectionName;
import org.tquadrat.foundation.testutil.TestBaseClass;
import org.tquadrat.foundation.value.TimeValue;

/**
 *  Some tests for the interface
 *  {@link org.tquadrat.foundation.perflog.PerfLogMBean}
 *  and its implementation class
 *  {@link PerfLogMBeanImpl}.
 *
 *  @extauthor Thomas Thrien - thomas.thrien@tquadrat.org
 *  @version $Id: TestPerfLogMBean.java 1211 2026-05-01 15:24:10Z tquadrat $
 */
@ClassVersion( sourceVersion = "$Id: TestPerfLogMBean.java 1211 2026-05-01 15:24:10Z tquadrat $" )
@DisplayName( "org.tquadrat.foundation.perflog.internal.TestPerfLogMBean" )
public class TestPerfLogMBean extends TestBaseClass
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
     *  Some tests for
     *  {@link org.tquadrat.foundation.perflog.PerfLogMBean#addPerformanceSection(PerformanceSection)},
     *  {@link PerfLogMBean#retrievePerformanceSection(PerformanceSectionName)}
     *  and
     *  {@link PerfLogMBean#getPerformanceSection(PerformanceSectionName)}.
     *
     *  @throws Exception   Something went awfully wrong.
     */
    @Test
    final void testAddPerformanceSection() throws Exception
    {
        skipThreadTest();

        final var nameString = "PerformanceSection";
        final var name = createPerformanceSectionName( nameString );
        final var section = new PerformanceSection( name, "Description", new TimeValue( MILLISECOND, 50 ), new TimeValue( MILLISECOND, 100 ), IGNORED, SEND_REPORT_FOR_ABORT );

        final var candidate = new PerfLogMBeanImpl();
        assertNotNull( candidate );

        assertTrue( candidate.getPerformanceSection( name ).isEmpty() );

        assertThrows( NullArgumentException.class, () -> candidate.addPerformanceSection( null ) );
        assertThrows( NullArgumentException.class, () -> candidate.retrievePerformanceSection( null ) );

        var result = candidate.retrievePerformanceSection( name );
        assertNotNull( result );
        assertEquals( section.getName(), result.getName() );
        assertEquals( result, section );
        assertFalse( result.isIgnored() );
        assertTrue( result.getThreshold().isEmpty() );
        assertTrue( result.getTimeout().isEmpty() );
        assertNotEquals( result.getDescription(), section.getDescription() );

        assertEquals( OptionalLong.of( 30L ), OptionalLong.of( 30L ) );
        assertNotSame( OptionalLong.of( 30L ), OptionalLong.of( 30L ) );
        assertNotEquals( OptionalLong.of( 50L ), OptionalLong.of( 30L ) );

        assertDoesNotThrow( () -> candidate.addPerformanceSection( section ) );
        result = candidate.retrievePerformanceSection( name );
        assertNotNull( result );
        assertEquals( section.getName(), result.getName() );
        assertEquals( result, section );
        assertEquals( result.isIgnored(), section.isIgnored() );
        assertTrue( result.getThreshold().isPresent() );
        assertEquals( result.getThreshold(), section.getThreshold() );
        assertTrue( result.getTimeout().isPresent() );
        assertEquals( result.getTimeout(), section.getTimeout() );
        assertEquals( result.getDescription(), section.getDescription() );
    }   //  testAddPerformanceSection()

    /**
     *  Tests the registration of the MBean.
     *
     *  @throws Exception   Something went awfully wrong.
     */
    @Test
    final void testRegistration() throws Exception
    {
        skipThreadTest();

        final var candidate = new PerfLogMBeanImpl();
        assertNotNull( candidate );

        final var mbeanServer = getMBeanServer();
        final var objectName = getPerfLogMBeanObjectName();

        assertFalse( mbeanServer.isRegistered( objectName ) );
        assertThrows( InstanceNotFoundException.class, () -> mbeanServer.unregisterMBean( objectName ) );
        assertDoesNotThrow( () -> mbeanServer.registerMBean( candidate, objectName ) );
        assertTrue( mbeanServer.isRegistered( objectName ) );
        assertTrue( mbeanServer.isInstanceOf( objectName, candidate.getClass().getName() ) );
        assertTrue( mbeanServer.isInstanceOf( objectName, PerfLogMBean.class.getName() ) );
        assertThrows( InstanceAlreadyExistsException.class, () -> mbeanServer.registerMBean( candidate, objectName ) );
        assertDoesNotThrow( () -> mbeanServer.unregisterMBean( objectName ) );
        assertFalse( mbeanServer.isRegistered( objectName ) );

        assertDoesNotThrow( () -> mbeanServer.createMBean( candidate.getClass().getName(), objectName ) );
        assertTrue( mbeanServer.isRegistered( objectName ) );
        assertTrue( mbeanServer.isInstanceOf( objectName, candidate.getClass().getName() ) );
        assertTrue( mbeanServer.isInstanceOf( objectName, PerfLogMBean.class.getName() ) );
        assertThrows( InstanceAlreadyExistsException.class, () -> mbeanServer.registerMBean( candidate, objectName ) );
        assertDoesNotThrow( () -> mbeanServer.unregisterMBean( objectName ) );
        assertFalse( mbeanServer.isRegistered( objectName ) );

        assertDoesNotThrow( () -> mbeanServer.registerMBean( candidate, objectName ) );
        final var info = mbeanServer.getMBeanInfo( objectName );
        assertNotNull( info );
        assertEquals( DESCRIPTION, info.getDescription() );

        /*
         * Create a proxy.
         */
        final var proxy = newMBeanProxy( mbeanServer, objectName, PerfLogMBean.class );
        assertNotNull( proxy );
        assertInstanceOf( PerfLogMBean.class, proxy );
    }   //  testRegistration()
}
//  class TestPerfLogMBean

/*
 *  End of File
 */