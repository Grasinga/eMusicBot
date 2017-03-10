package net.grasinga.discord.bots.eMusic;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.managers.AudioManager;

import javax.security.auth.login.LoginException;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <a href="http://ethereal.network/" target="_blank">Ethereal Network</a>'s MusicPlayer Bot<br>
 * eMusic is a music bot that brings basic music functionality to Discord.<br>
 * It requires a bot token and bot management role to be passed in.<br>
 * The arguments can be passed from the command line or a bot.properties file in the bot's directory.<br>
 * bot.properties file should have one argument per line.<br>
 * <br>
 * Arguments are as follows:<br>
 * - Bot Token<br>
 * - Bot Management GuildRole (Used to send command errors if any occur.)<br>
 * <br>
 * Created by <a href="https://github.com/Grasinga" target="_blank">Grasinga</a> using
 * <a href="https://github.com/DV8FromTheWorld/JDA" target="_blank">JDA</a> and
 * <a href="https://github.com/DV8FromTheWorld/JDA-Player" target="_blank">JDA-Player</a>
 */
class eMusic extends ListenerAdapter {

    /**
     * Creates and runs the eMusic bot with the given bot token and bot management role arguments.
     *
     * @param args Command line arguments.
     * @see eMusic
     */
    public static void main(String[] args) {
        String token = "";
        List<String> stations = new ArrayList<>();
        try {
            if (args.length >= 2) {
                token = args[0];
                managerRole = args[1];
            }
            else {
                BufferedReader br = new BufferedReader(new FileReader(new File("./bot.properties")));

                String properties = br.readLine();
                if (properties != null)
                    token = properties;

                properties = br.readLine();
                if (properties != null)
                    managerRole = properties;

                properties = br.readLine();
                while (properties != null) {
                    stations.add(properties);
                    properties = br.readLine();
                }

                getRadioStations(stations);

                br.close();
            }

            new JDABuilder(AccountType.BOT)
                    .setBulkDeleteSplittingEnabled(false)
                    .setToken(token)
                    .addListener(new eMusic())
                    .buildBlocking();
        }
        catch (IllegalArgumentException e){
            System.out.println("The config was not populated. Please make sure the bot token and management role were given.");
        }
        catch (LoginException e){
            System.out.println("The provided bot token was incorrect. Please provide a valid token.");
        }
        catch (InterruptedException | RateLimitedException e){
            System.out.println("A thread interruption occurred. Check Stack Trace below for source.");
            e.printStackTrace();
        }
        catch (FileNotFoundException e){
            System.out.println("Could not find bot.properties file!");
        }
        catch (IOException e){
            System.out.println("Could not read bot.properties file!");
        }
    }

    /**
     * Populates {@link #radioStations} from a list of stations. Stations are in the format "Name, URL".
     * @param stations {@link List} of stations to be added to {@link #radioStations}.
     */
    private static void getRadioStations(List<String> stations) {
        try {
            for (String station : stations) {
                String[] parts = station.split(", ");
                if(parts.length < 2) {
                    System.out.println("Could not load all the radio stations!");
                    break;
                }
                radioStations.put(parts[0],parts[1]);
            }
        } catch (Exception e) {System.out.println("Could not load all the radio stations!");}
    }

    /**
     * Bot's management role. Default role is Server.
     */
    private static String managerRole = "Server";

    /**
     * VoiceChannel that the bot is/will be playing in.
     */
    private VoiceChannel voiceChannel = null;

    /**
     * Holds internet radio stations: String 1 = Station name, String 2 = Station URL
     */
    private static HashMap<String,String> radioStations = new HashMap<>();

    private final AudioPlayerManager playerManager;

    private final Map<Long, GuildMusicManager> musicManagers;

    private boolean trackIsLoaded = false;

    private AudioTrack currentAudioTrack = null;

    private static boolean finishedPlayback = false;

    /**
     * Sets the bot management role and loads the {@link #playerManager}.
     */
    private eMusic() {
        this.musicManagers = new HashMap<>();

        this.playerManager = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerRemoteSources(playerManager);
        AudioSourceManagers.registerLocalSource(playerManager);
    }

    private synchronized GuildMusicManager getGuildAudioPlayer(Guild guild) {
        long guildId = Long.parseLong(guild.getId());
        GuildMusicManager musicManager = musicManagers.computeIfAbsent(guildId, k -> new GuildMusicManager(playerManager));

        guild.getAudioManager().setSendingHandler(musicManager.getSendHandler());

        return musicManager;
    }

