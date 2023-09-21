package game;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.util.Random;
import java.util.Scanner;
import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import game.Node.Neighbor;

public class Grid {

	// constants and global variables
	final static Color COLORBACK = Color.WHITE;
	final static Color COLORCELL = new Color(83,130,52); // green	 
	final static Color COLORGRID = Color.BLACK;
	final static Color COLORGOAL = new Color(0,0,0,200); // black
	final static Color COLORPLAYERS = new Color(197,91,17,200); // orange
	final static Color COLORBALL = new Color(51,62,80); // blue
	final static Color COLORTXT = Color.WHITE;
	final static int EMPTY = 0;
	final static int BSIZE = 9; // board size
	final static int HEXSIZE = 80; // hex height size in pixels
	final static int BORDERS = 15;  
	final static int SCRSIZE = HEXSIZE * (BSIZE + 1) + BORDERS*3; // screen size (vertical dimension)
	
	static int algorithm;
	static boolean gameOver = false;
	static int[][] locations = new int[BSIZE][BSIZE];
	static Node[][] nodes = new Node[BSIZE][BSIZE]; // all location nodes with neighbors
	Graphics2D g2;
	
	private Grid() {
		initGame();
		createAndShowGUI();
	}
	
	void initGame() {
		HexMech.setXYasVertex(false); // initialize this as false

		HexMech.setHeight(HEXSIZE); // setHeight must be run to initialize the hex
		HexMech.setBorders(BORDERS);
	}
	
	private void createAndShowGUI() {
		DrawingPanel panel = new DrawingPanel();
		
		JFrame frame = new JFrame("Adversarial Game against Reinforcement Learning Agent"); // new window
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // exit button
		Container content = frame.getContentPane();
		content.add(panel);
		// for hexes in the FLAT orientation, the height of a 10x10 grid is 1.1764 * the width. (from h / (s+t))
		frame.setSize((int)(SCRSIZE/1.23), SCRSIZE); // size of the window
		frame.setResizable(false);
		frame.setLocationRelativeTo(null);
		frame.setFocusable(true); // you can click on it
		frame.setVisible(true); // window is now visible
		
		keyListener(frame);
	}
	
	private void keyListener(JFrame frame) {
		
		frame.addKeyListener(new KeyListener() {
			
			@Override
			public void keyPressed(KeyEvent e) {
				int keyCode = e.getKeyCode();
				boolean player = (keyCode==KeyEvent.VK_W) || (keyCode==KeyEvent.VK_S) 
						|| (keyCode==KeyEvent.VK_Q) || (keyCode==KeyEvent.VK_E) 
						|| (keyCode==KeyEvent.VK_A) || (keyCode==KeyEvent.VK_D);
				if (player) {
					// Player's turn
					int playerCoordinates[] = search((int)'P'); // find player position on grid
					int[] newPlayerCoordinates = new int[3];
					if ((playerCoordinates[0] == 0 && (keyCode == KeyEvent.VK_Q || keyCode == KeyEvent.VK_A)) 
							|| (playerCoordinates[0] == 8 && (keyCode == KeyEvent.VK_E || keyCode == KeyEvent.VK_D))
							
							|| (playerCoordinates[1] == 0 && playerCoordinates[0]%2 == 0 && (keyCode == KeyEvent.VK_Q || keyCode == KeyEvent.VK_W || keyCode == KeyEvent.VK_E))
							|| (playerCoordinates[1] == 0 && playerCoordinates[0]%2 == 1 && keyCode == KeyEvent.VK_W)
							
							|| (playerCoordinates[1] == 8 && playerCoordinates[0]%2 == 0 && keyCode == KeyEvent.VK_S)
							|| (playerCoordinates[1] == 8 && playerCoordinates[0]%2 == 1 && (keyCode == KeyEvent.VK_A || keyCode == KeyEvent.VK_S || keyCode == KeyEvent.VK_D))
					) {
						System.err.println("Out of bounds, cannot move player -- Try again.");
					} else {
						newPlayerCoordinates = makeMoveonGrid((int)'P', playerCoordinates, keyCode); // move player to new position
						HexMech.fillHex(newPlayerCoordinates[0], newPlayerCoordinates[1], locations[newPlayerCoordinates[0]][newPlayerCoordinates[1]], g2);
						frame.repaint();
						printGrid();
						
						if (Player.goal() || Agent.goal()) {
							gameOver = true;
							System.out.println("GAME OVER");
							if (Player.goal()) {
								System.out.println("USER scores!!!");
							} else {
								System.out.println("AGENT scores!!!");
							}
							System.exit(0);
						}
						
						// check if there is a conflict
						if(newPlayerCoordinates[0] == playerCoordinates[0] 
								&& newPlayerCoordinates[1] == playerCoordinates[1]) {
							if (newPlayerCoordinates[2] == 0) {
								System.err.println("Player cannot move into new location -- Agent is there.");	
							} else if (newPlayerCoordinates[2] == 1) {
								System.err.println("Player cannot move into new location -- Ball is there and unable to move.\n"
										+ "Cannot kick ball into new location -- Agent is there.");
							}
						} else {
							// if there isn't a conflict, its agent's turn
							System.out.println("Agent's turn.");
							int agentCoordinates[] = search((int)'A'); // find agent position on grid
							int ballCoordinates[] = search((int)'B'); // find ball position on grid
							
							// erase agent's actual location temporarily, in order to go in algorithm's training mode
							locations[agentCoordinates[0]][agentCoordinates[1]] = EMPTY;
							
							int[] newAgentCoordinates = new int[3];
							int newPossibleAgentCoordinates[] = new int[2];
							int agentKeyCode;
							
							if (algorithm == 1) {
								newPossibleAgentCoordinates = Agent.QMove(newPlayerCoordinates, agentCoordinates, ballCoordinates);
							} else if (algorithm == 2) {
								newPossibleAgentCoordinates = Agent.MinimaxQMove(newPlayerCoordinates, agentCoordinates, ballCoordinates);
							}
							
							// find keyCode for agent
							agentKeyCode = 0;
							String agentOrientation = findOrientation(nodes[agentCoordinates[0]][agentCoordinates[1]], nodes[newPossibleAgentCoordinates[0]][newPossibleAgentCoordinates[1]]);
							agentKeyCode = stringToIntKeyCode(agentOrientation);
								
							newAgentCoordinates = makeMoveonGrid((int)'A', agentCoordinates, agentKeyCode); // move agent to new position

							// check if there is a conflict
							if(newAgentCoordinates[0] == agentCoordinates[0] 
									&& newAgentCoordinates[1] == agentCoordinates[1]) {
								locations[newAgentCoordinates[0]][newAgentCoordinates[1]] = (int)'A';
								if (newAgentCoordinates[2] == 0) {
									System.err.println("Agent cannot move into new location -- Player is there.");	
								} else if (newAgentCoordinates[2] == 1) {
									System.err.println("Agent cannot move into new location -- Ball is there and unable to move.\n"
											+ "Cannot kick ball into new location -- Player is there.");
								}
							}
							
							printGrid();
							HexMech.fillHex(newAgentCoordinates[0], newAgentCoordinates[1], locations[newAgentCoordinates[0]][newAgentCoordinates[1]], g2); // for Q
							frame.repaint();
							
							if (!Agent.goal() && !(locations[3][0] == (int)'A' || locations[4][1] == (int)'A' || locations[5][0] == (int)'A')) {
								System.out.println("Player's turn.");	
							}
						}
					}
					
					if (Player.goal() || Agent.goal()) {
						gameOver = true;
						System.out.println("GAME OVER");
						if (Player.goal()) {
							System.out.println("USER scores!!!");
						} else {
							System.out.println("AGENT scores!!!");
						}
						System.exit(0);
					}
				} else { // invalid button
					System.err.println("Invalid button, cannot move player -- Try again.");
				}
			}
	
			@Override
			public void keyReleased(KeyEvent e) {
				// TODO Auto-generated method stub
			}

			@Override
			public void keyTyped(KeyEvent e) {
				// TODO Auto-generated method stub
			}
		});
	}

