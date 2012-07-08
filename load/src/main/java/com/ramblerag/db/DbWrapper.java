package com.ramblerag.db;

import org.apache.log4j.Logger;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.tooling.GlobalGraphOperations;

import com.ramblerag.domain.Domain;
import com.ramblerag.domain.Lnk;
import com.ramblerag.domain.Nod;

public class DbWrapper {

	private static final String PROP_NODE_ID = "prop_node_id";
	private static final String PROP_RAILROAD = "prop_railroad";
	private static final String PROP_LONGITUDE = "prop_longitude";
	private static final String PROP_LATITUDE = "prop_latitude";
	private static final double SCALE_1E_6 = 1e-6;
	private static Logger log = Logger.getLogger(DbWrapper.class);
	private static final String DB_PATH = "var/graphDb";
	private static final String INDEX_NAME = "nodes";
	private GraphDatabaseService graphDb;
	Node nodsReferenceNode;

	private static enum RelTypes implements RelationshipType {
		DOMAIN_NODE, DOMAIN_LINK, NODS_REFERENCE
	}

	// Index of all nodes
	private Index<Node> nodeIndex;

	// Holds a ref to every node
	// private Node referenceNode;

	private static DbWrapper instance;

	public static DbWrapper getInstance() {
		if (instance == null) {
			instance = new DbWrapper();
		}
		return instance;
	}

	private DbWrapper() {
	}

	public void removeAll() throws ApplicationException {

		log.info("Removing all nodes and references.");

		Transaction tx = graphDb.beginTx();
		try {
			GlobalGraphOperations ops = GlobalGraphOperations.at(graphDb);

			for (Relationship relationship : ops.getAllRelationships()) {
				relationship.delete();
			}
			for (Node node : ops.getAllNodes()) {
				node.delete();
			}
			// referenceNode = null;

			// Index has refs to non-existent nodes. Just renew the index.
//			nodeIndex = graphDb.index().forNodes(INDEX_NAME);
			// nodeIndex.delete();

			tx.success();
			log.info("Deleted all relationships and nodes");

		} catch (Exception e) {
			log.error(e.toString());
			tx.failure();
			throw new ApplicationException(e);
		} finally {
			tx.finish();
		}
	}

	private static void registerShutdownHook(final GraphDatabaseService graphDb) {
		// Registers a shutdown hook for the Neo4j instance so that it
		// shuts down nicely when the VM exits (even if you "Ctrl-C" the
		// running example before it's completed)
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				graphDb.shutdown();
			}
		});
	}

	public GraphDatabaseService startDb() throws ApplicationException {
		graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(DB_PATH);
		nodeIndex = graphDb.index().forNodes(INDEX_NAME);
		registerShutdownHook(graphDb);
		
	//	initRefs();
		return graphDb;
	}

	public void initRefs() throws ApplicationException {
		Transaction tx = graphDb.beginTx();
		try {
			// Create railroad Nods sub reference node
			nodsReferenceNode = graphDb.createNode();
			
			//Node rn = graphDb.getReferenceNode();
			nodsReferenceNode.createRelationshipTo(nodsReferenceNode,
					RelTypes.NODS_REFERENCE);
		} catch (Exception e) {
			log.error(e.toString());
			tx.failure();
			throw new ApplicationException(e);
		} finally {
			tx.finish();
		}
		
	}

	public void shutdownDb() {
		graphDb.shutdown();
	}

	public void insert(Domain obj) throws ApplicationException {
		if (obj instanceof Nod) {
			createAndIndexNode((Nod) obj);
		} else if (obj instanceof Lnk) {
			createLink((Lnk) obj);
		}
	}

	private Node createAndIndexNode(final Nod domainNode)
			throws ApplicationException {
		Transaction tx = graphDb.beginTx();
		Node node = null;
		long nodID = -1;
		try {
			node = graphDb.createNode();

			nodID = Long.parseLong(domainNode.getNodeId().trim());
			double lat = Double.parseDouble(domainNode.getLatitude().trim())
					* SCALE_1E_6;
			double lon = Double.parseDouble(domainNode.getLongitude().trim())
					* SCALE_1E_6;
			String railroad = domainNode.getDescription().trim();

			node.setProperty(PROP_NODE_ID, nodID);
			node.setProperty(PROP_LATITUDE, lat);
			node.setProperty(PROP_LONGITUDE, lon);
			node.setProperty(PROP_RAILROAD, railroad);

			nodeIndex.add(node, PROP_NODE_ID, nodID);
			
//			if (null == nodsReferenceNode){
//				nodsReferenceNode = graphDb.createNode(); // temp
//			}
//			nodsReferenceNode.createRelationshipTo(node, RelTypes.DOMAIN_NODE );
			
			tx.success();
			
		} catch (Exception e) {
			log.error(e.toString());
			tx.failure();
			throw new ApplicationException(e);
		} finally {
			tx.finish();
		}


		// Debug assertion
//		IndexHits<Node> hits = nodeIndex.get(PROP_NODE_ID, nodID); // graphDb.getNodeById(id);
//		Node node2 = hits.getSingle();
//		// Node node2 = nodeIndex.get(PROP_NODE_ID, id).getSingle();
//		if (null == node2 || !node2.equals(node)) {
//			throw new ApplicationException(String.format(
//					"Node %s null or not equal to node %s", node, node2));
//		}
		return node;
	}

	private void createLink(final Lnk domainLink) throws ApplicationException {
		Transaction tx = graphDb.beginTx();
		Node nodeA = null;
		Node nodeB = null;
		try {
			if (!graphDb.index().existsForNodes(INDEX_NAME)){
				//nodeIndex = graphDb.index().forNodes(INDEX_NAME);
				throw new ApplicationException("Nodes index missing");
			}
			
			long keyValueA = Long.parseLong(domainLink.getaNode().trim());
			long keyValueB = Long.parseLong(domainLink.getbNode().trim());

			nodeA = nodeIndex.get(PROP_NODE_ID, keyValueA).getSingle();
			nodeB = nodeIndex.get(PROP_NODE_ID, keyValueB).getSingle();

			if (null == nodeA || null == nodeB) {
				throw new ApplicationException(
						String.format(
								"Link referenced %s A-Node with key %d, and the link referenced %s B-Node with key %d",
								nodeA, keyValueA, nodeB, keyValueB));
			}
			
			log.info(String.format(
					"Link inserted from  %s A-Node with key %d, to the link referenced %s B-Node with key %d",
					nodeA, keyValueA, nodeB, keyValueB));


			nodeA.createRelationshipTo(nodeB, RelTypes.DOMAIN_LINK);

			tx.success();
			
		} catch (Exception e) {
			log.error(e.toString());
			tx.failure();
			throw new ApplicationException(e);
		} finally {
			tx.finish();
		}
	}

	// private Node getReferenceNode() {
	// if (null == referenceNode) {
	// Transaction tx = graphDb.beginTx();
	// try {
	// referenceNode = graphDb.getReferenceNode();
	// } catch (Exception e) {
	// logger.error(e.toString());
	// tx.failure();
	// } finally {
	// tx.finish();
	// }
	// }
	// return referenceNode;
	// }
}