    /**
     * Checks for a command anytime a guild message is received.<br>
     * Creates parameters for all functions that run commands.<br>
     * Parameters are as follows:<br>
     * &nbsp;&nbsp;&nbsp;&nbsp;guild = Guild of the event's message.<br>
     * &nbsp;&nbsp;&nbsp;&nbsp;channel = TextChannel of the event's message.<br>
     * &nbsp;&nbsp;&nbsp;&nbsp;message = The event's message.<br>
     * &nbsp;&nbsp;&nbsp;&nbsp;commandline = The command and arguments of the event's message.<br>
     * &nbsp;&nbsp;&nbsp;&nbsp;command = The command of the event's message.<br>
     * &nbsp;&nbsp;&nbsp;&nbsp;audioManager = The guild's AudioManager.<br>
     *
     * Runs any functions that the command specifies by passing in the
     * above parameters where it's appropriate. (This is the main function
     * that calls all the other functions.)<br>
     *
     * If the MusicPlayer is not already created;
     * creates the MusicPlayer via createMusicPlayer(audioManager).<br>
     *
     * @param event Contains all the info needed for running commands.
     */
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        // Set argument variables for the current event.
        Guild guild = event.getGuild();
        TextChannel channel = event.getChannel();
        Message message = event.getMessage();
        String commandline = message.getContent();
        String[] parts = commandline.split(" "); // Quick parse to get command.
        String command = parts[0];
        AudioManager audioManager = guild.getAudioManager();
        GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());
        AudioPlayer player = musicManager.getPlayer();

        try {
            // Do an action based on the command given (case insensitive).
            switch (command.toLowerCase()){
                case "-join":
                    joinVoice(audioManager,guild,channel,message);
                    break;
                case "-leave":
                    leaveVoice(audioManager);
                    break;
                case "-play":
                    if(parts.length == 1) {
                        if (player.isPaused())
                            resume(channel);
                        else if (musicManager.scheduler.queue.size() < 1)
                            channel.sendMessage("Nothing to play or resume!").queue();
                        else {
                            AudioTrack next = musicManager.scheduler.getNextTrack();
                            channel.sendMessage("Now playing **" + next.getInfo().title + "**").queue();
                            musicManager.scheduler.nextTrack();
                        }
                    }
                    else if(parts.length == 2)
                        load(message, channel, parts[1]);
                    else {
                        for(int i = 2; i < parts.length-1; i++)
                            parts[1] += (" " + parts[i]);
                        load(message, channel, parts[1]);
                    }
                    break;
                case "-pause":
                    pause(channel);
                    break;
                case "-resume":
                    resume(channel);
                    break;
                case "-nowplaying":
                    nowPlaying(channel);
                    break;
                case "-queue":
                    currentQueue(channel);
                    break;
                case "-list":
                    currentQueue(channel);
                    break;
                case "-skip":
                    skip(channel);
                    break;
                case "-previous":
                    previous(channel);
                    break;
                case "-stop":
                    stop(channel);
                    break;
                case "-clear":
                    clear(channel);
                    break;
                case "-remove":
                    remove(message, channel);
                    break;
                case "-reset":
                    reset(channel);
                    break;
                case "-radio":
                    if(parts.length == 2)
                        radio(message, channel, parts[1]);
                    else
                        channel.sendMessage("Usage: -radio [Station URL | Search Term]").queue();
                    break;
                case "?emusic":
                    sendCommands(event.getAuthor());
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            // Let the command user know there was an error with the command used then notify bot maintenance.
            commandException(e,guild,channel,message);
        }
    }

    /**
     * Loads the audio track and then calls {@link #play(Message, GuildMusicManager, AudioTrack)}.
     * @param command "-play [Audio Source]"
     * @param channel {@link TextChannel} the command was used. Needed for output.
     * @param trackURL The [Audio Source].
     */
    private void load(final Message command, final TextChannel channel, final String trackURL) {
        trackIsLoaded = false;
        GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());

        playerManager.loadItemOrdered(musicManager, trackURL, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                if(track.getInfo().isStream && track.getInfo().title.toLowerCase().contains("unknown"))
                    channel.sendMessage("Added stream **" + track.getInfo().identifier + "** to queue.").queue();
                else
                    channel.sendMessage("Added **" + track.getInfo().title + "** to queue.").queue();

                setTrackLoaded(true);
                play(command, musicManager, track);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                AudioTrack firstTrack = playlist.getSelectedTrack();

                if (firstTrack == null) {
                    firstTrack = playlist.getTracks().get(0);
                }

                channel.sendMessage("Adding to queue **" + firstTrack.getInfo().title
                        + "** (first track of playlist ***" + playlist.getName() + "***).").queue();

                setTrackLoaded(true);
                for(AudioTrack track : playlist.getTracks())
                    play(command, musicManager, track);
            }

            @Override
            public void noMatches() {
                setTrackLoaded(false);
                play(command, musicManager, null);
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                setTrackLoaded(false);
                play(command, musicManager, null);
            }
        });
    }

    /**
     * Queues a song.
     * @param command Contains the arguments of the command and the {@link TextChannel} for output.
     * @param musicManager Handles queues.
     * @param track The song to be queued.
     */
    private void play(Message command, GuildMusicManager musicManager, AudioTrack track) {

        // Handles cases where a link or local file weren't found in load()
        if (!trackIsLoaded || track == null) {
            String[] parts = command.getContent().split(" ");
            if (parts.length > 1) {
                TextChannel channel = command.getTextChannel();
                String trackURL = parts[1];
                if (!trackURL.contains("https://") || !trackURL.contains("http://")) {
                    String youtubeLink = ("https://www.youtube.com/watch?v=" + YouTubeSearch.videoIdSearch(trackURL));
                    playerManager.loadItemOrdered(musicManager, youtubeLink, new AudioLoadResultHandler() {
                        @Override
                        public void trackLoaded(AudioTrack track) {
                            if(track.getInfo().isStream && track.getInfo().title.toLowerCase().contains("unknown"))
                                channel.sendMessage("Added stream **" + track.getInfo().identifier + "** to queue.").queue();
                            else
                                channel.sendMessage("Added **" + track.getInfo().title + "** to queue.").queue();

                            setTrackLoaded(true);
                            play(command, musicManager, track);
                        }

                        @Override
                        public void playlistLoaded(AudioPlaylist playlist) {
                            AudioTrack firstTrack = playlist.getSelectedTrack();

                            if (firstTrack == null) {
                                firstTrack = playlist.getTracks().get(0);
                            }

                            channel.sendMessage("Adding to queue **" + firstTrack.getInfo().title
                                    + "** (first track of playlist ***" + playlist.getName() + "***).").queue();

                            setTrackLoaded(true);
                            play(command, musicManager, firstTrack);
                        }

                        @Override
                        public void noMatches() {
                            channel.sendMessage("No matches for **" + trackURL + "**.").queue();
                        }

                        @Override
                        public void loadFailed(FriendlyException exception) {
                            channel.sendMessage("Loading failed for **" + trackURL + "**.").queue();
                        }
                    });
                    return;
                }
                command.getTextChannel().sendMessage("Could not load a track for **" + trackURL + "**.").queue();
            }
        }

        if(trackIsLoaded) {
            currentAudioTrack = musicManager.getPlayer().getPlayingTrack();
            checkVoiceConnection(command.getGuild(), command.getTextChannel(), command);
            musicManager.scheduler.queue(track);
        }
        else
            command.getTextChannel().sendMessage("Nothing to play or resume!").queue();
    }

    /**
     * Pauses playback.
     * @param channel {@link TextChannel} the command was used. Needed for output.
     */
    private void pause(TextChannel channel) {
        GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());
        AudioPlayer player = musicManager.getPlayer();
        if(player.isPaused())
            channel.sendMessage("Playback is already paused. Use -play or -resume to start playback.").queue();
        else if(trackIsLoaded) {
            player.setPaused(true);
            playbackFinished(false);
            channel.sendMessage("Playback has been paused.").queue();
        }
        else
            channel.sendMessage("Playback is not paused. There is currently no songs playing.").queue();
    }

    /**
     * Resumes playback.
     * @param channel {@link TextChannel} the command was used. Needed for output.
     */
    private void resume(TextChannel channel) {
        GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());
        AudioPlayer player = musicManager.getPlayer();
        if(trackIsLoaded) {
            long guildId = Long.parseLong(channel.getGuild().getId());
            currentAudioTrack = musicManagers.get(guildId).getPlayer().getPlayingTrack();
        }
        if (player.getPlayingTrack() == null && !player.isPaused()) {
            if(musicManager.scheduler.getNextTrack() != null) {
                currentAudioTrack = musicManager.scheduler.getNextTrack();
                player.startTrack(currentAudioTrack, false);
                channel.sendMessage("Now resuming with **" + currentAudioTrack.getInfo().title + "**.").queue();
            }
            else {
                channel.sendMessage("Nothing in queue to resume!").queue();
                return;
            }
        }
        if (!player.isPaused()) {
            channel.sendMessage("Playback is not paused.").queue();
            return;
        }
        if (currentAudioTrack != null) {
            player.setPaused(false);
            channel.sendMessage("Now resuming **" + currentAudioTrack.getInfo().title + "**.").queue();
        }
        else if(finishedPlayback)
            channel.sendMessage("Music Bot has no songs to resume.").queue();
        else
            channel.sendMessage("Nothing to play or resume.").queue();
    }

    /**
     * Skips to the next song.
     * @param channel Channel for {@link Guild} and output.
     */
    private void skip(TextChannel channel) {
        GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());
        if(musicManager.scheduler.queue.size() > 0) {
            musicManager.scheduler.nextTrack();
            channel.sendMessage("Skipped to the next track.").queue();
        }
        else
            channel.sendMessage("There are no tracks in the queue to skip to.").queue();
    }

    /**
     * Skips to the previous song.
     * @param channel Channel for {@link Guild} and output.
     */
    private void previous(TextChannel channel) {
        GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());

        if(musicManager.scheduler.lastTrack == null) {
            channel.sendMessage("There is no previous track.").queue();
            return;
        }

        List<AudioTrack> newListWithLast = new ArrayList<>();
        newListWithLast.add(musicManager.scheduler.lastTrack);
        newListWithLast.addAll(musicManager.scheduler.queue);
        musicManager.scheduler.queue = newListWithLast;
        musicManager.scheduler.nextTrack();

        channel.sendMessage("Skipped to the previous track.").queue();
    }

    /**
     * Display's song name and artist in channel.
     * @param channel {@link TextChannel} the command was used. Needed for output.
     */
    private void nowPlaying(TextChannel channel) {
        if(trackIsLoaded) {
            long guildId = Long.parseLong(channel.getGuild().getId());
            currentAudioTrack = musicManagers.get(guildId).getPlayer().getPlayingTrack();
        }
        if(currentAudioTrack != null) {
            if(currentAudioTrack.getInfo().isStream) {
                String song = InternetRadioParser.getCurrentSong(currentAudioTrack.getInfo().identifier);
                String artist = InternetRadioParser.getCurrentSongArtist(currentAudioTrack.getInfo().identifier);
                if(song.contains("Was unable to get JSON data"))
                    song = currentAudioTrack.getInfo().title;
                if(artist.contains("Was unable to get JSON data"))
                    artist = currentAudioTrack.getInfo().author;
                if(song.toLowerCase().contains("unknown") && artist.toLowerCase().contains("unknown"))
                    channel.sendMessage("**Radio Station:** " + currentAudioTrack.getInfo().identifier).queue();
                else
                    channel.sendMessage(
                            "**Song:** " + song
                                    + "\n**Artist:** " + artist
                    ).queue();
            }
            else
                channel.sendMessage(
                        "**Song:** " + currentAudioTrack.getInfo().title
                                + "\n**Artist:** " + currentAudioTrack.getInfo().author
                ).queue();
        }
        else
            channel.sendMessage("Nothing is playing currently.").queue();
    }

    /**
     * Display's all the currently queued songs.
     * @param channel {@link TextChannel} the command was used. Needed for output.
     */
    private void currentQueue(TextChannel channel) {
        GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());
        if(musicManager.scheduler.queue.size() < 1) {
            channel.sendMessage("There is currently no queue.").queue();
            return;
        }

        String list = "";
        long total = 0;
        String title;
        String time;
        for(AudioTrack track : musicManager.scheduler.queue) {
            long duration = track.getDuration();
            total += duration;
            title = track.getInfo().title;
            time = calculateDurationTime(duration);
            list += "\n" + title + " " + time;
        }
        String queue = "__**Current Queue " + calculateDurationTime(total) + ":**__\n";
        queue += list;

        if(queue.length() > 2000)
            splitMessage(queue, channel);
        else
            channel.sendMessage(queue).queue();
    }

    /**
     * Allows for messages over 2000 characters be sent.
     * @param longMessage {@link Message} that is over 2000 characters.
     * @param channel {@link TextChannel} that the split up {@link Message} will be sent to.
     */
    private void splitMessage(String longMessage, TextChannel channel) {
        String[] lines = longMessage.split("\n");
        String message = "";
        for(String s : lines) {
            if((message.length() + s.length()) < 2000)
                message += s + "\n";
            else {
                channel.sendMessage(message).queue();
                message = "";
            }
        }
        if(message.length() > 0)
            channel.sendMessage(message).queue();
    }

    /**
     * Calculate the hours, minutes, and seconds of a time of type long.
     * @param duration Time as long.
     * @return A String of the time in the format [hh:mm:ss].
     */
    private String calculateDurationTime(long duration) {
        String time = "[";

        int hours   = (int) ((duration / (1000*60*60)) % 24);
        int minutes = (int) ((duration / (1000*60)) % 60);
        int seconds = (int) (duration / 1000) % 60 ;

        if(hours > 0)
            time += String.format("%02d", hours) + ":";
        if(minutes > 0 || hours > 0)
            time += String.format("%02d", minutes) + ":";
        if(seconds > 0 || minutes > 0 || hours > 0)
            if (minutes <= 0 && hours <= 0)
                time += "00:" + String.format("%02d", seconds) + "]";
            else
                time += String.format("%02d", seconds) + "]";

        return time;
    }

    /**
     * Stops playback.
     * @param channel {@link TextChannel} the command was used. Needed for output.
     */
    private void stop(TextChannel channel) {
        GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());
        musicManager.getPlayer().stopTrack();
        currentAudioTrack = null;
        playbackFinished(true);
        channel.sendMessage("Playback has stopped.").queue();
    }

    /**
     * Clears the current currentQueue.
     * @param channel {@link TextChannel} the command was used. Needed for output.
     */
    private void clear(TextChannel channel) {
        GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());
        if(musicManager.scheduler.queue.size() < 1) {
            channel.sendMessage("No queue to clear.").queue();
            return;
        }
        musicManager.scheduler.queue.clear();
        channel.sendMessage("The current queue has been cleared.").queue();
    }

    /**
     * Removes the specified track from the current currentQueue.
     * @param message The {@link Message} that contains the specified track.
     * @param channel {@link TextChannel} the command was used. Needed for output.
     */
    private void remove(Message message, TextChannel channel) {
        GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());
        if(musicManager.scheduler.queue.size() < 1) {
            channel.sendMessage("No queue; no tracks to remove.").queue();
            return;
        }
        String[] parts = message.getContent().split(" ");

        if(musicManager.scheduler.queue.size() == 1 || parts.length == 1) {
            String title = musicManager.scheduler.queue.get(0).getInfo().title;
            musicManager.scheduler.queue.remove(0);
            channel.sendMessage("**" + title + "** has been removed from the queue.").queue();
        }
        else if(parts.length > 1) {
            int pos;
            try {
                pos = Integer.parseInt(parts[1]) - 1;
            } catch (Exception e) {
                channel.sendMessage("Invalid position number.").queue();
                return;
            }
            if(pos < musicManager.scheduler.queue.size()) {
                String title = musicManager.scheduler.queue.get(pos).getInfo().title;
                musicManager.scheduler.queue.remove(pos);
                channel.sendMessage("**" + title + "** has been removed from the queue.").queue();
            }
            else
                channel.sendMessage("There is no song at position " + (pos + 1) + ".").queue();
        }
        else
            channel.sendMessage("Usage: -remove [Position in Queue]").queue();
    }

    /**
     * Stops the current track, clears the currentQueue, and disconnects the bot from the voice channel.
     * @param channel {@link TextChannel} the command was used. Needed for output.
     */
    private void reset(TextChannel channel) {
        GuildMusicManager musicManager = getGuildAudioPlayer(channel.getGuild());
        musicManager.getPlayer().stopTrack();
        if(musicManager.scheduler.queue.size() >= 1)
            musicManager.scheduler.queue.clear();
        leaveVoice(channel.getManager().getGuild().getAudioManager());
        channel.sendMessage("The bot has been completely reset.").queue();
    }

    /**
     * Allows radio stations to be played with a link or by search term. If it's by a search term, then the radio station
     * is selected if {@link #radioStations} contains a key that has the genre in it. (Normal music links will also
     * still work as if the -play command was used).
     * @param message Passed through to {@link #load(Message, TextChannel, String)}.
     * @param channel {@link TextChannel} that messages will be set to.
     * @param station Radio station URL or music genre.
     */
    private void radio(Message message, TextChannel channel, String station) {
        if(station.toLowerCase().contains("http://") || station.toLowerCase().contains("https://"))
            load(message, channel, station);
        else {
            for(String key : radioStations.keySet())
                if(key.toLowerCase().contains(station.toLowerCase()))
                    station = radioStations.get(key);
            load(message, channel, station);
        }
    }

    /**
     * Checks to see if the bot is connected to the VoiceChannel of the message's author.
     * If it isn't, it connects to the VoiceChannel of the message's author.
     *
     * @param guild Used to get the guild's voice channels.
     * @param message Used to get the message's author.
     * @param channel Message author's text channel where command was entered.
     * @see eMusic#onGuildMessageReceived(GuildMessageReceivedEvent)
     */
    private void checkVoiceConnection(Guild guild, TextChannel channel, Message message) {
        if (!guild.getAudioManager().isConnected())
            for (VoiceChannel vc : guild.getVoiceChannels()) // Connect to user's channel.
                vc.getMembers().stream().filter(m -> m.getUser().equals(message.getAuthor()))
                        .forEach(u -> setVoiceChannel(guild, channel, vc.getName()));
    }

    /**
     * Joins a voice channel based off of message's contents.
     *
     * @param manager AudioManager used to close connection if connected to another voice channel.
     * @param guild Used to get the guild's VoiceChannel(s).
     * @param channel Used to get the TextChannel that message was sent from.
     * @param message Used to get content of message and the message's author.
     * @see eMusic#onGuildMessageReceived(GuildMessageReceivedEvent)
     */
    private void joinVoice(AudioManager manager, Guild guild, TextChannel channel, Message message) {
        String command = message.getContent();
        boolean validChannel = false;

        if (command.length() > 5) {
            try {
                for (VoiceChannel vc : guild.getVoiceChannels())
                    if(command.substring(6).equalsIgnoreCase(vc.getName()))
                        validChannel = true;
                if(validChannel) {
                    for (VoiceChannel vc : guild.getVoiceChannels()) // Disconnect if connected.
                        vc.getMembers().stream().filter(m -> m.getEffectiveName().equalsIgnoreCase("eMusic"))
                                .forEach(m -> manager.closeAudioConnection());
                    setVoiceChannel(guild,channel, command.substring(6));
                }
                else
                    channel.sendMessage("No voice channel named '" + command.substring(6) + "' exists!");
            } catch (Exception e) {channel.sendMessage("An error occurred when trying to join the channel.");}
        } else {
            for (VoiceChannel vc : guild.getVoiceChannels()) // Get Message Author's voice channel.
                for (Member m : vc.getMembers())
                    if (m.getUser().equals(message.getAuthor()))
                        command = vc.getName();

            for (VoiceChannel vc : guild.getVoiceChannels()) // Get eNetMusicBot's voice channel.
                vc.getMembers().stream().filter(m -> m.getEffectiveName().equalsIgnoreCase("eMusic"))
                        .forEach(m -> voiceChannel = vc);

            // Bot isn't in a/the voice channel.
            validChannel = channel == null || !command.equalsIgnoreCase(channel.getName());

            if (validChannel) {
                for (VoiceChannel vc : guild.getVoiceChannels()) // Disconnect if connected.
                    vc.getMembers().stream().filter(m -> m.getEffectiveName().equalsIgnoreCase("eMusic"))
                            .forEach(m -> manager.closeAudioConnection());
                for (VoiceChannel vc : guild.getVoiceChannels()) // Connect to user's channel.
                    vc.getMembers().stream().filter(m -> m.getUser().equals(message.getAuthor()))
                            .forEach(m -> setVoiceChannel(guild, channel, vc.getName()));
            }
        }
    }

    /**
     * Closes the current VoiceChannel connection if there is one.
     *
     * @param manager Used to get the audio connection.
     * @see eMusic#onGuildMessageReceived(GuildMessageReceivedEvent)
     */
    private void leaveVoice(AudioManager manager){manager.closeAudioConnection();}

    /**
     * Function that gets called if there was any sort of error with the
     * commands from onGuildMessageReceived().
     *
     * Prints the StackTrace of e and notifies both the user
     * of the command and the bot managers of the guild.
     *
     * @param e General Exception that occurred with the command.
     * @param guild Used to get the current guild where the Exception occurred.
     * @param channel Used to get the channel of the command that caused the Exception.
     * @param message Used to get the full message that caused the Exception.
     * @see eMusic#onGuildMessageReceived(GuildMessageReceivedEvent)
     */
    private void commandException(Exception e, Guild guild, TextChannel channel, Message message) {
        channel.sendMessage("An ***ERROR*** occurred when trying to execute command: "
                + message.getContent()
                + "\nAll users with the bot maintenance role have been notified. Check '?eMusic' in the mean time.").queue();
        e.printStackTrace();
        Role server = null;
        for(Role role : guild.getRoles())
            if(role.getName().equalsIgnoreCase(managerRole)){server = role; break;}
        if(server != null)
            for(Member m : guild.getMembersWithRoles(server)) {
                PrivateChannel pm = m.getUser().openPrivateChannel().complete();
                pm.sendMessage(
                        "An error occurred when " + message.getAuthor() + " issued command: " + message.getContent()
                                + "\nPlease check the console for the stacktrace.").queue();
            }
    }

    /**
     * Function runs after the '/join' command is entered.
     *
     * Places the bot in the VoiceChannel specified by chanName argument if possible.
     * If the VoiceChannel cannot be joined because it doesn't exist, a message is sent to channel
     * to notify the user of the command.
     *
     * @param guild Used to get the current guild where the Exception occurred.
     * @param channel Used to get the channel of the command that caused the Exception.
     * @param chanName VoiceChannel to join.
     * @see eMusic#joinVoice(AudioManager, Guild, TextChannel, Message)
     */
    private void setVoiceChannel(Guild guild, TextChannel channel, String chanName) {
        //Scans through the VoiceChannels in this Guild, looking for one with a case-insensitive matching name.
        voiceChannel = guild.getVoiceChannels().stream().filter(
                vChan -> vChan.getName().equalsIgnoreCase(chanName))
                .findFirst().orElse(null);  //If there isn't a matching name, return null.
        if (voiceChannel == null)
        {
            channel.sendMessage("There isn't a VoiceChannel called: '" + chanName + "'");
            return;
        }
        try {
            guild.getAudioManager().openAudioConnection(voiceChannel);
        }catch (Exception e){e.printStackTrace();}
    }

    /**
     * Function that gets called after the commands '/commands','/cmds', or '/help'
     * get used. Sends the user of the command a private message of all the commands usable
     * by the bot.
     *
     * @param u Used to get the user of the command and their private message channel.
     */
    private void sendCommands(User u) {
        PrivateChannel pm = u.openPrivateChannel().complete();
        pm.sendMessage(
                "__**Commands:**__\n" +
                "```java\n" +
                "!ALL COMMANDS ARE GUILD WIDE ACTIONS!\n" +
                "-join [VoiceChannel] // If no VoiceChannel is specified, then join the user's channel if possible.\n" +
                "-leave // Leaves the current VoiceChannel if in one.\n" +
                "-play [Audio Source] // Plays YouTube or SoundCloud audio from Term/Link. " +
                    "If no Term/Link is specified, then resume playback if possible.\n" +
                "-pause // Pauses playback.\n" +
                "-resume | -play // Resumes playback.\n" +
                "-skip // Skips to the next track.\n" +
                "-previous // Skips to the last track.\n" +
                "-nowplaying // Displays the current song's info.\n" +
                "-queue | -list // Displays the current queue if there is one.\n" +
                "-stop // Stops the current track and ends playback; keeps the queue.\n" +
                "-clear // Clears the queue.\n" +
                "-remove [Position in Queue] // Removes the specified track from the queue. " +
                    "If a position is not given, it will remove the first track in the queue.\n" +
                "-reset // Stops the current track, clears the queue, and disconnects the bot from the voice channel.\n" +
                "-radio [Station URL | Search Term] // Queues a radio station based on the term or url.\n" +
                "?eMusic // Messages the user a list of commands.\n" +
                "```"
        ).queue();
    }

    /**
     * Used to set {@link #trackIsLoaded}.
     * @param loaded Used to set the variable.
     */
    private void setTrackLoaded(boolean loaded) {
        trackIsLoaded = loaded;
    }

    /**
     * Set whether the playback is finished or not.
     * @param finished Playback's state.
     */
    static void playbackFinished(boolean finished) {
        finishedPlayback = finished;
    }
}