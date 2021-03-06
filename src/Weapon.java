public class Weapon extends Item{
  private int damage;

  public Weapon(String name, String description, int damage, int weight) {
    super(weight, name, false, description);
    this.damage = damage;
    
  }

  public Weapon() {
    super(0, "DEFAULT NAME", false, "DEFAULT DESCRIPTION");
    damage = 0;

  }

  /**
   * Return the description of the weapon
   */
  public String shortDescription() {
    return "Weapon: " + getName() + "\n\n" + getDescription();
  }

  /**
   * Return a long description of this weapon (Name and damage)
   */
  public String longDescription() {

    return "Weapon: " + getName() + "\n\n" + getDescription() + "\nDamage:"+damage;
  }

  public int getDamage() {
    return damage;
  }

  public void setDamage(int damage) {
    this.damage = damage;
  }
}