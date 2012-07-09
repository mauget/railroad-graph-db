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
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.kernel.Traversal;

import com.ramblerag.db.ApplicationException;
import com.ramblerag.db.DbWrapper;
import com.ramblerag.domain.DomainConstants;

/**
 * Dump route to KML suitable for feeding to Google Earth

 * @author mauget
 *
 */
public class Route {

	private final EstimateEvaluator<Double> estimateEval = CommonEvaluators.geoEstimateEvaluator(
			DomainConstants.PROP_LATITUDE, DomainConstants.PROP_LONGITUDE );
	
    private final CostEvaluator<Double> costEval = CommonEvaluators.doubleCostEvaluator( "undefined_key", 1 );

	public static void main(String[] args) {
		try {
			// Dumps to System.out PrintStream, but caller can supply any PrintStream
			new Route().route(System.out, 1, 133752);
		} catch (ApplicationException e) {
			e.printStackTrace();
		}
	}

	public void route(PrintStream ps, long keyValueA, long keyValueB)
			throws ApplicationException {

		DbWrapper dbw = DbWrapper.getInstance();

		GraphDatabaseService graphDb = dbw.startDb();

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

		dbw.shutdownDb();
	}

	private void emitCoordinate(PrintStream printSteam, PathFinder<WeightedPath> shortestPath, Node nodeA, Node nodeB) {

		Path path = shortestPath.findSinglePath(nodeA, nodeB);
		
		for (Node node : path.nodes()) {
			
			double lat = (Double) node.getProperty(DomainConstants.PROP_LATITUDE);
			double lon = (Double) node.getProperty(DomainConstants.PROP_LONGITUDE);
			
			printSteam.println(String.format("%f,%f,2300", lon, lat));
		}
		
	}

}
