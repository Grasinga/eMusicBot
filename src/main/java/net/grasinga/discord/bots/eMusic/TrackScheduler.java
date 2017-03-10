package net.grasinga.discord.bots.eMusic;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

import java.util.ArrayList;
import java.util.List;

/**
 * This class schedules tracks for the audio player. It contains the queue of tracks.
 */
public class TrackScheduler extends AudioEventAdapter {
    private AudioPlayer player;
    List<AudioTrack> queue;
    AudioTrack lastTrack = null;
    private AudioTrack nowPlaying = null;

    /**
     * @param player The audio player this scheduler uses
     */
    TrackScheduler(AudioPlayer player) {
        this.player = player;
        this.queue = new ArrayList<>();
    }

    /**
     * Add the next track to queue or play right away if nothing is in the queue.
     *
     * @param track The track to play or add to queue.
     */
    void queue(AudioTrack track) {
        // Calling startTrack with the noInterrupt set to true will start the track only if nothing is currently playing. If
        // something is playing, it returns false and does nothing. In that case the player was already playing so this
        // track goes to the queue instead.
        if(player.startTrack(track, true)) {
            lastTrack = track;
            nowPlaying = track;
        }
        else
            queue.add(track);
    }

    /**
     * Start the next track, stopping the current one if it is playing.
     */
    void nextTrack() {
        // Start the next track, regardless of if something is already playing or not. In case queue was empty, we are
        // giving null to startTrack, which is a valid argument and will simply stop the player.

        lastTrack = nowPlaying;

        AudioTrack track = queue.get(0);
        if(track == null)
            eMusic.playbackFinished(true);
        else
            eMusic.playbackFinished(false);
        try {
            player.startTrack(track, false);
        } catch (Exception e) {
            if(track != null)
                player.startTrack(track.makeClone(), false);
        }
        nowPlaying = track;
        queue.remove(track);
    }

    /**
     * Get the next track in the queue.
     * @return The next {@link AudioTrack} in the queue.
     */
    AudioTrack getNextTrack() {
        if(queue.size() > 0)
            return queue.get(0);
        else
            return null;
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        // Only start the next track if the end reason is suitable for it (FINISHED or LOAD_FAILED)
        if (endReason.mayStartNext) {
            lastTrack = track;
            nextTrack();
        }
    }
}
