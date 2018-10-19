package com.ramblerag.db.core;

import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.Index;
import static org.easymock.EasyMock.*;

public class DbWrapperTest {
	
	private DbWrapper target;
	private GraphDatabaseFactory graphDatabaseFactory;
	private GraphDatabaseService graphDb;
	private Index<Node> nodeIndex;

	@Before
	public void setUp() throws Exception {
		target = new DbWrapper();
		
		graphDatabaseFactory = createNiceMock(GraphDatabaseFactory.class);
		graphDb = createNiceMock(GraphDatabaseService.class);
		nodeIndex = createNiceMock(Index.class);
		
		target.setGraphDatabaseFactory(graphDatabaseFactory);
		target.setGraphDb(graphDb);
		target.setNodeIndex(nodeIndex);
	}

	@Test
	public void testRemoveAll() {
		replay(graphDatabaseFactory );
		
	}

	@Test
	public void testStartDb() {
		replay(graphDatabaseFactory );
		
	}

	@Test
	public void testInitRefs() {
		replay(graphDatabaseFactory );
		
	}

	@Test
	public void testShutdownDb() {
		replay(graphDatabaseFactory );
		target.shutdownDb();
	}

	@Test
	public void testInsert() {
		replay(graphDatabaseFactory );
		
	}

	@Test
	public void testGetters() {
		replay(graphDatabaseFactory );

		assertNotNull(target.getGraphDatabaseFactory());
		assertNotNull(target.getGraphDb());
		assertNotNull(target.getNodeIndex());
	}


}
