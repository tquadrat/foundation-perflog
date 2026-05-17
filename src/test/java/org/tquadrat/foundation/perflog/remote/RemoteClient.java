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

package org.tquadrat.foundation.perflog.remote;

import static java.lang.System.err;
import static java.lang.System.out;
import static java.lang.Thread.currentThread;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanException;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ReflectionException;
import java.io.IOException;

import org.tquadrat.foundation.annotation.ClassVersion;
import org.tquadrat.foundation.annotation.PlaygroundClass;

/**
 *  <p>{@summary A test bed for the Foundation Performance Logging and
 *  Monitoring.}</p>
 *  <p>This implements a client that receives the performance messages.</p>
 *
 *  @extauthor Thomas Thrien - thomas.thrien@tquadrat.org
 *  @version $Id: RemoteClient.java 1248 2026-05-17 11:08:34Z tquadrat $
 */
@ClassVersion( sourceVersion = "$Id: RemoteClient.java 1248 2026-05-17 11:08:34Z tquadrat $" )
@PlaygroundClass
/*SNIP_START*/
public final class RemoteClient
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
        currentThread().setUncaughtExceptionHandler( RemoteClient::uncaughtExceptionHandler );

        while( true )
        {
            try( final var remote = PerfLogRemote.connect( 9999, RemoteClient::listener ) )
            {
                out.println( remote.getMBeanInfo() );
                for( final var name : remote.getPerformanceSections() )
                {
                    out.println( remote.getPerformanceSection( name ) );
                }
                out.println( "=".repeat( 67 ) );
                while( true );
            }
            catch(  final IOException | InstanceNotFoundException | ReflectionException | IntrospectionException | AttributeNotFoundException | MBeanException e )
            {
                e.printStackTrace( err );
            }
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
/*SNIP_END*/
/*
 *  End of File
 */