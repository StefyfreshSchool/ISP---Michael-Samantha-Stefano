public class Item extends OpenableObject {
    private int weight;
    private String name;
    private String description;
    private boolean isOpenable;
    private GUI gui = GUI.getGUI();
  
    public Item(int weight, String name, boolean isOpenable, String description) {
      this.weight = weight;
      this.name = name;
      this.isOpenable = isOpenable;
      this.description = description;
    }

    public Item(int weight, String name, boolean isOpenable) {
      this.weight = weight;
      this.name = name;
      this.isOpenable = isOpenable;
      this.description = "DEFAULT DESCRIPTION";
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
  
    public String getName() {
      return name;
    }

    public String getDescription() {
      return description;
    }
  
    public void setName(String name) {
      this.name = name;
    }
  
    public boolean isOpenable() {
      return isOpenable;
    }
  
    public void setOpenable(boolean isOpenable) {
      this.isOpenable = isOpenable;
    }
  
  }
  