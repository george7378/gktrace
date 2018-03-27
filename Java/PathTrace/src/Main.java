
import java.util.ArrayList;
import java.util.Scanner;
import java.io.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

class Main
{
	static Vec3 trace(Vec3 rayorig, Vec3 raydir, RenderProperties prop, ArrayList <Shape> shapes)
	{
		Vec3 rayColour = new Vec3(1);
		
		while (rayColour.findmax() > prop.cutoff)
		{
			//STEP 1: Look for intersections with shapes in the scene, find distance to nearest shape
			float dnear = Global.INFINITY;
			Shape shape = null;
			for (Shape shp : shapes)
			{
				Intersect in = shp.getIntersect(rayorig, raydir);
				if (in.intersection)
				{
					if (in.distance < dnear) 
					{
						dnear = in.distance;
						shape = shp;
					}
				}
			}
	
			//STEP 2: If there's no intersection return background colour, if light return emission
			if (shape == null)
			{
				float skyFactor = (float)Math.exp(raydir.dot(new Vec3(raydir.x, 0, raydir.z).normalize()) - 1);
				rayColour = rayColour.multiply(skyFactor > 0.5f ? prop.background.multiply(skyFactor) : prop.background.multiply(0.5f));
				break;
			}
			
			if (shape.material.st == SurfaceType.LGHT)
			{
				rayColour = rayColour.multiply(shape.material.emitColour);
				break;
			}
	
			//STEP 3: If there was an intersection, calculate the hit point, normal, colour
			Vec3 phit = rayorig.add(raydir.multiply(dnear));
			Vec3 chit = shape.getSurfaceColour(phit);
			rayColour = rayColour.multiply(chit);

			Vec3 nhit = shape.getNormal(phit).normalize();
			boolean inside = false;
			if (raydir.dot(nhit) > 0){nhit = nhit.invert(); inside = true;}

			//STEP 4: Calculate a new direction for the ray based on surface type
			if (shape.material.st == SurfaceType.DIFF)
			{
				rayorig = phit.add(nhit.multiply(prop.bias));
				raydir = Global.randomHemisphereDirection(nhit);
				continue;
			}

			if (shape.material.st == SurfaceType.SPEC)
			{
				rayorig = phit.add(nhit.multiply(prop.bias));
				raydir = Global.reflect(raydir, nhit).normalize();
				continue;
			}

			if (shape.material.st == SurfaceType.GLSP)
			{
				rayorig = phit.add(nhit.multiply(prop.bias));
				raydir = Global.randomConeDirection(Global.reflect(raydir, nhit).normalize(), shape.material.glossAngle);
				continue;
			}

			if (shape.material.st == SurfaceType.REFR)
			{
				float n = inside ? shape.material.ior : 1/shape.material.ior;
				float cosi = -nhit.dot(raydir), k = 1 - n*n*(1 - cosi*cosi);
				rayorig = k > 0 ? phit.subtract(nhit.multiply(prop.bias)) : phit.add(nhit.multiply(prop.bias));
				raydir = (k > 0 ? Global.refract(n, raydir, nhit, cosi, k) : Global.reflect(raydir, nhit)).normalize();
				continue;
			}	

			if (shape.material.st == SurfaceType.GLRF)
			{
				float n = inside ? shape.material.ior : 1/shape.material.ior;
				float cosi = -nhit.dot(raydir), k = 1 - n*n*(1 - cosi*cosi);
				rayorig = k > 0 ? phit.subtract(nhit.multiply(prop.bias)) : phit.add(nhit.multiply(prop.bias));
				raydir = Global.randomConeDirection((k > 0 ? Global.refract(n, raydir, nhit, cosi, k) : Global.reflect(raydir, nhit)).normalize(), shape.material.glossAngle);
				continue;
			}
		}
		
		return rayColour;
	}
	
