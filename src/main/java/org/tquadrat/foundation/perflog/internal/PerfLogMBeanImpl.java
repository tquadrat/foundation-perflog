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

import static java.lang.Boolean.TRUE;
import static java.lang.System.currentTimeMillis;
import static java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME;
import static java.util.concurrent.Executors.newThreadPerTaskExecutor;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.apiguardian.api.API.Status.INTERNAL;
import static org.tquadrat.foundation.lang.Objects.isNull;
import static org.tquadrat.foundation.lang.Objects.requireNonNullArgument;
import static org.tquadrat.foundation.lang.Objects.requireNotBlankArgument;
import static org.tquadrat.foundation.perflog.PerfLogUtils.createPerformanceSectionName;
import static org.tquadrat.foundation.perflog.PerfLogUtils.getPerfLogMBeanObjectName;
import static org.tquadrat.foundation.perflog.remote.PerfLogRemote.JSONField_AbortedRuns;
import static org.tquadrat.foundation.perflog.remote.PerfLogRemote.JSONField_CompletedRuns;
import static org.tquadrat.foundation.perflog.remote.PerfLogRemote.JSONField_Error;
import static org.tquadrat.foundation.perflog.remote.PerfLogRemote.JSONField_FirstStart;
import static org.tquadrat.foundation.perflog.remote.PerfLogRemote.JSONField_LastUpdated;
import static org.tquadrat.foundation.perflog.remote.PerfLogRemote.JSONField_Message;
import static org.tquadrat.foundation.perflog.remote.PerfLogRemote.JSONField_Section;
import static org.tquadrat.foundation.perflog.remote.PerfLogRemote.JSONField_SectionDescription;
import static org.tquadrat.foundation.perflog.remote.PerfLogRemote.JSONField_SectionIgnored;
import static org.tquadrat.foundation.perflog.remote.PerfLogRemote.JSONField_SectionName;
import static org.tquadrat.foundation.perflog.remote.PerfLogRemote.JSONField_SectionStatistics;
import static org.tquadrat.foundation.perflog.remote.PerfLogRemote.JSONField_SectionThreshold;
import static org.tquadrat.foundation.perflog.remote.PerfLogRemote.JSONField_SectionThresholdOnlyReport;
import static org.tquadrat.foundation.perflog.remote.PerfLogRemote.JSONField_SectionTimeout;
import static org.tquadrat.foundation.perflog.remote.PerfLogRemote.JSONField_Success;
import static org.tquadrat.foundation.perflog.remote.PerfLogRemote.JSONField_ThresholdExceededRuns;
import static org.tquadrat.foundation.perflog.remote.PerfLogRemote.JSONField_TimedOutRuns;
import static org.tquadrat.foundation.perflog.remote.PerfLogRemote.NOTIFICATION_Type;
import static org.tquadrat.foundation.util.StringUtils.isNotEmptyOrBlank;
import static org.tquadrat.foundation.value.Time.MILLISECOND;

import javax.management.Descriptor;
import javax.management.ImmutableDescriptor;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.StandardMBean;
import javax.management.openmbean.ArrayType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenMBeanAttributeInfo;
import javax.management.openmbean.OpenMBeanAttributeInfoSupport;
import javax.management.openmbean.OpenMBeanConstructorInfo;
import javax.management.openmbean.OpenMBeanInfoSupport;
import javax.management.openmbean.OpenMBeanOperationInfo;
import javax.management.openmbean.OpenMBeanOperationInfoSupport;
import javax.management.openmbean.OpenMBeanParameterInfo;
import javax.management.openmbean.OpenMBeanParameterInfoSupport;
import javax.management.openmbean.SimpleType;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apiguardian.api.API;
import org.tquadrat.foundation.annotation.ClassVersion;
import org.tquadrat.foundation.exception.ImpossibleExceptionError;
import org.tquadrat.foundation.jsonbuilder.JSONBuilder;
import org.tquadrat.foundation.jsonbuilder.JSONObject;
import org.tquadrat.foundation.lang.AutoLock;
import org.tquadrat.foundation.lang.AutoLock.ExecutionFailedException;
import org.tquadrat.foundation.perflog.PerfLogMBean;
import org.tquadrat.foundation.perflog.PerformanceReport;
import org.tquadrat.foundation.perflog.PerformanceSection;
import org.tquadrat.foundation.perflog.PerformanceSectionName;

