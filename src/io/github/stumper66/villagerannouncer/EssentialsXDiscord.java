package io.github.stumper66.villagerannouncer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class EssentialsXDiscord implements DiscordInterface {
    private Boolean isInstalled;
    private boolean checkedApi;
    private @Nullable Object api;
    private @Nullable Object channel;
    private final boolean allowGroupMentions = false;
    private Method methodSendMessage;

    public boolean getIsInstalled(){
        if (isInstalled == null){
            final Plugin plugin = Bukkit.getPluginManager().getPlugin("EssentialsDiscord");
            isInstalled = plugin != null && plugin.isEnabled();
        }

        return isInstalled;
    }

    public void sendMessage(final Component component){
        if (!checkedApi) getApi();

        if (api == null)
            Log.war("api was null");
        else{
            final String text = PlainTextComponentSerializer.plainText().serialize(component);

            try {
                methodSendMessage.invoke(api, channel, text, allowGroupMentions);
            } catch (IllegalAccessException | InvocationTargetException e) {
                Log.war("Error sending message: " + e.getMessage());
            }
            //api.sendMessage(channel, text, allowGroupMentions);
            Log.inf("message was sent");
        }
    }

    public void sendTestMessage(final CommandSender sender){
        if (!checkedApi) getApi();

        if (api == null){
            sender.sendMessage("Unable to get EssentialsXDiscord api");
            return;
        }

        String testMessage = "This is a test message";
        if (sender instanceof Player player)
            testMessage += " from " + player.getName();

        try {
            methodSendMessage.invoke(api, channel, testMessage, allowGroupMentions);
        } catch (IllegalAccessException | InvocationTargetException e) {
            Log.war("Error sending message: " + e.getMessage());
        }
        //api.sendMessage(channel, testMessage, allowGroupMentions);
        sender.sendMessage("Test message sent");
    }

    private void getApi(){
        // using reflection since whenever I tried using their API I couldn't get it to build
        // due to 'com.github.MinnDevelopment:emoji-java:jar:v6.1.0' missing

        try {
            Class<?> clazzDiscordService = Class.forName("net.essentialsx.api.v2.services.discord.DiscordService");
            Class<?> clazzMessageType = Class.forName("net.essentialsx.api.v2.services.discord.MessageType");
            Class<?> clazzDefaultTypes = Class.forName("net.essentialsx.api.v2.services.discord.MessageType$DefaultTypes");

            Field fieldChat = clazzDefaultTypes.getDeclaredField("CHAT");
            channel = fieldChat.get(null);
            api = Bukkit.getServicesManager().load(clazzDiscordService);

            // void sendMessage(final MessageType type, final String message, final boolean allowGroupMentions);
            methodSendMessage = clazzDiscordService.getMethod(
                    "sendMessage",
                    clazzMessageType,
                    String.class,
                    boolean.class
            );
        }
        catch (ClassNotFoundException e){
            Log.war("Unable to find EssentialsXDiscord class: " + e.getMessage());
        } catch (NoSuchFieldException e) {
            Log.war("Unable to find EssentialsXDiscord field: " + e.getMessage());
        } catch (IllegalAccessException e) {
            Log.war("Unable to get EssentialsXDiscord field value: " + e.getMessage());
        } catch (NoSuchMethodException e) {
            Log.war("Unable to get EssentialsXDiscord method: " + e.getMessage());
        }

        /*
        channel = MessageType.DefaultTypes.CHAT;
        api = Bukkit.getServicesManager().load(DiscordService.class);
        */
        checkedApi = true;
    }

    public void reset(){
        isInstalled = null;
        api = null;
        channel = null;
        checkedApi = false;
    }
}
