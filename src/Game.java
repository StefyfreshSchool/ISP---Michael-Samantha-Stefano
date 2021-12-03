import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class Game {
  private GUI gui;
  private MusicPlayer music;

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
      roomMap.put(roomId, room);
    }
  }

  /**
   * Main play routine. Loops until end of play.
   */
  public void play() {
    gui = GUI.getGUI();
    gui.createWindow();
    while(!gui.isLoaded()){}
    printWelcome();
    startMusic();
    
    // Enter the main command loop. Here we repeatedly read commands and
    // execute them until the game is over.

    boolean finished = false;
    while (!finished) {
      Command command;
      try {
        command = parser.getCommand();
        finished = processCommand(command);
      } catch (IOException e) {
        e.printStackTrace();
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
  private boolean processCommand(Command command) {
    if (command.isUnknown()) {
      gui.println("I don't know what you mean...");
      return false;
    }
    String commandWord = command.getCommandWord();
    if (commandWord.equals("help"))
      printHelp();
    else if (commandWord.equals("go"))
      goRoom(command);
    else if (commandWord.equals("quit")) {
      if (command.hasSecondWord())
        gui.println("Quit what?");
      else
        return true; // signal that we want to quit
    } else if (commandWord.equals("eat")) {
      gui.println("Do you really think you should be eating at a time like this?");
    } else if (commandWord.equals("yell")){
      yell(command.getSecondWord());
    
    } else if (commandWord.equals("music")) {
      if (!command.hasSecondWord()) gui.println("What do you want to do with the music?");
      else if (command.getSecondWord().equals("stop")){
        music.stop();
        gui.println("Music stopped.");
      } 
      else if (command.getSecondWord().equals("play")){
        music.play();
        gui.println("Music started!");
      } 
      else if (music.getVolume() > -75.1f && command.getSecondWord().equals("volume-down")){
        music.setVolume(music.getVolume() - 5);
        gui.println("Music volume down.");
      } 
      else if (music.getVolume() < -5f && command.getSecondWord().equals("volume-up")){
        music.setVolume(music.getVolume() + 5);
        gui.println("Music volume up.");
      } 
      else {
        gui.println("Invalid music operation!");
      }

    }
    return false;
  }

  
  /** 
   * @param secondWord
   */
  private void yell(String secondWord) {
    if (secondWord != null){
      gui.println(secondWord.toUpperCase() + "!!!!!!");
      gui.println("Feel better?");
    }else{
      gui.println("ARGHHHHH!!!!!!");
      gui.println("Feel better?");
    }
  }

  // implementations of user commands:

  /**
   * Print out some help information. Here we print some stupid, cryptic message
   * and a list of the command words.
   */
  private void printHelp() {
    gui.println("You are lost. You are alone. You wander");
    gui.println("around at Monash Uni, Peninsula Campus.");
    gui.println();
    gui.println("Your command words are:");
    parser.showCommands();
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

    String direction = command.getSecondWord();

    // Try to leave current room.
    Room nextRoom = currentRoom.nextRoom(direction);
    
    if (nextRoom == null)
      gui.println("There is no door!");
    else {
      currentRoom = nextRoom;
      gui.println(currentRoom.longDescription());
    }
  }

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
