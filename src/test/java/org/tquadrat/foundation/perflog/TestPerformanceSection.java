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

import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.TWO;
import static java.math.BigDecimal.ZERO;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.tquadrat.foundation.lang.CommonConstants.EMPTY_STRING;
import static org.tquadrat.foundation.perflog.PerfLogUtils.createPerformanceSectionName;
import static org.tquadrat.foundation.perflog.PerformanceSection.PerformanceSectionFlags.IGNORED;
import static org.tquadrat.foundation.perflog.PerformanceSection.PerformanceSectionFlags.SEND_REPORT_FOR_ABORT;
import static org.tquadrat.foundation.util.StringUtils.isNotEmpty;
import static org.tquadrat.foundation.value.Time.MILLISECOND;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.tquadrat.foundation.annotation.ClassVersion;
import org.tquadrat.foundation.exception.BlankArgumentException;
import org.tquadrat.foundation.exception.EmptyArgumentException;
import org.tquadrat.foundation.exception.NullArgumentException;
import org.tquadrat.foundation.exception.UnexpectedExceptionError;
import org.tquadrat.foundation.exception.ValidationException;
import org.tquadrat.foundation.perflog.PerformanceSection.PerformanceSectionFlags;
import org.tquadrat.foundation.testutil.TestBaseClass;
import org.tquadrat.foundation.value.TimeValue;

/**
 *  Some tests for the class
 *  {@link PerformanceSection}.
 *
 *  @extauthor Thomas Thrien - thomas.thrien@tquadrat.org
 *  @version $Id: TestPerformanceSection.java 1258 2026-06-04 18:33:06Z tquadrat $
 */
@ClassVersion( sourceVersion = "$Id: TestPerformanceSection.java 1258 2026-06-04 18:33:06Z tquadrat $" )
@DisplayName( "org.tquadrat.foundation.perflog.TestPerformanceSection" )
public class TestPerformanceSection extends TestBaseClass
{
        /*------------------------*\
    ====** Static Initialisations **===========================================
        \*------------------------*/
    /**
     *  The method reference to
     *  {@link PerformanceSection#validateThresholdAndTimeout(TimeValue,TimeValue,TimeValue)}.
     */
    private static final Method m_ValidateThresholdAndTimeoutMethod;

    static
    {
        try
        {
            final var candidateClass = PerformanceSection.class;
            m_ValidateThresholdAndTimeoutMethod = candidateClass.getDeclaredMethod( "validateThresholdAndTimeout", TimeValue.class, TimeValue.class, TimeValue.class );
            m_ValidateThresholdAndTimeoutMethod.setAccessible( true );
        }
        catch( final NoSuchMethodException e )
        {
            throw new ExceptionInInitializerError( e );
        }
    }

