package zombie_game;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * WordManager
 * - word.txt에서 단어 읽기
 * - 단어 추가/삭제
 * - 전체 단어 리스트 반환
 * - 랜덤 단어 제공
 * - 싱글톤 패턴
 */
public class WordManager {

    private static WordManager instance;
    private final List<String> words = new ArrayList<>();

    private static final String WORD_FILE = "words.txt";

    /** 싱글톤 인스턴스 */
    public static synchronized WordManager getInstance() {
        if (instance == null) instance = new WordManager();
        return instance;
    }

    /** 생성자: word.txt 로딩만 수행 */
    private WordManager() {
        loadWordsFromFile();
    }

    // ----------------------------------------------------
    // word.txt 읽기
    // ----------------------------------------------------
    private void loadWordsFromFile() {
        words.clear();
        File file = new File(WORD_FILE);

        if (!file.exists()) {
            System.err.println("⚠ word.txt 파일이 존재하지 않음 → 새로 생성합니다.");
            saveWordsToFile();   // 빈 파일 생성
            return;
        }

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {

            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    words.add(line);
                }
            }

            System.out.println("✔ word.txt 로드 완료 (단어 수: " + words.size() + ")");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ----------------------------------------------------
    // word.txt 저장하기
    // ----------------------------------------------------
    private void saveWordsToFile() {
        try (BufferedWriter bw = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(WORD_FILE), StandardCharsets.UTF_8))) {

            for (String w : words) {
                bw.write(w);
                bw.newLine();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ----------------------------------------------------
    // 단어 추가
    // ----------------------------------------------------
    public synchronized void addWord(String word) {
        if (word == null) return;
        word = word.trim();
        if (word.isEmpty()) return;

        words.add(word);
        saveWordsToFile();
    }

    // ----------------------------------------------------
    // 단어 삭제
    // ----------------------------------------------------
    public synchronized void removeWord(String word) {
        if (word == null) return;

        words.removeIf(w -> w.equalsIgnoreCase(word));
        saveWordsToFile();
    }

    // ----------------------------------------------------
    // 모든 단어 반환
    // ----------------------------------------------------
    public synchronized List<String> getWords() {
        return new ArrayList<>(words);
    }

    /** 기존 코드 호환용 (ZombieFrame 등에서 호출) */
    public synchronized List<String> getAllWords() {
        return getWords();
    }

    // ----------------------------------------------------
    // 랜덤 단어 제공
    // ----------------------------------------------------
    private final Random random = new Random();

    public synchronized String getRandomWord() {
        if (words.isEmpty()) return "???";
        return words.get(random.nextInt(words.size()));
    }

}
