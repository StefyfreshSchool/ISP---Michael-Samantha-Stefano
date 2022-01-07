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
    private boolean isOpenable;
    private boolean isWeapon;
    private int damage = 0;
    private int quantity;
    private ArrayList<String> aliases;

    private transient GUI gui = GUI.getGUI();
    public static ArrayList<String> validItems;
    private String startingRoom;
  
    public Item(int weight, String name, String startingRoom, boolean isOpenable, String description, ArrayList<String> aliases, boolean isWeapon, int damage) { // FOR WEAPONS
      this.weight = weight;
      this.name = name;
      this.startingRoom = startingRoom;
      this.isOpenable = isOpenable;
      this.description = description;
      this.aliases = aliases;
      this.isWeapon = isWeapon;
      this.damage = damage;
      this.quantity = 1;
    }

    public Item(int weight, String name, String startingRoom, boolean isOpenable, String description, ArrayList<String> aliases, int quantity) {
      this.weight = weight;
      this.name = name;
      this.startingRoom = startingRoom;
      this.isOpenable = isOpenable;
      this.description = description;
      this.aliases = aliases;
      this.quantity = quantity;
      this.isWeapon = false;
      this.damage = 0;
    }

    public Item(int weight, String name,  String startingRoom, boolean isOpenable, String description, ArrayList<String> aliases) {
      this.weight = weight;
      this.name = name;
      this.startingRoom = startingRoom;
      this.isOpenable = isOpenable;
      this.description = description;
      this.aliases = aliases;
      this.quantity = 1;
      this.isWeapon = false;
      this.damage = 0;
    }

    public Item(int weight, String name, boolean isOpenable, String description) {
      this.weight = weight;
      this.name = name;
      this.isOpenable = isOpenable;
      this.description = description;
      this.quantity = 1;
      this.isWeapon = false;
      this.damage = 0;
    }

    public Item(Item item) {
      this.weight = item.weight;
      this.name = item.name;
      this.isOpenable = item.isOpenable;
      this.description = item.description;
      this.quantity = 1;
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
      if (!isOpenable)
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
  
    public boolean isOpenable() {
      return isOpenable;
    }
  
    public void setOpenable(boolean isOpenable) {
      this.isOpenable = isOpenable;
    }
  
  }
  