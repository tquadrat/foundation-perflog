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

import static java.util.Arrays.asList;
import static org.apiguardian.api.API.Status.STABLE;
import static org.tquadrat.foundation.lang.Objects.requireNonNullArgument;
import static org.tquadrat.foundation.perflog.PerfLogUtils.createPerformanceSectionName;

import java.util.Optional;

import org.apiguardian.api.API;
import org.tquadrat.foundation.annotation.ClassVersion;
import org.tquadrat.foundation.perflog.internal.PerfLogManagerImpl;

/**
 *  <p>{@summary This interface describes the Performance Logging and
 *  Monitoring Manager that is the frontend to the
 *  {@linkplain PerfLogMBean Performance Logging and Monitoring MBean}
 *  that does the work behind the scenes.}</p>
 *  <p>Instances of {@code PerfLogManager} are thread-safe, so they can be
 *  created as {@code static}:</p>
 *  {@include ${javadoc}/sample1a.txt:SOURCE}
 *  <p>or as attributes as well.</p>
 *  <p>Because {@code PerfLogManager} implements
 *  {@link AutoCloseable}
 *  it can be used with {@code try-with-resources}, too:</p>
 *  {@include ${javadoc}/sample1c.txt:SOURCE}
 *  <p>If an instance is abandoned, it will perform an automatic
 *  clean-up.</p>
 *
 *  @extauthor Thomas Thrien - thomas.thrien@tquadrat.org
 *  @version $Id: PerfLogManager.java 1246 2026-05-16 14:07:00Z tquadrat $
 *  @since 0.25.0
 *
 *  @UMLGraph.link
 */
@ClassVersion( sourceVersion = "$Id: PerfLogManager.java 1246 2026-05-16 14:07:00Z tquadrat $" )
@API( status = STABLE, since = "0.25.0" )
public sealed interface PerfLogManager extends AutoCloseable
    permits PerfLogManagerImpl
{
        /*---------*\
    ====** Methods **==========================================================
        \*---------*/
    /**
     *  {@inheritDoc}
     *  <p>After {@code close()} is called, calls to other methods of this
     *  instance may result in an
     *  {@link IllegalStateException}
     *  to be thrown.</p>
     */
    @Override
    public void close();

    /**
     *  <p>{@summary Creates a performance tracker for the
     *  {@link PerformanceSection}
     *  with the given name.}</p>
     *  <p>The return value will be empty if the performance section is
     *  currently ignored.</p>
     *  <p>If there is currently no performance section defined with the given
     *  name, one will be created with default values.</p>
     *
     *  @param  name    The name of the performance section.
     *  @return An instance of
     *      {@link Optional}
     *      that holds the new tracker.
     *  @throws IllegalStateException
     *      {@link #close()}
     *      was already called on this instance.
     */
    public default Optional<PerformanceTracker> createPerformanceTracker( final String name ) throws IllegalStateException
    {
        final var retValue = createPerformanceTracker( createPerformanceSectionName( name ) );

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  createPerformanceTracker()

    /**
     *  <p>{@summary Creates a performance tracker for the
     *  {@link PerformanceSection}
     *  with the given name.}</p>
     *  <p>The return value will be empty if the performance section is
     *  currently ignored.</p>
     *  <p>If there is currently no performance section defined with the given
     *  name, one will be created with default values.</p>
     *
     *  @param  name    The name of the performance section.
     *  @return An instance of
     *      {@link Optional}
     *      that holds the new tracker.
     *  @throws IllegalStateException
     *      {@link #close()}
     *      was already called on this instance.
     */
    public Optional<PerformanceTracker> createPerformanceTracker( final PerformanceSectionName name ) throws IllegalStateException;

    /**
     *  <p>{@summary Returns the performance section specified by the given
     *  name.}</p>
     *  <p>Delegates to
     *  {@link PerfLogMBean#getPerformanceSection(PerformanceSectionName)}</p>
     *
     *  @param  name    The name of the performance section.
     *  @return An instance of
     *      {@link Optional}
     *      that holds the retrieved performance section.
     *  @throws IllegalStateException
     *      {@link #close()}
     *      was already called on this instance.
     */
    public Optional<PerformanceSection> getPerformanceSection( final PerformanceSectionName name ) throws IllegalStateException;

    /**
     *  <p>{@summary Loads the
     *  {@linkplain PerformanceSection Performance Section definitions}.}</p>
     *  <p>Because these definitions are stored globally in the
     *  {@linkplain PerfLogMBean Performance Logging and Monitorin MBean},
     *  they need to be loaded only once. But overwriting them does not harm,
     *  other than performance.</p>
     *  <p>Each instance of {@code PerfLogManager} shares the same instances
     *  of
     *  {@link PerformanceSection}.</p>
     *
     *  @param  definitions The definitions for the performance sections.
     *  @throws IllegalStateException
     *      {@link #close()}
     *      was already called on this instance.
     */
    @SuppressWarnings( "NewMethodNamingConvention" )
    public default void loadPerformanceSectionDefinitions( final PerformanceSection... definitions ) throws IllegalStateException
    {
        loadPerformanceSectionDefinitions( asList( requireNonNullArgument( definitions, "definitions" ) ) );
    }   //  loadPerformanceSectionDefinitions()

    /**
     *  <p>{@summary Loads the
     *  {@linkplain PerformanceSection Performance Section definitions}.}</p>
     *  <p>Because these definitions are stored globally in the
     *  {@linkplain PerfLogMBean Performance Logging and Monitorin MBean},
     *  they need to be loaded only once. But overwriting them does not harm,
     *  other than performance.</p>
     *  <p>Each instance of {@code PerfLogManager} shares the same instances
     *  of
     *  {@link PerformanceSection}.</p>
     *
     *  @param  definitions The definitions for the performance sections.
     *  @throws IllegalStateException
     *      {@link #close()}
     *      was already called on this instance.
     */
    @SuppressWarnings( "NewMethodNamingConvention" )
    public void loadPerformanceSectionDefinitions( final Iterable<PerformanceSection> definitions ) throws IllegalStateException;
}
//  interface PerfLogManager

/*
 *  End of File
 */