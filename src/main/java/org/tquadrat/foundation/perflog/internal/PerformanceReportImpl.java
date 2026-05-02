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

package org.tquadrat.foundation.perflog.internal;

import static java.time.ZoneOffset.UTC;
import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static org.apiguardian.api.API.Status.INTERNAL;
import static org.tquadrat.foundation.lang.Objects.hash;
import static org.tquadrat.foundation.lang.Objects.requireNonNullArgument;
import static org.tquadrat.foundation.perflog.PerformanceTracker.TrackerStatus.STATUS_ABORTED;
import static org.tquadrat.foundation.perflog.remote.PerfLogRemote.JSONField_Aborted;
import static org.tquadrat.foundation.perflog.remote.PerfLogRemote.JSONField_Context;
import static org.tquadrat.foundation.perflog.remote.PerfLogRemote.JSONField_ElapsedTime;
import static org.tquadrat.foundation.perflog.remote.PerfLogRemote.JSONField_ExceededThreshold;
import static org.tquadrat.foundation.perflog.remote.PerfLogRemote.JSONField_Section;
import static org.tquadrat.foundation.perflog.remote.PerfLogRemote.JSONField_SectionDescription;
import static org.tquadrat.foundation.perflog.remote.PerfLogRemote.JSONField_SectionName;
import static org.tquadrat.foundation.perflog.remote.PerfLogRemote.JSONField_SectionThreshold;
import static org.tquadrat.foundation.perflog.remote.PerfLogRemote.JSONField_SectionThresholdOnlyReport;
import static org.tquadrat.foundation.perflog.remote.PerfLogRemote.JSONField_SectionTimeout;
import static org.tquadrat.foundation.perflog.remote.PerfLogRemote.JSONField_StartTime;
import static org.tquadrat.foundation.perflog.remote.PerfLogRemote.JSONField_TimedOut;
import static org.tquadrat.foundation.util.StringUtils.isNotEmptyOrBlank;
import static org.tquadrat.foundation.value.Time.MILLISECOND;

import java.util.Optional;

import org.apiguardian.api.API;
import org.tquadrat.foundation.annotation.ClassVersion;
import org.tquadrat.foundation.jsonbuilder.JSONBuilder;
import org.tquadrat.foundation.lang.Objects;
import org.tquadrat.foundation.perflog.PerformanceReport;
import org.tquadrat.foundation.perflog.PerformanceSection;
import org.tquadrat.foundation.perflog.PerformanceTracker;

/**
 *  <p>{@summary The container for report data going to the
 *  {@link org.tquadrat.foundation.perflog.PerfLogMBean}.}</p>
 *  <p>The method
 *  {@link #toJSON()}
 *  will generate the report message that is distributed through the
 *  {@link org.tquadrat.foundation.perflog.PerfLogMBean PerfLogMBean}.</p>
 *
 *  @extauthor Thomas Thrien - thomas.thrien@tquadrat.org
 *  @version $Id: PerformanceReportImpl.java 1216 2026-05-02 11:16:24Z tquadrat $
 *  @since 0.25.0
 *
 *  @UMLGraph.link
 */
@ClassVersion( sourceVersion = "$Id: PerformanceReportImpl.java 1216 2026-05-02 11:16:24Z tquadrat $" )
@API( status = INTERNAL, since = "0.25.0" )
public final class PerformanceReportImpl implements PerformanceReport
{
        /*------------*\
    ====** Attributes **=======================================================
        \*------------*/
    /**
     *  The optional cause that was added to the report in case the performance
     *  section was aborted.
     */
    private final Throwable m_Cause;

    /**
     *  <p>{@summary The JSON builder that is used to generated the
     *  notification messages.}</p>
     */
    private final JSONBuilder m_JSONBuilder;

    /**
     *  An optional message that was issued with the report.
     */
    private final String m_Message;

    /**
     *  The performance section.
     */
    private final PerformanceSection m_Section;

    /**
     *  The performance tracker.
     */
    private final PerformanceTrackerImpl m_Tracker;

        /*--------------*\
    ====** Constructors **=====================================================
        \*--------------*/
    /**
     *  Creates a new instance of {@code PerformanceReportImpl}.
     *
     *  @param  tracker The performance tracker with the data to report.
     *  @param  message An optional message that was issued with this report.
     *  @param  cause   The optional exception that can be issued in case the
     *      performance section was aborted.
     */
    public PerformanceReportImpl( final PerformanceTracker tracker, final String message, final Throwable cause )
    {
        m_Tracker = (PerformanceTrackerImpl) requireNonNullArgument( tracker, "tracker" );
        m_Section = m_Tracker.getPerformanceSection();
        m_Message = message; // Is optional,  can be null!
        m_Cause = cause; // Is optional,  can be null!

        m_JSONBuilder = JSONBuilder.getInstance();
    }   //  PerformanceReportImpl()

