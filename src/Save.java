import java.util.HashMap;

public class Save implements java.io.Serializable {
    private HashMap<String, Room> roomMap;
    private HashMap<String, Enemy> enemyMap;
    private Inventory inventory;
    private Room currentRoom;
    private Room pastRoom;
    private Player player;
    private String music;
    private boolean isInTrial;
    private boolean hasAnsweredNewsQuestions;
    private boolean hasOpenedVault;
    private boolean supportCheck;
    private int trial;

    public Save(HashMap<String, Room> roomMap, Inventory inventory, Room currentRoom, Room pastRoom, Player player, HashMap<String, Enemy> enemyMap, String music, boolean isInTrial, boolean hasAnsweredNewsQuestions, boolean hasOpenedVault, boolean supportCheck, int trial){
        this.roomMap = roomMap;
        this.inventory = inventory;
        this.currentRoom = currentRoom;
        this.pastRoom = pastRoom;
        this.player = player;
        this.enemyMap = enemyMap;
        this.music = music;
        this.isInTrial = isInTrial;
        this.hasAnsweredNewsQuestions = hasAnsweredNewsQuestions;
        this.hasOpenedVault = hasOpenedVault;
        this.supportCheck = supportCheck;
        this.trial = trial;
    }

    public int getTrial(){
        return trial;
    }

    public boolean getHasOpenedVault() {
        return hasOpenedVault;
    }

    public boolean getSupportCheck() {
        return supportCheck;
    }

    public boolean getHasAnsweredNewsQuestions() {
        return hasAnsweredNewsQuestions;
    }

    public boolean getIsInTrial() {
        return isInTrial;
    }

    public String getMusic() {
        return music;
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
