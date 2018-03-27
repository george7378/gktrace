/////////////////////////////////////////
//Data structures, associated functions//
/////////////////////////////////////////
//Material
struct Material
{
	float3 surfaceColour, emitColour;
	float ior, glossAngle;
	uint surfaceType; //1 = DIFF, 2 = SPEC, 3 = GLSP, 4 = REFR, 5 = GLRF, 6 = LGHT
};
Material Material_new(float3 sc, float3 ec, float ir, float ga, uint st)
{
	Material newMaterial = (Material)0;
	
	newMaterial.surfaceColour = sc;
	newMaterial.emitColour = ec;
	newMaterial.ior = ir;
	newMaterial.glossAngle = ga; 
	newMaterial.surfaceType = st;
	
	return newMaterial;
}

//Intersect
struct Intersect
{
	bool intersection;
	float dist;
};
Intersect Intersect_new(bool i, float d)
{
	Intersect newIntersect = (Intersect)0;
	
	newIntersect.intersection = i;
	newIntersect.dist = d;
	
	return newIntersect;
}

//Sphere
struct Sphere
{
	Material material;
	float3 centre;
	float radius, radiusSq;
};
Sphere Sphere_new(Material mat, float3 c, float r)
{
	Sphere newSphere = (Sphere)0;
	
	newSphere.material = mat;
	newSphere.centre = c;
	newSphere.radius = r;
	newSphere.radiusSq = r*r;
	
	return newSphere;
}
float3 Sphere_getnormal(Sphere sph, float3 p)
{
	return p - sph.centre;
}
float3 Sphere_getsurfacecolour(Sphere sph, float3 p)
{
	return sph.material.surfaceColour;
}
Intersect Sphere_getintersect(Sphere sph, float3 rayorig, float3 raydir)
{
	float3 l = sph.centre - rayorig;

	//If the ray travels away from the centre of the sphere, return false
	float tca = dot(l, raydir);
	if (tca < 0){return Intersect_new(false, 0);}

	//If the ray goes towards the sphere but misses, return false
	float d2 = dot(l, l) - tca*tca;
	if (d2 > sph.radiusSq){return Intersect_new(false, 0);}

	//Else, find the valid intersection distance and return true
	float thc = sqrt(sph.radiusSq - d2);
	float d = tca - thc;
	if (d < 0){d = tca + thc;}

	return Intersect_new(true, d);
}

//Plane
struct Plane
{
	Material material;
	float3 normal, pnt;
	float texscalex, texscaley;
};
Plane Plane_new(Material mat, float3 n, float3 p, float tx, float ty)
{
	Plane newPlane = (Plane)0;
	
	newPlane.material = mat;
	newPlane.normal = normalize(n);
	newPlane.pnt = p;
	newPlane.texscalex = tx;
	newPlane.texscaley = ty;
	
	return newPlane;
}
float3 Plane_getnormal(Plane pln)
{
	return pln.normal;
}
float3 Plane_getsurfacecolour(Plane pln, float3 p)
{
	return pln.material.surfaceColour;
}
Intersect Plane_getintersect(Plane pln, float3 rayorig, float3 raydir)
{
	float denom = dot(pln.normal, raydir);
	if (denom > 1e-6)
	{
		float3 origintopoint = pln.pnt - rayorig;
		float d = dot(origintopoint, pln.normal)/denom;
		if (d >= 0){return Intersect_new(true, d);}
	}

	return Intersect_new(false, 0);
}

//Camera
struct Camera
{
	float3 pos, look;
	float3 up, left;
	float fovy;
};
Camera Camera_new(float3 p, float3 l, float3 u, float fy)
{
	Camera newCamera = (Camera)0;
	
	newCamera.pos = p;
	float3 left_temp = cross(u, l);
	u = cross(l, left_temp);
	newCamera.look = normalize(l);
	newCamera.up = normalize(u);
	newCamera.left = normalize(left_temp);
	newCamera.fovy = fy;
	
	return newCamera;
}

//////////////////////////////////
//Global functions and variables//
//////////////////////////////////
const static float INFINITY = 1e8;
const static float PI = 3.14159265358979323846f;
float RandomModulator;

float random(float2 uv)
{
	return frac(sin(dot(uv, float2(12.9898, 78.233)))*43758.5453);
}
float3 randomHemisphereDirection(float3 n, float2 seed, inout int c)
{
	float r1 = 2*PI*random(seed*c); c += 1;
	float r2 = random(seed*c); c += 1;
	float r2s = sqrt(r2);
	float3 w = n;
	float3 u = normalize(cross(abs(w.x) > 0.1f ? float3(0, 1, 0) : float3(1, 0, 0), w));
	float3 v = cross(w, u);
	
	return normalize(u*cos(r1)*r2s + v*sin(r1)*r2s + w*sqrt(1 - r2));
}
float3 randomConeDirection(float3 r, float theta, float2 seed, inout int c)
{
	float r1 = 2*PI*random(seed*c); c += 1;
	float r2 = sin(theta)*sin(theta)*random(seed*c); c += 1;
	float r2s = sqrt(r2);
	float3 w = r;
	float3 u = normalize(cross(abs(w.x) > 0.1f ? float3(0, 1, 0) : float3(1, 0, 0), w));
	float3 v = cross(w, u);
	
	return normalize(u*cos(r1)*r2s + v*sin(r1)*r2s + w*sqrt(1 - r2));
}

