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

package org.tquadrat.foundation.perflog;

import static java.lang.System.out;
import static javax.management.MBeanServerFactory.createMBeanServer;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.tquadrat.foundation.perflog.PerfLogUtils.DOMAIN_NAME;
import static org.tquadrat.foundation.perflog.PerfLogUtils.createPerfLogManager;
import static org.tquadrat.foundation.perflog.PerfLogUtils.createPerformanceSectionName;
import static org.tquadrat.foundation.perflog.PerfLogUtils.getPerfLogMBeanObjectName;
import static org.tquadrat.foundation.perflog.PerformanceSection.PerformanceSectionFlags.SEND_REPORT_FOR_ABORT;
import static org.tquadrat.foundation.perflog.PerformanceTracker.TrackerStatus.STATUS_ABORTED;
import static org.tquadrat.foundation.perflog.PerformanceTracker.TrackerStatus.STATUS_READY;
import static org.tquadrat.foundation.perflog.PerformanceTracker.TrackerStatus.STATUS_STARTED;
import static org.tquadrat.foundation.perflog.PerformanceTracker.TrackerStatus.STATUS_STOPPED;
import static org.tquadrat.foundation.util.SystemUtils.repose;
import static org.tquadrat.foundation.value.Time.MILLISECOND;
import static org.tquadrat.foundation.value.Time.SECOND;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.tquadrat.foundation.annotation.ClassVersion;
import org.tquadrat.foundation.exception.NullArgumentException;
import org.tquadrat.foundation.lang.StringConverter;
import org.tquadrat.foundation.perflog.internal.PerformanceTrackerImpl;
import org.tquadrat.foundation.testutil.TestBaseClass;
import org.tquadrat.foundation.util.stringconverter.StringStringConverter;
import org.tquadrat.foundation.value.TimeValue;

/**
 *  Some tests for the interface
 *  {@link PerformanceTracker}
 *  and its implementation
 *  {@link org.tquadrat.foundation.perflog.internal.PerformanceTrackerImpl}.
 *
 *  @extauthor Thomas Thrien - thomas.thrien@tquadrat.org
 *  @version $Id: TestPerformanceTracker.java 1211 2026-05-01 15:24:10Z tquadrat $
 */
