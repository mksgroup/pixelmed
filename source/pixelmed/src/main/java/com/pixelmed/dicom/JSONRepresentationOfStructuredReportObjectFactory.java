/* Copyright (c) 2001-2020, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.dicom;

import com.pixelmed.utils.StringUtilities;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.JsonWriter;

import java.io.*;
import java.util.*;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>A class to encode a representation of a DICOM Structured Report object in a JSON form,
 * suitable for analysis as human-readable text.</p>
 *
 * <p>Note that JSON representations can either contain only the content tree, or also the additional
 * top level DICOM attributes other than those that encode the content tree, as individual
 * DICOM attributes, in the manner of {@link com.pixelmed.dicom.JSONRepresentationOfDicomObjectFactory JSONRepresentationOfDicomObjectFactory}.</p>
 *
 * <p>A typical example of usage to extract just the content tree would be:</p>
 * <pre>
try {
    AttributeList list = new AttributeList();
    list.read("dicomsrfile",null,true,true);
	StructuredReport sr = new StructuredReport(list);
    JsonArray document = new JSONRepresentationOfStructuredReportObjectFactory().getDocument(sr);
    JSONRepresentationOfStructuredReportObjectFactory.write(System.out,document);
} catch (Exception e) {
    slf4jlogger.error("",e);
 }
 * </pre>
 *
 * <p>or to include the top level attributes as well as the content tree, supply the attribute
 * list as well as the parsed SR content to the write() method:</p>
 * <pre>
try {
    AttributeList list = new AttributeList();
    list.read("dicomsrfile",null,true,true);
	StructuredReport sr = new StructuredReport(list);
    JsonArray document = new JSONRepresentationOfStructuredReportObjectFactory().getDocument(sr,list);
    JSONRepresentationOfStructuredReportObjectFactory.write(System.out,document);
} catch (Exception e) {
    slf4jlogger.error("",e);
 }
 * </pre>
 *
 * <p>or even simpler, if there is no further use for the JSON document or the SR tree model:</p>
 * <pre>
try {
    AttributeList list = new AttributeList();
    list.read("dicomsrfile",null,true,true);
    JSONRepresentationOfStructuredReportObjectFactory.createDocumentAndWriteIt(list,System.out);
} catch (Exception e) {
    slf4jlogger.error("",e);
 }
 * </pre>
 *
 * @see com.pixelmed.dicom.StructuredReport
 * @see com.pixelmed.dicom.JSONRepresentationOfDicomObjectFactory
 * @see com.pixelmed.utils.XPathQuery
 * @see org.w3c.dom.Document
 *
 * @author	dclunie
 */
public class JSONRepresentationOfStructuredReportObjectFactory {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/dicom/JSONRepresentationOfStructuredReportObjectFactory.java,v 1.30 2020/06/09 16:01:50 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(JSONRepresentationOfStructuredReportObjectFactory.class);
	
	protected static boolean elideSeparateContinuityOfContent = true;
	
	protected static boolean collapseAttributeValueArrays = true;
	protected static boolean collapseEmptyToNull = true;
	protected static boolean collapseContentTreeArrays = true;
	
	protected static boolean substituteUIDKeywords = true;
	
	protected static boolean useNumberForNumericContentItemValue = false;	// see also discussion at "http://www.npmjs.com/package/fhir#decimal-types"

	//protected static String symbolSignifyingReservedKeyword = "@";	// used in pre-public comment
	protected static String symbolSignifyingReservedKeyword = "_";		// is valid as JavaScript identifier and seems the least risky
	//protected static String symbolSignifyingReservedKeyword = "$";	// is valid as JavaScript identifier, but has meaning for jQuery, and may have special meaning in other languages

	protected static String businessNameToUseForAnonymousContentItems = symbolSignifyingReservedKeyword+"unnamed";

	protected static String reservedKeywordForCodeValueInBusinessNamesFile = symbolSignifyingReservedKeyword+"cv";
	protected static String reservedKeywordForCodingSchemeDesignatorInBusinessNamesFile = symbolSignifyingReservedKeyword+"csd";
	protected static String reservedKeywordForCodeMeaningInBusinessNamesFile = symbolSignifyingReservedKeyword+"cm";
	
	protected static String reservedKeywordForValueTypeInBusinessNamesFile = symbolSignifyingReservedKeyword+"vt";
	protected static String reservedKeywordForRelationshipTypeInBusinessNamesFile = symbolSignifyingReservedKeyword+"rel";
	
	protected static String reservedKeywordForCodingSchemeVersionInBusinessNamesFile = symbolSignifyingReservedKeyword+"csv";
	protected static String reservedKeywordForLongCodeValueInBusinessNamesFile = symbolSignifyingReservedKeyword+"lcv";
	protected static String reservedKeywordForURNCodeValueInBusinessNamesFile = symbolSignifyingReservedKeyword+"urncv";
	protected static String reservedKeywordForContextIdentifierInBusinessNamesFile = symbolSignifyingReservedKeyword+"cid";
	protected static String reservedKeywordForContextUIDInBusinessNamesFile = symbolSignifyingReservedKeyword+"cuid";
	protected static String reservedKeywordForMappingResourceInBusinessNamesFile = symbolSignifyingReservedKeyword+"cmr";
	protected static String reservedKeywordForMappingResourceUIDInBusinessNamesFile = symbolSignifyingReservedKeyword+"cmruid";
	protected static String reservedKeywordForMappingResourceNameInBusinessNamesFile = symbolSignifyingReservedKeyword+"cmrname";
	protected static String reservedKeywordForContextGroupVersionInBusinessNamesFile = symbolSignifyingReservedKeyword+"cvers";
	protected static String reservedKeywordForContextGroupExtensionFlagInBusinessNamesFile = symbolSignifyingReservedKeyword+"cext";
	protected static String reservedKeywordForContextGroupLocalVersionInBusinessNamesFile = symbolSignifyingReservedKeyword+"clocvers";
	protected static String reservedKeywordForContextGroupExtensionCreatorUIDInBusinessNamesFile = symbolSignifyingReservedKeyword+"cextcruid";
	
	protected static String reservedKeywordForObservationDateTimeAttributeInSRFile = symbolSignifyingReservedKeyword+"obsdt";
	protected static String reservedKeywordForObservationUIDAttributeInSRFile = symbolSignifyingReservedKeyword+"obsuid";

	protected static String reservedKeywordForContinuityOfContentAttributeInSRFile = symbolSignifyingReservedKeyword+"cont";
	protected static String reservedKeywordForTemplateMappingResourceAttributeInSRFile = symbolSignifyingReservedKeyword+"tmr";
	protected static String reservedKeywordForTemplateIdentifierAttributeInSRFile = symbolSignifyingReservedKeyword+"tid";
	
	protected static String reservedKeywordForSimplifiedLabelAttributeInSRFile = symbolSignifyingReservedKeyword+"label";
	protected static String reservedKeywordForSimplifiedReferenceToLabelAttributeInSRFile = symbolSignifyingReservedKeyword+"ref";
	
	protected static String reservedKeywordForReferencedSOPClassUIDAttributeInCompositeContentItem = symbolSignifyingReservedKeyword+"class";
	protected static String reservedKeywordForReferencedSOPInstanceUIDAttributeInCompositeContentItem = symbolSignifyingReservedKeyword+"instance";
	protected static String reservedKeywordForReferencedFrameNumberAttributeInCompositeContentItem = symbolSignifyingReservedKeyword+"frame";
	protected static String reservedKeywordForReferencedSegmentNumberAttributeInCompositeContentItem = symbolSignifyingReservedKeyword+"segment";
	protected static String reservedKeywordForPresentationStateSOPClassUIDAttributeInCompositeContentItem = symbolSignifyingReservedKeyword+"prclass";
	protected static String reservedKeywordForPresentationStateSOPInstanceUIDAttributeInCompositeContentItem = symbolSignifyingReservedKeyword+"prinstance";
	protected static String reservedKeywordForRealWorldValueMappingSOPClassUIDAttributeInCompositeContentItem = symbolSignifyingReservedKeyword+"rwvmclass";
	protected static String reservedKeywordForRealWorldValueMappingSOPInstanceUIDAttributeInCompositeContentItem = symbolSignifyingReservedKeyword+"rwvminstance";
	
	protected static String reservedKeywordForGraphicTypeAttributeInCoordinatesContentItem = symbolSignifyingReservedKeyword+"gtype";
	protected static String reservedKeywordFor2DCoordinatesAttributeInCoordinatesContentItem = symbolSignifyingReservedKeyword+"coord2d";
	protected static String reservedKeywordFor3DCoordinatesAttributeInCoordinatesContentItem = symbolSignifyingReservedKeyword+"coord3d";
	protected static String reservedKeywordForPixelOriginInterpretationAttributeInCoordinatesContentItem = symbolSignifyingReservedKeyword+"origin";
	protected static String reservedKeywordForFiducialUIDAttributeInCoordinatesContentItem = symbolSignifyingReservedKeyword+"fiducial";
	protected static String reservedKeywordForReferencedFrameOfReferenceUIDAttributeInCoordinatesContentItem = symbolSignifyingReservedKeyword+"for";

	protected static String reservedKeywordForMeasurementUnitsAttributeInNumericContentItem = symbolSignifyingReservedKeyword+"units";
	protected static String reservedKeywordForFloatingPointValueAttributeInNumericContentItem = symbolSignifyingReservedKeyword+"float";
	protected static String reservedKeywordForRationalNumeratorAttributeInNumericContentItem = symbolSignifyingReservedKeyword+"numerator";
	protected static String reservedKeywordForRationalDenominatorAttributeInNumericContentItem = symbolSignifyingReservedKeyword+"denominator";
	protected static String reservedKeywordForNumericValueQualifierAttributeInNumericContentItem = symbolSignifyingReservedKeyword+"numqual";

	protected static String reservedKeywordForAlphabeticPropertyInPersonNameContentItem = symbolSignifyingReservedKeyword+"alphabetic";
	protected static String reservedKeywordForIdeographicPropertyInPersonNameContentItem = symbolSignifyingReservedKeyword+"ideographic";
	protected static String reservedKeywordForPhoneticPropertyInPersonNameContentItem = symbolSignifyingReservedKeyword+"phonetic";

	protected boolean isCommonAnnotationAttribute(String attributeName) {
		return attributeName!= null && attributeName.length() > 0 && (
			   attributeName.equals(reservedKeywordForSimplifiedLabelAttributeInSRFile)
			|| attributeName.equals(reservedKeywordForSimplifiedReferenceToLabelAttributeInSRFile)
			|| attributeName.equals(reservedKeywordForObservationDateTimeAttributeInSRFile)
			|| attributeName.equals(reservedKeywordForObservationUIDAttributeInSRFile)
			);
	}

	protected static String simplifiedLabelPrefix = "label";

	private JsonBuilderFactory factory;
	
	protected Map<String,CodedSequenceItem> businessNames = new HashMap<String,CodedSequenceItem>();
	protected Map<String,SortedSet<String>> valueTypesByBusinessName = new HashMap<String,SortedSet<String>>();
	protected Map<String,SortedSet<String>> relationshipTypesByBusinessName = new HashMap<String,SortedSet<String>>();

	public static String makeBusinessNameFromCodeMeaning(String codeMeaning,boolean upperCamelCase) {
		String businessName = businessNameToUseForAnonymousContentItems;
		if (codeMeaning != null && codeMeaning.length() > 0) {
			businessName = codeMeaning.replaceAll("[^A-Za-z0-9]"," ");
			if (upperCamelCase) {
				businessName = StringUtilities.toUpperCamelCase(businessName);
			}
			businessName = businessName.replaceAll(" ","");
		}
		return businessName;
	}
	
	public static String makeBusinessNameFromCodeMeaning(CodedSequenceItem conceptName) {
		return conceptName == null
			? businessNameToUseForAnonymousContentItems
			: makeBusinessNameFromCodeMeaning(conceptName.getCodeMeaning(),!"UCUM".equals(conceptName.getCodingSchemeDesignator()));	// i.e., do not UpperCamelCase units code meaning
	}
	
	public CodedSequenceItem getCodedSequenceItemForBusinessName(String businessName,String location,boolean roleIsAsConceptName) {
		CodedSequenceItem businessNameCode = null;
		if (businessName == null) {
			slf4jlogger.error("getCodedSequenceItemForBusinessNameUsedAsConceptName(): {}: Null string for business name used as concept name",location);
		}
		else if (businessName.length() == 0) {
			slf4jlogger.warn("getCodedSequenceItemForBusinessNameUsedAsConceptName(): {}: Empty string for business name used as concept name - should be using reserved word instead ",location);
		}
		else if (businessName.equals(businessNameToUseForAnonymousContentItems)) {
			if (roleIsAsConceptName) {
				slf4jlogger.debug("getCodedSequenceItemForBusinessNameUsedAsConceptName(): {}: Encountered reserved anonymous business name keyword used as concept name {} ",location,businessName);
			}
			else {
				slf4jlogger.error("getCodedSequenceItemForBusinessNameUsedAsValue(): {}: Cannot use reserved anonymous business name keyword as concept value {} ",location,businessName);
			}
			// in either case, fall through with null ...
		}
		else {
			businessNameCode = businessNames.get(businessName);
			if (businessNameCode == null) {
				slf4jlogger.error("getCodedSequenceItemForBusinessNameUsedAsConceptName(): {}: Could not find a code for business name used as concept name {} ",location,businessName);
			}
			else {
				slf4jlogger.debug("getCodedSequenceItemForBusinessNameUsedAsConceptName(): {}: Code for business name used as concept name {} is {}",location,businessName,businessNameCode.toString());
			}
		}
		return businessNameCode;
	}
	
	public CodedSequenceItem getCodedSequenceItemForBusinessNameUsedAsConceptName(String businessName,String location) {
		return getCodedSequenceItemForBusinessName(businessName,location,true/*roleIsAsConceptName*/);
	}

	public CodedSequenceItem getCodedSequenceItemForBusinessNameUsedAsValue(String businessName,String location) {
		return getCodedSequenceItemForBusinessName(businessName,location,false/*roleIsAsConceptName*/);
	}

	public CodedSequenceItem getCodedSequenceItemForBusinessNameUsedAsUnits(String businessName,String location) {
		return getCodedSequenceItemForBusinessName(businessName,location,false/*roleIsAsConceptName*/);
	}

