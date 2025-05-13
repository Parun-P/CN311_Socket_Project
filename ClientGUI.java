// import javax.swing.*;
// import java.awt.*;
// import java.awt.event.*;
// import java.io.*;
// import java.net.*;

// public class ClientGUI extends JFrame {
//     private static final int SIZE = 10;
//     private JButton[][] myBoardButtons = new JButton[SIZE][SIZE];
//     private JButton[][] enemyBoardButtons = new JButton[SIZE][SIZE];
//     private PrintWriter out;
//     private BufferedReader in;
//     private JPanel myBoardPanel = new JPanel(new GridLayout(SIZE, SIZE));
//     private JPanel enemyBoardPanel = new JPanel(new GridLayout(SIZE, SIZE));
//     private JLabel statusLabel = new JLabel("Connecting...", SwingConstants.CENTER);
//     private boolean[][] enemyCellsAttacked = new boolean[SIZE][SIZE]; // Track which cells have been attacked
//     private final Color DEFAULT_BUTTON_COLOR = UIManager.getColor("Button.background");
//     private int lastAttackedX = -1;
//     private int lastAttackedY = -1;

//     public ClientGUI(String serverAddress) throws IOException {
//         Socket socket = new Socket(serverAddress, 12345);
//         in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
//         out = new PrintWriter(socket.getOutputStream(), true);

//         setTitle("Battleship Client");
//         setSize(900, 700);
//         setLayout(new BorderLayout());

//         // Status Label at the top
//         statusLabel.setFont(new Font("Arial", Font.BOLD, 18));
//         add(statusLabel, BorderLayout.NORTH);

//         // Split pane for boards
//         JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
//         splitPane.setResizeWeight(0.5);
//         splitPane.setDividerSize(4);

//         JPanel leftPanel = new JPanel(new BorderLayout());
//         JLabel myLabel = new JLabel("Your Board", SwingConstants.CENTER);
//         myLabel.setFont(new Font("Arial", Font.PLAIN, 16));
//         leftPanel.add(myLabel, BorderLayout.NORTH);
//         leftPanel.add(myBoardPanel, BorderLayout.CENTER);

//         JPanel rightPanel = new JPanel(new BorderLayout());
//         JLabel enemyLabel = new JLabel("Enemy Board", SwingConstants.CENTER);
//         enemyLabel.setFont(new Font("Arial", Font.PLAIN, 16));
//         rightPanel.add(enemyLabel, BorderLayout.NORTH);
//         rightPanel.add(enemyBoardPanel, BorderLayout.CENTER);

//         splitPane.setLeftComponent(leftPanel);
//         splitPane.setRightComponent(rightPanel);

//         add(splitPane, BorderLayout.CENTER);

//         initializeMyBoard();
//         initializeEnemyBoard();

//         setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//         setVisible(true);

//         new Thread(this::listenToServer).start();
//     }

//     private void initializeMyBoard() {
//         for (int y = 0; y < SIZE; y++) {
//             for (int x = 0; x < SIZE; x++) {
//                 JButton btn = new JButton();
//                 btn.setEnabled(false); // Don't allow clicking on own board
//                 myBoardButtons[y][x] = btn;
//                 myBoardPanel.add(btn);
//             }
//         }
        
//     }

//     private void initializeEnemyBoard() {
//         for (int y = 0; y < SIZE; y++) {
//             for (int x = 0; x < SIZE; x++) {
//                 final int fx = x;
//                 final int fy = y;
//                 JButton btn = new JButton();
//                 enemyBoardButtons[y][x] = btn;
//                 btn.addActionListener(e -> {
//                     out.println(fx + " " + fy);
//                     enemyCellsAttacked[fy][fx] = true; // Mark this cell as attacked
//                     lastAttackedX = fx; // Remember last cell attacked
//                     lastAttackedY = fy;
//                     btn.setEnabled(false);
//                 });
//                 btn.setEnabled(false); // Initially disable clicking
//                 enemyBoardPanel.add(btn);
//             }
//         }
//     }


//     private void listenToServer() {
//         try {
//             while (true) {
//                 String line = in.readLine();
//                 if (line == null)
//                     break;

