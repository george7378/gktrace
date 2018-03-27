import java.util.ArrayList;

enum SurfaceType
{
	DIFF, SPEC, GLSP, REFR, GLRF, LGHT
}

class Material
{
	public Vec3 surfaceColour, emitColour;
	public float ior, glossAngle;
	public SurfaceType st;
	
	public Material()
	{surfaceColour = new Vec3(); emitColour = new Vec3(); ior = 1; glossAngle = 0; st = SurfaceType.DIFF;}
	public Material(Vec3 sc, Vec3 ec, float ir, float ga, SurfaceType su)
	{surfaceColour = sc; emitColour = ec; ior = ir; glossAngle = ga; st = su;}
}

class Intersect
{
	public boolean intersection;
	public float distance;
	
	public Intersect()
	{intersection = false; distance = 0;}
	public Intersect(boolean i, float d)
	{intersection = i; distance = d;}
}

abstract class Shape
{
	public Material material;
	
	public Shape(Material mat)
	{material = mat;}
	
	public abstract Vec3 getNormal(Vec3 p);
	public abstract Vec3 getSurfaceColour(Vec3 p);
	public abstract Intersect getIntersect(Vec3 rayorig, Vec3 raydir);
}
class Sphere extends Shape
{
	private Vec3 centre;
	private float radius, radiusSq;
	
	public Sphere(Vec3 c, float r, Material mat)
	{super(mat); centre = c; radius = r; radiusSq = r*r;}
	
	public Vec3 getNormal(Vec3 p)
	{
		return p.subtract(centre);
	}
	public Vec3 getSurfaceColour(Vec3 p)
	{
		return material.surfaceColour;
	}
	public Intersect getIntersect(Vec3 rayorig, Vec3 raydir)
	{
		Vec3 l = centre.subtract(rayorig);

		//If the ray travels away from the centre of the sphere, return false
		float tca = l.dot(raydir);
		if (tca < 0){return new Intersect();}

		//If the ray goes towards the sphere but misses, return false
		float d2 = l.dot(l) - tca*tca;
		if (d2 > radiusSq){return new Intersect();}

		//Else, find the valid intersection distance and return true
		float thc = (float)Math.sqrt(radiusSq - d2);
		float d = tca - thc;
		if (d < 0){d = tca + thc;}

		return new Intersect(true, d);
	}
}
class Plane extends Shape	
{
	private Vec3 normal, point;
	private float texscalex, texscaley;
	
	public Plane(Vec3 n, Vec3 p, Material mat, float tx, float ty)
	{super(mat); normal = n; normal.normalize(); point = p; texscalex = tx; texscaley = ty;}

	public Vec3 getNormal(Vec3 p)
	{
		return normal;
	}
	public Vec3 getSurfaceColour(Vec3 p)
	{
		return material.surfaceColour;
	}
	public Intersect getIntersect(Vec3 rayorig, Vec3 raydir)
	{
		float denom = normal.dot(raydir);
		if (denom > 1e-6)
		{
			Vec3 origintopoint = point.subtract(rayorig);
			float d = origintopoint.dot(normal)/denom;
			if (d >= 0){return new Intersect(true, d);}
		}

		return new Intersect();
	}
}

class AABB
{
	private Vec3 boundariesMin, boundariesMax;
	private static final float normalDelta = 0.001f;
	
	public AABB(Vec3 bmin, Vec3 bmax)
	{boundariesMin = bmin; boundariesMax = bmax;}

