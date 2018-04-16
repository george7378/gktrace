package pathtrace.utility;

public class Camera
{
	public final Vec3 Position, LookDirection, UpDirection, LeftDirection;
	public final float FieldOfViewY;

	public Camera()
	{
		Position = new Vec3(0, 1, 0);
		LookDirection = new Vec3(0, 0, 1);
		UpDirection = new Vec3(0, 1, 0);
		LeftDirection = UpDirection.cross(LookDirection).normalize();
		FieldOfViewY = 40;
	}
	
	public Camera(Vec3 position, Vec3 lookDirection, Vec3 upDirection, float fieldOfViewY)
	{
		Position = position;
		LookDirection = lookDirection.normalize();
		LeftDirection = upDirection.cross(LookDirection).normalize();
		UpDirection = LookDirection.cross(LeftDirection).normalize();
		FieldOfViewY = fieldOfViewY;
	}
}
