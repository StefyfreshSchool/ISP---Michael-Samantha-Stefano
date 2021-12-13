public class Player {
    private int health;

    public Player(int health){
        this.health = health;
    }

    public void setHealth(int damage){
        health -= damage;
    }
}
