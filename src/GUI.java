//Wildcard imports because there are too many imported classes...
import javax.swing.text.*;
import javax.swing.*;

import javax.swing.plaf.basic.BasicScrollBarUI;
import static org.awaitility.Awaitility.await;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.BorderLayout;
import java.awt.Insets;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;


/**
 * The GUI represents the interface between the program and the user.
 * <p>
 * A GUI object creates a frame that the user interacts with to play the game.
 * <p>
 * The GUI class is a singleton class, meaning the class has only one object. 
 * To access the GUI object, the constructor is not used. Instead, a call to {@code GUI.getGUI()}
 * returns the GUI object of the singleton class.
 */
public class GUI {
    //instance variables
    private JFrame frame;
    private JPanel gameContainer;
    private String inputCommand;
    private static StyleContext styleContext;
    private StyledDocument outputDoc;
    private JTextField input;
    private JScrollPane scroll;
    private JPanel inputContainer;
    private JTextArea roomInfo;

    //class variable
    private static GUI gui;

    /** The private constructor for the singleton GUI class.*/
    private GUI(){}

    /**
     * getGUI is an accessor method of the GUI class that returns the singleton object of the class.
     * <p>
     * getGUI is the ONLY way to create a GUI object. The constructor for the GUI class is private,
     * as is the case in a singleton class, so to access the singleton GUI object of the GUI class 
     * one must call getGUI.
     * @return A {@ GUI} object.
     */
    public static GUI getGUI() {
        if (gui == null) gui = new GUI();
        return gui;
    }

    public void createWindow() {
        //Set up window
        System.setProperty("awt.useSystemAAFontSettings","on");
        System.setProperty("swing.aatext", "true");
        frame = new JFrame("ZORK - Adventure Into Tableland");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 450);
        frame.setIconImage(new ImageIcon(getClass().getResource("images/icon.png")).getImage());
        frame.setLocationRelativeTo(null);
        Container pane = frame.getContentPane();
        pane.setBackground(Color.BLACK);
        styleContext = new StyleContext();
        styleContext.addStyle("", null);


        //add the main "container" that holds all the elements
        gameContainer = new JPanel();
        gameContainer.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        gameContainer.setLayout(new BoxLayout(gameContainer, BoxLayout.Y_AXIS));
        gameContainer.setBackground(Color.BLACK);


        //create the output text area
        JTextPane output = new JTextPane();
        outputDoc = output.getStyledDocument();
        output.setEditable(false);
        output.setBackground(Color.BLACK);
        output.setSelectionColor(Color.WHITE);
        output.setMargin(new Insets(5,5,5,5));
        output.setFont(new Font("Consolas", Font.PLAIN, 14));
        output.setForeground(Color.LIGHT_GRAY);
        output.setAutoscrolls(true);
        

