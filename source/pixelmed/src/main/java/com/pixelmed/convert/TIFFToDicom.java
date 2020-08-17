/* Copyright (c) 2001-2020, David A. Clunie DBA Pixelmed Publishing. All rights reserved. */

package com.pixelmed.convert;

import com.pixelmed.apps.SetCharacteristicsFromSummary;
import com.pixelmed.apps.TiledPyramid;

import com.pixelmed.dicom.Attribute;
import com.pixelmed.dicom.AttributeList;
import com.pixelmed.dicom.AttributeTag;
import com.pixelmed.dicom.AttributeTagAttribute;
import com.pixelmed.dicom.BinaryOutputStream;
import com.pixelmed.dicom.CodeStringAttribute;
import com.pixelmed.dicom.CodedSequenceItem;
import com.pixelmed.dicom.CodingSchemeIdentification;
import com.pixelmed.dicom.CompressedFrameDecoder;
import com.pixelmed.dicom.CompressedFrameEncoder;
import com.pixelmed.dicom.DateAttribute;
import com.pixelmed.dicom.DateTimeAttribute;
import com.pixelmed.dicom.DecimalStringAttribute;
import com.pixelmed.dicom.DicomDictionary;
import com.pixelmed.dicom.DicomException;
import com.pixelmed.dicom.FileMetaInformation;
import com.pixelmed.dicom.FloatDoubleAttribute;
import com.pixelmed.dicom.FloatSingleAttribute;
import com.pixelmed.dicom.FunctionalGroupUtilities;
import com.pixelmed.dicom.IntegerStringAttribute;
import com.pixelmed.dicom.LongStringAttribute;
import com.pixelmed.dicom.OtherByteAttribute;
import com.pixelmed.dicom.OtherByteAttributeMultipleCompressedFrames;
import com.pixelmed.dicom.OtherByteAttributeMultipleFilesOnDisk;
import com.pixelmed.dicom.OtherWordAttribute;
import com.pixelmed.dicom.OtherWordAttributeMultipleFilesOnDisk;
import com.pixelmed.dicom.PersonNameAttribute;
import com.pixelmed.dicom.SequenceAttribute;
import com.pixelmed.dicom.SequenceItem;
import com.pixelmed.dicom.ShortStringAttribute;
import com.pixelmed.dicom.SOPClass;
import com.pixelmed.dicom.TagFromName;
import com.pixelmed.dicom.TiledFramesIndex;
import com.pixelmed.dicom.TimeAttribute;
import com.pixelmed.dicom.TransferSyntax;
import com.pixelmed.dicom.UIDGenerator;
import com.pixelmed.dicom.UniqueIdentifierAttribute;
import com.pixelmed.dicom.UnsignedLongAttribute;
import com.pixelmed.dicom.UnsignedShortAttribute;
import com.pixelmed.dicom.VersionAndConstants;

import com.pixelmed.display.SourceImage;

import com.pixelmed.utils.FileUtilities;

import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.StringReader;

import java.nio.ByteOrder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.pixelmed.slf4j.Logger;
import com.pixelmed.slf4j.LoggerFactory;

/**
 * <p>A class for converting TIFF files into DICOM images of a specified or appropriate SOP Class.</p>
 *
 * <p>Defaults to producing single frame output unless a multi-frame SOP Class is explicitly
 * requested (e.g., for WSI, request Whole Slide Microscopy Image Storage, which is
 * "1.2.840.10008.5.1.4.1.1.77.1.6".</p>
 *
 * <p>Supports conversion of tiled pyramidal whole slide images such as in Aperio/Leica SVS format.</p>
 *
 * <p>Supports creation of dual-personality DICOM-TIFF files using either classic TIFF or BigTIFF,
 * optionally with inclusion of a down-sampled pyramid inside the same file in a private DICOM attribute,
 * in order to support TIFF WSI viewers that won't work without a pyramid.</p>
 *
 * <p>Uses any ICC profile present in the TIFF file otherwise assumes sRGB.</p>
 *
 * <p>Uses a JSON summary description file as the source of identification and descriptive metadata
 * as described in {@link com.pixelmed.apps.SetCharacteristicsFromSummary SetCharacteristicsFromSummary}.</p>
 *
 * <p>E.g.:</p>
 * <pre>
 * {
 * 	"top" : {
 * 		"PatientName" : "PixelMed^AperioCMU-1",
 * 		"PatientID" : "PX7832548325932",
 * 		"StudyID" : "S07-100",
 * 		"SeriesNumber" : "1",
 * 		"AccessionNumber" : "S07-100",
 * 		"ContainerIdentifier" : "S07-100 A 5 1",
 * 		"IssuerOfTheContainerIdentifierSequence" : [],
 * 		"ContainerTypeCodeSequence" : { "cv" : "433466003", "csd" : "SCT", "cm" : "Microscope slide" },
 * 		"SpecimenDescriptionSequence" : [
 * 	      {
 * 		    "SpecimenIdentifier" : "S07-100 A 5 1",
 * 		    "IssuerOfTheSpecimenIdentifierSequence" : [],
 * 		    "SpecimenUID" : "1.2.840.99790.986.33.1677.1.1.19.5",
 * 		    "SpecimenShortDescription" : "Part A: LEFT UPPER LOBE, Block 5: Mass (2 pc), Slide 1: H&amp;E",
 * 		    "SpecimenDetailedDescription" : "A: Received fresh for intraoperative consultation, labeled with the patient's name, number and 'left upper lobe,' is a pink-tan, wedge-shaped segment of soft tissue, 6.9 x 4.2 x 1.0 cm. The pleural surface is pink-tan and glistening with a stapled line measuring 12.0 cm. in length. The pleural surface shows a 0.5 cm. area of puckering. The pleural surface is inked black. The cut surface reveals a 1.2 x 1.1 cm, white-gray, irregular mass abutting the pleural surface and deep to the puckered area. The remainder of the cut surface is red-brown and congested. No other lesions are identified. Representative sections are submitted. Block 5: 'Mass' (2 pieces)",
 * 		    "SpecimenPreparationSequence" : [
 * 		      {
 * 			    "SpecimenPreparationStepContentItemSequence" : [
 * 			      {
 * 		    		"ValueType" : "TEXT",
 * 					"ConceptNameCodeSequence" : { "cv" : "121041", "csd" : "DCM", "cm" : "Specimen Identifier" },
 * 		    		"TextValue" : "S07-100 A 5 1"
 * 			      },
 * 			      {
 * 		    		"ValueType" : "CODE",
 * 					"ConceptNameCodeSequence" : { "cv" : "111701", "csd" : "DCM", "cm" : "Processing type" },
 * 					"ConceptCodeSequence" :     { "cv" : "127790008", "csd" : "SCT", "cm" : "Staining" }
 * 			      },
 * 			      {
 * 		    		"ValueType" : "CODE",
 * 					"ConceptNameCodeSequence" : { "cv" : "424361007", "csd" : "SCT", "cm" : "Using substance" },
 * 					"ConceptCodeSequence" :     { "cv" : "12710003", "csd" : "SCT", "cm" : "hematoxylin stain" }
 * 			      },
 * 			      {
 * 		    		"ValueType" : "CODE",
 * 					"ConceptNameCodeSequence" : { "cv" : "424361007", "csd" : "SCT", "cm" : "Using substance" },
 * 					"ConceptCodeSequence" :     { "cv" : "36879007", "csd" : "SCT", "cm" : "water soluble eosin stain" }
 * 			      }
 * 			    ]
 * 		      }
 * 		    ],
 * 		    "PrimaryAnatomicStructureSequence" : { "cv" : "44714003", "csd" : "SCT", "cm" : "Left Upper Lobe of Lung" }
 * 	      }
 * 		],
 * 		"OpticalPathSequence" : [
 * 	      {
 * 		    "OpticalPathIdentifier" : "1",
 * 		    "IlluminationColorCodeSequence" : { "cv" : "414298005", "csd" : "SCT", "cm" : "Full Spectrum" },
 * 		    "IlluminationTypeCodeSequence" :  { "cv" : "111744",  "csd" : "DCM", "cm" : "Brightfield illumination" }
 * 	      }
 * 		]
 * 	}
 * }
 * </pre>
 *
 * @see	com.pixelmed.apps.SetCharacteristicsFromSummary
 * @see	com.pixelmed.apps.TiledPyramid
 * @see	com.pixelmed.dicom.SOPClass
 *
 * @author	dclunie
 */

public class TIFFToDicom {
	private static final String identString = "@(#) $Header: /userland/cvs/pixelmed/imgbook/com/pixelmed/convert/TIFFToDicom.java,v 1.17 2020/07/23 20:39:24 dclunie Exp $";

	private static final Logger slf4jlogger = LoggerFactory.getLogger(TIFFToDicom.class);

	private List<File> filesToDeleteAfterWritingDicomFile = null;

	private UIDGenerator u = new UIDGenerator();
	
	private static byte[] stripSOIEOIMarkers(byte[] bytes) {
		byte[] newBytes = null;
		int l = bytes.length;
		if (l >= 4
		 && (bytes[0]&0xff) == 0xff
		 && (bytes[1]&0xff) == 0xd8
		 && (bytes[l-2]&0xff) == 0xff
		 && (bytes[l-1]&0xff) == 0xd9) {
			if (l > 4) {
				int newL = l-4;
				newBytes = new byte[newL];
				System.arraycopy(bytes,2,newBytes,0,newL);
			}
			// else leave it null since now empty
		}
		else {
			slf4jlogger.error("stripSOIEOIMarkers(): Unable to remove SOI and EOI markers");
			newBytes = bytes;
		}
		return newBytes;
	}
	
	private static byte[] insertJPEGTablesIntoAbbreviatedBitStream(byte[] bytes,byte[] jpegTables) {
		byte[] newBytes = null;
		int l = bytes.length;
		if (l > 2
		 && (bytes[0]&0xff) == 0xff
		 && (bytes[1]&0xff) == 0xd8) {
			int tableL = jpegTables.length;
			int newL = l + tableL;
			newBytes = new byte[newL];
			System.arraycopy(bytes,     0,newBytes,0,       2);
			System.arraycopy(jpegTables,0,newBytes,2,       tableL);
			System.arraycopy(bytes,     2,newBytes,2+tableL,l-2);
		}
		else {
			slf4jlogger.error("insertJPEGTablesIntoAbbreviatedBitStream(): Unable to insert JPEG Tables");
			newBytes = bytes;
		}
		return newBytes;
	}
	
	// https://www.sno.phy.queensu.ca/~phil/exiftool/TagNames/JPEG.html#Adobe
	
	private static byte[] AdobeAPP14_RGB = {
		(byte)0xFF, (byte)0xEE,
		(byte)0x00, (byte)12,	/* big endian length includes lebgth itself but not the marker */
		(byte)'A', (byte)'d', (byte)'o', (byte)'b', (byte)'e',(byte)0x00,
		(byte)0x00, /* DCTEncodeVersion 0 */
		(byte)0x00, /* APP14Flags0 0 */
		(byte)0x00, /* APP14Flags1 0 */
		(byte)0x00 /* ColorTransform 0 = Unknown (RGB or CMYK) */
	};
	
	private static byte[] insertAdobeAPP14WithRGBTransformIntoBitStream(byte[] bytes) {
		byte[] newBytes = null;
		int l = bytes.length;
		if (l > 2
		 && (bytes[0]&0xff) == 0xff
		 && (bytes[1]&0xff) == 0xd8) {
			int app14L = AdobeAPP14_RGB.length;
			int newL = l + app14L;
			newBytes = new byte[newL];
			System.arraycopy(bytes,         0,newBytes,0,       2);
			System.arraycopy(AdobeAPP14_RGB,0,newBytes,2,       app14L);
			System.arraycopy(bytes,         2,newBytes,2+app14L,l-2);
		}
		else {
			slf4jlogger.error("insertAdobeAPP14WithRGBTransformIntoBitStream(): Unable to insert APP14");
			newBytes = bytes;
		}
		return newBytes;
	}

