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

import static java.lang.Integer.signum;
import static org.apiguardian.api.API.Status.INTERNAL;
import static org.tquadrat.foundation.lang.Objects.requireNonNullArgument;
import static org.tquadrat.foundation.lang.Objects.requireNotBlankArgument;

import org.apiguardian.api.API;
import org.tquadrat.foundation.annotation.ClassVersion;
import org.tquadrat.foundation.lang.GenericStringConverter;
import org.tquadrat.foundation.lang.StringConverter;
import org.tquadrat.foundation.perflog.PerformanceSectionName;

/**
 *  <p>{@summary The implementation of
 *  {@link PerformanceSectionName}.}</p>
 *
 *  @extauthor Thomas Thrien - thomas.thrien@tquadrat.org
 *  @version $Id: PerformanceSectionNameImpl.java 1211 2026-05-01 15:24:10Z tquadrat $
 *  @since 0.25.0
 *
 *  @UMLGraph.link
 */
@ClassVersion( sourceVersion = "$Id: PerformanceSectionNameImpl.java 1211 2026-05-01 15:24:10Z tquadrat $" )
@API( status = INTERNAL, since = "0.25.0" )
public final class PerformanceSectionNameImpl implements PerformanceSectionName
{
        /*------------*\
    ====** Attributes **=======================================================
        \*------------*/
    /**
     *  The internal value for the performance section name.
     */
    private final String m_Value;

        /*------------------------*\
    ====** Static Initialisations **===========================================
        \*------------------------*/
    /**
     *  The
     *  {@link StringConverter}
     *  for this class.
     */
    private static final StringConverter<PerformanceSectionNameImpl> STRING_CONVERTER;

    static
    {
        STRING_CONVERTER = new GenericStringConverter<>( PerformanceSectionNameImpl::new );
    }

        /*--------------*\
    ====** Constructors **=====================================================
        \*--------------*/
    /**
     *  Creates a new instance of {@code PerformanceSectionNameImpl}.
     *
     *  @param  value   The value for the name.
     */
    public PerformanceSectionNameImpl( final String value )
    {
        m_Value = requireNotBlankArgument( value, "value" );
    }   //  PerformanceSectionNameImpl()

    /**
     *  <p>{@summary Creates a new instance of
     *  {@code PerformanceSectionNameImpl}.}</p>
     *  <p>This constructor was introduced solely to simplify the
     *  implementation for the
     *  {@linkplain #STRING_CONVERTER string converter}
     *  provided by this class. Internally, it calls
     *  {@link #PerformanceSectionNameImpl(String)}.</p>
     *
     *  @param  value   The value for the name.
     *
     *  @see #getStringConverter()
     *  @see GenericStringConverter
     */
    private PerformanceSectionNameImpl( final CharSequence value )
    {
        this( requireNonNullArgument( value, "value" ).toString() );
    }   //  PerformanceSectionNameImpl()

        /*---------*\
    ====** Methods **==========================================================
        \*---------*/
    /**
     *  {@inheritDoc}
     */
    @Override
    public final int compareTo( final PerformanceSectionName o )
    {
        final var retValue = signum( toString().compareTo( requireNonNullArgument( o, "o" ).toString() ) );

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  compareTo()

    /**
     *  {@inheritDoc}
     */
    @Override
    public final boolean equals( final Object o )
    {
        var retValue = this == o;
        if( !retValue && o instanceof final PerformanceSectionNameImpl other )
        {
            retValue = m_Value.equals( other.m_Value );
        }

        //---* Done *----------------------------------------------------------
        return retValue;
    }   //  equals()

    /**
     *  Returns the
     *  {@link StringConverter}
     *  for instances of this class.
     *
     *  @param  <T> The type that is handled by the returned
     *      {@link StringConverter}
     *      instance.
     *  @return The {@code StringConverter}.
     */
    public static <T extends PerformanceSectionName> StringConverter<T> getStringConverter()
    {
        //noinspection unchecked
        return (StringConverter<T>) STRING_CONVERTER;
    }  //  getStringConverter()

    /**
     *  {@inheritDoc}
     */
    @Override
    public final int hashCode() { return m_Value.hashCode(); }

    /**
     *  {@inheritDoc}
     */
    @Override
    public final String toString() { return m_Value; }
}
//  class PerformanceSectionNameImpl

/*
 *  End of File
 */