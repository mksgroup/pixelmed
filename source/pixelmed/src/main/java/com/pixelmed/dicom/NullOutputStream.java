/* Copyright (c) 2001-2020, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import java.io.*;

/**
 * <p>A class that extends {@link java.io.OutputStream OutputStream} to discard output.</p>
 *
 * <p>Used, for example, to discard output but count byte offsets.</p>
 *
 * @see com.pixelmed.dicom.BinaryInputStream
 *
 * @author	dclunie
 */
public class NullOutputStream extends OutputStream {
	
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/NullOutputStream.java,v 1.8 2020/01/01 15:48:10 dclunie Exp $";

    public void write(int b) throws IOException {}
	public void write(byte b[]) throws IOException {}
	public void write(byte b[],int off,int len) throws IOException {}
	public void flush() throws IOException {}
	public void close() throws IOException {}
}

