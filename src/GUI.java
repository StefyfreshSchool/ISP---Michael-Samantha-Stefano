import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;

import java.awt.*;
import java.awt.event.*;

public class GUI {
    //instance variables
    private JFrame frame;
    private JPanel gameContainer;
    private String inputCommand;
    private boolean isLoaded = false;
    private static GUI gui;

    private GUI(){}

    public static GUI getGUI() {
        if (gui == null) gui = new GUI();
        return gui;
    }

    public void createWindow() {
        //Set up window
        System.setProperty("awt.useSystemAAFontSettings","on");
        System.setProperty("swing.aatext", "true");
        frame = new JFrame("ZORK - An Epic Text Adventure Game!");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 400);
        frame.setIconImage(new ImageIcon(getClass().getResource("images/icon.png")).getImage());
        Container pane = frame.getContentPane();
        pane.setBackground(Color.BLACK);


        //add the main "container" that holds all the elements
        gameContainer = new JPanel();
        gameContainer.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        gameContainer.setLayout(new BoxLayout(gameContainer, BoxLayout.Y_AXIS));
        gameContainer.setBackground(Color.BLACK);


        //create the output text area
        JTextArea output = new JTextArea();
        output.setLineWrap(true);
        output.setEditable(false);
        output.setBackground(Color.BLACK);
        output.setSelectionColor(Color.LIGHT_GRAY);
        output.setMargin(new Insets(5,5,5,5));
        output.setFont(new Font("Consolas", Font.PLAIN, 14));
        output.setForeground(Color.LIGHT_GRAY);
        output.setAutoscrolls(true);
        

