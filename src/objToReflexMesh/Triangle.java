package objToReflexMesh;

public class Triangle {
	public int[] v = new int[3];
	
	private final int size = 3;
	
	public Triangle() {
		
	}
	
	public Triangle(int[] ints) {
		if(ints.length != size) {
			throw new IllegalArgumentException();
		}
		
		this.v = ints;
	}
	
	public Triangle(int v1, int v2, int v3) {
		v = new int[] {v1, v2, v3};
	}
	
	public byte[] getBytes() {
		return Helper.intsTo2ByteArraysFlat(v);
	}
}
