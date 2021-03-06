import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Item extends OpenableObject implements java.io.Serializable {
    private int weight;
    private String name;
    private String description;
    private boolean isTakeable;
    private boolean isWeapon;
    private boolean isDroppable;
    private int damage = 0;
    private int quantity;
    private ArrayList<String> aliases;

    private transient GUI gui = GUI.getGUI();
    public static ArrayList<String> validItems;
    private String startingRoom;
  
    public Item(int weight, String name, String startingRoom, boolean isTakeable, String description, ArrayList<String> aliases, boolean isDroppable, boolean isWeapon, int damage) { // FOR WEAPONS
      this.weight = weight;
      this.name = name;
      this.startingRoom = startingRoom;
      this.isTakeable = isTakeable;
      this.description = description;
      this.aliases = aliases;
      this.isDroppable = isDroppable;
      this.isWeapon = isWeapon;
      this.damage = damage;
      this.quantity = 1;
    }

    public Item(int weight, String name, String startingRoom, boolean isTakeable, String description, ArrayList<String> aliases, boolean isDroppable, int quantity) {
      this.weight = weight;
      this.name = name;
      this.startingRoom = startingRoom;
      this.isTakeable = isTakeable;
      this.description = description;
      this.aliases = aliases;
      this.isDroppable = isDroppable;
      this.quantity = quantity;
      this.isWeapon = false;
      this.damage = 0;
    }

    public Item(int weight, String name,  String startingRoom, boolean isTakeable, String description, ArrayList<String> aliases, boolean isDroppable) {
      this.weight = weight;
      this.name = name;
      this.startingRoom = startingRoom;
      this.isTakeable = isTakeable;
      this.description = description;
      this.aliases = aliases;
      this.isDroppable = isDroppable;
      this.quantity = 1;
      this.isWeapon = false;
      this.damage = 0;
    }

    public Item(int weight, String name, boolean isTakeable, String description) {
      this.weight = weight;
      this.name = name;
      this.isTakeable = isTakeable;
      this.description = description;
      this.quantity = 1;
      this.isDroppable = true;
      this.isWeapon = false;
      this.damage = 0;
    }

    public Item(Item item) {
      this.weight = item.weight;
      this.name = item.name;
      this.isTakeable = item.isTakeable;
      this.description = item.description;
      this.quantity = 1;
      this.isDroppable = true;
      this.isWeapon = true;
      this.damage = item.damage;
    }
    
    /** Set the list of valid items globally for the Item class. */
    private static void setValidItems() {
      validItems = new ArrayList<String>();
      for (Object item : Item.getItems()) {
        validItems.add(((String) ((JSONObject) item).get("name")).toLowerCase());
        for (Object alias : (JSONArray) ((JSONObject) item).get("aliases")) {
          validItems.add((String) alias);
        }
      }
    }
    
    public static boolean isValidItem(String item) {
      if (validItems == null) setValidItems();
      if (validItems.contains(item.toLowerCase())) return true;
      return false;
    }

    public void open() {
      if (!isTakeable)
        gui.println("The " + name + " cannot be opened.");
  
    }
  
    public ArrayList<String> getAliases(){
      return aliases;
    }

    /**
   * Loads the items.json file and returns a JSONArray of the items in the game.
   * @return A JSONArray
   */
    public static JSONArray getItems() {
      try {
        JSONObject json = (JSONObject) new JSONParser().parse(Files.readString(Path.of("data/items.json")));
        return (JSONArray) json.get("items");
      } catch (ParseException | IOException e) {
        return null;
      }
    }

    public String getStartingRoom() {
        return startingRoom;
    }

    public int getWeight() {
      return weight;
    }
  
    public void setWeight(int weight) {
      this.weight = weight;
    }

    public int getQuantity() {
      return quantity;
    }

    public void setDamage(int damage){
      this.damage = damage;
    }

    public int getDamage() {
      if (this.isWeapon){
        return this.damage;
      }
      return 0;
    }
  
    public void setQuantity(int quantity) {
      this.quantity = quantity;
    }

    public void decrementQuantity() {
      this.quantity--;
    }
  
    public String getName() {
      return name;
    }

    public String getDescription() {
      return description;
    }
  
    public void setName(String name) {
      this.name = name;
    }
  
    public void setDescription(String description) {
      this.description = description;
    }
  
    public boolean getIsDroppable() {
      return isDroppable;
    }
  
    public void setIsDroppable(boolean state) {
      this.isDroppable = state;
    }
  
    /**
     * Get isTakeable state.
     * @return
     */
    public boolean isTakeable() {
      return isTakeable;
    }

    /**
     * Set isTakeable state.
     * @param state
     * @author Stefano
     */
    public void isTakeable(boolean state) {
      isTakeable = state;
    }

    /**
     * Checks if the item name inputted is the same as the current item ({@code this}).
     * @param itemName - The item name to check
     * @return True or false
     * @author Stefano
     */
    public boolean isThisItem(String itemName){
      boolean out = false;
      if (itemName.equalsIgnoreCase(name)) out = true;
      for (String alias : aliases){
        if (itemName.equalsIgnoreCase(alias)) out = true;
      }
      return out;
    }
  }
  