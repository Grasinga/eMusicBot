package net.grasinga.eNetMusic;

import net.dv8tion.jda.JDA;
import net.dv8tion.jda.JDABuilder;

import javax.security.auth.login.LoginException;
import java.io.*;

/**
 * Class that houses the main function to start the eNetMusic bot.
 *
 * @see eNetMusic
 */
public class StartMusicBot {

    /**
     * Creates and runs the eNetMusic bot.
     *
     * @param args Command line arguments.
     * @see eNetMusic
     */
    public static void main(String[] args)
    {
        try{
            BufferedReader br = new BufferedReader(new FileReader(new File("./BotToken.txt")));
            String token = br.readLine();
            JDA api = new JDABuilder()
                    .setBulkDeleteSplittingEnabled(false)
                    .setBotToken(token)
                    .addListener(new eNetMusic())
                    .buildBlocking();
        }
        catch (IllegalArgumentException e){
            System.out.println("The config was not populated. Please enter a bot token.");
        }
        catch (LoginException e){
            System.out.println("The provided bot token was incorrect. Please provide valid details.");
        }
        catch (InterruptedException e){
            e.printStackTrace();
        }
        catch (FileNotFoundException e){
            System.out.println("Could not find Bot Token file!");
        }
        catch (IOException e){
            System.out.println("Could not read Bot Token file!");
        }
    }
}