	@SuppressWarnings("serial")
	class DrawingPanel extends JPanel
	{		
		public DrawingPanel() {	
			setBackground(COLORBACK);
		}

		public void paintComponent(Graphics g) {
			g2 = (Graphics2D)g;
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setFont(new Font("TimesRoman", Font.PLAIN, 20));
			super.paintComponent(g2);
			
			// draw grid
			for (int i=0; i<BSIZE; i++) {
				for (int j=0; j<BSIZE; j++) {
					HexMech.drawHex(i, j, g2);
				}
			}
			
			// fill in hexes
			for (int i=0; i<BSIZE; i++) {
				for (int j=0; j<BSIZE; j++) {
					HexMech.fillHex(i, j, locations[i][j], g2);
				}
			}
		}	  
	}
	
	@SuppressWarnings("null")
	static int[] makeMoveonGrid(int decimal, int[] coordinates, int keyCode) {
		
		int[] newCoordinates = new int[3];
		newCoordinates[2] = 2;
		String orientation = null;
		
		int decimalOpponent = 0;
		if (decimal == 80) {
			decimalOpponent = 65;
		} else if (decimal == 65) {
			decimalOpponent = 80;
		}
		
		// calculate new location based on its keyCode 
		switch(keyCode) {
			case KeyEvent.VK_W: // up
				newCoordinates[0] = coordinates[0]; // row stays the same
				newCoordinates[1] = coordinates[1] - 1; // change column
				orientation = "UP";
				break;
			case KeyEvent.VK_S: // down
				newCoordinates[0] = coordinates[0]; // row stays the same
				newCoordinates[1] = coordinates[1] + 1; // change column
				orientation = "DOWN";
				break;
			case KeyEvent.VK_Q: // upper left
				if(coordinates[0]%2 == 1) {
					// only change the row
					newCoordinates[0] = coordinates[0] - 1;
					newCoordinates[1] = coordinates[1];
				} else {
					newCoordinates[0] = coordinates[0] - 1;
					newCoordinates[1] = coordinates[1] - 1;
					// change row and column
				}
				orientation = "UPPER LEFT";
				break;
			case KeyEvent.VK_E: // upper right
				if(coordinates[0]%2 == 1) {
					newCoordinates[0] = coordinates[0] + 1;
					newCoordinates[1] = coordinates[1];
					// only change the row
				} else {
					newCoordinates[0] = coordinates[0] + 1;
					newCoordinates[1] = coordinates[1] - 1;
					// change row and column
				}
				orientation = "UPPER RIGHT";
				break;
			case KeyEvent.VK_A: // lower left
				if(coordinates[0]%2 == 1) {
					newCoordinates[0] = coordinates[0] - 1;
					newCoordinates[1] = coordinates[1] + 1;
					// change row and column
				} else {
					newCoordinates[0] = coordinates[0] - 1;
					newCoordinates[1] = coordinates[1];
					// only change the row
				}
				orientation = "LOWER LEFT";
				break;
			case KeyEvent.VK_D: // lower right
				if(coordinates[0]%2 == 1) {
					newCoordinates[0] = coordinates[0] + 1;
					newCoordinates[1] = coordinates[1] + 1;
					// change row and column
				} else {
					newCoordinates[0] = coordinates[0] + 1;
					newCoordinates[1] = coordinates[1];
					// only change the row
				}
				orientation = "LOWER RIGHT";
				break;
			case 0:
				orientation = "NOWHERE";
				break;
		}
		
		if (locations[newCoordinates[0]][newCoordinates[1]] == EMPTY) {
			locations[coordinates[0]][coordinates[1]] = EMPTY; // since the player is moving, empty its previous location
			if (decimal == (int)'P') {
				locations[newCoordinates[0]][newCoordinates[1]] = (int)'P';	
			} else if (decimal == (int)'A') {
				locations[newCoordinates[0]][newCoordinates[1]] = (int)'A';	
			}	
		} else if (locations[newCoordinates[0]][newCoordinates[1]] == decimalOpponent) {
			// don't let the move happen, opponent is there
			// instead, keep player's old coordinates
			newCoordinates[0] = coordinates[0];
			newCoordinates[1] = coordinates[1];
			newCoordinates[2] = 0; // for player to print system.err
		} else if (locations[newCoordinates[0]][newCoordinates[1]] == (int)'B') {
			// the player kicks the ball, ball is there (at the location where the player is supposed to go)
			// move ball if only there's not a collision with the opponent
			int[] finalPlayerCoordinates = kickBall(coordinates, newCoordinates, orientation, decimal, decimalOpponent);
			newCoordinates[0] = finalPlayerCoordinates[0];
			newCoordinates[1] = finalPlayerCoordinates[1];
			newCoordinates[2] = finalPlayerCoordinates[2]; // for ball to print system.err
		}
		
		return newCoordinates;
	}
	
