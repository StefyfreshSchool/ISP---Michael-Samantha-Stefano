import java.util.ArrayList;

public class Inventory implements java.io.Serializable {
  private ArrayList<Item> items;
  private int maxWeight;
  private int currentWeight;

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
    if (item.getWeight() + currentWeight <= maxWeight){
      currentWeight += item.getWeight();
      return items.add(item);
    } else {
      return false;
    }
  }

  /**
   * Gets an Item object specified by the item name. <p>
   * If the Item is not in the inventory, it returns null.
   * @param name - The name of the item to get
   * @return The Item
   */
  public Item getItem(String name){
    int i = items.indexOf(Game.itemMap.get(name));
    if (i > -1) return items.get(i);
    for (Item invItem : items) {
      if (invItem.getName().equalsIgnoreCase(name)) return invItem;
      for (String alias : invItem.getAliases()) {
        if (alias.equalsIgnoreCase(name)) return invItem;
      }
    }
    return null;
  }

  public boolean removeItem(Item item) {
    currentWeight -= item.getWeight();
    return items.remove(item);
  }

  public ArrayList<Item> getItems() {
    return items;
  }

  public boolean hasItem(Item item){
    if (items.contains(item)) return true;
    for (Item invItem : items) {
      if (invItem.getName().equalsIgnoreCase(item.getName())) return true;
      for (String alias : invItem.getAliases()) {
        for (String alias2 : item.getAliases()) {
          if (alias.equalsIgnoreCase(alias2)) return true;
        }
      }
    }
    return false;
  }
}