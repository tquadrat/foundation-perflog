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

import static java.math.RoundingMode.CEILING;
import static java.util.concurrent.Executors.newScheduledThreadPool;
import static javax.management.JMX.newMBeanProxy;
import static org.apiguardian.api.API.Status.INTERNAL;
import static org.tquadrat.foundation.lang.Objects.nonNull;
import static org.tquadrat.foundation.lang.Objects.requireNonNullArgument;
import static org.tquadrat.foundation.value.Time.MILLISECOND;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.ref.Cleaner;
import java.lang.ref.Cleaner.Cleanable;
import java.math.MathContext;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import org.apiguardian.api.API;
import org.tquadrat.foundation.annotation.ClassVersion;
import org.tquadrat.foundation.exception.ImpossibleExceptionError;
import org.tquadrat.foundation.exception.UnexpectedExceptionError;
import org.tquadrat.foundation.perflog.PerfLogMBean;
import org.tquadrat.foundation.perflog.PerfLogManager;
import org.tquadrat.foundation.perflog.PerformanceSection;
import org.tquadrat.foundation.perflog.PerformanceSectionName;
import org.tquadrat.foundation.perflog.PerformanceTracker;

/**
 *  <p>{@summary The implementation for the interface
 *  {@link org.tquadrat.foundation.perflog.PerfLogManager}.}</p>
 *
 *  @extauthor Thomas Thrien - thomas.thrien@tquadrat.org
 *  @version $Id: PerfLogManagerImpl.java 1246 2026-05-16 14:07:00Z tquadrat $
 *  @since 0.25.0
 *
 *  @UMLGraph.link
 */
@ClassVersion( sourceVersion = "$Id: PerfLogManagerImpl.java 1246 2026-05-16 14:07:00Z tquadrat $" )
@API( status = INTERNAL, since = "0.25.0" )
public final class PerfLogManagerImpl implements PerfLogManager
{
        /*---------------*\
    ====** Inner Classes **====================================================
        \*---------------*/
    /**
     *  <p>{@summary The janitor that takes care of the housekeeping for an
     *  instance of
     *  {@link PerfLogManager}
     *  in case that was not properly closed.}</p>
     *
     *  @extauthor Thomas Thrien - thomas.thrien@tquadrat.org
     *  @version $Id: PerfLogManagerImpl.java 1246 2026-05-16 14:07:00Z tquadrat $
     *  @since 0.25.0
     *
     *  @param  scheduledExecutorService    The reference to the
     *      {@link ScheduledExecutorService}
     *      that needs to be terminated.
     *
     *  @UMLGraph.link
     */
    @SuppressWarnings( "NewClassNamingConvention" )
    @ClassVersion( sourceVersion = "$Id: PerfLogManagerImpl.java 1246 2026-05-16 14:07:00Z tquadrat $" )
    @API( status = INTERNAL, since = "0.25.0" )
    private record Janitor( ScheduledExecutorService scheduledExecutorService ) implements Runnable
    {
            /*---------*\
        ====** Methods **======================================================
            \*---------*/
        /**
         *  {@inheritDoc}
         */
        @Override
        public final void run()
        {
            if( nonNull( scheduledExecutorService ) && !scheduledExecutorService.isTerminated() )
            {
                scheduledExecutorService.shutdown();
            }
        }   //  run()
    }
    //  record Janitor

        /*------------*\
    ====** Attributes **=======================================================
        \*------------*/
    /**
     *  The
     *  {@link Cleanable}
     *  for this instance.
     */
    private final Cleanable m_Cleanable;

    /**
     *  The flag the indicates whether this manager is (still) active.
     */
    private boolean m_IsActive;

    /**
     *  The caretaker for this instance.
     */
    @SuppressWarnings( "FieldCanBeLocal" )
    private final Janitor m_Janitor;

    /**
     *  The proxy for the
     *  {@link org.tquadrat.foundation.perflog.PerfLogMBean}.
     */
    private final PerfLogMBean m_MBean;

