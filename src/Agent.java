package game;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import game.Node.Neighbor;

public class Agent {
	
	private final static int gridSize = Grid.BSIZE;
	 
	static boolean goal() {
		return Grid.locations[4][0] == (int)'B';
	}

	
    /***** Q LEARNING ALGORITHM *****/
	static double[][][][] Q = new double[gridSize*gridSize][19][19][6];
    
	/*********** TRAINING ***********/	
	static void calculateQ(int[] playerCurrentStateCoordinations, int[] agentCoordinates, int[] ballCoordinates) {
        final double alpha = 0.1; // agent's learning rate
        final double gamma = 0.9; // Eagerness - 0 looks in the near future, 1 looks in the distant future
        int i=0, j=0;
        
        // Initialize Q array values to zero:
        // for each state (total of 81)
        for (int q1 = 0; q1 < gridSize*gridSize; q1++) {
        	// agent's optical field: 
        	// agent is able to see two circles around himself, which is max 18 nodes.
        	// if someone (opponent or ball) is out of agent's optical field,
        	// then on the 19th position the q value is entered.
        	// 1. for opponent's position
    		for (int q2 = 0; q2 < 19; q2++) {
        		// 2. for ball's position 
        		for (int q3 = 0; q3 < 19; q3++) {
                	
        			// for each neighbor/action in that state (max 6) 
        			//for (int q4 = 0; q4 < currentNode.neighbors.size(); q4++) {
        			for (int q4 = 0; q4 < 6; q4++) {
        				// initialize the current value Q with zero
                		// when in current node, with that optical field and towards q4-neighbor/action
                		Q[q1][q2][q3][q4] = 0.0;
            		}
        		}
			}
    		j++; // move on to the next j (switch column)
    		if (j%9 == 0 && j != 0) { // if end of column, change row and set j to zero
    			i++; // move on to the next i (switch row)  
    			j = 0;
    		}
        }
        
		i = 0;
		j = 0;
		double epsilon = 1.0;
		for (int epoch = 0; epoch < 20000; epoch++) { // the epochs
			
			System.out.println("EPOCH: "+epoch);
			
			// choose initial state from the node array
			Node agentCurrentState = Grid.nodes[i][j];
		
			// check if that agent state is allowed and empty
            if (agentCurrentState == Grid.nodes[playerCurrentStateCoordinations[0]][playerCurrentStateCoordinations[1]]) {
            	Grid.locations[playerCurrentStateCoordinations[0]][playerCurrentStateCoordinations[1]] = Grid.EMPTY;
				int pi, pj;
                Random rand = new Random();
				do {
                	pi = rand.nextInt(9);
	                pj = rand.nextInt(9);
	                playerCurrentStateCoordinations[0] = pi;
	                playerCurrentStateCoordinations[1] = pj;
                } while ((pi == agentCurrentState.i && pj == agentCurrentState.j) ||
                		(pi == ballCoordinates[0] && pj == ballCoordinates[1]) ||
                		(pi == 4 && pj == 4));
				Grid.locations[playerCurrentStateCoordinations[0]][playerCurrentStateCoordinations[1]] = (int)'P';
			}
            if (agentCurrentState == Grid.nodes[ballCoordinates[0]][ballCoordinates[1]]) {
            	
            	Grid.locations[ballCoordinates[0]][ballCoordinates[1]] = Grid.EMPTY;
            	ballCoordinates[1] = ballCoordinates[1] - 1;
            	
            	if (playerCurrentStateCoordinations[0] == ballCoordinates[0] && 
            			playerCurrentStateCoordinations[1] == ballCoordinates[1]) {
            		Grid.locations[playerCurrentStateCoordinations[0]][playerCurrentStateCoordinations[1]] = Grid.EMPTY;
    				int pi, pj;
                    Random rand = new Random();
    				do {
                    	pi = rand.nextInt(9);
    	                pj = rand.nextInt(9);
    	                playerCurrentStateCoordinations[0] = pi;
    	                playerCurrentStateCoordinations[1] = pj;
                    } while ((pi == agentCurrentState.i && pj == agentCurrentState.j) ||
                    		(pi == ballCoordinates[0] && pj == ballCoordinates[1]) ||
                    		(pi == 4 && pj == 4));
    				Grid.locations[playerCurrentStateCoordinations[0]][playerCurrentStateCoordinations[1]] = (int)'P';
            	}
            	
            	Grid.locations[ballCoordinates[0]][ballCoordinates[1]] = (int)'B';
            }
            
            // while goal isn't achieved, repeat 
	        boolean agentMovement = true; 
			while (!goal() && !Player.goal()) {
				
    			// get agent coordinations
    			int[] agentCurrentStateCoordinations = agentCurrentState.getNodeCoordinations();
    			// find all possible agent actions
            	Node[] actionsFromCurrentState = new Node[agentCurrentState.neighbors.size()];
            	for (int z=0; z<agentCurrentState.neighbors.size(); z++) {
					int[] nodeCoordination = agentCurrentState.neighbors.get(z).getNodeCoordinations();
					actionsFromCurrentState[z] = Grid.nodes[nodeCoordination[0]][nodeCoordination[1]];
				}
            	
            	// get player's node 
    			Node playerCurrentState = Grid.nodes[playerCurrentStateCoordinations[0]][playerCurrentStateCoordinations[1]];
    			
            	// get agent's position on Q array
                int qA = Grid.findNumber(agentCurrentState);
                // get opponent's position, check if he's inside the agent's scope
                int qP = checkScope(agentCurrentState, playerCurrentState.getNodeCoordinations());
                // get ball's position, check if he's inside the agent's scope
                int qB = checkScope(agentCurrentState, ballCoordinates);
                
                // Pick an action from the ones possible - using the å-greedy function
            	Node agentNextState = epsilonGreedy(epoch, Q, qA, qP, qB, actionsFromCurrentState, epsilon, agentMovement);
                int[] agentNextStateCoordinations = agentNextState.getNodeCoordinations();
        		
        		// get agent's next state position
                int qNS = checkScope(agentCurrentState, agentNextStateCoordinations);
                
        		// initialize opponent's movement and next state 
        		boolean playerMovement = true;
        		Node playerNextState = null; 
    			int[] playerNextStateCoordinations = new int[2];
    			
    			// check if agent is allowed to move
        		agentMovement = Grid.changeRewardsAndCheckMovement((int)'A', playerCurrentStateCoordinations, agentCurrentState, agentNextState);
        		if (agentMovement) {
        			// MOVE AGENT
					String agentOrientation = Grid.findOrientation(agentCurrentState, agentNextState);
					int agentKeyCode = Grid.stringToIntKeyCode(agentOrientation);
					Grid.makeMoveonGrid((int)'A', agentCurrentStateCoordinations, agentKeyCode);
					ballCoordinates = Grid.search((int)'B');
					// MOVE PLAYER 
					/* selecting player's (agent's opponent) next state randomly */ 
					playerNextState = Player.getRandomPosition(playerCurrentState, agentNextState, ballCoordinates);
	            	playerNextStateCoordinations = playerNextState.getNodeCoordinations();
	            	// check if player is allowed to move
	            	playerMovement = Grid.changeRewardsAndCheckMovement((int)'P', agentNextStateCoordinations, playerCurrentState, playerNextState);
					
					if (playerMovement) {
						String playerOrientation = Grid.findOrientation(playerCurrentState, playerNextState);
						int playerKeyCode = Grid.stringToIntKeyCode(playerOrientation);
						Grid.makeMoveonGrid((int)'P', playerCurrentStateCoordinations, playerKeyCode);
						ballCoordinates = Grid.search((int)'B');
					}
				} else if (agentMovement == false && epoch >= 18000) {
					/* since agent can't move on exploit and he has to, select the second best position */
					Node res = epsilonGreedy(epoch, Q, qA, qP, qB, actionsFromCurrentState, epsilon, agentMovement);
					agentNextStateCoordinations[0] = res.i; 
					agentNextStateCoordinations[1] = res.j;
	                agentNextState = Grid.nodes[agentNextStateCoordinations[0]][agentNextStateCoordinations[1]];
	                String agentOrientation = Grid.findOrientation(agentCurrentState, agentNextState);
					int agentKeyCode = Grid.stringToIntKeyCode(agentOrientation);
					
					agentMovement = Grid.changeRewardsAndCheckMovement((int)'A', playerCurrentStateCoordinations, agentCurrentState, agentNextState);
					if (agentMovement) {
						Grid.makeMoveonGrid((int)'A', agentCurrentStateCoordinations, agentKeyCode);
						ballCoordinates = Grid.search((int)'B');
						
						/* selecting player's (agent's opponent) next state randomly */
						playerNextState = Player.getRandomPosition(playerCurrentState, agentNextState, ballCoordinates);
		            	playerNextStateCoordinations = playerNextState.getNodeCoordinations();
						playerMovement = Grid.changeRewardsAndCheckMovement((int)'P', agentNextStateCoordinations, playerCurrentState, playerNextState);
						
						if (playerMovement) {
							String playerOrientation = Grid.findOrientation(playerCurrentState, playerNextState);
							int playerKeyCode = Grid.stringToIntKeyCode(playerOrientation);
							Grid.makeMoveonGrid((int)'P', playerCurrentStateCoordinations, playerKeyCode);
							ballCoordinates = Grid.search((int)'B');
						}
					}
				}
                
				/* if movement is not allowed then it's not going to happen
				 * but the reward of nextState will be taken anyway in order to update the Q array value
				 * afterwards, the next state will be the current state */ 
				
				// get maximum Q value for this next state based on all possible actions
    			double[] results = maxQ(Q, qA, qP, qB);
                double maxQ = results[0];
                
                // find reward when in currentState and making action actionsFromCurrentState[index] to go to nextState
                int reward = agentNextState.weight;
                
                // get Q value from array
                double q = Q[qA][qP][qB][qNS];
               
                // Calculate: Q(state,action) =
                // Q(state,action) + alpha * (R(state,action) + gamma * Max(next state, all actions) - Q(state,action))
                double value = q + alpha * (reward + gamma * maxQ - q);
                Q[qA][qP][qB][qNS] = value;
            
                // if movement is not allowed then it's not going to happen
                int[] oldPlayerCoordinations = new int[2];
                oldPlayerCoordinations[0] = playerCurrentStateCoordinations[0];
                oldPlayerCoordinations[1] = playerCurrentStateCoordinations[1];
                if (agentMovement) {
                	agentCurrentState = agentNextState;
                	if (playerMovement) {
                		playerCurrentState = playerNextState;
                		playerCurrentStateCoordinations[0] = playerNextStateCoordinations[0];
    					playerCurrentStateCoordinations[1] = playerNextStateCoordinations[1];
                	}
                } 
                
                // reset rewards
                Grid.resetRewards(oldPlayerCoordinations, playerCurrentStateCoordinations, ballCoordinates, agentCurrentState);
            } // end while
			
			epsilon = epsilon - 0.000055; 
			// - 0.000055: when epoch is 18.000, epsilon is zero
			
			j++; // move on to the next j (switch column)
    		if (j%9 == 0 && j != 0) { // if end of column, change row and set j to zero
    			i++; // move on to the next i (switch row)
    			if (i%9 == 0) {
    				i = 0;
    			}
    			j = 0;
    		}
    		
    		// set position of agent 
    		agentCoordinates[0] = i;
    		agentCoordinates[1] = j;
    		// reset position of ball
    		ballCoordinates[0] = 4;
    		ballCoordinates[1] = 4;
    		
    		// reset grid
    		Grid.resetGrid("testing", playerCurrentStateCoordinations, agentCoordinates);
		} // end for
		
		/***** TRAINING IS COMPLETED *****/
		
        // reset position of player
		playerCurrentStateCoordinations[0] = 4;
		playerCurrentStateCoordinations[1] = 1;
		// reset position of agent
		agentCoordinates[0] = 4;
		agentCoordinates[1] = 7;
		// reset position of ball
		ballCoordinates[0] = 4;
		ballCoordinates[1] = 4;
		
		// reset grid
 		Grid.resetGrid("running", playerCurrentStateCoordinations, agentCoordinates);
    }

