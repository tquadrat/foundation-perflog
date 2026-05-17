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

import static java.lang.Boolean.getBoolean;
import static java.lang.management.ManagementFactory.getPlatformMBeanServer;
import static javax.management.MBeanServerFactory.createMBeanServer;
import static org.apiguardian.api.API.Status.STABLE;
import static org.tquadrat.foundation.lang.Objects.isNull;
import static org.tquadrat.foundation.lang.Objects.nonNull;
import static org.tquadrat.foundation.lang.Objects.requireNonNullArgument;
import static org.tquadrat.foundation.perflog.PerformanceTracker.TrackerStatus.STATUS_READY;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Optional;

import org.apiguardian.api.API;
import org.tquadrat.foundation.annotation.ClassVersion;
import org.tquadrat.foundation.annotation.UtilityClass;
import org.tquadrat.foundation.exception.PrivateConstructorForStaticClassCalledError;
import org.tquadrat.foundation.lang.StringConverter;
import org.tquadrat.foundation.perflog.internal.PerfLogManagerImpl;
import org.tquadrat.foundation.perflog.internal.PerformanceSectionNameImpl;
import org.tquadrat.foundation.perflog.internal.PerformanceTrackerImpl;
import org.tquadrat.foundation.perflog.remote.PerfLogRemote;

/**
 *  <p>{@summary Several tools for the Foundation Performance Logging and
 *  Monitoring.}</p>
 *
 *  @extauthor Thomas Thrien - thomas.thrien@tquadrat.org
 *  @version $Id: PerfLogUtils.java 1246 2026-05-16 14:07:00Z tquadrat $
 *  @since 0.25.0
 *
 *  @UMLGraph.link
 */
@ClassVersion( sourceVersion = "$Id: PerfLogUtils.java 1246 2026-05-16 14:07:00Z tquadrat $" )
@API( status = STABLE, since = "0.25.0" )
@UtilityClass
public final class PerfLogUtils
{
        /*---------------*\
    ====** Inner Classes **====================================================
        \*---------------*/
    /**
     *  <p>{@summary A holder for
     *  {@link PerformanceTracker}
     *  that allows to use it with try-with-resources.}</p>
     *
     *  @extauthor Thomas Thrien - thomas.thrien@tquadrat.org
     *  @version $Id: PerfLogUtils.java 1246 2026-05-16 14:07:00Z tquadrat $
     *  @since 0.25.0
     *
     *  @UMLGraph.link
     */
    @ClassVersion( sourceVersion = "$Id: PerfLogUtils.java 1246 2026-05-16 14:07:00Z tquadrat $" )
    @API( status = STABLE, since = "0.25.0" )
    public static final class PerformanceTrackerHolder implements AutoCloseable, PerformanceTracker
    {
            /*------------*\
        ====** Attributes **====================================================
            \*------------*/
        /**
         *  The flag that indicates whether
         *  {@link #close()}
         *  was already called on this instance.
         */
        private boolean m_IsClosed = false;

        /**
         *  <p>{@summary The reference to a volatile instance of
         *  {@link PerfLogManager}.}</p>
         *  <p>It is kept here to prevent it from prematurely being garbage
         *  collected.</p>
         */
        private PerfLogManager m_Manager;

        /**
         *  <p>{@summary The wrapped instance of
         *  {@link PerformanceTracker}.}</p>
         *  <p>It can be {@code null}.</p>
         */
        private final PerformanceTracker m_Tracker;

            /*--------------*\
        ====** Constructors **=================================================
            \*--------------*/
        /**
         *  <p>{@summary Creates a new instance of
         *  {@code PerformanceTrackerHolder} that holds an instance of
         *  {@link PerformanceTracker}.}</p>
         *
         *  @param  tracker The tracker instance to wrap.
         *  @throws ClassCastException  If called with an instance of
         *      {@code Holder}.
         */
        private PerformanceTrackerHolder( final PerformanceTrackerImpl tracker )
        {
            m_Tracker = requireNonNullArgument( tracker, "tracker" );
            m_Manager = null;
        }   //  PerformanceTrackerHolder()

        /**
         *  <p>{@summary Creates a new instance of
         *  {@code PerformanceTrackerHolder} that holds an instance of
         *  {@link PerformanceTracker}, created by calling
         *  {@link PerfLogManager#createPerformanceTracker(PerformanceSectionName)}
         *  on the given
         *  {@link PerfLogManager}
         *  instance with the given
         *  {@link PerformanceSectionName}.}</p>
         *
         *  @param  manager The Performance Logging manager.
         *  @param  name    The performance section name.
         */
        private PerformanceTrackerHolder( final PerfLogManager manager, final PerformanceSectionName name )
        {
            final var trackerOptional = requireNonNullArgument( manager, "manager" ).createPerformanceTracker( name );
            if( trackerOptional.isEmpty() )
            {
                m_Tracker = null;
                m_Manager = null;
            }
            else
            {
                m_Tracker = trackerOptional.get();
                m_Manager = manager;
            }
        }   //  PerformanceTrackerHolder()

