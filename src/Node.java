package game;

import java.util.ArrayList;
import java.util.List;

public class Node {

	// coordinates of each location
	int i;
	int j;
	int weight;
	
	// list filled with each's node neighbors
	List<Neighbor> neighbors;
	
	// each cell is considered a node 
	Node(int i, int j, int weight) {
		this.i = i; 
		this.j = j; 
		this.weight = weight; 
		this.neighbors = new ArrayList<>();
	}

	public void setI(int i) {
		this.i = i;
	}

	public void setJ(int j) {
		this.j = j;
	}
	
	public void setWeight(int weight) {
		this.weight = weight;
	}

	public int[] getNodeCoordinations() {
		int[] nodeCoordinations = new int[2];
		nodeCoordinations[0] = i;
		nodeCoordinations[1] = j;
		return nodeCoordinations;
	}
	
	public class Neighbor {
		Node node;

		Neighbor(Node node) {
			this.node = node;
		}
		
		public Node getNode() {
			return node;
		}
		public void setNode(Node node) {
			this.node = node;
		}
		
		public int[] getNodeCoordinations() {
			int[] nodeCoordinations = new int[2];
			nodeCoordinations[0] = node.i;
			nodeCoordinations[1] = node.j;
			return nodeCoordinations;
		}
	}
	
	void addNeighbor(Node node) {
		Neighbor newNeighbor = new Neighbor(node);
		neighbors.add(newNeighbor);
	}

	public List<Neighbor> getNeighbors() {
		return neighbors;
	}
}