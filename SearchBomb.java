import static com.orbischallenge.bombman.api.game.MapItems.*;
import com.orbischallenge.bombman.api.game.MapItems;
import com.orbischallenge.bombman.api.game.PlayerAction;
import com.orbischallenge.bombman.api.game.PowerUps;
import java.awt.Point;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 **
 * 
 */
public class SearchBomb {

	Bomb bomb;
	
	Point location;

	int timeToExplosion;
	
	boolean traversed;
	
	public SearchBomb(Bomb bomb, Point location) {
		this.bomb = bomb;
		this.location = location;
		this.timeToExplosion = bomb.getTimeleft();
		traversed = false;
	}
	
}