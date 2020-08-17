/* Copyright (c) 2001-2020, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.display;

import java.awt.*; 
import java.awt.event.*; 
import java.awt.image.*; 
import javax.swing.*; 
import javax.swing.event.*;

import com.pixelmed.event.ApplicationEventDispatcher;
import com.pixelmed.event.EventContext; 
import com.pixelmed.event.SelfRegisteringListener; 
import com.pixelmed.display.event.SourceSpectrumSelectionChangeEvent; 
import com.pixelmed.dicom.AttributeList;

/**
 * @author	dclunie
 */
class SourceSpectrumSortOrderPanel extends SourceInstanceSortOrderPanel {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/display/SourceSpectrumSortOrderPanel.java,v 1.17 2020/01/01 15:48:15 dclunie Exp $";

	// implement SourceSpectrumSelectionChangeListener ...
	
	private OurSourceSpectrumSelectionChangeListener ourSourceSpectrumSelectionChangeListener;

	class OurSourceSpectrumSelectionChangeListener extends SelfRegisteringListener {
	
		/**
		 * @param	eventContext
		 */
		public OurSourceSpectrumSelectionChangeListener(EventContext eventContext) {
			super("com.pixelmed.display.event.SourceSpectrumSelectionChangeEvent",eventContext);
//System.err.println("SourceSpectrumSortOrderPanel.OurSourceSpectrumSelectionChangeListener():");
		}
		
		/**
		 * @param	e
		 */
		public void changed(com.pixelmed.event.Event e) {
			if (active) {
				SourceSpectrumSelectionChangeEvent sss = (SourceSpectrumSelectionChangeEvent)e;
				byFrameOrderButton.setSelected(true);
				nSrcInstances=sss.getNumberOfSourceSpectra();			// sets in parent, else Slider won't appear when we update it later
				currentSrcInstanceAttributeList=sss.getAttributeList();
				replaceListOfDimensions(buildListOfDimensionsFromAttributeList(currentSrcInstanceAttributeList));
				currentSrcInstanceSortOrder=sss.getSortOrder();
				currentSrcInstanceIndex= sss.getIndex();
				updateCineSlider(1,nSrcInstances,currentSrcInstanceIndex+1);
//System.err.println(System.err.println("SourceSpectrumSortOrderPanel.OurSourceSpectrumSelectionChangeListener.changed(): on exit nSrcInstances = "+nSrcInstances);
//System.err.println(System.err.println("SourceSpectrumSortOrderPanel.OurSourceSpectrumSelectionChangeListener.changed(): on exit currentSrcInstanceIndex = "+currentSrcInstanceIndex);
//System.err.println(System.err.println("SourceSpectrumSortOrderPanel.OurSourceSpectrumSelectionChangeListener.changed(): on exit currentSrcInstanceSortOrder = "+currentSrcInstanceSortOrder);
			}
		}
	}

	/**
	 * @param	typeOfPanelEventContext
	 */
	public SourceSpectrumSortOrderPanel(EventContext typeOfPanelEventContext) {
		super(typeOfPanelEventContext);
		ourSourceSpectrumSelectionChangeListener = new OurSourceSpectrumSelectionChangeListener(typeOfPanelEventContext);
	}
}