        /**
         *  Creates a new instance of {@code PerformanceTrackerHolder} that
         *  does not hold an instance of
         *  {@link PerformanceTracker}.
         */
        private PerformanceTrackerHolder()
        {
            m_Tracker = null;
            m_Manager = null;
        }   //  PerformanceTrackerHolder()

            /*---------*\
        ====** Methods **======================================================
            \*---------*/
        /**
         *  {@inheritDoc}
         */
        @Override
        public final  void abort()
        {
            if( nonNull( m_Tracker ) ) m_Tracker.abort();
        }   //  abort()

        /**
         *  {@inheritDoc}
         */
        @Override
        public void abort( final String message )
        {
            if( nonNull( m_Tracker ) ) m_Tracker.abort( message );
        }   //  abort()

        /**
         *  {@inheritDoc}
         */
        @Override
        public void abort( final String message, final Throwable cause )
        {
            if( nonNull( m_Tracker ) ) m_Tracker.abort( message, cause );
        }   //  abort()

        /**
         *  {@inheritDoc}
         */
        @Override
        public final PerformanceTracker addContext( final String name, final String value )
        {
            if( nonNull( m_Tracker ) ) m_Tracker.addContext( name, value );

            //---* Done *------------------------------------------------------
            return this;
        }   //  addContext()

        /**
         *  {@inheritDoc}
         */
        @Override
        public final <T> PerformanceTracker addContext( final String name, final T value, final StringConverter<T> stringConverter )
        {
            if( nonNull( m_Tracker ) ) m_Tracker.addContext( name, value, stringConverter );

            //---* Done *------------------------------------------------------
            return this;
        }   //  addContext()

        /**
         *  {@inheritDoc}
         */
        @Override
        public final void close()
        {
            if( isActive() )
            {
                stop();
            }
            else
            {
                abort();
            }
            m_IsClosed = true;
            if( nonNull( m_Manager ) )
            {
                m_Manager.close();
                m_Manager = null;
            }
        }   //  close()

        /**
         *  {@inheritDoc}
         */
        @Override
        public final TrackerStatus getStatus()
        {
            final var retValue = isNull( m_Tracker ) ? STATUS_READY : m_Tracker.getStatus();

            //---* Done *------------------------------------------------------
            return retValue;
        }   //  getStatus()

        /**
         *  {@inheritDoc}
         *  <p>Throws also an
         *  {@link IllegalStateException}
         *  if this instance of {@code PerformanceTrackerHolder} was already
         *  closed.</p>
         */
        @Override
        public final PerformanceTracker reset( final boolean resetContext ) throws IllegalArgumentException
        {
            if( m_IsClosed ) throw new IllegalStateException( "Holder was already closed" );
            if( nonNull( m_Tracker ) ) m_Tracker.reset( resetContext );

            //---* Done *------------------------------------------------------
            return this;
        }   //  reset()

        /**
         *  {@inheritDoc}
         */
        @Override
        public final void start() throws IllegalArgumentException
        {
            if( nonNull( m_Tracker ) ) m_Tracker.start();
        }   //  start()

        /**
         *  {@inheritDoc}
         */
        @Override
        public final void stop() throws IllegalArgumentException
        {
            if( nonNull( m_Tracker ) ) m_Tracker.stop();
        }   //  stop()
    }
    //  class PerformanceTrackerHolder

        /*-----------*\
    ====** Constants **========================================================
        \*-----------*/
    /**
     *  <p>{@summary The domain name part of the
     *  {@link ObjectName}
     *  identifying the
     *  {@link PerfLogMBean}
     *  in the MBean server: {@value PerfLogRemote#DOMAIN_NAME}}.</p>
     *  <p>It is also used for the creation of the MBean server, if a dedicated
     *  MBean server is needed; usually the Platform MBean server is used.</p>
     *
     *  @see javax.management.MBeanServerFactory#createMBeanServer(String)
     *  @see java.lang.management.ManagementFactory#getPlatformMBeanServer
     */
    public static final String DOMAIN_NAME = PerfLogRemote.DOMAIN_NAME;

