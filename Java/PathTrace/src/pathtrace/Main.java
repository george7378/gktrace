package pathtrace;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

import javax.imageio.ImageIO;

import pathtrace.geometry.Shape;
import pathtrace.geometry.enums.SurfaceType;
import pathtrace.utility.Intersect;
import pathtrace.utility.Parse;
import pathtrace.utility.RenderProperties;
import pathtrace.utility.Scene;
import pathtrace.utility.Vec3;

public class Main
{
	private static Vec3 trace(Vec3 rayOrigin, Vec3 rayDirection, RenderProperties renderProperties, ArrayList<Shape> shapes)
	{
		Vec3 rayColour = new Vec3(1);
		
		while (rayColour.findMax() > renderProperties.CutoffBrightness)
		{
			// STEP 1: Look for intersections with shapes in the scene, find distance to nearest shape
			float dNear = Float.MAX_VALUE;
			
			Shape nearestShape = null;
			for (Shape shape : shapes)
			{
				Intersect intersect = shape.calculateIntersect(rayOrigin, rayDirection);
				if (intersect.Intersection && intersect.Distance < dNear)
				{
					dNear = intersect.Distance;
					nearestShape = shape;
				}
			}
	
			// STEP 2: If there's no intersection return background colour, if light return emission
			if (nearestShape == null)
			{
				float skyFactor = (float)Math.exp(rayDirection.dot(new Vec3(rayDirection.X, 0, rayDirection.Z).normalize()) - 1);
				rayColour = rayColour.multiply(skyFactor > 0.5f ? renderProperties.BackgroundColour.multiply(skyFactor) : renderProperties.BackgroundColour.multiply(0.5f));
				
				break;
			}
			
			if (nearestShape.Material.Type == SurfaceType.LGHT)
			{
				rayColour = rayColour.multiply(nearestShape.Material.EmissionColour);
				
				break;
			}
	
			// STEP 3: If there was an intersection, calculate the hit point, normal, colour
			Vec3 pHit = rayOrigin.add(rayDirection.multiply(dNear));
			Vec3 cHit = nearestShape.calculateSurfaceColour(pHit);
			Vec3 nHit = nearestShape.calculateNormal(pHit).normalize();

			rayColour = rayColour.multiply(cHit);
			
			boolean inside = false;
			if (rayDirection.dot(nHit) > 0)
			{
				nHit = nHit.invert();
				inside = true;
			}

			// STEP 4: Calculate a new direction for the ray based on surface type
			switch (nearestShape.Material.Type)
			{
				case DIFF:
					rayOrigin = pHit.add(nHit.multiply(renderProperties.NormalOffsetBias));
					rayDirection = Vec3.randomHemisphereDirection(nHit);
					break;
					
				case SPEC:
					rayOrigin = pHit.add(nHit.multiply(renderProperties.NormalOffsetBias));
					rayDirection = rayDirection.reflect(nHit).normalize();
					break;
					
				case GLSP:
					rayOrigin = pHit.add(nHit.multiply(renderProperties.NormalOffsetBias));
					rayDirection = Vec3.randomConeDirection(rayDirection.reflect(nHit).normalize(), nearestShape.Material.GlossAngle);
					break;
					
				case REFR:
					float etaRefractive = inside ? nearestShape.Material.RefractiveIndex : 1/nearestShape.Material.RefractiveIndex;
					float cosIRefractive = -nHit.dot(rayDirection);
					float kRefractive = 1 - etaRefractive*etaRefractive*(1 - cosIRefractive*cosIRefractive);
					rayOrigin = kRefractive > 0 ? pHit.subtract(nHit.multiply(renderProperties.NormalOffsetBias)) : pHit.add(nHit.multiply(renderProperties.NormalOffsetBias));
					rayDirection = (kRefractive > 0 ? rayDirection.refract(etaRefractive, nHit, cosIRefractive, kRefractive) : rayDirection.reflect(nHit)).normalize();
					break;
					
				case GLRF:
					float etaGlossyRefractive = inside ? nearestShape.Material.RefractiveIndex : 1/nearestShape.Material.RefractiveIndex;
					float cosIGlossyRefractive = -nHit.dot(rayDirection);
					float kGlossyRefractive = 1 - etaGlossyRefractive*etaGlossyRefractive*(1 - cosIGlossyRefractive*cosIGlossyRefractive);
					rayOrigin = kGlossyRefractive > 0 ? pHit.subtract(nHit.multiply(renderProperties.NormalOffsetBias)) : pHit.add(nHit.multiply(renderProperties.NormalOffsetBias));
					rayDirection = Vec3.randomConeDirection((kGlossyRefractive > 0 ? rayDirection.refract(etaGlossyRefractive, nHit, cosIGlossyRefractive, kGlossyRefractive) : rayDirection.reflect(nHit)).normalize(), nearestShape.Material.GlossAngle);
					break;
			
				default:
					break;
			}
		}
		
		return rayColour;
	}
	
