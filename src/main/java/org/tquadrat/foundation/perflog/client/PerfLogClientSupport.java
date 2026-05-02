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

import static org.apiguardian.api.API.Status.STABLE;

import javax.management.Notification;
import java.util.Optional;

import org.apiguardian.api.API;
import org.tquadrat.foundation.annotation.ClassVersion;

/**
 *  <p>{@summary An implementation of
 *  {@link PerfLogClientBase}.}</p>
 *  <p>This class provides a simple API for a client for the Foundation
 *  Performance Logging and Monitoring. Basically, this is a recipient for the
 *  {@link Notification}
 *  messages that are sent each time a
 *  {@linkplain org.tquadrat.foundation.perflog.PerformanceSection performance
 *  section} was left.</p>
 *
 *  @extauthor Thomas Thrien - thomas.thrien@tquadrat.org
 *  @version $Id: PerfLogClientSupport.java 1207 2026-04-24 21:50:11Z tquadrat $
 *  @since 0.25.0
 *
 *  @UMLGraph.link
 */
@ClassVersion( sourceVersion = "$Id: PerfLogClientSupport.java 1207 2026-04-24 21:50:11Z tquadrat $" )
@API( status = STABLE, since = "0.25.0" )
public final class PerfLogClientSupport extends PerfLogClientBase
{
        /*--------------*\
    ====** Constructors **=====================================================
        \*--------------*/
    /**
     *  Creates a new instance of {@code PerfLogClientSupport}.
     */
    public PerfLogClientSupport()
    {
        super();
    }   //  PerfLogClientSupport()

        /*---------*\
    ====** Methods **==========================================================
        \*---------*/
    /**
     *  Waits for an incoming message.
     *
     *  @return The message.
     *  @throws InterruptedException    An interrupt occurred while waiting.
     */
    public final String awaitMessage() throws InterruptedException
    {
        final var retValue = getQueue().take();

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  awaitMessage()

    /**
     *  <p>{@summary Pulls an incoming message.}</p>
     *  <p>Returns
     *  {@linkplain Optional#empty() emtpy}
     *  if there is currently no message.</p>
     *
     *  @return An instance of
     *      {@link Optional}
     *      that holds the message.
     */
    public final Optional<String> retrieveMessage()
    {
        final var retValue = Optional.ofNullable( getQueue().poll() );

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  retrieveMessage()
}
//  class PerfLogClientSupport

/*
 *  End of File
 */