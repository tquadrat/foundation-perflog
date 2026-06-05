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

import static java.lang.System.nanoTime;
import static org.apiguardian.api.API.Status.STABLE;
import static org.tquadrat.foundation.lang.Objects.isNull;
import static org.tquadrat.foundation.lang.Objects.nonNull;
import static org.tquadrat.foundation.lang.Objects.requireNonNullArgument;
import static org.tquadrat.foundation.perflog.PerformanceTracker.TrackerStatus.STATUS_ABORTED;
import static org.tquadrat.foundation.perflog.PerformanceTracker.TrackerStatus.STATUS_READY;
import static org.tquadrat.foundation.perflog.PerformanceTracker.TrackerStatus.STATUS_STARTED;
import static org.tquadrat.foundation.perflog.PerformanceTracker.TrackerStatus.STATUS_STOPPED;
import static org.tquadrat.foundation.value.Time.NANOSECOND;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;

import org.apiguardian.api.API;
import org.tquadrat.foundation.annotation.ClassVersion;
import org.tquadrat.foundation.perflog.PerfLogManager;
import org.tquadrat.foundation.perflog.PerformanceSection;
import org.tquadrat.foundation.perflog.PerformanceTracker;
import org.tquadrat.foundation.value.TimeValue;

/**
 *  <p>{@summary The implementation for the interface
 *  {@link PerformanceTracker}}.</p>
 *
 *  @extauthor Thomas Thrien - thomas.thrien@tquadrat.org
 *  @version $Id: PerformanceTrackerImpl.java 1258 2026-06-04 18:33:06Z tquadrat $
 *  @since 0.25.0
 *
 *  @UMLGraph.link
 */
@ClassVersion( sourceVersion = "$Id: PerformanceTrackerImpl.java 1258 2026-06-04 18:33:06Z tquadrat $" )
@API( status = STABLE, since = "0.25.0" )
@SuppressWarnings( "ClassWithTooManyFields" )
public final class PerformanceTrackerImpl implements PerformanceTracker
{
        /*------------*\
    ====** Attributes **=======================================================
        \*------------*/
    /**
     *  The flag that indicates whether the tracker was aborted.
     */
    private boolean m_Aborted = false;

    /**
     *  The context data.
     */
    private final Map<String,String> m_Context = new HashMap<>();

    /**
     *  <p>{@summary The end time for this performance tracker.}</p>
     *  <p>This is not an absolute time, but a relative value.</p>
     *
     *  @see System#nanoTime()
     */
    private long m_EndTime = 0L;

    /**
     *  The reference to the performance manager that created this tracker.
     */
    private final PerfLogManagerImpl m_PerfLogManager;

    /**
     *  The performance section for this tracker.
     */
    private final PerformanceSection m_PerformanceSection;

    /**
     *  The flag that indicates whether the tracker was started.
     */
    private boolean m_Started = false;

    /**
     *  <p>{@summary The start time for this performance tracker.}</p>
     *  <p>This is not an absolute time, but a relative value.</p>
     *
     *  @see System#nanoTime()
     */
    private long m_StartTime = 0L;

    /**
     *  The flag that indicates whether the tracker was stopped.
     */
    private boolean m_Stopped = false;

    /**
     *  <p>{@summary The flag that indicates whether this tracker was aborted
     *  because of a timeout.}</p>
     */
    private boolean m_TimedOut = false;

    /**
     *  <p>{@summary The future that is used for the timeout handling.}</p>
     *  <p>Will be {@null} when the timeout is disabled.</p>
     */
    private ScheduledFuture<?> m_TimeOutFuture = null;

    /**
     *  <p>{@summary The absolute time when this performance tracker was
     *  started.} This is only used for reporting purposes.</p>
     */
    private Instant m_Timestamp = null;

