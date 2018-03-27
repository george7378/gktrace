//#define D3D_DEBUG_INFO
#define _USE_MATH_DEFINES
#include <windows.h>
#include <math.h>
#include <string>
#include <random>
#include <d3d9.h>
#include <d3dx9.h>

using namespace std;

#include "dx_core.h"
#include "classes.h"

ScreenQuad screenSprite;
DrawableTex2D currentImage, prevImage;

#pragma comment (lib, "d3d9.lib")		//Direct3D 9
#pragma comment (lib, "d3dx9.lib")		//DirectX 9

//Initialisation
bool initResources()
{
	//Post-procesing initialisation
	if (!currentImage.CreateResources(WIDTH, HEIGHT, D3DFMT_X8R8G8B8)){return false;}
	if (!prevImage.CreateResources(WIDTH, HEIGHT, D3DFMT_X8R8G8B8)){return false;}
	if (!screenSprite.CreateResources()){return false;}

	//Shader initialisation
	if (FAILED(D3DXCreateEffectFromFile(d3ddev, "shaders/PathTrace.fx", NULL, NULL, D3DXSHADER_DEBUG, NULL, &globalLightingEffect, NULL)))
	{MessageBox(NULL, "Unable to read PathTrace.fx", "Error", MB_OK); return false;}

	return true;
}
bool initD3D(HWND hWnd)
{
	//Create D3D9
    d3d = Direct3DCreate9(D3D_SDK_VERSION);
	if (d3d == NULL){return false;}

	//Check current display mode
	D3DDISPLAYMODE d3ddm; 
	if (FAILED(d3d->GetAdapterDisplayMode(D3DADAPTER_DEFAULT, &d3ddm))){return false;}

	//DirectX parameters
    ZeroMemory(&d3dpp, sizeof(d3dpp));
	d3dpp.BackBufferFormat = d3ddm.Format;
	d3dpp.BackBufferWidth = WIDTH;
	d3dpp.BackBufferHeight = HEIGHT;
	d3dpp.BackBufferCount = 1;
    d3dpp.Windowed = TRUE;
    d3dpp.SwapEffect = D3DSWAPEFFECT_DISCARD;
	d3dpp.MultiSampleType = D3DMULTISAMPLE_4_SAMPLES;
	d3dpp.MultiSampleQuality = 0;
	d3dpp.hDeviceWindow = hWnd;
    d3dpp.EnableAutoDepthStencil = TRUE;
	d3dpp.AutoDepthStencilFormat = D3DFMT_D24S8;
	d3dpp.Flags = 0;
	d3dpp.FullScreen_RefreshRateInHz = 0;
	d3dpp.PresentationInterval = D3DPRESENT_INTERVAL_ONE;

	//Check shader/vertex processing caps
	D3DCAPS9 d3dCaps;
	if(FAILED(d3d->GetDeviceCaps(D3DADAPTER_DEFAULT, D3DDEVTYPE_HAL, &d3dCaps))){return false;}
	DWORD VertexProcessingMethod = d3dCaps.VertexProcessingCaps != 0 ? D3DCREATE_HARDWARE_VERTEXPROCESSING : D3DCREATE_SOFTWARE_VERTEXPROCESSING;

	if (d3dCaps.VertexShaderVersion < D3DVS_VERSION(2, 0) || d3dCaps.PixelShaderVersion < D3DPS_VERSION(2, 0))
	{MessageBox(NULL, "Default video adapter does not support shader version 2.0", "Error", MB_OK); return false;}

	//Device creation
    if(FAILED(d3d->CreateDevice(D3DADAPTER_DEFAULT, D3DDEVTYPE_HAL, hWnd, VertexProcessingMethod, &d3dpp, &d3ddev))){return false;}

	//Resource/device initiation
	if (!initResources()){return false;}

	return true;
}

//Device handling
bool onLostDevice()
{
	currentImage.DeleteResources();
	prevImage.DeleteResources();

	if (FAILED(globalLightingEffect->OnLostDevice())){return false;}

	return true;
}
bool onResetDevice()
{
	if (!currentImage.CreateResources(WIDTH, HEIGHT, D3DFMT_X8R8G8B8)){return false;}
	if (!prevImage.CreateResources(WIDTH, HEIGHT, D3DFMT_X8R8G8B8)){return false;}

	if (FAILED(globalLightingEffect->OnResetDevice())){return false;}

	return true;
}
bool isDeviceLost()
{
	HRESULT hr = d3ddev->TestCooperativeLevel();

	if (hr == D3DERR_DEVICELOST)
	{
		Sleep(100);
		return true;
	}

	else if (hr == D3DERR_DEVICENOTRESET)
	{
		if (!onLostDevice()){MessageBox(NULL, "Can't prepare lost device", "Error", MB_OK); return true;}
		if (FAILED(d3ddev->Reset(&d3dpp))){MessageBox(NULL, "Can't reset the present parameters for the device", "Error", MB_OK); return true;}
		if (!onResetDevice()){MessageBox(NULL, "Can't reset the device", "Error", MB_OK); return true;}
	}

	return false;
}

