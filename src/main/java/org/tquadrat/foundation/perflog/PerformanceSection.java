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
import static org.tquadrat.foundation.lang.Objects.isNull;
import static org.tquadrat.foundation.lang.Objects.nonNull;
import static org.tquadrat.foundation.lang.Objects.requireNonNullArgument;
import static org.tquadrat.foundation.lang.Objects.requireValidArgument;
import static org.tquadrat.foundation.perflog.PerfLogUtils.createPerformanceSectionName;
import static org.tquadrat.foundation.perflog.PerformanceSection.PerformanceSectionFlags.IGNORED;
import static org.tquadrat.foundation.perflog.PerformanceSection.PerformanceSectionFlags.SEND_REPORT_FOR_ABORT;
import static org.tquadrat.foundation.perflog.PerformanceSection.PerformanceSectionFlags.SEND_REPORT_ONLY_FOR_EXCEEDED_THRESHOLD;
import static org.tquadrat.foundation.util.StringUtils.mapFromEmpty;
import static org.tquadrat.foundation.value.Time.MILLISECOND;

import java.util.EnumSet;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apiguardian.api.API;
import org.tquadrat.foundation.annotation.ClassVersion;
import org.tquadrat.foundation.exception.ValidationException;
import org.tquadrat.foundation.lang.AutoLock;
import org.tquadrat.foundation.lang.AutoLock.ExecutionFailedException;
import org.tquadrat.foundation.value.TimeValue;

/**
 *  <p>{@summary This class describes a &quot;Performance Section&quot;.}</p>
 *
 *  @extauthor Thomas Thrien - thomas.thrien@tquadrat.org
 *  @version $Id: PerformanceSection.java 1218 2026-05-02 15:17:24Z tquadrat $
 *  @since 0.25.0
 *
 *  @UMLGraph.link
 */
@ClassVersion( sourceVersion = "$Id: PerformanceSection.java 1218 2026-05-02 15:17:24Z tquadrat $" )
@API( status = STABLE, since = "0.25.0" )
public final class PerformanceSection
{
        /*---------------*\
    ====** Inner Classes **====================================================
        \*---------------*/
    /**
     *  <p>{@summary The ignore status for a performance section.}</p>
     *
     *  @extauthor Thomas Thrien - thomas.thrien@tquadrat.org
     *  @version $Id: PerformanceSection.java 1218 2026-05-02 15:17:24Z tquadrat $
     *  @since 0.25.0
     *
     *  @UMLGraph.link
     */
    @ClassVersion( sourceVersion = "$Id: PerformanceSection.java 1218 2026-05-02 15:17:24Z tquadrat $" )
    @API( status = STABLE, since = "0.25.0" )
    public static enum PerformanceSectionFlags
    {
        /**
         *  The performance section will be ignored.
         */
        IGNORED,

        /**
         *  Indicates that a report should be sent also for aborted performance
         *  trackers.
         */
        SEND_REPORT_FOR_ABORT,

        /**
         *  Indicates that reports should be sent only when the threshold was
         *  exceeded.
         */
        @SuppressWarnings( "FieldNamingConvention" ) SEND_REPORT_ONLY_FOR_EXCEEDED_THRESHOLD
    }
    //  enum PerformanceSectionFlags

        /*------------*\
    ====** Attributes **=======================================================
        \*------------*/
    /**
     *  The description for the performance section.
     */
    private final String m_Description;

    /**
     *  The flog that controls whether this performance section is ignored.
     */
    private boolean m_Ignore;

    /**
     *  The flag that controls whether reports for this performance section are
     *  sent in case of an abort.
     */
    private boolean m_SendReportForAbort;

    /**
     *  The flag that controls whether reports for this performance section are
     *  sent only when the threshold was exceeded.
     */
    @SuppressWarnings( "FieldNamingConvention" )
    private boolean m_SendReportOnlyForExceededThreshold;

    /**
     *  The unique name of the performance section.
     */
    private final PerformanceSectionName m_Name;

    /**
     *  The guard for read operations.
     */
    private final AutoLock m_ReadGuard;

    /**
     *  <p>{@summary The threshold time.} A value of {@code null} means that
     *  there is no threshold for this performance section.</p>
     */
    @SuppressWarnings( "UnusedAssignment" )
    private TimeValue m_Threshold = null;

    /**
     *  <p>{@summary The timeout time.} A value of {@code null} means that
     *  there is no timeout for this performance section.</p>
     */
    @SuppressWarnings( "UnusedAssignment" )
    private TimeValue m_Timeout = null;

