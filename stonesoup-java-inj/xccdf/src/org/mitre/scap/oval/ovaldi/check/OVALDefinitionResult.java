/*****************************************************************************
 *  License Agreement
 *
 *  Copyright (c) 2011, The MITRE Corporation
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without modification, are
 *  permitted provided that the following conditions are met:
 *
 *      * Redistributions of source code must retain the above copyright notice, this list
 *        of conditions and the following disclaimer.
 *      * Redistributions in binary form must reproduce the above copyright notice, this
 *        list of conditions and the following disclaimer in the documentation and/or other
 *        materials provided with the distribution.
 *      * Neither the name of The MITRE Corporation nor the names of its contributors may be
 *        used to endorse or promote products derived from this software without specific
 *        prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
 *  EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *  OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 *  SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
 *  OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 *  HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 *  TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 *  EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *
 *  This code was originally developed at the National Institute of Standards and Technology,
 *  a government agency under the United States Department of Commerce, and was subsequently
 *  modified at The MITRE Corporation. Subsequently, the resulting program is not an official
 *  NIST version of the original source code. Pursuant to Title 17, Section 105 of the United
 *  States Code the original NIST code and any accompanying documentation created at NIST are
 *  not subject to copyright protection and are in the public domain.
 *
 *****************************************************************************/



package org.mitre.scap.oval.ovaldi.check;

import gov.nist.checklists.xccdf.x11.MessageType;
import gov.nist.checklists.xccdf.x11.MsgSevEnumType;
import gov.nist.checklists.xccdf.x11.ResultEnumType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.mitre.oval.xmlSchema.ovalCommon5.MessageLevelEnumeration;
import org.mitre.oval.xmlSchema.ovalCommon5.ClassEnumeration;
import org.mitre.oval.xmlSchema.ovalResults5.DefinitionType;
import org.mitre.oval.xmlSchema.ovalResults5.ResultEnumeration;


/**
 * Table 4-2 of NIST SP 800-126 states the following for XCCDF Results -> OVAL Results mappings
 *
 * OVAL Definition Result                             XCCDF Result
 * ----------------------                             --------------
 * ERROR                                              ERROR
 * UNKNOWN                                            UNKNOWN
 * NOT APPLICABLE                                     NOT APPLICABLE
 * NOT EVALUATED                                      NOT CHECKED
 *
 * 
 * Definition Class     Definition Result             XCCDF Result
 * -----------------    -----------------             ------------
 * COMPLIANCE           TRUE                          PASS
 * VULNERABILITY        FALSE                         PASS
 * INVENTORY            TRUE                          PASS
 * PATCH                FALSE                         PASS
 *
 * COMPLIANCE           FALSE                         FAIL
 * VULNERABILITY        TRUE                          FAIL
 * INVENTORY            FALSE                         FAIL
 * PATCH                TRUE                          FAIL
 *
 * @author bworrell
 */


public class OVALDefinitionResult {
    private final String id;
    private final ResultEnumType.Enum result;
    private final List<MessageType> messages;
    private final org.mitre.oval.xmlSchema.ovalCommon5.ClassEnumeration.Enum definitionType;


    public OVALDefinitionResult(final DefinitionType definition, org.mitre.oval.xmlSchema.ovalCommon5.ClassEnumeration.Enum definitionType ) {
        this.id             = definition.getDefinitionId();
        this.definitionType = definitionType;

        switch (definition.getResult().intValue()) {
            case ResultEnumeration.INT_ERROR:
                result = ResultEnumType.ERROR;
                break;
            case ResultEnumeration.INT_NOT_APPLICABLE:
                result = ResultEnumType.NOTAPPLICABLE;
                break;
            case ResultEnumeration.INT_NOT_EVALUATED:
                result = ResultEnumType.NOTCHECKED;
                break;
            case ResultEnumeration.INT_FALSE:
                if( definitionType.equals(ClassEnumeration.VULNERABILITY) ||
                    definitionType.equals(ClassEnumeration.PATCH ) )
                {
                  result = ResultEnumType.PASS;
                } else {
                  result = ResultEnumType.FAIL;
                }
                break;
            case ResultEnumeration.INT_TRUE:
                if( definitionType.equals(ClassEnumeration.COMPLIANCE ) ||
                    definitionType.equals(ClassEnumeration.INVENTORY )  ||
                    definitionType.equals(ClassEnumeration.MISCELLANEOUS ))
                {
                  result = ResultEnumType.PASS;
                } else {
                  result = ResultEnumType.FAIL;
                }
                break;
            case ResultEnumeration.INT_UNKNOWN:
            default:
                result = ResultEnumType.UNKNOWN;
                break;
        }
        if (definition.sizeOfMessageArray() == 0) {
            messages = Collections.emptyList();
        } else {
            messages = new ArrayList<MessageType>(definition.sizeOfMessageArray());
            for (org.mitre.oval.xmlSchema.ovalCommon5.MessageType messageType : definition.getMessageList()) {
                MessageType message = MessageType.Factory.newInstance();
                switch (messageType.getLevel().intValue()) {
                    case MessageLevelEnumeration.INT_DEBUG:
                        message.setSeverity(MsgSevEnumType.INFO);
                        message.setStringValue("DEBUG: "+messageType.getStringValue());
                        break;
                    case MessageLevelEnumeration.INT_ERROR:
                        message.setSeverity(MsgSevEnumType.ERROR);
                        message.setStringValue(messageType.getStringValue());
                        break;
                    case MessageLevelEnumeration.INT_FATAL:
                        message.setSeverity(MsgSevEnumType.ERROR);
                        message.setStringValue("Fatal: "+messageType.getStringValue());
                        break;
                    case MessageLevelEnumeration.INT_INFO:
                        message.setSeverity(MsgSevEnumType.INFO);
                        message.setStringValue(messageType.getStringValue());
                        break;
                    case MessageLevelEnumeration.INT_WARNING:
                        message.setSeverity(MsgSevEnumType.WARNING);
                        message.setStringValue(messageType.getStringValue());
                        break;
                }
            }
        }
    }

    public String getId() {
        return id;
    }

    public ResultEnumType.Enum getResult() {
        return result;
    }

    public List<MessageType> getMessages() {
        return messages;
    }
}
