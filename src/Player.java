public class Player implements java.io.Serializable{
    public int health;

    public Player(int health){
        this.health = health;
    }

    public void setHealth(int damage){
        health -= damage;
    }

    public void maxHeal(){
        health = 100;
    }

    public int getHealth(){
        return health;
    }
}
