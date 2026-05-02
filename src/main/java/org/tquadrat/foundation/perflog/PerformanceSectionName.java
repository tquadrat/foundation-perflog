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

import org.apiguardian.api.API;
import org.tquadrat.foundation.annotation.ClassVersion;
import org.tquadrat.foundation.lang.StringConverter;
import org.tquadrat.foundation.perflog.internal.PerformanceSectionNameImpl;

/**
 *  <p>{@summary This interface describes the name for a a &quot;Performance
 *  Section&quot;.}</p>
 *
 *  @extauthor Thomas Thrien - thomas.thrien@tquadrat.org
 *  @version $Id: PerformanceSectionName.java 1211 2026-05-01 15:24:10Z tquadrat $
 *  @since 0.25.0
 *
 *  @UMLGraph.link
 */
@ClassVersion( sourceVersion = "$Id: PerformanceSectionName.java 1211 2026-05-01 15:24:10Z tquadrat $" )
@API( status = STABLE, since = "0.25.0" )
public sealed interface PerformanceSectionName extends Comparable<PerformanceSectionName>
    permits PerformanceSectionNameImpl
{
        /*---------*\
    ====** Methods **==========================================================
        \*---------*/
    /**
     *  {@inheritDoc}
     */
    @Override
    public int compareTo( final PerformanceSectionName o );

    /**
     *  {@inheritDoc}
     */
    @Override
    public boolean equals( final Object o );

    /**
     *  Returns the
     *  {@link org.tquadrat.foundation.lang.StringConverter}
     *  for instances of this class.
     *
     *  @param  <T> The type that is handled by the returned
     *      {@link StringConverter}
     *      instance.
     *  @return The {@code StringConverter}.
     */
    public static <T extends PerformanceSectionName> StringConverter<T> getStringConverter()
    {
        return PerformanceSectionNameImpl.getStringConverter();
    }  //  getStringConverter()

    /**
     *  {@inheritDoc}
     */
    @Override
    public int hashCode();

    /**
     *  {@inheritDoc}
     */
    @Override
    public String toString();
}
//  interface PerformanceSectionName

/*
 *  End of File
 */