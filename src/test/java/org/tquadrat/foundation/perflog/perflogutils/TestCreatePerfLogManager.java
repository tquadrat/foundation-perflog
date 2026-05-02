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

import static java.lang.Boolean.getBoolean;
import static java.lang.management.ManagementFactory.getPlatformMBeanServer;
import static javax.management.MBeanServerFactory.createMBeanServer;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.tquadrat.foundation.perflog.PerfLogUtils.DOMAIN_NAME;
import static org.tquadrat.foundation.perflog.PerfLogUtils.SYSTEM_PROPERTY_UsedDedicatedMBeanServer;
import static org.tquadrat.foundation.perflog.PerfLogUtils.createPerfLogManager;
import static org.tquadrat.foundation.perflog.PerfLogUtils.getPerfLogMBeanObjectName;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.tquadrat.foundation.annotation.ClassVersion;
import org.tquadrat.foundation.perflog.PerfLogUtils;
import org.tquadrat.foundation.perflog.internal.PerfLogManagerImpl;
import org.tquadrat.foundation.testutil.TestBaseClass;

/**
 *  Test for the method
 *  {@link PerfLogUtils#createPerfLogManager()}
 *
 *  @extauthor Thomas Thrien - thomas.thrien@tquadrat.org
 *  @version $Id: TestCreatePerfLogManager.java 1211 2026-05-01 15:24:10Z tquadrat $
 */
@ClassVersion( sourceVersion = "$Id: TestCreatePerfLogManager.java 1211 2026-05-01 15:24:10Z tquadrat $" )
@DisplayName( "org.tquadrat.foundation.perflog.perflogutils.TestCreatePerfLogManager" )
public class TestCreatePerfLogManager extends TestBaseClass
{
        /*---------*\
    ====** Methods **==========================================================
        \*---------*/
    /**
     *  Unregisters the MBean after each test.
     */
    @AfterEach
    final void afterEach() throws InstanceNotFoundException, MBeanRegistrationException
    {
        final MBeanServer mbeanServer;
        if( getBoolean( SYSTEM_PROPERTY_UsedDedicatedMBeanServer ) )
        {
            mbeanServer = MBeanServerFactory.findMBeanServer( null )
                .stream()
                .filter( mbs -> mbs.getDefaultDomain().equals( DOMAIN_NAME ) )
                .findFirst()
                .orElseGet( () -> createMBeanServer( DOMAIN_NAME ) );
        }
        else
        {
            mbeanServer = getPlatformMBeanServer();
        }
        final var objectName = getPerfLogMBeanObjectName();
        if( mbeanServer.isRegistered( objectName ) )
        {
            mbeanServer.unregisterMBean( objectName );
        }
    }   //  afterEach()

    /**
     *  Some tests for the method
     *  {@link PerfLogUtils#createPerfLogManager()}
     *
     *  @throws Exception   Something went awfully wrong.
     */
    @Test
    final void testCreatePerLogManager() throws Exception
    {
        skipThreadTest();

        final var candidate = createPerfLogManager();

        assertNotNull( candidate );
        assertInstanceOf( PerfLogManagerImpl.class, candidate );
    }   //  testCreatePerLogManager()
}
//  class TestCreatePerfLogManager

/*
 *  End of File
 */