import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Item extends OpenableObject {
    private int weight;
    private String name;
    private String description;
    private boolean isOpenable;
    private int quantity;
    private GUI gui = GUI.getGUI();
    public static ArrayList<String> validItems;
  
    public Item(int weight, String name, boolean isOpenable, String description, int quantity) {
      this.weight = weight;
      this.name = name;
      this.isOpenable = isOpenable;
      this.description = description;
      this.quantity = quantity;
    }

    public Item(int weight, String name, boolean isOpenable, String description) {
      this.weight = weight;
      this.name = name;
      this.isOpenable = isOpenable;
      this.description = description;
      this.quantity = 1;
      setValidItems();
    }

    public Item(int weight, String name, boolean isOpenable) {
      this.weight = weight;
      this.name = name;
      this.isOpenable = isOpenable;
      this.description = "DEFAULT DESCRIPTION";
      this.quantity = 1;
      setValidItems();
    }
    
    /** Set the list of valid items globally for the Item class. */
    private static void setValidItems() {
      try {
        validItems = new ArrayList<String>();
        JSONObject json = (JSONObject) new JSONParser().parse(Files.readString(Path.of("src/data/items.json")));
        JSONArray items = (JSONArray) json.get("items");
        for (Object item : items) {
          validItems.add((String) ((JSONObject) item).get("name"));
        }
      } catch (ParseException | IOException e) {
        e.printStackTrace();
      }
    }
    
    public static boolean isValidItem(String item) {
      if (validItems == null) setValidItems();
      if (validItems.contains(item)) return true;
      return false;
    }

    public void open() {
      if (!isOpenable)
        gui.println("The " + name + " cannot be opened.");
  
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
  
    public void setQuantity(int quantity) {
      this.quantity = quantity;
    }

    public void setQuantity() {
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
  