	public JsonArray getBusinessNamesDocument() {
		slf4jlogger.debug("getBusinessNamesDocument():");
		JsonArrayBuilder arrayBuilder = factory.createArrayBuilder();
		for (String businessName : businessNames.keySet()) {
			CodedSequenceItem csi = businessNames.get(businessName);
			if (slf4jlogger.isDebugEnabled()) slf4jlogger.debug("getBusinessNamesDocument(): Creating JSON business name {} for {}",businessName,csi);
			// each entry will be object { "_cv" : "codevalue", "_csd" : "codingschemedesignator", "_cm" : "code meaning" }
			JsonObjectBuilder jsonCodedSequenceItem = factory.createObjectBuilder();
			jsonCodedSequenceItem.add(reservedKeywordForCodeValueInBusinessNamesFile,csi.getCodeValue());
			jsonCodedSequenceItem.add(reservedKeywordForCodingSchemeDesignatorInBusinessNamesFile,csi.getCodingSchemeDesignator());
			jsonCodedSequenceItem.add(reservedKeywordForCodeMeaningInBusinessNamesFile,csi.getCodeMeaning());
			
			{ String s = csi.getCodingSchemeVersion(); if (s.length() > 0) jsonCodedSequenceItem.add(reservedKeywordForCodingSchemeVersionInBusinessNamesFile,s); }
			{ String s = Attribute.getSingleStringValueOrEmptyString(csi.getAttributeList(),DicomDictionary.StandardDictionary.getTagFromName("LongCodeValue")); if (s.length() > 0) jsonCodedSequenceItem.add(reservedKeywordForLongCodeValueInBusinessNamesFile,s); }
			{ String s = Attribute.getSingleStringValueOrEmptyString(csi.getAttributeList(),DicomDictionary.StandardDictionary.getTagFromName("URNCodeValue")); if (s.length() > 0) jsonCodedSequenceItem.add(reservedKeywordForURNCodeValueInBusinessNamesFile,s); }
			{ String s = Attribute.getSingleStringValueOrEmptyString(csi.getAttributeList(),DicomDictionary.StandardDictionary.getTagFromName("ContextIdentifier")); if (s.length() > 0) jsonCodedSequenceItem.add(reservedKeywordForContextIdentifierInBusinessNamesFile,s); }
			{ String s = Attribute.getSingleStringValueOrEmptyString(csi.getAttributeList(),DicomDictionary.StandardDictionary.getTagFromName("ContextUID")); if (s.length() > 0) jsonCodedSequenceItem.add(reservedKeywordForContextUIDInBusinessNamesFile,s); }
			{ String s = Attribute.getSingleStringValueOrEmptyString(csi.getAttributeList(),DicomDictionary.StandardDictionary.getTagFromName("MappingResource")); if (s.length() > 0) jsonCodedSequenceItem.add(reservedKeywordForMappingResourceInBusinessNamesFile,s); }
			{ String s = Attribute.getSingleStringValueOrEmptyString(csi.getAttributeList(),DicomDictionary.StandardDictionary.getTagFromName("MappingResourceUID")); if (s.length() > 0) jsonCodedSequenceItem.add(reservedKeywordForMappingResourceUIDInBusinessNamesFile,s); }
			{ String s = Attribute.getSingleStringValueOrEmptyString(csi.getAttributeList(),DicomDictionary.StandardDictionary.getTagFromName("MappingResourceName")); if (s.length() > 0) jsonCodedSequenceItem.add(reservedKeywordForMappingResourceNameInBusinessNamesFile,s); }
			{ String s = Attribute.getSingleStringValueOrEmptyString(csi.getAttributeList(),DicomDictionary.StandardDictionary.getTagFromName("ContextGroupVersion")); if (s.length() > 0) jsonCodedSequenceItem.add(reservedKeywordForContextGroupVersionInBusinessNamesFile,s); }
			{ String s = Attribute.getSingleStringValueOrEmptyString(csi.getAttributeList(),DicomDictionary.StandardDictionary.getTagFromName("ContextGroupExtensionFlag")); if (s.length() > 0) jsonCodedSequenceItem.add(reservedKeywordForContextGroupExtensionFlagInBusinessNamesFile,s); }
			{ String s = Attribute.getSingleStringValueOrEmptyString(csi.getAttributeList(),DicomDictionary.StandardDictionary.getTagFromName("ContextGroupLocalVersion")); if (s.length() > 0) jsonCodedSequenceItem.add(reservedKeywordForContextGroupLocalVersionInBusinessNamesFile,s); }
			{ String s = Attribute.getSingleStringValueOrEmptyString(csi.getAttributeList(),DicomDictionary.StandardDictionary.getTagFromName("ContextGroupExtensionCreatorUID")); if (s.length() > 0) jsonCodedSequenceItem.add(reservedKeywordForContextGroupExtensionCreatorUIDInBusinessNamesFile,s); }

			{
				SortedSet<String> valueTypes = valueTypesByBusinessName.get(businessName);
				if (valueTypes != null && valueTypes.size() > 0) {
					JsonArrayBuilder jsonValueTypes = factory.createArrayBuilder();
					for (String valueType : valueTypes) {
						if (valueType != null && valueType.length() > 0) {
							jsonValueTypes.add(valueType);
						}
					}
					jsonCodedSequenceItem.add(reservedKeywordForValueTypeInBusinessNamesFile,jsonValueTypes);
				}
			}
			{
				SortedSet<String> relationshipTypes = relationshipTypesByBusinessName.get(businessName);
				if (relationshipTypes != null && relationshipTypes.size() > 0) {
					JsonArrayBuilder jsonRelationshipTypes = factory.createArrayBuilder();
					for (String relationshipType : relationshipTypes) {
						if (relationshipType != null && relationshipType.length() > 0) {
							jsonRelationshipTypes.add(relationshipType);
						}
					}
					jsonCodedSequenceItem.add(reservedKeywordForRelationshipTypeInBusinessNamesFile,jsonRelationshipTypes);
				}
			}

			JsonObjectBuilder jsonBusinessNameEntry = factory.createObjectBuilder();
			jsonBusinessNameEntry.add(businessName,jsonCodedSequenceItem);
			
			arrayBuilder.add(jsonBusinessNameEntry);
		}
		return arrayBuilder.build();
	}
	
	protected void addCodedSequenceItemPropertyFromBusinessName(JsonObject businessNamePayload,AttributeList csilist,AttributeTag tag,String reservedKeywordInBusinessNamesFile) {
		JsonString js = (JsonString)(businessNamePayload.get(reservedKeywordInBusinessNamesFile));
		if (js != null) {
			String s = js.getString();
			if (s != null && s.length() > 0) {
				try {
					Attribute a = AttributeFactory.newAttribute(tag);
					a.addValue(s);
					csilist.put(a);
				}
				catch (DicomException e) {
					slf4jlogger.error("addCodedSequenceItemPropertyFromBusinessName(): Failed to construct CodedSequenceItem AttributeList Attribute for {} from {} with value {}: {}",tag,reservedKeywordInBusinessNamesFile,s,e);
				}
			}
		}
	}
	
	/**
	 * <p>Load the business names encoded in a JSON document.</p>
	 *
	 * @param		document		the JSON document
	 * @throws	DicomException
	 */
	public void loadBusinessNamesDocument(JsonArray document) throws DicomException {
		for (int i=0; i<document.size(); ++i) {
			try {
				JsonObject businessNameEntry = document.getJsonObject(i);
				if (businessNameEntry != null ) {
					// should be, e.g. {"SpecificImageFindings":{"_cv":"999000","_csd":"LNdemo","_cm":"Specific Image Findings"}}
					String businessName = businessNameEntry.keySet().iterator().next();
					if (businessName != null && businessName.length() > 0) {
						try {
							JsonObject businessNamePayload = (JsonObject)(businessNameEntry.get(businessName));
							try {
								JsonString jsonCodeValue = (JsonString)(businessNamePayload.get(reservedKeywordForCodeValueInBusinessNamesFile));
								if (jsonCodeValue != null) {
									// business name is a coded concept tuple
									String cv = jsonCodeValue.getString();
									String csd = null;
									String cm = null;
									{
										JsonString jsonCodingSchemeDesignator = (JsonString)(businessNamePayload.get(reservedKeywordForCodingSchemeDesignatorInBusinessNamesFile));
										if (jsonCodingSchemeDesignator != null) {
											csd = jsonCodingSchemeDesignator.getString();
										}
										else {
											throw new DicomException("Missing "+reservedKeywordForCodingSchemeDesignatorInBusinessNamesFile+" for code "+cv+" for business name "+businessName);
										}
									}
									{
										JsonString jsonCodeMeaning = (JsonString)(businessNamePayload.get(reservedKeywordForCodeMeaningInBusinessNamesFile));
										if (jsonCodeMeaning != null) {
											cm = jsonCodeMeaning.getString();
										}
										else {
											throw new DicomException("Missing "+reservedKeywordForCodeMeaningInBusinessNamesFile+" for code "+cv+" for business name "+businessName);
										}
									}

									if (cv != null && cv.length() > 0
										&& csd != null && csd.length() > 0
										&& cm != null && cm.length() > 0
									) {
										CodedSequenceItem csi = new CodedSequenceItem(cv,csd,cm);
										AttributeList csilist = csi.getAttributeList();
										addCodedSequenceItemPropertyFromBusinessName(businessNamePayload,csilist,TagFromName.CodingSchemeVersion,reservedKeywordForCodingSchemeVersionInBusinessNamesFile);
										addCodedSequenceItemPropertyFromBusinessName(businessNamePayload,csilist,DicomDictionary.StandardDictionary.getTagFromName("LongCodeValue"),reservedKeywordForLongCodeValueInBusinessNamesFile);
										addCodedSequenceItemPropertyFromBusinessName(businessNamePayload,csilist,DicomDictionary.StandardDictionary.getTagFromName("URNCodeValue"),reservedKeywordForURNCodeValueInBusinessNamesFile);
										addCodedSequenceItemPropertyFromBusinessName(businessNamePayload,csilist,DicomDictionary.StandardDictionary.getTagFromName("ContextIdentifier"),reservedKeywordForContextIdentifierInBusinessNamesFile);
										addCodedSequenceItemPropertyFromBusinessName(businessNamePayload,csilist,DicomDictionary.StandardDictionary.getTagFromName("ContextUID"),reservedKeywordForContextUIDInBusinessNamesFile);
										addCodedSequenceItemPropertyFromBusinessName(businessNamePayload,csilist,DicomDictionary.StandardDictionary.getTagFromName("MappingResource"),reservedKeywordForMappingResourceInBusinessNamesFile);
										addCodedSequenceItemPropertyFromBusinessName(businessNamePayload,csilist,DicomDictionary.StandardDictionary.getTagFromName("MappingResourceUID"),reservedKeywordForMappingResourceUIDInBusinessNamesFile);
										addCodedSequenceItemPropertyFromBusinessName(businessNamePayload,csilist,DicomDictionary.StandardDictionary.getTagFromName("MappingResourceName"),reservedKeywordForMappingResourceNameInBusinessNamesFile);
										addCodedSequenceItemPropertyFromBusinessName(businessNamePayload,csilist,DicomDictionary.StandardDictionary.getTagFromName("ContextGroupVersion"),reservedKeywordForContextGroupVersionInBusinessNamesFile);
										addCodedSequenceItemPropertyFromBusinessName(businessNamePayload,csilist,DicomDictionary.StandardDictionary.getTagFromName("ContextGroupExtensionFlag"),reservedKeywordForContextGroupExtensionFlagInBusinessNamesFile);
										addCodedSequenceItemPropertyFromBusinessName(businessNamePayload,csilist,DicomDictionary.StandardDictionary.getTagFromName("ContextGroupLocalVersion"),reservedKeywordForContextGroupLocalVersionInBusinessNamesFile);
										addCodedSequenceItemPropertyFromBusinessName(businessNamePayload,csilist,DicomDictionary.StandardDictionary.getTagFromName("ContextGroupExtensionCreatorUID"),reservedKeywordForContextGroupExtensionCreatorUIDInBusinessNamesFile);

										if (slf4jlogger.isDebugEnabled()) slf4jlogger.debug("loadBusinessNamesDocument(): Loading JSON business name {} for {}",businessName,csi);
										businessNames.put(businessName,csi);
									}
									else {
										throw new DicomException("Incomplete code value, coding scheme designator or code meaning for code "+cv+" for business name "+businessName);
									}
								}
								else {
									throw new DicomException("Unrecognized business name pattern entry for business name "+businessName);
								}
								
								// extract value and relationship type regardless of whether code value is present or not, even though probably not usable in that case
								try {
									JsonArray jsonValueTypes = (JsonArray)(businessNamePayload.get(reservedKeywordForValueTypeInBusinessNamesFile));
									if (jsonValueTypes != null) {
										for (int j=0; j<jsonValueTypes.size(); ++j) {
											JsonValue jsonValueTypeObject = jsonValueTypes.get(j);
											try {
												String valueType = ((JsonString)jsonValueTypeObject).getString();
												if (valueType != null && valueType.length() > 0) {
													SortedSet<String> valueTypes = valueTypesByBusinessName.get(businessName);
													if (valueTypes == null) {
														valueTypes = new TreeSet<String>();
														valueTypesByBusinessName.put(businessName,valueTypes);
													}
													valueTypes.add(valueType);
												}
												else {
													throw new DicomException("Empty or missing value type for business name "+businessName);
												}
											}
											catch (ClassCastException e) {
												throw new DicomException("String value type required in array for business name "+businessName);
											}
										}
									}
									// else OK to be absent
								}
								catch (ClassCastException e) {
									throw new DicomException("Array of value types required for business name "+businessName);
								}

								try {
									JsonArray jsonRelationshipTypes = (JsonArray)(businessNamePayload.get(reservedKeywordForRelationshipTypeInBusinessNamesFile));
									if (jsonRelationshipTypes != null) {
										for (int j=0; j<jsonRelationshipTypes.size(); ++j) {
											JsonValue jsonRelationshipTypeObject = jsonRelationshipTypes.get(j);
											try {
												String relationshipType = ((JsonString)jsonRelationshipTypeObject).getString();
												if (relationshipType != null && relationshipType.length() > 0) {
													SortedSet<String> relationshipTypes = relationshipTypesByBusinessName.get(businessName);
													if (relationshipTypes == null) {
														relationshipTypes = new TreeSet<String>();
														relationshipTypesByBusinessName.put(businessName,relationshipTypes);
													}
													relationshipTypes.add(relationshipType);
												}
												else {
													throw new DicomException("Empty or missing relationship type for business name "+businessName);
												}
											}
											catch (ClassCastException e) {
												throw new DicomException("String relationship type required in array for business name "+businessName);
											}
										}
									}
									// else OK to be absent
								}
								catch (ClassCastException e) {
									throw new DicomException("Array of relationship types required for business name "+businessName);
								}
							}
							catch (ClassCastException e) {
								throw new DicomException("Expected strings for values of business name entry # "+i);
							}
						}
						catch (ClassCastException e) {
							throw new DicomException("Expected object as value of business name "+businessName+" entry # "+i);
						}
					}
					else {
						throw new DicomException("Missing or bad business name "+businessName+" in entry # "+i);
					}
				}
				else {
					throw new DicomException("Missing business name entry # "+i);
				}
			}
			catch (ClassCastException e) {
				throw new DicomException("Expected object for business name entry # "+i);
			}
		}
	}
	
	/**
	 * <p>Load the business names encoded in a JSON document.</p>
	 *
	 * @param		stream			the input stream containing the JSON document
	 * @throws	IOException
	 * @throws	DicomException
	 */
	public void loadBusinessNamesDocument(InputStream stream) throws IOException, DicomException {
		JsonReader jsonReader = Json.createReader(stream);
		JsonArray document = jsonReader.readArray();
		jsonReader.close();
		loadBusinessNamesDocument(document);
	}
	
	/**
	 * <p>Load the business names encoded in a JSON document.</p>
	 *
	 * @param		file			the input file containing the JSON document
	 * @throws	IOException
	 * @throws	DicomException
	 */
	public void loadBusinessNamesDocument(File file) throws IOException, DicomException {
		InputStream fi = new FileInputStream(file);
		BufferedInputStream bi = new BufferedInputStream(fi);
		try {
			loadBusinessNamesDocument(bi);
		}
		finally {
			bi.close();
			fi.close();
		}
	}
	
	/**
	 * <p>Load the business names encoded in a JSON document.</p>
	 *
	 * @param		name			the input file containing the JSON document
	 * @throws	IOException
	 * @throws	DicomException
	 */
	public void loadBusinessNamesDocument(String name) throws IOException, DicomException {
		loadBusinessNamesDocument(new File(name));
	}

	protected Map<ContentItem,String> contentItemIdentifiersByContentItem = new HashMap<ContentItem,String>();
	protected Map<String,String> simplifiedLabelByReferencedContentItemIdentifiers = new HashMap<String,String>();
	protected int simplifiedLabelCounter = 0;

	// Follows same pattern as StructuredReport.walkTreeBuldingString()
	/**
	 * <p>Walk the tree starting at the specified node.</p>
	 *
	 * @param	node
	 * @param	location	the dotted numeric string describing the location of the starting node
	 */
	protected void walkTreeBuildingSimplifiedLabelsForReferencedContentItemIdentifiers(ContentItem node,String location) {
		if (node != null) {
			contentItemIdentifiersByContentItem.put(node,location);
			// Does this node reference another? If so record that and assign it a simplified label (if not done already)
			{
				String referencedContentItemIdentifier = node.getReferencedContentItemIdentifier();
				if (referencedContentItemIdentifier != null && referencedContentItemIdentifier.length() > 0) {
					String simplifiedLabel = simplifiedLabelByReferencedContentItemIdentifiers.get(referencedContentItemIdentifier);
					if (simplifiedLabel == null) {
						simplifiedLabel = simplifiedLabelPrefix + Integer.toString(++simplifiedLabelCounter);
						simplifiedLabelByReferencedContentItemIdentifiers.put(referencedContentItemIdentifier,simplifiedLabel);
						slf4jlogger.debug("walkTreeBuildingSimplifiedLabelsForReferencedContentItemIdentifierswalkTreeBuildingSimplifiedLabelsForReferencedContentItemIdentifiers(): reference to {} is assigned simplified label {}",referencedContentItemIdentifier,simplifiedLabel);
					}
				}
			}
			int n = node.getChildCount();
			for (int i=0; i<n; ++i) walkTreeBuildingSimplifiedLabelsForReferencedContentItemIdentifiers((ContentItem)(node.getChildAt(i)),location+"."+Integer.toString(i+1));
		}
	}

