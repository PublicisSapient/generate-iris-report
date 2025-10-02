#!/usr/bin/env bash

# $out will be set to the video that is re-encoded, or to $1 if re-encoding is not needed.

set -euo pipefail

# input file
in="$1"   

if [ "$#" -lt "1" -a "$#" -gt "2" ]
then
    echo "Usage: $(basename "$0") <input-video-file> (<output-video-file)" 1>&2
    out=""
else


    # Detect input video codec (e.g., h264, hevc, av1, prores, vp9, ...)
    vcodec="$(ffprobe -v error -select_streams v:0 \
            -show_entries stream=codec_name \
            -of default=noprint_wrappers=1:nokey=1 "$in" \
            | tr '[:upper:]' '[:lower:]')"

    needs_av1_transcode=0
    case "$vcodec" in
        av1) needs_av1_transcode=1 ;;
    esac

    # Get nominal fps as rational (prefer avg_frame_rate)
    afr="$(ffprobe -v error -select_streams v:0 \
        -show_entries stream=avg_frame_rate \
        -of default=noprint_wrappers=1:nokey=1 "$in" || true)"

    if [[ -z "$afr" || "$afr" == "0/0" || "$afr" == "N/A" ]]; then
        afr="$(ffprobe -v error -select_streams v:0 \
            -show_entries stream=r_frame_rate \
            -of default=noprint_wrappers=1:nokey=1 "$in")"
    fi
    [[ "$afr" != */* ]] && afr="${afr}/1"

    num=${afr%/*}
    den=${afr#*/}

    # Compute whether fps is integer and what the target fps should be
    if (( den != 0 && num % den == 0 )); then
        # already integer fps
        target_fps=$(( num / den ))
        needs_fps_integerize=0
    else
        target_fps=$(( (num + den - 1) / den ))   # ceiling
        needs_fps_integerize=1
    fi

    # Decide: do we need to re-encode at all?
    if (( needs_av1_transcode == 0 && needs_fps_integerize == 0 )); then
        # No change needed; report back to caller
    out="$in"
        return 0 2>/dev/null || exit 0
    fi

    # ----- build output name preserving the input extension -----
    base="${in%.*}"
    if [[ "$base" == "$in" ]]; then
        # no extension
    out="${out:-${in}.cfr${target_fps}}"
    else
        ext="${in##*.}"
        out="${out:-${base}.cfr${target_fps}.${ext}}"
    fi

    # ----- choose encoder -----
    # If we are here because of AV1, force H.264 (libx264). Otherwise,
    # your script already uses libx264 for fps changes, so keep that.
    venc="libx264"

    # Container nicety for mp4/mov
    faststart=()
    ext_lc=$(printf '%s' "$ext" | tr '[:upper:]' '[:lower:]')
    case "$ext_lc" in
    mp4|mov) faststart=(-movflags +faststart) ;;
    *)       faststart=() ;;
    esac

    echo "Re-encoding: codec=$vcodec (AV1=$needs_av1_transcode), target_fps=${target_fps} → $out"

    ffmpeg -y -i "$in" \
        -vf "fps=${target_fps}" -r "${target_fps}" -vsync cfr \
        -c:v "${venc}" -crf 18 -preset veryfast \
        -c:a copy \
        "${faststart[@]}" \
        "$out"

    echo "Wrote: $out"
fi
