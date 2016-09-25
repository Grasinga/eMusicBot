package net.grasinga.eNetMusic;

import net.dv8tion.jda.MessageBuilder;
import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.Message;
import net.dv8tion.jda.entities.Role;
import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.entities.User;
import net.dv8tion.jda.entities.VoiceChannel;
import net.dv8tion.jda.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.hooks.ListenerAdapter;
import net.dv8tion.jda.managers.AudioManager;
import net.dv8tion.jda.player.MusicPlayer;
import net.dv8tion.jda.player.Playlist;
import net.dv8tion.jda.player.source.AudioInfo;
import net.dv8tion.jda.player.source.AudioSource;
import net.dv8tion.jda.player.source.AudioTimestamp;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * eNetMusic is a music bot that brings basic music functionality to Discord.
 * Created by <a href="http://grasinga.net/">Grasinga</a> using JDA & JDA-Player.
 *
 * @see <a href="https://github.com/Grasinga">Grasinga's GitHub</a>
 * @see <a href="https://github.com/DV8FromTheWorld/JDA">JDA</a>
 * @see <a href="https://github.com/DV8FromTheWorld/JDA-Player">JDA-Player</a>
 */
class eNetMusic extends ListenerAdapter
{
    // Initialize base MusicPlayer variable.
    private MusicPlayer player = null;

    // Bot Manager role for the guild:
    private String managerRole = "Server";

    // Initialize other global variables.
    private String audioLink = "";
    private String playerType = "";
    private VoiceChannel voiceChannel = null;
    private boolean muted = false;
    private float playerVolume = 0.15f;
    private DecimalFormat df = new DecimalFormat("#.00");
    private HashMap<String,String> radioStations = new HashMap<>();

    /**
     * Pre-loads some radio stations into radioStations.
     *
     * HashMap<String,String> radioStations;
     * First string is the radio station name, second string is the mp3 stream link.
     */
    eNetMusic(){
        // Get pre-loaded radio stations.
        radioStations.put("C1 Radio - Hits","http://maxxhits.us:8080/maxxhits_mp3");
        radioStations.put("Mix247EDM","http://streaming210.radionomy.com:80/Mix247EDM");
        radioStations.put("HouseTime.FM","http://mp3.stream.tb-group.fm:80/ht.mp3");
        radioStations.put("RADIO 1 ROCK","http://stream.radioreklama.bg:80/radio1rock64");
        radioStations.put("Top100Station - Pop","http://91.250.76.18:80/top100station.mp3");
        radioStations.put("Smooth Jazz Florida","http://streaming308.radionomy.com:80/southfloridasmoothjazz");
        radioStations.put("ABC-Piano - Classical","http://streaming207.radionomy.com:80/ABC-Piano");
        radioStations.put("Old School Classic Rap","http://www.radioson.ru:8009/OldschoolRapClassic.RadioSon.ru.mp3");
        radioStations.put("RnB Radio","http://ml1.t4e.dj:80/rnbradio_high.mp3");
        radioStations.put("BCB Hiphop Radio","http://streaming210.radionomy.com:80/BCBHIPHOPRADIO");
    }

    /**
     * Checks for a command anytime a guild message is received.
     * Creates parameters for all functions that run commands.
     * Parameters are as follows:
     *      <i>guild<i/> = Guild of the event's message.
     *      <i>channel<i/> = TextChannel of the event's message.
     *      <i>message<i/> = The event's message.
     *      <i>commandline<i/> = The command and arguments of the event's message.
     *      <i>command<i/> = The command of the event's message.
     *      <i>audioManager<i/> = The guild's AudioManager.
     *
     * <b>Runs any functions that the command specifies by passing in the
     * above parameters where it's appropriate. (This is the main function
     * that calls all the other functions.)<b/>
     *
     * If the MusicPlayer is not already created;
     * creates the MusicPlayer via <i>createMusicPlayer(audioManager)<i/>.
     *
     * @param event Contains all the info needed for running commands.
     * @see net.dv8tion.jda.events.message.guild.GuildMessageReceivedEvent
     * @see net.dv8tion.jda.entities.Guild
     * @see net.dv8tion.jda.entities.TextChannel
     * @see net.dv8tion.jda.entities.Message
     * @see net.dv8tion.jda.managers.AudioManager
     */
    public void onGuildMessageReceived(GuildMessageReceivedEvent event){
        // Set argument variables for the current event.
        Guild guild = event.getGuild();
        TextChannel channel = event.getChannel();
        Message message = event.getMessage();
        String commandline = message.getContent();
        String[] parts = commandline.split(" "); // Quick parse to get command.
        String command = parts[0];
        AudioManager audioManager = guild.getAudioManager();
        createMusicPlayer(audioManager);

        try{
            // Do an action based on the command given (case insensitive).
            switch (command.toLowerCase()){
                case "/join":
                    joinVoice(audioManager,guild,channel,message);
                    break;
                case "/leave":
                    leaveVoice(audioManager);
                    break;
                case "/play":
                    checkVoiceConnection(guild,channel,message);
                    if(commandline.equalsIgnoreCase("/play") || commandline.equalsIgnoreCase("/play "))
                        resumePlay(player,channel);
                    else if(!commandline.toLowerCase().contains("-radio") && commandline.length() > ("/play ").length())
                        playNonRadio(channel,commandline);
                    else
                        playRadio(channel,commandline);
                    break;
                case "/list":
                    getPlaylist(player,channel);
                    break;
                case "/nowplaying":
                    nowPlaying(player,channel);
                    break;
                case "/skip":
                    skipCurrentSong(player,channel);
                    break;
                case "/repeat":
                    repeatPlayer(player,channel);
                    break;
                case "/shuffle":
                    shufflePlayer(player,channel);
                    break;
                case "/remove":
                    removeFromQueue(player,channel,commandline);
                    break;
                case "/clear":
                    clearPlayList(player,channel);
                    break;
                case "/reset":
                    resetPlayer(player,audioManager,channel);
                    break;
                case "/pause":
                    pausePlayer(player,channel);
                    break;
                case "/stop":
                    stopPlayer(player,channel);
                    break;
                case "/restart":
                    restartPlayer(player,channel);
                    break;
                case "/mute":
                    mutePlayer(player,channel);
                    break;
                case "/unmute":
                    unmutePlayer(player,channel);
                    break;
                case "/volume":
                    if(commandline.length() > ("/volume").length())
                        setVolume(player,channel,commandline);
                    else
                        getVolume(channel);
                    break;
                case "/commands":
                    sendCommands(event.getAuthor());
                    break;
                case "/cmds":
                    sendCommands(event.getAuthor());
                    break;
                case "/help":
                    sendCommands(event.getAuthor());
                    break;
                default:
                    break;
            }
        }catch (Exception e){
            // Let the command user know there was an error with the command used then notify bot maintenance.
            commandException(e,guild,channel,message);
        }
    }

