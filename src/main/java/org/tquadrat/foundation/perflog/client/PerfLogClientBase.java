/*
 * ============================================================================
 *  Copyright © 2002-2026 by Thomas Thrien.
 *  All Rights Reserved.
 * ============================================================================
 *  Licensed to the public under the agreements of the GNU Lesser General Public
 *  License, version 3.0 (the "License"). You may obtain a copy of the License at
 *
 *       http://www.gnu.org/licenses/lgpl.html
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations
 *  under the License.
 */

package org.tquadrat.foundation.perflog.client;

import static javax.management.JMX.newMBeanProxy;
import static org.apiguardian.api.API.Status.INTERNAL;
import static org.apiguardian.api.API.Status.MAINTAINED;
import static org.tquadrat.foundation.lang.Objects.nonNull;
import static org.tquadrat.foundation.lang.Objects.requireNonNullArgument;
import static org.tquadrat.foundation.perflog.PerfLogUtils.getPerfLogMBeanObjectName;
import static org.tquadrat.foundation.perflog.PerfLogUtils.obtainMBeanServer;
import static org.tquadrat.foundation.perflog.remote.PerfLogRemote.NOTIFICATION_Type;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import java.lang.ref.Cleaner;
import java.lang.ref.Cleaner.Cleanable;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apiguardian.api.API;
import org.tquadrat.foundation.annotation.ClassVersion;
import org.tquadrat.foundation.exception.ImpossibleExceptionError;
import org.tquadrat.foundation.exception.UnexpectedExceptionError;
import org.tquadrat.foundation.perflog.PerfLogMBean;
import org.tquadrat.foundation.perflog.internal.PerfLogMBeanImpl;

/**
 *  <p>{@summary The abstract base class for a client for the Foundation
 *  Performance Logging and Monitoring.} Basically, this is a recipient for the
 *  {@link Notification}
 *  messages that are sent each time a
 *  {@linkplain org.tquadrat.foundation.perflog.PerformanceSection performance
 *  section} was left.</p>
 *
 *  @extauthor Thomas Thrien - thomas.thrien@tquadrat.org
 *  @version $Id: PerfLogClientBase.java 1258 2026-06-04 18:33:06Z tquadrat $
 *  @since 0.25.0
 *
 *  @UMLGraph.link
 */
