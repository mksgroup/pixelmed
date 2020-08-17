/* Copyright (c) 2001-2020, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.test;

import com.pixelmed.dicom.*;

import junit.framework.*;

public class TestDicomNetwork_AllTests extends TestCase {
	
	public static Test suite() {
		TestSuite suite = new TestSuite("All JUnit Tests");
		suite.addTest(TestCStore.suite());
		suite.addTest(TestCStore_ConvertTransferSyntaxes.suite());
		suite.addTest(TestCStore_AETs.suite());
		suite.addTest(TestCMove.suite());
		return suite;
	}
}
