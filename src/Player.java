public class Player implements java.io.Serializable{
    private int health;
    private boolean talkedToSkyGods;
    private boolean hasReadTome;
    private boolean[] trials;

    public Player(int health){
        this.health = health;
        trials = new boolean[11];
        talkedToSkyGods = false;
        hasReadTome = false;
    }

    public boolean setDamage(int damage){
        health -= damage;
        if (health <= 0){
            health = 0;
            return false;
        } 
        return true;
    }

    public void setHealth(int health){
        this.health = health;
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

    public boolean getHasReadTome(){
        return hasReadTome;
    }

    public void setHasReadTome(boolean state){
        hasReadTome = state;
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
