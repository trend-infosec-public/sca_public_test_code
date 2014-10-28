

/********************************************************************
 ** This software (or technical data) was produced for the U. S.
 ** Government under contract 2009-0917826-016 and is subject to 
 ** the Rights in Data-General Clause 52.227-14. Alt. IV (DEC 2007).
 ** 
 ** (c) Copyright 2012 The MITRE Corporation. All Rights Reserved.
 ********************************************************************/

/*******************************************
**
**
** 
** Date: 6/13/11
**
** Base Test Program -- ImageConverter
**
** This Base Test Program takes as input a color BMP file and produces a gray scale version of
** that image.
**
** Variant Test Case Program
**
** Nothing was altered in the base program since it already allows for
** path equivalence. The program allows for the output grayscale
** image to overwrite the original color image. In its current state
** the program isn't exploitable. However, this weakness could be 
** exploited if the program was to be run on a server. In this
** scenario the original image on the server could be deleted by a
** malicious user.
**
** STONESOUP Weakness Class: Tainted Data
** CWE ID: CWE-41
** Variant Spreadsheet Rev #: 1
** Variant Spreadsheet ID: 545
**
** Variant Features:
**   Source Taint: command_line
**   Data Type:    signed_short
**   Control Flow: infinite_loop
**   Data Flow:    array_index_array_content_value
**
*********************************************/

package stonesoup;

import java.io.*;
import java.awt.image.*;

public class ImageConverter
{
	/*
	 * Main routine.  Accepts two arguments: infile, outfile
	 * 
	 * @param args
	 */

	public static void main(String[] args)
	{	
		// Verify that the correct number of arguments have been supplied.

		if (args.length != 2)
		{
			System.err.println ("ERROR: Incorrect number of arguments provided, expecting two arguments: infile outfile");
			System.exit(1);
		}
		
		//***********************************************
		//
		// ARB - Removed the previous validation code which was too good, and added
		// in the following check that makes sure the output file doesn't overwrite
		// the input file.  The problem is that this new code does not guard against
		// the output filename being "./input_filename", so the attacker can provide
		// an input that will cause the original file to be overwritten and destroyed.
		
		if (args[1].matches(args[0]))  // STONESOUP:CROSSOVER_POINT
		{
			System.err.println("ERROR: output filename must be different than the input filename");
			System.exit(1);
		}

		// Validate that the filenames provided is just a filename and not a path to some other
		// directory.  We do this through a blacklist that excludes slashes (both forward and
		// backward) and the double dot sequence.
		//
		//if (args[0].matches("^((.*[:/\\\\].*)|(.*\\.\\..*))$") || args[1].matches("^((.*[:/\\\\].*)|(.*\\.\\..*))$")) //STONESOUP:CROSSOVER_POINT
		//{
		//	System.err.println("ERROR: Filename must not contain a colon, a slash, or a double dot.");
		//	System.exit(1);
		//}
		//
		//***********************************************

		int data1[] = {0, 1};
		String[] data2 = {args[data1[0]], args[data1[1]]};  //STONESOUP:INTERACTION_POINT //STONESOUP:SOURCE_TAINT:COMMAND_LINE //STONESOUP:DATA_FLOW:ARRAY_INDEX_ARRAY_CONTENT_VALUE

		final short A = (short)data2[data1[0]].charAt(0); //STONESOUP:DATA_TYPE:SIGNED_SHORT

		while(A <= 0 || A > 0) //STONESOUP:CONTROL_FLOW:INFINITE_LOOP
		{	
			// Arguments look good so start the conversion.
			
			ImageConverter ic = new ImageConverter();
			ic.convertBmpFileToGrayScale(data2[data1[0]], data2[data1[1]]);

			break;
		}
		
		System.out.println("RESULT: success");
	}

	/*
	 * convertBmpFileToGrayScale
	 */

	public void convertBmpFileToGrayScale(String fname_in, String fname_out)
	{
		BmpImageDecoder bmp = null;
		BufferedImage image = null;
		BufferedImage image_gs = null;
		
		try
		{
			bmp = new BmpImageDecoder();
			image = bmp.read(new File(fname_in));
			image_gs = convertToGrayScale(image);
			bmp.write(image_gs, new File(fname_out)); //STONESOUP:TRIGGER_POINT
		}
		catch (FileNotFoundException ex)
		{
			if (image == null) System.err.println("Unable to open file: " + fname_in + " - " + ex.getMessage());
			else System.err.println("Unable to write file: " + fname_out + " - " + ex.getMessage());
			System.exit(1);
		}
		catch (IOException ex)
		{
			System.err.println(ex.getMessage());
			System.exit(1);
		}
		catch (IllegalArgumentException ex)
		{
			System.err.println(ex.getMessage());
			System.exit(1);
		}
		catch (UnsupportedOperationException ex)
		{
			System.err.println(ex.getMessage());
			System.exit(1);
		}
		catch (Exception ex)
		{
			System.err.println(ex.getMessage());
			System.exit(1);
		}
	}

	/*
	 * convertToGrayScale
	 */

	public static BufferedImage convertToGrayScale(BufferedImage bi)
	{
		byte[] r = new byte[256];
		byte[] g = new byte[256];
		byte[] b = new byte[256];
		  
		int w = bi.getWidth();
		int h = bi.getHeight();
  
		for (int i = 0; i < 256; i++)
		{
			r[i] = (byte) i;
			g[i] = (byte) i;
			b[i] = (byte) i;
		}
		  
		Raster raster = bi.getData();
		ColorModel rcm = bi.getColorModel();
		IndexColorModel cm = new IndexColorModel (8, 256, r, g, b);
		WritableRaster r2 = cm.createCompatibleWritableRaster(w, h);
		  
		int[] pixel = new int[rcm.getNumComponents()];
		int[] gpixel = new int[cm.getNumComponents()];

		for (int x = 0; x < w; x++)
		{
			for (int y = 0; y < h; y++)
			{
				int gv;
				raster.getPixel(x, y, pixel);
				gv = (pixel[0] + pixel[1] + pixel[2]) / 3;
				gv &= 0xFF;
				gpixel[0] = gv;
				gpixel[1] = gv;
				gpixel[2] = gv;
				r2.setPixel(x, y, gpixel);
			}
		}
		  
		BufferedImage bi2 = new BufferedImage(cm, r2, false, null);

		return bi2;
	}

}