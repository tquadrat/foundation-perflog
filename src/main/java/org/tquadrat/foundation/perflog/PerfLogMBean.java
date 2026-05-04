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

import static org.apiguardian.api.API.Status.STABLE;

import javax.management.NotificationEmitter;
import java.util.Optional;

import org.apiguardian.api.API;
import org.tquadrat.foundation.annotation.ClassVersion;

/**
 *  <p>{@summary This interface describes the MBean for the Foundation
 *  Performance Logging and Monitoring.}</p>
 *
 *  @extauthor Thomas Thrien - thomas.thrien@tquadrat.org
 *  @version $Id: PerfLogMBean.java 1229 2026-05-04 19:11:41Z tquadrat $
 *  @since 0.25.0
 *
 *  @UMLGraph.link
 */
@ClassVersion( sourceVersion = "$Id: PerfLogMBean.java 1229 2026-05-04 19:11:41Z tquadrat $" )
@API( status = STABLE, since = "0.25.0" )
public interface PerfLogMBean extends NotificationEmitter
{
        /*-----------*\
    ====** Constants **========================================================
        \*-----------*/
    /**
     *  The MBean description: {@value}.
     *
     *  @see javax.management.MBeanInfo#getDescription()
     */
    public static final String DESCRIPTION = "tquadrat Foundation Performance Logging and Monitoring";

    /**
     *  The description for the
     *  {@link javax.management.Notification}
     *  instances emitted by this MBean: {@value}.
     */
    public static final String NOTIFICATION_Description = "The execution Notification for a Performance Section";

        /*---------*\
    ====** Methods **==========================================================
        \*---------*/
    /**
     *  Adds the definition for a performance section.
     *
     *  @param  definition  The definition for the performance section.
     */
    public void addPerformanceSection( final PerformanceSection definition );

    /**
     *  <p>{@summary Disables the performance section.}</p>
     *  <p>This is an operation for the MBean.</p>
     *
     *  @param  name    The name of the performance section.
     *  @return A message indicating success or failure of the operation, as a
     *  JSON String.
     */
    public String disablePerformanceSection( final String name );

    /**
     *  <p>{@summary Enables the performance section.}</p>
     *  <p>This is an operation for the MBean.</p>
     *
     *  @param  name    The name of the performance section.
     *  @return A message indicating success or failure of the operation, as a
     *  JSON String.
     */
    public String enablePerformanceSection( final String name );

    /**
     *  Returns the exceptions that caused a notification thread to abort.
     *
     *  @return A list of exceptions that let a notification thread go down.
     */
    public String[] getNotificationExceptions();

    /**
     *  Returns the notification sequence number.
     *
     *  @return The last used sequence number.
     */
    public long getNotificationSequenceNumber();

    /**
     *  <p>{@summary Returns the performance section specified by the given
     *  name.}</p>
     *
     *  @param  name    The name of the performance section.
     *  @return An instance of
     *      {@link Optional}
     *      that holds the retrieved performance section.
     */
    public Optional<PerformanceSection> getPerformanceSection( final PerformanceSectionName name );

    /**
     *  <p>{@summary Returns a list of the currently defined performance
     *  sections.}</p>
     *  <p>This is an attribute for the MBean.</p>
     *
     *  @return The list of the performance section names.
     */
    public String [] getPerformanceSections();

    /**
     *  <p>{@summary Retrieves the performance section for the given name.}</p>
     *  <p>If there is no performance section for the given name, a new one is
     *  created, with the
     *  {@linkplain PerformanceSection#getThreshold() threshold}
     *  and the
     *  {@linkplain PerformanceSection#getTimeout() timeout}
     *  disabled. Obviously, it should not be ignored.</p>
     *
     *  @param  name    The name of the performance section.
     *  @return The performance section; will never be {@code null}.
     */
    public PerformanceSection retrievePerformanceSection( final PerformanceSectionName name );

    /**
     *  Takes the performance report and transfers it further.
     *
     *  @param  report  The performance report.
     */
    public void receivePerformanceReport( final PerformanceReport report );

    /**
     *  <p>{@summary Shows the performance section status.}</p>
     *  <p>This is an operation for the MBean.</p>
     *
     *  @param  name    The name of the performance section.
     *  @return The status of the performance section, or a message indicating
     *      what failed in case of error, as a JSON String.
     */
    public String showPerformanceSection( final String name );
}
//  interface PerfLogMBean

/*
 *  End of File
 */