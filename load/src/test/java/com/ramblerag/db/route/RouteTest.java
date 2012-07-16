package com.ramblerag.db.route;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.index.IndexManager;
import org.powermock.api.easymock.PowerMock;
import org.powermock.modules.junit4.PowerMockRunner;

import com.ramblerag.db.core.DbWrapper;
import com.ramblerag.domain.DomainConstants;

@RunWith(PowerMockRunner.class)
public class RouteTest {

	private Router target;

	@Before
	public void setUp() throws Exception {
		target = new Router();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testRoute() throws Exception {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(bos);

		DbWrapper dbWrapper = PowerMock.createNiceMock(DbWrapper.class);
		target.setDbWrapper(dbWrapper);

		GraphDatabaseService graphDb = PowerMock.createNiceMock(GraphDatabaseService.class);
		expect(target.getDbWrapper().startDb()).andReturn(graphDb);
		
		//Index<Node> nodeIndex = graphDb.index().forNodes(DomainConstants.INDEX_NAME);
		
		IndexManager indexManager = PowerMock.createNiceMock(IndexManager.class);
		expect(graphDb.index()).andReturn(indexManager).times(10);
		
		Index<Node> nodeIndex = PowerMock.createNiceMock(Index.class);
		expect(indexManager.forNodes(DomainConstants.INDEX_NAME)).andReturn(nodeIndex).times(10);
		
		IndexHits<Node> hits = PowerMock.createNiceMock(IndexHits.class);
		
		long keyValueA = 1;
		long keyValueB = 2;
		expect(nodeIndex.get(DomainConstants.PROP_NODE_ID, keyValueA)).andReturn(hits).times(10);
		expect(nodeIndex.get(DomainConstants.PROP_NODE_ID, keyValueB)).andReturn(hits).times(10);

		Node nodeA = PowerMock.createNiceMock(Node.class);
		Node nodeB = PowerMock.createNiceMock(Node.class);

		expect(hits.getSingle()).andReturn(nodeA).times(10);
		expect(hits.getSingle()).andReturn(nodeB).times(10);
		
		Transaction tx = PowerMock.createNiceMock(Transaction.class);
		expect(graphDb.beginTx()).andReturn(tx);

		PowerMock.replayAll();

		target.getShortestRoute(ps, keyValueA, keyValueB);
		
		byte[] byteArray = bos.toByteArray();
		String route = new String(byteArray);
		assertTrue(route.length() > 0);
		System.out.println(route);
	}

}
