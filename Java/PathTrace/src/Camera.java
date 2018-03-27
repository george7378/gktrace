
class RenderProperties
{
	public int width, height, numsamples;
	public Vec3 background;
	public float cutoff, bias;

	public RenderProperties()
	{width = 640; height = 480; numsamples = 1; background = new Vec3(); cutoff = (float)4e-3; bias = (float)1e-3;}
	public RenderProperties(int w, int h, int n, Vec3 bg, float c, float b)
	{width = w; height = h; numsamples = n; background = bg; cutoff = c; bias = b;}
}

class Camera
{
	public Vec3 pos, look;
	public Vec3 up, left;
	public float fovy;

	public Camera()
	{pos = new Vec3(0, 1, 0); look = new Vec3(0, 0, 1); up = new Vec3(0, 1, 0); left = up.cross(look); left.normalize(); fovy = 40;}
	public Camera(Vec3 p, Vec3 l, Vec3 u, float fy)
	{pos = p; look = l; up = u; left = up.cross(look); up = look.cross(left); look.normalize(); up.normalize(); left.normalize(); fovy = fy;}
}