	/**
	 * <p>Create a multi-frame DICOM Pixel Data attribute from the TIFF pixel data, recompressing it if requested.</p>
	 *
	 * <p>Recompresses the frames if requested, returning an updated photometric value if changed by recompression.</p>
	 *
	 * <p>Otherwise uses the supplied compressed bitstream, fixing it if necessary to signal RGB if really RGB not YCbCr and
	 * inserting factored out JPEG tables to turn abbreviated into interchange format JPEG bitstreams.</p>
	 *
	 * @param	inputFile
	 * @param	list
	 * @param	numberOfTiles
	 * @param	tileOffsets
	 * @param	tileByteCounts
	 * @param	tileWidth
	 * @param	tileLength
	 * @param	bitsPerSample
	 * @param	compression				the compression value in the TIFF source
	 * @param	photometric				the photometric value in the TIFF source
	 * @param	jpegTables				the JPEG tables in the TIFF source to be inserted in to the abbreviated format JPEG stream to make interchange format or before decompression
	 * @param	iccProfile				the ICC Profile value in the TIFF source, if any
	 * @param	recompressAsFormat		scheme to recompress uncompressed or previously compressed data if different than what was read, either "jpeg" or "jpeg2000"
	 * @param	recompressLossy			use lossy rather than lossless recompression if supported by scheme (not yet implemented)
	 * @return							the updated TIFF photometric value, which may be changed by recompression
	 * @throws	IOException				if there is an error reading or writing
	 * @throws	DicomException			if the image cannot be compressed
	 * @throws	TIFFException
	 */
	private long generateDICOMPixelDataMultiFrameImageFromTIFFFile(TIFFFile inputFile,AttributeList list,
				int numberOfTiles,long[] tileOffsets,long[] tileByteCounts,long tileWidth,long tileLength,
				long bitsPerSample,long compression,long photometric,byte[] jpegTables,byte[] iccProfile,
				String recompressAsFormat,boolean recompressLossy) throws IOException, DicomException, TIFFException {

		long outputPhotometric = photometric;
		
		if (list == null) {
			list = new AttributeList();
		}
		
		if (numberOfTiles > 2147483647l) {	// (2^31)-1 IS positive value limit
			throw new TIFFException("Number of tiles exceeds maximum IS value for NumberOfFrames = "+numberOfTiles);
		}
		if (tileWidth > 65535l || tileLength > 65535l) {	// maximum US value
			throw new TIFFException("tileWidth "+tileWidth+" and/or tileLength "+tileLength+" exceeds maximum US value for Columns and/or Rows");
		}
		
		if (compression == 0 || compression == 1) {		// absent or specified as uncompressed
			if (recompressAsFormat == null || recompressAsFormat.length() == 0) {
				// repeat the same file for every tile so that we can reuse existing MultipleFilesOnDisk classes
				File file = new File(inputFile.getFileName());
				File[] files = new File[numberOfTiles];
				for (int i=0; i<numberOfTiles; ++i) {
					files[i] = file;
				}
				if (bitsPerSample == 8) {
					Attribute aPixelData = new OtherByteAttributeMultipleFilesOnDisk(TagFromName.PixelData,files,tileOffsets,tileByteCounts);
					long vl = aPixelData.getPaddedVL();
					if ((vl & 0xfffffffel) != vl) {
						throw new TIFFException("Value length of Pixel Data "+vl+" exceeds maximum Value Length supported by DICOM");
					}
					list.put(aPixelData);
				}
				else if (bitsPerSample == 16) {
					Attribute aPixelData = new OtherWordAttributeMultipleFilesOnDisk(TagFromName.PixelData,files,tileOffsets,tileByteCounts,inputFile.getByteOrder() == ByteOrder.BIG_ENDIAN);
					long vl = aPixelData.getPaddedVL();
					if ((vl & 0xfffffffel) != vl) {
						throw new TIFFException("Value length of Pixel Data "+vl+" exceeds maximum Value Length supported by DICOM");
					}
					list.put(aPixelData);
				}
				else {
					throw new TIFFException("Unsupported bitsPerSample = "+bitsPerSample);
				}
				// photometric unchanged
			}
			else {
				if (bitsPerSample == 8) {
					File[] files = new File[numberOfTiles];
					for (int tileNumber=0; tileNumber<numberOfTiles; ++tileNumber) {
						long pixelOffset = tileOffsets[tileNumber];
						long pixelByteCount = tileByteCounts[tileNumber];
						byte[] values = new byte[(int)pixelByteCount];
						inputFile.seek(pixelOffset);
						inputFile.read(values);
						BufferedImage img = SourceImage.createPixelInterleavedByteThreeComponentColorImage(
							(int)tileWidth,(int)tileLength,values,0/*offset*/,
							ColorSpace.getInstance(ColorSpace.CS_sRGB),	// should check for presence of TIFF ICC profile ? :(
							false/*isChrominanceHorizontallyDownsampledBy2*/);

						// recompressLossy not yet implemented ... default for JPEG is best quality, J2K is lossless :(
						// will always transform color space by default
						File tmpFile = CompressedFrameEncoder.getCompressedFrameAsFile(new AttributeList(),img,recompressAsFormat,File.createTempFile("TIFFToDicom","."+recompressAsFormat));
						files[tileNumber] = tmpFile;
						tmpFile.deleteOnExit();
						if (slf4jlogger.isTraceEnabled()) slf4jlogger.trace("Tile {} created compressed temporary file {}",tileNumber,tmpFile.toString());
						// photometric changed, since CompressedFrameEncoder always transforms color space
						outputPhotometric = 6;	// TIFF definition of YCbCr is generic, so use it to signal YBR_FULL_4r22 for JPEG and YBR_RCT or YBR_ICT for J2K
					}
					Attribute aPixelData = new OtherByteAttributeMultipleCompressedFrames(TagFromName.PixelData,files);
					list.put(aPixelData);
					if (filesToDeleteAfterWritingDicomFile == null) {
						filesToDeleteAfterWritingDicomFile = new ArrayList<>(Arrays.asList(files));	// make a copy because Arrays.asList() is documented not to support any adding elements "https://stackoverflow.com/questions/5755477/java-list-add-unsupportedoperationexception"
					}
					else {
						Collections.addAll(filesToDeleteAfterWritingDicomFile,files);
					}
				}
				else {
					throw new TIFFException("Unsupported bitsPerSample = "+bitsPerSample+" for compression");
				}
				//throw new TIFFException("Compression as "+(recompressLossy ? "lossy" : "lossless")+" "+recompressAsFormat+" not supported");
			}
		}
		else if (compression == 7 && recompressAsFormat.equals("jpeg")				// "new" JPEG per TTN2 as used by Aperio in SVS
			  || compression == 33005 && recompressAsFormat.equals("jpeg2000")) {	// Aperio J2K RGB
			// copy compressed bit stream without recompressing it
			// because we need to edit the stream to insert the jpegTables, need to write lots of temporary files to feed to OtherByteAttributeMultipleCompressedFrames file-based constructor
			File[] files = new File[numberOfTiles];
			for (int tileNumber=0; tileNumber<numberOfTiles; ++tileNumber) {
				long pixelOffset = tileOffsets[tileNumber];
				long pixelByteCount = tileByteCounts[tileNumber];
				if (pixelByteCount > Integer.MAX_VALUE) {
					throw new TIFFException("For frame "+tileNumber+", compressed pixelByteCount to be read "+pixelByteCount+" exceeds maximum Java array size "+Integer.MAX_VALUE+" and fragmentation not yet supported");
				}
				byte[] values = new byte[(int)pixelByteCount];
				inputFile.seek(pixelOffset);
				inputFile.read(values);
				
				if (jpegTables != null) {		// should not be present for 33005
					values = insertJPEGTablesIntoAbbreviatedBitStream(values,jpegTables);
				}
				if (compression == 7/*JPEG*/ && photometric == 2/*RGB*/) {
					slf4jlogger.trace("JPEG RGB so adding APP14");
					values = insertAdobeAPP14WithRGBTransformIntoBitStream(values);
				}
				
				if (values.length > 0xfffffffel) {
					throw new TIFFException("For frame "+tileNumber+", compressed pixelByteCount to be written "+values.length+" exceeds maximum single fragment size 0xfffffffe and fragmentation not yet supported");
				}
				File tmpFile = File.createTempFile("TIFFToDicom",".jpeg");
				files[tileNumber] = tmpFile;
				tmpFile.deleteOnExit();
				BufferedOutputStream o = new BufferedOutputStream(new FileOutputStream(tmpFile));
				o.write(values);
				o.flush();
				o.close();
				if (slf4jlogger.isTraceEnabled()) slf4jlogger.trace("Tile {} wrote {} bytes to {}",tileNumber,values.length,tmpFile.toString());
				// photometric unchanged
			}
			Attribute aPixelData = new OtherByteAttributeMultipleCompressedFrames(TagFromName.PixelData,files);
			list.put(aPixelData);
			if (filesToDeleteAfterWritingDicomFile == null) {
				filesToDeleteAfterWritingDicomFile = new ArrayList<>(Arrays.asList(files));	// make a copy because Arrays.asList() is documented not to support any adding elements "https://stackoverflow.com/questions/5755477/java-list-add-unsupportedoperationexception"
			}
			else {
				Collections.addAll(filesToDeleteAfterWritingDicomFile,files);
			}
		}
		else if ((compression == 7 || compression == 33005)		// "new" JPEG per TTN2 as used by Aperio in SVS, Aperio J2K RGB
			  && (recompressAsFormat.equals("jpeg") || recompressAsFormat.equals("jpeg2000"))) {
			// decompress and recompress each frame
			{
				if (bitsPerSample == 8) {
					String transferSyntax = compression == 7 ? TransferSyntax.JPEGBaseline : TransferSyntax.JPEG2000;	// take care to keep this in sync with enclosing test of supported schemes
					CompressedFrameDecoder decoder = new CompressedFrameDecoder(
						transferSyntax,
						1/*bytesPerSample*/,
						(int)tileWidth,(int)tileLength,
						3/*samples*/,		// hmmm ..../ :(
						ColorSpace.getInstance(ColorSpace.CS_sRGB));	// should check for presence of TIFF ICC profile ? :(

					File[] files = new File[numberOfTiles];
					for (int tileNumber=0; tileNumber<numberOfTiles; ++tileNumber) {
						long pixelOffset = tileOffsets[tileNumber];
						long pixelByteCount = tileByteCounts[tileNumber];
						if (pixelByteCount > Integer.MAX_VALUE) {
							throw new TIFFException("For frame "+tileNumber+", compressed pixelByteCount to be read "+pixelByteCount+" exceeds maximum Java array size "+Integer.MAX_VALUE+" and fragmentation not yet supported");
						}
						byte[] values = new byte[(int)pixelByteCount];
						inputFile.seek(pixelOffset);
						inputFile.read(values);

						if (jpegTables != null) {		// should not be present for 33005
							values = insertJPEGTablesIntoAbbreviatedBitStream(values,jpegTables);
						}
						if (compression == 7/*JPEG*/ && photometric == 2/*RGB*/) {
							slf4jlogger.trace("JPEG RGB so adding APP14");
							values = insertAdobeAPP14WithRGBTransformIntoBitStream(values);
						}
						
						BufferedImage img = decoder.getDecompressedFrameAsBufferedImage(values);
						
						// recompressLossy not yet implemented ... default for JPEG is best quality, J2K is lossless :(
						// will always transform color space by default
						File tmpFile = CompressedFrameEncoder.getCompressedFrameAsFile(new AttributeList(),img,recompressAsFormat,File.createTempFile("TIFFToDicom","."+recompressAsFormat));
						files[tileNumber] = tmpFile;
						tmpFile.deleteOnExit();
						if (slf4jlogger.isTraceEnabled()) slf4jlogger.trace("Tile {} created compressed temporary file {}",tileNumber,tmpFile.toString());
						// photometric changed, since CompressedFrameEncoder always transforms color space
						outputPhotometric = 6;	// TIFF definition of YCbCr is generic, so use it to signal YBR_FULL_4r22 for JPEG and YBR_RCT or YBR_ICT for J2K
					}
					Attribute aPixelData = new OtherByteAttributeMultipleCompressedFrames(TagFromName.PixelData,files);
					list.put(aPixelData);
					if (filesToDeleteAfterWritingDicomFile == null) {
						filesToDeleteAfterWritingDicomFile = new ArrayList<>(Arrays.asList(files));	// make a copy because Arrays.asList() is documented not to support any adding elements "https://stackoverflow.com/questions/5755477/java-list-add-unsupportedoperationexception"
					}
					else {
						Collections.addAll(filesToDeleteAfterWritingDicomFile,files);
					}
				}
				else {
					throw new TIFFException("Unsupported bitsPerSample = "+bitsPerSample+" for compression");
				}
				//throw new TIFFException("Recompression as "+(recompressLossy ? "lossy" : "lossless")+" "+recompressAsFormat+" not supported");
			}
		}
		else {
			throw new TIFFException("Unsupported compression = "+compression+" or unsupported transformation to "+recompressAsFormat);
		}

		return outputPhotometric;
	}
	
