

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
*********************************************/

package stonesoup;

import java.io.*;
import java.awt.image.*;

public class BmpImageDecoder
{
	// Set some program constants.  Note that these are derived
	// from wikipedia's "BMP file format" and Window's API
	// documentation.
	
	public static final int FILE_HEADER_SIZE = 12;
	public static final int BITMAP_INFO_SIZE = 40;
	public static final int WIDTH_MAXIMUM = 4000;		// maximum image width and height
	public static final int HEIGHT_MAXIMUM = 4000;		// ensures that width * height * 4 < Interger.MAX_VALUE
	
	// Class instance variables
	
	InputStream inputStream;		// source of BMP file bytes 
	OutputStream outputStream;		// destination of BMP output byte (as gray scale)
	
	BmpFileHeader fileHeader;		// created by readFileHeaders routine
	BITMAPINFOHEADER bitmapInfoHeader;	
	BufferedImage image;			// created by Decode routine
	
	// processing support variables
	
	int bytesPerRow = 0;			// pixel data storage related variables - set by verifyFileHeaders routine
	int paddingPerRow = 0;
	int bytesPerPixel = 1;
	int bitsPerPixel = 8;
	int pixelsPerByte = 1;
	int pixelShift = 0;
	int pixelMask = 0xFF;
	int colorTableSize = 0;			// colorTable size
	
	byte[] intBuffer = new byte[4];
	
	public BmpImageDecoder()
	{
		return;
	}
	
	/*
	 * read()
	 */

