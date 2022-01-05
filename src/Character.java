public class Character implements java.io.Serializable{
    private String name;
    private String catchphrase;

    public Character(String name, String catchphrase){
        this.name = name;
        this.catchphrase = catchphrase;
    }

    public String getName(){
        return name;
    }

    public String getCatchphrase(){
        return catchphrase;
    }
}
