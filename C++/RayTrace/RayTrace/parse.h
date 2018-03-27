#ifndef PARSE_H
#define PARSE_H

//Remove comments/whitespace
void cleanstring(string &in, const bool &whitespace)
{
	size_t comment = in.find("//");
	if (comment != string::npos){in.resize(comment);}
	size_t hash = in.find("#");
	if (hash != string::npos){in.resize(hash);}

	if (whitespace){in.erase(remove_if(in.begin(), in.end(), ::isspace), in.end());}
}

//String conversion
Vec3 stringtovec(const string &in, const unsigned &index)
{
	Vec3 result;
	size_t startpos = in.find("(") + 1, endpos;

	for (unsigned i = 1; i < index; i++)
	{
		startpos = in.find("(", startpos) + 1;
	}

	endpos = in.find(",", startpos);
	result.x = stof(in.substr(startpos, endpos - startpos));

	startpos = endpos + 1; endpos = in.find(",", startpos);
	result.y = stof(in.substr(startpos, endpos - startpos));

	startpos = endpos + 1; endpos = in.find(")", startpos);
	result.z = stof(in.substr(startpos, endpos - startpos));

	return result;
}
Material stringtomat(const string &in, vector <PPMTexture> *const tex)
{
	Material result;
	size_t startpos, endpos;

	startpos = in.find(":") + 1; endpos = in.find(",", startpos);
	string texture = in.substr(startpos, endpos - startpos);
	for (unsigned i = 0; i < tex->size(); i++)
	{
		if (tex->at(i).name == texture){result.surfaceTexture = &tex->at(i);}
	}

	result.surfaceColour = stringtovec(in, 1);

	startpos = in.find(",", in.find(")")) + 1; endpos = in.find(",", startpos);
	result.reflectivity = stof(in.substr(startpos, endpos - startpos));

	startpos = endpos + 1; endpos = in.find(",", startpos);
	result.transparency = stof(in.substr(startpos, endpos - startpos));

	startpos = endpos + 1; endpos = in.find(",", startpos);
	result.ior = stof(in.substr(startpos, endpos - startpos));

	startpos = endpos + 1; endpos = in.find(",", startpos);
	result.specpower = stoi(in.substr(startpos, endpos - startpos));

	startpos = endpos + 1; endpos = in.find(",", startpos);
	result.ka = stof(in.substr(startpos, endpos - startpos));

	startpos = endpos + 1; endpos = in.find(",", startpos);
	result.kd = stof(in.substr(startpos, endpos - startpos));

	startpos = endpos + 1; endpos = in.find(";", startpos);
	result.ks = stof(in.substr(startpos, endpos - startpos));

	return result;
}
Camera stringtocam(const string &in)
{
	float fovyTemp = stof(in.substr(in.find_last_of(",") + 1, in.find(";") - in.find_last_of(",") - 1));
	
	return Camera(stringtovec(in, 1), stringtovec(in, 2), stringtovec(in, 3), fovyTemp);
}
Light stringtolight(const string &in)
{
	float radTemp = stof(in.substr(in.find_last_of(",") + 1, in.find(";") - in.find_last_of(",") - 1));

	return Light(stringtovec(in, 1), stringtovec(in, 2), radTemp);
}
PPMTexture stringtotexture(const string &in)
{
	PPMTexture result;
	size_t startpos, endpos;

	startpos = 0; endpos = in.find(":", startpos);
	string name = in.substr(startpos, endpos - startpos);

	startpos = endpos + 1; endpos = in.find(";");
	string fname = in.substr(startpos, endpos - startpos);

	ifstream texture(fname, ios::in);
	if (texture.is_open())
	{
		result.name = name;
		result.pixels.clear();

		string line = "";

		//Check PPM type
		while (getline(texture, line))
		{
			cleanstring(line, true);
			if (line.size() != 0)
			{
				if (line == "P3"){break;}
			}
		}

		//Find image dimensions
		while (getline(texture, line))
		{
			cleanstring(line, FALSE);
			if (line.size() != 0)
			{
				size_t startpos = line.find_first_not_of(" \t\f\v\n\r");
				size_t endpos = line.find_first_of(" \t\f\v\n\r", startpos);
				result.w = atoi(line.substr(startpos, endpos - startpos).c_str());

				startpos = line.find_first_not_of(" \t\f\v\n\r", endpos);
				endpos = line.find_first_of(" \t\f\v\n\r", startpos);
				result.h = atoi(line.substr(startpos, endpos - startpos).c_str());

				break;
			}
		}

		//Find max. pixel value
		while (getline(texture, line))
		{
			cleanstring(line, true);
			if (line.size() != 0)
			{
				result.max = atoi(line.c_str());
				break;
			}
		}
		
		//Parse raster
		string R, G, B = "";
		while (getline(texture, R))
		{
			getline(texture, G);
			getline(texture, B);
			cleanstring(R, true);
			cleanstring(G, true);
			cleanstring(B, true);
			result.pixels.push_back(Vec3(float(stoi(R))/float(result.max), float(stoi(G))/float(result.max), float(stoi(B))/float(result.max)));
		}

		texture.close();
	}

	return result;
}
void stringtoshape(const string &in, vector <Shape *> *shp, vector <PPMTexture> *const tex, const vector <string> &mats)
{
	size_t startpos, endpos;

	startpos = in.find(":") + 1, endpos = in.find(",", startpos);
	string type = in.substr(startpos, endpos - startpos);

	if (type == "Sphere")
	{
		Vec3 centreTemp;
		float radiusTemp;

		centreTemp = stringtovec(in, 1);

		startpos = in.find(",", in.find(")")) + 1; endpos = in.find(",", startpos);
		radiusTemp = stof(in.substr(startpos, endpos - startpos));

		startpos = endpos + 1; endpos = in.find(";", startpos);
		string mat = in.substr(startpos, endpos - startpos);

		for (unsigned i = 0; i < mats.size(); i++)
		{
			startpos = 0; endpos = mats[i].find(":", startpos);
			string curmatname = mats[i].substr(startpos, endpos - startpos);
			if (curmatname == mat){shp->push_back(new Sphere(centreTemp, radiusTemp, stringtomat(mats[i], tex)));}
		}
	}

	else if (type == "Plane")
	{
		Vec3 normalTemp, pointTemp;
		float scalexTemp, scaleyTemp;

		normalTemp = stringtovec(in, 1);
		pointTemp = stringtovec(in, 2);

		startpos = in.find(",", in.find(")", in.find(")") + 1)) + 1; endpos = in.find(",", startpos);
		string mat = in.substr(startpos, endpos - startpos);

		startpos = endpos + 1; endpos = in.find(",", startpos);
		scalexTemp = stof(in.substr(startpos, endpos - startpos));

		startpos = endpos + 1; endpos = in.find(";", startpos);
		scaleyTemp = stof(in.substr(startpos, endpos - startpos));

		for (unsigned i = 0; i < mats.size(); i++)
		{
			startpos = 0; endpos = mats[i].find(":", startpos);
			string curmatname = mats[i].substr(startpos, endpos - startpos);
			if (curmatname == mat){shp->push_back(new Plane(normalTemp, pointTemp, stringtomat(mats[i], tex), scalexTemp, scaleyTemp));}
		}
	}
}