	/*********** RUNNING ***********/
	static int[] QMove(int[] playerCoordinates, int[] agentCoordinates, int[] ballCoordinates) {
		/***** AGENT NOW EXECUTES A MOVE BASED ON EXPLOITATION OF EPSILON FUNCTION *****/ 
		
        int[] finalStateCoordinates = new int[2];
        
		// find agent's actual current state
		Node actualCurrentState = Grid.nodes[agentCoordinates[0]][agentCoordinates[1]];
		
		// find all possible actions from current state
    	Node[] actionsFromActualCurrentState = new Node[actualCurrentState.neighbors.size()];
    	for (int z=0; z<actualCurrentState.neighbors.size(); z++) {
			int[] nodeCoordination = actualCurrentState.neighbors.get(z).getNodeCoordinations();
			actionsFromActualCurrentState[z] = Grid.nodes[nodeCoordination[0]][nodeCoordination[1]];
		}
    	
    	// find agent's actual next state
    	int qA = Grid.findNumber(actualCurrentState);
    	int qP = checkScope(actualCurrentState, playerCoordinates);
    	int qB = checkScope(actualCurrentState, ballCoordinates);
		Node actualNextState = epsilonGreedy(0, Q, qA, qP, qB, actionsFromActualCurrentState, 0, true);
		int[] nextStateCoordinations = actualNextState.getNodeCoordinations();
    	finalStateCoordinates[0] = nextStateCoordinations[0];
        finalStateCoordinates[1] = nextStateCoordinations[1];
         
		return finalStateCoordinates;
	}
	