//Draw calls
bool draw_Trace_Iteration()
{
	float curRandomMod = line(gen) + 1;
	globalLightingEffect->SetValue("scene_Width", &WIDTH, sizeof(unsigned));
	globalLightingEffect->SetValue("scene_Height", &HEIGHT, sizeof(unsigned));
	globalLightingEffect->SetValue("scene_CamPos", &camPos, sizeof(D3DXVECTOR3));
	globalLightingEffect->SetValue("scene_CamLook", &camLook, sizeof(D3DXVECTOR3));
	
	globalLightingEffect->SetValue("RandomModulator", &curRandomMod, sizeof(float));
	globalLightingEffect->SetValue("NumberOfIterations", &nIterations, sizeof(unsigned));
	globalLightingEffect->SetTexture("PrevImage", prevImage.drawTexture);

	globalLightingEffect->SetTechnique("PathTrace");
	UINT numPasses = 0;
	if (FAILED(globalLightingEffect->Begin(&numPasses, 0))){return false;}
	for (UINT i = 0; i < numPasses; i++)
	{
	if (FAILED(globalLightingEffect->BeginPass(i))){return false;}
		screenSprite.Render();
	if (FAILED(globalLightingEffect->EndPass())){return false;}
	}
	if (FAILED(globalLightingEffect->End())){return false;}

	nIterations += 1;
	
	return true;
}
bool draw_Screen_Texture()
{
	globalLightingEffect->SetTexture("ScreenTexture", currentImage.drawTexture);

	globalLightingEffect->SetTechnique("None");
	UINT numPasses = 0;
	if (FAILED(globalLightingEffect->Begin(&numPasses, 0))){return false;}
	for (UINT i = 0; i < numPasses; i++)
	{
	if (FAILED(globalLightingEffect->BeginPass(i))){return false;}
		screenSprite.Render();
	if (FAILED(globalLightingEffect->EndPass())){return false;}
	}
	if (FAILED(globalLightingEffect->End())){return false;}
	
	return true;
}

//Rendering
void preRender()
{
	if (GetAsyncKeyState(0x57)){camPos.x += 0.02f; nIterations = 0;}
	if (GetAsyncKeyState(0x53)){camPos.x -= 0.02f; nIterations = 0;}
}
void renderFrame()
{
//PRERENDERING
	preRender();
	LPDIRECT3DSURFACE9 backBuffer = NULL;
	d3ddev->GetRenderTarget(0, &backBuffer);

//PASS ONE: render next frame
	currentImage.SetAsTarget();

	d3ddev->Clear(0, NULL, D3DCLEAR_TARGET | D3DCLEAR_ZBUFFER | D3DCLEAR_STENCIL, D3DCOLOR_XRGB(0, 0, 0), 1, 0);
	d3ddev->BeginScene();

		draw_Trace_Iteration();

	d3ddev->EndScene();

//PASS TWO:
	d3ddev->SetRenderTarget(0, backBuffer);
	SAFE_RELEASE(&backBuffer);

	d3ddev->Clear(0, NULL, D3DCLEAR_TARGET | D3DCLEAR_ZBUFFER | D3DCLEAR_STENCIL, D3DCOLOR_XRGB(0, 0, 0), 1, 0);
	d3ddev->BeginScene();

		draw_Screen_Texture();

	d3ddev->EndScene();

	d3ddev->Present(NULL, NULL, NULL, NULL);

	d3ddev->StretchRect(currentImage.drawSurface, NULL, prevImage.drawSurface, NULL, D3DTEXF_NONE);
}

//Cleaning
void cleanD3D()
{
	SAFE_RELEASE(&globalLightingEffect);	
	SAFE_RELEASE(&d3ddev);					
	SAFE_RELEASE(&d3d);	

	currentImage.DeleteResources();
	prevImage.DeleteResources();

	screenSprite.DeleteResources();
}

//Win32
LRESULT CALLBACK WndProc(HWND hwnd, UINT msg, WPARAM wParam, LPARAM lParam)
{
	switch(msg)
    {
		case WM_KEYDOWN:
			switch(LOWORD(wParam))
			{
				
			}
			break;

        case WM_DESTROY:
            PostQuitMessage(WM_QUIT);
			break;
	}
	
	return DefWindowProc(hwnd, msg, wParam, lParam);
}
int WINAPI WinMain(HINSTANCE hInstance, HINSTANCE hPrevInstance, LPSTR lpCmdLine, int nCmdShow)
{  
    //Step 1: Registering the Window Class
	WNDCLASSEX wc;
    ZeroMemory(&wc, sizeof(WNDCLASSEX));
    wc.cbSize = sizeof(WNDCLASSEX);
    wc.lpfnWndProc = WndProc;
    wc.hInstance = hInstance;
	wc.hIcon = LoadIcon(NULL, IDI_APPLICATION);
    wc.hCursor = LoadCursor(NULL, IDC_ARROW);
    wc.lpszClassName = "WindowClass";

    if(!RegisterClassEx(&wc))
	{MessageBox(NULL, "The window could not be registered!", "Error", MB_ICONEXCLAMATION | MB_OK); return 0;}

    //Step 2: Creating the Window
	RECT clientRect = {0, 0, WIDTH, HEIGHT};
	AdjustWindowRect(&clientRect, WS_CAPTION | WS_SYSMENU | WS_MINIMIZEBOX, FALSE);
    hwnd = CreateWindow("WindowClass", "HLSLPathTrace", WS_CAPTION | WS_SYSMENU | WS_MINIMIZEBOX, 0, 0, clientRect.right - clientRect.left, clientRect.bottom - clientRect.top, NULL, NULL, hInstance, NULL);
	if(hwnd == NULL){MessageBox(NULL, "Window Creation Failed!", "Error!", MB_OK); return 0;}

	if (!initD3D(hwnd)){MessageBox(NULL, "Direct3D failed to initialise", "Error", MB_OK); return 0;}

	ShowWindow(hwnd, nCmdShow);

    //Step 3: The Message Loop
	MSG Msg;
	while (TRUE)
    {
		while (PeekMessage(&Msg, NULL, 0, 0, PM_REMOVE))
        {
            TranslateMessage(&Msg);
            DispatchMessage(&Msg);
        }

        if (Msg.message == WM_QUIT)
            break;

		if (!isDeviceLost()){renderFrame();}
    }

	    cleanD3D();
	    return Msg.wParam;
}
