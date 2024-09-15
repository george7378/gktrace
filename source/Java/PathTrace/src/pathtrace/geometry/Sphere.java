package pathtrace.geometry;

import pathtrace.utility.Intersect;
import pathtrace.utility.Material;
import pathtrace.utility.Vec3;

public class Sphere extends Shape
{
	private final Vec3 _centre;
	private final float _radiusSquared;
	
	public Sphere(Material material, Vec3 centre, float radius)
	{
		super(material);
		
		_centre = centre;
		_radiusSquared = radius*radius;
	}
	
	public Vec3 calculateNormal(Vec3 point)
	{
		return point.subtract(_centre);
	}
	
	public Vec3 calculateSurfaceColour(Vec3 point)
	{
		return Material.SurfaceColour;
	}
	
	public Intersect calculateIntersect(Vec3 rayOrigin, Vec3 rayDirection)
	{
		Vec3 l = _centre.subtract(rayOrigin);

		// If the ray travels away from the centre of the sphere, return false
		float tca = l.dot(rayDirection);
		if (tca < 0)
		{
			return new Intersect();
		}

		// If the ray goes towards the sphere but misses, return false
		float d2 = l.dot(l) - tca*tca;
		if (d2 > _radiusSquared)
		{
			return new Intersect();
		}

		// Else, find the valid intersection distance and return true
		float thc = (float)Math.sqrt(_radiusSquared - d2);
		float d = tca - thc;
		if (d < 0)
		{
			d = tca + thc;
		}

		return new Intersect(true, d);
	}
}