/**
 *  <p>{@summary The implementation of the interface
 *  {@link PerfLogMBean}.}</p>
 *
 *  @extauthor Thomas Thrien - thomas.thrien@tquadrat.org
 *  @version $Id: PerfLogMBeanImpl.java 1229 2026-05-04 19:11:41Z tquadrat $
 *  @since 0.25.0
 *
 *  @UMLGraph.link
 */
@ClassVersion( sourceVersion = "$Id: PerfLogMBeanImpl.java 1229 2026-05-04 19:11:41Z tquadrat $" )
@API( status = INTERNAL, since = "0.25.0" )
public class PerfLogMBeanImpl extends StandardMBean implements PerfLogMBean
{
        /*------------*\
    ====** Attributes **=======================================================
        \*------------*/
    /**
     *  The last 10 exceptions that caused notification threads to abort.
     */
    private final List<String> m_Exceptions = new LinkedList<>();

    /**
     *  The instance of
     *  {@link JSONBuilder}
     *  used by this MBean.
     */
    private final JSONBuilder m_JSONBuilder;

    /**
     *  <p>{@summary The sequence number for the notifications.} The initial
     *  value will be the current time in seconds since the beginning of the
     *  epoche, rounded down to ten seconds and multiplied with 10k.</p>
     */
    private final AtomicLong m_NotificationSequenceNumber;

    /**
     *  The implementation of
     *  {@link javax.management.NotificationEmitter}
     *  that is utilised by this MBean instance.
     */
    private final NotificationBroadcasterSupport m_NotificationBroadcasterSupport;

    /**
     *  <p>{@summary The registry for the performance section info
     *  instances.}</p>
     *  <p>The key for the map is the name of the performance section.</p>
     */
    private final Map<PerformanceSectionName,PerformanceSectionInfo> m_PerfSectionRegistry = new HashMap<>();

    /**
     *  The guard for read operations on the performance section registry.
     */
    private final AutoLock m_PerfRegistryReadGuard;

    /**
     *  The guard for write operations on the performance section registry.
     */
    private final AutoLock m_PerfRegistryWriteGuard;

    /**
     *  The thread pool that is used to send the notifications.
     *
     *  @see #m_NotificationBroadcasterSupport
     */
    private final ExecutorService m_ThreadPool;

        /*--------------*\
    ====** Constructors **=====================================================
        \*--------------*/
    /**
     *  Creates a new instance of {@code PerfLogMBeanImpl}.
     */
    public PerfLogMBeanImpl()
    {
        super( PerfLogMBean.class, false );

        m_JSONBuilder = JSONBuilder.getInstance();

        final var lock = new ReentrantReadWriteLock();
        m_PerfRegistryReadGuard = AutoLock.of( lock.readLock() );
        m_PerfRegistryWriteGuard = AutoLock.of( lock.writeLock() );

        //---* Create the ExecutorService for the notifications *--------------
        final var threadName = "NotificationThread";
        final var factory = Thread.ofVirtual()
            .name( threadName, 0L )
            .uncaughtExceptionHandler( this::uncaughtExceptionHandler )
            .factory();
        m_ThreadPool = newThreadPerTaskExecutor( factory );

        //---* Create the NotificationBroadcasterSupport *---------------------
        final var notificationInfo = getMBeanInfo().getNotifications();
        m_NotificationSequenceNumber = new AtomicLong( currentTimeMillis() / 10_000 * 10_000 );
        m_NotificationBroadcasterSupport = new NotificationBroadcasterSupport( m_ThreadPool, notificationInfo );
    }   //  PerfLogMBeanImpl()