	/********* Q FUNCTIONS *********/
	private static int checkScope(Node agentState, int[] coordinates) {
    	/***** CHECK IF GIVEN COORDINATES ARE WITHIN AGENT'S SCOPE *****/
		
		Node otherState = Grid.nodes[coordinates[0]][coordinates[1]];
    	boolean isNeighbor = false;
		for(Neighbor n : agentState.getNeighbors()) {
			if(n.node.equals(otherState)) {
				isNeighbor = true;
			}
		}
		
		int number = -1;
		
		// if they belong in agent's first circle of scope:
    	if (isNeighbor) {
    		String orientation = Grid.findOrientation(agentState, otherState);
    		switch(orientation) { 
    			case "UP":
    				number = 0;
    				break;
    			case "UPPER RIGHT":
    				number = 1;
    				break;
    			case "LOWER RIGHT":
    				number = 2;
    				break;
    			case "DOWN":
    				number = 3;
    				break;
    			case "LOWER LEFT":
    				number = 4;
    				break;
    			case "UPPER LEFT":
    				number = 5;
    				break;
    		}
    		
    	// if they belong in agent's second circle of scope:
    	} else if (agentState.i == coordinates[0] && agentState.j - 2 == coordinates[1]) {
			number = 6;
		} else if (agentState.i + 1 == coordinates[0] && agentState.j - 2 == coordinates[1]) {
			number = 7;
		} else if (agentState.i + 2 == coordinates[0] && agentState.j - 1 == coordinates[1]) {
			number = 8;
		} else if (agentState.i + 2 == coordinates[0] && agentState.j == coordinates[1]) {
			number = 9;
		} else if (agentState.i + 2 == coordinates[0] && agentState.j + 1 == coordinates[1]) {
			number = 10;
		} else if (agentState.i + 1 == coordinates[0] && agentState.j + 1 == coordinates[1]) {
			number = 11;
		} else if (agentState.i == coordinates[0] && agentState.j + 2 == coordinates[1]) {
			number = 12;
		} else if (agentState.i - 1 == coordinates[0] && agentState.j + 1 == coordinates[1]) {
			number = 13;
		} else if (agentState.i - 2 == coordinates[0] && agentState.j + 1 == coordinates[1]) {
			number = 14;
		} else if (agentState.i - 2 == coordinates[0] && agentState.j == coordinates[1]) {
			number = 15;
		} else if (agentState.i - 2 == coordinates[0] && agentState.j - 1 == coordinates[1]) {
			number = 16;
		} else if (agentState.i - 1 == coordinates[0] && agentState.j - 2 == coordinates[1]) {
			number = 17;
			
		} else {
    		// if they don't belong in agent's scope
    		number = 18;
    	}
    	
    	return number;
	}

