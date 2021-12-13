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

}