	static void render(String filename, Scene scn)
	{
		BufferedImage image = new BufferedImage(scn.prp.width, scn.prp.height, BufferedImage.TYPE_INT_RGB);
		
		//Set up the camera
		float invWidth = 1/(float)scn.prp.width, invHeight = 1/(float)scn.prp.height;
		float aspectratio = scn.prp.width/(float)scn.prp.height;
		float angle = (float)Math.tan(Math.PI/2*scn.cam.fovy/180);
		float invSamples = 1/(float)scn.prp.numsamples;

		//Trace rays
		int rowCount = 0, prevPercent = 0;
		for (int y = 0; y < scn.prp.height; y++)
		{
			rowCount += 1;
			int curPercent = (int)(100*rowCount*invHeight);
			if (curPercent != prevPercent)
			{
				prevPercent = curPercent;
				System.out.println(prevPercent + "% complete");
			}

			for (int x = 0; x < scn.prp.width; x++) 
			{
				Vec3 thisPixel = new Vec3();
				for (int s = 0; s < scn.prp.numsamples; s++)
				{
					float xx = (2*((x + (float)Math.random())*invWidth) - 1)*angle*aspectratio;
					float yy = (1 - 2*((y + (float)Math.random())*invHeight))*angle;
					thisPixel = thisPixel.add(trace(scn.cam.pos, (scn.cam.left.multiply(xx).add(scn.cam.up.multiply(yy)).add(scn.cam.look)).normalize(), scn.prp, scn.shp));
				}
				thisPixel = thisPixel.multiply(invSamples);
				int col = ((int)(Math.min(1, thisPixel.x)*255) << 16) | ((int)(Math.min(1, thisPixel.y)*255) << 8) | (int)(Math.min(1, thisPixel.z)*255);
				image.setRGB(x, y, col);
			}
		}
		
		//Save result to a PNG image
		System.out.println("Saving image...");
		try
		{
			File imfile = new File(filename + ".png");
			ImageIO.write(image, "PNG", imfile);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public static void main(String [] args)
	{
		Scanner in = new Scanner(System.in);
		
		System.out.println("GKPathTrace [java] V1.0");
		
		System.out.print("Enter PNG filename: ");
		String filename = in.nextLine();
		System.out.print("Enter Scene filename: ");
		String scenename = in.nextLine();
		
		in.close();
		
		System.out.println("Loading...");
		Scene mainscene = Parse.parse_scene(scenename + ".txt");
		
		/*RenderProperties properties = new RenderProperties(640, 480, 50, new Vec3(), (float)4e-3, (float)1e-3);
		Camera camera = new Camera(new Vec3(-4, 3, -5), new Vec3(0.4f, -0.1f, 0.5f), new Vec3(0, 1, 0), 50);
		ArrayList <Shape> shapes = new ArrayList <Shape>();
		shapes.add(new Sphere(new Vec3(1, 1, -1), 1, new Material(new Vec3(0.8f), new Vec3(), 1, 0, SurfaceType.SPEC)));
		shapes.add(new Sphere(new Vec3(-1, 1, 1), 1, new Material(new Vec3(0.9f), new Vec3(), 1.8f, 0, SurfaceType.REFR)));
		shapes.add(new Sphere(new Vec3(2, 2, 2), 2, new Material(new Vec3(0.75f), new Vec3(), 1, 0, SurfaceType.DIFF)));
		shapes.add(new AABB(new Vec3(-1, 0, -1), new Vec3(-0.5f, 1, -0.5f), new Material(new Vec3(0.6f), new Vec3(), 1.8f, 0.1f, SurfaceType.SPEC)));
		shapes.add(new Plane(new Vec3(0, -1, 0), new Vec3(0, 0, 0), new Material(new Vec3(0.75f, 0.25f, 0), new Vec3(), 1, 0, SurfaceType.DIFF), 2.5f, 2.5f));
		shapes.add(new Plane(new Vec3(0, 1, 0), new Vec3(0, 5, 0), new Material(new Vec3(0.25f, 0.75f, 0.25f), new Vec3(), 1, 0, SurfaceType.DIFF), 1, 1));
		shapes.add(new Plane(new Vec3(-1, 0, 0), new Vec3(-5, 0, 0), new Material(new Vec3(), new Vec3(3), 1, 0, SurfaceType.LGHT), 1, 1));
		shapes.add(new Plane(new Vec3(1, 0, 0), new Vec3(5, 0, 0), new Material(new Vec3(0.75f, 0.25f, 0.25f), new Vec3(), 1, 0, SurfaceType.DIFF), 1, 1));
		shapes.add(new Plane(new Vec3(0, 0, -1), new Vec3(0, 0, -5), new Material(new Vec3(0.25f, 0.25f, 0.75f), new Vec3(), 1, 0, SurfaceType.DIFF), 1, 1));
		shapes.add(new Plane(new Vec3(0, 0, 1), new Vec3(0, 0, 5), new Material(new Vec3(0.5f), new Vec3(), 1, 0, SurfaceType.SPEC), 1, 1));*/
		
		System.out.println("Rendering...");
		render(filename, mainscene);
		
		System.out.println("Done");
	}
}
