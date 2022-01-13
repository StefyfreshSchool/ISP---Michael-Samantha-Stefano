import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.awaitility.Awaitility;

public class Game implements java.io.Serializable {
  private static final String GAME_SAVE_LOCATION = "data/Game Save.ser";
  private transient static GUI gui;
  private static MusicPlayer music;
  public static HashMap<String, Room> roomMap;
  public static HashMap<String, Item> itemMap;
  public static HashMap<String, Enemy> enemyMap;
  private Inventory inventory;
  private Player player;
  private Parser parser;
  private Room currentRoom;
  private Room pastRoom;
  private boolean isInTrial;
  private boolean hasAnsweredNewsQuestions;
  private boolean supportCheck;
  private boolean hasOpenedVault;

  /**
   * Create the game and initialize its internal map.
   */
  public Game() {
    gui = GUI.getGUI();
    gui.sendGameObj(this);

    //Check that all dependencies are present
    try {
      existJavaDependencies();
    } catch (Error e) {
      GameError.javaDependenciesNotFound();
    }

    // init player stuff
    inventory = new Inventory(50);
    player = new Player(100);

    //Init rooms and game state
    try {
      initItems();
      initRooms();
      initEnemies();
      startMusic();
      currentRoom = roomMap.get("South of the Cyan House");
      hasAnsweredNewsQuestions = false;
      supportCheck = false;
      hasOpenedVault = false;
      
      //Initialize the game if a previous state was recorded
      Save save = null;
      try {
        FileInputStream fileIn = new FileInputStream(GAME_SAVE_LOCATION);
        ObjectInputStream in = new ObjectInputStream(fileIn);
        save = (Save) in.readObject();
        in.close();
        fileIn.close();
      } catch (InvalidClassException | ClassNotFoundException e) {
        gui.printerr("A local class has been altered without resetting the game save! Resetting.");
        gui.println();
        resetSaveState();
      } catch (FileNotFoundException e){
        gui.printInfo("No game save file was found! Creating file.");
        gui.println();
        resetSaveState();
      } catch (IOException e){
        gui.printerr("Error while loading saved game. Resetting.");
        gui.println();
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
            pastRoom = save.getPastRoom();
            currentRoom = save.getCurrentRoom();
            player = save.getPlayer();
            enemyMap = save.getEnemyMap();
            gui.printInfo("Restored from saved game.\n");
            validInput = true;
          } else if (in.equalsIgnoreCase("n") || in.equalsIgnoreCase("no") || in.equalsIgnoreCase("cancel")){
            gui.reset();
            gui.printInfo("Ignoring old game state.\n");
            resetSaveState();
            validInput = true;
          } else {
            gui.println("\"" + in + "\" is not a valid choice!");
          }
        } 
      }
    } catch (Exception e) {
      e.printStackTrace();
      gui.printerr("ERROR! Could not initialize the game!");
    }

    parser = new Parser();
  }

  /**Checks if the required Java dependencies are accessible. */
  private void existJavaDependencies() {
    new JSONArray();
    new Awaitility();
  }

  private void initEnemies() {
    if (Enemy.getEnemies() == null) GameError.fileNotFound("data/enemies.json");
    enemyMap = new HashMap<String, Enemy>();
    for (Object enemyObj : Enemy.getEnemies()){
      String id = (String) ((JSONObject) enemyObj).get("id");
      String name = (String) ((JSONObject) enemyObj).get("name");
      String catchphrase = (String) ((JSONObject) enemyObj).get("catchphrase");
      Long health = (Long) ((JSONObject) enemyObj).get("health");
      Long damageMin = (Long) ((JSONObject) enemyObj).get("damageMin");
      Long damageMax = (Long) ((JSONObject) enemyObj).get("damageMax");
      ArrayList<String> messages = new ArrayList<String>();
      for (Object message : (JSONArray) ((JSONObject) enemyObj).get("messages")) {
        messages.add((String) message);
      }
      ArrayList<String> aliases = new ArrayList<String>();
      for (Object alias : (JSONArray) ((JSONObject) enemyObj).get("aliases")) {
        aliases.add((String) alias);
      }

      Enemy enemy = new Enemy(name, catchphrase, health.intValue(), damageMin.intValue(), damageMax.intValue(), messages, aliases);
      enemyMap.put(id, enemy);
      for (String alias : aliases) {
        enemyMap.put(alias, enemy);
      }
    }
    isInTrial = false;
  }

  private void initItems() {
    if (Item.getItems() == null) GameError.fileNotFound("data/items.json");
    itemMap = new HashMap<String, Item>();
    for (Object itemObj : Item.getItems()){
      String itemId = (String) ((JSONObject) itemObj).get("id");
      String name = (String) ((JSONObject) itemObj).get("name");

      Long quantity = (Long) ((JSONObject) itemObj).get("quantity");
      Object weight = ((JSONObject) itemObj).get("weight");
      boolean isTakeable = (boolean) ((JSONObject) itemObj).get("isTakeable");
      boolean isWeapon = (boolean) ((JSONObject) itemObj).get("isWeapon");
      String description = (String) ((JSONObject) itemObj).get("description");
      String startingRoom = (String) ((JSONObject) itemObj).get("startingRoom");
      Long damage = (Long) ((JSONObject) itemObj).get("damage");
      ArrayList<String> aliases = new ArrayList<String>();
      for (Object alias : (JSONArray) ((JSONObject) itemObj).get("aliases")) {
        aliases.add((String) alias);
      }

      Item item;
      if (quantity == null && !isWeapon){
        item = new Item(Integer.parseInt(weight + ""), name, startingRoom, isTakeable, description, aliases);
      } else if (isWeapon) {
        item = new Item(Integer.parseInt(weight + ""), name, startingRoom, isTakeable, description, aliases, isWeapon, damage.intValue());
      } else {
        item = new Item(Integer.parseInt(weight + ""), name, startingRoom, isTakeable, description, aliases, quantity.intValue());
      }
      itemMap.put(itemId, item);

      for (String alias : aliases) {
        itemMap.put(alias, item);
      }
    }
  }

  private void initRooms() {
    if (Room.getRooms() == null) GameError.fileNotFound("data/rooms.json");
    roomMap = new HashMap<String, Room>();
    for (Object roomObj : Room.getRooms()) {
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
    
    // Enter the main command loop. Here we repeatedly read commands and
    // execute them until the game is over.
    boolean finished = false;
    while (!finished) {
      Command command;
      command = parser.getCommand();
      processCommand(command);
    }
  }

  /**Starts the background music. */
  private void startMusic() {
    try {
      music = new MusicPlayer("data/audio/background.wav", true);
    } catch (FileNotFoundException e) {
      GameError.fileNotFound("data/audio/background.wav");
    }
    music.setVolume(-25f);
    music.play();
  }

  public static MusicPlayer getMusicPlayer() {
    return music;
  }

  /** Print out the opening message for the player. */
  private void printWelcome() {
    gui.reset();
    gui.println("Welcome to Zork!");
    gui.println("Zork is an amazing text adventure game!");
    gui.println("Type 'help' for more information about the game and the available commands.");
    gui.println();
    gui.println(currentRoom.longDescription());
  }

  /**
   * Given a command, process (that is: execute) the command.
   * @param command
   * @return {@code 0} if no action is required, {@code 1} if the game should quit, {@code 2} if the game should restart
   */
  private boolean processCommand(Command command) {
    if (command.isUnknown()) {
      gui.println("I don't know what you mean...");
      return false;
    } 
    String commandWord = command.getCommandWord();
    if (commandWord.equals("test")) testing(command);
    else if (!command.isUnknown() && command.getFirstArg().equals("/?")){
      ArrayList<String> args = new ArrayList<String>();
      args.add(commandWord);
      Parser.printCommandHelp(new Command("help", args));
    } else if (commandWord.equals("help")){
      printHelp(command);
    }
    else if (commandWord.equals("go")){
      goRoom(command);
    }
    else if (commandWord.equals("quit")) {
      if (quitRestart("quit", command)) endGame();
    } else if (commandWord.equals("yell")){
      yell(command.getStringifiedArgs());
    
    } else if (commandWord.equals("music")) {
      music(command);

    } else if(commandWord.equals("hit")){
      hit(command);
    } else if (commandWord.equals("restart")) {
      if (quitRestart("restart", command)){
        restartGame();
        return true;
      } 
    } else if (commandWord.equals("save")){
      if (save(command)) endGame();
    } else if (commandWord.equals("take")){
      take(command);
    } else if (commandWord.equals("threaten")){
      threaten(command);
    } else if (commandWord.equals("drop")){
      drop(command);
    } else if (commandWord.equals("heal")){
      heal();
    } else if (commandWord.equals("wear")){
      wear(command.getStringifiedArgs());
    } else if (commandWord.equals("read")){
      read(command.getStringifiedArgs());
    } else if (commandWord.equals("pray")){
      pray();
    } else if (commandWord.equals("inflate")){
      inflate(command.getStringifiedArgs());
    } else if (commandWord.equals("info")){
      gui.println(currentRoom.longDescription());
      gui.println();
      gui.println("Player info:");
      gui.println("Inventory: " + inventory.getString());
      gui.println("Health: " + player.getHealth());
      gui.println();
    } else if (commandWord.equals("cls")){
      gui.reset();
    } else {
      gui.println("That command has no logic...");
    }
    return false;
  }

  private void restartGame() {
    resetSaveState();
    gui.reset();
    gui.printInfo("Game restarted.\n");
    try {
      initItems();
      initRooms();
      initEnemies();
      isInTrial = false;
      currentRoom = roomMap.get("South of the Cyan House");
      inventory = new Inventory(50);
    } catch (Exception e) {
      e.printStackTrace();
    }

    printWelcome();
  }

  private void endGame() {
    music.stop();
    gui.println("Thank you for playing. Goodbye!");

    //Nice transition to exit the game
    sleep(1000);
    System.exit(0);
  }

  /**
   * This method is for testing the game.
   * FEEL FREE to add stuff for testing things!!
   */
  private void testing(Command command) {
    // gui.println("Don't you dare use this command if you aren't a dev!");
    // return;
    // In the game, type "test #" to activate one of the following tests.
    String c = command.getStringifiedArgs();
    if (c.equals("1")){
      inventory.addItem(itemMap.get("pounds"));
      salesman();
    } else if (c.equals("2")){
      currentRoom = roomMap.get("Castle Grounds");
    } else if (c.equals("3")){
      currentRoom = roomMap.get("North of Crater");
    } else if (c.equals("4")){
      inventory.addItem(itemMap.get("sword"));
      inventory.addItem(itemMap.get("bottle"));
      currentRoom = roomMap.get("Mystery Door of Mystery");
    } else if (c.equals("5")){
      inventory.addItem(itemMap.get("sword"));
      inventory.addItem(itemMap.get("bottle"));
      currentRoom = roomMap.get("Caldera Bridge");
    } else if (c.equals("6")){
      player.talkedToSkyGods();
    } else if (c.equals("7")){
    } else if (c.equals("8")){
    } else if (c.equals("")){
      player.maxHeal();
    } else if (c.equals("crash")){
      GameError.crashGame();
    } else if (c.equals("img")){
      gui.printImg("data/images/img.png");
    }
    gui.println("Test activated.");
  }

  private Enemy enemyRoomCheck(Room room){
    String name = room.getRoomName();
    if (name.equals("The Lair")){
      return enemyMap.get("sasquatch");
    } else if(name.equals("Lower Hall of Enemies")){
      return enemyMap.get("vaccuum");
    } else if(name.equals("Upper Hall of Enemies")){
      return enemyMap.get("friends robot");
    } else if(name.equals("Dept. of Customer Service")){
      return enemyMap.get("balloony");
    } else if(name.equals("Hall of the Volcano King")){
      return enemyMap.get("deslauriers");
    } return null;
  }

  /**
   * Allows the player to hit an enemy.
   * @param command - 
   */
  private void hit(Command command) {
    int enemyHealth;
    Enemy enemy = enemyRoomCheck(currentRoom);
    if (enemy == null){
        gui.println("There is no enemy here. You cannot hit anything.");
    } else {
      ArrayList<String> args = command.getArgs();
      String argsStr = command.getStringifiedArgs().toLowerCase(); 
      if (!command.hasArgs() || argsStr.indexOf("with") == 0) { // hit, no args
        gui.println("Hit what enemy?");
      } else if (!args.contains("with") && enemyMap.get(argsStr.trim()) == null) { // hit, invalid enemy
        gui.println(argsStr + " is not an enemy.");
        gui.println("What would you like to hit?");
      } else if (!args.contains("with") && enemyMap.get(argsStr.substring(0, argsStr.indexOf("with")).trim()) == null) { // hit with, invalid enemy
        gui.println(argsStr.substring(0, argsStr.indexOf("with")).trim() + " is not an enemy.");
        gui.println("Who would you like to hit?");
      } else if (((!args.contains("geraldo") || !args.contains("sword") || !args.contains("water")) && command.getLastArg().equals("with")) || !args.contains("with")){ // hit, missing either weapon or with
        gui.println("Hit with what weapon?");
      } else if (!itemMap.get("geraldo").isThisItem(argsStr.substring(argsStr.indexOf(" with ") + 6).trim()) && !itemMap.get("sword").isThisItem(argsStr.substring(argsStr.indexOf(" with ") + 6).trim()) && !itemMap.get("water").isThisItem(argsStr.substring(argsStr.indexOf(" with ") + 6).trim())){ // hit enemy with, invalid weapon
        String weirdItemName = argsStr.substring(argsStr.indexOf(" with ") + 6, argsStr.length());
        gui.println(weirdItemName + " is not a weapon.");
        gui.println("What would you like to hit " + enemy.getName() + " with?");
      } else if (!enemyMap.get(argsStr.substring(0, argsStr.indexOf("with")).trim()).isThisEnemy(enemy.getName())){ // valid enemy, invalid room
        gui.println(enemyMap.get(argsStr.substring(0, argsStr.indexOf("with")).trim()) + " is not an enemy in this room.");
      } else if (enemyMap.get("friends robot").isThisEnemy(enemy.getName()) && !enemyMap.get("friends robot").getIsDead()) {
        String weaponName = argsStr.substring(argsStr.indexOf(" with ") + 6, argsStr.length());
        gui.println("The " + weaponName + " just bounces off its titanium armor. It dealt 0 damage.");
        gui.println("Maybe there's another way to defeat it?");
      } else if (enemyMap.get("deslauriers").isThisEnemy(enemy.getName()) && enemy.getHealth() <= 25 && !supportCheck) {
        String weaponName = argsStr.substring(argsStr.indexOf(" with ") + 6, argsStr.length());
        gui.println("Mr. DesLauriers eyes start to glow.");
        gui.println("The enemy has become too strong! The " + weaponName + " isn't doing any damage to it!");
      } else { // hit enemy with weapon
        if (enemy.getHealth() > 0){
          Item item = itemMap.get(command.getLastArg());
          enemy.attacked(item.getDamage());
          if (enemy.getHealth() <= 0) {
            enemyHealth = 0;
          } else {
            enemyHealth = enemy.getHealth();
          }
          if (item.getName().equals("Geraldo")){
            gui.print("You aim a rock at the enemy. ");
          } else if (item.getName().equals("Bottle of Water")){
            gui.print("You whack the enemy in the head. ");
          } else if (item.getDamage() == 25){
            gui.print("You valiantly slice the enemy. ");
          } else {
            gui.print("Through the power of moral support, you valiantly slice the enemy. ");
          }
          if (enemy.getName().equals(enemyMap.get("deslauriers").getName()) || enemy.getName().equals(enemyMap.get("balloony").getName())){
            gui.println(enemy.getName() + " loses " + item.getDamage() + " HP. It has " + enemyHealth + " HP left.");
          } else {
            gui.println("The " + enemy.getName() + " loses " + item.getDamage() + " HP. It has " + enemyHealth + " HP left.");
          }
          if (enemyHealth == 0) {
            enemy.setIsDead(true);
            if (enemy.getName().equals(enemyMap.get("deslauriers").getName()) || enemy.getName().equals(enemyMap.get("balloony").getName())){
              gui.println(enemy.getName() + " has been defeated.");
            } else {
              gui.println("The " + enemy.getName() + " has been defeated.");
            }
          }
        } else {
          gui.println("The " + enemy.getName() + " has already been defeated.");
        }
      }
    }
  }

  /**
   * Allows the player to threaten an enemy.
   * @param command - 
   */
  private void threaten(Command command) {
    Enemy enemy = enemyRoomCheck(currentRoom);
    if (enemy == null){
        gui.println("You are imposing. You are powerful. You stand a little bit straighter.");
    } else {
      ArrayList<String> args = command.getArgs();
      String argsStr = command.getStringifiedArgs();
      if (!command.hasArgs() || argsStr.indexOf("with") == 0){ // threaten, no args
        gui.println("Threaten what enemy?");
      } else if (!args.contains("with") && Game.enemyMap.get(argsStr.trim()) == null){ // threaten, invalid enemy
        gui.println(argsStr + " is not an enemy.");
        gui.println("What would you like to threaten?");
      } else if (args.contains("with") && Game.enemyMap.get(argsStr.substring(0, argsStr.indexOf("with")).trim()) == null){ // threaten with, invalid enemy
        gui.println(argsStr.substring(0, argsStr.indexOf("with")).trim() + " is not an enemy.");
        gui.println("What would you like to threaten?");
      } else if ((!args.contains("water") && command.getLastArg().equals("with")) || !args.contains("with")){ // threaten with, no weapon
        gui.println("Threaten with what?");
      } else if (!itemMap.get("water").isThisItem(argsStr.substring(argsStr.indexOf(" with ") + 6).trim())){ // threaten enemy with, invalid weapon
        gui.println("That doesn't seem to scare the enemy.");
      } else if (!enemyMap.get(args.get(0)).isThisEnemy(enemy.getName())){ // valid enemy, invalid room
        gui.println("You can't find " + args.get(0) + " anywhere.");
      } else { // threaten enemy with weapon
        if (enemyMap.get("friends robot").isThisEnemy(enemy.getName()) && !enemyMap.get("friends robot").getIsDead()){
          enemyMap.get("friends robot").setIsDead(true);
          enemyMap.get("friends robot").setHealth(0);
          gui.println("The Friends Robot cowers in fear from your dominance. It seems to be perturbed from the water bottle in your hand.");
          gui.println("\"Please don't hurt me! I have friends!\" it says, with a quaver in its voice.");
          gui.println("Trembling quietly, it moves out of your path, revealing a carefully chiseled inscription in the wall.");
          gui.println("The wall states: \"Pray before the three\". What could that possibly mean?");
        } else {
          gui.println("That doesn't seem to do anything.");
        }
      }
    }
  }

  /**
   * Allows the player to take items from the current room. Also now prints description.
   * @param command
   */
  private void take(Command command) {
    if (!command.hasArgs()){
      gui.println("Take what?");
      return;
    }
    String itemName = command.getStringifiedArgs();
    if (!Item.isValidItem(itemName)){
      gui.println("I don't know what you mean.");
    } else if (!currentRoom.containsItem(itemName)){
      gui.println("You can't seem to find that item here.");
    } else if (!currentRoom.getItem(itemName).isTakeable()) {
      gui.println("You can't take that item.");
    } else {
      if (inventory.addItem(currentRoom.getItem(itemName))){
        gui.println(currentRoom.getItem(itemName).getName() + " taken!");
        gui.println(currentRoom.getItem(itemName).getDescription());
        if (currentRoom.getItem(itemName).getDamage() != 0){
          gui.println("Deals " + currentRoom.getItem(itemName).getDamage() + " HP to enemies.");
        }
        currentRoom.removeItem(itemName);
      } else {
        gui.println("You are stuffed! You have no more room to take items.");
      }
    }
  }

  private void drop(Command command) {
    if (!command.hasArgs()){
      gui.println("Drop what?");
      return;
    }
    String itemName = command.getStringifiedArgs().toLowerCase();
    if (!Item.isValidItem(itemName)){
      gui.print("Not a valid item!");
    } else if (!inventory.hasItem(itemMap.get(itemName))){
      gui.print("You don't seem to have that item.");
    } else {
      Item item = itemMap.get(itemName);
      inventory.removeItem(item);
      currentRoom.addItem(item);
      gui.println("You dropped " + item.getName() + ".");
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
    } else if (command.getLastArg().equalsIgnoreCase("game") || !command.hasArgs()){ 
    } else if (command.getLastArg().equalsIgnoreCase("load")){
      loadSave();
      return false;
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

    Save game = new Save(roomMap, inventory, currentRoom, pastRoom, player, enemyMap);
    try {
      FileOutputStream fileOut = new FileOutputStream(GAME_SAVE_LOCATION);
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
      FileInputStream fileIn = new FileInputStream(GAME_SAVE_LOCATION);
      ObjectInputStream in = new ObjectInputStream(fileIn);
      save = (Save) in.readObject();
      in.close();
      fileIn.close();

      if (save != null){
        roomMap = save.getRoomMap();
        inventory = save.getInventory();
        pastRoom = save.getPastRoom();
        currentRoom = save.getCurrentRoom();
        player = save.getPlayer();
        enemyMap = save.getEnemyMap();
        
        gui.reset();
        gui.printInfo("Game reloaded from saved data.\n");
        gui.println(currentRoom.longDescription());
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
    if (command.getLastArg().equalsIgnoreCase("confirm") || command.getLastArg().equalsIgnoreCase("y")) return true;
    else if (command.getArgs() != null){
      gui.println("Not a valid " + string + " command!");
      return false;
    }
    gui.println("Are you sure you would like to " + string + " the game?");
    gui.println("Type \"y\" to confirm or \"n\" to cancel.");
    boolean validInput = false;
    while(!validInput){
      String in = gui.readCommand();
      if (in.equalsIgnoreCase("y") || in.equalsIgnoreCase("yes")) return true;
      else if (in.equalsIgnoreCase("n") || in.equalsIgnoreCase("no") || in.equalsIgnoreCase("cancel")){
        gui.println("Not " + (string.equals("quit") ? "quitting." : "restarting."));
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
    Room pastRoom = currentRoom;
    Room nextRoom = currentRoom.nextRoom(direction);
    
    if (nextRoom == null)
      gui.println(direction + " is not a valid direction.");
    else if (!currentRoom.canGoDirection(direction, inventory, player)){
      gui.println("You can't go this way yet. Try looking around.");
    } else {
      if(!isInTrial && (currentRoom.getRoomName().equals("The Lair") || nextRoom.getRoomName().equals("The Lair"))){
        currentRoom = nextRoom;
        gui.println(currentRoom.shortDescription());
        sasquatch();
        if(inventory.hasItem(itemMap.get("pounds"))){
          player.setTrial(0);
        }
      } else if (!isInTrial && (currentRoom.getRoomName().equals("Upper Hall of Enemies") || nextRoom.getRoomName().equals("Upper Hall of Enemies"))) {
        currentRoom = nextRoom;
        gui.println(currentRoom.shortDescription());
        robot();
      } else if (!isInTrial && (currentRoom.getRoomName().equals("Lower Hall of Enemies") || nextRoom.getRoomName().equals("Lower Hall of Enemies"))) {
        currentRoom = nextRoom;
        gui.println(currentRoom.shortDescription());
        vaccuum();
      } else if (!isInTrial && (currentRoom.getRoomName().equals("Hall of the Volcano King") || nextRoom.getRoomName().equals("Hall of the Volcano King"))) {
        currentRoom = nextRoom;
        gui.println(currentRoom.shortDescription());
        deslauriers();
      } else if (!isInTrial){
        currentRoom = nextRoom;
        gui.println(currentRoom.longDescription());
      } else {
        gui.println("You cannot leave while the enemy is still at large!");
      }
      if(currentRoom.getRoomName().equals("Fur Store")){
        gui.println(currentRoom.shortDescription());
        salesman();
      }
      if (currentRoom.getRoomName().equals("Cheese Vault")){
        cheeseVault();
      }
      if (currentRoom.getRoomName().equals("News News Vault")){
        newsNewsScroll();
      }
      if (currentRoom.getRoomName().equals("Mysterious Entrance")){
        frogsMadleneandJorge();
      }
    }
    if (pastRoom.getRoomName().equals("The Lair") && currentRoom.getRoomName().equals("North of Crater")){
      if (inventory.hasItem(itemMap.get("pounds"))){
        player.setTrial(0);
      }
    }else if((pastRoom.getRoomName().equals("Lower Hall of Enemies") && currentRoom.getRoomName().equals("Upper Hall of Enemies"))||(pastRoom.getRoomName().equals("Lower Hall of Enemies") && currentRoom.getRoomName().equals("Mystery Door of Mystery"))){
      if (inventory.hasItem(itemMap.get("key"))){
        player.setTrial(4);
      }
    }else if (pastRoom.getRoomName().equals("News News Vault") && currentRoom.getRoomName().equals("News News Temple")){
      if (inventory.hasItem(itemMap.get("scroll"))){
        player.setTrial(1);
      }
    }else if (pastRoom.getRoomName().equals("Cheese Vault") && currentRoom.getRoomName().equals("Upper Atrium")){
      if (inventory.hasItem(itemMap.get("cheese"))){
        player.setTrial(2);
      }
    }else if (pastRoom.getRoomName().equals("Dept. of Customer Service") && currentRoom.getRoomName().equals("Parliament Entrance Room")){
      if (inventory.hasItem(itemMap.get("corpse"))){
        player.setTrial(6);
      }
    }
  }

  /**
   * Does things when you encounter the Sasquatch.
   */
  public void sasquatch(){
    Enemy sasquatch = enemyMap.get("sasquatch");
    if (!(sasquatch.getHealth() <= 0)){
      isInTrial = true;
      gui.println("The Sasquatch steps out of the cave.");
      gui.println(sasquatch.getCatchphrase() + " He screams.");
      if (enemyAttack(sasquatch)) return;
      gui.println("Just inside of the cave you can see muddy pieces of paper. What are they?");
      isInTrial = false;
      currentRoom.getItem("pounds").isTakeable(true);
    } else if ((sasquatch.getHealth() <= 0) && currentRoom.getRoomName().equals("The Lair")) {
      gui.println("The sasquatch's corpse lies strewn on the ground.");
      gui.println("Past the corpse, you can see a dark, ominous cave.");
      if (!player.getTrial(0)) {
        gui.println("Just inside of the cave you can see muddy pieces of paper. What are they?");
      } else if (player.getTrial(0)) {
        gui.println("Your conscience speaks to you. \"There are more important things to do than explore perilous caves.\" You know you must leave this place.");
      }
    }
  }

    /**
   * Does things when you encounter the Vaccuum.
   */
  public void vaccuum(){
    Enemy vaccuum = enemyMap.get("vaccuum");
    if (vaccuum.getHealth() > 0){
      isInTrial = true;
      gui.println("The Vaccuum wheels itself towards you.");
      gui.println(vaccuum.getCatchphrase() + " Your ears ache from the noise.");
      if (enemyAttack(vaccuum)) return;
      gui.println("Past its lifeless body, you can see an aluminum ladder.");
      gui.println("A brass key lies on the floor, dropped by the vaccuum.");
      currentRoom.getItem("key of friendship").isTakeable(true);
      isInTrial = false;
    } else if(vaccuum.getHealth() < 1 && currentRoom.getRoomName().equals("Lower Hall of Enemies")){
      gui.println("The vaccuum sits on the concrete floor, out of battery.");
      if(!player.getTrial(4)){
        gui.println("A brass key lies on the floor, dropped by the vaccuum.");
      }
      if (!enemyMap.get("friends robot").getIsDead()){
        gui.println("Past its lifeless body, you can see an aluminum ladder.");
      }
    }
  }

  /**
   * Does things when you encounter the friends robot.
   */
  public void robot(){
    Enemy robot = enemyMap.get("friends robot");
    if (robot.getHealth() > 0){
      isInTrial = true;
      gui.println("The Friends Robot marches mechanically, gazing at you with a happy expression.");
      gui.println(robot.getCatchphrase() + " It beeps. It is blocking your path. You have no choice but to defeat it.");
      if (enemyAttack(robot)) return;
      isInTrial = false;
      player.setTrial(5);
    }
  }

  public void deslauriers(){
    Enemy deslauriers = enemyMap.get("deslauriers");
    if (!deslauriers.getIsDead()){
      isInTrial = true;
      gui.println("Mr. DesLauriers stands up from his throne. He is twelve feet tall.");
      gui.println(deslauriers.getCatchphrase() + " He yells.");
      if (enemyAttack(deslauriers)) return;
      gui.println("Mr. DesLauriers ascends towards the gods, eyes illuminated. With a flash, he disappears.");
      gui.println("The world seems a little more vibrant.");
      isInTrial = false;
    } else if (deslauriers.getIsDead() && currentRoom.getRoomName().equals("Hall of the Volcano King")) {
      gui.println("The world seems a little more vibrant.");
    }
  }

  private boolean enemyAttack(Enemy enemy) {
    while(enemy.getHealth() > 0){
      int tempDamage = enemy.getDamage();
      Command command = parser.getCommand();
      boolean exit = processCommand(command);
      if (exit) return true;
      if (!enemy.getIsDead()){
        if (enemy.isThisEnemy("deslauriers") && player.getHealth() - tempDamage < 1){
          tempDamage = player.getHealth() - 1;
          moralSupport();
        }
        if (!supportCheck){
          player.setHealth(tempDamage);
          gui.println(enemy.getHurtMessage() + " You lost " + tempDamage + " HP!");
        } else {
          gui.println("Mr. DesLauriers tried to attack, but you blocked with The Shield of Tableland!");
        }
      }
    }
    return false;
  }

  private void moralSupport() {
    supportCheck = true;
    gui.println("Mr. DesLauriers' slashes you down to 1 HP!");
    gui.println("You can feel your surroundings grow fainter... \n");
    gui.println("Suddenly, you feel a warmth in your pocket. The moral support has started to glow!");
    gui.println("Picking it up, it imbues with your soul. Voices of those who support you echo in your ears. \n");
    gui.println("You can do it!");
    gui.println("Add more messages!");
    gui.println("I can't think of anything!\n");
    gui.println("Your health has been completely restored!");
    gui.println("Your sword starts shining with the power of the gods. It now deals 100 damage!\n");
    gui.println("You face the enemy with a newfound confidence! You can do this!");
    player.maxHeal();
    itemMap.get("sword").setDamage(100);
  }

  public void newsNewsScroll(){
    if (!hasAnsweredNewsQuestions){
      gui.println("On the other side of the room, an antique scroll sits in a clear, glass case.");
      gui.println("You hear a booming, disembodied voice: \"Have you come to steal the precious scroll of News News, traveller? Well, you must solve these riddles six.\"");
      gui.println("Question 1: How many Whisperer articles have there been?");
      gui.println("Question 2: How many planets are in our solar system?");
      gui.println("Question 3: What is the largest number represented by a single character in hexadecimal?");
      gui.println("Question 4: What is the average age of the grade elevens?");
      gui.println("Question 5: What is the lowest prime number that contains consecutive digits?");
      gui.println("Question 6: What is the answer to the ultimate question of life, the universe, and everything?");
      gui.println("\"You will have six numbers, each an answer to the six questions. Only then you will prove your worth!\"");
      gui.println("\"What is the code?\"");
      if (newsNewsAnswers()){
        gui.println("\"Wow. I'm truly impressed. Those are the right numbers! Traveller, you have proved yourself more than worthy of the scroll.\"");
        gui.println("On the other side of the room, an antique scroll sits in a clear, glass case.");
      } else {
        gui.println("\"I'm afraid, traveller, that those aren't the right numbers. You clearly are not worthy to be in this temple! Good riddance!\"");
        currentRoom = roomMap.get("Temple Pavillion");
        gui.println(currentRoom.shortDescription());
        gui.println("You were teleported back to the entrance of the News News Temple.");
      }
      itemMap.get("scroll").isTakeable(true);
      hasAnsweredNewsQuestions = true;
    } else {
      if(player.getTrial(1)){
        gui.println("On the other side of the room is an empty glass case.");
      }else{
        gui.println("On the other side of the room, an antique scroll sits in a clear, glass case.");
      }
    }
  }

  // answers to news news problems: 4 8 15 16 23 42 (the numbers from Lost)
  private boolean newsNewsAnswers() {
    String in = gui.readCommand();
    if (in.equalsIgnoreCase("4 8 15 16 23 42") || in.equalsIgnoreCase("4, 8, 15, 16, 23, 42") || in.equalsIgnoreCase("4,8,15,16,23,42")){
      return true;
    } return false;
  }

  public void dogParadise(){
    if (!player.getTrial(7)){
      gui.println("Three adorable dogs walk up to you. The first dog is a caramel mini-labradoodle. The second is a lighter-coloured cockapoo. The third, a brown-and-white spotted Australian lab.");
      gui.println("Their name tags read 'Lucky', 'Luna', and 'Maggie' respectively.");
      gui.println("The dog named Lucky speaks to you. \"Hello, potential Whisperer successor. We would like to offer you our guidance as you complete your arduous journey.\"");
      inventory.addItem(currentRoom.getItem("moral support"));
      gui.println("\"We have added the glowing orb of moral support to your inventory.\"");
      gui.println("The dog named Luna speaks to you. \"This, mortal, is Moral Support. It will glow brighter than all the stars in the sky, and fill your head with the most encouraging thoughts.\"");
      gui.println("The dog named Maggie speaks to you. \"No being, mortal or deity, can harness its power alone. Its ethereal glow will activate when you need it most.\"");
      gui.println("You feel a sense of calm wash over you. You feel resolve for the first time in this whole journey.");
      gui.println("Lucky speaks. \"I sense your great potential. You have somewhere you need to be.\"");
      gui.println("Luna speaks. \"You are the Whisperer's successor. You must save our world.\"");
      gui.println("Maggie speaks. \"Do not fall astray from your path. We all will watch your journey with the greatest interest.\"");
      gui.println("The canine trio suddenly vanish when you blink, leaving you bewildered.");
      player.setTrial(7);
    } else{
      gui.println("There is nothing for you here.");
    }
  }

  public void deptCustomerService(){
    Enemy balloony = enemyMap.get("balloony");
    if (!player.getTrial(6) && balloony.getHealth()>0){
      isInTrial = true;
      gui.println("Floating above the wreckage is a large blue balloon.");
      gui.println("\"My name is Balloony, I am the rightful head of customer service of Tableland.\"");
      gui.println(balloony.getCatchphrase());
      if (enemyAttack(balloony)) return;
      isInTrial = false;
      currentRoom.getItem("balloony's corpse").isTakeable(true);
      gui.println("Balloony's corpse lays crumpled on the ground.");
      gui.println("You here a little voice inside you saying \"Take the balloon\"");
      gui.println("You never know when you'll need a balloon.");
    }else if(balloony.getHealth()<1 && !player.getTrial(6)){
      gui.println("Balloony's corpse lays crumpled on the ground.");
      gui.println("You here a little voice inside you saying \"Take the balloon\"");
      gui.println("You never know when you'll need a balloon.");
    }else{
      gui.println("There is nothing for you here.");
    }

  }

  /**
   *  Does when you enter the fur store location. IT WORKS GUYSSS
   */
  public void salesman(){
    if (!player.getTrial(3)){
      gui.println("A man dressed in a puffy fur coat approaches you, with a fur hat in hand.");
      gui.println("\"Would you like to buy my furs? Only for a small fee of £500!\" He says.");
      gui.println("Will you buy the fur hat? (\"yes\"/\"no\")");
      if (buyFurs()){
        if (inventory.hasItem(itemMap.get("pounds"))){
          inventory.removeItem(itemMap.get("pounds"));
          inventory.addItem(itemMap.get("hat")); 
          inventory.addItem(itemMap.get("euros"));
          gui.println("\"Pleasure doing business with you, good sir.\"");
          player.setTrial(3);
        } else if (inventory.hasItem(itemMap.get("pounds")) && inventory.getCurrentWeight() - itemMap.get("pounds").getWeight() + itemMap.get("hat").getWeight() + itemMap.get("euros").getWeight() > inventory.getMaxWeight()){
          gui.println("\"Hmm... I can sense your pockets are too heavy. What a shame.");
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
        gui.println("With a heavy kick, he blasts you out the door. You land in a pile of snow. \n");
        currentRoom = roomMap.get("Snowy Cabin");
        gui.println(currentRoom.shortDescription());
        return false;
      } else {
        gui.println("\"" + in + "\" is not a valid choice!");
      }
    }
    return false;
  }

  public void cheeseVault(){
    if(!hasOpenedVault){
      gui.println("The safe's dial taunts you. Maybe it's time to enter the code.");
      gui.println("ENTER CODE:");
      if (correctCode()){
        gui.println("Something clicks and the door swings open! Satisfied, you know you should grab a morsel of pristine Alaskan Cheese.");
        gui.println("Obviously, taking too much cheese is unbecoming of a future Whisperer. Only take one piece"); 
        hasOpenedVault = true;
      } else {
        gui.println("...Nothing happens. I guess that was the wrong code. You walk out of the room, feeling unsatisfied.");
        currentRoom = roomMap.get("Upper Atrium");
        gui.println(currentRoom.shortDescription());
      }
    } else {
      gui.println("The vault door still hangs wide open, just as you left it.");
      if(!player.getTrial(2)){
        gui.println("You see small morsels of pristine Alaskan Cheese. You know you should grab one.");
      }
    }
  }

  public boolean correctCode(){
    String in = gui.readCommand();
    if (in.equalsIgnoreCase("2956")) return true;
    return false;
  }

  private void inflate(String secondWord) {
    if (!secondWord.equals("")){
      if(player.skyGodsCheck()){
        if ((secondWord.equals("balloon") || secondWord.equals("balloony")) && inventory.hasItem(itemMap.get("balloony")) && currentRoom.equals(roomMap.get("Shadowed Plains"))){
          gui.println("You inflated Balloony's corpse.");
          gui.println("You feel the air rush around you, as the balloon propels you into the Gods' domain.");
          currentRoom = roomMap.get("Sky Temple Pavillion");
          gui.println(currentRoom.shortDescription());
        } else {
          gui.println("That doesn't seem like a good idea.");
        }
      }else{
        gui.println("You're not ready. You need to complete the 8 trials listed in the Tome of Tableland before you inflate Balloony's corpse.");
        gui.println("Try reading the Tome of Tableland.");
      }
    } else {
      gui.println("What would you like to inflate?");
    }
  }

  public void frogsMadleneandJorge(){
    if(player.getTalkedToSkyGods()){
      gui.print("You see a pair of frogs at the entrance.");
      if(!player.getTrial(9)){
        gui.println("More words here");
      }else{
        gui.println();
        gui.println("\"You should go on future Whisperer, Connie must be saved!\" Madlene says.");
      }
    }else{
      gui.println("The tunnel is boarded up, so you cannot go through it.");
      gui.println("You feel a strong pull from this place. You cannot go through the tunnel. However, somehow you know it's very important.");
    }
  }
  
  public void skyGods(){
    gui.println("You hear the most calming sound in the world. You are so calm, it feels like your brain is being massaged by a baby deer.");
    gui.println("You look up from your prayer and see the three gorgeous thrones. Now, on them sit three humans. Somehow, you know they are the true Gods of Tableland.");
    gui.println("\"Welcome to the Temple of the Sky Gods,\" the first figure says.");
    gui.println("\"You have made it past the first eight challenges,\" the second figure says.");
    gui.println("\"Now all you need to do to prove you are the true Whisperer succesor is go into hell and rescue the previous co-head of customer service of Tableland from the monster that resides there,\" says the third god.");
    gui.println("\"To defeat this monster you need these,\" the first figure says.");
    removeItems();
    inventory.addItem(itemMap.get("the sword of tableland"));
    inventory.addItem(itemMap.get("the shield of tableland"));
    gui.println("\"We have added the Sword and Shield of Tableland to your inventory and have removed everything unnessacary for the defeat of the monster,\" says the second god.");
    gui.println("\"Now go defeat the monster future Whisperer!\" says the third god.");
    player.talkedToSkyGods();
    gui.println("With that, the gods dissapear.");
    gui.println();
    gui.println("You now know what you must do.");
    gui.println("Go to hell and defeat the monster to save your friend.");
  }

  public void removeItems(){
    if(inventory.hasItem(itemMap.get("rocks"))){
      inventory.removeItem(itemMap.get("rocks"));
    }
    if(inventory.hasItem(itemMap.get("scroll"))){
      inventory.removeItem(itemMap.get("scroll"));
    }
    if(inventory.hasItem(itemMap.get("euros"))){
      inventory.removeItem(itemMap.get("euros"));
    }
    if(inventory.hasItem(itemMap.get("balloony's corpse"))){
      inventory.removeItem(itemMap.get("balloony's corpse"));
    }
    if(inventory.hasItem(itemMap.get("bottle"))){
      inventory.removeItem(itemMap.get("bottle"));
    }
    if(inventory.hasItem(itemMap.get("bandages"))){
      inventory.removeItem(itemMap.get("bandages"));
    }
    if(inventory.hasItem(itemMap.get("fur hat"))){
      inventory.removeItem(itemMap.get("fur hat"));
    }
    if(inventory.hasItem(itemMap.get("cheese"))){
      inventory.removeItem(itemMap.get("cheese"));
    }
    if(inventory.hasItem(itemMap.get("key of friendship"))){
      inventory.removeItem(itemMap.get("key of friendship"));
    }
  }

  /** 
   * @param secondWord
   */
  public void yell(String secondWord) {
    if (secondWord != ""){
        gui.println(secondWord.toUpperCase() + "!!!!!!");
        gui.println("Feel better?");
      }else{
        gui.println("ARGHHHHH!!!!!!");
        gui.println("Feel better?");
      }
  }

  /*
  * VERY IMPORTANT. lets player wear hat
  * @param command what the player is wearing
  */
  private void wear(String secondWord) {
    if (secondWord != ""){
      if ((secondWord.equals("hat") || secondWord.equals("cap")) && inventory.hasItem(itemMap.get("Coonskin Hat"))){
        gui.println("You are now wearing the fur cap. How stylish!");
        inventory.removeItem(itemMap.get("hat"));
      } else {
        gui.println("You cannot wear that!");
      }
    } else {
      gui.println("What would you like to wear?");
    }
  }

  private void pray() {
    if (currentRoom.getRoomName().equals("News News Temple")){
      gui.println("The sun's rays bounce off the skylight into your eyes.");
      gui.println("For they glow with the intensity of a thousand souls.");
      gui.println("For you know they can never be satisfied. \n");
      gui.println("Suddenly, you feel a quite compelling message from deep within your psyche.");
      gui.println("\"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA!!!!!!!!!!!!\"");
    } else if (currentRoom.getRoomName().equals("Temple of the Sky Gods")){
      skyGods();
    } else {
      gui.println("You cannot pray here. You can only pray at temples.");
    }
  }

  /**
   * Lets player read items (tome, diary)
   * @param command
   */
  private void read(String secondWord){
    if (secondWord != ""){
      if (itemMap.get("tome").isThisItem(secondWord) && inventory.hasItem(itemMap.get("tome"))){
        readTome();
      } else if (secondWord.equals("diary") && currentRoom.getRoomName().equals("Master Bedroom")){
        readDiary();
      } else if (itemMap.get("scroll").isThisItem(secondWord)){
        readScroll();
      } else {
        gui.println("You can't read that!");
      }
    } else {
      gui.println("What would you like to read?");
    }
  }

  private void readScroll() {
    gui.println("Garbled, messy writing is scrawled onto the page. It reads:");
    gui.println();
    gui.println("\"The DAILY news table land™: The official news channel of Tableland\"");
    gui.println("\"January 3 2022 | Balloony has taken down Connie!\"");
    gui.println("\"by Christopher Cha \"");
    gui.println("\"After serving harmoniously as co-heads of customer service for over two years, Balloony has forcibly removed Constantine from office.\"");
    gui.println("\"Connie was working in his office when Balloony entered with an armed sasquatch. The guards threw a bag over Connie's head, bound his hands and dragged him out of the room.\"");
    gui.println("\"Balloony told the people of Tableland this on a press conference on Friday. He has kidnapped his former co-head of customer service.\"");
    gui.println("\"Where could Balloony be keeping Connie?\" You think.");
  }

  /**
   *  Player reads princess diary
   */
  private void readDiary() {
    gui.println("\"Dear diary,\"");
    gui.println("\"Wow, I sure do love cheese.\"");
    gui.println("\"I sure am glad nobody knows that the secret code for the cheese vault is 2956.\"");
  }

  /**
   *  Player reads tome of tableland !!!
   */
  private void readTome() {
    gui.println("\"THE TWELVE TRIALS OF THE WHISPERER\"");
    gui.println("\"Doth whom unleavens ye agèd tome, come with ye eyes here.\"");
    gui.println("\"1. Conquer ye Guardian Sasquatch of legends yore.\"");
    gui.println("\"2. Procure thee News News scroll, doth of antiquity.\"");
    gui.println("\"3. Practice larceny upon morsels of Alaskan cheese.\"");
    gui.println("\"4. Secure ye fabulous furs in all Canadian lands.\"");
    gui.println("\"5. Upheave Vaccuum, a foe of many Cs.  \"");
    gui.println("\"6. Threaten the most deadliest liquid upon the Robot.\"");
    gui.println("\"7. Usurp the meddling head of Customer Service.\"");
    gui.println("\"8. Take a jaunt over to Dog Paradise.\"");
    gui.println("\"Only then thou will be granted access to the Trial in the Sky.\"");
  }

  /**
   * when player types "heal" (no args).
   */
  private void heal() {
    if (!inventory.hasItem(itemMap.get("Bandages"))){
      gui.println("You have no healing items!");
    } else if (player.getHealth() != 100){
      player.maxHeal();
      gui.println("Your wounds have healed. You have been restored to full health.");
      inventory.getItem("Bandages").decrementQuantity();
      if (inventory.getItem("Bandages").getQuantity() > 1){
        gui.println("You have " + inventory.getItem("Bandages").getQuantity() + " bandages left.");
      } else if (inventory.getItem("Bandages").getQuantity() != 0){
        gui.println("You have 1 bandage left.");
      } else {
        gui.println("You have no more bandages.");
        inventory.removeItem(itemMap.get("bandages"));
      }
    } else if (player.getHealth() == 100){
      gui.println("You are already at maximum health!");
      if (inventory.getItem("Bandages").getQuantity() > 1){
        gui.println("You have " + inventory.getItem("Bandages").getQuantity() + " bandages left.");
      } else if (inventory.getItem("Bandages").getQuantity() != 0){
        gui.println("You have 1 bandage left.");
      }
    }
    gui.println("Your current health is " + player.getHealth() + ".");
  }

  /**
   *  If you try to 'go up' with balloony in ur inventory
   */
  public static void printBalloonHelp() {
    gui.println("The clouds are too high in the sky. Maybe try inflating Balloony?");
  }
  /**
   * Print out some help information. Here we print some stupid, cryptic message
   * and a list of the command words.
   */
  public void printHelp(Command command) {
    if (command.hasArgs()) Parser.printCommandHelp(command);
    else{
      gui.println("You are an adventurer in the marvelous lands of Tableland,");
      gui.println("always in search for things to do and items to collect.");
      gui.println();
      gui.println("The available commands are:");
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
  
  public String getGUIGameString() {
    String roomExString = "";
    ArrayList<String> exits = new ArrayList<String>();
    for (Exit exit : currentRoom.getExits()) {
        exits.add(exit.getDirection());
    }
    roomExString = String.join(", ", exits);
    return "Inventory: " + inventory.getString() + " | Health: " + player.getHealth() + " | Exits: " + roomExString;
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
    }
  }

  /**
   * Resets the game save state.
   */
  private void resetSaveState() {
    try {
      FileOutputStream fileOut = new FileOutputStream(GAME_SAVE_LOCATION);
      ObjectOutputStream out = new ObjectOutputStream(fileOut);
      out.writeObject(null);
      out.close();
      fileOut.close();
    } catch (IOException i) {
      i.printStackTrace();
    }
  }
}
