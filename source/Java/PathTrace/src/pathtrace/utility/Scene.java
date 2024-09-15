package pathtrace.utility;

import java.util.ArrayList;

import pathtrace.geometry.Shape;

public class Scene
{
	public RenderProperties RenderProperties;
	public Camera Camera;
	public ArrayList<Shape> Shapes;
	
	public Scene()
	{
		RenderProperties = new RenderProperties();
		Camera = new Camera();
		Shapes = new ArrayList<Shape>();
	}
}
