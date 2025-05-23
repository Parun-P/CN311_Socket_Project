import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Client application for the Block Battle game.
 * Provides GUI and handles communication with the server.
 */
public class GameClient extends Application {
    // GUI components
    private GridPane myBoardGrid;
    private GridPane opponentBoardGrid;
    private Button readyButton;
    private ToggleGroup blockTypeGroup;
    private ToggleGroup orientationGroup;
    private Text statusText;
    private Text remainingBlocksText;
    // Game state
    private Board myBoard;
    private Board opponentBoard;
    private int playerId;
    private boolean myTurn;
    private boolean gameStarted;
    private boolean placementPhase;
    private Map<Entity.Type, Integer> blockCounts;

    // Remember last placement attempt
    private int lastPlacementRow;
    private int lastPlacementCol;
    private Entity.Type lastPlacementType;
    private boolean lastPlacementHorizontal;

    // Track opponent's block count
    private int opponentRemainingBlocks = 6;

    // Networking
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private static final String HOST = "localhost";
    private static final int PORT = 8080;

    // Cell display constants
    private static final int CELL_SIZE = 25;

    @Override
    public void start(Stage primaryStage) {
        myBoard = new Board();
        opponentBoard = new Board();
        placementPhase = true;
        gameStarted = false;
        myTurn = false;

        // Initialize available block counts
        blockCounts = new HashMap<>();
        blockCounts.put(Entity.Type.BLOCK_2x1, 2); // 2 blocks of size 2x1
        blockCounts.put(Entity.Type.BLOCK_3x1, 2); // 2 blocks of size 3x1
        blockCounts.put(Entity.Type.BLOCK_4x2, 1); // 1 block of size 4x2
        blockCounts.put(Entity.Type.BLOCK_5x1, 1); // 1 block of size 5x1

        // Connect to server
        connectToServer();

        // Build GUI
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(20));

        // Top section - Status
        VBox topSection = new VBox(10);
        topSection.setAlignment(Pos.CENTER);

        statusText = new Text("Place your blocks on the left board");
        statusText.setFont(Font.font(18));

        remainingBlocksText = new Text("Opponent blocks remaining: " + opponentRemainingBlocks);
        remainingBlocksText.setFont(Font.font(14));

        topSection.getChildren().addAll(statusText, remainingBlocksText);
        root.setTop(topSection);

        // Center section - Game boards
        HBox boardsContainer = new HBox(50);
        boardsContainer.setAlignment(Pos.CENTER);

        // My board
        VBox myBoardContainer = new VBox(10);
        myBoardContainer.setAlignment(Pos.CENTER);

        Text myBoardLabel = new Text("My Board");
        myBoardLabel.setFont(Font.font(16));

        myBoardGrid = createBoard(true);

        myBoardContainer.getChildren().addAll(myBoardLabel, myBoardGrid);

        // Opponent board
        VBox opponentBoardContainer = new VBox(10);
        opponentBoardContainer.setAlignment(Pos.CENTER);

        Text opponentBoardLabel = new Text("Opponent's Board");
        opponentBoardLabel.setFont(Font.font(16));

        opponentBoardGrid = createBoard(false);

        opponentBoardContainer.getChildren().addAll(opponentBoardLabel, opponentBoardGrid);

        boardsContainer.getChildren().addAll(myBoardContainer, opponentBoardContainer);
        root.setCenter(boardsContainer);

        // Bottom section - Controls
        VBox controlsContainer = new VBox(15);
        controlsContainer.setAlignment(Pos.CENTER);
        controlsContainer.setPadding(new Insets(20, 0, 0, 0));

        // Block type selection
        HBox blockTypeContainer = new HBox(10);
        blockTypeContainer.setAlignment(Pos.CENTER);

        Text blockTypeLabel = new Text("Block Type:");
        blockTypeGroup = new ToggleGroup();

        RadioButton block2x1Button = new RadioButton("2x1 (" + blockCounts.get(Entity.Type.BLOCK_2x1) + ")");
        block2x1Button.setToggleGroup(blockTypeGroup);
        block2x1Button.setUserData(Entity.Type.BLOCK_2x1);
        block2x1Button.setSelected(true);

        RadioButton block3x1Button = new RadioButton("3x1 (" + blockCounts.get(Entity.Type.BLOCK_3x1) + ")");
        block3x1Button.setToggleGroup(blockTypeGroup);
        block3x1Button.setUserData(Entity.Type.BLOCK_3x1);