    /**
     *  The type for the
     *  {@link ObjectName}
     *  identifying the
     *  {@link PerfLogMBean}
     *  in the MBean server: {@value PerfLogRemote#MBEAN_TYPE}.
     */
    public static final String MBEAN_TYPE = PerfLogRemote.MBEAN_TYPE;

    /**
     *  <p>{@summary The name of the system property that controls whether to
     *  use a dedicated
     *  {@link MBeanServer}: {@value}.}</p>
     *  <p>If not provided or set to {@code false}, the platform MBean server
     *  is used.</p>
     *
     *  @see java.lang.management.ManagementFactory#getPlatformMBeanServer
     */
    @SuppressWarnings( "FieldNamingConvention" )
    public static final String SYSTEM_PROPERTY_UsedDedicatedMBeanServer = "org.tquadrat.foundation.perflog.UseDedicatedMBeanServer";

        /*--------------*\
    ====** Constructors **=====================================================
        \*--------------*/
    /**
     *  Not instance allowed for this class!
     */
    private PerfLogUtils() { throw new PrivateConstructorForStaticClassCalledError( PerfLogUtils.class ); }

        /*---------*\
    ====** Methods **==========================================================
        \*---------*/
    /**
     *  <p>{@summary Creates an instance of
     *  {@link PerfLogManager}
     *  that handles the connection with the underlying
     *  {@link PerfLogMBean}.}</p>
     *  <p>When called multiple times with references to different
     *  {@linkplain MBeanServer MBean servers},
     *  each of these calls will create a new instance of
     *  {@link PerfLogMBean}.</p>
     *
     *  @param  mbeanServer The MBean server that holds the
     *      {@code PerfLogMBean}.
     *  @return The new performance logging manager.
     *
     *  @see #createPerfLogManager()
     *  @see #createPerfLogManager(UncaughtExceptionHandler)
     *  @see #obtainMBeanServer()
     */
    public static final PerfLogManager createPerfLogManager( final MBeanServer mbeanServer )
    {
        final var retValue = new PerfLogManagerImpl( requireNonNullArgument( mbeanServer, "mbeanServer" ), getPerfLogMBeanObjectName() );

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  createPerfLogManager()

    /**
     *  <p>{@summary Creates an instance of
     *  {@link PerfLogManager}
     *  that handles the connection with the underlying
     *  {@link PerfLogMBean}.}</p>
     *  <p>When called multiple times with references to different
     *  {@linkplain MBeanServer MBean servers},
     *  each of these calls will create a new instance of
     *  {@link PerfLogMBean}.</p>
     *
     *  @param  mbeanServer The MBean server that holds the
     *      {@code PerfLogMBean}.
     *  @param  uncaughtExceptionHandler    The implementation of
     *      {@link Thread.UncaughtExceptionHandler}
     *      that is used for the timeout monitoring thread.
     *  @return The new performance logging manager.
     *
     *  @see #createPerfLogManager()
     *  @see #createPerfLogManager(UncaughtExceptionHandler)
     *  @see #obtainMBeanServer()
     */
    @SuppressWarnings( "MethodParameterNamingConvention" )
    public static final PerfLogManager createPerfLogManager( final MBeanServer mbeanServer, final UncaughtExceptionHandler uncaughtExceptionHandler )
    {
        final var retValue = new PerfLogManagerImpl( requireNonNullArgument( mbeanServer, "mbeanServer" ), getPerfLogMBeanObjectName(), uncaughtExceptionHandler );

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  createPerfLogManager()

    /**
     *  <p>{@summary Creates an instance of
     *  {@link PerfLogManager}
     *  that handles the connection with the underlying
     *  {@link PerfLogMBean}.}</p>
     *  <p>The method uses the
     *  {@link MBeanServer}
     *  that is returned from
     *  {@link #obtainMBeanServer()}.</p>
     *
     *  @return The new performance logging manager.
     */
    public static final PerfLogManager createPerfLogManager()
    {
        final var mbeanServer = obtainMBeanServer();
        final var retValue = createPerfLogManager( mbeanServer );

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  createPerfLogManager()

    /**
     *  <p>{@summary Creates an instance of
     *  {@link PerfLogManager}
     *  that handles the connection with the underlying
     *  {@link PerfLogMBean}.}</p>
     *  <p>The method uses the
     *  {@link MBeanServer}
     *  that is returned from
     *  {@link #obtainMBeanServer()}.</p>
     *
     *  @param  uncaughtExceptionHandler    The implementation of
     *      {@link Thread.UncaughtExceptionHandler}
     *      that is used for the timeout monitoring thread.
     *
     *  @return The new performance logging manager.
     */
    @SuppressWarnings( "MethodParameterNamingConvention" )
    public static final PerfLogManager createPerfLogManager( final UncaughtExceptionHandler uncaughtExceptionHandler )
    {
        final var mbeanServer = obtainMBeanServer();
        final var retValue = new PerfLogManagerImpl( mbeanServer, getPerfLogMBeanObjectName(), uncaughtExceptionHandler );

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  createPerfLogManager()

    /**
     *  Creates a new instance for an implementation of
     *  {@code PerformanceSectionName} based on the given value.
     *
     *  @param  value   The value for the new name.
     *  @return The new name.
     */
    public static PerformanceSectionName createPerformanceSectionName( final String value )
    {
        final var retValue = new PerformanceSectionNameImpl( value );

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  createPerformanceSectionName()

    /**
     *  Returns the
     *  {@link ObjectName}
     *  for the
     *  {@link PerfLogMBean}.
     *
     *  @return The object name for the performance logging MBean.
     */
    public static final ObjectName getPerfLogMBeanObjectName() { return PerfLogRemote.getPerfLogMBeanObjectName(); }

    /**
     *  <p>{@summary Creates a holder for the given
     *  {@link PerformanceTracker}.}</p>
     *  <p>This is a convenience method that allows to use a performance
     *  tracker with {@code try-with-resources}.</p>
     *
     *  @param  tracker An instance of
     *      {@link Optional}
     *      that holds the tracker.
     *  @return A holder for the given tracker.
     *  @throws ClassCastException  If called with an instance of
     *      {@code Holder} instead of a raw
     *      {@link PerformanceTracker}.
     */
    @SuppressWarnings( {"OptionalUsedAsFieldOrParameterType"} )
    public static final PerformanceTrackerHolder hold( final Optional<? extends PerformanceTracker> tracker )
    {
        final var retValue = requireNonNullArgument( tracker, "tracker" ).map( v -> new PerformanceTrackerHolder( (PerformanceTrackerImpl) v ) ).orElseGet( PerformanceTrackerHolder::new );

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  hold()

    /**
     *  <p>{@summary Creates a
     *  {@link PerformanceTracker}
     *  for the
     *  {@link PerformanceSection}
     *  with the given
     *  {@linkplain PerformanceSectionName name}.}</p>
     *  <p>It creates an instance of
     *  {@link PerfLogManager}
     *  on the fly, using the
     *  {@link MBeanServer}
     *  returned by
     *  {@link #obtainMBeanServer()}.</p>
     *  <p>This is a convenience method that allows to use a performance
     *  tracker with {@code try-with-resources}.</p>
     *
     *  @param  name    The name of the performance section.
     *  @return A holder for the given tracker.
     */
    public static final PerformanceTrackerHolder hold( final PerformanceSectionName name )
    {
        final var retValue = new PerformanceTrackerHolder( createPerfLogManager(), name );

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  hold()

    /**
     *  <p>{@summary Creates a
     *  {@link PerformanceTracker}
     *  for the
     *  {@link PerformanceSection}
     *  with the given
     *  {@linkplain PerformanceSectionName name}.}</p>
     *  <p>This is a convenience method that allows to use a performance
     *  tracker with {@code try-with-resources}.</p>
     *
     *  @param  manager The Performance Logging Manager that connects to the
     *      {@link PerfLogMBean}.
     *  @param  name    The name of the performance section.
     *  @return A holder for the given tracker.
     */
    public static final PerformanceTrackerHolder hold(  final PerfLogManager manager, final PerformanceSectionName name )
    {
        final var retValue = hold( requireNonNullArgument( manager, "manager" ).createPerformanceTracker( name ) );

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  hold()

    /**
     *  <p>{@summary Creates a
     *  {@link PerformanceTracker}
     *  for the
     *  {@link PerformanceSection}
     *  with the given name.}</p>
     *  <p>It creates an instance of
     *  {@link PerfLogManager}
     *  on the fly, using the
     *  {@link MBeanServer}
     *  returned by
     *  {@link #obtainMBeanServer()}.</p>
     *  <p>This is a convenience method that allows to use a performance
     *  tracker with {@code try-with-resources}.</p>
     *
     *  @param  value   The name of the performance section.
     *  @return A holder for the given tracker.
     */
    public static final PerformanceTrackerHolder hold( final String value )
    {
        final var retValue = hold( createPerformanceSectionName( value ) );

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  hold()

    /**
     *  <p>{@summary Creates a holder for the given
     *  {@link PerformanceTracker}
     *  and immediately starts it.}</p>
     *  <p>This is a convenience method that allows to use a performance
     *  tracker with {@code try-with-resources}.</p>
     *
     *  @param  tracker An instance of
     *      {@link Optional}
     *      that holds the tracker.
     *  @return A holder for the given tracker.
     */
    @SuppressWarnings( "OptionalUsedAsFieldOrParameterType" )
    public static final PerformanceTrackerHolder holdAndStart( final Optional<? extends PerformanceTracker> tracker )
    {
        final var retValue = hold( tracker );
        retValue.start();

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  holdAndStart()

    /**
     *  <p>{@summary Creates a
     *  {@link PerformanceTracker}
     *  for the
     *  {@link PerformanceSection}
     *  with the given
     *  {@linkplain PerformanceSectionName name}
     *  and immediately starts it.}</p>
     *  <p>It creates an instance of
     *  {@link PerfLogManager}
     *  on the fly, using the
     *  {@link MBeanServer}
     *  returned by
     *  {@link #obtainMBeanServer()}.</p>
     *  <p>This is a convenience method that allows to use a performance
     *  tracker with {@code try-with-resources}.</p>
     *
     *  @param  name    The name of the performance section.
     *  @return A holder for the given tracker.
     */
    public static final PerformanceTrackerHolder holdAndStart( final PerformanceSectionName name )
    {
        final var retValue = hold( name );
        retValue.start();

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  holdAndStart()

    /**
     *  <p>{@summary Creates a
     *  {@link PerformanceTracker}
     *  for the
     *  {@link PerformanceSection}
     *  with the given
     *  {@linkplain PerformanceSectionName name}
     *  and immediately starts it.}</p>
     *  <p>It creates an instance of
     *  {@link PerfLogManager}
     *  on the fly, using the
     *  {@link MBeanServer}
     *  returned by
     *  {@link #obtainMBeanServer()}.</p>
     *  <p>This is a convenience method that allows to use a performance
     *  tracker with {@code try-with-resources}.</p>
     *
     *  @param  manager The Performance Logging Manager that connects to the
     *      {@link PerfLogMBean}.
     *  @param  name    The name of the performance section.
     *  @return A holder for the given tracker.
     */
    public static final PerformanceTrackerHolder holdAndStart( final PerfLogManager manager, final PerformanceSectionName name )
    {
        final var retValue = hold( manager, name );
        retValue.start();

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  holdAndStart()

    /**
     *  <p>{@summary Creates a
     *  {@link PerformanceTracker}
     *  for the
     *  {@link PerformanceSection}
     *  with the given name and immediately starts it.}</p>
     *  <p>It creates an instance of
     *  {@link PerfLogManager}
     *  on the fly, using the
     *  {@link MBeanServer}
     *  returned by
     *  {@link #obtainMBeanServer()}.</p>
     *  <p>This is a convenience method that allows to use a performance
     *  tracker with {@code try-with-resources}.</p>
     *
     *  @param  value   The name of the performance section.
     *  @return A holder for the given tracker.
     */
    public static final PerformanceTrackerHolder holdAndStart( final String value )
    {
        final var retValue = hold( value );
        retValue.start();

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  holdAndStart()

    /**
     *  <p>{@summary Retrieves the
     *  {@link MBeanServer}
     *  that is used for the registration of the
     *  {@link PerfLogMBean}.}</p>
     *  <p>If the system property
     *  {@value #SYSTEM_PROPERTY_UsedDedicatedMBeanServer}
     *  is set to {@code true}, a dedicated MBean server for the domain
     *  {@value PerfLogRemote#DOMAIN_NAME}
     *  will be used, otherwise the MBean server that is returned by
     *  {@link java.lang.management.ManagementFactory#getPlatformMBeanServer()}
     *  is returned.</p>
     *
     *  @return The MBean server.
     *
     *  @see #SYSTEM_PROPERTY_UsedDedicatedMBeanServer
     */
    public static final MBeanServer obtainMBeanServer()
    {
        final MBeanServer retValue;
        //noinspection AccessOfSystemProperties
        if( getBoolean( SYSTEM_PROPERTY_UsedDedicatedMBeanServer ) )
        {
            retValue = MBeanServerFactory.findMBeanServer( null )
                .stream()
                .filter( mbs -> mbs.getDefaultDomain().equals( DOMAIN_NAME ) )
                .findFirst()
                .orElseGet( () -> createMBeanServer( DOMAIN_NAME ) );
        }
        else
        {
            retValue = getPlatformMBeanServer();
        }

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  obtainMBeanServer()
}
//  class PerfLogUtils

/*
 *  End of File
 */