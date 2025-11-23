package temp_project;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 단어 저장/관리용 클래스
 * - 기본 단어들 + 사용자가 추가한 단어들
 * - 랜덤 단어 제공
 */
public class WordManager {

    private static WordManager instance;
    private final List<String> words = new ArrayList<>();
    private final Random random = new Random();

    private WordManager() {
        // 기본 단어들
        Collections.addAll(words,
                "가나다", "아몰라", "피", "생존", "도맏쳐",
                "콩", "비", "불", "물", "과제"
        );
    }

    public static WordManager getInstance() {
        if (instance == null) {
            instance = new WordManager();
        }
        return instance;
    }

    public synchronized void addWord(String w) {
        String s = w.trim();
        if (!s.isEmpty() && !words.contains(s)) {
            words.add(s);
        }
    }

    public synchronized List<String> getAllWords() {
        return new ArrayList<>(words);
    }

    public synchronized String getRandomWord() {
        if (words.isEmpty()) return "ZOMBIE";
        return words.get(random.nextInt(words.size()));
    }
}
