/**
 * Represents an entity on the game board (empty space or block part).
 */
public class Entity {
    /**
     * Possible entity types on the board
     */
    public enum Type {
        EMPTY,
        BLOCK_2x1, // Was FISH_2x1
        BLOCK_3x1, // Was FISH_3x1
        BLOCK_4x2, // Was FISH_4x2
        BLOCK_5x1 // Was FISH_5x1
    }

    private Type type; // Type of entity (empty or block type)
    private boolean isHit; // Whether this cell has been hit
    private int blockId; // ID of the block this entity belongs to (was fishId)
    private boolean isSunk; // Whether the block is completely sunk

    /**
     * Create a new entity (initially empty)
     */
    public Entity() {
        this.type = Type.EMPTY;
        this.isHit = false;
        this.blockId = -1; // Was fishId
        this.isSunk = false;
    }

    /**
     * Set the type of this entity
     * 
     * @param type The type to set
     */
    public void setType(Type type) {
        this.type = type;
    }

    /**
     * Get the type of this entity
     * 
     * @return The entity type
     */
    public Type getType() {
        return type;
    }

    /**
     * Check if this entity is hit
     * 
     * @return true if hit, false otherwise
     */
    public boolean isHit() {
        return isHit;
    }

    /**
     * Mark this entity as hit
     * 
     * @return true if a block was hit, false if empty cell
     */
    public boolean hit() {
        this.isHit = true;
        return type != Type.EMPTY;
    }

    /**
     * Check if this entity is part of a block
     * 
     * @return true if entity is a block part, false otherwise
     */
    public boolean isBlock() { // Was isFish
        return type != Type.EMPTY;
    }

    /**
     * Set whether this block entity is sunk
     * 
     * @param sunk true if the block is completely sunk
     */
    public void setSunk(boolean sunk) {
        this.isSunk = sunk;
    }

    /**
     * Check if this entity is part of a sunk block
     * 
     * @return true if entity is part of a completely sunk block
     */
    public boolean isSunk() {
        return isSunk;
    }

    /**
     * Set the block ID for this entity
     * 
     * @param id The block ID this entity belongs to
     */
    public void setBlockId(int id) { // Was setFishId
        this.blockId = id;
    }

    /**
     * Get the block ID this entity belongs to
     * 
     * @return The block ID
     */
    public int getBlockId() { // Was getFishId
        return blockId;
    }

    /**
     * Reset this entity to empty state
     */
    public void reset() {
        this.type = Type.EMPTY;
        this.isHit = false;
        this.blockId = -1; // Was fishId
        this.isSunk = false;
    }
}