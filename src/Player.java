public class Player implements java.io.Serializable{
    private int health;
    private boolean talkedToSkyGods;
    private boolean[] trials;

    public Player(int health){
        this.health = health;
        trials = new boolean[11];
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

    public boolean getTrial(int index){
        return trials[index];
    }

    public void setTrial(int index){
        trials[index] = true;
    }

    public void resetTrials(){
        trials = new boolean[11];
    }

    public boolean skyGodsCheck(){
        for (int i = 0; i < 8; i++){
            if (!trials[i]){
                return false;
            }
        }
        return true;
    }
}
