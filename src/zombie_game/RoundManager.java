package zombie_game;

public class RoundManager {

    private int round = 1;

    public int getRound() {
        return round;
    }

    /** 라운드별 좀비 이동 속도(기본 단위) */
    public int getZombieSpeed() {
        switch (round) {
            case 1:
                return 1;   // 가장 느림
            case 2:
                return 2;
            case 3:
            default:
                return 3;   // 가장 빠름
        }
    }

    /** 새 게임 시작 시 호출 */
    public void reset() {
        round = 1;
    }

    /**
     * 점수에 따라 라운드를 올릴지 여부 판단
     * 1라운드 통과: 10점
     * 2라운드 통과: 20점
     * 3라운드 통과: 30점
     */
    public boolean checkLevelUp(int score) {
        int target;
        switch (round) {
            case 1:
                target = 10;
                break;
            case 2:
                target = 20;
                break;
            case 3:
                target = 30;
                break;
            default:
                return false; // 4라운드 이후는 레벨업 없음
        }

        if (score >= target) {
            round++;
            return true;
        }
        return false;
    }
}
