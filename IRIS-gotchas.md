# IRIS Gotchas.

We will update this file with the lastest list of issues we have encountered when installing IRIS, which is a dependancy for this project.

## Table of Contents
- [Table of Contents](#table-of-contents)
- [Installing Ninja](#installing-ninja)
  - [OSX](#osx)
    - [Homebrew](#homebrew)
    - [Macports](#macports)
    - [Manual download](#manual-download)
  - [Linux](#linux)
    - [Package manager (recommended)](#package-manager-recommended)
    - [Install via pip (user-level)](#install-via-pip-user-level)
- [How to Check if Ninja is Installed](#how-to-check-if-ninja-is-installed)
- [cURL errors on OSX using Macports](#curl-errors-on-osx-using-macports)
- [CMake Issues:](#cmake-issues)


## Installing CMake

### Windows


1. The easiest way is using winget:

```
winget install --id Kitware.CMake --source winget
```

Now, open a new window and test to ensure it works:

```
cmake --version
```

It should give you a version number like 

```
cmake version 4.1.1
```

2. You can also download the official installer (MSI from Kitware)

[Download the Windows installer from the CMake site](https://cmake.org/download/).

During setup, choose the option to Add CMake to the system PATH (recommended).

Reopen your terminal and run cmake --version

3. Choco:

```
choco install cmake
cmake --version
```

4. Scoop 

```
scoop install cmake
cmake --version
```

## Installing Ninja 


### Windows


Here’s how to install the **Ninja** build system on Windows—pick your favorite method:

1. Winget (built into Win10/11)

```powershell
winget install -e --id Ninja-build.Ninja
```

Now, open a new window and test to ensure it works:

```
ninja --version
```

Winget installs the official Ninja binary published by the project. Note that you should also run this command to ensure ninja works with cmake:

```
cmake -S . -B build -G "Ninja"
```

2. Chocolatey

```powershell
choco install ninja
ninja --version
```

This pulls the `ninja` package from Chocolatey. ([Chocolatey Software][2])

3. Scoop (no admin needed)

```powershell
scoop install ninja
# (Alternative fork with GNU Make jobserver support:)
# scoop install ninja-kitware
ninja --version
```

Scoop provides both the upstream `ninja` and a Kitware fork (`ninja-kitware`). ([Scoop][3])

4. Download the portable EXE (no installer)

1. Download `ninja-win.zip` from the official releases.
2. Unzip `ninja.exe` somewhere on your PATH (e.g., `C:\Tools\ninja\`), or add that folder to PATH.
3. Run `ninja --version`.
   Ninja is just a single executable; no “install” is required. ([GitHub][4])


### OSX

#### Homebrew

   ```sh
   brew install ninja
   ```

   This installs Ninja and puts it in your PATH.

#### Macports

   ```sh
   sudo port install ninja 
   ```

#### Manual download

   * Go to the [Ninja GitHub releases page](https://github.com/ninja-build/ninja/releases).
   * Download the macOS binary (`ninja-mac.zip`).
   * Unzip and move the `ninja` executable into `/usr/local/bin` (or somewhere in your PATH).

### Linux

#### Package manager (recommended)

(Note that only the Debian install method has been tested.  The other methods have not but been, but are correct to the best of our knowledge.  Please [submit an Issue on GitHub](https://github.com/PublicisSapient/generate-iris-report/issues/new) if we are in error).

* **Debian/Ubuntu/Mint**

  ```bash
  sudo apt update && sudo apt install -y ninja-build
  ```
* **Fedora**

  ```bash
  sudo dnf install ninja-build
  ```
* **RHEL/CentOS** (8+ uses dnf; 7 needs EPEL)

  ```bash
  sudo dnf install ninja-build
  # or on 7:
  sudo yum install epel-release && sudo yum install ninja-build
  ```
* **Arch/Manjaro**

  ```bash
  sudo pacman -S ninja
  ```
* **openSUSE**

  ```bash
  sudo zypper install ninja
  ```
* **Alpine**

  ```bash
  sudo apk add ninja
  ```
* **Gentoo**

  ```bash
  sudo emerge dev-util/ninja
  ```
* **Void**

  ```bash
  sudo xbps-install -S ninja
  ```
* **Nix/NixOS**

  ```bash
  nix-env -iA nixpkgs.ninja   # or: nix-shell -p ninja
  ```

#### Install via pip (user-level)

This may be useful if you don't have root on the machine you are installing Ninja on.

```bash
python3 -m pip install --user ninja
# ensure ~/.local/bin is on PATH:
echo 'export PATH="$HOME/.local/bin:$PATH"' >> ~/.bashrc && source ~/.bashrc
```

## Installing vcpkg

Note: You will need to install Visual Studio Code before installing vcpkg


### Using Git (all operating systems)

```

git clone https://github.com/microsoft/vcpkg 
cd vcpkg
bootstrap-vcpkg.bat  # Windows
bootstrap-vcpkg.sh
```

You also need to set the shell variable VCPKG_ROOT:

```
# Windows: 
setx VCPKG_ROOT C:\path\to\git\vcpkg
# Linux/OSX:
export VCPKG_ROOT /path/to/git/vcpkg
```

Verify:

```
vcpkg version
```


### Windows 

* **Scoop** (no admin in powershell):

  ```
  scoop install vcpkg
  ```

  After that, verify install using:

  ```
  vcpkg version

  # Output should be 
  # vcpkg package management program version 2025-09-03-4580816534ed8fd9634ac83d46471440edd82dfe
  ```

You will then need to install the **C++ Build Tools** with the exact components you need

Pick **ARM64** (native on your Windows-on-ARM VM) and/or **x64** if you also want x64 builds. Run **one** of these:

**ARM64 toolchain:**

```powershell
Start-Process -Wait "C:\vs-setup\vs_BuildTools.exe" -ArgumentList @(
  "--passive","--norestart",
  "--add","Microsoft.VisualStudio.Workload.VCTools",
  "--add","Microsoft.VisualStudio.Component.VC.Tools.ARM64",
  "--add","Microsoft.VisualStudio.Component.Windows11SDK.22621",
  "--add","Microsoft.VisualStudio.Component.VC.CMake.Project"
)
```

**x64/x86 toolchain:**

```powershell
Start-Process -Wait "C:\vs-setup\vs_BuildTools.exe" -ArgumentList @(
  "--passive","--norestart",
  "--add","Microsoft.VisualStudio.Workload.VCTools",
  "--add","Microsoft.VisualStudio.Component.VC.Tools.x86.x64",
  "--add","Microsoft.VisualStudio.Component.Windows11SDK.22621",
  "--add","Microsoft.VisualStudio.Component.VC.CMake.Project"
)
```

Now, check if any VS 2022/Build Tools are installed

```
$vswhere = "C:\Program Files (x86)\Microsoft Visual Studio\Installer\vswhere.exe"

# human-readable
& $vswhere -products * -all -prerelease

# or JSON you can filter:
& $vswhere -products * -all -prerelease -format json |
  ConvertFrom-Json | Select displayName, productId, installationPath, installationVersion
```

Then, use the DevShell module (sets env in the current PowerShell)

# 1) Locate VS
$vswhere = "C:\Program Files (x86)\Microsoft Visual Studio\Installer\vswhere.exe"
$vs = & $vswhere -latest -products * -property installationPath

# 2) Load the DevShell module and enter the env you want
Import-Module "$vs\Common7\Tools\Microsoft.VisualStudio.DevShell.dll"
Enter-VsDevShell -VsInstallPath $vs -Arch arm64 -SkipAutomaticLocation   # or -Arch x64

# 3) Verify
cl /Bv

```



## How to Check if Ninja is Installed

Open **Terminal** and run:

```sh
ninja --version
```

* If installed, you’ll see something like:

  ```
  1.12.1
  ```
* If not installed, you’ll see an error such as:

  ```
  zsh: command not found: ninja
  ```


## cURL errors on OSX using Macports

Some users, when trying to build IRIS via `cmake -DBUILD_EXAMPLE_APP=ON --preset macos-release`, may get the following errors:

```
Trying https://sourceforge.net/projects/giflib/files///giflib-5.2.2.tar.gz/download?use_mirror=tenet 
error: curl: (60) SSL certificate problem: unable to get local issuer certificate
```

The `curl: (60) SSL certificate problem: unable to get local issuer certificate` error could mean your curl/vcpkg can’t find a trusted CA bundle when hitting SourceForge.

Using MacPorts, you'll need to use `curl-ca-bundle` or `openssl` packages instead of Homebrew’s `ca-certificates`.

1. **Install CA certificates via MacPorts**

   ```sh
   sudo port selfupdate
   sudo port install curl-ca-bundle
   ```

   That installs a system CA bundle, usually at:

   ```
   /opt/local/share/curl/curl-ca-bundle.crt
   ```

   You can verify:

   ```sh
   ls -l /opt/local/share/curl/curl-ca-bundle.crt
   ```

2. **Point your tools to the bundle**

   Set these environment variables before running `cmake`:

   ```sh
   export SSL_CERT_FILE=/opt/local/share/curl/curl-ca-bundle.crt
   export CURL_CA_BUNDLE=/opt/local/share/curl/curl-ca-bundle.crt
   ```

   That tells `curl` (and thus vcpkg) where to find the CA roots.

3. **Retry vcpkg**

   Go back into your IRIS directory and re-run:

   ```sh
   cmake -DBUILD_EXAMPLE_APP=ON --preset macos-release
   ```

4. **(Optional) Make it permanent**

   If you don’t want to set env vars every time, add the lines to your shell config:

   * For Zsh:

     ```sh
     export SSL_CERT_FILE=/opt/local/share/curl/curl-ca-bundle.crt
     export CURL_CA_BUNDLE=/opt/local/share/curl/curl-ca-bundle.crt
     export GIT_SSL_CAINFO=/opt/local/etc/openssl/cert.pem
     ```

5. **Test**

   Run:

   ```
   curl -I https://github.com
   ```

   If this fix works, you should read output like this:

   ```
   HTTP/1.1 200 OK
   Date: Tue, 16 Sep 2025 17:52:59 GMT
   Content-Type: text/html; charset=utf-8

   .... more data ...
   ```

   If it doesn't, you'll probably read something like this:

   ```
   curl: (60) SSL certificate problem: self-signed certificate in certificate chain More details here: https://curl.se/docs/sslcerts.html curl failed to verify the legitimacy of the server and therefore could not establish a secure connection to it. To learn more about this situation and how to fix it, please visit the webpage mentioned above.
   ```

If the latter is the case, then switch to `certsync` (this is especially recommended on corp networks)

1. Deactivate the conflicting bundle:

   ```bash
   sudo port -f deactivate curl-ca-bundle
   ```

2. Install and run certsync:

   ```bash
   sudo port install certsync
   sudo certsync
   # writes: /opt/local/etc/openssl/cert.pem
   ```

3. Re-Test:

   ```bash
   /opt/local/bin/curl -I https://github.com
   ```

> If that works, add those `export` lines to your `~/.zshrc`.

You can keep `curl-ca-bundle` **deactivated**. If you ever re-activate it, it will conflict with `certsync` again.


## CMake Issues:

When entering the command `cmake --preset linux-release` (or `windows-release` or `macos-release`), you may come across this error: 

```
CMake Error: Could not read presets from /path/to/git/IRIS: Unrecognized "version" field
```

That error means your **CMake is older than the schema version** used by IRIS’s `CMakePresets.json`. CMake parses the presets **before** it reads `CMakeLists.txt`, so an old CMake can’t understand the `"version"` field and bails out. Updating CMake will fix the issue.