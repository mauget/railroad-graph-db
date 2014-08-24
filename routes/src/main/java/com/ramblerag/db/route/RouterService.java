package com.ramblerag.db.route;

import java.io.PrintStream;

import org.apache.log4j.Logger;
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
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.kernel.Traversal;

import com.ramblerag.db.core.ApplicationException;
import com.ramblerag.db.core.DbWrapper;
import com.ramblerag.domain.DomainConstants;
import com.ramblerag.servlet.Initializer;

/**
 * Dump route to KML, suitable for feeding to Google Maps

 * @author mauget
 *
 */
public class RouterService {

	private static Logger log = Logger.getLogger(RouterService.class);
	
	// Injected
	private DbWrapper dbWrapper;

	// Routing helper functions
	private final EstimateEvaluator<Double> estimateEval = CommonEvaluators.geoEstimateEvaluator(
			DomainConstants.PROP_LATITUDE, DomainConstants.PROP_LONGITUDE );

	private final CostEvaluator<Double> costEval = new CostEvaluator<Double>() {

    	// Constant cost. Could be a function of lading type, location, fuel surcharge, washed-out track, ..
		public Double getCost(Relationship relationship, Direction direction) {
			return 1d;
		}};

	public void findShortestPath(PrintStream ps, long startNode, long endNode) {
		try {
			RouterService router = Initializer.getApplicationContext().getBean(RouterService.class);
			router.emitShortestPathKML(ps, startNode, endNode);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void emitShortestPathKML(PrintStream ps, long keyValueA, long keyValueB)
			throws ApplicationException {
		
		log.info(String.format("Finding least-expensive route from node %d to node %d", keyValueA, keyValueB));
		
		GraphDatabaseService graphDb = getDbWrapper().getDbRef();

		Index<Node> nodeIndex = graphDb.index().forNodes(DomainConstants.INDEX_NAME);

		Node nodeA = nodeIndex.get(DomainConstants.PROP_NODE_ID, keyValueA)
				.getSingle();
		
		Node nodeB = nodeIndex.get(DomainConstants.PROP_NODE_ID, keyValueB)
				.getSingle();

		log.info(String.format("nodeA ID %d", nodeA.getId()));
		log.info(String.format("nodeB ID %d", nodeB.getId()));
		
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

		//getDbWrapper().shutdownDb();
	}

	private void emitCoordinate(PrintStream printSteam, PathFinder<WeightedPath> shortestPath, Node nodeA, Node nodeB) {

		WeightedPath path = shortestPath.findSinglePath(nodeA, nodeB);
		
		if (null != path){
			if (true && path.length() == 0) {
				// No route found: show a balloon at nodeA
				emitNoRouteAtNode(printSteam, nodeA);
				System.out.println("No route found"); // temp
			} else 	
			for (Node node : path.nodes()) {
				// Spew the path of connected nodes.
				double lat = (Double) node.getProperty(DomainConstants.PROP_LATITUDE);
				double lon = (Double) node.getProperty(DomainConstants.PROP_LONGITUDE);
				
				printSteam.println(String.format("%f,%f,2300", lon, lat));
				
				//System.out.println(String.format("%f,%f,2300", lon, lat)); // temp
			}
			log.info(String.format("Emitted route having shortest path coordinates"));
		}
	}

	private void emitNoRouteAtNode(PrintStream printSteam, Node node) {
		String prototype =
		  "<name>Data+BalloonStyle</name>" +
				"<Style id='golf-balloon-style'>" +
		    "<BalloonStyle>" +
		      "<text>" +
		        "<![CDATA[" +
		        "  No path found." +
		        "]]>" +
		      "</text>" +
		    "</BalloonStyle>" +
		  "</Style>" +
		  "</Placemark>" +
		    "<name>Club house</name>" +
		    "<styleUrl>#balloon-style</styleUrl>" +
		    "<Point>" +
		      "<coordinates>%f,%f</coordinates>" +
		    "</Point>" +
		  "</Placemark>";
		
		printSteam.println(String.format(prototype, node.getProperty(DomainConstants.PROP_LATITUDE), node.getProperty(DomainConstants.PROP_LONGITUDE)));
	}

	public DbWrapper getDbWrapper() {
		return dbWrapper;
	}

	public void setDbWrapper(DbWrapper dbWrapper) {
		this.dbWrapper = dbWrapper;
	}

}
