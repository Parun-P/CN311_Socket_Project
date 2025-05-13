import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Server for the Fish Battle game.
 * Manages connections and game flow between two clients.
 */
public class GameServer {
    private static final int PORT = 8080;
    private ServerSocket serverSocket;
    private ExecutorService pool;
    private Socket player1;
    private Socket player2;
    private GameState gameState;

    /**
     * Class to hold the current game state
     */
    private static class GameState {
        Board[] boards;
        int currentPlayer;
        boolean gameOver;
        boolean[] playersReady;

        public GameState() {
            boards = new Board[2];
            boards[0] = new Board();
            boards[1] = new Board();
            currentPlayer = 0;
            gameOver = false;
            playersReady = new boolean[2];
        }
    }

    /**
     * Start the game server
     */
    public void start() {
        try {
            serverSocket = new ServerSocket(PORT);
            pool = Executors.newFixedThreadPool(2);
            gameState = new GameState();

            System.out.println("Fish Battle Server started on port " + PORT);
            System.out.println("Waiting for players to connect...");

            // Wait for two players to connect
            player1 = serverSocket.accept();
            System.out.println("Player 1 connected: " + player1.getInetAddress());
            pool.execute(new PlayerHandler(player1, 0));

            player2 = serverSocket.accept();
            System.out.println("Player 2 connected: " + player2.getInetAddress());
            pool.execute(new PlayerHandler(player2, 1));

        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handles communication with a player
     */
    private class PlayerHandler implements Runnable {
        private Socket socket;
        private int playerId;
        private BufferedReader in;
        private PrintWriter out;

        public PlayerHandler(Socket socket, int playerId) {
            this.socket = socket;
            this.playerId = playerId;
            try {
                this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                this.out = new PrintWriter(socket.getOutputStream(), true);

                // Send player ID to client
                out.println("PLAYER " + playerId);

            } catch (IOException e) {
                System.err.println("Error setting up player " + playerId + ": " + e.getMessage());
            }
        }

        @Override
        public void run() {
            try {
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    handleCommand(inputLine);
                }
            } catch (IOException e) {
                System.err.println("Connection lost with player " + playerId + ": " + e.getMessage());
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    // Ignore close exceptions
                }

                // If a player disconnects, notify the other player
                int otherPlayer = (playerId == 0) ? 1 : 0;
                Socket otherSocket = (playerId == 0) ? player2 : player1;
                if (otherSocket != null && !otherSocket.isClosed()) {
                    try {
                        PrintWriter otherOut = new PrintWriter(otherSocket.getOutputStream(), true);
                        otherOut.println("OPPONENT_DISCONNECTED");
                    } catch (IOException e) {
                        // Ignore if we can't notify
                    }
                }

                System.out.println("Player " + playerId + " disconnected");
            }
        }

        /**
         * Handle commands from the client
         * 
         * @param command The command string to process
         */
        private void handleCommand(String command) {
            System.out.println("Player " + playerId + " sent: " + command);

            if (command.startsWith("PLACE_FISH")) {
                // Format: PLACE_FISH row col type orientation
                String[] parts = command.split(" ");
                if (parts.length == 5) {
                    int row = Integer.parseInt(parts[1]);
                    int col = Integer.parseInt(parts[2]);
                    Entity.Type type = Entity.Type.valueOf(parts[3]);
                    boolean horizontal = Boolean.parseBoolean(parts[4]);

                    boolean placed = gameState.boards[playerId].placeFish(row, col, type, horizontal);
                    out.println(placed ? "FISH_PLACED" : "INVALID_PLACEMENT");
                }
            } else if (command.equals("READY")) {
                gameState.playersReady[playerId] = true;

                // Check if both players are ready
                if (gameState.playersReady[0] && gameState.playersReady[1]) {
                    // Start the game with player 0
                    broadcastToAll("GAME_START");
                    broadcastToAll("TURN 0");
                }
            } else if (command.startsWith("ATTACK")) {
                // Only allow attacks if it's this player's turn and game is not over
                if (gameState.currentPlayer == playerId && !gameState.gameOver) {
                    // Format: ATTACK row col
                    String[] parts = command.split(" ");
                    if (parts.length == 3) {
                        int row = Integer.parseInt(parts[1]);
                        int col = Integer.parseInt(parts[2]);

                        // Apply attack to opponent's board
                        int opponentId = (playerId == 0) ? 1 : 0;
                        int result = gameState.boards[opponentId].applyAttack(row, col);

                        // Send result to both players
                        String resultType = (result == 0) ? "MISS" : (result == 1) ? "HIT" : "SINK";
                        broadcastToAll("ATTACK_RESULT " + playerId + " " + row + " " + col + " " + resultType);

                        // Check win condition
                        if (gameState.boards[opponentId].allFishSunk()) {
                            gameState.gameOver = true;
                            broadcastToAll("GAME_OVER " + playerId);
                        } else {
                            // Switch turns
                            gameState.currentPlayer = opponentId;
                            broadcastToAll("TURN " + gameState.currentPlayer);
                        }
                    }
                } else {
                    out.println("NOT_YOUR_TURN");
                }
            } else if (command.equals("RESTART")) {
                // Handle game restart logic if implemented
            }
        }

        /**
         * Send message to all connected players
         * 
         * @param message The message to broadcast
         */
        private void broadcastToAll(String message) {
            try {
                if (player1 != null && !player1.isClosed()) {
                    PrintWriter p1Out = new PrintWriter(player1.getOutputStream(), true);
                    p1Out.println(message);
                }

                if (player2 != null && !player2.isClosed()) {
                    PrintWriter p2Out = new PrintWriter(player2.getOutputStream(), true);
                    p2Out.println(message);
                }
            } catch (IOException e) {
                System.err.println("Error broadcasting message: " + e.getMessage());
            }
        }
    }

    /**
     * Main method to start the server
     */
    public static void main(String[] args) {
        GameServer server = new GameServer();
        server.start();
    }
}