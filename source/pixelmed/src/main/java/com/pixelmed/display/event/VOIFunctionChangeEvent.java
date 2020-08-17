/* Copyright (c) 2001-2020, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.display.event;

import com.pixelmed.event.Event;
import com.pixelmed.event.EventContext;

/**
 * @author	dclunie
 */
public class VOIFunctionChangeEvent extends Event {

	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/display/event/VOIFunctionChangeEvent.java,v 1.8 2020/01/01 15:48:16 dclunie Exp $";

	private String function;

	/***/
	public static final String linearFunction = "LIN";
	/***/
	public static final String logisticFunction = "LOG";

	/**
	 * @param	eventContext
	 * @param	function
	 */
	public VOIFunctionChangeEvent(EventContext eventContext,String function) {
		super(eventContext);
		this.function=function;
//System.err.println("VOIFunctionChangeEvent() "+function);
	}

	/***/
	public String getFunction() { return function; }

	/***/
	public boolean isLinearFunction() { return function.equals(linearFunction); }

	/***/
	public boolean isLogisticFunction() { return function.equals(logisticFunction); }
}

