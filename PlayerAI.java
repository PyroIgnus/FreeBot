
import static com.orbischallenge.bombman.api.game.MapItems.*;

import com.orbischallenge.bombman.api.game.MapItems;
import com.orbischallenge.bombman.api.game.PlayerAction;
import com.orbischallenge.bombman.api.game.PowerUps;

import java.awt.Point;
import java.util.HashMap;
import java.util.Map;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Comparator;
import java.util.Arrays;

import javax.swing.plaf.basic.BasicInternalFrameTitlePane.MoveAction;

/**
 *
 * @author c.sham
 */
public class PlayerAI implements Player {

    List<Point> allBlocks;
    
    /**
     * The map of times until an explosion will occur.
     *
     */
    int[][] bombMap;
    
    Point[][] pathingBuffer;
    
    int phase = 0; // Phase 0 = Mining, Phase 1 = Evasive

    /**
     * Gets called every time a new game starts.
     *
     * @param map The map.
     * @param blocks All the blocks on the map.
     * @param players Current position, bomb range, and bomb count for both Bombers.
     * @param playerIndex Your player index.
     */
    @Override
    public void newGame(MapItems[][] map, List<Point> blocks, Bomber[] players, int playerIndex) {
        allBlocks = blocks;
        bombMap = new int[map.length][map[0].length];
        pathingBuffer = new Point[map.length][map[0].length];
        phase = 0;
    }

    /**
     * Gets called every time a move is requested from the game server.
     *
     * Provided is a very random and not smart AI which random moves without checking for
     * explosions, and places bombs whenever bombs can be used to destroy blocks.
     *
     * @param map The current map
     * @param bombLocations Bombs currently on the map and it's range, owner and time left. Exploding
     * bombs are excluded.
     * @param powerUpLocations Power-ups current on the map and it's type
     * @param players Current position, bomb range, and bomb count for both Bombers
     * @param explosionLocations Explosions currently on the map.
     * @param playerIndex Your player index.
     * @param moveNumber The current move number.
     * @return the PlayerAction you want your Bomber to perform.
     */
    @Override
    public PlayerAction getMove(MapItems[][] map, HashMap<Point, Bomb> bombLocations,
    		HashMap<Point, PowerUps> powerUpLocations, Bomber[] players,
    		List<Point> explosionLocations, int playerIndex, int moveNumber) {

        boolean bombMove = false;
        /**
         * Get Bomber's current position
         */
        Point curPosition = players[playerIndex].position;
        int enemyIndex = 0;
        if (playerIndex == 1) {
        	enemyIndex = 0;
        }
        else {
        	enemyIndex = 1;
        }
        Point enemyPosition = players[enemyIndex].position;

        /**
         * Keep track of which blocks are destroyed
         */
        for (Point explosions : explosionLocations) {
            if (allBlocks.contains(explosions)) {
                allBlocks.remove(explosions);
            }
        }

        // Creates bomb map.
        int bombCount = bombLocations.size();
        SearchBomb searchBombs[] = new SearchBomb[bombCount];
        int currentBombIndex = 0;
        for (Map.Entry<Point, Bomb> entry : bombLocations.entrySet()) {
        	searchBombs[currentBombIndex] = new SearchBomb(entry.getValue(), entry.getKey());
        	currentBombIndex++;
        }
        
        generateBombMap(map, bombLocations, searchBombs, bombMap);
        
        Move.Direction move = Move.still;	// Default don't move.
        
        // Phases (Mining or Full Evasive)
        // Mine (destroy nearby blocks and collect power ups).
        if (phase == 0) {
        	if (isThereAPath(curPosition, enemyPosition, map)) {
        		//phase = 1;
        	}
        	// Look for power ups within reach first.
        	Point directionToNearestPowerup = pathToNearestPowerup(curPosition, map, bombMap);
        	if (directionToNearestPowerup != null) {
        		move = Move.getDirection(directionToNearestPowerup.x, directionToNearestPowerup.y);
        	}
        	else {
	        	// Look for nearest block to destroy if there are no powerups to pick up.
	        	Point directionToNearestBlock = pathToNearestBlock(curPosition, map, bombMap);
	        	if (directionToNearestBlock != null && directionToNearestBlock.x != 0 && directionToNearestBlock.y != 0) {
	        		move = Move.getDirection(directionToNearestBlock.x, directionToNearestBlock.y);
	        	}
	        	// If next to block, evaluate safety of dropping a bomb at this location.
	        	// Create a hypothetical bomb map of placing a bomb here and ensure there is a safe path out.
	        	else {
		        	int[][] bombMapCopy = new int[map.length][map[0].length];
		        	SearchBomb searchBombsCopy[] = new SearchBomb[bombCount + 1];
		        	for (int i = 0; i < bombCount; i++) {
		        		searchBombsCopy[i] = searchBombs[i];
		        	}
		        	Bomb b = new Bomb(playerIndex, players[playerIndex].bombRange, 14);
		        	SearchBomb hBomb = new SearchBomb(b, curPosition);
		        	searchBombsCopy[bombCount] = hBomb;
		        	HashMap<Point, Bomb> bombLocationsCopy = (HashMap<Point, Bomb>)bombLocations.clone();
		        	bombLocationsCopy.put(curPosition, b);
		        	generateBombMap(map, bombLocationsCopy, searchBombsCopy, bombMapCopy);
		        	// If safe, then place bomb.
		        	Point directionToSafeSpaceHyp = pathToSafeSpace(curPosition, map, bombMapCopy);
		        	if (directionToSafeSpaceHyp != null) {
		        		// Drop bomb.
		        		bombMove = true;
		        	}
	        	}
	        	
        	}
        	
        } 
        else if (phase == 1) {
        	// Start evasive maneuvers once a path between the players is made.
        	
        }
        
        // Determines safe path based on bomb map(evades).
        Point directionToSafeSpace = pathToSafeSpace(curPosition, map, bombMap);
        
        if (directionToSafeSpace != null) {
        	if (directionToSafeSpace.x != 0 || directionToSafeSpace.y != 0) {
        		// Override any other movement if under threat.
        		move = Move.getDirection(directionToSafeSpace.x, directionToSafeSpace.y);
        	}
        }
        
        /**
         * Find which neighbours of Bomber's current position are currently unoccupied, so that I
         * can move into. Also counts how many blocks are neighbours.
         */
        /*LinkedList<Move.Direction> validMoves = new LinkedList<>();
        LinkedList<Move.Direction> blocks = new LinkedList<>();

        for (Move.Direction move : Move.getAllMovingMoves()) {
            int x = curPosition.x + move.dx;
            int y = curPosition.y + move.dy;

            if (map[x][y].isWalkable()) {
                validMoves.add(move);
            }
            if (allBlocks.contains(new Point(x, y))) {
                blocks.add(move);
            }
        } */

        /**
         * If there are blocks around, I should place a bomb in my current square.
         */
        /*if (!blocks.isEmpty()) {
            bombMove = false;
        } */

        /**
         * There's no place to go, I'm stuck. :(
         */
       /* if (validMoves.isEmpty()) {
            return Move.still.action;
        } */

        /**
         * There is some place I could go, so I randomly choose one direction and go off in that
         * direction.
         */
       // Move.Direction move = validMoves.get((int) (Math.random() * validMoves.size()));

        if (bombMove) {
            return move.bombaction;
        }
        return move.action;

    }

