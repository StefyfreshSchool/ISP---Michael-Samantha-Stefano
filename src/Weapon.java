public class Weapon extends Item{
    private int damage;

  /**
   * Create a room described "description". Initially, it has no exits.
   * "description" is something like "a kitchen" or "an open court yard".
   */
  public Weapon(String description, int weight, String name, int damage) {
    super(weight, name, false, description);
    this.damage = damage;
  }

  public Weapon() {
    super(0, "DEFAULT NAME", false, "DEFAULT DESCRIPTION");
    damage = 0;

  }

  public Weapon(String name, String description, int damage, int weight) {
    super(weight, name, false, description);
    this.damage = damage;
    
  }

  /**
   * Return the description of the room (the one that was defined in the
   * constructor).
   */
  public String shortDescription() {
    return "Weapon: " + getName() + "\n\n" + getDescription();
  }

  /**
   * Return a long description of this room, on the form: You are in the kitchen.
   * Exits: north west
   */
  public String longDescription() {

    return "Weapon: " + getName() + "\n\n" + getDescription() + "\nDamage:"+damage;
  }

  public Integer getDamage() {
    return damage;
  }

  public void setWeaponDamage(int damage) {
    this.damage = damage;
  }
}
