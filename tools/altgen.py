#!/usr/bin/env python3
"""
altgen.py — caption images to produce alt text (path -> caption JSON)
Refactored to use Salesforce/blip-image-captioning-base.

Usage examples:
  python tools/altgen.py --input ./tmp --output ./tmp/alt.json
  python tools/altgen.py --input /path/list.txt --output desc.json
  python tools/altgen.py --input ./imgs --output desc.json --batch-size 8 --num-beams 3 --max-new-tokens 32
"""

import argparse, json, sys
from pathlib import Path
from typing import List, Dict

from PIL import Image
import torch
from transformers import BlipProcessor, BlipForConditionalGeneration

# ----------------------------
# Helpers
# ----------------------------

def get_device() -> str:
    if torch.cuda.is_available():
        return "cuda"
    if getattr(torch.backends, "mps", None) and torch.backends.mps.is_available():
        return "mps"
    return "cpu"

def load_model(model_id: str, device: str, model_dir: str | None = None):
    dtype = torch.float32
    if model_dir:
        processor = BlipProcessor.from_pretrained(model_dir, local_files_only=True)
        model = BlipForConditionalGeneration.from_pretrained(model_dir, local_files_only=True)
    else:
        processor = BlipProcessor.from_pretrained(model_id)
        model = BlipForConditionalGeneration.from_pretrained(model_id)
    model.to(device, dtype=dtype)
    model.eval()
    return processor, model

def find_images_from_input(inp: Path) -> List[Path]:
    exts = ("*.png", "*.jpg", "*.jpeg", "*.webp", "*.bmp")
    if inp.is_dir():
        paths: List[Path] = []
        for pat in exts:
            paths.extend(sorted(inp.rglob(pat)))
        return paths
    else:
        # text file: one absolute or relative path per line
        paths = []
        for line in inp.read_text(encoding="utf-8").splitlines():
            p = Path(line.strip())
            if p.exists():
                paths.append(p)
        return paths

def caption_batch(
    paths: List[Path],
    processor: BlipProcessor,
    model: BlipForConditionalGeneration,
    device: str,
    num_beams: int,
    max_new_tokens: int,
) -> Dict[str, str]:
    out: Dict[str, str] = {}
    imgs = []
    kept = []
    for p in paths:
        try:
            imgs.append(Image.open(p).convert("RGB"))
            kept.append(p)
        except Exception as e:
            out[str(p)] = f"(alt unavailable: {e})"

    if not imgs:
        return out

    with torch.no_grad():
        inputs = processor(images=imgs, return_tensors="pt").to(device)
        # Ensure float tensors are the model's dtype
        if "pixel_values" in inputs:
            inputs["pixel_values"] = inputs["pixel_values"].to(model.dtype)

        generated = model.generate(
            **inputs,
            num_beams=max(1, num_beams),
            max_new_tokens=max_new_tokens,
            do_sample=False,  # deterministic for accessibility
            pad_token_id=processor.tokenizer.eos_token_id,
        )

        # Decode each sequence
        for i, p in enumerate(kept):
            # When batching, generated.size(0) == len(imgs).
            tokens = generated[i]
            text = processor.decode(tokens, skip_special_tokens=True).strip()
            if text and not text.endswith("."):
                text += "."
            out[str(p)] = text or "Image."
    return out

# ----------------------------
# Main
# ----------------------------

def main():
    ap = argparse.ArgumentParser(description="Generate alt text for images (BLIP).")
    ap.add_argument("--model", default="Salesforce/blip-image-captioning-base",
                    help="HF model id (default: Salesforce/blip-image-captioning-base)")
    ap.add_argument("--input", required=True,
                    help="Directory of images (recursive) OR a text file with one image path per line")
    ap.add_argument("--output", required=True, help="Where to write JSON {path: alt}")
    ap.add_argument("--batch-size", type=int, default=8, help="Batch size for captioning")
    ap.add_argument("--num-beams", type=int, default=3, help="Beam search width (>=1)")
    ap.add_argument("--max-new-tokens", type=int, default=32, help="Max generated tokens per caption")
    ap.add_argument("--model-dir", default=None,
                help="Local directory containing BLIP files (offline). "
                     "If set, --model is ignored.")
    args = ap.parse_args()

    device = get_device()
    processor, model = load_model(args.model, device, args.model_dir)

    inp = Path(args.input)
    out_json = Path(args.output)
    out_json.parent.mkdir(parents=True, exist_ok=True)

    paths = find_images_from_input(inp)
    if not paths:
        print(f"No images found from input: {inp}", file=sys.stderr)
        out_json.write_text("{}", encoding="utf-8")
        return

    results: Dict[str, str] = {}
    bs = max(1, args.batch_size)
    for i in range(0, len(paths), bs):
        batch = paths[i:i+bs]
        try:
            results.update(
                caption_batch(
                    batch, processor, model, device,
                    num_beams=args.num_beams,
                    max_new_tokens=args.max_new_tokens,
                )
            )
        except Exception as e:
            # If something went wrong for the whole batch, mark each
            for p in batch:
                results[str(p)] = f"(alt unavailable: {e})"

    out_json.write_text(json.dumps(results, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"Wrote {len(results)} alts to {out_json}", file=sys.stderr)

if __name__ == "__main__":
    main()
