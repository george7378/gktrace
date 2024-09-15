#ifndef DEFINITIONS_H
#define DEFINITIONS_H

//Variables and objects
#define INFINITY float(1e8)
random_device rd;
default_random_engine gen(rd());
uniform_real_distribution <float> line(0, 1);

//Regular functions
float tilecoord(const float &tileratio)
{
	return tileratio - floor(tileratio);
}
void clr(unsigned l)
{
	DWORD count;
	COORD c = {0, l};
	CONSOLE_SCREEN_BUFFER_INFO csbi;
	HANDLE stdhandle = GetStdHandle(STD_OUTPUT_HANDLE);
	GetConsoleScreenBufferInfo(stdhandle, &csbi);
	FillConsoleOutputCharacter(stdhandle, ' ', csbi.dwSize.X*csbi.dwSize.Y, c, &count);
	SetConsoleCursorPosition(stdhandle, c);
} 

//Misc. classes
class Vec3
{
public:
	float x, y, z;

	//Constructors
	Vec3()
	{x = 0; y = 0; z = 0;};
	Vec3(const float &xx)
	{x = xx; y = xx; z = xx;};
	Vec3(const float &xx, const float &yy, const float &zz)
	{x = xx; y = yy; z = zz;};

	//Functions
	float dot(const Vec3 &v) const
	{
		return x*v.x + y*v.y + z*v.z;
	}
	float lengthSq() const
	{
		return x*x + y*y + z*z;
	}
	float length() const
	{
		return sqrt(lengthSq());
	}
	Vec3& normalize()
	{
		float magSq = lengthSq();

		if (magSq > 0) 
		{
			*this *= 1/sqrt(magSq);
		}

		return *this;
	}
	float findmax() const
	{
		return x > y && x > z ? x : y > z ? y : z;
	}

	//Operators
	Vec3 operator * (const float &f) const {return Vec3(x*f, y*f, z*f);}
	Vec3 operator * (const Vec3 &v) const {return Vec3(x*v.x, y*v.y, z*v.z);}
	Vec3 operator - (const Vec3 &v) const {return Vec3(x - v.x, y - v.y, z - v.z);}
	Vec3 operator + (const Vec3 &v) const {return Vec3(x + v.x, y + v.y, z + v.z);}
	Vec3& operator += (const Vec3 &v) {x += v.x, y += v.y, z += v.z; return *this;}
	Vec3& operator *= (const Vec3 &v) {x *= v.x, y *= v.y, z *= v.z; return *this;}
	Vec3& operator *= (const float &f) {x *= f, y *= f, z *= f; return *this;}
	Vec3 operator - () const {return Vec3(-x, -y, -z);}
	Vec3 operator % (const Vec3 &v) const {return Vec3(y*v.z - z*v.y, z*v.x - x*v.z, x*v.y - y*v.x);} 
};
struct RenderProperties
{
	unsigned width, height, numsamples;
	Vec3 background;
	float cutoff, bias;
	bool rr;

	//Constructor
	RenderProperties()
	{width = 640; height = 480; numsamples = 1; background = Vec3(0); cutoff = float(4e-3); bias = float(1e-3); rr = false;};
};
class Camera
{
public:
	Vec3 pos, look;
	Vec3 up, left;
	float fovy;
	
	//Constructors
	Camera()
	{pos = Vec3(0, 1, 0); look = Vec3(0, 0, 1); up = Vec3(0, 1, 0); left = up % look; left.normalize(); fovy = 40;};
	Camera(const Vec3 &p, const Vec3 &l, const Vec3 &u, const float &fy)
	{pos = p; look = l; up = u; left = up % look; up = look % left; look.normalize(); up.normalize(); left.normalize(); fovy = fy;};

	//Functions
	void moveBy(const Vec3 &d)
	{
		pos += d;
	}
};

//Surface/material classes
enum SurfaceType
{
	DIFF, SPEC, GLSP, REFR, GLRF, LGHT
};
struct PPMTexture
{
	string name;
	unsigned w, h, max;
	vector <Vec3> pixels;

	//Constructors
	PPMTexture()
	{name = ""; w = 1; h = 1; max = 1; pixels.clear(); pixels.push_back(Vec3(0));};
};
struct Material
{
	PPMTexture *surfaceTexture;
	Vec3 surfaceColour, emitColour;
	float ior, glossAngle;
	SurfaceType st;

	//Constructors
	Material()
	{surfaceTexture = NULL; surfaceColour = Vec3(0); emitColour = Vec3(0); ior = 1; glossAngle = 0; st = DIFF;};
};

//Object classes
class Shape
{	
public:
	Material material;

