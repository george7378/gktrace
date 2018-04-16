package pathtrace.utility;

import pathtrace.geometry.enums.SurfaceType;

public class Material
{
	public Vec3 SurfaceColour, EmissionColour;
	public float RefractiveIndex, GlossAngle;
	public SurfaceType Type;
	
	public Material()
	{
		SurfaceColour = new Vec3();
		EmissionColour = new Vec3();
		RefractiveIndex = 1;
		GlossAngle = 0;
		Type = SurfaceType.DIFF;
	}
	
	public Material(Vec3 surfaceColour, Vec3 emissionColour, float refractiveIndex, float glossAngle, SurfaceType type)
	{
		SurfaceColour = surfaceColour;
		EmissionColour = emissionColour;
		RefractiveIndex = refractiveIndex;
		GlossAngle = glossAngle;
		Type = type;
	}
}