	/**
	 * @param	contentItem		content item node of the Structured Report
	 * @param	parentObject	the JSON object to add to
	 * @throws	DicomException
	 */
	private void addContentItemAndChildrenToJsonObject(ContentItem contentItem,JsonObjectBuilder parentObject) throws DicomException {
		if (contentItem != null) {
			int nChildren = contentItem.getChildCount();

			JsonArrayBuilder valuesAndChildren = null;		// lazy instantiation to allow for  when not needed due to special case of single string
			String leafValue = null;
			
			String valueType = contentItem.getValueType();
			//if (valueType == null || valueType.length() == 0) {
			//	throw new DicomException("Converting by-reference relationships to JSON not supported");
			//}
			
			String relationshipType = contentItem.getRelationshipType();
			
			//{
			//	String referencedContentItemIdentifier = contentItem.getReferencedContentItemIdentifier();
			//	if (referencedContentItemIdentifier != null && referencedContentItemIdentifier.length() > 0) {
			//		String referencedSimplifiedLabel = simplifiedLabelByReferencedContentItemIdentifiers.get(referencedContentItemIdentifier);
			//		if (referencedSimplifiedLabel != null && referencedSimplifiedLabel.length() > 0) {
			//			JsonObjectBuilder referenceObject = factory.createObjectBuilder();
			//			referenceObject.add(reservedKeywordForSimplifiedReferenceToLabelAttributeInSRFile,referencedSimplifiedLabel);
			//			valuesAndChildren.add(referenceObject);
			//		}
			//		// else should not happen since already walked tree to populate these
			//	}
			//}
			
			CodedSequenceItem conceptName = contentItem.getConceptName();
			String businessName = makeBusinessNameFromCodeMeaning(conceptName);
			if (slf4jlogger.isDebugEnabled()) slf4jlogger.debug("addContentItemAndChildrenToJsonObject(): businessName is {} for conceptName {}",businessName,conceptName);
			// OK for conceptName to be missing ... this is a so-called anonymous content item, e.g., as is often the case for IMAGE value type
			if (conceptName != null && businessName != null && businessName.length() > 0) {
				businessNames.put(businessName,conceptName);
			}
			if (valueType != null && valueType.length() > 0) {
				SortedSet<String> valueTypes = valueTypesByBusinessName.get(businessName);
				if (valueTypes == null) {
					valueTypes = new TreeSet<String>();
					valueTypesByBusinessName.put(businessName,valueTypes);
				}
				valueTypes.add(valueType);
			}
			if (relationshipType != null && relationshipType.length() > 0) {
				SortedSet<String> relationshipTypes = relationshipTypesByBusinessName.get(businessName);
				if (relationshipTypes == null) {
					relationshipTypes = new TreeSet<String>();
					relationshipTypesByBusinessName.put(businessName,relationshipTypes);
				}
				relationshipTypes.add(relationshipType);
			}
			
			JsonObjectBuilder contentItemAttributesObject = null;	// flag that it is not needed, lazy instantiation

			String contentItemIdentifier = contentItemIdentifiersByContentItem.get(contentItem);
			slf4jlogger.debug("addContentItemAndChildrenToJsonObject(): contentItemIdentifier {} for businessName {}",contentItemIdentifier,businessName);
			{
				String simplifiedLabel = simplifiedLabelByReferencedContentItemIdentifiers.get(contentItemIdentifier);
				slf4jlogger.debug("addContentItemAndChildrenToJsonObject(): contentItemIdentifier {} has simplifiedLabel {}",contentItemIdentifier,simplifiedLabel);
				if (simplifiedLabel != null) {
					if (contentItemAttributesObject == null) {
						contentItemAttributesObject = factory.createObjectBuilder();
					}
					contentItemAttributesObject.add(reservedKeywordForSimplifiedLabelAttributeInSRFile,simplifiedLabel);
				}
			}
			
			{
				String observationDateTime = contentItem.getObservationDateTime();
				String observationUID = contentItem.getObservationUID();
				if ((observationDateTime != null && observationDateTime.length() > 0)
				 || (observationUID != null && observationUID.length() > 0)
				) {
					slf4jlogger.debug("addContentItemAndChildrenToJsonObject(): adding generic contentItemAttributesObject to {}",contentItemIdentifier);
					if (contentItemAttributesObject == null) {
						contentItemAttributesObject = factory.createObjectBuilder();
					}
					if (observationDateTime != null && observationDateTime.length() > 0) {
						contentItemAttributesObject.add(reservedKeywordForObservationDateTimeAttributeInSRFile,observationDateTime);
					}
					if (observationUID != null && observationUID.length() > 0) {
						contentItemAttributesObject.add(reservedKeywordForObservationUIDAttributeInSRFile,observationUID);
					}
				}
			}

			if (contentItem instanceof ContentItemFactory.ContainerContentItem) {
				String continuityOfContent = ((ContentItemFactory.ContainerContentItem)contentItem).getContinuityOfContent();
				String templateMappingResource = ((ContentItemFactory.ContainerContentItem)contentItem).getTemplateMappingResource();
				String templateIdentifier = ((ContentItemFactory.ContainerContentItem)contentItem).getTemplateIdentifier();
				if ((continuityOfContent != null && continuityOfContent.length() > 0 && !elideSeparateContinuityOfContent || !continuityOfContent.equals("SEPARATE"))
				 || (templateMappingResource != null && templateMappingResource.length() > 0)
				 || (templateIdentifier != null && templateIdentifier.length() > 0)) {
					slf4jlogger.debug("addContentItemAndChildrenToJsonObject(): adding Container contentItemAttributesObject to {}",contentItemIdentifier);
					if (contentItemAttributesObject == null) {
						contentItemAttributesObject = factory.createObjectBuilder();
					}
					if (continuityOfContent != null && continuityOfContent.length() > 0 && !elideSeparateContinuityOfContent || !continuityOfContent.equals("SEPARATE")) {
						contentItemAttributesObject.add(reservedKeywordForContinuityOfContentAttributeInSRFile,continuityOfContent);
					}
					if (templateMappingResource != null && templateMappingResource.length() > 0) {
						contentItemAttributesObject.add(reservedKeywordForTemplateMappingResourceAttributeInSRFile,templateMappingResource);
					}
					if (templateIdentifier != null && templateIdentifier.length() > 0) {
						contentItemAttributesObject.add(reservedKeywordForTemplateIdentifierAttributeInSRFile,templateIdentifier);
					}
				}
				if (contentItemAttributesObject != null) {
					if (valuesAndChildren == null) valuesAndChildren = factory.createArrayBuilder();
					valuesAndChildren.add(contentItemAttributesObject);
				}
			}
			else if (contentItem instanceof ContentItemFactory.CodeContentItem) {
				if (contentItemAttributesObject != null) {
					if (valuesAndChildren == null) valuesAndChildren = factory.createArrayBuilder();
					valuesAndChildren.add(contentItemAttributesObject);
				}
				CodedSequenceItem conceptCode = ((ContentItemFactory.CodeContentItem)contentItem).getConceptCode();
				if (conceptCode != null) {
					String businessNameForConceptCode = makeBusinessNameFromCodeMeaning(conceptCode);
					if (businessNameForConceptCode != null && businessNameForConceptCode.length() > 0) {
						if (slf4jlogger.isDebugEnabled()) slf4jlogger.debug("addContentItemAndChildrenToJsonObject(): businessNameForConceptCode is {} for conceptCode {}",businessNameForConceptCode,conceptCode);
						businessNames.put(businessNameForConceptCode,conceptCode);
						// obviously not adding value type and relationship type, since used as value not concept name
						if (collapseContentTreeArrays && nChildren == 0 && contentItemAttributesObject == null) {
							leafValue = businessNameForConceptCode;
						}
						else {
							if (valuesAndChildren == null) valuesAndChildren = factory.createArrayBuilder();
							valuesAndChildren.add(businessNameForConceptCode);
						}
					}
					// else what does it mean not to be able to get a business name ? should be an exception :(
				}
			}
			else if (contentItem instanceof ContentItemFactory.NumericContentItem) {
				ContentItemFactory.NumericContentItem numericContentItem = ((ContentItemFactory.NumericContentItem)contentItem);
				
				// regardless of whether we needed any other attributes of this content item, all COMPOSITE descriptors are encoded as attributes
				if (contentItemAttributesObject == null) {
					contentItemAttributesObject = factory.createObjectBuilder();
				}
				// don't add to valuesAndChildren ... need to wait until contentItemAttributesObject is fully built

				{
					CodedSequenceItem unitsCode = ((ContentItemFactory.NumericContentItem)contentItem).getUnits();
					String businessNameForUnitsCode = null;
					if (unitsCode != null) {
						businessNameForUnitsCode = makeBusinessNameFromCodeMeaning(unitsCode);
						if (businessNameForUnitsCode != null && businessNameForUnitsCode.length() > 0) {
							if (slf4jlogger.isDebugEnabled()) slf4jlogger.debug("addContentItemAndChildrenToJsonObject(): businessNameForUnitsCode is {} for conceptCode {}",businessNameForUnitsCode,unitsCode);
							businessNames.put(businessNameForUnitsCode,unitsCode);	// obviously not adding value type and relationship type to businessNames map entry, since used as value not concept name
							contentItemAttributesObject.add(reservedKeywordForMeasurementUnitsAttributeInNumericContentItem,businessNameForUnitsCode);
						}
					}
				}
				
				if (numericContentItem.hasFloatingPointValue()) {
					double doubleValue = numericContentItem.getFloatingPointValue();
					contentItemAttributesObject.add(reservedKeywordForFloatingPointValueAttributeInNumericContentItem,doubleValue);
				}
				
				if (numericContentItem.hasRationalValue()) {
					int numeratorValue = numericContentItem.getRationalNumeratorValue();
					contentItemAttributesObject.add(reservedKeywordForRationalNumeratorAttributeInNumericContentItem,numeratorValue);
					long denominatorValue = numericContentItem.getRationalDenominatorValue();
					contentItemAttributesObject.add(reservedKeywordForRationalDenominatorAttributeInNumericContentItem,denominatorValue);
				}
				
				{
					CodedSequenceItem qualifierCode = numericContentItem.getQualifier();
					if (qualifierCode != null) {
						String businessNameForQualifierCode = makeBusinessNameFromCodeMeaning(qualifierCode);
						if (businessNameForQualifierCode != null && businessNameForQualifierCode.length() > 0) {
							if (slf4jlogger.isDebugEnabled()) slf4jlogger.debug("addContentItemAndChildrenToJsonObject(): businessNameForQualifierCode is {} for conceptCode {}",businessNameForQualifierCode,qualifierCode);
							businessNames.put(businessNameForQualifierCode,qualifierCode);	// obviously not adding value type and relationship type to businessNames map entry
							contentItemAttributesObject.add(reservedKeywordForNumericValueQualifierAttributeInNumericContentItem,businessNameForQualifierCode);
						}
					}
				}

				// do this now, after contentItemAttributesObject has been built but before we add the value
				if (valuesAndChildren == null) valuesAndChildren = factory.createArrayBuilder();
				valuesAndChildren.add(contentItemAttributesObject);

				{
					if (useNumberForNumericContentItemValue) {
						if (numericContentItem.hasFloatingPointValue()) {
							double doubleValue = numericContentItem.getFloatingPointValue();
							slf4jlogger.debug("addContentItemAndChildrenToJsonObject(): useNumberForNumericContentItemValue: adding getFloatingPointValue() numeric value {} for businessName {}",doubleValue,businessName);
							if (valuesAndChildren == null) valuesAndChildren = factory.createArrayBuilder();
							valuesAndChildren.add(doubleValue);
						}
						else {
							String stringValue = numericContentItem.getNumericValue();
							if (stringValue != null && stringValue.length() > 0) {
								try {
									double doubleValue = Double.parseDouble(stringValue);
									slf4jlogger.debug("addContentItemAndChildrenToJsonObject(): useNumberForNumericContentItemValue: adding Double.parseDouble() numeric value {} from decimal string {} for businessName {}",doubleValue,stringValue,businessName);
									if (valuesAndChildren == null) valuesAndChildren = factory.createArrayBuilder();
									valuesAndChildren.add(doubleValue);
								}
								catch (NumberFormatException e) {
									slf4jlogger.error("addContentItemAndChildrenToJsonObject(): numeric value {} is not valid decimal string for businessName {}",stringValue,businessName);
								}
							}
						}
					}
					else {
						String value = numericContentItem.getNumericValue();
						if (value != null && value.length() > 0) {
							if (valuesAndChildren == null) valuesAndChildren = factory.createArrayBuilder();
							valuesAndChildren.add(value);
						}
					}
				}

				// else what does it mean not to be able to get a value and units business name ? should be an exception :(
			}
			else if (contentItem instanceof ContentItemFactory.PersonNameContentItem) {
				ContentItemFactory.PersonNameContentItem personNameContentItem = ((ContentItemFactory.PersonNameContentItem)contentItem);
				String value = personNameContentItem.getConceptValue();
				if (value != null && value.length() > 0) {
					// regardless of whether we needed any other attributes of this content item, all PNAME entries are encoded as attributes
					if (contentItemAttributesObject == null) {
						contentItemAttributesObject = factory.createObjectBuilder();
					}
					// don't add to valuesAndChildren ... need to wait until contentItemAttributesObject is fully built

					JSONRepresentationOfDicomObjectFactory.addPersonNameAsComponentsToJsonObject(contentItemAttributesObject,value,
												reservedKeywordForAlphabeticPropertyInPersonNameContentItem,
												reservedKeywordForIdeographicPropertyInPersonNameContentItem,
												reservedKeywordForPhoneticPropertyInPersonNameContentItem);
				}
				if (contentItemAttributesObject != null) {
					// do this now, after contentItemAttributesObject has been built but before we add the value
					if (valuesAndChildren == null) valuesAndChildren = factory.createArrayBuilder();
					valuesAndChildren.add(contentItemAttributesObject);
				}
				// there is no "value" per se
			}
			else if (contentItem instanceof ContentItemFactory.StringContentItem) {
				if (contentItemAttributesObject != null) {
					if (valuesAndChildren == null) valuesAndChildren = factory.createArrayBuilder();
					valuesAndChildren.add(contentItemAttributesObject);
				}
				String value = ((ContentItemFactory.StringContentItem)contentItem).getConceptValue().trim();	// why do we have to trim()? because sometimes there is trailing padding- why ? :(
				if (value != null && value.length() > 0) {
					if (collapseContentTreeArrays && nChildren == 0 && contentItemAttributesObject == null) {
						leafValue = value;
					}
					else {
						if (valuesAndChildren == null) valuesAndChildren = factory.createArrayBuilder();
						valuesAndChildren.add(value);
					}
				}
				// else what does it mean not to be able to get a value ? should be an exception :(
			}
			else if (contentItem instanceof ContentItemFactory.SpatialCoordinatesContentItem) {
				// regardless of whether we needed any other attributes of this content item, all SCOORD descriptors are encoded as attributes
				if (contentItemAttributesObject == null) {
					contentItemAttributesObject = factory.createObjectBuilder();
				}
				// don't add to valuesAndChildren ... need to wait until contentItemAttributesObject is fully built
				
				String graphicType = ((ContentItemFactory.SpatialCoordinatesContentItem)contentItem).getGraphicType();
				if (graphicType != null) {	// regardless of whether zero length or not, need node to append data to
					float[] graphicData = ((ContentItemFactory.SpatialCoordinatesContentItem)contentItem).getGraphicData();
					if (graphicData != null) {
						JsonArrayBuilder graphicDataArray = factory.createArrayBuilder();
						for (int i=0; i<graphicData.length; ++i) {
							graphicDataArray.add((double)graphicData[i]);
						}
						contentItemAttributesObject.add(reservedKeywordForGraphicTypeAttributeInCoordinatesContentItem,graphicType);
						contentItemAttributesObject.add(reservedKeywordFor2DCoordinatesAttributeInCoordinatesContentItem,graphicDataArray);
						
						// need to add pixel origin and fiducial UID when supported by ContentItemFactory.SpatialCoordinatesContentItem (001182) :(
					}
				}

				// do this now, after contentItemAttributesObject has been built
				if (valuesAndChildren == null) valuesAndChildren = factory.createArrayBuilder();
				valuesAndChildren.add(contentItemAttributesObject);
			}
			else if (contentItem instanceof ContentItemFactory.SpatialCoordinates3DContentItem) {
				// regardless of whether we needed any other attributes of this content item, all SCOORD3D descriptors are encoded as attributes
				if (contentItemAttributesObject == null) {
					contentItemAttributesObject = factory.createObjectBuilder();
				}
				// don't add to valuesAndChildren ... need to wait until contentItemAttributesObject is fully built
				
				String graphicType = ((ContentItemFactory.SpatialCoordinates3DContentItem)contentItem).getGraphicType();
				if (graphicType != null) {	// regardless of whether zero length or not, need node to append data to
					float[] graphicData = ((ContentItemFactory.SpatialCoordinates3DContentItem)contentItem).getGraphicData();
					if (graphicData != null) {
						JsonArrayBuilder graphicDataArray = factory.createArrayBuilder();
						for (int i=0; i<graphicData.length; ++i) {
							graphicDataArray.add((double)graphicData[i]);
						}
						String referencedFrameOfReferenceUID = ((ContentItemFactory.SpatialCoordinates3DContentItem)contentItem).getReferencedFrameOfReferenceUID();

						contentItemAttributesObject.add(reservedKeywordForGraphicTypeAttributeInCoordinatesContentItem,graphicType);
						contentItemAttributesObject.add(reservedKeywordFor3DCoordinatesAttributeInCoordinatesContentItem,graphicDataArray);
						contentItemAttributesObject.add(reservedKeywordForReferencedFrameOfReferenceUIDAttributeInCoordinatesContentItem,referencedFrameOfReferenceUID);

						// need to add pixel origin and fiducial UID when supported by ContentItemFactory.SpatialCoordinatesContentItem (001182) :(
					}
				}

				// do this now, after contentItemAttributesObject has been built
				if (valuesAndChildren == null) valuesAndChildren = factory.createArrayBuilder();
				valuesAndChildren.add(contentItemAttributesObject);
			}
			//ContentItemFactory.TemporalCoordinatesContentItem
			else if (contentItem instanceof ContentItemFactory.CompositeContentItem) {
				// regardless of whether we needed any other attributes of this content item, all COMPOSITE descriptors are encoded as attributes
				if (contentItemAttributesObject == null) {
					contentItemAttributesObject = factory.createObjectBuilder();
				}
				// don't add to valuesAndChildren ... need to wait until contentItemAttributesObject is fully built
				
				String referencedSOPClassUID = ((ContentItemFactory.CompositeContentItem)contentItem).getReferencedSOPClassUID();
				referencedSOPClassUID = JSONRepresentationOfDicomObjectFactory.substituteKeywordForUIDIfPossibleAndRequested(referencedSOPClassUID,substituteUIDKeywords);
				String referencedSOPInstanceUID = ((ContentItemFactory.CompositeContentItem)contentItem).getReferencedSOPInstanceUID();
				if (referencedSOPClassUID != null && referencedSOPClassUID.length() > 0) {
					contentItemAttributesObject.add(reservedKeywordForReferencedSOPClassUIDAttributeInCompositeContentItem,referencedSOPClassUID);
				}
				else {
					slf4jlogger.error("addContentItemAndChildrenToJsonObject(): composite family content item missing or empty ReferencedSOPClassUID");
				}
				if (referencedSOPInstanceUID != null && referencedSOPInstanceUID.length() > 0) {
					contentItemAttributesObject.add(reservedKeywordForReferencedSOPInstanceUIDAttributeInCompositeContentItem,referencedSOPInstanceUID);
				}
				else {
					slf4jlogger.error("addContentItemAndChildrenToJsonObject(): composite family content item missing or empty ReferencedSOPClassUID");
				}
				{
					if (contentItem instanceof ContentItemFactory.ImageContentItem) {
						ContentItemFactory.ImageContentItem imageContentItem = (ContentItemFactory.ImageContentItem)contentItem;
						// need to handle multiple values as array (001181) :(
						int referencedFrameNumber = imageContentItem.getReferencedFrameNumber();
						slf4jlogger.debug("addContentItemAndChildrenToJsonObject(): IMAGE content item referencedFrameNumber {}",referencedFrameNumber);
						if (referencedFrameNumber != 0) {
							contentItemAttributesObject.add(reservedKeywordForReferencedFrameNumberAttributeInCompositeContentItem,referencedFrameNumber);
						}
						
						int referencedSegmentNumber = imageContentItem.getReferencedSegmentNumber();
						slf4jlogger.debug("addContentItemAndChildrenToJsonObject(): IMAGE content item referencedSegmentNumber {}",referencedSegmentNumber);
						if (referencedSegmentNumber != 0) {
							contentItemAttributesObject.add(reservedKeywordForReferencedSegmentNumberAttributeInCompositeContentItem,referencedSegmentNumber);
						}

						String presentationStateSOPClassUID = imageContentItem.getPresentationStateSOPClassUID();
						presentationStateSOPClassUID = JSONRepresentationOfDicomObjectFactory.substituteKeywordForUIDIfPossibleAndRequested(presentationStateSOPClassUID,substituteUIDKeywords);
						slf4jlogger.debug("addContentItemAndChildrenToJsonObject(): IMAGE content item presentationStateSOPClassUID {}",presentationStateSOPClassUID);
						if (presentationStateSOPClassUID != null && presentationStateSOPClassUID.length() > 0) {
							contentItemAttributesObject.add(reservedKeywordForPresentationStateSOPClassUIDAttributeInCompositeContentItem,presentationStateSOPClassUID);
						}
				
						String presentationStateSOPInstanceUID = imageContentItem.getPresentationStateSOPInstanceUID();
						slf4jlogger.debug("addContentItemAndChildrenToJsonObject(): IMAGE content item presentationStateSOPInstanceUID {}",presentationStateSOPInstanceUID);
						if (presentationStateSOPInstanceUID != null && presentationStateSOPInstanceUID.length() > 0) {
							contentItemAttributesObject.add(reservedKeywordForPresentationStateSOPInstanceUIDAttributeInCompositeContentItem,presentationStateSOPInstanceUID);
						}
						// should check if one is present, both are :(

						String realWorldValueMappingSOPClassUID = imageContentItem.getRealWorldValueMappingSOPClassUID();
						realWorldValueMappingSOPClassUID = JSONRepresentationOfDicomObjectFactory.substituteKeywordForUIDIfPossibleAndRequested(realWorldValueMappingSOPClassUID,substituteUIDKeywords);
						slf4jlogger.debug("addContentItemAndChildrenToJsonObject(): IMAGE content item realWorldValueMappingSOPClassUID {}",realWorldValueMappingSOPClassUID);
						if (realWorldValueMappingSOPClassUID != null && realWorldValueMappingSOPClassUID.length() > 0) {
							contentItemAttributesObject.add(reservedKeywordForRealWorldValueMappingSOPClassUIDAttributeInCompositeContentItem,realWorldValueMappingSOPClassUID);
						}

						String realWorldValueMappingSOPInstanceUID = imageContentItem.getRealWorldValueMappingSOPInstanceUID();
						slf4jlogger.debug("addContentItemAndChildrenToJsonObject(): IMAGE content item realWorldValueMappingSOPInstanceUID {}",realWorldValueMappingSOPInstanceUID);
						if (realWorldValueMappingSOPInstanceUID != null && realWorldValueMappingSOPInstanceUID.length() > 0) {
							contentItemAttributesObject.add(reservedKeywordForRealWorldValueMappingSOPInstanceUIDAttributeInCompositeContentItem,realWorldValueMappingSOPInstanceUID);
						}
						// should check if one is present, both are :(
					}
				}
				// do this now, after contentItemAttributesObject has been built
				if (valuesAndChildren == null) valuesAndChildren = factory.createArrayBuilder();
				valuesAndChildren.add(contentItemAttributesObject);
			}
			else {
				// no value type means By-Reference relationship
				String referencedContentItemIdentifier = contentItem.getReferencedContentItemIdentifier();
				if (referencedContentItemIdentifier != null && referencedContentItemIdentifier.length() > 0) {
					String referencedSimplifiedLabel = simplifiedLabelByReferencedContentItemIdentifiers.get(referencedContentItemIdentifier);
					if (referencedSimplifiedLabel != null && referencedSimplifiedLabel.length() > 0) {
						if (contentItemAttributesObject == null) {
							contentItemAttributesObject = factory.createObjectBuilder();
						}
						contentItemAttributesObject.add(reservedKeywordForSimplifiedReferenceToLabelAttributeInSRFile,referencedSimplifiedLabel);
					}
					// else should not happen since already walked tree to populate the mapping of contentItemIdentifier to simplifiedLabel for those that are actuallt referenced
					if (contentItemAttributesObject != null) {
						if (valuesAndChildren == null) valuesAndChildren = factory.createArrayBuilder();
						valuesAndChildren.add(contentItemAttributesObject);
					}
				}
				else {
					//throw new DicomException("Content item value type "+valueType+" conversion to JSON not yet supported");
					slf4jlogger.debug("addContentItemAndChildrenToJsonObject(): Content item {} value type {} conversion to JSON not yet supported",contentItemIdentifier,valueType);
				}
			}

			// now handle any children, which must be in array rather than object since concept names may not be unique and order must be preserved
			if (nChildren > 0) {
				JsonArrayBuilder children = factory.createArrayBuilder();
				for (int i=0; i<nChildren; ++i) {
					JsonObjectBuilder child = factory.createObjectBuilder();
					addContentItemAndChildrenToJsonObject((ContentItem)(contentItem.getChildAt(i)),child);
					children.add(child);
				}
				if (valuesAndChildren == null) valuesAndChildren = factory.createArrayBuilder();
				valuesAndChildren.add(children);
			}
			
			// businessName will already have been set at this point to businessNameToUseForAnonymousContentItems by earlier call to makeBusinessNameFromCodeMeaning()
			//if (businessName == null || businessName.length() == 0) {
			//	businessName = businessNameToUseForAnonymousContentItems;
			//}
			
			if (valuesAndChildren != null) {
				parentObject.add(businessName,valuesAndChildren);	// don't do this until AFTER children have been added or it doesn't work (children are ignored)
			}
			else if (leafValue != null) {
				parentObject.add(businessName,leafValue);
			}
		}
		
	}