//                 if (line.equals("YOUR_TURN")) {
//                     SwingUtilities.invokeLater(() -> {
//                         statusLabel.setText("Your turn. Click a cell on the enemy board.");
//                         enableEnemyButtons();
//                     });
//                 } else if (line.equals("WAIT")) {
//                     SwingUtilities.invokeLater(() -> {
//                         statusLabel.setText("Waiting for opponent's move...");
//                         disableEnemyButtons();
//                     });
//                 } else if (line.startsWith("HIT")) {
//                     SwingUtilities.invokeLater(() -> {
//                         // Update the last attacked cell to show hit
//                         if (lastAttackedX >= 0 && lastAttackedY >= 0) {
//                             enemyBoardButtons[lastAttackedY][lastAttackedX].setBackground(Color.RED);
//                         }
//                         JOptionPane.showMessageDialog(this, "You HIT a ship!");
//                     });
//                 } else if (line.startsWith("MISS")) {
//                     SwingUtilities.invokeLater(() -> {
//                         // Update the last attacked cell to show miss
//                         if (lastAttackedX >= 0 && lastAttackedY >= 0) {
//                             enemyBoardButtons[lastAttackedY][lastAttackedX].setBackground(Color.BLUE);
//                         }
//                         JOptionPane.showMessageDialog(this, "You missed.");
//                     });
//                 } else if (line.startsWith("OPPONENT_ATTACK")) {
//                     String[] parts = line.split(" ");
//                     final int x = Integer.parseInt(parts[1]);
//                     final int y = Integer.parseInt(parts[2]);
//                     final String result = parts[3];
//                     SwingUtilities.invokeLater(() -> {
//                         myBoardButtons[y][x].setBackground(result.equals("HIT") ? Color.RED : Color.BLUE);
//                     });
//                 } else if (line.equals("YOU_WIN") || line.equals("YOU_LOSE")) {
//                     final String message = line;
//                     SwingUtilities.invokeLater(() -> {
//                         statusLabel.setText("Game Over: " + message);
//                         JOptionPane.showMessageDialog(this, message);
//                     });
//                     break;
//                 } else if (line.startsWith("SHIP")) {
//                     String[] parts = line.split(" ");
//                     int x = Integer.parseInt(parts[1]);
//                     int y = Integer.parseInt(parts[2]);
//                     SwingUtilities.invokeLater(() -> {
//                         myBoardButtons[y][x].setBackground(Color.GRAY);
//                     });
//                 } else if (line.equals("SHIPS_DONE")) {
//                     // อาจจะใส่ log หรือเปลี่ยนสถานะก็ได้
//                 }
                
//             }
//         } catch (IOException e) {
//             e.printStackTrace();
//         }
//     }

//     private void enableEnemyButtons() {
//         for (int y = 0; y < SIZE; y++) {
//             for (int x = 0; x < SIZE; x++) {
//                 // Only enable buttons that haven't been clicked yet
//                 if (!enemyCellsAttacked[y][x]) {
//                     enemyBoardButtons[y][x].setEnabled(true);
//                 }
//             }
//         }
//     }

//     private void disableEnemyButtons() {
//         for (int y = 0; y < SIZE; y++) {
//             for (int x = 0; x < SIZE; x++) {
//                 enemyBoardButtons[y][x].setEnabled(false);
//             }
//         }
//     }

//     public static void main(String[] args) {
//         SwingUtilities.invokeLater(() -> {
//             try {
//                 String serverAddress = JOptionPane.showInputDialog("Enter Server IP:");
//                 if (serverAddress != null && !serverAddress.isEmpty()) {
//                     new ClientGUI(serverAddress);
//                 }
//             } catch (IOException e) {
//                 JOptionPane.showMessageDialog(null, "Error connecting to server: " + e.getMessage());
//                 e.printStackTrace();
//             }
//         });
//     }
// }

// ClientGUI.java
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

public class ClientGUI extends JFrame {
    private static final int SIZE = 10;
    private JButton[][] myBoardButtons = new JButton[SIZE][SIZE];
    private JButton[][] enemyBoardButtons = new JButton[SIZE][SIZE];
    private boolean[][] myShips = new boolean[SIZE][SIZE];
    private boolean[][] enemyAttacked = new boolean[SIZE][SIZE];
    private PrintWriter out;
    private BufferedReader in;
    private JLabel statusLabel = new JLabel("Connecting...", SwingConstants.CENTER);
    private boolean placingShips = true;
    private int shipsPlaced = 0;
    private int lastX, lastY;

