//Wildcard imports because there are too many imported classes...
import javax.swing.text.*;
import javax.swing.*;

import javax.imageio.ImageIO;
import javax.swing.plaf.basic.BasicScrollBarUI;
import static org.awaitility.Awaitility.await;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.BorderLayout;
import java.awt.Insets;
import java.awt.event.KeyListener;
import java.awt.Button;
import java.io.File;
import java.io.IOException;
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
    private JTextArea gameInfo;
    private boolean isErrored;
    private boolean commandsPrinted;
    private KeyListener inputListener;

    //class variables
    private static GUI gui;
    private static Game gameObj;
    private JTextPane output;

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
        frame = new JFrame("Adventure Into Tableland - Trials of The Whisperer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 450);
        try {
            frame.setIconImage(ImageIO.read(new File("data/images/icon.png")));
        } catch (IOException e1) {
            isErrored = true;
        }
        frame.setLocationRelativeTo(null);
        Container pane = frame.getContentPane();
        pane.setBackground(Color.BLACK);
        styleContext = new StyleContext();
        styleContext.addStyle("", null);
        commandsPrinted = true;


        //add the main "container" that holds all the elements
        gameContainer = new JPanel();
        gameContainer.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        gameContainer.setLayout(new BoxLayout(gameContainer, BoxLayout.Y_AXIS));
        gameContainer.setBackground(Color.BLACK);


        output = new JTextPane();
        outputDoc = output.getStyledDocument();
        output.setEditable(false);
        output.setHighlighter(null);
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
        });
        scroll.getHorizontalScrollBar().setBackground(Color.BLACK);
        scroll.getHorizontalScrollBar().setUI(new BasicScrollBarUI() {
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
        });
        JPanel corner = new JPanel();
        corner.setBackground(Color.BLACK);
        scroll.setCorner(JScrollPane.LOWER_RIGHT_CORNER, corner);
        gameContainer.add(scroll);


        //add game info
        gameInfo = new JTextArea();
        gameInfo.setLineWrap(true);
        gameInfo.setWrapStyleWord(true);
        gameInfo.setEditable(false);
        gameInfo.setCaretColor(Color.WHITE);
        gameInfo.setBackground(Color.BLACK);
        gameInfo.setHighlighter(null);
        gameInfo.setMaximumSize(new Dimension(800, 100));
        gameInfo.setFont(new Font("Consolas", Font.ITALIC, 14));
        gameInfo.setForeground(Color.LIGHT_GRAY);
        gameContainer.add(gameInfo);


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
        inputListener = new KeyListener(){
            ArrayList<String> commandsEntered = new ArrayList<String>();
            int commandIndex = 0;
            String inProgressCommand = "";
            boolean isBrowsing = false;

            @Override
            public void keyPressed(KeyEvent e){
                if (e.getKeyCode() == KeyEvent.VK_ENTER){
                    String command = input.getText();
                    inputCommand = command;
                    commandIndex = commandsEntered.size();
                    commandsEntered.add(command);
                    commandIndex++;
                    inProgressCommand = "";

                    input.setText("");
                    if (commandsPrinted){
                        append("\n> ", null);
                        append(command + "\n", stylize(false, false, Color.YELLOW));
                        flush();
                    }
                }
                if (e.getKeyCode() == KeyEvent.VK_UP && commandIndex > 0){
                    commandIndex--; 
                    if (!isBrowsing) inProgressCommand = input.getText();
                    input.setText(commandsEntered.get(commandIndex));
                    isBrowsing = true;
                } 
                else if (e.getKeyCode() == KeyEvent.VK_DOWN && commandIndex < commandsEntered.size() - 1){
                    commandIndex++;
                    input.setText(commandsEntered.get(commandIndex));
                }
                else if (e.getKeyCode() == KeyEvent.VK_DOWN && commandIndex == commandsEntered.size() - 1){
                    commandIndex++;
                    input.setText(inProgressCommand);
                    isBrowsing = false;
                }
                else if (e.getKeyCode() == KeyEvent.VK_ESCAPE){
                    input.setText("");
                    inProgressCommand = "";
                }
                else if (e.getKeyCode() == KeyEvent.VK_F23){
                    commandsEntered = new ArrayList<String>();
                    commandIndex = 0;
                    inProgressCommand = "";
                    isBrowsing = false;
                }
            }

            @Override
            public void keyTyped(KeyEvent e) {
            }

            @Override
            public void keyReleased(KeyEvent e) {
            }
        };
        input.addKeyListener(inputListener);


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
        input.requestFocusInWindow();
        append("\nStarting...\n\n", stylize(true, false, Color.LIGHT_GRAY));
        if (isErrored){
            GameError.fileNotFound("data/images/icon.png");
        } 
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
     * Creates a JPanel spacer with a specified height.
     * @param height - height of the spacer.
     * @return The new JPanel spacer element.
     */
    // private JPanel spacer(int height) {
    //     JPanel spacer = new JPanel();
    //     spacer.setPreferredSize(new Dimension(1, height));
    //     spacer.setMinimumSize(new Dimension(1, height));
    //     spacer.setMaximumSize(new Dimension(1, height));
    //     spacer.setBackground(Color.BLACK);
    //     return spacer;
    // }

    public void printImg(String src){
        if (!new File(src).isFile()) GameError.fileNotFound(src);
        printStyled("\n", iconStyle(src));
    }

    //Below are all the print methods

    /** Prints a stream to the output JTextArea and adds a new line.*/
    public void println() {
        append("\n", null);
        flush();
        gameInfo.setText(gameObj.getGUIGameString());
    }

    /**
     * Prints a stream to the output JTextArea and adds a new line.
     * @param x - to be printed
     */
    public void println(int x) {
        append(x + "\n",  null);
        flush();
        gameInfo.setText(gameObj.getGUIGameString());
    }

    /**
     * Prints a stream to the output JTextArea and adds a new line.
     * @param x - to be printed
     */
    public void println(boolean x) {
        append(x + "\n", null);
        flush();
        gameInfo.setText(gameObj.getGUIGameString());
    }

    /**
     * Prints a stream to the output JTextArea and adds a new line.
     * @param x - to be printed
     */
    public void println(Object x) {
        append(x + "\n", null);
        flush();
        gameInfo.setText(gameObj.getGUIGameString());
    }

    /**
     * Prints a stream to the output JTextArea and adds a new line.
     * @param x - to be printed
     */
    public void println(double x) {
        append(x + "\n", null);
        flush();
        gameInfo.setText(gameObj.getGUIGameString());
    }

    /**
     * Prints a stream to the output JTextArea.
     * @param x - to be printed
     */
    public void print(int x) {
        append(x + "", null);
        flush();
        gameInfo.setText(gameObj.getGUIGameString());
    }

    /**
     * Prints a stream to the output JTextArea.
     * @param x - to be printed
     */
    public void print(boolean x) {
        append(x + "", null);
        flush();
        gameInfo.setText(gameObj.getGUIGameString());
    }

    /**
     * Prints a stream to the output JTextArea.
     * @param x - to be printed
     */
    public void print(double x) {
        append(x + "", null);
        flush();
        gameInfo.setText(gameObj.getGUIGameString());
    }

    /**
     * Prints a stream to the output JTextArea.
     * @param x - to be printed
     */
    public void print(Object x) {
        append(x + "", null);
        flush();
        gameInfo.setText(gameObj.getGUIGameString());
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
        gameInfo.setText(gameObj.getGUIGameString());
    }

    /**
     * Prints a stream to the output JTextArea in an error format.
     * @param x - to be printed
     */
    public void printerr(Object x) {
        append(x + "\n", stylize(false, true, new Color(200, 50, 20)));
        flush();
    }

    /**
     * Prints a stream to the output JTextArea in an error format.
     * @param x - to be printed
     */
    public void printInfo(Object x) {
        append(x + "\n", stylize(true, false, Color.LIGHT_GRAY));
        gameInfo.setText(gameObj.getGUIGameString());
    }

    /**
     * Prints a stream to the output JTextArea without scrolling down.
     * @param x - to be printed
     */
    public void printlnNoScroll(Object x) {
        append(x + "\n", null);
        gameInfo.setText(gameObj.getGUIGameString());
    }

    /**
     * Prints a stream to the output JTextArea without scrolling down.
     */
    public void printlnNoScroll() {
        append("\n", null);
        gameInfo.setText(gameObj.getGUIGameString());
    }



    /** FLushes the output JTextArea by resetting the scrollbar position.*/
    private void flush() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run(){
                scroll.getVerticalScrollBar().setValue(scroll.getVerticalScrollBar().getMaximum() - scroll.getVerticalScrollBar().getHeight());
                scroll.getHorizontalScrollBar().setValue(scroll.getHorizontalScrollBar().getMaximum() - scroll.getHorizontalScrollBar().getHeight());
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
        styleContext.removeStyle("");
        Style style = styleContext.addStyle("", null);
        StyleConstants.setForeground(style, textColour);
        StyleConstants.setBold(style, bold);
        StyleConstants.setItalic(style, italics);
        return style;
    }

    private Style iconStyle(String src) {
        if (styleContext == null) styleContext = new StyleContext();
        styleContext.removeStyle("");
        Style style = styleContext.addStyle("", null);
        StyleConstants.setIcon(style, new ImageIcon(src));
        return style;
    }

    public void centerText(boolean state){
        SimpleAttributeSet center = new SimpleAttributeSet();
        if (state) StyleConstants.setAlignment(center, StyleConstants.ALIGN_CENTER);
        else StyleConstants.setAlignment(center, StyleConstants.ALIGN_LEFT);
        outputDoc.setParagraphAttributes(0, outputDoc.getLength(), center, false);
    }

    private JButton createZeroButton() {
        JButton button = new JButton();
        button.setPreferredSize(new Dimension(0, 0));
        button.setMinimumSize(new Dimension(0, 0));
        button.setMaximumSize(new Dimension(0, 0));
        return button;
    }

	public void sendGameObj(Game game) {
        gameObj = game;
	}
   
    public void scrollSmooth(int milisSteps) {
        int end = scroll.getVerticalScrollBar().getMaximum();
        int i = 0;
        while(i < end - scroll.getVerticalScrollBar().getHeight()){
            scroll.getVerticalScrollBar().setValue(i);
            end = scroll.getVerticalScrollBar().getMaximum();
            try {
                Thread.sleep(milisSteps);
            } catch (InterruptedException e) {}
            i++;
        }
    }

    /**
     * Specifies whether or not to allow commands.
     * @param state
     */
    public void commandsPrinted(boolean state) {
        commandsPrinted = state;
    }

    /**
     * Resets the command memory.
     */
    public void resetCommands(){
        Button a = new Button("click");
        KeyEvent e = new KeyEvent(a, 1, 20, 1, KeyEvent.VK_F23, 'a');
        inputListener.keyPressed(e);
    }
}
