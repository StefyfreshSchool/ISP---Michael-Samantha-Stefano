import java.util.ArrayList;

public class Command {
  private String commandWord;
  private ArrayList<String> args;

  /**
   * Create a command object. First and second word must be supplied, but either
   * one (or both) can be null. The command word should be null to indicate that
   * this was a command that is not recognized by this game.
   */
  public Command(String firstWord, ArrayList<String> args) {
    commandWord = firstWord;
    this.args = args;
  }

  /**
   * Return the command word (the first word) of this command. If the command was
   * not understood, the result is null.
   */
  public String getCommandWord() {
    return commandWord;
  }

  public String legitimateHitCommand() {
    String str = getStringifiedArgs();
    if(str==" "||str==""){
      return "A";
    }else if(str.indexOf(" with ")<0&&(!checkenemy(str)||!checkenemy(str+" "))){
      return "D";
    }
    if(str.indexOf(" with ")<0){
      return "B";
    }
    String cut = str.substring(str.indexOf(" with ")+6, str.length());
    if(cut.equals(" ")){
      return "B";
    }
    if(!cut.equals("geraldo")){
      return "E";
    }
    int count = 0;
    for(int i=0; i<args.size()-1; i++) {
      if(args.get(i).equals(" ")){
        count++;
      }
    }
    if(count!=3){
      //return 2;
    }
    return "";
    //return 0;
  }

  public boolean checkenemy(String str){
    for(Enemy enemy: Game.getEnemies()){
      if(str.equals(enemy.getName())){
        return true;
      }
    }
    return false;
    
  }

  /**
   * Return the first command argument of this command. Returns an empty string if there 
   * were no arguments.
   */
  public String getFirstArg() {
    if (!hasArgs()) return "";
    return args.get(0);
  }

  /**
   * Return the arguments of this command. Returns null if there were no args
   * word.
   */
  public ArrayList<String> getArgs() {
    return args;
  }

  /**
   * Returns a stringified String of this command's arguments. If there were no args, it returns null.
   */
  public String getStringifiedArgs(){
    String out = "";
    if (args == null) return out;
    for (String s : args) {
      out += s + " ";
    }
    if (out.equals("")) return null;
    else {
      out = out.substring(0, out.length() - 1);
    }
    return out;
  }

  /**
   * Returns the last argument of the command. If there were no args, it returns an empty string.
   */
  public String getLastArg(){
    if (args == null) return "";
    return args.get(args.size() - 1);
  }

  /**
   * Return true if this command was not understood.
   */
  public boolean isUnknown() {
    return (commandWord == null);
  }

  /**
   * Return true if the command has a second word.
   */
  public boolean hasArgs() {
    return (args != null);
  }
}
