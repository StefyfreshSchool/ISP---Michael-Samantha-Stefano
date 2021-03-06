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

/**
 * Plays Zork.
 */
public class Game implements java.io.Serializable { 
  private static final String GAME_SAVE_LOCATION = "data/Game Save.ser";
  private transient static GUI gui;
  private static MusicPlayer music;
  private boolean musicPlaying;
  private double musicVolumeOffset;
  public static HashMap<String, Room> roomMap; // hashmaps storing rooms, items, and enemies
  public static HashMap<String, Item> itemMap;
  public static HashMap<String, Enemy> enemyMap;
  private Inventory inventory;
  private Player player;
  private Parser parser;
  private Room currentRoom;
  private Room pastRoom;
  private boolean isInTrial; // various game checks. if player is in a trial
  private boolean hasAnsweredNewsQuestions; // if player has solved Temple riddles
  private boolean gameEnded; // if game ends
  private boolean supportCheck; // if player used Moral Support
  private boolean hasOpenedVault; // if player opens Alaskan Cheese vault 
  private final double DEFAULT_BACKGROUND_MUSIC_VOL = -15;
  private final int PLAYER_HEALTH = 100;
  private final int INVENTORY_WEIGHT = 50; // max weight you can carry
  private String musicString;
  private int trial;

  /**
   * Create the game and initialize its internal map.
   * @author Stefano - logic
   * @author adapted from Mr. DesLauriers's code
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
    inventory = new Inventory(INVENTORY_WEIGHT);
    player = new Player(PLAYER_HEALTH);

    //Init rooms and game state
    try {
      initItems();
      initRooms();
      initEnemies();
      musicPlaying = true;
      startMusic("data/audio/background.wav", DEFAULT_BACKGROUND_MUSIC_VOL);
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
            roomMap = save.getRoomMap();
            inventory = save.getInventory();
            pastRoom = save.getPastRoom();
            currentRoom = save.getCurrentRoom();
            player = save.getPlayer();
            enemyMap = save.getEnemyMap();
            isInTrial = save.getIsInTrial();
            trial = save.getTrial();
            hasAnsweredNewsQuestions = save.getHasAnsweredNewsQuestions();
            hasOpenedVault = save.getHasOpenedVault();
            supportCheck = save.getSupportCheck();

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

  /**Checks if the required Java dependencies are accessible. 
   * @author Stefano - logic
   * @author adapted from Mr. DesLauriers' code
  */
  private void existJavaDependencies() {
    new JSONArray();
    new Awaitility();
  }

  /**Initializes Enemies json 
   * @author Stefano - everything else
   * @author Michael - catchphrases, messages, damageMin/Max
  */
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
        messages.add((String) message); // messages when the enemy attacks you!
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

  /**Initializes items json 
   * @author Stefano - logic, everything else
   * @author Michael - quantity, isWeapon, isDroppable, damage
   * @author adapted from Mr. DesLauriers' code
  */
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
      boolean isDroppable = (boolean) ((JSONObject) itemObj).get("isDroppable");
      String description = (String) ((JSONObject) itemObj).get("description");
      String startingRoom = (String) ((JSONObject) itemObj).get("startingRoom");
      Long damage = (Long) ((JSONObject) itemObj).get("damage");
      ArrayList<String> aliases = new ArrayList<String>();
      for (Object alias : (JSONArray) ((JSONObject) itemObj).get("aliases")) {
        aliases.add((String) alias);
      }

      Item item;
      if (quantity == null && !isWeapon){
        item = new Item(Integer.parseInt(weight + ""), name, startingRoom, isTakeable, description, aliases, isDroppable);
      } else if (isWeapon) {
        item = new Item(Integer.parseInt(weight + ""), name, startingRoom, isTakeable, description, aliases, isDroppable, isWeapon, damage.intValue());
      } else {
        item = new Item(Integer.parseInt(weight + ""), name, startingRoom, isTakeable, description, aliases, isDroppable, quantity.intValue());
      }
      itemMap.put(itemId, item);