/////////////////////
//Scene definitions//
/////////////////////
uint scene_Width, scene_Height;
const static float3 scene_BackgroundColour = float3(0, 0, 0);
const static float scene_Cutoff = 4e-3, scene_Bias = 1e-3;

float3 scene_CamPos, scene_CamLook;
static Camera scene_MainCamera = Camera_new(scene_CamPos, scene_CamLook, float3(0, 1, 0), 50);
static float fovAngle = tan(PI*0.5f*scene_MainCamera.fovy/180);

const static uint N_SPHERES = 3;
static Sphere scene_Spheres[N_SPHERES] =
{
	Sphere_new(Material_new(float3(0.8f, 0.8f, 0.8f), float3(0, 0, 0), 1, 0, 2), float3(1, 1, -1), 1),
	Sphere_new(Material_new(float3(0.9f, 0.9f, 0.9f), float3(0, 0, 0), 1.8f, 0, 4), float3(-1, 1, 1), 1),
	Sphere_new(Material_new(float3(0.75f, 0.75f, 0.75f), float3(0, 0, 0), 1, 0, 1), float3(2, 2, 2), 2),
};

const static uint N_PLANES = 6;
static Plane scene_Planes[N_PLANES] =
{
	Plane_new(Material_new(float3(0.75f, 0.25f, 0), float3(0, 0, 0), 1, 0, 1), float3(0, -1, 0), float3(0, 0, 0), 2.5f, 2.5f),
	Plane_new(Material_new(float3(0.25f, 0.75f, 0.25f), float3(0, 0, 0), 1, 0, 1), float3(0, 1, 0), float3(0, 5, 0), 1, 1),
	Plane_new(Material_new(float3(0, 0, 0), float3(3, 3, 3), 1, 0, 6), float3(-1, 0, 0), float3(-5, 0, 0), 1, 1),
	Plane_new(Material_new(float3(0.75f, 0.25f, 0.25f), float3(0, 0, 0), 1, 0, 1), float3(1, 0, 0), float3(5, 0, 0), 1, 1),
	Plane_new(Material_new(float3(0.25f, 0.25f, 0.75f), float3(0, 0, 0), 1, 0, 1), float3(0, 0, -1), float3(0, 0, -5), 1, 1),
	Plane_new(Material_new(float3(0.5f, 0.5f, 0.5f), float3(0, 0, 0), 1, 0, 2), float3(0, 0, 1), float3(0, 0, 5), 1, 1),
};

