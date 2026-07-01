@echo on
echo =======================================================
echo     DANG BIEN DICH TASK 6 (OPENSSL 4.0.0 TAI D:\utecrypto)
echo =======================================================

C:\msys64\mingw64\bin\g++.exe -std=c++17 -O2 -Wall -Wextra -D_WIN32_WINNT=0x0A00 -Iinclude -ID:\utecrypto\include src\evp_aes_direct_tool.cpp src\evp_aes_direct_main.cpp D:\utecrypto\lib64\libcrypto.a -lws2_32 -lcrypt32 -static -static-libgcc -static-libstdc++ -o evp_aes_direct_lab.exe

if %ERRORLEVEL% EQU 0 (
    echo - Thanh cong EXE
) else (
    echo - LOI EXE
)

C:\msys64\mingw64\bin\g++.exe -std=c++17 -O2 -Wall -Wextra -D_WIN32_WINNT=0x0A00 -DEVPAESDIRECT_EXPORTS -Iinclude -ID:\utecrypto\include src\evp_aes_direct_tool.cpp src\evp_aes_direct_c_api.cpp -shared D:\utecrypto\lib64\libcrypto.a -lws2_32 -lcrypt32 -static -static-libgcc -static-libstdc++ -o evpaesdirect.dll -Wl,--out-implib,libevpaesdirect.dll.a

if %ERRORLEVEL% EQU 0 (
    echo - Thanh cong DLL
) else (
    echo - LOI DLL
)
