import java.io.Serializable;
import java.util.HashMap;

public class Save implements Serializable {
    private HashMap<String, Room> roomMap;
    private HashMap<String, Enemy> enemyMap;
    private Inventory inventory;
    private Room currentRoom;
    private boolean inProgress;
    private Player player;

    public Save(){
        inProgress = false;
    }

    
    public Save(HashMap<String, Room> roomMap, Inventory inventory, Room currentRoom, Player player, HashMap<String, Enemy> enemyMap){
        this.roomMap = roomMap;
        this.inventory = inventory;
        this.currentRoom = currentRoom;
        this.player = player;
        this.enemyMap = enemyMap;
        inProgress = true;
    }

    public HashMap<String, Room> getRoomMap() {
        return roomMap;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public Room getCurrentRoom() {
        return currentRoom;
    }

    public boolean getInProgress(){
        return inProgress;
    }

    public Player getPlayer() {
        return player;
    }
    
    public HashMap<String, Enemy> getEnemyMap() {
        return enemyMap;
    }
}