    /**
     *  The guard for write operations.
     */
    private final AutoLock m_WriteGuard;

        /*--------------*\
    ====** Constructors **=====================================================
        \*--------------*/
    /**
     *  Creates a new instance of {@code PerformanceSection}.
     *
     *  @param  name    The unique name of the performance section.
     *  @param  description The description for the performance section; can be
     *      {@code null}.
     *  @param  threshold   The threshold time in milliseconds; a value of
     *      {@code null} means that no threshold was defined for this
     *      performance section. A value less than 1 is invalid and causes a
     *      {@link org.tquadrat.foundation.exception.ValidationException}
     *      to be thrown.
     *  @param  timeout The timeout time in milliseconds; a value of
     *      {@code null} means that no timeout was defined for this performance
     *      section. A value less than 1 is invalid and causes a
     *      {@link org.tquadrat.foundation.exception.ValidationException}
     *      to be thrown. The timeout value must be greater than the threshold
     *      value, if provided.
     *  @param  flags   Provides the configuration for this performance
     *      section.
     */
    public PerformanceSection( final String name, final String description, final Long threshold, final Long timeout, final PerformanceSectionFlags... flags )
    {
        this(
            createPerformanceSectionName( name ),
            description,
            isNull( threshold ) ? null : new TimeValue( MILLISECOND, requireValidArgument( threshold, "threshold", t -> t > 0L ) ),
            isNull( timeout ) ? null : new TimeValue( MILLISECOND, requireValidArgument( timeout, "timeout", t -> t > 0L ) ),
            flags
        );
    }   //  PerformanceSection()

    /**
     *  Creates a new instance of {@code PerformanceSection}.
     *
     *  @param  name    The unique name of the performance section.
     *  @param  description The description for the performance section; can be
     *      {@code null}.
     *  @param  threshold   The threshold time; a value of {@code null} means
     *      that no threshold was defined for this performance section. A value
     *      of 0 is invalid and causes a
     *      {@link org.tquadrat.foundation.exception.ValidationException}
     *      to be thrown.
     *  @param  timeout The timeout time; a value of {@code null} means that
     *      no timeout was defined for this performance section. A value of 0
     *      is invalid and causes a
     *      {@link org.tquadrat.foundation.exception.ValidationException}
     *      to be thrown. The timeout value must be greater than the threshold
     *      value, if provided.
     *  @param  flags   Provides the configuration for this performance
     *      section.
     */
    public PerformanceSection( final PerformanceSectionName name, final String description, final TimeValue threshold, final TimeValue timeout, final PerformanceSectionFlags... flags )
    {
        m_Description = mapFromEmpty( description, "?" );
        final Set<PerformanceSectionFlags> flagsSet = EnumSet.noneOf( PerformanceSectionFlags.class );
        flagsSet.addAll( asList( requireNonNullArgument( flags, "flags" ) ) );
        m_Ignore = flagsSet.contains( IGNORED );
        m_Name = requireNonNullArgument( name, "name" );
        m_SendReportForAbort = flagsSet.contains( SEND_REPORT_FOR_ABORT );
        m_SendReportOnlyForExceededThreshold = flagsSet.contains( SEND_REPORT_ONLY_FOR_EXCEEDED_THRESHOLD );
        m_Threshold = validateThresholdAndTimeout( threshold, threshold, null );
        m_Timeout = validateThresholdAndTimeout( timeout, threshold, timeout );

        final var lock = new ReentrantReadWriteLock();
        m_ReadGuard = AutoLock.of( lock.readLock() );
        m_WriteGuard = AutoLock.of( lock.writeLock() );
    }   //  PerformanceSection()