	private long generateDICOMPixelDataSingleFrameImageFromTIFFFileMergingStrips(TIFFFile inputFile,AttributeList list,
				long[] pixelOffset,long[] pixelByteCount,long pixelWidth,long pixelLength,
				long bitsPerSample,long compression,long photometric,byte[] jpegTables,byte[] iccProfile,String recompressAsFormat,boolean recompressLossy) throws IOException, DicomException, TIFFException {
		
		long outputPhotometric = photometric;
		
		if (list == null) {
			list = new AttributeList();
		}

		if (compression == 0 || compression == 1) {		// absent or specified as uncompressed
			if (recompressAsFormat == null || recompressAsFormat.length() == 0) {
				if (bitsPerSample == 8) {
					long totalLength = 0;
					for (int i=0; i<pixelByteCount.length; ++i) {
						totalLength += pixelByteCount[i];
					}
					if (totalLength%2 == 1) ++totalLength;
					slf4jlogger.debug("generateDICOMPixelDataSingleFrameImageFromTIFFFileMergingStrips(): totalLength = {}",totalLength);
					if (totalLength > Integer.MAX_VALUE) {
						throw new TIFFException("Uncompressed image too large to allocate = "+totalLength);
					}
					byte[] values = new byte[(int)totalLength];
					int offsetIntoValues = 0;
					for (int i=0; i<pixelOffset.length; ++i) {
						long fileOffset = pixelOffset[i];
						slf4jlogger.debug("generateDICOMPixelDataSingleFrameImageFromTIFFFileMergingStrips(): pixelOffset[{}] = {}",i,fileOffset);
						inputFile.seek(fileOffset);
						int bytesToRead = (int)pixelByteCount[i];
						slf4jlogger.debug("generateDICOMPixelDataSingleFrameImageFromTIFFFileMergingStrips(): pixelByteCount[{}] = {}",i,bytesToRead);
						inputFile.read(values,offsetIntoValues,bytesToRead);
						offsetIntoValues += bytesToRead;
					}
					Attribute aPixelData = new OtherByteAttribute(TagFromName.PixelData);
					aPixelData.setValues(values);
					list.put(aPixelData);
				}
				else {
					throw new TIFFException("Unsupported bitsPerSample = "+bitsPerSample);
				}
			}
			else {
				throw new TIFFException("Compression as "+(recompressLossy ? "lossy" : "lossless")+" "+recompressAsFormat+" not supported");
			}
		}
		else {
			throw new TIFFException("Unsupported compression = "+compression);
		}

		return outputPhotometric;
	}

	private long generateDICOMPixelDataSingleFrameImageFromTIFFFile(TIFFFile inputFile,AttributeList list,
				long pixelOffset,long pixelByteCount,long pixelWidth,long pixelLength,
				long bitsPerSample,long compression,long photometric,byte[] jpegTables,byte[] iccProfile,String recompressAsFormat,boolean recompressLossy) throws IOException, DicomException, TIFFException {
		
		long outputPhotometric = photometric;
		
		if (list == null) {
			list = new AttributeList();
		}
		
		inputFile.seek(pixelOffset);
		if (compression == 0 || compression == 1) {		// absent or specified as uncompressed
			if (recompressAsFormat == null || recompressAsFormat.length() == 0) {
				if (bitsPerSample == 8) {
					byte[] values = new byte[(int)pixelByteCount];
					inputFile.read(values);
					Attribute aPixelData = new OtherByteAttribute(TagFromName.PixelData);
					aPixelData.setValues(values);
					list.put(aPixelData);
				}
				else if (bitsPerSample == 16) {
					short[] values = new short[(int)(pixelByteCount/2)];
					inputFile.read(values);
					Attribute aPixelData = new OtherWordAttribute(TagFromName.PixelData);
					aPixelData.setValues(values);
					list.put(aPixelData);
				}
				else {
					throw new TIFFException("Unsupported bitsPerSample = "+bitsPerSample);
				}
			}
			else {
				throw new TIFFException("Compression as "+(recompressLossy ? "lossy" : "lossless")+" "+recompressAsFormat+" not supported");
			}
		}
		else if (compression == 7				// "new" JPEG per TTN2 as used by Aperio in SVS
			  || compression == 33005) {		// Aperio J2K RGB
			byte[] values = new byte[(int)pixelByteCount];
			inputFile.read(values);
			if (jpegTables != null) {			// should not be present for 33005
				values = insertJPEGTablesIntoAbbreviatedBitStream(values,jpegTables);
			}
			if (compression == 7/*JPEG*/ && photometric == 2/*RGB*/) {
//System.err.println("JPEG RGB so adding APP14");
				values = insertAdobeAPP14WithRGBTransformIntoBitStream(values);
			}
			byte[][] frames = new byte[1][];
			frames[0] = values;
			Attribute aPixelData = new OtherByteAttributeMultipleCompressedFrames(TagFromName.PixelData,frames);
			list.put(aPixelData);
		}
		else {
			throw new TIFFException("Unsupported compression = "+compression);
		}

		return outputPhotometric;
	}
	
	private static AttributeList generateDICOMPixelDataModuleAttributes(AttributeList list,
			int numberOfFrames,long pixelWidth,long pixelLength,
			long bitsPerSample,long compression,long photometric,long samplesPerPixel,long planarConfig,long sampleFormat,String recompressAsFormat,boolean recompressLossy,String sopClass) throws IOException, DicomException, TIFFException {
		
		if (list == null) {
			list = new AttributeList();
		}
		
		String photometricInterpretation = "";
		switch ((int)photometric) {
			case 0:	photometricInterpretation = "MONOCHROME1"; break;
			case 1:	photometricInterpretation = "MONOCHROME2"; break;
			case 2:	photometricInterpretation = "RGB"; break;
			case 3:	photometricInterpretation = "PALETTE COLOR"; break;
			case 4:	photometricInterpretation = "TRANSPARENCY"; break;		// not standard DICOM
			case 5:	photometricInterpretation = "CMYK"; break;				// retired in DICOM
			case 6:	photometricInterpretation = (recompressAsFormat != null && recompressAsFormat.equals("jpeg2000")) ? (recompressLossy ? "YBR_ICT" : "YBR_RCT") : "YBR_FULL_422"; break;
			case 8:	photometricInterpretation = "CIELAB"; break;			// not standard DICOM
		}
		{ Attribute a = new CodeStringAttribute(TagFromName.PhotometricInterpretation); a.addValue(photometricInterpretation); list.put(a); }

		{ Attribute a = new UnsignedShortAttribute(TagFromName.BitsAllocated); a.addValue((int)bitsPerSample); list.put(a); }
		{ Attribute a = new UnsignedShortAttribute(TagFromName.BitsStored); a.addValue((int)bitsPerSample); list.put(a); }
		{ Attribute a = new UnsignedShortAttribute(TagFromName.HighBit); a.addValue((int)bitsPerSample-1); list.put(a); }
		{ Attribute a = new UnsignedShortAttribute(TagFromName.Rows); a.addValue((int)pixelLength); list.put(a); }
		{ Attribute a = new UnsignedShortAttribute(TagFromName.Columns); a.addValue((int)pixelWidth); list.put(a); }
			

		boolean signed = false;
		if (sampleFormat == 2) {
			signed = true;
			// do not check for other values, like 3 for IEEE float
		}
		{ Attribute a = new UnsignedShortAttribute(TagFromName.PixelRepresentation); a.addValue(signed ? 1 : 0); list.put(a); }

		list.remove(TagFromName.NumberOfFrames);
		if (SOPClass.isMultiframeImageStorage(sopClass)) {
			Attribute a = new IntegerStringAttribute(TagFromName.NumberOfFrames); a.addValue(numberOfFrames); list.put(a);
		}
			
		{ Attribute a = new UnsignedShortAttribute(TagFromName.SamplesPerPixel); a.addValue((int)samplesPerPixel); list.put(a); }
						
		list.remove(TagFromName.PlanarConfiguration);
		if (samplesPerPixel > 1) {
				Attribute a = new UnsignedShortAttribute(TagFromName.PlanarConfiguration); a.addValue((int)planarConfig-1); list.put(a);	// TIFF is 1 or 2 but sometimes absent (0), DICOM is 0 or 1
		}

		return list;
	}

	// copied and derived from CommonConvertedAttributeGeneration.addParametricMapFrameTypeSharedFunctionalGroup() - should refactor :(
	private static AttributeList addWholeSlideMicroscopyImageFrameTypeSharedFunctionalGroup(AttributeList list) throws DicomException {
		// override default from CommonConvertedAttributeGeneration; same as FrameType; no way of determining this and most are VOLUME not LABEL or LOCALIZER :(
		Attribute aFrameType = new CodeStringAttribute(TagFromName.FrameType);
		aFrameType.addValue("DERIVED");
		aFrameType.addValue("PRIMARY");
		aFrameType.addValue("VOLUME");
		aFrameType.addValue("NONE");
		list = FunctionalGroupUtilities.generateFrameTypeSharedFunctionalGroup(list,DicomDictionary.StandardDictionary.getTagFromName("WholeSlideMicroscopyImageFrameTypeSequence"),aFrameType);
		return list;
	}
	
	private byte[] addICCProfileToOpticalPathSequence(AttributeList list,byte[] iccProfile) throws DicomException {
		AttributeList opticalPathSequenceItemList = SequenceAttribute.getAttributeListFromWithinSequenceWithSingleItem(list,DicomDictionary.StandardDictionary.getTagFromName("OpticalPathSequence"));
		if (opticalPathSequenceItemList != null) {
			if (iccProfile == null || iccProfile.length == 0) {
				InputStream iccProfileStream = getClass().getResourceAsStream("/com/pixelmed/dicom/sRGBColorSpaceProfileInputDevice.icc");
				try {
					iccProfile = FileUtilities.readAllBytes(iccProfileStream);
					int iccProfileLength = iccProfile.length;
					if (iccProfileLength %2 != 0) {
						byte[] newICCProfile = new byte[iccProfileLength+1];
						System.arraycopy(iccProfile,0,newICCProfile,0,iccProfileLength);
						iccProfile = newICCProfile;
						iccProfileLength = iccProfile.length;
					}
				}
				catch (IOException e) {
					throw new DicomException("Failed to read ICC profile resource: "+e);
				}
				{ Attribute a = new CodeStringAttribute(DicomDictionary.StandardDictionary.getTagFromName("ColorSpace")); a.addValue("SRGB"); opticalPathSequenceItemList.put(a); }
			}
			else {
				slf4jlogger.debug("Using ICC Profile from TIFF IFD");
				// do not add ColorSpace since we do not know what it is or if it is any recognized standard value
			}
			if (iccProfile != null && iccProfile.length > 0) {
				{ Attribute a = new OtherByteAttribute(TagFromName.ICCProfile); a.setValues(iccProfile); opticalPathSequenceItemList.put(a); }
				slf4jlogger.debug("addICCProfileToOpticalPathSequence(): Created ICC Profile attribute of length {}",iccProfile.length);
			}
		}
		return iccProfile;
	}