	public Vec3 getNormal(Vec3 p)
	{
		if (Math.abs(p.x - boundariesMin.x) <= normalDelta)
		{
			return new Vec3(-1, 0, 0);
		}
		else if (Math.abs(p.x - boundariesMax.x) <= normalDelta)
		{
			return new Vec3(1, 0, 0);
		}
		else if (Math.abs(p.y - boundariesMin.y) <= normalDelta)
		{
			return new Vec3(0, -1, 0);
		}
		else if (Math.abs(p.y - boundariesMax.y) <= normalDelta)
		{
			return new Vec3(0, 1, 0);
		}
		else if (Math.abs(p.z - boundariesMin.z) <= normalDelta)
		{
			return new Vec3(0, 0, -1);
		}
		return new Vec3(0, 0, 1);
	}
	public Intersect getIntersect(Vec3 rayorig, Vec3 raydir)
	{
		float tmin = (boundariesMin.x - rayorig.x)/raydir.x;
		float tmax = (boundariesMax.x - rayorig.x)/raydir.x;
		if (tmin > tmax)
		{
			float ttemp = tmin;
			tmin = tmax;
			tmax = ttemp;
		}
		
		float tymin = (boundariesMin.y - rayorig.y)/raydir.y;
		float tymax = (boundariesMax.y - rayorig.y)/raydir.y;
		if (tymin > tymax)
		{
			float ttemp = tymin;
			tymin = tymax;
			tymax = ttemp;
		}
		
		if (tmin > tymax || tymin > tmax)
	        return new Intersect();
		
	    if (tymin > tmin)
	        tmin = tymin;
	    
	    if (tymax < tmax)
	        tmax = tymax;
	    
	    float tzmin = (boundariesMin.z - rayorig.z)/raydir.z;
	    float tzmax = (boundariesMax.z - rayorig.z)/raydir.z;
	    
	    if (tzmin > tzmax)
	    {
			float ttemp = tzmin;
			tzmin = tzmax;
			tzmax = ttemp;
		}
	 
	    if (tmin > tzmax || tzmin > tmax)
	        return new Intersect();
	 
	    if (tzmin > tmin)
	        tmin = tzmin;
	 
	    if (tzmax < tmax)
	        tmax = tzmax;
	    
	    float t = tmin > 0 ? tmin : tmax;
        if (t < 0)
        	return new Intersect();
	    
	    return new Intersect(true, t);
	}
}
class Triangle
{
	private Vec3 v0, v1, v2, normal;
	private Vec3 v0v1, v0v2;
	
	public Triangle(Vec3 vert0, Vec3 vert1, Vec3 vert2, Vec3 n)
	{v0 = vert0; v1 = vert1; v2 = vert2; normal = n; normal.normalize();
	 v0v1 = v1.subtract(v0); v0v2 = v2.subtract(v0);}

	public Vec3 getNormal(Vec3 p)
	{
		return normal;
	}
	public Intersect getIntersect(Vec3 rayorig, Vec3 raydir)
	{
	    Vec3 pvec = raydir.cross(v0v2);
	    float det = v0v1.dot(pvec);
	    if (Math.abs(det) < 1e-6)
	    	return new Intersect();
	    
	    float invdet = 1/det;
	    Vec3 tvec = rayorig.subtract(v0);
	    float u = tvec.dot(pvec)*invdet;
	    if (u < 0 || u > 1)
	    	return new Intersect();
	    
	    Vec3 qvec = tvec.cross(v0v1);
	    float v = raydir.dot(qvec)*invdet;
	    if (v < 0 || u + v > 1)
	    	return new Intersect();
	    
	    float d = v0v2.dot(qvec)*invdet;
	    return d >= 0 ? new Intersect(true, d) : new Intersect();
	}
}
class Mesh extends Shape
{
	private AABB boundingBox;
	private ArrayList <Triangle> triangles;
	private Vec3 lastIntersectNormal;
	
	public Mesh(AABB bound, ArrayList <Triangle> tri, Material mat)
	{super(mat); boundingBox = bound; triangles = tri; lastIntersectNormal = new Vec3(1);}

	public Vec3 getNormal(Vec3 p)
	{
		return lastIntersectNormal;
	}
	public Vec3 getSurfaceColour(Vec3 p)
	{
		return material.surfaceColour;
	}
	public Intersect getIntersect(Vec3 rayorig, Vec3 raydir)
	{
		Intersect boundIntersect = boundingBox.getIntersect(rayorig, raydir);
		if (boundIntersect.intersection)
		{
			float dnear = Global.INFINITY;
			Intersect resultIntersect = new Intersect();
			
			for (Triangle tri : triangles)
			{
				Intersect triIntersect = tri.getIntersect(rayorig, raydir);
				if (triIntersect.intersection)
				{
					if (triIntersect.distance < dnear) 
					{
						dnear = triIntersect.distance;
						resultIntersect.intersection = true;
						resultIntersect.distance = dnear;
						lastIntersectNormal = tri.getNormal(raydir);
					}
				}
			}
			
			return resultIntersect;
		}
		
		return new Intersect();
	}
}