    /**
     * Uses the pathingBuffer.
     *
     * 
     */
    public Point pathToSafeSpace(Point start, MapItems[][] map, int[][] bombMap) {
    	resetPathingBuffer(pathingBuffer);
    	
    	Queue<Point> open = new LinkedList();
    	open.add(start);
    	
    	while (!open.isEmpty()) {
    		Point currentPoint = open.remove();
    		    		
    		if (bombMap[currentPoint.x][currentPoint.y] == 99) {
    			
    			Point previous = null;
    			for (Point current = currentPoint; 
    					current != start;
    					previous = current, current = pathingBuffer[current.x][current.y]) {
    				
    			}
    			
    			if (previous == null) {
    				return new Point(0, 0);
    			} else {
    				Point dP = new Point(previous.x - start.x, previous.y - start.y);
    				return dP;
    			}
    		}
    		
    		for (Move.Direction direction : Move.getAllMovingMoves()) {
    			int x = currentPoint.x + direction.dx;
    			int y = currentPoint.y + direction.dy;
    			
    			Point neighbour = new Point(x, y);
    			
    			if (map[x][y].isWalkable() && map[x][y] != MapItems.EXPLOSION && pathingBuffer[x][y] == null) {
    				open.add(neighbour);
        			pathingBuffer[x][y] = currentPoint;
    			}
    			
    		}
    	
    	}
    	
    	return null;
    }
    
