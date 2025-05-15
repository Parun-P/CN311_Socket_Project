import java.util.ArrayList;
import java.util.List;

/**
 * Represents a player's game board.
 */
public class Board {
    public static final int SIZE = 10; // Board size (10x10)

    private Entity[][] grid; // 2D grid of entities
    private List<int[][]> blockCoordinates; // List of coordinates for each block (was fishCoordinates)
    private int nextBlockId; // Next available block ID (was nextFishId)
    private int totalBlocks; // Total number of blocks on the board (was totalFish)
    private int sunkBlocks; // Number of sunk blocks (was sunkFish)

    /**
     * Create a new empty board
     */
    public Board() {
        grid = new Entity[SIZE][SIZE];
        blockCoordinates = new ArrayList<>();
        nextBlockId = 0;
        totalBlocks = 0;
        sunkBlocks = 0;

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
     * Try to place a block on the board
     * 
     * @param row        Starting row
     * @param col        Starting column
     * @param type       Type of block
     * @param horizontal true if horizontal orientation, false if vertical
     * @return true if block was placed successfully
     */
    public boolean placeBlock(int row, int col, Entity.Type type, boolean horizontal) { // Was placeFish
        int width, height;

        // Determine dimensions based on block type
        switch (type) {
            case BLOCK_2x1: // Was FISH_2x1
                width = horizontal ? 2 : 1;
                height = horizontal ? 1 : 2;
                break;
            case BLOCK_3x1: // Was FISH_3x1
                width = horizontal ? 3 : 1;
                height = horizontal ? 1 : 3;
                break;
            case BLOCK_4x2: // Was FISH_4x2
                width = horizontal ? 4 : 2;
                height = horizontal ? 2 : 4;
                break;
            case BLOCK_5x1: // Was FISH_5x1
                width = horizontal ? 5 : 1;
                height = horizontal ? 1 : 5;
                break;
            default:
                return false;
        }

        // Check if block is within bounds
        if (row + height > SIZE || col + width > SIZE) {
            return false;
        }

        // Check if position is occupied
        for (int r = row; r < row + height; r++) {
            for (int c = col; c < col + width; c++) {
                if (grid[r][c].isBlock()) { // Was isFish
                    return false;
                }
            }
        }

        // Place the block
        int blockId = nextBlockId++; // Was fishId
        int[][] coordinates = new int[width * height][2];
        int index = 0;

        for (int r = row; r < row + height; r++) {
            for (int c = col; c < col + width; c++) {
                grid[r][c].setType(type);
                grid[r][c].setBlockId(blockId); // Was setFishId
                coordinates[index][0] = r;
                coordinates[index][1] = c;
                index++;
            }
        }

        blockCoordinates.add(coordinates);
        totalBlocks++;
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

        // Check if the block is now sunk
        int blockId = targetEntity.getBlockId(); // Was getFishId
        boolean allHit = true;

        // Check if all parts of the block are hit
        for (int[] coord : blockCoordinates.get(blockId)) {
            if (!grid[coord[0]][coord[1]].isHit()) {
                allHit = false;
                break;
            }
        }

        if (allHit) {
            // Mark all parts of the block as sunk
            for (int[] coord : blockCoordinates.get(blockId)) {
                grid[coord[0]][coord[1]].setSunk(true);
            }
            sunkBlocks++; // Was sunkFish
            return 2; // Hit and sink
        }

        return 1; // Hit only
    }

    /**
     * Check if all blocks on this board are sunk
     * 
     * @return true if all blocks are sunk
     */
    public boolean allBlocksSunk() { // Was allFishSunk
        return sunkBlocks >= totalBlocks && totalBlocks > 0;
    }

    /**
     * Get the number of remaining blocks
     * 
     * @return Number of blocks that are not sunk
     */
    public int getRemainingBlocks() { // Was getRemainingFish
        return totalBlocks - sunkBlocks;
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
        blockCoordinates.clear();
        nextBlockId = 0;
        totalBlocks = 0;
        sunkBlocks = 0;
    }

    /**
     * Get the next available block ID
     * 
     * @return The next block ID
     */
    public int getNextBlockId() { // Was getNextFishId
        return nextBlockId;
    }

    /**
     * Increment the total number of blocks on the board
     */
    public void incrementTotalBlocks() { // Was incrementTotalFish
        totalBlocks++;
    }

    public int getTotalBlocks() { // Was getTotalFish
        return totalBlocks;
    }

    /**
     * Set the total number of blocks on the board
     */
    public void setTotalBlocks(int count) { // Was setTotalFish
        totalBlocks = count;
    }

    /**
     * Get the number of sunk blocks
     */
    public int getSunkBlocks() { // Was getSunkFish
        return sunkBlocks;
    }

    /**
     * Increment the number of sunk blocks
     */
    public void incrementSunkBlocks() { // Was incrementSunkFish
        sunkBlocks++;
    }
}