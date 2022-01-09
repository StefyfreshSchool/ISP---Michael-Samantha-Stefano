public class Enemy extends Character{
    private int health;
    private String name;
    private int damageMin;
    private int damageMax;
    private String m1; // three messages when the enemy hurts you
    private String m2;
    private String m3;
    private boolean isDead;

    public Enemy(String name, String catchphrase, int health, int damageMin, int damageMax, String m1, String m2, String m3){
        super(name, catchphrase);
        this.health = health;
        this.damageMin = damageMin;
        this.damageMax = damageMax;
        this.m1 = m1;
        this.m2 = m2;
        this.m3 = m3;
        this.isDead = false;
    }

    public Enemy(){
        super("DEFAULT NAME", "DEFAULT CATCHPHRASE");
        this.health = 10;
    }

    public Enemy(Enemy src){
        super(src.name, src.getCatchphrase());
        this.health = src.health;
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
}