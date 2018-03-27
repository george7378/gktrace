import bpy

#Triangulate the mesh before exporting it (edit mode -> CTRL + T)
#Delete all objects you don't want to export, then save your .blend file, close blender and re-open the .blend file - 
#this is very important as the vertex buffer can retain old vertices if you don't restart blender.

#Output path - change!
output_data = open("C:/Users/****/Documents/exportedmesh.txt", "w")

output_data.write("[VERTEX]\n")
for mesh in bpy.data.meshes:
    for vertex in mesh.vertices:
        output_data.write("(" + str(vertex.co.x) + "," + str(vertex.co.y) + "," + str(vertex.co.z) + ")\n")
output_data.write("[/VERTEX]\n")

output_data.write("[INDEX]\n")
for mesh in bpy.data.meshes:
    for poly in mesh.polygons:
        output_data.write(str(poly.vertices[0]) + "," + str(poly.vertices[1]) + "," + str(poly.vertices[2]) + "\n")
output_data.write("[/INDEX]\n")

output_data.write("[NORMAL]\n")
for mesh in bpy.data.meshes:
    for poly in mesh.polygons:
        output_data.write("(" + str(poly.normal[0]) + "," + str(poly.normal[1]) + "," + str(poly.normal[2]) + ")\n")
output_data.write("[/NORMAL]\n")

output_data.write("[BBOX]\n")
for obj in bpy.data.objects:
	all_x = [obj.bound_box[0][0], obj.bound_box[1][0], obj.bound_box[2][0], obj.bound_box[3][0], obj.bound_box[4][0], obj.bound_box[5][0], obj.bound_box[6][0], obj.bound_box[7][0]]
	all_y = [obj.bound_box[0][1], obj.bound_box[1][1], obj.bound_box[2][1], obj.bound_box[3][1], obj.bound_box[4][1], obj.bound_box[5][1], obj.bound_box[6][1], obj.bound_box[7][1]]
	all_z = [obj.bound_box[0][2], obj.bound_box[1][2], obj.bound_box[2][2], obj.bound_box[3][2], obj.bound_box[4][2], obj.bound_box[5][2], obj.bound_box[6][2], obj.bound_box[7][2]]
	output_data.write("(" + str(min(all_x)) + "," + str(min(all_y)) + "," + str(min(all_z)) + ")")
	output_data.write(",(" + str(max(all_x)) + "," + str(max(all_y)) + "," + str(max(all_z)) + ")\n")
output_data.write("[/BBOX]")

output_data.close()