	/**
	 * <p>Construct a factory object, which can be used to get JSON documents from DICOM objects.</p>
	 *
	 */
	public JSONRepresentationOfStructuredReportObjectFactory() {
		factory = Json.createBuilderFactory(null/*config*/);
	}
	
	/**
	 * <p>Given a DICOM attribute list encoding a Structured Report, get a JSON document.</p>
	 *
	 * @param		list			the attribute list
	 * @exception	DicomException
	 * @return						the JSON encoded DICOM SR document

	 */
	public JsonArray getDocument(AttributeList list) throws DicomException {
		return getDocument(null,list);
	}
	
	/**
	 * <p>Given a DICOM Structured Report, get a JSON document of the content tree only.</p>
	 *
	 * @param		sr				the Structured Report
	 * @exception	DicomException
	 * @return						the JSON encoded DICOM SR document
	 */
	public JsonArray getDocument(StructuredReport sr) throws DicomException {
		return getDocument(sr,null);
	}
	
	/**
	 * <p>Given a DICOM Structured Report, get a JSON document of the content tree and the top level DICOM elements.</p>
	 *
	 * @param		sr				the Structured Report			may be null if list is not - will build an sr tree model
	 * @param		list			the attribute list				may be null if only the sr content tree is to be added
	 * @exception	DicomException
	 * @return						the JSON encoded DICOM SR document
	 */
	public JsonArray getDocument(StructuredReport sr,AttributeList list) throws DicomException {
		JsonObjectBuilder topLevelObjectBuilder = factory.createObjectBuilder();
		if (sr == null) {
			try {
				sr = new StructuredReport(list);
			}
			catch (DicomException e) {
				slf4jlogger.error("",e);
			}
		}
		if (list != null) {
			AttributeList clonedList = (AttributeList)(list.clone());
 			//clonedList.removePrivateAttributes();					// there is no reason to remove private attributes from the top level data set
 			clonedList.removeGroupLengthAttributes();
 			clonedList.removeMetaInformationHeaderAttributes();
 			//clonedList.remove(TagFromName.ContentSequence);
			//clonedList.remove(TagFromName.ValueType);
			//clonedList.remove(TagFromName.ContentTemplateSequence);
			//clonedList.remove(TagFromName.ContinuityOfContent);
			//clonedList.remove(TagFromName.ConceptNameCodeSequence);
			
			new JSONRepresentationOfDicomObjectFactory().addAttributesFromListToJsonObject(clonedList,topLevelObjectBuilder,true/*useKeywordInsteadOfTag*/,false/*addTag*/,false/*addKeyword*/,false/*addVR*/,collapseAttributeValueArrays,collapseEmptyToNull,true/*ignoreSR*/,substituteUIDKeywords,false/*useNumberForIntegerOrDecimalString*/);
		}
		if (sr != null) {
			walkTreeBuildingSimplifiedLabelsForReferencedContentItemIdentifiers((ContentItem)(sr.getRoot()),"1");
			addContentItemAndChildrenToJsonObject((ContentItem)(sr.getRoot()),topLevelObjectBuilder);
		}
		return factory.createArrayBuilder().add(topLevelObjectBuilder).build();
	}
	
	/**
	 * <p>Given a DICOM object encoded as a list of attributes, get a JSON document.</p>
	 *
	 * @param	file	the DICOM file
	 * @return							the JSON document
	 * @throws	IOException
	 * @throws	DicomException
	 */
	public JsonArray getDocument(File file) throws IOException, DicomException {
		AttributeList list = new AttributeList();
		list.setDecompressPixelData(false);
		list.read(file);
		return getDocument(null/*sr*/,list);	// will build an sr tree model from list
	}
	
	/**
	 * <p>Given a DICOM object encoded as a list of attributes, get a JSON document.</p>
	 *
	 * @param	filename				the DICOM file name
	 * @return							the JSON document
	 * @throws	IOException
	 * @throws	DicomException
	 */
	public JsonArray getDocument(String filename) throws IOException, DicomException {
		return getDocument(new File(filename));
	}

	protected ContentItemFactory contentItemFactory;
	
	protected String getStringFromSelectedContentItemValue(JsonArray contentItemValues,int which,String what,String valueType,CodedSequenceItem concept) {
		String value = null;
		JsonValue entry = contentItemValues.get(which);
		if (entry != null && entry.getValueType() == JsonValue.ValueType.STRING) {
			value = ((JsonString)entry).getString();
			if (value == null || value.length() == 0) {
				slf4jlogger.error("Missing {} in {} content item for concept {} ",what,valueType,concept);
			}
		}
		else {
			slf4jlogger.error("Missing {} string in {} content item for concept {} ",what,valueType,concept);
		}
		return value;
	}


	protected String getSingleStringValueOrNullFromJsonStringOrNumberContentItemValue(JsonValue entry,String valueType,CodedSequenceItem concept) {
		String value = null;
		if (entry == null) {
			slf4jlogger.error("Missing value string in {} content item for concept {} ",valueType,concept);
		}
		else {
			JsonValue.ValueType jsonValueType = entry.getValueType();
			if (jsonValueType == JsonValue.ValueType.STRING) {
				value = ((JsonString)entry).getString();
				if (value == null || value.length() == 0) {
					slf4jlogger.error("Missing or empty string value in {} content item for concept {} ",valueType,concept);
				}
			}
			else if (jsonValueType == JsonValue.ValueType.NUMBER) {
				// pattern copied from JSONRepresentationOfDicomObjectFactory.addAttributesFromListToJsonObject() ? refactor :(
				value = Double.toString(((JsonNumber)entry).doubleValue());			// NB. will add trailing ".0" if an integer value :(
				if (value.endsWith(".0")) {
					value =  value.substring(0,value.length()-2);
				}
			}
			else {
				slf4jlogger.error("Invalid JsonValue.ValueType {} string in {} content item for concept {} ",valueType,concept);
			}
		}
		return value;
	}

	protected String getSingleStringValueOrNullFromJsonContentItemValue(JsonValue entry,String valueType,CodedSequenceItem concept) {
		String value = null;
		if (entry != null && entry.getValueType() == JsonValue.ValueType.STRING) {
			value = ((JsonString)entry).getString();
			if (value == null || value.length() == 0) {
				slf4jlogger.error("Missing or empty string value in {} content item for concept {} ",valueType,concept);
			}
		}
		else {
			slf4jlogger.error("Missing or invalid type of value string in {} content item for concept {} ",valueType,concept);
		}
		return value;
	}

	protected boolean haveChildrenForSingleStringOrCodeJsonContentItemValue(JsonValue contentItemValue,JsonObject contentItemAttributesObject) {
		return contentItemValue != null && contentItemValue.getValueType() == JsonValue.ValueType.ARRAY && ((JsonArray)contentItemValue).size() > (contentItemAttributesObject == null ? 1 : 2);
	}
	