        RadioButton block4x2Button = new RadioButton("4x2 (" + blockCounts.get(Entity.Type.BLOCK_4x2) + ")");
        block4x2Button.setToggleGroup(blockTypeGroup);
        block4x2Button.setUserData(Entity.Type.BLOCK_4x2);

        RadioButton block5x1Button = new RadioButton("5x1 (" + blockCounts.get(Entity.Type.BLOCK_5x1) + ")");
        block5x1Button.setToggleGroup(blockTypeGroup);
        block5x1Button.setUserData(Entity.Type.BLOCK_5x1);

        blockTypeContainer.getChildren().addAll(
                blockTypeLabel, block2x1Button, block3x1Button, block4x2Button, block5x1Button);

        // Orientation selection
        HBox orientationContainer = new HBox(10);
        orientationContainer.setAlignment(Pos.CENTER);

        Text orientationLabel = new Text("Orientation:");
        orientationGroup = new ToggleGroup();

        RadioButton horizontalButton = new RadioButton("Horizontal");
        horizontalButton.setToggleGroup(orientationGroup);
        horizontalButton.setUserData(Boolean.TRUE);
        horizontalButton.setSelected(true);

        RadioButton verticalButton = new RadioButton("Vertical");
        verticalButton.setToggleGroup(orientationGroup);
        verticalButton.setUserData(Boolean.FALSE);

        orientationContainer.getChildren().addAll(orientationLabel, horizontalButton, verticalButton);

        // Ready button
        readyButton = new Button("Ready");
        readyButton.setDisable(true);
        readyButton.setOnAction(e -> {
            if (placementPhase) {
                out.println("READY");
                disablePlacementControls();
                statusText.setText("Waiting for opponent...");
            }
        });

        controlsContainer.getChildren().addAll(blockTypeContainer, orientationContainer, readyButton);
        root.setBottom(controlsContainer);

