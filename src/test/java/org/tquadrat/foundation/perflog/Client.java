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

import static java.lang.System.err;
import static java.lang.System.out;
import static java.lang.Thread.currentThread;
import static org.tquadrat.foundation.perflog.PerfLogUtils.getPerfLogMBeanObjectName;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServerConnection;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;

import org.tquadrat.foundation.annotation.ClassVersion;
import org.tquadrat.foundation.annotation.PlaygroundClass;

/**
 *  <p>{@summary A test bed for the Foundation Performance Logging and
 *  Monitoring.}</p>
 *  <p>This implements a client that receives the performance messages.</p>
 *
 *  @extauthor Thomas Thrien - thomas.thrien@tquadrat.org
 *  @version $Id: Client.java 1213 2026-05-02 07:00:29Z tquadrat $
 */
@SuppressWarnings( "NewClassNamingConvention" )
@ClassVersion( sourceVersion = "$Id: Client.java 1213 2026-05-02 07:00:29Z tquadrat $" )
@PlaygroundClass
public final class Client
{
        /*---------*\
    ====** Methods **==========================================================
        \*---------*/
    /**
     *  The implementation for
     *  {@link NotificationListener}.
     *
     *  @param  notification    The notification.
     *  @param  handback    The handback.
     */
    private static final void listener( final Notification notification, final Object handback )
    {
        out.printf( """
            Type:    %1$s
            Message: %3$s
            SeqNo:   %2$d
            -------------------------------------------------------------------
            """, notification.getType(), notification.getSequenceNumber(), notification.getMessage() );
    }   //  listener()

    /**
     *  The program entry point.
     *
     *  @param  args    The command line arguments.
     */
    public static final void main( final String... args )
    {
        //---* Initialise the error handling *---------------------------------
        currentThread().setUncaughtExceptionHandler( Client::uncaughtExceptionHandler );

        try
        {
            final var url = new JMXServiceURL( "service:jmx:rmi:///jndi/rmi://localhost:9999/jmxrmi" );
            final var connector = JMXConnectorFactory.connect( url );
            MBeanServerConnection mbsc = connector.getMBeanServerConnection();

            final var objectName = getPerfLogMBeanObjectName();
            mbsc.addNotificationListener( objectName, Client::listener, null, null );

            while( true );
        }
        catch(  final IOException | InstanceNotFoundException e )
        {
            e.printStackTrace( err );
        }
    }   //  main()

    /**
     *  An implementation of
     *  {@link Thread.UncaughtExceptionHandler}.
     *
     *  @param  t   The aborted thread.
     *  @param  e   The
     *      {@link Throwable}
     *      that caused the abort.
     */
    private static final void uncaughtExceptionHandler( final Thread t, final Throwable e )
    {
        err.printf( "Thread %s aborted due to an exception%n", t.getName() );
        e.printStackTrace( err );
    }   //  uncaughtExceptionHandler()
}
//  class Client

/*
 *  End of File
 */