	protected String determineUnknownValueType(String parentValueType,JsonValue childObjectValue) {
		slf4jlogger.debug("determineUnknownValueType(): parentValueType {} childObjectValue {}",parentValueType,childObjectValue);
		int firstEntryIndex = -1;
		if (childObjectValue != null && childObjectValue.getValueType() == JsonValue.ValueType.ARRAY) {
			JsonArray childValueAndChildrenArray = (JsonArray)childObjectValue;
			JsonObject contentItemAttributesObject = getContentItemAttributesObject(childValueAndChildrenArray);
			if (contentItemAttributesObject != null) {
				if (childValueAndChildrenArray.size() > 1) {
					firstEntryIndex = 1;
					slf4jlogger.debug("determineUnknownValueType(): have attribute object preceding value and children array");
				}
			}
			else {
				firstEntryIndex = 0;
				slf4jlogger.debug("determineUnknownValueType(): no attribute object preceding value and children array");
			}
			slf4jlogger.debug("determineUnknownValueType(): firstEntryIndex {}",firstEntryIndex);
			
			// regardless of parentValueType
			if (contentItemAttributesObject != null) {
				slf4jlogger.debug("determineUnknownValueType(): have attribute object so checking for IMAGE, WAVEFORM, COMPOSITE");
				{
					JsonValue attributeValue = contentItemAttributesObject.get(reservedKeywordForReferencedSOPClassUIDAttributeInCompositeContentItem);
					if (attributeValue != null && attributeValue.getValueType() == JsonValue.ValueType.STRING) {
						String referencedSOPClassUID = ((JsonString)attributeValue).getString();
						slf4jlogger.debug("determineUnknownValueType(): referencedSOPClassUID is {}",referencedSOPClassUID);
						if (referencedSOPClassUID != null && referencedSOPClassUID.length() > 0) {
							referencedSOPClassUID = JSONRepresentationOfDicomObjectFactory.substituteUIDForKeywordIfPossible(referencedSOPClassUID);
							slf4jlogger.debug("determineUnknownValueType(): referencedSOPClassUID after key word replacement with UID is {}",referencedSOPClassUID);
							if (SOPClass.isImageStorage(referencedSOPClassUID)) {
								slf4jlogger.debug("determineUnknownValueType(): referencedSOPClassUID is recognized as IMAGE");
								return "IMAGE";
							}
							else if (SOPClass.isWaveform(referencedSOPClassUID)) {
								slf4jlogger.debug("determineUnknownValueType(): referencedSOPClassUID is recognized as WAVEFORM");
								return "WAVEFORM";
							}
							else if (SOPClass.isStorage(referencedSOPClassUID)) {
								slf4jlogger.debug("determineUnknownValueType(): referencedSOPClassUID is recognized as storage but not image or waveform, so assume COMPOSITE");
								return "COMPOSITE";
							}
						}
					}
				}
				{
					JsonValue attributeValue = contentItemAttributesObject.get(reservedKeywordFor2DCoordinatesAttributeInCoordinatesContentItem);
					if (attributeValue != null) {
						return "SCOORD";
					}
				}
				{
					JsonValue attributeValue = contentItemAttributesObject.get(reservedKeywordFor3DCoordinatesAttributeInCoordinatesContentItem);
					if (attributeValue != null) {
						return "SCOORD3D";
					}
				}
			}
		}
		return null;
	}
	
	protected static String selectTheOtherOfTwoStringsInSet(String method,String location,Set<String> theSet,String doNotWant) {
		slf4jlogger.debug("{}(): {}: is not {} so looking for the other choice",method,location,doNotWant);
		for (String s : theSet) {
			slf4jlogger.debug("{}(): {}: checking choice of {}",method,location,s);
			if (!s.equals(doNotWant)) {
				slf4jlogger.debug("{}(): {}: selected other choice than {} is {}",method,location,doNotWant,s);
				return s;
			}
		}
		return null;	// should not happen
	}

	protected String selectFromAlternativeValueTypesForBusinessName(String childLocation,String childBusinessName,String parentValueType,Set<String> childValueTypes,JsonValue childObjectValue) {
		String childValueType = null;
		if (childValueTypes != null && childValueTypes.size() > 0) {
			if (childValueTypes.size() == 1) {
				childValueType = childValueTypes.iterator().next();
			}
			else {
				slf4jlogger.debug("selectFromAlternativeValueTypesForBusinessName(): {}: Ambiguous choice of value types for {} - attempting to disambiguate",childLocation,childBusinessName);
				if (childValueTypes.size() == 2) {
					slf4jlogger.debug("selectFromAlternativeValueTypesForBusinessName(): {}: Have two choices",childLocation);
					// use case: value type includes CONTAINER and any other value type - assume that if single String value is present for child then it is not a CONTAINER
					if (childValueTypes.contains("CONTAINER")) {
						if (childObjectValue != null
						 && childObjectValue.getValueType() == JsonValue.ValueType.STRING) {
							childValueType = selectTheOtherOfTwoStringsInSet("selectFromAlternativeValueTypesForBusinessName",childLocation,childValueTypes,"CONTAINER");
						}
						else {
							childValueType = "CONTAINER";	// blech :) This could fail if single string value was enclosed in an array of values of length 1 rather than being collapsed, annotations or children were present, etc.
							// but is needed to work around horrible sr_syngoviamm3dsc.dcm use of (111535,"DCM","DateTime problem observed") as both CONTAINER and DATETIME :(
						}
					}
					else if (childValueTypes.contains("CODE")
						  && (childValueTypes.contains("TEXT") || childValueTypes.contains("DATE") || childValueTypes.contains("TIME") || childValueTypes.contains("DATETIME") || childValueTypes.contains("UIDREF"))) {
						// use case: value type includes CODE and TEXT or similar only - disambiguate based on whether single string value type and not amongst business names
						// is needed for sr_syngoviamm3dsc.dcm use of (121401,"DCM","Derivation") as both CODE and TEXT (which is quite legal)
						slf4jlogger.debug("selectFromAlternativeValueTypesForBusinessName(): {}: is CODE or something TEXT-like",childLocation);
						if (childObjectValue != null
						 && childObjectValue.getValueType() == JsonValue.ValueType.STRING) {
							String stringValue = ((JsonString)childObjectValue).getString();
							slf4jlogger.debug("selectFromAlternativeValueTypesForBusinessName(): {}: have child stringValue of {}",childLocation,stringValue);
							if (businessNames.keySet().contains(stringValue)) {
								childValueType = "CODE";
							}
							else {
								childValueType = selectTheOtherOfTwoStringsInSet("selectFromAlternativeValueTypesForBusinessName",childLocation,childValueTypes,"CODE");
							}
						}
						// else can't tell
					}
				}
			}
		}
		return childValueType;
	}

	protected String selectFromAlternativeRelationshipTypesForBusinessName(String childLocation,String childBusinessName,String parentValueType,String childValueType,Set<String> childRelationshipTypes) {
		String childRelationshipType =  null;
		if (childRelationshipTypes != null && childRelationshipTypes.size() > 0) {
			if (childRelationshipTypes.size() == 1) {
				childRelationshipType = childRelationshipTypes.iterator().next();
			}
			else {
				slf4jlogger.debug("selectFromAlternativeRelationshipTypesForBusinessName(): {}: Ambiguous choice of relationship types for {} - attempting to disambiguate",childLocation,childBusinessName);
				// use case: two choices available, any child value type of CONTAINER parent used either as CONTAINS or (something other than HAS CONCEPT MOD, HAS ACQ CONTEXT or HAS OBS CONTEXT)
				// use case: any child value type used either as CONTAINS or HAS PROPERTIES - disambiguate based on CONTAINER parent or not
				if (childRelationshipTypes.size() == 2) {
					slf4jlogger.debug("selectFromAlternativeRelationshipTypesForBusinessName(): {}: Have two choices",childLocation);
					if (childRelationshipTypes.contains("CONTAINS")
					 && !childRelationshipTypes.contains("HAS CONCEPT MOD")		// also valid for CONTAINER parent, so can't use parentValueType of CONTAINER to choose
					 && !childRelationshipTypes.contains("HAS ACQ CONTEXT")		// also valid for CONTAINER parent, so can't use parentValueType of CONTAINER to choose
					 && !childRelationshipTypes.contains("HAS OBS CONTEXT")		// also valid for CONTAINER parent, so can't use parentValueType of CONTAINER to choose
					) {
						slf4jlogger.debug("selectFromAlternativeRelationshipTypesForBusinessName(): {}: Have choice of CONTAINS and one other than AS CONCEPT MOD, HAS ACQ CONTEXT or HAS OBS CONTEXT",childLocation);
						if (parentValueType.equals("CONTAINER")) {
							slf4jlogger.debug("selectFromAlternativeRelationshipTypesForBusinessName(): {}: parent is CONTAINER so use CONTAINS",childLocation);
							childRelationshipType = "CONTAINS";
						}
						else {
							// parent is not CONTAINER so use the other choice than CONTAINS
							childRelationshipType = selectTheOtherOfTwoStringsInSet("selectFromAlternativeRelationshipTypesForBusinessName",childLocation,childRelationshipTypes,"CONTAINS");
						}
					}
				}
			}
		}
		return childRelationshipType;
	}
	
	protected String determineUnknownRelationshipType(String parentValueType,String childValueType,JsonValue childObjectValue) {
		slf4jlogger.debug("determineUnknownRelationshipType(): parentValueType {} childValueType {}",parentValueType,childValueType);
		if (parentValueType.equals("TEXT") || parentValueType.equals("CODE") || parentValueType.equals("NUM")) {	// per PS3.3 Table A.35.3-2. Relationship Content Constraints for Comprehensive SR IOD
			if ("SCOORD".equals(childValueType)) {	// allow for null, e.g. by reference
				return "INFERRED FROM";
			}
		}
		if (parentValueType.equals("SCOORD")) {	// per PS3.3 Table A.35.3-2. Relationship Content Constraints for Comprehensive SR IOD
			if (childValueType == null || childValueType.equals("IMAGE")) {	// null is for by reference
				return "SELECTED FROM";
			}
		}
		if (parentValueType.equals("CONTAINER")) {
			return "CONTAINS";
		}
		return null;
	}

	// the attributes that describe a content item (such as label and template ID) are distinguished from the succeeding values and children by being a JSON OBJECT rather than an ARRAY
	protected JsonObject getContentItemAttributesObject(JsonArray contentItemAttributesAndChildrenArray) {
		JsonObject contentItemAttributesObject = null;
		if (contentItemAttributesAndChildrenArray.size() > 0) {
			JsonValue firstEntryCandidate = contentItemAttributesAndChildrenArray.get(0);
			if (firstEntryCandidate != null && firstEntryCandidate.getValueType() == JsonValue.ValueType.OBJECT) {
				contentItemAttributesObject = (JsonObject)firstEntryCandidate;
				slf4jlogger.debug("getContentItemAttributesObject(): content item has attributes");
			}
		}
		return contentItemAttributesObject;
	}

	protected Map<String,String> referencedContentItemIdentifiersBySimplifiedLabel = new HashMap<String,String>();

