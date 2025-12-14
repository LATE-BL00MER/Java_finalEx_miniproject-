package zombie_game;

public class RoundManager {

    private int round = 1;

    public int getRound() {
        return round;
    }

    /** 라운드별 좀비 이동 속도(기본 단위) */
    public int getZombieSpeed() {
        // ★ 테스트용: 모든 라운드가 1라운드 속도와 동일
        return 1;
    }

    /**
     * 점수에 따라 라운드를 올릴지 여부 체크
     *  - round 1 : 점수 5 이상 → round 2
     *  - round 2 : 점수 10 이상 → round 3
     *  - round 3 : 점수 15 이상 → (더 이상 라운드 없음, false 반환)
     *
     * @param score 현재 점수
     * @return 라운드가 실제로 1 올라갔으면 true, 아니면 false
     */
    public boolean checkLevelUp(int score) {
        int target;

        switch (round) {
            case 1:
                target = 5;   // 1 -> 2
                break;
            case 2:
                target = 15;  // 2 -> 3 (누적 15점)
                break;
            case 3:
                return false; // 3라운드는 무한 유지(레벨업 없음)
            default:
                return false;
        }

        if (score >= target) {
            round++;
            return true;
        }
        return false;
    }


    /** 새 게임 시작 시 라운드 초기화 */
    public void reset() {
        round = 1;
    }
}
