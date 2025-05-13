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
 * Client application for the Fish Battle game.
 * Provides GUI and handles communication with the server.
 */
public class GameClient extends Application {
    // GUI components
    private GridPane myBoardGrid;
    private GridPane opponentBoardGrid;
    private Button readyButton;
    private ToggleGroup fishTypeGroup;
    private ToggleGroup orientationGroup;
    private Text statusText;
    private Text remainingFishText;

    // Game state
    private Board myBoard;
    private Board opponentBoard;
    private int playerId;
    private boolean myTurn;
    private boolean gameStarted;
    private boolean placementPhase;
    private Map<Entity.Type, Integer> fishCounts;

    // Remember last placement attempt
    private int lastPlacementRow;
    private int lastPlacementCol;
    private Entity.Type lastPlacementType;
    private boolean lastPlacementHorizontal;

    // Track opponent's fish count
    private int opponentRemainingFish = 6;

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

        // Initialize available fish counts
        fishCounts = new HashMap<>();
        fishCounts.put(Entity.Type.FISH_2x1, 2); // 2 fish of size 2x1
        fishCounts.put(Entity.Type.FISH_3x1, 2); // 2 fish of size 3x1
        fishCounts.put(Entity.Type.FISH_4x2, 1); // 1 fish of size 4x2
        fishCounts.put(Entity.Type.FISH_5x1, 1); // 1 fish of size 5x1

        // Connect to server
        connectToServer();