	private static Node epsilonGreedy(int epoch, double[][][][] Q, int qA, int qP, int qB, 
			Node[] actionsFromCurrentState, double epsilon, boolean movement) {
		double n = ThreadLocalRandom.current().nextDouble(0, 1);
		if (n < epsilon) { // explore
			Random rand1 = new Random();
			int index = rand1.nextInt(actionsFromCurrentState.length);
            Node nextState = actionsFromCurrentState[index];
			return nextState;
		} else { // exploit
			double[] results = maxQ(Q, qA, qP, qB);
			Node nextState = Grid.nodes[(int) results[1]][(int) results[2]];
			if (movement == true) {
				nextState = Grid.nodes[(int) results[1]][(int) results[2]];
			} else if (movement == false && epoch >= 18000) {
				/* since agent can't move on exploit and he has to, select the second best position */
				nextState = Grid.nodes[(int) results[4]][(int) results[5]];
			}
			return nextState;
		}
	}

	private static double[] maxQ(double[][][][] Q, int qA, int qP, int qB) {
		double[] result = new double[6];
		double max = -100000;
        double secondmax = -100000;
    	
        // get agent's state
        int[] agentCoordinates = Grid.findPosition(qA);
    	Node agentState = Grid.nodes[agentCoordinates[0]][agentCoordinates[1]];
    	
    	// initialize neighbor coordinations
    	int[] neighborCoor = agentState.neighbors.get(0).getNodeCoordinations();
		
    	// initialize first and second best next state coordinations
    	double iNS =  neighborCoor[0]; // get i from coordinates
    	double jNS =  neighborCoor[1]; // get j from coordinates
    	double siNS =  neighborCoor[0]; // get i from coordinates
    	double sjNS =  neighborCoor[1]; // get j from coordinates
    	
		for (int qNS = 0; qNS < agentState.neighbors.size(); qNS++) {
			if (Q[qA][qP][qB][qNS] > max) {
				secondmax = max;
	        	max = Q[qA][qP][qB][qNS];
	        	
	        	// set the coordinates of the second best action
	        	siNS =  neighborCoor[0]; // get i from coordinates
				sjNS =  neighborCoor[1]; // get j from coordinates
				
	        	// get neighbor's node
	        	neighborCoor = agentState.neighbors.get(qNS).getNodeCoordinations();
	        	
	        	// set the coordinates of the best action
	        	iNS =  neighborCoor[0]; // get i from coordinates
				jNS =  neighborCoor[1]; // get j from coordinates
	        }
		}
		
		result[0] = max;
		result[1] = iNS;
		result[2] = jNS;
		result[3] = secondmax;
		result[4] = siNS;
		result[5] = sjNS;
		
		/* if agent node has only two neighbors, then on the second max insert the second neighbor on the second max
		in order to not be the same as the first one */
		if (secondmax == -100000.0) {
			result[3] = Q[qA][qP][qB][1];
			neighborCoor = agentState.neighbors.get(1).getNodeCoordinations();
        	siNS =  neighborCoor[0]; // get i from coordinates
			sjNS =  neighborCoor[1]; // get j from coordinates
			result[4] = siNS;
			result[5] = sjNS;
		}
		
		return result;
	}
	
	
	/***** MINIMAX Q LEARNING ALGORITHM *****/
	static double[][][][][] MinimaxQ = new double[gridSize*gridSize][19][19][6][6];
	static double[][][][] P = new double[gridSize*gridSize][19][19][6];
    static double[][][] V = new double[gridSize*gridSize][19][19];
    
