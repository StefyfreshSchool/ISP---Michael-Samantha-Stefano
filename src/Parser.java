import java.util.ArrayList;

public class Parser {
  private static final String validCommands[] = { "go", "quit", "help", "eat", "yell", "music", "restart", "hit", "save" };


  public Command getCommand() {
    GUI gui = GUI.getGUI();
    String inputLine = gui.readCommand();
    String[] words;
    ArrayList<String> args = new ArrayList<String>();

    words = inputLine.split(" ");
    
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
  public boolean isCommand(String aString) {
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
    for (int i = 0; i < validCommands.length; i++) {
      gui.print(validCommands[i] + "  ");
    }
    gui.println();
  }
}
