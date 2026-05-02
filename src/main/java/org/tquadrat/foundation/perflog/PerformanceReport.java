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

import java.util.Optional;

import org.apiguardian.api.API;
import org.tquadrat.foundation.annotation.ClassVersion;
import org.tquadrat.foundation.perflog.internal.PerformanceReportImpl;

/**
 *  <p>{@summary The container for report data going to the
 *  {@link org.tquadrat.foundation.perflog.PerfLogMBean}.}</p>
 *
 *  @extauthor Thomas Thrien - thomas.thrien@tquadrat.org
 *  @version $Id: PerformanceReport.java 1216 2026-05-02 11:16:24Z tquadrat $
 *  @since 0.25.0
 *
 *  @UMLGraph.link
 */
@ClassVersion( sourceVersion = "$Id: PerformanceReport.java 1216 2026-05-02 11:16:24Z tquadrat $" )
@API( status = STABLE, since = "0.25.0" )
public sealed interface PerformanceReport
    permits PerformanceReportImpl
{
        /*-----------*\
    ====** Constants **========================================================
        \*-----------*/

        /*---------*\
    ====** Methods **==========================================================
        \*---------*/
    /**
     *  Returns the optional cause that was issued with this report.
     *
     *  @return An instance of
     *      {@link Optional}
     *      that holds the cause.
     */
    public Optional<Throwable> getCause();

    /**
     *  Returns the optional message that was issued with this report.
     *
     *  @return An instance of
     *      {@link Optional}
     *      that holds the message.
     */
    public Optional<String> getMessage();

    /**
     *  Returns the performance section that is referred by this report.
     *
     *  @return The performance section.
     */
    public PerformanceSection getPerformanceSection();

    /**
     *  Returns the performance tracker that is issued by this report.
     *
     *  @return The performance tracker.
     */
    public PerformanceTracker getPerformanceTracker();
}
//  class PerformanceReport

/*
 *  End of File
 */