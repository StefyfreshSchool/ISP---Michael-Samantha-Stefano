import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.NotSerializableException;
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
  public static HashMap<String, Item> itemMap = new HashMap<String, Item>();
  private Inventory inventory;
  private Player player;
  private Parser parser;
  private Room currentRoom;
  Enemy sasquatch;
  Weapon geraldo;

  private static final int MAX_WEIGHT = 10;
  
  /**
   * Create the game and initialize its internal map.
   */
  public Game() {
    //Init GUI and player stuff
    gui = GUI.getGUI();
    gui.createWindow();
    inventory = new Inventory(MAX_WEIGHT);
    player = new Player(100);
    startMusic();

    //Init enemies and items
    //TODO: PUT THESE IN MAPS!! IMPORTANT!
    sasquatch = new Enemy("Sasquatch", "\"You have missed a day of school! You are my dinner now!\"", 25);
    geraldo = new Weapon();

    //Init rooms and game state
    try {
      initRooms("src\\data\\rooms.json");
      initItems("src/data/items.json");
      currentRoom = roomMap.get("South of the Cyan House");
      
      //Initialize the game if a previous state was recorded
      Save save = null;
      try (FileInputStream fileIn = new FileInputStream("src/data/game.ser")) {
        ObjectInputStream in = new ObjectInputStream(fileIn);
        save = (Save) in.readObject();
        in.close();
        fileIn.close();
      } catch (InvalidClassException e) {
        gui.printerr("InvalidClassException - a local class has been altered! Resetting game save.");
        resetSaveState();
      } catch (IOException e){
        System.err.println("Error while loading game state. Resetting.");
        resetSaveState();
      }

      if (save != null){
        gui.println("A previously saved game state was recorded.");
        gui.println("Would you like to restore from that save?");
        gui.println();
        gui.println("Type \"y\" to restore or \"n\" to ignore.");

        boolean validInput = false;
        while(!validInput){
          String in = gui.readCommand();
          if (in.equalsIgnoreCase("y") || in.equalsIgnoreCase("yes")){
            gui.reset();

            roomMap = save.getRoomMap();
            inventory = save.getInventory();
            currentRoom = save.getCurrentRoom();
            player = save.getPlayer();
            gui.printInfo("Restored from saved game.\n");
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

  private void initItems(String fileName) {
    for (Object itemObj : Item.getItems()){
      String itemId = (String) ((JSONObject) itemObj).get("id");
      String name = (String) ((JSONObject) itemObj).get("name");

      Object quantity = ((JSONObject) itemObj).get("quantity");
      Object weight = ((JSONObject) itemObj).get("weight");
      boolean isOpenable = (boolean) ((JSONObject) itemObj).get("isOpenable");
      String description = (String) ((JSONObject) itemObj).get("description");
      String startingRoom = (String) ((JSONObject) itemObj).get("startingRoom");
      ArrayList<String> aliases = new ArrayList<String>();
      for (Object alias : (JSONArray) ((JSONObject) itemObj).get("aliases")) {
        aliases.add((String) alias);
      }

      Item item;
      if (quantity == null){
        item = new Item(Integer.parseInt(weight + ""), name, startingRoom, isOpenable, description, aliases);
      } else {
        item = new Item(Integer.parseInt(weight + ""), name, startingRoom, isOpenable, description, aliases, ((Long) quantity).intValue());
      }

      itemMap.put(itemId, item);

      for (String alias : aliases) {
        itemMap.put(alias, item);
      }
    }
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

  /** Main play routine. Loops until end of play. */
  public void play() {
    printWelcome();
    gui.setGameInfo(inventory.getString(), player.getHealth(), currentRoom.getExits());
    
    
    // Enter the main command loop. Here we repeatedly read commands and
    // execute them until the game is over.
    boolean finished = false;
    while (!finished) {
      Command command;
      command = parser.getCommand();
      int status = processCommand(command);
      gui.setGameInfo(inventory.getString(), player.getHealth(), currentRoom.getExits());
      if (status == 1){
        finished = true;
      } 
      if (status == 2){
        music.stop();
        gui.reset();
        gui.printInfo("Game restarted.\n");
        try {
          initRooms("src\\data\\rooms.json");
          currentRoom = roomMap.get("South of the Cyan House");
          inventory = new Inventory(MAX_WEIGHT);
        } catch (Exception e) {
          e.printStackTrace();
        }
        printWelcome();
        startMusic();
        gui.setGameInfo(inventory.getString(), player.getHealth(), currentRoom.getExits());
      }
      
    }

    gui.println("\nThank you for playing. Good bye.");

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

  /** Print out the opening message for the player. */
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
    if (commandWord.equals("test")) testing(command);
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
      hit(command);
    } else if (commandWord.equals("restart")) {
      if (quitRestart("restart", command)){
        resetSaveState();
        return 2;
      }
    } else if (commandWord.equals("save")){
      if (save(command)) return 1;
    } else if (commandWord.equals("take")){
      take(command);
    } else if (commandWord.equals("heal")){
      heal(command);
    } else if (commandWord.equals("wear")){
      wear(command);
    }
    return 0;
  }

  /**
   * VERY IMPORTANT. lets player wear hat
   * @param command what the player is wearing
   * This method is for testing the game.
   * FEEL FREE to add stuff for testing things!!
   */
  private void testing(Command command) {
    //In the game, type "test #" to activate one of the following tests.
    if (command.getStringifiedArgs().equals("1")){
      inventory.addItem(itemMap.get("pounds"));
      salesman();
    } else if (command.getStringifiedArgs().equals("2")){

    } 
  }

  /**
   * Allows the player to hit an enemy.
   * @param command - 
   */
  private void hit(Command command) {
    if (command.isUnknown()) {
      gui.println("I don't know what you mean...");
    }
    String commandWord = command.getCommandWord();
    if(commandWord.equals("hit")){
      int healthstandin;
      Enemy enemy;
      //Weapon weapon;
      if(currentRoom.getRoomName().equals("The Lair")){
        enemy = new Enemy(sasquatch);
        //weapon = new Weapon();
      }
      /*enemy.setHealth(weapon.getDamage());
      if(enemy.getHealth()<=0){
        healthstandin=0;
      }else{
        healthstandin = enemy.getHealth();
      }
        gui.println("The "+enemy.getName()+" lost 10 Health points. It has "+healthstandin+" left.");
      if(healthstandin==0){
        gui.println("The "+enemy.getName()+" has died.");
      }*/
    }
    Enemy enemy;
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
    } else if (command.getLastArg().equalsIgnoreCase("game")){ 
    } else if (command.getLastArg().equalsIgnoreCase("load")){
      loadSave();
      gui.setGameInfo(inventory.getString(), player.getHealth(), currentRoom.getExits());
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

    Save game = new Save(roomMap, inventory, currentRoom, player);
    try {
      FileOutputStream fileOut = new FileOutputStream("src/data/game.ser");
      ObjectOutputStream out = new ObjectOutputStream(fileOut);
      out.writeObject(game);
      out.close();
      fileOut.close();

      gui.println(quit ? "Game saved! Quitting." : "Game saved!");
    } catch (NotSerializableException e){
      gui.printerr("NotSerializableException - A class that needs to be saved does not implement Serializable!");
    } catch (IOException e){
      e.printStackTrace();
    }

    return quit;
  }

  /**
   * Allows the game to load a previously saved state of the game.
   */
  private void loadSave() {
    Save save = null;
    try {
      FileInputStream fileIn = new FileInputStream("src/data/game.ser");
      ObjectInputStream in = new ObjectInputStream(fileIn);
      save = (Save) in.readObject();
      in.close();
      fileIn.close();

      if (save != null){
        roomMap = save.getRoomMap();
        inventory = save.getInventory();
        currentRoom = save.getCurrentRoom();
        player = save.getPlayer();
        
        music.stop();
        startMusic();
        gui.reset();
        gui.printInfo("Game reloaded from saved data.\n");
        gui.println(currentRoom.longDescription());
        gui.setGameInfo(inventory.getString(), player.getHealth(), currentRoom.getExits());
      } else {
        gui.println("There is no valid state to load!");
      }
    } catch (ClassNotFoundException | IOException e) {
      gui.printerr("Error while loading! Could not load.");
      e.printStackTrace();
    }   
  }

  /**
   * Prompts the user if they want to quit or restart the game. 
   * After user input, it returns true or false.
   * @param string - Prints whether the operation is a quit or restart.
   * @return True or false based on if the user cancelled the operation or not.
   */
  private boolean quitRestart(String string, Command command) {
    if (command.getLastArg().equalsIgnoreCase("confirm")) return true;
    else if (command.getArgs() != null){
      gui.println("Not a valid restart command!");
      return false;
    }
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
    gui.setGameInfo(inventory.getString(), player.getHealth(), currentRoom.getExits());
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
    gui.setGameInfo(inventory.getString(), player.getHealth(), currentRoom.getExits());
    if (!inventory.hasItem(itemMap.get("Coonskin Hat"))){
      gui.println("A man dressed in a puffy fur coat approaches you, with a fur hat in hand.");
      gui.println("\"Would you like to buy my furs? Only for a small fee of £500!\" He says.");
      gui.println("Will you buy the fur hat? (\"yes\"/\"no\")");
      if (buyFurs()){
        if (inventory.hasItem(itemMap.get("1000 British Pounds"))){
          gui.println("\"Pleasure doing business with you, good sir.\"");
          inventory.removeItem(itemMap.get("pounds"));
          inventory.addItem(itemMap.get("hat")); 
          inventory.addItem(itemMap.get("euros"));
        } else {
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

  private void wear(Command command) {
    String commandWord = command.getCommandWord();
    if ((commandWord.equals("hat") || commandWord.equals("cap")) && inventory.getString().contains("Coonskin Hat")){
      gui.println("You are now wearing the fur cap. How stylish");
    } else {
      gui.println("You cannot wear that!");
    }
  }

  /**
   * when player types "heal" (no args).
   */
  private void heal(Command command) {
    if (inventory.getString().contains("Bandages") && player.getHealth() != 100){
      player.maxHeal();
      inventory.getItem(inventory.find("Bandages")).setQuantity();
      gui.println("Your wounds have healed. You have been restored to full health.");
    } else if (inventory.getString().contains("Bandages") && player.getHealth() == 100){
      gui.println("You are already at maximum health!");
    } else {
      gui.println("You have no healing items!");
    }
    gui.println("Your current health is " + player.getHealth() + ".");
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
    try {
      FileOutputStream fileOut = new FileOutputStream("src/data/game.ser");
      ObjectOutputStream out = new ObjectOutputStream(fileOut);
      out.writeObject(null);
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
