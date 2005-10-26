/*
 * ################################################################
 *
 * ProActive: The Java(TM) library for Parallel, Distributed,
 *            Concurrent computing with Security and Mobility
 *
 * Copyright (C) 1997-2002 INRIA/University of Nice-Sophia Antipolis
 * Contact: proactive-support@inria.fr
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
 * USA
 *
 *  Initial developer(s):               The ProActive Team
 *                        http://www.inria.fr/oasis/ProActive/contacts.html
 *  Contributor(s):
 *
 * ################################################################
 */
package org.objectweb.proactive.core.process.rsh;

import org.objectweb.proactive.core.util.RemoteProcessMessageLogger;


/**
 * <p>
 * This class has the same functionalities than RSHJVMProcess, except that the class associated with the target JVMProcess
 * ie the class that the target process will start is set automatically to <code>org.objectweb.proactive.core.runtime.startRuntime</code>.
 * </p>
 * @author  ProActive Team
 * @version 1.0,  2002/09/20
 * @since   ProActive 0.9.4
 */
public class RSHNodeProcess extends RSHJVMProcess {

    /**
     * Creates a new instance of RSHNodeProcess.
     */
    public RSHNodeProcess() {
        this(new StandardOutputMessageLogger());
        //setClassname("org.objectweb.proactive.core.runtime.StartRuntime");
    }

    /**
     * Creates a new instance of RSHNodeProcess.
     * @param messageLogger The logger that handles input and error stream of the target JVMProcess
     */
    public RSHNodeProcess(RemoteProcessMessageLogger messageLogger) {
        super(messageLogger);
        setClassname("org.objectweb.proactive.core.runtime.StartRuntime");
    }

    /**
     * Creates a new instance of SSHNodeProcess.
     * @param inputMessageLogger The logger that handles input stream of the target JVMProcess
     * @param errorMessageLogger The logger that handles error stream of the target JVMProcess
     */
    public RSHNodeProcess(RemoteProcessMessageLogger inputMessageLogger,
        RemoteProcessMessageLogger errorMessageLogger) {
        super(inputMessageLogger, errorMessageLogger);
        setClassname("org.objectweb.proactive.core.runtime.StartRuntime");
    }
}
