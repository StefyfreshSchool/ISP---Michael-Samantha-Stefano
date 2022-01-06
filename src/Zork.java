public class Zork {
  public static void main(String[] args) {
    try {
      Game game = new Game();
      game.play();
    } catch (Error e) {
      GUI.getGUI().printerr("\nGame cannot start.");
    }
  }
}