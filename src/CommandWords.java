class CommandWords {
  // a constant array that holds all valid command words
  private static final String validCommands[] = { "go", "quit", "help", "eat", "yell", "music" };

  /**
   * Constructor - initialize the command words.
   */
  public CommandWords() {
    // nothing to do at the moment...
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

  /*
   * Print all valid commands to System.out.
   */
  public void showAll() {
    GUI gui = GUI.getGUI();
    for (int i = 0; i < validCommands.length; i++) {
      gui.print(validCommands[i] + "  ");
    }
    gui.println();
  }
}
