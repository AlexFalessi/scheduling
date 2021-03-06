/*
 * ProActive Parallel Suite(TM):
 * The Open Source library for parallel and distributed
 * Workflows & Scheduling, Orchestration, Cloud Automation
 * and Big Data Analysis on Enterprise Grids & Clouds.
 *
 * Copyright (c) 2007 - 2017 ActiveEon
 * Contact: contact@activeeon.com
 *
 * This library is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation: version 3 of
 * the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * If needed, contact us to obtain a release under GPL Version 2 or 3
 * or a different license than the AGPL.
 */
package org.ow2.proactive.resourcemanager.nodesource.infrastructure;

import static com.google.common.truth.Truth.assertThat;
import static functionaltests.nodesrecovery.RecoverInfrastructureTestHelper.NODES_RECOVERABLE;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import java.io.Serializable;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.objectweb.proactive.core.node.Node;
import org.ow2.proactive.resourcemanager.authentication.Client;
import org.ow2.proactive.resourcemanager.db.NodeSourceData;
import org.ow2.proactive.resourcemanager.db.RMDBManager;
import org.ow2.proactive.resourcemanager.exception.RMException;
import org.ow2.proactive.resourcemanager.nodesource.NodeSource;
import org.ow2.proactive.resourcemanager.nodesource.NodeSourceDescriptor;
import org.ow2.proactive.resourcemanager.nodesource.NodeSourceStatus;
import org.ow2.proactive.resourcemanager.nodesource.common.Configurable;
import org.ow2.proactive.resourcemanager.nodesource.policy.StaticPolicy;
import org.ow2.proactive.resourcemanager.rmnode.RMDeployingNode;


/**
 * @author ActiveEon Team
 * @since 06/02/17
 */
public class InfrastructureManagerTest {

    private static final int INITIAL_DYNAMIC_PARAMETER_VALUE = 2;

    private static final int UPDATED_DYNAMIC_PARAMETER_VALUE = 5;

    private TestingInfrastructureManager infrastructureManager;

    @Mock
    private NodeSource nodeSource;

    @Mock
    private RMDBManager dbManager;

    @Mock
    private NodeSourceData nodeSourceData;