	private AttributeList generateDICOMWholeSlideMicroscopyImageAttributes(AttributeList list,
			long imageWidth,long imageLength,double mmPerPixel,
			String containerIdentifier,String specimenIdentifier,String specimenUID) throws DicomException {
		
		if (list == null) {
			list = new AttributeList();
		}
		
		// Whole Slide Microscopy Series Module
		
		// Multi-frame Functional Groups Module
		
		addWholeSlideMicroscopyImageFrameTypeSharedFunctionalGroup(list);

		{
			SequenceAttribute aSharedFunctionalGroupsSequence = (SequenceAttribute)list.get(TagFromName.SharedFunctionalGroupsSequence);
			AttributeList sharedFunctionalGroupsSequenceList = SequenceAttribute.getAttributeListFromWithinSequenceWithSingleItem(aSharedFunctionalGroupsSequence);

			SequenceAttribute aPixelMeasuresSequence = new SequenceAttribute(TagFromName.PixelMeasuresSequence);
			sharedFunctionalGroupsSequenceList.put(aPixelMeasuresSequence);
			AttributeList itemList = new AttributeList();
			aPixelMeasuresSequence.addItem(itemList);

			// note that order in DICOM in PixelSpacing is "adjacent row spacing", then "adjacent column spacing" ...
			{ Attribute a = new DecimalStringAttribute(TagFromName.PixelSpacing); a.addValue(mmPerPixel); a.addValue(mmPerPixel); itemList.put(a); }
			{ Attribute a = new DecimalStringAttribute(TagFromName.SliceThickness); a.addValue(0); itemList.put(a); }	// No way of determining this but required :(
			//{ Attribute a = new DecimalStringAttribute(TagFromName.SpacingBetweenSlices); a.addValue(sliceSpacing); itemList.put(a); }
		}

		
		// Multi-frame Dimension Module - add it even though we are using TILED_FULL so not adding Per-Frame Functional Group :(
		{
			// derived from IndexedLabelMapToSegmentation.IndexedLabelMapToSegmentation() - should refactor :(
			String dimensionOrganizationUID = u.getAnotherNewUID();
			{
				SequenceAttribute saDimensionOrganizationSequence = new SequenceAttribute(TagFromName.DimensionOrganizationSequence);
				list.put(saDimensionOrganizationSequence);
				{
					AttributeList itemList = new AttributeList();
					saDimensionOrganizationSequence.addItem(itemList);
					{ Attribute a = new UniqueIdentifierAttribute(TagFromName.DimensionOrganizationUID); a.addValue(dimensionOrganizationUID); itemList.put(a); }
				}
			}
			{ Attribute a = new CodeStringAttribute(TagFromName.DimensionOrganizationType); a.addValue("TILED_FULL"); list.put(a); }
			{
				SequenceAttribute saDimensionIndexSequence = new SequenceAttribute(TagFromName.DimensionIndexSequence);
				list.put(saDimensionIndexSequence);
				{
					AttributeList itemList = new AttributeList();
					saDimensionIndexSequence.addItem(itemList);
					{ AttributeTagAttribute a = new AttributeTagAttribute(TagFromName.DimensionIndexPointer); a.addValue(TagFromName.RowPositionInTotalImagePixelMatrix); itemList.put(a); }
					{ AttributeTagAttribute a = new AttributeTagAttribute(TagFromName.FunctionalGroupPointer); a.addValue(TagFromName.PlanePositionSlideSequence); itemList.put(a); }
					{ Attribute a = new UniqueIdentifierAttribute(TagFromName.DimensionOrganizationUID); a.addValue(dimensionOrganizationUID); itemList.put(a); }
					{ Attribute a = new LongStringAttribute(TagFromName.DimensionDescriptionLabel); a.addValue("Row Position"); itemList.put(a); }
				}
				{
					AttributeList itemList = new AttributeList();
					saDimensionIndexSequence.addItem(itemList);
					{ AttributeTagAttribute a = new AttributeTagAttribute(TagFromName.DimensionIndexPointer); a.addValue(TagFromName.ColumnPositionInTotalImagePixelMatrix); itemList.put(a); }
					{ AttributeTagAttribute a = new AttributeTagAttribute(TagFromName.FunctionalGroupPointer); a.addValue(TagFromName.PlanePositionSlideSequence); itemList.put(a); }
					{ Attribute a = new UniqueIdentifierAttribute(TagFromName.DimensionOrganizationUID); a.addValue(dimensionOrganizationUID); itemList.put(a); }
					{ Attribute a = new LongStringAttribute(TagFromName.DimensionDescriptionLabel); a.addValue("Column Position"); itemList.put(a); }
				}
			}
		}


		// Specimen Module

		{ Attribute a = new LongStringAttribute(DicomDictionary.StandardDictionary.getTagFromName("ContainerIdentifier")); a.addValue(containerIdentifier); list.put(a); }					// Dummy value - should be able to override this :(
		{ Attribute a = new SequenceAttribute(DicomDictionary.StandardDictionary.getTagFromName("IssuerOfTheContainerIdentifierSequence")); list.put(a); }
		CodedSequenceItem.putSingleCodedSequenceItem(list,DicomDictionary.StandardDictionary.getTagFromName("ContainerTypeCodeSequence"),"433466003","SCT","Microscope slide");	// No way of determining this :(
		{
			SequenceAttribute aSpecimenDescriptionSequence = new SequenceAttribute(DicomDictionary.StandardDictionary.getTagFromName("SpecimenDescriptionSequence"));
			list.put(aSpecimenDescriptionSequence);
			{
				AttributeList itemList = new AttributeList();
				aSpecimenDescriptionSequence.addItem(itemList);
				{ Attribute a = new LongStringAttribute(DicomDictionary.StandardDictionary.getTagFromName("SpecimenIdentifier")); a.addValue(specimenIdentifier); itemList.put(a); }	// Dummy value - should be able to override this :(
				{ Attribute a = new SequenceAttribute(DicomDictionary.StandardDictionary.getTagFromName("IssuerOfTheSpecimenIdentifierSequence")); itemList.put(a); }
				{ Attribute a = new UniqueIdentifierAttribute(DicomDictionary.StandardDictionary.getTagFromName("SpecimenUID")); a.addValue(specimenUID); itemList.put(a); }
				{ Attribute a = new SequenceAttribute(DicomDictionary.StandardDictionary.getTagFromName("SpecimenPreparationSequence")); itemList.put(a); }						// Would be nice to be able to populate this :(
			}
		}

		// Whole Slide Microscopy Image Module
		
		{ Attribute a = new CodeStringAttribute(TagFromName.ImageType); a.addValue("DERIVED"); a.addValue("PRIMARY"); a.addValue("VOLUME"); a.addValue("NONE"); list.put(a); }	// override default from CommonConvertedAttributeGeneration; same as FrameType; no way of determining this and most are VOLUME not LABEL or LOCALIZER :(

		{ Attribute a = new FloatSingleAttribute(DicomDictionary.StandardDictionary.getTagFromName("ImagedVolumeWidth"));  a.addValue(imageWidth*mmPerPixel); list.put(a); }
		{ Attribute a = new FloatSingleAttribute(DicomDictionary.StandardDictionary.getTagFromName("ImagedVolumeHeight")); a.addValue(imageLength*mmPerPixel); list.put(a); }
		{ Attribute a = new FloatSingleAttribute(DicomDictionary.StandardDictionary.getTagFromName("ImagedVolumeDepth"));  a.addValue(0); list.put(a); }							// No way of determining this :(

		{ Attribute a = new UnsignedLongAttribute(DicomDictionary.StandardDictionary.getTagFromName("TotalPixelMatrixColumns")); a.addValue(imageWidth);  list.put(a); }
		{ Attribute a = new UnsignedLongAttribute(DicomDictionary.StandardDictionary.getTagFromName("TotalPixelMatrixRows")); a.addValue(imageLength); list.put(a); }
		{ Attribute a = new UnsignedLongAttribute(DicomDictionary.StandardDictionary.getTagFromName("TotalPixelMatrixFocalPlanes")); a.addValue(1); list.put(a); }
		{
			SequenceAttribute aTotalPixelMatrixOriginSequence = new SequenceAttribute(DicomDictionary.StandardDictionary.getTagFromName("TotalPixelMatrixOriginSequence"));
			list.put(aTotalPixelMatrixOriginSequence);
			{
				AttributeList itemList = new AttributeList();
				aTotalPixelMatrixOriginSequence.addItem(itemList);
				{ Attribute a = new DecimalStringAttribute(DicomDictionary.StandardDictionary.getTagFromName("XOffsetInSlideCoordinateSystem")); a.addValue(0); itemList.put(a); }
				{ Attribute a = new DecimalStringAttribute(DicomDictionary.StandardDictionary.getTagFromName("YOffsetInSlideCoordinateSystem")); a.addValue(0); itemList.put(a); }
			}
		}
		// assume slide on its side with label on left, which seems to be what Aperio, Hamamatsu, AIDPATH are
		{ Attribute a = new DecimalStringAttribute(TagFromName.ImageOrientationSlide); a.addValue(0.0); a.addValue(-1.0); a.addValue(0.0); a.addValue(-1.0); a.addValue(0.0); a.addValue(0.0); list.put(a); }
		{ Attribute a = new DateTimeAttribute(TagFromName.AcquisitionDateTime); list.put(a); }							// No way of determining this :(
		// AcquisitionDuration is optional after CP 1821
		// Lossy Image Compression
		// Lossy Image Compression Ratio
		// Lossy Image Compression Method
		{ Attribute a = new CodeStringAttribute(TagFromName.VolumetricProperties); a.addValue("VOLUME"); list.put(a); }
		{ Attribute a = new CodeStringAttribute(DicomDictionary.StandardDictionary.getTagFromName("SpecimenLabelInImage")); a.addValue("NO"); list.put(a); }		// No way of determining this and most not :(
		{ Attribute a = new CodeStringAttribute(DicomDictionary.StandardDictionary.getTagFromName("FocusMethod")); a.addValue("AUTO"); list.put(a); }			// No way of determining this and most are :(
		{ Attribute a = new CodeStringAttribute(DicomDictionary.StandardDictionary.getTagFromName("ExtendedDepthOfField")); a.addValue("NO"); list.put(a); }		// No way of determining this and most not :(
		// NumberOfFocalPlanes - not need if ExtendedDepthOfField NO
		// DistanceBetweenFocalPlanes - not need if ExtendedDepthOfField NO
		// AcquisitionDeviceProcessingDescription - Type 3
		// ConvolutionKernel - Type 3
		{ Attribute a = new UnsignedShortAttribute(DicomDictionary.StandardDictionary.getTagFromName("RecommendedAbsentPixelCIELabValue")); a.addValue(0xFFFF); a.addValue(0); a.addValue(0); list.put(a); }		// white (0xFFFF is 100 per PS3.3 C.10.7.1.1)

		// Optical Path Module

		{ Attribute a = new UnsignedLongAttribute(DicomDictionary.StandardDictionary.getTagFromName("NumberOfOpticalPaths")); a.addValue(1); list.put(a); }
		{
			SequenceAttribute aOpticalPathSequence = new SequenceAttribute(DicomDictionary.StandardDictionary.getTagFromName("OpticalPathSequence"));
			list.put(aOpticalPathSequence);
			{
				AttributeList opticalPathSequenceItemList = new AttributeList();
				aOpticalPathSequence.addItem(opticalPathSequenceItemList);
				{ Attribute a = new ShortStringAttribute(DicomDictionary.StandardDictionary.getTagFromName("OpticalPathIdentifier")); a.addValue("1"); opticalPathSequenceItemList.put(a); }
				CodedSequenceItem.putSingleCodedSequenceItem(opticalPathSequenceItemList,DicomDictionary.StandardDictionary.getTagFromName("IlluminationColorCodeSequence"),"414298005","SCT","Full Spectrum");
				CodedSequenceItem.putSingleCodedSequenceItem(opticalPathSequenceItemList,DicomDictionary.StandardDictionary.getTagFromName("IlluminationTypeCodeSequence"),"111744","DCM","Brightfield illumination");
				// ICCProfile and ColorSpace are added later
			}
		}

		// Multi-Resolution Navigation Module
		// Slide Label Module

		return list;
	}
	
	// reuse same private group and creator as for com.pixelmed.dicom.PrivatePixelData
	private static final String pixelmedPrivateCreatorForPyramidData = "PixelMed Publishing";
	private static final int pixelmedPrivatePyramidDataGroup = 0x7FDF;	// Must be BEFORE (7FE0,0010) because we assume elsewhere that DataSetTrailingPadding will immediately follow (7FE0,0010)
	private static final AttributeTag pixelmedPrivatePyramidDataBlockReservation = new AttributeTag(pixelmedPrivatePyramidDataGroup,0x0010);
	private static final AttributeTag pixelmedPrivatePyramidData = new AttributeTag(pixelmedPrivatePyramidDataGroup,0x1001);
	
	private void queueTemporaryPixelDataFilesForDeletion(Attribute aPixelData) {
		if (aPixelData != null) {
			File[] frameFiles = null;
			if (aPixelData instanceof OtherByteAttributeMultipleCompressedFrames) {
				frameFiles = ((OtherByteAttributeMultipleCompressedFrames)aPixelData).getFiles();
			}
			else if (aPixelData instanceof OtherByteAttributeMultipleFilesOnDisk) {
				frameFiles = ((OtherByteAttributeMultipleFilesOnDisk)aPixelData).getFiles();
			}
			else if (aPixelData instanceof OtherWordAttributeMultipleFilesOnDisk) {
				frameFiles = ((OtherWordAttributeMultipleFilesOnDisk)aPixelData).getFiles();
			}
			if (frameFiles != null) {
				if (filesToDeleteAfterWritingDicomFile == null) {
					filesToDeleteAfterWritingDicomFile = new ArrayList<>(Arrays.asList(frameFiles));	// make a copy because Arrays.asList() is documented not to support any adding elements "https://stackoverflow.com/questions/5755477/java-list-add-unsupportedoperationexception"
				}
				else {
					Collections.addAll(filesToDeleteAfterWritingDicomFile,frameFiles);
				}
			}
		}
	}

	private int generateDICOMPyramidPixelDataModule(AttributeList list,String outputformat,String transferSyntax) throws DicomException, IOException {
		int numberOfPyramidLevels = 1;
		{ Attribute a = new LongStringAttribute(pixelmedPrivatePyramidDataBlockReservation); a.addValue(pixelmedPrivateCreatorForPyramidData); list.put(a); }
		SequenceAttribute pyramidData = new SequenceAttribute(pixelmedPrivatePyramidData);
		list.put(pyramidData);
		
		boolean isFirstList = true;
		AttributeList oldList = list;
		while (true) {
			TiledFramesIndex index = new TiledFramesIndex(oldList,true/*physical*/,false/*buildInverseIndex*/,true/*ignorePlanePosition*/);
			int numberOfColumnsOfTiles = index.getNumberOfColumnsOfTiles();
			int numberOfRowsOfTiles = index.getNumberOfRowsOfTiles();
			if (numberOfColumnsOfTiles <= 1 && numberOfRowsOfTiles <= 1) break;
			++numberOfPyramidLevels;
			slf4jlogger.debug("generateDICOMPyramidPixelDataModule(): downsampling from numberOfColumnsOfTiles = {}, numberOfRowsOfTiles = {}",numberOfColumnsOfTiles,numberOfRowsOfTiles);
			AttributeList newList = new AttributeList();
			if (!isFirstList) {
				Attribute a = new UniqueIdentifierAttribute(TagFromName.TransferSyntaxUID); a.addValue(transferSyntax); oldList.put(a);	// need this or won't decompress
			}
			TiledPyramid.createDownsampledDICOMAttributes(oldList,newList,index,outputformat,true/*populateunchangedimagepixeldescriptionmacroattributes*/,false/*populatefunctionalgroups*/);
			if (!isFirstList) {
				oldList.remove(TagFromName.TransferSyntaxUID);	// need to remove it again since not allowed anywhere except meta header, which will be added later
			}
			pyramidData.addItem(newList);
			queueTemporaryPixelDataFilesForDeletion(newList.get(TagFromName.PixelData));	// PixelData in newList will use files that need to be deleted after writing
			oldList = newList;
			isFirstList = false;
		}
		return numberOfPyramidLevels;
	}
	
