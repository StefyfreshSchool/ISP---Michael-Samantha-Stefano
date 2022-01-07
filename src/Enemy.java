public class Enemy extends Character{
    private int health;
    private String name;
    private int damage;
    private boolean isDead;

    public Enemy(String name, String catchphrase, int health, int damage){
        super(name, catchphrase);
        this.health = health;
        this.damage = damage;
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
        return damage;
    }
}