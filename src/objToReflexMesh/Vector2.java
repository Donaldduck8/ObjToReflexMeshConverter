package objToReflexMesh;

public class Vector2 {
	public float x = 0.0F;
	public float y = 0.0F;
	
	private final int size = 2;
	
	public Vector2() {
		
	}
	
	public Vector2(float[] array) {
		if(array.length != size) {
			throw new IllegalArgumentException();
		}
		
		this.x = array[0];
		this.y = array[1];
	}
	
	public Vector2(float x, float y) {
		this.x = x;
		this.y = y;
	}
	
	public float[] getFloats() {
		return new float[] {x,y};
	}
	
	public byte[] getBytes() {
		return Helper.concat(Helper.floatToByteArray(x), Helper.floatToByteArray(y));
	}
}
