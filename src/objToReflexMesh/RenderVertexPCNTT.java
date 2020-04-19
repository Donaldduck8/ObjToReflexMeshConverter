package objToReflexMesh;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import objToReflexMesh.Helper;

public class RenderVertexPCNTT {
	public Vector3 p = new Vector3();
	public byte[] c = Helper.hexStringToByteArray("FFFFFFFF");
	public Vector3 n = new Vector3();
	public Vector2 tx = new Vector2();
	public Vector4 t = new Vector4();
	
	public RenderVertexPCNTT() {
		
	}
	
	public float[] getFloats() {
		return Helper.concat(p.getFloats(), new float[] {ByteBuffer.wrap(c).order(ByteOrder.LITTLE_ENDIAN).getFloat()}, n.getFloats(), tx.getFloats(), t.getFloats());
	}
	
	public byte[] getBytes() {
		return Helper.concat(p.getBytes(), c, n.getBytes(), tx.getBytes(), t.getBytes());
	}
}