	//Constructors
	Shape(const Material &mat)
	{material = mat;};
	
	//Functions
	virtual Vec3 getNormal(const Vec3 &p) const
	{
		return Vec3(0, 1, 0);
	}
	virtual Vec3 getSurfaceColour(const Vec3 &p) const
	{
		return material.surfaceColour;
	}
	virtual bool getIntersect(const Vec3 &rayorig, const Vec3 &raydir, float *d = NULL) const
	{
		return false;
	}
	virtual void moveBy(const Vec3 &d)
	{}
};
class Sphere : public Shape
{
private:
	Vec3 centre;
	float radius, radiusSq;	
public:
							
	//Constructors
	Sphere(const Vec3 &c, const float &r, const Material &mat) : Shape(mat)
	{centre = c; radius = r; radiusSq = r*r;};

	//Functions
	Vec3 getNormal(const Vec3 &p) const
	{
		return p - centre;
	}
	Vec3 getSurfaceColour(const Vec3 &p) const
	{
		return material.surfaceColour;
	}
	bool getIntersect(const Vec3 &rayorig, const Vec3 &raydir, float *d = NULL) const
	{
		Vec3 l = centre - rayorig;

		//If the ray travels away from the centre of the sphere, return false
		float tca = l.dot(raydir);
		if (tca < 0){return false;}

		//If the ray goes towards the sphere but misses, return false
		float d2 = l.dot(l) - tca*tca;
		if (d2 > radiusSq){return false;}

		//Else, find the valid intersection distance and return true
		float thc = sqrt(radiusSq - d2);
		if (d) 
		{
			*d = tca - thc;
			if (*d < 0){*d = tca + thc;}
		}

		return true;
	}
	void moveBy(const Vec3 &d)
	{
		centre += d;
	}
};
class Plane : public Shape	
{
private:
	Vec3 point, normal;
	float texscalex, texscaley;
public:
	
	//Constructors
	Plane(const Vec3 &n, const Vec3 &p, const Material &mat, const float &tx, const float &ty) : Shape(mat)
	{normal = n; normal.normalize(); point = p; texscalex = tx; texscaley = ty;};

	//Functions
	Vec3 getNormal(const Vec3 &p) const
	{
		return normal;
	}
	Vec3 getSurfaceColour(const Vec3 &p) const
	{
		if (material.surfaceTexture)
		{
			unsigned u = unsigned(material.surfaceTexture->w*tilecoord(p.x/texscalex));
			unsigned v = unsigned(material.surfaceTexture->h*tilecoord(p.z/texscaley));
			return material.surfaceTexture->pixels[v*material.surfaceTexture->w + u];
		}

		else
		{
			return material.surfaceColour;
		}
	}
	bool getIntersect(const Vec3 &rayorig, const Vec3 &raydir, float *d = NULL) const
	{
		float denom = normal.dot(raydir);
		if (denom > 1e-6)
		{
			if (d)
			{
				Vec3 origintopoint = point - rayorig;
				*d = origintopoint.dot(normal)/denom;
				if (*d >= 0){return true;}
			}
		}

		return false;
	}
	void moveBy(const Vec3 &d)
	{
		point += d;
	}
};

//Class functions
Vec3 reflect(const Vec3 &i, const Vec3 &n)
{
	return i - n*2*i.dot(n);
}
Vec3 refract(const float &eta, const Vec3 &i, const Vec3 &n)
{
	float cosi = -n.dot(i);
	float k = 1 - eta*eta*(1 - cosi*cosi);
	return i*eta + n*(eta*cosi - sqrt(k));
}
Vec3 randomHemisphereDirection(const Vec3 &n)
{
	float r1 = 2*float(M_PI)*line(gen), r2 = line(gen), r2s = sqrt(r2);
	Vec3 w = n;
	Vec3 u = ((fabs(w.x) > 0.1f ? Vec3(0, 1, 0) : Vec3(1, 0, 0)) % w).normalize();
	Vec3 v = w % u;
	return (u*cos(r1)*r2s + v*sin(r1)*r2s + w*sqrt(1 - r2)).normalize();
}
Vec3 randomConeDirection(const Vec3 &r, const float &theta)
{
	float r1 = 2*float(M_PI)*line(gen), r2 = sin(theta)*sin(theta)*line(gen), r2s = sqrt(r2);
	Vec3 w = r;
	Vec3 u = ((fabs(w.x) > 0.1f ? Vec3(0, 1, 0) : Vec3(1, 0, 0)) % w).normalize();
	Vec3 v = w % u;
	return (u*cos(r1)*r2s + v*sin(r1)*r2s + w*sqrt(1 - r2)).normalize();
}

#endif