    @Mock
    private InfrastructureManager infrastructureManagerMock;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        infrastructureManager = new TestingInfrastructureManager();
        infrastructureManager.internalConfigure(new Integer(INITIAL_DYNAMIC_PARAMETER_VALUE));
        infrastructureManager.setRmDbManager(dbManager);
        infrastructureManager.setNodeSource(nodeSource);
        when(nodeSource.getName()).thenReturn("NodeSource#1");
        when(nodeSource.nodesRecoverable()).thenReturn(NODES_RECOVERABLE);
        when(dbManager.getNodeSource(anyString())).thenReturn(nodeSourceData);
    }

    @Test
    public void testGetDeployingNodeUnknownNode() {
        RMDeployingNode rmNode = new RMDeployingNode("deploying", nodeSource, "command", null);

        assertThat(infrastructureManager.getDeployingNodesWithLock()).hasSize(0);
        assertThat(infrastructureManager.getPersistedDeployingNodesUrl()).hasSize(0);
        assertThat(infrastructureManager.getLostNodesWithLock()).hasSize(0);
        assertThat(infrastructureManager.getPersistedLostNodesUrl()).hasSize(0);

        RMDeployingNode rmNodeFound = infrastructureManager.getDeployingOrLostNode(rmNode.getNodeURL());

        assertThat(rmNodeFound).isNull();
    }

    @Test
    public void testGetDeployingNodeDeployingStateKnow() {
        RMDeployingNode rmNode = new RMDeployingNode("deploying", nodeSource, "command", null);
        infrastructureManager.addDeployingNodeWithLockAndPersist(rmNode.getNodeURL(), rmNode);

        assertThat(infrastructureManager.getDeployingNodesWithLock()).hasSize(1);
        assertThat(infrastructureManager.getPersistedDeployingNodesUrl()).hasSize(1);
        assertThat(infrastructureManager.getLostNodesWithLock()).hasSize(0);
        assertThat(infrastructureManager.getPersistedLostNodesUrl()).hasSize(0);

        RMDeployingNode rmNodeFound = infrastructureManager.getDeployingOrLostNode(rmNode.getNodeURL());

        assertThat(rmNodeFound).isSameAs(rmNode);
    }

    @Test
    public void testGetDeployingNodeLostStateKnow() {
        RMDeployingNode deployingNode = new RMDeployingNode("deploying", nodeSource, "command", null);
        infrastructureManager.addDeployingNodeWithLockAndPersist(deployingNode.getNodeURL(), deployingNode);
        RMDeployingNode lostNode = new RMDeployingNode("lost", nodeSource, "command", null);
        lostNode.setLost();
        infrastructureManager.addLostNodeWithLockAndPersist(lostNode.getNodeURL(), lostNode);

        assertThat(infrastructureManager.getDeployingNodesWithLock()).hasSize(1);
        assertThat(infrastructureManager.getPersistedDeployingNodesUrl()).hasSize(1);
        assertThat(infrastructureManager.getLostNodesWithLock()).hasSize(1);
        assertThat(infrastructureManager.getPersistedLostNodesUrl()).hasSize(1);

        RMDeployingNode rmNodeFound = infrastructureManager.getDeployingOrLostNode(lostNode.getNodeURL());
        assertThat(rmNodeFound).isSameAs(lostNode);
        assertThat(rmNodeFound).isNotSameAs(deployingNode);

        rmNodeFound = infrastructureManager.getDeployingOrLostNode(deployingNode.getNodeURL());
        assertThat(rmNodeFound).isSameAs(deployingNode);
        assertThat(rmNodeFound).isNotSameAs(lostNode);
    }

    @Test
    public void testGetDeployingNodeConflictingUrls() {
        RMDeployingNode deployingNode = new RMDeployingNode("deploying", nodeSource, "command", null);
        infrastructureManager.addDeployingNodeWithLockAndPersist(deployingNode.getNodeURL(), deployingNode);
        RMDeployingNode lostNode = new RMDeployingNode("deploying", nodeSource, "command", null);
        lostNode.setLost();
        infrastructureManager.addLostNodeWithLockAndPersist(lostNode.getNodeURL(), lostNode);

        assertThat(infrastructureManager.getDeployingNodesWithLock()).hasSize(1);
        assertThat(infrastructureManager.getPersistedDeployingNodesUrl()).hasSize(1);
        assertThat(infrastructureManager.getLostNodesWithLock()).hasSize(1);
        assertThat(infrastructureManager.getPersistedLostNodesUrl()).hasSize(1);

        // deploying nodes have priority over lost nodes
        RMDeployingNode rmNodeFound = infrastructureManager.getDeployingOrLostNode(lostNode.getNodeURL());
        assertThat(rmNodeFound).isSameAs(deployingNode);
        assertThat(rmNodeFound).isNotSameAs(lostNode);
    }

    @Test
    public void testUpdateUnknownNode() {
        RMDeployingNode rmNode = new RMDeployingNode("deploying", nodeSource, "command", null);

        assertThat(infrastructureManager.getDeployingNodesWithLock()).hasSize(0);
        assertThat(infrastructureManager.getPersistedDeployingNodesUrl()).hasSize(0);
        assertThat(infrastructureManager.getLostNodesWithLock()).hasSize(0);
        assertThat(infrastructureManager.getPersistedLostNodesUrl()).hasSize(0);

        RMDeployingNode oldRmNode = infrastructureManager.update(rmNode);

        assertThat(infrastructureManager.getDeployingNodesWithLock()).hasSize(0);
        assertThat(infrastructureManager.getPersistedDeployingNodesUrl()).hasSize(0);
        assertThat(infrastructureManager.getLostNodesWithLock()).hasSize(0);
        assertThat(infrastructureManager.getPersistedLostNodesUrl()).hasSize(0);
        assertThat(oldRmNode).isNull();
    }

    @Test
    public void testUpdateDeployingNodeKnown() {
        RMDeployingNode rmNode = new RMDeployingNode("deploying", nodeSource, "command", null);
        infrastructureManager.addDeployingNodeWithLockAndPersist(rmNode.getNodeURL(), rmNode);

        assertThat(infrastructureManager.getDeployingNodesWithLock()).hasSize(1);
        assertThat(infrastructureManager.getPersistedDeployingNodesUrl()).hasSize(1);
        assertThat(infrastructureManager.getLostNodesWithLock()).hasSize(0);
        assertThat(infrastructureManager.getPersistedLostNodesUrl()).hasSize(0);

        RMDeployingNode rmNode2 = new RMDeployingNode("deploying", nodeSource, "command2", null);

        RMDeployingNode oldRmNode = infrastructureManager.update(rmNode2);

        assertThat(oldRmNode).isSameAs(rmNode);
        assertThat(infrastructureManager.getDeployingNodesWithLock()).hasSize(1);
        assertThat(infrastructureManager.getPersistedDeployingNodesUrl()).hasSize(1);
        assertThat(infrastructureManager.getLostNodesWithLock()).hasSize(0);
        assertThat(infrastructureManager.getPersistedLostNodesUrl()).hasSize(0);

        assertThat(infrastructureManager.getDeployingNodesWithLock()).contains(rmNode2);
    }

    @Test
    public void testUpdateLostNodeKnown() {
        RMDeployingNode deployingNode = new RMDeployingNode("deploying", nodeSource, "command", null);
        infrastructureManager.addDeployingNodeWithLockAndPersist(deployingNode.getNodeURL(), deployingNode);
        RMDeployingNode lostNode = new RMDeployingNode("lost", nodeSource, "command", null);
        lostNode.setLost();
        infrastructureManager.addLostNodeWithLockAndPersist(lostNode.getNodeURL(), lostNode);

        assertThat(infrastructureManager.getDeployingNodesWithLock()).hasSize(1);
        assertThat(infrastructureManager.getPersistedDeployingNodesUrl()).hasSize(1);
        assertThat(infrastructureManager.getLostNodesWithLock()).hasSize(1);
        assertThat(infrastructureManager.getPersistedLostNodesUrl()).hasSize(1);

        RMDeployingNode lostNode2 = new RMDeployingNode("lost", nodeSource, "command2", null);
        lostNode2.setLost();

        RMDeployingNode oldRmNode = infrastructureManager.update(lostNode2);

        assertThat(oldRmNode).isSameAs(lostNode);
        assertThat(infrastructureManager.getDeployingNodesWithLock()).hasSize(1);
        assertThat(infrastructureManager.getPersistedDeployingNodesUrl()).hasSize(1);
        assertThat(infrastructureManager.getLostNodesWithLock()).hasSize(1);
        assertThat(infrastructureManager.getPersistedLostNodesUrl()).hasSize(1);

        assertThat(infrastructureManager.getLostNodesWithLock()).contains(lostNode2);
        assertThat(infrastructureManager.getPersistedLostNodesUrl()).contains(lostNode.getNodeURL());
        assertThat(infrastructureManager.getPersistedLostNodesUrl()).contains(lostNode2.getNodeURL());
    }

    @Test
    public void testUpdateLostNodeKnownConflictingUrls() {
        RMDeployingNode deployingNode = new RMDeployingNode("deploying", nodeSource, "command", null);
        infrastructureManager.addDeployingNodeWithLockAndPersist(deployingNode.getNodeURL(), deployingNode);
        RMDeployingNode lostNode = new RMDeployingNode("deploying", nodeSource, "command", null);
        lostNode.setLost();
        infrastructureManager.addLostNodeWithLockAndPersist(lostNode.getNodeURL(), lostNode);

        assertThat(infrastructureManager.getDeployingNodesWithLock()).hasSize(1);
        assertThat(infrastructureManager.getPersistedDeployingNodesUrl()).hasSize(1);
        assertThat(infrastructureManager.getLostNodesWithLock()).hasSize(1);
        assertThat(infrastructureManager.getPersistedLostNodesUrl()).hasSize(1);

        RMDeployingNode lostNode2 = new RMDeployingNode("deploying", nodeSource, "command2", null);
        lostNode2.setLost();

        RMDeployingNode oldRmNode = infrastructureManager.update(lostNode2);

        assertThat(oldRmNode).isSameAs(lostNode);
        assertThat(oldRmNode).isNotSameAs(deployingNode);
        assertThat(infrastructureManager.getDeployingNodesWithLock()).hasSize(1);
        assertThat(infrastructureManager.getPersistedDeployingNodesUrl()).hasSize(1);
        assertThat(infrastructureManager.getLostNodesWithLock()).hasSize(1);
        assertThat(infrastructureManager.getPersistedLostNodesUrl()).hasSize(1);

        assertThat(infrastructureManager.getLostNodesWithLock()).contains(lostNode2);
        assertThat(infrastructureManager.getPersistedLostNodesUrl()).contains(lostNode.getNodeURL());
        assertThat(infrastructureManager.getPersistedLostNodesUrl()).contains(lostNode2.getNodeURL());
    }

    @Test
    public void testPersistInfrastructureVariables() {
        infrastructureManager.setPersistedNodeSourceData(nodeSourceData);
        infrastructureManager.persistInfrastructureVariables();
        verify(nodeSourceData, times(1)).setInfrastructureVariables(anyMap());
        verify(dbManager, times(1)).updateNodeSource(eq(nodeSourceData));
    }

    @Test
    public void testDoNotPersistInfrastructureVariablesWhenCannotFindNodeSource() {
        when(dbManager.getNodeSource(anyString())).thenReturn(null);
        infrastructureManager.persistInfrastructureVariables();
        verify(nodeSourceData, times(0)).setInfrastructureVariables(anyMap());
        verify(dbManager, times(0)).updateNodeSource(eq(nodeSourceData));
    }

    @Test
    public void testInternalInfrastructureConfigurationDoesNotPersistAnything() {
        when(dbManager.getNodeSource(anyString())).thenReturn(null);
        infrastructureManager.internalConfigure();
        verify(nodeSource, times(0)).nodesRecoverable();
    }

    @Test
    public void testAttemptToPersistInInternalInfrastructureConfigurationIsHandled() {
        WrongTestingInfrastructureManager wrongInfrastructureManager = new WrongTestingInfrastructureManager();
        wrongInfrastructureManager.setRmDbManager(dbManager);
        when(dbManager.getNodeSource(anyString())).thenReturn(null);

        wrongInfrastructureManager.internalConfigure();
        // the invocation to the following method should still be avoided
        // because an exception should be thrown and caught instead
        verify(nodeSource, times(0)).nodesRecoverable();
    }

    @Test
    public void testInfrastructureReconfiguredModifiesDynamicParamters() {
        assertThat(infrastructureManager.dynamicNumberTest).isEqualTo(INITIAL_DYNAMIC_PARAMETER_VALUE);
        infrastructureManager.reconfigure(UPDATED_DYNAMIC_PARAMETER_VALUE);
        assertThat(infrastructureManager.dynamicNumberTest).isEqualTo(UPDATED_DYNAMIC_PARAMETER_VALUE);
    }

    public static class TestingInfrastructureManager extends InfrastructureManager {

        @Configurable(description = "dynamic field", dynamic = true)
        private int dynamicNumberTest;

        @Override
        public String getDescription() {
            return "test infrastructure";
        }

        @Override
        protected void configure(Object... parameters) {
            setDynamicNumberTest(parameters);
        }

        @Override
        public void reconfigure(Object... parameters) {
            setDynamicNumberTest(parameters);
        }

        @Override
        public void acquireNode() {

        }

        @Override
        public void acquireAllNodes() {

        }

        @Override
        public void removeNode(Node node) throws RMException {

        }

        @Override
        protected void notifyAcquiredNode(Node node) throws RMException {

        }

        @Override
        protected void initializePersistedInfraVariables() {

        }

        @Override
        public void notifyDownNode(String nodeName, String nodeUrl, Node node) throws RMException {

        }

        private void setDynamicNumberTest(Object[] parameters) {
            if (parameters.length > 0) {
                this.dynamicNumberTest = (int) parameters[0];
            }
        }

    }

    private static final class WrongTestingInfrastructureManager extends TestingInfrastructureManager {

        @Override
        public String getDescription() {
            return null;
        }

        @Override
        protected void configure(Object... parameters) {
            setPersistedInfraVariable((PersistedInfraVariablesHandler<Void>) () -> {
                persistedInfraVariables.put("testKey", "testValue");
                return null;
            });
        }

    }

}