    /**
     *  The timeout scheduler for this manager.
     */
    private final ScheduledExecutorService m_TimeoutScheduler;

        /*------------------------*\
    ====** Static Initialisations **===========================================
        \*------------------------*/
    /**
     *  The cleaner that is used to finalise instances of
     *  {@code PerfLogManagerImpl}.
     */
    private static final Cleaner m_Cleaner = Cleaner.create();

    /**
     *  The instance of
     *  {@link MathContext}
     *  that is used by
     *  {@link #registerTimeoutMonitor(PerformanceTrackerImpl)}
     *  to calculate the timeout waiting period from the given
     *  {@link org.tquadrat.foundation.value.TimeValue}.
     */
    private static final MathContext MATH_CONTEXT = new MathContext( 32, CEILING );

        /*--------------*\
    ====** Constructors **=====================================================
        \*--------------*/
    /**
     *  Creates a new instance of {@code PerfLogManagerImpl}.
     *
     *  @param  mbeanServer The MBean server that is used.
     *  @param  objectName  The name of the
     *      {@link PerfLogMBean}.
     */
    public PerfLogManagerImpl( final MBeanServer mbeanServer, final ObjectName objectName )
    {
        this( false, mbeanServer, objectName, null );
    }   //  PerfLogManagerImpl()

    /**
     *  Creates a new instance of {@code PerfLogManagerImpl}.
     *
     *  @param  mbeanServer The MBean server that is used.
     *  @param  objectName  The name of the
     *      {@link PerfLogMBean}.
     *  @param  uncaughtExceptionHandler    The implementation of
     *      {@link Thread.UncaughtExceptionHandler}
     *      that is used for the timeout monitoring thread.
     */
    @SuppressWarnings( "MethodParameterNamingConvention" )
    public PerfLogManagerImpl( final MBeanServer mbeanServer, final ObjectName objectName, final UncaughtExceptionHandler uncaughtExceptionHandler )
    {
        this( false, mbeanServer, objectName, requireNonNullArgument( uncaughtExceptionHandler, "uncaughtExceptionHandler" ) );
    }   //  PerfLogManagerImpl()

    /**
     *  Creates a new instance of {@code PerfLogManagerImpl}.
     *
     *  @param  ignored Discriminator
     *  @param  mbeanServer The MBean server that is used.
     *  @param  objectName  The name of the
     *      {@link PerfLogMBean}.
     *  @param  uncaughtExceptionHandler    The implementation of
     *      {@link Thread.UncaughtExceptionHandler}
     *      that is used for the timeout monitoring thread.
     */
    @SuppressWarnings( "MethodParameterNamingConvention" )
    private PerfLogManagerImpl( final boolean ignored, final MBeanServer mbeanServer, final ObjectName objectName, final UncaughtExceptionHandler uncaughtExceptionHandler )
    {
        //---* Connect to the MBean *------------------------------------------
        m_MBean = connectToMBean( mbeanServer, objectName );

        //---* Create the timeout scheduler *----------------------------------
        m_TimeoutScheduler = createTimeoutMonitor( uncaughtExceptionHandler );

        //---* Register the timeout monitor for housekeeping *-----------------
        m_Janitor = new Janitor( m_TimeoutScheduler );
        //noinspection ThisEscapedInObjectConstruction
        m_Cleanable = m_Cleaner.register( this, m_Janitor );

        m_IsActive = true;
    }   //  PerfLogManagerImpl()

        /*---------*\
    ====** Methods **==========================================================
        \*---------*/
    /**
     *  <p>{@summary Checks whether this performance logging manager is still
     *  active.} Throws an
     *  {@link IllegalStateException}
     *  if not.
     *
     *  @return {@code true} if the manager is still active.
     *  @throws IllegalStateException
     *      {@link #close()}
     *      was already called on this instance.
     */
    @SuppressWarnings( "UnusedReturnValue" )
    private final boolean checkActive() throws IllegalStateException
    {
        if( !m_IsActive ) throw new IllegalStateException( "PerfLogManager was already terminated" );

        //---* Done *----------------------------------------------------------
        //noinspection ConstantValue
        return m_IsActive;
    }   //  checkActive()

