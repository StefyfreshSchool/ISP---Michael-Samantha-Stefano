import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class Zork {
  public static void main(String[] args) {
    GUI gui = GUI.getGUI();
    try {
      gui.createWindow();
      Game game = new Game();
      game.play();
    } catch (Throwable t){
      gui.printerr("\nGame has crashed.");
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      t.printStackTrace(new PrintStream(out));
      gui.printerr("\n" + out.toString());
    }
  }
}