	static int[] kickBall(int[] oldPlayerCoordinates, int[] newPlayerCoordinates, String orientation, 
			int decimal, int decimalOpponent) {
		/* since the player is moving, empty its previous location
		 * also, move the ball since the player kicked it
		 * if only there is no collision with the position of its opponent */

		int[] finalPlayerCoordinates = new int[3];
		finalPlayerCoordinates[2] = 0;
		int[] ballCoordinates = new int[2];
		
		// calculate new ball location based on player's orientation 
		switch(orientation) {
			case "UP":
				ballCoordinates[0] = newPlayerCoordinates[0]; // row stays the same
				ballCoordinates[1] = newPlayerCoordinates[1] - 1; // change column
				break;
			case "DOWN":
				ballCoordinates[0] = newPlayerCoordinates[0]; // row stays the same
				ballCoordinates[1] = newPlayerCoordinates[1] + 1; // change column
				break;
			case "UPPER LEFT":
				if(newPlayerCoordinates[0]%2 == 1) {
					// only change the row
					ballCoordinates[0] = newPlayerCoordinates[0] - 1;
					ballCoordinates[1] = newPlayerCoordinates[1];
				} else {
					ballCoordinates[0] = newPlayerCoordinates[0] - 1;
					ballCoordinates[1] = newPlayerCoordinates[1] - 1;
					// change row and column
				}
				break;
			case "UPPER RIGHT":
				if(newPlayerCoordinates[0]%2 == 1) {
					ballCoordinates[0] = newPlayerCoordinates[0] + 1;
					ballCoordinates[1] = newPlayerCoordinates[1];
					// only change the row
				} else {
					ballCoordinates[0] = newPlayerCoordinates[0] + 1;
					ballCoordinates[1] = newPlayerCoordinates[1] - 1;
					// change row and column
				}
				break;
			case "LOWER LEFT":
				if(newPlayerCoordinates[0]%2 == 1) {
					ballCoordinates[0] = newPlayerCoordinates[0] - 1;
					ballCoordinates[1] = newPlayerCoordinates[1] + 1;
					// change row and column
				} else {
					ballCoordinates[0] = newPlayerCoordinates[0] - 1;
					ballCoordinates[1] = newPlayerCoordinates[1];
					// only change the row
				}
				break;
			case "LOWER RIGHT":
				if(newPlayerCoordinates[0]%2 == 1) {
					ballCoordinates[0] = newPlayerCoordinates[0] + 1;
					ballCoordinates[1] = newPlayerCoordinates[1] + 1;
					// change row and column
				} else {
					ballCoordinates[0] = newPlayerCoordinates[0] + 1;
					ballCoordinates[1] = newPlayerCoordinates[1];
					// only change the row
				}
				break;
		}
		
		// if ball's coordinates are out of bounds, find new coordinates for the ball
		if (ballCoordinates[0] == -1 || ballCoordinates[0] == 9 
				|| ballCoordinates[1] == -1 || ballCoordinates[1] == 9) {
			ballCoordinates = ballOutOfBounds(oldPlayerCoordinates, newPlayerCoordinates);
		}
		
		finalPlayerCoordinates[0] = newPlayerCoordinates[0];
		finalPlayerCoordinates[1] = newPlayerCoordinates[1];
		
		if (locations[ballCoordinates[0]][ballCoordinates[1]] == decimalOpponent) { // if there's collision with the opponent, movement cannot be made
			finalPlayerCoordinates[0] = oldPlayerCoordinates[0];
			finalPlayerCoordinates[1] = oldPlayerCoordinates[1];
			finalPlayerCoordinates[2] = 1;
		} else { // if there's no collision with the opponent
			locations[oldPlayerCoordinates[0]][oldPlayerCoordinates[1]] = EMPTY; 
			locations[ballCoordinates[0]][ballCoordinates[1]] = (int)'B';
			if (decimal == (int)'A') {
				/* old ball's position/new player's position reward 
				 * set to 10 because it's now a neighbor */
				Node newPlayer = nodes[newPlayerCoordinates[0]][newPlayerCoordinates[1]];
				if (newPlayer != nodes[4][7] && newPlayer != nodes[4][8]
						&& newPlayer != nodes[3][7] && newPlayer != nodes[3][8]
						&& newPlayer != nodes[5][7] && newPlayer != nodes[5][8]
						&& newPlayer != nodes[4][0]) {
					nodes[newPlayerCoordinates[0]][newPlayerCoordinates[1]].setWeight(10); 
				}
				
				// find ball & ball's new neighbors and set the reward to 70 and 10
				// also set the previous ball's neighbors reward to -1 since the ball moved
				for (Neighbor n : nodes[newPlayerCoordinates[0]][newPlayerCoordinates[1]].getNeighbors()) {
					if (n.node != nodes[4][0] && n.node != nodes[4][8]
							&& n.node != nodes[3][7] && n.node != nodes[3][8]
							&& n.node != nodes[5][7] && n.node != nodes[5][8]
							&& n.node != nodes[4][7]) {
						n.node.setWeight(-1); // old ball's neighbors reward set back to -1	
					}
				}
				for (Neighbor n : nodes[ballCoordinates[0]][ballCoordinates[1]].getNeighbors()) {
					if (n.node != nodes[4][0] && n.node != nodes[4][8]
							&& n.node != nodes[3][7] && n.node != nodes[3][8]
							&& n.node != nodes[5][7] && n.node != nodes[5][8]
							&& n.node != nodes[4][7]) {
						n.node.setWeight(10); // new ball's neighbors reward set to 10
					}
				}
				
				// new ball's position reward set to 70
				Node newBall = nodes[ballCoordinates[0]][ballCoordinates[1]];
				if (newBall != nodes[4][7] && newBall != nodes[4][8]
						&& newBall != nodes[3][7] && newBall != nodes[3][8]
						&& newBall != nodes[5][7] && newBall != nodes[5][8]
						&& newBall != nodes[4][0]) {
					nodes[ballCoordinates[0]][ballCoordinates[1]].setWeight(70); 
				}
				
				// if ball is about to go in goal, agent should get reward 100
				if (ballCoordinates[0] == 4 && ballCoordinates[1] == 0) {
					nodes[newPlayerCoordinates[0]][newPlayerCoordinates[1]].setWeight(100);
				}
				locations[newPlayerCoordinates[0]][newPlayerCoordinates[1]] = (int)'A';
				
			} else if (decimal == (int)'P') {
				// old ball's position reward set to 10 because it's now a neighbor
				nodes[newPlayerCoordinates[0]][newPlayerCoordinates[1]].setWeight(10); 
				
				// find ball & ball's new neighbors and set the reward to 70 and 10
				// also set the previous ball's neighbors reward to -1 since the ball moved
				for (Neighbor n : nodes[newPlayerCoordinates[0]][newPlayerCoordinates[1]].getNeighbors()) {
					if (n.node != nodes[4][0] && n.node != nodes[4][8]) {
						n.node.setWeight(-1); // old ball's neighbors reward set back to -1	
					}
				}
				for (Neighbor n : nodes[ballCoordinates[0]][ballCoordinates[1]].getNeighbors()) {
					if (n.node != nodes[4][0] && n.node != nodes[4][8]) {
						n.node.setWeight(10); // new ball's neighbors reward set to 10
					}
				}
				
				// new ball's position reward set to 70
				nodes[ballCoordinates[0]][ballCoordinates[1]].setWeight(70); 
				locations[ballCoordinates[0]][ballCoordinates[1]] = (int)'B';
				// if ball is about to go in goal, goal's reward should be 100
				if (ballCoordinates[0] == 4 && ballCoordinates[1] == 0) {
					nodes[4][0].setWeight(100);
				}
				
				locations[newPlayerCoordinates[0]][newPlayerCoordinates[1]] = (int)'P';
			}
		}
		
		return finalPlayerCoordinates;
	}

