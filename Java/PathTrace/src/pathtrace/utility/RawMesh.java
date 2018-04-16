package pathtrace.utility;

import java.util.ArrayList;

public class RawMesh
{
	public final ArrayList<String> Vertices, Indices, Normals, Boundaries;
	
	public RawMesh()
	{
		Vertices = new ArrayList<String>();
		Indices = new ArrayList<String>();
		Normals = new ArrayList<String>();
		Boundaries = new ArrayList<String>();
	}
	
	public RawMesh(ArrayList<String> vertices, ArrayList<String> indices, ArrayList<String> normals, ArrayList<String> boundaries)
	{
		Vertices = vertices;
		Indices = indices;
		Normals = normals;
		Boundaries = boundaries;
	}
}
