import java.util.HashMap;

public class Save implements java.io.Serializable {
    private HashMap<String, Room> roomMap;
    private HashMap<String, Enemy> enemyMap;
    private Inventory inventory;
    private Room currentRoom;
    private Room pastRoom;
    private Player player;

    public Save(HashMap<String, Room> roomMap, Inventory inventory, Room currentRoom, Room pastRoom, Player player, HashMap<String, Enemy> enemyMap){
        this.roomMap = roomMap;
        this.inventory = inventory;
        this.currentRoom = currentRoom;
        this.pastRoom = pastRoom;
        this.player = player;
        this.enemyMap = enemyMap;
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

    public Room getPastRoom() {
        return pastRoom;
    }

    public Player getPlayer() {
        return player;
    }
    
    public HashMap<String, Enemy> getEnemyMap() {
        return enemyMap;
    }
}
