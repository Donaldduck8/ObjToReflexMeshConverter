package objToReflexMesh;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import objToReflexMesh.Helper;

public class Converter {
	private static String messages = "";
	
	public static void main(String[] args) throws IOException {
		System.out.println(".obj to .mesh converter 1.1.1 by Donald");
		System.out.println("    https://github.com/Donaldduck8/");
		System.out.println("=======================================");
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
					errorOut("ERROR: Cannot access input directory. Make sure the specified path is correct and the directory is not protected, hidden or on another drive.");
				}
				
				File outputDirectory = new File(args[1]);
				outputDirectory.mkdirs();
				
				if(!outputDirectory.exists()) {
					errorOut("ERROR: Cannot create output directory.");
				}
				
				for(final File f : input.listFiles()) {
					if(f.isFile() && f.canRead() && f.getAbsolutePath().endsWith(".obj")) {
						//Reset message buffer
						messages = "";
						
						//Print file name
						System.out.println(f.getName());
						
						//Read .obj file
						List<String> lines = Files.readAllLines(f.toPath());
						
						//Convert to .mesh file
						byte[] rfxMesh = convertObjToRfxMesh(lines);
						
						//Write .mesh file, skip if errors occured
						if(rfxMesh != null) {
							try (FileOutputStream fos = new FileOutputStream(outputDirectory.getAbsolutePath() + File.separator + f.getName().replaceFirst("[.][^.]+$", "") + ".mesh")) {
								   fos.write(rfxMesh);
							}
						} else {
							System.out.println("Skipping " + f.getName());
							System.out.println();
						}
					}
				}
			} else {
				//File conversion
				if(!input.exists() || !input.isFile() || !input.canRead()) {
					errorOut("ERROR: Cannot access input file. Make sure the specified path is correct and the file is readable.");
				}
				
				if(!input.getAbsolutePath().endsWith(".obj")) {
					errorOut("ERROR: The specified file is not a .obj file.");
				}
				
				File outputFile = new File(args[1]);
				File outputFileDirectory = new File(outputFile.getParent());
				outputFileDirectory.mkdirs();
				
				if(!outputFileDirectory.exists()) {
					errorOut("ERROR: Cannot create output directory.");
				}
				
				System.out.println(input.getName());
				List<String> lines = Files.readAllLines(input.toPath());
				byte[] rfxMesh = convertObjToRfxMesh(lines);
				if(rfxMesh != null) {
					try (FileOutputStream fos = new FileOutputStream(outputFile.getAbsolutePath())) {
						   fos.write(rfxMesh);
					}
				}
			}

		} catch(IOException e) {
			e.printStackTrace();
			errorOut("ERROR: An IOException occured.");
		}
	}

	public static byte[] convertObjToRfxMesh(List<String> lines) {
		int gCount = 1;
		int vCount = 0;
		int vnCount = 0;
		int vtCount = 0;
		int fCount = 0;
		
		boolean normalsUsed = false, texCoordsUsed = false, texCoordsUVWUsed = false, incompleteFaceFound = false;
		
		int gIndex = 0;
		ArrayList<Integer> gSizesArrayList = new ArrayList<Integer>();
		
		//Clean up lines, triangulate faces
		System.out.println("Step 0: Triangulating faces");
		lines = prepare(lines);
		
		//Iterate through all lines and get all count information, as well as group size information
		System.out.println("Step 1: Gathering number of groups, vertices and faces");
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
			System.out.println("ERROR: Too many vertices! Reflex can only address 65536 vertex combinations per submesh.");
			return null;
		}
		
		if(gCount > 8) {
			System.out.println("ERROR: Too many groups! Reflex only supports 8 groups per mesh.");
			return null;
		}
		
		//Give warnings to user if the mesh is missing information
		if(vnCount == 0) {
			messages += "WARNING: Mesh does not contain vertex normals. Lighting and textures will not be supported.\r\n";
		}
		
		if(vtCount == 0) {
			messages += "WARNING: Mesh does not contain texture coordinates. Textures will not be supported.\r\n";
		}
		
		//Collect 3d vertices, vertex normals and vertex texture coordinates
		System.out.println("Step 2: Parsing and storing vertex floats");
		int vIndex = 0;
		int vnIndex = 0;
		int vtIndex = 0;
		
		Vector3[] vertices = new Vector3[vCount];
		Vector3[] vertexNormals = new Vector3[vnCount];
		Vector2[] vertexTextureCoordinates = new Vector2[vtCount];
		Vector3 meshSpace_min = new Vector3(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
		Vector3 meshSpace_max = new Vector3(Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE);
		
		try {
			for(int i = 0; i < lines.size(); i++) {
				String line = lines.get(i);
				String[] floats = line.split(" ");
				
				//Parse and store floats
				if(line.startsWith("v ")) {
					if(floats.length != 4) {
						System.out.println("ERROR: Found invalid 3D vertex: " + line);
						return null;
					}
					
					if(vertices[vIndex] == null) 
						vertices[vIndex] = new Vector3();
					
					vertices[vIndex].x = Float.parseFloat(floats[1]);
					vertices[vIndex].y = Float.parseFloat(floats[2]);
					vertices[vIndex].z = Float.parseFloat(floats[3]);
					
					//Keep track of mesh space
					meshSpace_min.x = Math.min(meshSpace_min.x, vertices[vIndex].x);
					meshSpace_min.y = Math.min(meshSpace_min.y, vertices[vIndex].y);
					meshSpace_min.z = Math.min(meshSpace_min.z, vertices[vIndex].z);
					
					meshSpace_max.x = Math.max(meshSpace_max.x, vertices[vIndex].x);
					meshSpace_max.y = Math.max(meshSpace_max.y, vertices[vIndex].y);
					meshSpace_max.z = Math.max(meshSpace_max.z, vertices[vIndex].z);
					
					vIndex++;
				} else if(line.startsWith("vn ")) {
					if(floats.length != 4) {
						System.out.println("ERROR: Found invalid vertex normal: " + line);
						return null;
					}
					
					vertexNormals[vnIndex] = new Vector3();
					
					vertexNormals[vnIndex].x = Float.parseFloat(floats[1]);
					vertexNormals[vnIndex].y = Float.parseFloat(floats[2]);
					vertexNormals[vnIndex].z = Float.parseFloat(floats[3]);
					vnIndex++;
				} else if(line.startsWith("vt ")) {
					if(floats.length < 3) {
						System.out.println("ERROR: Found invalid vertex texture coordinates: " + line);
						return null;
					} else if(floats.length > 3) {
						texCoordsUVWUsed = true;
					}
					
					vertexTextureCoordinates[vtIndex] = new Vector2();
					
					vertexTextureCoordinates[vtIndex].x = Float.parseFloat(floats[1]);
					vertexTextureCoordinates[vtIndex].y = Float.parseFloat(floats[2]);
					vtIndex++;
				}
			}
		} catch(NumberFormatException e) {
			e.printStackTrace();
			System.out.println("ERROR: Found invalid float.");
			return null;
		} catch(Exception e) {
			e.printStackTrace();
			System.out.println("ERROR: An error occured while extracting vertex information. One of your vertex lines is likely malformed.");
			return null;
		}
		
		//Warn user that generated texture coordinates are not supported
		if(texCoordsUVWUsed) {
			messages += "WARNING: Mesh contains generated (U,V,W) texture coordinates which Reflex doesn't support. Textures may not map properly to the mesh.\r\n";
		}
		
		//Iterate over all faces and store all vertex combinations as well as all faces
		Triangle[] faces = new Triangle[fCount];
		int faceIndex = 0;
		
		//Store all vertex combinations in a list, this way every combination is implicitly mapped to an index
		//This will be necessary to create the face table later on
		System.out.println("Step 3: Setting up vertex combinations");
		LinkedList<String> vertexCombinations = new LinkedList<String>();
		for(int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);
			
			if(line.startsWith("f ")) {
				String[] faceVertexCombinations = line.split(" ");
				faces[faceIndex] = new Triangle();
				
				//Iterate over vertex combinations in face (keep in mind faceVertexCombinations[0] is the prefix "f")
				for(int j = 1; j < faceVertexCombinations.length; j++) {
					String faceVertexCombination = faceVertexCombinations[j];
					
					//Add vertex combination to the list if the list doesn't already contain it
					//Update face table
					int index = vertexCombinations.indexOf(faceVertexCombination);
					
					if(index == -1) {
						vertexCombinations.add(faceVertexCombination);
						faces[faceIndex].v[j-1] = vertexCombinations.size() - 1;
					} else {
						faces[faceIndex].v[j-1] = index;
					}
				}
				faceIndex++;
			}
		}
		
		//Check that limits haven't been exceeded
		if(vertexCombinations.size() > 65536) {
			System.out.println("ERROR: Too many vertex combinations! Reflex can only handle 65536 vertex combinations per submesh.");
			return null;
		}
		
		//Create combined vertices
		RenderVertexPCNTT[] combinedVertices = new RenderVertexPCNTT[vertexCombinations.size()];
		
		//Iterate through the list of vertexCombinations and store the combined vertex floats in combinedVertices
		System.out.println("Step 4: Parsing and storing vertex combinations");
		for(int i = 0; i < vertexCombinations.size(); i++) {
			String faceVertexCombination = vertexCombinations.get(i);
			combinedVertices[i] = new RenderVertexPCNTT();
			if(faceVertexCombination.contains("//")) { 							// v/vn
				normalsUsed = true;
				
				String[] vertexCluster = faceVertexCombination.split("//");
				combinedVertices[i].p = vertices[Integer.parseInt(vertexCluster[0]) - 1];
				combinedVertices[i].n = vertexNormals[Integer.parseInt(vertexCluster[1]) - 1];
				
				if(vtCount > 0) incompleteFaceFound = true;
				
			} else if(faceVertexCombination.contains("/")){						// v/vt or v/vt/vn
				texCoordsUsed = true;
				
				String[] vertexCluster = faceVertexCombination.split("/");
				combinedVertices[i].p = vertices[Integer.parseInt(vertexCluster[0]) - 1];
				combinedVertices[i].tx = vertexTextureCoordinates[Integer.parseInt(vertexCluster[1]) - 1];
				
				if(vertexCluster.length == 3) {									// v/vt/vn
					normalsUsed = true;
					
					combinedVertices[i].n = vertexNormals[Integer.parseInt(vertexCluster[2]) - 1];
				} else if(vnCount > 0) incompleteFaceFound = true;
			} else {															// v
				combinedVertices[i].p = vertices[Integer.parseInt(faceVertexCombination) - 1];
				if(vnCount > 0 || vtCount > 0) incompleteFaceFound = true;
			}
		}
		
		//Give warnings to user if mesh doesn't use some information
		if(!normalsUsed && vnCount > 0) {
			messages += "WARNING: Mesh does not use vertex normals. Lighting and materials may not map properly to the mesh.\r\n";
		}
		
		if(!texCoordsUsed && vtCount > 0) {
			messages += "WARNING: Mesh does not use texture coordinates. Textures will not be supported.\r\n";
		}
		
		if(incompleteFaceFound) {
			messages += "WARNING: Mesh contains incomplete faces. Lighting, materials and textures may not map properly to the mesh.\r\n";
		}
		
		//Calculate per-vertex tangents
		System.out.println("Step 5: Calculating and storing per-vertex tangents");
		Vector3[] tan1 = new Vector3[combinedVertices.length];
		Vector3[] tan2 = new Vector3[combinedVertices.length];
		Vector4[] tangent = new Vector4[combinedVertices.length];
		for(int i = 0; i < faces.length; i++) {
			Triangle tri = faces[i];
			
			Vector3 v1 = combinedVertices[tri.v[0]].p;
			Vector3 v2 = combinedVertices[tri.v[1]].p;
			Vector3 v3 = combinedVertices[tri.v[2]].p;
			
			Vector2 w1 = combinedVertices[tri.v[0]].tx;
			Vector2 w2 = combinedVertices[tri.v[1]].tx;
			Vector2 w3 = combinedVertices[tri.v[2]].tx;
			
	        float x1 = v2.x - v1.x;
	        float x2 = v3.x - v1.x;
	        float y1 = v2.y - v1.y;
	        float y2 = v3.y - v1.y;
	        float z1 = v2.z - v1.z;
	        float z2 = v3.z - v1.z;
	        
	        float s1 = w2.x - w1.x;
	        float s2 = w3.x - w1.x;
	        float t1 = w2.y - w1.y;
	        float t2 = w3.y - w1.y;
	        
	        float r = 1.0F / (s1 * t2 - s2 * t1);
	        Vector3 sdir = new Vector3((t2 * x1 - t1 * x2) * r, (t2 * y1 - t1 * y2) * r, (t2 * z1 - t1 * z2) * r);
	        Vector3 tdir = new Vector3((s1 * x2 - s2 * x1) * r, (s1 * y2 - s2 * y1) * r, (s1 * z2 - s2 * z1) * r);
	        
	        tan1[tri.v[0]] = sdir;
	        tan1[tri.v[1]] = sdir;
	        tan1[tri.v[2]] = sdir;
	        
	        tan2[tri.v[0]] = tdir;
	        tan2[tri.v[1]] = tdir;
	        tan2[tri.v[2]] = tdir;
		}
		
		for(int i = 0; i < combinedVertices.length; i++) {
			Vector3 n = combinedVertices[i].n;
			Vector3 t = tan1[i];
			
			//Gram-Schmidt orthogonalize
			//Vector3 temp = (t - n * Dot(n, t)).Normalize();
			Vector3 temp = Vector3.normalize(Vector3.subtract(t, Vector3.scalarMultiply(n, Vector3.dot(n, t))));
			tangent[i] = new Vector4(temp.x, temp.y, temp.z, 0.0F);
			
			//Calculate handedness
			tangent[i].w = (Vector3.dot(Vector3.cross(n, t), tan2[i]) < 0.0F) ? -1.0F : 1.0F;
			
			combinedVertices[i].t = tangent[i];
		}
		
		byte[] rfxmesh = assembleRfxMesh(combinedVertices, faces, meshSpace_min, meshSpace_max, gCount, gSizes);
		
		return rfxmesh;
	}
	
	public static byte[] assembleRfxMesh(RenderVertexPCNTT[] vertices, Triangle[] faces, Vector3 minCoords, Vector3 maxCoords, int gCount, int[] gSizes) {
		//Initialization
		int RENDERING_VERTEX_BYTELENGTH = 52;
		int PROJECTION_VERTEX_BYTELENGTH = 12;
		byte[][] renderingVerticesBytes = new byte[vertices.length][RENDERING_VERTEX_BYTELENGTH];
		byte[][] projectionVerticesBytes = new byte[vertices.length][PROJECTION_VERTEX_BYTELENGTH];

		//Set up headers
		System.out.println("Step 6: Setting up mesh headers");
		byte[] meshInfoHeader_1 = Helper.hexStringToByteArray("23000AD0");
		byte[] numGroups = Helper.intToByteArray(gCount);
		byte[] meshSpace_min = minCoords.getBytes();
		byte[] meshSpace_max = maxCoords.getBytes();
		byte[] meshInfoHeader_2 = Helper.hexStringToByteArray("000000000000000009000000");
		byte[] materialNames = new byte[10 * gCount];
		for(int i = 0; i < gCount; i++) {
			byte[] materialName = Helper.hexStringToByteArray("4D6174657269616C4100");
			materialName[8] += i; //Iterate from A-H. The maximum group count is 8.
			System.arraycopy(materialName, 0, materialNames, 10 * i, 10);
		}
		
		//Set up all vertices
		System.out.println("Step 7: Assembling vertex bytes");
		for(int i = 0; i < vertices.length; i++) {
			renderingVerticesBytes[i] = vertices[i].getBytes();
			projectionVerticesBytes[i] = vertices[i].p.getBytes();
		}
		byte[] projectionVerticesBytesFlat = Helper.unravel(projectionVerticesBytes);

		//Set up group loop
		int fIndex = 0;
		byte[][] groupBytesAll = new byte[gCount][];
		
		//Create byte array for each group / submesh
		System.out.println("Step 8: Assembling optimized vertex table and face table for each submesh");
		for(int i = 0; i < gCount; i++) {
			//Fetch faces for this group
			Triangle[] groupFaces = new Triangle[gSizes[i]];
			System.arraycopy(faces, fIndex, groupFaces, 0, gSizes[i]);
			fIndex += gSizes[i];
			
			//Get all unique vertex references from group's faces and add them to a list
			//This will implicitly give each referenced vertex combination an index, starting from 0
			//Update group face table to reflect this
			LinkedList<Integer> groupReferencedVertexCombinations = new LinkedList<Integer>();
			for(int j = 0; j < groupFaces.length; j++) {
				for(int k = 0; k < groupFaces[j].v.length; k++) {
					int index = groupReferencedVertexCombinations.indexOf(groupFaces[j].v[k]);
					if(index == -1) {
						groupReferencedVertexCombinations.add(groupFaces[j].v[k]);
						groupFaces[j].v[k] = groupReferencedVertexCombinations.size() - 1;
					} else {
						groupFaces[j].v[k] = index;
					}
				}
			}
			
			byte[] groupFacesBytesFlat = Helper.unravelFaces(groupFaces);
			
			//Create a new vertex byte array using this information
			byte[][] groupRenderingBytes = new byte[groupReferencedVertexCombinations.size()][];
			for(int j = 0; j < groupReferencedVertexCombinations.size(); j++) {
				groupRenderingBytes[j] = renderingVerticesBytes[groupReferencedVertexCombinations.get(j)];
			}	
			byte[] groupRenderingBytesFlat = Helper.unravel(groupRenderingBytes);
			
			//Set up group rendering header
			byte[] numVertices = Helper.intToByteArray(groupRenderingBytes.length);
			byte[] numFaceVertices = Helper.intToByteArray(gSizes[i] * 3);
			byte[] mysteriousBitmask_1 = Helper.hexStringToByteArray("1D000000");
			byte[] mysteriousUInt32_1 = Helper.hexStringToByteArray("04000000");
			byte[] groupRenderingInfoHeader = Helper.concat(numVertices, numFaceVertices, mysteriousBitmask_1, mysteriousUInt32_1);
			
			//Initialize group projection header
			byte[] groupProjectionInfoHeader = new byte[16];
			
			if(i == 0) {
				//Set up group projection header
				//Group 0 contains all projection vertices, all other groups leave this empty
				byte[] projectionFacesBytesFlat = Helper.unravelFaces(faces);
				
				byte[] numProjectionVertices = Helper.intToByteArray(projectionVerticesBytes.length);
				byte[] numProjectionFaceVertices = Helper.intToByteArray(faces.length * 3);
				groupProjectionInfoHeader = Helper.concat(numProjectionVertices, numProjectionFaceVertices, mysteriousBitmask_1, mysteriousUInt32_1);
				
				//Designate the following vertex and face tables to be for shadow-casting and click detection with mysteriousBitmask_1
				//mysteriousBitmask_1 is set to 00 00 00 00
				groupProjectionInfoHeader[8] = Helper.hexStringToByte("00");
				
				//Assemble
				byte[] groupBytes = Helper.concat(groupRenderingInfoHeader, groupRenderingBytesFlat, groupFacesBytesFlat, groupProjectionInfoHeader, projectionVerticesBytesFlat, projectionFacesBytesFlat);
				groupBytesAll[i] = groupBytes;
			} else {
				//All groups except for group 0 contain no projection vertices, so the header can be largely blank
				//mysteriousUInt32_1 must still be 04 00 00 00
				groupProjectionInfoHeader[12] = Helper.hexStringToByte("04");
				
				//Assemble group
				byte[] groupBytes = Helper.concat(groupRenderingInfoHeader, groupRenderingBytesFlat, groupFacesBytesFlat, groupProjectionInfoHeader);
				groupBytesAll[i] = groupBytes;
			}
		}
		
		//Assemble mesh
		System.out.println("Step 9: Assembling mesh file");
		byte[] groupBytesAllFlat = Helper.unravel(groupBytesAll);
		byte[] meshFile = Helper.concat(meshInfoHeader_1, numGroups, meshSpace_min, meshSpace_max, meshInfoHeader_2, materialNames, groupBytesAllFlat);
		
		//Flush any messages, reset message buffer
		System.out.print(messages);
		messages = "";
		
		System.out.println("Success!");
		System.out.println();
		
		return meshFile;
	}
	
	public static List<String> prepare(List<String> lines) {
		for(int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);
			
			
			//If lines contains double spaces, replace them
			while(line.contains("  ")) {
				line = line.replaceAll("  ", " ");
			}
			
			lines.set(i, line);
			
			//If line is face and has more than 3 vertex combinations, triangulate
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
