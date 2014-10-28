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


package org.mitre.scap.xccdf.util;

import gov.nist.checklists.xccdf.x11.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.xmlbeans.*;
import javax.xml.namespace.QName;


public class PropertyExtensionResolver<T extends XmlObject> {
    public enum Action {
        PREPEND,
        APPEND,
        OVERRIDE,
        REPLACE;
    }

    public static PropertyExtensionResolver<TextWithSubType> getTextWithSubTypeResolver() {
        return textWithSubTypeResolver;
    }

    public static PropertyExtensionResolver<TextType> getTextTypeResolver() {
        return textTypeResolver;
    }

    public static PropertyExtensionResolver<ProfileNoteType> getProfileNoteTypeResolver() {
        return profileNoteTypeResolver;
    }

    public static PropertyExtensionResolver<HtmlTextWithSubType> getHtmlTextWithSubTypeResolver() {
        return htmlTextWithSubTypeResolver;
    }

    public static PropertyExtensionResolver<ReferenceType> getReferenceTypeResolver() {
        return referenceTypeResolver;
    }

    public static PropertyExtensionResolver<IdrefType> getIdrefTypeResolver() {
        return idRefTypeResolver;
    }

    public static PropertyExtensionResolver<IdrefListType> getIdrefListTypeResolver() {
        return idRefListTypeResolver;
    }

    public static PropertyExtensionResolver<URIidrefType> getURIIdRefTypeResolver() {
        return uriIdRefTypeResolver;
    }

    public static PropertyExtensionResolver<XmlObject> getXmlObjectResolver() {
        return simpleXmlObjectResolver;
    }


    public static PropertyExtensionResolver<IdentType> getIdentTypeResolver(){
        return identTypeResolver;
    }


    public static PropertyExtensionResolver<XmlObject> getKeyedAttrResolver( final String attrKey ){
        PropertyExtensionResolver<XmlObject> resolver
          = new PropertyExtensionResolver<XmlObject>(
              new PropertyKeyHandler<XmlObject>(){
                @Override
                public String getKey(XmlObject o) {
                  return o.newCursor().getAttributeText( new QName( attrKey ) );
                }
        });

        return resolver;
    }


    private final PropertyKeyHandler<T> handler;



    private static final PropertyExtensionResolver<TextType> textTypeResolver
        = new PropertyExtensionResolver<TextType>(
            new PropertyKeyHandler<TextType>() {
                @Override
                public String getKey(final TextType o) {
                    return o.getLang();
                }
                @Override
                public boolean isOverride(final TextType o) {
                    return o.getOverride();
                }
        });


    private static final PropertyExtensionResolver<TextWithSubType> textWithSubTypeResolver
        = new PropertyExtensionResolver<TextWithSubType>(
            new PropertyKeyHandler<TextWithSubType>() {
                @Override
                public String getKey(final TextWithSubType o) {
                    return o.getLang();
                }
                @Override
                public boolean isOverride(final TextWithSubType o) {
                    return o.getOverride();
                }
        });

    private static final PropertyExtensionResolver<HtmlTextWithSubType> htmlTextWithSubTypeResolver
        = new PropertyExtensionResolver<HtmlTextWithSubType>(
            new PropertyKeyHandler<HtmlTextWithSubType>() {
                @Override
                public String getKey(final HtmlTextWithSubType o) {
                    return o.getLang();
                }
                @Override
                public boolean isOverride(final HtmlTextWithSubType o) {
                    return o.getOverride();
                }
        });

    private static final PropertyExtensionResolver<ReferenceType> referenceTypeResolver
        = new PropertyExtensionResolver<ReferenceType>(
            new PropertyKeyHandler<ReferenceType>() {
                @Override
                public String getKey(final ReferenceType o) {
                    return o.toString();
                }
                @Override
                public boolean isOverride(final ReferenceType o) {
                    return o.getOverride();
                }
        });

    private static final PropertyExtensionResolver<ProfileNoteType> profileNoteTypeResolver
        = new PropertyExtensionResolver<ProfileNoteType>(
            new PropertyKeyHandler<ProfileNoteType>() {
                @Override
                public String getKey(final ProfileNoteType o) {
                    return o.getLang();
                }
        });


    private static final PropertyExtensionResolver<URIidrefType> uriIdRefTypeResolver
        = new PropertyExtensionResolver<URIidrefType>(
            new PropertyKeyHandler<URIidrefType>() {
                @Override
                public String getKey(final URIidrefType o) {
                    return o.getIdref();
                }
        });