        /*---------*\
    ====** Methods **==========================================================
        \*---------*/
    /**
     *  Some tests for the constructors of
     *  {@link PerformanceSection}.
     *
     *  @throws Exception   Something went awfully wrong.
     */
    @Test
    final void testConstructor() throws Exception
    {
        skipThreadTest();

        final var nameString = "PerformanceSection";
        final var name = createPerformanceSectionName( nameString );
        final var description = "Description";
        final var thresholdValue = Long.valueOf( 5 );
        final var threshold = new TimeValue( MILLISECOND, thresholdValue );
        final var timeoutValue = Long.valueOf( 10 );
        final var timeout = new TimeValue( MILLISECOND, timeoutValue );
        final var flags = new PerformanceSectionFlags[] {IGNORED, SEND_REPORT_FOR_ABORT};

        assertThrows( NullArgumentException.class, () -> new PerformanceSection( null, description, threshold, timeout, flags ) );

        assertThrows( NullArgumentException.class, () -> new PerformanceSection( null, description, thresholdValue, timeoutValue, flags ) );
        assertThrows( EmptyArgumentException.class, () -> new PerformanceSection( EMPTY_STRING, description, thresholdValue, timeoutValue, flags ) );
        assertThrows( BlankArgumentException.class, () -> new PerformanceSection( " ", description, thresholdValue, timeoutValue, flags ) );

        assertThrows( NullArgumentException.class, () -> new PerformanceSection( null, description, threshold, timeout, flags ) );

        assertThrows( ValidationException.class, () -> new PerformanceSection( nameString, description, 0L, timeoutValue, flags ) );
        assertThrows( ValidationException.class, () -> new PerformanceSection( nameString, description, 200L, timeoutValue, flags ) );
        assertThrows( ValidationException.class, () -> new PerformanceSection( nameString, description, -200L, timeoutValue, flags ) );

        assertThrows( ValidationException.class, () -> new PerformanceSection( name, description, new TimeValue( MILLISECOND, ZERO ), timeout, flags ) );
        assertThrows( ValidationException.class, () -> new PerformanceSection( name, description, new TimeValue( MILLISECOND, 200 ), timeout, flags ) );

        assertThrows( ValidationException.class, () -> new PerformanceSection( nameString, description, null, 0L, flags ) );
        assertThrows( ValidationException.class, () -> new PerformanceSection( nameString, description, null, -200L, flags ) );

        assertThrows( ValidationException.class, () -> new PerformanceSection( name, description, null, new TimeValue( MILLISECOND, ZERO ), flags ) );

        assertThrows( ValidationException.class, () -> new PerformanceSection( nameString, description, thresholdValue, 0L, flags ) );
        assertThrows( ValidationException.class, () -> new PerformanceSection( nameString, description, thresholdValue, -200L, flags ) );
        assertThrows( ValidationException.class, () -> new PerformanceSection( nameString, description, thresholdValue, thresholdValue, flags ) );

        assertThrows( ValidationException.class, () -> new PerformanceSection( name, description, threshold, new TimeValue( MILLISECOND, ZERO ), flags ) );
        assertThrows( ValidationException.class, () -> new PerformanceSection( name, description, threshold, threshold, flags ) );

        assertThrows( NullArgumentException.class, () -> new PerformanceSection( nameString, description, thresholdValue, timeoutValue, (PerformanceSectionFlags[]) null ) );

        var candidate = assertDoesNotThrow( () -> new PerformanceSection( nameString, description, thresholdValue, timeoutValue, flags ) );
        assertNotNull( candidate );
        assertEquals( name, candidate.getName() );
        assertTrue( candidate.getThreshold().isPresent() );
        assertEquals( threshold, candidate.getThreshold().get() );
        assertTrue( candidate.getTimeout().isPresent() );
        assertEquals( timeout, candidate.getTimeout().get() );

        candidate = new PerformanceSection( name, description, threshold, timeout, flags );
        assertNotNull( candidate );
        assertSame( name, candidate.getName() );
        assertTrue( candidate.getThreshold().isPresent() );
        assertEquals( threshold, candidate.getThreshold().get() );
        assertTrue( candidate.getTimeout().isPresent() );
        assertEquals( timeout, candidate.getTimeout().get() );

        candidate = assertDoesNotThrow( () -> new PerformanceSection( nameString, description, null, null, flags ) );
        assertNotNull( candidate );

        candidate = assertDoesNotThrow( () -> new PerformanceSection( nameString, description, null, null, flags ) );
        assertNotNull( candidate );

        candidate = assertDoesNotThrow( () -> new PerformanceSection( name, description, null, null, flags ) );
        assertNotNull( candidate );

        assertEquals( description, candidate.getDescription() );

        candidate = assertDoesNotThrow( () -> new PerformanceSection( name, null, null, null, flags ) );
        assertNotNull( candidate );
        assertTrue( isNotEmpty( candidate.getDescription() ) );

        candidate = assertDoesNotThrow( () -> new PerformanceSection( name, EMPTY_STRING, null, null, flags ) );
        assertNotNull( candidate );
        assertTrue( isNotEmpty( candidate.getDescription() ) );
    }   //  testConstructor()

