/* Copyright (c) 2001-2020, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.database;

import com.pixelmed.query.QueryResponseGenerator;
import com.pixelmed.query.QueryResponseGeneratorFactory;

class DicomDatabaseQueryResponseGeneratorFactory implements QueryResponseGeneratorFactory {
	/***/
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/database/DicomDatabaseQueryResponseGeneratorFactory.java,v 1.8 2020/01/01 15:48:06 dclunie Exp $";
	/***/
	private DatabaseInformationModel databaseInformationModel;

	DicomDatabaseQueryResponseGeneratorFactory(DatabaseInformationModel databaseInformationModel) {
//System.err.println("DicomDatabaseQueryResponseGeneratorFactory():");
		this.databaseInformationModel=databaseInformationModel;
	}
	
	public QueryResponseGenerator newInstance() {
		return new DicomDatabaseQueryResponseGenerator(databaseInformationModel);
	}
}

