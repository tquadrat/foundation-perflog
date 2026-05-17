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

import static java.lang.IO.println;
import static java.lang.System.err;
import static java.lang.System.out;
import static java.lang.Thread.currentThread;
import static org.tquadrat.foundation.mgmt.JMXUtils.enableRemoteAccess;
import static org.tquadrat.foundation.perflog.PerfLogUtils.createPerfLogManager;
import static org.tquadrat.foundation.perflog.PerfLogUtils.createPerformanceSectionName;
import static org.tquadrat.foundation.perflog.PerfLogUtils.hold;
import static org.tquadrat.foundation.perflog.PerfLogUtils.obtainMBeanServer;
import static org.tquadrat.foundation.perflog.PerformanceSection.PerformanceSectionFlags.IGNORED;
import static org.tquadrat.foundation.perflog.PerformanceSection.PerformanceSectionFlags.SEND_REPORT_FOR_ABORT;
import static org.tquadrat.foundation.perflog.PerformanceSection.PerformanceSectionFlags.SEND_REPORT_ONLY_FOR_EXCEEDED_THRESHOLD;
import static org.tquadrat.foundation.util.SystemUtils.getRandom;
import static org.tquadrat.foundation.util.SystemUtils.repose;

import javax.management.InstanceNotFoundException;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.tquadrat.foundation.annotation.ClassVersion;
import org.tquadrat.foundation.annotation.PlaygroundClass;
import org.tquadrat.foundation.exception.UnexpectedExceptionError;
import org.tquadrat.foundation.perflog.client.PerfLogClientSupport;
import org.tquadrat.foundation.util.stringconverter.IntegerStringConverter;
import org.tquadrat.foundation.value.Time;
import org.tquadrat.foundation.value.TimeValue;

/**
 *  <p>{@summary A test bed for the Foundation Performance Logging and
 *  Monitoring.}</p>
 *
 *  @extauthor Thomas Thrien - thomas.thrien@tquadrat.org
 *  @version $Id: Application.java 1248 2026-05-17 11:08:34Z tquadrat $
 */
@ClassVersion( sourceVersion = "$Id: Application.java 1248 2026-05-17 11:08:34Z tquadrat $" )
@PlaygroundClass
public final class Application
{
        /*---------*\
    ====** Methods **==========================================================
        \*---------*/
    /**
     *  The program entry point.
     *
     *  @param  args    The command line arguments.
     */
    public static final void main( final String... args )
    {
        //---* Initialise the error handling *---------------------------------
        currentThread().setUncaughtExceptionHandler( Application::uncaughtExceptionHandler );

        final var mbeanServer = obtainMBeanServer();
        try( final var clientSupport = new PerfLogClientSupport();
            final var perfLogManager = createPerfLogManager( mbeanServer, Application::uncaughtExceptionHandler ) )
        {
            clientSupport.connect(  mbeanServer, true );
            final var url = enableRemoteAccess( mbeanServer, 9999, Map.of() );
            out.printf( "Connection URL: %s%n", url.toString() );
            final var thread = new Thread( () ->
            {
               var proceed = true;
               while( proceed )
               {
                   try
                   {
                       final var message = clientSupport.awaitMessage();
                       println( "Message: %s".formatted( message ) );
                   }
                   catch( final  InterruptedException _ )
                   {
                       proceed = false;
                   }
               }
            });
            thread.start();

            final var random = getRandom();

            final var thresholdSectionName = createPerformanceSectionName( "PS3" );
            final Collection<PerformanceSection> performanceSections = new ArrayList<>();
            performanceSections.add( new PerformanceSection( "PS1", "Test Section 1", null, null, SEND_REPORT_FOR_ABORT ) );
            performanceSections.add( new PerformanceSection( "PS2", "Inactive Section 2", 100L, 1000L, IGNORED, SEND_REPORT_FOR_ABORT ) );
            performanceSections.add( new PerformanceSection( thresholdSectionName, "Threshold only Section 3", new TimeValue( Time.SECOND,30 ), null, SEND_REPORT_ONLY_FOR_EXCEEDED_THRESHOLD ) );
            perfLogManager.loadPerformanceSectionDefinitions( performanceSections );

            for( final var ps : performanceSections )
            {
                perfLogManager.getPerformanceSection( ps.getName() ).ifPresent( out::println );
            }

            final var loopSectionName = createPerformanceSectionName( "PS1" );
            var loopCounter = 0;
            println( "Looping …" );
            //noinspection InfiniteLoopStatement
            while( true )
            {
                try( final var thresholdTracker = hold( perfLogManager.createPerformanceTracker( thresholdSectionName ) ) )
                {
                    thresholdTracker.addContext( "LoopCounter", ++loopCounter, IntegerStringConverter.INSTANCE );
                    thresholdTracker.start();
                    final var loopTracker = hold( perfLogManager.createPerformanceTracker( loopSectionName ) );
                    try( loopTracker )
                    {
                        loopTracker.addContext( "LoopCounter", loopCounter, IntegerStringConverter.INSTANCE );
                        loopTracker.start();
                        repose( Duration.ofSeconds( 29 ) );
                    }
                    catch( final Exception e )
                    {
                        loopTracker.abort( "Exception caught", e );
                    }
                    repose( (long) random.nextInt( 800 ) + 300 );
                }
            }
        }
        catch( final InstanceNotFoundException | IOException e )
        {
            e.printStackTrace( err );
        }
    }   //  main()

    /**
     *  Receive the Performance Logging and Monitoring Notifications and
     *  process them.
     */
    public final void run()
    {
        final var mbeanServer = obtainMBeanServer();
        try( final var clientSupport = new PerfLogClientSupport() )
        {
            clientSupport.connect( mbeanServer, true );
            var proceed = true;
            while( proceed )
            {
                try
                {
                    final var message = clientSupport.awaitMessage();
                    // Process the message!!
                }
                catch( final InterruptedException _ )
                {
                    proceed = false;
                }
            }
        }
        catch( final InstanceNotFoundException e )
        {
            throw new UnexpectedExceptionError( "Should not happen as we set force = 'true' when connecting", e );
        }
    }   //  run()

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
//  class Application

/*
 *  End of File
 */