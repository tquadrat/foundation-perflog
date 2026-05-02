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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.tquadrat.foundation.lang.CommonConstants.EMPTY_STRING;
import static org.tquadrat.foundation.perflog.PerfLogUtils.createPerformanceSectionName;
import static org.tquadrat.foundation.perflog.PerformanceSectionName.getStringConverter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.tquadrat.foundation.annotation.ClassVersion;
import org.tquadrat.foundation.exception.BlankArgumentException;
import org.tquadrat.foundation.exception.EmptyArgumentException;
import org.tquadrat.foundation.exception.NullArgumentException;
import org.tquadrat.foundation.lang.StringConverter;
import org.tquadrat.foundation.perflog.PerformanceSectionName;
import org.tquadrat.foundation.testutil.TestBaseClass;

/**
 *  Some tests for the interface
 *  {@link org.tquadrat.foundation.perflog.PerformanceSectionName}
 *  and its implementation class
 *  {@link PerformanceSectionNameImpl}.
 *
 *  @extauthor Thomas Thrien - thomas.thrien@tquadrat.org
 *  @version $Id: TestPerformanceSectionName.java 1211 2026-05-01 15:24:10Z tquadrat $
 */
@ClassVersion( sourceVersion = "$Id: TestPerformanceSectionName.java 1211 2026-05-01 15:24:10Z tquadrat $" )
@DisplayName( "org.tquadrat.foundation.perflog.internal.TestPerformanceSectionName" )
public class TestPerformanceSectionName extends TestBaseClass
{
        /*---------*\
    ====** Methods **==========================================================
        \*---------*/
    /**
     *  Some tests for the
     *  {@linkplain PerformanceSectionNameImpl#PerformanceSectionNameImpl(String) constructor}
     *  of
     *  {@link PerformanceSectionNameImpl}.
     *
     *  @throws Exception   Something went awfully wrong.
     */
    @Test
    final void testConstructor() throws Exception
    {
        skipThreadTest();

        final var name = "PerformanceSection";

        assertThrows( NullArgumentException.class, () -> new PerformanceSectionNameImpl( null ) );
        assertThrows( EmptyArgumentException.class, () -> new PerformanceSectionNameImpl( EMPTY_STRING ) );
        assertThrows( BlankArgumentException.class, () -> new PerformanceSectionNameImpl( " " ) );

        final var candidate = new PerformanceSectionNameImpl( name );
        assertNotNull( candidate );
        assertEquals( name, candidate.toString() );
    }   //  testConstructor()

    /**
     *  Tests for
     *  {@link PerformanceSectionName#equals(Object)},
     *  {@link PerformanceSectionName#hashCode()}
     *  and
     *  {@link PerformanceSectionName#compareTo(PerformanceSectionName)}.
     *
     *  @throws Exception   Something went awfully wrong.
     */
    @SuppressWarnings( "SimplifiableAssertion" )
    @Test
    final void testEqualsAndCompare() throws Exception
    {
        skipThreadTest();

        final var nameStrings = new String [] { "name0", "name1", "name2" };

        //---* Equals *--------------------------------------------------------
        final var candidate1 = createPerformanceSectionName( nameStrings [0] );
        final var candidate2 = createPerformanceSectionName( nameStrings [0] );

        assertNotNull( candidate1 );
        assertNotNull( candidate2 );

        assertTrue( candidate1.equals( candidate2 ) );
        assertTrue( candidate2.equals( candidate1 ) );

        assertThrows( NullArgumentException.class, () -> candidate1.compareTo( null ) );

        assertEquals( 0, candidate1.compareTo( candidate2 ) );
        assertEquals( 0, candidate2.compareTo( candidate1 ) );

        assertEquals( candidate1.hashCode(), candidate2.hashCode() );

        //---* Not equals *----------------------------------------------------
        final var candidateL = createPerformanceSectionName( nameStrings [0] );
        final var candidateM = createPerformanceSectionName( nameStrings [1] );
        final var candidateG = createPerformanceSectionName( nameStrings [2] );

        assertNotNull( candidateL );
        assertNotNull( candidateM );
        assertNotNull( candidateG );

        assertFalse( candidateL.equals( candidateM ) );
        assertFalse( candidateL.equals( candidateG ) );
        assertFalse( candidateM.equals( candidateL ) );
        assertFalse( candidateM.equals( candidateG ) );
        assertFalse( candidateG.equals( candidateL ) );
        assertFalse( candidateG.equals( candidateM ) );

        assertEquals( -1, candidateL.compareTo( candidateM ) );
        assertEquals( -1, candidateL.compareTo( candidateG ) );
        assertEquals( 1, candidateM.compareTo( candidateL ) );
        assertEquals( -1, candidateM.compareTo( candidateG ) );
        assertEquals( 1, candidateG.compareTo( candidateL ) );
        assertEquals( 1, candidateG.compareTo( candidateM ) );
    }   //  testEqualsAndCompare()

    /**
     *  Test the
     *  {@link org.tquadrat.foundation.lang.StringConverter}.
     *
     *  @see PerformanceSectionName#getStringConverter()
     *
     *  @throws Exception   Something went awfully wrong.
     */
    @Test
    final void testStringConverter() throws Exception
    {
        skipThreadTest();

        final var nameString = "PerformanceSectionName";
        final var name = createPerformanceSectionName( nameString );

        final var candidate = getStringConverter();
        assertNotNull( candidate );
        assertInstanceOf( StringConverter.class, candidate );

        assertEquals( nameString, candidate.toString( name ) );
        assertEquals( name, candidate.fromString( nameString ) );
    }   //  testStringConverter()
}
//  class TestPerformanceSectionName

/*
 *  End of File
 */