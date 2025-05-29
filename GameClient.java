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

public class GameClient extends Application {
    // GUI
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

    private int lastPlacementRow;
    private int lastPlacementCol;
    private Entity.Type lastPlacementType;
    private boolean lastPlacementHorizontal;

    private int opponentRemainingBlocks = 6;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private static final String HOST = "localhost";
    private static final int PORT = 8080;

    private static final int CELL_SIZE = 25;

    @Override
    public void start(Stage primaryStage) {
        myBoard = new Board();
        opponentBoard = new Board();
        placementPhase = true;
        gameStarted = false;
        myTurn = false;

        blockCounts = new HashMap<>();
        blockCounts.put(Entity.Type.BLOCK_2x1, 2);
        blockCounts.put(Entity.Type.BLOCK_3x1, 2);
        blockCounts.put(Entity.Type.BLOCK_4x2, 1);
        blockCounts.put(Entity.Type.BLOCK_5x1, 1);

        connectToServer();

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(20));

        // section บน - Status
        VBox topSection = new VBox(10);
        topSection.setAlignment(Pos.CENTER);

        statusText = new Text("Place your blocks on the left board");
        statusText.setFont(Font.font(18));

        remainingBlocksText = new Text("Opponent blocks remaining: " + opponentRemainingBlocks);
        remainingBlocksText.setFont(Font.font(14));

        topSection.getChildren().addAll(statusText, remainingBlocksText);
        root.setTop(topSection);

        // section กลาง - Game boards
        HBox boardsContainer = new HBox(50);
        boardsContainer.setAlignment(Pos.CENTER);

        VBox myBoardContainer = new VBox(10);
        myBoardContainer.setAlignment(Pos.CENTER);

        Text myBoardLabel = new Text("My Board");
        myBoardLabel.setFont(Font.font(16));

        myBoardGrid = createBoard(true);

        myBoardContainer.getChildren().addAll(myBoardLabel, myBoardGrid);

        VBox opponentBoardContainer = new VBox(10);
        opponentBoardContainer.setAlignment(Pos.CENTER);

        Text opponentBoardLabel = new Text("Opponent's Board");
        opponentBoardLabel.setFont(Font.font(16));

        opponentBoardGrid = createBoard(false);

        opponentBoardContainer.getChildren().addAll(opponentBoardLabel, opponentBoardGrid);

        boardsContainer.getChildren().addAll(myBoardContainer, opponentBoardContainer);
        root.setCenter(boardsContainer);

        // section ล่าง
        VBox controlsContainer = new VBox(15);
        controlsContainer.setAlignment(Pos.CENTER);
        controlsContainer.setPadding(new Insets(20, 0, 0, 0));

        // ตัวเลือกชนิด block
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

        // ตัวเลือกทิศทาง block
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

        // ปุ่ม Ready
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

