package pathtrace.geometry;

import pathtrace.utility.Intersect;
import pathtrace.utility.Vec3;

public class Triangle
{
	private final Vec3 _v0, _v0V1, _v0V2, _normal;
	
	public Triangle(Vec3 v0, Vec3 v1, Vec3 v2, Vec3 normal)
	{
		_v0 = v0;
		_v0V1 = v1.subtract(_v0);
		_v0V2 = v2.subtract(_v0);
		 _normal = normal.normalize();
	}

	public Vec3 calculateNormal(Vec3 point)
	{
		return _normal;
	}
	
	public Intersect calculateIntersect(Vec3 rayOrigin, Vec3 rayDirection)
	{
	    Vec3 pVec = rayDirection.cross(_v0V2);
	    float det = _v0V1.dot(pVec);
	    if (Math.abs(det) < 1e-6)
	    {
	    	return new Intersect();	    	
	    }
	    
	    float invDet = 1/det;
	    Vec3 tVec = rayOrigin.subtract(_v0);
	    float u = tVec.dot(pVec)*invDet;
	    if (u < 0 || u > 1)
	    {
	    	return new Intersect();	    	
	    }
	    
	    Vec3 qVec = tVec.cross(_v0V1);
	    float v = rayDirection.dot(qVec)*invDet;
	    if (v < 0 || u + v > 1)
	    {
	    	return new Intersect();	    	
	    }
	    
	    float d = _v0V2.dot(qVec)*invDet;
	    return d >= 0 ? new Intersect(true, d) : new Intersect();
	}
}
