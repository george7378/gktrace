#ifndef CLASSES_H
#define CLASSES_H

//Texture
class DrawableTex2D
{
public:
	LPDIRECT3DSURFACE9 drawSurface;
	LPDIRECT3DTEXTURE9 drawTexture;	
	
	DrawableTex2D(){};	

	bool CreateResources(unsigned width, unsigned height, D3DFORMAT format)
	{
		if (FAILED(d3ddev->CreateTexture(width, height, 1, D3DUSAGE_RENDERTARGET, format, D3DPOOL_DEFAULT, &drawTexture, NULL))){return false;}
		if (FAILED(drawTexture->GetSurfaceLevel(0, &drawSurface))){return false;}
		
		return true;
	}

	bool SetAsTarget()
	{
		d3ddev->SetRenderTarget(0, drawSurface);
		
		return true;
	}

	void DeleteResources()
	{
		SAFE_RELEASE(&drawTexture);
		SAFE_RELEASE(&drawSurface);
	}
};

//Mesh
struct ScreenQuadVertex
{
	D3DXVECTOR4 pos;
	D3DXVECTOR2 texCoords;
};
class ScreenQuad
{
private:
	LPDIRECT3DVERTEXBUFFER9 vertexBuffer;
	LPDIRECT3DVERTEXDECLARATION9 vertexDecleration;
public:

	ScreenQuad(){};

	bool CreateResources()
	{
		ScreenQuadVertex vertices[] =
		{
			{D3DXVECTOR4(-1, -1, 0, 1), D3DXVECTOR2(0, 1)},		//Bottom Left
			{D3DXVECTOR4(-1, 1, 0, 1), D3DXVECTOR2(0, 0)},		//Top Left
			{D3DXVECTOR4(1, -1, 0, 1), D3DXVECTOR2(1, 1)},		//Bottom Right
			{D3DXVECTOR4(1, 1, 0, 1), D3DXVECTOR2(1, 0)},		//Top Right
		};
		for (unsigned i = 0; i < 4; i++)
		{
			vertices[i].pos.x -= 1.0f/WIDTH;
			vertices[i].pos.y += 1.0f/HEIGHT;
		}
		
		D3DVERTEXELEMENT9 elements[] =
		{
			{0, sizeof(float)*0, D3DDECLTYPE_FLOAT4, D3DDECLMETHOD_DEFAULT, D3DDECLUSAGE_POSITION, 0},
			{0, sizeof(float)*4, D3DDECLTYPE_FLOAT2, D3DDECLMETHOD_DEFAULT, D3DDECLUSAGE_TEXCOORD, 0},
			D3DDECL_END()
		};
		vertexDecleration = 0;
		if (FAILED(d3ddev->CreateVertexDeclaration(elements, &vertexDecleration))){return false;}
		if (FAILED(d3ddev->CreateVertexBuffer(4*sizeof(ScreenQuadVertex), 0, 0, D3DPOOL_MANAGED, &vertexBuffer, 0))){return false;}
		
		void* pVoid;
		if (FAILED(vertexBuffer->Lock(0, 0, (void**)&pVoid, 0))){return false;}
		memcpy(pVoid, vertices, sizeof(vertices));
		if (FAILED(vertexBuffer->Unlock())){return false;}

		return true;
	}

	bool Render()
	{
		if (FAILED(d3ddev->SetVertexDeclaration(vertexDecleration))){return false;}
		if (FAILED(d3ddev->SetStreamSource(0, vertexBuffer, 0, sizeof(ScreenQuadVertex)))){return false;}
		
		if (FAILED(d3ddev->DrawPrimitive(D3DPT_TRIANGLESTRIP, 0, 2))){return false;}

		return true;
	}

	void DeleteResources()
	{
		SAFE_RELEASE(&vertexBuffer);
		SAFE_RELEASE(&vertexDecleration);
	}
};

#endif