	protected ContentItem getContentItemAndChildrenFromJSONObjectValue(String businessName,CodedSequenceItem concept,String valueType,String relationshipType,JsonValue contentItemValue,String location) throws DicomException {
		slf4jlogger.debug("getContentItemAndChildrenFromJSONObjectValue(): {}:  businessName = {}",location,businessName);
		if (slf4jlogger.isDebugEnabled()) slf4jlogger.debug("getContentItemAndChildrenFromJSONObjectValue(): {}: concept = {}",location,concept);
		ContentItem contentItem = null;
		if (contentItemValue != null) {
			slf4jlogger.debug("getContentItemAndChildrenFromJSONObjectValue(): {}: valueType = {}",location,valueType);
			slf4jlogger.debug("getContentItemAndChildrenFromJSONObjectValue(): {}: relationshipType = {}",location,relationshipType);
			String observationDateTime = null;
			String observationUID = null;
			JsonArray contentItemChildren = null;			// as we process each type of SR content item, we will find and populate this, if not a leaf
			JsonObject contentItemAttributesObject = null;	// may not be any attributes
			int firstEntryIndex = -1;						// flag that there are no entries
			JsonValue firstEntryCandidate = null;			// may be empty if container and no value (does not include contentItemAttributesObject, which have already been extracted)
			JsonArray lastEntryInValueArray = null;			// depending on SR content item value type, this will probably be what we want for contentItemChildren
			JsonArray contentItemValueAndChildrenArray = null;
			if (contentItemValue != null && contentItemValue.getValueType() == JsonValue.ValueType.ARRAY) {
				contentItemValueAndChildrenArray = (JsonArray)contentItemValue;
				if (contentItemValueAndChildrenArray.size() > 0) {
					contentItemAttributesObject = getContentItemAttributesObject(contentItemValueAndChildrenArray);
					if (contentItemAttributesObject != null) {
						if (contentItemValueAndChildrenArray.size() > 1) {
							firstEntryIndex = 1;
							slf4jlogger.debug("getContentItemAndChildrenFromJSONObjectValue(): {}: have attribute object preceding value and children array",location);
						}
					}
					else {
						firstEntryIndex = 0;
						slf4jlogger.debug("getContentItemAndChildrenFromJSONObjectValue(): {}: no attribute object preceding value and children array",location);
					}
					if (firstEntryIndex >= 0) {
						firstEntryCandidate = contentItemValueAndChildrenArray.get(firstEntryIndex);
					}

					JsonValue lastEntryCandidate = contentItemValueAndChildrenArray.get(contentItemValueAndChildrenArray.size()-1);
					if (lastEntryCandidate != null && lastEntryCandidate.getValueType() == JsonValue.ValueType.ARRAY) {
						lastEntryInValueArray = (JsonArray)(lastEntryCandidate);
						slf4jlogger.debug("getContentItemAndChildrenFromJSONObjectValue(): {}: have potential children in last array entry",location);
					}
				}
			}
			String referencedLabelString = null;
			{
				if (contentItemAttributesObject != null) {
					{
						JsonValue labelValue = contentItemAttributesObject.get(reservedKeywordForSimplifiedLabelAttributeInSRFile);
						if (labelValue != null && labelValue.getValueType() == JsonValue.ValueType.STRING) {
							String labelString = ((JsonString)labelValue).getString();
							slf4jlogger.debug("getContentItemAndChildrenFromJSONObjectValue(): {}: have labelString {}",location,labelString);
							referencedContentItemIdentifiersBySimplifiedLabel.put(labelString,location);
						}
					}
					{
						JsonValue referencedLabelValue = contentItemAttributesObject.get(reservedKeywordForSimplifiedReferenceToLabelAttributeInSRFile);
						if (referencedLabelValue != null && referencedLabelValue.getValueType() == JsonValue.ValueType.STRING) {
							referencedLabelString = ((JsonString)referencedLabelValue).getString();
							slf4jlogger.debug("getContentItemAndChildrenFromJSONObjectValue(): {}: have referencedLabelString {}",location,referencedLabelString);
						}
					}
					{
						JsonValue observationUIDValue = contentItemAttributesObject.get(reservedKeywordForObservationUIDAttributeInSRFile);
						if (observationUIDValue != null && observationUIDValue.getValueType() == JsonValue.ValueType.STRING) {
							observationUID = ((JsonString)observationUIDValue).getString();
							slf4jlogger.debug("getContentItemAndChildrenFromJSONObjectValue(): {}: have observationUID {}",location,observationUID);
						}
					}
					{
						JsonValue observationDateTimeValue = contentItemAttributesObject.get(reservedKeywordForObservationDateTimeAttributeInSRFile);
						if (observationDateTimeValue != null && observationDateTimeValue.getValueType() == JsonValue.ValueType.STRING) {
							observationDateTime = ((JsonString)observationDateTimeValue).getString();
							slf4jlogger.debug("getContentItemAndChildrenFromJSONObjectValue(): {}: have observationDateTime {}",location,observationDateTime);
						}
					}
				}
			}
			
			if (valueType != null) {
				if (valueType.equals("CONTAINER")) {
					String continuity = "SEPARATE";		// default if not specified
					String template = null;
					String templatemappingresource = null;
					if (contentItemAttributesObject != null) {
						for (String attributeName : contentItemAttributesObject.keySet()) {
							JsonValue attributeValue = contentItemAttributesObject.get(attributeName);
							if (attributeValue != null && attributeValue.getValueType() == JsonValue.ValueType.STRING) {
								String attributeValueString = ((JsonString)attributeValue).getString();
								if (attributeName.equals(reservedKeywordForContinuityOfContentAttributeInSRFile)) {
									continuity = attributeValueString;
									if (slf4jlogger.isDebugEnabled()) slf4jlogger.debug("getContentItemAndChildrenFromJSONObjectValue(): {}: continuity = {}",location,continuity);
								}
								else if (attributeName.equals(reservedKeywordForTemplateIdentifierAttributeInSRFile)) {
									template = attributeValueString;
									if (slf4jlogger.isDebugEnabled()) slf4jlogger.debug("getContentItemAndChildrenFromJSONObjectValue(): {}: template = {}",location,template);
								}
								else if (attributeName.equals(reservedKeywordForTemplateMappingResourceAttributeInSRFile)) {
									templatemappingresource = attributeValueString;
									if (slf4jlogger.isDebugEnabled()) slf4jlogger.debug("getContentItemAndChildrenFromJSONObjectValue(): {}: templatemappingresource = {}",location,templatemappingresource);
								}
								else if (!isCommonAnnotationAttribute(attributeName)) {
									slf4jlogger.warn("{}: Unrecognized CONTAINER attribute {} for concept {}",location,attributeName,concept);
								}
							}
							else {
								slf4jlogger.error("{}: Incorrect JSON type for value of attribute {} for concept {}",location,attributeName,concept);
							}
						}
					}
					
					if (firstEntryCandidate != null && firstEntryCandidate.getValueType() == JsonValue.ValueType.ARRAY) {
						contentItemChildren = (JsonArray)firstEntryCandidate;
						if (slf4jlogger.isDebugEnabled()) slf4jlogger.debug("getContentItemAndChildrenFromJSONObjectValue(): {}: CONTAINER with children but no attributes concept = {}",location,concept);
					}
					else {
						slf4jlogger.error("{}: Malformed non-empty array for CONTAINER content item for concept {}",location,concept);
					}
					
					contentItem = contentItemFactory.makeContainerContentItem(
																			  null /* parent will be set later by addChild() operation */,
																			  relationshipType,
																			  concept,
																			  continuity.equals("SEPARATE"),
																			  templatemappingresource,template,
																			  observationDateTime,observationUID);
				}
				else if (valueType.equals("CODE")) {
					CodedSequenceItem value = null;
					String valueBusinessName = null;
					if (contentItemValue != null && contentItemValue.getValueType() == JsonValue.ValueType.STRING) {
						valueBusinessName = ((JsonString)contentItemValue).getString();
					}
					else if (firstEntryCandidate != null && firstEntryCandidate.getValueType() == JsonValue.ValueType.STRING) {
						valueBusinessName = ((JsonString)firstEntryCandidate).getString();
					}
					if (valueBusinessName != null) {
						value = getCodedSequenceItemForBusinessNameUsedAsValue(valueBusinessName,location);
						if (value == null) {
							slf4jlogger.error("{}: Unrecognized business name {} for value in CODE content item for concept {}",location,valueBusinessName,concept);
						}
					}
					else {
						slf4jlogger.error("{}: Missing business name string in CODE content item for concept {}",location,concept);
					}
					if (haveChildrenForSingleStringOrCodeJsonContentItemValue(contentItemValue,contentItemAttributesObject)) {
						contentItemChildren = lastEntryInValueArray;
					}
					
					contentItem = contentItemFactory.makeCodeContentItem(
																		 null /* parent will be set later by addChild() operation */,
																		 relationshipType,
																		 concept,
																		 value,
																		 observationDateTime,observationUID);
				}
				else if (valueType.equals("NUM")) {
					String value = null;
					CodedSequenceItem units = null;
					Double floatingPointValue = null;
					Integer rationalNumeratorValue = null;
					Long rationalDenominatorValue = null;
					CodedSequenceItem qualifierCode = null;
					{
						if (contentItemAttributesObject != null) {
							for (String attributeName : contentItemAttributesObject.keySet()) {
								JsonValue attributeValue = contentItemAttributesObject.get(attributeName);
								if (attributeValue != null) {
									if (attributeValue.getValueType() == JsonValue.ValueType.STRING) {
										String attributeValueString = ((JsonString)attributeValue).getString();
										if (attributeName.equals(reservedKeywordForMeasurementUnitsAttributeInNumericContentItem)) {
											String unitsBusinessName = attributeValueString;
											if (slf4jlogger.isDebugEnabled()) slf4jlogger.debug("getContentItemAndChildrenFromJSONObjectValue(): {}: unitsBusinessName = {}",location,unitsBusinessName);
											units = getCodedSequenceItemForBusinessNameUsedAsUnits(unitsBusinessName,location);
										}
										else if (attributeName.equals(reservedKeywordForNumericValueQualifierAttributeInNumericContentItem)) {
											String businessNameForQualifierCode = attributeValueString;
											if (slf4jlogger.isDebugEnabled()) slf4jlogger.debug("getContentItemAndChildrenFromJSONObjectValue(): {}: businessNameForQualifierCode = {}",location,businessNameForQualifierCode);
											qualifierCode = getCodedSequenceItemForBusinessNameUsedAsUnits(businessNameForQualifierCode,location);
										}
										else if (!isCommonAnnotationAttribute(attributeName)) {
											slf4jlogger.warn("{}: Unrecognized NUM attribute {} for concept {}",location,attributeName,concept);
										}
									}
									else if (attributeValue.getValueType() == JsonValue.ValueType.NUMBER) {
										double attributeValueNumber = ((JsonNumber)attributeValue).doubleValue();
										if (attributeName.equals(reservedKeywordForFloatingPointValueAttributeInNumericContentItem)) {
											floatingPointValue = new Double(attributeValueNumber);
										}
										else if (attributeName.equals(reservedKeywordForRationalNumeratorAttributeInNumericContentItem)) {
											rationalNumeratorValue = new Integer((int)attributeValueNumber);
										}
										else if (attributeName.equals(reservedKeywordForRationalDenominatorAttributeInNumericContentItem)) {
											rationalDenominatorValue = new Long((long)attributeValueNumber);
										}
										else if (!isCommonAnnotationAttribute(attributeName)) {
											slf4jlogger.warn("{}: Unrecognized NUM attribute {} for concept {}",location,attributeName,concept);
										}
									}
								}
							}
						}
						else {
							slf4jlogger.error("{}: No units in NUM content item for concept {} ",location,concept);
						}
						if (units == null) {
							slf4jlogger.error("{}: Missing or empty or unrecognized units in NUM content item for concept {}",location,concept);
						}
					}
					{
						JsonValue useValue = null;
						// should we be using, allowing, checking for NUMBER rather than STRING? E.g., consistent with CP 1861 that allows either for Annex F JSON :(
						if (contentItemValue != null && (contentItemValue.getValueType() == JsonValue.ValueType.STRING || contentItemValue.getValueType() == JsonValue.ValueType.NUMBER)) {
							useValue = contentItemValue;
						}
						else if (firstEntryCandidate != null && (firstEntryCandidate.getValueType() == JsonValue.ValueType.STRING || firstEntryCandidate.getValueType() == JsonValue.ValueType.NUMBER)) {
							useValue = firstEntryCandidate;
						}
						value = getSingleStringValueOrNullFromJsonStringOrNumberContentItemValue(useValue,valueType,concept);
						if (value == null) {
							slf4jlogger.error("{}: No value in NUM content item for concept {}",location,concept);
						}
					}

					if (haveChildrenForSingleStringOrCodeJsonContentItemValue(contentItemValue,contentItemAttributesObject)) {
						contentItemChildren = lastEntryInValueArray;
					}
					
					contentItem = contentItemFactory.makeNumericContentItem(
																			null /* parent will be set later by addChild() operation */,
																			relationshipType,
																			concept,
																			value,
																			floatingPointValue,
																			rationalNumeratorValue,
																			rationalDenominatorValue,
																			units,
																			qualifierCode,
																			observationDateTime,observationUID);
				}
				else if (valueType.equals("DATETIME")) {
					JsonValue useValue = null;
					if (contentItemValue != null && contentItemValue.getValueType() == JsonValue.ValueType.STRING) {
						useValue = contentItemValue;
					}
					else if (firstEntryCandidate != null && firstEntryCandidate.getValueType() == JsonValue.ValueType.STRING) {
						useValue = firstEntryCandidate;
					}
					String value = getSingleStringValueOrNullFromJsonContentItemValue(useValue,valueType,concept);
					if (haveChildrenForSingleStringOrCodeJsonContentItemValue(contentItemValue,contentItemAttributesObject)) {
						contentItemChildren = lastEntryInValueArray;
					}
					contentItem = contentItemFactory.makeDateTimeContentItem(
																			 null /* parent will be set later by addChild() operation */,
																			 relationshipType,
																			 concept,
																			 value,
																			 observationDateTime,observationUID);
				}
				else if (valueType.equals("DATE")) {
					JsonValue useValue = null;
					if (contentItemValue != null && contentItemValue.getValueType() == JsonValue.ValueType.STRING) {
						useValue = contentItemValue;
					}
					else if (firstEntryCandidate != null && firstEntryCandidate.getValueType() == JsonValue.ValueType.STRING) {
						useValue = firstEntryCandidate;
					}
					String value = getSingleStringValueOrNullFromJsonContentItemValue(useValue,valueType,concept);
					if (haveChildrenForSingleStringOrCodeJsonContentItemValue(contentItemValue,contentItemAttributesObject)) {
						contentItemChildren = lastEntryInValueArray;
					}
					contentItem = contentItemFactory.makeDateContentItem(
																		 null /* parent will be set later by addChild() operation */,
																		 relationshipType,
																		 concept,
																		 value,
																		 observationDateTime,observationUID);
				}
				else if (valueType.equals("TIME")) {
					JsonValue useValue = null;
					if (contentItemValue != null && contentItemValue.getValueType() == JsonValue.ValueType.STRING) {
						useValue = contentItemValue;
					}
					else if (firstEntryCandidate != null && firstEntryCandidate.getValueType() == JsonValue.ValueType.STRING) {
						useValue = firstEntryCandidate;
					}
					String value = getSingleStringValueOrNullFromJsonContentItemValue(useValue,valueType,concept);
					if (haveChildrenForSingleStringOrCodeJsonContentItemValue(contentItemValue,contentItemAttributesObject)) {
						contentItemChildren = lastEntryInValueArray;
					}
					contentItem = contentItemFactory.makeTimeContentItem(
																		 null /* parent will be set later by addChild() operation */,
																		 relationshipType,
																		 concept,
																		 value,
																		 observationDateTime,observationUID);
				}
				else if (valueType.equals("PNAME")) {
					String value = null;
					JsonValue useValue = null;
					if (contentItemValue != null && contentItemValue.getValueType() == JsonValue.ValueType.STRING) {
						useValue = contentItemValue;
					}
					else if (firstEntryCandidate != null && firstEntryCandidate.getValueType() == JsonValue.ValueType.STRING) {
						useValue = firstEntryCandidate;
					}
					if (useValue != null) {
						slf4jlogger.debug("getContentItemAndChildrenFromJSONObjectValue(): {}: PNAME encoded as single string not attributes",location);
						value = getSingleStringValueOrNullFromJsonContentItemValue(useValue,valueType,concept);
						if (haveChildrenForSingleStringOrCodeJsonContentItemValue(contentItemValue,contentItemAttributesObject)) {
							contentItemChildren = lastEntryInValueArray;
						}
					}
					else if (contentItemAttributesObject != null) {
						slf4jlogger.debug("getContentItemAndChildrenFromJSONObjectValue(): {}: PNAME extracting components from contentItemAttributesObject",location);
						value = JSONRepresentationOfDicomObjectFactory.getJsonPersonNameFromPropertiesInJsonObject(contentItemAttributesObject,
							reservedKeywordForAlphabeticPropertyInPersonNameContentItem,
							reservedKeywordForIdeographicPropertyInPersonNameContentItem,
							reservedKeywordForPhoneticPropertyInPersonNameContentItem);
						slf4jlogger.debug("getContentItemAndChildrenFromJSONObjectValue(): {}: PNAME is {}",location,value);
						if (lastEntryInValueArray != null && lastEntryInValueArray.getValueType() == JsonValue.ValueType.ARRAY) {	// hmm :(
							slf4jlogger.debug("getContentItemAndChildrenFromJSONObjectValue(): {}: PNAME have children",location);
							contentItemChildren = lastEntryInValueArray;
						}
						else {
							slf4jlogger.debug("getContentItemAndChildrenFromJSONObjectValue(): {}: PNAME has no children",location);
						}
					}
					
					contentItem = contentItemFactory.makePersonNameContentItem(
																			   null /* parent will be set later by addChild() operation */,
																			   relationshipType,
																			   concept,
																			   value,
																			   observationDateTime,observationUID);
				}
				else if (valueType.equals("UIDREF")) {
					JsonValue useValue = null;
					if (contentItemValue != null && contentItemValue.getValueType() == JsonValue.ValueType.STRING) {
						useValue = contentItemValue;
					}
					else if (firstEntryCandidate != null && firstEntryCandidate.getValueType() == JsonValue.ValueType.STRING) {
						useValue = firstEntryCandidate;
					}
					String value = getSingleStringValueOrNullFromJsonContentItemValue(useValue,valueType,concept);
					if (haveChildrenForSingleStringOrCodeJsonContentItemValue(contentItemValue,contentItemAttributesObject)) {
						contentItemChildren = lastEntryInValueArray;
					}
					contentItem = contentItemFactory.makeUIDContentItem(
																		null /* parent will be set later by addChild() operation */,
																		relationshipType,
																		concept,
																		value,
																		observationDateTime,observationUID);
				}
				else if (valueType.equals("TEXT")) {
					JsonValue useValue = null;
					if (contentItemValue != null && contentItemValue.getValueType() == JsonValue.ValueType.STRING) {
						useValue = contentItemValue;
					}
					else if (firstEntryCandidate != null && firstEntryCandidate.getValueType() == JsonValue.ValueType.STRING) {
						useValue = firstEntryCandidate;
					}
					String value = getSingleStringValueOrNullFromJsonContentItemValue(useValue,valueType,concept);
					if (haveChildrenForSingleStringOrCodeJsonContentItemValue(contentItemValue,contentItemAttributesObject)) {
						contentItemChildren = lastEntryInValueArray;
					}
					contentItem = contentItemFactory.makeTextContentItem(
																		 null /* parent will be set later by addChild() operation */,
																		 relationshipType,
																		 concept,
																		 value,
																		 observationDateTime,observationUID);
				}
				else if (valueType.equals("SCOORD")) {
					String graphicType = null;
					float[] graphicData = null;
					// need to add pixel origin and fiducial UID when supported by ContentItemFactory.SpatialCoordinatesContentItem (001182) :(

					if (contentItemAttributesObject != null) {
						for (String attributeName : contentItemAttributesObject.keySet()) {
							JsonValue attributeValue = contentItemAttributesObject.get(attributeName);
							if (attributeValue != null) {
								if (attributeValue.getValueType() == JsonValue.ValueType.STRING) {
									String attributeValueString = ((JsonString)attributeValue).getString();
									if (attributeName.equals(reservedKeywordForGraphicTypeAttributeInCoordinatesContentItem)) {
										graphicType = attributeValueString;
										if (slf4jlogger.isDebugEnabled()) slf4jlogger.debug("getContentItemAndChildrenFromJSONObjectValue(): {}: graphicType = {}",location,graphicType);
									}
									else if (!isCommonAnnotationAttribute(attributeName)) {
										slf4jlogger.warn("{}: Unrecognized SCOORD attribute {} for concept {}",location,attributeName,concept);
									}
								}
								else if (attributeValue.getValueType() == JsonValue.ValueType.ARRAY) {
									JsonArray attributeValueArray = (JsonArray)attributeValue;
									if (attributeName.equals(reservedKeywordFor2DCoordinatesAttributeInCoordinatesContentItem)) {
										int n = attributeValueArray.size();
										if (n > 0) {
											graphicData = new float[n];
											for (int i=0; i<n; ++i) {
												JsonValue arrayValue = attributeValueArray.get(i);
												if (arrayValue != null && arrayValue.getValueType() == JsonValue.ValueType.NUMBER) {
													graphicData[i] = (float)(((JsonNumber)arrayValue).doubleValue());
												}
												else {
													slf4jlogger.error("{}: Missing graphicData array value type {} in SCOORD content item for concept {} ",location,arrayValue.getValueType(),concept);
												}
											}
										}
									}
									else if (!isCommonAnnotationAttribute(attributeName)) {
										slf4jlogger.warn("{}: Unrecognized SCOORD attribute {} for concept {}",location,attributeName,concept);
									}
								}
							}
						}
					}
					else {
						slf4jlogger.error("{}: No graphic type and coordinates in SCOORD content item for concept {} ",location,concept);
					}

					if (graphicType == null || graphicType.length() == 0) {
						slf4jlogger.error("{}: Missing or empty graphicType in SCOORD content item for concept {}",location,concept);
					}
					if (graphicData == null) {
						slf4jlogger.error("{}: Missing graphicData array in SCOORD content item for concept {}",location,concept);
					}

					// check we have children ... we have no value per se, so if there is anything beyond the attributes, that will be the child array
					if (contentItemValueAndChildrenArray.size() > (contentItemAttributesObject == null ? 0 : 1)) {
						contentItemChildren = lastEntryInValueArray;
					}

					contentItem = contentItemFactory.makeSpatialCoordinatesContentItem(
																					   null /* parent will be set later by addChild() operation */,
																					   relationshipType,
																					   concept,
																					   graphicType,
																					   graphicData,
																					   observationDateTime,observationUID);
				}
				else if (valueType.equals("SCOORD3D")) {
					String graphicType = null;
					float[] graphicData = null;
					String referencedFrameOfReferenceUID = null;
					// need to add fiducial UID when supported by ContentItemFactory.SpatialCoordinates3DContentItem (001182) :(

					if (contentItemAttributesObject != null) {
						for (String attributeName : contentItemAttributesObject.keySet()) {
							JsonValue attributeValue = contentItemAttributesObject.get(attributeName);
							if (attributeValue != null) {
								if (attributeValue.getValueType() == JsonValue.ValueType.STRING) {
									String attributeValueString = ((JsonString)attributeValue).getString();
									if (attributeName.equals(reservedKeywordForGraphicTypeAttributeInCoordinatesContentItem)) {
										graphicType = attributeValueString;
										if (slf4jlogger.isDebugEnabled()) slf4jlogger.debug("getContentItemAndChildrenFromJSONObjectValue(): {}: graphicType = {}",location,graphicType);
									}
									else if (attributeName.equals(reservedKeywordForReferencedFrameOfReferenceUIDAttributeInCoordinatesContentItem)) {
										referencedFrameOfReferenceUID = attributeValueString;
										if (slf4jlogger.isDebugEnabled()) slf4jlogger.debug("getContentItemAndChildrenFromJSONObjectValue(): {}: referencedFrameOfReferenceUID = {}",location,referencedFrameOfReferenceUID);
									}
									else if (!isCommonAnnotationAttribute(attributeName)) {
										slf4jlogger.warn("{}: Unrecognized SCOORD3D attribute {} for concept {}",location,attributeName,concept);
									}
								}
								else if (attributeValue.getValueType() == JsonValue.ValueType.ARRAY) {
									JsonArray attributeValueArray = (JsonArray)attributeValue;
									if (attributeName.equals(reservedKeywordFor3DCoordinatesAttributeInCoordinatesContentItem)) {
										int n = attributeValueArray.size();
										if (n > 0) {
											graphicData = new float[n];
											for (int i=0; i<n; ++i) {
												JsonValue arrayValue = attributeValueArray.get(i);
												if (arrayValue != null && arrayValue.getValueType() == JsonValue.ValueType.NUMBER) {
													graphicData[i] = (float)(((JsonNumber)arrayValue).doubleValue());
												}
												else {
													slf4jlogger.error("{}: Missing graphicData array value type {} in SCOORD content item for concept {} ",location,arrayValue.getValueType(),concept);
												}
											}
										}
									}
									else if (!isCommonAnnotationAttribute(attributeName)) {
										slf4jlogger.warn("{}: Unrecognized SCOORD3D attribute {} for concept {}",location,attributeName,concept);
									}								}
							}
						}
					}
					else {
						slf4jlogger.error("{}: No graphic type and coordinates in SCOORD content item for concept {} ",location,concept);
					}

					if (graphicType == null || graphicType.length() == 0) {
						slf4jlogger.error("{}: Missing or empty graphicType in SCOORD content item for concept {}",location,concept);
					}
					if (graphicData == null) {
						slf4jlogger.error("{}: Missing graphicData array in SCOORD content item for concept {}",location,concept);
					}
					if (referencedFrameOfReferenceUID == null || referencedFrameOfReferenceUID.length() == 0) {
						slf4jlogger.error("{}: Missing or empty referencedFrameOfReferenceUID in SCOORD content item for concept {}",location,concept);
					}

					// check we have children ... we have no value per se, so if there is anything beyond the attributes, that will be the child array
					if (contentItemValueAndChildrenArray.size() > (contentItemAttributesObject == null ? 0 : 1)) {
						contentItemChildren = lastEntryInValueArray;
					}

					contentItem = contentItemFactory.makeSpatialCoordinates3DContentItem(
																					   null /* parent will be set later by addChild() operation */,
																					   relationshipType,
																					   concept,
																					   graphicType,
																					   graphicData,
																					   referencedFrameOfReferenceUID,
																					   observationDateTime,observationUID);
				}
				else if (valueType.equals("IMAGE")) {
					if (slf4jlogger.isDebugEnabled()) slf4jlogger.debug("getContentItemAndChildrenFromJSONObjectValue(): {}: populating IMAGE content item",location);
					String referencedSOPClassUID = null;
					String referencedSOPInstanceUID = null;
					int referencedFrameNumber = 0;	// need to handle multiple values as array (001181) :(
					int referencedSegmentNumber = 0;
					String presentationStateSOPClassUID = null;
					String presentationStateSOPInstanceUID = null;
					String realWorldValueMappingSOPClassUID = null;
					String realWorldValueMappingSOPInstanceUID = null;
					
					if (contentItemAttributesObject != null) {
						for (String attributeName : contentItemAttributesObject.keySet()) {
							JsonValue attributeValue = contentItemAttributesObject.get(attributeName);
							if (attributeValue != null && attributeValue.getValueType() == JsonValue.ValueType.STRING) {
								String attributeValueString = ((JsonString)attributeValue).getString();
								if (attributeName.equals(reservedKeywordForReferencedSOPClassUIDAttributeInCompositeContentItem)) {
									referencedSOPClassUID = attributeValueString;
									slf4jlogger.debug("getContentItemAndChildrenFromJSONObjectValue(): {}: referencedSOPClassUID = {}",location,referencedSOPClassUID);
									referencedSOPClassUID = JSONRepresentationOfDicomObjectFactory.substituteUIDForKeywordIfPossible(referencedSOPClassUID);
									slf4jlogger.debug("getContentItemAndChildrenFromJSONObjectValue(): {}: referencedSOPClassUID after replacement of keyword with UID = {}",location,referencedSOPClassUID);
								}
								else if (attributeName.equals(reservedKeywordForReferencedSOPInstanceUIDAttributeInCompositeContentItem)) {
									referencedSOPInstanceUID = attributeValueString;
									slf4jlogger.debug("getContentItemAndChildrenFromJSONObjectValue(): {}: referencedSOPInstanceUID = {}",location,referencedSOPInstanceUID);
								}
								else if (attributeName.equals(reservedKeywordForPresentationStateSOPClassUIDAttributeInCompositeContentItem)) {
									presentationStateSOPClassUID = attributeValueString;
									slf4jlogger.debug("getContentItemAndChildrenFromJSONObjectValue(): {}: presentationStateSOPClassUID = {}",location,presentationStateSOPClassUID);
									presentationStateSOPClassUID = JSONRepresentationOfDicomObjectFactory.substituteUIDForKeywordIfPossible(presentationStateSOPClassUID);
									slf4jlogger.debug("getContentItemAndChildrenFromJSONObjectValue(): {}: presentationStateSOPClassUID after replacement of keyword with UID = {}",location,presentationStateSOPClassUID);
								}
								else if (attributeName.equals(reservedKeywordForPresentationStateSOPInstanceUIDAttributeInCompositeContentItem)) {
									presentationStateSOPInstanceUID = attributeValueString;
									slf4jlogger.debug("getContentItemAndChildrenFromJSONObjectValue(): {}: presentationStateSOPInstanceUID = {}",location,presentationStateSOPInstanceUID);
								}
								else if (attributeName.equals(reservedKeywordForRealWorldValueMappingSOPClassUIDAttributeInCompositeContentItem)) {
									realWorldValueMappingSOPClassUID = attributeValueString;
									slf4jlogger.debug("getContentItemAndChildrenFromJSONObjectValue(): {}: realWorldValueMappingSOPClassUID = {}",location,realWorldValueMappingSOPClassUID);
									realWorldValueMappingSOPClassUID = JSONRepresentationOfDicomObjectFactory.substituteUIDForKeywordIfPossible(realWorldValueMappingSOPClassUID);
									slf4jlogger.debug("getContentItemAndChildrenFromJSONObjectValue(): {}: realWorldValueMappingSOPClassUID after replacement of keyword with UID = {}",location,realWorldValueMappingSOPClassUID);
								}
								else if (attributeName.equals(reservedKeywordForRealWorldValueMappingSOPInstanceUIDAttributeInCompositeContentItem)) {
									realWorldValueMappingSOPInstanceUID = attributeValueString;
									slf4jlogger.debug("getContentItemAndChildrenFromJSONObjectValue(): {}: realWorldValueMappingSOPInstanceUID = {}",location,realWorldValueMappingSOPInstanceUID);
								}
								else if (!isCommonAnnotationAttribute(attributeName)) {
									slf4jlogger.warn("{}: Unrecognized IMAGE String attribute {} for concept {}",location,attributeName,concept);
								}
							}
							else if (attributeValue != null && attributeValue.getValueType() == JsonValue.ValueType.NUMBER) {
								int attributeValueNumber = (int)(((JsonNumber)attributeValue).intValue());
								if (attributeName.equals(reservedKeywordForReferencedFrameNumberAttributeInCompositeContentItem)) {
									referencedFrameNumber = attributeValueNumber;
									if (slf4jlogger.isDebugEnabled()) slf4jlogger.debug("getContentItemAndChildrenFromJSONObjectValue(): {}: referencedFrameNumber = {}",location,referencedFrameNumber);
								}
								else if (attributeName.equals(reservedKeywordForReferencedSegmentNumberAttributeInCompositeContentItem)) {
									referencedSegmentNumber = attributeValueNumber;
									if (slf4jlogger.isDebugEnabled()) slf4jlogger.debug("getContentItemAndChildrenFromJSONObjectValue(): {}: referencedSegmentNumber = {}",location,referencedSegmentNumber);
								}
								else if (!isCommonAnnotationAttribute(attributeName)) {
									slf4jlogger.warn("{}: Unrecognized IMAGE Number attribute {} for concept {}",location,attributeName,concept);
								}
							}
							// also need to handle multiple values of referencedFrameNumber as array (001181) :(
							else {
								slf4jlogger.error("{}: Incorrect JSON type for value of attribute {} for concept {}",location,attributeName,concept);
							}
						}
					}
					else {
						slf4jlogger.error("{}: No SOP Class and SOP Instance in IMAGE content item for concept {} ",location,concept);
					}
					
					// check we have children ... we have no value per se, so if there is anything beyond the attributes, that will be the child array
					if (contentItemValueAndChildrenArray.size() > (contentItemAttributesObject == null ? 0 : 1)) {
						contentItemChildren = lastEntryInValueArray;
					}
					
					contentItem = contentItemFactory.makeImageContentItem(
																		  null /* parent will be set later by addChild() operation */,
																		  relationshipType,
																		  concept,
																		  referencedSOPClassUID,
																		  referencedSOPInstanceUID,
																		  referencedFrameNumber,
																		  referencedSegmentNumber,
																		  presentationStateSOPClassUID,presentationStateSOPInstanceUID,
																		  realWorldValueMappingSOPClassUID,realWorldValueMappingSOPInstanceUID,
																		  observationDateTime,observationUID);
				}
				else if (valueType.equals("COMPOSITE")) {
					if (slf4jlogger.isDebugEnabled()) slf4jlogger.debug("getContentItemAndChildrenFromJSONObjectValue(): {}: populating COMPOSITE content item",location);
					String referencedSOPClassUID = null;
					String referencedSOPInstanceUID = null;
					
					if (contentItemAttributesObject != null) {
						for (String attributeName : contentItemAttributesObject.keySet()) {
							JsonValue attributeValue = contentItemAttributesObject.get(attributeName);
							if (attributeValue != null && attributeValue.getValueType() == JsonValue.ValueType.STRING) {
								String attributeValueString = ((JsonString)attributeValue).getString();
								if (attributeName.equals(reservedKeywordForReferencedSOPClassUIDAttributeInCompositeContentItem)) {
									referencedSOPClassUID = attributeValueString;
									slf4jlogger.debug("getContentItemAndChildrenFromJSONObjectValue(): {}: referencedSOPClassUID = {}",location,referencedSOPClassUID);
									referencedSOPClassUID = JSONRepresentationOfDicomObjectFactory.substituteUIDForKeywordIfPossible(referencedSOPClassUID);
									slf4jlogger.debug("getContentItemAndChildrenFromJSONObjectValue(): {}: referencedSOPClassUID after replacement of keyword with UID = {}",location,referencedSOPClassUID);
								}
								else if (attributeName.equals(reservedKeywordForReferencedSOPInstanceUIDAttributeInCompositeContentItem)) {
									referencedSOPInstanceUID = attributeValueString;
									slf4jlogger.debug("getContentItemAndChildrenFromJSONObjectValue(): {}: referencedSOPInstanceUID = {}",location,referencedSOPInstanceUID);
								}
								else if (!isCommonAnnotationAttribute(attributeName)) {
									slf4jlogger.warn("{}: Unrecognized COMPOSITE String attribute {} for concept {}",location,attributeName,concept);
								}
							}
							else {
								slf4jlogger.error("{}: Incorrect JSON type for value of attribute {} for concept {}",location,attributeName,concept);
							}
						}
					}
					else {
						slf4jlogger.error("{}: No SOP Class and SOP Instance in COMPOSITE content item for concept {} ",location,concept);
					}
					
					// check we have children ... we have no value per se, so if there is anything beyond the attributes, that will be the child array
					if (contentItemValueAndChildrenArray.size() > (contentItemAttributesObject == null ? 0 : 1)) {
						contentItemChildren = lastEntryInValueArray;
					}
					
					contentItem = contentItemFactory.makeCompositeContentItem(
																		  null /* parent will be set later by addChild() operation */,
																		  relationshipType,
																		  concept,
																		  referencedSOPClassUID,
																		  referencedSOPInstanceUID,
																		  observationDateTime,observationUID);
				}
				else {
					// unrecognized content item valueType ... so what ?
					slf4jlogger.error("{}: Unrecognized value type {} for concept {}",location,valueType,concept);
				}
			}
			else {
				slf4jlogger.debug("getContentItemAndChildrenFromJSONObjectValue(): {}: No value type so checking for by-reference",location);
				slf4jlogger.debug("getContentItemAndChildrenFromJSONObjectValue(): {}: referencedLabelString = {}",location,referencedLabelString);
				if (referencedLabelString != null) {
					String referencedContentItemIdentifier = referencedContentItemIdentifiersBySimplifiedLabel.get(referencedLabelString);
					if (referencedContentItemIdentifier != null) {
					slf4jlogger.debug("getContentItemAndChildrenFromJSONObjectValue(): {}: referencedLabelString {} maps to referencedContentItemIdentifier",location,referencedLabelString,referencedContentItemIdentifier);
						contentItem = new ContentItemWithReference(
							null /* parent will be set later by addChild() operation */,
							relationshipType,
							referencedContentItemIdentifier);
					}
					else {
						slf4jlogger.error("{}: Unable to resolve simplified label {}",location,referencedLabelString);
					}
				}
			}
			
			if (contentItemChildren != null && contentItemChildren.size() > 0) {
				int n = contentItemChildren.size();
				for (int i=0; i<n; ++i) {
					slf4jlogger.debug("getContentItemAndChildrenFromJSONObjectValue(): {}: Processing child # {}",location,i);
					String childLocation = location+"."+Integer.toString(i+1);
					JsonValue arrayValue = contentItemChildren.get(i);
					if (arrayValue != null && arrayValue.getValueType() == JsonValue.ValueType.OBJECT) {
						JsonObject contentItemChild = (JsonObject)arrayValue;
						Set<String> childBusinessNames = contentItemChild.keySet();
						if (childBusinessNames.size() == 1) {
							String childBusinessName = childBusinessNames.iterator().next();
							slf4jlogger.debug("getContentItemAndChildrenFromJSONObjectValue(): {}: JSON child businessName = {}",childLocation,childBusinessName);
							if (childBusinessName != null) {
								CodedSequenceItem childBusinessNameCode = getCodedSequenceItemForBusinessNameUsedAsConceptName(childBusinessName,childLocation);
								JsonValue childObjectValue = contentItemChild.get(childBusinessName);
								String childValueType = null;
								{
									Set<String> childValueTypes = valueTypesByBusinessName.get(childBusinessName);
									childValueType = selectFromAlternativeValueTypesForBusinessName(childLocation,childBusinessName,valueType,childValueTypes,childObjectValue);
								}
								slf4jlogger.debug("getContentItemAndChildrenFromJSONObjectValue(): {}: childValueType from lookup {} for {}",childLocation,childValueType,childBusinessName);
								
								if (childValueType == null) {
									childValueType = determineUnknownValueType(valueType,childObjectValue);
									slf4jlogger.debug("getContentItemAndChildrenFromJSONObjectValue(): {}: unknown childValueType determined to be {} for {}",childLocation,childValueType,childBusinessName);
								}
								
								if (childValueType == null) {
									slf4jlogger.debug("{}: Could not determine value type for child {} of {} {} - OK if this is a reference",childLocation,childBusinessName,valueType,businessName);
								}

								String childRelationshipType = null;
								{
									Set<String> childRelationshipTypes = relationshipTypesByBusinessName.get(childBusinessName);
									childRelationshipType = selectFromAlternativeRelationshipTypesForBusinessName(childLocation,childBusinessName,valueType,childValueType,childRelationshipTypes);
									slf4jlogger.debug("getContentItemAndChildrenFromJSONObjectValue(): {}: childRelationshipType from lookup {} for business name {}",childLocation,childRelationshipType,childBusinessName);
								}
								
								if (childRelationshipType == null) {
									childRelationshipType = determineUnknownRelationshipType(valueType,childValueType,childObjectValue);
									slf4jlogger.debug("getContentItemAndChildrenFromJSONObjectValue(): {}: unknown childRelationshipType determined to be {} for {}",childLocation,childRelationshipType,childBusinessName);
								}
								
								if (childRelationshipType == null) {
									slf4jlogger.error("{}: Could not determine relationship type for child {}",childLocation,childBusinessName);
								}
								
								// OK for childValueType == null && childRelationshipType == null if by reference
								ContentItem childContentItem = getContentItemAndChildrenFromJSONObjectValue(childBusinessName,childBusinessNameCode,childValueType,childRelationshipType,childObjectValue,childLocation);	// recurses ... will convert all its children now too
								if (childContentItem != null) {
									contentItem.addChild(childContentItem);
								}
							}
							else {
								slf4jlogger.debug("getContentItemAndChildrenFromJSONObjectValue(): {}: Ignoring anonymous business name for now",childLocation);	// can this even happen ?? :(
							}
						}
						else {
							slf4jlogger.error("{}: Expected only one entry for child object # {} in array of children",childLocation,i);
						}
					}
				}
			}
		}
		// else don't have value/children ... so what ? :(
		return contentItem;
	}
	