        //add scroll bar beside text area
        scroll = new JScrollPane(output);
        scroll.getVerticalScrollBar().setBackground(Color.BLACK);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        scroll.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = Color.DARK_GRAY;
            }

            @Override
            protected JButton createDecreaseButton(int orientation) {
               return createZeroButton();
            }

            @Override
            protected JButton createIncreaseButton(int orientation) {
                return createZeroButton();
            }

            private JButton createZeroButton() {
                JButton button = new JButton();
                button.setPreferredSize(new Dimension(0, 0));
                button.setMinimumSize(new Dimension(0, 0));
                button.setMaximumSize(new Dimension(0, 0));
                return button;
            }
        });
        gameContainer.add(scroll);


        //TODO: add image stuff
        JLabel img = new JLabel();


        //add game info
        roomInfo = new JTextArea();
        roomInfo.setLineWrap(true);
        roomInfo.setWrapStyleWord(true);
        roomInfo.setEditable(false);
        roomInfo.setCaretColor(Color.WHITE);
        roomInfo.setBackground(Color.BLACK);
        roomInfo.setHighlighter(null);
        roomInfo.setMaximumSize(new Dimension(800, 100));
        roomInfo.setFont(new Font("Consolas", Font.ITALIC, 14));
        roomInfo.setForeground(Color.LIGHT_GRAY);
        gameContainer.add(roomInfo);

        


        //add the input text box
        input = new JTextField();
        input.setEditable(true);
        input.setCaretColor(Color.WHITE);
        input.setBackground(Color.BLACK);
        input.setSelectionColor(Color.WHITE);
        input.setBorder(BorderFactory.createEmptyBorder());
        input.setFont(new Font("Consolas", Font.PLAIN, 14));
        input.setForeground(Color.LIGHT_GRAY);
        //add key listener for the input box to check when a command is entered
        input.addKeyListener(new KeyListener(){
            ArrayList<String> commandsEntered = new ArrayList<String>();
            int commandIndex = 0;

            @Override
            public void keyPressed(KeyEvent e){
                if(e.getKeyCode() == KeyEvent.VK_ENTER){
                    String command = input.getText();
                    inputCommand = command;
                    commandIndex = commandsEntered.size();
                    commandsEntered.add(command);
                    commandIndex++;

                    input.setText("");
                    append("\n> ", null);
                    append(command + "\n", stylize(false, false, Color.YELLOW));
                    flush();
                }
                if(e.getKeyCode() == KeyEvent.VK_UP && commandIndex > 0){
                    commandIndex--; 
                    input.setText(commandsEntered.get(commandIndex));
                } 
                else if (e.getKeyCode() == KeyEvent.VK_DOWN && commandIndex < commandsEntered.size() - 1){
                    commandIndex++;
                    input.setText(commandsEntered.get(commandIndex));
                }
                else if(e.getKeyCode() == KeyEvent.VK_DOWN && commandIndex == commandsEntered.size() - 1){
                    commandIndex++;
                    input.setText("");
                }
            }

            @Override
            public void keyTyped(KeyEvent e) {
            }

            @Override
            public void keyReleased(KeyEvent e) {
    }
        });


        //add text area for the ">"
        JTextField cmd = new JTextField("> ");
        cmd.setEditable(false);
        cmd.setFont(new Font("Consolas", Font.PLAIN, 14));
        cmd.setForeground(Color.LIGHT_GRAY);
        cmd.setBackground(Color.BLACK);
        cmd.setSelectionColor(Color.WHITE);
        cmd.setBorder(BorderFactory.createEmptyBorder());
        

        //add container for the ">" and the text input area
        inputContainer = new JPanel();
        inputContainer.setLayout(new BorderLayout());
        inputContainer.add(input, BorderLayout.CENTER);
        inputContainer.add(cmd, BorderLayout.LINE_START);
        inputContainer.setMaximumSize(new Dimension(800, 40));
        gameContainer.add(inputContainer);


        //initialize the frame
        pane.add(gameContainer);
        frame.setVisible(true);
    }

    /**
     * Reads the command input from the GUI.
     * @return The command String.
     */
    public String readCommand() {
        String command;
        await().forever().until(() -> inputCommand != null);
        command = inputCommand;
        inputCommand = null;
        return command;
    }
    
    /**
     * Sets the game info - the info box above the command field.
     * <p>
     * <b>IMPORTANT NOTE: This is done very poorly at the current time.</b>
     * <p>
     * @param inventory - Player's inventory.
     * @param roomExits - Player's room exits.
     */
    public void setGameInfo(String inventory, int health, ArrayList<Exit> roomExits) {
        if (roomExits == null || inventory == null) throw new IllegalArgumentException("Parameters must be non-null.");
        String roomExString = "";
        ArrayList<String> exits = new ArrayList<String>();
        for (Exit exit : roomExits) {
            exits.add(exit.getDirection());
        }
        roomExString = String.join(", ", exits);
        roomInfo.setText("Inventory: " + inventory + " | Health: " + health + " | Exits: " + roomExString);
    }

    /**
     * Creates a JPanel spacer with a specified height.
     * @param height - height of the spacer.
     * @return The new JPanel spacer element.
     */
    private JPanel spacer(int height) {
        JPanel spacer = new JPanel();
        spacer.setPreferredSize(new Dimension(1, height));
        spacer.setMinimumSize(new Dimension(1, height));
        spacer.setMaximumSize(new Dimension(1, height));
        spacer.setBackground(Color.BLACK);
        return spacer;
    }

    //Below are all the print methods

    /** Prints a stream to the output JTextArea and adds a new line.*/
    public void println() {
        append("\n", null);
        flush();
    }

    /**
     * Prints a stream to the output JTextArea and adds a new line.
     * @param x - to be printed
     */
    public void println(int x) {
        append(x + "\n",  null);
        flush();
    }

    /**
     * Prints a stream to the output JTextArea and adds a new line.
     * @param x - to be printed
     */
    public void println(boolean x) {
        append(x + "\n", null);
        flush();
    }

    /**
     * Prints a stream to the output JTextArea and adds a new line.
     * @param x - to be printed
     */
    public void println(String x) {
        append(x + "\n", null);
        flush();
    }

    /**
     * Prints a stream to the output JTextArea and adds a new line.
     * @param x - to be printed
     */
    public void println(Object x) {
        append(x + "\n", null);
        flush();
    }

    /**
     * Prints a stream to the output JTextArea and adds a new line.
     * @param x - to be printed
     */
    public void println(double x) {
        append(x + "\n", null);
        flush();
    }

    /**
     * Prints a stream to the output JTextArea.
     * @param x - to be printed
     */
    public void print(int x) {
        append(x + "", null);
        flush();
    }

    /**
     * Prints a stream to the output JTextArea.
     * @param x - to be printed
     */
    public void print(boolean x) {
        append(x + "", null);
        flush();
    }

    /**
     * Prints a stream to the output JTextArea.
     * @param x - to be printed
     */
    public void print(String x) {
        append(x + "", null);
        flush();
    }

    /**
     * Prints a stream to the output JTextArea.
     * @param x - to be printed
     */
    public void print(double x) {
        append(x + "", null);
        flush();
    }

    /**
     * Prints a stream to the output JTextArea.
     * @param x - to be printed
     */
    public void print(Object x) {
        append(x + "", null);
        flush();
    }

    /**
     * Prints a stream to the output JTextArea.
     * <p>
     * To get a Style for stylizing the text, call {@code GUI.stylize()}.
     * @param x - to be printed
     * @param style - the Style for the text
     */
    public void printStyled(Object x, Style style) {
        append(x + "", style);
        flush();
    }

    /**
     * Prints a stream to the output JTextArea in an error format.
     * @param x - to be printed
     */
    public void printerr(Object x) {
        append(x + "\n", stylize(false, true, new Color(200, 50, 20)));
    }

    /**
     * Prints a stream to the output JTextArea in an error format.
     * @param x - to be printed
     */
    public void printInfo(Object x) {
        append(x + "\n", stylize(true, false, Color.LIGHT_GRAY));
    }

    /** FLushes the output JTextArea by resetting the scrollbar position.*/
    private void flush() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run(){
                scroll.getVerticalScrollBar().setValue(scroll.getVerticalScrollBar().getMaximum() - scroll.getVerticalScrollBar().getHeight());
            }
        });
    } 
    
    /**
     * Fully clears the output JTextArea.
     * @param x - to be printed
     */
    public void reset() {
        try {
            outputDoc.remove(0, outputDoc.getLength());
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
        flush();
    }

    /**
     * Appends text to a JTextPane.
     * @param str - The string to add.
     * @param attr - The attributes for the text added.
     */
    private void append(String str, AttributeSet attr) {
        try {
            outputDoc.insertString(outputDoc.getLength(), str, attr);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    /**
     * Stylizes text for printing to the output JTextPane.
     * @param italics - Specifies whether the text is italic or not.
     * @param bold - Specifies whether the text is bold or not.
     * @param textColour - Specifies the colour of the text.
     * @return A {@code Style} with the specified properties.
     */
    public static Style stylize(boolean italics, boolean bold, Color textColour) {
        if (styleContext == null) styleContext = new StyleContext();
        Style style = styleContext.getStyle("");
        StyleConstants.setForeground(style, textColour);
        StyleConstants.setBold(style, bold);
        StyleConstants.setItalic(style, italics);
        return style;
    }
}
