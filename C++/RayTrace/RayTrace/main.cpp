#define _USE_MATH_DEFINES
#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#include <math.h>
#include <fstream>
#include <vector>
#include <iostream>
#include <string>
#include <random>
#include <time.h>
#include <algorithm>
#include <omp.h>

using namespace std;

#include "definitions.h"
#include "parse.h"

Vec3 trace(const Vec3 &rayorig, const Vec3 &raydir, const RenderProperties &prop, const vector <Shape *> &shapes, const vector <Light> &lights, const float &atten)
{
	float dnear = INFINITY;
	const Shape *shape = NULL;

	//STEP 1: Look for intersections with shapes in the scene, find distance to nearest shape
	for (unsigned i = 0; i < shapes.size(); i++) 
	{
		float d = INFINITY;
		if (shapes[i]->getIntersect(rayorig, raydir, &d))
		{
			if (d < dnear) 
			{
				dnear = d;
				shape = shapes[i];
			}
		}
	}

	//STEP 2: If there's no intersection return sky colour
	if (!shape)
	{
		float skyFactor = exp(raydir.dot(Vec3(raydir.x, 0, raydir.z).normalize()) - 1);
		return skyFactor > 0.5f ? prop.background*skyFactor : prop.background*0.5f;
	}

	//STEP 3: If there was an intersection, calculate the hit point, normal and colour
	Vec3 surfaceColour;
	const Vec3 phit = rayorig + raydir*dnear;
	Vec3 nhit = shape->getNormal(phit).normalize();
	const Vec3 chit = shape->getSurfaceColour(phit);
	
	//STEP 4: Calculate colour of reflected/refracted rays
	bool inside = false;
	if (raydir.dot(nhit) > 0){nhit = -nhit; inside = true;}
	if (shape->material.transparency || shape->material.reflectivity)
	{
		//float fresneleffect = lerp(pow(1 + raydir.dot(nhit), 3), 1, 0.1f); //Change the lerp value to tweak the effect

		//REFLECTION
		Vec3 reflectionColour;
		float reflectionAtten = atten*shape->material.reflectivity;
		if (reflectionAtten > prop.cutoff) 
		{
			reflectionColour = trace(phit + nhit*prop.bias, reflect(raydir, nhit).normalize(), prop, shapes, lights, reflectionAtten);
		}
		
		//REFRACTION
		Vec3 refractionColour;
		float refractionAtten = atten*shape->material.transparency;
		if (refractionAtten > prop.cutoff)
		{
			float n = inside ? shape->material.ior : 1/shape->material.ior;
			refractionColour = trace(phit - nhit*prop.bias, refract(n, raydir, nhit).normalize(), prop, shapes, lights, refractionAtten);
		}

		//surfaceColour += (reflectionColour*fresneleffect*shape->material.reflectivity + refractionColour*(1 - fresneleffect)*shape->material.transparency)*chit;
		surfaceColour += (reflectionColour*shape->material.reflectivity + refractionColour*shape->material.transparency)*chit;
	}

	//STEP 5: Calculate the phong contributions by looping through the lights
	if (shape->material.kd || shape->material.ks)
	{
		for (unsigned i = 0; i < lights.size(); i++) 
		{
			Vec3 diff, spec;
			Vec3 lightDirection = (lights[i].pos - phit).normalize();
			float cosFactor = max(0, nhit.dot(lightDirection));
			float transmission = float(prop.shadowrays);

			if (cosFactor)
			{
				if (shape->material.kd){diff = shape->material.kd*cosFactor;}
				if (shape->material.ks){spec = shape->material.ks*powf(max(0, raydir.dot(reflect(lightDirection, nhit))), float(shape->material.specpower));}
				
				for (unsigned j = 0; j < prop.shadowrays; j++)
				{
					Vec3 currentDirection = (lights[i].getPoint(2*float(M_PI)*line(gen), float(M_PI)*line(gen)) - phit).normalize();

					for (unsigned k = 0; k < shapes.size(); k++) 
					{
						if (shapes[k]->getIntersect(phit + nhit*prop.bias, currentDirection, NULL)) 
						{
							transmission -= 1;
							break;
						}
					}
				}
			}

			transmission /= prop.shadowrays;
			surfaceColour += chit*lights[i].colour*transmission*(diff + spec);
		}
	}

	//STEP 6: Calculate ambient contribution
	if (shape->material.ka)
	{
		surfaceColour += chit*shape->material.ka;
	}

	return surfaceColour;
}