        /*--------------*\
    ====** Constructors **=====================================================
        \*--------------*/
    /**
     *  Creates a new instance of {@code PerformanceTrackerImpl}.
     *
     *  @param  perfLogManager  The reference to the performance manager that
     *      created this tracker instance.
     *  @param  performanceSection  The reference to the definition for the
     *      performance section.
     */
    public PerformanceTrackerImpl( final PerfLogManager perfLogManager, final PerformanceSection performanceSection )
    {
        m_PerfLogManager = (PerfLogManagerImpl) requireNonNullArgument( perfLogManager,"perfLogManager" );
        m_PerformanceSection = requireNonNullArgument( performanceSection, "performanceSection" );
    }   //  PerformanceTrackerImpl()

        /*---------*\
    ====** Methods **==========================================================
        \*---------*/
    /**
     *  {@inheritDoc}
     */
    @Override
    public final void abort() { abort( null, null, false ); }

    /**
     *  {@inheritDoc}
     */
    @Override
    public final void abort( final String message ) { abort( message, null ); }

    /**
     *  {@inheritDoc}
     */
    @Override
    public final void abort( final String message, final Throwable cause ) { abort( message, cause, false ); }

    /**
     *  <p>{@summary Stops the performance timer and sets the
     *  {@linkplain #isTimedOut() timeout flag}.}</p>
     *  <p>If the tracker has been aborted or stopped already, nothing happens.
     *  Same if it was never started.</p>
     *
     *  @param  flag    {@true} if the tracker timed out, {@false}
     *      if this is a &quot;regular&quot; abort.
     */
    @SuppressWarnings( "PublicMethodNotExposedInInterface" )
    public final void abort( final boolean flag ) { abort( null, null, flag ); }

    /**
     *  <p>{@summary Stops the performance timer, sets the
     *  {@linkplain #isTimedOut() timeout flag}
     *  and sends a report if required.}</p>
     *  <p>If the tracker has been aborted or stopped already, nothing happens.
     *  Same if it was never started.</p>
     *
     *  @param  message The message describing the reason for the abort.
     *  @param  cause   The exception that caused the abort.
     *  @param  flag    {@true} if the tracker timed out, {@false}
     *      if this is a &quot;regular&quot; abort.
     */
    private final void abort( final String message, final Throwable cause, final boolean flag )
    {
        if( m_Started && !m_Stopped && !m_Aborted )
        {
            terminateTimeoutMonitoring();
            m_Aborted = true;
            m_TimedOut = m_PerformanceSection.getTimeout().isPresent() && flag;
            if( m_TimedOut || m_PerformanceSection.isSendingReportForAbort() ) m_PerfLogManager.sendReport( this, message, cause );
        }
    }   //  abort()

    /**
     *  {@inheritDoc}
     */
    @Override
    public final PerformanceTracker addContext( final String name, final String value )
    {
        requireNonNullArgument( name, "name" );
        if( isNull( value ) )
        {
            m_Context.remove( name );
        }
        else
        {
            m_Context.put( name, value );
        }

        //---* Done *----------------------------------------------------------
        return this;
    }   //  addContext

    /**
     *  Returns the context information.
     *
     *  @return The context information.
     */
    @SuppressWarnings( "PublicMethodNotExposedInInterface" )
    public final Map<String,String> getContext() { return Map.copyOf( m_Context ); }

