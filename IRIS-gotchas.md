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


## Installing Ninja 

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