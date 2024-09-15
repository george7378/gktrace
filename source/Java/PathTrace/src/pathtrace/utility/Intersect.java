package pathtrace.utility;

public class Intersect
{
	public boolean Intersection;
	public float Distance;
		
	public Intersect()
	{
		Intersection = false;
		Distance = 0;
	}
	
	public Intersect(boolean intersection, float distance)
	{
		Intersection = intersection;
		Distance = distance;
	}
}
