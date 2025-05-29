import java.util.ArrayList;
import java.util.List;

public class Board {
    public static final int SIZE = 10;

    private Entity[][] grid;
    private List<int[][]> blockCoordinates; // เก็บตำแหน่ง block ที่อยู่ใน board
    private int nextBlockId;
    private int totalBlocks;
    private int sunkBlocks;

    // สร้าง board เปล่า
    public Board() {
        grid = new Entity[SIZE][SIZE];
        blockCoordinates = new ArrayList<>();
        nextBlockId = 0;
        totalBlocks = 0;
        sunkBlocks = 0;

        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                grid[row][col] = new Entity();
            }
        }
    }

    public boolean isValidCoordinate(int row, int col) {
        return row >= 0 && row < SIZE && col >= 0 && col < SIZE;
    }

    public Entity getEntity(int row, int col) {
        if (isValidCoordinate(row, col)) {
            return grid[row][col];
        }
        return null;
    }

    public boolean placeBlock(int row, int col, Entity.Type type, boolean horizontal) {
        int width, height;

        switch (type) {
            case BLOCK_2x1:
                width = horizontal ? 2 : 1;
                height = horizontal ? 1 : 2;
                break;
            case BLOCK_3x1:
                width = horizontal ? 3 : 1;
                height = horizontal ? 1 : 3;
                break;
            case BLOCK_4x2:
                width = horizontal ? 4 : 2;
                height = horizontal ? 2 : 4;
                break;
            case BLOCK_5x1:
                width = horizontal ? 5 : 1;
                height = horizontal ? 1 : 5;
                break;
            default:
                return false;
        }

        // check ว่า block เลย board ไหม
        if (row + height > SIZE || col + width > SIZE) {
            return false;
        }

        // check ว่าถ้ามี block อื่นจะวางไม่ได้
        for (int r = row; r < row + height; r++) {
            for (int c = col; c < col + width; c++) {
                if (grid[r][c].isBlock()) {
                    return false;
                }
            }
        }

        int blockId = nextBlockId++;
        int[][] coordinates = new int[width * height][2];
        int index = 0;

        for (int r = row; r < row + height; r++) {
            for (int c = col; c < col + width; c++) {
                grid[r][c].setType(type);
                grid[r][c].setBlockId(blockId);
                coordinates[index][0] = r;
                coordinates[index][1] = c;
                index++;
            }
        }

        blockCoordinates.add(coordinates);
        totalBlocks++;
        return true;
    }

    // โจมตี โดย return 0 : Miss ,1 : Hit ,2 : Sink และ Hit
    public int applyAttack(int row, int col) {
        if (!isValidCoordinate(row, col)) {
            return 0;
        }

        Entity targetEntity = grid[row][col];

        if (targetEntity.isHit()) {
            return 0; // Already hit
        }

        boolean isHit = targetEntity.hit();

        if (!isHit) {
            return 0; // Miss
        }

        int blockId = targetEntity.getBlockId();
        boolean allHit = true;

        for (int[] coord : blockCoordinates.get(blockId)) {
            if (!grid[coord[0]][coord[1]].isHit()) {
                allHit = false;
                break;
            }
        }

        // ถ้า block sunk
        if (allHit) {
            for (int[] coord : blockCoordinates.get(blockId)) {
                grid[coord[0]][coord[1]].setSunk(true);
            }
            sunkBlocks++;
            return 2;
        }

        return 1;
    }

    public boolean allBlocksSunk() {
        return sunkBlocks >= totalBlocks && totalBlocks > 0;
    }

    public int getNextBlockId() {
        return nextBlockId;
    }

    public void incrementTotalBlocks() {
        totalBlocks++;
    }
}