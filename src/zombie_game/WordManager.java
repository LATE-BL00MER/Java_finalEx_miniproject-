package zombie_game;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 단어 저장/관리용 클래스
 * - words.txt 파일에서 단어를 읽어와 관리
 * - 파일이 없으면 기본 단어들로 초기화 후 자동 생성
 * - 랜덤 단어 제공
 */
public class WordManager {

    private static WordManager instance;

    // IntelliJ 기준 경로: 프로젝트/src/temp_project/words.txt
    private static final String WORD_FILE_PATH = "src/temp_project/words.txt";

    private final List<String> words = new ArrayList<>();
    private final Random random = new Random();

    private WordManager() {
        // 1) 파일에서 단어 읽기 시도
        loadWordsFromFile();

        // 2) 파일이 없거나, 읽은 단어가 하나도 없으면 기본 단어 세트 사용
        if (words.isEmpty()) {
            loadDefaultWords();
            saveWordsToFile(); // 기본 단어를 파일로 써놓기
        }
    }

    public static synchronized WordManager getInstance() {
        if (instance == null) {
            instance = new WordManager();
        }
        return instance;
    }

    /** ------------------------------------
     *  파일 입출력
     *  ------------------------------------ */

    // words.txt에서 단어 읽기
    private synchronized void loadWordsFromFile() {
        words.clear();

        File f = new File(WORD_FILE_PATH);
        if (!f.exists()) {
            // 파일이 아직 없으면 그냥 리턴 (기본 단어 사용 예정)
            return;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                String s = line.trim();
                if (!s.isEmpty() && !words.contains(s)) {
                    words.add(s.toUpperCase());  // 모두 대문자로 통일
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Collections.sort(words);
    }

    // 현재 words 리스트를 words.txt에 저장
    private synchronized void saveWordsToFile() {
        File f = new File(WORD_FILE_PATH);
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(f))) {
            for (String w : words) {
                bw.write(w);
                bw.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** ------------------------------------
     *  기본 단어 세트 (파일 없을 때 사용)
     *  ------------------------------------ */
    private void loadDefaultWords() {
        Collections.addAll(words,
                "ZOMBIE", "ATTACK", "DANGER", "SURVIVE", "APOCALYPSE",
                "BLOOD", "BRAIN", "NIGHTMARE", "INFECTION", "OUTBREAK",
                "PANIC", "ESCAPE", "GUNSHOT", "BULLET", "HUNTER",
                "CRAWLER", "RUNNER", "SCREAM", "VIRUS", "ANTIDOTE"
        );
        Collections.sort(words);
    }

    /** ------------------------------------
     *  외부에서 사용할 메서드들
     *  ------------------------------------ */

    // 새 단어 추가 (코드에서 사용 시)
    public synchronized void addWord(String word) {
        if (word == null) return;
        String s = word.trim().toUpperCase();
        if (s.isEmpty()) return;

        if (!words.contains(s)) {
            words.add(s);
            Collections.sort(words);
            saveWordsToFile(); // 파일에도 반영
        }
    }

    // 단어 삭제
    public synchronized void removeWord(String word) {
        if (word == null) return;
        String s = word.trim().toUpperCase();
        if (words.remove(s)) {
            saveWordsToFile();
        }
    }

    // 전체 단어 목록 (복사본 반환)
    public synchronized List<String> getAllWords() {
        return new ArrayList<>(words);
    }

    // 랜덤 단어 하나 가져오기
    public synchronized String getRandomWord() {
        if (words.isEmpty()) return "ZOMBIE";
        return words.get(random.nextInt(words.size()));
    }

    // 필요하면, 강제로 파일 다시 읽어오는 메서드 (옵션)
    public synchronized void reloadFromFile() {
        loadWordsFromFile();
        if (words.isEmpty()) {
            loadDefaultWords();
        }
    }
}
