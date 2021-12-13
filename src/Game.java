import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class Game implements java.io.Serializable {
  private transient static GUI gui;
  private static MusicPlayer music;

  public static HashMap<String, Room> roomMap = new HashMap<String, Room>();

  private Inventory inventory;
  private static final int MAX_WEIGHT = 10;

  private Parser parser;
  private Room currentRoom;

  /**
   * Create the game and initialize its internal map.
   */
  public Game() {
    gui = GUI.getGUI();
    gui.createWindow();
    inventory = new Inventory(MAX_WEIGHT);
    try {
      initRooms("src\\data\\rooms.json");
      currentRoom = roomMap.get("South of the Cyan House");
      
      //Initialize the game if a previous state was recorded
      Save game = null;
      try (FileInputStream fileIn = new FileInputStream("src/data/game.ser")) {
        ObjectInputStream in = new ObjectInputStream(fileIn);
        game = (Save) in.readObject();
        in.close();
        fileIn.close();
      } catch (ClassNotFoundException | IOException e) {
        e.printStackTrace();
      }
      if (game != null && game.getInProgress()){
        gui.println("A previously saved game state was recorded.");
        gui.println("Would you like to restore from that save?");
        gui.println();
        gui.println("Type \"y\" to restore or \"n\" to ignore.");

        boolean validInput = false;
        while(!validInput){
          String in = gui.readCommand();
          if (in.equalsIgnoreCase("y") || in.equalsIgnoreCase("yes")){
            gui.reset();

            roomMap = game.getRoomMap();
            inventory = game.getInventory();
            currentRoom = game.getCurrentRoom();
            gui.println("Restored from saved game.\n");
            validInput = true;
          } else if (in.equalsIgnoreCase("n") || in.equalsIgnoreCase("no") || in.equalsIgnoreCase("cancel")){
            gui.reset();
            gui.println("Ignoring old game state.\n");
            resetSaveState();
            validInput = true;
          } else {
            gui.println("\"" + in + "\" is not a valid choice!");
          }
        } 
      }  
    } catch (Exception e) {
      e.printStackTrace();
    }
    parser = new Parser();
  }

  private void initRooms(String fileName) throws Exception {
    Path path = Path.of(fileName);
    String jsonString = Files.readString(path);
    JSONParser parser = new JSONParser();
    JSONObject json = (JSONObject) parser.parse(jsonString);

    JSONArray jsonRooms = (JSONArray) json.get("rooms");

    for (Object roomObj : jsonRooms) {
      Room room = new Room();
      String roomName = (String) ((JSONObject) roomObj).get("name");
      String roomId = (String) ((JSONObject) roomObj).get("id");
      String roomDescription = (String) ((JSONObject) roomObj).get("description");
      room.setDescription(roomDescription);
      room.setRoomName(roomName);

      JSONArray jsonExits = (JSONArray) ((JSONObject) roomObj).get("exits");
      ArrayList<Exit> exits = new ArrayList<Exit>();
      for (Object exitObj : jsonExits) {
        String direction = (String) ((JSONObject) exitObj).get("direction");
        String adjacentRoom = (String) ((JSONObject) exitObj).get("adjacentRoom");
        String keyId = (String) ((JSONObject) exitObj).get("keyId");
        Boolean isLocked = (Boolean) ((JSONObject) exitObj).get("isLocked");
        Boolean isOpen = (Boolean) ((JSONObject) exitObj).get("isOpen");
        Exit exit = new Exit(direction, adjacentRoom, isLocked, keyId, isOpen);
        exits.add(exit);
      }
      room.setExits(exits);
      room.initItems();
      roomMap.put(roomId, room);
    }
  }

  /**
   * Main play routine. Loops until end of play.
   */
  public void play() {
    printWelcome();
    startMusic();
    gui.setGameInfo(inventory.getString(), currentRoom.getExits());
    
    // Enter the main command loop. Here we repeatedly read commands and
    // execute them until the game is over.

    boolean finished = false;
    while (!finished) {
      Command command;
      command = parser.getCommand();
      int status = processCommand(command);
      gui.setGameInfo(inventory.getString(), currentRoom.getExits());
      if (status == 1){
        finished = true;
      } 
      if (status == 2){
        music.stop();
        gui.reset();
        gui.println("Game restarted.\n");
        try {
          initRooms("src\\data\\rooms.json");
          currentRoom = roomMap.get("South of the Cyan House");
          inventory = new Inventory(MAX_WEIGHT);
        } catch (Exception e) {
          e.printStackTrace();
        }
        printWelcome();
        startMusic();
        gui.setGameInfo(inventory.getString(), currentRoom.getExits());
      }
      
    }

    gui.println("Thank you for playing. Good bye.");

    //Nice transition to exit the game
    sleep(1000);
    System.exit(0);
  }

  /**Starts the background music. */
  private void startMusic() {
    music = new MusicPlayer("src/audio/background.wav");
    music.setVolume(-25f);
    music.play();
  }

  public static MusicPlayer getMusicPlayer() {
    return music;
  }

  /**
   * Print out the opening message for the player.
   */
  private void printWelcome() {
    gui.println("Welcome to Zork!");
    gui.println("Zork is an amazing text adventure game!");
    gui.println("Type 'help' if you need help.");
    gui.println();
    gui.println(currentRoom.longDescription());
  }

  /**
   * Given a command, process (that is: execute) the command.
   * @param command
   * @return {@code 0} if no action is required, {@code 1} if the game should quit, {@code 2} if the game should restart
   */
  private int processCommand(Command command) {
    if (command.isUnknown()) {
      gui.println("I don't know what you mean...");
      return 0;
    } 
    String commandWord = command.getCommandWord();
    if (!command.isUnknown() && command.getFirstArg().equals("/?")){
      ArrayList<String> args = new ArrayList<String>();
      args.add(commandWord);
      Parser.printCommandHelp(new Command("help", args));
      return 0;
    }
    if (commandWord.equals("help")){
      printHelp(command);
    }
    else if (commandWord.equals("go")){
      goRoom(command);
    }
    else if (commandWord.equals("quit")) {
      if (quitRestart("quit", command)){
        resetSaveState();
        return 1;
      }

    } else if (commandWord.equals("yell")){
      yell(command.getStringifiedArgs());
    
    } else if (commandWord.equals("music")) {
      music(command);

    } else if(commandWord.equals("hit")){
      //TODO: hit() when inventory is ready
    } else if (commandWord.equals("restart")) {
      if (quitRestart("restart", command)){
        resetSaveState();
        return 2;
      }
    } else if (commandWord.equals("save")){
      if (save(command)) return 1;
    } else if (commandWord.equals("take")){
      take(command);
    }
    return 0;
  }

    /**
   * Given a command, process (that is: execute) the command.
   * <p>
   * TODO: figure out if this can be merged with above method.
   * @param command
   * @param weapon
   */
  private void processCommand(Command command, Weapon weapon) {
    if (command.isUnknown()) {
      gui.println("I don't know what you mean...");
    }
    String commandWord = command.getCommandWord();
    if(commandWord.equals("hit")){
      //if(getRoomName().equals("The Lair"))
    }
  }

  /**
   * Allows the player to take items from the current room.
   * @param command
   */
  private void take(Command command) {
    if (!command.hasArgs()){
      gui.println("Take what?");
      return;
    }
    String itemName = command.getStringifiedArgs();
    if (!command.hasArgs()){
      gui.println("Take what?");
      return;
    } else {
      if (!Item.isValidItem(itemName)){
        gui.print("Not a valid item!");
      } else if (!currentRoom.containsItem(itemName)){
        gui.print("That item is not in this room!");
      } else {
        if (inventory.addItem(currentRoom.getItem(itemName))){
          gui.println(currentRoom.getItem(itemName).getName() + " taken!");
          currentRoom.removeItem(itemName);
        }
      }
    }
  }

  /**
   * Saves the game and optionally quits.
   * @param command
   */
  private boolean save(Command command) {
    boolean quit = false;
    if (command.getLastArg().equalsIgnoreCase("quit")){
      quit = true;
      gui.println("Game saved! Quitting.");
    } else if (command.getLastArg().equalsIgnoreCase("game")){ 
      gui.println("Game saved!");
    } else if (command.getLastArg().equalsIgnoreCase("load")){
      loadSave();
      gui.setGameInfo(inventory.getString(), currentRoom.getExits());
      return false;
    } else if (command.getLastArg().equals("")){
      gui.println("What would you like to do?");
    } else if (command.getLastArg().equalsIgnoreCase("clear") || command.getLastArg().equalsIgnoreCase("reset")){
      resetSaveState();
      gui.println("Cleared game save.");
      return false;
    } else {
      gui.println("save " + command.getStringifiedArgs() + " is not a valid save command!");
      return false;
    }

    HashMap<String, Object> data = new HashMap<String, Object>();
    data.put("inProgress", true);
    data.put("room", currentRoom.getRoomName());

    Save game = new Save(roomMap, inventory, currentRoom);
    try {
      FileOutputStream fileOut = new FileOutputStream("src/data/game.ser");
      ObjectOutputStream out = new ObjectOutputStream(fileOut);
      out.writeObject(game);
      out.close();
      fileOut.close();
   } catch (IOException i) {
      i.printStackTrace();
   }
    return quit;
  }

  /**
   * Allows the game to load a previously saved state of the game.
   */
  private void loadSave() {
    gui.print("Loading save");
    sleep(150);
    gui.print(".");
    sleep(150);
    gui.print(".");
    sleep(150);
    gui.print(".");
    sleep(150);

    Save game = null;
    try (FileInputStream fileIn = new FileInputStream("src/data/game.ser")) {
      ObjectInputStream in = new ObjectInputStream(fileIn);
      game = (Save) in.readObject();
      in.close();
      fileIn.close();

      roomMap = game.getRoomMap();
      inventory = game.getInventory();
      currentRoom = game.getCurrentRoom();
    } catch (ClassNotFoundException | IOException e) {
      e.printStackTrace();
    }

    music.stop();
    startMusic();
    gui.reset();
    gui.println("Game reloaded from saved data.\n");
    gui.println(currentRoom.longDescription());
    gui.setGameInfo(inventory.getString(), currentRoom.getExits());
  }

  /**
   * Prompts the user if they want to quit or restart the game. 
   * After user input, it returns true or false.
   * @param string - Prints whether the operation is a quit or restart.
   * @return True or false based on if the user cancelled the operation or not.
   */
  private boolean quitRestart(String string, Command command) {
    if (command.getLastArg().equalsIgnoreCase("confirm")) return true;
    gui.println("Are you sure you would like to " + string + " the game?");
    gui.println("Type \"y\" to confirm or \"n\" to cancel.");
    boolean validInput = false;
    while(!validInput){
      String in = gui.readCommand();
      if (in.equalsIgnoreCase("y") || in.equalsIgnoreCase("yes")) return true;
      else if (in.equalsIgnoreCase("n") || in.equalsIgnoreCase("no") || in.equalsIgnoreCase("cancel")){
        gui.println("Cancelled.");
        validInput = true;
      } else {
        gui.println("\"" + in + "\" is not a valid choice!");
      }
    }
    return false;
  }

  /**
   * Try to go to one direction. If there is an exit, enter the new room,
   * otherwise print an error message.
   */
  private void goRoom(Command command) {
    if (!command.hasArgs()) {
      // if there is no second word, we don't know where to go...
      gui.println("Go where?");
      return;
    }

    String direction = command.getStringifiedArgs();

    // Try to leave current room.
    Room nextRoom = currentRoom.nextRoom(direction);
    
    if (nextRoom == null)
      gui.println(direction + " is not a valid direction.");
    else if (!currentRoom.canGoDirection(direction)){
      gui.println("That exit is locked! Come back later.");
    } else {
      currentRoom = nextRoom;
      if(currentRoom.getRoomName().equals("The Lair")){
        gui.println(currentRoom.shortDescription());
        sasquatch();
      } else if(currentRoom.getRoomName().equals("Fur Store")){
        gui.println(currentRoom.shortDescription());
        salesman();
      } else {
        gui.println(currentRoom.longDescription());
      }
    }
    gui.setGameInfo(inventory.getString(), currentRoom.getExits());
  }

  /**
   * Does things when you encounter the Sasquatch.
   */
  public void sasquatch(){
    gui.println("The Sasquatch steps out of the cave");
    gui.println("\"You have missed a day of school! You are my dinner now!\" He screams.");
    gui.println("What would you like to do?");
    Parser.showCommands();

  }

    /**
   * Does things when you enter the fur store. WORKS if you say yes the first time, kinda if you say no
   */
  public void salesman(){
    if (!inventory.getString().contains("Coonskin Hat")){
      gui.println("A man dressed in a puffy fur coat approaches you, with a fur hat in hand.");
      gui.println("\"Would you like to buy my furs? Only for a small fee of £500!\" He says.");
      gui.println("Will you buy the fur hat? (\"yes\"/\"no\")");
      if (buyFurs()){
        if (inventory.getString().contains("1000 British Pounds")){
        gui.println("\"Pleasure doing business with you, good sir.\"");
        // TODO implement inventory so you can get fur hat!
        // inventory.removeItem(pounds);
        // inventory.addItem(hat); 
        // inventory.addItem(euros);
        } else { //&& !inventory.getString().contains("1000 British Pounds")
        gui.println("\"Hmm... I can sense you are lacking the funds. What a shame.\"");
        }
      }
    } else {
      gui.println("A pelt-clothed man sits in the corner of the lodge, slowly counting his money...");
    }
  }

  /**
   * Asks user if they want to buy furs.
   * @return true or false
   */
  public boolean buyFurs(){
    boolean validInput = false;
    while(!validInput){
      String in = gui.readCommand();
      if (in.equalsIgnoreCase("y") || in.equalsIgnoreCase("yes")) return true;
      else if (in.equalsIgnoreCase("n") || in.equalsIgnoreCase("no")){
        gui.println("\"Then what are you doing in a fur shop? Buy something or get out!\"");
        validInput = true;
      } else {
        gui.println("\"" + in + "\" is not a valid choice!");
      }
    }
    return false;
  }

  /** 
   * @param secondWord
   */
  public void yell(String secondWord) {
    if (secondWord != null){
        gui.println(secondWord.toUpperCase() + "!!!!!!");
        gui.println("Feel better?");
      }else{
        gui.println("ARGHHHHH!!!!!!");
        gui.println("Feel better?");
      }
  }

  /**
   * Print out some help information. Here we print some stupid, cryptic message
   * and a list of the command words.
   */
  public void printHelp(Command command) {
    if (command.hasArgs()) Parser.printCommandHelp(command);
    else{
      //TODO: Fix help messages
      gui.println("You are lost. You are alone. You wander");
      gui.println("around at Monash Uni, Peninsula Campus.");
      gui.println();
      gui.println("Your command words are:");
      Parser.showCommands();
    }    
  }

  /**
   * Plays music.
   */
  public void music(Command command){
    if (!command.hasArgs()) gui.println("What do you want to do with the music?");
    else if (command.getStringifiedArgs().equals("stop")){
      Game.getMusicPlayer().stop();
      gui.println("Music stopped.");
    } 
    else if (command.getStringifiedArgs().equals("start")){
      Game.getMusicPlayer().play();
      gui.println("Music started!");
    } 
    else if (command.getStringifiedArgs().equals("play")){
      Game.getMusicPlayer().play();
      gui.println("Music started!");
    } 
    else if (Game.getMusicPlayer().getVolume() > -75.1f && command.getStringifiedArgs().equals("volume-down")){
      Game.getMusicPlayer().setVolume(Game.getMusicPlayer().getVolume() - 5);
      gui.println("Music volume down.");
    } 
    else if (Game.getMusicPlayer().getVolume() < -5f && command.getStringifiedArgs().equals("volume-up")){
      Game.getMusicPlayer().setVolume(Game.getMusicPlayer().getVolume() + 5);
      gui.println("Music volume up.");
    } 
    else if (Game.getMusicPlayer().getVolume() > -75.1f && command.getStringifiedArgs().equals("volume down")){
      Game.getMusicPlayer().setVolume(Game.getMusicPlayer().getVolume() - 5);
      gui.println("Music volume down.");
    } 
    else if (Game.getMusicPlayer().getVolume() < -5f && command.getStringifiedArgs().equals("volume up")){
      Game.getMusicPlayer().setVolume(Game.getMusicPlayer().getVolume() + 5);
      gui.println("Music volume up.");
    } 
    else {
      gui.println("Invalid music operation!");
    }
  }
  
  /**
   * Resets the game save state.
   */
  private void resetSaveState() {
    Save game = new Save();
    try {
      FileOutputStream fileOut = new FileOutputStream("src/data/game.ser");
      ObjectOutputStream out = new ObjectOutputStream(fileOut);
      out.writeObject(game);
      out.close();
      fileOut.close();
    } catch (IOException i) {
      i.printStackTrace();
    }
  }

  //Below are utility functions, serving a purpose only for internal game management.

  /**
   * Causes the currently executing thread to sleep (temporarily cease execution) 
   * for the specified number of milliseconds, subject to the precision and accuracy 
   * of system timers and schedulers.
   * @param m - milliseconds to sleep for.
   */
  public void sleep(long m){
    try {
      Thread.sleep(m);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }
}
