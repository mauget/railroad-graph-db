package com.ramblerag.db.route;

import java.io.PrintStream;

import org.neo4j.graphalgo.CommonEvaluators;
import org.neo4j.graphalgo.CostEvaluator;
import org.neo4j.graphalgo.EstimateEvaluator;
import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Expander;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.kernel.Traversal;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.ramblerag.db.core.ApplicationException;
import com.ramblerag.db.core.DbWrapper;
import com.ramblerag.db.core.GlobalConstants;
import com.ramblerag.domain.DomainConstants;

/**
 * Dump route to KML, suitable for feeding to Google Earth

 * @author mauget
 *
 */
public class Router {
	
	// Injected
	private DbWrapper dbWrapper;

	// Routing helper functions
	private final EstimateEvaluator<Double> estimateEval = CommonEvaluators.geoEstimateEvaluator(
			DomainConstants.PROP_LATITUDE, DomainConstants.PROP_LONGITUDE );

    private final CostEvaluator<Double> costEval = new CostEvaluator<Double>() {

    	// Constant cost. Could be a function of lading type, location, fuel surcharge, ..
		public Double getCost(Relationship relationship, Direction direction) {
			return 1d;
		}};

	public static void main(String[] args) {

		try {
			ClassPathXmlApplicationContext appContext = new ClassPathXmlApplicationContext(
		        new String[] {GlobalConstants.APPLICATION_CONTEXT_XML});
		
			Router router = appContext.getBean(Router.class);
			
			// Dumps to System.out PrintStream, but caller can supply any PrintStream
//			router.getShortestRoute(System.out, 1, 2000);
//			router.getShortestRoute(System.out, 1000, 8000);
			router.getShortestRoute(System.out, 1, 133752);
//			router.getShortestRoute(System.out, 11000, 2400);
//			router.getShortestRoute(System.out, 8000, 2000);
//			router.getShortestRoute(System.out, 13000, 123);
		} catch (ApplicationException e) {
			e.printStackTrace();
		}
	}

	public void getShortestRoute(PrintStream ps, long keyValueA, long keyValueB)
			throws ApplicationException {
		
		GraphDatabaseService graphDb = getDbWrapper().startDb();

		Index<Node> nodeIndex = graphDb.index().forNodes(DomainConstants.INDEX_NAME);

		Node nodeA = nodeIndex.get(DomainConstants.PROP_NODE_ID, keyValueA)
				.getSingle();
		
		Node nodeB = nodeIndex.get(DomainConstants.PROP_NODE_ID, keyValueB)
				.getSingle();

		Transaction tx = graphDb.beginTx();
		try {
			Expander relExpander = Traversal.expanderForTypes(
					DomainConstants.RelTypes.DOMAIN_LINK, Direction.BOTH);

			relExpander.add(DomainConstants.RelTypes.DOMAIN_LINK, Direction.BOTH);

			PathFinder<WeightedPath> shortestPath = GraphAlgoFactory.aStar(relExpander,
					costEval, estimateEval);

			ps.println(KMLConstants.KML_LINE_START);
			
			emitCoordinate(ps, shortestPath, nodeA, nodeB);
			
			ps.println(KMLConstants.KML_LINE_END);
			
			tx.success();
			
		} finally {
			tx.finish();
		}

		getDbWrapper().shutdownDb();
	}

	private void emitCoordinate(PrintStream printSteam, PathFinder<WeightedPath> shortestPath, Node nodeA, Node nodeB) {

		Path path = shortestPath.findSinglePath(nodeA, nodeB);
		
		for (Node node : path.nodes()) {
			
			double lat = (Double) node.getProperty(DomainConstants.PROP_LATITUDE);
			double lon = (Double) node.getProperty(DomainConstants.PROP_LONGITUDE);
			
			printSteam.println(String.format("%f,%f,2300", lon, lat));
		}
		
	}

	public DbWrapper getDbWrapper() {
		return dbWrapper;
	}

	public void setDbWrapper(DbWrapper dbWrapper) {
		this.dbWrapper = dbWrapper;
	}

}
