package zombie_game;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 점수 관리 매니저
 * - ranking.dat 파일에 직렬화해서 저장
 * - 싱글톤 패턴
 */
public class ScoreManager {

    private static final String FILE_NAME = "ranking.dat";

    private static ScoreManager instance;

    // 점수 목록 (내림차순 정렬)
    private final List<ScoreEntry> scores = new ArrayList<>();

    /** 싱글톤 인스턴스 얻기 */
    public static synchronized ScoreManager getInstance() {
        if (instance == null) {
            instance = new ScoreManager();
        }
        return instance;
    }

    /** 생성자 : 파일에서 점수 불러오기 */
    private ScoreManager() {
        loadScores();
    }

    /** 점수 추가 후 자동 정렬 + 저장 */
    public synchronized void addScore(String name, int score) {
        if (name == null || name.trim().isEmpty()) {
            name = "Player";
        }
        scores.add(new ScoreEntry(name.trim(), score));

        // 점수 내림차순 정렬
        Collections.sort(scores, (a, b) -> Integer.compare(b.score, a.score));

        saveScores();
    }

    /** 최고 점수 반환 (신기록 여부 판단용) */
    public synchronized int getHighestScore() {
        if (scores.isEmpty()) return 0;
        return scores.get(0).score; // 이미 내림차순 정렬 상태
    }

    /** 상위 N개 점수만 반환 (필요하면 랭킹 화면에서 사용 가능) */
    public synchronized List<ScoreEntry> getTopScores(int limit) {
        int end = Math.min(limit, scores.size());
        return new ArrayList<>(scores.subList(0, end));
    }

    /** 🔥 랭킹 패널에서 쓰는 전체 점수 리스트 반환 */
    public synchronized List<ScoreEntry> getAllScores() {
        // 외부에서 리스트를 수정 못하도록 복사본을 넘겨줌
        return new ArrayList<>(scores);
    }

    /** ranking.dat에서 점수 불러오기 */
    @SuppressWarnings("unchecked")
    private void loadScores() {
        File f = new File(FILE_NAME);
        if (!f.exists()) {
            return; // 처음 실행이면 파일 없음
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
            Object obj = ois.readObject();
            if (obj instanceof List) {
                List<?> list = (List<?>) obj;
                scores.clear();
                for (Object o : list) {
                    if (o instanceof ScoreEntry) {
                        scores.add((ScoreEntry) o);
                    }
                }
                // 혹시 몰라 다시 한 번 정렬
                Collections.sort(scores, (a, b) -> Integer.compare(b.score, a.score));
            }
        } catch (Exception e) {
            // 디버깅 출력 제거 (실행 흐름 동일)
        }
    }

    /** ranking.dat에 점수 저장하기 */
    private void saveScores() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILE_NAME))) {
            oos.writeObject(scores);
        } catch (IOException e) {
            // 디버깅 출력 제거 (실행 흐름 동일)
        }
    }

    /** 하나의 점수 정보 */
    public static class ScoreEntry implements Serializable {
        private static final long serialVersionUID = 1L;

        public final String name;
        public final int score;

        public ScoreEntry(String name, int score) {
            this.name = name;
            this.score = score;
        }

        @Override
        public String toString() {
            return String.format("%-12s : %d점", name, score);
        }
    }
}
