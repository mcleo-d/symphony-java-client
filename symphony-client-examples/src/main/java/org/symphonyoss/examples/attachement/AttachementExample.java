/*
 *
 * Copyright 2016 The Symphony Software Foundation
 *
 * Licensed to The Symphony Software Foundation (SSF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.symphonyoss.examples.attachement;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.client.SymphonyClient;
import org.symphonyoss.client.SymphonyClientFactory;
import org.symphonyoss.client.model.Chat;
import org.symphonyoss.client.model.SymAuth;
import org.symphonyoss.client.services.ChatListener;
import org.symphonyoss.client.services.ChatServiceListener;
import org.symphonyoss.symphony.agent.model.Message;
import org.symphonyoss.symphony.agent.model.MessageSubmission;
import org.symphonyoss.symphony.clients.AuthorizationClient;
import org.symphonyoss.symphony.clients.model.SymAttachmentInfo;
import org.symphonyoss.symphony.clients.model.SymMessage;
import org.symphonyoss.symphony.clients.model.SymUser;
import org.symphonyoss.symphony.pod.model.User;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Example of handling attachments.
 * <p>
 * Will store an incoming file(s) to the bot and echo them back to the sender.
 * <p>
 * <p>
 * <p>
 * REQUIRED VM Arguments or System Properties:
 * <p>
 * -Dsessionauth.url=https://pod_fqdn:port/sessionauth
 * -Dkeyauth.url=https://pod_fqdn:port/keyauth
 * -Dsymphony.agent.pod.url=https://agent_fqdn:port/pod
 * -Dsymphony.agent.agent.url=https://agent_fqdn:port/agent
 * -Dcerts.dir=/dev/certs/
 * -Dkeystore.password=(Pass)
 * -Dtruststore.file=/dev/certs/server.truststore
 * -Dtruststore.password=(Pass)
 * -Dbot.user=bot.user1
 * -Dbot.domain=@domain.com
 * -Duser.call.home=frank.tarsillo@markit.com
 * <p>
 * <p>
 * <p>
 * <p>
 * Created by Frank Tarsillo on 5/15/2016.
 */
public class AttachementExample implements ChatListener, ChatServiceListener {


    private final Logger logger = LoggerFactory.getLogger(AttachementExample.class);
    private SymphonyClient symClient;

    public AttachementExample() {


        init();


    }

    public static void main(String[] args) {


        System.out.println("Attachment example starting...");
        new AttachementExample();

    }

    public void init() {


        try {

            //Create a basic client instance.
            symClient = SymphonyClientFactory.getClient(SymphonyClientFactory.TYPE.BASIC);

            logger.debug("{} {}", System.getProperty("sessionauth.url"),
                    System.getProperty("keyauth.url"));


            //Init the Symphony authorization client, which requires both the key and session URL's.  In most cases,
            //the same fqdn but different URLs.
            AuthorizationClient authClient = new AuthorizationClient(
                    System.getProperty("sessionauth.url"),
                    System.getProperty("keyauth.url"));


            //Set the local keystores that hold the server CA and client certificates
            authClient.setKeystores(
                    System.getProperty("truststore.file"),
                    System.getProperty("truststore.password"),
                    System.getProperty("certs.dir") + System.getProperty("bot.user") + ".p12",
                    System.getProperty("keystore.password"));

            //Create a SymAuth which holds both key and session tokens.  This will call the external service.
            SymAuth symAuth = authClient.authenticate();

            //With a valid SymAuth we can now init our client.
            symClient.init(
                    symAuth,
                    System.getProperty("bot.user") + System.getProperty("bot.domain"),
                    System.getProperty("symphony.agent.agent.url"),
                    System.getProperty("symphony.agent.pod.url")
            );

            //Will notify the bot of new Chat conversations.
            symClient.getChatService().registerListener(this);

            //A message to send when the BOT comes online.
            SymMessage aMessage = new SymMessage();
            aMessage.setFormat(SymMessage.Format.TEXT);
            aMessage.setMessage("Hello master, I'm alive again....");


            //Creates a Chat session with that will receive the online message.
            Chat chat = new Chat();
            chat.setLocalUser(symClient.getLocalUser());
            Set<SymUser> remoteUsers = new HashSet<>();
            remoteUsers.add(symClient.getUsersClient().getUserFromEmail(System.getProperty("user.call.home")));
            chat.setRemoteUsers(remoteUsers);
            chat.registerListener(this);
            chat.setStream(symClient.getStreamsClient().getStream(remoteUsers));

            //Add the chat to the chat service, in case the "master" continues the conversation.
            symClient.getChatService().addChat(chat);


            //Send a message to the master user.
            symClient.getMessageService().sendMessage(chat, aMessage);


        } catch (Exception e) {
            e.printStackTrace();
        }


    }


    public void onChatMessage(Message message) {

    }


    //Chat sessions callback method.
    public void onChatMessage(SymMessage message) {
        if (message == null)
            return;

        logger.debug("TS: {}\nFrom ID: {}\nSymMessage: {}\nSymMessage Type: {}",
                message.getTimestamp(),
                message.getFromUserId(),
                message.getMessage(),
                message.getMessageType());


        //Do we have any attachments in the incoming message
        if (message.getAttachments() != null) {

            List<SymAttachmentInfo> attachmentInfos = message.getAttachments();

            //Check for multiple files
            for (SymAttachmentInfo symAttachmentInfo : attachmentInfos) {

                try {

                    File outFile = new File(symAttachmentInfo.getName());
                    OutputStream out = new FileOutputStream(outFile);
                    out.write(symClient.getAttachmentsClient().getAttachmentData(symAttachmentInfo, message));

                    logger.info("Received file {} with ID: {}", symAttachmentInfo.getName(), symAttachmentInfo.getId());


                } catch (Exception e) {

                    logger.error("Failed to process file..", e);
                }

            }


            //Lets construct the reply.
            SymMessage symMessage = new SymMessage();
            symMessage.setMessage("Echo the files you sent....");
            symMessage.setStreamId(message.getStreamId());
            symMessage.setFormat(SymMessage.Format.TEXT);

            //Post all the incoming files back to the stream.
            List<SymAttachmentInfo> replyAttachmentInfos = new ArrayList<>();
            for (SymAttachmentInfo attachmentInfo : attachmentInfos) {

                try {
                    replyAttachmentInfos.add(
                            symClient.getAttachmentsClient().postAttachment(symMessage.getStreamId(), new File(attachmentInfo.getName()))
                    );
                } catch (Exception e) {

                    logger.error("Could not post file to stream", e);
                }

            }
            //Update all the attachment info details in the reply message.
            symMessage.setAttachments(replyAttachmentInfos);

            //Send the message back..
            Chat chat = symClient.getChatService().getChatByStream(message.getStreamId());

            try {
                if (chat != null)
                    symClient.getMessageService().sendMessage(chat, symMessage);
            }catch (Exception e){
                logger.error("Could not send echo reply to user",e);
            }

        }


    }

    public void onNewChat(Chat chat) {

        chat.registerListener(this);

        logger.debug("New chat session detected on stream {} with {}", chat.getStream().getId(), chat.getRemoteUsers());
    }

    public void onRemovedChat(Chat chat) {

    }

}
