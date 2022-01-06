public class GameError {
    private static GUI gui = GUI.getGUI();

    public static void javaDependenciesNotFound() throws Error{
        gui.printerr("ERROR! Required java dependencies not found!");
        gui.printerr("Please ensure the 'lib' folder exists and has not been modified.");
        throw new Error();
    }

    public static void fileNotFound(String file) throws Error{
        gui.printerr("ERROR! File '" + file + "' is missing!");
        gui.printerr("Please ensure the 'data' folder exists and has not been modified.");
        throw new Error();
    }
}