	private void parseTIFFImageDescription(String[] description,AttributeList descriptionList,AttributeList commonDescriptionList) throws DicomException {
		AttributeList list = new AttributeList();
		String manufacturer = "";
		String manufacturerModelName = "";
		String softwareVersions = "";
		String deviceSerialNumber = "";
		String date = "";
		String time = "";
		boolean downsampled = false;
		double micronsPerPixel = 0d;
		if (description != null && description.length > 0) {
			slf4jlogger.debug("parseTIFFImageDescription(): description.length = {}",description.length);
			for (String d : description) {
				slf4jlogger.debug("parseTIFFImageDescription(): String = {}",d);
				
				if (d.contains("Aperio")) {
					manufacturer = "Aperio";
					slf4jlogger.debug("parseTIFFImageDescription(): found manufacturer {}",manufacturer);

				
					// Aperio Image Library v10.0.51
					// 46920x33014 [0,100 46000x32914] (256x256) JPEG/RGB Q=30|AppMag = 20|StripeWidth = 2040|ScanScope ID = CPAPERIOCS|Filename = CMU-1|Date = 12/29/09|Time = 09:59:15|User = b414003d-95c6-48b0-9369-8010ed517ba7|Parmset = USM Filter|MPP = 0.4990|Left = 25.691574|Top = 23.449873|LineCameraSkew = -0.000424|LineAreaXOffset = 0.019265|LineAreaYOffset = -0.000313|Focus Offset = 0.000000|ImageID = 1004486|OriginalWidth = 46920|Originalheight = 33014|Filtered = 5|ICC Profile = ScanScope v1

					// Aperio Image Library v10.0.51
					// 46000x32914 -> 1024x732 - |AppMag = 20|StripeWidth = 2040|ScanScope ID = CPAPERIOCS|Filename = CMU-1|Date = 12/29/09|Time = 09:59:15|User = b414003d-95c6-48b0-9369-8010ed517ba7|Parmset = USM Filter|MPP = 0.4990|Left = 25.691574|Top = 23.449873|LineCameraSkew = -0.000424|LineAreaXOffset = 0.019265|LineAreaYOffset = -0.000313|Focus Offset = 0.000000|ImageID = 1004486|OriginalWidth = 46920|Originalheight = 33014|Filtered = 5|ICC Profile = ScanScope v1

					// Aperio Image Library v10.0.51
					// 46920x33014 [0,100 46000x32914] (256x256) -> 11500x8228 JPEG/RGB Q=65
				
					// Aperio Image Library v11.2.1
					// 46000x32914 [0,0 46000x32893] (240x240) J2K/KDU Q=30;CMU-1;Aperio Image Library v10.0.51
					// 46920x33014 [0,100 46000x32914] (256x256) JPEG/RGB Q=30|AppMag = 20|StripeWidth = 2040|ScanScope ID = CPAPERIOCS|Filename = CMU-1|Date = 12/29/09|Time = 09:59:15|User = b414003d-95c6-48b0-9369-8010ed517ba7|Parmset = USM Filter|MPP = 0.4990|Left = 25.691574|Top = 23.449873|LineCameraSkew = -0.000424|LineAreaXOffset = 0.019265|LineAreaYOffset = -0.000313|Focus Offset = 0.000000|ImageID = 1004486|OriginalWidth = 46920|Originalheight = 33014|Filtered = 5|OriginalWidth = 46000|OriginalHeight = 32914

					// Aperio Image Library v11.2.1
					// 46000x32893 -> 1024x732 - ;CMU-1;Aperio Image Library v10.0.51
					// 46920x33014 [0,100 46000x32914] (256x256) JPEG/RGB Q=30|AppMag = 20|StripeWidth = 2040|ScanScope ID = CPAPERIOCS|Filename = CMU-1|Date = 12/29/09|Time = 09:59:15|User = b414003d-95c6-48b0-9369-8010ed517ba7|Parmset = USM Filter|MPP = 0.4990|Left = 25.691574|Top = 23.449873|LineCameraSkew = -0.000424|LineAreaXOffset = 0.019265|LineAreaYOffset = -0.000313|Focus Offset = 0.000000|ImageID = 1004486|OriginalWidth = 46920|Originalheight = 33014|Filtered = 5|OriginalWidth = 46000|OriginalHeight = 32914

					// Aperio Image Library v11.2.1
					// macro 1280x431
					
					downsampled = d.contains("->");	// need to detect this when MPP of base layer described for downsampled layer
					slf4jlogger.debug("parseTIFFImageDescription(): found downsampled = {}",downsampled);

					try {
						BufferedReader r = new BufferedReader(new StringReader(d));
						String line = null;
						while ((line=r.readLine()) != null) {
							{
								// |Date = 12/29/09|
								Pattern p = Pattern.compile(".*[|]Date[ ]*=[ ]*([0-9][0-9])/([0-9][0-9])/([0-9][0-9])[|].*");
								Matcher m = p.matcher(line);
								if (m.matches()) {
									slf4jlogger.trace("parseTIFFImageDescription(): have date match");
									int groupCount = m.groupCount();
									if (groupCount == 3) {
										slf4jlogger.trace("parseTIFFImageDescription(): have date correct groupCount");
										String month = m.group(1);
										String day = m.group(2);
										String twodigityear = m.group(3);
										date = "20" + twodigityear + month + day;
										slf4jlogger.debug("parseTIFFImageDescription(): found date {}",date);
									}
								}
							}
							{
								// |Time = 09:59:15|
								Pattern p = Pattern.compile(".*[|]Time[ ]*=[ ]*([0-9][0-9]):([0-9][0-9]):([0-9][0-9])[|].*");
								Matcher m = p.matcher(line);
								if (m.matches()) {
									slf4jlogger.trace("parseTIFFImageDescription(): have time match");
									int groupCount = m.groupCount();
									if (groupCount == 3) {
										slf4jlogger.trace("parseTIFFImageDescription(): have time correct groupCount");
										String hh = m.group(1);
										String mm = m.group(2);
										String ss = m.group(3);
										time = hh + mm + ss;
										slf4jlogger.debug("parseTIFFImageDescription(): found time {}",time);
									}
								}
							}
							{
								// |MPP = 0.4990|
								Pattern p = Pattern.compile(".*[|]MPP[ ]*=[ ]*([0-9][.][0-9][0-9]*)[|].*");
								Matcher m = p.matcher(line);
								if (m.matches()) {
									slf4jlogger.debug("parseTIFFImageDescription(): have MPP match");
									int groupCount = m.groupCount();
									if (groupCount == 1) {
										slf4jlogger.debug("parseTIFFImageDescription(): have MPP correct groupCount");
										try {
											micronsPerPixel = Double.parseDouble(m.group(1));
											slf4jlogger.debug("parseTIFFImageDescription(): found micronsPerPixel (MPP) {}",micronsPerPixel);
										}
										catch (NumberFormatException e) {
											slf4jlogger.error("Failed to parse MPP to double ",e);
										}
									}
								}
							}
						}
					}
					catch (IOException e) {
						slf4jlogger.error("Failed to parse ImageDescription ",e);
					}
				}
				else if (d.contains("X scan size")) {
					// encountered in 3D Histech uncompressed TIFF samples
					manufacturer = "3D Histech";
					slf4jlogger.debug("parseTIFFImageDescription(): guessing manufacturer {}",manufacturer);
					
					// ImageDescription: X scan size = 4.27mm
					// Y scan size = 28.90mm
					// X offset = 74.00mm
					// Y offset = 23.90mm
					// X resolution = 17067
					// Y resolution = 115600
					// Triple Simultaneous Acquisition
					// Resolution (um) = 0.25
					// Tissue Start Pixel = 40400
					// Tissue End Pixel = 108800
					// Source = Bright Field
					
					try {
						BufferedReader r = new BufferedReader(new StringReader(d));
						String line = null;
						while ((line=r.readLine()) != null) {
							{
								// Resolution (um) = 0.25
								Pattern p = Pattern.compile(".*Resolution[ ]*[(]um[)][ ]*=[ ]*([0-9][.][0-9][0-9]*).*");
								Matcher m = p.matcher(line);
								if (m.matches()) {
									slf4jlogger.debug("parseTIFFImageDescription(): have Resolution (um) match");
									int groupCount = m.groupCount();
									if (groupCount == 1) {
										slf4jlogger.debug("parseTIFFImageDescription(): have Resolution (um) correct groupCount");
										try {
											micronsPerPixel = Double.parseDouble(m.group(1));
											slf4jlogger.debug("parseTIFFImageDescription(): found Resolution (um) {}",micronsPerPixel);
										}
										catch (NumberFormatException e) {
											slf4jlogger.error("Failed to parse MPP to double ",e);
										}
									}
								}
							}
						}
					}
					catch (IOException e) {
						slf4jlogger.error("Failed to parse ImageDescription ",e);
					}
				}
			}
			// common ...
			if (manufacturer.length() > 0) {
				{ Attribute a = new LongStringAttribute(TagFromName.Manufacturer); a.addValue(manufacturer); commonDescriptionList.put(a); }
			}
			if (manufacturerModelName.length() > 0) {
				{ Attribute a = new LongStringAttribute(TagFromName.ManufacturerModelName); a.addValue(manufacturerModelName); commonDescriptionList.put(a); }
			}
			if (softwareVersions.length() > 0) {
				{ Attribute a = new LongStringAttribute(TagFromName.SoftwareVersions); a.addValue(softwareVersions); commonDescriptionList.put(a); }
			}
			if (deviceSerialNumber.length() > 0) {
				{ Attribute a = new LongStringAttribute(TagFromName.DeviceSerialNumber); a.addValue(deviceSerialNumber); commonDescriptionList.put(a); }
			}
			if (date.length() > 0) {
				{ Attribute a = new DateTimeAttribute(TagFromName.AcquisitionDateTime); a.addValue(date+time); commonDescriptionList.put(a); }
				{ Attribute a = new DateAttribute(TagFromName.AcquisitionDate); a.addValue(date); commonDescriptionList.put(a); }
				{ Attribute a = new DateAttribute(TagFromName.ContentDate); a.addValue(date); commonDescriptionList.put(a); }
				{ Attribute a = new DateAttribute(TagFromName.SeriesDate); a.addValue(date); commonDescriptionList.put(a); }
				{ Attribute a = new DateAttribute(TagFromName.StudyDate); a.addValue(date); commonDescriptionList.put(a); }
			}
			if (time.length() > 0) {
				{ Attribute a = new TimeAttribute(TagFromName.AcquisitionTime); a.addValue(time); commonDescriptionList.put(a); }
				{ Attribute a = new TimeAttribute(TagFromName.ContentTime); a.addValue(time); commonDescriptionList.put(a); }
				{ Attribute a = new TimeAttribute(TagFromName.SeriesTime); a.addValue(time); commonDescriptionList.put(a); }
				{ Attribute a = new TimeAttribute(TagFromName.StudyTime); a.addValue(time); commonDescriptionList.put(a); }
			}
			
			// NOT common ...
			if (!downsampled && micronsPerPixel != 0) {		// take care not to use MPP of base layer specified for downsampled layer, e.g., for 2nd directory of strips
				double mmPerPixel = micronsPerPixel/1000.0d;
				{ Attribute a = new DecimalStringAttribute(TagFromName.PixelSpacing); a.addValue(mmPerPixel); descriptionList.put(a); }
			}
		}
		else {
				slf4jlogger.debug("parseTIFFImageDescription(): no ImageDescription");
		}
	}

