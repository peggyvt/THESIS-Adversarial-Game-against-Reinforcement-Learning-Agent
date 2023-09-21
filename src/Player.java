package game;

import java.util.Random;

public class Player {
	
	static boolean goal() {
		return Grid.locations[4][8] == (int)'B';
	}
	
	public static Node getRandomPosition(Node playerCurrentState, Node agentCurrentState, int[] ballCoordinations) {
		
		// get agent coordinations
		int[] agentCoordinations = agentCurrentState.getNodeCoordinations();
		
		// find all possible player actions
    	Node[] actionsFromCurrentState = new Node[playerCurrentState.neighbors.size()];
        for (int a=0; a<playerCurrentState.neighbors.size(); a++) {
			int[] nodeCoordination = playerCurrentState.neighbors.get(a).getNodeCoordinations();
			actionsFromCurrentState[a] = Grid.nodes[nodeCoordination[0]][nodeCoordination[1]];	
		}
    	
    	// pick a random next state
    	Node playerNextState;
    	boolean movement = false;
    	do {
			Random rand = new Random();
			int index = rand.nextInt(actionsFromCurrentState.length);
			playerNextState = actionsFromCurrentState[index];
	        
	        // check if that movement is allowed
	        movement = Grid.changeRewardsAndCheckMovement((int)'P', agentCoordinations, playerCurrentState, playerNextState);
	    /* if the selected action is not allowed to happen, choose again randomly */
		} while (movement == false);
		return playerNextState;
	}
	
}