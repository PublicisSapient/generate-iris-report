#!/bin/bash

if [ "$#" -ne "2" -a "$#" -ne "3" ]
then
    echo "Usage: $(basename "$0") <input-video> (<output-pdf>|<output-html)" 1>&2
    exit 1
fi

if [[ $2 == *.pdf ]]
then
  OUTPUT_NAME="pdfName=$2"
elif [[ $2 == *.html ]]
then
  OUTPUT_NAME="htmlName=$2"
else 
  echo "Error: $2 must end with .html or .pdf" 1>&2
  exit 2
fi

detect_env() {
  # Safe uname
  local u; u="$(uname -s 2>/dev/null || echo unknown)"

  # Cygwin
  case "$u" in CYGWIN*) echo cygwin; return 0;; esac

  # MSYS2 / Git-Bash / MinGW (Windows "linux-like" shells)
  if [ -n "${MSYSTEM:-}" ]; then
    case "$MSYSTEM" in
      MINGW64|MINGW32|CLANG64|UCRT64) echo mingw; return 0;;
      MSYS) echo msys; return 0;;
    esac
  fi
  case "$u" in MINGW*|MSYS*) echo mingw; return 0;; esac

  # WSL (Windows Subsystem for Linux)
  if [ -e /proc/sys/fs/binfmt_misc/WSLInterop ] || \
     grep -qiE 'microsoft|wsl' /proc/version 2>/dev/null; then
    uname -r | grep -qi 'WSL2' && { echo wsl2; return 0; }
    echo wsl; return 0
  fi

  # macOS
  case "$u" in Darwin) echo macos; return 0;; esac

  # Linux (plain)
  case "$u" in Linux) echo linux; return 0;; esac

  # BSDs
  case "$u" in FreeBSD|OpenBSD|NetBSD) echo bsd; return 0;; esac

  echo unknown
}

OS=`detect_env`

mvn -Djava.awt.headless=true exec:java -Dexec.args="useGui=false $3 videoPath=$1 irisPath=../IRIS/bin/build/$OS-release/example/IrisApp $OUTPUT_NAME"