//Parsing
bool parse_scene(const string &fname, RenderProperties *prp, Camera *cam, vector <Shape *> *shp, vector <Light> *lgt, vector <PPMTexture> *tex)
{
	ifstream scene(fname, ios::in);
	if (!scene.is_open() || !prp || !cam || !shp || !lgt || !tex){return false;}
	shp->clear(); lgt->clear(); tex->clear();
	string line = "";
	vector <string> ms, ps, cs, os, ls, ts;

	while (getline(scene, line))
	{
		cleanstring(line, true);

		//Parse materials
		if (line == "[MAT]")
		{
			while (getline(scene, line))
			{
				cleanstring(line, true);
				if (line != "[/MAT]")
				{
					if (line.size() != 0){ms.push_back(line);}
				}
				else {break;}
			}
		}

		//Parse textures
		if (line == "[TEX]")
		{
			while (getline(scene, line))
			{
				cleanstring(line, true);
				if (line != "[/TEX]")
				{
					if (line.size() != 0){ts.push_back(line);}
				}
				else {break;}
			}
		}

		//Parse render properties
		if (line == "[PROP]")
		{
			while (getline(scene, line))
			{
				cleanstring(line, true);
				if (line != "[/PROP]")
				{
					if (line.size() != 0){ps.push_back(line);}
				}
				else {break;}
			}
		}
		
		//Parse camera
		if (line == "[CAM]")
		{
			while (getline(scene, line))
			{
				cleanstring(line, true);
				if (line != "[/CAM]")
				{
					if (line.size() != 0){cs.push_back(line);}
				}
				else {break;}
			}
		}

		//Parse objects
		if (line == "[OBJ]")
		{
			while (getline(scene, line))
			{
				cleanstring(line, true);
				if (line != "[/OBJ]")
				{
					if (line.size() != 0){os.push_back(line);}
				}
				else {break;}
			}
		}

		//Parse Lights
		if (line == "[LGT]")
		{
			while (getline(scene, line))
			{
				cleanstring(line, true);
				if (line != "[/LGT]")
				{
					if (line.size() != 0){ls.push_back(line);}
				}
				else {break;}
			}
		}
	}

	scene.close();

	//Assign textures
	for (unsigned i = 0; i < ts.size(); i++)
	{
		tex->push_back(stringtotexture(ts[i]));
	}

	//Assign render properties
	for (unsigned i = 0; i < ps.size(); i++)
	{
		if (ps[i].find("AntiAliasing") != string::npos){prp->aa = stoi(ps[i].substr(ps[i].find("=") + 1, ps[i].find(";") - ps[i].find("=") - 1));}
		else if (ps[i].find("Width") != string::npos){prp->width = stoi(ps[i].substr(ps[i].find("=") + 1, ps[i].find(";") - ps[i].find("=") - 1));}
		else if (ps[i].find("Height") != string::npos){prp->height = stoi(ps[i].substr(ps[i].find("=") + 1, ps[i].find(";") - ps[i].find("=") - 1));}
		else if (ps[i].find("BGC") != string::npos){prp->background = stringtovec(ps[i], 1);}
		else if (ps[i].find("Cutoff") != string::npos){prp->cutoff = stof(ps[i].substr(ps[i].find("=") + 1, ps[i].find(";") - ps[i].find("=") - 1));}
		else if (ps[i].find("Bias") != string::npos){prp->bias = stof(ps[i].substr(ps[i].find("=") + 1, ps[i].find(";") - ps[i].find("=") - 1));}
		else if (ps[i].find("ShadowRays") != string::npos){prp->shadowrays = stoi(ps[i].substr(ps[i].find("=") + 1, ps[i].find(";") - ps[i].find("=") - 1));}
	}

	//Assign camera
	if (cs.size() > 0){*cam = stringtocam(cs[0]);}

	//Assign objects
	for (unsigned i = 0; i < os.size(); i++)
	{
		stringtoshape(os[i], shp, tex, ms);
	}

	//Assign lights
	for (unsigned i = 0; i < ls.size(); i++)
	{
		lgt->push_back(stringtolight(ls[i]));
	}

	return true;
}

#endif
