import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GameServer {
    private static final int PORT = 8080;
    private ServerSocket serverSocket;
    private ExecutorService pool;
    private Socket player1;
    private Socket player2;
    private GameState gameState;

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

    public void start() {
        try {
            serverSocket = new ServerSocket(PORT);
            pool = Executors.newFixedThreadPool(2);
            gameState = new GameState();

            System.out.println("Block Battle Server started on port " + PORT);
            System.out.println("Waiting for players to connect...");

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

    // Handles การสื่อสารกับ client
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

                // ส่ง player id ไปให้ client แต่ละคน
                out.println("PLAYER " + (playerId + 1));

            } catch (IOException e) {
                System.err.println("Error setting up player " + (playerId + 1) + ": " + e.getMessage());
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
                System.err.println("Connection lost with player " + (playerId + 1) + ": " + e.getMessage());
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    System.err.println("Error closing socket: " + e.getMessage());
                }

                // เมื่อ player disconnect จะทำการแจ้งเตือนผู้เล่นอีกผ่าย
                Socket otherSocket = (playerId == 0) ? player2 : player1;
                if (otherSocket != null && !otherSocket.isClosed()) {
                    try {
                        PrintWriter otherOut = new PrintWriter(otherSocket.getOutputStream(), true);
                        otherOut.println("OPPONENT_DISCONNECTED");
                    } catch (IOException e) {
                        System.err.println("Could not notify opponent disconnection " + e.getMessage());
                    }
                }

                System.out.println("Player " + (playerId + 1) + " disconnected");
            }
        }

        private void handleCommand(String command) {
            System.out.println("Player " + (playerId + 1) + " sent: " + command);

            if (command.startsWith("PLACE_BLOCK")) {
                // Format: PLACE_BLOCK row col type orientation
                String[] parts = command.split(" ");
                if (parts.length == 5) {
                    int row = Integer.parseInt(parts[1]);
                    int col = Integer.parseInt(parts[2]);
                    Entity.Type type = Entity.Type.valueOf(parts[3]);
                    boolean horizontal = Boolean.parseBoolean(parts[4]);

                    boolean placed = gameState.boards[playerId].placeBlock(row, col, type, horizontal); // place block
                    out.println(placed ? "BLOCK_PLACED" : "INVALID_PLACEMENT");
                }
            } else if (command.equals("READY")) {
                gameState.playersReady[playerId] = true;

                if (gameState.playersReady[0] && gameState.playersReady[1]) {
                    // player 0 เริ่มก่อน
                    broadcastToAll("GAME_START");
                    broadcastToAll("TURN 1");
                }
            } else if (command.startsWith("ATTACK")) {
                if (gameState.currentPlayer == playerId && !gameState.gameOver) {
                    // Format: ATTACK row col
                    String[] parts = command.split(" ");
                    if (parts.length == 3) {
                        int row = Integer.parseInt(parts[1]);
                        int col = Integer.parseInt(parts[2]);

                        // โจมตี board ฝั่งตรงข้าม
                        int opponentId = (playerId == 0) ? 1 : 0;
                        int result = gameState.boards[opponentId].applyAttack(row, col);

                        String resultType = (result == 0) ? "MISS" : (result == 1) ? "HIT" : "SINK"; // 0:MISS,1:HIT,2:SINK
                        broadcastToAll("ATTACK_RESULT " + (playerId + 1) + " " + row + " " + col + " " + resultType);

                        // มีคนชนะ
                        if (gameState.boards[opponentId].allBlocksSunk()) {
                            gameState.gameOver = true;
                            broadcastToAll("GAME_OVER " + (playerId + 1));
                        } else {
                            // เปลี่ยน turn
                            gameState.currentPlayer = opponentId;
                            broadcastToAll("TURN " + (gameState.currentPlayer + 1));
                        }
                    }
                }
            }
        }

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

    public static void main(String[] args) {
        GameServer server = new GameServer();
        server.start();
    }
}