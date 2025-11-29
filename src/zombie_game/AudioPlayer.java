package zombie_game;

import javax.sound.sampled.*;
import java.io.File;

/**
 * 단순 BGM 재생용 유틸 클래스
 * - new AudioPlayer("bgm.wav");
 * - playLoop() : 계속 반복 재생
 * - pause()    : 일시 정지
 * - resume()   : 다시 재생
 * - stop()     : 완전히 정지 후 처음으로
 */
public class AudioPlayer {
    private Clip clip;

    public AudioPlayer(String fileName) {
        try {
            // 프로젝트 실행 위치(최상위 폴더) 기준으로 파일 찾기
            File file = new File(fileName);
            if (!file.exists()) {
                System.err.println("오디오 파일을 찾을 수 없습니다: " + file.getAbsolutePath());
                return;
            }

            AudioInputStream ais = AudioSystem.getAudioInputStream(file);
            clip = AudioSystem.getClip();
            clip.open(ais);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** 무한 반복 재생 */
    public void playLoop() {
        if (clip == null) return;
        clip.stop();
        clip.setFramePosition(0);
        clip.loop(Clip.LOOP_CONTINUOUSLY);
    }

    /** 완전히 정지하고 처음 위치로 */
    public void stop() {
        if (clip == null) return;
        clip.stop();
        clip.setFramePosition(0);
    }

    /** 일시정지 */
    public void pause() {
        if (clip != null && clip.isRunning()) {
            clip.stop();
        }
    }

    /** 일시 정지된 상태에서 다시 재생 */
    public void resume() {
        if (clip != null && !clip.isRunning()) {
            clip.start();
        }
    }
}
