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

package org.tquadrat.foundation.perflog.perflogutils;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.tquadrat.foundation.lang.CommonConstants.EMPTY_STRING;
import static org.tquadrat.foundation.perflog.PerfLogUtils.createPerformanceSectionName;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.tquadrat.foundation.annotation.ClassVersion;
import org.tquadrat.foundation.exception.BlankArgumentException;
import org.tquadrat.foundation.exception.EmptyArgumentException;
import org.tquadrat.foundation.exception.NullArgumentException;
import org.tquadrat.foundation.testutil.TestBaseClass;

/**
 *  Some tests for the method
 *  {@link org.tquadrat.foundation.perflog.PerfLogUtils#createPerformanceSectionName(String)}.
 *
 *  @extauthor Thomas Thrien - thomas.thrien@tquadrat.org
 *  @version $Id: TestCreatePerformanceSectionName.java 1211 2026-05-01 15:24:10Z tquadrat $
 */
@ClassVersion( sourceVersion = "$Id: TestCreatePerformanceSectionName.java 1211 2026-05-01 15:24:10Z tquadrat $" )
@DisplayName( "org.tquadrat.foundation.perflog.perflogutils.TestCreatePerformanceSectionName" )
public class TestCreatePerformanceSectionName extends TestBaseClass
{
        /*---------*\
    ====** Methods **==========================================================
        \*---------*/
    /**
     *  Some tests for the method
     *  {@link org.tquadrat.foundation.perflog.PerfLogUtils#createPerformanceSectionName(String)}.
     *
     *  @throws Exception   Something went awfully wrong.
     */
    @Test
    final void testCreatePerformanceSectionName() throws Exception
    {
        skipThreadTest();

        assertThrows( NullArgumentException.class, () -> createPerformanceSectionName( null ) );
        assertThrows( EmptyArgumentException.class, () -> createPerformanceSectionName( EMPTY_STRING ) );
        assertThrows( BlankArgumentException.class, () -> createPerformanceSectionName( " " ) );
    }   //  testCreatePerformanceSectionName()
}
//  class TestCreatePerformanceSectionName

/*
 *  End of File
 */