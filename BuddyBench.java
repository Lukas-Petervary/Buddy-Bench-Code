package com.github.DingDingWasTaken.DiscordBot;

import discord4j.core.*;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.presence.Activity;
import reactor.core.publisher.*;
import java.time.Duration;
import java.util.*;

@SuppressWarnings({"BlockingMethodInNonBlockingContext", "ConstantConditions"})
public class BuddyBench {
    private static final Map<String, Command> commands = new HashMap<>();

    static {
        //hashmap commands
        commands.put("help", event -> event.getMessage().getChannel().flatMap(channel -> channel.createMessage("bruh just don't need help idiot") ).then() );

        commands.put("search", event -> {
            System.out.println("Search Called;");

            //saves the channel, guild and members
            Mono<MessageChannel> originalChannel = event.getMessage().getChannel();
            Guild guild = event.getGuild().block();
            List<Member> members = guild.getMembers().collectList().block(Duration.ofSeconds(1));


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

            String paramsConcat = "{";
            for(String[] criteria11: params)
                for(String parameters11: criteria11)
                    paramsConcat += parameters11 + ", ";

            System.out.println("Search Parameters:: " +  paramsConcat + "}");

            List<Member> users = search(params, members);
            //prints the parsed criteria and parameters
            StringBuilder str = new StringBuilder();
            for(Member m: users)
                str.append(m.getDisplayName()).append(", ");


            final String output;
            if (str.length() > 1)
                output = str.toString();
            else
                output = "so apparently i cant put an 'empty string' ... like discord dont tell me what to do >:[";

            return originalChannel.flatMap( channel -> channel.createMessage( output.substring(0, Math.min(2000,output.length())) ) ).then();
        } );
    }

    public static void main(String[] args){
        final GatewayDiscordClient client = DiscordClientBuilder.create(args[0]).build().login().block();

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
        List<Member> curTotal = new ArrayList<>();

        //repeat for every parameter
        for(String[] p: param){
            //repeat through all members
            String p1;
            for(Member m: mem){
                p1 = "";
                if (p.length > 1)
                    p1 = p[1];
                System.out.println("param List:: " + p[0] + ", " + p[1] );
                if ( hasParam(p[0], p1 , m) )
                    curTotal.add(m);
            }
        }

        return curTotal;
    }

    public static boolean hasParam(String criteria, String param, Member member) {
        List<Activity> userActivity;
        System.out.println("Searching " + member.getDisplayName() + " for " + criteria + ":" + param);

        switch (criteria) {
            case "inGame" -> {
                userActivity = member.getPresence().block().getActivities();
                System.out.println( userActivity.size() );

                for (Activity acc : userActivity)
                    if (acc.getType().equals(Activity.Type.PLAYING)) {
                        System.out.println("is playing:: ");
                        if (!param.isBlank()){
                            System.out.println("not blank:: ");
                            return (acc.getDetails().get().equalsIgnoreCase(param) );}
                        else{
                            System.out.println("is blank:: ");
                            return true;}
                    }
            }
            case "bySong" -> {
            }
            case "byArtist" -> {
            }
            default -> {
            }
        }
        return false;
    }
}

interface Command {
    Mono<Void> execute(MessageCreateEvent event);
}