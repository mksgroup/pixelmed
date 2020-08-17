/* Copyright (c) 2001-2020, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.display;

import java.awt.*; 
import java.awt.event.*; 
import java.awt.image.*; 
import java.awt.color.*; 
import java.util.*; 
import java.io.*; 
import javax.swing.*; 
import javax.swing.event.*;

import com.pixelmed.dicom.*;

/**
 * @author	dclunie
 */
class DisplayDicomDirectoryBrowser extends DicomDirectoryBrowser {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/display/DisplayDicomDirectoryBrowser.java,v 1.12 2020/01/01 15:48:14 dclunie Exp $";

	private int frameWidthWanted;
	private int frameHeightWanted;

	/**
	 * @param	list
	 * @param	parentFilePath
	 * @param	frame
	 * @param	frameWidthWanted
	 * @param	frameHeightWanted
	 * @throws	DicomException
	 */
	public DisplayDicomDirectoryBrowser(AttributeList list,String parentFilePath,JFrame frame,
			int frameWidthWanted,int frameHeightWanted) throws DicomException {
		super(list,parentFilePath,frame);
		this.frameWidthWanted=frameWidthWanted;
		this.frameHeightWanted=frameHeightWanted;
	}

	/**
	 * @param	paths
	 */
	protected void doSomethingWithSelectedFiles(Vector paths) {
		DicomBrowser.loadAndDisplayImagesFromDicomFiles(paths,
			getDicomDirectory().getMapOfSOPInstanceUIDToReferencedFileName(getParentFilePath()),
			frameWidthWanted,frameHeightWanted);
	}
}


