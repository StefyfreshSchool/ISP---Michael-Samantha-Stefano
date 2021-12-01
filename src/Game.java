import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Scanner;

/**
 * Class Game - the main class of the "Zork" game.
 *
 * Author: Michael Kolling Version: 1.1 Date: March 2000
 * 
 * This class is the main class of the "Zork" application. Zork is a very
 * simple, text based adventure game. Users can walk around some scenery. That's
 * all. It should really be extended to make it more interesting!
 * 
 * To play this game, create an instance of this class and call the "play"
 * routine.
 * 
 * This main class creates and initialises all the others: it creates all rooms,
 * creates the parser and starts the game. It also evaluates the commands that
 * the parser returns.
 */
public class Game {
  private GUI gui;
  private MusicPlayer music;

  private Parser parser;
  private Room currentRoom;
  // This is a MASTER object that contains all of the rooms and is easily
  // accessible.
  // The key will be the name of the room -> no spaces (Use all caps and
  // underscore -> Great Room would have a key of GREAT_ROOM
  // In a hashmap keys are case sensitive.
  // masterRoomMap.get("GREAT_ROOM") will return the Room Object that is the Great
  // Room (assuming you have one).
  private HashMap<String, Room> masterRoomMap;

  private void initRooms(String fileName) throws Exception {
    masterRoomMap = new HashMap<String, Room>();
    Scanner roomScanner;
    try {
      HashMap<String, HashMap<String, String>> exits = new HashMap<String, HashMap<String, String>>();
      roomScanner = new Scanner(new File(fileName));
      while (roomScanner.hasNext()) {
        Room room = new Room();
        // Read the Name
        String roomName = roomScanner.nextLine();
        room.setRoomName(roomName.split(":")[1].trim());
        // Read the Description
        String roomDescription = roomScanner.nextLine();
        room.setDescription(roomDescription.split(":")[1].replaceAll("<br>", "\n").trim());
        // Read the Exits
        String roomExits = roomScanner.nextLine();
        // An array of strings in the format E-RoomName
        String[] rooms = roomExits.split(":")[1].split(",");
        HashMap<String, String> temp = new HashMap<String, String>();
        for (String s : rooms) {
          temp.put(s.split("-")[0].trim(), s.split("-")[1]);
        }

        exits.put(roomName.substring(10).trim().toUpperCase().replaceAll(" ", "_"), temp);

        // This puts the room we created (Without the exits in the masterMap)
        masterRoomMap.put(roomName.toUpperCase().substring(10).trim().replaceAll(" ", "_"), room);

        // Now we better set the exits.
      }

      for (String key : masterRoomMap.keySet()) {
        Room roomTemp = masterRoomMap.get(key);
        HashMap<String, String> tempExits = exits.get(key);
        for (String s : tempExits.keySet()) {
          // s = direction
          // value is the room.

          String roomName2 = tempExits.get(s.trim());
          Room exitRoom = masterRoomMap.get(roomName2.toUpperCase().replaceAll(" ", "_"));
          roomTemp.setExit(s.trim(), exitRoom);
        }
      }

      roomScanner.close();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
  }

  /**
   * Create the game and initialise its internal map.
   */
  public Game() {
    try {
      initRooms("data/rooms.dat");
      currentRoom = masterRoomMap.get("ROOM_2");
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    parser = new Parser();
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
      Command command = parser.getCommand();
      finished = processCommand(command);
    }
    gui.println("Thank you for playing.  Good bye.");

    //Nice transition to exit the game
    sleep(1000);
    System.exit(0);
  }

  private void startMusic() {
    music = new MusicPlayer("data/audio/background.wav");
    music.setVolume(-20f);
    music.play();
  }

  /**
   * Print out the opening message for the player.
   */
  private void printWelcome() {
    gui.println();
    gui.println("Welcome to Zork!");
    gui.println("Zork is a new, incredibly boring adventure game.");
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

  public GUI getGUI(){
    return gui;
  }

  public void sleep(long m){
    try {
      Thread.sleep(m);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }
}