    /**
     *  Returns the elapsed time in nanoseconds.
     *
     *  @return An instance of
     *      {@link java.util.Optional}
     *      holding the elapsed time.
     */
    @SuppressWarnings( "PublicMethodNotExposedInInterface" )
    public final Optional<TimeValue> getElapsedTime()
    {
        final Optional<TimeValue> retValue = m_Started && m_Stopped
            ? Optional.of( new TimeValue( NANOSECOND, BigDecimal.valueOf( m_EndTime - m_StartTime ) ) )
            : Optional.empty();

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  getElapsedTime()

    /**
     *  Returns the performance section.
     *
     *  @return The performance section.
     */
    @SuppressWarnings( "PublicMethodNotExposedInInterface" )
    public final PerformanceSection getPerformanceSection() { return m_PerformanceSection; }

    /**
     *  {@inheritDoc}
     */
    @Override
    public final TrackerStatus getStatus()
    {
        var retValue = STATUS_READY;

        if( m_Aborted )
        {
            retValue = STATUS_ABORTED;
        }
        else if( m_Stopped )
        {
            retValue = STATUS_STOPPED;
        }
        else if( m_Started )
        {
            retValue = STATUS_STARTED;
        }

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  getStatus()

    /**
     *  Returns the timestamp.
     *
     *  @return The timestamp.
     */
    @SuppressWarnings( "PublicMethodNotExposedInInterface" )
    public final Instant getTimestamp() { return m_Timestamp; }

    /**
     *  <p>{@summary Checks whether the threshold was exceeded.} This means the
     *  operation covered by the performance section for this tracker took
     *  longer than the estimated time.
     *
     *  @return {@true} if the operation took longer than the provided
     *      threshold, {@false} otherwise, or no threshold was provided.
     */
    @SuppressWarnings( "PublicMethodNotExposedInInterface" )
    public boolean isThresholdExceeded()
    {
        final var threshold = m_PerformanceSection.getThreshold();
        final var elapsedTime = getElapsedTime();

        final var retValue = threshold.isPresent() && elapsedTime.stream().anyMatch( v -> threshold.get().compareTo( v ) < 0 );

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  isThresholdExceeded()

    /**
     *  <p>{@summary Checks whether the tracker was aborted due to a
     *  timeout.}</p>
     *
     *  @return {@true} if the tracker was aborted due to a timeout,
     *      {@false} if it is still running, if it was regularly stopped,
     *      if aborted due to other reasons, or if the timeout was disabled for
     *      the respective performance section.
     */
    @SuppressWarnings( "PublicMethodNotExposedInInterface" )
    public final boolean isTimedOut() { return m_TimedOut; }

    /**
     *  Registers the timeout monitor for this performance tracker.
     */
    private final void registerTimeoutMonitor()
    {
        m_TimeOutFuture = m_PerfLogManager.registerTimeoutMonitor( this );
    }   //  registerTimeoutMonitor()

    /**
     *  {@inheritDoc}
     */
    @Override
    public final PerformanceTracker reset( final boolean resetContext ) throws IllegalStateException
    {
        if( m_Started && !(m_Aborted || m_Stopped) ) throw new IllegalStateException( "Tracker is active" );

        m_Aborted = false;
        m_Stopped = false;
        m_Started = false;
        m_TimedOut = false;

        m_Timestamp = null;
        m_StartTime = 0L;
        m_EndTime = 0L;

        if( resetContext ) m_Context.clear();

        m_TimeOutFuture = null;

        //---* Done *----------------------------------------------------------
        return this;
    }   //  reset()

    /**
     *  {@inheritDoc}
     */
    @Override
    public final void start() throws IllegalStateException
    {
        reset( false );

        m_Timestamp = Instant.now();
        m_Started = true;
        m_PerformanceSection.getTimeout().ifPresent( _ -> registerTimeoutMonitor() );
        m_StartTime = nanoTime();
    }   //  start()

    /**
     *  {@inheritDoc}
     */
    @Override
    public final void stop() throws IllegalStateException
    {
        m_EndTime = nanoTime();
        if( m_Stopped ) throw new IllegalStateException( "Tracker was already stopped" );
        m_Stopped = true;
        terminateTimeoutMonitoring();
        if( m_Started && !m_Aborted )
        {
            m_PerfLogManager.sendReport( this, null, null );
        }
    }   //  stop()

    /**
     *  Terminates the timeout monitor.
     */
    private final void terminateTimeoutMonitoring()
    {
        if( nonNull( m_TimeOutFuture ) )
        {
            m_TimeOutFuture.cancel( true );
            m_TimeOutFuture = null;
        }
    }   //  terminateTimeoutMonitoring()
}
//  class PerformanceTrackerImpl

/*
 *  End of File
 */