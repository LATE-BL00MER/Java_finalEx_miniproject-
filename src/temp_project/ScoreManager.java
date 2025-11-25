package temp_project;

import java.io.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 점수(랭킹)를 관리하는 싱글톤 매니저
 * - 게임 종료 시 addScore(...) 호출
 * - ranking.dat 파일로 직렬화해서 저장/로드
 * - 상위 MAX_RANK 개까지만 유지
 */
public class ScoreManager {

    // 랭킹 저장 파일명 (프로젝트 실행 폴더 기준)
    private static final String FILE_NAME = "ranking.dat";
    private static final int MAX_RANK = 10;

    private static ScoreManager instance;

    // 실제 랭킹 데이터
    private final List<ScoreEntry> scores = new ArrayList<>();

    // ---------------- 싱글톤 ----------------

    private ScoreManager() {
        loadScores();
    }

    public static synchronized ScoreManager getInstance() {
        if (instance == null) {
            instance = new ScoreManager();
        }
        return instance;
    }

    // ---------------- 외부에서 쓰는 메서드 ----------------

    /** 점수 추가 후 정렬 + 파일 저장 */
    public synchronized void addScore(String name, int score) {
        if (name == null || name.trim().isEmpty()) {
            name = "NONAME";
        }
        name = name.trim();

        scores.add(new ScoreEntry(name, score));

        // 점수 높은 순으로 정렬
        scores.sort(Comparator.comparingInt((ScoreEntry e) -> e.score).reversed());

        // 상위 MAX_RANK 개만 남기기 (subList 사용하지 않음)
        while (scores.size() > MAX_RANK) {
            scores.remove(scores.size() - 1);
        }

        saveScores();
    }

    /** 랭킹 전체 목록 (복사본 반환) - 새 표준 메서드 */
    public synchronized List<ScoreEntry> getScores() {
        return new ArrayList<>(scores);
    }

    /** 호환용 메서드 (예전 코드에서 쓰던 이름) */
    public synchronized List<ScoreEntry> getTopScores() {
        // 현재 구현에서는 getScores()와 동일하게 동작
        return new ArrayList<>(scores);
    }

    // ---------------- 파일 입출력 ----------------

    /** ranking.dat 에 저장된 랭킹 불러오기 */
    @SuppressWarnings("unchecked")
    private synchronized void loadScores() {
        scores.clear();

        File f = new File(FILE_NAME);
        if (!f.exists()) {
            // 파일이 없으면 그냥 빈 리스트 유지
            return;
        }

        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(f))) {
            Object obj = in.readObject();
            if (obj instanceof List<?>) {
                List<?> list = (List<?>) obj;
                for (Object o : list) {
                    if (o instanceof ScoreEntry) {
                        scores.add((ScoreEntry) o);
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            // 파일이 깨졌거나 형식이 다른 경우: 그냥 무시하고 새로 시작
            e.printStackTrace();
        }

        // 혹시라도 정렬이 안 되어 있으면 다시 정렬
        scores.sort(Comparator.comparingInt((ScoreEntry e) -> e.score).reversed());

        // MAX_RANK 개수 제한
        while (scores.size() > MAX_RANK) {
            scores.remove(scores.size() - 1);
        }
    }

    /** 현재 scores 리스트를 ranking.dat 에 저장 */
    private synchronized void saveScores() {
        File f = new File(FILE_NAME);
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(f))) {
            // ★ 꼭 새 ArrayList 로 감싸서 순수 ArrayList만 직렬화
            out.writeObject(new ArrayList<>(scores));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ---------------- ScoreEntry 내부 클래스 ----------------

    /**
     * 한 사람의 점수 정보
     */
    public static class ScoreEntry implements Serializable {
        private static final long serialVersionUID = 1L;

        public String name;
        public int score;

        public ScoreEntry(String name, int score) {
            this.name = name;
            this.score = score;
        }

        @Override
        public String toString() {
            return String.format("%-10s : %d점", name, score);
        }
    }
}