        /*---------*\
    ====** Methods **==========================================================
        \*---------*/
    /**
     * {@inheritDoc}
     * <p>Two instances of {@code PerformanceSection} are equal if their names
     * are equal.</p>
     */
    @Override
    public final boolean equals( final Object o )
    {
        var retValue = this == o;
        if( !retValue && o instanceof final PerformanceSection other )
        {
            retValue = getName().equals( other.getName() );
        }

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  equals()

    /**
     *  Returns the description for the performance section.
     *
     *  @return The description for the performance section.
     */
    public final String getDescription() { return m_Description; }

    /**
     *  Returns the name of the performance section.
     *
     *  @return The name of the performance section.
     */
    public final PerformanceSectionName getName() { return m_Name; }

    /**
     *  Returns the threshold for this performance section.
     *
     *  @return An instance of
     *      {@link OptionalLong}
     *      that holds the threshold in milliseconds.
     */
    public final Optional<TimeValue> getThreshold()
    {
        final var retValue = m_ReadGuard.execute( () -> m_Threshold );

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  getThreshold()

    /**
     *  Returns the timeout for this performance section.
     *
     *  @return An instance of
     *      {@link OptionalLong}
     *      that holds the timeout in milliseconds.
     */
    public final Optional<TimeValue> getTimeout()
    {
        final var retValue = m_ReadGuard.execute( () -> m_Timeout );

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  getTimeout()

    /**
     * {@inheritDoc}
     * <p>Two instances of {@code PerformanceSection} are equal if their names
     * are equal.</p>
     */
    @Override
    public final int hashCode() { return getName().hashCode(); }

    /**
     *  Returns the flag that controls whether this performance section is
     *  ignored.
     *
     *  @return {@code true} if the performance section is currently ignored,
     *      {@code false} if the performance section is currently observed.
     */
    public final boolean isIgnored() { return m_ReadGuard.evaluate( () -> m_Ignore ); }

    /**
     *  Returns the flag that indicates whether reports should be sent if a
     *  {@link PerformanceTracker}
     *  is aborted.
     *
     *  @return {@code true} if a report should be sent, {@code false} if not.
     *
     *  @see PerformanceTracker#abort(String)
     *  @see PerformanceTracker#abort(String, Throwable)
     */
    public final boolean isSendingReportForAbort() { return m_ReadGuard.evaluate( () -> m_SendReportForAbort ); }

    /**
     *  Returns the flag that indicates whether reports should be sent only if
     *  the threshold was exceeded.
     *
     *  @return {@code true} if a report should be sent only when the threshold
     *      was exceeded, {@code false} if a report should be sent always.
     */
    @SuppressWarnings( "NewMethodNamingConvention" )
    public final boolean isSendingReportOnlyForExceededThreshold() { return m_ReadGuard.evaluate( () -> m_SendReportOnlyForExceededThreshold ); }

    /**
     *  Sets the flag that controls whether this performance section is
     *  ignored.
     *
     *  @param  flag    {@code true} if the performance section should be
     *      ignored from now on, {@code false} if it has to be observed in
     *      future.
     */
    public final void setIgnoreFlag( final boolean flag ) { m_WriteGuard.perform( () -> m_Ignore = flag ); }

    /**
     *  Sets the flag that controls whether reports are sent also for aborted
     *  {@linkplain PerformanceTracker performance trackers}.
     *
     *  @param  flag {@code true} if reports should be sent, {@code false} if
     *      not.
     */
    public final void setSendReportForAbortFlag( final boolean flag )
    {
        m_WriteGuard.perform( () -> m_SendReportForAbort = flag );
    }   //  setSendReportForAbortFlag()

    /**
     *  Sets the flag that controls whether reports are sent only when the
     *  threshold was exceeded.
     *
     *  @param  flag {@code true} if reports should be sent only when the
     *      threshold was exceeded, {@code false} if reports should be sent
     *      always.
     */
    @SuppressWarnings( "NewMethodNamingConvention" )
    public final void setSendReportOnlyForExceededThresholdFlag( final boolean flag )
    {
        m_WriteGuard.perform( () -> m_SendReportOnlyForExceededThreshold = flag );
    }   //  setSendReportOnlyForExceededThresholdFlag()

    /**
     *  Sets the threshold and enables it.
     *
     *  @param  value   The new threshold value; must be greater than 0.
     */
    public final void setThreshold( final TimeValue value )
    {
        try
        {
            m_WriteGuard.perform( () -> m_Threshold = validateThreshold( value ) );
        }
        catch( final ExecutionFailedException e )
        {
            final var cause = e.getCause();
            if( cause instanceof final IllegalArgumentException iae ) throw iae;
            throw e;
        }
    }   //  setThreshold()

    /**
     *  Sets the timeout and enables it.
     *
     *  @param  value   The new timeout value; must be greater than 0.
     */
    public final void setTimeout( final TimeValue value )
    {
        try
        {
            m_WriteGuard.perform( () -> m_Timeout = validateTimeout( value ) );
        }
        catch( final ExecutionFailedException e )
        {
            final var cause = e.getCause();
            if( cause instanceof final IllegalArgumentException iae ) throw iae;
            throw e;
        }
    }   //  setTimeout()

    /**
     *  Disables the threshold for this performance section.
     */
    public final void switchOffThreshold() { m_WriteGuard.perform( () -> m_Threshold = null ); }

    /**
     *  Disables the timeout for this performance section.
     */
    public final void switchOffTimeout() { m_WriteGuard.perform( () -> m_Timeout = null ); }

    /**
     * {@inheritDoc}
     */
    @Override
    public final String toString()
    {
        final var buffer = new StringBuilder( "PerformanceSection{" )
            .append( "m_Name='" ).append( m_Name ).append( "', " )
            .append( "m_Description='" ).append( m_Description ).append( "', " )
            .append( "m_Threshold=" );
        try( final var _ = m_ReadGuard.lock() )
        {
            if( nonNull( m_Threshold ) )
            {
                buffer.append( "%s".formatted( m_Threshold ) );
            }
            else
            {
                buffer.append( "none" );
            }
            buffer.append( ", m_Timeout=" );
            if( nonNull( m_Timeout ) )
            {
                buffer.append( "%s".formatted( m_Timeout ) );
            }
            else
            {
                buffer.append( "none" );
            }
            buffer.append( ", m_Ignore=" ).append( m_Ignore )
                .append( ", m_SendReportForAbort=" ).append( m_SendReportForAbort )
                .append( ", m_SendReportOnlyForExceededThreshold=" ).append( m_SendReportOnlyForExceededThreshold )
                .append( '}' );
        }
        final var retValue = buffer.toString();

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  toString()

    /**
     *  <p>{@summary Validates the given threshold.}</p>
     *  <p>The value must be greater than 0, and, if the
     *  timeout is not {@code null}, it must be less than the timeout.</p>
     *
     *  @note The method is called from inside a guarded area only!
     *
     *  @param  value   The value for the threshold.
     *  @return The given value.
     *  @throws IllegalArgumentException The value is invalid.
     */
    private final TimeValue validateThreshold( final TimeValue value ) throws IllegalArgumentException
    {
        final var retValue = validateThresholdAndTimeout( requireNonNullArgument( value, "threshold" ), value, m_Timeout );

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  validateThreshold()

    /**
     *  <p>{@summary Validates threshold and timeout.}</p>
     *  <p>Both values must be either -1 or greater than 0, and, if the
     *  threshold is not -1, the timeout must be greater than the
     *  threshold.</p>
     *
     *  @param  retValue    The return value.
     *  @param  threshold   The value for the threshold.
     *  @param  timeout The value for the timeout.
     *  @return The return value.
     *  @throws IllegalArgumentException A value is invalid.
     */
    @SuppressWarnings( "OverlyComplexMethod" )
    private static final TimeValue validateThresholdAndTimeout( final TimeValue retValue, final TimeValue threshold, final TimeValue timeout ) throws IllegalArgumentException
    {
        if( nonNull( retValue ) && !retValue.equals( threshold ) && !retValue.equals( timeout ) ) throw new IllegalArgumentException( "return value is neither threshold nor timeout" );
        if( nonNull( threshold ) && threshold.value().longValue() < 1L ) throw new ValidationException( "threshold value '%s' is invalid".formatted( threshold ) );
        if( nonNull( timeout ) && timeout.value().longValue() < 1L ) throw new ValidationException( "timeout value '%s' is invalid".formatted( timeout ) );
        if( nonNull( timeout ) && nonNull( threshold ) && timeout.baseValue().compareTo( threshold.baseValue() ) <= 0 ) throw new ValidationException( "timeout value '%s' is not greater than threshold value '%s'".formatted( timeout, threshold ) );

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  validateThresholdAndTimeout()

    /**
     *  <p>{@summary Validates the given timeout.}</p>
     *  <p>The value must be greater than 0, and, if the threshold is not
     *  {@code null}, the timeout must be greater than the threshold.</p>
     *
     *  @note The method is called from inside a guarded area only!
     *
     *  @param  value   The value for the timeout.
     *  @return The given value.
     *  @throws IllegalArgumentException The value is invalid.
     */
    private final TimeValue validateTimeout( final TimeValue value ) throws IllegalArgumentException
    {
        final var retValue = validateThresholdAndTimeout( requireNonNullArgument( value, "timeout" ), m_Threshold, value );

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  validateThresholdAndTimeout()
}
//  class PerformanceSection

/*
 *  End of File
 */