	private static void render(String fileName, Scene scene)
	{
		BufferedImage image = new BufferedImage(scene.RenderProperties.Width, scene.RenderProperties.Height, BufferedImage.TYPE_INT_RGB);
		
		// Set up the camera
		float invWidth = 1/(float)scene.RenderProperties.Width;
		float invHeight = 1/(float)scene.RenderProperties.Height;
		float aspectRatio = scene.RenderProperties.Width/(float)scene.RenderProperties.Height;
		float angle = (float)Math.tan(Math.PI/2*scene.Camera.FieldOfViewY/180);
		float invSamples = 1/(float)scene.RenderProperties.NumberOfSamples;

		// Trace rays
		int rowCount = 0;
		int prevPercent = 0;
		for (int y = 0; y < scene.RenderProperties.Height; y++)
		{
			rowCount += 1;
			int curPercent = (int)(100*rowCount*invHeight);
			if (curPercent != prevPercent)
			{
				prevPercent = curPercent;
				System.out.println(prevPercent + "% complete");
			}

			for (int x = 0; x < scene.RenderProperties.Width; x++) 
			{
				Vec3 thisPixel = new Vec3();
				for (int s = 0; s < scene.RenderProperties.NumberOfSamples; s++)
				{
					float xx = (2*((x + (float)Math.random())*invWidth) - 1)*angle*aspectRatio;
					float yy = (1 - 2*((y + (float)Math.random())*invHeight))*angle;
					
					thisPixel = thisPixel.add(trace(scene.Camera.Position, (scene.Camera.LeftDirection.multiply(xx).add(scene.Camera.UpDirection.multiply(yy)).add(scene.Camera.LookDirection)).normalize(), scene.RenderProperties, scene.Shapes));
				}
				
				thisPixel = thisPixel.multiply(invSamples);
				
				int col = ((int)(Math.min(1, thisPixel.X)*255) << 16) | ((int)(Math.min(1, thisPixel.Y)*255) << 8) | (int)(Math.min(1, thisPixel.Z)*255);
				image.setRGB(x, y, col);
			}
		}
		
		// Save result to a PNG image
		System.out.println("Saving image...");
		try
		{
			File imageFile = new File(fileName + ".png");
			ImageIO.write(image, "PNG", imageFile);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public static void main(String [] args)
	{
		Scanner inputScanner = new Scanner(System.in);

		System.out.print("Enter PNG filename: ");
		String fileName = inputScanner.nextLine();
		
		System.out.print("Enter Scene filename: ");
		String sceneName = inputScanner.nextLine();
		
		inputScanner.close();
		
		System.out.println("Loading...");
		Scene scene = Parse.parseScene(sceneName + ".txt");

		System.out.println("Rendering...");
		render(fileName, scene);
		
		System.out.println("Done");
	}
}