@ClassVersion( sourceVersion = "$Id: TestPerformanceTracker.java 1211 2026-05-01 15:24:10Z tquadrat $" )
@DisplayName( "org.tquadrat.foundation.perflog.TestPerformanceTracker" )
public class TestPerformanceTracker extends TestBaseClass
{
        /*------------------------*\
    ====** Static Initialisations **===========================================
        \*------------------------*/
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
     *  Some tests for the methods
     *  {@link PerformanceTracker#addContext(String,String)}
     *  and
     *  {@link PerformanceTracker#addContext(String,Object,StringConverter)}.
     *
     *  @throws Exception   Something went awfully wrong.
     */
    @Test
    final void testAddContext() throws Exception
    {
        skipThreadTest();

        final var section = new PerformanceSection( "PerformanceSection", "Description", null, null, SEND_REPORT_FOR_ABORT );

        try( final var manager = createPerfLogManager() )
        {
            final var candidate = assertDoesNotThrow( () -> new PerformanceTrackerImpl( manager, section ) );
            assertNotNull( candidate );

            assertThrows( NullArgumentException.class, () -> candidate.addContext( null, "Value" ) );
            assertThrows( NullArgumentException.class, () -> candidate.addContext( null, "Value", StringStringConverter.INSTANCE ) );
            assertThrows( NullArgumentException.class, () -> candidate.addContext( "Name", "Value", null ) );
            assertThrows( NullArgumentException.class, () -> candidate.addContext( "Name", null, null ) );

            assertTrue( candidate.getContext().isEmpty() );

            assertDoesNotThrow( () -> candidate.addContext( "Name", "Value" ) );
            assertDoesNotThrow( () -> candidate.addContext( "Name", null ) );
            assertDoesNotThrow( () -> candidate.addContext( "Name", "Value", StringStringConverter.INSTANCE ) );
            assertDoesNotThrow( () -> candidate.addContext( "Name", null, StringStringConverter.INSTANCE ) );
            assertTrue( candidate.getContext().isEmpty() );

            final var key = "Context";
            final var value = "Value";
            candidate.addContext( key, value );
            final var context = candidate.getContext();
            assertFalse( context.isEmpty() );
            assertTrue( context.containsKey( key ) );
            assertNotNull( context.get( key ) );
            assertEquals( value, context.get( key ) );

            assertDoesNotThrow( () -> candidate.reset( true ) );
            assertTrue( candidate.getContext().isEmpty() );
        }
    }   //  testAddContext()

    /**
     *  Some tests for the
     *  {@linkplain PerformanceTrackerImpl#PerformanceTrackerImpl(PerfLogManager,PerformanceSection) constructor}
     *  of
     *  {@link PerformanceTracker}.
     *
     *  @throws Exception   Something went awfully wrong.
     */
    @Test
    final void testConstructor() throws Exception
    {
        skipThreadTest();

        final var section = new PerformanceSection( "PerformanceSection", "Description", null, null, SEND_REPORT_FOR_ABORT );

        try( final var manager = createPerfLogManager() )
        {
            assertThrows( NullArgumentException.class, () -> new PerformanceTrackerImpl( null, section ) );
            assertThrows( NullArgumentException.class, () -> new PerformanceTrackerImpl( manager,null ) );

            final var candidate = assertDoesNotThrow( () -> new PerformanceTrackerImpl( manager, section ) );
            assertNotNull( candidate );

            /*
             * The tracker was not yet started, and it was not yet stopped.
             */
            assertEquals( STATUS_READY, candidate.getStatus() );
            assertFalse( candidate.isActive() );
            assertTrue( candidate.getElapsedTime().isEmpty() );
            assertFalse( candidate.isThresholdExceeded() );
            assertFalse( candidate.isTimedOut() );
            assertDoesNotThrow( () -> candidate.reset( false ) );
        }
    }   //  testConstructor()

    /**
     *  Some tests for the status changes.
     *
     *  @throws Exception   Something went awfully wrong.
     */
    @Test
    final void testStatusChange() throws Exception
    {
        skipThreadTest();

        final var nameString = "PerformanceSection";
        final var name = createPerformanceSectionName( nameString );
        final var description = "Description";
        final var threshold = new TimeValue( MILLISECOND, 5 );
        var timeout = new TimeValue( SECOND, 10 );
        final var section = new PerformanceSection( name, description, threshold, timeout, SEND_REPORT_FOR_ABORT );

        try( final var manager = createPerfLogManager() )
        {
            final var candidate = assertDoesNotThrow( () -> new PerformanceTrackerImpl( manager, section ) );
            assertNotNull( candidate );

            assertDoesNotThrow( () -> candidate.addContext( "Context", "Value" ) );

            /*
             * The tracker was not yet started, and it was not yet stopped.
             */
            assertEquals( STATUS_READY, candidate.getStatus() );
            assertFalse( candidate.isActive() );
            assertTrue( candidate.getElapsedTime().isEmpty() );
            assertFalse( candidate.isThresholdExceeded() );
            assertFalse( candidate.isTimedOut() );
            assertDoesNotThrow( () -> candidate.reset( false ) );

            assertDoesNotThrow( candidate::stop );
            assertDoesNotThrow( () -> candidate.abort() );
            assertDoesNotThrow( () -> candidate.reset( false ) );

            /*
             * The tracker was started, but it was not yet stopped.
             */
            assertDoesNotThrow( candidate::start );
            assertEquals( STATUS_STARTED, candidate.getStatus() );
            assertTrue( candidate.isActive() );
            assertTrue( candidate.getElapsedTime().isEmpty() );
            assertFalse( candidate.isThresholdExceeded() );
            assertFalse( candidate.isTimedOut() );
            assertThrows( IllegalStateException.class, () -> candidate.reset( false ) );

            assertThrows( IllegalStateException.class, candidate::start );
            assertDoesNotThrow( candidate::stop );
            assertDoesNotThrow( candidate::start );
            assertDoesNotThrow( () -> candidate.abort() );
            assertDoesNotThrow( candidate::start );
            assertDoesNotThrow( candidate::stop );
            assertDoesNotThrow( () -> candidate.abort() );
            assertDoesNotThrow( candidate::start );
            assertDoesNotThrow( () -> candidate.abort() );
            assertDoesNotThrow( candidate::stop );
            assertDoesNotThrow( () -> candidate.reset( false ) );

            assertDoesNotThrow( candidate::start );
            assertEquals( STATUS_STARTED, candidate.getStatus() );
            assertTrue( candidate.isActive() );
            assertTrue( candidate.getElapsedTime().isEmpty() );
            assertFalse( candidate.isThresholdExceeded() );
            assertFalse( candidate.isTimedOut() );
            assertThrows( IllegalStateException.class, () -> candidate.reset( false ) );

            assertDoesNotThrow( () -> candidate.abort() );
            assertFalse( candidate.isActive() );
            assertEquals( STATUS_ABORTED, candidate.getStatus() );
            assertTrue( candidate.getElapsedTime().isEmpty() );
            assertFalse( candidate.isThresholdExceeded() );
            assertFalse( candidate.isTimedOut() );
            assertDoesNotThrow( () -> candidate.reset( false ) );

            assertDoesNotThrow( candidate::start );
            assertDoesNotThrow( () -> candidate.abort( true ) );
            assertFalse( candidate.isActive() );
            assertEquals( STATUS_ABORTED, candidate.getStatus() );
            assertTrue( candidate.getElapsedTime().isEmpty() );
            assertFalse( candidate.isThresholdExceeded() );
            assertTrue( candidate.isTimedOut() );
            assertDoesNotThrow( () -> candidate.reset( false ) );

            assertDoesNotThrow( candidate::start );
            assertEquals( STATUS_STARTED, candidate.getStatus() );
            assertTrue( candidate.isActive() );
            assertTrue( candidate.getElapsedTime().isEmpty() );
            assertFalse( candidate.isThresholdExceeded() );
            assertFalse( candidate.isTimedOut() );
            assertThrows( IllegalStateException.class, () -> candidate.reset( false ) );

            assertDoesNotThrow( candidate::stop );
            assertThrows( IllegalStateException.class, candidate::stop );
            assertEquals( STATUS_STOPPED, candidate.getStatus() );
            assertFalse( candidate.isActive() );
            assertTrue( candidate.getElapsedTime().isPresent() );

            final var elapsedTime = candidate.getElapsedTime().get();
            if( elapsedTime.compareTo( threshold ) > 0 )
            {
                assertTrue( candidate.isThresholdExceeded() );
            }
            else
            {
                assertFalse( candidate.isThresholdExceeded() );
            }

            assertFalse( candidate.isTimedOut() );
            assertDoesNotThrow( () -> candidate.reset( false ) );

            assertDoesNotThrow( candidate::start );
            assertEquals( STATUS_STARTED, candidate.getStatus() );
            assertTrue( candidate.isActive() );
            assertTrue( candidate.getElapsedTime().isEmpty() );
            assertFalse( candidate.isThresholdExceeded() );
            assertFalse( candidate.isTimedOut() );
            assertThrows( IllegalStateException.class, () -> candidate.reset( false ) );

            out.printf( "Threshold/Waiting Period: %s%n", threshold );
            repose( threshold.asDuration().plusMillis( 1 ) );
            out.println( "Finished waiting!" );

            assertDoesNotThrow( candidate::stop );
            assertThrows( IllegalStateException.class, candidate::stop );
            assertEquals( STATUS_STOPPED, candidate.getStatus() );
            assertFalse( candidate.isActive() );
            assertTrue( candidate.getElapsedTime().isPresent() );
            assertTrue( candidate.isThresholdExceeded() );
            assertFalse( candidate.isTimedOut() );
            assertDoesNotThrow( () -> candidate.reset( false ) );

            timeout = new TimeValue( MILLISECOND, 10 );
            section.setTimeout( timeout );
            assertDoesNotThrow( candidate::start );
            assertEquals( STATUS_STARTED, candidate.getStatus() );
            assertThrows( IllegalStateException.class, () -> candidate.reset( false ) );

            out.printf( "Timeout/Waiting Period: %s%n", timeout );
            repose( timeout.asDuration().plusMillis( 1 ) );
            out.println( "Finished waiting!" );

            assertDoesNotThrow( candidate::stop );
            assertThrows( IllegalStateException.class, candidate::stop );
            assertEquals( STATUS_ABORTED, candidate.getStatus() );
            assertTrue( candidate.isTimedOut() );
            assertFalse( candidate.isActive() );
            assertTrue( candidate.getElapsedTime().isPresent() );
            assertTrue( candidate.isThresholdExceeded() );
            assertDoesNotThrow( () -> candidate.reset( false ) );
        }
    }   //  testStatusChange()
}
//  class TestPerformanceTracker

/*
 *  End of File
 */