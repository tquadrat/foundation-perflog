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
import static org.tquadrat.foundation.lang.Objects.requireNonNullArgument;
import static org.tquadrat.foundation.perflog.PerformanceTracker.TrackerStatus.STATUS_STARTED;

import org.apiguardian.api.API;
import org.tquadrat.foundation.annotation.ClassVersion;
import org.tquadrat.foundation.lang.StringConverter;
import org.tquadrat.foundation.perflog.PerfLogUtils.PerformanceTrackerHolder;
import org.tquadrat.foundation.perflog.internal.PerformanceTrackerImpl;

/**
 *  <p>{@summary This interface describes a performance tracker for the
 *  Foundation Performance Logging and Monitoring.}</p>
 *  <p>This is basically collecting the time that is spent within a performance
 *  section.</p>
 *  <p>Obviously, an instance of the implementation of this interface is not
 *  thread-safe, but it can be reused multiple times.</p>
 *
 *  @extauthor Thomas Thrien - thomas.thrien@tquadrat.org
 *  @version $Id: PerformanceTracker.java 1248 2026-05-17 11:08:34Z tquadrat $
 *  @since 0.25.0
 *
 *  @UMLGraph.link
 */
@ClassVersion( sourceVersion = "$Id: PerformanceTracker.java 1248 2026-05-17 11:08:34Z tquadrat $" )
@API( status = STABLE, since = "0.25.0" )
public sealed interface PerformanceTracker
    permits PerformanceTrackerHolder, PerformanceTrackerImpl
{
        /*---------------*\
    ====** Inner Classes **====================================================
        \*---------------*/
    /**
     *  <p>{@summary The status of a
     *  {@link PerformanceTracker}.}</p>
     *
     *  @extauthor Thomas Thrien - thomas.thrien@tquadrat.org
     *  @version $Id: PerformanceTracker.java 1248 2026-05-17 11:08:34Z tquadrat $
     *  @since 0.25.0
     *
     *  @UMLGraph.link
     */
    @ClassVersion( sourceVersion = "$Id: PerformanceTracker.java 1248 2026-05-17 11:08:34Z tquadrat $" )
    @API( status = STABLE, since = "0.25.0" )
    public static enum TrackerStatus
    {
            /*------------------*\
        ====** Enum Declaration **=============================================
            \*------------------*/
        /**
         *  The tracker was not started yet.
         */
        STATUS_READY,

        /**
         *  The tracker was started, but not yet stopped or aborted.
         */
        STATUS_STARTED,

        /**
         *  The tracker was stopped after it was started.
         */
        STATUS_STOPPED,

        /**
         *  The tracker was aborted after it was started.
         */
        STATUS_ABORTED
    }
    //  enum TrackerStatus

        /*---------*\
    ====** Methods **==========================================================
        \*---------*/
    /**
     *  <p>{@summary Stops the performance timer.}</p>
     *  <p>If the tracker has been aborted or stopped already, nothing happens.
     *  Same if it was never started.</p>
     */
    public void abort();

    /**
     *  <p>{@summary Stops the performance timer and takes a message describing
     *  the reason for the abort.}</p>
     *  <p>If the tracker has been aborted or stopped already, nothing happens.
     *  Same if it was never started.</p>
     *
     *  @param  message The message describing the reason for the abort.
     */
    public void abort( final String message );

    /**
     *  <p>{@summary Stops the performance timer and takes a message describing
     *  the reason for the abort, plus the exception that caused it.}</p>
     *  <p>If the tracker has been aborted or stopped already, nothing happens.
     *  Same if it was never started.</p>
     *
     *  @param  message The message describing the reason for the abort.
     *  @param  cause   The exception that caused the abort.
     */
    public void abort( final String message, final Throwable cause );

    /**
     *  <p>{@summary Adds context information to this tracker.} This is also
     *  transferred to the
     *  {@link PerfLogMBean}
     *  when the tracker is
     *  {@linkplain #stop() stopped} or
     *  {@linkplain #abort() aborted}.</p>
     *  <p>The given name must conform a valid JSON name and may not start with
     *  an underscore (&quot;_&quot;/#x005f).</p>
     *  <p>The given name is unique; if a value is added with an already known
     *  name, it will overwrite the existing value.</p>
     *  <p>A call to
     *  {@link #start()}
     *  will not reset the context.</p>
     *  <p>An example on how to use the context may look like this:</p>
     *  {@include ${javadoc}/sample3c.txt:SOURCE}
     *
     *  @param  name    The name of the context value.
     *  @param  value   The context value; {@code null} removes the entry with
     *      the given name.
     *  @return This instance.
     */
    public PerformanceTracker addContext( final String name, final String value );

    /**
     *  <p>{@summary Adds context information to this tracker.} This is also
     *  transferred to the
     *  {@link PerfLogMBean}
     *  when the tracker is
     *  {@linkplain #stop() stopped}.</p>
     *  <p>The given name is unique; if a value is added with an already known
     *  name, it will overwrite the existing value.</p>
     *  <p>A call to
     *  {@link #start()}
     *  will not reset the context.</p>
     *
     *  @param  <T> The type of the context value.
     *  @param  name    The name of the context value.
     *  @param  value   The context value; {@code null} removes the entry with
     *      the given name.
     *  @param  stringConverter The instance of
     *      {@link StringConverter}
     *      that is used to translate the value into a String.
     *  @return This instance.
     *
     *  @see #addContext(String,String)
     */
    public default <T> PerformanceTracker addContext( final String name, final T value, final StringConverter<T> stringConverter )
    {
        final var retValue = addContext( requireNonNullArgument( name, "name" ), requireNonNullArgument( stringConverter, "stringConverter" ).toString( value ) );

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  addContext

    /**
     *  Returns the status of this tracker instance.
     *
     *  @return The tracker status.
     */
    public TrackerStatus getStatus();

    /**
     *  Checks whether the tracker is active.
     *
     *  @return {@code true} if the tracker was started, but not yet aborted or
     *      stopped, {@code false} otherwise.
     */
    public default boolean isActive() { return getStatus() == STATUS_STARTED; }

    /**
     *  <p>{@summary Resets the instance.}</p>
     *  <p>If the tracker was already started, but never stopped or aborted, an
     *  {@link IllegalStateException}
     *  is thrown.</p>
     *
     *  @param  resetContext    {@code true} if also the context should be
     *      reset, {@code false} to keep the current context.
     *  @throws IllegalStateException   The tracker was already started but not
     *      stopped or aborted.
     *  @return This instance.
     */
    public PerformanceTracker reset( final boolean resetContext ) throws IllegalStateException;

    /**
     *  <p>{@summary Resets the instance and starts the performance timer.}</p>
     *  <p>If the tracker was already started, but never stopped or aborted, an
     *  {@link IllegalStateException}
     *  is thrown.</p>
     *
     *  @throws IllegalStateException   The tracker was already started but not
     *      stopped or aborted.
     */
    public void start() throws IllegalStateException;

    /**
     *  <p>{@summary Stops the performance timer and transfers the elapsed time
     *  to the
     *  {@link PerfLogMBean}
     *  for further processing.}</p>
     *  <p>If the tracker has been aborted already, nothing happens. Same if it
     *  was never started.</p>
     *
     *  @throws IllegalStateException   The tracker was already stopped
     *      previously.
     */
    public void stop() throws IllegalStateException;
}
//  interface PerformanceTracker

/*
 *  End of File
 */