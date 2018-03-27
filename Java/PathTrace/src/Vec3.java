
class Vec3
{
	public float x, y, z;
	
	public Vec3()
	{x = 0; y = 0; z = 0;}
	public Vec3(float xx)
	{x = xx; y = xx; z = xx;}
	public Vec3(float xx, float yy, float zz)
	{x = xx; y = yy; z = zz;}
	
	public float dot(Vec3 v)
	{
		return x*v.x + y*v.y + z*v.z;
	}
	public Vec3 cross(Vec3 v)
	{
		return new Vec3(y*v.z - z*v.y, z*v.x - x*v.z, x*v.y - y*v.x);
	}
	public float lengthSq()
	{
		return x*x + y*y + z*z;
	}
	public float length()
	{
		return (float)Math.sqrt(lengthSq());
	}
	public Vec3 normalize()
	{
		float magSq = lengthSq();

		if (magSq > 0)
		{
			x *= 1/(float)Math.sqrt(magSq);
			y *= 1/(float)Math.sqrt(magSq);
			z *= 1/(float)Math.sqrt(magSq);
		}

		return this;
	}
	public float findmax()
	{
		return x > y && x > z ? x : y > z ? y : z;
	}
	
	public Vec3 multiply(float f){return new Vec3(x*f, y*f, z*f);}
	public Vec3 multiply(Vec3 v){return new Vec3(x*v.x, y*v.y, z*v.z);}
	public Vec3 subtract(Vec3 v){return new Vec3(x - v.x, y - v.y, z - v.z);}
	public Vec3 add(Vec3 v){return new Vec3(x + v.x, y + v.y, z + v.z);}
	public Vec3 invert(){return new Vec3(-x, -y, -z);}
}