      for (String alias : aliases) {
        itemMap.put(alias, item);
      }
    }
  }

  /**Initializes rooms json 
   * @author Stefano - logic
   * @author adapted from Mr. DesLauriers' code
  */
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

  /** Main play routine. Loops until end of play.
   * @author Stefano - logic
   * @author adapted from Mr. DesLauriers' code
  */
  public void play() {
    printWelcome();
    if (isInTrial){
      if (trial == 1) sasquatch();
      if (trial == 2) vaccuum();
      if (trial == 3) robot();
      if (trial == 4) deslauriers();
      if (trial == 5) balloony();
    }
    
    // Enter the main command loop. Here we repeatedly read commands and
    // execute them until the game is over.
    boolean finished = false;
    while (!finished) {
      Command command;
      command = parser.getCommand();
      processCommand(command);
    }
  }

  /**Starts the background music. 
   * @author Stefano - everything
  */
  private void startMusic(String musicSrc, double volume) {
    try {
      music = new MusicPlayer(musicSrc, true);
    } catch (FileNotFoundException e) {
      GameError.fileNotFound(musicSrc);
    }
    double vol = volume + musicVolumeOffset;
    if (vol < -79.9) vol = -80;
    if (vol > 0) vol = 0;
    music.setVolume(vol);
    if (!musicPlaying) music.stop();
    musicString = musicSrc;
  }

  public static MusicPlayer getMusicPlayer() {
    return music;
  }

  /** Print out the opening message for the player.
   * @author Stefano - logic
   * @author Michael - dialogue
   * @author adapted from Mr. DesLauriers' code
   */
  private void printWelcome() {
    gui.reset();
    gui.println("Welcome to the illustrious realm of Tableland!\n");
    gui.println("You are here to prove your worth, conquering all trials in your way. Now be off.");
    gui.println("Type 'help' for more information about the game and the available commands.\n");
    gui.centerText(true);
    gui.println();
    gui.println(currentRoom.longDescription());
    
  }

  /**
   * Given a command, process (that is: execute) the command.
   * @param command
   * @return {@code 0} if no action is required, {@code 1} if the game should quit, {@code 2} if the game should restart
   * @author Everyone added commands to this
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

    } else if (commandWord.equals("hit")){
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

  /**
   * Restarts the game
   * @author Stefano - everything
   */
  private void restartGame() {
    resetSaveState();
    gui.reset();
    gui.resetCommands();
    gui.centerText(false);
    gui.printInfo("Game restarted.\n");
    try {
      music.stop();
      initItems();
      initRooms();
      initEnemies();
      isInTrial = false;
      hasAnsweredNewsQuestions = false;
      hasOpenedVault = false;
      supportCheck = false;
      gameEnded = false;
      currentRoom = roomMap.get("South of the Cyan House");
      inventory = new Inventory(INVENTORY_WEIGHT);
      player = new Player(PLAYER_HEALTH);
      startMusic("data/audio/background.wav", DEFAULT_BACKGROUND_MUSIC_VOL);
    } catch (Exception e) {
      e.printStackTrace();
    }
    printWelcome();
  }

  /**
   * Restarts the game
   * @author Stefano - everything
   * @author adapted from Mr. DesLauriers' code
   */
  private void endGame() {
    music.stop();
    gui.println("Thank you for playing. Goodbye!");

    // Nice transition to exit the game
    sleep(1000);
    System.exit(0);
  }

  /**
   * This method is for testing the game.
   * FEEL FREE to add stuff for testing things!
   * @author Stefano - everything
   * @author Michael - added tests
   */
  private void testing(Command command) {
    if (command.getStringifiedArgs().equals("easter egg")) gui.println("You found an easter egg!\nYou are either really curious or cheated and looked at the game code.");
    else gui.println("Don't you dare use this command if you aren't a dev!");
    return;
    // In the game, type "test #" to activate one of the following tests.
    // String c = command.getStringifiedArgs();
    // if (c.equals("1")){
    //   inventory.addItem(itemMap.get("pounds"));
    //   salesman();
    // } else if (c.equals("2")){
    //   currentRoom = roomMap.get("Castle Grounds");
    // } else if (c.equals("3")){
    //   currentRoom = roomMap.get("North of Crater");
    // } else if (c.equals("4")){
    //   inventory.addItem(itemMap.get("sword"));
    //   inventory.addItem(itemMap.get("bottle"));
    //   inventory.addItem(itemMap.get("rocks"));
    //   currentRoom = roomMap.get("Mystery Door of Mystery");
    // } else if (c.equals("5")){
    //   inventory.addItem(itemMap.get("sword"));
    //   inventory.addItem(itemMap.get("bottle"));
    //   currentRoom = roomMap.get("Caldera Bridge");
    // } else if (c.equals("6")){
    //   player.talkedToSkyGods();
    // } else if (c.equals("7")){
    //   skyGods();
    // } else if (c.equals("8")){
    //   dogParadise();
    // } else if (c.equals("heal")){
    //   player.maxHeal();
    // } else if (c.equals("crash")){
    //   GameError.crashGame();
    // } else if (c.equals("img")){
    //   gui.printImg("data/images/img.png");
    // } else if (c.equals("credits")){
    //   endOfGame();
    // } else if (c.equals("sasquatch")){
    //   currentRoom = roomMap.get("The Lair");
    //   sasquatch();
    // } else if (c.equals("9")){
    //   player.talkedToSkyGods();
    //   frogsMadleneAndJorge();
    // } else if (c.equals("10")){
    //   skyGods();
    // } else if (c.equals("11")){
    //   inventory.addItem(itemMap.get("balloony"));
    //   inventory.addItem(itemMap.get("tome"));
    //   player.setTrial(0);
    //   player.setTrial(1);
    //   player.setTrial(2);
    //   player.setTrial(3);
    //   player.setTrial(4);
    //   player.setTrial(5);
    //   player.setTrial(6);
    //   player.setTrial(7);
    //   player.setTrial(8);
    // }
  }
  
  /**
   * @param room
   * @return the enemy that is present in the room.
   * @author Samantha - Sasquatch and Balloony
   * @author Michael - Vaccuum, Friends Robot, Mr. DesLauriers
   */
  private Enemy enemyRoomCheck(Room room){
    String name = room.getRoomName();
    if (name.equals("The Lair")){
      return enemyMap.get("sasquatch");
    } else if (name.equals("Lower Hall of Enemies")){
      return enemyMap.get("vaccuum");
    } else if (name.equals("Upper Hall of Enemies")){
      return enemyMap.get("friends robot");
    } else if (name.equals("Dept. of Customer Service")){
      return enemyMap.get("balloony");
    } else if (name.equals("Hall of the Volcano King")){
      return enemyMap.get("deslauriers");
    } return null;
  }

  /**
   * Allows the player to hit an enemy.
   * @param command - string of enemy you want to hit and the weapon you want to hit that enemy with: "enemy with item"
   * @author Samantha - Logic
   * @author Stefano - Logic
   * @author Michael - Friends robot, Mr. DesLauriers, battling code
   * 
   */
  private void hit(Command command) {
    int enemyHealth;
    Enemy enemy = enemyRoomCheck(currentRoom);
    if (enemy == null){
      gui.println("There is no enemy here. You cannot hit anything.");
    } else if (enemy.getIsDead()){
      gui.println("The threat has been neutralized. There is no longer an enemy here. You cannot hit anything.");
    } else {
      ArrayList<String> args = command.getArgs();
      String argsStr = command.getStringifiedArgs().toLowerCase(); 
      if (!command.hasArgs() || argsStr.indexOf("with") == 0) { // hit, no args
        gui.println("Hit what enemy?");
      } else if (!args.contains("with") && enemyMap.get(argsStr.trim()) == null) { // hit, invalid enemy
        gui.println(argsStr + " is not an enemy.");
        gui.println("What would you like to hit?");
      } else if (((!args.contains("geraldo") || !args.contains("sword") || !args.contains("water")) && command.getLastArg().equals("with")) || !args.contains("with")){ // hit, args missing either weapon or with
        gui.println("Hit with what weapon?");
      } else if (enemyMap.get(argsStr.substring(0, argsStr.indexOf("with")).trim()) == null) { // hit with, invalid enemy
        gui.println(argsStr.substring(0, argsStr.indexOf("with")).trim() + " is not an enemy.");
        gui.println("Who would you like to hit?");
      } else if (!itemMap.get("geraldo").isThisItem(argsStr.substring(argsStr.indexOf(" with ") + 6).trim()) && !itemMap.get("sword").isThisItem(argsStr.substring(argsStr.indexOf(" with ") + 6).trim()) && !itemMap.get("water").isThisItem(argsStr.substring(argsStr.indexOf(" with ") + 6).trim())){ // hit enemy with, invalid weapon
        String weirdItemName = argsStr.substring(argsStr.indexOf(" with ") + 6, argsStr.length());
        gui.println(weirdItemName + " is not a weapon.");
        gui.println("What would you like to hit " + enemy.getName() + " with?");
      } else if (!enemyMap.get(argsStr.substring(0, argsStr.indexOf("with")).trim()).isThisEnemy(enemy.getName())){ // valid enemy, invalid room
        gui.println(enemyMap.get(argsStr.substring(0, argsStr.indexOf("with")).trim()).getName() + " is not an enemy in this room.");
      } else if (!inventory.hasItem(itemMap.get(argsStr.substring(argsStr.indexOf(" with ") + 6, argsStr.length())))) {
        gui.println("You do not have that item.");
      } else if (enemyMap.get("friends robot").isThisEnemy(enemy.getName()) && !enemyMap.get("friends robot").getIsDead()) { //runs when you try to hit the friends robot with something
        String weaponName = argsStr.substring(argsStr.indexOf(" with ") + 6, argsStr.length());
        gui.println("The " + weaponName + " just bounces off its titanium armor. It dealt 0 damage.");
        gui.println("Maybe there's another way to defeat it?");
        gui.println("Is there anything that you have in your inventory that machines hate?");
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
   * @param command - string of enemy you want to threaten and the weapon you want to threaten that enemy with: "enemy with item"
   * @author Stefano - all logic
   * @author Michael - everything else
   * @author Samantha - check to see if item is in player's inventory
   */
  private void threaten(Command command) {
    Enemy enemy = enemyRoomCheck(currentRoom);
    if (enemy == null){
        gui.println("You are imposing. You are powerful. You stand a little bit straighter.");
        gui.println("There is no enemy here to threaten.");
    } else if (enemy.getIsDead()){
      gui.println("The threat has been neutralized. There is no longer an enemy here to threaten.");
    } else {
      String argsStr = command.getStringifiedArgs();
      ArrayList<String> args = command.getArgs();
      if (!command.hasArgs() || argsStr.indexOf("with") == 0) { // hit, no args
        gui.println("Threaten what enemy?");
      } else if (!args.contains("with") && enemyMap.get(argsStr.trim()) == null) { // hit, invalid enemy
        gui.println(argsStr + " is not an enemy.");
        gui.println("What would you like to threaten?");
      } else if (((!args.contains("geraldo") || !args.contains("sword") || !args.contains("water")) && command.getLastArg().equals("with")) || !args.contains("with")){ // hit, missing either weapon or with
        gui.println("Threaten with what weapon?");
      } else if (enemyMap.get(argsStr.substring(0, argsStr.indexOf("with")).trim()) == null) { // hit with, invalid enemy
        gui.println(argsStr.substring(0, argsStr.indexOf("with")).trim() + " is not an enemy.");
        gui.println("Who would you like to threaten?");
      } else if (!itemMap.get("geraldo").isThisItem(argsStr.substring(argsStr.indexOf(" with ") + 6).trim()) && !itemMap.get("sword").isThisItem(argsStr.substring(argsStr.indexOf(" with ") + 6).trim()) && !itemMap.get("water").isThisItem(argsStr.substring(argsStr.indexOf(" with ") + 6).trim())){ // hit enemy with, invalid weapon
        String weirdItemName = argsStr.substring(argsStr.indexOf(" with ") + 6, argsStr.length()); 
        gui.println(weirdItemName + " is not a weapon.");
        gui.println("What would you like to threaten " + enemy.getName() + " with?");
      } else if (!enemyMap.get(argsStr.substring(0, argsStr.indexOf("with")).trim()).isThisEnemy(enemy.getName())){ // valid enemy, invalid room
        gui.println(enemyMap.get(argsStr.substring(0, argsStr.indexOf("with")).trim()).getName() + " is not an enemy in this room.");
      } else if (!inventory.hasItem(itemMap.get(argsStr.substring(argsStr.indexOf(" with ") + 6, argsStr.length())))) { //check to see if item is in player's inventory
        gui.println("You do not have that item.");
      } else if (!itemMap.get("water").isThisItem(argsStr.substring(argsStr.indexOf(" with ") + 6).trim())){ // threaten enemy with, invalid weapon
        gui.println("That doesn't seem to scare the enemy.");
      } else if (!enemyMap.get(args.get(0)).isThisEnemy(enemy.getName())){ // valid enemy, invalid room
        gui.println("You can't find that enemy here.");
      } else { // threaten enemy with weapon
        if (enemyMap.get("friends robot").isThisEnemy(enemy.getName()) && !enemyMap.get("friends robot").getIsDead()){
          enemyMap.get("friends robot").setIsDead(true);
          enemyMap.get("friends robot").setHealth(0);
          gui.cutsceneMode(true);
          sleep(1000);
          gui.println();
          gui.println("The Friends Robot cowers in fear from your dominance. It seems to be perturbed from the water bottle in your hand.");
          gui.println("\"P1eA5e d0N't hUrt m3! 1 hav3 frI3nDs!\" it says, with a robotic quaver in its voice.");
          gui.println("Trembling quietly, it moves out of your path, revealing a carefully chiseled inscription in the wall.");
          sleep(3000);
          gui.cutsceneMode(false);
        } else {
          gui.println("That doesn't seem to do anything.");
        }
      }
    }
  }

  /**
   * Allows the player to take items from the current room. Also now prints description.
   * @param command
   * @author Stefano - everything
   * @author adapted from Mr. DesLauriers' code
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

  /**
   * Allows the player to drop items.
   * @param command
   * @author Stefano - everything
   * @author adapted from Mr. DesLauriers' code
   */
  private void drop(Command command) {
    if (!command.hasArgs()){
      gui.println("Drop what?");
      return;
    }
    String itemName = command.getStringifiedArgs().toLowerCase();
    if (!Item.isValidItem(itemName)){
      gui.println("Not a valid item!");
    } else if (!inventory.hasItem(itemMap.get(itemName))){
      gui.println("You don't seem to have that item.");
    } else if (!itemMap.get(itemName).getIsDroppable()){
      gui.println("You can't drop that item!");
    } else {
      Item item = itemMap.get(itemName);
      inventory.removeItem(inventory.getItem(itemName));
      currentRoom.addItem(item);
      gui.println("You dropped " + item.getName() + ".");
    }
  }

  /**
   * Saves the game and optionally quits.
   * @param command
   * @author Stefano - everything
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
    Save game = new Save(roomMap, inventory, currentRoom, pastRoom, player, enemyMap, musicString, isInTrial, hasAnsweredNewsQuestions, hasOpenedVault, supportCheck, trial);
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
      gui.printerr("Error while saving! Could not save.");
    }
    return quit;
  }

  /**
   * Allows the game to load a previously saved state of the game.
   * @author Stefano - everything
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
        music.stop();
        roomMap = save.getRoomMap();
        inventory = save.getInventory();
        pastRoom = save.getPastRoom();
        currentRoom = save.getCurrentRoom();
        player = save.getPlayer();
        enemyMap = save.getEnemyMap();
        isInTrial = save.getIsInTrial();
        trial = save.getTrial();
        hasAnsweredNewsQuestions = save.getHasAnsweredNewsQuestions();
        hasOpenedVault = save.getHasOpenedVault();
        supportCheck = save.getSupportCheck();
        gui.reset();
        gui.centerText(false);
        gui.printInfo("Game reloaded from saved data.\n");
        printWelcome();
        if (!isInTrial){
          startMusic(save.getMusic(), DEFAULT_BACKGROUND_MUSIC_VOL);          
        } else {
          if (trial == 1) sasquatch();
          if (trial == 2) vaccuum();
          if (trial == 3) robot();
          if (trial == 4) deslauriers();
          if (trial == 5) balloony();
        }
      } else {
        gui.println("There is no valid state to load!");
      }
    } catch (ClassNotFoundException | IOException e) {
      gui.printerr("Error while loading! Could not load.");
    }   
  }

  /**
   * Prompts the user if they want to quit or restart the game. 
   * After user input, it returns true or false.
   * @param string - Prints whether the operation is a quit or restart.
   * @return True or false based on if the user cancelled the operation or not.
   * @author Stefano - everything
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
   * Try to go to one direction. If there is an exit, enter the new room, and do something
   * otherwise print an error message.
   * @param command - direction the player wants to go
   * @author Everyone did a lot of things here.
   */
  private void goRoom(Command command) {
    if (!command.hasArgs()) {
      gui.println("Go where?");
      return;
    }
    String direction = command.getStringifiedArgs().trim();
    
    // Try to leave current room.
    Room pastRoom = currentRoom;
    Room nextRoom = currentRoom.nextRoom(direction);
    
    if (nextRoom == null)
      gui.println(direction + " is not a valid direction.");
    else if (!currentRoom.canGoDirection(direction, inventory, player)){
      if (nextRoom.getRoomName().equals("Tableland Plains") || nextRoom.getRoomName().equals("Town Plaza") || nextRoom.getRoomName().equals("Shadowed Plains")){
        printTomeHelp();
      }
      gui.println("You can't go this way yet. Try looking around.");
    } else {
       //print images
       if (nextRoom.getRoomName().equals("East of the Cyan House")) gui.printImg("data/images/cyan_house_east.png");
       if (nextRoom.getRoomName().equals("Parliament Entrance Room") && !isInTrial) gui.printImg("data/images/parliament.png");
       if (nextRoom.getRoomName().equals("News News Temple")) gui.printImg("data/images/temple_room.png");
       if (nextRoom.getRoomName().equals("Campsite Ruins")) gui.printImg("data/images/laser_frog.png");

      // Tests for going into trials
      if (!isInTrial && nextRoom.getRoomName().equals("Upper Hall of Enemies")) {
        currentRoom = nextRoom;
        gui.println(currentRoom.shortDescription());
        robot();
      } else if (!isInTrial && nextRoom.getRoomName().equals("Lower Hall of Enemies")) {
        currentRoom = nextRoom;
        gui.println(currentRoom.shortDescription());
        vaccuum();
      } else if (!isInTrial && nextRoom.getRoomName().equals("The Lair")){
        currentRoom = nextRoom;
        gui.println(currentRoom.shortDescription());
        sasquatch();
      } else if (!isInTrial && nextRoom.getRoomName().equals("Hall of the Volcano King")) {
        currentRoom = nextRoom;
        gui.println(currentRoom.shortDescription());
        deslauriers();
      } else if (!isInTrial && nextRoom.getRoomName().equals("Dept. of Customer Service")){
        currentRoom = nextRoom;
        gui.println(currentRoom.shortDescription());
        balloony();
      } else if (!isInTrial){
        currentRoom = nextRoom;
        gui.println(currentRoom.longDescription());
      } else {
        gui.println("You cannot leave while the enemy is still at large!");
      }
      if (currentRoom.getRoomName().equals("Fur Store")){
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
        frogsMadleneAndJorge();
      }
      if (currentRoom.getRoomName().equals("Dog Paradise")){
        dogParadise();
      }
    }
    
    // Set trial stuff 
    if (pastRoom.getRoomName().equals("The Lair") && currentRoom.getRoomName().equals("North of Crater")){
      if (inventory.hasItem(itemMap.get("1000 british pounds"))){
        player.setTrial(0);
        gui.println(); // do not delete
      }
    } else if ((pastRoom.getRoomName().equals("Lower Hall of Enemies") && currentRoom.getRoomName().equals("Upper Hall of Enemies")) || (pastRoom.getRoomName().equals("Lower Hall of Enemies") && currentRoom.getRoomName().equals("Mystery Door of Mystery"))){
      if (inventory.hasItem(itemMap.get("key of friendship"))){
        player.setTrial(4);
        gui.println(); // do not delete
      }
    } else if (pastRoom.getRoomName().equals("News News Vault") && currentRoom.getRoomName().equals("News News Temple")){
      if (inventory.hasItem(itemMap.get("scroll of news news"))){
        player.setTrial(1);
        gui.println(); // do not delete
      }
    } else if (pastRoom.getRoomName().equals("Cheese Vault") && currentRoom.getRoomName().equals("Upper Atrium")){
      if (inventory.hasItem(itemMap.get("alaskan cheese"))){
        player.setTrial(2);
        gui.println(); // do not delete
      }
    } else if (pastRoom.getRoomName().equals("Dept. of Customer Service") && currentRoom.getRoomName().equals("Parliament Entrance Room") || pastRoom.getRoomName().equals("Dept. of Customer Service") && currentRoom.getRoomName().equals("Teleporter Room")){
      if (inventory.hasItem(itemMap.get("balloony's corpse"))){
        player.setTrial(6);
        gui.println();// do not delete
      }
    }

    // change music
    if (roomMap.get("Gloomy Forest 1") == nextRoom && pastRoom.getRoomName().equals("Gates of Hell")){
      fadeMusic(music, 30);
      music.stop();
      startMusic("data/audio/hell.wav", DEFAULT_BACKGROUND_MUSIC_VOL - 5);
    }

    if (pastRoom.getRoomName().equals("Sky Temple Pavillion") && nextRoom.getRoomName().equals("Shadowed Plains")){
      fadeMusic(music, 30);
      music.stop();
      startMusic("data/audio/background.wav", DEFAULT_BACKGROUND_MUSIC_VOL);
    }
  }

  /**
   * The player's encounter with the sasquatch
   * @author Samantha - everything important
   * @author Stefano - music
   */
  public void sasquatch(){
    Enemy sasquatch = enemyMap.get("sasquatch");
    if (!(sasquatch.getHealth() <= 0)){
      isInTrial = true;
      trial = 1;
      gui.println("The Sasquatch steps out of the cave.");
      gui.println(sasquatch.getCatchphrase() + " He screams.");
      gui.println("You panic, frozen with terror.");
      gui.println("Then you notice the pile of rocks on the ground. Maybe they can be used as a weapon?");
      fadeMusic(music);
      startMusic("data/audio/fighting.wav", -60);
      fadeInMusic(music, 1, -60, -25);
      if (enemyAttack(sasquatch)) return;
      fadeMusic(music, 20);
      startMusic("data/audio/background.wav", DEFAULT_BACKGROUND_MUSIC_VOL);
      gui.println();
      gui.println("Just inside of the cave you can see muddy pieces of paper. What are they?");
      isInTrial = false;
      trial = 0;
      currentRoom.getItem("pounds").isTakeable(true);
    } else if ((sasquatch.getHealth() <= 0) && currentRoom.getRoomName().equals("The Lair")) {
      gui.println("The sasquatch's corpse lies strewn on the ground.");
      gui.println("Past the corpse, you can see a dark, ominous cave.");
      if (!player.getTrial(0)) {
        gui.println("Just inside of the cave you can see muddy pieces of paper. What are they?");
      } else if (player.getTrial(0)) {
        gui.println("Your conscience speaks to you. \"There are more important things to do than explore perilous caves.\" You know you must leave this place.");
      }
      gui.println(currentRoom.exitString());
    }
  }

  /**
   * Does things when you encounter the Vaccuum.
   * @author Michael - everything important
   * @author Stefano - music
   */
  public void vaccuum(){
    Enemy vaccuum = enemyMap.get("vaccuum");
    if (vaccuum.getHealth() > 0){
      isInTrial = true;
      trial = 2;
      gui.println("The Vaccuum wheels itself towards you.");
      gui.println(vaccuum.getCatchphrase() + " Your ears ache from the noise.");
      fadeMusic(music);
      startMusic("data/audio/fighting.wav", -60);
      fadeInMusic(music, 1, -60, -25);
      if (enemyAttack(vaccuum)) return;
      fadeMusic(music, 20);
      startMusic("data/audio/background.wav", DEFAULT_BACKGROUND_MUSIC_VOL);
      gui.println();
      gui.println("Past its lifeless body, you can see an aluminum ladder.");
      gui.println("A brass key lies on the floor, dropped by the vaccuum.");
      itemMap.get("key of friendship").isTakeable(true);
      isInTrial = false;
      trial = 0;
    } else if (vaccuum.getIsDead() && currentRoom.getRoomName().equals("Lower Hall of Enemies")){
      gui.println("The vaccuum sits on the concrete floor, out of battery.");
      if (!player.getTrial(4)){
        gui.println("A brass key lies on the floor, dropped by the vaccuum.");
      }
      if (!enemyMap.get("friends robot").getIsDead()){
        gui.println("Past its lifeless body, you can see an aluminum ladder.");
      }
      gui.println(currentRoom.exitString());
    }
  }

  /**
   * Does things when you encounter the friends robot.
   * @author Michael - everything important
   * @author Stefano - music
   */
  public void robot(){
    Enemy robot = enemyMap.get("friends robot");
    if (robot.getHealth() > 0){
      isInTrial = true;
      trial = 3;
      gui.println("The Friends Robot marches mechanically, gazing at you with a happy expression.");
      gui.println(robot.getCatchphrase() + " It beeps. It is blocking your path. You have no choice but to defeat it.");
      fadeMusic(music);
      startMusic("data/audio/fighting.wav", -60);
      fadeInMusic(music, 1, -60, -25);
      if (enemyAttack(robot)) return;
      player.setTrial(5);
      gui.println();
      fadeMusic(music, 20);
      startMusic("data/audio/background.wav", DEFAULT_BACKGROUND_MUSIC_VOL);
      isInTrial = false;
      trial = 0;
    }
    if (enemyMap.get("friends robot").getIsDead() && currentRoom.getRoomName().equals("Upper Hall of Enemies")){
      gui.println("The wall states: \"Pray before the three\". What could that possibly mean?");
      gui.println("The Friends Robot cowers in fear in the corner, and has now developed a phobia of water.");
      gui.println(currentRoom.exitString());
    }
  }

  /**
   * Does things when you encounter Mr. DesLauriers.
   * @author Michael - everything
   */
  public void deslauriers(){
    Enemy deslauriers = enemyMap.get("deslauriers");
    if (!deslauriers.getIsDead()){
      isInTrial = true;
      trial = 4;
      gui.println("Eyes blazing, Mr. DesLauriers suddenly stands up from his throne. He is twelve feet tall. \nHe is the guardian of this realm, and you know you must defeat him.");
      gui.println(deslauriers.getCatchphrase() + " He yells.");
      fadeMusic(music);
      startMusic("data/audio/end.wav", -60);
      fadeInMusic(music, 10, -60, 0);
      if (enemyAttack(deslauriers)) return;
      fadeMusic(music, 20);
      startMusic("data/audio/background.wav", DEFAULT_BACKGROUND_MUSIC_VOL);
      gameEnded = true;
      gui.cutsceneMode(true);
      sleep(2000);
      gui.println("\nMr. DesLauriers ascends towards the gods, eyes illuminated. With a flash, he disappears.");
      gui.println("The world seems a little more vibrant.");
      endOfGame();
      isInTrial = false;
      trial = 0;
    } else if (deslauriers.getIsDead() && currentRoom.getRoomName().equals("Hall of the Volcano King")) {
      gui.println("The world seems a little more vibrant.");
      gui.println(currentRoom.exitString());
    }
  }

  /**
   * Allows the enemy to attack you.
   * @param enemy the enemy that will attack you
   * @return boolean value which represents whether or not the player has died
   * @author Stefano - GUI-based logic
   * @author Michael - Everything else, implementation in enemies
   */
  private boolean enemyAttack(Enemy enemy) {
    while(enemy.getHealth() > 0){
      int tempDamage = enemy.getDamage();
      Command command = parser.getCommand();
      boolean exit = processCommand(command);
      if (exit) return true;
      if (!enemy.getIsDead()){
        if (enemy.isThisEnemy("deslauriers") && player.getHealth() - tempDamage < 1){
          tempDamage = player.getHealth() - 1;
          player.setHealth(1);
          moralSupport();
        }
        if (!supportCheck){
          if (!player.setDamage(tempDamage)){
            gui.commandsPrinted(false);
            gui.println();
            gui.println("You have perished!");
            gui.println("You are evidently not future Whisperer material.");
            gui.println("Press [y] to play again, [n] to quit.\n");
            boolean validInput = false;
            while(!validInput){
              String in = gui.readCommand();
              if (in.equals("y")){
                gui.commandsPrinted(true);
                restartGame();
                validInput = true;
                return true;
              } else if (in.equals("n")) endGame();
            }
          }
          gui.println(enemy.getHurtMessage() + " You lost " + tempDamage + " HP!");
        } else {
          sleep(1000);
          gui.println("Mr. DesLauriers tried to attack, but you blocked with The Shield of Tableland!");
        }
      }
    }
    return false;
  }

  /**
   * Occurs when Mr. DesLauriers slashes you down to 1 HP!
   * @author Michael - everything important
   * @author Stefano - GUI cutscene
   */
  private void moralSupport() {
    supportCheck = true;
    gui.cutsceneMode(true);
    gui.println("Mr. DesLauriers' slashes you down to 1 HP!");
    gui.println("You can feel your surroundings grow fainter... \n");
    sleep(3000);
    gui.println("Suddenly, you feel a warmth in your pocket. The moral support has started to glow!");
    gui.println("Picking it up, it imbues with your soul. Voices of those who support you echo in your ears. \n");
    gui.println("\"You can do it!\"");
    gui.println("\"Don't give up!\"");
    gui.println("\"I believe in you!\"");
    sleep(4500);
    gui.println("\nYour health has been completely restored!");
    gui.println("Your sword starts shining with the power of the gods. It now deals 100 damage!");
    gui.println("You face the enemy with a newfound confidence! You can do this!\n");
    gui.cutsceneMode(false);
    player.maxHeal();
    itemMap.get("sword").setDamage(100);
  }

/**
   * The code for the News News Trial.
   * @author Samantha - code and dialogue
   * @author Michael - dialogue
   * @author Stefano - GUI cutscene
   */
  public void newsNewsScroll(){
    if (!hasAnsweredNewsQuestions){
      gui.cutsceneMode(true);
      gui.println();
      gui.println("On the other side of the room, an antique scroll sits in a clear, glass case.");
      sleep(2000);
      gui.println();
      gui.println("You hear a booming, disembodied voice: \"Have you come to steal the precious scroll of News News, traveller? Well, you must solve these riddles six.\"");
      sleep(4000);
      gui.println();
      gui.println("Question 1: How many Whisperer articles have there been?");
      gui.println("Question 2: How many planets are in our solar system, not including dwarf planets?");
      gui.println("Question 3: What is the largest number represented by a single character in hexadecimal?");
      gui.println("Question 4: What is the average age of the grade elevens?");
      gui.println("Question 5: What is the lowest prime number with consecutive digits?");
      gui.println("Question 6: What is the answer to the ultimate question of life, the universe, and everything?");
      gui.println("\"You will have six numbers, each an answer to the six questions. Only then you will prove your worth!\"");
      gui.println("\"What is the code?\"");
      if (newsNewsAnswers()){
        gui.cutsceneMode(true);
        gui.println("\"Wow. I'm truly impressed. Those are the right numbers! Traveller, you have proved yourself more than worthy of the scroll.\"");
        gui.println();
        gui.println("On the other side of the room, an antique scroll sits in an unlocked, glass case.");
      } else {
        gui.cutsceneMode(true);
        gui.println("\"I'm afraid, traveller, that those aren't the right numbers. You clearly are not worthy to be in this temple! Good riddance!\"");
        currentRoom = roomMap.get("Temple Pavillion");
        gui.println(currentRoom.shortDescription());
        gui.println("You have been forcefully relocated to the entrance of the News News Temple.");
      }
      itemMap.get("scroll").isTakeable(true);
      hasAnsweredNewsQuestions = true;
      gui.cutsceneMode(false);
    } else {
      if (player.getTrial(1)){
        gui.println("On the other side of the room is an empty glass case.");
      } else {
        gui.println("On the other side of the room, an antique scroll sits in a clear, glass case.");
      }
    }
  }

  /**
   * Checks if the answer to the News News Trial's riddles are correct.
   * Answer is: 4 8 15 16 23 42 (the numbers from Lost)
   * @author Samantha - everything
   */
  private boolean newsNewsAnswers() {
    gui.cutsceneMode(false);
    String in = gui.readCommand();
    if (in.equalsIgnoreCase("4 8 15 16 23 42") || in.equalsIgnoreCase("4, 8, 15, 16, 23, 42") || in.equalsIgnoreCase("4,8,15,16,23,42")){
      return true;
    } return false;
  }

  /**
   * The code for Dog Paradise
   * @author Samantha - logic and dialogue
   * @author Michael - dialogue
   * @author Stefano - GUI cutscene
   */
  public void dogParadise(){
    if (!player.getTrial(7)){
      gui.cutsceneMode(true);
      sleep(3000);
      gui.println();
      gui.println("Three adorable dogs walk up to you. The first dog is a caramel mini-labradoodle. The second is a lighter-coloured cockapoo. The third, a brown-and-white spotted Australian lab.");
      gui.println("Their name tags read 'Lucky', 'Luna', and 'Maggie' respectively.");
      sleep(6500);
      gui.println();
      gui.println("The dog named Lucky speaks to you. \"Hello, potential Whisperer successor. We would like to offer you our guidance as you complete your arduous journey.\"");
      inventory.addItem(itemMap.get("moral support"));
      gui.println("\"We have bestowed the glowing orb of moral support upon you.\"\n");
      gui.println("Moral support taken!");
      gui.println(itemMap.get("moral support").getDescription());
      sleep(7500);
      gui.println();
      gui.println("The dog named Luna speaks to you. \"This, mortal, is Moral Support. It will glow brighter than all the stars in the god's realm, and fill your head with the most encouraging of thoughts.\"");
      sleep(5500);
      gui.println();
      gui.println("The dog named Maggie speaks to you. \"No being, mortal or deity, can harness its power alone. Its ethereal glow will activate when you need it most.\"");
      sleep(4000);
      gui.println();
      gui.println("You feel a sense of calm wash over you. You feel resolve for the first time in this whole journey.");
      sleep(3000);
      gui.println();
      gui.println("Lucky speaks. \"I sense your great potential. You have somewhere you need to be.\"");
      gui.println("Luna speaks. \"You are the Whisperer's successor. You must save our world.\"");
      gui.println("Maggie speaks. \"Do not fall astray from your path. We all will watch your journey with the greatest interest.\"");
      sleep(5500);
      gui.println();
      player.setTrial(7);
      gui.println("The canine trio suddenly vanish when you blink, leaving you bewildered.");
      gui.cutsceneMode(false);
    } else {
      gui.println("There is nothing for you here.");
    }
  }

  /**
   * Does things when you meet balloony.
   * @author Samantha - everything important
   * @author Stefano - music fades
   */
  public void balloony(){
    Enemy balloony = enemyMap.get("balloony");
    if (!player.getTrial(6) && balloony.getHealth() > 0){
      isInTrial = true;
      trial = 5;
      gui.println("Floating above the wreckage is a large blue balloon.");
      gui.println("\"My name is Balloony, I am the rightful head of customer service of Tableland. Prepare to die.\"");
      gui.println(balloony.getCatchphrase());
      fadeMusic(music);
      startMusic("data/audio/fighting.wav", -60);
      fadeInMusic(music, 1, -60, -25);
      if (enemyAttack(balloony)) return;
      fadeMusic(music, 20);
      startMusic("data/audio/background.wav", DEFAULT_BACKGROUND_MUSIC_VOL);
      gui.println();
      isInTrial = false;
      trial = 0;
      itemMap.get("balloony's corpse").isTakeable(true);
      gui.println("Balloony's corpse lays crumpled on the ground.");
      gui.println("You hear a little voice inside you saying \"Take the balloon.\"");
      gui.println("You never know when you'll need a balloon.");
    } else if (balloony.getHealth() < 1 && !player.getTrial(6) && currentRoom.getRoomName().equals("Dept. of Customer Service")){
      gui.println("Balloony's corpse lays crumpled on the ground.");
      gui.println("You hear a little voice inside you saying \"Take the balloon.\"");
      gui.println("You never know when you'll need a balloon.");
      gui.println(currentRoom.exitString());
    } else if (currentRoom.getRoomName().equals("Dept. of Customer Service")){
      gui.println("There is nothing for you here.");
      gui.println(currentRoom.exitString());
    }
  }

  /**
   * Does things when you go into the Fur Store room.
   * @author Michael - logic and dialogue
   * @author Samantha - setTrials
   */
  public void salesman(){
    if (!player.getTrial(3)){
      gui.println("A man dressed in a puffy fur coat approaches you, with a fur hat in hand.");
      gui.println("\"Would you like to buy my furs? Only for a small fee of ??500!\" He says.");
      gui.println("Will you buy the fur hat? (\"yes\"/\"no\")");
      if (buyFurs()){
        if (inventory.hasItem(itemMap.get("1000 british pounds")) && inventory.getCurrentWeight() - itemMap.get("1000 british pounds").getWeight() + itemMap.get("coonskin hat").getWeight() + itemMap.get("five hundred euros").getWeight() > inventory.getMaxWeight()){
          gui.println("\"Hmm... I can sense your pockets are too heavy. What a shame.");
        } else if (inventory.hasItem(itemMap.get("1000 british pounds"))){
          inventory.removeItem(itemMap.get("1000 british pounds"));
          inventory.addItem(itemMap.get("coonskin hat")); 
          inventory.addItem(itemMap.get("five hundred euros"));
          gui.println("Coonskin Hat taken!");
          gui.println(itemMap.get("coonskin hat").getDescription());
          player.setTrial(3);
          gui.println("\n\"Pleasure doing business with you, good sir.\"");
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
   * @author Michael - everything
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

  /**
   * Does things if you enter the Cheese Vault room.
   * @author Michael - everything
   */
  public void cheeseVault(){
    if (!hasOpenedVault){
      gui.println("The safe's dial taunts you. Maybe it's time to enter the code.");
      gui.println("ENTER CODE:");
      if (correctCode()){
        gui.println("Something clicks and the door swings open! Satisfied, you know you should grab a morsel of pristine Alaskan Cheese.");
        gui.println("Obviously, taking too much cheese is unbecoming of a future Whisperer. Only take one piece!"); 
        hasOpenedVault = true;
      } else {
        gui.println("...Nothing happens. I guess that was the wrong code. You walk out of the room, feeling unsatisfied.");
        currentRoom = roomMap.get("Upper Atrium");
        gui.println(currentRoom.shortDescription());
      }
    } else {
      gui.println("The vault door still hangs wide open, just as you left it.");
      if (!player.getTrial(2)){
        gui.println("You see small morsels of pristine Alaskan Cheese inside the princess' vault.");
      }
    }
  }

  /**
   * Checks if you entered the right code for the vault.
   * @return if the code was correct
   * @author Michael - everything
   */
  public boolean correctCode(){
    String in = gui.readCommand();
    if (in.equalsIgnoreCase("2956")) return true;
    return false;
  }

  /**
   * Does stuff for inflate command.
   * @param secondWord
   * @author Michael - everything
   */
  private void inflate(String secondWord) {
    if (!secondWord.equals("")){
      if (player.skyGodsCheck()){
        if ((secondWord.equals("balloon") || secondWord.equals("balloony")) && inventory.hasItem(itemMap.get("balloony")) && currentRoom.equals(roomMap.get("Shadowed Plains"))){
          gui.println("You inflated Balloony's corpse.");
          gui.println("You feel the air rush around you, as the balloon propels you into the Gods' domain.");
          fadeMusic(music, 30);
          startMusic("data/audio/sky.wav", DEFAULT_BACKGROUND_MUSIC_VOL - 5);
          currentRoom = roomMap.get("Sky Temple Pavillion");
          gui.println(currentRoom.longDescription());
        } else if (!player.getTalkedToSkyGods()){
          gui.println("The gods block entrance to their domain. You must do this, you tell yourself.");
        } else {
          gui.println("Your soul is not ready. Complete the 8 trials detailed in the Tome of Tableland before attempting.");
          gui.println("Try reading the Tome of Tableland.");
        }
      } else {
        gui.println("That doesn't seem like a good idea.");
      }
    } else {
      gui.println("What would you like to inflate?");
    }
  }

  /**
   * Activates when you go the the mysterious entrance to Hell (west tableland).
   * @author Samantha - everything important
   * @author Stefano - GUI cutscene
   */
  public void frogsMadleneAndJorge(){
    if (player.getTalkedToSkyGods()){
      gui.cutsceneMode(true);
      gui.println("You see a pair of frogs at the entrance.");
      if (!player.getTrial(9)) {
        sleep(2000);
        gui.println();
        gui.println("\"Hello future Whisperer. We are messagers from the Sky Gods. We are here to give you further instructions on how to rescue your friend and save Tableland.\" says one of the frogs.");
        gui.println("\"My name is Madlene,\" the first frog says, \"and this is Jorge,\" she gestures to the other frog.");
        sleep(5500);
        gui.println();
        gui.println("\"You must venture forth into the plains of Hell in West Tableland and save your friend from the being that resides in the Volcano!\" Jorge says. ");
        sleep(3500);
        gui.println();
        gui.println("Madlene hopped up to the tunnel and removed the boards from the tunnel.");
        gui.println("You can peer into the tunnel, but all you see is darkness.");
        gui.println("\"Go forward and save your friend!\" Jorge says. ");
        sleep(3000);
      } else {
        gui.println();
        gui.println("\"I urge you forward, future Whisperer! Connie must be saved!\" Madlene says.");
      }
      gui.cutsceneMode(false);
    } else {
      gui.println("The tunnel is boarded up, and you cannot pass through wood.");
      gui.println("Even still, you feel a strong pull from this place. Somehow you know it's very important. Maybe you should come back much later.");
    }
  }
  
  /**
   * Activates when you complete the trial in the sky.
   * @author Samantha - code, dialogue
   * @author Michael - dialogue
   * @author Stefano - GUI cutscene
   */
  public void skyGods(){
    gui.cutsceneMode(true);
    gui.println("The soothing ambience of the gods ring in your ears. It feels like your brain is being massaged by a baby deer.");
    gui.println("You glance up from your prayer and see the three towering thrones. On them sit three humans, who were not there before. Somehow, you know they are the true Gods of Tableland.\n");
    sleep(6000);
    gui.println("\"Welcome to the Temple of the Sky Gods, traveller.\" the first figure says.");
    gui.println("\"You have made it past the first eight trials, traveller.\" the second figure says.");
    gui.println("\"All you must do to prove yourself worthy of the title Whisperer...  Venture forth west and rescue the missing Customer Serviceman, from the being that resides there.\" says the third god.");
    gui.println("\"To aid you on your journey, we bestow upon you these divine artifacts.\" the first figure says.\n");
    sleep(9000);
    removeItems();
    inventory.addItem(itemMap.get("the sword of tableland"));
    inventory.addItem(itemMap.get("the shield of tableland"));
    gui.println("The Sword of Tableland taken!");
    gui.println(itemMap.get("the sword of tableland").getDescription());
    gui.println("\nThe Shield of Tableland taken!");
    gui.println(itemMap.get("the shield of tableland").getDescription());
    sleep(3000);
    gui.println("\n\"We have graced you with the sacred Sword and Shield of Tableland. These are the vices you must use.\" says the first god.");
    gui.println("\"We'll be taking any of your worthless mortal trinkets. You won't be needing any of those, I'm afraid.\" says the second god.");
    gui.println("\"Now go! Defeat what thou awaits you! Reclaim your destiny, future Whisperer!\" says the third god.\n");
    sleep(7000);
    player.talkedToSkyGods();
    gui.println("With that, the gods vanish before your eyes, and the peaceful ambience returns.");
    sleep(3000);
    gui.println();
    gui.println("You know what you must do.");
    gui.println("Go to Hell to save your friend, once and for all.");
    gui.cutsceneMode(false);
  }

  /**
   * Removes all your items (except moral support!)
   * @author Samantha - everything
   */
  public void removeItems(){
    if (inventory.hasItem(itemMap.get("geraldo"))){
      inventory.removeItem(itemMap.get("geraldo"));
    }
    if (inventory.hasItem(itemMap.get("scroll of news news"))){
      inventory.removeItem(itemMap.get("scroll of news news"));
    }
    if (inventory.hasItem(itemMap.get("five hundred euros"))){
      inventory.removeItem(itemMap.get("five hundred euros"));
    }
    if (inventory.hasItem(itemMap.get("balloony's corpse"))){
      inventory.removeItem(itemMap.get("balloony's corpse"));
    }
    if (inventory.hasItem(itemMap.get("bottle of water"))){
      inventory.removeItem(itemMap.get("bottle of water"));
    }
    if (inventory.hasItem(itemMap.get("bandages"))){
      inventory.removeItem(itemMap.get("bandages"));
    }
    if (inventory.hasItem(itemMap.get("coonskin hat"))){
      inventory.removeItem(itemMap.get("coonskin hat"));
    }
    if (inventory.hasItem(itemMap.get("alaskan cheese"))){
      inventory.removeItem(itemMap.get("alaskan cheese"));
    }
    if (inventory.hasItem(itemMap.get("key of friendship"))){
      inventory.removeItem(itemMap.get("key of friendship"));
    }
  }

  /** 
   * Allows the player to yell in the game
   * @param secondWord
   * @author Mr. DesLauriers
   */
  public void yell(String secondWord) {
    if (secondWord != ""){
        gui.println(secondWord.toUpperCase() + "!!!!!!");
        gui.println("Feel better?");
      } else {
        gui.println("ARGHHHHH!!!!!!");
        gui.println("Feel better?");
      }
  }

  /**
  * VERY IMPORTANT. lets player wear hat
  * @param command what the player is wearing
  * @author Michael - everything
  */
  private void wear(String secondWord) {
    if (secondWord != ""){
      if ((secondWord.equals("hat") || secondWord.equals("cap")) && inventory.hasItem(itemMap.get("coonskin hat"))){
        gui.println("You are now wearing the fur cap. How stylish!");
        player.setTrial(3);
        inventory.removeItem(itemMap.get("coonskin hat"));
      } else {
        gui.println("You cannot wear that!");
      }
    } else {
      gui.println("What would you like to wear?");
    }
  }

  /**
   * Does stuff when you pray.
   * @author Michael - everything
   */
  private void pray() {
    if (currentRoom.getRoomName().equals("News News Temple")){
      gui.println("The sun's rays bounce off the skylight into your eyes.");
      gui.println("For they glow with the intensity of a thousand souls.");
      gui.println("For you know they can never be satisfied. \n");
      gui.println("Suddenly, you feel a quite compelling message from deep within your psyche.");
      gui.println("\"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA!!!!!!!!!!!!\"");
    } else if (currentRoom.getRoomName().equals("Temple of the Sky Gods") && !player.getTalkedToSkyGods()){
      skyGods();
    } else {
      gui.println("You cannot pray here. You can only pray in divine places.");
    }
  }

  /**
   * Lets player read items (tome, diary, scroll)
   * @param command
   * @author Michael - for tome and diary
   * @author Samantha - for scroll
   */
  private void read(String secondWord){
    if (secondWord != ""){
      if (itemMap.get("tome").isThisItem(secondWord) && inventory.hasItem(itemMap.get("tome"))){
        readTome();
      } else if (secondWord.equals("diary") && currentRoom.getRoomName().equals("Master Bedroom")){
        readDiary();
      } else if (itemMap.get("scroll").isThisItem(secondWord) && inventory.hasItem(itemMap.get("scroll of news news"))){
        readScroll();
      } else {
        gui.println("You can't read that!");
      }
    } else {
      gui.println("What would you like to read?");
    }
  }

  /**
   *  What happens when the scroll is read.
   * @author Samantha - dialogue
   * @author Michael - dialogue
   */
  private void readScroll() {
    gui.println("Garbled, messy writing is scrawled onto the page. It reads:");
    gui.println();
    gui.println("\"The DAILY news table land???: The official news channel of Tableland!\"");
    gui.println("\"January 3 2022 | Balloony has taken down Connie!\"");
    gui.println("\"by Christopher Cha\"");
    gui.println("\"After serving harmoniously as co-heads of customer service for over two years, Balloony has forcibly removed Constantine from office.\"");
    gui.println("\"Connie was working in his office when Balloony entered with an armed sasquatch. The guards threw a bag over Connie's head, bound his hands and dragged him out of the room.\"");
    gui.println("\"Balloony told the people of Tableland this on a press conference on Friday. He has kidnapped his former co-head of customer service.\"");
    gui.println("\"Where could Balloony be keeping Connie?\" You think.");
  }

  /**
   *  Player reads princess diary
   * @author Michael - everything
   */
  private void readDiary() {
    gui.println("\"Dear diary,\"");
    gui.println("\"Wow, I sure do love cheese.\"");
    gui.println("\"I sure am glad nobody knows that the secret code for the cheese vault is 2956.\"");
  }

  /**
   *  Player reads tome of tableland !!!
   * @author Michael - everything
   */
  private void readTome() {
    player.setHasReadTome(true);
    gui.println("\"THE TWELVE TRIALS OF THE WHISPERER\"");
    gui.println("\"Doth whom unleavens ye ag??d tome, come with ye eyes here.\"");
    gui.println("\"1. Conquer ye Guardian Sasquatch of legends yore.\"");
    gui.println("\"2. Procure thee News News scroll, doth of antiquity.\"");
    gui.println("\"3. Practice larceny upon morsels of Alaskan cheese.\"");
    gui.println("\"4. Secure ye fabulous furs in all Canadian lands.\"");
    gui.println("\"5. Upheave Vaccuum, a foe of many C's.\"");
    gui.println("\"6. Threaten the most deadliest liquid upon the Robot.\"");
    gui.println("\"7. Usurp the meddling head of Customer Service.\"");
    gui.println("\"8. Take a jaunt over to Dog Paradise.\"");
    gui.println("\"Only then thou will be granted access to the Trial in the Sky, where the final four trials await...\"");
  }

  /**
   * when player types "heal" (no args).
   * @author Michael - everything
   */
  private void heal() {
    if (!inventory.hasItem(itemMap.get("bandages"))){
      gui.println("You have no healing items!");
    } else if (player.getHealth() != 100){
      player.maxHeal();
      gui.println("Your wounds have healed, and your thoughts have been assuaged. You have been restored to full health.");
      inventory.getItem("bandages").decrementQuantity();
      if (inventory.getItem("bandages").getQuantity() > 1){
        gui.println("You have " + inventory.getItem("bandages").getQuantity() + " bandages left.");
      } else if (inventory.getItem("bandages").getQuantity() != 0){
        gui.println("You have 1 bandage left. Your stash grows thin.");
      } else {
        gui.println("You have no more bandages.");
        inventory.removeItem(itemMap.get("bandages"));
      }
    } else if (player.getHealth() == 100){
      gui.println("Your being overflows with vigor. You are already at maximum health!");
      if (inventory.getItem("bandages").getQuantity() > 1){
        gui.println("You have " + inventory.getItem("bandages").getQuantity() + " bandages left.");
      } else if (inventory.getItem("bandages").getQuantity() != 0){
        gui.println("You have 1 bandage left. Your stash grows thin.");
      }
    }
    gui.println("Your current health is " + player.getHealth() + ".");
  }

  /**
   *  If you try to 'go up' with balloony in your inventory
   * @author Michael - everything
   */
  public static void printBalloonHelp() {
    gui.println("The clouds are too high in the sky. Maybe try inflating Balloony?");
  }

   /**
   *  If you try to explore without reading tome
   * @author Michael - everything
   */
  public static void printTomeHelp() {
    gui.println("Maybe you should read the Tome of Tableland first.");
  }

  /**
   * Print out some help information. Here we print some stupid, cryptic message
   * and a list of the command words.
   * @author Stefano - dialogue
   * @author Michael - dialogue
   */
  public void printHelp(Command command) {
    if (command.hasArgs()) Parser.printCommandHelp(command);
    else {
      gui.println("You are an adventurer in the marvelous lands of Tableland,");
      gui.println("always in search for things to do and items to collect.");
      gui.println();
      gui.println("The available commands are:");
      Parser.showCommands();
    }    
  }

  /**
   * Plays music.
   * @author Stefano - everything
   */
  public void music(Command command){
    if (!command.hasArgs()) gui.println("What do you want to do with the music?");
    else if (command.getStringifiedArgs().equals("stop")){
      music.stop();
      musicPlaying = false;
      gui.println("Music stopped.");
    } 
    else if (command.getStringifiedArgs().equals("start") || command.getStringifiedArgs().equals("play")){
      music.play();
      musicPlaying = true;
      gui.println("Music started!");
    } 
    else if (Game.getMusicPlayer().getVolume() > -75.1f && command.getStringifiedArgs().equals("volume-down")){
      music.setVolume(Game.getMusicPlayer().getVolume() - 5);
      musicVolumeOffset -= 5;
      gui.println("Music volume down.");
    } 
    else if (Game.getMusicPlayer().getVolume() < -5f && command.getStringifiedArgs().equals("volume-up")){
      music.setVolume(Game.getMusicPlayer().getVolume() + 5);
      musicVolumeOffset += 5;
      gui.println("Music volume up.");
    } 
    else if (Game.getMusicPlayer().getVolume() > -75.1f && command.getStringifiedArgs().equals("volume down")){
      music.setVolume(Game.getMusicPlayer().getVolume() - 5);
      musicVolumeOffset -= 5;
      gui.println("Music volume down.");
    } 
    else if (Game.getMusicPlayer().getVolume() < -5f && command.getStringifiedArgs().equals("volume up")){
      music.setVolume(Game.getMusicPlayer().getVolume() + 5);
      musicVolumeOffset += 5;
      gui.println("Music volume up.");
    } 
    else {
      gui.println("Invalid music operation!");
    }
  }

  /**
   * Plays after you kill Mr. DesLauriers (RIP)
   * @author Stefano - Implemented credits, GUI cutscene, music
   * @author Michael - Wrote credits, dialogue
   */
  private void endOfGame() {
    gameEnded = true;
    gui.cutsceneMode(true);
    sleep(3500);
    gui.println();
    gui.println("You feel the ever-changing world shift once again under your feet.");
    gui.println("With the power of the gods at your side, you have vanquished the terrorizing foe and have saved this realm. \n");
    sleep(5000);
    gui.println("Suddenly, Constantine, co-head of Customer Service, appears before you, hovering metres in the air.");
    gui.println("He motions cryptically with his hand. \n");
    sleep(4500);
    gui.println("The earth shakes once more. The volcano is about to collapse on itself!");
    gui.println("You dash to its edges, looking for a way out, when your vision suddenly blanks... \n");
    sleep(4500);
    gui.println("To be continued...");
    gui.println("\nPress Enter to continue.");
    gui.cutsceneMode(false);
    gui.readCommand();
    gui.reset();
    gui.centerText(false);
    gui.cutsceneMode(true);
    fadeMusic(music, 30);
    MusicPlayer credits = null;
    try {
      credits = new MusicPlayer("data/audio/credits.wav", false);
    } catch (FileNotFoundException e) {
      GameError.fileNotFound("data/audio/credits.wav");
    }
    credits.setVolume(0 + musicVolumeOffset <= 0 ? musicVolumeOffset : 0);
    if (!musicPlaying) credits.stop();
    else credits.play();
    for (int i = 0; i < gui.textLines() - 2; i++) {
      gui.printlnNoScroll();
    }
    gui.printlnNoScroll("Credits");
    gui.printlnNoScroll("\n\n\n\n");
    gui.printlnNoScroll("Developers");
    gui.printlnNoScroll();
    gui.printlnNoScroll("Gods of Tableland Studios");
    gui.printlnNoScroll("Stefano Esposto       Michael Gee       Samantha Sedran");
    gui.printlnNoScroll("\n\n\n");
    gui.printlnNoScroll("Characters");
    gui.printlnNoScroll();
    gui.printlnNoScroll("Mr. DesLauriers        Himself        ");
    gui.printlnNoScroll(" Constantine Vrachas Matthaios        Himself, Customer Service Rep.");
    gui.printlnNoScroll("           Christopher Cha        Himself, Head of News News");
    gui.printlnNoScroll("                Ms. Dybala        Princess of Alaskan Cheese");
    gui.printlnNoScroll();
    gui.printlnNoScroll("Jorge and Madlene        Pair of Frogs    ");
    gui.printlnNoScroll("                Sasquatch        Connie's Former Bodyguard");
    gui.printlnNoScroll("     Geraldo        Abused Rocks");
    gui.printlnNoScroll("  Lucky        Herself");
    gui.printlnNoScroll("   Luna        Herself");
    gui.printlnNoScroll(" Maggie        Herself");
    gui.printlnNoScroll();
    gui.printInfo("Disclaimer: Any similarities to actual people,");
    gui.printInfo("living or dead, is purely coincidental.");
    gui.printlnNoScroll("\n\n\n");
    gui.printlnNoScroll("Music");
    gui.printlnNoScroll();
    gui.printlnNoScroll("Daniel Rosenfeld (C418)");
    gui.printlnNoScroll("Lena Raine");
    gui.printlnNoScroll("Samantha Sedran");
    gui.printlnNoScroll("\n\n\n\n\n");
    gui.printlnNoScroll("Rest in Peace");
    gui.printlnNoScroll();
    gui.printlnNoScroll("Balloony");
    gui.printlnNoScroll("2018-2019");
    gui.printlnNoScroll();
    gui.printlnNoScroll("Forever in our hearts");    
    gui.printlnNoScroll("\n\n\n\n\n\n\n");
    gui.printlnNoScroll("Special Thanks\n");
    gui.printlnNoScroll("You!\n\n\n\n\n\n\n\n\n\n\n\n");
    gui.printlnNoScroll("Thanks for playing! Would you like to play again?\n");
    gui.printlnNoScroll("Press [y] to play again, [n] to quit.\n");
    gui.centerText(true);
    gui.scrollSmooth(50);
    gui.cutsceneMode(false);
    boolean validInput = false;
    while(!validInput){
      String in = gui.readCommand();
      if (in.equals("y")){
        credits.stop();
        music.setVolume(DEFAULT_BACKGROUND_MUSIC_VOL);
        if (musicPlaying) music.play();
        restartGame();
        validInput = true;
      } else if (in.equals("n")) endGame();
    }
    gui.commandsPrinted(true);
  }

  /**
   * Fades the specified MusicPlayer out based on the time factor specified.
   * @param toFade - The MusicPlayer to fade out.
   * @param timeFactor - The time factor for the fade.
   * This is roughly equivalent to a tenth of a second, so
   * a time factor of 40 is roughly equivalent to 4 seconds.
   * @author Stefano - everything
   */
  private void fadeMusic(MusicPlayer toFade, int timeFactor) {
    int vol = (int) toFade.getVolume();
    while(toFade.getVolume() > -80){
      toFade.setVolume(music.getVolume() - 0.000004);
      if (toFade.getVolume() % 1 == 0) sleep(timeFactor - (vol + 20) > 0 ? timeFactor - (vol + 20) : 0);
    }
    toFade.stop();
  }

  /**
   * Fades the specified MusicPlayer out.
   * @param toFade - The MusicPlayer to fade out.
   * @author Stefano - everything
   */
  private void fadeMusic(MusicPlayer toFade) {
    while(toFade.getVolume() > -64){
      toFade.setVolume(music.getVolume() - 0.000002);
    }
    toFade.stop();
  }

  /**
   * Fades the specified MusicPlayer in based on the time factor specified.
   * Also starts the music.
   * @param toFade - The MusicPlayer to fade in.
   * @param timeFactor - The time factor for the fade.
   * This is not a specific amount of time.
   * @param fromVol - Volume to start at.
   * @param toVol - Volume to get to.
   * @author Stefano - everything
   */
  private void fadeInMusic(MusicPlayer toFade, int timeFactor, double fromVol, double toVol) {
    toFade.setVolume(fromVol);
    if (musicPlaying) toFade.play();
    while(toFade.getVolume() < toVol - 1){
      toFade.setVolume(music.getVolume() + 0.000004);
      if (toFade.getVolume() % 1 == 0) sleep(timeFactor > 0 ? timeFactor : 0);
    }
  }

  //Below are utility functions, serving a purpose only for internal game management.

  /**
   * Causes the currently executing thread to sleep (temporarily cease execution) 
   * for the specified number of milliseconds, subject to the precision and accuracy 
   * of system timers and schedulers.
   * @param m - milliseconds to sleep for.
   * @author Stefano - everything
   */
  public void sleep(long m){
    try {
      Thread.sleep(m);
    } catch (InterruptedException e) {
    }
  }

  /**
   * Resets the game save state.
   * @author Stefano - everything
   */
  private void resetSaveState() {
    try {
      FileOutputStream fileOut = new FileOutputStream(GAME_SAVE_LOCATION);
      ObjectOutputStream out = new ObjectOutputStream(fileOut);
      out.writeObject(null);
      out.close();
      fileOut.close();
    } catch (IOException i) {
      gui.printerr("Error while resetting game save! Could not save.");
    }
  }

  /**
   * Returns the string for use in the GUI's info panel.
   * @return The string.
   * @author Stefano - everything
   */
  public String getGUIGameString() {
    if (gameEnded) return "";
    String roomExString = "";
    ArrayList<String> exits = new ArrayList<String>();
    for (Exit exit : currentRoom.getExits()) {
        exits.add(exit.getDirection());
    }
    roomExString = String.join(", ", exits);
    return "Inventory: " + inventory.getString() + " | Health: " + player.getHealth() + " | Exits: " + roomExString;
  }
}
