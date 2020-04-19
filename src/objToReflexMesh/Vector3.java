package objToReflexMesh;

public class Vector3 {
	public float x = 0.0F;
	public float y = 0.0F;
	public float z = 0.0F;
	
	private final int size = 3;
	
	public Vector3() {
		
	}
	
	public Vector3(float[] array) {
		if(array.length != size) {
			throw new IllegalArgumentException();
		}
		
		this.x = array[0];
		this.y = array[1];
		this.z = array[2];
	}
	
	public Vector3(float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public float[] getFloats() {
		return new float[] {x,y,z};
	}
	
	public byte[] getBytes() {
		return Helper.concat(Helper.floatToByteArray(x), Helper.floatToByteArray(y), Helper.floatToByteArray(z));
	}
	
	public static Vector3 scalarMultiply(Vector3 a, float s) {
		return new Vector3(a.x * s, a.y * s, a.z * s);
	}
	
	public static Vector3 scalarDivide(Vector3 a, float s) {
		return new Vector3(a.x / s, a.y / s, a.z / s);
	}
	
	public static Vector3 add(Vector3 a, Vector3 b) {
		return new Vector3(a.x+b.x, a.y+b.y, a.z+b.z);
	}
	
	public static Vector3 subtract(Vector3 a, Vector3 b) {
		return new Vector3(a.x-b.x, a.y-b.y, a.z-b.z);
	}
	
	public static float dot(Vector3 a, Vector3 b) {
		return a.x*b.x + a.y*b.y + a.z*b.z;
	}
	
	public static Vector3 cross(Vector3 a, Vector3 b) {
		return new Vector3(a.y*b.z - a.z*b.y, a.z*b.x - a.x*b.z, a.x*b.y - a.y*b.x);
	}
	
	public static Vector3 normalize(Vector3 a) {
		float len = (float) Math.sqrt((a.x * a.x) + (a.y * a.y) + (a.z * a.z));
		return scalarDivide(a, len);
	}
}
