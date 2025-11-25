package zombie_game;

public class RoundManager {
    private int currentRound = 1;
    private int nextRoundScore = 5;
    private int zombieSpeed = 10;

    // 게임 시작 시 초기화
    public void reset() {
        currentRound = 1;
        nextRoundScore = 5;
        zombieSpeed = 10;
    }

    /**
     * 점수를 확인하여 라운드 업 여부를 반환
     * @param currentScore 현재 점수
     * @return 라운드가 올랐으면 true
     */
    public boolean checkLevelUp(int currentScore) {
        if (currentScore >= nextRoundScore) {
            currentRound++;
            nextRoundScore += 5; // 라운드당 필요 점수 증가량
            zombieSpeed += 2;    // 좀비 속도 증가
            return true;
        }
        return false;
    }

    public int getRound() {
        return currentRound;
    }

    public int getZombieSpeed() {
        return zombieSpeed;
    }
}