// import java.io.*;
// import java.net.*;
// import java.util.*;

// public class Server {
//     private static final int PORT = 12345;
//     private static final int BOARD_SIZE = 10;
//     private static final char SHIP = 'S';
//     private static final char HIT = 'X';
//     private static final char MISS = 'O';

//     private static class Player {
//         Socket socket;
//         BufferedReader in;
//         PrintWriter out;
//         char[][] board = new char[BOARD_SIZE][BOARD_SIZE];

//         Player(Socket socket) throws IOException {
//             this.socket = socket;
//             this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
//             this.out = new PrintWriter(socket.getOutputStream(), true);
//             initBoard();
//         }

//         void initBoard() {
//             for (char[] row : board)
//                 Arrays.fill(row, '.');

//             // สุ่มวางเรือ 5 ตำแหน่ง
//             Random rand = new Random();
//             for (int i = 0; i < 5; i++) {
//                 int x = rand.nextInt(BOARD_SIZE);
//                 int y = rand.nextInt(BOARD_SIZE);
//                 board[y][x] = SHIP;
//             }
//         }

//         void sendShipPositions() {
//             for (int y = 0; y < BOARD_SIZE; y++) {
//                 for (int x = 0; x < BOARD_SIZE; x++) {
//                     if (board[y][x] == SHIP) {
//                         out.println("SHIP " + x + " " + y);
//                     }
//                 }
//             }
//             out.println("SHIPS_DONE");
//         }        

//         boolean isDefeated() {
//             for (char[] row : board)
//                 for (char c : row)
//                     if (c == SHIP)
//                         return false;
//             return true;
//         }

//         boolean attack(int x, int y) {
//             if (board[y][x] == SHIP) {
//                 board[y][x] = HIT;
//                 return true;
//             } else {
//                 board[y][x] = MISS;
//                 return false;
//             }
//         }
//     }

//     public static void main(String[] args) throws IOException {
//         ServerSocket listener = new ServerSocket(PORT);
//         System.out.println("Battleship Server started...");

//         try {
//             Player player1 = new Player(listener.accept());
//             player1.out.println("WELCOME Player 1");
//             Player player2 = new Player(listener.accept());
//             player2.out.println("WELCOME Player 2");

//             player1.out.println("START");
//             player2.out.println("START");
            
//             player1.sendShipPositions();
//             player2.sendShipPositions();

//             Player current = player1;
//             Player opponent = player2;

//             while (true) {
//                 current.out.println("YOUR_TURN");
//                 opponent.out.println("WAIT");

//                 String move = current.in.readLine();  // x y
//                 String[] parts = move.split(" ");
//                 int x = Integer.parseInt(parts[0]);
//                 int y = Integer.parseInt(parts[1]);

//                 boolean hit = opponent.attack(x, y);
//                 current.out.println(hit ? "HIT" : "MISS");
//                 opponent.out.println("OPPONENT_ATTACK " + x + " " + y + " " + (hit ? "HIT" : "MISS"));

//                 if (opponent.isDefeated()) {
//                     current.out.println("YOU_WIN");
//                     opponent.out.println("YOU_LOSE");
//                     break;
//                 }

//                 // swap
//                 Player temp = current;
//                 current = opponent;
//                 opponent = temp;
//             }

//         } finally {
//             listener.close();
//         }
//     }
// }

// Server.java
import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    private static final int PORT = 12345;
    private static final int SIZE = 10;
    private static final char SHIP = 'S';
    private static final char HIT = 'X';
    private static final char MISS = 'O';

    private static class Player {
        Socket socket;
        BufferedReader in;
        PrintWriter out;
        char[][] board = new char[SIZE][SIZE];

        Player(Socket socket) throws IOException {
            this.socket = socket;
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.out = new PrintWriter(socket.getOutputStream(), true);
            for (char[] row : board)
                Arrays.fill(row, '.');
        }

        void setShip(int x, int y) {
            board[y][x] = SHIP;
        }

        boolean isDefeated() {
            for (char[] row : board)
                for (char c : row)
                    if (c == SHIP)
                        return false;
            return true;
        }

        boolean attack(int x, int y) {
            if (board[y][x] == SHIP) {
                board[y][x] = HIT;
                return true;
            } else if (board[y][x] == '.') {
                board[y][x] = MISS;
                return false;
            }
            return false; // Already hit
        }
    }

    public static void main(String[] args) throws IOException {
        ServerSocket listener = new ServerSocket(PORT);
        System.out.println("Server started...");

        try {
            Player p1 = new Player(listener.accept());
            p1.out.println("WELCOME 1");
            Player p2 = new Player(listener.accept());
            p2.out.println("WELCOME 2");

            setupShips(p1);
            setupShips(p2);

            p1.out.println("START");
            p2.out.println("START");

            Player current = p1, opponent = p2;

            while (true) {
                current.out.println("YOUR_TURN");
                opponent.out.println("WAIT");

                String move = current.in.readLine();
                String[] parts = move.split(" ");
                int x = Integer.parseInt(parts[0]);
                int y = Integer.parseInt(parts[1]);

                boolean hit = opponent.attack(x, y);
                current.out.println(hit ? "HIT" : "MISS");
                opponent.out.println("OPPONENT_ATTACK " + x + " " + y + " " + (hit ? "HIT" : "MISS"));

                if (opponent.isDefeated()) {
                    current.out.println("YOU_WIN");
                    opponent.out.println("YOU_LOSE");
                    break;
                }

                Player temp = current;
                current = opponent;
                opponent = temp;
            }
        } finally {
            listener.close();
        }
    }

    private static void setupShips(Player player) throws IOException {
        player.out.println("PLACE_SHIPS");
        for (int i = 0; i < 5; i++) {
            String line = player.in.readLine(); // format: x y
            String[] parts = line.split(" ");
            int x = Integer.parseInt(parts[0]);
            int y = Integer.parseInt(parts[1]);
            player.setShip(x, y);
        }
    }
}