	/**
	 * <p>Given a DICOM SR object encoded in a JSON document
	 * convert it to a StructuredReport using the content tree and ignoring any header attributes.</p>
	 *
	 * @param		topLevelObject	the first object of the array that is the JSON document
	 * @return						the StructuredReport
	 * @throws	DicomException
	 */
	public StructuredReport getStructuredReport(JsonObject topLevelObject) throws DicomException {
		DicomDictionary dictionary = DicomDictionary.StandardDictionary;
		StructuredReport structuredReport = null;
		try {
			if (topLevelObject != null) {
				slf4jlogger.debug("Looking for SR container entry in top level object amongst all the DICOM keywords");
				String rootContainerBusinessName = null;
				CodedSequenceItem rootContainerBusinessNameCode = null;
				String location = "1";
				// a JsonObject is a Map<String,JsonValue>, so iterate through map entry keys
				for (String businessName : topLevelObject.keySet()) {
					slf4jlogger.debug("JSON businessName = {}",businessName);
					if (businessName != null) {
						// we are at the top level, so DICOM standard keywords or hex tags override any coded business names, since may be duplicates, e.g., (111060,DCM,"Study Date")
						if (dictionary.getTagFromName(businessName) == null && JSONRepresentationOfDicomObjectFactory.getAttributeTagFromHexadecimalGroupElementValues(businessName) == null) {
							CodedSequenceItem businessNameCode = getCodedSequenceItemForBusinessNameUsedAsConceptName(businessName,location);
							if (slf4jlogger.isDebugEnabled()) slf4jlogger.debug("getStructuredReport(): businessName {} is {}",businessName,businessNameCode);
							if (businessNameCode != null) {
								if (rootContainerBusinessNameCode == null) {
									rootContainerBusinessName = businessName;
									rootContainerBusinessNameCode = businessNameCode;
								}
								else {
									throw new DicomException("Could not parse JSON document - more than one potential root content item business name");
								}
							}
							else {
								throw new DicomException("Could not parse JSON document - unrecognized business name "+businessName+" that is neither a DICOM data element keyword nor in the supplied business name dictionary");
							}
						}
						else {
							slf4jlogger.debug("getStructuredReport(): Ignoring businessName {} that is a DICOM keyword",businessName);
						}
					}
					else {
						throw new DicomException("Could not parse JSON document - missing business name");
					}
				}
				// if we get here, we found one and only one coded business name to use as rootContainerBusinessNameCode
				JsonValue topLevelObjectValue = topLevelObject.get(rootContainerBusinessName);
				if (topLevelObjectValue != null) {
					contentItemFactory = new ContentItemFactory();
					ContentItem root = getContentItemAndChildrenFromJSONObjectValue(rootContainerBusinessName,rootContainerBusinessNameCode,"CONTAINER",null/*relationshipType*/,topLevelObjectValue,location);	// processes entire tree
					structuredReport = new StructuredReport(root);
				}
				else {
					throw new DicomException("Could not parse JSON document - missing top level object value");
				}
			}
			else {
				throw new DicomException("Could not parse JSON document - missing top level object");
			}
		}
		catch (IndexOutOfBoundsException e) {
			throw new DicomException("Could not parse JSON document - exactly one object in top level array expected "+e);
		}
		catch (ClassCastException e) {
			throw new DicomException("Could not parse JSON document - expected object in top level array "+e);
		}
//System.err.println("getStructuredReport(JsonObject topLevelObject): structuredReport is "+structuredReport);
		return structuredReport;
	}

	
	/**
	 * <p>Given a DICOM SR object encoded as a JSON document
	 * convert it to a StructuredReport using the content tree and ignoring any header attributes.</p>
	 *
	 * @param		document		the JSON document
	 * @return						the StructuredReport
	 * @throws	DicomException
	 */
	public StructuredReport getStructuredReport(JsonArray document) throws DicomException {
		StructuredReport structuredReport = null;
		try {
			JsonObject topLevelObject = document.getJsonObject(0);
			structuredReport = getStructuredReport(topLevelObject);
		}
		catch (IndexOutOfBoundsException e) {
			throw new DicomException("Could not parse JSON document - exactly one object in top level array expected "+e);
		}
		catch (ClassCastException e) {
			throw new DicomException("Could not parse JSON document - expected object in top level array "+e);
		}
//System.err.println("getStructuredReport(JsonArray document): structuredReport is "+structuredReport);
		return structuredReport;
	}
	
