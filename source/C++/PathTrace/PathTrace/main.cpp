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

Vec3 trace(Vec3 rayorig, Vec3 raydir, const RenderProperties &prop, const vector <Shape *> &shapes)
{
	Vec3 rayColour(1);

	while (rayColour.findmax() > prop.cutoff)
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

		//STEP 2: If there's no intersection return sky colour, if light return emission
		if (!shape)
		{
			float skyFactor = exp(raydir.dot(Vec3(raydir.x, 0, raydir.z).normalize()) - 1);
			rayColour *= skyFactor > 0.5f ? prop.background*skyFactor : prop.background*0.5f;
			break;
		}

		if (shape->material.st == LGHT)
		{
			rayColour *= shape->material.emitColour;
			break;
		}

		//STEP 3: If there was an intersection, calculate the hit point, normal, colour
		const Vec3 phit = rayorig + raydir*dnear;
		const Vec3 chit = shape->getSurfaceColour(phit);
		rayColour *= chit;

		if (prop.rr)
		{
			float p = chit.findmax();
			if (line(gen) < p){rayColour *= 1/p;}
			else {rayColour = Vec3(0); continue;}
		}

		Vec3 nhit = shape->getNormal(phit).normalize();
		bool inside = false;
		if (raydir.dot(nhit) > 0){nhit = -nhit; inside = true;}

		//STEP 4: Calculate a new direction for the ray based on surface type
		if (shape->material.st == DIFF)
		{
			rayorig = phit + nhit*prop.bias;
			raydir = randomHemisphereDirection(nhit);
			continue;
		}

		if (shape->material.st == SPEC)
		{
			rayorig = phit + nhit*prop.bias;
			raydir = reflect(raydir, nhit).normalize();
			continue;
		}

		if (shape->material.st == GLSP)
		{
			rayorig = phit + nhit*prop.bias;
			raydir = randomConeDirection(reflect(raydir, nhit).normalize(), shape->material.glossAngle);
			continue;
		}

		if (shape->material.st == REFR)
		{
			float n = inside ? shape->material.ior : 1/shape->material.ior;
			rayorig = phit - nhit*prop.bias;
			raydir = refract(n, raydir, nhit).normalize();
			continue;
		}	

		if (shape->material.st == GLRF)
		{
			float n = inside ? shape->material.ior : 1/shape->material.ior;
			rayorig = phit - nhit*prop.bias;
			raydir = randomConeDirection(refract(n, raydir, nhit).normalize(), shape->material.glossAngle);
			continue;
		}
	}

	return rayColour;
}

void render(const string &filename, const RenderProperties &prop, const Camera &cam, const vector<Shape *> &shapes)
{
	//Create image
	Vec3 *image = new Vec3[prop.width*prop.height];
	float invWidth = 1/float(prop.width), invHeight = 1/float(prop.height);
	float aspectratio = prop.width/float(prop.height);
	float angle = tan(float(M_PI)*0.5f*cam.fovy/180);
	float invSamples = 1/float(prop.numsamples);

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
			for (unsigned s = 0; s < prop.numsamples; s++)
			{
				float xx = (2*((x + line(gen))*invWidth) - 1)*angle*aspectratio;
				float yy = (1 - 2*((y + line(gen))*invHeight))*angle;
				image[y*prop.width + x] += trace(cam.pos, (cam.left*xx + cam.up*yy + cam.look).normalize(), prop, shapes);
			}
			image[y*prop.width + x] *= invSamples;
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
	cout << "GKPathTrace V1.0 : " << omp_get_num_procs() << " CPU cores detected\n---" << endl;

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
	vector <PPMTexture> textures;

	//Parse scene
	if (!parse_scene(scenename, &properties, &camera, &shapes, &textures))
	{
		cout << "Unable to parse '" << scenename << "'" << endl;

		cin.get();
		return 0;
	}
	
	//Render scene
	cout << "Rendering..." << endl;
	render(filename, properties, camera, shapes);

	//Clean scene
	cout << "Cleaning scene..." << endl;
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
