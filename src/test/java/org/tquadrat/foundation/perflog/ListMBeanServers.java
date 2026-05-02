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
import static java.lang.management.ManagementFactory.getPlatformMBeanServer;
import static org.tquadrat.foundation.perflog.PerfLogUtils.DOMAIN_NAME;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import java.util.List;

import org.tquadrat.foundation.annotation.ClassVersion;
import org.tquadrat.foundation.annotation.PlaygroundClass;

/**
 *  <p>{@summary A tester that lists all currently available MBeanServers.}</p>
 *
 *  @extauthor Thomas Thrien - thomas.thrien@tquadrat.org
 *  @version $Id: ListMBeanServers.java 1211 2026-05-01 15:24:10Z tquadrat $
 */
@ClassVersion( sourceVersion = "$Id: ListMBeanServers.java 1211 2026-05-01 15:24:10Z tquadrat $" )
@PlaygroundClass
public final class ListMBeanServers
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
        try
        {
            final List<MBeanServer> servers = MBeanServerFactory.findMBeanServer( null );
            for( final var server : servers )
            {
                out.println( server.getDefaultDomain() );
            }
            servers.clear();
            out.println( "-".repeat( 40 ) );

            getPlatformMBeanServer();
            servers.addAll( MBeanServerFactory.findMBeanServer( null ) );
            for( final var server : servers )
            {
                out.println( server.getDefaultDomain() );
            }
            servers.clear();
            out.println( "-".repeat( 40 ) );

            MBeanServerFactory.createMBeanServer( DOMAIN_NAME );
            servers.addAll( MBeanServerFactory.findMBeanServer( null ) );
            for( final var server : servers )
            {
                out.println( server.getDefaultDomain() );
            }
            out.println( "Done!" );
        }
        catch( final Throwable t )
        {
            t.printStackTrace( err );
        }
    }   //  main()
}
//  class ListMBeanServers

/*
 *  End of File
 */