package objToReflexMesh;

public class Vector4 {
	public float x = 0.0F;
	public float y = 0.0F;
	public float z = 0.0F;
	public float w = 0.0F;
	
	private final int size = 4;
	
	public Vector4() {
		
	}
	
	public Vector4(float[] array) {
		if(array.length != size) {
			throw new IllegalArgumentException();
		}
		
		this.x = array[0];
		this.y = array[1];
		this.z = array[2];
		this.w = array[3];
	}
	
	public Vector4(float x, float y, float z, float w) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.w = w;
	}
	
	public float[] getFloats() {
		return new float[] {x,y};
	}
	
	public byte[] getBytes() {
		return Helper.concat(Helper.floatToByteArray(x), Helper.floatToByteArray(y), Helper.floatToByteArray(z), Helper.floatToByteArray(w));
	}
}
