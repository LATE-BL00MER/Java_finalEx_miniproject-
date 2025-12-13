package zombie_game;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class BossWordManager {

    // ✅ (추가) 싱글톤
    private static final BossWordManager instance = new BossWordManager();
    public static BossWordManager getInstance() { return instance; }

    private final List<String> wordPool = new ArrayList<>();
    private final Random random = new Random();

    // ✅ 생성자에서 자동 로드 (기존 실행 흐름 유지)
    private BossWordManager() {
        loadFromFile("boss_words.txt");
    }

    // ---------- 로딩 ----------
    public void loadFromFile(String fileName) {
        wordPool.clear();

        File f = new File(fileName);
        if (!f.exists()) {
            System.out.println("[BossWordManager] 파일이 없습니다: " + f.getAbsolutePath());
            return;
        }

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))) {

            String line;
            while ((line = br.readLine()) != null) {
                String w = line.trim();
                if (!w.isEmpty()) wordPool.add(w);
            }

            System.out.println("[BossWordManager] 보스 단어 " + wordPool.size() + "개 로드 완료");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ✅ 핵심: 중복 없는 단어 뽑기 (public으로 제공)
    public String[] getRandomBossWords(int count) {
        if (wordPool.isEmpty() || count <= 0) return null;

        // (1) 풀 크기가 충분하면: 완전 중복 없이 뽑기
        if (wordPool.size() >= count) {
            List<String> copy = new ArrayList<>(wordPool);
            Collections.shuffle(copy, random);
            String[] result = new String[count];
            for (int i = 0; i < count; i++) result[i] = copy.get(i);
            return result;
        }

        // (2) 풀 크기가 부족하면: 연속 중복만 방지
        String[] result = new String[count];
        String prev = null;

        for (int i = 0; i < count; i++) {
            String w;
            int guard = 0;
            do {
                w = wordPool.get(random.nextInt(wordPool.size()));
                guard++;
                if (guard > 100) break;
            } while (prev != null && w.equals(prev));

            result[i] = w;
            prev = w;
        }

        return result;
    }
}