	/**
	 * <p>Given a DICOM SR object encoded in a JSON document
	 * convert it to a list of attributes.</p>
	 *
	 * @param		topLevelObject	the first object of the array that is the JSON document
	 * @return						the list of DICOM attributes
	 * @throws	DicomException
	 */
	public AttributeList getAttributeList(JsonObject topLevelObject) throws DicomException {
		AttributeList list = null;
		list = new JSONRepresentationOfDicomObjectFactory().getAttributeList(topLevelObject,true/*ignoreUnrecognizedTags*/,true/*ignoreSR*/);	// ignoreUnrecognizedTags is need to skip the SR business names mixed in with the content, but not the private tags
		{
			StructuredReport structuredReport = getStructuredReport(topLevelObject);
			AttributeList structuredReportList = structuredReport.getAttributeList();
			if (structuredReportList != null) {
				list.putAll(structuredReportList);
			}
			// else wasn't an SR so ignore it
		}
//System.err.println("getAttributeList(JsonObject topLevelObject): List is "+list);
		return list;
	}

	/**
	 * <p>Given a DICOM SR object encoded as a JSON document
	 * convert it to a list of attributes.</p>
	 *
	 * @param		document		the JSON document
	 * @return						the list of DICOM attributes
	 * @throws	DicomException
	 */
	public AttributeList getAttributeList(JsonArray document) throws DicomException {
		AttributeList list = null;
		try {
			JsonObject topLevelObject = document.getJsonObject(0);
			list = getAttributeList(topLevelObject);
		}
		catch (IndexOutOfBoundsException e) {
			throw new DicomException("Could not parse JSON document - exactly one object in top level array expected "+e);
		}
		catch (ClassCastException e) {
			throw new DicomException("Could not parse JSON document - expected object in top level array "+e);
		}
//System.err.println("getAttributeList(JsonArray document): List is "+list);
		return list;
	}
	
	/**
	 * <p>Given a DICOM SR object encoded as a JSON document in a stream
	 * convert it to a list of attributes.</p>
	 *
	 * @param		stream			the input stream containing the JSON document
	 * @return						the list of DICOM attributes
	 * @throws	IOException
	 * @throws	DicomException
	 */
	public AttributeList getAttributeList(InputStream stream) throws IOException, DicomException {
		JsonReader jsonReader = Json.createReader(stream);
		JsonArray document = jsonReader.readArray();
		jsonReader.close();
		return getAttributeList(document);
	}
	
	/**
	 * <p>Given a DICOM SR object encoded as a JSON document in a file
	 * convert it to a list of attributes.</p>
	 *
	 * @param		file			the input file containing the JSON document
	 * @return						the list of DICOM attributes
	 * @throws	IOException
	 * @throws	DicomException
	 */
	public AttributeList getAttributeList(File file) throws IOException, DicomException {
		InputStream fi = new FileInputStream(file);
		BufferedInputStream bi = new BufferedInputStream(fi);
		AttributeList list = null;
		try {
			list = getAttributeList(bi);
		}
		finally {
			bi.close();
			fi.close();
		}
		return list;
	}
	
	/**
	 * <p>Given a DICOM SR object encoded as a JSON document in a named file
	 * convert it to a list of attributes.</p>
	 *
	 * @param		name			the input file containing the JSON document
	 * @return						the list of DICOM attributes
	 * @throws	IOException
	 * @throws	DicomException
	 */
	public AttributeList getAttributeList(String name) throws IOException, DicomException {
		return getAttributeList(new File(name));
	}

	/**
	 * <p>Serialize a JSON document.</p>
	 *
	 * @param	out		the output stream to write to
	 * @param	document	the JSON document
	 * @throws	IOException
	 */
	public static void write(OutputStream out,JsonArray document) throws IOException {
		JsonWriter writer = Json.createWriterFactory(null/*config*/).createWriter(out);	// charset is UTF-8
		writer.writeArray(document);
		writer.close();
	}

	/**
	 * <p>Serialize a JSON document.</p>
	 *
	 * @param	outputFile	the output file to write to
	 * @param	document	the JSON document
	 * @throws	IOException
	 */
	public static void write(File outputFile,JsonArray document) throws IOException {
		OutputStream out = new FileOutputStream(outputFile);
		write(out,document);
		out.close();
	}

	/**
	 * <p>Serialize a JSON document.</p>
	 *
	 * @param	outputPath	the output path to write to
	 * @param	document	the JSON document
	 * @throws	IOException
	 */
	public static void write(String outputPath,JsonArray document) throws IOException {
		write(new File(outputPath),document);
	}

	/**
	 * <p>Serialize a JSON document created from a DICOM Structured Report.</p>
	 *
	 * @param	list	the attribute list
	 * @param	out		the output stream to write to
	 * @throws	IOException
	 * @throws	DicomException
	 */
	public static void createDocumentAndWriteIt(AttributeList list,OutputStream out) throws IOException, DicomException {
		createDocumentAndWriteIt(null,list,out);
	}
	
	/**
	 * <p>Serialize a JSON document created from a DICOM Structured Report.</p>
	 *
	 * @param	sr		the Structured Report
	 * @param	out		the output stream to write to
	 * @throws	IOException
	 * @throws	DicomException
	 */
	public static void createDocumentAndWriteIt(StructuredReport sr,OutputStream out) throws IOException, DicomException {
		createDocumentAndWriteIt(sr,null,out);
	}
	
	/**
	 * <p>Serialize a JSON document created from a DICOM attribute list.</p>
	 *
	 * @param	sr		the Structured Report - may be null if list is not - will build an sr tree model
	 * @param	list	the list of DICOM attributes
	 * @param	out		the output stream to write to
	 * @throws	DicomException
	 */
	public static void createDocumentAndWriteIt(StructuredReport sr,AttributeList list,OutputStream out) throws DicomException {
		try {
			JsonArray document = new JSONRepresentationOfStructuredReportObjectFactory().getDocument(sr,list);
			write(out,document);
		}
		catch (Exception e) {
			throw new DicomException("Could not create JSON document - could not transform to JSON "+e);
		}
	}

	/**
	 * <p>Serialize a JSON document created from a DICOM attribute list.</p>
	 *
	 * @param	sr				the Structured Report - may be null if list is not - will build an sr tree model
	 * @param	list			the list of DICOM attributes
	 * @param	outputFile		the output file to write to
	 * @throws	IOException
	 * @throws	DicomException
	 */
	public static void createDocumentAndWriteIt(StructuredReport sr,AttributeList list,File outputFile) throws IOException, DicomException {
		OutputStream out = new FileOutputStream(outputFile);
		createDocumentAndWriteIt(sr,list,out);
		out.close();
	}

	/**
	 * <p>Serialize a JSON document created from a DICOM attribute list.</p>
	 *
	 * @param	sr			the Structured Report - may be null if list is not - will build an sr tree model
	 * @param	list		the list of DICOM attributes
	 * @param	outputPath	the output path to write to
	 * @throws	IOException
	 * @throws	DicomException
	 */
	public static void createDocumentAndWriteIt(StructuredReport sr,AttributeList list,String outputPath) throws IOException, DicomException {
		createDocumentAndWriteIt(sr,list,new File(outputPath));
	}

	/**
	 * <p>Read a DICOM dataset (that contains a structured report) and write a JSON representation of it to the standard output or specified path, or vice versa.</p>
	 *
	 * @param	arg	either one input path of the file containing the DICOM/JSON dataset and a business name file path to read or write, or a direction argument (toDICOM or toJSON, case insensitive) and an input path and an input or output business name path, and optionally an output path
	 */
	public static void main(String arg[]) {
		try {
			boolean toJSON = true;
			
			String inputPath = null;
			String businessNamesPath = null;
			String outputPath = null;
			
			//boolean useKeywordInsteadOfTag = false;

			int numberOfFixedArguments = 2;
			int numberOfFixedAndOptionalArguments = 4;
			int endOptionsPosition = arg.length;
			
			boolean bad = false;
			
			if (endOptionsPosition < numberOfFixedArguments) {
				bad = true;
			}
			boolean keepLooking = true;
			while (keepLooking && endOptionsPosition > numberOfFixedArguments) {
				String option = arg[endOptionsPosition-1].trim().toUpperCase();
				switch (option) {
					//case "USEKEYWORD":			useKeywordInsteadOfTag = true; --endOptionsPosition; break;
					//case "USETAG":				useKeywordInsteadOfTag = false; --endOptionsPosition; break;
					
					default:	if (endOptionsPosition > numberOfFixedAndOptionalArguments) {
									slf4jlogger.error("Unrecognized argument {}",option);
									bad = true;
								}
								keepLooking = false;
								break;
				}
			}

			if (!bad) {
				if (endOptionsPosition == 2) {
					toJSON = true;
					inputPath = arg[0];
					businessNamesPath = arg[1];
				}
				else if (endOptionsPosition == 3) {
					if (arg[0].toLowerCase(java.util.Locale.US).equals("tojson")) {
						inputPath = arg[1];
						businessNamesPath = arg[2];
						toJSON = true;
					}
					else if (arg[0].toLowerCase(java.util.Locale.US).equals("todicom") || arg[0].toLowerCase(java.util.Locale.US).equals("todcm")) {
						inputPath = arg[1];
						businessNamesPath = arg[2];
						bad = false;
						toJSON = false;
					}
					else {
						inputPath = arg[0];
						businessNamesPath = arg[1];
						outputPath = arg[2];
						toJSON = true;
					}
				}
				else if (endOptionsPosition == 4) {
					if (arg[0].toLowerCase(java.util.Locale.US).equals("tojson")) {
						inputPath = arg[1];
						businessNamesPath = arg[2];
						outputPath = arg[3];
						toJSON = true;
					}
					else if (arg[0].toLowerCase(java.util.Locale.US).equals("todicom") || arg[0].toLowerCase(java.util.Locale.US).equals("todcm")) {
						inputPath = arg[1];
						businessNamesPath = arg[2];
						outputPath = arg[3];
						toJSON = false;
					}
					else {
						bad = true;
					}
				}
				else {
					bad = true;
				}
			}
			
			//if (!toJSON && (useKeywordInsteadOfTag || addTag || addKeyword)) {
			//	System.err.println("Unexpected options specified for conversion to DICOM that are only applicable to conversion to JSON");
			//	bad = true;
			//}
			
			if (bad) {
				System.err.println("usage: JSONRepresentationOfStructuredReportObjectFactory [toJSON] inputpath businessnamespath [outputpath]"
					//+" [USEKEYWORD|USETAG]"
				);
				//System.err.println("usage: JSONRepresentationOfStructuredReportObjectFactory toDICOM inputpath businessnamespath [outputpath]");
				System.exit(1);
			}
			else {
				if (toJSON) {
					JSONRepresentationOfStructuredReportObjectFactory j = new JSONRepresentationOfStructuredReportObjectFactory();
					{
						JsonArray document = j.getDocument(inputPath);
						//System.err.println(toString(document));
						if (outputPath == null) {
							write(System.out,document);
						}
						else {
							write(outputPath,document);
						}
					}
					if (businessNamesPath != null) {
						JsonArray businessNamesDocument = j.getBusinessNamesDocument();
						write(businessNamesPath,businessNamesDocument);
					}
				}
				else {
					JSONRepresentationOfStructuredReportObjectFactory j = new JSONRepresentationOfStructuredReportObjectFactory();
					j.loadBusinessNamesDocument(businessNamesPath);
					AttributeList list = j.getAttributeList(inputPath);
					
					//System.err.println(list);
					
					String sourceApplicationEntityTitle = Attribute.getSingleStringValueOrEmptyString(list,TagFromName.SourceApplicationEntityTitle);
					list.insertSuitableSpecificCharacterSetForAllStringValues();	// (001158)
					list.removeMetaInformationHeaderAttributes();
					FileMetaInformation.addFileMetaInformation(list,TransferSyntax.ExplicitVRLittleEndian,sourceApplicationEntityTitle);
					if (outputPath == null) {
						list.write(System.out,TransferSyntax.ExplicitVRLittleEndian,true/*useMeta*/,true/*useBufferedStream*/);
					}
					else {
						list.write(outputPath,TransferSyntax.ExplicitVRLittleEndian,true/*useMeta*/,true/*useBufferedStream*/);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace(System.err);	// no need to use SLF4J since command line utility/test
		}
	}
}

