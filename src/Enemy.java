public class Enemy extends Character{
    private int health;

    public Enemy(String name, String catchphrase, int health){
        super(name, catchphrase);
        this.health = health;
    }

    public void setHealth(int damage){
        health -= damage;
        
    }
}