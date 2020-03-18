# ObjToReflexMeshConverter
A Java program to convert Wavefront .obj files to Reflex .mesh files. This program requires at least Java 1.3.

## About
The conversion is based on my reverse engineering of the Reflex .mesh file format. It supports vertex normals, vertex texture coordinates and groups. Curious mappers can view an exploded and annotated view of the .mesh file structure in "Reflex Mesh File Structure.txt".

## Usage
```
File conversion:   
java -jar ObjToReflexMeshConverter "C:\Objects\container.obj" "C:\Meshes\container.mesh"
ObjToReflexMeshConverter.exe "C:\Objects\container.obj" "C:\Meshes\container.mesh"

Folder conversion: 
java -jar ObjToReflexMeshConverter "C:\Objects" "C:\Meshes"
ObjToReflexMeshConverter.exe "C:\Objects" "C:\Meshes"
```
