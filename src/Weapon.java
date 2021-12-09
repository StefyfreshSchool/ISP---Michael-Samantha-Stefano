public class Weapon {
    private String name;
    private String description;
    private int damage;
    private int weight;
    private GUI gui = GUI.getGUI();

  /**
   * Create a room described "description". Initially, it has no exits.
   * "description" is something like "a kitchen" or "an open court yard".
   */
  public Weapon(String description) {
    this.description = description;
  }

  public Weapon() {
    name = "DEFAULT WEAPON";
    description = "DEFAULT DESCRIPTION";
    damage = 0;
    weight = 0;
  }

  public Weapon(String name, String description, int damage, int weight) {
    this.name = name;
    this.description = description;
    this.damage = damage;
    this.weight = weight;
  }

  /**
   * Return the description of the room (the one that was defined in the
   * constructor).
   */
  public String shortDescription() {
    return "Weapon: " + name + "\n\n" + description;
  }

  /**
   * Return a long description of this room, on the form: You are in the kitchen.
   * Exits: north west
   */
  public String longDescription() {

    return "Weapon: " + name + "\n\n" + description + "\nDamage:"+damage;
  }

  public String getWeaponName() {
    return name;
  }

  public void setWeaponName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Integer getDamage() {
    return damage;
  }

  public void setWeaponDamage(int damage) {
    this.damage = damage;
  }
}