	private static int[] ballOutOfBounds(int[] oldPlayerCoordinates, int[] oldBallCoordinates) {
		
		int[] newBallCoordinates = new int[2];
		
		// left grid border 
		if (oldBallCoordinates[0] == 0 && 
				(oldBallCoordinates[1] >= 1 && oldBallCoordinates[1] <= 7)) {
			if (oldPlayerCoordinates[0] == oldBallCoordinates[0] + 1 && 
					oldPlayerCoordinates[1] == oldBallCoordinates[1] - 1) { // if player upper right
				// ball goes lower right
				newBallCoordinates[0] = oldBallCoordinates[0] + 1;
				newBallCoordinates[1] = oldBallCoordinates[1];
			} else if (oldPlayerCoordinates[0] == oldBallCoordinates[0] + 1 && 
					oldPlayerCoordinates[1] == oldBallCoordinates[1]) { // if player lower right
				// ball goes upper right
				newBallCoordinates[0] = oldBallCoordinates[0] + 1;
				newBallCoordinates[1] = oldBallCoordinates[1] - 1;
			}
		}
		
		// right grid border 
		if (oldBallCoordinates[0] == 8 && 
				(oldBallCoordinates[1] >= 1 && oldBallCoordinates[1] <= 7)) {
			if (oldPlayerCoordinates[0] == oldBallCoordinates[0] - 1 && 
					oldPlayerCoordinates[1] == oldBallCoordinates[1] - 1) { // if player upper left
				// ball goes lower left
				newBallCoordinates[0] = oldBallCoordinates[0] - 1;
				newBallCoordinates[1] = oldBallCoordinates[1];
			} else if (oldPlayerCoordinates[0] == oldBallCoordinates[0] - 1 && 
					oldPlayerCoordinates[1] == oldBallCoordinates[1]) { // if player lower left
				// ball goes upper left
				newBallCoordinates[0] = oldBallCoordinates[0] - 1;
				newBallCoordinates[1] = oldBallCoordinates[1] - 1;
			}
		}
		
		// upper grid border
		if (oldBallCoordinates[1] == 0 && 
				(oldBallCoordinates[0] >= 1 && oldBallCoordinates[0] <= 7)) {
			if (oldBallCoordinates[0]%2 == 0) {
				if (oldPlayerCoordinates[0] == oldBallCoordinates[0] - 1 && 
						oldPlayerCoordinates[1] == oldBallCoordinates[1]) { // if player lower left
					// ball goes lower right
					newBallCoordinates[0] = oldBallCoordinates[0] + 1;
					newBallCoordinates[1] = oldBallCoordinates[1];
				} else if (oldPlayerCoordinates[0] == oldBallCoordinates[0] + 1 && 
						oldPlayerCoordinates[1] == oldBallCoordinates[1]) { // if player lower right
					// ball goes lower left
					newBallCoordinates[0] = oldBallCoordinates[0] - 1;
					newBallCoordinates[1] = oldBallCoordinates[1];
				}
			}
			if (oldPlayerCoordinates[0] == oldBallCoordinates[0] && 
					oldPlayerCoordinates[1] == oldBallCoordinates[1] + 1) { // if player down
				// ball goes down
				newBallCoordinates[0] = oldPlayerCoordinates[0];
				newBallCoordinates[1] = oldPlayerCoordinates[1];
			}
		}
		
		// lower grid border
		if (oldBallCoordinates[1] == 8 && 
				(oldBallCoordinates[0] >= 1 && oldBallCoordinates[0] <= 7)) {
			if (oldBallCoordinates[0]%2 == 1) { // good
				if (oldPlayerCoordinates[0] == oldBallCoordinates[0] - 1 && 
						oldPlayerCoordinates[1] == oldBallCoordinates[1]) { // if player upper left
					// ball goes upper right
					newBallCoordinates[0] = oldBallCoordinates[0] + 1;
					newBallCoordinates[1] = oldBallCoordinates[1];
				} else if (oldPlayerCoordinates[0] == oldBallCoordinates[0] + 1 && 
						oldPlayerCoordinates[1] == oldBallCoordinates[1]) { // if player upper right
					// ball goes upper left
					newBallCoordinates[0] = oldBallCoordinates[0] - 1;
					newBallCoordinates[1] = oldBallCoordinates[1];
				}
			}
			if (oldPlayerCoordinates[0] == oldBallCoordinates[0] && 
					oldPlayerCoordinates[1] == oldBallCoordinates[1] - 1) { // if player up
				// ball goes up
				newBallCoordinates[0] = oldPlayerCoordinates[0];
				newBallCoordinates[1] = oldPlayerCoordinates[1];
			}
		}
		
		// corners grid border
		if (oldBallCoordinates[0] == 0 && oldBallCoordinates[1] == 0) {
			newBallCoordinates[0] = 1;
			newBallCoordinates[1] = 0;
		} else if (oldBallCoordinates[0] == 8 && oldBallCoordinates[1] == 0) {
			newBallCoordinates[0] = 7;
			newBallCoordinates[1] = 0;
		} else if (oldBallCoordinates[0] == 0 && oldBallCoordinates[1] == 8) {
			newBallCoordinates[0] = 1;
			newBallCoordinates[1] = 7;
		} else if (oldBallCoordinates[0] == 8 && oldBallCoordinates[1] == 8) {
			newBallCoordinates[0] = 7;
			newBallCoordinates[1] = 7;
		}
		
		return newBallCoordinates;
	}
		