        /*---------*\
    ====** Methods **==========================================================
        \*---------*/
    /**
     *  Adds an error message to the given
     *  {@link JSONObject}.
     *
     *  @param  object  The JSON object.
     *  @param  message The error message.
     */
    private final void addErrorToJSON( final JSONObject object, final String message )
    {
        final var error = requireNonNullArgument( object, "object" ).setObject( JSONField_Error );
        error.set( JSONField_Message, requireNotBlankArgument( message, "message" ) );
    }   //  addErrorToJSON()

    /**
     *  Adds a success message to the given
     *  {@link JSONObject}.
     *
     *  @param  object  The JSON object.
     *  @param  message The error message.
     */
    private final void addSuccessToJSON( final JSONObject object, final String message )
    {
        final var error = requireNonNullArgument( object, "object" ).setObject( JSONField_Success );
        error.set( JSONField_Message, requireNotBlankArgument( message, "message" ) );
    }   //  addErrorToJSON()

    /**
     *  {@inheritDoc}
     */
    @Override
    public final void addNotificationListener( final NotificationListener listener, final NotificationFilter filter, final Object handback )
    {
        m_NotificationBroadcasterSupport.addNotificationListener( listener, filter, handback );
    }   //  addNotificationListener()

    /**
     *  {@inheritDoc}
     */
    @Override
    public final void addPerformanceSection( final PerformanceSection definition )
    {
        try
        {
            m_PerfRegistryWriteGuard.execute( () -> m_PerfSectionRegistry.put( requireNonNullArgument( definition, "definition" ).getName(), new PerformanceSectionInfo( definition ) ) );
        }
        catch( final ExecutionFailedException e )
        {
            final var cause = e.getCause();
            if( cause instanceof final IllegalArgumentException iae ) throw iae;
            throw e;
        }
    }   //  addPerformanceSection()

