public class Player implements java.io.Serializable{
    public int health;
    public boolean talkedToSkyGods;

    public Player(int health){
        this.health = health;
        talkedToSkyGods = false;
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

    public void changeTalkedToSkyGods(){
        talkedToSkyGods = true;
    }

    public boolean getTalkedToSkyGods(){
        return talkedToSkyGods;
    }
}
