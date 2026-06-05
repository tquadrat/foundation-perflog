package org.tquadrat.foundation.perflog.internal;

import static java.time.ZoneOffset.UTC;
import static org.apiguardian.api.API.Status.INTERNAL;
import static org.tquadrat.foundation.lang.CommonConstants.EMPTY_STRING;
import static org.tquadrat.foundation.lang.Objects.hash;
import static org.tquadrat.foundation.lang.Objects.isNull;
import static org.tquadrat.foundation.lang.Objects.requireNonNullArgument;
import static org.tquadrat.foundation.perflog.PerformanceTracker.TrackerStatus.STATUS_ABORTED;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.StringJoiner;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apiguardian.api.API;
import org.tquadrat.foundation.annotation.ClassVersion;
import org.tquadrat.foundation.lang.AutoLock;
import org.tquadrat.foundation.perflog.PerformanceSection;
import org.tquadrat.foundation.perflog.PerformanceSectionName;
import org.tquadrat.foundation.value.Time;
import org.tquadrat.foundation.value.TimeValue;

/**
 *  <p>{@summary Instances of this class holds the execution status for a
 *  &quot;Performance Section&quot;.}</p>
 *
 *  @extauthor Thomas Thrien - thomas.thrien@tquadrat.org
 *  @version $Id: PerformanceSectionInfo.java 1258 2026-06-04 18:33:06Z tquadrat $
 *  @since 0.25.0
 *
 *  @UMLGraph.link
 */
@ClassVersion( sourceVersion = "$Id: PerformanceSectionInfo.java 1258 2026-06-04 18:33:06Z tquadrat $" )
@API( status = INTERNAL, since = "0.25.0" )
public class PerformanceSectionInfo
{
        /*------------*\
    ====** Attributes **=======================================================
        \*------------*/
    /**
     *  The accumulated execution time of the performance section since the
     *  last restart of the program, in milliseconds.
     *
     *  @see #m_FirstStart
     */
    private long m_CumulatedExecutionTime = 0L;

    /**
     *  <p>{@summary The time when the performance section was first started
     *  after the last restart of the program.}</p>
     */
    private Instant m_FirstStart;

    /**
     *  <p>{@summary The time when this performance section info was last
     *  updated.} This attribute is used to determine when the report entries
     *  for the performance section should be discarded.</p>
     */
    private Instant m_LastUpdated;

    /**
     *  <p>{@summary The number of aborted runs that timed out since the last
     *  restart of the program.} These are <i>not</i> included in
     *  {@link #m_NumberOfCompletedRuns}.</p>
     *
     *  @see #m_FirstStart
     */
    private int m_NumberOfAbortedRuns = 0;

    /**
     *  The number of runs with an elapsed time since the last restart of the
     *  program.
     *
     *  @see #m_FirstStart
     */
    private int m_NumberOfCompletedRuns = 0;

    /**
     *  <p>{@summary The number of runs that exceeded the threshold since the
     *  last restart of the program.} These are included in
     *  {@link #m_NumberOfCompletedRuns}.</p>
     *
     *  @see #m_FirstStart
     */
    @SuppressWarnings( "FieldNamingConvention" )
    private int m_NumberOfRunsThatExceededThreshold = 0;

    /**
     *  <p>{@summary The number of runs that timed out since the last restart
     *  of the program.} These are <i>not</i> included in
     *  {@link #m_NumberOfCompletedRuns}
     *  nor in
     *  {@link #m_NumberOfAbortedRuns}.</p>
     *
     *  @see #m_FirstStart
     */
    private int m_NumberOfTimedOutRuns = 0;

    /**
     *  The performance section.
     */
    private final PerformanceSection m_PerformanceSection;

    /**
     *  <p>{@summary The read guard for the statistics attributes.} These
     *  are:</p>
     *  <ul>
     *      <li>{@link #m_FirstStart}</li>
     *      <li>{@link #m_LastUpdated}</li>
     *      <li>{@link #m_NumberOfAbortedRuns}</li>
     *      <li>{@link #m_NumberOfCompletedRuns}</li>
     *      <li>{@link #m_NumberOfRunsThatExceededThreshold}</li>
     *      <li>{@link #m_NumberOfTimedOutRuns}</li>
     *  </ul>
     */
    private final AutoLock m_ReadGuard;