	private void convertTIFFTilesToDicomMultiFrame(String jsonfile,TIFFFile inputFile,String outputFileName,int instanceNumber,
				long imageWidth,long imageLength,
				long[] tileOffsets,long[] tileByteCounts,long tileWidth,long tileLength,
				long bitsPerSample,long compression,byte[] jpegTables,byte[] iccProfile,long photometric,long samplesPerPixel,long planarConfig,long sampleFormat,double mmPerPixel,
				String modality,String sopClass,String transferSyntax,
				String containerIdentifier,String specimenIdentifier,String specimenUID,
				AttributeList descriptionList,
				boolean addTIFF,boolean useBigTIFF,boolean addPyramid) throws IOException, DicomException, TIFFException {

		slf4jlogger.debug("convertTIFFTilesToDicomMultiFrame(): instanceNumber = {}",instanceNumber);
		slf4jlogger.debug("convertTIFFTilesToDicomMultiFrame(): transferSyntax supplied = {}",transferSyntax);
		slf4jlogger.debug("convertTIFFTilesToDicomMultiFrame(): photometric in TIFF file = {}",photometric);

		if (transferSyntax == null || transferSyntax.length() == 0) {
			if (compression == 0 || compression == 1) {		// absent or specified as uncompressed
				transferSyntax = TransferSyntax.ExplicitVRLittleEndian;
			}
			else if (compression == 7) {		// "new" JPEG per TTN2 as used by Aperio in SVS
				// really should check what is in there ... could be lossless, or 12 bit per TTN2 :(
				transferSyntax = TransferSyntax.JPEGBaseline;
			}
			else if (compression == 33005) {	// Aperio J2K RGB
				transferSyntax = TransferSyntax.JPEG2000;
			}
			else {
				throw new TIFFException("Unsupported compression = "+compression);
			}
		}
		slf4jlogger.debug("convertTIFFTilesToDicomMultiFrame(): transferSyntax now = {}",transferSyntax);

		String recompressAsFormat = null;
		boolean recompressLossy = false;
		if (transferSyntax != null && transferSyntax.length() > 0) {
			recompressAsFormat = CompressedFrameEncoder.chooseOutputFormatForTransferSyntax(transferSyntax);
			recompressLossy = new TransferSyntax(transferSyntax).isLossy();
		}
		slf4jlogger.debug("convertTIFFTilesToDicomMultiFrame(): recompressAsFormat = {}",recompressAsFormat);
		
		AttributeList list = new AttributeList();
		
		int numberOfTiles = tileOffsets.length;
		long outputPhotometric = generateDICOMPixelDataMultiFrameImageFromTIFFFile(inputFile,list,numberOfTiles,tileOffsets,tileByteCounts,tileWidth,tileLength,bitsPerSample,compression,photometric,jpegTables,iccProfile,recompressAsFormat,recompressLossy);
		long outputCompression = recompressAsFormat == null
			? compression
			: (recompressAsFormat.equals("jpeg")
				? 7
				: (recompressAsFormat.equals("jpeg2000")
					? 33005			 	// Aperio J2K RGB
					: compression));	// leave alone if unrecognized ... should not happen

		slf4jlogger.debug("convertTIFFTilesToDicomMultiFrame(): photometric changed from {} to {}",photometric,outputPhotometric);
		slf4jlogger.debug("convertTIFFTilesToDicomMultiFrame(): compression changed from {} to {}",compression,outputCompression);

		generateDICOMPixelDataModuleAttributes(list,numberOfTiles,tileWidth,tileLength,bitsPerSample,outputCompression,outputPhotometric,samplesPerPixel,planarConfig,sampleFormat,recompressAsFormat,recompressLossy,sopClass);
		
		CommonConvertedAttributeGeneration.generateCommonAttributes(list,""/*patientName*/,""/*patientID*/,""/*studyID*/,""/*seriesNumber*/,Integer.toString(instanceNumber),modality,sopClass,false/*generateUnassignedConverted*/);

		if (SOPClass.VLWholeSlideMicroscopyImageStorage.equals(sopClass)) {
			generateDICOMWholeSlideMicroscopyImageAttributes(list,imageWidth,imageLength,mmPerPixel,containerIdentifier,specimenIdentifier,specimenUID);
		}

		{ Attribute a = new LongStringAttribute(TagFromName.ManufacturerModelName); a.addValue(this.getClass().getName()); list.put(a); }

		if (descriptionList != null) {
			// override such things as Manufacturer, UIDs, dates, times, if they were obtained from the ImageDescription TIFF tag or need to be common
			list.putAll(descriptionList);
		}
		
		new SetCharacteristicsFromSummary(jsonfile,list);

		// only now add ICC profile, so as not be overriden by any OpticalPathSequence in SetCharacteristicsFromSummary
		if (SOPClass.VLWholeSlideMicroscopyImageStorage.equals(sopClass)) {
			if (samplesPerPixel > 1) {	// (001229)
				iccProfile = addICCProfileToOpticalPathSequence(list,iccProfile);	// adds known or default, since required
			}
		}
		else if (iccProfile != null && iccProfile.length > 0) {		// add known
			{ Attribute a = new OtherByteAttribute(TagFromName.ICCProfile); a.setValues(iccProfile); list.put(a); }
			slf4jlogger.debug("Created ICC Profile attribute of length {}",iccProfile.length);
		}
		// else do not add default ICC Profile
		
		CodingSchemeIdentification.replaceCodingSchemeIdentificationSequenceWithCodingSchemesUsedInAttributeList(list);
		
		list.insertSuitableSpecificCharacterSetForAllStringValues();	// (001158)

		FileMetaInformation.addFileMetaInformation(list,transferSyntax,"OURAETITLE");
		
		int numberOfPyramidLevels = 1;	// at a minimum the base layer in top level data set PixelData

		//boolean addPyramid = SOPClass.VLWholeSlideMicroscopyImageStorage.equals(sopClass);
		//boolean addPyramid = false;

		if (addPyramid) {
			try {
				numberOfPyramidLevels = generateDICOMPyramidPixelDataModule(list,recompressAsFormat,transferSyntax);	// will use existing PixelData attribute contents ... need to do this after TransferSyntax is set in FileMetaInformation
			}
			catch (DicomException e) {
				e.printStackTrace(System.err);
			}
		}

		byte[] preamble = null;
		
		if (addTIFF) {
			long lowerPhotometric =	// what to use when/if making lower pyramidal levels
				transferSyntax.equals(TransferSyntax.JPEGBaseline) || transferSyntax.equals(TransferSyntax.JPEG2000)
				? 6				// YCbCr, since that is what the codec will do regardless, i.e., be consistent with what TiledPyramid.createDownsampledDICOMAttributes() does
				: photometric;	// leave it the same unless we are recompressing it; is independent of whatever outputPhotometric happens to be
			slf4jlogger.debug("convertTIFFTilesToDicomMultiFrame(): lowerPhotometric = {}",lowerPhotometric);

			try {
				long[][] frameDataByteOffsets = new long[numberOfPyramidLevels][];
				long[][] frameDataLengths = new long[numberOfPyramidLevels][];
				long[] imageWidths = new long[numberOfPyramidLevels];
				long[] imageLengths = new long[numberOfPyramidLevels];
				long  byteOffsetFromFileStartOfNextAttributeAfterPixelData = AddTIFFOrOffsetTables.getByteOffsetsAndLengthsOfFrameDataFromStartOfFile(list,transferSyntax,frameDataByteOffsets,frameDataLengths,imageWidths,imageLengths);
				preamble = AddTIFFOrOffsetTables.makeTIFFInPreambleAndAddDataSetTrailingPadding(byteOffsetFromFileStartOfNextAttributeAfterPixelData,numberOfPyramidLevels,frameDataByteOffsets,frameDataLengths,imageWidths,imageLengths,list,
					tileWidth,tileLength,bitsPerSample,outputCompression,outputPhotometric,lowerPhotometric,samplesPerPixel,planarConfig,sampleFormat,iccProfile,mmPerPixel,useBigTIFF);
			}
			catch (DicomException e) {
				e.printStackTrace(System.err);
			}
		}
		
		list.write(outputFileName,transferSyntax,true,true,preamble);
		
		if (filesToDeleteAfterWritingDicomFile != null) {
			for (File tmpFile : filesToDeleteAfterWritingDicomFile) {
				tmpFile.delete();
			}
			filesToDeleteAfterWritingDicomFile = null;
		}
	}
	
	private void convertTIFFTilesToDicomSingleFrameMergingStrips(String jsonfile,TIFFFile inputFile,String outputFileName,int instanceNumber,
				long imageWidth,long imageLength,
				long[] pixelOffset,long[] pixelByteCount,long pixelWidth,long pixelLength,
				long bitsPerSample,long compression,byte[] jpegTables,byte[] iccProfile,long photometric,long samplesPerPixel,long planarConfig,long sampleFormat,
				String modality,String sopClass,String transferSyntax,
				AttributeList descriptionList,
				boolean addTIFF,boolean useBigTIFF) throws IOException, DicomException, TIFFException {

		slf4jlogger.debug("convertTIFFTilesToDicomSingleFrameMergingStrips(): instanceNumber = {}",instanceNumber);

		if (addTIFF) slf4jlogger.warn("convertTIFFTilesToDicomSingleFrameMergingStrips(): Adding TIFF not yet implemented for single frame conversion");

		if (transferSyntax == null || transferSyntax.length() == 0) {
			if (compression == 0 || compression == 1) {		// absent or specified as uncompressed
				transferSyntax = TransferSyntax.ExplicitVRLittleEndian;
			}
			else if (compression == 7) {		// "new" JPEG per TTN2 as used by Aperio in SVS
				// really should check what is in there ... could be lossless, or 12 bit per TTN2 :(
				transferSyntax = TransferSyntax.JPEGBaseline;
			}
			else if (compression == 33005) {	// Aperio J2K RGB
				transferSyntax = TransferSyntax.JPEG2000;
			}
			else {
				throw new TIFFException("Unsupported compression = "+compression);
			}
		}
		
		String recompressAsFormat = null;
		boolean recompressLossy = false;
		if (transferSyntax != null && transferSyntax.length() > 0) {
			recompressAsFormat = CompressedFrameEncoder.chooseOutputFormatForTransferSyntax(transferSyntax);
			recompressLossy = new TransferSyntax(transferSyntax).isLossy();
		}
		
		AttributeList list = new AttributeList();
		
		long outputPhotometric = generateDICOMPixelDataSingleFrameImageFromTIFFFileMergingStrips(inputFile,list,pixelOffset,pixelByteCount,pixelWidth,pixelLength,bitsPerSample,compression,photometric,jpegTables,iccProfile,recompressAsFormat,recompressLossy);
		long outputCompression = compression;	// do not currently support recompression for single frame images, so this never changes

		slf4jlogger.debug("convertTIFFTilesToDicomSingleFrameMergingStrips(): photometric changed from {} to {}",photometric,outputPhotometric);
		slf4jlogger.debug("convertTIFFTilesToDicomSingleFrameMergingStrips(): compression changed from {} to {}",compression,outputCompression);

		generateDICOMPixelDataModuleAttributes(list,1/*numberOfFrames*/,pixelWidth,pixelLength,bitsPerSample,outputCompression,outputPhotometric,samplesPerPixel,planarConfig,sampleFormat,recompressAsFormat,recompressLossy,sopClass);

		if (iccProfile != null && iccProfile.length > 0) {
			{ Attribute a = new OtherByteAttribute(TagFromName.ICCProfile); a.setValues(iccProfile); list.put(a); }
			slf4jlogger.debug("convertTIFFTilesToDicomSingleFrameMergingStrips(): Created ICC Profile attribute of length {}",iccProfile.length);
		}

		CommonConvertedAttributeGeneration.generateCommonAttributes(list,""/*patientName*/,""/*patientID*/,""/*studyID*/,""/*seriesNumber*/,Integer.toString(instanceNumber),modality,sopClass,false/*generateUnassignedConverted*/);
		
		{ Attribute a = new LongStringAttribute(TagFromName.ManufacturerModelName); a.addValue(this.getClass().getName()); list.put(a); }
		
		if (descriptionList != null) {
			// override such things as Manufacturer if they were obtained from the ImageDescription TIFF tag
			list.putAll(descriptionList);
		}
		
		new SetCharacteristicsFromSummary(jsonfile,list);

		CodingSchemeIdentification.replaceCodingSchemeIdentificationSequenceWithCodingSchemesUsedInAttributeList(list);

		list.insertSuitableSpecificCharacterSetForAllStringValues();

		FileMetaInformation.addFileMetaInformation(list,transferSyntax,"OURAETITLE");
		list.write(outputFileName,transferSyntax,true,true);
		
		if (filesToDeleteAfterWritingDicomFile != null) {
			for (File tmpFile : filesToDeleteAfterWritingDicomFile) {
				tmpFile.delete();
			}
			filesToDeleteAfterWritingDicomFile = null;
		}
	}
	
