package objToReflexMesh;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Helper {
	public static String byteArrayToHexString(byte[] b) {
		StringBuilder sb = new StringBuilder();
		for(byte aa : b) {
			sb.append(String.format("%02X", aa));
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

	public static byte[] intToByteArray (int value) {
		ByteBuffer buffer = ByteBuffer.allocate(4);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		buffer.putInt(value);
		return buffer.array();
	}
	
	public static byte[] intTo2ByteArray (int value) {
		ByteBuffer buffer = ByteBuffer.allocate(4);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		buffer.putInt(value);
		byte[] result4Bytes = buffer.array();
		byte[] result = new byte[2];
		result[0] = result4Bytes[0];
		result[1] = result4Bytes[1];
		
		return result;
	}
	
	public static byte[] intsTo2ByteArraysFlat (int[] value) {
		byte[][] bAC = new byte[value.length][];
		for(int i = 0; i < value.length; i++) {
			bAC[i] = intTo2ByteArray(value[i]);
		}
		return concat(bAC);
	}
	
	public static byte[] concat(byte[]... arrays) {
		//Get total length
		int length = 0;
		for (byte[] array : arrays) {
			length += array.length;
		}
		
		//Initialize return array with same length
		byte[] result = new byte[length];
		int pos = 0;
		
		//Copy elements
		for (byte[] array : arrays) {
			System.arraycopy(array, 0, result, pos, array.length);
			pos += array.length;
		}
		
		return result;
	}
	
	public static float[] concat(float[]... arrays) {
		//Get total length
		int length = 0;
		for (float[] array : arrays) {
			length += array.length;
		}
		
		//Initialize return array with same length
		float[] ret = new float[length];
		int index = 0;
		
		//Copy elements
		for (float[] array : arrays) {
			System.arraycopy(array, 0, ret, index, array.length);
			index += array.length;
		}
		
		return ret;
	}
	
	public static byte[] unravel(byte[][] bytes) {
		//Get total length
		int length = 0;
		for(int i = 0; i < bytes.length; i++) {
			length += bytes[i].length;
		}
		
		//Initialize return array with same length
		byte[] ret = new byte[length];
		int index = 0;
		
		//Copy elements
		for(int i = 0; i < bytes.length; i++) {
			for(int j = 0; j < bytes[i].length; j++) {
				ret[index] = bytes[i][j];
				index++;
			}
		}
		
		return ret;
	}
	
	public static int[] unravel(int[][] ints) {
		//Get total length
		int length = 0;
		for(int i = 0; i < ints.length; i++) {
			length += ints[i].length;
		}
		
		//Initialize return array with same length
		int[] ret = new int[length];
		int index = 0;
		
		//Copy elements
		for(int i = 0; i < ints.length; i++) {
			for(int j = 0; j < ints[i].length; j++) {
				ret[index] = ints[i][j];
				index++;
			}
		}
		
		return ret;
	}
	
	public static byte[] unravelFaces(Triangle[] faces) {
		int FACE_BYTELENGTH = 6;
		byte[] ret = new byte[faces.length * FACE_BYTELENGTH];
		for(int i = 0; i < faces.length; i++) {
			byte[] face = faces[i].getBytes();
			System.arraycopy(face, 0, ret, i*FACE_BYTELENGTH, FACE_BYTELENGTH);
		}
		return ret;
	}
}
