import java.util.ArrayList;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class Room implements java.io.Serializable {
  private String roomName;
  private String description;
  private ArrayList<Exit> exits;
  private ArrayList<Item> items;

  public ArrayList<Exit> getExits() {
    return exits;
  }

  public void setExits(ArrayList<Exit> exits) {
    this.exits = exits;
  }

  /**
   * Create a room described "description". Initially, it has no exits.
   * "description" is something like "a kitchen" or "an open court yard".
   */
  public Room(String description) {
    this.description = description;
    exits = new ArrayList<Exit>();
  }

  public Room() {
    roomName = "DEFAULT ROOM";
    description = "DEFAULT DESCRIPTION";
    exits = new ArrayList<Exit>();
  }
  
  /**
   * Initializes the items for the current room.
   */
  public void initItems() {
    items = new ArrayList<Item>();
    for (Object itemObj : Item.getItems()) {
      if (((JSONObject) itemObj).get("startingRoom").equals(roomName)){
        Object weight = ((JSONObject) itemObj).get("weight");
        String name = (String) ((JSONObject) itemObj).get("name");
        boolean isOpenable = (boolean) ((JSONObject) itemObj).get("isOpenable");
        String description = (String) ((JSONObject) itemObj).get("description");
        ArrayList<String> aliases = new ArrayList<String>();
        for (Object alias : (JSONArray) ((JSONObject) itemObj).get("aliases")) {
          aliases.add((String) alias);
        }
        Item item = new Item(Integer.parseInt(weight + ""), name, isOpenable, description, aliases);
        items.add(item);
      }
      }
  }

  public boolean containsItem(String item) {
    for (Item itemObj : items) {
      if (itemObj.getName().equalsIgnoreCase(item)) return true;
      for (String alias : itemObj.getAliases()) {
        if (alias.equalsIgnoreCase(item)) return true;
      }
    }

    return false;
  }

  public void addExit(Exit exit) throws Exception {
    exits.add(exit);
  }

  /**
   * Return the description of the room (the one that was defined in the
   * constructor).
   */
  public String shortDescription() {
    return "Room: " + roomName + "\n\n" + description;
  }

  /**
   * Return a long description of this room, on the form: You are in the kitchen.
   * Exits: north west
   */
  public String longDescription() {
    return "Room: " + roomName + "\n\n" + description + "\n" + exitString();
  }

  /**
   * Return a string describing the room's exits.
   * ".
   */
  public String exitString() {
    ArrayList<String> exitStrings = new ArrayList<String>();
    for (Exit exit : exits) {
        exitStrings.add(exit.getDirection());
    }
    return "Exits: " + String.join(", ", exitStrings);
  }

  /**
   * Returns whether or not you can go in the direction specified.
   * @param direction - The direction to go.
   * @throws IllegalArgumentException if the direction is not valid.
   */
  public boolean canGoDirection(String direction) {
    for (Exit exit : exits) {
      if (exit.getDirection().equalsIgnoreCase(direction)) {
        return !exit.isLocked();
      }
    }
    throw new IllegalArgumentException(direction + " is not a valid direction.");
  }

  /**
   * Return the room that is reached if we go from this room in direction
   * "direction". If there is no room in that direction, return null.
   */
  public Room nextRoom(String direction){
    for (Exit exit : exits) {
      if (exit.getDirection().equalsIgnoreCase(direction)) {
        String adjacentRoom = exit.getAdjacentRoom();

        return Game.roomMap.get(adjacentRoom);
      }
    }
    return null;
  }

  /**
   * CHecks if the item specified exists in this room.
   * If it does not exist, it throws an {@code IllegalArgumentException}.
   * @param itemName - the String of the item name to compare to.
   * @return If found, the Item.
   * @throws IllegalArgumentException If the item does not exist.
   */
  public Item getItem(String itemName){
    for (Item item : items){
      if (item.getName().equalsIgnoreCase(itemName)) return item;
      for (String alias : item.getAliases()) {
        if (alias.equalsIgnoreCase(itemName)) return item;
      }
    }
    throw new IllegalArgumentException("Item not found in this room.");
  }

  public ArrayList<Item> getItems(){
    return items;
  }

  public void setItems(ArrayList<Item> items){
    this.items = items;
  }


  /*
   * private int getDirectionIndex(String direction) { int dirIndex = 0; for
   * (String dir : directions) { if (dir.equals(direction)) return dirIndex; else
   * dirIndex++; }
   * 
   * throw new IllegalArgumentException("Invalid Direction"); }
   */
  public String getRoomName() {
    return roomName;
  }

  public void setRoomName(String roomName) {
    this.roomName = roomName;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * Removes an item from the items list for a room.
   * @param itemName - The item name to remove.
   * @throws IllegalArgumentException if the item is not found in the room.
   */
  public void removeItem(String itemName) {
    for (int i = 0; i < items.size(); i++) {
      if (items.get(i).getName().equalsIgnoreCase(itemName)){
        items.remove(i);
        return;
      }
      for (String alias : items.get(i).getAliases()) {
        if (alias.equalsIgnoreCase(itemName)){
          items.remove(i);
          return;
        }
      }
    }
    throw new IllegalArgumentException("Item not found in this room.");
  }
}
