package temp_project;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ScoreManager {
    private static final String FILE_NAME = "ranking.dat";
    private static ScoreManager instance;
    private List<ScoreEntry> scores;

    private ScoreManager() {
        scores = new ArrayList<>();
        loadScores();
    }

    public static ScoreManager getInstance() {
        if (instance == null) {
            instance = new ScoreManager();
        }
        return instance;
    }

    public void addScore(String name, int score) {
        scores.add(new ScoreEntry(name, score));
        // 점수 높은 순 정렬
        Collections.sort(scores, (o1, o2) -> o2.score - o1.score);

        // 상위 10개만 유지
        if (scores.size() > 10) {
            scores = scores.subList(0, 10);
        }
        saveScores();
    }

    public List<ScoreEntry> getTopScores() {
        return scores;
    }

    private void saveScores() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILE_NAME))) {
            oos.writeObject(scores);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private void loadScores() {
        File f = new File(FILE_NAME);
        if (!f.exists()) return;

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
            scores = (List<ScoreEntry>) ois.readObject();
        } catch (Exception e) {
            e.printStackTrace();
            scores = new ArrayList<>();
        }
    }

    // 내부 클래스 (직렬화 필요)
    public static class ScoreEntry implements Serializable {
        String name;
        int score;

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