	private void convertTIFFTilesToDicomSingleFrame(String jsonfile,TIFFFile inputFile,String outputFileName,int instanceNumber,
				long imageWidth,long imageLength,
				long pixelOffset,long pixelByteCount,long pixelWidth,long pixelLength,
				long bitsPerSample,long compression,byte[] jpegTables,byte[] iccProfile,long photometric,long samplesPerPixel,long planarConfig,long sampleFormat,
				String modality,String sopClass,String transferSyntax,
				AttributeList descriptionList,
				boolean addTIFF,boolean useBigTIFF) throws IOException, DicomException, TIFFException {

		slf4jlogger.debug("convertTIFFTilesToDicomSingleFrame(): instanceNumber = {}",instanceNumber);

		if (addTIFF) slf4jlogger.warn("convertTIFFTilesToDicomSingleFrame(): Adding TIFF not yet implemented for single frame conversion");

		if (transferSyntax == null || transferSyntax.length() == 0) {
			if (compression == 0 || compression == 1) {		// absent or specified as uncompressed
				transferSyntax = TransferSyntax.ExplicitVRLittleEndian;
			}
			else if (compression == 7) {		// "new" JPEG per TTN2 as used by Aperio in SVS
				// really should check what is in there ... could be lossless, or 12 bit per TTN2 :(
				transferSyntax = TransferSyntax.JPEGBaseline;
			}
			else if (compression == 33005) {	// Aperio J2K RGB
				transferSyntax = TransferSyntax.JPEG2000;
			}
			else {
				throw new TIFFException("Unsupported compression = "+compression);
			}
		}
		
		String recompressAsFormat = null;
		boolean recompressLossy = false;
		if (transferSyntax != null && transferSyntax.length() > 0) {
			recompressAsFormat = CompressedFrameEncoder.chooseOutputFormatForTransferSyntax(transferSyntax);
			recompressLossy = new TransferSyntax(transferSyntax).isLossy();
		}
		
		AttributeList list = new AttributeList();
		
		long outputPhotometric = generateDICOMPixelDataSingleFrameImageFromTIFFFile(inputFile,list,pixelOffset,pixelByteCount,pixelWidth,pixelLength,bitsPerSample,compression,photometric,jpegTables,iccProfile,recompressAsFormat,recompressLossy);
		long outputCompression = compression;	// do not currently support recompression for single frame images, so this never changes

		slf4jlogger.debug("convertTIFFTilesToDicomSingleFrame(): photometric changed from {} to {}",photometric,outputPhotometric);
		slf4jlogger.debug("convertTIFFTilesToDicomSingleFrame(): compression changed from {} to {}",compression,outputCompression);

		generateDICOMPixelDataModuleAttributes(list,1/*numberOfFrames*/,pixelWidth,pixelLength,bitsPerSample,outputCompression,outputPhotometric,samplesPerPixel,planarConfig,sampleFormat,recompressAsFormat,recompressLossy,sopClass);

		if (iccProfile != null && iccProfile.length > 0) {
			{ Attribute a = new OtherByteAttribute(TagFromName.ICCProfile); a.setValues(iccProfile); list.put(a); }
			slf4jlogger.debug("Created ICC Profile attribute of length {}",iccProfile.length);
		}

		CommonConvertedAttributeGeneration.generateCommonAttributes(list,""/*patientName*/,""/*patientID*/,""/*studyID*/,""/*seriesNumber*/,Integer.toString(instanceNumber),modality,sopClass,false/*generateUnassignedConverted*/);
		
		{ Attribute a = new LongStringAttribute(TagFromName.ManufacturerModelName); a.addValue(this.getClass().getName()); list.put(a); }
		
		if (descriptionList != null) {
			// override such things as Manufacturer if they were obtained from the ImageDescription TIFF tag
			list.putAll(descriptionList);
		}
		
		new SetCharacteristicsFromSummary(jsonfile,list);

		CodingSchemeIdentification.replaceCodingSchemeIdentificationSequenceWithCodingSchemesUsedInAttributeList(list);

		list.insertSuitableSpecificCharacterSetForAllStringValues();

		FileMetaInformation.addFileMetaInformation(list,transferSyntax,"OURAETITLE");
		list.write(outputFileName,transferSyntax,true,true);
		
		if (filesToDeleteAfterWritingDicomFile != null) {
			for (File tmpFile : filesToDeleteAfterWritingDicomFile) {
				tmpFile.delete();
			}
			filesToDeleteAfterWritingDicomFile = null;
		}
	}