	static boolean changeRewardsAndCheckMovement(int decimal, int[] playerCoordinations, Node agentCurrentState, Node agentNextState) {
		// there are four categories: 
		// A. agent can't move because player is on the position that agent wants to move into (player being in its way)
		// B. agent can't move because it cannot kick the ball because player is on the other cell (where the ball would go)
		// C. agent can move to the desired state it wishes in order to kick the ball
		// D. agent can move to where it wants as a simple transition
		
		boolean movement = false;
		
		// get player state
		Node playerState = nodes[playerCoordinations[0]][playerCoordinations[1]];
		// get ball state
		int[] ballCoordinations = Grid.search('B'); // ball's position will change when the agent kicks it during training
		Node ballState = nodes[ballCoordinations[0]][ballCoordinations[1]];
		
		// get current to next state orientation
		String agentOrientation = findOrientation(agentCurrentState, agentNextState);
		// get agent to player orientation if they are neighbors, if they're not value is null
		String agentToPlayerOrientation = findOrientation(agentCurrentState, playerState);
		
		
		/***** CATEGORY A *****/
		/* check if player is interfering with agent, when agent wants to go to the player's position
		 * (check if agent cannot move due to player being in its way) */
		if (agentNextState == nodes[playerCoordinations[0]][playerCoordinations[1]]) {
			if (decimal==(int)'A' && agentNextState.weight != -100) {
				agentNextState.setWeight(-10);
			}
		} 
			
		
		/***** CATEGORY B *****/
		// check if agent and ball are neighbors, if they are the orientation wouldn't be null 
		String agentToBallOrientation = findOrientation(agentCurrentState, ballState);
		// check if ball and player are neighbors, if they are the orientation wouldn't be null
		String ballToPlayerOrientation = findOrientation(ballState, playerState);
		
		/* check if player is in the same direction that the agent wants to move to, 
		 * when the ball is in between them 
		 * if they have the same orientation and it is not null: 
		 * it means that the agent, ball and player are in sequential nodes/cells 
		 * and there will be a collision because player blocks the agent from kicking the ball (reward: -10).
		 * for that reason, we need to reduce the reward that the agent would get if it were to step into the cell that the ball is */
		if (agentToBallOrientation == ballToPlayerOrientation 
				&& agentToBallOrientation == agentOrientation && agentToBallOrientation != null) {
			if (decimal==(int)'A' && agentNextState.weight != -100) {
				agentNextState.setWeight(-10);
			}
		} else {
			// if ball's new coordinates are out of bounds, find new coordinates for the ball
			int[] oobBallCoordinations = new int[2];
			if (agentToBallOrientation != null && agentToBallOrientation == agentOrientation) {
				oobBallCoordinations = orientationToCoordinations(ballCoordinations, agentToBallOrientation);
				if (oobBallCoordinations[0] == -1 || oobBallCoordinations[0] == 9 
						|| oobBallCoordinations[1] == -1 || oobBallCoordinations[1] == 9) {
					int[] oldAgentCoordinations = agentCurrentState.getNodeCoordinations();
					int[] newAgentCoordinations = agentNextState.getNodeCoordinations();
					ballCoordinations = ballOutOfBounds(oldAgentCoordinations, newAgentCoordinations);
					if (ballCoordinations[0] == playerCoordinations[0] 
							&& ballCoordinations[1] == playerCoordinations[1]) {
						if (decimal==(int)'A' && agentNextState.weight != -100) {
							agentNextState.setWeight(-10);
						}
					} else {
						movement = true;
					}
				} else {
					/***** CATEGORY C *****/
					/* check if agent is neighbor with the ball and if player doesn't stop agent from kicking the ball
					 * then, the cell in which the ball exists should have a 70 reward in order to attract the agent to kick it */
					if (agentToBallOrientation != null && agentToBallOrientation == agentOrientation && agentToBallOrientation != ballToPlayerOrientation) {
						movement = true;
						if (agentNextState.weight != -100) {
							agentNextState.setWeight(70);
						}
					}
				}
			}
		}
		
		
		/***** CATEGORY D *****/ 
		/* check if agent can move to next state as a simple transition:
		 * without kicking the ball (the ball is not its neighbor) and
		 * without having the player in its way (the player is not its neighbor) */
		if (locations[agentNextState.i][agentNextState.j] == EMPTY) {
			movement = true;
		}
		
		String newAgentToPlayerOrientation = findOrientation(agentNextState, playerState);
		if (agentToPlayerOrientation != null && newAgentToPlayerOrientation == null) {
			agentCurrentState.setWeight(-1);
		}
		
		return movement;
	}

