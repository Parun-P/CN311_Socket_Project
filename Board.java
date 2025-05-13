import java.util.ArrayList;
import java.util.List;

/**
 * Represents a player's game board.
 */
public class Board {
    public static final int SIZE = 10; // Board size (10x10)

    private Entity[][] grid; // 2D grid of entities
    private List<int[][]> fishCoordinates; // List of coordinates for each fish
    private int nextFishId; // Next available fish ID
    private int totalFish; // Total number of fish on the board
    private int sunkFish; // Number of sunk fish

    /**
     * Create a new empty board
     */
    public Board() {
        grid = new Entity[SIZE][SIZE];
        fishCoordinates = new ArrayList<>();
        nextFishId = 0;
        totalFish = 0;
        sunkFish = 0;

        // Initialize all cells as empty entities
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                grid[row][col] = new Entity();
            }
        }
    }

    /**
     * Get the entity at the specified coordinates
     * 
     * @param row Row index
     * @param col Column index
     * @return The entity at the specified position
     */
    public Entity getEntity(int row, int col) {
        if (isValidCoordinate(row, col)) {
            return grid[row][col];
        }
        return null;
    }

    /**
     * Check if the coordinates are valid
     * 
     * @param row Row index
     * @param col Column index
     * @return true if coordinates are within bounds
     */
    public boolean isValidCoordinate(int row, int col) {
        return row >= 0 && row < SIZE && col >= 0 && col < SIZE;
    }

    /**
     * Try to place a fish on the board
     * 
     * @param row        Starting row
     * @param col        Starting column
     * @param type       Type of fish
     * @param horizontal true if horizontal orientation, false if vertical
     * @return true if fish was placed successfully
     */
    public boolean placeFish(int row, int col, Entity.Type type, boolean horizontal) {
        int width, height;

        // Determine dimensions based on fish type
        switch (type) {
            case FISH_2x1:
                width = horizontal ? 2 : 1;
                height = horizontal ? 1 : 2;
                break;
            case FISH_3x1:
                width = horizontal ? 3 : 1;
                height = horizontal ? 1 : 3;
                break;
            case FISH_4x2:
                width = horizontal ? 4 : 2;
                height = horizontal ? 2 : 4;
                break;
            case FISH_5x1:
                width = horizontal ? 5 : 1;
                height = horizontal ? 1 : 5;
                break;
            default:
                return false;
        }

        // Check if fish is within bounds
        if (row + height > SIZE || col + width > SIZE) {
            return false;
        }

        // Check if position is occupied
        for (int r = row; r < row + height; r++) {
            for (int c = col; c < col + width; c++) {
                if (grid[r][c].isFish()) {
                    return false;
                }
            }
        }

        // Place the fish
        int fishId = nextFishId++;
        int[][] coordinates = new int[width * height][2];
        int index = 0;

        for (int r = row; r < row + height; r++) {
            for (int c = col; c < col + width; c++) {
                grid[r][c].setType(type);
                grid[r][c].setFishId(fishId);
                coordinates[index][0] = r;
                coordinates[index][1] = c;
                index++;
            }
        }

        fishCoordinates.add(coordinates);
        totalFish++;
        return true;
    }

    /**
     * Apply an attack to the board
     * 
     * @param row Row to attack
     * @param col Column to attack
     * @return 0 if miss, 1 if hit, 2 if hit & sink
     */
    public int applyAttack(int row, int col) {
        if (!isValidCoordinate(row, col)) {
            return 0; // Miss (invalid coordinate)
        }

        Entity targetEntity = grid[row][col];

        if (targetEntity.isHit()) {
            return 0; // Already hit
        }

        boolean isHit = targetEntity.hit();

        if (!isHit) {
            return 0; // Miss
        }

        // Check if the fish is now sunk
        int fishId = targetEntity.getFishId();
        boolean allHit = true;

        // Check if all parts of the fish are hit
        for (int[] coord : fishCoordinates.get(fishId)) {
            if (!grid[coord[0]][coord[1]].isHit()) {
                allHit = false;
                break;
            }
        }

        if (allHit) {
            // Mark all parts of the fish as sunk
            for (int[] coord : fishCoordinates.get(fishId)) {
                grid[coord[0]][coord[1]].setSunk(true);
            }
            sunkFish++;
            return 2; // Hit and sink
        }

        return 1; // Hit only
    }

    /**
     * Check if all fish on this board are sunk
     * 
     * @return true if all fish are sunk
     */
    public boolean allFishSunk() {
        return sunkFish >= totalFish && totalFish > 0;
    }

    /**
     * Get the number of remaining fish
     * 
     * @return Number of fish that are not sunk
     */
    public int getRemainingFish() {
        return totalFish - sunkFish;
    }

    /**
     * Reset the board to empty state
     */
    public void reset() {
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                grid[row][col].reset();
            }
        }
        fishCoordinates.clear();
        nextFishId = 0;
        totalFish = 0;
        sunkFish = 0;
    }

    /**
     * Get the next available fish ID
     * 
     * @return The next fish ID
     */
    public int getNextFishId() {
        return nextFishId;
    }

    /**
     * Increment the total number of fish on the board
     */
    public void incrementTotalFish() {
        totalFish++;
    }

    public int getTotalFish() {
        return totalFish;
    }

    // 2. Set the total number of fish on the board
    public void setTotalFish(int count) {
        totalFish = count;
    }

    // 3. Get the number of sunk fish
    public int getSunkFish() {
        return sunkFish;
    }

    // 4. Increment the number of sunk fish
    public void incrementSunkFish() {
        sunkFish++;
    }
}