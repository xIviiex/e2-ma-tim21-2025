package com.team21.questify.utils;

public class LevelCalculator {
    public static int getRequiredXpForNextLevel(int currentLevel) {
        if (currentLevel < 1) {
            return 200;
        }

        int previousLevelXp;
        if (currentLevel == 1) {
            previousLevelXp = 200;
        } else {
            previousLevelXp = getRequiredXpForNextLevel(currentLevel - 1);
        }

        double newXp = (previousLevelXp * 2) + (previousLevelXp / 2.0);
        return (int) (Math.ceil(newXp / 100) * 100);
    }

    public static int getPowerPointsForLevel(int currentLevel) {
        if (currentLevel < 2) {
            return 0;
        }
        if (currentLevel == 2) {
            return 40;
        }

        int previousLevelPp = getPowerPointsForLevel(currentLevel - 1);
        double newPp = previousLevelPp + (0.75 * previousLevelPp);
        return (int) Math.round(newPp);
    }

    public static String getTitleForLevel(int currentLevel) {
        switch (currentLevel) {
            case 1:
                return "Adventurer";
            case 2:
                return "Journeyman";
            case 3:
                return "Hero";
            default:
                return "Master";
        }
    }
}