	private static int[] orientationToCoordinations(int[] coordinates, String orientation) {
		/***** receives the String orientation and translates that 
		 * into the coordinates it will result in *****/
		
		int[] newCoordinates = new int[2];
		
		// calculate new ball location based on player's orientation 
		switch(orientation) {
			case "UP":
				newCoordinates[0] = coordinates[0]; // row stays the same
				newCoordinates[1] = coordinates[1] - 1; // change column
				break;
			case "DOWN":
				newCoordinates[0] = coordinates[0]; // row stays the same
				newCoordinates[1] = coordinates[1] + 1; // change column
				break;
			case "UPPER LEFT":
				if(coordinates[0]%2 == 1) {
					// only change the row
					newCoordinates[0] = coordinates[0] - 1;
					newCoordinates[1] = coordinates[1];
				} else {
					newCoordinates[0] = coordinates[0] - 1;
					newCoordinates[1] = coordinates[1] - 1;
					// change row and column
				}
				break;
			case "UPPER RIGHT":
				if(coordinates[0]%2 == 1) {
					newCoordinates[0] = coordinates[0] + 1;
					newCoordinates[1] = coordinates[1];
					// only change the row
				} else {
					newCoordinates[0] = coordinates[0] + 1;
					newCoordinates[1] = coordinates[1] - 1;
					// change row and column
				}
				break;
			case "LOWER LEFT":
				if(coordinates[0]%2 == 1) {
					newCoordinates[0] = coordinates[0] - 1;
					newCoordinates[1] = coordinates[1] + 1;
					// change row and column
				} else {
					newCoordinates[0] = coordinates[0] - 1;
					newCoordinates[1] = coordinates[1];
					// only change the row
				}
				break;
			case "LOWER RIGHT":
				if(coordinates[0]%2 == 1) {
					newCoordinates[0] = coordinates[0] + 1;
					newCoordinates[1] = coordinates[1] + 1;
					// change row and column
				} else {
					newCoordinates[0] = coordinates[0] + 1;
					newCoordinates[1] = coordinates[1];
					// only change the row
				}
				break;
			default:
				newCoordinates[0] = coordinates[0];
				newCoordinates[1] = coordinates[1];
				break;
		}
		return newCoordinates;
	}

	static int stringToIntKeyCode(String agentOrientation) {
		/***** receives the String orientation and translates that 
		 * into the integer key code it is *****/
		
		int keyCode = 0;
		switch(agentOrientation) {
			case "UP": // up
				keyCode = KeyEvent.VK_W;
				break;
			case "DOWN": // down
				keyCode = KeyEvent.VK_S;
				break;
			case "UPPER LEFT": // upper left
				keyCode = KeyEvent.VK_Q;
				break;
			case "UPPER RIGHT": // upper right
				keyCode = KeyEvent.VK_E;
				break;
			case "LOWER LEFT": // lower left
				keyCode = KeyEvent.VK_A;
				break;
			case "LOWER RIGHT": // lower right
				keyCode = KeyEvent.VK_D;
				break;
		}
		return keyCode;
	}

	static String findOrientation(Node currentState, Node nextState) {
		/***** find orientation for agent's move *****/
		
		String orientation = null;
		if (currentState.i == nextState.i 
				&& currentState.j == nextState.j + 1) {
			orientation = "UP"; // up
		} else if (currentState.i == nextState.i 
				&& currentState.j == nextState.j - 1) {
			orientation = "DOWN"; // down
		} else if ((currentState.i%2 == 1 && currentState.i - 1 == nextState.i 
				&& currentState.j == nextState.j) 
				|| (currentState.i%2 == 0 && currentState.i - 1 == nextState.i 
				&& currentState.j - 1 == nextState.j)) {
			orientation = "UPPER LEFT"; // upper left
		} else if ((currentState.i%2 == 1 && currentState.i + 1 == nextState.i 
				&& currentState.j == nextState.j) 
				|| (currentState.i%2 == 0 && currentState.i + 1 == nextState.i
				&& currentState.j - 1 == nextState.j)) {
			orientation = "UPPER RIGHT"; // upper right
		} else if ((currentState.i%2 == 1 && currentState.i - 1 == nextState.i 
				&& currentState.j + 1 == nextState.j) 
				|| (currentState.i%2 == 0 && currentState.i - 1 == nextState.i 
				&& currentState.j == nextState.j)) {
			orientation = "LOWER LEFT"; // lower left
		} else if ((currentState.i%2 == 1 && currentState.i + 1 == nextState.i
				&& currentState.j + 1 == nextState.j) 
				|| (currentState.i%2 == 0 && currentState.i + 1 == nextState.i 
				&& currentState.j == nextState.j)) {
			orientation = "LOWER RIGHT"; // lower right
		}
		
		return orientation;
	}
	
	public static int[] findPosition(int number) {
		/***** receives the integer number of the position number of [0,81] 
		 * that's the total of environment cells 
		 * and translates that into the coordinations of the cell 
		 * ex. number/cell 12 has the coordinates i=1, j=3 *****/
		
		int[] position = new int[2];
		int i = 0, j = 0;
		
		for (int k=0; k<81; k++) {
			if (number == k) {
				position[0] = i;
				position[1] = j;
			}
			j++; // move on to the next j (switch column)
    		if (j%9 == 0 && j != 0) { // if end of column, change row and set j to zero
    			i++; // move on to the next i (switch row)  
    			j = 0;
    		}
		}
		
		return position;
	}
	
