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



package org.mitre.scap.oval.ovaldi;

import java.io.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;


public class IORedirect extends Thread {
    private final boolean         outputVerbose;
    private final BufferedReader  bufIn;
    private final BufferedWriter  bufOut;
    private       String          processName = "Checking Engine";

    public IORedirect(final InputStream in,final OutputStream out, boolean outputVerbose) {
        bufIn = new BufferedReader(new InputStreamReader(in));
        bufOut = new BufferedWriter(new OutputStreamWriter(out));
        this.outputVerbose = outputVerbose;
    }

    public IORedirect(final InputStream in,final BufferedWriter out, boolean outputVerbose) {
        bufIn = new BufferedReader(new InputStreamReader(in));
        bufOut = out;
        this.outputVerbose = outputVerbose;
    }

    public IORedirect(final InputStream in,final Writer out, boolean outputVerbose) {
        bufIn = new BufferedReader(new InputStreamReader(in));
        bufOut = new BufferedWriter(out);
        this.outputVerbose = outputVerbose;
    }
    

    public void setProcessName( String name ){ this.processName = name; }


    @Override
    public void run() {
      char[] buf = new char[256];
      int len   = 0;
      DefaultPrint dp = new DefaultPrint( this.processName );
        try {
          if( this.outputVerbose == false ){
            Thread t = new Thread( dp );
            t.start();
          }

          while ( ( len = bufIn.read( buf ) ) != -1 ){
            if( this.outputVerbose && (len > 1) )  bufOut.write( buf, 0, len );
            else ;
         }
        } catch (IOException e) {
            Logger.getLogger(IORedirect.class.getName()).log(Level.SEVERE, null, e);
        }
        finally {
          try { bufOut.flush(); dp.run = false; }
          catch( Exception ex ){ Logger.getLogger(IORedirect.class.getName()).log(Level.SEVERE, null, ex); }
          //System.out.println("\n** finished executing " + this.processName );
          System.out.println();
        }
    }
}


class DefaultPrint implements Runnable {
  public volatile boolean run = true;
  private static final String[] defaultOutputTable = new String[]{".", "..", "....", "......", "........", "............"};
  private String  processName = "checking engine";

  public DefaultPrint( String processName ){
    this.processName = processName;
  }

  @Override
  public void run() {
    int index = 0;
    while( run == true ){
      System.out.printf( "** executing %s%s                            \r", this.processName, this.defaultOutputTable[index]);
      System.out.flush();
      index = ( ++index % 6 );

      try { Thread.sleep( 150 ); }
      catch( Exception ex ){ }
    }
  }



}