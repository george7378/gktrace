package pathtrace.utility;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import pathtrace.geometry.AxisAlignedBoundingBox;
import pathtrace.geometry.Mesh;
import pathtrace.geometry.Plane;
import pathtrace.geometry.Shape;
import pathtrace.geometry.Sphere;
import pathtrace.geometry.Triangle;
import pathtrace.geometry.enums.SurfaceType;

public final class Parse
{
	public static String cleanString(String input, boolean removeWhitespace)
	{
		int commentPos = input.lastIndexOf("//");
		if (commentPos != -1)
		{
			input = input.substring(0, commentPos);
		}
		
		int hashPos = input.lastIndexOf("#");
		if (hashPos != -1)
		{
			input = input.substring(0, hashPos);
		}
		
		if (removeWhitespace)
		{
			input = input.replaceAll("\\s+", "");
		}
		
		return input;
	}
	
	public static Vec3 stringToVec3(String input, int vec3Index)
	{
		int startPos = input.indexOf("(") + 1;
		for (int i = 1; i < vec3Index; i++)
		{
			startPos = input.indexOf("(", startPos) + 1;
		}
		int endPos = input.indexOf(",", startPos);
		
		float x = Float.parseFloat(input.substring(startPos, endPos));

		startPos = endPos + 1;
		endPos = input.indexOf(",", startPos);
		
		float y = Float.parseFloat(input.substring(startPos, endPos));

		startPos = endPos + 1;
		endPos = input.indexOf(")", startPos);
		
		float z = Float.parseFloat(input.substring(startPos, endPos));

		return new Vec3(x, y, z);
	}
	
	public static Material stringToMaterial(String input)
	{
		Vec3 surfaceColour = stringToVec3(input, 1);
		Vec3 emissionColour = stringToVec3(input, 2);

		int startPos = input.indexOf(",", input.indexOf(")", input.indexOf(")") + 1)) + 1;
		int endPos = input.indexOf(",", startPos);
		
		float refractiveIndex = Float.parseFloat(input.substring(startPos, endPos));

		startPos = endPos + 1;
		endPos = input.indexOf(",", startPos);
		
		float glossAngle = Float.parseFloat(input.substring(startPos, endPos));

		startPos = endPos + 1;
		endPos = input.indexOf(";", startPos);
		
		SurfaceType type = SurfaceType.valueOf(input.substring(startPos, endPos));
		
		return new Material(surfaceColour, emissionColour, refractiveIndex, glossAngle, type);
	}
	
	public static Camera stringToCamera(String input)
	{
		float fieldOfViewY = Float.parseFloat(input.substring(input.lastIndexOf(",") + 1, input.indexOf(";")));
		
		return new Camera(stringToVec3(input, 1), stringToVec3(input, 2), stringToVec3(input, 3), fieldOfViewY);
	}
	
