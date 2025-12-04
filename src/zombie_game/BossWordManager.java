package zombie_game;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * boss_words.txt 에서 보스 단어 풀을 읽어오는 매니저
 *
 * - boss_words.txt 형식:
 *
 *   한 줄 = 한 단어
 *
 *   예)
 *   핵펀치
 *   좀비대군
 *   도시붕괴
 *   맹독안개
 *   광기폭주
 *   절망의비
 */
public class BossWordManager {

    private static final BossWordManager instance = new BossWordManager();
    public static BossWordManager getInstance() {
        return instance;
    }

    // 보스가 쓸 수 있는 단어 전체 풀
    private final List<String> wordPool = new ArrayList<>();
    private final Random random = new Random();

    private BossWordManager() {
        loadFromFile();
    }

    /** boss_words.txt 한 줄씩 읽기 (한 줄 = 한 단어) */
    private void loadFromFile() {
        File file = new File("boss_words.txt"); // words.txt 와 같은 위치

        if (!file.exists()) {
            System.err.println("[BossWordManager] boss_words.txt 파일을 찾을 수 없습니다. (기본 단어 사용 예정)");
            return;
        }

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {

            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                // 빈 줄, 주석(#)은 무시
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                wordPool.add(line);
            }

            System.out.println("[BossWordManager] 보스 단어 " + wordPool.size() + "개 로드 완료");

        } catch (IOException e) {
            System.err.println("[BossWordManager] boss_words.txt 읽기 실패");
            e.printStackTrace();
        }
    }

    /**
     * 보스에게 쓸 단어들을 랜덤으로 N개 뽑아서 반환
     * - 항상 length == count 가 되도록 생성
     */
    public String[] getRandomBossWords(int count) {
        if (wordPool.isEmpty() || count <= 0) {
            return null;
        }

        String[] result = new String[count];

        // 단어 개수가 부족해도 돌아가게 (중복 허용)
        for (int i = 0; i < count; i++) {
            String w = wordPool.get(random.nextInt(wordPool.size()));
            result[i] = w;
        }

        return result;
    }
}