	public static int findNumber(Node node) {
		/***** receives a node and translates that into the position number of the cell [0,81] 
		 * that's the total of environment cells *****/
		
		int number = -1;
		int i = 0, j = 0;
		int ni = node.i;
		int nj = node.j;
		
		for (int k=0; k<81; k++) {
			if (i == ni && j == nj) {
				number = k;
			}
			j++; // move on to the next j (switch column)
    		if (j%9 == 0 && j != 0) { // if end of column, change row and set j to zero
    			i++; // move on to the next i (switch row)  
    			j = 0;
    		}
		}
		
		return number;
	}
	
	public static int[] search(int decimal) {
		/***** finds the position of a specific decimal 
		 * ex. finds the position of the agent on grid and returns its coordinates *****/
		
		int coordinates[] = new int[2];
		coordinates[0] = -1;
		coordinates[1] = -1;
		int k = 0;
		
		for (int i=0; i<BSIZE; i++) {
			for (int j=0; j<BSIZE; j++) {
				if (locations[i][j] == decimal) {
					coordinates[0] = i;
					coordinates[1] = j;
					k=1;
					break;
				}
			}
			if (k==1) {
				break;
			}	
		}
		return coordinates;
	}
	
	static void printGrid() {
		/***** prints the grid and whoever is in it *****/
		
		for (int i=0; i<BSIZE; i++) {
			for (int j=0; j<BSIZE; j++) {
				System.out.print("\t" + locations[j][i] + "(" + nodes[j][i].weight + ")");
			}
			System.out.println();
		}
		System.out.println();
	}
	
	private static void firstLocations() throws IllegalArgumentException, IllegalAccessException {
		/***** initializes locations, list of neighbors for each node and sets weights *****/
		
		for (int i=0; i<BSIZE; i++) {
			for (int j=0; j<BSIZE; j++) {
				locations[i][j] = EMPTY;
				nodes[i][j] = new Node(i, j, -1); // i,j coordinations and weight
			}
		}	
		
		locations[4][1] = (int)'P';
		locations[4][4] = (int)'B';
		locations[4][7] = (int)'A';

		// create neighbors for each node, add them clockwise
		for (int i=0; i<BSIZE; i++) {
			for (int j=0; j<BSIZE; j++) {
				// up
				if (j-1>=0) {
					nodes[i][j].addNeighbor(nodes[i][j-1]);
				}
				
				// upper right
				if(i%2 == 1) {
					// only change the row
					if (i+1<BSIZE) {
						nodes[i][j].addNeighbor(nodes[i+1][j]);	
					}
				} else {
					// change row and column
					if (i+1<BSIZE && j-1>=0) {
						nodes[i][j].addNeighbor(nodes[i+1][j-1]);
					}
				}
				
				// lower right
				if(i%2 == 1) {
					// change row and column
					if (i+1<BSIZE && j+1<BSIZE) {
						nodes[i][j].addNeighbor(nodes[i+1][j+1]);
					}
				} else {
					// only change the row
					if (i+1<BSIZE) {
						nodes[i][j].addNeighbor(nodes[i+1][j]);
					}
				}
				
				// down
				if (j+1<BSIZE) {
					nodes[i][j].addNeighbor(nodes[i][j+1]);	
				}
				
				// lower left
				if(i%2 == 1) {
					// change row and column
					if (i-1>=0 && j+1<BSIZE) {
						nodes[i][j].addNeighbor(nodes[i-1][j+1]);
					}
				} else {
					// only change the row
					if (i-1>=0) {
						nodes[i][j].addNeighbor(nodes[i-1][j]);	
					}
				}
				
				// upper left
				if(i%2 == 1) {
					// only change the row
					if (i-1>=0) {
						nodes[i][j].addNeighbor(nodes[i-1][j]);
					}
				} else {
					// change row and column
					if (i-1>=0 && j-1>=0) {
						nodes[i][j].addNeighbor(nodes[i-1][j-1]);
					}
				}
			}
		}

		// change weight at goal cells
		// because they have value -1 instead of 100/-100
		//nodes[4][0].setWeight(100);
		nodes[3][7].setWeight(-100);
		nodes[3][8].setWeight(-100);
		nodes[4][7].setWeight(-100);
		nodes[4][8].setWeight(-100);
		nodes[5][7].setWeight(-100);
		nodes[5][8].setWeight(-100);
		
		// change weight around the ball
		// in order for agent to get close to it
		nodes[4][4].setWeight(70);
		nodes[3][3].setWeight(10);
		nodes[3][4].setWeight(10);
		nodes[4][3].setWeight(10);
		nodes[4][5].setWeight(10);
		nodes[5][3].setWeight(10);
		nodes[5][4].setWeight(10);
	}
	
	public static void main(String[] args) throws IllegalArgumentException, IllegalAccessException {
		// initializing the locations first
		firstLocations();
		
		Scanner scanner = new Scanner(System.in);
		
		// menu print
		System.out.println("Please Select an Algorithm (Enter Number):\n"
				+ "1. Q Learning Algorithm\n"
				+ "2. Minimax Q Learning Algorithm\n"
				+ "3. EXIT");
		do {
			algorithm = scanner.nextInt();
			if (algorithm < 1 || algorithm > 3) {
				System.err.println("Wrong Input - Try again.");
			}
		} while (algorithm < 1 || algorithm > 3);
		
		// initializing coordinates
		int[] playerCoordinates = new int[2];
		playerCoordinates[0] = 4;
		playerCoordinates[1] = 1;
		int[] agentCoordinates = new int[2];
		agentCoordinates[0] = 4;
		agentCoordinates[1] = 7;
		int[] ballCoordinates = new int[2];
		ballCoordinates[0] = 4;
		ballCoordinates[1] = 4;
		
		if (algorithm == 1) {
			System.out.println("Q LEARNING ALGORITHM");
			// erase agent's actual location temporarily, in order to go in algorithm's training mode
			locations[agentCoordinates[0]][agentCoordinates[1]] = EMPTY;
			System.out.println("Training agent...");
			Agent.calculateQ(playerCoordinates, agentCoordinates, ballCoordinates);
			System.out.println("Training completed!\n");
		} else if (algorithm == 2) {
			System.out.println("MINIMAX Q LEARNING ALGORITHM");
			// erase agent's actual location temporarily, in order to go in algorithm's training mode
			locations[agentCoordinates[0]][agentCoordinates[1]] = EMPTY;
			System.out.println("Training agent...");
			Agent.calculateMinimaxQ(playerCoordinates, agentCoordinates, ballCoordinates);
			System.out.println("Training completed!\n");
		}
		
		if (algorithm != 3) {
			System.out.println("GAME IS NOW ON!\nMay the best one win.\n\nStarting State:");
			printGrid();
			System.out.println("Player starts.");
			SwingUtilities.invokeLater(new Runnable() {
				public void run() { // game loop 
					new Grid();
				}
			});
		} else {
			System.out.println("OKAY, BYE");
		}
	}
	