        /*---------*\
    ====** Methods **==========================================================
        \*---------*/
    /**
     *  {@inheritDoc}
     */
    @Override
    public final boolean equals( final Object o )
    {
        var retValue = this == o;
        if( !retValue && o instanceof final PerformanceReportImpl other )
        {
            retValue = m_Tracker.equals( other.m_Tracker )
                && Objects.equals( m_Message, other.m_Message )
                && Objects.equals( m_Cause, other.m_Cause );
        }

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  equals()

    /**
     *  {@inheritDoc}
     */
    @Override
    public final Optional<Throwable> getCause() { return Optional.ofNullable( m_Cause ); }

    /**
     *  {@inheritDoc}
     */
    @Override
    public Optional<String> getMessage()
    {
        final Optional<String> retValue = isNotEmptyOrBlank( m_Message ) ? Optional.ofNullable( m_Message ) : Optional.empty();

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  getMessage()

    /**
     *  {@inheritDoc}
     */
    @Override
    public final PerformanceSection getPerformanceSection() { return m_Section; }

    /**
     *  {@inheritDoc}
     */
    @Override
    public final PerformanceTracker getPerformanceTracker() { return m_Tracker; }

    /**
     *  {@inheritDoc}
     */
    @Override
    public final int hashCode() { return hash( m_Tracker, m_Cause, m_Message ); }

    /**
     *  <p>{@summary Returns the contents of this report instance as a JSON
     *  string.}</p>
     *  <p>The JSON has the following structure:</p>
     *  <ul>
     *      <li>{@value org.tquadrat.foundation.perflog.remote.PerfLogRemote#JSONField_Section}
     *      [Object] – The definition of the
     *      {@link org.tquadrat.foundation.perflog.PerformanceSection}.
     *      <ul>
     *          <li>{@value org.tquadrat.foundation.perflog.remote.PerfLogRemote#JSONField_SectionName}
     *          [String] – The name of the performance section.</li>
     *          <li>{@value org.tquadrat.foundation.perflog.remote.PerfLogRemote#JSONField_SectionDescription}
     *          [String] – The description for the performance section.</li>
     *          <li>{@value org.tquadrat.foundation.perflog.remote.PerfLogRemote#JSONField_SectionThresholdOnlyReport}
     *          [Boolean] – Indicates whether a report is sent only when the threshold was exceeded.</li>
     *          <li>{@value org.tquadrat.foundation.perflog.remote.PerfLogRemote#JSONField_SectionThreshold}
     *          [optional Object] – The threshold time from the performance section.
     *          <ul>
     *              <li>{@value JSONBuilder#JSONField_Unit} [String] – The
     *              dimension.</li>
     *              <li>{@value JSONBuilder#JSONField_Value} [Number] – The
     *              value.</li>
     *          </ul>
     *          </li>
     *          <li>{@value org.tquadrat.foundation.perflog.remote.PerfLogRemote#JSONField_SectionTimeout}
     *          [optional Object] – The timeout time from the performance section.
     *          <ul>
     *              <li>{@value JSONBuilder#JSONField_Unit} [String] – The
     *              dimension.</li>
     *              <li>{@value JSONBuilder#JSONField_Value} [Number] – The
     *              value.</li>
     *          </ul>
     *          </li>
     *      </ul>
     *      </li>
     *      <li>{@value org.tquadrat.foundation.perflog.remote.PerfLogRemote#JSONField_StartTime} [String] – The start time; the
     *      time when the performance section was entered.</li>
     *      <li>{@value org.tquadrat.foundation.perflog.remote.PerfLogRemote#JSONField_ElapsedTime} [optional Object] – The time
     *      that was spent in the performance section. Only present when the
     *      operation was not aborted.
     *      <ul>
     *          <li>{@value JSONBuilder#JSONField_Unit} [String] – The
     *          dimension.</li>
     *          <li>{@value JSONBuilder#JSONField_Value} [Number] – The
     *          value.</li>
     *      </ul>
     *      </li>
     *      <li>{@value org.tquadrat.foundation.perflog.remote.PerfLogRemote#JSONField_Context}
     *      [optional Object] – The context for
     *      the current execution of the performance section. The member names
     *      are specific for the respective performance section.</li>
     *      <li>{@value org.tquadrat.foundation.perflog.remote.PerfLogRemote#JSONField_Message}
     *      [optional String] – A message that was issued with this
     *      report.</li>
     *      <li>{@value org.tquadrat.foundation.perflog.remote.PerfLogRemote#JSONField_Cause}
     *      [optional String] – An exception that was issued with this report
     *      as the reason for aborting the performance section.</li>
     *  </ul>
     *
     *  @return A JSON formatted representation of this report instance.
     */
    @SuppressWarnings( "PublicMethodNotExposedInInterface" )
    public final String toJSON()
    {
        final var json = m_JSONBuilder.createObject();
        final var section = json.setObject( JSONField_Section );

        section.set( JSONField_SectionName, m_Section.getName().toString() )
            .set( JSONField_SectionDescription, m_Section.getDescription() )
            .set( JSONField_SectionThresholdOnlyReport, m_Section.isSendingReportOnlyForExceededThreshold() );
        m_Section.getThreshold().ifPresent( v -> section.set( JSONField_SectionThreshold, v, MILLISECOND ) );
        m_Section.getTimeout().ifPresent( v -> section.set( JSONField_SectionTimeout, v, MILLISECOND ) );

        final var startTime = m_Tracker.getTimestamp().atZone( UTC );
        json.set( JSONField_StartTime, startTime.format( ISO_OFFSET_DATE_TIME ) );
        m_Tracker.getElapsedTime().ifPresent( v -> json.set( JSONField_ElapsedTime, v, MILLISECOND) );
        json.set( JSONField_ExceededThreshold, m_Tracker.isThresholdExceeded() )
            .set( JSONField_TimedOut, m_Tracker.isTimedOut() )
            .set( JSONField_Aborted, m_Tracker.getStatus() == STATUS_ABORTED );

        final var context = m_Tracker.getContext();
        if( !context.isEmpty() )
        {
            final var contextObject = json.setObject( JSONField_Context );
            for( final var entry : context.entrySet() )
            {
                contextObject.set( entry.getKey(), entry.getValue() );
            }
        }

        final var retValue = json.toString();

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  toJSON()

    /**
     *  {@inheritDoc}
     */
    @Override
    public final String toString() { return ""; }
}
//  class PerformanceReportImpl

/*
 *  End of File
 */