    public Point pathToNearestBlock(Point start, MapItems[][] map, int[][] bombMap) {
    	resetPathingBuffer(pathingBuffer);
    	
    	Queue<Point> open = new LinkedList();
    	open.add(start);
    	
    	while (!open.isEmpty()) {
    		Point currentPoint = open.remove();
    		    		
    		if (map[currentPoint.x][currentPoint.y] == MapItems.BLOCK) {
    			
    			Point previous = null;
    			for (Point current = currentPoint; 
    					current != start;
    					previous = current, current = pathingBuffer[current.x][current.y]) {
    				
    			}
    			
    			if (previous == null) {
    				return new Point(0, 0);
    			} else {
    				Point dP = new Point(previous.x - start.x, previous.y - start.y);
    				return dP;
    			}
    		}
    		
    		for (Move.Direction direction : Move.getAllMovingMoves()) {
    			int x = currentPoint.x + direction.dx;
    			int y = currentPoint.y + direction.dy;
    			
    			Point neighbour = new Point(x, y);
    			
    			if (map[x][y] != MapItems.BOMB && map[x][y] != MapItems.WALL && map[x][y] != MapItems.EXPLOSION
    					&& pathingBuffer[x][y] == null) {
    				open.add(neighbour);
        			pathingBuffer[x][y] = currentPoint;
    			}
    			
    		}
    	
    	}
    	
    	return null;
    }
    
    public Point pathToNearestPowerup(Point start, MapItems[][] map, int[][] bombMap) {
    	resetPathingBuffer(pathingBuffer);
    	
    	Queue<Point> open = new LinkedList();
    	open.add(start);
    	
    	while (!open.isEmpty()) {
    		Point currentPoint = open.remove();
    		    		
    		if (map[currentPoint.x][currentPoint.y] == MapItems.POWERUP) {
    			
    			Point previous = null;
    			for (Point current = currentPoint; 
    					current != start;
    					previous = current, current = pathingBuffer[current.x][current.y]) {
    				
    			}
    			
    			if (previous == null) {
    				return new Point(0, 0);
    			} else {
    				Point dP = new Point(previous.x - start.x, previous.y - start.y);
    				return dP;
    			}
    		}
    		
    		for (Move.Direction direction : Move.getAllMovingMoves()) {
    			int x = currentPoint.x + direction.dx;
    			int y = currentPoint.y + direction.dy;
    			
    			Point neighbour = new Point(x, y);
    			
    			if (map[x][y].isWalkable() && map[x][y] != MapItems.EXPLOSION && pathingBuffer[x][y] == null) {
    				open.add(neighbour);
        			pathingBuffer[x][y] = currentPoint;
    			}
    			
    		}
    	
    	}
    	
    	return null;
    }
    
    /**
     * Uses Breadth First Search to find if a walkable path from point A to point B exists.
     *
     * This method does not consider the if tiles are dangerous or not. As long as all the tiles in
     * are walkable.
     *
     * @param start The starting point
     * @param end The end point
     * @param map The map use to check if a path exists between point A and point B
     * @return True if there is a walkable path between point A and point B, False otherwise.
     */
    public boolean isThereAPath(Point start, Point end, MapItems[][] map) {
        //Keeps track of points we have to check
        Queue<Point> open = new LinkedList<>();

        //Keeps track of points we have already visited
        List<Point> visited = new LinkedList<>();

        open.add(start);
        while (!open.isEmpty()) {
            Point curPoint = open.remove();

            //Check all the neighbours of the current point in question
            for (Move.Direction direction : Move.getAllMovingMoves()) {
                int x = curPoint.x + direction.dx;
                int y = curPoint.y + direction.dy;

                Point neighbour = new Point(x, y);

                // if the point is the destination, we are done
                if (end.equals(neighbour)) {
                    return true;
                }

                // if we have already visited this point, we skip it
                if (visited.contains(neighbour)) {
                    continue;
                }

                // if bombers can walk onto this point, then we add it to the list of points we should check
                if (map[x][y].isWalkable()) {
                    open.add(neighbour);
                }

                // add to visited so we don't check it again
                visited.add(neighbour);
            }
        }
        return false;
    }
    
    /***
     * Note: That the pathingBuffer is reset and repurposed on each call.
     *
     * Based strongly on isThereAPath(...).
     */
    public Point pathTo(Point start, Point end, MapItems[][] map) {
    	resetPathingBuffer(pathingBuffer);
    	
    	Queue<Point> open = new LinkedList();
    	open.add(start);
    	
    	while (!open.isEmpty()) {
    		Point currentPoint = open.remove();
    		    		
    		if (currentPoint == end) {
    			
    			Point previous = null;
    			for (Point current = end; 
    					current != start;
    					previous = current, current = pathingBuffer[current.x][current.y]) {
    				
    			}
    			
    			if (previous == null) {
    				return new Point(0, 0);
    			} else {
    				return new Point(previous.x - start.x, previous.y - start.y);
    			}
    		}
    		
    		for (Move.Direction direction : Move.getAllMovingMoves()) {
    			int x = currentPoint.x + direction.dx;
    			int y = currentPoint.y + direction.dy;
    			
    			Point neighbour = new Point(x, y);
    			
    			if (map[x][y].isWalkable() && map[x][y] != MapItems.EXPLOSION && pathingBuffer[x][y] == null) {
    				open.add(neighbour);
    			}
    			
    			pathingBuffer[x][y] = currentPoint;
    		}
    	
    	}
    	
    	return null;
    }
    