    /**
     * Creates a MusicPlayer object to be used for audio.
     *
     * @param audioManager Used for getting and setting the MusicPlayer's SendingHandler.
     * @see net.dv8tion.jda.player.MusicPlayer
     * @see eNetMusic#onGuildMessageReceived(GuildMessageReceivedEvent)
     */
    private void createMusicPlayer(AudioManager audioManager){
        if (audioManager.getSendingHandler() == null){
            player = new MusicPlayer();
            player.setVolume(playerVolume);
            audioManager.setSendingHandler(player);
        }
        else
            player = (MusicPlayer) audioManager.getSendingHandler();
    }

    /**
     * Runs before the '/play' command.
     * Checks to see if the bot is connected to the VoiceChannel of the <i>message<i/>'s author.
     * If it isn't, it connects to the VoiceChannel of the <i>message<i/>'s author.
     *
     * @param guild Used to get the guild's voice channels.
     * @param message Used to get the message's author.
     * @param channel Message author's text channel where command was entered.
     * @see eNetMusic#onGuildMessageReceived(GuildMessageReceivedEvent)
     */
    private void checkVoiceConnection(Guild guild, TextChannel channel, Message message){
        if (!guild.getAudioManager().isConnected())
            for (VoiceChannel vc : guild.getVoiceChannels()) // Connect to user's channel.
                vc.getUsers().stream().filter(u -> u.equals(message.getAuthor())).forEach(u -> setVoiceChannel(guild, channel, vc.getName()));
    }

    /**
     * Joins a voice channel based off of <i>message<i/>'s contents.
     *
     * @param manager AudioManager used to close connection if connected to another voice channel.
     * @param guild Used to get the guild's VoiceChannel(s).
     * @param channel Used to get the TextChannel that message was sent from.
     * @param message Used to get content of message and the message's author.
     * @see eNetMusic#onGuildMessageReceived(GuildMessageReceivedEvent)
     */
    private void joinVoice(AudioManager manager, Guild guild, TextChannel channel, Message message){
        String command = message.getContent();
        boolean validChannel = false;

        if (command.length() > 5) {
            try {
                for (VoiceChannel vc : guild.getVoiceChannels())
                    if(command.substring(6).equalsIgnoreCase(vc.getName()))
                        validChannel = true;
                if(validChannel) {
                    for (VoiceChannel vc : guild.getVoiceChannels()) // Disconnect if connected.
                        vc.getUsers().stream().filter(u -> u.getUsername().equalsIgnoreCase("eNetMusic")).forEach(u -> manager.closeAudioConnection());
                    setVoiceChannel(guild,channel, command.substring(6));
                }
                else
                    channel.sendMessage("No voice channel named '" + command.substring(6) + "' exists!");
            } catch (Exception e) {channel.sendMessage("An error occurred when trying to join the channel.");}
        } else {
            for (VoiceChannel vc : guild.getVoiceChannels()) // Get Message Author's voice channel.
                for (User u : vc.getUsers())
                    if (u.equals(message.getAuthor()))
                        command = vc.getName();

            for (VoiceChannel vc : guild.getVoiceChannels()) // Get eNetMusicBot's voice channel.
                vc.getUsers().stream().filter(u -> u.getUsername().equalsIgnoreCase("eNetMusic")).forEach(u -> voiceChannel = vc);

            // Bot isn't in a/the voice channel.
            validChannel = channel == null || !command.equalsIgnoreCase(channel.getName());

            if (validChannel) {
                for (VoiceChannel vc : guild.getVoiceChannels()) // Disconnect if connected.
                    vc.getUsers().stream().filter(u -> u.getUsername().equalsIgnoreCase("eNetMusic")).forEach(u -> manager.closeAudioConnection());
                for (VoiceChannel vc : guild.getVoiceChannels()) // Connect to user's channel.
                    vc.getUsers().stream().filter(u -> u.equals(message.getAuthor())).forEach(u -> setVoiceChannel(guild, channel, vc.getName()));
            }
        }
    }

