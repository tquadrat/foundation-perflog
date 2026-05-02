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

/**
 *  The module for the performance logging and monitoring classes of the
 *  Foundation Library.
 *
 *  @version $Id: module-info.java 1216 2026-05-02 11:16:24Z tquadrat $
 *
 *  @todo task.list
 */
@SuppressWarnings("JavadocDeclaration")
module org.tquadrat.foundation.library.org.tquadrat.foundation.perflog
{
    requires java.management;
    requires org.apiguardian.api;
    requires org.tquadrat.foundation.util;
    requires transitive org.tquadrat.foundation.value;
    requires org.tquadrat.foundation.jsonbuilder;
    requires org.tquadrat.foundation.perflog.remote;

    //---* For common use *----------------------------------------------------
    exports org.tquadrat.foundation.perflog;
    exports org.tquadrat.foundation.perflog.client;
}

/*
 *  End of File
 */