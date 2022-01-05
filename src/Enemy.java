public class Enemy extends Character implements java.io.Serializable{
    private int health;
    private String name;

    public Enemy(String name, String catchphrase, int health){
        super(name, catchphrase);
        this.health = health;
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

    public int getHealth(){
        return health;
        
    }
}