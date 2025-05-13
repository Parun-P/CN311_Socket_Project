/**
 * Represents an entity on the game board (empty water or fish part).
 */
public class Entity {
    /**
     * Possible entity types on the board
     */
    public enum Type {
        EMPTY,
        FISH_2x1,
        FISH_3x1,
        FISH_4x2,
        FISH_5x1
    }

    private Type type; // Type of entity (empty or fish type)
    private boolean isHit; // Whether this cell has been hit
    private int fishId; // ID of the fish this entity belongs to
    private boolean isSunk; // Whether the fish is completely sunk

    /**
     * Create a new entity (initially empty)
     */
    public Entity() {
        this.type = Type.EMPTY;
        this.isHit = false;
        this.fishId = -1;
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
     * @return true if a fish was hit, false if empty cell
     */
    public boolean hit() {
        this.isHit = true;
        return type != Type.EMPTY;
    }

    /**
     * Check if this entity is part of a fish
     * 
     * @return true if entity is a fish part, false otherwise
     */
    public boolean isFish() {
        return type != Type.EMPTY;
    }

    /**
     * Set whether this fish entity is sunk
     * 
     * @param sunk true if the fish is completely sunk
     */
    public void setSunk(boolean sunk) {
        this.isSunk = sunk;
    }

    /**
     * Check if this entity is part of a sunk fish
     * 
     * @return true if entity is part of a completely sunk fish
     */
    public boolean isSunk() {
        return isSunk;
    }

    /**
     * Set the fish ID for this entity
     * 
     * @param id The fish ID this entity belongs to
     */
    public void setFishId(int id) {
        this.fishId = id;
    }

    /**
     * Get the fish ID this entity belongs to
     * 
     * @return The fish ID
     */
    public int getFishId() {
        return fishId;
    }

    /**
     * Reset this entity to empty state
     */
    public void reset() {
        this.type = Type.EMPTY;
        this.isHit = false;
        this.fishId = -1;
        this.isSunk = false;
    }
}