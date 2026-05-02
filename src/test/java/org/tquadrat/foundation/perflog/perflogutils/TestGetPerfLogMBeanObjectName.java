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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.tquadrat.foundation.perflog.PerfLogUtils.DOMAIN_NAME;
import static org.tquadrat.foundation.perflog.PerfLogUtils.MBEAN_TYPE;
import static org.tquadrat.foundation.perflog.PerfLogUtils.getPerfLogMBeanObjectName;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.tquadrat.foundation.annotation.ClassVersion;
import org.tquadrat.foundation.perflog.PerfLogUtils;
import org.tquadrat.foundation.testutil.TestBaseClass;

/**
 *  Some tests for the method
 *  {@link PerfLogUtils#getPerfLogMBeanObjectName()}.
 *
 *  @extauthor Thomas Thrien - thomas.thrien@tquadrat.org
 *  @version $Id: TestGetPerfLogMBeanObjectName.java 1211 2026-05-01 15:24:10Z tquadrat $
 */
@ClassVersion( sourceVersion = "$Id: TestGetPerfLogMBeanObjectName.java 1211 2026-05-01 15:24:10Z tquadrat $" )
@DisplayName( "org.tquadrat.foundation.perflog.perflogutils.TestGetPerfLogMBeanObjectName" )
public class TestGetPerfLogMBeanObjectName extends TestBaseClass
{
        /*---------*\
    ====** Methods **==========================================================
        \*---------*/
    /**
     *  Some tests for the method
     *  {@link PerfLogUtils#getPerfLogMBeanObjectName()}.
     *
     *  @throws Exception   Something went awfully wrong.
     */
    @Test
    final void testGetPerfLogMBeanObjectName() throws Exception
    {
        skipThreadTest();

        final var candidate = getPerfLogMBeanObjectName();
        assertNotNull( candidate );
        assertEquals( DOMAIN_NAME, candidate.getDomain() );
        assertFalse( candidate.isDomainPattern() );
        assertFalse( candidate.isPattern() );
        assertFalse( candidate.isPropertyPattern() );
        assertFalse( candidate.isPropertyListPattern() );
        assertFalse( candidate.isPropertyValuePattern() );
        assertEquals( MBEAN_TYPE, candidate.getKeyProperty( "type" ) );
    }   //  testGetPerfLogMBeanObjectName()
}
//  class TestGetPerfLogMBeanObjectName

/*
 *  End of File
 */