package com.pixelmed.convert;

import java.io.EOFException;
import java.io.IOException;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import java.util.ArrayList;

/**
 * <p>A class encapsulating the content of one or more TIFF or BigTIFF Image File Directories (IFDs).</p>
 *
 * @author	dclunie
 */

public class TIFFImageFileDirectories {

	protected TIFFFile f;
	protected ArrayList<TIFFImageFileDirectory> ifdList;
	
	public TIFFFile getFile() { return f; }
	
	public ArrayList<TIFFImageFileDirectory> getListOfImageFileDirectories() { return ifdList; }
	
	public TIFFImageFileDirectories() {
		f = null;
		ifdList = new ArrayList<TIFFImageFileDirectory>();
	}

	public void read(String filename) throws EOFException, IOException, TIFFException {
		f = new TIFFFile(filename);

		long ifdOffset = f.getOffset();
//System.err.println("ifdOffset="+ifdOffset);

		while (ifdOffset != 0) {
			TIFFImageFileDirectory ifd = new TIFFImageFileDirectory();
			ifdOffset = ifd.read(f,ifdOffset);
			ifdList.add(ifd);
		}
	}
	
	public String toString() {
		StringBuffer buf = new StringBuffer();
		if (f != null) {
			buf.append(f);
		}
		int dirNum = 0;
		String prefix = "";
		for (TIFFImageFileDirectory ifd : ifdList) {
			buf.append(prefix);
			buf.append("Directory ");
			buf.append(dirNum++);
			buf.append(": ");
			buf.append(ifd);
			prefix = "\n";
		}
		return buf.toString();
	}

	/**
	 * <p>Read TIFF or BigTIFF input file and extract its Image File Directories.</p>
	 *
	 * <p>Output to stderr mimics libtiff tiffdump tool, except that long lists of values are not truncated with ellipsis.</p>
	 *
	 * @param	arg	the TIFF or BigTIFF file 
	 */
	public static void main(String arg[]) {
		try {
			if (arg.length == 1) {
				TIFFImageFileDirectories ifds = new TIFFImageFileDirectories();
				ifds.read(arg[0]);
				System.err.print(ifds);
			}
			else {
				System.err.println("Error: Incorrect number of arguments");
				System.err.println("Usage: TIFFImageFileDirectories filename");
				System.exit(1);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
}