    /**
     *  <p>{@summary The write guard for the statistics attributes.} These
     *  are:</p>
     *  <ul>
     *      <li>{@link #m_FirstStart}</li>
     *      <li>{@link #m_LastUpdated}</li>
     *      <li>{@link #m_NumberOfAbortedRuns}</li>
     *      <li>{@link #m_NumberOfCompletedRuns}</li>
     *      <li>{@link #m_NumberOfRunsThatExceededThreshold}</li>
     *      <li>{@link #m_NumberOfTimedOutRuns}</li>
     *  </ul>
     */
    private final AutoLock m_WriteGuard;

        /*--------------*\
    ====** Constructors **=====================================================
        \*--------------*/
    /**
     *  Creates a new instance of {@code PerformanceSectionInfo}.
     *
     *  @param  performanceSection  The performance section.
     */
    public PerformanceSectionInfo( final PerformanceSection performanceSection )
    {
        m_PerformanceSection = requireNonNullArgument( performanceSection, "performanceSection" );

        final var lock = new ReentrantReadWriteLock();
        m_ReadGuard = AutoLock.of( lock.readLock() );
        m_WriteGuard = AutoLock.of( lock.writeLock() );
    }   //  PerformanceSectionInfo()

    /**
     *  <p>{@summary Creates a new instance of
     *  {@code PerformanceSectionInfo}.}</p>
     *  <p>The wrapped
     *  {@link PerformanceSection}
     *  instance will be created on the fly with default values:</p>
     *  <ul>
     *      <li>{@linkplain PerformanceSection#getDescription() Description}: empty</li>
     *      <li>{@linkplain PerformanceSection#getThreshold() Threshold}: disabled</li>
     *      <li>{@linkplain PerformanceSection#getTimeout() Timeout}: disabled</li>
     *      <li>{@linkplain PerformanceSection#isSendingReportForAbort() Sending Report for Abort}: true</li>
     *      <li>{@linkplain PerformanceSection#isIgnored() Active}: true</li>
     *  </ul>
     *
     *  @param  performanceSectionName  The name for a new performance section
     *      with default settings.
     */
    @SuppressWarnings( "MethodParameterNamingConvention" )
    public PerformanceSectionInfo( final PerformanceSectionName performanceSectionName )
    {
        this( new PerformanceSection( requireNonNullArgument( performanceSectionName, "performanceSectionName" ), EMPTY_STRING, null, null ) );
    }   //  PerformanceSectionInfo()

    /**
     *  <p>{@summary Creates a new instance of
     *  {@code PerformanceSectionInfo}.}</p>
     *  <p>The wrapped
     *  {@link PerformanceSection}
     *  instance will be created on the fly with default values:</p>
     *  <ul>
     *      <li>{@linkplain PerformanceSection#getDescription() Description}: empty</li>
     *      <li>{@linkplain PerformanceSection#getThreshold() Threshold}: disabled</li>
     *      <li>{@linkplain PerformanceSection#getTimeout() Timeout}: disabled</li>
     *      <li>{@linkplain PerformanceSection#isSendingReportForAbort() Sending Report for Abort}: true</li>
     *      <li>{@linkplain PerformanceSection#isIgnored() Active}: true</li>
     *  </ul>
     *
     *  @param  performanceSectionName  The name for a new performance section
     *      with default settings.
     */
    @SuppressWarnings( "MethodParameterNamingConvention" )
    public PerformanceSectionInfo( final String performanceSectionName )
    {
        this( new PerformanceSectionNameImpl( performanceSectionName ) );
    }   //  PerformanceSectionInfo()

