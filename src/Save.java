import java.io.Serializable;
import java.util.HashMap;

public class Save implements Serializable{
    private HashMap<String, Room> roomMap;
    private Inventory inventory;
    private Room currentRoom;
    private boolean inProgress;

    public Save(){
        inProgress = false;
    }

    public Save(HashMap<String, Room> roomMap, Inventory inventory, Room currentRoom){
        this.roomMap = roomMap;
        this.inventory = inventory;
        this.currentRoom = currentRoom;
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
}
