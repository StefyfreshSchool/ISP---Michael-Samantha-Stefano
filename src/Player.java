public class Player {
    public int health = 100;

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
