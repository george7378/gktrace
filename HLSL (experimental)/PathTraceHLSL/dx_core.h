#ifndef CORE_H
#define CORE_H

//D3D and Windows
unsigned WIDTH = 640;
unsigned HEIGHT = 480;
HWND hwnd = NULL;		
LPDIRECT3D9 d3d = NULL;				
LPDIRECT3DDEVICE9 d3ddev = NULL;	
D3DPRESENT_PARAMETERS d3dpp;

//Effects
LPD3DXEFFECT globalLightingEffect = NULL;

//Path tracing parameters
D3DXVECTOR3 camPos(-4, 3, -5);
D3DXVECTOR3 camLook(0.4f, -0.1f, 0.5f);
unsigned nIterations = 0;

//Misc
random_device rd;
default_random_engine gen(rd());
uniform_real_distribution <float> line(0, 1);

template <class T> void SAFE_RELEASE(T **ppT) //Release and nullify pointers
{
    if (*ppT)
	{
        (*ppT)->Release();
        *ppT = NULL;
    }
}

#endif
