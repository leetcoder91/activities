package com.activities.utils;

import java.util.*;

/**
 * A directed acyclic graph's (DAG) vertex.
 */
public class DAGVertex<E> {

// Attributes

/**
 * The value for the vertex.
 */
private final E mValue;

// Associations

/**
 * List of edges to preceding vertices.
 */
private final List<DAGEdge<DAGVertex<E>>> mPrecedingList =
	new ArrayList<DAGEdge<DAGVertex<E>>>();

/**
 * List of edges to succeeding vertices.
 */
private final List<DAGEdge<DAGVertex<E>>> mSucceedingList =
	new ArrayList<DAGEdge<DAGVertex<E>>>();

// Constructors

/**
 * Constructs a DAG vertex with the provided value.
 *
 * @param value The value to set.
 */
public DAGVertex(E value) {
    mValue = value;
}

// Operations

/**
 * @return Returns the value.
 */
public E getValue() {
    return mValue;
}

/**
 * Add the succeeding vertex after this vertex. Also, add this vertex to precede the succeeding
 * vertex.
 *
 * @param succeedingVertex The succeeding vertex.
 */
public void before(DAGVertex<E> succeedingVertex) {
    if (this == succeedingVertex || this.equals(succeedingVertex)) {
        throw new IllegalStateException("A vertex cannot be before or after itself: "
    		+ getValue());
    }

    final DAGEdge<DAGVertex<E>> edge = new DAGEdge<DAGVertex<E>>(this, succeedingVertex);

    mSucceedingList.add(edge);
    succeedingVertex.mPrecedingList.add(edge);
}

/**
 * Add the preceding vertex before this vertex. Also, add this vertex to succeed the preceding
 * vertex.
 *
 * @param precedingVertex The preceding vertex.
 */
public void after(DAGVertex<E> precedingVertex) {
    precedingVertex.before(this);
}

/**
 * @param precedingVertex The preceding vertex.
 * @param succeedingVertex The succeeding vertex.
 *
 * @return True, if there exists an edge from the preceding to the succeeding or vice versa.
 * Otherwise, false.
 */
private boolean doesEdgeExists(DAGVertex<E> precedingVertex,
	DAGVertex<E> succeedingVertex) {
	// There may be a cycle in the graph
	if (precedingVertex == succeedingVertex) {
		return true;
	}

    for (DAGEdge<DAGVertex<E>> vertexEdgeToSucceeding : precedingVertex.mSucceedingList) {
        if (vertexEdgeToSucceeding.getDestination() == succeedingVertex ||
    		vertexEdgeToSucceeding.getSource() == succeedingVertex) {
        	return true;
        }
    }

    for (DAGEdge<DAGVertex<E>> vertexEdgeToPreceding : succeedingVertex.mPrecedingList) {
        if (vertexEdgeToPreceding.getSource() == precedingVertex ||
    		vertexEdgeToPreceding.getDestination() == precedingVertex) {
        	return true;
        }
    }

    return false;
}

/**
 * Remove this vertex while establishing mappings between the preceding and succeeding vertices.
 */
public void remove() {
	// Link all parents of this node to the children of this node
    for (DAGEdge<DAGVertex<E>> vertexEdgeToSucceeding : mSucceedingList) {
        DAGVertex<E> succeedingVertex = vertexEdgeToSucceeding.getDestination();

        // Remove the edge to this (parent) node from the child
        succeedingVertex.mPrecedingList.remove(vertexEdgeToSucceeding);

        for (DAGEdge<DAGVertex<E>> vertexEdgeToPreceding : mPrecedingList) {
        	DAGVertex<E> precedingVertex = vertexEdgeToPreceding.getSource();

        	// Remove edge to succeeding vertex from the preceding vertex
        	precedingVertex.mSucceedingList.remove(vertexEdgeToPreceding);

        	// Add new edge from the preceding vertex to succeeding vertex if it does not already
        	// exist
        	if (!doesEdgeExists(precedingVertex, succeedingVertex) &&
    			!doesEdgeExists(succeedingVertex, precedingVertex)) {
        		precedingVertex.before(succeedingVertex);
        	}
        }
    }

    // Link all children of this node to the parents of this node
    for (DAGEdge<DAGVertex<E>> vertexEdgeToPreceding : mPrecedingList) {
    	DAGVertex<E> precedingVertex = vertexEdgeToPreceding.getSource();

    	// If there are no children, then remove the edges to this node from all its parents
    	precedingVertex.mSucceedingList.remove(vertexEdgeToPreceding);
    }

    mSucceedingList.clear();
    mPrecedingList.clear();
}

/* (non-Javadoc)
 * @see java.lang.Object#toString()
 */
@Override
public String toString() {
    return "DAGVertex [value=" + mValue + "]";
}

/**
 * @param vertexList The collection of vertices.
 * @return A map of set of dependent vertices' value indexed by vertex's value.
 */
public static <V> HashMap<V, HashSet<V>> getMapOfDependents(
	Collection<DAGVertex<V>> vertexList) {
	HashMap<V, HashSet<V>> dependentSetByVertexMap = new HashMap<V, HashSet<V>>(3);

    for(DAGVertex<V> vertex : vertexList) {
    	HashSet<V> dependentSet = new HashSet<V>();

    	dependentSetByVertexMap.put(vertex.getValue(), dependentSet);

        for (Iterator<DAGEdge<DAGVertex<V>>> it = vertex.mSucceedingList.iterator();
    		it.hasNext();) {
            DAGEdge<DAGVertex<V>> vertextEdgeToDependent = it.next();
            DAGVertex<V> dependentVertex = vertextEdgeToDependent.getDestination();

            dependentSet.add(dependentVertex.getValue());
        }
    }

	return dependentSetByVertexMap;
}

/**
 * @param edgeList List of edges between vertices.
 * @return True, if all edges in the list are marked as removed.
 */
private static <V> boolean isEmpty(List<DAGEdge<DAGVertex<V>>> edgeList) {
    for (DAGEdge<DAGVertex<V>> edge : edgeList) {
    	if (!edge.isRemoved()) {
    		return false;
    	}
    }

    return true;
}

/**
 * @param edgeList List of edges between vertices.
 * Marks the edges in the list as not removed. 
 */
private static <V> void resetRemoved(List<DAGEdge<DAGVertex<V>>> edgeList) {
    for (DAGEdge<DAGVertex<V>> edge : edgeList) {
		edge.markRemoved(false);
    }
}

/**
 * Given a raw (unordered) collection of vertices, return a sorted list of vertices.
 * Independent vertices will be placed first in the list.
 *
 * @param vertexList The collection of vertices to be sorted.
 * @return A sorted list of vertices based on their declared dependency information.
 * @throws CyclicDataException Thrown if there exists a cyclic dependency
 *         (A depends on B depends on C depends on A) in the collection of
 * 		   vertices
 */
public static <V> List<DAGVertex<V>> sort(Collection<DAGVertex<V>> vertexList)
		throws CyclicDataException {
    List<DAGVertex<V>> sortedVertexList = new ArrayList<DAGVertex<V>>();

    // List of independent vertices (with no vertices preceding it)
    List<DAGVertex<V>> independentVertexList = new ArrayList<DAGVertex<V>>();

    for(DAGVertex<V> vertex : vertexList) {
        if (vertex.mPrecedingList.size() == 0) {
            independentVertexList.add(vertex);
        }
    }

    // while the list of independent vertices is not empty
    while (!independentVertexList.isEmpty()) {
        // remove an independent vertex from the list of independent vertices
        DAGVertex<V> independentVertex = independentVertexList.remove(0);

        // insert the removed independent vertex into the list of sorted vertices
        sortedVertexList.add(independentVertex);

        // for each dependent vertex with an edge from the independent to dependent vertex do
        for (Iterator<DAGEdge<DAGVertex<V>>> it = independentVertex.mSucceedingList.iterator();
    		it.hasNext();) {
            // remove edge e from the graph
            DAGEdge<DAGVertex<V>> independentToDependentVertexEdge = it.next();

            DAGVertex<V> dependentVertex =
        		independentToDependentVertexEdge.getDestination();

            // Remove the independent to dependent Vertex edge from the independent vertex
            independentToDependentVertexEdge.markRemoved(true);

            // Only tinker with vertices the caller is interested in. If the caller is not
            // interested in a vertex that perhaps is independent, but belongs to another
            // sub-tree, we need to ensure whether it exists in the vertex list that the
            // user is interested in
            if (vertexList.contains(dependentVertex)) {
                // Remove the independent to dependent Vertex edge from the dependent vertex
	            int edgeIdx =
            		dependentVertex.mPrecedingList.indexOf(independentToDependentVertexEdge);

	            if (edgeIdx != -1) {
	            	DAGEdge<DAGVertex<V>> edge = dependentVertex.mPrecedingList.get(edgeIdx);

	            	edge.markRemoved(true);
	            }

	            // if dependent vertex has no other incoming edges then insert the dependent vertex
	            // into the list of independent vertices.
	            if (isEmpty(dependentVertex.mPrecedingList)) {
	            	independentVertexList.add(dependentVertex);
	            }
            }
        }
    }

    // Check to see if all edges are removed
    for (DAGVertex<V> vertex : vertexList) {
    	if (!isEmpty(vertex.mPrecedingList)) {
            throw new CyclicDataException(
                "A cycle has been detected in the list of vertex dependencies. " +
                "A complete topological sort is not possible. " +
                formatDepsForException(vertex));
        }
    }

    // reset removed flags
    for(DAGVertex<V> vertex : vertexList) {
        resetRemoved(vertex.mSucceedingList);
        resetRemoved(vertex.mPrecedingList);
    }

    return sortedVertexList;
}

private static <V> String formatDepsForException(DAGVertex<V> vertex) {
    final StringBuilder result = new StringBuilder();
    
    result.append("'").append(vertex.getValue()).append("' depends on ");

    for (DAGEdge<DAGVertex<V>> dep : vertex.mPrecedingList) {
        DAGVertex<V> source = dep.getSource();
        DAGVertex<V> destination = dep.getDestination();

        result.append(" [");
        result.append(" from: '").append(source.getValue()).append("'");
        result.append(" to: '").append(destination.getValue()).append("'");
        result.append(" ]");
    }

    return result.toString();
}

/**
 * @param vertexCollection The collection of vertices.
 * @return A map of set of depends on vertices' value indexed by vertex's value. The keys are
 * ordered in order of ascending number of dependencies.
 */
public static <V> LinkedHashMap<V, HashSet<V>> getMapOfDependsOn(
	Collection<DAGVertex<V>> vertexCollection) {
	LinkedHashMap<V, HashSet<V>> dependsOnSetByVertexMap = new LinkedHashMap<V, HashSet<V>>(3);

	if (vertexCollection != null) {
		List<DAGVertex<V>> vertextList = new ArrayList<DAGVertex<V>>(vertexCollection);

		Collections.sort(vertextList, new Comparator<DAGVertex<V>>() {
			@Override
			public int compare(DAGVertex<V> vertex1, DAGVertex<V> vertex2) {
				return vertex1.mPrecedingList.size() - vertex2.mPrecedingList.size();
			}
		});

	    for(DAGVertex<V> vertex : vertextList) {
    		HashSet<V> dependendsOnSet = new HashSet<V>();

	    	dependsOnSetByVertexMap.put(vertex.getValue(), dependendsOnSet);

	        for (Iterator<DAGEdge<DAGVertex<V>>> it = vertex.mPrecedingList.iterator();
	    		it.hasNext();) {
	            DAGEdge<DAGVertex<V>> vertexEdgeToDependsOn = it.next();
	            DAGVertex<V> dependsOnVertex = vertexEdgeToDependsOn.getSource();

	            dependendsOnSet.add(dependsOnVertex.getValue());
	        }
    	}
	}

    return dependsOnSetByVertexMap;
}

/**
 * Normalizes the vertex name.
 *
 * @param vertexName The name of the vertex.
 *
 * @return The normalized vertex name.
 */
private static String normalizeVertexName(String vertexName) {
	if (vertexName != null && !vertexName.isEmpty()) {
		return vertexName.replaceAll("(\\W+)", "_");
	}

	return "unknown";
}

/**
 * @param vertexList The collection of vertices.
 * @param xSize The size of the graph along the x-axis.
 * @param ySize The size of the graph along the y-axis.
 *
 * @return Generated GraphViz output.
 */
public static <V> String generateGraphVizOutput(Collection<DAGVertex<V>> vertexList, int xSize,
	int ySize) {

	if (vertexList.isEmpty()) {
		return "Cannot generate GraphViz output in DOT language because there are no vertices.";
	}

	final String lineDelimiter = ";";

	HashMap<V, HashSet<V>> dependentSetByVertexMap = getMapOfDependents(vertexList);
    StringBuilder graphVizOutput = new StringBuilder();

    graphVizOutput.append("digraph Activities {");
    graphVizOutput.append("\nsize = \"" + xSize + "," + ySize + "\"" + lineDelimiter);

    for (Map.Entry<V, HashSet<V>> entry : dependentSetByVertexMap.entrySet()) {
    	V vertex = entry.getKey();
    	HashSet<V> dependentSet = entry.getValue();

    	graphVizOutput.append("\n\t");
    	graphVizOutput.append(normalizeVertexName(vertex.toString()));

    	if (dependentSet.isEmpty()) {
    		graphVizOutput.append(lineDelimiter);
    	}
    	else {
    		graphVizOutput.append(" -> ");

    		if (dependentSet.size() == 1) {
	        	for (V dependentVertex : dependentSet) {
	            	graphVizOutput.append(normalizeVertexName(dependentVertex.toString()));
	        	}

	        	graphVizOutput.append(lineDelimiter);
	    	}
	    	else {
	    		graphVizOutput.append("{");

	        	for (V dependentVertex : dependentSet) {
	            	graphVizOutput.append(normalizeVertexName(dependentVertex.toString()));
	            	graphVizOutput.append(" " + lineDelimiter + " ");
	        	}

	        	graphVizOutput.append("}");
	    	}
    	}
    }

    graphVizOutput.append("\n}");

    return graphVizOutput.toString();
}

// Inner Classes

/**
 * A directed acyclic graph's (DAG) edge.
 */
private static final class DAGEdge<T> {

// Attributes

/**
 * The source vertex.
 */
private T mSource;

/**
 * The destination vertex.
 */
private T mDestination;

/**
 * Indicates whether the edge between two vertices was removed.
 */
private boolean mRemoved;

// Constructors

/**
 * Constructs a DAG edge.
 *
 * @param source The source vertex.
 * @param destination The destination vertex.
 */
public DAGEdge(T source, T destination) {
	setSource(source);
	setDestination(destination);
}

// Operations

/**
 * @return The source vertex.
 */
public T getSource() {
	return mSource;
}

/**
 * Sets the source vertex.
 *
 * @param source The source vertex to set.
 */
public void setSource(T source) {
	mSource = source;
}

/**
 * @return The destination vertex.
 */
public T getDestination() {
	return mDestination;
}

/**
 * Sets the destination vertex.
 *
 * @param destination The destination vertex.
 */
public void setDestination(T destination) {
	mDestination = destination;
}

/**
 * @return The removed flag.
 */
public boolean isRemoved() {
	return mRemoved;
}

/**
 * Marks the edge between two vertices as removed.
 *
 * @param removed The removed flag.
 */
public void markRemoved(boolean removed) {
	mRemoved = removed;
}

/* (non-Javadoc)
 * @see java.lang.Object#hashCode()
 */
@Override
public int hashCode() {
    final int prime = 31;
    int result = 1;

    result = prime * result + ((mSource == null) ? 0 : mSource.hashCode());
    result = prime * result + ((mDestination == null) ? 0 : mDestination.hashCode());

    return result;
}

/* (non-Javadoc)
 * @see java.lang.Object#equals(java.lang.Object)
 */
@SuppressWarnings("unchecked")
@Override
public boolean equals(Object obj) {
    if (this == obj) {
        return true;
    }

    if (obj == null) {
        return false;
    }

    if (getClass() != obj.getClass()) {
        return false;
    }

    final DAGEdge<T> other = (DAGEdge<T>)obj;

    if (mSource == null) {
        if (other.mSource != null) {
            return false;
        }
    }
    else if (!mSource.equals(other.mSource)) {
        return false;
    }

    if (mDestination == null) {
        if (other.mDestination != null) {
            return false;
        }
    }
    else if (!mDestination.equals(other.mDestination)) {
        return false;
    }

    return true;
}

}

}