    /**
     * Closes the current VoiceChannel connection if there is one.
     *
     * @param manager Used to get the audio connection.
     * @see eNetMusic#onGuildMessageReceived(GuildMessageReceivedEvent)
     */
    private void leaveVoice(AudioManager manager){manager.closeAudioConnection();}

    /**
     * Resumes playback if a MusicPlayer exists and contains music.
     *
     * @param player Used to get the current MusicPlayer.
     * @param channel Used to get the TextChannel the command was entered.
     * @see eNetMusic#onGuildMessageReceived(GuildMessageReceivedEvent)
     */
    private void resumePlay(MusicPlayer player, TextChannel channel){
        if (player.isPlaying())
        {
            channel.sendMessage("Player is already playing!");
        }
        else if (player.isPaused())
        {
            player.play();
            channel.sendMessage("Playback as been resumed.");
        }
        else
        {
            if (player.getAudioQueue().isEmpty())
                channel.sendMessage("The current audio queue is empty! Add something to the queue first!");
            else
            {
                player.play();
                channel.sendMessage("Player has started playing!");
            }
        }
    }

    /**
     * Gets a Youtube or SoundCloud video from the given argument.
     * Argument expected is a valid YouTube link, SoundCloud link, or
     * a search term. If the YouTube or SoundCloud link is invalid, the
     * function will assume it is a search term. The search term is
     * then sent to <i>getVideoId(term)<i/>.
     *
     * Link is then passed into <i>addToPlaylist()<i/>.
     *
     * @param channel Used to get the TextChannel the command was entered on.
     * @param commandline Used to get the arguments of the command.
     * @see eNetMusic#getVideoId(String)
     * @see eNetMusic#addToPlaylist(TextChannel, String)
     * @see eNetMusic#onGuildMessageReceived(GuildMessageReceivedEvent)
     */
    private void playNonRadio(TextChannel channel, String commandline){
        try {
            if (!commandline.contains("-radio") && commandline.contains("youtube.com") || commandline.contains("youtu.be") || commandline.contains("soundcloud.com"))
                audioLink = commandline.substring("/play ".length());
            else {
                commandline = "/play " + "https://www.youtube.com/watch?v=" + getVideoId(commandline.substring("/play ".length()));
                audioLink = "https://www.youtube.com/watch?v=" + getVideoId(commandline.substring("/play ".length()));
            }
        } catch (Exception e) {channel.sendMessage("An error occurred with the '/play' command!");}

        if(!(audioLink.length() > 1 && audioLink.substring(audioLink.indexOf(".")).length() > 1)) {
            channel.sendMessage("Not a valid YouTube or SoundCloud link!");
            channel.sendMessage("For radio stations use: /play -radio <link>");
            return;
        }

        addToPlaylist(channel,commandline);
    }

    /**
     * Gets a radio station from the given argument.
     * Argument expected is a valid mp3 stream source link or a
     * search term to check for in <i>radioStations<i/>. If link is invalid
     * the function assumes it to be a search term. If the search term
     * does not match a station in <i>radioStations<i/>, then the default
     * radio station is selected instead.
     *
     * Station link is then passed into <i>addToPlaylist()<i/>.
     *
     * @param channel Used to get the TextChannel the command was entered on.
     * @param commandline Used to get the arguments of the command.
     * @see eNetMusic#addToPlaylist(TextChannel, String)
     * @see eNetMusic#onGuildMessageReceived(GuildMessageReceivedEvent)
     */
    private void playRadio(TextChannel channel, String commandline){
        String invalidRadio = "";
        boolean foundStation = false;

        if (commandline.equalsIgnoreCase("/play -radio")
                || commandline.equalsIgnoreCase("/play -radio ")
                || commandline.substring("/play -radio ".length()).equalsIgnoreCase("default")) {
            commandline = "/play -radio http://ethereal.network:8080/enet_128";
            foundStation = true;
        }
        else if(commandline.contains(".") && commandline.substring(commandline.indexOf(".")).length() > 1){
            foundStation = true;
        }
        else
            for(String s : radioStations.keySet())
                if (s.toLowerCase().contains(commandline.substring("/play -radio ".length()).toLowerCase())) {
                    commandline = "/play -radio " + radioStations.get(s);
                    foundStation = true;
                    break;
                }
                else {
                    invalidRadio = commandline.substring("/play -radio ".length());
                }

        if(!foundStation) {
            channel.sendMessage(
                    "'" + invalidRadio + "' is not a valid radio station! Using default radio station instead.");
            commandline = "/play -radio http://ethereal.network:8080/enet_128";
        }
        audioLink = commandline.substring("/play -radio ".length());

        addToPlaylist(channel,commandline);
    }