    /**
     * Returns the Manhattan Distance between the two points.
     *
     * @param start the starting point
     * @param end the end point
     * @return the Manhattan Distance between the two points.
     */
    public int manhattanDistance(Point start, Point end) {
        return (Math.abs(start.x - end.x) + Math.abs(start.y - end.y));
    }
    
    Move.Direction directionToMove(Point direction) {
    	if (direction.x == 1) {
    		return Move.right;
    	} else if (direction.y == -1) {
    		return Move.up;
    	} else if (direction.x == -1) {
    		return Move.left;
    	} else if (direction.y == 1) {
    		return Move.down;
    	} else {
    		return Move.still;
    	}
    }
    
    Point changePointElement(Point p, int axis, int newValue) {
    	Point pPrime = (Point)p.clone();
    	if (axis == 0) {
    		pPrime.x = newValue;
    	} else {
    		pPrime.y = newValue;
    	}
    	
    	return pPrime;
    }
    
    int getPointElement(Point p, int axis) {
    	if (axis == 0) {
    		return p.x;
    	} else {
    		return p.y;
    	}
    }
    
    void printBombMap(int[][] bombMap) {
    	for (int i = 0; i < bombMap.length; i++) {
    		for (int j = 0; j < bombMap[0].length; j++) {
    			System.out.print (bombMap[i][j] + "   ");
    		}
    		System.out.println("");
    	}
    }
    
    void resetBombMap(int[][] bombMap, int defaultValue) {
    	for (int i = 0; i < bombMap.length; i++) {
    		for (int j = 0; j < bombMap[0].length; j++) {
    			bombMap[i][j] = defaultValue;
    		}
    	}
    }
    
    void resetPathingBuffer(Point[][] buffer) {
    	for (int i = 0; i < buffer.length; i++) {
    		for (int j = 0; j < buffer[0].length; j++) {
    			buffer[i][j] = null;
    		}
    	}
    }
    
    void spreadExplosion(MapItems[][] items, HashMap<Point, Bomb> bombHash, SearchBomb[] bombs,
    		int[][] bombMap, boolean[] blocked, SearchBomb bomb, Point p, int direction) {
    	if (!blocked[direction]) {
			bombMap[p.x][p.y] = Math.min(bombMap[p.x][p.y], bomb.timeToExplosion);
			if (items[p.x][p.y] == MapItems.WALL || items[p.x][p.y] == MapItems.BLOCK) {
				blocked[direction] = true;
			}
			
			if (items[p.x][p.y] == MapItems.BOMB) {
				SearchBomb foundBomb = null;
				for (int i = 0; i < bombs.length; i++) {
					if (bombs[i].location.equals(p)) {
						foundBomb = bombs[i];
					}
				}
				foundBomb.timeToExplosion = bomb.timeToExplosion;
				generateBombMap(items, bombHash, bombs, bombMap, foundBomb);
			}
		}
    }
    
    void generateBombMap(MapItems[][] items, HashMap<Point, Bomb> bombHash, SearchBomb[] bombs,
    		int[][] bombMap, SearchBomb bomb) {
    	if (!bomb.traversed) {
			bomb.traversed = true;
    		
    		for (int axis = 0; axis < 2; axis++) {
    			boolean[] blocked = {false, false};
    			
    			for (int distance = 0; distance <= bomb.bomb.getRange(); distance++) {
    				int p1DAxis = getPointElement(bomb.location, axis) + distance;
    				int p2DAxis = getPointElement(bomb.location, axis) - distance;
    				Point p1 = changePointElement(bomb.location, axis, p1DAxis);
    				Point p2 = changePointElement(bomb.location, axis, p2DAxis);
    				
    				spreadExplosion(items, bombHash, bombs, bombMap, blocked, bomb, p1, 0);
    				spreadExplosion(items, bombHash, bombs, bombMap, blocked, bomb, p2, 1);
    			}
    		}
		}
    }
    
    void generateBombMap(MapItems[][] items, HashMap<Point, Bomb> bombHash, SearchBomb[] bombs,
    		int[][] bombMap) {
    	Arrays.sort(bombs, new Comparator<SearchBomb>() {
    	    public int compare(SearchBomb a, SearchBomb b) {
    	        return Integer.compare(a.timeToExplosion, b.timeToExplosion);
    	    }
    	});
    	
    	resetBombMap(bombMap, 99);
    	
    	for (int i = 0; i < bombs.length; i++) {
    		generateBombMap(items, bombHash, bombs, bombMap, bombs[i]);
    	}
    
    }
}