//////////////////
//Trace function//
//////////////////
float3 trace(float3 rayorig, float3 raydir, float2 seed)
{
	float3 rayColour = float3(1, 1, 1);
	int randCounter = 3; //Used 1, 2 already in main PS function
	
	[unroll(5)]
	while (max(max(rayColour.x, rayColour.y), rayColour.z) > scene_Cutoff)
	{
		float nearestShapeDistance = INFINITY;
		uint nearestShapeIndex = 0;
		uint nearestShapeType = 0; //0 = nothing, 1 = Sphere, 2 = Plane
		Material nearestShapeMaterial = (Material)0;
		
		//STEP 1: Look for intersections with shapes in the scene, find distance to nearest shape
		for (uint i = 0; i < N_SPHERES; i++) 
		{
			Intersect curIntersect = Sphere_getintersect(scene_Spheres[i], rayorig, raydir);
			if (curIntersect.intersection)
			{
				if (curIntersect.dist < nearestShapeDistance)
				{
					nearestShapeDistance = curIntersect.dist;
					nearestShapeIndex = i;
					nearestShapeType = 1;
					nearestShapeMaterial = scene_Spheres[i].material;
				}
			}
		}
		for (uint j = 0; j < N_PLANES; j++) 
		{
			Intersect curIntersect = Plane_getintersect(scene_Planes[j], rayorig, raydir);
			if (curIntersect.intersection)
			{
				if (curIntersect.dist < nearestShapeDistance) 
				{
					nearestShapeDistance = curIntersect.dist;
					nearestShapeIndex = j;
					nearestShapeType = 2;
					nearestShapeMaterial = scene_Planes[j].material;
				}
			}
		}
	
		//STEP 2: If there's no intersection return background colour, if light return emission
		if (nearestShapeType == 0)
		{
			return scene_BackgroundColour;
		}
	
		if (nearestShapeMaterial.surfaceType == 6)
		{
			rayColour *= nearestShapeMaterial.emitColour;
			break;
		}
	
		//STEP 3: If there was an intersection, calculate the hit point, normal, colour
		float3 phit = rayorig + raydir*nearestShapeDistance;
		float3 chit = nearestShapeType == 1 ? Sphere_getsurfacecolour(scene_Spheres[nearestShapeIndex], phit) : Plane_getsurfacecolour(scene_Planes[nearestShapeIndex], phit);
		rayColour *= chit;

		float3 nhit = nearestShapeType == 1 ? Sphere_getnormal(scene_Spheres[nearestShapeIndex], phit) : Plane_getnormal(scene_Planes[nearestShapeIndex]);
		normalize(nhit);
		bool inside = false;
		if (dot(raydir, nhit) > 0){nhit = -nhit; inside = true;}

		//STEP 4: Calculate a new direction for the ray based on surface type
		if (nearestShapeMaterial.surfaceType == 1)
		{
			rayorig = phit + nhit*scene_Bias;
			raydir = randomHemisphereDirection(nhit, seed, randCounter);
			continue;
		}

		if (nearestShapeMaterial.surfaceType == 2)
		{
			rayorig = phit + nhit*scene_Bias;
			raydir = normalize(reflect(raydir, nhit));
			continue;
		}

		if (nearestShapeMaterial.surfaceType == 3)
		{
			rayorig = phit + nhit*scene_Bias;
			raydir = randomConeDirection(normalize(reflect(raydir, nhit)), nearestShapeMaterial.glossAngle, seed, randCounter);
			continue;
		}

		if (nearestShapeMaterial.surfaceType == 4)
		{
			float n = inside ? nearestShapeMaterial.ior : 1/nearestShapeMaterial.ior;
			rayorig = phit - nhit*scene_Bias;
			raydir = normalize(refract(raydir, nhit, n));
			continue;
		}	

		if (nearestShapeMaterial.surfaceType == 5)
		{
			float n = inside ? nearestShapeMaterial.ior : 1/nearestShapeMaterial.ior;
			rayorig = phit - nhit*scene_Bias;
			raydir = randomConeDirection(normalize(refract(raydir, nhit, n)), nearestShapeMaterial.glossAngle, seed, randCounter);
			continue;
		}
	}
		
	return rayColour;
}

////////////////
//Shader stuff//
////////////////
Texture PrevImage, ScreenTexture;
uint NumberOfIterations;

sampler PrevImageSampler = sampler_state
{
	texture = <PrevImage>;
	magfilter = POINT;
	minfilter = POINT; 
	mipfilter = POINT; 
	AddressU = Mirror; 
	AddressV = Mirror;
};

sampler ScreenTextureSampler = sampler_state
{
	texture = <ScreenTexture>;
	magfilter = POINT;
	minfilter = POINT;
	mipfilter = POINT;
	AddressU = Mirror;
	AddressV = Mirror;
};

struct PixelColourOut
{
	float4 Colour : COLOR0;
};

struct ScreenQuadVertexToPixel
{
    float4 Position	   	 : POSITION;
    float2 TexCoords     : TEXCOORD0;
};

ScreenQuadVertexToPixel ScreenQuadVertexShader(float4 inPos : POSITION, float2 inTexCoords : TEXCOORD0)
{
    ScreenQuadVertexToPixel Output = (ScreenQuadVertexToPixel)0;

    Output.Position = inPos;
    Output.TexCoords = inTexCoords;

    return Output;
}

PixelColourOut PathTracePixelShader(ScreenQuadVertexToPixel PSIn)
{
	PixelColourOut Output = (PixelColourOut)0;
	
	float2 curSeed = PSIn.TexCoords*RandomModulator;
	float xx = (2*((PSIn.TexCoords.x*scene_Width + random(curSeed))/scene_Width) - 1)*fovAngle*scene_Width/scene_Height;
	float yy = (1 - 2*((PSIn.TexCoords.y*scene_Height + random(curSeed*2))/scene_Height))*fovAngle;
	
	float3 newSample = trace(scene_MainCamera.pos, normalize(scene_MainCamera.left*xx + scene_MainCamera.up*yy + scene_MainCamera.look), curSeed);

    Output.Colour = float4((newSample + NumberOfIterations*tex2D(PrevImageSampler, PSIn.TexCoords).xyz)/(NumberOfIterations + 1), 1);
	
    return Output;
}

PixelColourOut ScreenPixelShader(ScreenQuadVertexToPixel PSIn)
{
	PixelColourOut Output = (PixelColourOut)0;
	
    Output.Colour = tex2D(ScreenTextureSampler, PSIn.TexCoords);
    
    return Output;
}

technique None
{
    pass Pass0
    {
    	VertexShader = compile vs_3_0 ScreenQuadVertexShader();
        PixelShader = compile ps_3_0 ScreenPixelShader();
    }
}

technique PathTrace
{
    pass Pass0
    {
    	VertexShader = compile vs_3_0 ScreenQuadVertexShader();
        PixelShader = compile ps_3_0 PathTracePixelShader();
    }
}
