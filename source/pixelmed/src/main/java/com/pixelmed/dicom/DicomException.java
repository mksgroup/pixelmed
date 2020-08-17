/* Copyright (c) 2001-2020, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

/**
 * @author	dclunie
 */
public class DicomException extends Exception {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/DicomException.java,v 1.11 2020/01/01 15:48:09 dclunie Exp $";

	/**
	 * <p>Constructs a new exception with the specified detail message.</p>
	 *
	 * @param	msg	the detail message
	 */
	public DicomException(String msg) {
		super(msg);
	}
}


