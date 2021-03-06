import java.util.ArrayList;

public class Parser {
  private static final String validCommands[] = { "go", "quit", "help", "eat", "yell", "music", "restart", "hit", "save", "take", "heal", "test", "wear", "read", "pray", "inflate", "info", "cls", "drop", "threaten" };
  private static GUI gui = GUI.getGUI();

  public Command getCommand() {
    GUI gui = GUI.getGUI();
    String inputLine = gui.readCommand();
    String[] words;
    ArrayList<String> args = new ArrayList<String>();

    words = inputLine.trim().split(" ");
    
    for (int i = 1; i < words.length; i++) {
      args.add(words[i]);
    }

    String word1 = words[0].toLowerCase();
    ArrayList<String> word2 = null;

    if (words.length > 1)
      word2 = args;

    if (isCommand(word1))
      return new Command(word1, word2);
    else
      return new Command(null, word2);
  }


  /**
   * Check whether a given String is a valid command word. Return true if it is,
   * false if it isn't.
   **/
  public static boolean isCommand(String aString) {
    for (int i = 0; i < validCommands.length; i++) {
      if (validCommands[i].equals(aString))
        return true;
    }
    // if we get here, the string was not found in the commands
    return false;
  }


  /**
   * Print out a list of valid command words.
   */
  public static void showCommands() {
    GUI gui = GUI.getGUI();
    for (int i = 0; i < validCommands.length - 1; i++) {
      gui.print(validCommands[i] + ", ");
    }
    gui.print(validCommands[validCommands.length - 1]);
    gui.println();
  }

  /**
   * Prints help for each specific command.
   * @param command
   */
  public static void printCommandHelp(Command command) {
    String commandWord = command.getFirstArg();
     if (!cmdValid(command, 0)){
      gui.println("\"" + command.getFirstArg() + "\" is not a valid command!");
    } else if (commandWord.equalsIgnoreCase("go")){
      gui.println("Takes you across the game map.");
      gui.println();
      gui.println("GO direction");
      gui.println();
      gui.println("  direction     Specifies the direction to go.");
    } else if (commandWord.equalsIgnoreCase("yell")){
      gui.println("Yells.");
      gui.println();
      gui.println("YELL [text]");
      gui.println();
      gui.println("  text     Specifies the text to yell.");
    } else if (commandWord.equalsIgnoreCase("music")){
      gui.println("Controls the background music of the game.");
      gui.println();
      gui.println("MUSIC <stop | start | volume-down | volume-up>");
      gui.println();
      gui.println("  stop          Stops the music.");
      gui.println("  start         Starts the music.");
      gui.println("  volume-up     Increases the volume of the music.");
      gui.println("  volume-down   Increases the volume of the music.");
    } else if (commandWord.equalsIgnoreCase("restart")){
      gui.println("Restarts the game.");
      gui.println();
      gui.println("RESTART [confirm / y]");
      gui.println();
      gui.println("  confirm     Forces an immediate restart, suppressing the confirmation prompt.");
    } else if (commandWord.equalsIgnoreCase("quit")){
      gui.println("Quits the game.");
      gui.println();
      gui.println("QUIT [confirm / y]");
      gui.println();
      gui.println("  confirm     Forces an immediate exit, suppressing the confirmation prompt.");
    } else if (commandWord.equalsIgnoreCase("hit")){
      gui.println("Hits an enemy.");
      gui.println();
      gui.println("HIT enemy WITH weapon");
      gui.println();
      gui.println("  enemy     The enemy to hit.");
      gui.println("  weapon    The weapon to hit with.");
    } else if (commandWord.equalsIgnoreCase("threaten")){
      gui.println("Threatens an enemy.");
      gui.println();
      gui.println("THREATEN enemy WITH item");
      gui.println();
      gui.println("  enemy     The enemy to threaten.");
      gui.println("  item      The item to threaten with.");
    } else if (commandWord.equalsIgnoreCase("save")){
      gui.println("Saves the game or loads from a previously saved state.");
      gui.println();
      gui.println("SAVE <quit | game | load | clear>");
      gui.println();
      gui.println("  quit      Saves the game and quits.");
      gui.println("  game      Saves the game without quitting.");
      gui.println("  load      Loads the game from a previously saved state.");
      gui.println("  clear     Clears the saved state of the game.");
    } else if (commandWord.equalsIgnoreCase("help")){
      gui.println("Prints help about the commands that can be used in the game.");
      gui.println();
      gui.println("HELP [command]");
      gui.println();
      gui.println("  command     A command to learn more about.");
      gui.println();
      gui.println("Alternatively, you can type \"/?\" after any command to print");
      gui.println("the same information as \"help [command]\".");
    } else if (commandWord.equalsIgnoreCase("eat")){
      gui.println("Allows you to eat. Does nothing at the current time.");
      gui.println();
      gui.println("EAT");
    } else if (commandWord.equalsIgnoreCase("take")){
      gui.println("Picks up an item from the current room.");
      gui.println();
      gui.println("TAKE item");
      gui.println();
      gui.println("  item     The item to take.");
    } else if (commandWord.equalsIgnoreCase("heal")){
      gui.println("Heals you.");
      gui.println();
      gui.println("HEAL");
    } else if (commandWord.equalsIgnoreCase("wear")){
      gui.println("Lets you wear clothing items.");
      gui.println();
      gui.println("WEAR item");
      gui.println();
      gui.println("  item     The item to wear.");
    } else if (commandWord.equalsIgnoreCase("read")){
      gui.println("Lets you read items with text on them.");
      gui.println();
      gui.println("READ item");
      gui.println();
      gui.println("  item     The item to read.");
    } else if (commandWord.equalsIgnoreCase("pray")){
      gui.println("Lets you pray when in the News News Temple Room.");
      gui.println();
      gui.println("PRAY");
    } else if (commandWord.equalsIgnoreCase("inflate")){
      gui.println("Lets you inflate an item that can be inflated.");
      gui.println();
      gui.println("INFLATE item");
      gui.println();
      gui.println("  item     The item to be inflated.");
    } else if (commandWord.equalsIgnoreCase("info")){
      gui.println("Displays info about the game.");
      gui.println();
      gui.println("INFO");
    } else if (commandWord.equalsIgnoreCase("cls")){
      gui.println("Clears the screen.");
      gui.println();
      gui.println("CLS");
    } else if (commandWord.equalsIgnoreCase("drop")){
      gui.println("Drops an item from your inventory and puts it in the current room.");
      gui.println();
      gui.println("DROP item");
      gui.println();
      gui.println("  item     The item to drop.");
    } else if (commandWord.equalsIgnoreCase("test")){
      gui.println("Internal function for testing the game. Do not use.");
    }
    else {
      gui.println("CODE ERROR: Please write a condition for the " + commandWord + " command in Parser.java.");
    }
  }

  private static boolean cmdValid(Command command, int argIndex) {
    return isCommand(command.getFirstArg());
  }
}
