package pathtrace.geometry;

import pathtrace.utility.Intersect;
import pathtrace.utility.Material;
import pathtrace.utility.Vec3;

public abstract class Shape
{
	public final Material Material;
	
	public Shape(Material material)
	{
		Material = material;
	}
	
	public abstract Vec3 calculateNormal(Vec3 point);
	
	public abstract Vec3 calculateSurfaceColour(Vec3 point);
	
	public abstract Intersect calculateIntersect(Vec3 rayOrigin, Vec3 rayDirection);
}
