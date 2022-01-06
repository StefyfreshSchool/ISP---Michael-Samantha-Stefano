public class Player implements java.io.Serializable{
    private int health;
    private boolean talkedToSkyGods;

    public Player(int health){
        this.health = health;
        talkedToSkyGods = false;
    }

    public void setHealth(int damage){
        health -= damage;
        if (health < 0){
            health = 0;
        }
    }

    public void maxHeal(){
        health = 100;
    }

    public int getHealth(){
        return health;
    }

    public void talkedToSkyGods(){
        talkedToSkyGods = true;
    }

    public boolean getTalkedToSkyGods(){
        return talkedToSkyGods;
    }
}
