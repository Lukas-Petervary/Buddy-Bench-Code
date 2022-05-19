package com.github.DingDingWasTaken.DiscordBot;

import discord4j.core.*;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.*;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.presence.Activity;
import reactor.core.publisher.*;
import java.time.Duration;
import java.util.*;

@SuppressWarnings({"BlockingMethodInNonBlockingContext", "ConstantConditions", "OptionalGetWithoutIsPresent"})
public class BuddyBench {
    private static final Map<String, Command> commands = new HashMap<>();
    static GatewayDiscordClient client;
    static int maxLength;

    static {
        //hashmap commands
        commands.put("help", event -> event.getMessage().getChannel().flatMap(channel -> channel.createMessage("""
                Using the command `~search`, users can search for users of specific criteria. Just add criteria after the search command to add more search parameters:\s

                 `~maxListSize:"<param>"`:\t\tDefaults output list size to the first 20 people, or to length of `<param>`
                 `~inGame:"<param>"`:\t\t\t\t  Searches for users in a game, or in a game named `<param>`\s
                 `~bySong:"<param>"`:\t\t\t\t  Searches for users listening to Spotify, or a song named `<param>`
                 `~byArtist:"<param>"`:\t\t\t  Searches for users listening to Spotify, or an artist named `<param>`
                 `~hasPartySpace:"<param>"`:\tSearches for users with space in their party, or with `<param>` number of spaces
                 
                 Example: `~search ~maxListSize:"5" ~inGame ~bySong:"Song Name"`""") ).then() );

        commands.put("search", event -> {
            System.out.println("Search Called;");

            //saves the channel, guild and members
            Mono<MessageChannel> originalChannel = event.getMessage().getChannel();
            Guild guild = event.getGuild().block();
            List<Member> members = guild.getMembers().collectList().block(Duration.ofSeconds(1));
            maxLength = 20;

            //saves the initial text and removes excess spaces
            String userInput = event.getMessage().getContent().substring(7).trim();
            System.out.println("User Input :: " + userInput + " ::");

            String[] l1 = Arrays.copyOfRange(userInput.split("~"), 1, userInput.split("~").length);
            String[][] params = new String[l1.length][2];

            //splits via "~" delimiter, then by ":" " delimiter, then removes the second quotation mark
            for(int i = 0; i < l1.length; i++) {
                params[i][0] = l1[i].split(":\"")[0];
                try{params[i][1] = l1[i].split(":\"")[1].trim().replaceAll("\"", "");}
                catch(IndexOutOfBoundsException e){params[i][1] = "";}
            }

            List<Member> users = search(params, members);

            //builds final string to print as message
            StringBuilder str = new StringBuilder();
            users.stream().limit(maxLength).forEach(m -> str.append("\n~ ").append(m.getDisplayName()));


            final String output;
            if (!users.isEmpty())
                output = str.toString();
            else
                output = "There are no users that match the criteria! Make sure the commands are typed correctly. \n(Tip: use ~help for a list of commands)";
            
            return originalChannel.flatMap( channel -> channel.createMessage( output.substring(0, Math.min(2000,output.length())) ) ).then();
        } );
    }

    public static void main(String[] args){
        client = DiscordClientBuilder.create(args[0]).build().login().block();

        client.getEventDispatcher().on(MessageCreateEvent.class)
                .flatMap( event -> Mono.just(event.getMessage().getContent() )
                        .flatMap( content -> Flux.fromIterable( commands.entrySet() )
                                .filter( entry -> content.startsWith('~' + entry.getKey() ) )
                                .flatMap( entry -> entry.getValue().execute(event) )
                                .next()))
                .subscribe();

        System.out.println("Bot Initialized;");

        client.onDisconnect().block();
    }

    //returns string of found applicable members
    public static List<Member> search(String[][] param, List<Member> mem){
        List<Member> curTotal = new ArrayList<>(mem);

        //repeat for all members for every parameter
        for(String[] p: param)
            for (Member m : mem)
                if (!hasParam(p[0].toLowerCase(), p[1], m))
                    curTotal.remove(m);

        return curTotal;
    }

    public static boolean hasParam(String criteria, String param, Member member) {
        List<Activity> userActivity;
        //System.out.println("Searching " + member.getDisplayName() + " for " + criteria + ":" + param);

        switch (criteria) {
            case "ingame" -> {
                userActivity = member.getPresence().block().getActivities();

                for (Activity acc : userActivity)
                    if (acc.getType().equals(Activity.Type.PLAYING)) {
                        if (!param.isBlank())
                            return (acc.getName().equalsIgnoreCase(param));
                        return true;
                    }
            }

            case "bysong" -> {
                userActivity = member.getPresence().block().getActivities();

                for (Activity acc : userActivity)
                    if (acc.getType().equals(Activity.Type.LISTENING)) {
                        if (!param.isBlank()){
                            try{return (acc.getDetails().get().equalsIgnoreCase(param));}catch(NoSuchElementException e){return false;}}
                        return true;
                    }
            }

            case "byartist" -> {
                userActivity = member.getPresence().block().getActivities();

                for (Activity acc : userActivity)
                    if (acc.getType().equals(Activity.Type.LISTENING)) {
                        if (!param.isBlank())
                            try{return (acc.getState().get().toUpperCase().contains(param.toUpperCase()));}catch(NoSuchElementException e){return false;}
                        return true;
                    }
            }

            case "haspartyspace" -> {
                userActivity = member.getPresence().block().getActivities();

                for (Activity acc : userActivity) {
                    try{
                        if(!param.isBlank())
                            return (acc.getMaxPartySize().getAsLong() - acc.getCurrentPartySize().getAsLong() > Integer.parseInt(param) );
                        return (acc.getMaxPartySize().getAsLong() - acc.getCurrentPartySize().getAsLong() > 0);
                    } catch(NoSuchElementException | NumberFormatException e){return false;}
                }
            }

            case "maxlistsize" -> {
                try{maxLength = Integer.parseInt(param); return true;}catch(NumberFormatException e){maxLength = 20; return false;}
            }

            default -> {}
        }
        return false;
    }
}

interface Command {
    Mono<Void> execute(MessageCreateEvent event);
}