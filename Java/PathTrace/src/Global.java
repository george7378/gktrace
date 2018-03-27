
class Global
{
	static final float INFINITY = (float)1e8;
	
	static Vec3 reflect(Vec3 i, Vec3 n)
	{
		return i.subtract(n.multiply(2).multiply(i.dot(n)));
	}
	static Vec3 refract(float eta, Vec3 i, Vec3 n, float cosi, float k)
	{
		return i.multiply(eta).add(n.multiply(eta*cosi - (float)Math.sqrt(k)));
	}
	static Vec3 randomHemisphereDirection(Vec3 n)
	{
		float r1 = 2*(float)Math.PI*(float)Math.random(), r2 = (float)Math.random(), r2s = (float)Math.sqrt(r2);
		Vec3 w = n;
		Vec3 u = ((Math.abs(w.x) > 0.1f ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0)).cross(w)).normalize();
		Vec3 v = w.cross(u);
		return (u.multiply((float)Math.cos(r1)).multiply(r2s).add(v.multiply((float)Math.sin(r1)).multiply(r2s)).add(w.multiply((float)Math.sqrt(1 - r2)))).normalize();
	}
	static Vec3 randomConeDirection(Vec3 r, float theta)
	{
		float r1 = 2*(float)Math.PI*(float)Math.random(), r2 = (float)Math.sin(theta)*(float)Math.sin(theta)*(float)Math.random(), r2s = (float)Math.sqrt(r2);
		Vec3 w = r;
		Vec3 u = ((Math.abs(w.x) > 0.1f ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0)).cross(w)).normalize();
		Vec3 v = w.cross(u);
		return (u.multiply((float)Math.cos(r1)).multiply(r2s).add(v.multiply((float)Math.sin(r1)).multiply(r2s)).add(w.multiply((float)Math.sqrt(1 - r2)))).normalize();
	}
}
