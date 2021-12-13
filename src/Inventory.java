import java.util.ArrayList;

public class Inventory {
  private ArrayList<Item> items;
  private int maxWeight;
  private int currentWeight;
  private GUI gui = GUI.getGUI();

  public Inventory(int maxWeight) {
    this.items = new ArrayList<Item>();
    this.maxWeight = maxWeight;
    this.currentWeight = 0;
  }

  /**
   * Gets a string of all the elements in the player's inventory. If there are none, it returns "Empty".
   * @return The {@code String} of the player's inventory.
   */
  public String getString() {
    ArrayList<String> itemStrings = new ArrayList<String>();
    for (Item item : items) {
        itemStrings.add(item.getName());
    }
    if (itemStrings.size() == 0) return "Empty";
    return String.join(", ", itemStrings);
  }

  public int getMaxWeight() {
    return maxWeight;
  }

  public int getCurrentWeight() {
    return currentWeight;
  }

  public boolean addItem(Item item) {
    if (item.getWeight() + currentWeight <= maxWeight)
      return items.add(item);
    else {
      gui.println("There is no room to add the item.");
      return false;
    }
  }

  public Item getItem(int index){
    return items.get(index);
  }

  public int find(String name){
    for(int i=0; i<items.size(); i++){
      if(items.get(i).getName().equals(name)){
        return i;
      }
    }
    return -1;
  }

  public String printInventory(){
    String in = "";
    for(int i=0; i<items.size(); i++){
      in += items.get(i).getName()+", ";
    }
    return in;
    
  }

  public boolean removeItem(Item item) {
    return items.remove(item);
  }

  public ArrayList<Item> getItems() {
    return items;
  }
}