	public BufferedImage read(File file) throws FileNotFoundException, IOException
	{
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(file);
			int gap_sz, fsize = fis.available();
			inputStream = fis;
			readFileHeaders(fsize);
			gap_sz = verifyFileHeaders(fileHeader, bitmapInfoHeader, fsize);
			verifySupportedFormat(bitmapInfoHeader);
			if (gap_sz > 0)
			{
				long actualSkip = inputStream.skip(gap_sz);
				if (actualSkip != gap_sz)
				{
					System.out.println("Warning: didn't skip requested amount: " + actualSkip + " " + gap_sz);
				}
			}
			image = readRgbPixelArray() ;
			return image ;
		}
		finally
		{
			if (fis != null) fis.close();
		}
	}

	/*
	 * readFileHeaders()
	 * 
	 * This function reads both the BmpFileHeader and the BITMAPINFOHEADER
	 */

	public void readFileHeaders(int fsize) throws IOException
	{
		byte b0, b1 ;

		if (fsize < FILE_HEADER_SIZE + BITMAP_INFO_SIZE + 2) throwFileFormatException("File header missing");
		
		// Check that the file starts with "BM" which signifies a
		// BMP image.

		b0 = readByte();
		b1 = readByte();
		if (b0 != 0x42 || b1 != 0x4D) throwFileFormatException("Magic 'BM' not found");
		
		fileHeader = readFileHeader();
		if (fileHeader.file_sz < 0 || fileHeader.file_sz > fsize) throwFileFormatException("File size mismatch");
		
		bitmapInfoHeader = readBitmapInfoHeader();
		if (bitmapInfoHeader.header_sz > BITMAP_INFO_SIZE)
		{
			long gap_sz = bitmapInfoHeader.header_sz - BITMAP_INFO_SIZE;
			long actualSkip = inputStream.skip(gap_sz);
			if (actualSkip != gap_sz)
			{
				System.out.println("Warning: didn't skip requested amount: " + actualSkip + " " + gap_sz);
			}
		}
	}
	
	/*
	 * verifyFileHeaders()
	 * 
	 * This function returns gap between color table and start of pixel data.
	 */

	public int verifyFileHeaders(BmpFileHeader bfh, BITMAPINFOHEADER bih, int fsize)
	{
		int bmap_size, hdr_size = 2 + FILE_HEADER_SIZE + bih.header_sz;
		
		// calculate pixel access/storage values
		
		bitsPerPixel = bih.bitspp;

		if (bitsPerPixel <= 8)
		{
			if (bitsPerPixel != 1 && bitsPerPixel != 2 && bitsPerPixel != 4 && bitsPerPixel != 8 ) throwFileFormatException("invalid bits per pixel") ;

			pixelsPerByte = 8 / bitsPerPixel;
			colorTableSize = (1 << bitsPerPixel) * 4;
			if (bih.ncolors != 0)
			{
				if (bih.ncolors * 4 > colorTableSize) throwFileFormatException("invalid number of colors");					
				colorTableSize = bih.ncolors * 4;
			}
			if (bitsPerPixel < 8)
			{
				pixelShift = bitsPerPixel;
				pixelMask = (1 << bitsPerPixel) - 1;
			}	
		}
		else
		{
			if (bitsPerPixel != 16 && bitsPerPixel != 24 && bitsPerPixel != 32) throwFileFormatException("invalid bits per pixel");
			bytesPerPixel = bih.bitspp / 8;
			if (bih.ncolors != 0) throwFileFormatException("invalid number of colors");
		}
		
		if (bih.nimpcolors != 0 && bih.nimpcolors * 4 > colorTableSize) throwFileFormatException("invalid number of important colors");
		
		// verify valid width and height
		
		if (bih.width > WIDTH_MAXIMUM || bih.height > HEIGHT_MAXIMUM) throwFileFormatException("invalid width or height");
		
		// Calculate bytesPerRow values and verify bitmap size
		
		bytesPerRow = (bih.width * bytesPerPixel) / pixelsPerByte;
		paddingPerRow = bytesPerRow % 4;
		if (paddingPerRow > 0)
		{
			paddingPerRow = 4 - paddingPerRow;
			bytesPerRow += paddingPerRow;
		}
		bmap_size = bih.height * bytesPerRow;
		if (bih.bmp_bytesz != 0 && bih.bmp_bytesz != bmap_size) throwFileFormatException("invalid bitmap size");
		
		// verify file size and pixelDataOffset
		
		if (bfh.file_sz > fsize) throwFileFormatException("File size in header invalid");
		fsize = bfh.file_sz;	// extra data in file is allowed
		
		if (bfh.pixelArrayOffset < hdr_size + colorTableSize) throwFileFormatException("Pixel data offset invalid");
			
		fsize -= hdr_size + colorTableSize;
		if (fsize < bmap_size) throwFileFormatException("File size too small for bitmap");

		return bfh.pixelArrayOffset - colorTableSize - hdr_size;
	}
	
	/*
	 * verifySupportedFormat()
	 * 
	 * currently only 24 bits per pixel is supported
	 */

	public void verifySupportedFormat(BITMAPINFOHEADER bih)
	{
		if (bih.compress_type != 0) throwUnsupportFormatException("compressed pixel data");
		if (bih.bitspp != 24) throwUnsupportFormatException("must be 24 bits per pixel");
	}
	
	public BufferedImage readRgbPixelArray() throws IOException
	{
		ColorModel cm;
			
		WritableRaster raster;
		BufferedImage image;
		
		int w, h, rgb, pad = 0;
		int[] colors;
		w = bitmapInfoHeader.width;
		h = bitmapInfoHeader.height;
		pad = (w * 3) % 4;
		if (pad > 0) pad = 4 - pad;
		cm = ColorModel.getRGBdefault();
		colors = new int[cm.getNumComponents()];

		raster = cm.createCompatibleWritableRaster(w, h);
		image = new BufferedImage(cm, raster, true, null);
		
		for (int y = h-1; y > 0; y--)
		{
			for (int x = 0; x < w; x++)
			{
				rgb = readRGB ();
				colors[0] = cm.getRed(rgb);
				colors[1] = cm.getGreen(rgb);
				colors[2] = cm.getBlue(rgb);
				if (colors.length >= 4) colors[3] = 0xFF;	// full alpha
				raster.setPixel(x, y, colors);
			}
			if (pad > 0) readInt(pad);
		}
		
		return image;
	}
	
	// write routine for Bmp bitmap files: modeled (loosely) on the java.imageIO.write routine	
	//   reuses BITMAPINFOHEADER (hres and vres) and BmpFileHeader (applParam1 and applParam2)

	
	public void write (RenderedImage bi, String fname) throws FileNotFoundException, IOException
	{
		write (bi, new File (fname));
	}

	public void write(RenderedImage bi, File file) throws FileNotFoundException, IOException
	{
		BmpFileHeader bfh;
		BITMAPINFOHEADER bih;
		
		// If bitmapInfoHeader has not been set, then set values normally copied
		// from input BMP file
		
		if (bitmapInfoHeader == null)
		{
			bfh = new BmpFileHeader();
			bfh.applParam1 = 0;
			bfh.applParam2 = 0;
			bih = new BITMAPINFOHEADER();
			bih.hres = 4724;	// pixels per meter (LCD screen)
			bih.vres = 4724;	
		}
		
		// Otherwise use the existing fileHeader and bitmapInfoHeader
		
		else
		{
			bfh = fileHeader.clone();
			bih = bitmapInfoHeader.clone();
		}
		
		write(bi, file, bfh, bih);
	}
	
	public void write (RenderedImage bi, File file, BmpFileHeader bfh, BITMAPINFOHEADER bih) throws FileNotFoundException, IOException
	{	// Requires that image file has been loaded
		
		ColorModel cm;
		int bpr, bmsz, hsz = FILE_HEADER_SIZE + BITMAP_INFO_SIZE + 2;
		
		bih.header_sz = BITMAP_INFO_SIZE;
		bih.width = bi.getWidth();
		bih.height = bi.getHeight();
		bih.nplanes = 1;
		bih.bitspp = 8;
		bih.ncolors = 0;	// use default
		bih.nimpcolors  = 0;
		bih.compress_type = 0;
		
		bfh.pixelArrayOffset = hsz + 1024 ;  // file headers + color table (256 * 4)
		bpr = bih.width % 4;		// bytes per row (1 byte per pixel + padding)
		if (bpr > 0) bpr = 4 - bpr;
		bpr += bih.width;
		bmsz = bpr * bih.height;
		bih.bmp_bytesz = 0;		// zero allowed for uncompressed pixel data
		bfh.file_sz = bfh.pixelArrayOffset + bmsz;
		
		FileOutputStream fos = new FileOutputStream(file);
		outputStream = fos;
		writeFileHeader(bfh, bih);
		cm = bi.getColorModel();
		if (cm instanceof IndexColorModel) writeColorTable((IndexColorModel) cm);
		writePixelArray(bi);
		fos.close();
	}
	
	public void writeColorTable(IndexColorModel cm) throws IOException
	{
		byte[] reds, greens, blues;
		int cm_size = cm.getMapSize();
		reds = new byte[cm_size];
		greens = new byte[cm_size];
		blues = new byte[cm_size];
		cm.getReds(reds);
		cm.getGreens(greens);
		cm.getBlues(blues);
		
		for (int i = 0; i < cm_size; i++)
		{
			writeByte(blues[i]);
			writeByte(greens[i]);
			writeByte(reds[i]);
			writeByte((byte) 0);
		}
	}

	public void writePixelArray(RenderedImage image) throws IOException
	{
		ColorModel cm;			
		Raster raster;		
		int w, h, pixel, ncomp, pad = 0;
		int[] colors ;

		w = image.getWidth();
		h = image.getHeight();
		pad = w % 4;		// add padding bytes to ensure multiple of 4
		if (pad > 0) pad = 4 - pad;

		raster = image.getData() ;
		cm = image.getColorModel() ;
		ncomp = cm.getNumComponents();
		colors = new int[ncomp] ;
		
		// Loop through each row.  Note that rows are stored from
		// bottom to top.
		
		for (int y = h-1; y >= 0; y--)
		{
			for (int x = 0; x < w; x++)
			{
				pixel = raster.getSample(x, y, 0);
				cm.getComponents(pixel, colors, 0);
				writeByte((byte) (pixel & 0xFF));
			}
			if (pad > 0) writeInt(pad, 0);
		}
	}

	public void writeFileHeader(BmpFileHeader bfh, BITMAPINFOHEADER bih) throws IOException
	{
		// Write 'BM' magic
		
		writeByte((byte) 0x42);
		writeByte((byte) 0x4D);
		
		writeFileHeader(bfh);
		writeBitmapInfoHeader(bih);		
	}
	
	public BmpFileHeader readFileHeader() throws IOException
	{
		BmpFileHeader bfh = new BmpFileHeader();
		bfh.file_sz = readInt();
		bfh.applParam1 = readShort();
		bfh.applParam1 = readShort();
		bfh.pixelArrayOffset = readInt();
		if (bfh.file_sz <= 0 || bfh.pixelArrayOffset <= 0) throwFileFormatException("Bad file header");
		return bfh;	
	}

	public void writeFileHeader(BmpFileHeader bfh) throws IOException
	{
		writeInt(bfh.file_sz);
		writeShort(bfh.applParam1);
		writeShort(bfh.applParam1);
		writeInt(bfh.pixelArrayOffset);
	}
	
	public BITMAPINFOHEADER readBitmapInfoHeader() throws IOException
	{
		BITMAPINFOHEADER bih = new BITMAPINFOHEADER ();
		
		bih.header_sz = readInt();
		
		if (bih.header_sz < 40)
		{
			if (bih.header_sz == 12) throwUnsupportFormatException("Header BITMAPCOREINFO");
			throwFileFormatException("BitmapInfoHeader size too small");
		}

		if (bih.header_sz > 40)
		{
			// check for BITMAPV4HEADER and BITMAPV5HEADER
			if (bih.header_sz != 108 && bih.header_sz != 108) throwUnsupportFormatException("Unknown BitmapInfoHeader size");
		}
	
		bih.width = readInt();
		bih.height = readInt();
		bih.nplanes = readShort();
		bih.bitspp = readShort();
		bih.compress_type = readInt();
		bih.bmp_bytesz = readInt();
		bih.hres = readInt();
		bih.vres = readInt();
		bih.ncolors = readInt();
		bih.nimpcolors = readInt();
		
		// ensure java int and short (signed) have positive values
		
		if (bih.nplanes != 1 || bih.width <= 0 || bih.height <= 0 || bih.bitspp <= 0) throwFileFormatException("Bad bitmap header");
		if (bih.bmp_bytesz < 0 || bih.ncolors < 0 || bih.nimpcolors < 0) throwFileFormatException("Bad bitmap header");
	
		return bih;
	}
	
	public void writeBitmapInfoHeader(BITMAPINFOHEADER bih) throws IOException
	{		
		writeInt(bih.header_sz);
		writeInt(bih.width) ;
		writeInt(bih.height);
		writeShort(bih.nplanes);
		writeShort(bih.bitspp);
		writeInt(bih.compress_type);
		writeInt(bih.bmp_bytesz);
		writeInt(bih.hres);
		writeInt(bih.vres);
		writeInt(bih.ncolors);
		writeInt(bih.nimpcolors);
	}
	
	// Read little-endian integers - inputStream must be valid
	
	public int readInt() throws IOException
	{
		return readInt(4);
	}

	public int readRGB() throws IOException
	{
		return readInt(3);
	}

	public int readInt(int bcnt) throws IOException
	{
		int rcnt = 0;
		int rval = 0;
		
		if ((rcnt = inputStream.read(intBuffer, 0, bcnt)) < 0 || rcnt != bcnt) throwEndOfDataException();
		
		for (int i = bcnt-1; i >= 0; i--)
		{
			rval <<= 8;
			rval |= intBuffer[i] & 0xFF;
		}
		
		return rval;
	}

	public short readShort() throws IOException
	{
		int b0, b1;
		
		b0 = inputStream.read();
		if (b0 < 0) throwEndOfDataException();
		
		b1 = inputStream.read();
		if (b1 < 0) throwEndOfDataException();
		
		return (short) (((b1 << 8) & 0xFF00) | (b0 & 0xFF));
	}

	public byte readByte() throws IOException
	{
		int b0;
	
		b0 = inputStream.read();
		if (b0 < 0) throwEndOfDataException();
		
		return (byte) b0;
	}
	
	// Write little-endian signed integers - outputStream must be valid
	
	public void writeInt(int rval) throws IOException
	{
		writeInt(4, rval);
	}

	public void writeInt(int bcnt, int rval) throws IOException
	{
		for (int i = 0; i < bcnt; i++)
		{
			intBuffer[i] = (byte) (rval & 0xFF);
			rval >>= 8;
		}
		outputStream.write(intBuffer, 0, bcnt);
	}

	public void writeShort(short rval) throws IOException
	{
		byte b0, b1;
		b0 = (byte) (rval & 0xFF);
		b1 = (byte) ((rval >> 8) & 0xFF);
		outputStream.write(b0);
		outputStream.write(b1);
	}
	
	public void writeByte(byte rval) throws IOException
	{
		outputStream.write(rval);
	}
	
	// Exceptions - mapped to existing java.lang Exceptions
	
	public void throwEndOfDataException()
	{
		throw new IllegalArgumentException("BMP file format error: Insufficient Data");
	}
	
	public void throwFileFormatException(String msg)
	{
		throw new IllegalArgumentException("BMP file format error: " + msg);
	}
	
	public void throwUnsupportFormatException(String msg)
	{
		throw new UnsupportedOperationException("BMP file format unsupported: " + msg);
	}
	
	/*
	 * CLASS: BmpFileHeader
	 */
	
	public class BmpFileHeader
	{
		public int file_sz;
		public short applParam1;
		public short applParam2;
		public int pixelArrayOffset;

		public BmpFileHeader clone()
		{
			BmpFileHeader bfh = new BmpFileHeader();
			bfh.file_sz = file_sz;
			bfh.applParam1 = applParam1;
			bfh.applParam1 = applParam1;
			bfh.pixelArrayOffset = pixelArrayOffset;		
			return bfh;
		}
	}
	
	/*
	 * CLASS: BITMAPINFOHEADER
	 * 
	 * Information about storage of color table and pixel data in file
	 */

	public class BITMAPINFOHEADER
	{
		public int header_sz;
		public int width;
		public int height;
		public short nplanes;
		public short bitspp;
		public int compress_type;
		public int bmp_bytesz;
		public int hres;
		public int vres;
		public int ncolors;
		public int nimpcolors;
		
		public BITMAPINFOHEADER clone()
		{
			BITMAPINFOHEADER bih = new BITMAPINFOHEADER();
			bih.header_sz = header_sz;
			bih.width = width;
			bih.height = height;
			bih.nplanes = nplanes;
			bih.bitspp = bitspp;
			bih.bmp_bytesz = bmp_bytesz;
			bih.hres = hres;
			bih.vres = vres;
			bih.ncolors = ncolors;
			bih.nimpcolors = nimpcolors;
			return bih;
		}
	}
}