package com.team21.questify.utils;

import android.util.Log;

public class LevelCalculator {
    public static int getRequiredXpForNextLevel(int currentLevel) {
        if (currentLevel < 2) {
            return 200;
        }

        int previousLevelXp;
        if (currentLevel == 2) {
            previousLevelXp = 200;
        } else {
            previousLevelXp = getRequiredXpForNextLevel(currentLevel - 1);
        }

        double newXp = (previousLevelXp * 2) + (previousLevelXp / 2.0);
        return (int) (Math.ceil(newXp / 100) * 100);
    }

    public static int getPowerPointsForLevel(int nextLevel) {
        if (nextLevel < 2) {
            return 0;
        }
        if (nextLevel == 2) {
            return 40;
        }

        int previousLevelPp = getPowerPointsForLevel(nextLevel - 1);
        double newPp = previousLevelPp + (0.75 * previousLevelPp);
        return (int) Math.round(newPp);
    }

    public static String getTitleForLevel(int nextLevel) {
        switch (nextLevel) {
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

    public static int getXpForDifficultyOrPriority(int currentLevel, int baseDiffPriorXp) {
        double xp = baseDiffPriorXp;
        for (int i = 0; i < currentLevel - 1; i++) {
            xp = xp + (xp / 2.0);
        }
        return (int) Math.round(xp);
    }

    public static int getCoinsForLevel(int level) {
        if (level == 0) {
            return 150;
        }
        switch (level) {
            case 1:
                return 200;
            case 2:
                return 240;
            case 3:
                return 288;
            default:
                return 0;
        }
    }
}