	public static void resetRewards(int[] oldplayer, int[] player, int[] ball, Node agent) {
		// get ball state
		Node ballState = nodes[ball[0]][ball[1]];
		
		// ball reward 
		nodes[ball[0]][ball[1]].setWeight(70);
				
		// agent's goal reward
		nodes[4][8].setWeight(-100);
		nodes[4][7].setWeight(-100);
		nodes[3][7].setWeight(-100);
		nodes[3][8].setWeight(-100);
		nodes[5][7].setWeight(-100);
		nodes[5][8].setWeight(-100);
		
		// get player's old and new states
		Node oldPlayerState = nodes[oldplayer[0]][oldplayer[1]];
		Node playerState = nodes[player[0]][player[1]];
		/* if player moved then change the rewards of its previous and new position */
		if (oldplayer[0] != player[0] || oldplayer[1] != player[1]) {
			
			// reset reward of its previous position
			if (nodes[oldplayer[0]][oldplayer[1]] != nodes[3][7]
					&& nodes[oldplayer[0]][oldplayer[1]] != nodes[3][8]
					&& nodes[oldplayer[0]][oldplayer[1]] != nodes[4][7]
					&& nodes[oldplayer[0]][oldplayer[1]] != nodes[4][8]
					&& nodes[oldplayer[0]][oldplayer[1]] != nodes[5][7]
					&& nodes[oldplayer[0]][oldplayer[1]] != nodes[5][8]) {
				
				nodes[oldplayer[0]][oldplayer[1]].setWeight(-1);
				boolean isNeighbor = false;
				for(Neighbor n : ballState.getNeighbors()) {
					if(n.node.equals(oldPlayerState)) {
						isNeighbor = true;
					}
				}
				if (isNeighbor) {
					nodes[oldplayer[0]][oldplayer[1]].setWeight(10);
				} else {
					nodes[oldplayer[0]][oldplayer[1]].setWeight(-1);
				}
			} else {
				nodes[oldplayer[0]][oldplayer[1]].setWeight(-100);
				nodes[player[0]][player[1]].setWeight(-100);
			}
			
			// reset reward of its new position
			if (nodes[player[0]][player[1]] != nodes[3][7]
					&& nodes[player[0]][player[1]] != nodes[3][8]
					&& nodes[player[0]][player[1]] != nodes[4][7]
					&& nodes[player[0]][player[1]] != nodes[4][8]
					&& nodes[player[0]][player[1]] != nodes[5][7]
					&& nodes[player[0]][player[1]] != nodes[5][8]) {
				
				boolean isNeighbor = false;
				for(Neighbor n : ballState.getNeighbors()) {
					if(n.node.equals(playerState)) {
						isNeighbor = true;
					}
				}
				if (isNeighbor) {
					nodes[player[0]][player[1]].setWeight(10);
				} else {
					nodes[player[0]][player[1]].setWeight(-1);
				}
			} else {
				nodes[player[0]][player[1]].setWeight(-100);
			}
		}
			
		// player's goal reward
		nodes[4][0].setWeight(-1);
	}

	public static void resetGrid(String phase, int[] playerCoordinates, int[] agentCoordinates) {
		for (int i=0; i<BSIZE; i++) {
			for (int j=0; j<BSIZE; j++) {
				locations[i][j] = EMPTY;
				nodes[i][j].setWeight(-1); // reset weight back to -1
			}
		}
		
		// reset position of agent
		locations[agentCoordinates[0]][agentCoordinates[1]] = (int)'A';
		
		// reset position of ball
		locations[4][4] = (int)'B';
		// reset weight around the ball
		nodes[4][4].setWeight(70);
		for (Neighbor n : nodes[4][4].getNeighbors()) {
			if (!(n.node.i == 4 && (n.node.j == 0 || n.node.j == 8))) {
				n.node.setWeight(10); // ball's neighbors reward set to 10
			}
		}
		
		// reset position of player
		if (phase.contentEquals("testing")) {
			
			// get player's node 
			Node playerCurrentState = Grid.nodes[playerCoordinates[0]][playerCoordinates[1]];
	        
			/* if player's current position gets in conflict 
			 * with either the agent's position or the ball's position,
			 * find a random position for the player that is conflict-free.
			 */
			if (playerCurrentState == Grid.nodes[agentCoordinates[0]][agentCoordinates[1]]
					|| playerCurrentState == Grid.nodes[4][4]) {
	        	int pi, pj;
				Random rand = new Random();
				do {
	            	pi = rand.nextInt(9);
	                pj = rand.nextInt(9);
	                playerCoordinates[0] = pi;
	                playerCoordinates[1] = pj;
	            } while ((pi == agentCoordinates[0] && pj == agentCoordinates[1]) ||
	            		(pi == 4 && pj == 4));
			}
			locations[playerCoordinates[0]][playerCoordinates[1]] = (int)'P';
		} else if (phase.contentEquals("running")) {
			locations[playerCoordinates[0]][playerCoordinates[1]] = (int)'P';
		}
		
		// reset penalty for agent's goal
		nodes[3][7].setWeight(-100);
		nodes[3][8].setWeight(-100);
		nodes[4][7].setWeight(-100);
		nodes[4][8].setWeight(-100);
		nodes[5][7].setWeight(-100);
		nodes[5][8].setWeight(-100);
	}
}