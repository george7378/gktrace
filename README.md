# gktrace
GKTrace is a collection of two programs - a traditional recursive ray tracer (C++ only) along with an iterative Monte Carlo path tracer (C++ and Java - Java version supports 3D meshes). I hope you might find them useful if you want to learn more about these rendering techniques yourself!

The Java version is the primary version as it supports 3D meshes and seems to outperform the C++ program. Note: it does not yet support textures. Also included is an extra GPU path tracer using HLSL. With this you can see the lighting process happening in real-time.

You can also find a script to export meshes from Blender in the format accepted by the Java program.

![Ring mesh](https://raw.githubusercontent.com/george7378/gktrace/master/_img/1.png)
![Path tracing with textures](https://raw.githubusercontent.com/george7378/gktrace/master/_img/2.png)
![Diamond mesh](https://raw.githubusercontent.com/george7378/gktrace/master/_img/3.png)
![Enclosed box](https://raw.githubusercontent.com/george7378/gktrace/master/_img/4.png)
![Ray tracing with textures](https://raw.githubusercontent.com/george7378/gktrace/master/_img/5.png)
![HLSL GPU tracer](https://raw.githubusercontent.com/george7378/gktrace/master/_img/6.png)
