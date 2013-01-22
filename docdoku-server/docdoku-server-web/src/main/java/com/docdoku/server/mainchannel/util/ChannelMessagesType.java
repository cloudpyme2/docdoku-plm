/*
 * DocDoku, Professional Open Source
 * Copyright 2006 - 2013 DocDoku SARL
 *
 * This file is part of DocDokuPLM.
 *
 * DocDokuPLM is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DocDokuPLM is distributed in the hope that it will be useful,  
 * but WITHOUT ANY WARRANTY; without even the implied warranty of  
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the  
 * GNU Affero General Public License for more details.  
 *  
 * You should have received a copy of the GNU Affero General Public License  
 * along with DocDokuPLM.  If not, see <http://www.gnu.org/licenses/>.  
 */

package com.docdoku.server.mainchannel.util;

public class ChannelMessagesType {

    public static final String WEBRTC_INVITE = "WEBRTC_INVITE";
    public static final String WEBRTC_ACCEPT = "WEBRTC_ACCEPT";
    public static final String WEBRTC_REJECT = "WEBRTC_REJECT";
    public static final String WEBRTC_HANGUP = "WEBRTC_HANGUP" ;

    public static final String WEBRTC_OFFER  = "offer";
    public static final String WEBRTC_ANSWER = "answer";
    public static final String WEBRTC_BYE = "bye";
    public static final String WEBRTC_CANDIDATE = "candidate";

    public static final String CHAT_MESSAGE = "CHAT_MESSAGE" ;
    public static final String TEST = "TEST" ;
}