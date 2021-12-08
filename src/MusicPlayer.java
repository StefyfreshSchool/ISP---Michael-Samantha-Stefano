import java.io.File;
import java.io.IOException;

import javax.sound.sampled.*;

/**
 * A simple music player that accepts AIFC, AIFF, AU, SND and WAVE input formats.
 */
public class MusicPlayer {
    private AudioInputStream audioInput;
    private Clip clip;

    /**
     * Instantiates a new MusicPlayer with an input file specified by {@code filePath}.
     * <p>
     * It unobtrusively logs errors to {@code System.err}.
     * @param filePath - path to playable music file
     */
    public MusicPlayer(String filePath) {
        try {
            audioInput = AudioSystem.getAudioInputStream(new File(filePath));
            clip = AudioSystem.getClip();
            clip.open(audioInput);
        } catch (IOException | LineUnavailableException e) {
            System.err.println("ERROR: Audio not playing!");
            e.printStackTrace();
        } catch (UnsupportedAudioFileException e) {
            System.err.println("ERROR: Audio file not supported!");
            e.printStackTrace();
        }
        clip.loop(Clip.LOOP_CONTINUOUSLY);
        //TODO: keep thread alive to let music loop
    }
    

    /**Starts the music. */
    public void play() {
        clip.start();
    }

    /**Stops the music. */
    public void stop() {
        clip.stop();
    }
    
    /**
     * Sets the current music volume, in decibels, between 0 and -80 dB.
     * @param vol - A {@code float} of the new volume.
     */
    public void setVolume(float vol){
        if (vol > 0 || vol < -80.0f) throw new IllegalArgumentException("Value must be between 0 and -80");
        FloatControl volume = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
        // vol = (float) Math.log10(vol) * 20;
        volume.setValue(vol);

    }

    /**
     * Gets the current music volume, in decibels, between 0 and -80 dB.
     * @return A {@code float} of the current volume.
     */
    public float getVolume() {
        FloatControl volume = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
        // return (float) Math.pow(10, volume.getValue() / 20);
        return volume.getValue();
    }
}