        // Create scene - Make it responsive to window resizing
        Scene scene = new Scene(root);
        primaryStage.setTitle("Block Battle - Connecting...");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);

        // Make the boards resize with the window
        boardsContainer.prefWidthProperty().bind(scene.widthProperty().subtract(40));
        boardsContainer.prefHeightProperty().bind(scene.heightProperty().subtract(200));

        primaryStage.setOnCloseRequest(e -> {
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            Platform.exit();
        });
        primaryStage.show();

        // Start listener thread
        new Thread(this::receiveMessages).start();
    }

    /**
     * Create a 10x10 grid for game board
     * 
     * @param isMyBoard true if this is the player's board, false for opponent's
     *                  board
     * @return The created grid pane
     */
    private GridPane createBoard(boolean isMyBoard) {
        GridPane grid = new GridPane();
        grid.setHgap(2);
        grid.setVgap(2);
        grid.setStyle("-fx-background-color: lightblue; -fx-padding: 5;");

        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                Rectangle cell = new Rectangle(CELL_SIZE, CELL_SIZE);
                cell.setFill(Color.LIGHTBLUE);
                cell.setStroke(Color.DARKBLUE);

                final int r = row;
                final int c = col;

                if (isMyBoard) {
                    // For my board: place blocks during setup
                    cell.setOnMouseClicked(e -> {
                        if (placementPhase) {
                            placeBlock(r, c);
                        }
                    });
                } else {
                    // For opponent board: attack during game
                    cell.setOnMouseClicked(e -> {
                        if (gameStarted && myTurn) {
                            attackCell(r, c);
                        }
                    });
                }

                grid.add(cell, col, row);
            }
        }

        return grid;
    }

    /**
     * Place a block on the board
     * 
     * @param row Row index
     * @param col Column index
     */
    private void placeBlock(int row, int col) {
        Entity.Type selectedType = (Entity.Type) blockTypeGroup.getSelectedToggle().getUserData();
        Boolean horizontal = (Boolean) orientationGroup.getSelectedToggle().getUserData();

        // Check if we have any blocks of this type left
        if (blockCounts.get(selectedType) <= 0) {
            statusText.setText("No more blocks of this type available");
            return;
        }

        // Store last placement attempt
        lastPlacementRow = row;
        lastPlacementCol = col;
        lastPlacementType = selectedType;
        lastPlacementHorizontal = horizontal;

        // Send placement command to server
        out.println("PLACE_BLOCK " + row + " " + col + " " + selectedType + " " + horizontal);
    }

    /**
     * Attack a cell on the opponent's board
     * 
     * @param row Row index
     * @param col Column index
     */
    private void attackCell(int row, int col) {
        // Check if cell was already hit
        if (opponentBoard.getEntity(row, col).isHit()) {
            statusText.setText("You already attacked this position!");
            return;
        }

        // Send attack command to server
        out.println("ATTACK " + row + " " + col);
        myTurn = false; // Disable further attacks until turn comes back
    }

    /**
     * Update the visual representation of a cell
     * 
     * @param isMyBoard true if updating player's board, false for opponent's board
     * @param row       Row index
     * @param col       Column index
     * @param entity    The entity to display
     */
    private void updateCell(boolean isMyBoard, int row, int col, Entity entity) {
        GridPane grid = isMyBoard ? myBoardGrid : opponentBoardGrid;
        Rectangle cell = (Rectangle) grid.getChildren().get(row * Board.SIZE + col);

        if (isMyBoard) {
            // My board: show blocks
            if (entity.isBlock()) {
                if (entity.isHit()) {
                    cell.setFill(Color.DARKRED); // Sunk
                } else {
                    switch (entity.getType()) {
                        case BLOCK_2x1:
                            cell.setFill(Color.LIGHTGREEN);
                            break;
                        case BLOCK_3x1:
                            cell.setFill(Color.LIGHTGREEN);
                            break;
                        case BLOCK_4x2:
                            cell.setFill(Color.LIGHTGREEN);
                            break;
                        case BLOCK_5x1:
                            cell.setFill(Color.LIGHTGREEN);
                            break;
                        default:
                            cell.setFill(Color.LIGHTBLUE);
                    }
                }
            } else if (entity.isHit()) {
                cell.setFill(Color.DARKGRAY); // Miss
            } else {
                cell.setFill(Color.LIGHTBLUE); // Empty
            }
        } else {
            // Opponent board: hide unhit blocks
            if (entity.isHit()) {
                if (entity.isBlock()) {
                    cell.setFill(Color.RED); // Only current hit is marked red
                } else {
                    cell.setFill(Color.DARKGRAY); // Miss
                }
            } else {
                cell.setFill(Color.LIGHTBLUE); // Unknown or empty
            }
        }
    }

    /**
     * Connect to the game server
     */
    private void connectToServer() {
        try {
            socket = new Socket(HOST, PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            System.out.println("Connected to server");
        } catch (IOException e) {
            System.err.println("Could not connect to server: " + e.getMessage());
            showErrorAndExit("Could not connect to server. Please make sure the server is running.");
        }
    }

    /**
     * Show error dialog and exit the application
     * 
     * @param message Error message to display
     */
    private void showErrorAndExit(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Connection Error");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
            Platform.exit();
        });
    }

    /**
     * Disable placement controls when ready
     */
    private void disablePlacementControls() {
        readyButton.setDisable(true);
        for (Toggle toggle : blockTypeGroup.getToggles()) {
            ((RadioButton) toggle).setDisable(true);
        }
        for (Toggle toggle : orientationGroup.getToggles()) {
            ((RadioButton) toggle).setDisable(true);
        }
    }

    /**
     * Update block count UI
     * 
     * @param type     Block type
     * @param decrease true to decrease count, false to increase
     */
    private void updateBlockCount(Entity.Type type, boolean decrease) {
        if (decrease) {
            blockCounts.put(type, blockCounts.get(type) - 1);
        } else {
            blockCounts.put(type, blockCounts.get(type) + 1);
        }

        // Update radio button text
        for (Toggle toggle : blockTypeGroup.getToggles()) {
            RadioButton rb = (RadioButton) toggle;
            Entity.Type buttonType = (Entity.Type) rb.getUserData();
            if (buttonType == type) {
                rb.setText(getTypeName(type) + " (" + blockCounts.get(type) + ")");
                break;
            }
        }

        // Enable/disable Ready button if all blocks are placed
        int totalRemaining = 0;
        for (Integer count : blockCounts.values()) {
            totalRemaining += count;
        }
        readyButton.setDisable(totalRemaining > 0);
    }

    /**
     * Get display name for block type
     * 
     * @param type Block type
     * @return String representation of block type
     */
    private String getTypeName(Entity.Type type) {
        switch (type) {
            case BLOCK_2x1:
                return "2x1";
            case BLOCK_3x1:
                return "3x1";
            case BLOCK_4x2:
                return "4x2";
            case BLOCK_5x1:
                return "5x1";
            default:
                return "Unknown";
        }
    }

    /**
     * Receive and process messages from the server
     */
    private void receiveMessages() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                final String message = line;
                Platform.runLater(() -> processServerMessage(message));
            }
        } catch (IOException e) {
            if (!socket.isClosed()) {
                System.err.println("Connection lost: " + e.getMessage());
                showErrorAndExit("Connection to server lost.");
            }
        }
    }

    /**
     * Process incoming server messages
     * 
     * @param message The message from server
     */
    private void processServerMessage(String message) {
        System.out.println("Server: " + message);

        if (message.startsWith("PLAYER")) {
            // Set player ID
            playerId = Integer.parseInt(message.split(" ")[1]);
            System.out.println("Assigned player ID: " + playerId);

            // Update the window title with the correct player ID
            Platform.runLater(() -> {
                Stage stage = (Stage) myBoardGrid.getScene().getWindow();
                stage.setTitle("Block Battle - Player " + playerId);
            });
        } else if (message.equals("BLOCK_PLACED")) {
            // Block placement successful
            Entity.Type selectedType = lastPlacementType;

            // Update our local board since the server accepted the placement
            int width = 0;
            int height = 0;

            // Calculate dimensions based on block type and orientation
            switch (selectedType) {
                case BLOCK_2x1:
                    width = lastPlacementHorizontal ? 2 : 1;
                    height = lastPlacementHorizontal ? 1 : 2;
                    break;
                case BLOCK_3x1:
                    width = lastPlacementHorizontal ? 3 : 1;
                    height = lastPlacementHorizontal ? 1 : 3;
                    break;
                case BLOCK_4x2:
                    width = lastPlacementHorizontal ? 4 : 2;
                    height = lastPlacementHorizontal ? 2 : 4;
                    break;
                case BLOCK_5x1:
                    width = lastPlacementHorizontal ? 5 : 1;
                    height = lastPlacementHorizontal ? 1 : 5;
                    break;
                default:
                    break;
            }

            // Update our local board to reflect the placed block
            int blockId = myBoard.getNextBlockId();
            for (int r = lastPlacementRow; r < lastPlacementRow + height; r++) {
                for (int c = lastPlacementCol; c < lastPlacementCol + width; c++) {
                    Entity entity = myBoard.getEntity(r, c);
                    entity.setType(selectedType);
                    entity.setBlockId(blockId);
                }
            }
            myBoard.incrementTotalBlocks();

            updateBlockCount(selectedType, true);
            refreshBoard(true);
            statusText.setText("Block placed successfully!");
        } else if (message.equals("INVALID_PLACEMENT")) {
            // Block placement failed
            statusText.setText("Invalid block placement! Try again.");
        } else if (message.equals("GAME_START")) {
            // Game has started
            placementPhase = false;
            gameStarted = true;
            statusText.setText("Game started!");
        } else if (message.startsWith("TURN")) {
            // Turn notification
            int turnPlayerId = Integer.parseInt(message.split(" ")[1]);
            myTurn = (turnPlayerId == playerId);
            statusText.setText(myTurn ? "Your turn!" : "Opponent's turn");
        } else if (message.startsWith("ATTACK_RESULT")) {
            // Format: ATTACK_RESULT attackerId row col result
            String[] parts = message.split(" ");
            int attackerId = Integer.parseInt(parts[1]);
            int row = Integer.parseInt(parts[2]);
            int col = Integer.parseInt(parts[3]);
            String result = parts[4];

            boolean isMyAttack = (attackerId == playerId);

            if (isMyAttack) {
                // My attack on opponent's board
                Entity entity = opponentBoard.getEntity(row, col);
                entity.hit();

                if (result.equals("HIT") || result.equals("SINK")) {
                    // Updated: Set a temporary blockId for tracking connected cells
                    int tempBlockId = 100 + row * Board.SIZE + col; // Generate unique ID
                    entity.setType(Entity.Type.BLOCK_2x1); // Default, since we don't know actual type
                    entity.setBlockId(tempBlockId); // Set temporary block ID
                }

                if (result.equals("SINK")) {
                    entity.setSunk(true);
                    // Now when we mark adjacent cells, we'll use this tempBlockId
                    markAdjacentCellsAsSunk(row, col, false);

                    // Additional alert for sinking a block
                    showAlert("Block Sunk!", "You sunk an opponent's block!");

                    // Decrement opponent's block count
                    opponentRemainingBlocks--;
                    remainingBlocksText.setText("Opponent blocks remaining: " + opponentRemainingBlocks);
                }

                updateCell(false, row, col, entity);
            } else {
                // Opponent attack on my board
                Entity entity = myBoard.getEntity(row, col);
                entity.hit();

                if (result.equals("SINK")) {
                    markAllCellsOfBlockAsSunk(entity.getBlockId());

                    // Notify the player that their block was sunk
                    showAlert("Block Lost!", "Your opponent sunk one of your blocks!");
                }

                updateCell(true, row, col, entity);
            }
        } else if (message.startsWith("GAME_OVER")) {
            // Game over notification
            int winnerId = Integer.parseInt(message.split(" ")[1]);
            boolean isWinner = (winnerId == playerId);

            // Show a more prominent game over message
            showGameOverDialog(isWinner);

            statusText.setText(isWinner ? "You win!" : "You lose!");

            // Disable further interaction
            gameStarted = false;
            myTurn = false;
        } else if (message.equals("OPPONENT_DISCONNECTED")) {
            // Opponent disconnected
            statusText.setText("Opponent disconnected. Game over!");
            showAlert("Game Over", "Your opponent has disconnected from the game.");
            gameStarted = false;
            myTurn = false;
        }
    }

    /**
     * Show game over dialog with win/lose message
     * 
     * @param isWinner true if player won, false if player lost
     */
    private void showGameOverDialog(boolean isWinner) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Game Over");
        alert.setHeaderText(null);

        if (isWinner) {
            alert.setContentText("Congratulations! You sunk all opponent's blocks and won the game!");
        } else {
            alert.setContentText("Game over! Your opponent sunk all your blocks.");
        }

        alert.showAndWait();
    }

    /**
     * Show alert dialog
     * 
     * @param title   Dialog title
     * @param message Message to display
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Refresh the visual representation of the board
     * 
     * @param isMyBoard true to refresh player's board, false for opponent's board
     */
    private void refreshBoard(boolean isMyBoard) {
        Board board = isMyBoard ? myBoard : opponentBoard;

        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                Entity entity = board.getEntity(row, col);
                updateCell(isMyBoard, row, col, entity);
            }
        }
    }

    /**
     * Mark adjacent cells as part of a sunk block
     * 
     * @param row       Starting row
     * @param col       Starting column
     * @param isMyBoard true if player's board, false for opponent's board
     */
    private void markAdjacentCellsAsSunk(int row, int col, boolean isMyBoard) {
        Board board = isMyBoard ? myBoard : opponentBoard;
        Entity entity = board.getEntity(row, col);
        int blockId = entity.getBlockId();

        // Only continue if this is a valid block cell
        if (!entity.isBlock() || blockId <= 0)
            return;

        // Check all directions for hit cells and mark them as sunk
        int[][] directions = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };

        for (int[] dir : directions) {
            int newRow = row + dir[0];
            int newCol = col + dir[1];

            if (board.isValidCoordinate(newRow, newCol)) {
                Entity adjacent = board.getEntity(newRow, newCol);
                // Only mark as sunk if it's the same block (same blockId)
                if (adjacent.isHit() && adjacent.isBlock() && !adjacent.isSunk() &&
                // For opponent board, we don't know blockId so we just check if it's hit
                        (isMyBoard ? adjacent.getBlockId() == blockId : true)) {

                    adjacent.setSunk(true);
                    updateCell(isMyBoard, newRow, newCol, adjacent);
                    // Recursively mark connected cells
                    markAdjacentCellsAsSunk(newRow, newCol, isMyBoard);
                }
            }
        }
    }

    /**
     * Mark all cells of a block as sunk
     * 
     * @param blockId The ID of the block to mark as sunk
     */
    private void markAllCellsOfBlockAsSunk(int blockId) {
        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                Entity entity = myBoard.getEntity(row, col);
                if (entity.getBlockId() == blockId) {
                    entity.setSunk(true);
                    updateCell(true, row, col, entity);
                }
            }
        }
    }

    /**
     * Main method to launch the application
     */
    public static void main(String[] args) {
        launch(args);
    }
}