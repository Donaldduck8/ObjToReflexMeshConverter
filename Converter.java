package objToReflexMesh;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Converter {
	public static void main(String[] args) {
		System.out.println(".obj to .mesh converter 1.0 by Donald");
		System.out.println("=====================================");
		System.out.println();
		
		try {
			if(args.length != 2 || args[0] == null || args[1] == null || args[0].length() == 0 || args[1].length() == 0) {
				errorOut("You must provide at least two arguments.\r\n" +
			"Example (file conversion):   'ObjToReflexMeshConverter.exe \"C:\\Objects\\container.obj\" \"C:\\Meshes\\container.mesh\"'\r\n" +
			"Example (folder conversion): 'ObjToReflexMeshConverter.exe \"C:\\Objects\" \"C:\\Meshes\"'");
			}
			
			File input = new File(args[0]);
			if(input.isDirectory()) {
				//Folder conversion
				
				if(!input.exists()) {
					errorOut("Cannot access input directory. Make sure the specified path is correct and the directory is not protected, hidden or on another drive.");
				}
				
				File outputDirectory = new File(args[1]);
				outputDirectory.mkdirs();
				
				if(!outputDirectory.exists()) {
					errorOut("Cannot create output directory.");
				}
				
				for(final File f : input.listFiles()) {
					if(f.isFile() && f.canRead() && f.getAbsolutePath().endsWith(".obj")) {
						System.out.println(f.getName());
						List<String> lines = Files.readAllLines(f.toPath());
						byte[] rfxMesh = convertObjToRfxMesh2(lines);
						if(rfxMesh != null) {
							try (FileOutputStream fos = new FileOutputStream(outputDirectory.getAbsolutePath() + File.separator + f.getName().replaceFirst("[.][^.]+$", "") + ".mesh")) {
								   fos.write(rfxMesh);
							}
						} else {
							System.out.println("Skipping " + f.getName());
						}
					}
				}
			} else {
				//File conversion
				if(!input.exists() || !input.isFile() || !input.canRead()) {
					errorOut("Cannot access input file. Make sure the specified path is correct and the file is readable.");
				}
				
				if(!input.getAbsolutePath().endsWith(".obj")) {
					errorOut("The specified file is not a .obj file.");
				}
				
				File outputFile = new File(args[1]);
				File outputFileDirectory = new File(outputFile.getParent());
				outputFileDirectory.mkdirs();
				
				if(!outputFileDirectory.exists()) {
					errorOut("Cannot create output directory.");
				}
				
				System.out.println(input.getName());
				List<String> lines = Files.readAllLines(input.toPath());
				byte[] rfxMesh = convertObjToRfxMesh2(lines);
				if(rfxMesh != null) {
					try (FileOutputStream fos = new FileOutputStream(outputFile.getAbsolutePath())) {
						   fos.write(rfxMesh);
					}
				}
			}

		} catch(IOException e) {
			e.printStackTrace();
			errorOut("An IOException occured.");
		}
	}

	public static byte[] convertObjToRfxMesh2(List<String> lines) {
		int gCount = 1;
		int vCount = 0;
		int vnCount = 0;
		int vtCount = 0;
		int fCount = 0;
		
		int gIndex = 0;
		ArrayList<Integer> gSizesArrayList = new ArrayList<Integer>();
		
		//Clean up lines, triangulate faces
		System.out.println("Step 1: Triangulating faces");
		lines = prepare(lines);
		
		//Iterate through all lines and get all count information, as well as group size information
		System.out.println("Step 2: Gathering number of groups, vertices and faces");
		for(int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);
			if(line != null && line.length() > 0) {
				if(line.startsWith("g ")) { //Group
					if(fCount > 0) {
						gCount++;
						gSizesArrayList.add(gIndex);
						gIndex = 0;
					}
				} else if(line.startsWith("v ")) { //3D vertex
					vCount++;
				} else if(line.startsWith("vn ")) { //Vertex normal
					vnCount++;
				} else if(line.startsWith("vt ")) { //Vertex texture coordinates
					vtCount++;
				} else if(line.startsWith("f ")) { //Face
					fCount++;
					gIndex++;
				}
			}
			
			if(i == lines.size() - 1) {
				gSizesArrayList.add(gIndex);
			}
		}
		
		//Unwrap group sizes
		Object[] gSizesWrapper = (Object[]) gSizesArrayList.toArray();
		int[] gSizes = new int[gSizesWrapper.length];
		for(int i = 0; i < gSizes.length; i++) {
			gSizes[i] = (int) gSizesWrapper[i];
		}

		//Check that limits haven't already been exceeded
		if(vCount > 65536 || vnCount > 65536 || vtCount > 65536) {
			System.out.println("Too many vertices! Reflex can only handle 65536 vertex combinations per submesh.");
			return null;
		}
		
		if(gCount > 8) {
			System.out.println("Too many groups! Reflex can only have 8 custom materials in one mesh.");
			return null;
		}
		
		//Collect 3d vertices, vertex normals and vertex texture coordinates
		System.out.println("Step 3: Parsing and storing vertex floats");
		int vIndex = 0;
		int vnIndex = 0;
		int vtIndex = 0;
		
		float[][] vertices = new float[vCount][3];
		float[][] vertexNormals = new float[vnCount][3];
		float[][] vertexTextureCoordinates = new float[vtCount][];
		float[] meshSpace_min = new float[] {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE};
		float[] meshSpace_max = new float[] {Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE};
		
		try {
			for(int i = 0; i < lines.size(); i++) {
				String line = lines.get(i);
				
				//Parse and store floats
				if(line.startsWith("v ")) {
					String[] floats = line.split(" ");
					
					if(floats.length != 4) {
						System.out.println("Found invalid 3D vertex: " + line);
						return null;
					}
					
					vertices[vIndex][0] = Float.parseFloat(floats[1]);
					vertices[vIndex][1] = Float.parseFloat(floats[2]);
					vertices[vIndex][2] = Float.parseFloat(floats[3]);
					
					//Keep track of mesh space
					meshSpace_min[0] = Math.min(meshSpace_min[0], vertices[vIndex][0]);
					meshSpace_min[1] = Math.min(meshSpace_min[1], vertices[vIndex][1]);
					meshSpace_min[2] = Math.min(meshSpace_min[2], vertices[vIndex][2]);
					
					meshSpace_max[0] = Math.max(meshSpace_max[0], vertices[vIndex][0]);
					meshSpace_max[1] = Math.max(meshSpace_max[1], vertices[vIndex][1]);
					meshSpace_max[2] = Math.max(meshSpace_max[2], vertices[vIndex][2]);
					
					vIndex++;
				} else if(line.startsWith("vn ")) {
					String[] floats = line.split(" ");
					
					if(floats.length != 4) {
						System.out.println("Found invalid vertex normal: " + line);
						return null;
					}
					
					vertexNormals[vnIndex][0] = Float.parseFloat(floats[1]);
					vertexNormals[vnIndex][1] = Float.parseFloat(floats[2]);
					vertexNormals[vnIndex][2] = Float.parseFloat(floats[3]);
					vnIndex++;
				} else if(line.startsWith("vt ")) {
					String[] floats = line.split(" ");
					
					if(floats.length < 3 || floats.length > 4) {
						System.out.println("Found invalid vertex texture coordinates: " + line);
						return null;
					}
					
					vertexTextureCoordinates[vtIndex] = new float[floats.length - 1];
					for(int j = 0; j < floats.length - 1; j++) {
						vertexTextureCoordinates[vtIndex][j] = Float.parseFloat(floats[j+1]);
					}
					vtIndex++;
				}
			}
		} catch(NumberFormatException e) {
			e.printStackTrace();
			System.out.println("Found invalid float.");
			return null;
		} catch(Exception e) {
			e.printStackTrace();
			System.out.println("An error occured while extracting vertex information. One of your vertex lines is likely malformed.");
			return null;
		}
		
		//Iterate over all faces and store all vertex combinations as well as all faces
		int[][] faces = new int[fCount][3];
		int faceIndex = 0;
		
		//Store all vertex combinations in a list, this way every combination is implicitly mapped to an index
		//This will be necessary to create the face table later on
		System.out.println("Step 4: Setting up index permutation for vertex combinations");
		LinkedList<String> vertexCombinations = new LinkedList<String>();
		for(int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);
			if(line.startsWith("f ")) {
				String[] faceVertexCombinations = line.split(" ");
				faces[faceIndex] = new int[faceVertexCombinations.length - 1];
				
				//Iterate over vertex combinations in face (keep in mind faceVertexCombinations[0] is the prefix "f")
				for(int j = 1; j < faceVertexCombinations.length; j++) {
					String faceVertexCombination = faceVertexCombinations[j];
					
					//Add to list if the list doesn't already contain it
					if(!vertexCombinations.contains(faceVertexCombination)) {
						vertexCombinations.add(faceVertexCombination);
					}
					
					//Update face table
					faces[faceIndex][j-1] = (int) vertexCombinations.indexOf(faceVertexCombination);
				}
				faceIndex++;
			}
		}
		
		//Check that limits haven't been exceeded
		if(vertexCombinations.size() > 65536) {
			System.out.println("Too many vertex combinations! Reflex can only handle 65536 vertex combinations per submesh.");
			return null;
		}
		
		//Create combined vertices
		float[][] combinedVertices = new float[vertexCombinations.size()][12];
		
		//Iterate through the list of vertexCombinations and store the combined vertex floats in combinedVertices
		System.out.println("Step 5: Parsing and storing vertex combinations");
		for(int i = 0; i < vertexCombinations.size(); i++) {
			String faceVertexCombination = vertexCombinations.get(i);
			if(faceVertexCombination.contains("//")) { 							// v/vn
				String[] vertexCluster = faceVertexCombination.split("//");
				
				float[] vertexFloats = vertices[Integer.parseInt(vertexCluster[0]) - 1];
				float[] vertexNormalFloats = vertexNormals[Integer.parseInt(vertexCluster[1]) - 1];
				float[] vertexTextureCoordinateFloats = new float[3];
				float[] unknownFloat3 = new float[3];
				
				combinedVertices[i] = concat(vertexFloats, vertexNormalFloats, vertexTextureCoordinateFloats, unknownFloat3);
				
			} else if(faceVertexCombination.contains("/")){						// v/vt or v/vt/vn
				String[] vertexCluster = faceVertexCombination.split("/");
				if(vertexCluster.length == 2) {									// v/vt
					float[] vertexFloats = vertices[Integer.parseInt(vertexCluster[0]) - 1];
					float[] vertexNormalFloats = new float[3];
					float[] vertexTextureCoordinateFloats = vertexTextureCoordinates[Integer.parseInt(vertexCluster[1]) - 1];
					float[] unknownFloat3 = new float[3];
					
					combinedVertices[i] = concat(vertexFloats, vertexNormalFloats, vertexTextureCoordinateFloats, unknownFloat3);
					
				} else {														// v/vt/vn
					float[] vertexFloats = vertices[Integer.parseInt(vertexCluster[0]) - 1];
					float[] vertexNormalFloats = vertexNormals[Integer.parseInt(vertexCluster[2]) - 1];
					float[] vertexTextureCoordinateFloats = vertexTextureCoordinates[Integer.parseInt(vertexCluster[1]) - 1];
					float[] unknownFloat3 = new float[3];
					
					combinedVertices[i] = concat(vertexFloats, vertexNormalFloats, vertexTextureCoordinateFloats, unknownFloat3);
				}
			} else {															// v
				float[] vertexFloats = vertices[Integer.parseInt(faceVertexCombination) - 1];
				float[] vertexNormalFloats = new float[3];
				float[] vertexTextureCoordinateFloats = new float[3];
				float[] unknownFloat3 = new float[3];
				
				combinedVertices[i] = concat(vertexFloats, vertexNormalFloats, vertexTextureCoordinateFloats, unknownFloat3);
			}
		}
		
		byte[] rfxmesh = writeRfxMeshFile2(combinedVertices, faces, meshSpace_min, meshSpace_max, gCount, gSizes);
		
		return rfxmesh;
	}
	
	public static byte[] writeRfxMeshFile2(float[][] vertices, int[][] faces, float[] minCoords, float[] maxCoords, int gCount, int[] gSizes) {
		//Initialization
		int RENDERING_VERTEX_BYTELENGTH = 52;
		int PROJECTION_VERTEX_BYTELENGTH = 12;
		byte[][] renderingVerticesBytes = new byte[vertices.length][RENDERING_VERTEX_BYTELENGTH];
		byte[][] projectionVerticesBytes = new byte[vertices.length][PROJECTION_VERTEX_BYTELENGTH];

		//Set up headers
		System.out.println("Step 6: Setting up mesh headers");
		byte[] meshInfoHeader_1 = hexStringToByteArray("23000AD0");
		byte[] numGroups = intToByteArray(gCount);
		byte[] meshSpace_min = floatsToByteArrayFlat(minCoords);
		byte[] meshSpace_max = floatsToByteArrayFlat(maxCoords);
		byte[] meshInfoHeader_2 = hexStringToByteArray("000000000000000009000000");
		byte[] materialNames = new byte[10 * gCount];
		for(int i = 0; i < gCount; i++) {
			byte[] materialName = hexStringToByteArray("4D6174657269616C4100");
			materialName[8] += i; //Iterate from A-Z. Since the maximum group count is 26, we will never cause the byte to overflow
			System.arraycopy(materialName, 0, materialNames, 10 * i, 10);
		}
		
		//Set up all vertices
		System.out.println("Step 7: Assembling vertex bytes");
		for(int i = 0; i < vertices.length; i++) {
			byte[] verticesFloatBytes = floatsToByteArrayFlat(vertices[i]);
			System.arraycopy(verticesFloatBytes, 0, renderingVerticesBytes[i], 0, 12);
			renderingVerticesBytes[i][12] = hexStringToByte("FF");
			renderingVerticesBytes[i][13] = hexStringToByte("FF");
			renderingVerticesBytes[i][14] = hexStringToByte("FF");
			renderingVerticesBytes[i][15] = hexStringToByte("FF");
			System.arraycopy(verticesFloatBytes, 12, renderingVerticesBytes[i], 16, 36);
			
			float[] vertexCoordFloats = new float[3];
			System.arraycopy(vertices[i], 0, vertexCoordFloats, 0, 3);
			projectionVerticesBytes[i] = floatsToByteArrayFlat(vertexCoordFloats);
		}
		byte[] projectionVerticesBytesFlat = unravel(projectionVerticesBytes);

		//Set up group loop
		int fIndex = 0;
		byte[][] groupBytesAll = new byte[gCount][];
		
		//Create byte array for each group / submesh
		System.out.println("Step 8: Assembling optimized vertex table and face table for each submesh");
		for(int i = 0; i < gCount; i++) {
			//Fetch faces for this group
			int[][] groupFaces = new int[gSizes[i]][];
			System.arraycopy(faces, fIndex, groupFaces, 0, gSizes[i]);
			fIndex += gSizes[i];
			
			//Get all unique vertex references from group's faces and add them to a list
			//This will implicitly give each referenced vertex combination an optimized index
			LinkedList<Integer> groupReferencedVertexCombinations = new LinkedList<Integer>();
			for(int j = 0; j < groupFaces.length; j++) {
				for(int k = 0; k < groupFaces[j].length; k++) {
					if(!groupReferencedVertexCombinations.contains(groupFaces[j][k])) {
						groupReferencedVertexCombinations.add(groupFaces[j][k]);
					}
				}
			}
			
			//Update group face table to reflect this change
			for(int j = 0; j < groupFaces.length; j++) {
				for(int k = 0; k < groupFaces[j].length; k++) {
					groupFaces[j][k] = groupReferencedVertexCombinations.indexOf(groupFaces[j][k]);
				}
			}
			
			int[] groupFacesFlat = unravel(groupFaces);
			byte[] groupFacesBytesFlat = intsTo2ByteArraysFlat(groupFacesFlat);
			
			//Create a new vertex byte array using this information
			byte[][] groupRenderingBytes = new byte[groupReferencedVertexCombinations.size()][];
			for(int j = 0; j < groupReferencedVertexCombinations.size(); j++) {
				groupRenderingBytes[j] = renderingVerticesBytes[groupReferencedVertexCombinations.get(j)];
			}	
			byte[] groupRenderingBytesFlat = unravel(groupRenderingBytes);
			
			//Set up group rendering header
			byte[] numVertices = intToByteArray(groupRenderingBytes.length);
			byte[] numFaceVertices = intToByteArray(gSizes[i] * 3);
			byte[] mysteriousBitmask_1 = hexStringToByteArray("1D000000");
			byte[] mysteriousUInt32_1 = hexStringToByteArray("04000000");
			byte[] groupRenderingInfoHeader = new byte[16];
			System.arraycopy(numVertices, 0, groupRenderingInfoHeader, 0, 4);
			System.arraycopy(numFaceVertices, 0, groupRenderingInfoHeader, 4, 4);
			System.arraycopy(mysteriousBitmask_1, 0, groupRenderingInfoHeader, 8, 4);
			System.arraycopy(mysteriousUInt32_1, 0, groupRenderingInfoHeader, 12, 4);
			
			//Initialize group projection header
			byte[] groupProjectionInfoHeader = new byte[16];
			
			if(i == 0) {
				//Set up group projection header
				//Group 0 contains all projection vertices, all other groups leave this empty
				int[] projectionFacesFlat = unravel(faces);
				byte[] projectionFacesBytesFlat = intsTo2ByteArraysFlat(projectionFacesFlat);
				byte[] numProjectionVertices = intToByteArray(projectionVerticesBytes.length);
				byte[] numProjectionFaceVertices = intToByteArray(projectionFacesFlat.length);
				System.arraycopy(numProjectionVertices, 0, groupProjectionInfoHeader, 0, 4);
				System.arraycopy(numProjectionFaceVertices, 0, groupProjectionInfoHeader, 4, 4);
				System.arraycopy(mysteriousBitmask_1, 0, groupProjectionInfoHeader, 8, 4);
				System.arraycopy(mysteriousUInt32_1, 0, groupProjectionInfoHeader, 12, 4);
				
				//Designate the following vertex and face tables to be for shadow-casting and click detection with mysteriousBitmask_1
				//mysteriousBitmask_1 is set to 00 00 00 00
				groupProjectionInfoHeader[8] = hexStringToByte("00");
				
				//Assemble
				byte[] groupBytes = concat(groupRenderingInfoHeader, groupRenderingBytesFlat, groupFacesBytesFlat, groupProjectionInfoHeader, projectionVerticesBytesFlat, projectionFacesBytesFlat);
				groupBytesAll[i] = groupBytes;
			} else {
				//All groups except for group 0 contain no projection vertices, so the header can be largely blank
				//mysteriousUInt32_1 must still be 04 00 00 00
				groupProjectionInfoHeader[12] = hexStringToByte("04");
				
				//Assemble group
				byte[] groupBytes = concat(groupRenderingInfoHeader, groupRenderingBytesFlat, groupFacesBytesFlat, groupProjectionInfoHeader);
				groupBytesAll[i] = groupBytes;
			}
		}
		
		//Assemble mesh
		System.out.println("Step 9: Assembling mesh file");
		byte[] groupBytesAllFlat = unravel(groupBytesAll);
		byte[] meshFile = concat(meshInfoHeader_1, numGroups, meshSpace_min, meshSpace_max, meshInfoHeader_2, materialNames, groupBytesAllFlat);
		
		System.out.println("Success!");
		
		return meshFile;
	}
	
	public static List<String> prepare(List<String> lines) {
		for(int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);
			
			
			//If lines contains double spaces, replace them
			while(line.contains("  ")) {
				line = line.replaceAll("  ", " ");
			}
			
			//If face and more than 3 vertex combinations, triangulate
			if(line.startsWith("f ")) {
				String[] faceVertexCombinations = line.split(" ");
				if(faceVertexCombinations.length > 4) { //Keep in mind faceVertexCombinations[0] is "f"
					//Triangulate
					lines.remove(i);
					
					for(int j = 1; j < faceVertexCombinations.length - 2; j++) {
						String newTriFace = "f " + faceVertexCombinations[1] + " " + faceVertexCombinations[1+j] + " " + faceVertexCombinations[2+j];
						lines.add(i, newTriFace);
					}
				}
			}
		}
		
		return lines;
	}

	public static String byteArrayToHexString(byte[] b) {
		StringBuilder sb = new StringBuilder();
		for(byte aa : b) {
			sb.append(String.format("%02X", aa));
		}
		String ret = sb.toString();
		sb.setLength(0);
		return ret;
	}

	public static String byteToBinaryString(byte b) {
		StringBuilder sb = new StringBuilder();
		for (int i = 7; i >= 0; --i) {
			sb.append(b >>> i & 1);
		}
		String ret = sb.toString();
		sb.setLength(0);
		return ret;
	}

	public static byte[] hexStringToByteArray(String s) {
		int len = s.length();
		byte[] ret = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			ret[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
					+ Character.digit(s.charAt(i+1), 16));
		}
		return ret;
	}

	public static byte hexStringToByte(String s) {
		if(s.length() != 2) {
			throw new IllegalArgumentException();
		}
		byte ret = (byte) Character.digit(s.charAt(0), 16);
		ret = (byte) (ret << 4);
		ret = (byte) ((byte) ret + Character.digit(s.charAt(1), 16));
		return ret;
	}

	public static byte[] floatToByteArray (float value) {
		ByteBuffer buffer = ByteBuffer.allocate(4);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		buffer.putFloat(value);
		return buffer.array();
	}
	
	public static byte[] floatsToByteArrayFlat (float[] value) {
		byte[][] bAC = new byte[value.length][];
		for(int i = 0; i < value.length; i++) {
			bAC[i] = floatToByteArray(value[i]);
		}
		return concat(bAC);
	}

	public static byte[] longToByteArray (long value) {
		ByteBuffer buffer = ByteBuffer.allocate(8);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		buffer.putLong(value);
		return buffer.array();
	}

	public static byte[] intToByteArray (int value) {
		ByteBuffer buffer = ByteBuffer.allocate(4);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		buffer.putInt(value);
		byte[] result = buffer.array();
		//Rotate array
		//Get index of first non-zero byte in array
		int index = 0;
		for(int i = 0; i < result.length; i++) {
			if(result[i] != 0) {
				index = i;
				break;
			}
		}
		
		//Rotate byte array left by that index
		byte[] resultRotated = new byte[result.length];
		for(int i = 0; i < result.length; i++) {
			resultRotated[i] = result[(i + index) % result.length];
		}
		
		return resultRotated;
	}
	
	public static byte[] intTo2ByteArray (int value) {
		ByteBuffer buffer = ByteBuffer.allocate(4);
		buffer.order(ByteOrder.BIG_ENDIAN);
		buffer.putInt(value);
		byte[] result4Bytes = buffer.array();
		byte[] result = new byte[2];
		result[0] = result4Bytes[3];
		result[1] = result4Bytes[2];
		
		return result;
	}
	
	public static byte[] intsTo2ByteArraysFlat (int[] value) {
		byte[][] bAC = new byte[value.length][];
		for(int i = 0; i < value.length; i++) {
			bAC[i] = intTo2ByteArray(value[i]);
		}
		return concat(bAC);
	}
	
	public static byte[] shortToByteArray (short value) {
		ByteBuffer buffer = ByteBuffer.allocate(2);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		buffer.putShort(value);
		return buffer.array();
	}
	
	public static byte[] shortsToByteArrayFlat (short[] value) {
		byte[][] bAC = new byte[value.length][];
		for(int i = 0; i < value.length; i++) {
			bAC[i] = shortToByteArray(value[i]);
		}
		return concat(bAC);
	}
	
	public static byte[] concat(byte[]... arrays) {
		int length = 0;
		for (byte[] array : arrays) {
			length += array.length;
		}
		byte[] result = new byte[length];
		int pos = 0;
		for (byte[] array : arrays) {
			System.arraycopy(array, 0, result, pos, array.length);
			pos += array.length;
		}
		return result;
	}
	
	public static float[] concat(float[]... arrays) {
		int length = 0;
		for (float[] array : arrays) {
			length += array.length;
		}
		float[] result = new float[length];
		int pos = 0;
		for (float[] array : arrays) {
			System.arraycopy(array, 0, result, pos, array.length);
			pos += array.length;
		}
		return result;
	}
	
	public static byte[] unravel(byte[][] bytes) {
		ArrayList<Byte> ret = new ArrayList<Byte>();
		
		for(int i = 0; i < bytes.length; i++) {
			for(int j = 0; j < bytes[i].length; j++) {
				ret.add(bytes[i][j]);
			}
		}
		
		Object[] object_array_wrapper = ret.toArray();
		
		Byte[] ret2 = new Byte[object_array_wrapper.length];
		System.arraycopy(object_array_wrapper, 0, ret2, 0, object_array_wrapper.length);
		byte[] ret3 = new byte[ret2.length];
		
		int i = 0;
		for(Byte b : ret2) {
			ret3[i++] = Byte.valueOf(b);
		}
		
		return ret3;
	}
	
	public static short[] unravel(short[][] shorts) {
		ArrayList<Short> ret = new ArrayList<Short>();
		
		for(int i = 0; i < shorts.length; i++) {
			for(int j = 0; j < shorts[i].length; j++) {
				ret.add(shorts[i][j]);
			}
		}
		
		Object[] object_array_wrapper = ret.toArray();
		
		Short[] ret2 = new Short[object_array_wrapper.length];
		System.arraycopy(object_array_wrapper, 0, ret2, 0, object_array_wrapper.length);
		short[] ret3 = new short[ret2.length];
		
		int i = 0;
		for(Short b : ret2) {
			ret3[i++] = Short.valueOf(b);
		}
		
		return ret3;
	}
	
	public static int[] unravel(int[][] ints) {
		ArrayList<Integer> ret = new ArrayList<Integer>();
		
		for(int i = 0; i < ints.length; i++) {
			for(int j = 0; j < ints[i].length; j++) {
				ret.add(ints[i][j]);
			}
		}
		
		Object[] object_array_wrapper = ret.toArray();
		
		Integer[] ret2 = new Integer[object_array_wrapper.length];
		System.arraycopy(object_array_wrapper, 0, ret2, 0, object_array_wrapper.length);
		int[] ret3 = new int[ret2.length];
		
		int i = 0;
		for(Integer b : ret2) {
			ret3[i++] = Integer.valueOf(b);
		}
		
		return ret3;
	}
	
	public static void errorOut(String errMessage) {
		try {
			System.out.println(errMessage);
			Thread.sleep(250);
			System.exit(1);
		} catch (InterruptedException e) {
			System.exit(1);
		}
	}
}