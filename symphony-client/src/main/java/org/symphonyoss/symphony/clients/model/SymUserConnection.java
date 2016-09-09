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

package org.symphonyoss.symphony.clients.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.symphonyoss.symphony.pod.model.UserConnection;

/**
 * Created by frank.tarsillo on 9/8/2016.
 */
public class SymUserConnection extends UserConnection{


    public enum Status {
        PENDING_INCOMING("pending_incoming"),

        PENDING_OUTGOING("pending_outgoing"),

        ACCEPTED("accepted"),

        REJECTED("rejected"),

        ALL("all");


        private String value;

        Status(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }


        @JsonCreator
        public static Status fromValue(String text) {
            for (Status b : Status.values()) {
                if (String.valueOf(b.value).equals(text)) {
                    return b;
                }
            }
            return null;
        }

    }

    public static SymUserConnection toSymUserConnection(UserConnection userConnection){

        SymUserConnection symUserConnection = new SymUserConnection();
        symUserConnection.setUserId(userConnection.getUserId());
        symUserConnection.setFirstRequestedAt(userConnection.getFirstRequestedAt());
        symUserConnection.setRequestCounter(userConnection.getRequestCounter());
        symUserConnection.setStatus(userConnection.getStatus());
        symUserConnection.setUpdatedAt(userConnection.getUpdatedAt());

        return symUserConnection;

    }


}
