package pathtrace.geometry;

import pathtrace.utility.Intersect;
import pathtrace.utility.Vec3;

public class AxisAlignedBoundingBox
{
	private static final float NormalDelta = 0.001f;
	
	private final Vec3 _boundariesMin, _boundariesMax;
		
	public AxisAlignedBoundingBox(Vec3 boundariesMin, Vec3 boundariesMax)
	{
		_boundariesMin = boundariesMin;
		_boundariesMax = boundariesMax;
	}

	public Vec3 calculateNormal(Vec3 point)
	{
		if (Math.abs(point.X - _boundariesMin.X) <= NormalDelta)
		{
			return new Vec3(-1, 0, 0);
		}
		else if (Math.abs(point.X - _boundariesMax.X) <= NormalDelta)
		{
			return new Vec3(1, 0, 0);
		}
		else if (Math.abs(point.Y - _boundariesMin.Y) <= NormalDelta)
		{
			return new Vec3(0, -1, 0);
		}
		else if (Math.abs(point.Y - _boundariesMax.Y) <= NormalDelta)
		{
			return new Vec3(0, 1, 0);
		}
		else if (Math.abs(point.Z - _boundariesMin.Z) <= NormalDelta)
		{
			return new Vec3(0, 0, -1);
		}
		return new Vec3(0, 0, 1);
	}
	
	public Intersect calculateIntersect(Vec3 rayOrigin, Vec3 rayDirection)
	{
		float tMin = (_boundariesMin.X - rayOrigin.X)/rayDirection.X;
		float tMax = (_boundariesMax.X - rayOrigin.X)/rayDirection.X;
		if (tMin > tMax)
		{
			float tTemp = tMin;
			tMin = tMax;
			tMax = tTemp;
		}
		
		float tyMin = (_boundariesMin.Y - rayOrigin.Y)/rayDirection.Y;
		float tyMax = (_boundariesMax.Y - rayOrigin.Y)/rayDirection.Y;
		if (tyMin > tyMax)
		{
			float tTemp = tyMin;
			tyMin = tyMax;
			tyMax = tTemp;
		}
		
		if (tMin > tyMax || tyMin > tMax)
		{
			return new Intersect();
		}
		
	    if (tyMin > tMin)
	    {
	    	tMin = tyMin;	    	
	    }
	    
	    if (tyMax < tMax)
	    {
	    	tMax = tyMax;	    	
	    }
	    
	    float tzMin = (_boundariesMin.Z - rayOrigin.Z)/rayDirection.Z;
	    float tzMax = (_boundariesMax.Z - rayOrigin.Z)/rayDirection.Z;
	    if (tzMin > tzMax)
	    {
			float tTemp = tzMin;
			tzMin = tzMax;
			tzMax = tTemp;
		}
	 
	    if (tMin > tzMax || tzMin > tMax)
	    {
	    	return new Intersect();	    	
	    }
	 
	    if (tzMin > tMin)
	    {
	    	tMin = tzMin;	    	
	    }
	 
	    if (tzMax < tMax)
	    {
	    	tMax = tzMax;	    	
	    }
	    
	    float t = tMin > 0 ? tMin : tMax;
        if (t < 0)
        {
        	return new Intersect();        	
        }
	    
	    return new Intersect(true, t);
	}
}
