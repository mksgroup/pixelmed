/* Copyright (c) 2001-2020, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.display.event;

import com.pixelmed.event.Event;
import com.pixelmed.event.EventContext;

/**
 * @author	dclunie
 */
public class GraphicDisplayChangeEvent extends Event {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/display/event/GraphicDisplayChangeEvent.java,v 1.8 2020/01/01 15:48:16 dclunie Exp $";

	private boolean overlays;

	/**
	 * @param	eventContext
	 * @param	overlays
	 */
	public GraphicDisplayChangeEvent(EventContext eventContext,boolean overlays) {
		super(eventContext);
		this.overlays=overlays;
//System.err.println("GraphicDisplayChangeEvent() overlays = "+overlays);
	}

	/***/
	public boolean showOverlays() { return overlays; }

}

