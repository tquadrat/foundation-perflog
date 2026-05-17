/*
 * ============================================================================
 * Copyright © 2002-2026 by Thomas Thrien.
 * All Rights Reserved.
 * ============================================================================
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

package org.tquadrat.foundation.perflog.perflogutils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.tquadrat.foundation.perflog.PerfLogUtils.createPerfLogManager;
import static org.tquadrat.foundation.perflog.PerfLogUtils.createPerformanceSectionName;
import static org.tquadrat.foundation.perflog.PerfLogUtils.hold;
import static org.tquadrat.foundation.perflog.PerfLogUtils.holdAndStart;
import static org.tquadrat.foundation.perflog.PerformanceTracker.TrackerStatus.STATUS_ABORTED;
import static org.tquadrat.foundation.perflog.PerformanceTracker.TrackerStatus.STATUS_READY;
import static org.tquadrat.foundation.perflog.PerformanceTracker.TrackerStatus.STATUS_STARTED;
import static org.tquadrat.foundation.perflog.PerformanceTracker.TrackerStatus.STATUS_STOPPED;
import static org.tquadrat.foundation.util.SystemUtils.repose;
import static org.tquadrat.foundation.value.Time.MILLISECOND;

import java.time.Duration;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.tquadrat.foundation.annotation.ClassVersion;
import org.tquadrat.foundation.exception.NullArgumentException;
import org.tquadrat.foundation.perflog.PerfLogManager;
import org.tquadrat.foundation.perflog.PerfLogUtils;
import org.tquadrat.foundation.perflog.PerfLogUtils.PerformanceTrackerHolder;
import org.tquadrat.foundation.perflog.PerformanceSection;
import org.tquadrat.foundation.perflog.PerformanceSectionName;
import org.tquadrat.foundation.perflog.PerformanceTracker;
import org.tquadrat.foundation.perflog.internal.PerfLogManagerImpl;
import org.tquadrat.foundation.testutil.TestBaseClass;
import org.tquadrat.foundation.value.TimeValue;

/**
 *  <p>{@summary Some tests for the method
 *  {@link PerfLogUtils#hold(PerformanceSectionName)}
 *  and overloaded methods.}</p>
 *
 *  @extauthor Thomas Thrien - thomas.thrien@tquadrat.org
 *  @version $Id: TestHold.java 1246 2026-05-16 14:07:00Z tquadrat $
 *  @since 0.25.0
 */
@ClassVersion( sourceVersion = "$Id: TestHold.java 1246 2026-05-16 14:07:00Z tquadrat $" )
@DisplayName( "org.tquadrat.foundation.perflog.perflogutils.TestHold" )
public class TestHold extends TestBaseClass
{
        /*---------*\
    ====** Methods **==========================================================
        \*---------*/
    /**
     *  Test for the argument exceptions.
     *
     *  @throws Exception   Something went awfully wrong.
     */
    @Test
    final void testArgumentValidation() throws Exception
    {
        skipThreadTest();

        final Optional<PerformanceTracker> tracker = null;
        final String nameString = null;
        final PerformanceSectionName name = null;
        final PerfLogManager manager = null;

        final PerformanceTrackerHolder holder = assertDoesNotThrow( () -> hold( Optional.empty() ) );

        assertThrows( NullArgumentException.class, () -> hold( tracker ) );
        assertThrows( NullArgumentException.class, () -> hold( nameString ) );
        assertThrows( NullArgumentException.class, () -> hold( name ) );

        assertThrows( NullArgumentException.class, () -> hold( manager, createPerformanceSectionName( "name" ) ) );

        assertThrows( ClassCastException.class, () -> hold( Optional.of( holder ) ) );

        assertThrows( NullArgumentException.class, () -> holdAndStart( tracker ) );
        assertThrows( NullArgumentException.class, () -> holdAndStart( nameString ) );
        assertThrows( NullArgumentException.class, () -> holdAndStart( name ) );

        assertThrows( NullArgumentException.class, () -> holdAndStart( manager, createPerformanceSectionName( "name" ) ) );

        assertThrows( ClassCastException.class, () -> holdAndStart( Optional.of( holder ) ) );
    }   //  testArgumentValidation()

    /**
     *  Tests whether
     *  {@link PerfLogUtils#hold(PerformanceSectionName)}
     *  works as intended.
     *
     *  @throws Exception   Something went awfully wrong.
     */
    @Test
    final void testHoldWithVolatileManager() throws Exception
    {
        skipThreadTest();

        final var holderClass = PerformanceTrackerHolder.class;
        final var managerField = assertDoesNotThrow( () -> holderClass.getDeclaredField( "m_Manager" ) );
        managerField.setAccessible( true );

        final var managerClass = PerfLogManagerImpl.class;
        final var isActiveField = assertDoesNotThrow( () -> managerClass.getDeclaredField( "m_IsActive" ) );
        isActiveField.setAccessible( true );

        /*
         * We need a section with a timeout.
         */
        final var name = createPerformanceSectionName( "PS1" );
        final var timeoutValue = new TimeValue( MILLISECOND, 30 );
        final var section = new PerformanceSection( name, "description", null, timeoutValue );
        createPerfLogManager().loadPerformanceSectionDefinitions( section );

        final var holder1 = assertDoesNotThrow( () -> holdAndStart( name ) );
        assertEquals( STATUS_STARTED, holder1.getStatus() );
        repose( timeoutValue.asDuration() );
        repose( timeoutValue.asDuration() );
        assertEquals( STATUS_ABORTED, holder1.getStatus() );
        assertFalse( holder1.isActive() );

        final var holder2 = assertDoesNotThrow( () -> hold( name ) );
        assertEquals( STATUS_READY, holder2.getStatus() );
        repose( Duration.ofMillis( 30 ) );
        assertDoesNotThrow( () -> holder2.start() );
        assertEquals( STATUS_STARTED, holder2.getStatus() );
        repose( timeoutValue.asDuration() );
        repose( timeoutValue.asDuration() );
        assertEquals( STATUS_ABORTED, holder2.getStatus() );
        assertFalse( holder2.isActive() );

        final var holder3 = assertDoesNotThrow( () -> hold( name ) );
        try( holder3 )
        {
            holder3.start();
            assertTrue( holder3.isActive() );
            final var manager = assertDoesNotThrow( () -> managerField.get( holder3 ) );
            assertInstanceOf( PerfLogManagerImpl.class, manager );
        }
        assertFalse( holder3.isActive() );
        assertEquals( STATUS_STOPPED, holder3.getStatus() );

        assertNull( assertDoesNotThrow( () -> managerField.get( holder3 ) ) );
    }   //  testHoldWithVolatileManager()
}
//  class TestHold

/*
 *  End of File
 */