    /*************** TRAINING ***************/   
	static void calculateMinimaxQ(int[] playerCurrentStateCoordinations, int[] agentCoordinates, int[] ballCoordinates) {
        double alpha = 1.0; // agent's learning rate that decays over time
        final float decay = 0.0000052632f; // slow decay rate of alpha and explor
        final double gamma = 0.9; // IGNORE: Eagerness - 0 looks in the near future, 1 looks in the distant future
        
    	Random rand = new Random();
    	
        // Initialize Q array values to one:
        // for each agent's position
        for (int q1 = 0; q1 < gridSize*gridSize; q1++) {
        	
        	// 1. for each opponent's position
        	/* agent's optical field:  
        	 * agent is able to see two circles around himself, 
        	 * which is max 18 nodes. 
        	 * if the opponent is out of agent's optical field,
        	 * then on the 19th position the q value is entered. */
    		for (int q2 = 0; q2 < 19; q2++) {
        		
    			// 2. for ball's position
    			/* agent's optical field: 
            	 * agent is able to see the ball two circles around himself, 
            	 * which is max 18 nodes.
            	 * if the ball is out of agent's optical field,
            	 * then on the 19th position the q value is entered. */
        		for (int q3 = 0; q3 < 19; q3++) {
                	
        			// for each agent's neighbor/action in that state (max 6) 
        			for (int q4 = 0; q4 < 6; q4++) {

    					// for each opponent's neighbor/action in that state (max 6)
        				for (int q5 = 0; q5 < 6; q5++) {
        					// initialize the current Q value with one
                    		/* when agent in specific node, having that specific optical field 
                    		 * and agent heading towards q4 neighbor/action
                    		 * having the opponent head towards the q5 neighbor/action */
        					MinimaxQ[q1][q2][q3][q4][q5] = 1.0;
        				}
            		}
        		}
			}
        }

        int i=0, j=0;
        // Initialize P array values to 1/|A|:
        // for each agent's position
        for (int p1 = 0; p1 < gridSize*gridSize; p1++) {
        	Node agentNode = Grid.nodes[i][j];
        	
        	// for opponent's position
        	for (int p2 = 0; p2 < 19; p2++) {
        		
        		// for ball's position
        		for (int p3 = 0; p3 < 19; p3++) {
        			
        			// for each agent's neighbor/action in that state (max 6)
        			for (int p4 = 0; p4 < 6; p4++) {
        				// initialize the current pi value with 1/|A|
        	    		// when in current agentNode and towards p4-neighbor/action
        	    		// where |A| is number of neighbor nodes in current state
        				P[p1][p2][p3][p4] = 1/agentNode.neighbors.size();
        			}
        		}
        	}
        	j++; // move on to the next j (switch column)
    		if (j%9 == 0 && j != 0) { // if end of column, change row and set j to zero
    			i++; // move on to the next i (switch row)  
    			j = 0;
    		}
        }
        
		// Initialize V array values to one:
        // for each state (agent's position, opponent's position, ball's position)
        for (int v1 = 0; v1 < gridSize*gridSize; v1++) {
    		for (int v2 = 0; v2 < 19; v2++) {
        		for (int v3 = 0; v3 < 19; v3++) {
        			// initialize the current V value with one
        			V[v1][v2][v3] = 1.0;
        		}
    		}
        }
  		
        i = 0; 
        j = 0;
        double explor = 1.0;
        for (int epoch = 0; epoch < 200000; epoch++) { // the epochs

        	System.out.println("EPOCH: "+epoch);
			
			// choose initial state from the node array
			Node agentCurrentState = Grid.nodes[i][j];
			
			// check if that agent state is allowed and empty
            if (agentCurrentState == Grid.nodes[playerCurrentStateCoordinations[0]][playerCurrentStateCoordinations[1]]) {
            	Grid.locations[playerCurrentStateCoordinations[0]][playerCurrentStateCoordinations[1]] = Grid.EMPTY;
				int pi, pj;
                rand = new Random();
				do {
                	pi = rand.nextInt(9);
	                pj = rand.nextInt(9);
	                playerCurrentStateCoordinations[0] = pi;
	                playerCurrentStateCoordinations[1] = pj;
                } while ((pi == agentCurrentState.i && pj == agentCurrentState.j) ||
                		(pi == ballCoordinates[0] && pj == ballCoordinates[1]) ||
                		(pi == 4 && pj == 4));
				Grid.locations[playerCurrentStateCoordinations[0]][playerCurrentStateCoordinations[1]] = (int)'P';
			}
            if (agentCurrentState == Grid.nodes[ballCoordinates[0]][ballCoordinates[1]]) {
            	
            	Grid.locations[ballCoordinates[0]][ballCoordinates[1]] = Grid.EMPTY;
            	ballCoordinates[1] = ballCoordinates[1] - 1;
            	
            	if (playerCurrentStateCoordinations[0] == ballCoordinates[0] && 
            			playerCurrentStateCoordinations[1] == ballCoordinates[1]) {
            		Grid.locations[playerCurrentStateCoordinations[0]][playerCurrentStateCoordinations[1]] = Grid.EMPTY;
    				int pi, pj;
                    rand = new Random();
    				do {
                    	pi = rand.nextInt(9);
    	                pj = rand.nextInt(9);
    	                playerCurrentStateCoordinations[0] = pi;
    	                playerCurrentStateCoordinations[1] = pj;
                    } while ((pi == agentCurrentState.i && pj == agentCurrentState.j) ||
                    		(pi == ballCoordinates[0] && pj == ballCoordinates[1]) ||
                    		(pi == 4 && pj == 4));
    				Grid.locations[playerCurrentStateCoordinations[0]][playerCurrentStateCoordinations[1]] = (int)'P';
            	}
            	
            	Grid.locations[ballCoordinates[0]][ballCoordinates[1]] = (int)'B';
            }
            
            // while goal isn't achieved, repeat
	        boolean agentMovement = true; 
			while (!goal() && !Player.goal()) {
				
				// get agent coordinations
				int[] agentCurrentStateCoordinations = agentCurrentState.getNodeCoordinations();
				// find all possible agent actions
	        	Node[] actionsFromCurrentState = new Node[agentCurrentState.neighbors.size()];
	        	for (int z=0; z<agentCurrentState.neighbors.size(); z++) {
					int[] nodeCoordination = agentCurrentState.neighbors.get(z).getNodeCoordinations();
					actionsFromCurrentState[z] = Grid.nodes[nodeCoordination[0]][nodeCoordination[1]];
				}
	        	
	        	// get opponent's node 
				Node playerCurrentState = Grid.nodes[playerCurrentStateCoordinations[0]][playerCurrentStateCoordinations[1]];
				
	        	// get agent's position on Q array
                int qA = Grid.findNumber(agentCurrentState);
                // get opponent's position, check if he's inside the agent's scope
                int qP = checkScope(agentCurrentState, playerCurrentState.getNodeCoordinations());
                // get ball's position, check if he's inside the agent's scope
                int qB = checkScope(agentCurrentState, ballCoordinates);
                
	        	// Pick an action from the ones possible - using the explor function
            	Node agentNextState = explorGreedy(epoch, P, qA, qP, qB, actionsFromCurrentState, explor, agentMovement);
            	int[] agentNextStateCoordinations = agentNextState.getNodeCoordinations();
            	
            	// get agent's next state position
                int qANS = checkScope(agentCurrentState, agentNextStateCoordinations);
                
                // initialize opponent's movement and next state 
                boolean playerMovement = true;
        		Node playerNextState = null; 
    			int[] playerNextStateCoordinations = new int[2];
    			// initialize opponent's next state's position
                int qPNS = 0;
                
                // check if agent is allowed to move
                agentMovement = Grid.changeRewardsAndCheckMovement((int)'A', playerCurrentStateCoordinations, agentCurrentState, agentNextState);
				if (agentMovement) {
					// MOVE AGENT
					String agentOrientation = Grid.findOrientation(agentCurrentState, agentNextState);
					int agentKeyCode = Grid.stringToIntKeyCode(agentOrientation);
					Grid.makeMoveonGrid((int)'A', agentCurrentStateCoordinations, agentKeyCode);
					ballCoordinates = Grid.search((int)'B');
					// MOVE PLAYER 
					/* selecting player's (agent's opponent) next state randomly */
	                playerNextState = Player.getRandomPosition(playerCurrentState, agentNextState, ballCoordinates);
	            	playerNextStateCoordinations = playerNextState.getNodeCoordinations();
	            	// get opponent's next state position
	                qPNS = checkScope(playerCurrentState, playerNextStateCoordinations);
	                // check if player is allowed to move
	            	playerMovement = Grid.changeRewardsAndCheckMovement((int)'P', agentNextStateCoordinations, playerCurrentState, playerNextState);
	            	
					if (playerMovement) {
						String playerOrientation = Grid.findOrientation(playerCurrentState, playerNextState);
						int playerKeyCode = Grid.stringToIntKeyCode(playerOrientation);
						Grid.makeMoveonGrid((int)'P', playerCurrentStateCoordinations, playerKeyCode);
						ballCoordinates = Grid.search((int)'B');
					}
				} else if (agentMovement == false && epoch >= 190000) {
					/* since agent can't move on exploit and he has to, select the second best position */
					Node res = explorGreedy(epoch, P, qA, qP, qB, actionsFromCurrentState, explor, agentMovement);
					agentNextStateCoordinations[0] = res.i; 
					agentNextStateCoordinations[1] = res.j;
					agentNextState = Grid.nodes[agentNextStateCoordinations[0]][agentNextStateCoordinations[1]];
	                String agentOrientation = Grid.findOrientation(agentCurrentState, agentNextState);
					int agentKeyCode = Grid.stringToIntKeyCode(agentOrientation);
					
					agentMovement = Grid.changeRewardsAndCheckMovement((int)'A', playerCurrentStateCoordinations, agentCurrentState, agentNextState);
					if (agentMovement) {
						Grid.makeMoveonGrid((int)'A', agentCurrentStateCoordinations, agentKeyCode);
						ballCoordinates = Grid.search((int)'B');
						
						/* selecting player's (agent's opponent) next state randomly */
		                playerNextState = Player.getRandomPosition(playerCurrentState, agentNextState, ballCoordinates);
		            	playerNextStateCoordinations = playerNextState.getNodeCoordinations();
		            	// get opponent's next state position
		                qPNS = checkScope(playerCurrentState, playerNextStateCoordinations);
		            	playerMovement = Grid.changeRewardsAndCheckMovement((int)'P', agentNextStateCoordinations, playerCurrentState, playerNextState);
		            	
						if (playerMovement) {
							String playerOrientation = Grid.findOrientation(playerCurrentState, playerNextState);
							int playerKeyCode = Grid.stringToIntKeyCode(playerOrientation);
							Grid.makeMoveonGrid((int)'P', playerCurrentStateCoordinations, playerKeyCode);
							ballCoordinates = Grid.search((int)'B');
						}
					}
				}
				
				/* if movement is not allowed then it's not going to happen
				 * but the reward of nextState will be taken anyway in order to update the Q array value
				 * afterwards, the next state will be the current state
				 * So: find reward when the agent wants to go to nextState
                 * whether he's actually going or not - 
                 * he should get a negative reward when the movement is not allowed
                 * in order to learn that he shouldn't go there again */
                int reward = agentNextState.weight; 
                
                // get Q value from array
				double q = MinimaxQ[qA][qP][qB][qANS][qPNS];
				
                // get agent's next position on Q array
                int qAV = Grid.findNumber(agentNextState);
                // get v(s') value
                double v = V[qAV][qP][qB];
                
                // Calculate: Q(state, agentAction, opponentAction) =
                // (1 - alpha) * Q(state, agentAction, opponentAction) + alpha * (R(state, agentAction) + gamma * V(s'))
                double value = (1 - alpha) * q  + alpha * (reward + gamma * v);
                MinimaxQ[qA][qP][qB][qANS][qPNS] = value;
                
                // find minimum sum for each possible opponent state  
                double min = 10000.0;
                // for each possible opponent's action
        		for (int oa=0; oa<playerCurrentState.neighbors.size(); oa++) { // oa: opponent's action
            		double sum = 0.0;
            		// for each possible agent's action
                	for (int aa=0; aa<agentCurrentState.neighbors.size(); aa++) { // aa: agent's action
                		sum = sum + (P[qA][qP][qB][aa]*MinimaxQ[qA][qP][qB][aa][oa]);
                	}
            		if (sum < min) {
						min = sum;
					}
            	}
				V[qA][qP][qB] = min;
                
                // if movement is not allowed then it's not going to happen
                int[] oldPlayerCoordinations = new int[2];
                oldPlayerCoordinations[0] = playerCurrentStateCoordinations[0];
                oldPlayerCoordinations[1] = playerCurrentStateCoordinations[1];
                if (agentMovement) {
                	agentCurrentState = agentNextState;
                	if (playerMovement) {
                		playerCurrentState = playerNextState;
                		playerCurrentStateCoordinations[0] = playerNextStateCoordinations[0];
    					playerCurrentStateCoordinations[1] = playerNextStateCoordinations[1];
                	}
                }
                
                // reset rewards
                Grid.resetRewards(oldPlayerCoordinations, playerCurrentStateCoordinations, ballCoordinates, agentCurrentState);
			} // end while

            // decrease alpha value
            alpha = alpha - decay; // decay: 0.0000052632f
            // decrease explor value
			explor = explor - decay; // decay: 0.0000052632f
			/* alpha and explor turn zero (-0.000002719638814596692) when epoch: 189.998 */
			
			j++; // move on to the next j (switch column)
    		if (j%9 == 0 && j != 0) { // if end of column, change row and set j to zero
    			i++; // move on to the next i (switch row)
    			if (i%9 == 0) {
    				i = 0;
    			}
    			j = 0;
    		}
    		
    		// set position of agent
    		agentCoordinates[0] = i;
    		agentCoordinates[1] = j;
    		// reset ball
    		ballCoordinates[0] = 4;
    		ballCoordinates[1] = 4;
    		
    		// reset grid
    		Grid.resetGrid("testing", playerCurrentStateCoordinations, agentCoordinates);
		} // end for
        
        /***** TRAINING IS COMPLETED *****/
		
        // reset position of player 
        playerCurrentStateCoordinations[0] = 4;
        playerCurrentStateCoordinations[1] = 1;
        // reset position of player
		agentCoordinates[0] = 4;
		agentCoordinates[1] = 7;
		
		// reset grid
 		Grid.resetGrid("running", playerCurrentStateCoordinations, agentCoordinates);
    }
	