	public static RawMesh parseRawMesh(String fileName)
	{
		BufferedReader meshBuffer = null;
		try
		{
			meshBuffer = new BufferedReader(new FileReader(fileName));
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}

		try
		{
			if (meshBuffer.ready())
			{
				ArrayList<String> vertices = new ArrayList<String>();
				ArrayList<String> indices = new ArrayList<String>();
				ArrayList<String> normals = new ArrayList<String>();
				ArrayList<String> boundaries = new ArrayList<String>();

				String line = "";
				while ((line = meshBuffer.readLine()) != null)
				{
					line = cleanString(line, true);
					
					// Parse vertices
					if (line.equals("[VERTEX]"))
					{
						while ((line = meshBuffer.readLine()) != null)
						{
							line = cleanString(line, true);
							if (!line.equals("[/VERTEX]"))
							{
								if (line.length() != 0)
								{
									vertices.add(line);
								}
							}
							else
							{
								break;
							}
						}
					}

					// Parse indices
					if (line.equals("[INDEX]"))
					{
						while ((line = meshBuffer.readLine()) != null)
						{
							line = cleanString(line, true);
							if (!line.equals("[/INDEX]"))
							{
								if (line.length() != 0)
								{
									indices.add(line);
								}
							}
							else
							{
								break;
							}
						}
					}
					
					// Parse normals
					if (line.equals("[NORMAL]"))
					{
						while ((line = meshBuffer.readLine()) != null)
						{
							line = cleanString(line, true);
							if (!line.equals("[/NORMAL]"))
							{
								if (line.length() != 0)
								{
									normals.add(line);
								}
							}
							else
							{
								break;
							}
						}
					}

					// Parse bounding boxes
					if (line.equals("[BBOX]"))
					{
						while ((line = meshBuffer.readLine()) != null)
						{
							line = cleanString(line, true);
							if (!line.equals("[/BBOX]"))
							{
								if (line.length() != 0)
								{
									boundaries.add(line);
								}
							}
							else
							{
								break;
							}
						}
					}
				}

				meshBuffer.close();
				
				return new RawMesh(vertices, indices, normals, boundaries);
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		return null;
	}
	
	public static Shape stringToShape(String input, ArrayList<String> materialStrings)
	{
		int startPos = input.indexOf(":") + 1;
		int endPos = input.indexOf(",", startPos);
		
		String type = input.substring(startPos, endPos);

		if (type.equals("Sphere"))
		{
			Vec3 centre = stringToVec3(input, 1);

			startPos = input.indexOf(",", input.indexOf(")")) + 1;
			endPos = input.indexOf(",", startPos);
			
			float radius = Float.parseFloat(input.substring(startPos, endPos));
			
			startPos = endPos + 1; 
			endPos = input.indexOf(";", startPos);
			
			String materialName = input.substring(startPos, endPos);

			for (String materialString : materialStrings)
			{
				startPos = 0;
				endPos = materialString.indexOf(":", startPos);
				
				String materialStringName = materialString.substring(startPos, endPos);
				if (materialStringName.equals(materialName))
				{
					return new Sphere(stringToMaterial(materialString), centre, radius);
				}
			}
		}
		else if (type.equals("Plane"))
		{
			Vec3 normal = stringToVec3(input, 1);
			Vec3 point = stringToVec3(input, 2);

			startPos = input.indexOf(",", input.indexOf(")", input.indexOf(")") + 1)) + 1;
			endPos = input.indexOf(";", startPos);
			
			String materialName = input.substring(startPos, endPos);

			for (String materialString : materialStrings)
			{
				startPos = 0;
				endPos = materialString.indexOf(":", startPos);
				
				String materialStringName = materialString.substring(startPos, endPos);
				if (materialStringName.equals(materialName))
				{
					return new Plane(stringToMaterial(materialString), normal, point);
				}
			}
		}	
		else if (type.equals("Mesh"))
		{
			Vec3 centre = stringToVec3(input, 1);
			
			startPos = input.indexOf(",", input.indexOf(")")) + 1;
			endPos = input.indexOf(",", startPos);
			
			String meshName = input.substring(startPos, endPos);
			
			startPos = endPos + 1;
			endPos = input.indexOf(";", startPos);
			
			String materialName = input.substring(startPos, endPos);
			
			RawMesh rawMesh = parseRawMesh(meshName);
			
			ArrayList<Triangle> triangles = new ArrayList<Triangle>();
			for (int i = 0; i < rawMesh.Indices.size(); i++)
			{
				String [] indices = rawMesh.Indices.get(i).split(",");
				if (indices.length != 3)
				{
					continue;
				}
		
				Vec3 v0 = stringToVec3(rawMesh.Vertices.get(Integer.parseInt(indices[0])), 1);
				Vec3 v1 = stringToVec3(rawMesh.Vertices.get(Integer.parseInt(indices[1])), 1);
				Vec3 v2 = stringToVec3(rawMesh.Vertices.get(Integer.parseInt(indices[2])), 1);
				Vec3 normal = stringToVec3(rawMesh.Normals.get(i), 1);
				
				triangles.add(new Triangle(v0.add(centre), v1.add(centre), v2.add(centre), normal));
			}
			
			AxisAlignedBoundingBox boundingBox = new AxisAlignedBoundingBox(stringToVec3(rawMesh.Boundaries.get(0), 1).add(centre), stringToVec3(rawMesh.Boundaries.get(0), 2).add(centre));
			
			for (String materialString : materialStrings)
			{
				startPos = 0;
				endPos = materialString.indexOf(":", startPos);
				
				String materialStringName = materialString.substring(startPos, endPos);
				if (materialStringName.equals(materialName))
				{
					return new Mesh(stringToMaterial(materialString), boundingBox, triangles);
				}
			}
		}
		
		return null;
	}
	
	public static Scene parseScene(String fileName)
	{
		Scene result = new Scene();
		
		BufferedReader sceneBuffer = null;
		try
		{
			sceneBuffer = new BufferedReader(new FileReader(fileName));
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}
		
		try
		{
			if (sceneBuffer.ready())
			{
				String line = "";
				ArrayList<String> materialStrings = new ArrayList<String>();
				ArrayList<String> propertyStrings = new ArrayList<String>();
				ArrayList<String> cameraStrings = new ArrayList<String>();
				ArrayList<String> objectStrings = new ArrayList<String>();

				while ((line = sceneBuffer.readLine()) != null)
				{
					line = cleanString(line, true);
					
					// Parse materials
					if (line.equals("[MAT]"))
					{
						while ((line = sceneBuffer.readLine()) != null)
						{
							line = cleanString(line, true);
							if (!line.equals("[/MAT]"))
							{
								if (line.length() != 0)
								{
									materialStrings.add(line);
								}
							}
							else
							{
								break;
							}
						}
					}

					// Parse render properties
					if (line.equals("[PROP]"))
					{
						while ((line = sceneBuffer.readLine()) != null)
						{
							line = cleanString(line, true);
							if (!line.equals("[/PROP]"))
							{
								if (line.length() != 0)
								{
									propertyStrings.add(line);
								}
							}
							else
							{
								break;
							}
						}
					}
					
					// Parse camera
					if (line.equals("[CAM]"))
					{
						while ((line = sceneBuffer.readLine()) != null)
						{
							line = cleanString(line, true);
							if (!line.equals("[/CAM]"))
							{
								if (line.length() != 0)
								{
									cameraStrings.add(line);
								}
							}
							else
							{
								break;
							}
						}
					}

					// Parse objects
					if (line.equals("[OBJ]"))
					{
						while ((line = sceneBuffer.readLine()) != null)
						{
							line = cleanString(line, true);
							if (!line.equals("[/OBJ]"))
							{
								if (line.length() != 0)
								{
									objectStrings.add(line);
								}
							}
							else
							{
								break;
							}
						}
					}
				}

				sceneBuffer.close();

				// Assign render properties
				for (int i = 0; i < propertyStrings.size(); i++)
				{
					if (propertyStrings.get(i).indexOf("Width") != -1)
					{
						result.RenderProperties.Width = Integer.parseInt(propertyStrings.get(i).substring(propertyStrings.get(i).indexOf("=") + 1, propertyStrings.get(i).indexOf(";")));
					}
					else if (propertyStrings.get(i).indexOf("Height") != -1)
					{
						result.RenderProperties.Height = Integer.parseInt(propertyStrings.get(i).substring(propertyStrings.get(i).indexOf("=") + 1, propertyStrings.get(i).indexOf(";")));
					}
					else if (propertyStrings.get(i).indexOf("NumSamples") != -1)
					{
						result.RenderProperties.NumberOfSamples = Integer.parseInt(propertyStrings.get(i).substring(propertyStrings.get(i).indexOf("=") + 1, propertyStrings.get(i).indexOf(";")));
					}
					else if (propertyStrings.get(i).indexOf("BGC") != -1)
					{
						result.RenderProperties.BackgroundColour = stringToVec3(propertyStrings.get(i), 1);
					}
					else if (propertyStrings.get(i).indexOf("Cutoff") != -1)
					{
						result.RenderProperties.CutoffBrightness = Float.parseFloat(propertyStrings.get(i).substring(propertyStrings.get(i).indexOf("=") + 1, propertyStrings.get(i).indexOf(";")));
					}
					else if (propertyStrings.get(i).indexOf("Bias") != -1)
					{
						result.RenderProperties.NormalOffsetBias = Float.parseFloat(propertyStrings.get(i).substring(propertyStrings.get(i).indexOf("=") + 1, propertyStrings.get(i).indexOf(";")));
					}
				}
				
				// Assign camera
				if (cameraStrings.size() > 0)
				{
					result.Camera = stringToCamera(cameraStrings.get(0));
				}
				
				// Assign objects
				for (int i = 0; i < objectStrings.size(); i++)
				{
					Shape curObj = stringToShape(objectStrings.get(i), materialStrings);
					if (curObj != null)
					{
						result.Shapes.add(curObj);
					}
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
