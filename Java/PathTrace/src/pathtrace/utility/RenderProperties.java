package pathtrace.utility;

public class RenderProperties
{
	public int Width, Height, NumberOfSamples;
	public Vec3 BackgroundColour;
	public float CutoffBrightness, NormalOffsetBias;

	public RenderProperties()
	{
		Width = 640;
		Height = 480;
		NumberOfSamples = 1;
		BackgroundColour = new Vec3();
		CutoffBrightness = (float)4e-3;
		NormalOffsetBias = (float)1e-3;
	}
	
	public RenderProperties(int width, int height, int numberOfSamples, Vec3 backgroundColour, float cutoffBrightness, float normalOffsetBias)
	{
		Width = width;
		Height = height;
		NumberOfSamples = numberOfSamples;
		BackgroundColour = backgroundColour; 
		CutoffBrightness = cutoffBrightness;
		NormalOffsetBias = normalOffsetBias;
	}
}
