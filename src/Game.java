import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class Game {
  private GUI gui;
  private static MusicPlayer music;

  public static HashMap<String, Room> roomMap = new HashMap<String, Room>();
  
  private Parser parser;
  private Room currentRoom;

  /**
   * Create the game and initialize its internal map.
   */
  public Game() {
    try {
      initRooms("src\\data\\rooms.json");
      currentRoom = roomMap.get("South of the Cyan House");
      
      //Initialize the game if a previous state was recorded
      JSONObject gameState = (JSONObject) new JSONParser().parse(Files.readString(Path.of("src/data/gameState.json")));
      if (gameState.get("inProgress").equals(true)){
        for (Map.Entry<String,Room> roomObj : roomMap.entrySet()) {
          String roomName = roomObj.getKey();
          if (roomName.equals(gameState.get("room"))){
            currentRoom = roomObj.getValue();
            break;
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    parser = new Parser();

    //reset game state to blank
    HashMap<String, Object> data = new HashMap<String, Object>();
    data.put("inProgress", false);
    data.put("room", null);
    JSONWriter("src/data/gameState.json", data);
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
      roomMap.put(roomId, room);
    }
  }

  /**
   * Main play routine. Loops until end of play.
   */
  public void play() {
    gui = GUI.getGUI();
    gui.createWindow();
    printWelcome();
    startMusic();
    gui.setGameInfo("Empty", currentRoom.getExits());
    
    // Enter the main command loop. Here we repeatedly read commands and
    // execute them until the game is over.

    boolean finished = false;
    while (!finished) {
      Command command;
      command = parser.getCommand();
      int status = processCommand(command);
      if (status == 1) finished = true;
      if (status == 2){
        music.stop();
        gui.print("Restarting");
        sleep(200);
        gui.print(".");
        sleep(200);
        gui.print(".");
        sleep(200);
        gui.print(".");
        sleep(400);
        gui.reset();
        try {
          initRooms("src\\data\\rooms.json");
          currentRoom = roomMap.get("South of the Cyan House");
        } catch (Exception e) {
          e.printStackTrace();
        }
        printWelcome();
        startMusic();
        gui.setGameInfo("Empty", currentRoom.getExits());
      }
      
    }
    gui.println("Thank you for playing.  Good bye.");

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
    gui.println();
    gui.println("Welcome to Zork!");
    gui.println("Zork is an amazing text adventure game!");
    gui.println("Type 'help' if you need help.");
    gui.println();
    gui.println(currentRoom.longDescription());
  }

  /**
   * Given a command, process (that is: execute) the command. If this command ends
   * the game, true is returned, otherwise false is returned.
   */
  private int processCommand(Command command) {
    if (command.isUnknown()) {
      gui.println("I don't know what you mean...");
      return 0;
    }
    String commandWord = command.getCommandWord();
    if (commandWord.equals("help")){
      printHelp();
    }
    else if (commandWord.equals("go")){
      goRoom(command);
    }
    else if (commandWord.equals("quit")) {
      if (command.hasSecondWord())
        gui.println("Quit what?");
      else
        if (quitRestart("quit")){
          return 1;
        }

    } else if (commandWord.equals("yell")){
      yell(command.getStringifiedArgs());
    
    } else if (commandWord.equals("music")) {
      music(command);

    } else if(commandWord.equals("hit")){
      //TODO: hit() when inventory is ready
    } else if (commandWord.equals("restart")) {
      if (quitRestart("restart")){
        return 2;
      }
    } else if (commandWord.equals("save")){
      if (save(command)) return 1;
    }
    return 0;
  }

  /**
   * Saves the game and optionally quits.
   * @param command
   */
  private boolean save(Command command) {
    boolean quit = false;
    gui.println("Would you like to save and quit the game, or just save?");
    gui.println("Type \"q\" to save and quit, \"s\" to just save, and \"c\" to cancel.");
    boolean validInput = false;
    while(!validInput){
      String in = gui.readCommand();
      if (in.equalsIgnoreCase("q")){
        gui.println("Game saved! Quitting.");
        quit = true;
        validInput = true;
      }
      else if (in.equalsIgnoreCase("c")){
        gui.println("Cancelled.");
        validInput = true;
      } else if (in.equalsIgnoreCase("s")){
        gui.println("Game saved!");
        validInput = true;
      } else {
        gui.println("\"" + in + "\" is not a valid choice!");
      }
      
    }
    HashMap<String, Object> data = new HashMap<String, Object>();
    data.put("inProgress", true);
    data.put("room", currentRoom.getRoomName());
    JSONWriter("src/data/gameState.json", data);

    return quit;
  }

  private void JSONWriter(String filePath, HashMap<String, Object> data) {
    try {
      FileWriter file = new FileWriter(filePath);
      file.write(JSONObject.toJSONString(data));
      file.flush();
      file.close();

    } catch (IOException e) {
      e.printStackTrace();
    } 
  }

  /**
   * Prompts the user if they want to quit or restart the game. 
   * After user input, it returns true or false.
   * @param string - Prints whether the operation is a quit or restart.
   * @return True or false based on if the user cancelled the operation or not.
   */
  private boolean quitRestart(String string) {
    gui.println("Are you sure you would like to " + string + " the game?");
    gui.println("Type \"y\" to confirm or \"n\" to cancel.");
    boolean validInput = false;
    while(!validInput){
      String in = gui.readCommand();
      if (in.equalsIgnoreCase("y")) return true;
      else if (in.equalsIgnoreCase("n")){
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
    if (!command.hasSecondWord()) {
      // if there is no second word, we don't know where to go...
      gui.println("Go where?");
      return;
    }

    String direction = command.getStringifiedArgs();

    // Try to leave current room.
    Room nextRoom = currentRoom.nextRoom(direction);
    if (nextRoom == null)
      gui.println("CODE ERROR: Room going " + direction + " does not exist!");
    else if (!currentRoom.canGoDirection(direction)){
      gui.println("That exit is locked! Come back later.");
    } else {
      currentRoom = nextRoom;
      if(currentRoom.getRoomName().equals("The Lair")){
        gui.println(currentRoom.shortDescription());
        sasquatch();
      }else{
        gui.println(currentRoom.longDescription());
      }
    }
    gui.setGameInfo("Empty", currentRoom.getExits());
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
  public void printHelp() {
    gui.println("You are lost. You are alone. You wander");
    gui.println("around at Monash Uni, Peninsula Campus.");
    gui.println();
    gui.println("Your command words are:");
    Parser.showCommands();
  }

  /**
   * Plays music.
   */
  public void music(Command command){
    if (!command.hasSecondWord()) gui.println("What do you want to do with the music?");
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