void render(const string &filename, const RenderProperties &prop, const Camera &cam, const vector<Shape *> &shapes, const vector<Light> &lights)
{
	//Create image
	Vec3 *image = new Vec3[prop.width*prop.height];
	float invWidth = 1/float(prop.width), invHeight = 1/float(prop.height);
	float aspectratio = prop.width/float(prop.height);
	float angle = tan(float(M_PI)*0.5f*cam.fovy/180);

	//Trace rays
	unsigned rowCount = 0, prevPercent = 0;
	#pragma omp parallel for schedule(dynamic, 1)
	for (int y = 0; y < int(prop.height); y++) 
	{
		rowCount += 1;
		unsigned curPercent = unsigned(100*rowCount*invHeight);
		if (curPercent != prevPercent)
		{
			clr(6);
			prevPercent = curPercent;
			cout << prevPercent << "% complete" << endl;
		}

		for (unsigned x = 0; x < prop.width; x++) 
		{
			//Anti-aliasing
			if (prop.aa == 1)
			{
				float xx[2] = {(2*((x + 0.25f)*invWidth) - 1)*angle*aspectratio,
							   (2*((x + 0.75f)*invWidth) - 1)*angle*aspectratio};
				float yy[2] = {(1 - 2*((y + 0.25f)*invHeight))*angle,
							   (1 - 2*((y + 0.75f)*invHeight))*angle};

				for (unsigned a = 0; a < 2; a++)
				{
					for (unsigned b = 0; b < 2; b++)
					{
						image[y*prop.width + x] += trace(cam.pos, (cam.left*xx[a] + cam.up*yy[b] + cam.look).normalize(), prop, shapes, lights, 1);
					}
				}
				image[y*prop.width + x] = image[y*prop.width + x]*0.25f;
			}

			//No AA
			else
			{
				float xx = (2*((x + 0.5f)*invWidth) - 1)*angle*aspectratio;
				float yy = (1 - 2*((y + 0.5f)*invHeight))*angle;
				image[y*prop.width + x] = trace(cam.pos, (cam.left*xx + cam.up*yy + cam.look).normalize(), prop, shapes, lights, 1);
			}
		}
	}

	//Save result to a PPM image
	cout << "Saving image..." << endl;
	ofstream ofs(filename, ios::out | ios::binary);
	ofs << "P6\n" << prop.width << " " << prop.height << "\n255\n";
	for (unsigned i = 0; i < prop.width*prop.height; i++) 
	{
		ofs << unsigned char(min(1, image[i].x)*255) << unsigned char(min(1, image[i].y)*255) << unsigned char(min(1, image[i].z)*255); 
	}
	ofs.close();
	delete [] image;
}

int main()
{
	SetConsoleTextAttribute(GetStdHandle(STD_OUTPUT_HANDLE), FOREGROUND_RED | FOREGROUND_GREEN | FOREGROUND_INTENSITY);

	omp_set_num_threads(omp_get_num_procs());
	cout << "GKTrace V1.0 : " << omp_get_num_procs() << " CPU cores detected\n---" << endl;

	string filename = "untitled";
	string scenename = "scene";
	
	cout << "Enter filename: ";
	getline(cin, filename);
	filename += ".ppm";

	cout << "Enter scene filename: ";
	getline(cin, scenename);
	scenename += ".txt";

	time_t starttime = time(NULL);
	cout << "Creating scene..." << endl;

	RenderProperties properties;
	Camera camera;
	vector <Shape *> shapes;
	vector <Light> lights;
	vector <PPMTexture> textures;

	//Parse scene
	if (!parse_scene(scenename, &properties, &camera, &shapes, &lights, &textures))
	{
		cout << "Unable to parse '" << scenename << "'" << endl;

		cin.get();
		return 0;
	}
	
	cout << "Rendering..." << endl;

	//Render scene
	render(filename, properties, camera, shapes, lights);
	
	cout << "Cleaning scene..." << endl;

	//Clean scene
	while (!shapes.empty()) 
	{
		Shape *shp = shapes.back();
		shapes.pop_back();
		delete shp;
	}

	cout << "Completed in " << difftime(time(NULL), starttime) << " seconds..." << endl;

	cin.get();
	return 0;
}