        // Build GUI
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(20));

        // Top section - Status
        VBox topSection = new VBox(10);
        topSection.setAlignment(Pos.CENTER);

        statusText = new Text("Place your fish on the left board");
        statusText.setFont(Font.font(18));

        remainingFishText = new Text("Opponent fish remaining: " + opponentRemainingFish);
        remainingFishText.setFont(Font.font(14));

        topSection.getChildren().addAll(statusText, remainingFishText);
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

        // Fish type selection
        HBox fishTypeContainer = new HBox(10);
        fishTypeContainer.setAlignment(Pos.CENTER);

        Text fishTypeLabel = new Text("Fish Type:");
        fishTypeGroup = new ToggleGroup();

        RadioButton fish2x1Button = new RadioButton("2x1 (" + fishCounts.get(Entity.Type.FISH_2x1) + ")");
        fish2x1Button.setToggleGroup(fishTypeGroup);
        fish2x1Button.setUserData(Entity.Type.FISH_2x1);
        fish2x1Button.setSelected(true);

        RadioButton fish3x1Button = new RadioButton("3x1 (" + fishCounts.get(Entity.Type.FISH_3x1) + ")");
        fish3x1Button.setToggleGroup(fishTypeGroup);
        fish3x1Button.setUserData(Entity.Type.FISH_3x1);

        RadioButton fish4x2Button = new RadioButton("4x2 (" + fishCounts.get(Entity.Type.FISH_4x2) + ")");
        fish4x2Button.setToggleGroup(fishTypeGroup);
        fish4x2Button.setUserData(Entity.Type.FISH_4x2);

        RadioButton fish5x1Button = new RadioButton("5x1 (" + fishCounts.get(Entity.Type.FISH_5x1) + ")");
        fish5x1Button.setToggleGroup(fishTypeGroup);
        fish5x1Button.setUserData(Entity.Type.FISH_5x1);

        fishTypeContainer.getChildren().addAll(
                fishTypeLabel, fish2x1Button, fish3x1Button, fish4x2Button, fish5x1Button);

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

        controlsContainer.getChildren().addAll(fishTypeContainer, orientationContainer, readyButton);
        root.setBottom(controlsContainer);

        // Create scene - Make it responsive to window resizing
        Scene scene = new Scene(root);
        primaryStage.setTitle("Fish Battle - Connecting...");
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
                    // For my board: place fish during setup
                    cell.setOnMouseClicked(e -> {
                        if (placementPhase) {
                            placeFish(r, c);
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
     * Place a fish on the board
     * 
     * @param row Row index
     * @param col Column index
     */
    private void placeFish(int row, int col) {
        Entity.Type selectedType = (Entity.Type) fishTypeGroup.getSelectedToggle().getUserData();
        Boolean horizontal = (Boolean) orientationGroup.getSelectedToggle().getUserData();

        // Check if we have any fish of this type left
        if (fishCounts.get(selectedType) <= 0) {
            statusText.setText("No more fish of this type available");
            return;
        }

        // Store last placement attempt
        lastPlacementRow = row;
        lastPlacementCol = col;
        lastPlacementType = selectedType;
        lastPlacementHorizontal = horizontal;

        // Send placement command to server
        out.println("PLACE_FISH " + row + " " + col + " " + selectedType + " " + horizontal);
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
            // My board: show fish
            if (entity.isFish()) {
                if (entity.isHit()) {
                    if (entity.isSunk()) {
                        cell.setFill(Color.DARKRED); // Sunk
                    } else {
                        cell.setFill(Color.RED); // Hit
                    }
                } else {
                    switch (entity.getType()) {
                        case FISH_2x1:
                            cell.setFill(Color.LIGHTGREEN);
                            break;
                        case FISH_3x1:
                            cell.setFill(Color.LIGHTGREEN);
                            break;
                        case FISH_4x2:
                            cell.setFill(Color.LIGHTGREEN);
                            break;
                        case FISH_5x1:
                            cell.setFill(Color.LIGHTGREEN);
                            break;
                        default:
                            cell.setFill(Color.LIGHTBLUE);
                    }
                }
            } else if (entity.isHit()) {
                cell.setFill(Color.DARKGRAY); // Miss
            } else {
                cell.setFill(Color.LIGHTBLUE); // Empty water
            }
        } else {
            // Opponent board: hide unhit fish
            if (entity.isHit()) {
                if (entity.isFish()) {
                    cell.setFill(Color.RED); // Only current hit is marked red
                } else {
                    cell.setFill(Color.DARKGRAY); // Miss
                }
            } else {
                cell.setFill(Color.LIGHTBLUE); // Unknown or empty water
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
        for (Toggle toggle : fishTypeGroup.getToggles()) {
            ((RadioButton) toggle).setDisable(true);
        }
        for (Toggle toggle : orientationGroup.getToggles()) {
            ((RadioButton) toggle).setDisable(true);
        }
    }

    /**
     * Update fish count UI
     * 
     * @param type     Fish type
     * @param decrease true to decrease count, false to increase
     */
    private void updateFishCount(Entity.Type type, boolean decrease) {
        if (decrease) {
            fishCounts.put(type, fishCounts.get(type) - 1);
        } else {
            fishCounts.put(type, fishCounts.get(type) + 1);
        }

        // Update radio button text
        for (Toggle toggle : fishTypeGroup.getToggles()) {
            RadioButton rb = (RadioButton) toggle;
            Entity.Type buttonType = (Entity.Type) rb.getUserData();
            if (buttonType == type) {
                rb.setText(getTypeName(type) + " (" + fishCounts.get(type) + ")");
                break;
            }
        }

        // Enable/disable Ready button if all fish are placed
        int totalRemaining = 0;
        for (Integer count : fishCounts.values()) {
            totalRemaining += count;
        }
        readyButton.setDisable(totalRemaining > 0);
    }

    /**
     * Get display name for fish type
     * 
     * @param type Fish type
     * @return String representation of fish type
     */
    private String getTypeName(Entity.Type type) {
        switch (type) {
            case FISH_2x1:
                return "2x1";
            case FISH_3x1:
                return "3x1";
            case FISH_4x2:
                return "4x2";
            case FISH_5x1:
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
                stage.setTitle("Fish Battle - Player " + playerId);
            });
        } else if (message.equals("FISH_PLACED")) {
            // Fish placement successful
            Entity.Type selectedType = lastPlacementType;

            // Update our local board since the server accepted the placement
            int width = 0;
            int height = 0;

            // Calculate dimensions based on fish type and orientation
            switch (selectedType) {
                case FISH_2x1:
                    width = lastPlacementHorizontal ? 2 : 1;
                    height = lastPlacementHorizontal ? 1 : 2;
                    break;
                case FISH_3x1:
                    width = lastPlacementHorizontal ? 3 : 1;
                    height = lastPlacementHorizontal ? 1 : 3;
                    break;
                case FISH_4x2:
                    width = lastPlacementHorizontal ? 4 : 2;
                    height = lastPlacementHorizontal ? 2 : 4;
                    break;
                case FISH_5x1:
                    width = lastPlacementHorizontal ? 5 : 1;
                    height = lastPlacementHorizontal ? 1 : 5;
                    break;
            }

            // Update our local board to reflect the placed fish
            int fishId = myBoard.getNextFishId();
            for (int r = lastPlacementRow; r < lastPlacementRow + height; r++) {
                for (int c = lastPlacementCol; c < lastPlacementCol + width; c++) {
                    Entity entity = myBoard.getEntity(r, c);
                    entity.setType(selectedType);
                    entity.setFishId(fishId);
                }
            }
            myBoard.incrementTotalFish();

            updateFishCount(selectedType, true);
            refreshBoard(true);
            statusText.setText("Fish placed successfully!");
        } else if (message.equals("INVALID_PLACEMENT")) {
            // Fish placement failed
            statusText.setText("Invalid fish placement! Try again.");
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

                    // Updated: Set a temporary fishId for tracking connected cells
                    int tempFishId = 100 + row * Board.SIZE + col; // Generate unique ID
                    entity.setType(Entity.Type.FISH_2x1); // Default, since we don't know actual type
                    entity.setFishId(tempFishId); // Set temporary fish ID
                }

                if (result.equals("SINK")) {
                    entity.setSunk(true);
                    // Now when we mark adjacent cells, we'll use this tempFishId
                    markAdjacentCellsAsSunk(row, col, false);

                    // Additional alert for sinking a fish
                    showAlert("Fish Sunk!", "You sunk an opponent's fish!");

                    // Decrement opponent's fish count
                    opponentRemainingFish--;
                    remainingFishText.setText("Opponent fish remaining: " + opponentRemainingFish);
                }

                updateCell(false, row, col, entity);
            } else {
                // Opponent attack on my board
                Entity entity = myBoard.getEntity(row, col);
                entity.hit();

                if (result.equals("SINK")) {
                    markAllCellsOfFishAsSunk(entity.getFishId());

                    // Notify the player that their fish was sunk
                    showAlert("Fish Lost!", "Your opponent sunk one of your fish!");
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
            alert.setContentText("Congratulations! You sunk all opponent's fish and won the game!");
        } else {
            alert.setContentText("Game over! Your opponent sunk all your fish.");
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
     * Mark adjacent cells as part of a sunk fish
     * 
     * @param row       Starting row
     * @param col       Starting column
     * @param isMyBoard true if player's board, false for opponent's board
     */
    private void markAdjacentCellsAsSunk(int row, int col, boolean isMyBoard) {
        Board board = isMyBoard ? myBoard : opponentBoard;
        Entity entity = board.getEntity(row, col);
        int fishId = entity.getFishId();

        // Only continue if this is a valid fish cell
        if (!entity.isFish() || fishId <= 0)
            return;

        // Check all directions for hit cells and mark them as sunk
        int[][] directions = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };

        for (int[] dir : directions) {
            int newRow = row + dir[0];
            int newCol = col + dir[1];

            if (board.isValidCoordinate(newRow, newCol)) {
                Entity adjacent = board.getEntity(newRow, newCol);
                // Only mark as sunk if it's the same fish (same fishId)
                if (adjacent.isHit() && adjacent.isFish() && !adjacent.isSunk() &&
                // For opponent board, we don't know fishId so we just check if it's hit
                        (isMyBoard ? adjacent.getFishId() == fishId : true)) {

                    adjacent.setSunk(true);
                    updateCell(isMyBoard, newRow, newCol, adjacent);
                    // Recursively mark connected cells
                    markAdjacentCellsAsSunk(newRow, newCol, isMyBoard);
                }
            }
        }
    }

    /**
     * Mark all cells of a fish as sunk
     * 
     * @param fishId The ID of the fish to mark as sunk
     */
    private void markAllCellsOfFishAsSunk(int fishId) {
        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                Entity entity = myBoard.getEntity(row, col);
                if (entity.getFishId() == fishId) {
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