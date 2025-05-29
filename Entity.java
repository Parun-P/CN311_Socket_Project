public class Entity {
    public enum Type {
        EMPTY,
        BLOCK_2x1,
        BLOCK_3x1,
        BLOCK_4x2,
        BLOCK_5x1
    }

    private Type type;
    private boolean isHit;
    private int blockId;
    private boolean isSunk;

    // Create Initial Entity
    public Entity() {
        this.type = Type.EMPTY;
        this.isHit = false;
        this.blockId = -1;
        this.isSunk = false;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public boolean isHit() {
        return isHit;
    }

    // Entity was hit
    public boolean hit() {
        this.isHit = true;
        return type != Type.EMPTY; // return true if hit false miss
    }

    // Check if Entity is a part of block
    public boolean isBlock() {
        return type != Type.EMPTY;
    }

    public void setSunk(boolean sunk) {
        this.isSunk = sunk;
    }

    public boolean isSunk() {
        return isSunk;
    }

    public void setBlockId(int id) {
        this.blockId = id;
    }

    public int getBlockId() {
        return blockId;
    }

}