/* Copyright (c) 2001-2020, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.display.event;

import com.pixelmed.event.Event;
import com.pixelmed.event.EventContext;

/**
 * @author	dclunie
 */
public class ApplyShutterChangeEvent extends Event {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/display/event/ApplyShutterChangeEvent.java,v 1.7 2020/01/01 15:48:16 dclunie Exp $";

	private boolean shutter;

	/**
	 * @param	eventContext
	 * @param	shutter
	 */
	public ApplyShutterChangeEvent(EventContext eventContext,boolean shutter) {
		super(eventContext);
		this.shutter=shutter;
//System.err.println("ApplyShutterChangeEvent() shutter = "+shutter);
	}

	/***/
	public boolean applyShutter() { return shutter; }

}