@SuppressWarnings( "AbstractClassWithoutAbstractMethods" )
@ClassVersion( sourceVersion = "$Id: PerfLogClientBase.java 1258 2026-06-04 18:33:06Z tquadrat $" )
@API( status = MAINTAINED, since = "0.25.0" )
public abstract class PerfLogClientBase implements AutoCloseable
{
        /*---------------*\
    ====** Inner Classes **====================================================
        \*---------------*/
    /**
     *  <p>{@summary The janitor that takes care of the housekeeping for an
     *  instance of
     *  {@link PerfLogClientBase}
     *  in case that was not properly closed.}</p>
     *
     *  @extauthor Thomas Thrien - thomas.thrien@tquadrat.org
     *  @version $Id: PerfLogClientBase.java 1258 2026-06-04 18:33:06Z tquadrat $
     *  @since 0.25.0
     *
     *  @param  mbean   The MBean the listener is connected with.
     *  @param  listener    The notification listener that needs to be removed.
     *
     *  @UMLGraph.link
     */
    @SuppressWarnings( "NewClassNamingConvention" )
    @ClassVersion( sourceVersion = "$Id: PerfLogClientBase.java 1258 2026-06-04 18:33:06Z tquadrat $" )
    @API( status = INTERNAL, since = "0.25.0" )
    private record Janitor( NotificationEmitter mbean, PerfLogNotificationListener listener ) implements Runnable
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
            try
            {
                mbean.removeNotificationListener( listener, listener, null );
            }
            catch( final ListenerNotFoundException _ )
            {
                /*
                 * Deliberately ignored!
                 * If the listener cannot be found in the MBean any more, it
                 * must have been removed already. As this is what we want to
                 * achieve with this method, we can ignore the exception here.
                 */
            }
        }   //  run()
    }
    //  record Janitor

    /**
     * <p>{@summary The implementation of
     * {@link NotificationListener NotificationListener}
     * that receives the
     * {@linkplain Notification notifications}
     * from the
     * {@link PerfLogMBean MBean}.}</p>
     *
     * @param messageQueue  The reference to the destination for the messages.
     *
     * @version $Id: PerfLogClientBase.java 1258 2026-06-04 18:33:06Z tquadrat $
     * @extauthor Thomas Thrien - thomas.thrien@tquadrat.org
     * @UMLGraph.link
     * @since 0.25.0
     */
    private record PerfLogNotificationListener( Queue<String> messageQueue ) implements NotificationFilter, NotificationListener
    {
            /*--------------*\
        ====** Constructors **=================================================
            \*--------------*/
        /**
         *  Creates a new instance of {@code PerfLogNotificationListener}.
         *
         *  @param  messageQueue    The reference to the destination for the
         *     messages.
         */
        public PerfLogNotificationListener
        {
            requireNonNullArgument( messageQueue, "messageQueue" );
        }   //  PerfLogNotificationListener()

            /*---------*\
        ====** Methods **======================================================
            \*---------*/
        /**
         * {@inheritDoc}
         */
        @Override
        public final void handleNotification( final Notification notification, final Object handback )
        {
            messageQueue.offer( notification.getMessage() );
        }   //  handleNotification()

        /**
         * {@inheritDoc}
         */
        @Override
        public final boolean isNotificationEnabled( final Notification notification )
        {
            final var retValue = nonNull( notification )
                && notification.getType().equals( NOTIFICATION_Type );

            //---* Done *------------------------------------------------------
            return retValue;
        }   //  isNotificationEnabled()
    }
    //  record PerfLogNotificationListener

        /*------------*\
    ====** Attributes **=======================================================
        \*------------*/
    /**
     *  The
     *  {@link Cleanable}
     *  for this instance.
     */
    private Cleanable m_Cleanable;

    /**
     *  The caretaker for this instance.
     */
    private Janitor m_Janitor;

    /**
     *  The messages.
     */
    private final BlockingQueue<String> m_Messages = new LinkedBlockingQueue<>();

        /*------------------------*\
    ====** Static Initialisations **===========================================
        \*------------------------*/
    /**
     *  The cleaner that is used to finalise instances of
     *  {@code PerfLogClientBase}.
     */
    private static final Cleaner m_Cleaner = Cleaner.create();

        /*--------------*\
    ====** Constructors **=====================================================
        \*--------------*/
    /**
     *  Creates a new instance of {@code PerfLogClientBase}.
     */
    protected PerfLogClientBase()
    {
        // Just exists.
    }   //  PerfLogClientBase()

        /*---------*\
    ====** Methods **==========================================================
        \*---------*/
    /**
     *  {@inheritDoc}
     */
    @Override
    public final void close()
    {
        if( nonNull( m_Cleanable ) )
        {
            m_Cleanable.clean();
            m_Cleanable = null;
            m_Janitor = null;
        }
    }   //  close()

    /**
     *  <p>{@summary Establishes the connection to the MBean and starts the
     *  listening.}</p>
     *
     *  @param  forceFlag   {@true} if the MBean should be registered in
     *      case it is not yet registered, {@false} otherwise.
     *  @throws IllegalStateException   The connection is already established.
     *  @throws InstanceNotFoundException   {@code forceFlag} is {@false},
     *      the
     *      {@link PerfLogMBean}
     *      is not registered, and the notification listener cannot connect to
     *      it.
     */
    public final void connect( final boolean forceFlag ) throws IllegalStateException, InstanceNotFoundException
    {
        if( nonNull( m_Cleanable ) ) throw new IllegalStateException( "Connection already established" );

        final var mbeanServer = obtainMBeanServer();
        connect( mbeanServer, forceFlag );
    }   //  connect()

    /**
     *  <p>{@summary Establishes the connection to the MBean and starts the
     *  listening.}</p>
     *
     *  @param  mbeanServer The MBean server that holds the MBean.
     *  @param  forceFlag   {@true} if the MBean should be registered in
     *      case it is not yet registered, {@false} otherwise.
     *  @throws IllegalStateException   The connection is already established.
     *  @throws InstanceNotFoundException   {@code forceFlag} is {@false},
     *      the
     *      {@link PerfLogMBean}
     *      is not registered, and the notification listener cannot connect to
     *      it.
     */
    public final void connect( final MBeanServer mbeanServer, final boolean forceFlag ) throws IllegalStateException, InstanceNotFoundException
    {
        if( nonNull( m_Cleanable ) ) throw new IllegalStateException( "Connection already established" );

        final var objectName = getPerfLogMBeanObjectName();
        final var mbean = connectToMBean( requireNonNullArgument( mbeanServer, "mbeanServer" ), objectName, forceFlag );
        final var notificationListener = new PerfLogNotificationListener( m_Messages );
        m_Janitor = new Janitor( mbean, notificationListener );
        m_Cleanable = m_Cleaner.register( this, m_Janitor );
        mbean.addNotificationListener( notificationListener, notificationListener, null );
    }   //  connect()

    /**
     *  <p>{@summary Establishes the connection with the
     *  {@link PerfLogMBean }
     *  on the given
     *  {@linkplain MBeanServer MBean server}
     *  and returns a proxy for it.}</p>
     *  <p>If the MBean is not registered an exception is thrown.</p>
     *
     *  @param  mbeanServer The MBean server that is used.
     *  @param  objectName  The name for the MBean.
     *  @param  forceFlag   {@true} if the MBean should be registered in
     *      case it is not yet registered, {@false} otherwise.
     *      {@link PerfLogMBean}.
     *  @return The MBean that we connected to.
     *  @throws InstanceNotFoundException   {@code forceFlag} is {@false}
     *      and the MBean is not registered in the given MBean server.
     */
    private static final PerfLogMBean connectToMBean( final MBeanServer mbeanServer, final ObjectName objectName, final boolean forceFlag ) throws InstanceNotFoundException
    {
        final PerfLogMBean retValue;
        if( !requireNonNullArgument( mbeanServer, "mbeanServer" ).isRegistered( requireNonNullArgument( objectName, "objectName" ) ) )
        {
            if( !forceFlag ) throw new InstanceNotFoundException( "No MBean registered for ObjectName '%s'".formatted( objectName.toString() ) );
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
     *  Returns a reference to the internal message queue.
     *
     *  @return The message queue.
     */
    @SuppressWarnings( "AssignmentOrReturnOfFieldWithMutableType" )
    protected final BlockingQueue<String> getQueue() { return m_Messages; }
}
//  class PerfLogClientBase

/*
 *  End of File
 */