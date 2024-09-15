package pathtrace.geometry;

import java.util.ArrayList;

import pathtrace.utility.Intersect;
import pathtrace.utility.Material;
import pathtrace.utility.Vec3;

public class Mesh extends Shape
{
	private final AxisAlignedBoundingBox _boundingBox;
	private final ArrayList<Triangle> _triangles;
	private Vec3 _lastIntersectNormal;
	
	public Mesh(Material material, AxisAlignedBoundingBox boundingBox, ArrayList<Triangle> triangles)
	{
		super(material);
		
		_boundingBox = boundingBox;
		_triangles = triangles;
		_lastIntersectNormal = new Vec3(1);
	}

	public Vec3 calculateNormal(Vec3 point)
	{
		return _lastIntersectNormal;
	}
	
	public Vec3 calculateSurfaceColour(Vec3 point)
	{
		return Material.SurfaceColour;
	}
	
	public Intersect calculateIntersect(Vec3 rayOrigin, Vec3 rayDirection)
	{
		Intersect boundingBoxIntersect = _boundingBox.calculateIntersect(rayOrigin, rayDirection);
		if (boundingBoxIntersect.Intersection)
		{
			float dNear = Float.MAX_VALUE;
			Intersect resultIntersect = new Intersect();
			
			for (Triangle triangle : _triangles)
			{
				Intersect triangleIntersect = triangle.calculateIntersect(rayOrigin, rayDirection);
				if (triangleIntersect.Intersection && triangleIntersect.Distance < dNear)
				{
					dNear = triangleIntersect.Distance;
					
					resultIntersect.Intersection = true;
					resultIntersect.Distance = dNear;
					
					_lastIntersectNormal = triangle.calculateNormal(rayDirection);
				}
			}
			
			return resultIntersect;
		}
		
		return new Intersect();
	}
}
