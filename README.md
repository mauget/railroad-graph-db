railroad-graph-db
=================

We load a USA 1996 census railroad network into Neo4j, a NoSQL graph datbase. 
A command app emits shortest rail route to stdout using A* algorithm built into Neo4j. 
Output is KML for Google Earth rendering. The mobile web application uses Neo4J A* 
algorithm to find shortest route between any two selected rail stations points. 
The result is a KML layer on  Google Maps