        /*---------*\
    ====** Methods **==========================================================
        \*---------*/
    /**
     * {@inheritDoc}
     * <p>Two instances of {@code PerformanceSectionInfo} are equal if both
     * refer to equal
     * {@link PerformanceSection}
     * instances.</p>
     */
    @Override
    public final boolean equals( final Object o )
    {
        var retValue = this == o;
        if( !retValue && o instanceof final PerformanceSectionInfo other )
        {
            retValue = m_PerformanceSection.equals( other.m_PerformanceSection );
        }

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  equals()

    /**
     *  Returns the average execution time of the performance section.
     *
     *  @return An instance of
     *      {@link OptionalLong}
     *      holding the average execution time in milliseconds. Will be empty
     *      if no successful execution was recorded so far.
     */
    public final OptionalLong getAverageExecutionTime()
    {
        final OptionalLong retValue;
        try( final var _ = m_ReadGuard.lock() )
        {
            retValue = m_NumberOfCompletedRuns == 0
                ? OptionalLong.empty()
                : OptionalLong.of( m_CumulatedExecutionTime / (long) m_NumberOfCompletedRuns );
        }

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  getAverageExecutionTime()

    /**
     *  Returns the description for the performance section.
     *
     *  @return The description for the performance section.
     */
    public final String getDescription() { return m_PerformanceSection.getDescription(); }

    /**
     *  Returns the start time for the first execution of the performance
     *  section after the last restart of the program.
     *
     *  @return An instance of
     *      {@link Optional}
     *      that holds the first start time.
     */
    public final Optional<ZonedDateTime> getFirstStart()
    {
        final var retValue = m_ReadGuard.execute( () -> m_FirstStart ).map( v -> v.atZone( UTC ) );

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  getFirstStart()

    /**
     *  Returns the time for the last update of this performance section info
     *  instance after the last restart of the program.
     *
     *  @return An instance of
     *      {@link Optional}
     *      that holds the last update time.
     */
    public final Optional<ZonedDateTime> getLastUpdated()
    {
        final var retValue = m_ReadGuard.execute( () -> m_LastUpdated ).map( v -> v.atZone( UTC ) );

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  getLastUpdated()

    /**
     *  Returns the name of the performance section.
     *
     *  @return The name of the performance section.
     */
    public final PerformanceSectionName getName() { return m_PerformanceSection.getName(); }

    /**
     *  <p>{@summary Returns the number of aborted runs.} Will be empty if no
     *  run was recorded yet.</p>
     *  <p>Runs that timed out are not included in this number.</p>
     *
     *  @return An instance of
     *      {@link OptionalInt}
     *      that holds the number of aborted runs.
     */
    public final OptionalInt getNumberOfAbortedRuns()
    {
        final OptionalInt retValue;
        try( final var _ = m_ReadGuard.lock() )
        {
            retValue = isNull( m_LastUpdated )
                ? OptionalInt.empty()
                : OptionalInt.of( m_NumberOfAbortedRuns );
        }

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  getNumberOfAbortedRuns()

    /**
     *  <p>{@summary Returns the number of completed runs.} These are the runs
     *  that provided an
     *  {@linkplain PerformanceTrackerImpl#getElapsedTime() elapsed time}.
     *  Will be empty if no run was recorded yet.</p>
     *
     *  @return An instance of
     *      {@link OptionalInt}
     *      that holds the number of completed runs.
     */
    public final OptionalInt getNumberOfCompletedRuns()
    {
        final OptionalInt retValue;
        try( final var _ = m_ReadGuard.lock() )
        {
            retValue = isNull( m_LastUpdated )
                ? OptionalInt.empty()
                : OptionalInt.of( m_NumberOfCompletedRuns );
        }

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  getNumberOfCompletedRuns()

    /**
     *  <p>{@summary Returns the number of completed runs that exceeded the
     *  {@linkplain #getThreshold() threshold}.} These runs are also included
     *  into the number of
     *  {@linkplain #getNumberOfCompletedRuns() completed runs}.
     *  Will be empty if no run was recorded yet.</p>
     *
     *  @return An instance of
     *      {@link OptionalInt}
     *      that holds the number of completed runs exceeding the threshold.
     */
    @SuppressWarnings( "NewMethodNamingConvention" )
    public final OptionalInt getNumberOfRunsThatExceededThreshold()
    {
        final OptionalInt retValue;
        try( final var _ = m_ReadGuard.lock() )
        {
            retValue = isNull( m_LastUpdated )
                ? OptionalInt.empty()
                : OptionalInt.of( m_NumberOfRunsThatExceededThreshold );
        }

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  getNumberOfRunsThatExceededThreshold()

    /**
     *  <p>{@summary Returns the number of runs that timed ot.} Will be empty
     *  if no run was recorded yet.</p>
     *  <p>Runs that were aborted due to other reasons than a timeout are not
     *  included in this number.</p>
     *
     *  @return An instance of
     *      {@link OptionalInt}
     *      that holds the number of timed out runs.
     */
    public final OptionalInt getNumberOfTimedOutRuns()
    {
        final OptionalInt retValue;
        try( final var _ = m_ReadGuard.lock() )
        {
            retValue = isNull( m_LastUpdated )
                ? OptionalInt.empty()
                : OptionalInt.of( m_NumberOfTimedOutRuns );
        }

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  getNumberOfTimedOutRuns()

    /**
     *  Returns the
     *  {@link PerformanceSection}
     *  hold by this {@code PerformanceSectionInfo} instance.
     *
     *  @return The performance section.
     */
    public final PerformanceSection getPerformanceSection() { return m_PerformanceSection; }

    /**
     *  Returns the threshold for the performance section.
     *
     *  @return An instance of
     *      {@link OptionalLong}
     *      that holds the threshold in milliseconds.
     */
    public final Optional<TimeValue> getThreshold() { return m_PerformanceSection.getThreshold(); }

    /**
     *  Returns the timeout for this performance section.
     *
     *  @return An instance of
     *      {@link OptionalLong}
     *      that holds the timeout in milliseconds.
     */
    public final Optional<TimeValue> getTimeout() { return m_PerformanceSection.getTimeout(); }

    /**
     * {@inheritDoc}
     * <p>Two instances of {@code PerformanceSectionInfo} are equal if both
     * refer to equal
     * {@link PerformanceSection}
     * instances.</p>
     */
    @Override
    public final int hashCode() { return hash( m_PerformanceSection ); }

    /**
     *  Returns the flag that controls whether the performance section is
     *  currently ignored.
     *
     *  @return {@true} if the performance section is currently ignored,
     *      {@false} if the performance section is currently observed.
     */
    public final boolean isIgnored() { return m_PerformanceSection.isIgnored(); }

    /**
     *  <p>{@summary Checks whether the average execution time is above the
     *  threshold for the performance section.} If the threshold is disabled,
     *  the method returns {@false}.</p>
     *
     *  @return {@true} if the average execution time is above the
     *      threshold, {@false} otherwise.
     *
     *  @see PerformanceSection#getThreshold()
     */
    public final boolean isAverageAboveThreshold()
    {
        final var averageTime = getAverageExecutionTime();
        final var threshold = getThreshold().map( v -> v.convert( Time.MILLISECOND ).longValue() );

        final var retValue = averageTime.isPresent() && threshold.isPresent() && averageTime.getAsLong() > threshold.get().longValue();

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  isAverageAboveThreshold()

    /**
     *  Returns the flag that indicates whether reports should be sent only if
     *  the threshold was exceeded.
     *
     *  @return {@true} if a report should be sent only when the threshold
     *     was exceeded, {@false} if a report should be sent always.
     */
    @SuppressWarnings( "NewMethodNamingConvention" )
    public final boolean isSendingReportOnlyForExceededThreshold() { return m_PerformanceSection.isSendingReportOnlyForExceededThreshold(); }

    /**
     *  Processes the given performance tracker.
     *
     *  @param  tracker The tracker to process.
     */
    public final void processTracker( final PerformanceTrackerImpl tracker )
    {
        try( final var _ = m_WriteGuard.lock() )
        {
            if( m_PerformanceSection.equals( requireNonNullArgument( tracker, "tracker" ).getPerformanceSection() ) )
            {
                m_LastUpdated = Instant.now();
                if( isNull( m_FirstStart ) || tracker.getTimestamp().isBefore( m_FirstStart ) ) m_FirstStart = tracker.getTimestamp();
            }

            tracker.getElapsedTime().ifPresent( v ->
            {
                m_CumulatedExecutionTime += v.convert( Time.MILLISECOND ).longValue();
                ++m_NumberOfCompletedRuns;

                if( tracker.isThresholdExceeded() ) ++m_NumberOfRunsThatExceededThreshold;
            });

            if( tracker.isTimedOut() )
            {
                ++m_NumberOfTimedOutRuns;
            }
            else if( tracker.getStatus() == STATUS_ABORTED )
            {
                ++m_NumberOfAbortedRuns;
            }
        }
    }   //  processTracker()

    /**
     * {@inheritDoc}
     */
    @Override
    public final String toString()
    {
        final var buffer = new StringJoiner( ",", "PerformanceSectionInfo{", "}" )
            .add( "m_PerformanceSection=".concat( m_PerformanceSection.toString() ) )
            .add( "m_CumulatedExecutionTime=%dms".formatted( m_CumulatedExecutionTime ) )
            .add( "m_FirstStart=".concat( m_FirstStart.toString() ) )
            .add( "m_LastUpdated=".concat( m_LastUpdated.toString() ) )
            .add( "m_NumberOfAbortedRuns=%d".formatted( m_NumberOfAbortedRuns ) )
            .add( "m_NumberOfCompletedRuns=%d".formatted( m_NumberOfCompletedRuns ) )
            .add( "m_NumberOfRunsThatExceededThreshold=%d".formatted( m_NumberOfRunsThatExceededThreshold ) )
            .add( "m_NumberOfTimedOutRuns=%d".formatted( m_NumberOfTimedOutRuns ) );
        final var retValue = buffer.toString();

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  toString()
}
//  class PerformanceSectionInfo

/*
 *  End of File
 */