    /**
     * Receives an audio link from <i>playNonRadio()<i/> or <i>playRadio()<i/>.
     * Plays the provided link's song if nothing is playing. If there is a
     * current song playing, the provided audio link gets placed into the playlist.
     *
     * @param channel Used to get the TextChannel the command was entered on.
     * @param commandline Used to get the arguments of the command.
     * @see eNetMusic#playNonRadio(TextChannel, String)
     * @see eNetMusic#playRadio(TextChannel, String)
     */
    private void addToPlaylist(TextChannel channel, String commandline){
        try {
            Playlist playlist = Playlist.getPlaylist(audioLink);
            List<AudioSource> sources = new LinkedList<>(playlist.getSources());
            if (sources.size() > 1)
            {
                channel.sendMessage("Found a playlist with **" + sources.size() + "** entries.\n" +
                        "Proceeding to gather information and queue sources. This may take some time...");
                final MusicPlayer fPlayer = player;
                Thread thread = new Thread()
                {
                    @Override
                    public void run()
                    {
                        for (Iterator<AudioSource> it = sources.iterator(); it.hasNext();)
                        {
                            AudioSource source = it.next();
                            AudioInfo info = source.getInfo();
                            List<AudioSource> queue = fPlayer.getAudioQueue();
                            if (info.getError() == null)
                            {
                                queue.add(source);
                                if (fPlayer.isStopped())
                                    fPlayer.play();
                            }
                            else
                            {
                                channel.sendMessage("Error detected, skipping source. Error:\n" + info.getError());
                                it.remove();
                            }
                        }
                        channel.sendMessage("Finished queuing provided playlist. Successfully queued **" + sources.size() + "** sources");
                    }
                };
                thread.start();
            }
            else
            {
                String msg = "";
                AudioSource source = sources.get(0);
                AudioInfo info = source.getInfo();
                if (info.getError() == null)
                {
                    player.getAudioQueue().add(source);
                    if (!(commandline.toLowerCase().contains("youtube.com") || commandline.toLowerCase().contains("youtu.be")
                            || commandline.toLowerCase().contains("soundcloud.com"))) {
                        if(Radio.getStationName(audioLink).toLowerCase().contains("could not connect to: ")) {
                            for(String s : radioStations.keySet()) {
                                if (radioStations.get(s).equalsIgnoreCase(commandline.substring("/play -radio ".length()))) {
                                    msg = "**" + s + "** has been queued";
                                    break;
                                } else {
                                    String[] parts = audioLink.split("/");
                                    if(parts.length > 3)
                                        msg = "**" + parts[2] + "** has been queued";
                                    else
                                        msg = "**" + parts[0] + "** has been queued";
                                }
                            }
                        }
                        else
                            msg = "**" + Radio.getStationName(audioLink) + "** has been queued";
                    }
                    else {
                        msg += "**" + info.getTitle() + "** has been queued";
                    }
                    if (player.isStopped())
                    {
                        player.play();
                        msg += " and has started playing";
                    }
                    channel.sendMessage(msg + ".");
                }
                else
                {
                    channel.sendMessage("There was an error while loading the provided URL.\n" +
                            "Error: " + info.getError());
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            channel.sendMessage("An ***ERROR*** occurred when trying to load: " + audioLink);
            channel.sendMessage("Error: " + e.getMessage());
        }
    }

    /**
     * Sends the current playlist to <i>channel<i/> as a message.
     *
     * @param player Used to get the current MusicPlayer.
     * @param channel Used to get the TextChannel to send the playlist to.
     * @see eNetMusic#onGuildMessageReceived(GuildMessageReceivedEvent)
     */
    private void getPlaylist(MusicPlayer player, TextChannel channel){
        List<AudioSource> queue = player.getAudioQueue();
        if (queue.isEmpty())
        {
            channel.sendMessage("The queue is currently empty!");
            return;
        }


        MessageBuilder builder = new MessageBuilder();
        boolean unreadableRadio = false;
        builder.appendString("**__Currently Queued:__**\n\n");
        for (int i = 0; i < queue.size() && i < 10; i++)
        {
            AudioInfo info = queue.get(i).getInfo();
            String audioSource = queue.get(i).getSource();
            if (info == null)
                builder.appendString("*Could not get info for this song.*");
            else
            {
                AudioTimestamp duration = info.getDuration();
                builder.appendString("`[");
                if(!(audioSource.toLowerCase().contains("youtube.com") || audioSource.toLowerCase().contains("youtu.be")
                        || audioSource.toLowerCase().contains("soundcloud.com"))){
                    builder.appendString("Radio");
                    if(Radio.getStationName(audioSource).toLowerCase().contains("could not connect to: "))
                        for (String s : radioStations.keySet()) {
                            if (radioStations.get(s).equalsIgnoreCase(audioSource)) {
                                builder.appendString("]` " + s + "\n");
                                unreadableRadio = true;
                            }
                        }
                    if(!unreadableRadio)
                        builder.appendString("]` " + Radio.getStationName(audioSource) + "\n");
                }
                else if (duration == null) {
                    builder.appendString("N/A");
                    builder.appendString("]` " + info.getTitle() + "\n");
                }
                else {
                    builder.appendString(duration.getTimestamp());
                    builder.appendString("]` " + info.getTitle() + "\n");
                }
            }
        }

        boolean hasRadioStation = false;
        boolean error = false;
        int totalSeconds = 0;
        for (AudioSource source : queue)
        {
            if(source.getSource().toLowerCase().contains("youtube.com") || source.getSource().toLowerCase().contains("youtu.be")
                    || source.getSource().toLowerCase().contains("soundcloud.com")) {
                AudioInfo info = source.getInfo();
                if (info == null || info.getDuration() == null) {
                    error = true;
                    continue;
                }
                totalSeconds += info.getDuration().getTotalSeconds();
            }
            else
                hasRadioStation = true;
        }

        if(hasRadioStation)
            builder.appendString("\nTotal Queue Time Length: " + AudioTimestamp.fromSeconds(totalSeconds).getTimestamp()
                    + "\n(Total Queue Time Length does not include a radio station's playlist length.)");
        else
            builder.appendString("\nTotal Queue Time Length: " + AudioTimestamp.fromSeconds(totalSeconds).getTimestamp());

        if (error)
            builder.appendString("`An error occured calculating total time. Might not be completely valid.");
        channel.sendMessage(builder.build());
    }

    /**
     * Sends the currently playing song's info to <i>channel<i/> as a message.
     *
     * @param player Used to get the current MusicPlayer.
     * @param channel Used to get the TextChannel to send the message to.
     * @see eNetMusic#onGuildMessageReceived(GuildMessageReceivedEvent)
     */
    private void nowPlaying(MusicPlayer player, TextChannel channel){
        if (player.isPlaying())
        {
            String audioSource = player.getCurrentAudioSource().getSource();
            if(audioSource.toLowerCase().contains("youtube.com") || audioSource.toLowerCase().contains("youtu.be"))
                playerType = "youtube";
            else if(audioSource.contains("soundcloud.com"))
                playerType = "soundcloud";
            else
                playerType = "radio";

            boolean unreadableRadio = false;
            String radioName = "";
            if(playerType.equalsIgnoreCase("radio")){
                if(Radio.getStationName(audioSource).toLowerCase().contains("could not connect to: ")) {
                    String[] parts = audioSource.split("/");
                    if(parts.length > 3)
                        radioName = parts[2];
                    else
                        radioName = parts[0];
                    for (String s : radioStations.keySet())
                        if (radioStations.get(s).equalsIgnoreCase(audioSource))
                            radioName = s;
                    unreadableRadio = true;
                }
                if(!(Radio.getStationName(audioSource).equalsIgnoreCase(audioSource) && Radio.getCurrentSong(audioSource).equalsIgnoreCase(audioSource))
                        && !unreadableRadio)
                    channel.sendMessage(
                            "**Playing:** " + Radio.getStationName(audioSource) + "\n" +
                                    "**Current Song:** " + Radio.getCurrentSong(audioSource));
                else if(unreadableRadio)
                    channel.sendMessage(
                            "**Playing:** " + radioName + "\n" +
                                    "**Current Song:** <Not Available>");
                else
                    channel.sendMessage("Could not get information from current radio station!");
            }
            else {
                AudioTimestamp currentTime = player.getCurrentTimestamp();
                AudioInfo info = player.getCurrentAudioSource().getInfo();
                if (info.getError() == null) {
                    channel.sendMessage(
                            "**Playing:** " + info.getTitle() + "\n" +
                                    "**Time:**    [" + currentTime.getTimestamp() + " / " + info.getDuration().getTimestamp() + "]");
                } else {
                    channel.sendMessage(
                            "**Playing:** Info Error. Known source: " + player.getCurrentAudioSource().getSource() + "\n" +
                                    "**Time:**    [" + currentTime.getTimestamp() + " / (N/A)]");
                }
            }
        }
        else
            channel.sendMessage("The player is not currently playing anything!");
    }

    /**
     * Skips the currently playing song if there is one playing and sends a
     * message to <i>channel<i/> that the song has/hasn't been skipped.
     *
     * @param player Used to get the current MusicPlayer.
     * @param channel Used to get the TextChannel to send the message to.
     * @see eNetMusic#onGuildMessageReceived(GuildMessageReceivedEvent)
     */
    private void skipCurrentSong(MusicPlayer player, TextChannel channel){
        player.skipToNext();
        channel.sendMessage("Skipped the current song.");
    }

    /**
     * Toggles the current MusicPlayer to repeat its playlist and sends
     * a message to <i>channel</i> stating if it is on or off.
     *
     * @param player Used to get the current MusicPlayer.
     * @param channel Used to get the TextChannel to send the message to.
     * @see eNetMusic#onGuildMessageReceived(GuildMessageReceivedEvent)
     */
    private void repeatPlayer(MusicPlayer player, TextChannel channel){
        if (player.isRepeat())
        {
            player.setRepeat(false);
            channel.sendMessage("The player has been set to **not** repeat.");
        }
        else
        {
            player.setRepeat(true);
            channel.sendMessage("The player been set to repeat.");
        }
    }

    /**
     * Toggles the current MusicPlayer to shuffle its playlist and sends
     * a message to <i>channel</i> stating if it is on or off.
     *
     * @param player Used to get the current MusicPlayer.
     * @param channel Used to get the TextChannel to send the message to.
     * @see eNetMusic#onGuildMessageReceived(GuildMessageReceivedEvent)
     */
    private void shufflePlayer(MusicPlayer player, TextChannel channel){
        if (player.isShuffle())
        {
            player.setShuffle(false);
            channel.sendMessage("The player has been set to **not** shuffle.");
        }
        else
        {
            player.setShuffle(true);
            channel.sendMessage("The player been set to shuffle.");
        }
    }

    /**
     * Removes the specified song from the playlist based on the argument provided.
     * If no argument is given, the function will remove the last added
     * song if possible. Valid arguments are: the position of the song in
     * the playlist's queue (number), audio links, or a search term.
     *
     * Sends a message to <i>channel</i> notifying if the song was removed.
     *
     * @param player Used to get the current MusicPlayer.
     * @param channel Used to get the TextChannel to send the message to.
     * @param commandline Used to get the arguments of the command.
     * @see eNetMusic#onGuildMessageReceived(GuildMessageReceivedEvent)
     */
    private void removeFromQueue(MusicPlayer player, TextChannel channel, String commandline){
        if(commandline.equalsIgnoreCase("/remove")) {
            if(player.getAudioQueue().size() < 1) {
                channel.sendMessage("Nothing to remove in the queue.");
                return;
            }
            player.getAudioQueue().remove(player.getAudioQueue().size()-1);
            channel.sendMessage("Removed last song in the queue.");
            return;
        }

        String source;
        int oldSize = player.getAudioQueue().size();

        boolean foundStation = false;
        if(commandline.substring(("/remove").length()).length() > 1) {
            try {
                int spot = Integer.parseInt(commandline.substring(("/remove ").length())) - 1;
                String name = player.getAudioQueue().get(spot).getSource();
                player.getAudioQueue().remove(spot);
                channel.sendMessage("Removed **" + name + "** from the queue!");
                return;
            } catch (Exception e) {
                if (commandline.toLowerCase().contains("youtube.com") || commandline.toLowerCase().contains("youtu.be")
                        || commandline.toLowerCase().contains("soundcloud.com"))
                    source = commandline.substring("/remove ".length());
                else if (commandline.substring(("/remove ").length()).contains("-radio")) {
                    if (commandline.substring(("/remove ").length()).equalsIgnoreCase("-radio")
                            || commandline.substring(("/remove ").length()).equalsIgnoreCase("-radio ")
                            || commandline.substring("/remove -radio ".length()).equalsIgnoreCase("default")) {
                        commandline = "/remove -radio http://maxxhits.us:8080/maxxhits_mp3";
                        foundStation = true;
                    } else if (commandline.contains(".") && commandline.substring(commandline.indexOf(".")).length() > 1) {
                        foundStation = true;
                    } else
                        for (String s : radioStations.keySet())
                            if (s.toLowerCase().contains(commandline.substring("/remove -radio ".length()).toLowerCase())) {
                                commandline = "/remove -radio " + radioStations.get(s);
                                foundStation = true;
                                break;
                            }
                    if (!foundStation)
                        commandline = "/remove -radio http://maxxhits.us:8080/maxxhits_mp3";
                    source = commandline.substring("/remove -radio ".length());
                } else {
                    commandline = "/remove " + "https://www.youtube.com/watch?v=" + getVideoId(commandline.substring("/remove ".length()));
                    source = "https://www.youtube.com/watch?v=" + getVideoId(commandline.substring("/remove ".length()));
                }
            }

            int index = -1;
            for (int i = 0; i < player.getAudioQueue().size(); i++) {
                if (player.getAudioQueue().get(i).getSource().contains(source)) {
                    index = i;
                }
            }
            if (index == -1) {
                channel.sendMessage("The specified song was not found in the queue!");
                return;
            }

            AudioSource removedSong = player.getAudioQueue().get(index);

            if (removedSong.getSource().toLowerCase().contains("youtube.com") || removedSong.getSource().toLowerCase().contains("youtu.be"))
                playerType = "youtube";
            else if (removedSong.getSource().contains("soundcloud.com"))
                playerType = "soundcloud";
            else
                playerType = "radio";

            player.getAudioQueue().remove(index);
            if (oldSize == player.getAudioQueue().size())
                channel.sendMessage("Could not remove **" + commandline.substring(("/remove ").length()) + "** from the queue!");
            else {
                if (playerType.equalsIgnoreCase("youtube") || playerType.equalsIgnoreCase("soundcloud"))
                    channel.sendMessage("Removed **" + removedSong.getInfo().getTitle() + "** from the queue!");
                else if (playerType.equalsIgnoreCase("radio")) {
                    String radioName = "";
                    boolean unreadableRadio = false;
                    if (Radio.getStationName(removedSong.getSource()).toLowerCase().contains("could not connect to: ")) {
                        String[] parts = removedSong.getSource().split("/");
                        if (parts.length > 3)
                            radioName = parts[2];
                        else
                            radioName = parts[0];
                        for (String s : radioStations.keySet())
                            if (radioStations.get(s).equalsIgnoreCase(removedSong.getSource()))
                                radioName = s;
                        unreadableRadio = true;
                    }
                    if (!(Radio.getStationName(removedSong.getSource()).equalsIgnoreCase(removedSong.getSource())
                            && Radio.getCurrentSong(removedSong.getSource()).equalsIgnoreCase(removedSong.getSource()))
                            && !unreadableRadio)
                        channel.sendMessage("Removed **" + Radio.getStationName(removedSong.getSource()) + "** from the queue!");
                    else if (unreadableRadio)
                        channel.sendMessage("Removed **" + radioName + "** from the queue!");
                    else
                        channel.sendMessage("Removed **" + source + "** from the queue!");
                }
            }
        }
    }

    /**
     * Clears the current playlist if possible and sends a message to
     * <i>channel</i> saying if it was cleared.
     *
     * @param player Used to get the current MusicPlayer.
     * @param channel Used to get the TextChannel to send the message to.
     * @see eNetMusic#onGuildMessageReceived(GuildMessageReceivedEvent)
     */
    private void clearPlayList(MusicPlayer player, TextChannel channel){
        player.getAudioQueue().clear();
        channel.sendMessage("The current queue has been cleared.");
    }

    /**
     * Resets the MusicPlayer by stopping the current song and removing
     * all songs in the playlist (including the stopped song). Then sends
     * a message to <i>channel</i> to notify that it has been reset.
     *
     * @param player Used to get the current MusicPlayer.
     * @param manager Used to get the current SendingHandler and reset it.
     * @param channel Used to get the TextChannel to send the message to.
     * @see eNetMusic#onGuildMessageReceived(GuildMessageReceivedEvent)
     */
    private void resetPlayer(MusicPlayer player, AudioManager manager, TextChannel channel){
        player.stop();
        player = new MusicPlayer();
        player.setVolume(playerVolume);
        manager.setSendingHandler(player);
        channel.sendMessage("Music player has been completely reset.");
    }

    /**
     * Pauses the current playback and sends a message to <i>channel</i>
     * saying it has been paused.
     *
     * @param player Used to get the current playback of MusicPlayer.
     * @param channel Used to get the TextChannel to send the message to.
     * @see eNetMusic#onGuildMessageReceived(GuildMessageReceivedEvent)
     */
    private void pausePlayer(MusicPlayer player, TextChannel channel){
        player.pause();
        channel.sendMessage("Playback has been paused.");
    }

    /**
     * Stops current playback completely and sends a message to <i>channel</i>
     * saying it has been stopped.
     *
     * @param player Used to get the current playback of MusicPlayer.
     * @param channel Used to get the TextChannel to send the message to.
     * @see eNetMusic#onGuildMessageReceived(GuildMessageReceivedEvent)
     */
    private void stopPlayer(MusicPlayer player, TextChannel channel){
        player.stop();
        channel.sendMessage("Playback has been completely stopped.");
    }

    /**
     * Restarts current song if playing, otherwise starts last played song if possible
     * and sends a message to <i>channel</i> saying it has been restarted.
     *
     * @param player Used to get the current song of MusicPlayer.
     * @param channel Used to get the TextChannel to send the message to.
     * @see eNetMusic#onGuildMessageReceived(GuildMessageReceivedEvent)
     */
    private void restartPlayer(MusicPlayer player, TextChannel channel){
        if (player.isStopped())
        {
            if (player.getPreviousAudioSource() != null)
            {
                player.reload(true);
                channel.sendMessage("The previous song has been restarted.");
            }
            else
            {
                channel.sendMessage("The player has never played a song, so it cannot restart a song.");
            }
        }
        else
        {
            player.reload(true);
            channel.sendMessage("The currently playing song has been restarted!");
        }
    }

    /**
     * Mutes the current playback and sends a message to <i>channel</i>
     * saying it has been muted or that it was already muted.
     *
     * @param player Used to get the current playback of MusicPlayer.
     * @param channel Used to get the TextChannel to send the message to.
     * @see eNetMusic#onGuildMessageReceived(GuildMessageReceivedEvent)
     */
    private void mutePlayer(MusicPlayer player, TextChannel channel){
        player.setVolume(0);
        if(!muted)
            channel.sendMessage("Playback muted!");
        else
            channel.sendMessage("Playback already muted!");
        muted = true;
    }

    /**
     * Unmutes the current playback and sends a message to <i>channel</i>
     * saying it has been unmuted or that it was already unmuted.
     *
     * @param player Used to get the current playback of MusicPlayer.
     * @param channel Used to get the TextChannel to send the message to.
     * @see eNetMusic#onGuildMessageReceived(GuildMessageReceivedEvent)
     */
    private void unmutePlayer(MusicPlayer player, TextChannel channel){
        player.setVolume(playerVolume);
        if(muted)
            channel.sendMessage("Playback un-muted!");
        else
            channel.sendMessage("Playback already un-muted!");
        muted = false;}

    /**
     * Gets the current volume and sends it to <i>channel</i>
     * as a message.
     *
     * @param channel Used to get the TextChannel to send the message to.
     * @see eNetMusic#onGuildMessageReceived(GuildMessageReceivedEvent)
     */
    private void getVolume(TextChannel channel){channel.sendMessage("**Current Volume:** " + df.format(playerVolume * 100) + "%");}

    /**
     * Sets the volume of the current MusicPlayer and then sends it to <i>channel</i>
     * as a message.
     *
     * @param player Used to get the current MusicPlayer.
     * @param channel Used to get the TextChannel to send the message to.
     * @param commandline Used to get the arguments of the command.
     * @see eNetMusic#onGuildMessageReceived(GuildMessageReceivedEvent)
     */
    private void setVolume(MusicPlayer player, TextChannel channel, String commandline){
        try {
            if(Float.parseFloat(commandline.substring(8)) > 100)
                commandline = commandline.substring(0,8) + "100";
            if(Float.parseFloat(commandline.substring(8)) < 0)
                commandline = commandline.substring(0,8) + "0";
            playerVolume = (Float.parseFloat(commandline.substring(8)) / 100);
            player.setVolume(playerVolume);
            muted = false;
            if(commandline.equalsIgnoreCase("/volume 0") || Integer.parseInt(commandline.substring(8)) < 1)
                channel.sendMessage("**Volume Set to:** 0" + df.format(playerVolume * 100) + "%");
            else
                channel.sendMessage("**Volume Set to:** " + df.format(playerVolume * 100) + "%");
        }catch (Exception e){channel.sendMessage("'" + commandline.substring(8) + "' is not a valid number!");}
    }

    /**
     * Function that gets called if there was any sort of error with the
     * commands from onGuildMessageReceived().
     *
     * Prints the StackTrace of <i>e</i> and notifies both the user
     * of the command and the bot managers of the guild.
     *
     * @param e General Exception that occurred with the command.
     * @param guild Used to get the current guild where the Exception occurred.
     * @param channel Used to get the channel of the command that caused the Exception.
     * @param message Used to get the full message that caused the Exception.
     * @see eNetMusic#onGuildMessageReceived(GuildMessageReceivedEvent)
     */
    private void commandException(Exception e, Guild guild, TextChannel channel, Message message){
        channel.sendMessage("An ***ERROR*** occurred when trying to execute command: "
                + message.getContent()
                + "\nA server admin has been notified. Check '/help' in the mean time.");
        e.printStackTrace();
        Role server = null;
        for(Role role : guild.getRoles())
            if(role.getName().equalsIgnoreCase(managerRole)){server = role; break;}
        if(server != null)
            for(User u : guild.getUsersWithRole(server))
                u.getPrivateChannel().sendMessage(
                        "An error occurred when " + message.getAuthor() + " issued command: " + message.getContent()
                                + "\nPlease check the console for the stacktrace.");
    }

    /**
     * Function runs after the '/join' command is entered.
     *
     * Places the bot in the VoiceChannel specified by <i>chanName</i> argument if possible.
     * If the VoiceChannel cannot be joined because it doesn't exist, a message is sent to <i>channel</i>
     * to notify the user of the command.
     *
     * @param guild Used to get the current guild where the Exception occurred.
     * @param channel Used to get the channel of the command that caused the Exception.
     * @param chanName VoiceChannel to join.
     * @see eNetMusic#joinVoice(AudioManager, Guild, TextChannel, Message)
     */
    private void setVoiceChannel(Guild guild, TextChannel channel, String chanName){
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
     * Function that gets called from <i>playNonRadio()<i/> if a search term was used
     * instead of an audio link.
     *
     * @param videoName Search term that is passed into the YouTubeSearch function.
     * @return The video id of the YouTube video received from the videoName parameter.
     * @see YouTubeSearch#videoIdSearch(String)
     * @see eNetMusic#playNonRadio(TextChannel, String)
     */
    private String getVideoId(String videoName){return YouTubeSearch.videoIdSearch(videoName);}

    /**
     * Function that gets called after the commands '/commands','/cmds', or '/help'
     * get used. Sends the user of the command a private message of all the commands usable
     * by the bot.
     *
     * @param u Used to get the user of the command and their private message channel.
     */
    private void sendCommands(User u){
        u.getPrivateChannel().sendMessage(
                "__**Commands:**__\n" +
                "```java\n" +
                "!ALL COMMANDS ARE GUILD WIDE ACTIONS!\n" +
                "/commands | /cmds | /help // Messages the user a list of commands.\n" +
                "/join [VoiceChannel] // If no VoiceChannel is specified, then join the user's channel if possible.\n" +
                "/leave // Leaves the current VoiceChannel if in one.\n" +
                "/play [Search Term | YouTube Link | SoundCloud Link] // Plays YouTube or SoundCloud audio from Term/Link. " +
                    "If no Term/Link is specified, then resume playback if possible.\n" +
                "/play -radio [Station] // Plays radio station from default stations or valid Icecast link.\n" +
                "/list // Gets the current playlist.\n" +
                "/nowplaying // Gets the current song's info if possible.\n" +
                "/skip // Skips the current song.\n" +
                "/repeat // Repeats the playlist if toggled on.\n" +
                "/shuffle // Shuffles the playlist if toggled on.\n" +
                "/reset // Completely clears the playlist and stops playback.\n" +
                "/pause // Pauses playback.\n" +
                "/stop // Stops playback and clears the current song from the queue.\n" +
                "/restart // If a song is currently playing it restarts it, otherwise, it plays the last song played.\n" +
                "/mute // Mutes playback.\n" +
                "/unmute // Unmutes playback.\n" +
                "/volume [Number] // Sets the volume to [Number], otherwise, shows current volume settings.\n" +
                "/remove [Position in Queue | SearchTerm | YouTube Link | SoundCloud Link | -radio Station] " +
                    "// Removes the specified song from the queue. If no argument is given, it removes the last" +
                        "song added to the queue.\n" +
                "/clear // Completely clears the current queue/playlist." +
                "```"
        );
    }

}