	/*************** RUNNING ***************/
	static int[] MinimaxQMove(int[] playerCoordinates, int[] agentCoordinates, int[] ballCoordinates) {
		/***** AGENT NOW EXECUTES A MOVE BASED ON EXPLOITATION OF EXPLOR FUNCTION *****/ 
		
        int[] finalStateCoordinates = new int[2];
        
		// find agent's actual current state
		Node actualCurrentState = Grid.nodes[agentCoordinates[0]][agentCoordinates[1]];
		
		// find all possible actions from current state
    	Node[] actionsFromActualCurrentState = new Node[actualCurrentState.neighbors.size()];
    	for (int z=0; z<actualCurrentState.neighbors.size(); z++) {
			int[] nodeCoordination = actualCurrentState.neighbors.get(z).getNodeCoordinations();
			actionsFromActualCurrentState[z] = Grid.nodes[nodeCoordination[0]][nodeCoordination[1]];
		}
    	
    	// find agent's actual next state
    	int qA = Grid.findNumber(actualCurrentState);
    	int qP = checkScope(actualCurrentState, playerCoordinates);
    	int qB = checkScope(actualCurrentState, ballCoordinates);
		Node actualNextState = explorGreedy(0, P, qA, qP, qB, actionsFromActualCurrentState, 0, true);
		int[] nextStateCoordinations = actualNextState.getNodeCoordinations();
    	finalStateCoordinates[0] = nextStateCoordinations[0];
        finalStateCoordinates[1] = nextStateCoordinations[1];
         
		return finalStateCoordinates;
	}
	
