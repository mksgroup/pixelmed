/* Copyright (c) 2001-2020, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.event.*;
import java.util.*;
import java.io.*;

import com.pixelmed.utils.JTreeWithAdditionalKeyStrokeActions;

import com.pixelmed.display.ApplicationFrame;	// for main() test

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>The {@link com.pixelmed.dicom.AttributeTreeBrowser AttributeTreeBrowser} class implements a Swing graphical user interface
 * to browse the contents of an {@link com.pixelmed.dicom.AttributeTree AttributeTree}.</p>
 *
 * <p>A main() method is provided for testing and as a utility that reads a DICOM file as a list of attributes and displays them.</p>
 *
 * @author	dclunie
 */
public class AttributeTreeBrowser {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/AttributeTreeBrowser.java,v 1.19 2020/07/26 19:26:08 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(AttributeTreeBrowser.class);

	private JTree tree;
	private AttributeTree treeModel;

	/**
	 * <p>Build and display a graphical user interface view of a tree of attributes.</p>
	 *
	 * <p>Implicitly builds a tree from the attribute list.</p>
	 *
	 * @param	list					the list whose attributes to browse
	 * @param	treeBrowserScrollPane	the scrolling pane in which the tree view of the attributes will be rendered
	 * @throws	DicomException		if error in DICOM encoding
	 */
	public AttributeTreeBrowser(AttributeList list,JScrollPane treeBrowserScrollPane) throws DicomException {
		treeModel=new AttributeTree(list);
		tree=new JTreeWithAdditionalKeyStrokeActions(treeModel);
		treeBrowserScrollPane.setViewportView(tree);
	}
	
	
	/**
	 * <p>Set the sort order to be alphabetical by attribute name, or numerical by group and element tag.</p>
	 *
	 * @param	treeBrowserScrollPane		the scroll pane containing the tree
	 * @param	sortByName			true if sort alphabetically by attribute name
	 */
	public static void setSortByName(JScrollPane treeBrowserScrollPane,boolean sortByName) {
		if (treeBrowserScrollPane != null) {
			JViewport viewport = treeBrowserScrollPane.getViewport();
			if (viewport != null) {
				Component view = viewport.getView();
				if (view != null) {
					if (view instanceof JTree) {
						JTree tree = (JTree)view;
						TreeModel model = tree.getModel();
						if (model != null) {
							if (model instanceof AttributeTree) {
								AttributeTree attributeTree = (AttributeTree)model;
								attributeTree.setSortByName(sortByName);
							}
						}
					}
				}
			}
		}
	}

	/**
	 * <p>For testing.</p>
	 *
	 * <p>Displays a tree browser built from the attributes in the file named on the command line.</p>
	 *
	 * @param	arg DICOM file
	 */
	public static void main(String arg[]) {
		AttributeList list = new AttributeList();
		long startReadTime = System.currentTimeMillis();
		list.setDecompressPixelData(false);	// (001007)
		try {
			list.read(arg[0]);
			slf4jlogger.info("main(): read - done in {} ms",(System.currentTimeMillis()-startReadTime));
		} catch (Exception e) {
			System.err.println(e);
			e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
			System.exit(0);
		}
		
		ApplicationFrame af = new ApplicationFrame();
		JScrollPane scrollPane = new JScrollPane();
		try {
			AttributeTreeBrowser browser = new AttributeTreeBrowser(list,scrollPane);
		}
		catch (DicomException e) {
			e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
			System.exit(0);
		}
		af.getContentPane().add(scrollPane);
		af.pack();
		af.setVisible(true);
	}
}