        // Create scene
        Scene scene = new Scene(root);
        primaryStage.setTitle("Block Battle - Connecting...");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);

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

        // เริ่มทำงานของ thread
        new Thread(this::receiveMessages).start();
    }

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
                    cell.setOnMouseClicked(e -> {
                        if (placementPhase) {
                            placeBlock(r, c);
                        }
                    });
                } else {
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

    private void placeBlock(int row, int col) {
        Entity.Type selectedType = (Entity.Type) blockTypeGroup.getSelectedToggle().getUserData();
        Boolean horizontal = (Boolean) orientationGroup.getSelectedToggle().getUserData();

        if (blockCounts.get(selectedType) <= 0) {
            statusText.setText("No more blocks of this type available");
            return;
        }

        lastPlacementRow = row;
        lastPlacementCol = col;
        lastPlacementType = selectedType;
        lastPlacementHorizontal = horizontal;

        // ส่ง PLACE_BLOCK... ไปให้ server
        out.println("PLACE_BLOCK " + row + " " + col + " " + selectedType + " " + horizontal);
    }

    // Attack opponent's board
    private void attackCell(int row, int col) {
        if (opponentBoard.getEntity(row, col).isHit()) {
            statusText.setText("You already attacked this position!");
            return;
        }

        out.println("ATTACK " + row + " " + col);
        myTurn = false;
    }

    private void updateCell(boolean isMyBoard, int row, int col, Entity entity) {
        GridPane grid = isMyBoard ? myBoardGrid : opponentBoardGrid;
        Rectangle cell = (Rectangle) grid.getChildren().get(row * Board.SIZE + col);

        if (isMyBoard) {
            if (entity.isBlock()) {
                if (entity.isHit()) {
                    cell.setFill(Color.DARKRED); // Hit
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
            // Opponent board
            if (entity.isHit()) {
                if (entity.isBlock()) {
                    cell.setFill(Color.RED); // Hit
                } else {
                    cell.setFill(Color.DARKGRAY); // Miss
                }
            } else {
                cell.setFill(Color.LIGHTBLUE); // Empty
            }
        }
    }

    // เชื่อมต่อกับ server
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

    private void disablePlacementControls() {
        readyButton.setDisable(true);
        for (Toggle toggle : blockTypeGroup.getToggles()) {
            ((RadioButton) toggle).setDisable(true);
        }
        for (Toggle toggle : orientationGroup.getToggles()) {
            ((RadioButton) toggle).setDisable(true);
        }
    }

    private void updateBlockCount(Entity.Type type, boolean decrease) {
        if (decrease) {
            blockCounts.put(type, blockCounts.get(type) - 1);
        } else {
            blockCounts.put(type, blockCounts.get(type) + 1);
        }

        // Update radio button of select block
        for (Toggle toggle : blockTypeGroup.getToggles()) {
            RadioButton rb = (RadioButton) toggle;
            Entity.Type buttonType = (Entity.Type) rb.getUserData();
            if (buttonType == type) {
                rb.setText(getTypeName(type) + " (" + blockCounts.get(type) + ")");
                break;
            }
        }

        int totalRemaining = 0;
        for (Integer count : blockCounts.values()) {
            totalRemaining += count;
        }
        readyButton.setDisable(totalRemaining > 0); // ถ้าวางครบแล้ว จะ enable ปุ่ม ready
    }

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

    // รับ message จาก server
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

    // Process message จาก server
    private void processServerMessage(String message) {
        System.out.println("Server: " + message);

        if (message.startsWith("PLAYER")) {
            playerId = Integer.parseInt(message.split(" ")[1]);
            System.out.println("Assigned player ID: " + playerId);

            Platform.runLater(() -> {
                Stage stage = (Stage) myBoardGrid.getScene().getWindow();
                stage.setTitle("Block Battle - Player " + playerId);
            });
        } else if (message.equals("BLOCK_PLACED")) {
            Entity.Type selectedType = lastPlacementType;

            int width = 0;
            int height = 0;

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

            // Update type และ block id ต่างๆ ของ cell ที่วาง
            int blockId = myBoard.getNextBlockId();
            for (int r = lastPlacementRow; r < lastPlacementRow + height; r++) {
                for (int c = lastPlacementCol; c < lastPlacementCol + width; c++) {
                    Entity entity = myBoard.getEntity(r, c);
                    entity.setType(selectedType);
                    entity.setBlockId(blockId);
                }
            }
            myBoard.incrementTotalBlocks();

            updateBlockCount(selectedType, true); // update block count on radio
            refreshBoard(true); // update board UI
            statusText.setText("Block placed successfully!");
        } else if (message.equals("INVALID_PLACEMENT")) {
            statusText.setText("Invalid block placement! Try again.");
        } else if (message.equals("GAME_START")) {
            placementPhase = false;
            gameStarted = true;
            statusText.setText("Game started!");
        } else if (message.startsWith("TURN")) {
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
                Entity entity = opponentBoard.getEntity(row, col);
                entity.hit();

                if (result.equals("HIT") || result.equals("SINK")) {
                    // Set a blockId ชั่วคราวสำหรับการ track cell ที่ connect กัน
                    int tempBlockId = 100 + row * Board.SIZE + col;
                    entity.setType(Entity.Type.BLOCK_2x1);
                    entity.setBlockId(tempBlockId);
                }

                if (result.equals("SINK")) {
                    entity.setSunk(true);

                    showAlert("Block Sunk!", "You sunk an opponent's block!");

                    opponentRemainingBlocks--;
                    remainingBlocksText.setText("Opponent blocks remaining: " + opponentRemainingBlocks);
                }

                updateCell(false, row, col, entity);
            } else {
                // Opponent attack on my board
                Entity entity = myBoard.getEntity(row, col);
                entity.hit();

                if (result.equals("SINK")) {

                    showAlert("Block Lost!", "Your opponent sunk one of your blocks!");
                }

                updateCell(true, row, col, entity);
            }
        } else if (message.startsWith("GAME_OVER")) {
            // Game over notification
            int winnerId = Integer.parseInt(message.split(" ")[1]);
            boolean isWinner = (winnerId == playerId);

            showGameOverDialog(isWinner);

            statusText.setText(isWinner ? "You win!" : "You lose!");

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

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void refreshBoard(boolean isMyBoard) {
        Board board = isMyBoard ? myBoard : opponentBoard;

        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                Entity entity = board.getEntity(row, col);
                updateCell(isMyBoard, row, col, entity);
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}