    /**
     *  Builds the attribute info list for
     *  {@link #getMBeanInfo()}.
     *
     *  @return The attribute info list.
     */
    private final OpenMBeanAttributeInfo [] buildAttributeInfoList()
    {
        final Collection<OpenMBeanAttributeInfo> buffer = new ArrayList<>();

        try
        {
            //---* The list of notification exceptions *-----------------------
            buffer.add( new OpenMBeanAttributeInfoSupport(
                "NotificationExceptions",
                "The last 10 exceptions thrown by a notification thread",
                new ArrayType<String []>( SimpleType.STRING, false ),
                true, // readable
                false, // not writable
                false // standard get naming
                ) );

            //---* The notification sequence number *--------------------------
            buffer.add( new OpenMBeanAttributeInfoSupport(
                "NotificationSequenceNumber",
                "The last sequence number used for a notification",
                SimpleType.LONG,
                true, // readable
                false, // not writable
                false // standard get naming
                ) );

            //---* The list of currently defined performance sections *--------
            buffer.add( new OpenMBeanAttributeInfoSupport(
                "PerformanceSections",
                "The currently known Performance Sections",
                new ArrayType<String []>( SimpleType.STRING, false ),
                true, // readable
                false, // not writable
                false // standard get naming
                ) );
        }
        catch( final OpenDataException e )
        {
            throw new ImpossibleExceptionError( e );
        }

        final var retValue = buffer.toArray( OpenMBeanAttributeInfo []::new );

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  buildAttributeInfoList()

    /**
     *  Builds the notifications info list for
     *  {@link #getMBeanInfo()}.
     *
     *  @return The notifications info list.
     */
    private final MBeanNotificationInfo [] buildNotificationInfoList()
    {
        final Collection<MBeanNotificationInfo> buffer = new ArrayList<>();

        buffer.add( new MBeanNotificationInfo( new String[]{NOTIFICATION_Type}, Notification.class.getName(), NOTIFICATION_Description ) );

        final var retValue = buffer.toArray( MBeanNotificationInfo []::new );

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  buildNotificationInfoList()

    /**
     *  Builds the operation info list for
     *  {@link #getMBeanInfo()}.
     *
     *  @return The operation info list.
     */
    private final OpenMBeanOperationInfo [] buildOperationInfoList()
    {
        final Collection<OpenMBeanOperationInfo> buffer = new ArrayList<>();

//        try
//        {
            //---* The status for a performance sections *---------------------
            var params = new OpenMBeanParameterInfo[]
                { new OpenMBeanParameterInfoSupport( "name", "The Name of the Performance Section to inspect", SimpleType.STRING ) };
            buffer.add( new OpenMBeanOperationInfoSupport(
                "showPerformanceSection",
                "Shows the current status of the given Performance Section",
                params,
                SimpleType.STRING,
                MBeanOperationInfo.INFO
                ) );

            //---* Enable a performance sections *-----------------------------
            params = new OpenMBeanParameterInfo[]
                { new OpenMBeanParameterInfoSupport( "name", "The Name of the Performance Section to enable", SimpleType.STRING ) };
            buffer.add( new OpenMBeanOperationInfoSupport(
                "enablePerformanceSection",
                "Enable the given Performance Section",
                params,
                SimpleType.STRING,
                MBeanOperationInfo.ACTION
                ) );

            //---* Disable a performance sections *----------------------------
            params = new OpenMBeanParameterInfo[]
                { new OpenMBeanParameterInfoSupport( "name", "The Name of the Performance Section to disable", SimpleType.STRING ) };
            buffer.add( new OpenMBeanOperationInfoSupport(
                "disablePerformanceSection",
                "Disable the given Performance Section",
                params,
                SimpleType.STRING,
                MBeanOperationInfo.ACTION
                ) );
//        }
//        catch( final OpenDataException e )
//        {
//            throw new ImpossibleExceptionError( e );
//        }

        final var retValue = buffer.toArray( OpenMBeanOperationInfo []::new );

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  buildOperationInfoList()

    /**
     *  {@inheritDoc}
     */
    @Override
    public String disablePerformanceSection( final String name )
    {
        final var json = m_JSONBuilder.createObject();
        if( isNotEmptyOrBlank( name ) )
        {
            try
            {
                final var performanceSectionName = createPerformanceSectionName( name );
                final var performanceSection = getPerformanceSection( performanceSectionName );
                if( performanceSection.isPresent() )
                {
                    final var ps = performanceSection.get();
                    if( ps.isIgnored() )
                    {
                        addSuccessToJSON( json, "Performance Section '%s' is already disabled".formatted( name ) );
                    }
                    else
                    {
                        ps.setIgnoreFlag( true );
                        addSuccessToJSON( json, "Performance Section '%s' successfully disabled".formatted( name ) );
                    }
                }
                else
                {
                    addErrorToJSON( json, "No entry for this name: %s".formatted( name ) );
                }
            }
            catch( final IllegalArgumentException e )
            {
                addErrorToJSON( json, "The given name '%s' is invalid: %s".formatted( name, e.getMessage() ) );
            }
        }
        else
        {
            addErrorToJSON( json, "The given name is null, empty or blank" );
        }

        final var retValue = json.toString();

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  disablePerformanceSection()

    /**
     *  {@inheritDoc}
     */
    @Override
    public String enablePerformanceSection( final String name )
    {
        final var json = m_JSONBuilder.createObject();
        if( isNotEmptyOrBlank( name ) )
        {
            try
            {
                final var performanceSectionName = createPerformanceSectionName( name );
                final var performanceSection = getPerformanceSection( performanceSectionName );
                if( performanceSection.isPresent() )
                {
                    final var ps = performanceSection.get();
                    if( !ps.isIgnored() )
                    {
                        addSuccessToJSON( json, "Performance Section '%s' is already enabled".formatted( name ) );
                    }
                    else
                    {
                        ps.setIgnoreFlag( false );
                        addSuccessToJSON( json, "Performance Section '%s' successfully enabled".formatted( name ) );
                    }
                }
                else
                {
                    addErrorToJSON( json, "No entry for this name: %s".formatted( name ) );
                }
            }
            catch( final IllegalArgumentException e )
            {
                addErrorToJSON( json, "The given name '%s' is invalid: %s".formatted( name, e.getMessage() ) );
            }
        }
        else
        {
            addErrorToJSON( json, "The given name is null, empty or blank" );
        }

        final var retValue = json.toString();

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  enablePerformanceSection()

    /**
     *  {@inheritDoc}
     */
    @Override
    public final String[] getNotificationExceptions()
    {
        final var retValue = m_Exceptions.toArray( String []::new );

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  getNotificationExceptions()

    /**
     *  <p>{@summary Get the
     *  {@link MBeanInfo}
     *  for this MBean.} That is in fact an instance of
     *  {@link OpenMBeanInfoSupport}
     *  as an implementation of
     *  {@link javax.management.openmbean.OpenMBeanInfo}.
     *  This identifies this MBean as an Open MBean.</p>
     *  <p>This method implements
     *  {@link javax.management.DynamicMBean#getMBeanInfo() DynamicMBean.getMBeanInfo()}
     *  and overwrites the
     *  {@linkplain StandardMBean#getMBeanInfo() implementation}
     *  from
     *  {@link StandardMBean}.</p>
     *  <p>This method first calls
     *  {@link #getCachedMBeanInfo()}
     *  in order to retrieve the cached {@code MBeanInfo} for this MBean, if
     *  any. If the result from this call is not {@code null}, it will be
     *  returned.</p>
     *  <p>Otherwise, this method builds a {@code MBeanInfo} for this MBean
     *  from scratch.</p>
     *  <p>Finally, it calls
     *  {@link #cacheMBeanInfo(javax.management.MBeanInfo) cacheMBeanInfo()}
     *  in order to cache the new MBeanInfo for subsequent calls.
     *
     *  @return The cached {@code MBeanInfo} for this MBean, if not
     *      {@code null}, or a newly built {@code MBeanInfo} if none was
     *      cached.
     */
    @SuppressWarnings( "UnnecessaryJavaDocLink" )
    @Override
    public final MBeanInfo getMBeanInfo()
    {
        var retValue = getCachedMBeanInfo();
        if( isNull( retValue ) )
        {
            final var className = getClass().getName();

            //---* Set the attributes *----------------------------------------
            final var attributes = buildAttributeInfoList();

            //---* Set the constructors *--------------------------------------
            /*
             * We do not publish the constructors for this MBean.
             */
            final OpenMBeanConstructorInfo[] constructors = null;

            //---* Set the operations *----------------------------------------
            final var operations = buildOperationInfoList();

            //---* Set the notification infos *--------------------------------
            final var notificationInfos = buildNotificationInfoList();

            //---* Set the descriptor *----------------------------------------
            final Map<String,?> descriptorFields = Map.of( "immutableInfo", TRUE.toString() );
            final Descriptor descriptor = new ImmutableDescriptor( descriptorFields );

            //---* Create the return value *-----------------------------------
            //noinspection ConstantValue
            retValue = new OpenMBeanInfoSupport( className, DESCRIPTION, attributes, constructors, operations, notificationInfos, descriptor );

            //---* Keep the new MBeanInfo *------------------------------------
            cacheMBeanInfo( retValue );
        }

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  getMBeanInfo()

    /**
     *  {@inheritDoc}
     */
    @Override
    public final MBeanNotificationInfo[] getNotificationInfo()
    {
        return getMBeanInfo().getNotifications();
    }   //  getNotificationInfo()

    /**
     *  {@inheritDoc}
     */
    @Override
    public final long getNotificationSequenceNumber() { return m_NotificationSequenceNumber.get(); }

    /**
     *  {@inheritDoc}
     */
    @Override
    public final Optional<PerformanceSection> getPerformanceSection( final PerformanceSectionName name )
    {
        final Optional<PerformanceSection> retValue;
        try
        {
            retValue = m_PerfRegistryReadGuard.execute( () -> m_PerfSectionRegistry.get( requireNonNullArgument( name, "name" ) ) )
                .map( PerformanceSectionInfo::getPerformanceSection );
        }
        catch( final ExecutionFailedException e )
        {
            final var cause = e.getCause();
            if( cause instanceof final IllegalArgumentException iae ) throw iae;
            throw e;
        }

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  getPerformanceSection()

    /**
     *  {@inheritDoc}
     */
    @Override
    public final String [] getPerformanceSections()
    {
        final String [] retValue;
        try( final var _ = m_PerfRegistryReadGuard.lock() )
        {
            retValue = m_PerfSectionRegistry.keySet()
                .stream()
                .sorted()
                .map( PerformanceSectionName::toString )
                .toArray( String []::new );
        }

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  getPerformanceSections()

    /**
     *  {@inheritDoc}
     */
    @Override
    public final void postDeregister()
    {
        super.postDeregister();

        //---* Shutdown the thread pool *--------------------------------------
        try
        {
            m_ThreadPool.shutdown();
            //noinspection ResultOfMethodCallIgnored
            m_ThreadPool.awaitTermination( 1L, MINUTES );
        }
        catch( final InterruptedException _ )
        {
            /*
             * Deliberately ignored!
             */
        }
    }   //  postDeregister()

    /**
     *  {@inheritDoc}
     *  <p>Finally, this method sends a notification.</p>
     *  <p>If an
     *  {@link java.util.concurrent.Executor}
     *  was specified in the constructor for the
     *  {@link NotificationBroadcasterSupport}
     *  instance, it will be given one task per selected listener to deliver
     *  the notification to that listener.</p>
     */
    @Override
    public final void receivePerformanceReport( final PerformanceReport report )
    {
        //---* Update info *---------------------------------------------------
        final var sectionName = requireNonNullArgument( report, "report" ).getPerformanceSection().getName();
        final var sectionInfo = m_PerfRegistryReadGuard.execute( () -> m_PerfSectionRegistry.get( sectionName ) );
        sectionInfo.ifPresent( perfSectionInfo -> perfSectionInfo.processTracker( (PerformanceTrackerImpl) report.getPerformanceTracker() ) );

        //---* Compose the notification and send it *--------------------------
        final var notification = new Notification( NOTIFICATION_Type, getPerfLogMBeanObjectName(), m_NotificationSequenceNumber.getAndIncrement(), currentTimeMillis(), ((PerformanceReportImpl) report).toJSON() );

        m_NotificationBroadcasterSupport.sendNotification( notification );
    }   //  receivePerformanceReport()

    /**
     *  {@inheritDoc}
     */
    @Override
    public final void removeNotificationListener( final NotificationListener listener ) throws ListenerNotFoundException
    {
        m_NotificationBroadcasterSupport.removeNotificationListener( listener );
    }   //  removeNotificationListener()

    /**
     *  {@inheritDoc}
     */
    @Override
    public final void removeNotificationListener( final NotificationListener listener, final NotificationFilter filter, final Object handback ) throws ListenerNotFoundException
    {
        m_NotificationBroadcasterSupport.removeNotificationListener( listener, filter, handback );
    }   //  removeNotificationListener()

    /**
     *  {@inheritDoc}
     */
    @Override
    public final PerformanceSection retrievePerformanceSection( final PerformanceSectionName name )
    {
        final PerformanceSection retValue;
        try( final var _ = m_PerfRegistryWriteGuard.lock() )
        {
            final var sectionInfo = m_PerfSectionRegistry.computeIfAbsent( requireNonNullArgument( name, "name" ), PerformanceSectionInfo::new );
            retValue = sectionInfo.getPerformanceSection();
        }

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  retrievePerformanceSection()

    /**
     *  {@inheritDoc}
     */
    @Override
    public final String showPerformanceSection( final String name )
    {
        final var json = m_JSONBuilder.createObject();
        if( isNotEmptyOrBlank( name ) )
        {
            try
            {
                final var performanceSectionName = createPerformanceSectionName( name );
                final var performanceSectionInfo = m_PerfRegistryReadGuard.execute( () -> m_PerfSectionRegistry.get( performanceSectionName ) );
                if( performanceSectionInfo.isPresent() )
                {
                    final var sectionInfo = performanceSectionInfo.get();
                    final var section = json.setObject( JSONField_Section );
                    section.set( JSONField_SectionName, name )
                        .set( JSONField_SectionDescription, sectionInfo.getDescription() )
                        .set( JSONField_SectionIgnored, sectionInfo.isIgnored() )
                        .set( JSONField_SectionThresholdOnlyReport, sectionInfo.isSendingReportOnlyForExceededThreshold() );
                    sectionInfo.getThreshold().ifPresentOrElse( v -> section.set( JSONField_SectionThreshold, v, MILLISECOND ), () -> section.set( JSONField_SectionThreshold, "disabled" ) );
                    sectionInfo.getTimeout().ifPresentOrElse( v -> section.set( JSONField_SectionTimeout, v, MILLISECOND ), () -> section.set( JSONField_SectionTimeout, "disabled" ) );
                    final var statistics = m_JSONBuilder.createObject();
                    sectionInfo.getFirstStart().ifPresent( v -> statistics.set( JSONField_FirstStart, v.format( ISO_ZONED_DATE_TIME ) ) );
                    sectionInfo.getLastUpdated().ifPresent( v -> statistics.set( JSONField_LastUpdated, v.format( ISO_ZONED_DATE_TIME ) ) );
                    sectionInfo.getNumberOfCompletedRuns().ifPresent( v -> statistics.set( JSONField_CompletedRuns, v ) );
                    sectionInfo.getNumberOfRunsThatExceededThreshold().ifPresent( v -> json.set( JSONField_ThresholdExceededRuns, v ) );
                    sectionInfo.getNumberOfAbortedRuns().ifPresent( v -> statistics.set( JSONField_AbortedRuns, v ) );
                    sectionInfo.getNumberOfTimedOutRuns().ifPresent( v -> statistics.set( JSONField_TimedOutRuns, v ) );
                    if( !statistics.isEmpty() ) section.set( JSONField_SectionStatistics, statistics );
                }
                else
                {
                    addErrorToJSON( json, "No entry for this name: %s".formatted( name ) );
                }
            }
            catch( final IllegalArgumentException e )
            {
                addErrorToJSON( json, "The given name '%s' is invalid: %s".formatted( name, e.getMessage() ) );
            }
        }
        else
        {
            addErrorToJSON( json, "The given name is null, empty or blank" );
        }

        final var retValue = "%s".formatted( json );

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  showPerformanceSection()

    /**
     *  <p>{@summary This method is invoked when the given thread terminates
     *  due to the given uncaught exception.}</p>
     *  <p>Any exception thrown by this method will be ignored by the Java
     *  Virtual Machine.</p>
     *  <p>This
     *  {@link Thread.UncaughtExceptionHandler}
     *  will be assigned to the
     *  {@link java.util.concurrent.ThreadFactory}
     *  used by the
     *  {@linkplain #m_ThreadPool thread pool}
     *  for the
     *  {@link NotificationBroadcasterSupport}
     *  instance.</p>
     *
     *  @param  t   The thread.
     *  @param  e   The exception.
     *
     *  @see Thread.UncaughtExceptionHandler#uncaughtException(Thread,Throwable)
     */
    private final void uncaughtExceptionHandler( final Thread t, final Throwable e )
    {
        m_Exceptions.addFirst( "%s - Thread %s aborted: %s".formatted( Instant.now(), t.getName(), e.toString() ) );
        while( m_Exceptions.size() > 10 ) m_Exceptions.removeLast();
    }   //  uncaughtExceptionHandler()
}
//  class PerfLogMBeanImpl

/*
 *  End of File
 */