    /**
     *  {@inheritDoc}
     */
    @Override
    public final void close()
    {
        if( m_IsActive )
        {
            m_Cleanable.clean();
            m_IsActive = false;
        }
    }   //  close()

    /**
     *  <p>{@summary Establishes the connection with the
     *  {@link PerfLogMBean}
     *  on the given
     *  {@linkplain MBeanServer MBean server}
     *  and returns a proxy for it.}</p>
     *  <p>If the MBean had not been registered yet, this method will register
     *  it first.</p>
     *
     *  @param  mbeanServer The MBean server that is used.
     *  @param  objectName  The name for the MBean.
     *  @return A proxy for
     *      {@link PerfLogMBean}.
     */
    private static final PerfLogMBean connectToMBean( final MBeanServer mbeanServer, final ObjectName objectName )
    {
        final PerfLogMBean retValue;
        if( !requireNonNullArgument( mbeanServer, "mbeanServer" ).isRegistered( requireNonNullArgument( objectName, "objectName" ) ) )
        {
            final var mbean = new PerfLogMBeanImpl();
            try
            {
                mbeanServer.registerMBean( mbean, objectName );
            }
            catch( final InstanceAlreadyExistsException _ )
            {
                /*
                 * Someone else was faster to register the MBean. We ignore the
                 * exception and try to create the proxy.
                 */
            }
            catch( final MBeanRegistrationException e )
            {
                throw new UnexpectedExceptionError( e );
            }
            catch( final NotCompliantMBeanException e )
            {
                throw new ImpossibleExceptionError( e );
            }
        }
        retValue = newMBeanProxy( mbeanServer, objectName, PerfLogMBean.class );

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  connectToMBean

    /**
     *  {@inheritDoc}
     */
    @Override
    public final Optional<PerformanceTracker> createPerformanceTracker( final PerformanceSectionName name )
    {
        checkActive();

        final var performanceSection = m_MBean.retrievePerformanceSection( requireNonNullArgument( name, "name" ) );

        final Optional<PerformanceTracker> retValue;
        if( performanceSection.isIgnored() )
        {
            retValue = Optional.empty();
        }
        else
        {
            final var tracker = new PerformanceTrackerImpl( this, performanceSection );
            // TODO - Implement the registration for the timeout!
            retValue = Optional.of( tracker );
        }

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  createPerformanceTracker()

    /**
     *  Creates the
     *  {@link ScheduledExecutorService}
     *  instance that is used as the timeout monitor and registers it for the
     *  housekeeping.
     *
     *  @param  uncaughtExceptionHandler    The implementation of
     *      {@link Thread.UncaughtExceptionHandler}
     *      that is used for the timeout monitoring threads.
     *  @return The timeout monitor.
     */
    @SuppressWarnings( "MethodParameterNamingConvention" )
    private final ScheduledExecutorService createTimeoutMonitor( final UncaughtExceptionHandler uncaughtExceptionHandler )
    {
        //---* Create the ScheduledExecutorService *---------------------------
        final var threadName = "TimeoutMonitor";
        final var builder = Thread.ofVirtual()
            .name( threadName );
        if( nonNull( uncaughtExceptionHandler ) ) builder.uncaughtExceptionHandler( uncaughtExceptionHandler);
        final var retValue = newScheduledThreadPool( 1, builder.factory() );

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  createTimeoutMonitor()

    /**
     *  <p>{@summary Provides a reference to the internal instance of
     *  {@link PerfLogMBean}.}</p>
     *
     *  @note This is the proxy, but <i>not</i> the MBean itself.
     *  @note The API is not limited to the OpenMBean API that is published to
     *      a remote client.
     *
     *  @return The reference to the MBean.
     */
    @SuppressWarnings( "PublicMethodNotExposedInInterface" )
    public final PerfLogMBean getMBean() { return m_MBean; }

    /**
     *  {@inheritDoc}
     */
    @Override
    public final Optional<PerformanceSection> getPerformanceSection( final PerformanceSectionName name )
    {
        checkActive();

        final var retValue = m_MBean.getPerformanceSection( requireNonNullArgument( name, "name" ) );

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  getPerformanceSection()

    /**
     *  {@inheritDoc}
     */
    @SuppressWarnings( "NewMethodNamingConvention" )
    @Override
    public void loadPerformanceSectionDefinitions( final Iterable<PerformanceSection> definitions )
    {
        checkActive();

        for( final var definition : requireNonNullArgument( definitions, "definitions" ) )
        {
            m_MBean.addPerformanceSection( definition );
        }
    }   //  loadPerformanceSectionDefinitions

    /**
     *  Registers a timeout monitor from a performance tracker.
     *
     *  @param  tracker The performance tracker to register.
     *  @return The timeout monitor; will be {@code null} if the given tracker
     *      is not active, or the performance section for that tracker does
     *      not define a timeout value.
     *  @throws IllegalStateException
     *      {@link #close()}
     *      was already called on this instance.
     */
    @SuppressWarnings( "PublicMethodNotExposedInInterface" )
    public final ScheduledFuture<?> registerTimeoutMonitor( final PerformanceTrackerImpl tracker ) throws IllegalStateException
    {
        checkActive();

        ScheduledFuture<?> retValue = null;
        final var section = requireNonNullArgument( tracker, "tracker" ).getPerformanceSection();
        final var timeout = section.getTimeout();
        if( tracker.isActive() && timeout.isPresent() )
        {
            final var unit = MILLISECOND;
            final var timeoutValue = timeout.get().convert( unit ).round( MATH_CONTEXT ).longValue();
            retValue = m_TimeoutScheduler.schedule( () -> tracker.abort( true ), timeoutValue, unit.getTimeUnit() );
        }

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  registerTimeoutMonitor()

    /**
     *  Sends a performance report to the
     *  {@link PerfLogMBean}.
     *
     *  @param  tracker The tracker that collected the performance data.
     *  @param  message The message describing the reason for the abort of the
     *      tracker; can be {@code null}.
     *  @param  cause   The exception that caused the abort; can be
     *      {@code null}.
     *  @throws IllegalStateException
     *      {@link #close()}
     *      was already called on this instance.
     */
    @SuppressWarnings( "PublicMethodNotExposedInInterface" )
    public final void sendReport( final PerformanceTrackerImpl tracker, final String message, final Throwable cause ) throws IllegalStateException
    {
        if( checkActive() && nonNull( tracker ) )
        {
            final var report = new PerformanceReportImpl( tracker, message, cause );
            final var section = tracker.getPerformanceSection();
            final var sendReportForAbort = section.isSendingReportForAbort();
            @SuppressWarnings( "LocalVariableNamingConvention" )
            final var sendReportOnlyForThresholdExceeded = section.isSendingReportOnlyForExceededThreshold();
            final var isTimedOut = tracker.isTimedOut();
            final var isThresholdExceeded = tracker.isThresholdExceeded();
            final var elapsedTime = tracker.getElapsedTime();
            switch( tracker.getStatus() )
            {
                case STATUS_ABORTED ->
                {
                    if( isTimedOut || sendReportForAbort )
                    {
                        m_MBean.receivePerformanceReport( report );
                    }
                }

                case STATUS_STOPPED ->
                {
                    if( elapsedTime.isPresent() && (!sendReportOnlyForThresholdExceeded || isThresholdExceeded) )
                    {
                        m_MBean.receivePerformanceReport( report );
                    }
                }

                default -> { /* Do nothing – although this case should never happen */ }
            }
        }
    }   //  sendReport()
}
//  class PerfLogManagerImpl

/*
 *  End of File
 */