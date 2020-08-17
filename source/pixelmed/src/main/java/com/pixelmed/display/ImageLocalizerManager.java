/* Copyright (c) 2001-2020, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.display;

import com.pixelmed.display.event.FrameSelectionChangeEvent;
import com.pixelmed.display.event.FrameSortOrderChangeEvent;
import com.pixelmed.display.event.SourceImageSelectionChangeEvent;
import com.pixelmed.display.event.StatusChangeEvent;
import com.pixelmed.event.ApplicationEventDispatcher;
import com.pixelmed.event.Event;
import com.pixelmed.event.EventContext;
import com.pixelmed.event.SelfRegisteringListener;
import com.pixelmed.geometry.GeometryOfVolume;
import com.pixelmed.geometry.LocalizerPoster;

import java.util.Vector;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

class ImageLocalizerManager extends LocalizerManager {
	
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/display/ImageLocalizerManager.java,v 1.13 2020/05/16 13:02:13 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(ImageLocalizerManager.class);

	/***/
	protected GeometryOfVolume mainImageGeometry;
	/***/
	protected int mainIndex;				// Already has been mapped through mainImageSortOrder, if present
	/***/
	protected int[] mainImageSortOrder;
	
	/**
	 */
	protected void drawOutlineOnLocalizerReferenceImagePanel() {
		if (referencedImagePanel != null && mainImageGeometry != null) {
			slf4jlogger.debug("drawOutlineOnLocalizer(): setting shape and requesting repaint of reference image panel; mainIndex={}",mainIndex);
			localizerPoster.setLocalizerGeometry((referenceImageGeometry.getGeometryOfSlices())[referenceIndex]);
			Vector shapes = localizerPoster.getOutlineOnLocalizerForThisGeometry((mainImageGeometry.getGeometryOfSlices())[mainIndex]);
			referencedImagePanel.setLocalizerShapes(shapes);
			referencedImagePanel.getParent().validate();	// otherwise sometimes doesn't actually repaint until cursor focus moved away from list of images
			referencedImagePanel.repaint();
		}
	}
	
	/***/
	private OurMainSourceImageSelectionChangeListener mainSourceImageSelectionChangeListener;

	class OurMainSourceImageSelectionChangeListener extends SelfRegisteringListener {
	
		/**
		 * @param	eventContext
		 */
		public OurMainSourceImageSelectionChangeListener(EventContext eventContext) {
			super("com.pixelmed.display.event.SourceImageSelectionChangeEvent",eventContext);
		}
		
		/**
		 * @param	e
		 */
		public void changed(Event e) {
			SourceImageSelectionChangeEvent sis = (SourceImageSelectionChangeEvent)e;
			mainImageSortOrder = sis.getSortOrder();
			mainIndex = sis.getIndex();
			if (mainImageSortOrder != null) {
				mainIndex=mainImageSortOrder[mainIndex];
			}
			mainImageGeometry=sis.getGeometryOfVolume();
			if (mainImageGeometry != null && mainImageGeometry.getGeometryOfSlices() == null) {
				slf4jlogger.debug("OurMainSourceImageSelectionChangeListener.changed(): getGeometryOfSlices() is null, so not using mainImageGeometry");
				mainImageGeometry=null;
				ApplicationEventDispatcher.getApplicationEventDispatcher().processEvent(new StatusChangeEvent("Selected image does not contain necessary geometry for localization."));
			}
		}
	}
	
	/***/
	//private EventContext mainSourceImageSelectionContext;
	
	/*
	 * @param	mainSourceImageSelectionContext
	 */
	public void setMainSourceImageSelectionContext(EventContext mainSourceImageSelectionContext) {
		//this.mainSourceImageSelectionContext=mainSourceImageSelectionContext;
		if (mainSourceImageSelectionChangeListener == null) {
			mainSourceImageSelectionChangeListener = new OurMainSourceImageSelectionChangeListener(mainSourceImageSelectionContext);
		}
		else {
			mainSourceImageSelectionChangeListener.setEventContext(mainSourceImageSelectionContext);
		}
	}
	
	/***/
	private OurMainImageFrameSelectionChangeListener mainImageFrameSelectionChangeListener;

	class OurMainImageFrameSelectionChangeListener extends SelfRegisteringListener {
	
		/**
		 * @param	eventContext
		 */
		public OurMainImageFrameSelectionChangeListener(EventContext eventContext) {
			super("com.pixelmed.display.event.FrameSelectionChangeEvent",eventContext);
		}
		
		/**
		 * @param	e
		 */
		public void changed(Event e) {
			slf4jlogger.debug("OurMainImageFrameSelectionChangeListener.changed():");
			FrameSelectionChangeEvent fse = (FrameSelectionChangeEvent)e;
			mainIndex = fse.getIndex();
			if (mainImageSortOrder != null) {
				mainIndex=mainImageSortOrder[mainIndex];
			}
			if (slf4jlogger.isTraceEnabled()) slf4jlogger.trace("OurMainImageFrameSelectionChangeListener.changed(): referenceImageGeometry {}",referenceImageGeometry.toString());
			if (slf4jlogger.isTraceEnabled()) slf4jlogger.trace("OurMainImageFrameSelectionChangeListener.changed(): mainImageGeometry {}",mainImageGeometry.toString());
			if (referenceImageGeometry != null && mainImageGeometry != null) {	// don't do anything if no referenced image
				slf4jlogger.debug("OurMainImageFrameSelectionChangeListener.changed(): updating localizer outline");
				drawOutlineOnLocalizerReferenceImagePanel();
			}
		}
	}
	
	/***/
	//private EventContext mainImageFrameSelectionContext;
	
	/*
	 * @param	mainImageFrameSelectionContext
	 */
	public void setMainImageFrameSelectionContext(EventContext mainImageFrameSelectionContext) {
		//this.mainImageFrameSelectionContext=mainImageFrameSelectionContext;
		if (mainImageFrameSelectionChangeListener == null) {
			mainImageFrameSelectionChangeListener = new OurMainImageFrameSelectionChangeListener(mainImageFrameSelectionContext);
		}
		else {
			mainImageFrameSelectionChangeListener.setEventContext(mainImageFrameSelectionContext);
		}
	}
	
	/***/
	private OurMainImageFrameSortOrderChangeListener mainImageFrameSortOrderChangeListener;

	class OurMainImageFrameSortOrderChangeListener extends SelfRegisteringListener {
	
		/**
		 * @param	eventContext
		 */
		public OurMainImageFrameSortOrderChangeListener(EventContext eventContext) {
			super("com.pixelmed.display.event.FrameSortOrderChangeEvent",eventContext);
		}
		
		/**
		 * @param	e
		 */
		public void changed(com.pixelmed.event.Event e) {
			slf4jlogger.debug("OurMainImageFrameSortOrderChangeListener.changed():");
			FrameSortOrderChangeEvent fso = (FrameSortOrderChangeEvent)e;
			mainImageSortOrder = fso.getSortOrder();
			mainIndex = fso.getIndex();
			if (mainImageSortOrder != null) {
				mainIndex=mainImageSortOrder[mainIndex];
			}
			if (referenceImageGeometry != null && mainImageGeometry != null) {	// don't do anything if no referenced image
				slf4jlogger.debug("OurMainImageFrameSortOrderChangeListener.changed(): updating localizer outline");
				drawOutlineOnLocalizerReferenceImagePanel();
			}
		}
	}

	/***/
	//private EventContext mainImageFrameSortOrderContext;
	
	/*
	 * @param	mainImageFrameSortOrderContext
	 */
	public void setMainImageFrameSortOrderContext(EventContext mainImageFrameSortOrderContext) {
		//this.mainImageFrameSortOrderContext=mainImageFrameSortOrderContext;
		if (mainImageFrameSortOrderChangeListener == null) {
			mainImageFrameSortOrderChangeListener = new OurMainImageFrameSortOrderChangeListener(mainImageFrameSortOrderContext);
		}
		else {
			mainImageFrameSortOrderChangeListener.setEventContext(mainImageFrameSortOrderContext);
		}
	}

	public void reset() {
		slf4jlogger.debug("reset():");
		super.reset();
		mainIndex=0;
		mainImageSortOrder=null;
		mainImageGeometry=null;
	}

}

