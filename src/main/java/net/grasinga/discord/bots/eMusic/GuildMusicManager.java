package net.grasinga.discord.bots.eMusic;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;

/**
 * Holder for both the player and a track scheduler for one guild.
 */
class GuildMusicManager {
    /**
     * Audio player for the guild.
     */
    private final AudioPlayer player;
    /**
     * Track scheduler for the player.
     */
    final TrackScheduler scheduler;

    /**
     * Creates a player and a track scheduler.
     * @param manager Audio player manager to use for creating the player.
     */
    GuildMusicManager(AudioPlayerManager manager) {
        player = manager.createPlayer();
        scheduler = new TrackScheduler(player);
        player.addListener(scheduler);
    }

    /**
     * Returns the {@link AudioPlayer}.
     * @return The {@link AudioPlayer}.
     */
    AudioPlayer getPlayer() {
        return player;
    }

    /**
     * @return Wrapper around AudioPlayer to use it as an AudioSendHandler.
     */
    AudioPlayerSendHandler getSendHandler() {
        return new AudioPlayerSendHandler(player);
    }
}
