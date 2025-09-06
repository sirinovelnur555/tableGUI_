package rankTable;

import java.io.Serializable;

public class Team implements Serializable {
    private String name;
    private int points;
    private int collateral; // Girov

    public Team(String name, int points) {
        this.name = name;
        this.points = points;
        this.collateral = 0; // default
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }
    public void addPoints(int pointsToAdd) { this.points += pointsToAdd; }

    public int getCollateral() { return collateral; }
    public void setCollateral(int collateral) { this.collateral = collateral; }

    @Override
    public String toString() {
        return name + " - " + points + " - " + collateral;
    }
}
