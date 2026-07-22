package net.minecraft.scoreboard;

import java.util.Collection;
import java.util.Collections;

public class Scoreboard {
    public ScoreObjective getObjectiveInDisplaySlot(int slot) {
        return null;
    }

    public Collection<Score> getSortedScores(ScoreObjective objective) {
        return Collections.emptyList();
    }

    public ScorePlayerTeam getPlayersTeam(String playerName) {
        return null;
    }
}