    /**
     *  Some tests for the setters of class
     *  {@link PerformanceSection}.
     *
     *  @throws Exception   Something went awfully wrong.
     */
    @Test
    final void testSetters() throws Exception
    {
        skipThreadTest();
        final var candidate = assertDoesNotThrow( () -> new PerformanceSection( "PerformanceSection", "Description", null, null, IGNORED, SEND_REPORT_FOR_ABORT ) );
        assertNotNull( candidate );
        assertTrue( candidate.isIgnored() );
        assertTrue( candidate.getThreshold().isEmpty() );
        assertTrue( candidate.getTimeout().isEmpty() );

        candidate.setIgnoreFlag( false );
        assertFalse( candidate.isIgnored() );

        candidate.switchOffThreshold();
        assertTrue( candidate.getThreshold().isEmpty() );

        candidate.switchOffTimeout();
        assertTrue( candidate.getTimeout().isEmpty() );

        assertThrows( NullArgumentException.class, () -> candidate.setThreshold( null ) );
        assertThrowsExactly( ValidationException.class, () -> candidate.setThreshold( new TimeValue( MILLISECOND, ZERO ) ) );

        assertThrows( NullArgumentException.class, () -> candidate.setTimeout( null ) );
        assertThrowsExactly( ValidationException.class, () -> candidate.setTimeout( new TimeValue( MILLISECOND, ZERO ) ) );

        final var threshold = new TimeValue( MILLISECOND, 10 );
        candidate.setThreshold( threshold );
        assertTrue( candidate.getThreshold().isPresent() );
        assertTrue( candidate.getTimeout().isEmpty() );
        assertEquals( threshold, candidate.getThreshold().get() );

        final var timeout = (TimeValue) threshold.multiply( 2 );
        assertThrows( ValidationException.class, () -> candidate.setTimeout( threshold ) );
        assertThrows( ValidationException.class, () -> candidate.setTimeout( (TimeValue) threshold.divide( 2 ) ) );
        candidate.setTimeout( timeout );
        assertTrue( candidate.getTimeout().isPresent() );
        assertTrue( candidate.getThreshold().isPresent() );
        assertEquals( timeout, candidate.getTimeout().get() );

        assertThrows( ValidationException.class, () -> candidate.setThreshold( timeout ) );
        assertThrows( ValidationException.class, () -> candidate.setThreshold( (TimeValue) timeout.multiply( 2 ) ) );

        candidate.switchOffThreshold();
        assertTrue( candidate.getThreshold().isEmpty() );
        assertTrue( candidate.getTimeout().isPresent() );

        candidate.switchOffTimeout();
        assertTrue( candidate.getTimeout().isEmpty() );
    }   //  testSetters()

    /**
     *  Tests for the method
     *  {@link PerformanceSection#validateThresholdAndTimeout(TimeValue,TimeValue,TimeValue)}.
     *
     *  @throws Exception   Something went awfully wrong.
     */
    @Test
    final void testValidation() throws Exception
    {
        skipThreadTest();

        assertThrows( IllegalArgumentException.class, () -> validateThresholdAndTimeout( new TimeValue( MILLISECOND, ONE ), new TimeValue( MILLISECOND, TWO ), new TimeValue( MILLISECOND, 3 ) ) );
        assertThrows( ValidationException.class, () -> validateThresholdAndTimeout( new TimeValue( MILLISECOND, ZERO ), new TimeValue( MILLISECOND, ZERO ), null ) );
        assertThrows( ValidationException.class, () -> validateThresholdAndTimeout( new TimeValue( MILLISECOND, ZERO ), null, new TimeValue( MILLISECOND, ZERO ) ) );
        assertThrows( ValidationException.class, () -> validateThresholdAndTimeout( new TimeValue( MILLISECOND, 5 ), new TimeValue( MILLISECOND, 7 ), new TimeValue( MILLISECOND, 5 ) ) );

        assertNull( validateThresholdAndTimeout( null, null, null ) );
        assertNull( validateThresholdAndTimeout( null, new TimeValue( MILLISECOND, 7 ), null ) );
        assertNull( validateThresholdAndTimeout( null, null, new TimeValue( MILLISECOND, 7 ) ) );
        assertEquals( new TimeValue( MILLISECOND, 7 ), validateThresholdAndTimeout( new TimeValue( MILLISECOND, 7 ), new TimeValue( MILLISECOND, 7 ), null ) );
        assertEquals( new TimeValue( MILLISECOND, 7 ), validateThresholdAndTimeout( new TimeValue( MILLISECOND, 7 ), null, new TimeValue( MILLISECOND, 7 ) ) );
    }   //  testValidation()

    /**
     *  <p>{@summary Validates threshold and timeout.}</p>
     *  <p>Both values must be either {@null} or greater than 0, and, if
     *  the threshold is not {@null}, the timeout must be greater than the
     *  threshold.</p>
     *
     *  @param  value    The return value.
     *  @param  threshold   The value for the threshold.
     *  @param  timeout The value for the timeout.
     *  @return The return value.
     *  @throws ValidationException A value is invalid.
     */
    private static final TimeValue validateThresholdAndTimeout( final TimeValue value, final TimeValue threshold, final TimeValue timeout ) throws ValidationException
    {
        final TimeValue retValue;
        try
        {
            retValue = ((TimeValue) m_ValidateThresholdAndTimeoutMethod.invoke( null, value, threshold, timeout ) );
        }
        catch( final InvocationTargetException e )
        {
            final var cause = e.getCause();
            if( cause instanceof final IllegalArgumentException iae ) throw iae;
            throw new UnexpectedExceptionError( e );
        }
        catch( final IllegalAccessException e )
        {
            throw new UnexpectedExceptionError( e );
        }

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  validateThresholdAndTimeout()
}
//  class TestPerformanceSection

/*
 *  End of File
 */