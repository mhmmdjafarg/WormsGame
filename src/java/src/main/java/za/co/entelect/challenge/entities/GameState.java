package za.co.entelect.challenge.entities;

import com.google.gson.annotations.SerializedName;

public class GameState {

    @SerializedName("currentRound")
    public int currentRound;

    @SerializedName("maxRounds")
    public int maxRounds;

    @SerializedName('pushbackDamage')
    public int pushbackDamage;

    @SerializedName('lavaDamage')
    public int lavaDamage;

    @SerializedName("mapSize")
    public int mapSize;

    @SerializedName("currentWormId")
    public int currentWormId;

    @SerializedName("consecutiveDoNothingCount")
    public int consecutiveDoNothingCount;

    @SerializedName("myPlayer")
    public MyPlayer myPlayer;

    @SerializedName("opponents")
    public Opponent[] opponents;

    @SerializedName("map")
    public Cell[][] map;
}