        //add scroll bar beside text area
        JScrollPane scroll = new JScrollPane(output);
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
                JButton jbutton = new JButton();
                jbutton.setPreferredSize(new Dimension(0, 0));
                jbutton.setMinimumSize(new Dimension(0, 0));
                jbutton.setMaximumSize(new Dimension(0, 0));
                return jbutton;
            }
        });
        gameContainer.add(scroll);


        //TODO: add image stuff
        JLabel img = new JLabel();


        //add a spacer
        gameContainer.add(spacer(10));


        //add the input text box
        JTextField input = new JTextField();
        input.setEditable(true);
        input.setCaretColor(Color.WHITE);
        input.setBackground(Color.BLACK);
        input.setForeground(Color.WHITE);
        input.setSelectionColor(Color.LIGHT_GRAY);
        input.setBorder(BorderFactory.createEmptyBorder());
        input.setFont(new Font("Consolas", Font.PLAIN, 14));
        input.setForeground(Color.LIGHT_GRAY);
        //add key listener for the input box to check when a command is entered
        input.addKeyListener(new KeyListener(){
            @Override
            public void keyPressed(KeyEvent e){
                if(e.getKeyCode() == KeyEvent.VK_ENTER){
                    inputCommand = input.getText();
                    input.setText("");
                    flush();
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
        cmd.setSelectionColor(Color.LIGHT_GRAY);
        cmd.setBorder(BorderFactory.createEmptyBorder());
        

        //add container for the ">" and the text input area
        JPanel inputContainer = new JPanel();
        inputContainer.setLayout(new BorderLayout());
        inputContainer.add(input, BorderLayout.CENTER);
        inputContainer.add(cmd, BorderLayout.LINE_START);
        inputContainer.setMaximumSize(new Dimension(1000,40));
        gameContainer.add(inputContainer);


        //initialize the frame
        pane.add(gameContainer);
        frame.setVisible(true);
        scroll.getVerticalScrollBar().setValue(Integer.MAX_VALUE);
        isLoaded = true;

        /**
         * NOTE
         *                                                                      JScrollPane       JViewport         JTextArea         
         * JTextArea jt = (JTextArea) ((JViewport) ((JScrollPane) gameContainer.getComponent(0)).getComponent(0)).getComponent(0);
         */
    }
    
    /**
     * Reads the command input from the GUI.
     * @return The command String.
     */
    public String readCommand() {
        String command = null;
        if (inputCommand != null){
            command = inputCommand;
            inputCommand = null;
        }
        return command;
    }

    public boolean isLoaded() {
        return isLoaded;
    }

    /**
     * Creates a JPanel spacer with a specified height.
     * @param height - height of the spacer
     * @return the new JPanel spacer element
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

    /**
     * Prints a stream to the output JTextArea and adds a new line.
     */
    public void println() {
        JTextArea jt = (JTextArea) ((JViewport) ((JScrollPane) gameContainer.getComponent(0)).getComponent(0)).getComponent(0);
        jt.append("\n");
        flush();
    }

    /**
     * Prints a stream to the output JTextArea and adds a new line.
     * @param x - to be printed
     */
    public void println(int x) {
        JTextArea jt = (JTextArea) ((JViewport) ((JScrollPane) gameContainer.getComponent(0)).getComponent(0)).getComponent(0);
        jt.append(x +"\n");
        flush();
    }

    /**
     * Prints a stream to the output JTextArea and adds a new line.
     * @param x - to be printed
     */
    public void println(boolean x) {
        JTextArea jt = (JTextArea) ((JViewport) ((JScrollPane) gameContainer.getComponent(0)).getComponent(0)).getComponent(0);
        jt.append(x +"\n");
        flush();
    }

    /**
     * Prints a stream to the output JTextArea and adds a new line.
     * @param x - to be printed
     */
    public void println(String x) {
        JTextArea jt = (JTextArea) ((JViewport) ((JScrollPane) gameContainer.getComponent(0)).getComponent(0)).getComponent(0);
        jt.append(x +"\n");
        flush();
    }

    /**
     * Prints a stream to the output JTextArea and adds a new line.
     * @param x - to be printed
     */
    public void println(Object x) {
        JTextArea jt = (JTextArea) ((JViewport) ((JScrollPane) gameContainer.getComponent(0)).getComponent(0)).getComponent(0);
        jt.append(x +"\n");
        flush();
    }

    /**
     * Prints a stream to the output JTextArea and adds a new line.
     * @param x - to be printed
     */
    public void println(double x) {
        JTextArea jt = (JTextArea) ((JViewport) ((JScrollPane) gameContainer.getComponent(0)).getComponent(0)).getComponent(0);
        jt.append(x +"\n");
        flush();
    }

    /**
     * Prints a stream to the output JTextArea.
     */
    public void print() {
        JTextArea jt = (JTextArea) ((JViewport) ((JScrollPane) gameContainer.getComponent(0)).getComponent(0)).getComponent(0);
        jt.append("\n");
        flush();
    }

    /**
     * Prints a stream to the output JTextArea.
     * @param x - to be printed
     */
    public void print(int x) {
        JTextArea jt = (JTextArea) ((JViewport) ((JScrollPane) gameContainer.getComponent(0)).getComponent(0)).getComponent(0);
        jt.append(x + "\n");
        flush();
    }

    /**
     * Prints a stream to the output JTextArea.
     * @param x - to be printed
     */
    public void print(boolean x) {
        JTextArea jt = (JTextArea) ((JViewport) ((JScrollPane) gameContainer.getComponent(0)).getComponent(0)).getComponent(0);
        jt.append(x + "\n");
        flush();
    }

    /**
     * Prints a stream to the output JTextArea.
     * @param x - to be printed
     */
    public void print(String x) {
        JTextArea jt = (JTextArea) ((JViewport) ((JScrollPane) gameContainer.getComponent(0)).getComponent(0)).getComponent(0);
        jt.append(x + "\n");
        flush();
    }

    /**
     * Prints a stream to the output JTextArea.
     * @param x - to be printed
     */
    public void print(double x) {
        JTextArea jt = (JTextArea) ((JViewport) ((JScrollPane) gameContainer.getComponent(0)).getComponent(0)).getComponent(0);
        jt.append(x + "\n");
        flush();
    }

    /**
     * Prints a stream to the output JTextArea.
     * @param x - to be printed
     */
    public void print(Object x) {
        JTextArea jt = (JTextArea) ((JViewport) ((JScrollPane) gameContainer.getComponent(0)).getComponent(0)).getComponent(0);
        jt.append(x + "\n");
        flush();
    }

    /**
     * FLushes the output JTextArea by resetting the cursor/scroll position.
     */
    private void flush() {
        JScrollPane scroll = (JScrollPane) gameContainer.getComponent(0);
        scroll.getVerticalScrollBar().setValue(0);
        try {
            Thread.sleep(12);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        scroll.getVerticalScrollBar().setValue(Integer.MAX_VALUE);
    }    
}
