package pathtrace.geometry;

import pathtrace.utility.Intersect;
import pathtrace.utility.Material;
import pathtrace.utility.Vec3;

public class Plane extends Shape	
{
	private final Vec3 _normal, _point;

	public Plane(Material material, Vec3 normal, Vec3 point)
	{
		super(material);
		
		_normal = normal.normalize();
		_point = point;
	}

	public Vec3 calculateNormal(Vec3 point)
	{
		return _normal;
	}
	
	public Vec3 calculateSurfaceColour(Vec3 point)
	{
		return Material.SurfaceColour;
	}
	
	public Intersect calculateIntersect(Vec3 rayOrigin, Vec3 rayDirection)
	{
		float denom = _normal.dot(rayDirection);
		if (denom > 1e-6)
		{
			Vec3 originToPoint = _point.subtract(rayOrigin);
			
			float d = originToPoint.dot(_normal)/denom;
			if (d >= 0)
			{
				return new Intersect(true, d);
			}
		}

		return new Intersect();
	}
}
