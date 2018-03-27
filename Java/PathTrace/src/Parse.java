
import java.util.ArrayList;
import java.io.*;

class Scene
{
	public RenderProperties prp;
	public Camera cam;
	public ArrayList <Shape> shp;
	
	public Scene()
	{prp = new RenderProperties(); cam = new Camera(); shp = new ArrayList <Shape>();}
}

class RawMesh
{
	public ArrayList <String> verts, inds, norms, bounds;
	
	public RawMesh()
	{verts = new ArrayList <String>(); inds = new ArrayList <String>(); norms = new ArrayList <String>(); bounds = new ArrayList <String>();}
}

class Parse
{
	static String cleanstring(String in, boolean whitespace)
	{
		int comment = in.lastIndexOf("//");
		if (comment != -1){in = in.substring(0, comment);}
		int hash = in.lastIndexOf("#");
		if (hash != -1){in = in.substring(0, hash);}
		
		if (whitespace){in = in.replaceAll("\\s+", "");}
		
		return in;
	}
	static Vec3 stringtovec(String in, int index)
	{
		Vec3 result = new Vec3();
		int startpos = in.indexOf("(") + 1, endpos = 0;

		for (int i = 1; i < index; i++)
		{
			startpos = in.indexOf("(", startpos) + 1;
		}

		endpos = in.indexOf(",", startpos);
		result.x = Float.parseFloat(in.substring(startpos, endpos));

		startpos = endpos + 1; endpos = in.indexOf(",", startpos);
		result.y = Float.parseFloat(in.substring(startpos, endpos));

		startpos = endpos + 1; endpos = in.indexOf(")", startpos);
		result.z = Float.parseFloat(in.substring(startpos, endpos));

		return result;
	}
	static Material stringtomat(String in)
	{
		Material result = new Material();
		int startpos, endpos;

		result.surfaceColour = stringtovec(in, 1);
		result.emitColour = stringtovec(in, 2);

		startpos = in.indexOf(",", in.indexOf(")", in.indexOf(")") + 1)) + 1; endpos = in.indexOf(",", startpos);
		result.ior = Float.parseFloat(in.substring(startpos, endpos));

		startpos = endpos + 1; endpos = in.indexOf(",", startpos);
		result.glossAngle = Float.parseFloat(in.substring(startpos, endpos));

		startpos = endpos + 1; endpos = in.indexOf(";", startpos);
		String surftypeTemp = in.substring(startpos, endpos);

		if (surftypeTemp.equals("DIFF")){result.st = SurfaceType.DIFF;}
		else if (surftypeTemp.equals("SPEC")){result.st = SurfaceType.SPEC;}
		else if (surftypeTemp.equals("GLSP")){result.st = SurfaceType.GLSP;}
		else if (surftypeTemp.equals("REFR")){result.st = SurfaceType.REFR;}
		else if (surftypeTemp.equals("GLRF")){result.st = SurfaceType.GLRF;}
		else if (surftypeTemp.equals("LGHT")){result.st = SurfaceType.LGHT;}

		return result;
	}
	static Camera stringtocam(String in)
	{
		float fovyTemp = Float.parseFloat(in.substring(in.lastIndexOf(",") + 1, in.indexOf(";")));
		
		return new Camera(stringtovec(in, 1), stringtovec(in, 2), stringtovec(in, 3), fovyTemp);
	}
	static RawMesh parserawmesh(String fname)
	{
		RawMesh result = new RawMesh();
		
		BufferedReader meshbuffer = null;
		try
		{
			meshbuffer = new BufferedReader(new FileReader(fname));
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}

		try
		{
			if (meshbuffer.ready())
			{
				result.verts.clear();
				result.inds.clear();
				result.norms.clear();
				result.bounds.clear();

				String line = "";
				while ((line = meshbuffer.readLine()) != null)
				{
					line = cleanstring(line, true);
					
					//Parse vertices
					if (line.equals("[VERTEX]"))
					{
						while ((line = meshbuffer.readLine()) != null)
						{
							line = cleanstring(line, true);
							if (!line.equals("[/VERTEX]"))
							{
								if (line.length() != 0){result.verts.add(line);}
							}
							else {break;}
						}
					}

					//Parse indices
					if (line.equals("[INDEX]"))
					{
						while ((line = meshbuffer.readLine()) != null)
						{
							line = cleanstring(line, true);
							if (!line.equals("[/INDEX]"))
							{
								if (line.length() != 0){result.inds.add(line);}
							}
							else {break;}
						}
					}
					
					//Parse normals
					if (line.equals("[NORMAL]"))
					{
						while ((line = meshbuffer.readLine()) != null)
						{
							line = cleanstring(line, true);
							if (!line.equals("[/NORMAL]"))
							{
								if (line.length() != 0){result.norms.add(line);}
							}
							else {break;}
						}
					}

					//Parse bounding boxes
					if (line.equals("[BBOX]"))
					{
						while ((line = meshbuffer.readLine()) != null)
						{
							line = cleanstring(line, true);
							if (!line.equals("[/BBOX]"))
							{
								if (line.length() != 0){result.bounds.add(line);}
							}
							else {break;}
						}
					}
				}

				meshbuffer.close();
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		return result;
	}
	static Shape stringtoshape(String in, ArrayList <String> mats)
	{
		int startpos = in.indexOf(":") + 1, endpos = in.indexOf(",", startpos);
		String type = in.substring(startpos, endpos);

		if (type.equals("Sphere"))
		{
			Vec3 centreTemp = stringtovec(in, 1);

			startpos = in.indexOf(",", in.indexOf(")")) + 1; endpos = in.indexOf(",", startpos);
			float radiusTemp = Float.parseFloat(in.substring(startpos, endpos));
			
			startpos = endpos + 1; endpos = in.indexOf(";", startpos);
			String mat = in.substring(startpos, endpos);

			for (String curmat : mats)
			{
				startpos = 0; endpos = curmat.indexOf(":", startpos);
				String curmatname = curmat.substring(startpos, endpos);
				if (curmatname.equals(mat)){return new Sphere(centreTemp, radiusTemp, stringtomat(curmat));}
			}
		}

		else if (type.equals("Plane"))
		{
			Vec3 normalTemp = stringtovec(in, 1), pointTemp = stringtovec(in, 2);

			startpos = in.indexOf(",", in.indexOf(")", in.indexOf(")") + 1)) + 1; endpos = in.indexOf(",", startpos);
			String mat = in.substring(startpos, endpos);

			startpos = endpos + 1; endpos = in.indexOf(",", startpos);
			float scalexTemp = Float.parseFloat(in.substring(startpos, endpos));

			startpos = endpos + 1; endpos = in.indexOf(";", startpos);
			float scaleyTemp = Float.parseFloat(in.substring(startpos, endpos));

			for (String curmat : mats)
			{
				startpos = 0; endpos = curmat.indexOf(":", startpos);
				String curmatname = curmat.substring(startpos, endpos);
				if (curmatname.equals(mat)){return new Plane(normalTemp, pointTemp, stringtomat(curmat), scalexTemp, scaleyTemp);}
			}
		}
		
		else if (type.equals("Mesh"))
		{
			Vec3 locTemp = stringtovec(in, 1);
			
			startpos = in.indexOf(",", in.indexOf(")")) + 1; endpos = in.indexOf(",", startpos);
			String msh = in.substring(startpos, endpos);
			
			startpos = endpos + 1; endpos = in.indexOf(";", startpos);
			String mat = in.substring(startpos, endpos);
			
			RawMesh raw = parserawmesh(msh);
			
			ArrayList <Triangle> triTemp = new ArrayList <Triangle>();
			for (int i = 0; i < raw.inds.size(); i++)
			{
				String [] indices = raw.inds.get(i).split(",");
				if (indices.length != 3)
					continue;
				
				Vec3 vertex0 = stringtovec(raw.verts.get(Integer.parseInt(indices[0])), 1);
				Vec3 vertex1 = stringtovec(raw.verts.get(Integer.parseInt(indices[1])), 1);
				Vec3 vertex2 = stringtovec(raw.verts.get(Integer.parseInt(indices[2])), 1);
				Vec3 normal = stringtovec(raw.norms.get(i), 1);
				
				triTemp.add(new Triangle(vertex0.add(locTemp), vertex1.add(locTemp), vertex2.add(locTemp), normal));
			}
			
			AABB boundTemp = new AABB(stringtovec(raw.bounds.get(0), 1).add(locTemp), stringtovec(raw.bounds.get(0), 2).add(locTemp));
			
			for (String curmat : mats)
			{
				startpos = 0; endpos = curmat.indexOf(":", startpos);
				String curmatname = curmat.substring(startpos, endpos);
				if (curmatname.equals(mat)){return new Mesh(boundTemp, triTemp, stringtomat(curmat));}
			}
		}
		
		return null;
	}
	static Scene parse_scene(String fname)
	{
		Scene result = new Scene();
		
		BufferedReader scenebuffer = null;
		try
		{
			scenebuffer = new BufferedReader(new FileReader(fname));
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}
		
		try
		{
			if (scenebuffer.ready())
			{
				String line = "";
				ArrayList <String> ms = new ArrayList <String>();
				ArrayList <String> ps = new ArrayList <String>();
				ArrayList <String> cs = new ArrayList <String>();
				ArrayList <String> os = new ArrayList <String>();

				while ((line = scenebuffer.readLine()) != null)
				{
					line = cleanstring(line, true);
					
					//Parse materials
					if (line.equals("[MAT]"))
					{
						while ((line = scenebuffer.readLine()) != null)
						{
							line = cleanstring(line, true);
							if (!line.equals("[/MAT]"))
							{
								if (line.length() != 0){ms.add(line);}
							}
							else {break;}
						}
					}

					//Parse render properties
					if (line.equals("[PROP]"))
					{
						while ((line = scenebuffer.readLine()) != null)
						{
							line = cleanstring(line, true);
							if (!line.equals("[/PROP]"))
							{
								if (line.length() != 0){ps.add(line);}
							}
							else {break;}
						}
					}
					
					//Parse camera
					if (line.equals("[CAM]"))
					{
						while ((line = scenebuffer.readLine()) != null)
						{
							line = cleanstring(line, true);
							if (!line.equals("[/CAM]"))
							{
								if (line.length() != 0){cs.add(line);}
							}
							else {break;}
						}
					}

					//Parse objects
					if (line.equals("[OBJ]"))
					{
						while ((line = scenebuffer.readLine()) != null)
						{
							line = cleanstring(line, true);
							if (!line.equals("[/OBJ]"))
							{
								if (line.length() != 0){os.add(line);}
							}
							else {break;}
						}
					}
				}

				scenebuffer.close();

				//Assign render properties
				for (int i = 0; i < ps.size(); i++)
				{
					if (ps.get(i).indexOf("Width") != -1){result.prp.width = Integer.parseInt(ps.get(i).substring(ps.get(i).indexOf("=") + 1, ps.get(i).indexOf(";")));}
					else if (ps.get(i).indexOf("Height") != -1){result.prp.height = Integer.parseInt(ps.get(i).substring(ps.get(i).indexOf("=") + 1, ps.get(i).indexOf(";")));}
					else if (ps.get(i).indexOf("NumSamples") != -1){result.prp.numsamples = Integer.parseInt(ps.get(i).substring(ps.get(i).indexOf("=") + 1, ps.get(i).indexOf(";")));}
					else if (ps.get(i).indexOf("BGC") != -1){result.prp.background = stringtovec(ps.get(i), 1);}
					else if (ps.get(i).indexOf("Cutoff") != -1){result.prp.cutoff = Float.parseFloat(ps.get(i).substring(ps.get(i).indexOf("=") + 1, ps.get(i).indexOf(";")));}
					else if (ps.get(i).indexOf("Bias") != -1){result.prp.bias = Float.parseFloat(ps.get(i).substring(ps.get(i).indexOf("=") + 1, ps.get(i).indexOf(";")));}
				}
				
				//Assign camera
				if (cs.size() > 0){result.cam = stringtocam(cs.get(0));}
				
				//Assign objects
				for (int i = 0; i < os.size(); i++)
				{
					Shape curObj = stringtoshape(os.get(i), ms);
					if (curObj != null)
						result.shp.add(curObj);
				}	
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		return result;
	}
}