   private static final PropertyExtensionResolver<IdrefType> idRefTypeResolver
    = new PropertyExtensionResolver<IdrefType>(
      new PropertyKeyHandler<IdrefType>() {
        @Override
        public String getKey(final IdrefType o) { return o.getIdref(); }
    });


    private static final PropertyExtensionResolver<IdrefListType> idRefListTypeResolver
    = new PropertyExtensionResolver<IdrefListType>(
        new PropertyKeyHandler<IdrefListType>() {
            @Override
            public String getKey(final IdrefListType o) {
              List list = o.getIdref();
              StringBuffer key = new StringBuffer();
              /*
              final String delim = ",";

              for( int i = 0; i < list.size(); i++ ){
                Object obj = list.get( i );
                key.append( obj.toString());
                if( i != (list.size() - 1) ) key.append( delim );
              }
              return key.toString();
              */

              for( Object obj : list ){
                key.append( obj.toString() );
              }

              return key.toString();
            }
    });


    private static final PropertyExtensionResolver<IdentType> identTypeResolver
      = new PropertyExtensionResolver<IdentType>(
        new PropertyKeyHandler<IdentType>() {
          @Override
          public String getKey(final IdentType o) {
            return o.getSystem();
          }
    });




    private static final PropertyExtensionResolver<XmlObject> simpleXmlObjectResolver
      = new PropertyExtensionResolver<XmlObject>(
          new PropertyKeyHandler<XmlObject>() {
              @Override
              public String getKey(final XmlObject o) { return o.xmlText(); }
      });



    public PropertyExtensionResolver(final PropertyKeyHandler<T> handler) {
        this.handler = handler;
    }

    public <S extends T> void resolve( final List<S> extendingList, final List<S> extendedList, final Action action) {

        if (extendingList == null) {
            throw(new IllegalArgumentException("extendingList"));
        }

        if (extendedList == null) {
            throw(new IllegalArgumentException("extendedList"));
        }


        // This takes care of the REPLACE Action
        if (extendedList.size() == 0) {
            // Do nothing
            return;
        } else if (extendingList.size() == 0
                && extendedList.size() > 0) {
            extendingList.addAll(extendedList);
            return ;
        }

        Map<String, S> temp = new LinkedHashMap<String, S>(extendedList.size()+extendingList.size());

        if (action == Action.PREPEND) {
            // Build an ordered map of elements based on the key returned by the
            // PropertyKeyHandler.  First add the extended elements following
            // with the extending elements.  Duplicate elements will be
            // eliminated within the map with preference given to elements
            // in the extending List.

            for (S extended : extendedList) {
                @SuppressWarnings("unchecked")
                S copy = (S)extended.copy();
                temp.put(handler.getKey(copy), copy);
            }

            // Next the extending Profile
            for (T extending : extendingList) {
                @SuppressWarnings("unchecked")
                S copy = (S)extending.copy();
                temp.put(handler.getKey(copy), copy);
            }
        } else if (action == Action.APPEND) {
            // Build an ordered map of elements based on the key returned by the
            // PropertyKeyHandler.  First add the extending elements following
            // with the extended elements.  Duplicate elements will be
            // eliminated within the map with preference given to elements
            // in the extended List.

            for (T extending : extendingList) {
                @SuppressWarnings("unchecked")
                S copy = (S)extending.copy();
                temp.put(handler.getKey(copy), copy);
            }

            for (T extended : extendedList) {
                @SuppressWarnings("unchecked")
                S copy = (S)extended.copy();
                temp.put(handler.getKey(copy), copy);
            }
        } else if (action == Action.OVERRIDE) {
            for (T extended : extendedList) {
                @SuppressWarnings("unchecked")
                S copy = (S)extended.copy();
                temp.put(handler.getKey(copy), copy);
            }

            for (T extending : extendingList) {
                final String key = handler.getKey(extending);
                XmlObject o = temp.get(key);

                if (o == null || handler.isOverride( extending )) {
                    @SuppressWarnings("unchecked")
                    S copy = (S)extending.copy();
                    temp.put(key, copy);
                }
            }
        }
       
        if( action != Action.REPLACE ){
          extendingList.clear();
          extendingList.addAll(temp.values());
        }
    }
}