    public ClientGUI(String serverAddress) throws IOException {
        Socket socket = new Socket(serverAddress, 12345);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        setTitle("Battleship Client");
        setSize(900, 700);
        setLayout(new BorderLayout());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        statusLabel.setFont(new Font("Arial", Font.BOLD, 18));
        add(statusLabel, BorderLayout.NORTH);

        JPanel boardsPanel = new JPanel(new GridLayout(1, 2));

        JPanel myBoardPanel = new JPanel(new GridLayout(SIZE, SIZE));
        JPanel enemyBoardPanel = new JPanel(new GridLayout(SIZE, SIZE));

        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                JButton btn = new JButton();
                myBoardButtons[y][x] = btn;
                int fx = x, fy = y;
                btn.addActionListener(e -> {
                    if (placingShips && !myShips[fy][fx]) {
                        btn.setBackground(Color.GRAY);
                        myShips[fy][fx] = true;
                        out.println(fx + " " + fy);
                        shipsPlaced++;
                        if (shipsPlaced == 5) {
                            placingShips = false;
                            statusLabel.setText("Waiting for opponent to place ships...");
                        }
                    }
                });
                myBoardPanel.add(btn);
            }
        }

        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                JButton btn = new JButton();
                enemyBoardButtons[y][x] = btn;
                int fx = x, fy = y;
                btn.addActionListener(e -> {
                    if (!enemyAttacked[fy][fx]) {
                        lastX = fx;
                        lastY = fy;
                        out.println(fx + " " + fy);
                        disableEnemyBoard();
                    }
                });
                btn.setEnabled(false);
                enemyBoardPanel.add(btn);
            }
        }

        boardsPanel.add(myBoardPanel);
        boardsPanel.add(enemyBoardPanel);
        add(boardsPanel, BorderLayout.CENTER);

        setVisible(true);

        new Thread(this::listenToServer).start();
    }

    private void listenToServer() {
        try {
            while (true) {
                String line = in.readLine();
                if (line == null)
                    break;

                if (line.startsWith("WELCOME")) {
                    int playerNum = Integer.parseInt(line.split(" ")[1]);
                    statusLabel.setText("Welcome Player " + playerNum + ". Place 5 ships.");
                } else if (line.equals("PLACE_SHIPS")) {
                    statusLabel.setText("Place 5 ships.");
                } else if (line.equals("START")) {
                    statusLabel.setText("Game started. Waiting for your turn...");
                } else if (line.equals("YOUR_TURN")) {
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText("Your turn. Click enemy cell to attack.");
                        enableEnemyBoard();
                    });
                } else if (line.equals("WAIT")) {
                    SwingUtilities.invokeLater(() -> statusLabel.setText("Waiting for opponent's move..."));
                } else if (line.startsWith("HIT")) {
                    SwingUtilities.invokeLater(() -> {
                        enemyBoardButtons[lastY][lastX].setBackground(Color.RED);
                        enemyAttacked[lastY][lastX] = true;
                        JOptionPane.showMessageDialog(this, "You HIT a ship!");
                    });
                } else if (line.startsWith("MISS")) {
                    SwingUtilities.invokeLater(() -> {
                        enemyBoardButtons[lastY][lastX].setBackground(Color.BLUE);
                        enemyAttacked[lastY][lastX] = true;
                        JOptionPane.showMessageDialog(this, "You missed.");
                    });
                } else if (line.startsWith("OPPONENT_ATTACK")) {
                    String[] parts = line.split(" ");
                    int x = Integer.parseInt(parts[1]);
                    int y = Integer.parseInt(parts[2]);
                    String result = parts[3];
                    SwingUtilities.invokeLater(() -> {
                        myBoardButtons[y][x].setBackground(result.equals("HIT") ? Color.RED : Color.BLUE);
                    });
                } else if (line.equals("YOU_WIN") || line.equals("YOU_LOSE")) {
                    String result = line.equals("YOU_WIN") ? "You win!" : "You lose.";
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText(result);
                        JOptionPane.showMessageDialog(this, result);
                    });
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void enableEnemyBoard() {
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                if (!enemyAttacked[y][x])
                    enemyBoardButtons[y][x].setEnabled(true);
            }
        }
    }

    private void disableEnemyBoard() {
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                enemyBoardButtons[y][x].setEnabled(false);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                String serverAddress = JOptionPane.showInputDialog("Enter Server IP:");
                if (serverAddress != null && !serverAddress.isEmpty()) {
                    new ClientGUI(serverAddress);
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, "Failed to connect: " + e.getMessage());
            }
        });
    }
}