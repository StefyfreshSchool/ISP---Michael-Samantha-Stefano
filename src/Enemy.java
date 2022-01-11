import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Enemy extends Character{
    private int health;
    private int damageMin;
    private int damageMax;
    private String m1; // three messages when the enemy hurts you
    private String m2;
    private String m3;
    private ArrayList<String> aliases;
    private boolean isDead;

    public Enemy(String name, String catchphrase, int health, int damageMin, int damageMax, String m1, String m2, String m3, ArrayList<String> aliases){
        super(name, catchphrase);
        this.health = health;
        this.damageMin = damageMin;
        this.damageMax = damageMax;
        this.m1 = m1;
        this.m2 = m2;
        this.m3 = m3;
        this.aliases = aliases;
        this.isDead = false;
    }

    public Enemy(){
        super("DEFAULT NAME", "DEFAULT CATCHPHRASE");
        this.health = 10;
    }

    public void setHealth(int health){
        this.health = health;
    }

    public void attacked(int damage){
        health -= damage;
    }

    public boolean getIsDead(){
        return isDead;
    }

    public void setIsDead(boolean state){
        isDead = state;
    }

    public int getHealth(){
        return health;
    }

    public int getDamage() {
        return (int) (Math.random() * (damageMax - damageMin)) + damageMin;
    }

    public String getHurtMessage() {
        int num = (int) (Math.random() * 3) + 1;
        if (num == 1){
            return m1;
        } else if (num == 2){
            return m2;
        } return m3;
    }

    public static JSONArray getEnemies() {
        try {
            JSONObject json = (JSONObject) new JSONParser().parse(Files.readString(Path.of("data/enemies.json")));
            return (JSONArray) json.get("enemies");
        } catch (ParseException | IOException e) {
            return null;
        }
    }

    /**
     * Checks if the enemy name inputted is the same as the current enemy ({@code this}).
     * @param enemyName - The enemy name to check
     * @return True or false
     */
    public boolean isThisEnemy(String enemyName){
        boolean out = false;
        if (enemyName.equalsIgnoreCase(getName())) out = true;
        for (String alias : aliases){
          if (enemyName.equalsIgnoreCase(alias)) out = true;
        }
        return out;
    }
}