	/**
	 * <p>Read a TIFF image input format file and create an image of a specified or appropriate SOP Class.</p>
	 *
	 * @param	jsonfile		JSON file describing the functional groups and attributes and values to be added or replaced
	 * @param	inputFileName
	 * @param	outputFilePrefix
	 * @param	modality	may be null
	 * @param	sopClass	may be null
	 * @param	transferSyntax	may be null
	 * @param	addTIFF		whether or not to add a TIFF IFD in the DICOM preamble to make a dual=personality DICOM-TIFF file sharing the same pixel data
	 * @param	useBigTIFF	whether or not to create a BigTIFF rather than Classic TIFF file
	 * @param	addPyramid	whether or not to add multi-resolution pyramid (downsampled) layers to the TIFF IFD and a corresponding DICOM private data element in the same file
	 * @param	mergeStrips	whether or not to merge an image with more than one strip into a single DICOM image, or to create a separate image or frame for each strip
	 * @exception			IOException
	 * @exception			DicomException
	 * @exception			TIFFException
	 * @exception			NumberFormatException
	 */
	public TIFFToDicom(String jsonfile,String inputFileName,String outputFilePrefix,String modality,String sopClass,String transferSyntax,boolean addTIFF,boolean useBigTIFF,boolean addPyramid,boolean mergeStrips)
			throws IOException, DicomException, TIFFException, NumberFormatException {
			
		TIFFImageFileDirectories ifds = new TIFFImageFileDirectories();
		ifds.read(inputFileName);
		
		boolean isWSI = sopClass != null && sopClass.equals(SOPClass.VLWholeSlideMicroscopyImageStorage);
		slf4jlogger.debug("isWSI={}",isWSI);
		
		double mmPerPixelBaseLayerDefault = isWSI ? (0.5/1000) : 0.0;	// typically 20× (0.5 μm/pixel) and 40× (0.25 μm/pixel) - assume 20x for 1st IFD if not overriden later :(
		slf4jlogger.debug("mmPerPixelBaseLayerDefault={}",mmPerPixelBaseLayerDefault);
		
		long widthOfBaseLayerInPixels = 0;

		byte[] iccProfileOfBaseLayer = null;

		AttributeList commonDescriptionList = new AttributeList();		// keep track of stuff defined once but reusable for subsequent images
		{ Attribute a = new UniqueIdentifierAttribute(TagFromName.SeriesInstanceUID); a.addValue(u.getAnotherNewUID()); commonDescriptionList.put(a); }	// (001163)
		{ Attribute a = new UniqueIdentifierAttribute(TagFromName.StudyInstanceUID); a.addValue(u.getAnotherNewUID()); commonDescriptionList.put(a); }	// (001163)
		{
			java.util.Date currentDateTime = new java.util.Date();
			String currentDate = new java.text.SimpleDateFormat("yyyyMMdd").format(currentDateTime);
			String currentTime = new java.text.SimpleDateFormat("HHmmss.SSS").format(currentDateTime);
			{ Attribute a = new DateAttribute(TagFromName.StudyDate);   a.addValue(currentDate); commonDescriptionList.put(a); }
			{ Attribute a = new DateAttribute(TagFromName.SeriesDate);  a.addValue(currentDate); commonDescriptionList.put(a); }
			{ Attribute a = new DateAttribute(TagFromName.ContentDate); a.addValue(currentDate); commonDescriptionList.put(a); }
			{ Attribute a = new TimeAttribute(TagFromName.StudyTime);   a.addValue(currentTime); commonDescriptionList.put(a); }
			{ Attribute a = new TimeAttribute(TagFromName.SeriesTime);  a.addValue(currentTime); commonDescriptionList.put(a); }
			{ Attribute a = new TimeAttribute(TagFromName.ContentTime); a.addValue(currentTime); commonDescriptionList.put(a); }
		}
		
		String containerIdentifier = "SLIDE_1";
		String specimenIdentifier = "SPECIMEN_1";
		String specimenUID = u.getAnotherNewUID();

		int dirNum = 0;
		ArrayList<TIFFImageFileDirectory> ifdlist = ifds.getListOfImageFileDirectories();
		for (TIFFImageFileDirectory ifd : ifdlist) {
			slf4jlogger.debug("Directory={}",dirNum);
		
			// SubFileType (254) LONG (4) 1<0>
			long imageWidth = ifd.getSingleNumericValue(TIFFTags.IMAGEWIDTH,0,0);
			slf4jlogger.debug("imageWidth={}",imageWidth);
			long imageLength = ifd.getSingleNumericValue(TIFFTags.IMAGELENGTH,0,0);
			slf4jlogger.debug("imageLength={}",imageLength);
			long bitsPerSample = ifd.getSingleNumericValue(TIFFTags.BITSPERSAMPLE,0,0);
			slf4jlogger.debug("bitsPerSample={}",bitsPerSample);
			long compression = ifd.getSingleNumericValue(TIFFTags.COMPRESSION,0,0);
			slf4jlogger.debug("compression={}",compression);
			long photometric = ifd.getSingleNumericValue(TIFFTags.PHOTOMETRIC,0,0);
			slf4jlogger.debug("photometric={}",photometric);
			// Orientation (274) SHORT (3) 1<1>
			long samplesPerPixel = ifd.getSingleNumericValue(TIFFTags.SAMPLESPERPIXEL,0,0);
			slf4jlogger.debug("samplesPerPixel={}",samplesPerPixel);
			// XResolution (282) RATIONAL (5) 1<10>			Generic-TIFF/CMU-1.tiff - obviously invalid
			// YResolution (283) RATIONAL (5) 1<10>			Generic-TIFF/CMU-1.tiff - obviously invalid
			// XResolution (282) RATIONAL (5) 1<40000/1>
			// YResolution (283) RATIONAL (5) 1<40000/1>
			// XResolution (282) RATIONAL (5) 1<20576.4>	PESO - missing denominator
			// YResolution (283) RATIONAL (5) 1<20576.4>	PESO - missing denominator
			double xResolution = ifd.getSingleRationalValue(TIFFTags.XRESOLUTION,0,0);
			slf4jlogger.debug("xResolution={}",xResolution);
			double yResolution = ifd.getSingleRationalValue(TIFFTags.YRESOLUTION,0,0);
			slf4jlogger.debug("yResolution={}",yResolution);

			long planarConfig = ifd.getSingleNumericValue(TIFFTags.PLANARCONFIG,0,1);	// default is 1 (chunky not planar format)
			slf4jlogger.debug("planarConfig={}",planarConfig);

			long sampleFormat = ifd.getSingleNumericValue(TIFFTags.SAMPLEFORMAT,0,1);	// assume unsigned if absent, and assume same for all samples (though that is not required)
			slf4jlogger.debug("sampleFormat={}",sampleFormat);

			byte[] jpegTables = null;
			if (compression == 7) {
				jpegTables = ifd.getByteValues(TIFFTags.JPEGTABLES);
				if (jpegTables != null) {
					slf4jlogger.debug("jpegTables present");
					jpegTables = stripSOIEOIMarkers(jpegTables);
				}
			}
			
			byte[] iccProfile = ifd.getByteValues(TIFFTags.ICCPROFILE);
			if (iccProfile != null) {
				slf4jlogger.debug("ICC profile present, of length {}",iccProfile.length);
			}
			if (iccProfile != null && iccProfile.length > 0) {
				if (dirNum == 0) {
					iccProfileOfBaseLayer = iccProfile;		// store this in case need not specified in subsequent layers
				}
			}
			else {
				if (isWSI && iccProfileOfBaseLayer != null && iccProfileOfBaseLayer.length > 0) {
					slf4jlogger.debug("ICC profile absent or empty so using profile of base layer");
					iccProfile = iccProfileOfBaseLayer;		// use base layer profile if not specified in subsequent layers
				}
			}
			
			boolean makeMultiFrame = SOPClass.isMultiframeImageStorage(sopClass);
			slf4jlogger.debug("makeMultiFrame={}",makeMultiFrame);

			// ResolutionUnit (296) SHORT (3) 1<3>
			long resolutionUnit = ifd.getSingleNumericValue(TIFFTags.RESOLUTIONUNIT,0,2);	// 1 = none, 2 = inch (default), 3 = cm
			slf4jlogger.debug("resolutionUnit={}",resolutionUnit);
			// PageNumber (297) SHORT (3) 2<4 5>
			long tileWidth = ifd.getSingleNumericValue(TIFFTags.TILEWIDTH,0,0);
			slf4jlogger.debug("tileWidth={}",tileWidth);
			long tileLength = ifd.getSingleNumericValue(TIFFTags.TILELENGTH,0,0);
			slf4jlogger.debug("tileLength={}",tileLength);
			
			AttributeList descriptionList = new AttributeList();
			parseTIFFImageDescription(ifd.getStringValues(TIFFTags.IMAGEDESCRIPTION),descriptionList,commonDescriptionList);
			
			double mmPerPixel = Attribute.getSingleDoubleValueOrDefault(descriptionList,TagFromName.PixelSpacing,0d);
			slf4jlogger.debug("mmPerPixel from PixelSpacing extracted from descriptionList={}",mmPerPixel);
			
			if (mmPerPixel == 0) {
				slf4jlogger.debug("mmPerPixel is zero after parsing descriptionList");
				if (xResolution > 0 && (!isWSI || xResolution > 10)) {		// not just greater than missing value of 0, but greater than meaningless incorrect value of 10 in Generic-TIFF/CMU-1.tiff
					if (xResolution == yResolution) {
						if (resolutionUnit == 2) {		// inch
							mmPerPixel = 25.4d / xResolution;
						}
						else if (resolutionUnit == 3) {	// cm
							mmPerPixel = 10.0d / xResolution;
						}
						else if (resolutionUnit == 1) {
							slf4jlogger.debug("not using no meaningful RESOLUTIONUNIT for PixelSpacing");
						}
						else {
							slf4jlogger.debug("not using unrecognized RESOLUTIONUNIT {} for PixelSpacing",resolutionUnit);
						}
					}
					else {
						slf4jlogger.debug("not using non-square or uncalibrated X/YRESOLUTION for PixelSpacing");
					}
				}
				else {
					slf4jlogger.debug("not using missing or obviously invalid XRESOLUTION of {} for PixelSpacing",xResolution);
				}
				slf4jlogger.debug("mmPerPixel is {} after checking X/YRESOLUTION and RESOLUTIONUNIT",mmPerPixel);
			}

			if (isWSI) {
				if (dirNum == 0) {
					widthOfBaseLayerInPixels = imageWidth;		// store this in case need to calculate pixel spacing for subsequent layers
				}
				if (mmPerPixel == 0) {
					slf4jlogger.debug("mmPerPixel is zero for WSI");
					if (dirNum == 0) {		// assume base layer is first ... could check this with extra pass through IFDs :(
						slf4jlogger.debug("using default {} mmPerPixel, assuming first directory is base layer for WSI",mmPerPixelBaseLayerDefault);
						mmPerPixel = mmPerPixelBaseLayerDefault;
					}
					else {
						slf4jlogger.debug("deriving mmPerPixel from pixel width {} relative to base layer width {} and pixel spacing {}",imageWidth,widthOfBaseLayerInPixels,mmPerPixelBaseLayerDefault);
						mmPerPixel = mmPerPixelBaseLayerDefault * widthOfBaseLayerInPixels/imageWidth;		// assumes all images are same physical width
					}
				}
				slf4jlogger.debug("Using mmPerPixel={}",mmPerPixel);
			}

			try {
				long[] tileOffsets = ifd.getNumericValues(TIFFTags.TILEOFFSETS);
				long[] tileByteCounts = ifd.getNumericValues(TIFFTags.TILEBYTECOUNTS);
				if (tileOffsets != null) {
					int numberOfTiles = tileOffsets.length;
					if (tileByteCounts.length != numberOfTiles) {
						throw new TIFFException("Number of tiles uncertain: tileOffsets length = "+tileOffsets.length+" different from tileByteCounts length "+tileByteCounts.length);
					}
					slf4jlogger.debug("numberOfTiles={}",numberOfTiles);
					if (makeMultiFrame) {
						String outputFileName = outputFilePrefix + "_" + dirNum + ".dcm";
						slf4jlogger.info("outputFileName={}",outputFileName);
						int instanceNumber = dirNum+1;
						convertTIFFTilesToDicomMultiFrame(jsonfile,ifds.getFile(),outputFileName,instanceNumber,imageWidth,imageLength,tileOffsets,tileByteCounts,tileWidth,tileLength,bitsPerSample,compression,jpegTables,iccProfile,photometric,samplesPerPixel,planarConfig,sampleFormat,mmPerPixel,
														  modality,sopClass,transferSyntax,containerIdentifier,specimenIdentifier,specimenUID,commonDescriptionList,addTIFF,useBigTIFF,addPyramid);
					}
					else {
						for (int tileNumber=0; tileNumber<numberOfTiles; ++tileNumber) {
							String outputFileName = outputFilePrefix + "_" + dirNum + "_" + tileNumber + ".dcm";
							slf4jlogger.info("outputFileName={}",outputFileName);
							int instanceNumber = (dirNum+1)*100000+(tileNumber+1);
							convertTIFFTilesToDicomSingleFrame(jsonfile,ifds.getFile(),outputFileName,instanceNumber,imageWidth,imageLength,tileOffsets[tileNumber],tileByteCounts[tileNumber],tileWidth,tileLength,bitsPerSample,compression,jpegTables,iccProfile,photometric,samplesPerPixel,planarConfig,sampleFormat,
												   modality,sopClass,transferSyntax,commonDescriptionList,addTIFF,useBigTIFF);
						}
					}
				}
				else {
					long rowsPerStrip = ifd.getSingleNumericValue(TIFFTags.ROWSPERSTRIP,0,0);
					slf4jlogger.debug("rowsPerStrip={}",rowsPerStrip);
					long[] stripOffsets = ifd.getNumericValues(TIFFTags.STRIPOFFSETS);
					long[] stripByteCounts = ifd.getNumericValues(TIFFTags.STRIPBYTECOUNTS);
					if (stripByteCounts != null) {
						slf4jlogger.debug("Strips rather than tiled");
						int numberOfStrips = stripOffsets.length;
						slf4jlogger.debug("numberOfStrips={}",numberOfStrips);
						if (stripByteCounts.length != numberOfStrips) {
							throw new TIFFException("Number of strips uncertain: stripOffsets length = "+stripOffsets.length+" different from stripByteCounts length "+stripByteCounts.length);
						}
						if (rowsPerStrip == imageLength) {
							slf4jlogger.debug("Single strip for entire image");
							if (numberOfStrips != 1) {
								throw new TIFFException("Number of strips uncertain: stripOffsets length = "+stripOffsets.length+" > 1 but rowsPerStrip == imageLength of "+rowsPerStrip);
							}
							String outputFileName = outputFilePrefix + "_" + dirNum + ".dcm";
							slf4jlogger.info("outputFileName={}",outputFileName);
							int instanceNumber = dirNum+1;
							convertTIFFTilesToDicomSingleFrame(jsonfile,ifds.getFile(),outputFileName,instanceNumber,imageWidth,imageLength,stripOffsets[0],stripByteCounts[0],imageWidth,rowsPerStrip,bitsPerSample,compression,jpegTables,iccProfile,photometric,samplesPerPixel,planarConfig,sampleFormat,
												   modality,sopClass,transferSyntax,commonDescriptionList,addTIFF,useBigTIFF);
						}
						else {
							if (mergeStrips) {
								slf4jlogger.debug("Merging strips into single image");
								String outputFileName = outputFilePrefix + "_" + dirNum + ".dcm";
								slf4jlogger.info("outputFileName={}",outputFileName);
								int instanceNumber = dirNum+1;
								convertTIFFTilesToDicomSingleFrameMergingStrips(jsonfile,ifds.getFile(),outputFileName,instanceNumber,imageWidth,imageLength,stripOffsets,stripByteCounts,imageWidth,imageLength,bitsPerSample,compression,jpegTables,iccProfile,photometric,samplesPerPixel,planarConfig,sampleFormat,
													modality,sopClass,transferSyntax,commonDescriptionList,addTIFF,useBigTIFF);
							}
							else {
								slf4jlogger.debug("Not merging strips - each becomes single frame or image");
								if (makeMultiFrame) {
									String outputFileName = outputFilePrefix + "_" + dirNum + ".dcm";
									slf4jlogger.info("outputFileName={}",outputFileName);
									int instanceNumber = dirNum+1;
									convertTIFFTilesToDicomMultiFrame(jsonfile,ifds.getFile(),outputFileName,instanceNumber,imageWidth,imageLength,stripOffsets,stripByteCounts,imageWidth,rowsPerStrip,bitsPerSample,compression,jpegTables,iccProfile,photometric,samplesPerPixel,planarConfig,sampleFormat,mmPerPixel,
													modality,sopClass,transferSyntax,containerIdentifier,specimenIdentifier,specimenUID,commonDescriptionList,addTIFF,useBigTIFF,addPyramid);
								}
								else {
									for (int stripNumber=0; stripNumber<numberOfStrips; ++stripNumber) {
										String outputFileName = outputFilePrefix + "_" + dirNum + "_" + stripNumber + ".dcm";
										slf4jlogger.info("outputFileName={}",outputFileName);
										int instanceNumber = (dirNum+1)*100000+(stripNumber+1);
										convertTIFFTilesToDicomSingleFrame(jsonfile,ifds.getFile(),outputFileName,instanceNumber,imageWidth,imageLength,stripOffsets[stripNumber],stripByteCounts[stripNumber],imageWidth,rowsPerStrip,bitsPerSample,compression,jpegTables,iccProfile,photometric,samplesPerPixel,planarConfig,sampleFormat,
													modality,sopClass,transferSyntax,commonDescriptionList,addTIFF,useBigTIFF);
									}
								}
							}
						}
					}
					else {
						throw new TIFFException("Unsupported encoding");
					}
				}
			}
			catch (Exception e) {
				slf4jlogger.error("Failed to construct DICOM image: ",e);
			}
			++dirNum;
		}
	}
	
	/**
	 * <p>Read a TIFF image input format file consisting of one or more pages or tiles, and create one or more images of a specified or appropriate SOP Class.</p>
	 *
	 * <p>Options are:</p>
	 * <p>ADDTIFF | DONOTADDTIFF (default)</p>
	 * <p>USEBIGTIFF (default) | DONOTUSEBIGTIFF</p>
	 * <p>ADDPYRAMID | DONOTADDPYRAMID (default)</p>
	 * <p>MERGESTRIPS (default) | DONOTMERGESTRIPS</p>
	 *
	 * @param	arg	three, four or five parameters plus options, a JSON file describing the functional groups and attributes and values to be added or replaced, the TIFF inputFile, DICOM file outputFilePrefix, and optionally the modality, the SOP Class, and the Transfer Syntax to use, then various options controlling conversion
	 */
	public static void main(String arg[]) {
		try {
			boolean addTIFF = false;
			boolean useBigTIFF = true;
			boolean addPyramid = false;
			boolean mergeStrips = true;

			int numberOfFixedArguments = 3;
			int numberOfFixedAndOptionalArguments = 6;
			int endOptionsPosition = arg.length;
			boolean bad = false;
			
			if (endOptionsPosition < numberOfFixedArguments) {
				bad = true;
			}
			boolean keepLooking = true;
			while (keepLooking && endOptionsPosition > numberOfFixedArguments) {
				String option = arg[endOptionsPosition-1].trim().toUpperCase();
				switch (option) {
					case "ADDTIFF":				addTIFF = true; --endOptionsPosition; break;
					case "DONOTADDTIFF":		addTIFF = false; --endOptionsPosition; break;
					
					case "USEBIGTIFF":			useBigTIFF = true; --endOptionsPosition; break;
					case "DONOTUSEBIGTIFF":		useBigTIFF = false; --endOptionsPosition; break;

					case "ADDPYRAMID":			addPyramid = true; --endOptionsPosition; break;
					case "DONOTADDPYRAMID":		addPyramid = false; --endOptionsPosition; break;

					case "MERGESTRIPS":			mergeStrips = true; --endOptionsPosition; break;
					case "DONOTMERGESTRIPS":	mergeStrips = false; --endOptionsPosition; break;

					default:	if (endOptionsPosition > numberOfFixedAndOptionalArguments) {
									slf4jlogger.error("Unrecognized argument {}",option);
									bad = true;
								}
								keepLooking = false;
								break;
				}
			}
			
			if (!bad) {
				String jsonfile = arg[0];
				String inputFile = arg[1];
				String outputFilePrefix = arg[2];
				String modality = null;
				String sopClass = null;
				String transferSyntax = null;

				if (endOptionsPosition >= 4) {
					modality = arg[3];
				}
				if (endOptionsPosition >= 5) {
					sopClass = arg[4];
				}
				if (endOptionsPosition >= 6) {
					transferSyntax = arg[5];
				}
				
				new TIFFToDicom(jsonfile,inputFile,outputFilePrefix,modality,sopClass,transferSyntax,addTIFF,useBigTIFF,addPyramid,mergeStrips);
			}
			else {
				System.err.println("Error: Incorrect number of arguments or bad arguments");
				System.err.println("Usage: TIFFToDicom jsonfile inputFile outputFilePrefix [modality [SOPClass [TransferSyntax]]]"
					+" [ADDTIFF|DONOTADDTIFF]"
					+" [USEBIGTIFF|DONOTUSEBIGTIFF]"
					+" [ADDPYRAMID|DONOTADDPYRAMID]"
					+" [MERGESTRIPS|SPLITSTRIPS]"
				);
				System.exit(1);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}