	/********* MINIMAX Q FUNCTIONS *********/	
	private static Node explorGreedy(int epoch, double[][][][] P, int qA, int qP, int qB, 
			Node[] actionsFromCurrentState, double explor, boolean movement) {
		double n = ThreadLocalRandom.current().nextDouble(0, 1);
		if (n < explor) { // explore
			Random rand1 = new Random();
			int index = rand1.nextInt(actionsFromCurrentState.length);
            Node nextState = actionsFromCurrentState[index];
            return nextState;
		} else { // exploit
			double[] results = maxProbability(P, qA, qP, qB);
			Node nextState = Grid.nodes[(int) results[0]][(int) results[1]];
			if (movement == true) {
				nextState = Grid.nodes[(int) results[0]][(int) results[1]];
			} else if (movement == false && epoch >= 189000) {
				/* since agent can't move on exploit and he has to, select the second best position */
				nextState = Grid.nodes[(int) results[2]][(int) results[3]];
			}
			return nextState;
		}
	}
	
	private static double[] maxProbability(double[][][][] P,  int qA, int qP, int qB) {	
		/* Using linear programming to find pi[s,.] such that:
		 * pi[s,.] = argmax{pi'[s,.], min{o', sum{a', pi[s,a'] * Q[s,a',o']}}}
		 * MEANING: to find the action which gives the maximum value for the agent 
		 * and the minimum value for the opponent 
		 * (maxmin strategy) */
		 
		// get agent's state
		int[] agentCoordinates = Grid.findPosition(qA);
		Node agentCurrentState = Grid.nodes[agentCoordinates[0]][agentCoordinates[1]];
		
		// get opponent's state
		int[] playerCoordinates = Grid.findPosition(qP);
		Node playerCurrentState = Grid.nodes[playerCoordinates[0]][playerCoordinates[1]];
		
		// initialize neighbor coordinations
    	int[] agentNeighborCoordinations = agentCurrentState.neighbors.get(0).getNodeCoordinations();
    	
    	// initialize first and second best agent's actions coordinations
    	double iNS =  agentNeighborCoordinations[0]; // i of best action (next state)
    	double jNS =  agentNeighborCoordinations[1]; // j of best action (next state)
    	double siNS =  agentNeighborCoordinations[0]; // i of second best action (second next state)
    	double sjNS =  agentNeighborCoordinations[1]; // j of second best action (second next state)
    	
    	// initialize the argMaxArray
		double[] argMaxArray = new double[agentCurrentState.neighbors.size()]; // size: [0, 6)
		
		// calculate the argMaxArray data as follows:	
		// for each of agent's next possible action
		for (int a=0; a<agentCurrentState.neighbors.size(); a++) { 
			argMaxArray[a] = 0.0;
			double[] sumArray = new double[6];
			
			// for each possible opponent's action
			for (int oa=0; oa<playerCurrentState.neighbors.size(); oa++) { // oa: opponent's action
				double sum = 0.0;
			
				// for each possible agent's action
				for (int aa=0; aa<agentCurrentState.neighbors.size(); aa++) { // aa: agent's action
					sum = sum + (P[qA][qP][qB][aa]*MinimaxQ[qA][qP][qB][aa][oa]); 
				}
				sumArray[oa] = sum;
			}
		 
			double min = 10000.0;
			for (int oa=0; oa<playerCurrentState.neighbors.size(); oa++) { // oa: opponent's action
				if (sumArray[oa] < min) {
					min = sumArray[oa];
				}
			}
			
			argMaxArray[a] = min;
		}
		
		/* now that we have the argmax data, we can find the best (max) value from that array */
	
		double max = -100000.0;
		double secondmax = -100000.0;
		int argmax = 0;
		//int secondargmax = 0;
		
		for (int a=0; a<agentCurrentState.neighbors.size(); a++) {
			if (argMaxArray[a] > max) {
				secondmax = max;
				//secondargmax = argmax;
				
				max = argMaxArray[a];
				argmax = a;
				
				/* at this point, the agentNeighborCoordinations[] contain the coordinations of the previous best neighbor
				 * aka currently the second best neighbor/action 
				 * aka agentNeighborCoordinations equals agentCurrentState.neighbors.get(secondargmax).getNodeCoordinations() */
				// set the coordinates of the second best action
				siNS =  agentNeighborCoordinations[0]; // set the i of the second best coordinate
				sjNS =  agentNeighborCoordinations[1]; // set the j of the second best coordinate
				
	        	// find current neighbor's node (variable: argmax or a)
				agentNeighborCoordinations = agentCurrentState.neighbors.get(argmax).getNodeCoordinations();
				
				// set the coordinates of the best action
				iNS =  agentNeighborCoordinations[0]; // set the i of the best coordinate
				jNS =  agentNeighborCoordinations[1]; // set the j of the best coordinate
			}
		}
		
		/* in the integer argmax variable it's the pointer of the correct action 
		 * that the agent chose that gives him the maximum reward */
		P[qA][qP][qB][argmax] = max;

		double[] result = new double[4];
		
		result[0] = iNS;
		result[1] = jNS;
		result[2] = siNS;
		result[3] = sjNS;
		
		/* if agent node has only two neighbors, then on the second max insert the second neighbor on the second max
		in order to not be the same as the first one */
		if (secondmax == -100000.0) {
			agentNeighborCoordinations = agentCurrentState.neighbors.get(1).getNodeCoordinations();
			siNS =  agentNeighborCoordinations[0]; // get i from coordinates
			sjNS =  agentNeighborCoordinations[1]; // get j from coordinates
			result[2] = siNS;
			result[3] = sjNS;
		}
				
		return result;
	}	
}