package pathtrace.utility;

public class Vec3
{
	public float X, Y, Z;
	
	public Vec3()
	{
		X = 0;
		Y = 0;
		Z = 0;
	}
	
	public Vec3(float v)
	{
		X = v;
		Y = v;
		Z = v;
	}
	
	public Vec3(float x, float y, float z)
	{
		X = x;
		Y = y;
		Z = z;
	}
	
	public float dot(Vec3 v)
	{
		return X*v.X + Y*v.Y + Z*v.Z;
	}
	
	public Vec3 cross(Vec3 v)
	{
		return new Vec3(Y*v.Z - Z*v.Y, Z*v.X - X*v.Z, X*v.Y - Y*v.X);
	}
	
	public float lengthSquared()
	{
		return X*X + Y*Y + Z*Z;
	}
	
	public float length()
	{
		return (float)Math.sqrt(lengthSquared());
	}
	
	public Vec3 normalize()
	{
		float lengthSquared = lengthSquared();

		if (lengthSquared > 0)
		{
			float invLength = 1/(float)Math.sqrt(lengthSquared);
			
			X *= invLength;
			Y *= invLength;
			Z *= invLength;
		}

		return this;
	}
	
	public Vec3 reflect(Vec3 normal)
	{
		return this.subtract(normal.multiply(2).multiply(this.dot(normal)));
	}
	
	public Vec3 refract(float eta, Vec3 normal, float cosI, float k)
	{
		return this.multiply(eta).add(normal.multiply(eta*cosI - (float)Math.sqrt(k)));
	}
	
	public float findMax()
	{
		return X > Y && X > Z ? X : Y > Z ? Y : Z;
	}
	
	public static Vec3 randomHemisphereDirection(Vec3 normal)
	{
		float r1 = 2*(float)Math.PI*(float)Math.random();
		float r2 = (float)Math.random();
		float r2s = (float)Math.sqrt(r2);
		
		Vec3 w = normal;
		Vec3 u = ((Math.abs(w.X) > 0.1f ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0)).cross(w)).normalize();
		Vec3 v = w.cross(u);
		
		return (u.multiply((float)Math.cos(r1)).multiply(r2s).add(v.multiply((float)Math.sin(r1)).multiply(r2s)).add(w.multiply((float)Math.sqrt(1 - r2)))).normalize();
	}
	
	public static Vec3 randomConeDirection(Vec3 r, float theta)
	{
		float r1 = 2*(float)Math.PI*(float)Math.random();
		float r2 = (float)Math.sin(theta)*(float)Math.sin(theta)*(float)Math.random();
		float r2s = (float)Math.sqrt(r2);
		
		Vec3 w = r;
		Vec3 u = ((Math.abs(w.X) > 0.1f ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0)).cross(w)).normalize();
		Vec3 v = w.cross(u);
		
		return (u.multiply((float)Math.cos(r1)).multiply(r2s).add(v.multiply((float)Math.sin(r1)).multiply(r2s)).add(w.multiply((float)Math.sqrt(1 - r2)))).normalize();
	}
	
	public Vec3 multiply(float f){ return new Vec3(X*f, Y*f, Z*f); }
	public Vec3 multiply(Vec3 v){ return new Vec3(X*v.X, Y*v.Y, Z*v.Z); }
	public Vec3 subtract(Vec3 v){ return new Vec3(X - v.X, Y - v.Y, Z - v.Z); }
	public Vec3 add(Vec3 v){ return new Vec3(X + v.X, Y + v.Y, Z + v.Z); }
	public Vec3 invert(){ return new Vec3(-X, -Y, -Z); }
}
